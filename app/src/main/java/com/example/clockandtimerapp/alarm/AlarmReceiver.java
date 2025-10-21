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

        // Start foreground service that shows notification + plays audio
        Intent svc = new Intent(context, AlarmRingService.class)
                .setAction(AlarmRingService.ACTION_START);
        svc.putExtra(EXTRA_ID, id);
        svc.putExtra(EXTRA_LABEL, label);
        svc.putExtra(EXTRA_VIBRATE, vibrate);
        svc.putExtra(EXTRA_RINGTONE, ringtone);

        if (Build.VERSION.SDK_INT >= 26) {
            ContextCompat.startForegroundService(context, svc);
        } else {
            context.startService(svc);
        }
    }
}
