package com.example.clockandtimerapp.worldclock;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockandtimerapp.R;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import android.util.Log;

public class AddClockFragment extends Fragment implements AddClockAdapter.OnCitySelectListener {

    private final List<TimezoneInfo> addedClocks;
    private List<TimezoneInfo> allAvailableClocks;
    private List<TimezoneInfo> filteredClocks;
    private AddClockAdapter adapter;

    /**
     * Constructor for passing data from ClockFragment (list of already added clocks)
     */
    public AddClockFragment(List<TimezoneInfo> addedClocks) {
        this.addedClocks = addedClocks;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_clock, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Get Master List and filter out already added clocks
        // Note: For a simpler project, we hardcode the master list here.
        allAvailableClocks = createMasterTimezoneList();

        // 2. Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_add_clock_cities);
        adapter = new AddClockAdapter(requireContext(), new ArrayList<>(), this); // Start with empty list
        recyclerView.setAdapter(adapter);

        // 3. Setup Search Bar
        EditText editSearchCity = view.findViewById(R.id.edit_search_city);
        editSearchCity.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterClocks(s.toString());
            }
        });

        // Initial filtering to show all available cities (excluding added ones)
        filterClocks("");

        // Automatically show keyboard for search
        new Handler().postDelayed(() -> {
            editSearchCity.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editSearchCity, InputMethodManager.SHOW_IMPLICIT);
        }, 100);
    }

    private void filterClocks(String query) {
        String lowerCaseQuery = query.toLowerCase(Locale.getDefault());

        // Filter the master list (excluding already added clocks)
        filteredClocks = allAvailableClocks.stream()
                .filter(info -> !addedClocks.contains(info)) // Exclusion logic
                .filter(info -> query.isEmpty() ||
                        info.getCityName().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) ||
                        info.getCountryName().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery))
                .collect(Collectors.toList());

        adapter.updateList(filteredClocks);
    }

    // --- AddClockAdapter.OnCitySelectListener Implementation ---

    @Override
    public void onCitySelected(TimezoneInfo selectedInfo) {
        Log.d("AddClock", "City selected: " + selectedInfo.getCityName() + ". Attempting to communicate with target.");

        // Check if the target is set and is the correct type
        Fragment target = getTargetFragment();

        if (target instanceof ClockFragment) {

            TimezoneInfo newClock = createTimezoneInfoWithDifference(
                    selectedInfo.getCountryName(),
                    selectedInfo.getCityName(),
                    selectedInfo.getTimezoneId()
            );

            // Safely cast and call the public method on the target fragment
            ((ClockFragment) target).addNewClock(newClock);
            Log.d("AddClock", "Successfully passed new clock to target ClockFragment.");

        } else {
            // This confirms if the target was null or the wrong type
            Log.e("AddClock", "ERROR: Target Fragment is null or not ClockFragment.");
        }

        // Pop fragment off the back stack to return to the ClockFragment view
        getParentFragmentManager().popBackStack();
    }
    /**
     * Calculates the time difference in minutes from India Standard Time (IST)
     * for persistence, fulfilling the "faster calculation" requirement.
     */
    private TimezoneInfo createTimezoneInfoWithDifference(String country, String city, String timezoneId) {
        TimeZone indianTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
        TimeZone selectedTimeZone = TimeZone.getTimeZone(timezoneId);
        Date currentTime = new Date();

        long indianOffset = indianTimeZone.getOffset(currentTime.getTime());
        long cityOffset = selectedTimeZone.getOffset(currentTime.getTime());

        // Difference in minutes from IST
        long differenceMinutes = (cityOffset - indianOffset) / (1000 * 60);

        return new TimezoneInfo(country, city, timezoneId, differenceMinutes);
    }

    /**
     * STUB: Master list of available timezones.
     */
    private List<TimezoneInfo> createMasterTimezoneList() {
        List<TimezoneInfo> list = new ArrayList<>();
        // Note: differenceMinutes is set to 0 here; it will be calculated on selection.
        list.add(new TimezoneInfo("UK", "London", "Europe/London", 0));
        list.add(new TimezoneInfo("Australia", "Sydney", "Australia/Sydney", 0));
        list.add(new TimezoneInfo("Japan", "Tokyo", "Asia/Tokyo", 0));
        list.add(new TimezoneInfo("USA", "New York", "America/New_York", 0));
        list.add(new TimezoneInfo("UAE", "Dubai", "Asia/Dubai", 0));
        list.add(new TimezoneInfo("Brazil", "Sao Paulo", "America/Sao_Paulo", 0));
        list.add(new TimezoneInfo("France", "Paris", "Europe/Paris", 0));
        list.add(new TimezoneInfo("China", "Shanghai", "Asia/Shanghai", 0));
        return list;
    }
}