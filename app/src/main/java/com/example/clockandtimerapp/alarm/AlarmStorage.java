package com.example.clockandtimerapp.alarm;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.clockandtimerapp.alarm.model.Alarm;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public final class AlarmStorage {
    private static final String PREFS = "alarms_prefs";
    private static final String KEY_JSON = "alarms_json";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_NEXT_ID = "next_id";

    // FIX: Added public static
    public static ArrayList<Alarm> load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_JSON, null);
        ArrayList<Alarm> list = new ArrayList<>();
        if (json == null || json.isEmpty()) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                // Backward compatible parse
                int id       = o.optInt("id", i + 1);
                int hour24   = o.optInt("hour24", parseHourFromLegacy(o));
                int minute   = o.optInt("minute", parseMinuteFromLegacy(o));
                String label = o.optString("label",
                        o.optString("subtitle", "Alarm"));
                boolean en   = o.optBoolean("enabled", o.optBoolean("on", false));
                String tone  = o.optString("ringtone", null);
                boolean vib  = o.optBoolean("vibrate", false);
                int dow  = o.optInt("dayOfWeek", -1);
                // skip invalid
                if (hour24 < 0 || minute < 0) continue;
                list.add(new Alarm(id, hour24, minute, label, en, tone, vib, dow));
            }
        } catch (JSONException ignored) {}
        return list;
    }

    // FIX: Added public static
    public static void save(Context ctx, ArrayList<Alarm> alarms) {
        JSONArray arr = new JSONArray();
        for (Alarm a : alarms) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", a.id);
                o.put("hour24", a.hour24);
                o.put("minute", a.minute);
                o.put("label", a.label);
                o.put("enabled", a.enabled);
                o.put("ringtone", a.ringtone);
                o.put("vibrate", a.vibrate);
                o.put("dayOfWeek", a.dayOfWeek);
                arr.put(o);
            } catch (JSONException ignored) {}
        }
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_JSON, arr.toString()).apply();
    }

    /**
     * Checks if an alarm with the same hour, minute, and dayOfWeek already exists.
     * Note: This only checks against currently ENABLED alarms.
     */
    public static boolean isDuplicate(Context ctx, int hour24, int minute, int dayOfWeek) {
        ArrayList<Alarm> existingAlarms = load(ctx);
        for (Alarm a : existingAlarms) {
            if (a.enabled && a.hour24 == hour24 && a.minute == minute && a.dayOfWeek == dayOfWeek) {
                return true;
            }
        }
        return false;
    }


    // FIX: Added public static
    public static boolean isInitialized(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_INITIALIZED, false);
    }

    // FIX: Added public static
    public static void setInitialized(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_INITIALIZED, true).apply();
    }

    // FIX: Added public static
    public static int nextId(Context ctx){
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int v = sp.getInt(KEY_NEXT_ID, 1);
        sp.edit().putInt(KEY_NEXT_ID, v + 1).apply();
        return v;
    }

    // ------- legacy helpers -------
    private static int parseHourFromLegacy(JSONObject o){
        // legacy "time" like "04:30 AM"
        String t = o.optString("time", null);
        return timeToHour(t);
    }
    private static int parseMinuteFromLegacy(JSONObject o){
        String t = o.optString("time", null);
        return timeToMinute(t);
    }
    private static int timeToHour(String t){
        if (t == null) return -1;
        try {
            String[] parts = t.split(" ");
            String[] hm = parts[0].split(":");
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);
            boolean pm = parts.length > 1 && "PM".equalsIgnoreCase(parts[1]);
            if (h == 12) h = 0;
            if (pm) h += 12;
            return h;
        } catch(Exception e){ return -1; }
    }
    private static int timeToMinute(String t){
        if (t == null) return -1;
        try {
            String[] parts = t.split(" ");
            String[] hm = parts[0].split(":");
            return Integer.parseInt(hm[1]);
        } catch(Exception e){ return -1; }
    }
}