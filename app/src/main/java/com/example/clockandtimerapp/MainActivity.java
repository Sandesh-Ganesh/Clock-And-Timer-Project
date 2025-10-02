package com.example.clockandtimerapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private MaterialToolbar toolbar;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        // Set up the Toolbar as the Activity's Action Bar
        setSupportActionBar(toolbar);

        // Handle Settings icon click
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                // Load SettingsFragment, adding it to the back stack
                loadFragment(new SettingsFragment(), "Settings");
                return true;
            }
            return false;
        });

        // Handle Bottom Navigation clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            // Load primary fragments (clears back stack)
            if (itemId == R.id.nav_world_clock) {
                loadPrimaryFragment(new ClockFragment(), "World Clock");
                return true;
            } else if (itemId == R.id.nav_alarm) {
                loadPrimaryFragment(new AlarmFragment(), "Alarm");
                return true;
            } else if (itemId == R.id.nav_stopwatch) {
                loadPrimaryFragment(new StopwatchFragment(), "Stopwatch");
                return true;
            } else if (itemId == R.id.nav_timer) {
                loadPrimaryFragment(new TimerFragment(), "Timer");
                return true;
            } else if (itemId == R.id.nav_games) {
                loadPrimaryFragment(new GamesFragment(), "Games");
                return true;
            }
            return false;
        });

        // Listen for changes in the fragment back stack
        fragmentManager.addOnBackStackChangedListener(() -> {
            // Check if any fragment is stacked (e.g., SettingsFragment)
            boolean isSecondaryFragment = fragmentManager.getBackStackEntryCount() > 0;

            // Show or hide the Up button based on back stack count
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(isSecondaryFragment);
            }

            // Manage BottomNavigationView visibility and state
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);

            if (currentFragment instanceof SettingsFragment) {
                // Secondary screen: Hide bottom nav and deselect items
                bottomNavigationView.setVisibility(View.GONE);
                bottomNavigationView.getMenu().setGroupCheckable(0, false, true);
            } else {
                // Primary screen: Show bottom nav and re-enable selection
                bottomNavigationView.setVisibility(View.VISIBLE);
                bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
            }
        });

        // Load the initial default fragment without adding it to the back stack
        if (savedInstanceState == null) {
            loadInitialFragment(new ClockFragment(), "World Clock");
            bottomNavigationView.setSelectedItemId(R.id.nav_world_clock);
        }
    }

    // Handle Up button (back arrow) click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Pop the last fragment off the back stack
            fragmentManager.popBackStack();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Loads the very first fragment, NOT added to the back stack.
     */
    private void loadInitialFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Loads a primary fragment from the bottom navigation. Clears existing back stack.
     */
    private void loadPrimaryFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);
        // Clear all previous fragments from the back stack
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    /**
     * Loads a secondary fragment (like Settings), adding it to the back stack.
     */
    private void loadFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);

        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // Add to stack to allow back navigation
                .commit();
    }
}