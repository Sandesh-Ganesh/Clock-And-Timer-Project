package com.example.clockandtimerapp.worldclock;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// --- Add these imports for menu handling ---
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
// ------------------------------------------
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockandtimerapp.MainActivity;
import com.example.clockandtimerapp.R;
import com.example.clockandtimerapp.utils.TimeFormatPreference;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ClockFragment extends Fragment implements WorldClockAdapter.OnDeleteClickListener {

    private TextView textCurrentTime, textCurrentTimezone, textCurrentDate;
    private WorldClockAdapter clockAdapter;
    private WorldClockManager clockManager;
    private List<TimezoneInfo> clockList;

    // Handler for updating the local time every second
    private final Handler timeUpdateHandler = new Handler();
    private final Runnable timeUpdater = new Runnable() {
        @Override
        public void run() {
            updateCurrentLocalTime();
            timeUpdateHandler.postDelayed(this, 1000); // Update every 1 second
        }
    };

    // **********************************************
    // *** FIX: Re-enables the Settings Menu Icon ***
    // **********************************************
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This tells the Activity that this fragment wants to display
        // or contribute to the options menu (which holds the Settings icon).
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Managers
        clockManager = new WorldClockManager(requireContext());

        // 2. Initialize Views
        textCurrentTime = view.findViewById(R.id.text_current_time);
        textCurrentTimezone = view.findViewById(R.id.text_current_timezone);
        textCurrentDate = view.findViewById(R.id.text_current_date);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_world_clocks);
        FloatingActionButton fabAddClock = view.findViewById(R.id.fab_add_clock);

        // 3. Load Data & Setup Adapter
        clockList = clockManager.loadClocks();
        clockAdapter = new WorldClockAdapter(requireContext(), clockList, this);
        recyclerView.setAdapter(clockAdapter);

        // 4. Setup FAB click listener
        fabAddClock.setOnClickListener(v -> navigateToAddClock());
    }

    private void updateCurrentLocalTime() {
        // Requirement: Main clock is always Indian Clock (Asia/Kolkata)
        TimeZone indianTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
        Date currentTime = new Date();

        // 1. Time
        String timePattern = TimeFormatPreference.getHourFormatPattern(requireContext());
        SimpleDateFormat timeFormat = new SimpleDateFormat(timePattern + ":ss a", Locale.getDefault());
        timeFormat.setTimeZone(indianTimeZone);
        textCurrentTime.setText(timeFormat.format(currentTime));

        // 2. Timezone
        textCurrentTimezone.setText("India Standard Time");

        // 3. Date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        dateFormat.setTimeZone(indianTimeZone);
        textCurrentDate.setText(dateFormat.format(currentTime));
    }

    /**
     * Called by AddClockFragment after a new city is selected.
     */
    public void addNewClock(TimezoneInfo timezoneInfo) {
        if (!clockList.contains(timezoneInfo)) {
            // 1. Save using the Manager (CRITICAL)
            clockManager.addClock(timezoneInfo, clockList);

            // 2. Notify the adapter to redraw (CRITICAL)
            clockAdapter.notifyItemInserted(0);

            // 3. Scroll to the new item (Optional but helpful)
            if (getView() != null) {
                RecyclerView recyclerView = getView().findViewById(R.id.recycler_world_clocks);
                if (recyclerView != null) {
                    recyclerView.scrollToPosition(0);
                }
            }
            // LOG: Confirms data received and stored.
            Log.d("ClockFragment", "Clock received and added: " + timezoneInfo.getCityName());

        } else {
            Toast.makeText(getContext(), timezoneInfo.getCityName() + " is already added.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToAddClock() {
        if (getActivity() instanceof MainActivity) {

            // 1. Create the AddClockFragment instance
            AddClockFragment addClockFragment = new AddClockFragment(clockList);

            // 2. SET THE TARGET FRAGMENT for reliable communication
            addClockFragment.setTargetFragment(this, 0);

            // 3. Use the MainActivity's loadFragment to handle the transaction
            ((MainActivity) getActivity()).loadFragment(
                    addClockFragment,
                    "Add Clock"
            );
        }
    }

    // --- WorldClockAdapter.OnDeleteClickListener Implementation ---

    @Override
    public void onDeleteClicked(int position) {
        TimezoneInfo clockToDelete = clockList.get(position);

        // Show the Delete Clock confirmation dialog
        new DeleteConfirmationDialog(clockToDelete.getCityName(), () -> {
            // User confirmed delete: update manager, remove from list, update adapter
            clockManager.deleteClock(clockToDelete, clockList);
            clockAdapter.notifyItemRemoved(position);
            Toast.makeText(getContext(), clockToDelete.getCityName() + " removed.", Toast.LENGTH_SHORT).show();
        }).show(getParentFragmentManager(), "DeleteClockDialog");
    }

    // --- Fragment Lifecycle ---

    @Override
    public void onResume() {
        super.onResume();
        // Start updates for main clock and list clocks
        timeUpdateHandler.post(timeUpdater);
        clockAdapter.startUpdates();

        // When coming back from settings, force a redraw for format change
        updateCurrentLocalTime();
        clockAdapter.notifyDataSetChanged();

        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop updates to prevent memory leaks
        timeUpdateHandler.removeCallbacks(timeUpdater);
        clockAdapter.stopUpdates();
    }
}