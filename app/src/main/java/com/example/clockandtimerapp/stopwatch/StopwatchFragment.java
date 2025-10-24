package com.example.clockandtimerapp.stopwatch;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockandtimerapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

public class StopwatchFragment extends Fragment {

    private static final int REQ_POST_NOTIFICATIONS = 100;

    private TextView txtElapsed;
    private ImageView btnStartPause, btnReset, btnLap;
    private RecyclerView rvLaps;
    private LapAdapter lapAdapter;
    private final ArrayList<Lap> laps = new ArrayList<>();

    private boolean lastKnownRunning = false;
    private long lastKnownElapsed = 0L;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;
            long elapsed = intent.getLongExtra(StopwatchService.EXTRA_ELAPSED, 0L);
            boolean running = intent.getBooleanExtra(StopwatchService.EXTRA_RUNNING, false);
            long[] totals = intent.getLongArrayExtra(StopwatchService.EXTRA_LAPS_TOTALS);

            lastKnownElapsed = elapsed;
            lastKnownRunning = running;
            txtElapsed.setText(formatTime(elapsed));
            btnStartPause.setImageResource(running ? R.drawable.ic_pause_24 : R.drawable.ic_play_24);
            btnLap.setEnabled(running);
            btnReset.setEnabled(running || elapsed > 0);

            laps.clear();
            if (totals != null) {
                for (long t : totals) {
                    laps.add(new Lap(t));
                }
            }
            lapAdapter.submit(laps);
            // Scroll to the newest lap (LapAdapter should handle reversal, if needed)
            if (!laps.isEmpty()) {
                rvLaps.scrollToPosition(0);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stopwatch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        txtElapsed = v.findViewById(R.id.txtElapsed);
        btnStartPause = v.findViewById(R.id.btnStartPause);
        btnReset = v.findViewById(R.id.btnReset);
        btnLap = v.findViewById(R.id.btnLap);
        rvLaps = v.findViewById(R.id.rvLaps);

        lapAdapter = new LapAdapter();
        rvLaps.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLaps.setAdapter(lapAdapter);

        btnStartPause.setOnClickListener(x -> sendServiceAction(lastKnownRunning
                ? StopwatchService.ACTION_PAUSE
                : StopwatchService.ACTION_START));

        btnLap.setOnClickListener(x -> sendServiceAction(StopwatchService.ACTION_LAP));

        btnReset.setOnClickListener(x -> onReset());

        txtElapsed.setText(formatTime(0L));
        btnLap.setEnabled(false);
        btnReset.setEnabled(false);

        // request POST_NOTIFICATIONS if needed
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
            }
        }

        // --- Toolbar Menu Integration ---
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

    // --- Notification Visibility Control ---
    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(updateReceiver, new IntentFilter(StopwatchService.ACTION_UPDATE_BROADCAST));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hide notification when app is visible
        sendServiceAction(StopwatchService.ACTION_HIDE_NOTIFICATION);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Show notification when app goes to background, but ONLY if timer is running
        if (lastKnownRunning) {
            sendServiceAction(StopwatchService.ACTION_SHOW_NOTIFICATION);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(updateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Uncomment the following line if you want the timer to stop when leaving the stopwatch tab:
        // Intent i = new Intent(requireContext(), StopwatchService.class);
        // requireContext().stopService(i);
    }

    private void onReset() {
        if (!laps.isEmpty()) {
            showStatsDialog();
        } else {
            sendServiceAction(StopwatchService.ACTION_RESET);
        }
    }

    private void showStatsDialog() {
        if (laps.isEmpty()) {
            sendServiceAction(StopwatchService.ACTION_RESET);
            return;
        }

        ArrayList<Long> durations = new ArrayList<>();
        // Lap durations are calculated from totals
        for (int i = 0; i < laps.size(); i++) {
            long totalThis = laps.get(i).getTotalAtMs();
            // The lap total *before* this lap (or 0 if this is the last item, which is Lap 1)
            long totalNext = (i + 1 < laps.size()) ? laps.get(i + 1).getTotalAtMs() : 0L;
            durations.add(totalThis - totalNext);
        }

        long fastest = durations.get(0);
        long slowest = durations.get(0);
        long sum = 0;
        for (long d : durations) {
            if (d < fastest) fastest = d;
            if (d > slowest) slowest = d;
            sum += d;
        }
        long average = sum / durations.size();

        String message = "Fastest Lap: " + formatTime(fastest)
                + "\nSlowest Lap: " + formatTime(slowest)
                + "\nAverage Lap: " + formatTime(average);

        // FIX: Using 0 lets the builder inherit the theme from the Activity/Fragment context
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext(), 0);

        builder.setTitle("Lap Statistics")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> sendServiceAction(StopwatchService.ACTION_RESET))
                .setCancelable(false);

        androidx.appcompat.app.AlertDialog dialog = builder.show();
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(Color.parseColor("#2F66FF"));
    }

    private void sendServiceAction(String action) {
        Intent i = new Intent(requireContext(), StopwatchService.class);
        i.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(i);
        } else {
            requireContext().startService(i);
        }
    }

    private static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            // nothing special: user allowed or denied; notification may still be blocked in system settings
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}