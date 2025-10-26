package com.example.clockandtimerapp.stopwatch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.clockandtimerapp.MainActivity;
import com.example.clockandtimerapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StopwatchService extends Service {

    // --- Actions/Extras ---
    public static final String BASE_ACTION = "com.example.clockandtimerapp.stopwatch";
    public static final String ACTION_START = BASE_ACTION + ".ACTION_START";
    public static final String ACTION_PAUSE = BASE_ACTION + ".ACTION_PAUSE";
    public static final String ACTION_RESET = BASE_ACTION + ".ACTION_RESET";
    public static final String ACTION_LAP = BASE_ACTION + ".ACTION_LAP";
    public static final String ACTION_UPDATE_BROADCAST = BASE_ACTION + ".ACTION_UPDATE";
    public static final String ACTION_REQUEST_STATE = BASE_ACTION + ".ACTION_REQUEST_STATE"; // NEW ACTION

    public static final String EXTRA_ELAPSED = "elapsedMs";
    public static final String EXTRA_RUNNING = "running";
    public static final String EXTRA_LAPS_TOTALS = "lapsTotals";
    public static final String EXTRA_FRAGMENT_TO_LOAD = "fragmentToLoad";

    private static final String CHANNEL_ID = "stopwatch_channel";
    private static final int NOTIF_ID = 1001;
    private static final String TAG = "StopwatchService";

    private boolean running = false;
    private long startRealtime = 0L;
    private long accumulatedMs = 0L;
    private final List<Long> lapsTotals = new ArrayList<>();

    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (running) {
                broadcastUpdate();
                handler.postDelayed(this, 10);
            }
        }
    };

    private final Runnable notificationUpdater = new Runnable() {
        @Override
        public void run() {
            if (running) {
                Notification n = buildNotification(formatTime(getTotalElapsedMs()), true);
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.notify(NOTIF_ID, n);

                handler.postDelayed(this, 1000);
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = (intent != null) ? intent.getAction() : null;

        // CRITICAL FIX 1: IMMEDIATE startForeground()
        if (ACTION_START.equals(action) || running) {
            boolean isRunningForNotif = running || ACTION_START.equals(action);
            startForeground(NOTIF_ID, buildNotification(formatTime(getTotalElapsedMs()), isRunningForNotif));

            if (isRunningForNotif && !running) {
                handler.post(notificationUpdater);
            }
        }

        // Process Intent Action
        if (action != null) {
            switch (action) {
                case ACTION_START:
                    startStopwatch();
                    break;

                case ACTION_PAUSE:
                    pauseStopwatch();
                    stopForeground(false);
                    handler.removeCallbacks(notificationUpdater);
                    break;

                case ACTION_RESET:
                    resetStopwatch();
                    stopForeground(true);
                    stopSelf();
                    handler.removeCallbacks(notificationUpdater);
                    return START_NOT_STICKY;

                case ACTION_LAP:
                    recordLap();
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null && running) nm.notify(NOTIF_ID, buildNotification(formatTime(getTotalElapsedMs()), running));
                    break;

                case ACTION_REQUEST_STATE:
                    // FIX: Fragment requested state, send it immediately.
                    broadcastUpdate();
                    break;
            }
        }

        if (running) {
            // Ticker is handled by startStopwatch()
        }

        return START_NOT_STICKY;
    }

    // --- Timer Logic (Unchanged) ---
    private void startStopwatch() {
        if (!running) {
            startRealtime = SystemClock.elapsedRealtime();
            running = true;
            handler.post(ticker);
            broadcastUpdate();
        }
    }

    private void pauseStopwatch() {
        if (running) {
            accumulatedMs = getTotalElapsedMs();
            running = false;
            handler.removeCallbacks(ticker);
            broadcastUpdate();
        }
    }

    private void resetStopwatch() {
        running = false;
        handler.removeCallbacks(ticker);
        accumulatedMs = 0L;
        startRealtime = 0L;
        lapsTotals.clear();
        broadcastUpdate();
    }

    private void recordLap() {
        long totalNow = getTotalElapsedMs();
        lapsTotals.add(0, totalNow);
        broadcastUpdate();
    }

    private long getTotalElapsedMs() {
        if (running) {
            return accumulatedMs + (SystemClock.elapsedRealtime() - startRealtime);
        } else {
            return accumulatedMs;
        }
    }

    // --- Broadcast/Notification Logic ---
    private void broadcastUpdate() {
        Intent i = new Intent(ACTION_UPDATE_BROADCAST);
        i.putExtra(EXTRA_ELAPSED, getTotalElapsedMs());
        i.putExtra(EXTRA_RUNNING, running);
        long[] arr = new long[lapsTotals.size()];
        for (int k = 0; k < lapsTotals.size(); k++) arr[k] = lapsTotals.get(k);
        i.putExtra(EXTRA_LAPS_TOTALS, arr);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
    }

    private Notification buildNotification(String timeText, boolean isRunning) {
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        openApp.putExtra(EXTRA_FRAGMENT_TO_LOAD, "Stopwatch");

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openApp, pendingFlags());

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Stopwatch Active")
                .setContentText("Elapsed: " + timeText)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(contentIntent)
                .setOngoing(isRunning)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        Intent toggleIntent = new Intent(this, StopwatchService.class);
        toggleIntent.setAction(isRunning ? ACTION_PAUSE : ACTION_START);
        PendingIntent togglePi = PendingIntent.getService(this, 1, toggleIntent, pendingFlags());
        b.addAction(isRunning ? R.drawable.custom_pause_icon : R.drawable.custom_play_icon,
                isRunning ? "Pause" : "Start", togglePi);

        Intent resetIntent = new Intent(this, StopwatchService.class);
        resetIntent.setAction(ACTION_RESET);
        PendingIntent resetPi = PendingIntent.getService(this, 3, resetIntent, pendingFlags());
        b.addAction(R.drawable.custom_reset_icon, "Reset", resetPi);

        return b.build();
    }

    private int pendingFlags() {
        return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID, "Stopwatch", NotificationManager.IMPORTANCE_LOW);
            chan.setDescription("Stopwatch running in background");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(chan);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static String formatTime(long ms) {
        long hours = ms / 3600000;
        long minutes = (ms % 3600000) / 60000;
        long seconds = (ms % 60000) / 1000;
        long hundredths = (ms % 1000) / 10;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}