package com.example.clockandtimerapp.worldclock;
import java.io.Serializable;
import java.util.Objects;

public class TimezoneInfo implements Serializable {

    private final String countryName;
    private final String cityName;
    private final String timezoneId; // e.g., "America/New_York", "Asia/Dubai"
    private long differenceMinutes; // Pre-calculated difference from local time for display text

    public TimezoneInfo(String countryName, String cityName, String timezoneId, long differenceMinutes) {
        this.countryName = countryName;
        this.cityName = cityName;
        this.timezoneId = timezoneId;
        this.differenceMinutes = differenceMinutes;
    }

    // Getters
    public String getCountryName() { return countryName; }
    public String getCityName() { return cityName; }
    public String getTimezoneId() { return timezoneId; }
    public long getDifferenceMinutes() { return differenceMinutes; }

    // This setter is included to allow updating the time difference (e.g., due to DST changes)
    public void setDifferenceMinutes(long differenceMinutes) {
        this.differenceMinutes = differenceMinutes;
    }

    // Used for comparison/de-duplication based on timezoneId
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimezoneInfo that = (TimezoneInfo) o;
        return timezoneId.equals(that.timezoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timezoneId);
    }
}
