package com.example.clockandtimerapp.alarm.model;

import android.net.Uri;

public class Alarm {
    public int id;           // unique per alarm (used for PendingIntent requestCode)
    public int hour24;       // 0..23
    public int minute;       // 0..59
    public String label;     // user label
    public boolean enabled;  // scheduled or not
    public String ringtone;  // Uri.toString() or null
    public boolean vibrate;  // vibrate on fire
    public int dayOfWeek;    // Calendar.SUNDAY..SATURDAY, or -1 for 'no specific day'

    public Alarm(int id, int hour24, int minute, String label, boolean enabled, String ringtone, boolean vibrate){
        this.id = id;
        this.hour24 = hour24;
        this.minute = minute;
        this.label = label;
        this.enabled = enabled;
        this.ringtone = ringtone;
        this.vibrate = vibrate;
        this.dayOfWeek = -1;
    }

    public Alarm(int id, int hour24, int minute, String label, boolean enabled, String ringtone, boolean vibrate, int dayOfWeek){
        this.id = id;
        this.hour24 = hour24;
        this.minute = minute;
        this.label = label;
        this.enabled = enabled;
        this.ringtone = ringtone;
        this.vibrate = vibrate;
        this.dayOfWeek = dayOfWeek;
    }

    public String displayTime() {
        int h12 = hour24 % 12; if (h12 == 0) h12 = 12;
        String ampm = hour24 < 12 ? "AM" : "PM";
        return String.format("%02d:%02d %s", h12, minute, ampm);
    }

    public String displaySubtitle() {
        return label == null || label.isEmpty() ? "Alarm" : label;
    }

    public Uri ringtoneUriOrDefault(){
        if (ringtone == null || ringtone.isEmpty()) return null;
        return Uri.parse(ringtone);
    }
}
