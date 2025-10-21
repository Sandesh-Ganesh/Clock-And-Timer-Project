package com.example.clockandtimerapp.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateFormat;

/**
 * Handles persistence for the user's preferred time format (12-hour or 24-hour).
 */
public class TimeFormatPreference {
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_TIME_FORMAT_24H = "is_24_hour_format";

    public static void set24HourFormat(Context context, boolean is24H) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sharedPref.edit().putBoolean(KEY_TIME_FORMAT_24H, is24H).apply();
    }

    public static boolean is24HourFormat(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Default to the system's current setting if not explicitly saved
        return sharedPref.getBoolean(KEY_TIME_FORMAT_24H, DateFormat.is24HourFormat(context));
    }

    public static String getHourFormatPattern(Context context) {
        // Return "HH:mm" for 24hr or "h:mm" for 12hr
        return is24HourFormat(context) ? "HH:mm" : "h:mm";
    }
}