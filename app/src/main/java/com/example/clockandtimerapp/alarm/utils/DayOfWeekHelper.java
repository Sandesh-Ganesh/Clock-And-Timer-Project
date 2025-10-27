package com.example.clockandtimerapp.alarm.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DayOfWeekHelper {

    private static String getDayName(int dayOfWeek) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        // Use full display name (e.g., "Monday")
        return c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
    }

    public static List<String> getRelativeDayList() {
        List<String> dayList = new ArrayList<>(7);
        Calendar calendar = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            String displayDay;
            int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            String fullDayName = getDayName(currentDayOfWeek);

            if (i == 0) {
                displayDay = "Today";
            } else if (i == 1) {
                displayDay = "Tomorrow";
            } else {
                displayDay = fullDayName;
            }

            // Append the actual day name in parenthesis if using Today/Tomorrow, otherwise just the name.
            String finalDisplay = (i == 0 || i == 1) ? displayDay + " (" + fullDayName + ")" : fullDayName;

            dayList.add(finalDisplay);

            // Move the calendar forward one day for the next iteration
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return dayList;
    }
}