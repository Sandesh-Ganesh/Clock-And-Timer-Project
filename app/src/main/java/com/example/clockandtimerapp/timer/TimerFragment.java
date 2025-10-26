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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        setupNumberPickers();
        setupPresetButtons();
        setupPomodoroButton();
        setupFABs();

        updateTimeLeft();
        updateCountDownText();
        updatePlayPauseButtonState();
    }

    private void setupNumberPickers() {
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

        NumberPicker.OnValueChangeListener listener = (picker, oldVal, newVal) -> {
            updateTimeLeft();
            updateCountDownText();
            setActivePreset(View.NO_ID);
            updatePlayPauseButtonState();
        };

        numberPickerHr.setOnValueChangedListener(listener);
        numberPickerMin.setOnValueChangedListener(listener);
        numberPickerSec.setOnValueChangedListener(listener);
    }

    private void setupPresetButtons() {
        button1Min.setOnClickListener(v -> setPresetTime(0, 1, 0, R.id.button1Min));
        button5Min.setOnClickListener(v -> setPresetTime(0, 5, 0, R.id.button5Min));
        button15Min.setOnClickListener(v -> setPresetTime(0, 15, 0, R.id.button15Min));
    }

    private void setupPomodoroButton() {
        buttonPomodoro.setOnClickListener(v -> navigateToPomodoroActivity());
    }

    private void setupFABs() {
        fabPlayPause.setOnClickListener(v -> {
            if (timerRunning) pauseTimer();
            else startTimer();
        });

        fabReset.setOnClickListener(v -> resetTimer());
    }

    private void navigateToPomodoroActivity() {
        Intent intent = new Intent(requireActivity(), pomodoroActivity.class);
        startActivity(intent);
    }

    private void setPresetTime(int hours, int minutes, int seconds, int buttonId) {
        if (timerRunning) pauseTimer();

        numberPickerHr.setValue(hours);
        numberPickerMin.setValue(minutes);
        numberPickerSec.setValue(seconds);

        updateTimeLeft();
        updateCountDownText();
        setActivePreset(buttonId);
        durationSetInMillis = timeLeftInMillis;

        updatePlayPauseButtonState();
    }

    private void updateTimeLeft() {
        long hours = numberPickerHr.getValue();
        long minutes = numberPickerMin.getValue();
        long seconds = numberPickerSec.getValue();
        timeLeftInMillis = (hours * 3600 + minutes * 60 + seconds) * 1000;

        if (timeLeftInMillis > 0 && !timerRunning) {
            durationSetInMillis = timeLeftInMillis;
        }
    }

    private void updateCountDownText() {
        long totalSeconds = timeLeftInMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        textViewCurrentTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void startTimer() {
        if (timeLeftInMillis == 0) {
            Toast.makeText(requireContext(), "Please set a time first.", Toast.LENGTH_SHORT).show();
            return;
        }

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                resetDisplayToZero();
                Toast.makeText(requireContext(), "Timer Finished!", Toast.LENGTH_LONG).show();
            }
        }.start();

        timerRunning = true;
        setNumberPickersEnabled(false);
        setPresetButtonsEnabled(false);
        updatePlayPauseButtonState();
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timerRunning = false;
        setNumberPickersEnabled(true);
        setPresetButtonsEnabled(true);
        updatePlayPauseButtonState();
    }

    private void resetTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timerRunning = false;

        timeLeftInMillis = durationSetInMillis;
        long totalSeconds = timeLeftInMillis / 1000;
        numberPickerHr.setValue((int)(totalSeconds / 3600));
        numberPickerMin.setValue((int)((totalSeconds % 3600) / 60));
        numberPickerSec.setValue((int)(totalSeconds % 60));

        updateCountDownText();
        setNumberPickersEnabled(true);
        setPresetButtonsEnabled(true);
        setActivePreset(View.NO_ID);
        updatePlayPauseButtonState();
    }

    private void resetDisplayToZero() {
        timeLeftInMillis = 0;
        durationSetInMillis = 0;
        numberPickerHr.setValue(0);
        numberPickerMin.setValue(0);
        numberPickerSec.setValue(0);
        updateCountDownText();
        setNumberPickersEnabled(true);
        setPresetButtonsEnabled(true);
        setActivePreset(View.NO_ID);
        updatePlayPauseButtonState();
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
        int surfaceColor = R.color.surface_dark;
        button1Min.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), surfaceColor));
        button5Min.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), surfaceColor));
        button15Min.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), surfaceColor));
        activePresetButtonId = View.NO_ID;
    }

    private void updatePlayPauseButtonState() {
        boolean hasTime = timeLeftInMillis > 0;

        fabPlayPause.setEnabled(hasTime);
        fabPlayPause.setImageResource(timerRunning ? R.drawable.custom_pause_icon : R.drawable.custom_play_icon);
        fabPlayPause.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                hasTime ? R.color.primary_blue : R.color.button_disabled_mask));
        fabPlayPause.setSupportImageTintList(ContextCompat.getColorStateList(requireContext(),
                hasTime ? android.R.color.white : R.color.white));

        setResetButtonEnabled(hasTime || timerRunning);
    }

    private void setResetButtonEnabled(boolean enabled) {
        fabReset.setEnabled(enabled);
        fabReset.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(),
                enabled ? R.color.surface_dark : R.color.button_disabled_mask));
        fabReset.setSupportImageTintList(ContextCompat.getColorStateList(requireContext(),
                enabled ? R.color.text_primary : R.color.icon_disabled_tint));
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

    @Override
    public void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
            timerRunning = false;
        }
    }
}
