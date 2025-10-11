package com.example.clockandtimerapp.stopwatch;

import java.util.List;

public class Lap {
    private final long totalAtMs;

    public Lap(long totalAtMs) {
        this.totalAtMs = totalAtMs;
    }

    public long getTotalAtMs() {
        return totalAtMs;
    }

    public long getLapMs(int position, List<Lap> list) {
        long totalThis = totalAtMs;
        long totalNext = 0L;

        // If there’s a next lap, calculate difference
        if (position + 1 < list.size()) {
            totalNext = list.get(position + 1).totalAtMs;
        }

        // Duration of current lap = difference between two totals
        return totalThis - totalNext;
    }
}
