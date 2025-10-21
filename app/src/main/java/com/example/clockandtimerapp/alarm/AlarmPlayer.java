package com.example.clockandtimerapp.alarm;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.Settings;

public class AlarmPlayer {

    private static MediaPlayer player;

    public static synchronized void start(Context ctx, String ringtone, boolean playSound) {
        stop(); // stop any previous sound first

        if (!playSound) {
            return; // 🔕 do nothing (vibration-only mode)
        }

        try {
            Uri uri;
            if (ringtone != null && !ringtone.isEmpty()) {
                uri = Uri.parse(ringtone);
            } else {
                uri = Settings.System.DEFAULT_ALARM_ALERT_URI;
            }

            player = MediaPlayer.create(ctx.getApplicationContext(), uri);
            if (player != null) {
                player.setLooping(true);
                player.start();
            }
        } catch (Exception ignored) {
        }
    }

    public static synchronized void stop() {
        try {
            if (player != null) {
                if (player.isPlaying()) {
                    player.stop();
                }
                player.release();
            }
        } catch (Exception ignored) {
        }
        player = null;
    }
}
