package com.example.clockandtimerapp;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class KabaddiMainFragment extends Fragment implements NextRaidDialog.NextRaidListener, TimeoutDecisionDialog.TimeoutDecisionListener {

    // Constants
    private static final long RAID_TIMER_TICK = 100;
    private static final long MATCH_TIMER_TICK = 1000;
    private static final long TIMEOUT_DURATION = 30 * 1000L;
    private static final long TIMEOUT_TIMER_TICK = 100;

    // UI Elements
    private TextView tvMatchTimer, tvHalfIndicator, tvFullTime;
    private TextView tvTeamAName, tvTeamBName;
    private TextView tvTeamATimer, tvTeamBTimer;
    private TextView tvTeamAMsg, tvTeamBMsg;
    private TextView tvTeamATimeoutLeft, tvTeamBTimeoutLeft;
    private LinearLayout panelTeamA, panelTeamB;
    private MaterialButton btnPause, btnTimeout, btnReset;

    // UI elements for Timeout Overlay
    private TextView tvTimeoutCountdown;
    private FrameLayout flOverlayContainer;

    // Data from Setup
    private String teamAName, teamBName;
    private int matchTimeMinutes, raidTimeSeconds;
    private long halfTimeDuration;
    private boolean isTeamAFirst;
    private boolean isTeamAStartedFirstHalf;

    // Color References
    private int colorActive, colorInactive, colorTextPrimary, colorIconDisabledTint, colorButtonDisabledMask;
    private int colorTextActive, colorPanelActive;

    // Timer variables
    private CountDownTimer matchTimer, raidTimer, timeoutTimer;
    private long matchTimeRemaining;
    private long raidTimeDefault;
    private long raidTimeRemaining;
    private long timeoutTimeRemaining = TIMEOUT_DURATION;

    // State variables
    private boolean isMatchRunning = false;
    private boolean isRaidRunning = false;
    private boolean isFirstHalf = true;
    private boolean isMatchPaused = false;
    private boolean isTimeoutActive = false;
    private boolean teamACalledTimeout;

    // TIMEOUT STATE
    private int teamATimeoutsLeft = 2;
    private int teamBTimeoutsLeft = 2;

    // Current raiding team (true = Team A, false = Team B)
    private boolean isTeamARaiding;

    // Score (Placeholder)
    private int teamAScore = 0;
    private int teamBScore = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kabaddi_main, container, false);

        Bundle bundle = getArguments();
        if (bundle != null) {
            teamAName = bundle.getString("teamA", "Team A");
            teamBName = bundle.getString("teamB", "Team B");
            matchTimeMinutes = bundle.getInt("matchTime", 40);
            raidTimeSeconds = bundle.getInt("raidTime", 30);
            isTeamAFirst = bundle.getBoolean("isTeamAFirst", true);
        }

        halfTimeDuration = (matchTimeMinutes * 60 * MATCH_TIMER_TICK) / 2;
        matchTimeRemaining = halfTimeDuration;

        raidTimeDefault = raidTimeSeconds * MATCH_TIMER_TICK;
        raidTimeRemaining = raidTimeDefault;

        isTeamAStartedFirstHalf = isTeamAFirst;
        isTeamARaiding = isTeamAFirst;

        initViews(view);
        setupListeners();
        updateUI();

        return view;
    }

    private void initViews(View view) {
        tvMatchTimer = view.findViewById(R.id.tv_match_timer);
        tvHalfIndicator = view.findViewById(R.id.tv_half_indicator);
        tvFullTime = view.findViewById(R.id.tv_fullTime);
        tvTeamAName = view.findViewById(R.id.tv_team_a_name);
        tvTeamBName = view.findViewById(R.id.tv_team_b_name);
        tvTeamATimer = view.findViewById(R.id.tv_team_a_timer);
        tvTeamBTimer = view.findViewById(R.id.tv_team_b_timer);
        panelTeamA = view.findViewById(R.id.panel_team_a);
        panelTeamB = view.findViewById(R.id.panel_team_b);
        btnPause = view.findViewById(R.id.btn_pause);
        btnTimeout = view.findViewById(R.id.btn_timeout);
        btnReset = view.findViewById(R.id.btn_reset);

        // MAPPING FIX: Use the existing IDs in the provided XML for the overlay
        tvTimeoutCountdown = view.findViewById(R.id.tv_timeout_timer);
        flOverlayContainer = view.findViewById(R.id.fl_overlay_container);

        tvTeamAMsg = view.findViewById(R.id.tv_team_a_msg);
        tvTeamBMsg = view.findViewById(R.id.tv_team_b_msg);

        tvTeamATimeoutLeft = view.findViewById(R.id.tv_team_a_timeout_left);
        tvTeamBTimeoutLeft = view.findViewById(R.id.tv_team_b_timeout_left);

        tvTeamAName.setText(teamAName);
        tvTeamBName.setText(teamBName);

        // Initialize colors from resources
        colorActive = ContextCompat.getColor(requireContext(), R.color.primary_blue);
        colorInactive = ContextCompat.getColor(requireContext(), R.color.inactive_kabaddi);
        colorTextPrimary = ContextCompat.getColor(requireContext(), R.color.text_primary);
        colorIconDisabledTint = ContextCompat.getColor(requireContext(), R.color.icon_disabled_tint);
        colorButtonDisabledMask = ContextCompat.getColor(requireContext(), R.color.button_disabled_mask);

        // Colors for timeout state
        colorTextActive = ContextCompat.getColor(requireContext(), R.color.primary_blue);
        colorPanelActive = ContextCompat.getColor(requireContext(), R.color.text_primary);
    }

    private void setupListeners() {

        panelTeamA.setOnClickListener(v -> {
            if (!isMatchRunning && !isMatchPaused) {
                startMatch(matchTimeRemaining);
                return;
            }
            if (isMatchRunning && !isMatchPaused && !isTimeoutActive) {
                if (isTeamARaiding) {
                    if (!isRaidRunning) {
                        startRaid(true, raidTimeDefault);
                    } else {
                        endRaid(true);
                    }
                }
            }
        });

        panelTeamB.setOnClickListener(v -> {
            if (!isMatchRunning && !isMatchPaused) {
                startMatch(matchTimeRemaining);
                return;
            }
            if (isMatchRunning && !isMatchPaused && !isTimeoutActive) {
                if (!isTeamARaiding) {
                    if (!isRaidRunning) {
                        startRaid(false, raidTimeDefault);
                    } else {
                        endRaid(false);
                    }
                }
            }
        });

        btnPause.setOnClickListener(v -> togglePause());
        btnTimeout.setOnClickListener(v -> handleTimeout());
        btnReset.setOnClickListener(v -> handleReset());
    }

    private void updateButtonStates() {
        boolean isMatchActive = isMatchRunning && !isMatchPaused && !isTimeoutActive;

        // Pause Button
        btnPause.setEnabled(isMatchRunning || isMatchPaused);
        btnPause.setIconTint(ColorStateList.valueOf(btnPause.isEnabled() ? colorTextPrimary : colorIconDisabledTint));

        // Timeout Button
        boolean canCallTimeout = isMatchActive && !isRaidRunning;
        btnTimeout.setEnabled(canCallTimeout);
        btnTimeout.setIconTint(ColorStateList.valueOf(btnTimeout.isEnabled() ? colorTextPrimary : colorIconDisabledTint));

        // Reset Button (Always enabled)
        btnReset.setEnabled(true);
    }

    private void startMatch(long duration) {
        if (duration <= 0) return;

        isMatchRunning = true;
        isMatchPaused = false;

        setBottomNavVisibility(View.GONE);

        if (isTimeoutActive && timeoutTimer != null) {
            timeoutTimer.cancel();
            isTimeoutActive = false;
        }

        updateButtonStates();

        matchTimer = new CountDownTimer(duration, MATCH_TIMER_TICK) {
            @Override
            public void onTick(long millisUntilFinished) {
                matchTimeRemaining = millisUntilFinished;
                updateMatchTimer();

                if (isFirstHalf && matchTimeRemaining <= 1000 && !isMatchPaused) {
                    matchTimeRemaining = 0;
                    matchTimer.cancel();
                    handleHalftimeCheck();
                }
            }

            @Override
            public void onFinish() {
                matchTimeRemaining = 0;
                updateMatchTimer();
                isMatchRunning = false;
                isMatchPaused = true;
                stopRaid(true);

                updateButtonStates();

                tvHalfIndicator.setText("Match Ended!");
                Toast.makeText(getContext(), "Match Finished!", Toast.LENGTH_LONG).show();
                setBottomNavVisibility(View.VISIBLE);
                updateTeamPanels();
            }
        }.start();
        updateTeamPanels();
    }

    private void startMatch() {
        startMatch(matchTimeRemaining);
    }

    private void startRaid(boolean isTeamA, long duration) {
        if (raidTimer != null) {
            raidTimer.cancel();
        }

        isTeamARaiding = isTeamA;
        isRaidRunning = true;

        updateRaidMessage(isTeamARaiding, false);

        updateTeamPanels();
        updateButtonStates();

        raidTimer = new CountDownTimer(duration, RAID_TIMER_TICK) {
            @Override
            public void onTick(long millisUntilFinished) {
                raidTimeRemaining = millisUntilFinished;
                updateRaidTimer();

                if (matchTimeRemaining <= 0) {
                    if (matchTimer != null) matchTimer.cancel();
                }
            }

            @Override
            public void onFinish() {
                raidTimeRemaining = raidTimeDefault;
                updateRaidTimer();
                isRaidRunning = false;
                updateButtonStates();

                if (matchTimeRemaining <= 0 && isFirstHalf) {
                    handleHalftimeTransition();
                    return;
                }

                isTeamARaiding = !isTeamARaiding;
                Toast.makeText(getContext(), "Raid Time Out! Change of possession.", Toast.LENGTH_SHORT).show();

                updateRaidMessage(isTeamARaiding, true);
                updateTeamPanels();
            }
        }.start();
    }

    private void endRaid(boolean raidingTeam) {
        if (raidTimer != null) {
            raidTimer.cancel();
        }
        isRaidRunning = false;
        raidTimeRemaining = raidTimeDefault;
        updateRaidTimer();
        updateButtonStates();

        if (matchTimeRemaining <= 0 && isFirstHalf) {
            handleHalftimeTransition();
            return;
        }

        isTeamARaiding = !raidingTeam;
        Toast.makeText(getContext(), "Raid finished! Next raider: " + (isTeamARaiding ? teamAName : teamBName), Toast.LENGTH_SHORT).show();

        updateRaidMessage(isTeamARaiding, true);
        updateTeamPanels();
    }

    private void stopRaid(boolean fullReset) {
        if (raidTimer != null) {
            raidTimer.cancel();
        }
        isRaidRunning = false;
        updateButtonStates();

        if (fullReset) {
            raidTimeRemaining = raidTimeDefault;
            updateRaidTimer();
        }

        if (isMatchRunning && !isMatchPaused && matchTimeRemaining > 0) {
            updateRaidMessage(isTeamARaiding, true);
        } else {
            updateRaidMessage(true, false);
            updateRaidMessage(false, false);
        }
        updateTeamPanels();
    }

    private void togglePause() {
        if (matchTimeRemaining <= 0 && !isFirstHalf) {
            Toast.makeText(getContext(), "Match has finished.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isMatchRunning || isTimeoutActive) {
            // PAUSE SEQUENCE
            if (matchTimer != null) matchTimer.cancel();
            if (raidTimer != null) raidTimer.cancel();
            if (timeoutTimer != null) timeoutTimer.cancel();

            isMatchRunning = false;
            isMatchPaused = true;
            isTimeoutActive = false;

            // REQUIRED: Toggle icon to PLAY
            btnPause.setIconResource(R.drawable.ic_play_24);

            panelTeamA.setClickable(false);
            panelTeamB.setClickable(false);

            updateButtonStates();
            updateTeamPanels();
            updateRaidMessage(true, false);
            updateRaidMessage(false, false);

            Toast.makeText(getContext(), "Match Paused", Toast.LENGTH_SHORT).show();

        } else if (isMatchPaused) {
            // RESUME/PLAY SEQUENCE
            // REQUIRED: Show the dialog to decide possession
            showNextRaidDecisionDialog();

            // Actual resumption is in onRaidDecision
        }
    }

    private void resumeMatchAfterPause() {
        // Called by onRaidDecision()

        if (tvHalfIndicator.getText().equals("Halftime Break")) {
            tvHalfIndicator.setText("Second Half");
            isFirstHalf = false;
            isTeamARaiding = !isTeamAStartedFirstHalf;
        }

        // 1. UI updates: Change icon back to PAUSE
        btnPause.setIconResource(R.drawable.ic_pause);

        panelTeamA.setClickable(true);
        panelTeamB.setClickable(true);

        // 2. Resume Match Clock. This sets isMatchRunning=true, isMatchPaused=false.
        startMatch(matchTimeRemaining);

        // 3. Force update the Raid Timer display (time was reset in onRaidDecision)
        updateRaidTimer();

        // 4. Set UI for the next raid
        updateRaidMessage(isTeamARaiding, true);
        isRaidRunning = false; // Next click will START the raid

        updateButtonStates();
        Toast.makeText(getContext(), "Match Resumed", Toast.LENGTH_SHORT).show();
        updateTeamPanels();
    }

    private void showNextRaidDecisionDialog() {
        // FIX: The implementation for this listener method was missing, causing the error.
        NextRaidDialog dialog = new NextRaidDialog(teamAName, teamBName, isTeamARaiding);
        dialog.setNextRaidListener(this);
        dialog.show(getParentFragmentManager(), "NextRaidDecision");
    }

    // FIX: Required implementation of NextRaidDialog.NextRaidListener
    @Override
    public void onRaidDecision(boolean isTeamA, boolean isSameTeam) {
        // Called by NextRaidDialog

        // 1. Set the designated raider based on the dialog's decision
        isTeamARaiding = isTeamA;

        // 2. The raid timer must always reset to 30s after the dialog is used.
        raidTimeRemaining = raidTimeDefault;

        // 3. Resume the match now that possession is decided
        resumeMatchAfterPause();
    }


    // --- TIMEOUT LOGIC ---

    private void handleTimeout() {
        if (!isMatchRunning || isMatchPaused) {
            Toast.makeText(getContext(), "Match must be running to call a timeout.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isTimeoutActive) {
            Toast.makeText(getContext(), "Timeout already active.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isRaidRunning) {
            Toast.makeText(getContext(), "Cannot call timeout during a raid.", Toast.LENGTH_SHORT).show();
            return;
        }

        showTimeoutDecisionDialog();
    }

    private void showTimeoutDecisionDialog() {
        TimeoutDecisionDialog dialog = new TimeoutDecisionDialog(
                teamAName, teamBName, teamATimeoutsLeft, teamBTimeoutsLeft
        );
        dialog.setTimeoutDecisionListener(this);
        dialog.show(getParentFragmentManager(), "TimeoutDecision");
    }

    @Override
    public void onTimeoutDecision(boolean isTeamA) {
        // Called by TimeoutDecisionDialog after selection

        teamACalledTimeout = isTeamA;

        // 1. Stop all clocks and update state
        if (matchTimer != null) matchTimer.cancel();
        if (raidTimer != null) raidTimer.cancel();

        isMatchRunning = false;
        isRaidRunning = false;
        isTimeoutActive = true;

        // 2. Decrement count
        if (teamACalledTimeout) {
            teamATimeoutsLeft--;
        } else {
            teamBTimeoutsLeft--;
        }
        updateTimeoutUI();

        // 3. Update UI state
        updateButtonStates();

        panelTeamA.setClickable(false);
        panelTeamB.setClickable(false);

        updateRaidMessage(true, false);
        updateRaidMessage(false, false);

        updateTeamPanels();

        // 4. Start 30s Timeout Timer
        timeoutTimeRemaining = TIMEOUT_DURATION;
        Toast.makeText(getContext(), (teamACalledTimeout ? teamAName : teamBName) + " Timeout (30s)", Toast.LENGTH_LONG).show();

        timeoutTimer = new CountDownTimer(TIMEOUT_DURATION, TIMEOUT_TIMER_TICK) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeoutTimeRemaining = millisUntilFinished;
                updateTimeoutCountdown();
            }

            @Override
            public void onFinish() {
                isTimeoutActive = false;
                flOverlayContainer.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Timeout ended. Resuming match.", Toast.LENGTH_LONG).show();

                resumeMatchAfterTimeoutFromTimeout();
            }
        }.start();
        flOverlayContainer.setVisibility(View.VISIBLE);
        updateTimeoutCountdown();
    }

    private void updateTimeoutCountdown() {
        long totalSeconds = timeoutTimeRemaining / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        tvTimeoutCountdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void resumeMatchAfterTimeoutFromTimeout() {
        // Match resumes immediately after timeout, continuing the pre-timeout possession

        // Reset raid timer for a new raid attempt
        raidTimeRemaining = raidTimeDefault;
        updateRaidTimer();

        // Resume Match Clock. This sets isMatchRunning=true, isMatchPaused=false.
        startMatch(matchTimeRemaining);

        panelTeamA.setClickable(true);
        panelTeamB.setClickable(true);

        // Set UI for the next raid (using the possession state from before the timeout)
        updateRaidMessage(isTeamARaiding, true);

        // Update states: Crucial: Raid is NOT running, next click starts it.
        isMatchPaused = false;
        isRaidRunning = false;

        updateButtonStates();
        updateTeamPanels();
        Toast.makeText(getContext(), "Match Resumed after Timeout.", Toast.LENGTH_SHORT).show();
    }


    private void handleReset() {
        if (matchTimer != null) matchTimer.cancel();
        if (raidTimer != null) raidTimer.cancel();
        if (timeoutTimer != null) timeoutTimer.cancel();
        getParentFragmentManager().popBackStack();
    }

    // --- UI Updates ---

    private void updateTimeoutUI() {
        if (tvTeamATimeoutLeft != null) {
            tvTeamATimeoutLeft.setText(String.format(Locale.getDefault(), "Timeouts Left: %d", teamATimeoutsLeft));
        }
        if (tvTeamBTimeoutLeft != null) {
            tvTeamBTimeoutLeft.setText(String.format(Locale.getDefault(), "Timeouts Left: %d", teamBTimeoutsLeft));
        }
    }

    private void updateMatchTimer() {
        long totalSeconds = matchTimeRemaining / MATCH_TIMER_TICK;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        tvMatchTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateRaidTimer() {
        long seconds = raidTimeRemaining / 1000;
        long millis = (raidTimeRemaining % 1000) / 10;

        String timeStr;
        if (seconds >= 10) {
            timeStr = String.format(Locale.getDefault(), "%02d", seconds);
        } else if (seconds >= 0) {
            timeStr = String.format(Locale.getDefault(), "%d.%02d", seconds, millis);
        } else {
            timeStr = "0.00";
        }

        if (isTeamARaiding) {
            tvTeamATimer.setText(timeStr);
            tvTeamBTimer.setText(String.format(Locale.getDefault(), "%02d", raidTimeSeconds));
        } else {
            tvTeamBTimer.setText(timeStr);
            tvTeamATimer.setText(String.format(Locale.getDefault(), "%02d", raidTimeSeconds));
        }
    }

    private void updateRaidMessage(boolean isTeamA, boolean isVisible) {
        tvTeamAMsg.setVisibility(View.INVISIBLE);
        tvTeamBMsg.setVisibility(View.INVISIBLE);

        if (isVisible) {
            TextView msgView = isTeamA ? tvTeamAMsg : tvTeamBMsg;
            msgView.setText("Click to Start");
            msgView.setVisibility(View.VISIBLE);
        }
    }

    private void updateTeamPanels() {
        tvTeamAName.setBackgroundColor(colorInactive);
        tvTeamBName.setBackgroundColor(colorInactive);
        tvTeamAName.setTextColor(colorTextPrimary);
        tvTeamBName.setTextColor(colorTextPrimary);


        if (isTimeoutActive) {
            // Highlight the team that called the timeout
            TextView tvName = teamACalledTimeout ? tvTeamAName : tvTeamBName;

            tvName.setBackgroundColor(colorPanelActive);
            tvName.setTextColor(colorTextActive);

        } else if (isMatchRunning && !isMatchPaused) {
            // Match is running: Highlight the raiding team (normal state)
            if (isTeamARaiding) {
                tvTeamAName.setBackgroundColor(colorActive);
            } else {
                tvTeamBName.setBackgroundColor(colorActive);
            }
        } else {
            // Paused, Match Ended, or Initial State: Neutral/Initial Highlight
            if (!isMatchRunning && !isMatchPaused && !isTimeoutActive) {
                if(isTeamAFirst){
                    tvTeamAName.setBackgroundColor(colorActive);
                }else{
                    tvTeamBName.setBackgroundColor(colorActive);
                }
            }
        }
    }

    private void handleHalftimeCheck() {
        if (isRaidRunning) {
            return;
        }
        handleHalftimeTransition();
    }

    private void handleHalftimeTransition() {
        isFirstHalf = true;
        isMatchRunning = false;
        isMatchPaused = true;
        isRaidRunning = false;
        isTimeoutActive = false;

        flOverlayContainer.setVisibility(View.GONE);

        matchTimeRemaining = halfTimeDuration;
        raidTimeRemaining = raidTimeDefault;
        updateMatchTimer();
        updateRaidTimer();

        tvHalfIndicator.setText("Halftime Break");

        btnPause.setIconResource(R.drawable.ic_play_24);

        panelTeamA.setClickable(false);
        panelTeamB.setClickable(false);

        updateButtonStates();
        updateTeamPanels();
        updateRaidMessage(true, false);
        updateRaidMessage(false, false);

        Toast.makeText(getContext(), "HALFTIME! Tap START button to begin Second Half.", Toast.LENGTH_LONG).show();
        setBottomNavVisibility(View.VISIBLE);
    }

    // --- Utility Methods ---

    private void updateUI() {
        updateMatchTimer();
        updateRaidTimer();

        tvHalfIndicator.setText(isFirstHalf ? "First Half" : "Second Half");

        if (tvFullTime != null) {
            tvFullTime.setText("Full Time : "+String.format(Locale.getDefault(), "%d:%02d", matchTimeMinutes, 0));
        }

        updateRaidMessage(isTeamAFirst, true);
        updateTimeoutUI();

        flOverlayContainer.setVisibility(View.GONE);

        updateButtonStates();

        panelTeamA.setClickable(true);
        panelTeamB.setClickable(true);

        updateTeamPanels();
    }

    private void setBottomNavVisibility(int visibility) {
        if (getActivity() instanceof MainActivity) {
            BottomNavigationView nav = ((MainActivity) getActivity()).getBottomNavigationView();
            if (nav != null) {
                nav.setVisibility(visibility);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (matchTimer != null) matchTimer.cancel();
        if (raidTimer != null) raidTimer.cancel();
        if (timeoutTimer != null) timeoutTimer.cancel();
    }
}