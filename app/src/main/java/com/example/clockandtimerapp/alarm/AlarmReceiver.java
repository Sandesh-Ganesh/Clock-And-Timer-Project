package com.example.clockandtimerapp.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String EXTRA_ID       = "id";
    private static final String EXTRA_LABEL    = "label";
    private static final String EXTRA_VIBRATE  = "vibrate";
    private static final String EXTRA_RINGTONE = "ringtone";

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(EXTRA_ID, 999999);
        String label = intent.getStringExtra(EXTRA_LABEL);
        boolean vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, false);
        String ringtone = intent.getStringExtra(EXTRA_RINGTONE);

        // --- NEW BEHAVIOR: Launch the full-screen AlarmRingActivity ---
        Intent activityIntent = new Intent(context, AlarmRingActivity.class);

        // Pass necessary alarm data to the activity
        activityIntent.putExtra("id", id);
        activityIntent.putExtra("label", label);
        activityIntent.putExtra("vibrate", vibrate);
        activityIntent.putExtra("ringtone", ringtone);

        // Flags to ensure the activity can launch from the BroadcastReceiver and over the lock screen
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        context.startActivity(activityIntent);

    }
}