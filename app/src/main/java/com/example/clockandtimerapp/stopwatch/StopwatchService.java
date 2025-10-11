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

    // --- Actions/Extras (Unified Constants) ---
    public static final String BASE_ACTION = "com.example.clockandtimerapp.stopwatch";
    public static final String ACTION_START = BASE_ACTION + ".ACTION_START";
    public static final String ACTION_PAUSE = BASE_ACTION + ".ACTION_PAUSE";
    public static final String ACTION_RESET = BASE_ACTION + ".ACTION_RESET";
    public static final String ACTION_LAP = BASE_ACTION + ".ACTION_LAP";
    public static final String ACTION_UPDATE_BROADCAST = BASE_ACTION + ".ACTION_UPDATE";
    public static final String ACTION_HIDE_NOTIFICATION = BASE_ACTION + ".ACTION_HIDE_NOTIFICATION"; // NEW
    public static final String ACTION_SHOW_NOTIFICATION = BASE_ACTION + ".ACTION_SHOW_NOTIFICATION"; // NEW

    public static final String EXTRA_ELAPSED = "elapsedMs";
    public static final String EXTRA_RUNNING = "running";
    public static final String EXTRA_LAPS_TOTALS = "lapsTotals";
    public static final String EXTRA_FRAGMENT_TO_LOAD = "fragmentToLoad"; // NEW for navigation

    private static final String CHANNEL_ID = "stopwatch_channel";
    private static final int NOTIF_ID = 1001;
    private static final String TAG = "StopwatchService";

    private boolean running = false;
    private long startRealtime = 0L;
    private long accumulatedMs = 0L;
    private long lastLapTotalMs = 0L;
    private final List<Long> lapsTotals = new ArrayList<>();

    private final Handler handler = new Handler();
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (running) {
                broadcastUpdate();
                // We update the notification less often to save battery,
                // but the UI broadcasts happen every 10ms.
                // updateNotification();
                handler.postDelayed(this, 10); // FASTER TICKER (10ms)
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
        if (intent != null && intent.getAction() != null) {
            String a = intent.getAction();
            switch (a) {
                case ACTION_START:
                    startStopwatch();
                    startForeground(NOTIF_ID, buildNotification(formatTime(getTotalElapsedMs()), running)); // Start FG on START
                    break;
                case ACTION_PAUSE:
                    pauseStopwatch();
                    stopForeground(false); // Keep notification visible on pause (optional)
                    break;
                case ACTION_RESET:
                    resetStopwatch();
                    stopForeground(true); // Remove notification on reset
                    break;
                case ACTION_LAP:
                    recordLap();
                    break;
                case ACTION_HIDE_NOTIFICATION: // NEW
                    stopForeground(true);
                    break;
                case ACTION_SHOW_NOTIFICATION: // NEW
                    if (running) {
                        startForeground(NOTIF_ID, buildNotification(formatTime(getTotalElapsedMs()), running));
                    }
                    break;
            }
        }

        // If service is started without a specific action, ensure state is maintained
        if (running) {
            // We only call startForeground/stopForeground via specific actions now
            // startForeground(NOTIF_ID, buildNotification(formatTime(getTotalElapsedMs()), running));
        }

        return START_STICKY;
    }

    // --- Timer Logic (startStopwatch, pauseStopwatch, etc.) remains the same ---
    private void startStopwatch() {
        if (!running) {
            startRealtime = SystemClock.elapsedRealtime();
            running = true;
            handler.post(ticker);
            broadcastUpdate();
            updateNotification(); // Initial notification update
            Log.d(TAG, "started");
        }
    }

    private void pauseStopwatch() {
        if (running) {
            accumulatedMs = getTotalElapsedMs();
            running = false;
            handler.removeCallbacks(ticker);
            broadcastUpdate();
            updateNotification();
            Log.d(TAG, "paused");
        }
    }

    private void resetStopwatch() {
        running = false;
        handler.removeCallbacks(ticker);
        accumulatedMs = 0L;
        startRealtime = 0L;
        lastLapTotalMs = 0L;
        lapsTotals.clear();
        broadcastUpdate();
        updateNotification();
        Log.d(TAG, "reset");
    }

    private void recordLap() {
        long totalNow = getTotalElapsedMs();
        lastLapTotalMs = totalNow;
        lapsTotals.add(0, totalNow); // newest-first
        broadcastUpdate();
        updateNotification();
        Log.d(TAG, "lap");
    }

    private long getTotalElapsedMs() {
        if (running) {
            return accumulatedMs + (SystemClock.elapsedRealtime() - startRealtime);
        } else {
            return accumulatedMs;
        }
    }
    // --- End Timer Logic ---


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

    private void updateNotification() {
        // Only update notification every 1 second (1000ms) to save battery/resources
        handler.removeCallbacks(notificationUpdater);
        handler.postDelayed(notificationUpdater, 1000);
    }

    private final Runnable notificationUpdater = new Runnable() {
        @Override
        public void run() {
            Notification n = buildNotification(formatTime(getTotalElapsedMs()), running);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(NOTIF_ID, n);

            // Re-schedule for next second if running
            if (running) {
                handler.postDelayed(this, 1000);
            }
        }
    };


    private Notification buildNotification(String timeText, boolean running) {
        // FIX: Add EXTRA_FRAGMENT_TO_LOAD to ensure MainActivity opens the correct tab
        Intent openApp = new Intent(this, com.example.clockandtimerapp.MainActivity.class);
        openApp.putExtra(EXTRA_FRAGMENT_TO_LOAD, "Stopwatch");
        openApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Good practice

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, openApp, pendingFlags());

        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Stopwatch Running")
                .setContentText("Elapsed: " + timeText)
                .setSmallIcon(R.drawable.ic_play_24)
                .setColor(getColorCompat(R.color.sw_accent))
                .setContentIntent(contentIntent)
                .setOngoing(running) // Only set ongoing if timer is running
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        // Toggle action
        Intent toggleIntent = new Intent(this, StopwatchService.class);
        toggleIntent.setAction(running ? ACTION_PAUSE : ACTION_START);
        PendingIntent togglePi = PendingIntent.getService(this, 1, toggleIntent, pendingFlags());
        b.addAction(running ? R.drawable.ic_pause_24 : R.drawable.ic_play_24,
                running ? "Pause" : "Start", togglePi);

        // Lap action (only if running)
        if (running) {
            Intent lapIntent = new Intent(this, StopwatchService.class);
            lapIntent.setAction(ACTION_LAP);
            PendingIntent lapPi = PendingIntent.getService(this, 2, lapIntent, pendingFlags());
            b.addAction(R.drawable.ic_flag_24, "Lap", lapPi);
        }

        // Reset action (always available)
        Intent resetIntent = new Intent(this, StopwatchService.class);
        resetIntent.setAction(ACTION_RESET);
        PendingIntent resetPi = PendingIntent.getService(this, 3, resetIntent, pendingFlags());
        b.addAction(R.drawable.ic_reset_24, "Reset", resetPi);

        return b.build();
    }
    // ... (rest of helper methods: pendingFlags, getColorCompat, createNotificationChannel, onBind, formatTime)
    private int pendingFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }

    private int getColorCompat(int colorRes) {
        try {
            return getResources().getColor(colorRes, null);
        } catch (Exception e) {
            return getResources().getColor(colorRes);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID, "Stopwatch", NotificationManager.IMPORTANCE_LOW);
            chan.setDescription("Stopwatch running");
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
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }
}