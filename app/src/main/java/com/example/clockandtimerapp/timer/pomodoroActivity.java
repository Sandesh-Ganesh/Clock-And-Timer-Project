package com.example.clockandtimerapp.timer;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.clockandtimerapp.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class pomodoroActivity extends AppCompatActivity {

    private TextView mTextViewTimer, mTextViewStatus, mTextViewCycle, mTextViewBackArrow;
    private MaterialButton mBtnStart, mBtnEnd;
    private TextView mBtnPomodoro, mBtnShortBreak, mBtnLongBreak;

    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning = false;
    private boolean mIsPlaying = false;

    private static final long POMODORO_TIME = 25 * 60 * 1000;   // 25 minutes
    private static final long SHORT_BREAK_TIME = 5 * 60 * 1000;  // 5 minutes
    private static final long LONG_BREAK_TIME = 15 * 60 * 1000;  // 15 minutes


    private long mTimeLeftInMillis;
    private long currentPresetTime = POMODORO_TIME;
    private int currentPomodoroCycle = 1; // 1 to 4
    private boolean isLongBreak = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pomodoro_timer);

        // Initialize views
        mTextViewTimer = findViewById(R.id.text_timer);
        mTextViewStatus = findViewById(R.id.text_status);
        mTextViewCycle = findViewById(R.id.text_cycle);
        mTextViewBackArrow = findViewById(R.id.text_back_arrow);

        mBtnStart = findViewById(R.id.btn_start);
        mBtnEnd = findViewById(R.id.btn_end);

        mBtnPomodoro = findViewById(R.id.btn_pomodoro);
        mBtnShortBreak = findViewById(R.id.btn_short_break);
        mBtnLongBreak = findViewById(R.id.btn_long_break);

        // Initial timer state
        mTimeLeftInMillis = POMODORO_TIME;
        currentPresetTime = POMODORO_TIME;

        // Back button
        mTextViewBackArrow.setOnClickListener(v -> finish());

        // Start / Pause toggle
        mBtnStart.setOnClickListener(v -> toggleTimer());

        // Reset button
        mBtnEnd.setOnClickListener(v -> resetSession());

        // Mode buttons (manual switch)
        mBtnPomodoro.setOnClickListener(v -> switchMode(POMODORO_TIME));
        mBtnShortBreak.setOnClickListener(v -> switchMode(SHORT_BREAK_TIME));
        mBtnLongBreak.setOnClickListener(v -> switchMode(LONG_BREAK_TIME));

        updateModeUI(getViewForPreset(currentPresetTime), getStatusTextForPreset(currentPresetTime));
        updateCountDownText();
        updateCycleText();
        updatePlayIcon();
    }

    // --- Timer Control ---
    private void toggleTimer() {
        if (mTimerRunning) {
            pauseTimer();
        } else {
            startTimer();
        }
        mIsPlaying = !mIsPlaying;
        updatePlayIcon();
    }

    private void startTimer() {
        if (mTimeLeftInMillis <= 0) mTimeLeftInMillis = currentPresetTime;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                mIsPlaying = false;
                updatePlayIcon();
                handleNextCycle();
            }
        }.start();
        mTimerRunning = true;
    }

    private void pauseTimer() {
        if (mCountDownTimer != null) mCountDownTimer.cancel();
        mTimerRunning = false;
    }

    private void resetTimer() {
        if (mCountDownTimer != null) mCountDownTimer.cancel();
        mTimerRunning = false;
        mTimeLeftInMillis = currentPresetTime;
        updateCountDownText();
    }

    private void resetSession() {
        pauseTimer();
        currentPomodoroCycle = 1;
        isLongBreak = false;
        currentPresetTime = POMODORO_TIME;
        resetTimer();
        updateModeUI(getViewForPreset(currentPresetTime), getStatusTextForPreset(currentPresetTime));
        updateCycleText();
        Toast.makeText(this, "Session Reset", Toast.LENGTH_SHORT).show();
    }

    // --- Mode Switching ---
    private void switchMode(long newTime) {
        if (mTimerRunning) {
            Toast.makeText(this, "Pause timer before switching mode.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentPresetTime = newTime;
        isLongBreak = (newTime == LONG_BREAK_TIME);
        resetTimer();
        updateModeUI(getViewForPreset(currentPresetTime), getStatusTextForPreset(currentPresetTime));
        updateCycleText();
    }

    // --- Next Cycle Logic ---
    private void handleNextCycle() {
        if (currentPresetTime == POMODORO_TIME) {
            // Pomodoro finished
            if (currentPomodoroCycle < 4) {
                switchMode(SHORT_BREAK_TIME);
            } else {
                switchMode(LONG_BREAK_TIME);
                isLongBreak = true;
            }
        } else if (currentPresetTime == SHORT_BREAK_TIME) {
            // Short break finished
            currentPomodoroCycle++;
            switchMode(POMODORO_TIME);
        } else if (currentPresetTime == LONG_BREAK_TIME) {
            // Long break finished
            currentPomodoroCycle = 1;
            switchMode(POMODORO_TIME);
            isLongBreak = false;
        }
        startTimer();
    }

    // --- UI Updates ---
    private void updateCountDownText() {
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
        int minutes = (int) ((mTimeLeftInMillis / 1000) / 60) % 60;
        String formatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        mTextViewTimer.setText(formatted);
        mTextViewStatus.setText(getStatusTextForPreset(currentPresetTime));
    }

    private void updateCycleText() {
        if (isLongBreak) {
            mTextViewCycle.setText("");
        } else {
            mTextViewCycle.setText(String.format(Locale.getDefault(), "Cycle: %d / 4", currentPomodoroCycle));
        }
    }

    private TextView getViewForPreset(long preset) {
        if (preset == POMODORO_TIME) return mBtnPomodoro;
        if (preset == SHORT_BREAK_TIME) return mBtnShortBreak;
        return mBtnLongBreak;
    }

    private String getStatusTextForPreset(long preset) {
        if (preset == POMODORO_TIME) return "Focus Time!";
        if (preset == SHORT_BREAK_TIME) return "Short Break";
        return "Long Break";
    }

    private void updateModeUI(TextView activeView, String statusText) {
        int grey = ContextCompat.getColor(this, R.color.text_light_grey);
        int white = ContextCompat.getColor(this, R.color.white);

        mBtnPomodoro.setTextColor(grey);
        mBtnShortBreak.setTextColor(grey);
        mBtnLongBreak.setTextColor(grey);

        mBtnPomodoro.setBackground(null);
        mBtnShortBreak.setBackground(null);
        mBtnLongBreak.setBackground(null);

        activeView.setTextColor(white);
        activeView.setBackgroundResource(R.drawable.selected_mode_background);
        mTextViewStatus.setText(statusText);
    }

    private void updatePlayIcon() {
        int iconRes = mIsPlaying ? R.drawable.ic_pause_24 : R.drawable.ic_play_24;
        mBtnStart.setIconResource(iconRes);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCountDownTimer != null) mCountDownTimer.cancel();
    }
}
