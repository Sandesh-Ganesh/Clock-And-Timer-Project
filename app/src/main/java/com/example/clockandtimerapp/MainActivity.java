package com.example.clockandtimerapp;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.clockandtimerapp.stopwatch.StopwatchService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.clockandtimerapp.stopwatch.StopwatchFragment;
import com.example.clockandtimerapp.worldclock.ClockFragment;
import com.example.clockandtimerapp.chess.ChessTimerFragment;
import com.example.clockandtimerapp.alarm.AlarmFragment;
import com.example.clockandtimerapp.timer.TimerFragment;
import com.example.clockandtimerapp.timer.pomodoroActivity;

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
            // Check if any fragment is stacked (e.g., SettingsFragment, Kabaddi fragments)
            boolean isSecondaryFragment = fragmentManager.getBackStackEntryCount() > 0;

            // Show or hide the Up button based on back stack count
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(isSecondaryFragment);
            }

            // Manage BottomNavigationView visibility and state
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);

            // Check if current fragment is a secondary fragment (not a primary bottom nav fragment)
            boolean isPrimaryFragment = currentFragment instanceof ClockFragment ||
                    currentFragment instanceof AlarmFragment ||
                    currentFragment instanceof StopwatchFragment ||
                    currentFragment instanceof TimerFragment ||
                    currentFragment instanceof GamesFragment;

            if (isPrimaryFragment) {
                // Primary screen: Show bottom nav and re-enable selection
                bottomNavigationView.setVisibility(View.VISIBLE);
                bottomNavigationView.getMenu().setGroupCheckable(0, true, true);

                // Update toolbar title based on current primary fragment
                updateToolbarTitleForFragment(currentFragment);

                invalidateOptionsMenu();
            } else {
                // Secondary screen: Hide bottom nav and deselect items
                // (SettingsFragment, KabaddiSetupFragment, KabaddiMainFragment, etc.)
                bottomNavigationView.setVisibility(View.GONE);
                bottomNavigationView.getMenu().setGroupCheckable(0, false, true);

                // Update toolbar title based on current secondary fragment
                updateToolbarTitleForFragment(currentFragment);

                invalidateOptionsMenu();
            }
        });

        // Load the initial default fragment without adding it to the back stack
        if (savedInstanceState == null) {
            String fragmentToLoad = getIntent().getStringExtra(StopwatchService.EXTRA_FRAGMENT_TO_LOAD);
            if ("Stopwatch".equals(fragmentToLoad)) {
                // Notification clicked: Load StopwatchFragment
                loadInitialFragment(new StopwatchFragment(), "Stopwatch");
                bottomNavigationView.setSelectedItemId(R.id.nav_stopwatch);
            } else {
                // Default launch: Load ClockFragment
                loadInitialFragment(new ClockFragment(), "World Clock");
                bottomNavigationView.setSelectedItemId(R.id.nav_world_clock);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        if (settingsItem != null) {

            // Check if we are on a secondary screen (i.e., back stack has items)
            boolean isSecondaryFragment = fragmentManager.getBackStackEntryCount() > 0;

            // Hide the settings icon if the Up button is visible (secondary screen)
            settingsItem.setVisible(!isSecondaryFragment);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // R.menu.toolbar_menu contains your action_settings item
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
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
    public void loadFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);

        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // Add to stack to allow back navigation
                .commit();
    }

    /**
     * Public getter method for fragments to access BottomNavigationView
     * This allows fragments to show/hide the bottom navigation if needed
     */
    public BottomNavigationView getBottomNavigationView() {
        return bottomNavigationView;
    }

    /**
     * Helper method to update toolbar title based on current fragment
     */
    private void updateToolbarTitleForFragment(Fragment fragment) {
        String title = "Clock"; // default

        if (fragment instanceof ClockFragment) {
            title = "World Clock";
        } else if (fragment instanceof AlarmFragment) {
            title = "Alarm";
        } else if (fragment instanceof StopwatchFragment) {
            title = "Stopwatch";
        } else if (fragment instanceof TimerFragment) {
            title = "Timer";
        } else if (fragment instanceof GamesFragment) {
            title = "Games";
        } else if (fragment instanceof SettingsFragment) {
            title = "Settings";
        } else if (fragment instanceof KabaddiSetupFragment) {
            title = "Kabaddi Timer Setup";
        } else if (fragment instanceof KabaddiMainFragment) {
            title = "Kabaddi Match";
        } else if (fragment instanceof ChessTimerFragment) {
            title = "Chess Timer"; // This is the title for the new screen
        }

        toolbar.setTitle(title);
    }
}