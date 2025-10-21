package com.example.clockandtimerapp.worldclock;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles persistence for the list of world clocks using SharedPreferences and Gson.
 */
public class WorldClockManager {
    private static final String PREF_NAME = "world_clock_data";
    private static final String KEY_CLOCK_LIST = "saved_clocks";
    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public WorldClockManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public List<TimezoneInfo> loadClocks() {
        String json = sharedPreferences.getString(KEY_CLOCK_LIST, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<TimezoneInfo>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void saveClocks(List<TimezoneInfo> clockList) {
        String json = gson.toJson(clockList);
        sharedPreferences.edit().putString(KEY_CLOCK_LIST, json).apply();
    }

    public void addClock(TimezoneInfo clock, List<TimezoneInfo> currentList) {
        if (!currentList.contains(clock)) {
            // IMPORTANT: Add the new clock to the list instance passed from the fragment
            currentList.add(0, clock);

            // IMPORTANT: Save the *updated* list to SharedPreferences
            saveClocks(currentList);
        }
    }

    public void deleteClock(TimezoneInfo clock, List<TimezoneInfo> currentList) {
        currentList.remove(clock);
        saveClocks(currentList);
    }
}