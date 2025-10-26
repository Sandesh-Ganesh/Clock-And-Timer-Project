package com.example.clockandtimerapp.alarm;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // Added for empty view
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockandtimerapp.R;
import com.example.clockandtimerapp.alarm.model.Alarm;
import com.example.clockandtimerapp.alarm.ui.AlarmAdapter;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List; // Added for explicit List type

public class AlarmFragment extends Fragment implements AlarmAdapter.Callbacks {

    private final ArrayList<Alarm> alarms = new ArrayList<>();
    private AlarmAdapter adapter;
    private View rootView;
    private TextView noAlarmsMessage; // Added

    public AlarmFragment() {
        super();
    }

    // -------------------------------------------------------------------------
    // LAUNCHER: CREATE NEW ALARM
    // -------------------------------------------------------------------------
    private final ActivityResultLauncher<Intent> editLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData()!=null) {
                    Intent data = result.getData();
                    int hour   = data.getIntExtra("hour24", 7);
                    int minute = data.getIntExtra("minute", 0);
                    String label = data.getStringExtra("label");
                    boolean vibrate = data.getBooleanExtra("vibrate", false);

                    // Defensive coding: Ensure 'tone' is not null
                    String tone = data.getStringExtra("ringtone");
                    if (tone == null) tone = "";

                    String day = data.getStringExtra("day");
                    int dayOfWeek = mapDayToCalendar(day);

                    // NEW: DUPLICATE CHECK IMPLEMENTATION
                    if (AlarmStorage.isDuplicate(requireContext(), hour, minute, dayOfWeek)) {
                        Toast.makeText(requireContext(),
                                "Alarm already exists for this time and day.",
                                Toast.LENGTH_LONG).show();
                        return; // Do not save or schedule the duplicate alarm
                    }
                    // END DUPLICATE CHECK

                    int id = AlarmStorage.nextId(requireContext());

                    // NOTE: Constructor arguments must match Alarm model exactly (id, hour, minute, label, enabled, tone, vibrate, dayOfWeek)
                    Alarm a = new Alarm(id, hour, minute, label, true, tone, vibrate, dayOfWeek);

                    alarms.add(a);
                    AlarmStorage.save(requireContext(), alarms);
                    AlarmScheduler.schedule(requireContext(), a);
                    adapter.submit(new ArrayList<>(alarms));
                    updateEmptyView(); // Added
                }
            });

    // -------------------------------------------------------------------------
    // LAUNCHER: EDIT EXISTING ALARM (Includes UI Refresh Fix)
    // -------------------------------------------------------------------------
    private final ActivityResultLauncher<Intent> editExistingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData()!=null) {
                    Intent data = result.getData();
                    int editId = data.getIntExtra("editId", -1);
                    if (editId == -1) return;

                    int hour   = data.getIntExtra("hour24", -1);
                    int minute = data.getIntExtra("minute", -1);

                    // Defensive coding: Ensure 'label' is not null
                    String label = data.getStringExtra("label");
                    if (label == null) label = "Alarm";

                    boolean vibrate = data.getBooleanExtra("vibrate", false);

                    // Defensive coding: Ensure 'tone' is not null
                    String tone = data.getStringExtra("ringtone");
                    if (tone == null) tone = "";

                    String day = data.getStringExtra("day");
                    int dayOfWeek = mapDayToCalendar(day);

                    int idx = findIndexById(editId);
                    if (idx >= 0) {
                        Alarm a = alarms.get(idx);
                        a.hour24 = hour >= 0 ? hour : a.hour24;
                        a.minute = minute >= 0 ? minute : a.minute;

                        // Set the guaranteed non-null label/tone
                        a.label = label;
                        a.vibrate = vibrate;
                        a.ringtone = tone;
                        a.dayOfWeek = dayOfWeek;

                        // 1. Save the updated list to storage
                        AlarmStorage.save(requireContext(), alarms);

                        // 2. Re-schedule or cancel the alarm
                        AlarmScheduler.cancel(requireContext(), a);
                        if (a.enabled) AlarmScheduler.schedule(requireContext(), a);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                // 1. Reload the latest data
                                alarms.clear();
                                alarms.addAll(AlarmStorage.load(requireContext()));

                                // 2. Submit the new list object to the adapter
                                adapter.submit(new ArrayList<>(alarms));

                                // Optional (but safe) check if the RecyclerView itself is available
                                // This ensures the list redraws even if DiffUtil is slow.
                                adapter.notifyDataSetChanged();
                                updateEmptyView(); // Added
                            });
                        }
                    }
                }
            });

    // -------------------------------------------------------------------------
    // FRAGMENT LIFECYCLE METHODS
    // -------------------------------------------------------------------------
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Layout inflation
        rootView = inflater.inflate(R.layout.fragment_alarm, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- 1. SETUP RECYCLERVIEW AND ADAPTER (with null check) ---
        RecyclerView rv = view.findViewById(R.id.alarmList);
        noAlarmsMessage = view.findViewById(R.id.noAlarmsMessage); // Added: Initialize TextView

        if (rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new AlarmAdapter(this);
            rv.setAdapter(adapter);

            // Load saved alarms
            alarms.clear();
            alarms.addAll(AlarmStorage.load(requireContext()));

            // Seed once (first install)
            if (!AlarmStorage.isInitialized(requireContext())) {
                if (alarms.isEmpty()) {
                    int id = AlarmStorage.nextId(requireContext());
                    Alarm seed = new Alarm(id, 4, 30, "Morning Alarm | Sunday", true, "", false, Calendar.SUNDAY);
                    alarms.add(seed);
                    AlarmStorage.save(requireContext(), alarms);
                }
                AlarmStorage.setInitialized(requireContext());
            }

            adapter.submit(new ArrayList<>(alarms));
            updateEmptyView(); // Added: Initial check
            checkAndRequestOverlayPermission();
        } else {
            Toast.makeText(requireContext(), "Error: Alarm List (R.id.alarmList) not found in layout!", Toast.LENGTH_LONG).show();
            return;
        }

        // --- 2. SETUP FLOATING ACTION BUTTON (with null check) ---
        FloatingActionButton fab = view.findViewById(R.id.fab);

        if (fab != null) {
            // FAB Found: Setup click listener for ADD NEW ALARM
            fab.setOnClickListener(v -> editLauncher.launch(new Intent(requireContext(), EditAlarmActivity.class)));
        } else {
            Toast.makeText(requireContext(), "Warning: Add Alarm Button (R.id.fab) not found in layout!", Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onToggle(int position, boolean on) {
        if (position >= 0 && position < alarms.size()) {
            Alarm a = alarms.get(position);
            a.enabled = on;
            AlarmStorage.save(requireContext(), alarms);

            // Check for permission before scheduling
            if (on) {
                AlarmScheduler.schedule(requireContext(), a);
            } else {
                AlarmScheduler.cancel(requireContext(), a);
            }
        }
    }

    @Override
    public void onDelete(int position) {
        if (position >= 0 && position < alarms.size()) {
            Alarm a = alarms.remove(position);
            AlarmScheduler.cancel(requireContext(), a);
            AlarmStorage.save(requireContext(), alarms);
            adapter.submit(new ArrayList<>(alarms));
            updateEmptyView(); // Added
        }
    }

    @Override
    public void onEdit(int position) {
        if (position < 0 || position >= alarms.size()) return;
        Alarm a = alarms.get(position);

        // Intent to launch EditAlarmActivity for editing
        Intent i = new Intent(requireContext(), EditAlarmActivity.class);
        i.putExtra("mode", "edit");
        i.putExtra("editId", a.id);
        i.putExtra("hour24", a.hour24);
        i.putExtra("minute", a.minute);
        i.putExtra("label", a.label);
        i.putExtra("vibrate", a.vibrate);
        i.putExtra("ringtone", a.ringtone);
        i.putExtra("day", mapCalendarToDayString(a.dayOfWeek));
        editExistingLauncher.launch(i);
    }

    private void updateEmptyView() {
        if (noAlarmsMessage != null) {
            if (alarms.isEmpty()) {
                noAlarmsMessage.setVisibility(View.VISIBLE);
            } else {
                noAlarmsMessage.setVisibility(View.GONE);
            }
        }
    }

    private int findIndexById(int id){
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).id == id) return i;
        }
        return -1;
    }

    private int mapDayToCalendar(String day){
        if (day == null) return -1;
        switch (day.toLowerCase()) {
            case "sunday": return Calendar.SUNDAY;
            case "monday": return Calendar.MONDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return -1;
        }
    }

    private String mapCalendarToDayString(int dayOfWeek){
        switch (dayOfWeek){
            case Calendar.SUNDAY: return "Sunday";
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            default: return null;
        }
    }
    private final ActivityResultLauncher<Intent> overlayPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // The user is redirected back here after attempting to grant the permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.canDrawOverlays(requireContext())) {
                        Toast.makeText(requireContext(), "Alarm screen display is disabled.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(requireContext(), "Overlay permission granted.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    // Add this helper method
    private void checkAndRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(requireContext())) {
                // Permission is not granted, request it
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + requireContext().getPackageName()));

                // NOTE: The request code is no longer needed with ActivityResultLauncher
                overlayPermissionLauncher.launch(intent);
            }
        }
    }
}