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

        // If there’s a next lap, totalNext is the total time of the subsequent lap.
        // Since laps are stored newest-first, position + 1 refers to an OLDER lap.
        // The time of the next segment (older segment) is stored in the list.
        if (position + 1 < list.size()) {
            totalNext = list.get(position + 1).totalAtMs;
        } else if (position + 1 == list.size()) {
            // If this is the last item in the list (the first lap recorded),
            // totalNext remains 0L, which is correct (lap time = total time).
        }

        // Duration of current lap = difference between two totals
        return totalThis - totalNext;
    }
}