package com.example.clockandtimerapp.alarm;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmRingService extends Service {

    public static final String ACTION_START    = "com.example.alarm.ACTION_START_RING";
    public static final String ACTION_DISMISS  = "com.example.alarm.ACTION_DISMISS_RING";
    public static final String ACTION_SNOOZE   = "com.example.alarm.ACTION_SNOOZE_RING";
    public static final String ACTION_RENOTIFY = "com.example.alarm.ACTION_RENOTIFY";

    private static final String EXTRA_ID       = "id";
    private static final String EXTRA_LABEL    = "label";
    private static final String EXTRA_VIBRATE  = "vibrate";
    private static final String EXTRA_RINGTONE = "ringtone";

    // Silent, high-importance channel — media audio is handled by MediaPlayer
    private static final String CH_ID = "alarm_ring_channel_v3_silent";
    private static final int    FALLBACK_NOTIF_ID = 999999;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private AlarmManager alarmManager;

    @Override public void onCreate() {
        super.onCreate();
        createChannelIfNeeded();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final String action = intent.getAction();

        if (ACTION_DISMISS.equals(action)) {
            handleDismiss();
            return START_NOT_STICKY;
        }

        if (ACTION_SNOOZE.equals(action)) {
            handleSnooze(intent);
            return START_NOT_STICKY;
        }

        if (ACTION_RENOTIFY.equals(action)) {
            int id = intent.getIntExtra(EXTRA_ID, FALLBACK_NOTIF_ID);
            String label = intent.getStringExtra(EXTRA_LABEL);
            postRingingNotification(id, label);
            return START_STICKY;
        }

        if (ACTION_START.equals(action)) {
            handleStart(intent);
        }

        return START_STICKY;
    }

    private void handleStart(Intent intent) {
        final int notifId = intent.getIntExtra(EXTRA_ID, FALLBACK_NOTIF_ID);
        final String label = intent.getStringExtra(EXTRA_LABEL);
        final boolean vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, false);
        final String ringtoneStr = intent.getStringExtra(EXTRA_RINGTONE);

        try {
            Notification notif = buildRingingNotification(notifId, label);
            startForeground(notifId == 0 ? FALLBACK_NOTIF_ID : notifId, notif);
        } catch (Throwable ignored) {
            postRingingNotification(notifId, label);
        }

        scheduleReNotify(notifId, label);
        startRinging(vibrate, ringtoneStr);
    }

    private void handleDismiss() {
        stopRinging();
        stopForegroundSafely();
        stopSelf();
    }

    private void handleSnooze(Intent intent) {
        stopRinging();
        stopForegroundSafely();

        int id = intent.getIntExtra(EXTRA_ID, FALLBACK_NOTIF_ID);
        String label = intent.getStringExtra(EXTRA_LABEL);

        long triggerAt = System.currentTimeMillis() + 5 * 60 * 1000L;

        Intent fire = new Intent(this, AlarmReceiver.class);
        fire.putExtra(EXTRA_ID, id);
        fire.putExtra(EXTRA_LABEL, label);

        int flagsPi = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(this, id, fire, flagsPi);

        // Add API level check and handle potential SecurityException
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } else {
                    // Fallback for when exact alarms are denied by the user
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            } else {
                // For older Android versions, call the original method
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } catch (SecurityException e) {
            // As a last resort, if permission is missing, use a non-exact alarm
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            //e.printStackTrace(); // Log the exception for debugging
        }

        Notification n = new NotificationCompat.Builder(this, CH_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(label == null || label.isEmpty() ? "Alarm" : label)
                .setContentText("Snoozed for 5 minutes")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(this).notify(id + 500000, n);

        stopSelf();
    }


    private Notification buildRingingNotification(int id, @Nullable String label) {
        int flagsPi = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        Intent dismissIntent = new Intent(this, AlarmRingService.class).setAction(ACTION_DISMISS);
        PendingIntent dismissPi = PendingIntent.getService(this, id + 2000, dismissIntent, flagsPi);

        Intent snoozeI = new Intent(this, AlarmRingService.class).setAction(ACTION_SNOOZE);
        snoozeI.putExtra(EXTRA_ID, id);
        snoozeI.putExtra(EXTRA_LABEL, label);
        PendingIntent snoozePi = PendingIntent.getService(this, id + 3000, snoozeI, flagsPi);

        return new NotificationCompat.Builder(this, CH_ID)
                .setContentTitle(label == null || label.isEmpty() ? "Alarm" : label)
                .setContentText("Ringing…")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setDefaults(0) // channel stays silent; MediaPlayer handles audio
                .addAction(android.R.drawable.ic_media_next, "Snooze 5 min", snoozePi)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPi)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Ringing…"))
                .build();
    }

    private void scheduleReNotify(int notifId, String label) {
        try {
            Intent renotify = new Intent(this, AlarmRingService.class).setAction(ACTION_RENOTIFY);
            renotify.putExtra(EXTRA_ID, notifId);
            renotify.putExtra(EXTRA_LABEL, label);

            int requestCode = (notifId == 0 ? FALLBACK_NOTIF_ID : notifId) + 7777;
            int flagsPi = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
            PendingIntent rePi = PendingIntent.getService(this, requestCode, renotify, flagsPi);

            long triggerAt = System.currentTimeMillis() + 5 * 60 * 1000L;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, rePi);
                } else {
                    // Fallback for when exact alarms are denied by the user.
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, rePi);
                }
            } else {
                // For older Android versions, the permission is granted by default.
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, rePi);
            }
        } catch (SecurityException e) {
            // The SecurityException is caught here if permission is denied.
            e.printStackTrace(); // Log the exception for debugging.
        } catch (Throwable ignored) {
            // The original catch block remains for other potential issues.
        }
    }



    private void postRingingNotification(int id, @Nullable String label) {
        Notification n = buildRingingNotification(id, label);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        NotificationManagerCompat.from(this).notify(id == 0 ? FALLBACK_NOTIF_ID : id, n);
    }

    private void startRinging(boolean vibrateEnabled, @Nullable String ringtoneStr) {
        stopRinging(); // clean state

        // Vibrate pattern while ringing (loops)
        if (vibrateEnabled && vibrator != null && vibrator.hasVibrator()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect effect = VibrationEffect.createWaveform(
                            new long[]{0, 600, 400}, 0);
                    vibrator.vibrate(effect);
                } else {
                    vibrator.vibrate(new long[]{0, 600, 400}, 0);
                }
            } catch (Throwable ignored) { }
        }

        // Loop the selected tone with MediaPlayer
        try {
            Uri uri = (ringtoneStr != null && !ringtoneStr.isEmpty()) ? Uri.parse(ringtoneStr) : null;
            if (uri == null) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (uri == null) return; // No ringtone found

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setLooping(true);
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopRinging();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (Throwable ignored) {
            // if it fails, notification still shows and vibration may run
        }
    }

    private void stopRinging() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Throwable ignored) { }
            mediaPlayer = null;
        }

        if (vibrator != null) {
            try { vibrator.cancel(); } catch (Throwable ignored) { }
        }
    }

    private void stopForegroundSafely() {
        try {
            stopForeground(true);
        } catch (Throwable ignored) { }
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        // SILENT high-importance channel: no beep; we control audio via MediaPlayer
        NotificationChannel ch = new NotificationChannel(
                CH_ID, "Alarm (Silent Channel)", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Alarm ringing (notification only, audio handled by app)");
        ch.enableVibration(false);
        ch.setSound(null, null);
        ch.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try { nm.createNotificationChannel(ch); } catch (Throwable ignored) { }
    }

    @Override public void onDestroy() {
        stopRinging();
        super.onDestroy();
    }
}
