package com.example.clockandtimerapp.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.example.clockandtimerapp.alarm.model.Alarm;

import java.util.Calendar;

public final class AlarmScheduler {

    // FIX: Added public static
    public static void schedule(Context ctx, Alarm alarm){
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);

        // --- 1. RUNTIME PERMISSION CHECK FOR EXACT ALARMS (API 31+) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            if (!am.canScheduleExactAlarms()) {

                // Permission Denied: Notify user and redirect to settings
                Toast.makeText(ctx,
                        "Please grant 'Alarms & reminders' permission to set an exact alarm.",
                        Toast.LENGTH_LONG).show();

                // Launch the system setting screen to request the permission
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);

                // Must use the NEW_TASK flag since this is called from a non-Activity context (Fragment/Service)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);

                return; // HALT execution if permission is denied
            }
        }

        // --- 2. EXISTING SCHEDULING LOGIC (Only runs if permission is granted or not needed) ---

        // This PI is the Broadcast that fires when the alarm time hits.
        PendingIntent piTrigger = buildPendingIntent(ctx, alarm);

        // --- MODIFICATION: Create an Activity PI for the AlarmClockInfo (optional but best practice) ---
        // This Intent tells the system what to launch if the user taps the alarm icon on the status bar/lock screen.
        Intent showIntent = new Intent(ctx, AlarmRingActivity.class);
        showIntent.putExtra("id", alarm.id);
        showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent piShow = PendingIntent.getActivity(
                ctx,
                alarm.id, // Use the same request code as the trigger for consistency
                showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // --- END MODIFICATION ---

        long trigger = nextTriggerUtcMillis(alarm.hour24, alarm.minute, alarm.dayOfWeek);

        // Use AlarmClockInfo for user-visible alarms
        // Use piShow as the ShowIntent
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(trigger, piShow);
        am.setAlarmClock(info, piTrigger); // piTrigger is the intent that calls AlarmReceiver
    }

    // FIX: Added public static
    public static void cancel(Context ctx, Alarm alarm){
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        am.cancel(buildPendingIntent(ctx, alarm));
    }

    private static PendingIntent buildPendingIntent(Context ctx, Alarm alarm){
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.setAction("com.example.alarm.ACTION_FIRE");
        i.putExtra("id", alarm.id);
        i.putExtra("label", alarm.label);
        i.putExtra("ringtone", alarm.ringtone);
        i.putExtra("vibrate", alarm.vibrate);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(ctx, alarm.id, i, flags);
    }

    private static long nextTriggerUtcMillis(int hour24, int minute, int dayOfWeek){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.HOUR_OF_DAY, hour24);
        c.set(Calendar.MINUTE, minute);
        long now = System.currentTimeMillis();
        if (dayOfWeek >= Calendar.SUNDAY && dayOfWeek <= Calendar.SATURDAY) {
            int today = c.get(Calendar.DAY_OF_WEEK);
            int daysUntil = (dayOfWeek - today + 7) % 7;
            // If it's the same day but time already passed, schedule 7 days later
            if (daysUntil == 0 && c.getTimeInMillis() <= now) {
                daysUntil = 7;
            }
            c.add(Calendar.DAY_OF_YEAR, daysUntil);
        } else {
            // No specific day selected -> next occurrence (today or tomorrow)
            if (c.getTimeInMillis() <= now) {
                c.add(Calendar.DAY_OF_YEAR, 1);
            }
        }
        return c.getTimeInMillis();
    }
}