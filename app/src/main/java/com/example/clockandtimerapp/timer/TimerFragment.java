package com.example.clockandtimerapp.timer;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.example.clockandtimerapp.R;
// REQUIRED IMPORT: Assumed package structure
import com.example.clockandtimerapp.timer.pomodoroActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.util.Locale;

public class TimerFragment extends Fragment {

    private NumberPicker numberPickerHr, numberPickerMin, numberPickerSec;
    private TextView textViewCurrentTime;
    private FloatingActionButton fabPlayPause, fabReset;
    private Button button1Min, button5Min, button15Min, buttonPomodoro;

    private CountDownTimer countDownTimer;
    private boolean timerRunning;
    private long timeLeftInMillis;
    private long durationSetInMillis;

    private int activePresetButtonId = View.NO_ID;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // ASSUMPTION: The layout file is fragment_timer.xml
        return inflater.inflate(R.layout.fragment_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize UI components
        numberPickerHr = view.findViewById(R.id.numberPickerHr);
        numberPickerMin = view.findViewById(R.id.numberPickerMin);
        numberPickerSec = view.findViewById(R.id.numberPickerSec);
        textViewCurrentTime = view.findViewById(R.id.textViewCurrentTime);
        fabPlayPause = view.findViewById(R.id.fabPlayPause);
        fabReset = view.findViewById(R.id.fabReset);
        button1Min = view.findViewById(R.id.button1Min);
        button5Min = view.findViewById(R.id.button5Min);
        button15Min = view.findViewById(R.id.button15Min);
        buttonPomodoro = view.findViewById(R.id.buttonPomodoro);

        // 2. Setup NumberPickers
        numberPickerHr.setMinValue(0);
        numberPickerHr.setMaxValue(23);
        numberPickerMin.setMinValue(0);
        numberPickerMin.setMaxValue(59);
        numberPickerSec.setMinValue(0);
        numberPickerSec.setMaxValue(59);

        setNumberPickerColorAndDivider(numberPickerHr);
        setNumberPickerColorAndDivider(numberPickerMin);
        setNumberPickerColorAndDivider(numberPickerSec);

        numberPickerHr.setValue(0);
        numberPickerMin.setValue(0);
        numberPickerSec.setValue(0);

        // 3. Set Listeners
        NumberPicker.OnValueChangeListener numberPickerChangeListener = (picker, oldVal, newVal) -> {
            updateTimeLeft();
            updateCountDownText();
            setActivePreset(View.NO_ID);
            updatePlayPauseButtonState();
        };
        numberPickerHr.setOnValueChangedListener(numberPickerChangeListener);
        numberPickerMin.setOnValueChangedListener(numberPickerChangeListener);
        numberPickerSec.setOnValueChangedListener(numberPickerChangeListener);

        updateTimeLeft();
        updateCountDownText();

        button1Min.setOnClickListener(v -> {
            setPresetTime(0, 1, 0);
            setActivePreset(R.id.button1Min);
            updatePlayPauseButtonState();
        });
        button5Min.setOnClickListener(v -> {
            setPresetTime(0, 5, 0);
            setActivePreset(R.id.button5Min);
            updatePlayPauseButtonState();
        });
        button15Min.setOnClickListener(v -> {
            setPresetTime(0, 15, 0);
            setActivePreset(R.id.button15Min);
            updatePlayPauseButtonState();
        });
        buttonPomodoro.setOnClickListener(v -> navigateToPomodoroActivity());

        fabPlayPause.setOnClickListener(v -> {
            if (timerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        fabReset.setOnClickListener(v -> resetTimer());

        resetPresetButtonBackgrounds();

        // Initial state setup
        fabPlayPause.setImageResource(R.drawable.custom_play_icon);
        updatePlayPauseButtonState();
    }

    // --- Button State Management ---

    private void updatePlayPauseButtonState() {
        // --- Get Colors ---
        int primaryBlue = ContextCompat.getColor(requireContext(), R.color.primary_blue);
        int white = ContextCompat.getColor(requireContext(), android.R.color.white);
        int disabledIconTint = ContextCompat.getColor(requireContext(), R.color.icon_disabled_tint);
        int disabledBgMask = ContextCompat.getColor(requireContext(), R.color.button_disabled_mask);

        // --- ColorStateList Construction ---
        // Define states: { 0: Enabled }, { 1: Disabled }
        int[][] states = new int[][] {
                { android.R.attr.state_enabled },
                { -android.R.attr.state_enabled }
        };

        // BACKGROUND: primaryBlue when enabled, mask when disabled
        int[] bgColors = new int[] { primaryBlue, disabledBgMask };
        ColorStateList bgCsl = new ColorStateList(states, bgColors);

        // ICON: White when enabled, disabled tint when disabled
        int[] iconColors = new int[] { white, disabledIconTint };
        ColorStateList iconCsl = new ColorStateList(states, iconColors);

        // --- Apply Logic ---

        // Apply the CSLs upfront to take full programmatic control
        fabPlayPause.setSupportImageTintList(iconCsl); // CRITICAL: This controls icon color based on enabled state

        boolean isTimeSet = timeLeftInMillis > 0;

        if (!timerRunning) {
            // State: Paused or Initial (Play icon)
            fabPlayPause.setImageResource(R.drawable.custom_play_icon);

            if (isTimeSet) {
                // ENABLED: Primary Blue BG
                fabPlayPause.setEnabled(true);
                // Apply the CSL which forces primaryBlue when enabled
                fabPlayPause.setBackgroundTintList(bgCsl);
            } else {
                // DISABLED: Force Mask BG
                fabPlayPause.setEnabled(false);
                // CRITICAL FIX: Manually set background tint to mask color when disabled
                fabPlayPause.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.button_disabled_mask));
            }

        } else {
            // State: Running (Pause icon)
            fabPlayPause.setImageResource(R.drawable.custom_pause_icon);
            fabPlayPause.setEnabled(true);
            // Ensure it's fully blue via CSL
            fabPlayPause.setBackgroundTintList(bgCsl);
        }

        setResetButtonEnabled(isTimeSet || timerRunning);
    }

    private void setResetButtonEnabled(boolean enabled) {
        fabReset.setEnabled(enabled);

        // Reset Enabled Colors
        int resetEnabledBgRes = R.color.surface_dark;
        int resetEnabledIconRes = R.color.text_primary;

        // Reset Disabled Colors
        int disabledIconTintRes = R.color.icon_disabled_tint;
        int disabledBgMaskRes = R.color.button_disabled_mask;

        int resetBg = enabled ? resetEnabledBgRes : disabledBgMaskRes;
        int resetTint = enabled ? resetEnabledIconRes : disabledIconTintRes;

        fabReset.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), resetBg));
        fabReset.setSupportImageTintList(ContextCompat.getColorStateList(requireContext(), resetTint));
    }


    // --- Preset Button Management ---

    private void setActivePreset(int buttonId) {
        if (activePresetButtonId == buttonId) return;

        resetPresetButtonBackgrounds();

        activePresetButtonId = buttonId;

        if (buttonId != View.NO_ID) {
            Button activeBtn = getView().findViewById(buttonId);
            if (activeBtn != null) {
                activeBtn.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_blue));
            }
        }
    }

    private void resetPresetButtonBackgrounds() {
        int surfaceColorRes = R.color.surface_dark;

        if (button1Min != null) button1Min.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), surfaceColorRes));
        if (button5Min != null) button5Min.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), surfaceColorRes));
        if (button15Min != null) button15Min.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), surfaceColorRes));

        activePresetButtonId = View.NO_ID;
    }


    // --- Core Timer Logic ---

    private void navigateToPomodoroActivity() {
        Intent intent = new Intent(requireActivity(), pomodoroActivity.class);
        startActivity(intent);
    }

    private void setNumberPickerColorAndDivider(NumberPicker numberPicker) {
        try {
            Field[] fields = NumberPicker.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getName().equals("mSelectionDivider")) {
                    field.setAccessible(true);
                    field.set(numberPicker, new ColorDrawable(Color.TRANSPARENT));
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTimeLeft() {
        long hours = numberPickerHr.getValue();
        long minutes = numberPickerMin.getValue();
        long seconds = numberPickerSec.getValue();
        timeLeftInMillis = (hours * 3600 + minutes * 60 + seconds) * 1000;
        if (timeLeftInMillis > 0) {
            durationSetInMillis = timeLeftInMillis;
        }
    }

    private void startTimer() {
        if (timeLeftInMillis == 0) {
            Toast.makeText(requireContext(), "Please set a time first.", Toast.LENGTH_SHORT).show();
            return;
        }

        durationSetInMillis = timeLeftInMillis;
        setActivePreset(View.NO_ID);
        setNumberPickersEnabled(false);

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                fabPlayPause.setImageResource(R.drawable.custom_play_icon);
                setNumberPickersEnabled(true);
                setPresetButtonsEnabled(true);
                Toast.makeText(requireContext(), "Timer Finished!", Toast.LENGTH_LONG).show();
                resetDisplayToZero();
            }
        }.start();

        timerRunning = true;
        fabPlayPause.setImageResource(R.drawable.custom_pause_icon);

        // Ensure FAB is enabled and fully blue while running
        fabPlayPause.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary_blue));
        fabPlayPause.setSupportImageTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.white));

        fabPlayPause.setEnabled(true);
        setResetButtonEnabled(true);
        setPresetButtonsEnabled(false);
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        fabPlayPause.setImageResource(R.drawable.custom_play_icon);
        setNumberPickersEnabled(true);
        setPresetButtonsEnabled(true);
        updatePlayPauseButtonState();
    }

    private void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        fabPlayPause.setImageResource(R.drawable.custom_play_icon);

        resetPresetButtonBackgrounds();

        timeLeftInMillis = durationSetInMillis;

        long totalSeconds = durationSetInMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        numberPickerHr.setValue((int) hours);
        numberPickerMin.setValue((int) minutes);
        numberPickerSec.setValue((int) seconds);

        updateCountDownText();

        setNumberPickersEnabled(true);
        setPresetButtonsEnabled(true);
        updatePlayPauseButtonState();
    }

    private void resetDisplayToZero() {
        timeLeftInMillis = 0;
        durationSetInMillis = 0;
        numberPickerHr.setValue(0);
        numberPickerMin.setValue(0);
        numberPickerSec.setValue(0);
        updateCountDownText();
        resetPresetButtonBackgrounds();
        updatePlayPauseButtonState();
    }

    private void setPresetTime(int hours, int minutes, int seconds) {
        if (timerRunning) {
            pauseTimer();
        }
        numberPickerHr.setValue(hours);
        numberPickerMin.setValue(minutes);
        numberPickerSec.setValue(seconds);
        updateTimeLeft();
        updateCountDownText();
    }

    private void updateCountDownText() {
        long totalSeconds = timeLeftInMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        textViewCurrentTime.setText(timeLeftFormatted);
    }

    private void setNumberPickersEnabled(boolean enabled) {
        numberPickerHr.setEnabled(enabled);
        numberPickerMin.setEnabled(enabled);
        numberPickerSec.setEnabled(enabled);
    }

    private void setPresetButtonsEnabled(boolean enabled) {
        button1Min.setEnabled(enabled);
        button5Min.setEnabled(enabled);
        button15Min.setEnabled(enabled);
        buttonPomodoro.setEnabled(enabled);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            timerRunning = false;
        }
    }
}