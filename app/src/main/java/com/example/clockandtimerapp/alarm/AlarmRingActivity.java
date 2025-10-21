package com.example.clockandtimerapp.alarm;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clockandtimerapp.R;
import com.example.clockandtimerapp.alarm.model.Alarm;

import java.util.Calendar;

public class AlarmRingActivity extends AppCompatActivity {

    private int alarmId;
    private String label;
    private String ringtone;
    private boolean vibrate;

    private Vibrator vibrator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_ClockDark);
        setContentView(R.layout.activity_alarm_ring);

        // Make sure screen turns on and shows over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // API 27 check
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            // For older APIs, use deprecated flags (if your project supports them)
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        // Get alarm details
        Intent i = getIntent();
        alarmId = i.getIntExtra("id", 0);
        label = i.getStringExtra("label");
        ringtone = i.getStringExtra("ringtone");
        vibrate = i.getBooleanExtra("vibrate", false);

        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(label == null || label.isEmpty() ? "Alarm" : label);

        Button btnDismiss = findViewById(R.id.btnDismiss);
        Button btnSnooze = findViewById(R.id.btnSnooze);

        // Stop any existing sound just in case
        AlarmPlayer.stop();

        // ✅ Either vibrate OR play sound
        if (vibrate) {
            // Optional debug message (you can remove this)
            Toast.makeText(this, "Vibrate ON: Only vibration will run", Toast.LENGTH_SHORT).show();

            // Start vibration slightly delayed (some devices need a small delay)
            new Handler().postDelayed(this::startVibration, 300);
        } else {
            Toast.makeText(this, "Vibrate OFF: Playing sound", Toast.LENGTH_SHORT).show();
            AlarmPlayer.start(this, ringtone, true); // play sound
        }

        btnDismiss.setOnClickListener(v -> {
            dismissNotification(this, alarmId);
            stopVibration();
            AlarmPlayer.stop();
            finish();
        });

        btnSnooze.setOnClickListener(v -> {
            // Schedule same alarm 5 min later
            Calendar c = Calendar.getInstance();
            c.add(Calendar.MINUTE, 5);

            Alarm base = new Alarm(
                    alarmId,
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    label,
                    true,
                    ringtone,
                    vibrate
            );
            AlarmScheduler.schedule(this, base);

            dismissNotification(this, alarmId);
            stopVibration();
            AlarmPlayer.stop();
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVibration();
        AlarmPlayer.stop();
    }

    /**
     * Start vibration only mode.
     */
    private void startVibration() {
        try {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            if (vibrator == null || !vibrator.hasVibrator()) {
                Toast.makeText(this, "Device has no vibrator hardware!", Toast.LENGTH_SHORT).show();
                return;
            }

            long[] pattern = new long[]{0, 1000, 500, 1000}; // vibrate 1s, pause 0.5s, repeat

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(pattern, 0);
            }

        } catch (SecurityException e) {
            Toast.makeText(this, "VIBRATE permission missing!", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Stop vibration if active.
     */
    private void stopVibration() {
        if (vibrator != null) {
            try {
                vibrator.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Dismisses the system notification.
     */
    private void dismissNotification(Context ctx, int id) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(id);
    }
}
