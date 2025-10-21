package com.example.clockandtimerapp.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class NotificationUtils {

    public static final String CHANNEL_SOUND = "alarm_sound";
    public static final String CHANNEL_SILENT = "alarm_silent";

    public static void createChannels(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        // Sound channel – plays the device's default alarm sound (or set a custom Uri)
        if (nm.getNotificationChannel(CHANNEL_SOUND) == null) {
            NotificationChannel chSound = new NotificationChannel(
                    CHANNEL_SOUND,
                    "Alarm (Sound)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            chSound.setSound(sound, attrs);
            chSound.enableVibration(false);
            nm.createNotificationChannel(chSound);
        }

        // Silent channel – absolutely no sound (for vibrate-only mode)
        if (nm.getNotificationChannel(CHANNEL_SILENT) == null) {
            NotificationChannel chSilent = new NotificationChannel(
                    CHANNEL_SILENT,
                    "Alarm (Silent)",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chSilent.setSound(null, null);   // <-- key: no sound
            chSilent.enableVibration(true);  // allow vibration
            nm.createNotificationChannel(chSilent);
        }
    }
}
