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

        if (position + 1 < list.size()) {
            totalNext = list.get(position + 1).totalAtMs;
        } else if (position + 1 == list.size()) {
        }

        return totalThis - totalNext;
    }
}