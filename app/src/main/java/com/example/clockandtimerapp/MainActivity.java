package com.example.clockandtimerapp;

import android.os.Bundle;
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

        // Setup toolbar
        setSupportActionBar(toolbar);

        // Setup bottom navigation
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_world_clock) {
                loadFragment(new ClockFragment(), "Clock");
                return true;
            } else if (itemId == R.id.nav_alarm) {
                loadFragment(new AlarmFragment(), "Alarm");
                return true;
            } else if (itemId == R.id.nav_stopwatch) {
                loadFragment(new StopwatchFragment(), "Stopwatch");
                return true;
            } else if (itemId == R.id.nav_timer) {
                loadFragment(new TimerFragment(), "Timer");
                return true;
            } else if (itemId == R.id.nav_games) {
                loadFragment(new GamesFragment(), "Games");
                return true;
            } else if (itemId == R.id.nav_settings) {
                loadFragment(new SettingsFragment(), "Settings");
                return true;
            }
            return false;
        });

        // Load default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_world_clock);
        }
    }

    private void loadFragment(Fragment fragment, String title) {
        toolbar.setTitle(title);
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}