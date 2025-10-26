package com.example.clockandtimerapp.stopwatch;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockandtimerapp.R;
import com.example.clockandtimerapp.stopwatch.Lap;
import com.example.clockandtimerapp.stopwatch.LapAdapter;
import com.example.clockandtimerapp.stopwatch.StopwatchService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StopwatchFragment extends Fragment {

    private boolean isRunning = false;
    private TextView txtElapsed;
    private RecyclerView rvLaps;
    private LapAdapter lapAdapter;
    private final List<Lap> lapData = new ArrayList<>();
    private long lastTotalElapsedMs = 0L;

    // Receiver to get state updates from StopwatchService
    private final BroadcastReceiver stopwatchUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StopwatchService.ACTION_UPDATE_BROADCAST.equals(intent.getAction())) {
                long elapsedMs = intent.getLongExtra(StopwatchService.EXTRA_ELAPSED, 0L);
                boolean serviceIsRunning = intent.getBooleanExtra(StopwatchService.EXTRA_RUNNING, false);
                long[] lapsArray = intent.getLongArrayExtra(StopwatchService.EXTRA_LAPS_TOTALS);

                lastTotalElapsedMs = elapsedMs;
                txtElapsed.setText(formatTime(elapsedMs));

                if (isRunning != serviceIsRunning) {
                    isRunning = serviceIsRunning;
                    updatePlayPauseIcon(getView());
                }

                // Update Lap List: Convert raw long[] to List<Lap>
                if (lapsArray != null) {
                    List<Lap> newLapList = new ArrayList<>(lapsArray.length);
                    for (long totalTime : lapsArray) {
                        newLapList.add(new Lap(totalTime));
                    }
                    lapAdapter.submit(newLapList);
                }
            }
        }
    };

    private void sendServiceAction(String action) {
        Intent intent = new Intent(requireContext(), StopwatchService.class);
        intent.setAction(action);

        if (StopwatchService.ACTION_START.equals(action)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireContext().startForegroundService(intent);
            } else {
                requireContext().startService(intent);
            }
        } else {
            requireContext().startService(intent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stopwatch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        txtElapsed = view.findViewById(R.id.txtElapsed);
        ImageView btnStartPause = view.findViewById(R.id.btnStartPause);
        ImageView btnReset = view.findViewById(R.id.btnReset);
        ImageView btnLap = view.findViewById(R.id.btnLap);
        rvLaps = view.findViewById(R.id.rvLaps);

        lapAdapter = new LapAdapter(lapData);
        rvLaps.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLaps.setAdapter(lapAdapter);

        // 1. Play/Pause Button Logic
        btnStartPause.setOnClickListener(v -> {
            if (isRunning) {
                sendServiceAction(StopwatchService.ACTION_PAUSE);
            } else {
                sendServiceAction(StopwatchService.ACTION_START);
            }
        });

        // 2. Reset Button Logic - Check for stats
        btnReset.setOnClickListener(v -> {
            boolean lapsExist = !lapAdapter.data.isEmpty();
            boolean timeExists = lastTotalElapsedMs > 0;

            if (lapsExist) {
                // Scenario 1: Laps exist. ALWAYS show stats dialog before resetting.
                showStatsDialog();
            } else if (timeExists) {
                // Scenario 2: Time exists, but no laps. Perform immediate, silent reset.
                sendServiceAction(StopwatchService.ACTION_RESET);
                txtElapsed.setText(formatTime(0));
                updatePlayPauseIcon(view);
            } else {
                // Scenario 3: Time is 0 and no laps. Do nothing (already reset).
                // We still call the reset action for safety, though it shouldn't be necessary.
                sendServiceAction(StopwatchService.ACTION_RESET);
            }
        });

        // 3. Lap Button Logic
        btnLap.setOnClickListener(v -> {
            if (isRunning) {
                sendServiceAction(StopwatchService.ACTION_LAP);
            }
        });

        updatePlayPauseIcon(view);
        txtElapsed.setText(formatTime(lastTotalElapsedMs));

        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.toolbar_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        };

        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner());
    }

    private void showStatsDialog() {
        List<Lap> allLaps = lapAdapter.data;
        if (allLaps.isEmpty()) return;

        long minLapTime = Long.MAX_VALUE;
        long maxLapTime = Long.MIN_VALUE;
        long totalLapTime = 0;
        int lapCount = allLaps.size();

        // Calculate the segment times to find min/max/average
        for (int i = 0; i < lapCount; i++) {
            long segment = allLaps.get(i).getLapMs(i, allLaps);

            totalLapTime += segment;
            if (segment < minLapTime) minLapTime = segment;
            if (segment > maxLapTime) maxLapTime = segment;
        }

        long avgLapTime = lapCount > 0 ? totalLapTime / lapCount : 0L;

        // Build the final statistics message
        StringBuilder statSummary = new StringBuilder();
        statSummary.append(String.format(Locale.getDefault(), "Total Time: %s\n\n", formatTime(lastTotalElapsedMs)));
        statSummary.append(String.format(Locale.getDefault(), "Fastest Lap: %s\n", formatTime(minLapTime)));
        statSummary.append(String.format(Locale.getDefault(), "Slowest Lap: %s\n", formatTime(maxLapTime)));
        statSummary.append(String.format(Locale.getDefault(), "Average Lap: %s", formatTime(avgLapTime)));

        // Show the dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Stopwatch Statistics")
                .setMessage(statSummary.toString())
                .setPositiveButton("Reset", (dialog, which) -> {
                    // Reset the service AND UI after viewing stats
                    sendServiceAction(StopwatchService.ACTION_RESET);
                    txtElapsed.setText(formatTime(0));
                    updatePlayPauseIcon(getView());
                    lapAdapter.submit(Arrays.asList()); // Clear the list
                    lastTotalElapsedMs = 0L;
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    @Override
    public void onStart() {
        super.onStart();

        // 1. Register the BroadcastReceiver
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                stopwatchUpdateReceiver, new IntentFilter(StopwatchService.ACTION_UPDATE_BROADCAST));

        // 2. FIX: Request the current state from the running service
        // This makes sure the UI updates instantly when returning from the notification.
        sendServiceAction(StopwatchService.ACTION_REQUEST_STATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(stopwatchUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendServiceAction(StopwatchService.ACTION_RESET);
    }

    private void updatePlayPauseIcon(View view) {
        if (view == null) return;
        ImageView btn = view.findViewById(R.id.btnStartPause);
        if (btn != null) {
            btn.setImageResource(isRunning ? R.drawable.ic_pause_24 : R.drawable.ic_play_24);
        }
    }

    private static String formatTime(long ms) {
        long hours = ms / 3600000;
        long minutes = (ms % 3600000) / 60000;
        long seconds = (ms % 60000) / 1000;
        long hundredths = (ms % 1000) / 10;
        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", hours, minutes, seconds, hundredths);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
        }
    }
}