package com.example.clockandtimerapp;

import android.app.AlertDialog;
import android.content.Context; // Added explicit Context import for safety
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ContextThemeWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.clockandtimerapp.worldclock.WorldClockManager;
import com.example.clockandtimerapp.utils.TimeFormatPreference;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

// ALARM MODULE IMPORTS
import com.example.clockandtimerapp.alarm.AlarmScheduler;
import com.example.clockandtimerapp.alarm.AlarmStorage;
import com.example.clockandtimerapp.alarm.model.Alarm;

public class SettingsFragment extends Fragment {

    private TextView textTimeFormatSummary;
    private WorldClockManager clockManager;
    // Inside onViewCreated(), add after other click listeners



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize manager
        clockManager = new WorldClockManager(requireContext());

        // Initialize views
        RelativeLayout settingTimeFormat = view.findViewById(R.id.setting_time_format);
        RelativeLayout settingResetClock = view.findViewById(R.id.setting_reset_world_clock);
        RelativeLayout settingResetAppData = view.findViewById(R.id.setting_reset_app_data);
        settingResetAppData.setOnClickListener(v -> showResetAppDataDialog());

        RelativeLayout settingDeleteAllAlarms = view.findViewById(R.id.setting_delete_all_alarms);

        textTimeFormatSummary = view.findViewById(R.id.text_time_format_summary);

        updateTimeFormatSummary();

        settingTimeFormat.setOnClickListener(v -> showTimeFormatDialog());

        settingResetClock.setOnClickListener(v -> showResetClockDialog());

        settingDeleteAllAlarms.setOnClickListener(v -> showDeleteAllAlarmsDialog());
    }

    private void updateTimeFormatSummary() {
        boolean is24H = TimeFormatPreference.is24HourFormat(requireContext());
        String formatText = is24H ? "24-hour format (HH:mm)" : "12-hour format (h:mm a)";
        textTimeFormatSummary.setText(getString(R.string.currently_set_to, formatText));
    }

    private void showTimeFormatDialog() {
        boolean is24H = TimeFormatPreference.is24HourFormat(requireContext());
        String[] options = {"12-hour format (h:mm a)", "24-hour format (HH:mm)"};
        int checkedItem = is24H ? 1 : 0; // 0 for 12hr, 1 for 24hr

        ContextThemeWrapper themedContext = new ContextThemeWrapper(requireContext(), R.style.AlertDialogDarkTheme);

        new AlertDialog.Builder(themedContext)
                .setTitle("Select Time Format")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    boolean new24HSetting = (which == 1); // 1 = 24-hour

                    TimeFormatPreference.set24HourFormat(requireContext(), new24HSetting);
                    updateTimeFormatSummary();
                    dialog.dismiss();

                    Snackbar.make(requireView(), "Time format updated. Restart module to see changes.", Snackbar.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showResetClockDialog() {
        ContextThemeWrapper themedContext = new ContextThemeWrapper(requireContext(), R.style.AlertDialogDarkTheme);
        Context ctx = requireContext();

        new AlertDialog.Builder(themedContext)
                .setTitle("Confirm Reset")
                .setMessage("Are you sure you want to remove ALL added world clocks? This action cannot be undone.")
                .setPositiveButton("Reset Clocks", (dialog, which) -> {

                    // Action: Save an empty list, clearing the persistence data
                    clockManager.saveClocks(new ArrayList<>());

                    // Using local ctx variable for safety
                    Toast.makeText(ctx, "All world clocks have been removed.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAllAlarmsDialog() {
        ContextThemeWrapper themedContext = new ContextThemeWrapper(requireContext(), R.style.AlertDialogDarkTheme);
        Context ctx = requireContext();

        new AlertDialog.Builder(themedContext)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to remove ALL saved alarms? This action cannot be undone and will cancel all scheduled alarms.")
                .setPositiveButton("Delete All", (dialog, which) -> {

                    // 1. Load all current alarms
                    List<Alarm> currentAlarms = AlarmStorage.load(ctx);
                    int count = currentAlarms.size();

                    // 2. Cancel all scheduled alarms
                    for (Alarm alarm : currentAlarms) {
                        AlarmScheduler.cancel(ctx, alarm);
                    }

                    // 3. Save an empty list, clearing the persistence data
                    AlarmStorage.save(ctx, new ArrayList<>());
                    // Note: If you have seeding logic in AlarmFragment, you might want to call
                    // AlarmStorage.setInitialized(ctx) here to allow the seed to run again later.

                    Toast.makeText(ctx, count + " alarms have been permanently deleted.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showResetAppDataDialog() {
        ContextThemeWrapper themedContext = new ContextThemeWrapper(requireContext(), R.style.AlertDialogDarkTheme);
        Context ctx = requireContext();

        new AlertDialog.Builder(themedContext)
                .setTitle("Confirm App Reset")
                .setMessage("This will delete all World Clocks, Alarms, and reset all app settings. This action cannot be undone.")
                .setPositiveButton("Reset App Data", (dialog, which) -> {

                    // 1. Clear all World Clocks
                    if (clockManager != null) {
                        clockManager.saveClocks(new ArrayList<>());
                    }

                    // 2. Clear all Alarms
                    List<Alarm> alarms = AlarmStorage.load(ctx);
                    for (Alarm alarm : alarms) {
                        AlarmScheduler.cancel(ctx, alarm);
                    }
                    AlarmStorage.save(ctx, new ArrayList<>());

                    // 3. Reset app preferences (like 12/24 hour format)
                    TimeFormatPreference.resetToDefault(ctx);

                    Toast.makeText(ctx, "All app data has been reset.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


}