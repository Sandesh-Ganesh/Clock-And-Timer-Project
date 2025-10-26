package com.example.clockandtimerapp.timer;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.clockandtimerapp.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * PomodoroActivity manages the main timer logic and UI state for the Pomodoro, Short Break, and Long Break modes.
 */
public class pomodoroActivity extends AppCompatActivity {

    private TextView mTextViewTimer;
    private TextView mTextViewStatus;
    private MaterialButton mButtonStart, mButtonEnd;
    private TextView mBtnPomodoro, mBtnShortBreak, mBtnLongBreak;
    private TextView mTextViewCycle;
    private TextView mTextViewBackArrow;

    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;

    // --- Time Constants for different modes (in milliseconds) ---
    // Note: These are 25m, 5m, 15m.
    private static final long POMODORO_TIME = 1500000;
    private static final long SHORT_BREAK_TIME = 300000;
    private static final long LONG_BREAK_TIME = 900000;

    private long mTimeLeftInMillis;
    private long currentPresetTime = POMODORO_TIME;
    private int currentCycle = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro_timer);

        // 1. Initialize views by linking them to their IDs
        mTextViewTimer = findViewById(R.id.text_timer);
        mTextViewStatus = findViewById(R.id.text_status);
        mButtonStart = findViewById(R.id.btn_start);
        mButtonEnd = findViewById(R.id.btn_end);
        mTextViewCycle = findViewById(R.id.text_cycle);

        mBtnPomodoro = findViewById(R.id.btn_pomodoro);
        mBtnShortBreak = findViewById(R.id.btn_short_break);
        mBtnLongBreak = findViewById(R.id.btn_long_break);

        mTextViewBackArrow = findViewById(R.id.text_back_arrow);

        // Initialize state
        mTimeLeftInMillis = POMODORO_TIME;
        currentPresetTime = POMODORO_TIME;

        // Back button click listener to navigate back
        if (mTextViewBackArrow != null) {
            mTextViewBackArrow.setOnClickListener(v -> {
                // finish() closes the current activity
                finish();
            });
        }

        // 2. --- Timer Button Listeners ---
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                }
            }
        });

        mButtonEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Correctly resets the whole cycle as well
                currentCycle = 1;
                currentPresetTime = POMODORO_TIME;
                resetTimer();
                updateModeUI(getViewForPreset(currentPresetTime), getStatusTextForPreset(currentPresetTime));
                updateCycleText();
                Toast.makeText(pomodoroActivity.this, "Session Ended and Reset", Toast.LENGTH_SHORT).show();
            }
        });

        // 3. --- Mode Selector Listeners ---
        mBtnPomodoro.setOnClickListener(v -> switchMode(POMODORO_TIME));
        mBtnShortBreak.setOnClickListener(v -> switchMode(SHORT_BREAK_TIME));
        mBtnLongBreak.setOnClickListener(v -> switchMode(LONG_BREAK_TIME));

        // Set initial UI state
        updateModeUI(getViewForPreset(currentPresetTime), getStatusTextForPreset(currentPresetTime));
        updateCountDownText();
        updateCycleText();
    }

    // --- Core Timer Logic ---
    private void startTimer() {
        if (mTimeLeftInMillis == 0) {
            Toast.makeText(this, "Time is zero. Resetting to full preset.", Toast.LENGTH_SHORT).show();
            mTimeLeftInMillis = currentPresetTime;
        }

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                mTextViewStatus.setText(currentPresetTime == POMODORO_TIME ? "Working Hard!" : "Relaxing...");
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;

                // Corrected Pomodoro Cycle Logic
                if (currentPresetTime == POMODORO_TIME) {
                    if (currentCycle % 4 == 0) {
                        switchMode(LONG_BREAK_TIME); // Go to Long Break
                    } else {
                        switchMode(SHORT_BREAK_TIME); // Go to Short Break
                    }
                    currentCycle++; // Advance cycle count
                    if (currentCycle > 4) currentCycle = 1; // Reset cycle after Long Break
                } else {
                    switchMode(POMODORO_TIME);
                }

                updateCycleText();
                Toast.makeText(pomodoroActivity.this, "Time's up! Starting " + getStatusTextForPreset(currentPresetTime), Toast.LENGTH_LONG).show();
                startTimer();
            }
        }.start();

        mTimerRunning = true;
    }

    private void pauseTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        mTimerRunning = false;
    }

    private void resetTimer() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
        mTimerRunning = false;
        mTimeLeftInMillis = currentPresetTime;
        updateCountDownText();
        updateModeUI(getViewForPreset(currentPresetTime), getStatusTextForPreset(currentPresetTime));
    }

    private void switchMode(long newTime) {
        if (mTimerRunning && mTimeLeftInMillis != currentPresetTime) {
            Toast.makeText(this, "Timer running. Pause before switching modes.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentPresetTime != newTime) {
            currentPresetTime = newTime;
        }
        resetTimer();
    }

    private void updateCountDownText() {
        int minutes = (int) (mTimeLeftInMillis / 1000) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        mTextViewTimer.setText(timeLeftFormatted);
    }

    private void updateCycleText() {
        int displayCycle = currentCycle == 4 ? 4 : (currentCycle > 4 ? 1 : currentCycle);
        mTextViewCycle.setText(String.format(Locale.getDefault(), "Cycle: %d / 4", displayCycle));
    }

    private TextView getViewForPreset(long preset) {
        if (preset == POMODORO_TIME) return mBtnPomodoro;
        if (preset == SHORT_BREAK_TIME) return mBtnShortBreak;
        return mBtnLongBreak;
    }

    private String getStatusTextForPreset(long preset) {
        if (preset == POMODORO_TIME) return "Focus Time!";
        if (preset == SHORT_BREAK_TIME) return "Short Rest.";
        return "Time for a Long Break!";
    }

    private void updateModeUI(TextView activeView, String statusText) {
        // Use ContextCompat for modern, safer color access
        int greyColor = ContextCompat.getColor(this, R.color.text_light_grey);
        int whiteColor = ContextCompat.getColor(this, R.color.white);

        // NOTE: R.color.text_light_grey and R.drawable.selected_mode_background must exist

        mBtnPomodoro.setTextColor(greyColor);
        mBtnShortBreak.setTextColor(greyColor);
        mBtnLongBreak.setTextColor(greyColor);
        mBtnPomodoro.setBackground(null);
        mBtnShortBreak.setBackground(null);
        mBtnLongBreak.setBackground(null);

        activeView.setTextColor(whiteColor);
        activeView.setBackgroundResource(R.drawable.selected_mode_background);
        mTextViewStatus.setText(statusText);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cancel timer when leaving the activity to prevent leaks
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }
}