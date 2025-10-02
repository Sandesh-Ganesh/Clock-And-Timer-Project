package com.example.clockandtimerapp;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;

public class KabaddiMainFragment extends Fragment {

    // UI Elements
    private TextView tvMatchTimer, tvHalfIndicator;
    private TextView tvTeamAName, tvTeamBName;
    private TextView tvTeamATimer, tvTeamBTimer;
    private LinearLayout panelTeamA, panelTeamB;
    private MaterialButton btnPause, btnTimeout, btnReset;

    // Data
    private String teamAName, teamBName;
    private int matchTimeMinutes, raidTimeSeconds;
    private int halfTimeMinutes;
    private boolean isTeamAFirst;

    // Timer variables
    private CountDownTimer matchTimer, raidTimer;
    private long matchTimeRemaining; // in milliseconds
    private long raidTimeRemaining; // in milliseconds
    private boolean isMatchRunning = false;
    private boolean isRaidRunning = false;
    private boolean isFirstHalf = true;

    // Current raiding team (true = Team A, false = Team B)
    private boolean isTeamARaiding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kabaddi_main, container, false);

        // Get data from bundle
        Bundle bundle = getArguments();
        if (bundle != null) {
            teamAName = bundle.getString("teamA", "Team A");
            teamBName = bundle.getString("teamB", "Team B");
            matchTimeMinutes = bundle.getInt("matchTime", 40);
            raidTimeSeconds = bundle.getInt("raidTime", 30);
            isTeamAFirst = bundle.getBoolean("isTeamAFirst", true);
        }

        // Calculate half time
        halfTimeMinutes = matchTimeMinutes / 2;
        matchTimeRemaining = matchTimeMinutes * 60 * 1000L; // Convert to milliseconds
        raidTimeRemaining = raidTimeSeconds * 1000L;
        isTeamARaiding = isTeamAFirst;

        // Initialize views
        initViews(view);
        setupListeners();
        updateUI();

        return view;
    }

    private void initViews(View view) {
        tvMatchTimer = view.findViewById(R.id.tv_match_timer);
        tvHalfIndicator = view.findViewById(R.id.tv_half_indicator);
        tvTeamAName = view.findViewById(R.id.tv_team_a_name);
        tvTeamBName = view.findViewById(R.id.tv_team_b_name);
        tvTeamATimer = view.findViewById(R.id.tv_team_a_timer);
        tvTeamBTimer = view.findViewById(R.id.tv_team_b_timer);
        panelTeamA = view.findViewById(R.id.panel_team_a);
        panelTeamB = view.findViewById(R.id.panel_team_b);
        btnPause = view.findViewById(R.id.btn_pause);
        btnTimeout = view.findViewById(R.id.btn_timeout);
        btnReset = view.findViewById(R.id.btn_reset);

        // Set team names
        tvTeamAName.setText(teamAName);
        tvTeamBName.setText(teamBName);
    }

    private void setupListeners() {
        // Team A panel click - start raid for Team A
        panelTeamA.setOnClickListener(v -> {
            if (!isMatchRunning) {
                startMatch();
            }
            if (isTeamARaiding || !isRaidRunning) {
                startRaid(true);
            }
        });

        // Team B panel click - start raid for Team B
        panelTeamB.setOnClickListener(v -> {
            if (!isMatchRunning) {
                startMatch();
            }
            if (!isTeamARaiding || !isRaidRunning) {
                startRaid(false);
            }
        });

        // Pause button
        btnPause.setOnClickListener(v -> togglePause());

        // Timeout button
        btnTimeout.setOnClickListener(v -> handleTimeout());

        // Reset button
        btnReset.setOnClickListener(v -> handleReset());
    }

    private void startMatch() {
        isMatchRunning = true;
        matchTimer = new CountDownTimer(matchTimeRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                matchTimeRemaining = millisUntilFinished;
                updateMatchTimer();
                checkHalfTime();
            }

            @Override
            public void onFinish() {
                matchTimeRemaining = 0;
                updateMatchTimer();
                isMatchRunning = false;
                stopRaid();
                Toast.makeText(getContext(), "Match Finished!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }

    private void startRaid(boolean isTeamA) {
        // Stop current raid if running
        if (isRaidRunning) {
            stopRaid();
        }

        isTeamARaiding = isTeamA;
        isRaidRunning = true;
        raidTimeRemaining = raidTimeSeconds * 1000L;

        // Highlight active team panel
        updateTeamPanels();

        raidTimer = new CountDownTimer(raidTimeRemaining, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                raidTimeRemaining = millisUntilFinished;
                updateRaidTimer();
            }

            @Override
            public void onFinish() {
                raidTimeRemaining = 0;
                updateRaidTimer();
                isRaidRunning = false;
                // Auto switch to other team
                Toast.makeText(getContext(), "Raid time over! Switch teams.", Toast.LENGTH_SHORT).show();
                updateTeamPanels();
            }
        }.start();
    }

    private void stopRaid() {
        if (raidTimer != null) {
            raidTimer.cancel();
        }
        isRaidRunning = false;
        raidTimeRemaining = raidTimeSeconds * 1000L;
        updateRaidTimer();
        updateTeamPanels();
    }

    private void togglePause() {
        if (isMatchRunning) {
            // Pause
            if (matchTimer != null) {
                matchTimer.cancel();
            }
            if (raidTimer != null) {
                raidTimer.cancel();
            }
            isMatchRunning = false;
            boolean wasRaidRunning = isRaidRunning;
            isRaidRunning = false;
            btnPause.setText("Resume");

            // Store raid running state for resume
            btnPause.setTag(wasRaidRunning);
        } else {
            // Resume
            btnPause.setText("Pause");
            startMatch();

            // Resume raid if it was running
            Boolean wasRaidRunning = (Boolean) btnPause.getTag();
            if (wasRaidRunning != null && wasRaidRunning) {
                startRaid(isTeamARaiding);
            }
        }
    }

    private void handleTimeout() {
        if (isRaidRunning) {
            // Pause only raid timer
            if (raidTimer != null) {
                raidTimer.cancel();
            }
            isRaidRunning = false;
            Toast.makeText(getContext(), "Raid timer paused for timeout", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No active raid to timeout", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleReset() {
        // Stop all timers
        if (matchTimer != null) {
            matchTimer.cancel();
        }
        if (raidTimer != null) {
            raidTimer.cancel();
        }

        // Go back to setup screen
        getParentFragmentManager().popBackStack();
    }

    private void updateMatchTimer() {
        long minutes = (matchTimeRemaining / 1000) / 60;
        long seconds = (matchTimeRemaining / 1000) % 60;
        tvMatchTimer.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void updateRaidTimer() {
        long seconds = raidTimeRemaining / 1000;
        long millis = (raidTimeRemaining % 1000) / 100;

        if (isTeamARaiding) {
            tvTeamATimer.setText(String.format("%d.%d", seconds, millis));
            tvTeamBTimer.setText(String.valueOf(raidTimeSeconds));
        } else {
            tvTeamBTimer.setText(String.format("%d.%d", seconds, millis));
            tvTeamATimer.setText(String.valueOf(raidTimeSeconds));
        }
    }

    private void updateTeamPanels() {
        int activeColor = getResources().getColor(R.color.team_active, null);
        int inactiveColor = getResources().getColor(R.color.team_a_bg, null);

        if (isRaidRunning) {
            if (isTeamARaiding) {
                panelTeamA.setBackgroundColor(activeColor);
                panelTeamB.setBackgroundColor(inactiveColor);
            } else {
                panelTeamB.setBackgroundColor(activeColor);
                panelTeamA.setBackgroundColor(inactiveColor);
            }
        } else {
            panelTeamA.setBackgroundColor(inactiveColor);
            panelTeamB.setBackgroundColor(inactiveColor);
        }
    }

    private void checkHalfTime() {
        long minutesRemaining = (matchTimeRemaining / 1000) / 60;

        if (isFirstHalf && minutesRemaining <= halfTimeMinutes) {
            isFirstHalf = false;
            tvHalfIndicator.setText("Second Half");
            Toast.makeText(getContext(), "Second Half Started!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        updateMatchTimer();
        tvTeamATimer.setText(String.valueOf(raidTimeSeconds));
        tvTeamBTimer.setText(String.valueOf(raidTimeSeconds));
        tvHalfIndicator.setText("First Half");
        updateTeamPanels();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up timers
        if (matchTimer != null) {
            matchTimer.cancel();
        }
        if (raidTimer != null) {
            raidTimer.cancel();
        }
    }
}