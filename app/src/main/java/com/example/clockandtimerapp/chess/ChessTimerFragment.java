package com.example.clockandtimerapp.chess;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment; // CHANGE: Now a Fragment

import com.example.clockandtimerapp.R; // Assumes your R file path is here

import java.util.Locale;

public class ChessTimerFragment extends Fragment {

    private TextView tvTopTime, tvBottomTime;
    private TextView tvTopName, tvBottomName;
    private TextView tvTopMoves, tvBottomMoves;
    private TextView tvTopPreset, tvBottomPreset;
    private FrameLayout panelTop, panelBottom;
    private ImageButton btnPause, btnReset, btnSettings;
    private ImageButton btnTopAdjust, btnBottomAdjust, btnSound;

    private View rootView; // NEW: Holds the inflated view

    private boolean soundOn = true;

    // timing (ms)
    private long initialTimePerPlayer = 3 * 60 * 1000L;
    private long incrementPerMove = 0L;

    // runtime state
    private long topRemaining, bottomRemaining;
    private boolean topRunning = false;
    private boolean bottomRunning = false;
    private boolean paused = true;

    private int topMoves = 0;
    private int bottomMoves = 0;

    private final Handler handler = new Handler();
    private Runnable tickRunnable;
    private long lastTickTime;

    // NEW: onCreateView to inflate the layout
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the Activity's old layout (R.layout.activity_main)
        rootView = inflater.inflate(R.layout.fragment_chess_timer, container, false);
        return rootView;
    }

    // NEW: onViewCreated to find views and set up listeners
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // find views (use rootView.findViewById)
        tvTopTime = rootView.findViewById(R.id.tvTopTime);
        tvBottomTime = rootView.findViewById(R.id.tvBottomTime);
        tvTopName = rootView.findViewById(R.id.tvTopName);
        tvBottomName = rootView.findViewById(R.id.tvBottomName);
        tvTopMoves = rootView.findViewById(R.id.tvTopMoves);
        tvBottomMoves = rootView.findViewById(R.id.tvBottomMoves);
        tvTopPreset = rootView.findViewById(R.id.tvTopPreset);
        tvBottomPreset = rootView.findViewById(R.id.tvBottomPreset);
        panelTop = rootView.findViewById(R.id.panelTop);
        panelBottom = rootView.findViewById(R.id.panelBottom);
        btnPause = rootView.findViewById(R.id.btnPause);
        btnReset = rootView.findViewById(R.id.btnReset);
        btnSettings = rootView.findViewById(R.id.btnSettings);
        btnTopAdjust = rootView.findViewById(R.id.btnTopAdjust);
        btnBottomAdjust = rootView.findViewById(R.id.btnBottomAdjust);
        btnSound = rootView.findViewById(R.id.btnSound);

        // map interactions:
        if (btnTopAdjust != null) btnTopAdjust.setOnClickListener(v -> showAdjustTimeDialog(true));
        if (btnBottomAdjust != null) btnBottomAdjust.setOnClickListener(v -> showAdjustTimeDialog(false));

        if (btnSettings != null) btnSettings.setOnClickListener(v -> showCustomTimeDialog());

        if (btnSound != null) btnSound.setOnClickListener(v -> {
            soundOn = !soundOn;
            if (soundOn) {
                btnSound.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
                Toast.makeText(requireContext(), "Sound On", Toast.LENGTH_SHORT).show(); // CHANGE: Use requireContext()
            } else {
                btnSound.setImageResource(android.R.drawable.ic_lock_silent_mode);
                Toast.makeText(requireContext(), "Sound Off", Toast.LENGTH_SHORT).show(); // CHANGE: Use requireContext()
            }
        });

        updatePresetTexts();
        resetClocks();
        updateUI();

        tickRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                if (!paused) {
                    long elapsed = now - lastTickTime;
                    if (topRunning) {
                        topRemaining -= elapsed;
                        if (topRemaining < 0) topRemaining = 0;
                    } else if (bottomRunning) {
                        bottomRemaining -= elapsed;
                        if (bottomRemaining < 0) bottomRemaining = 0;
                    }
                    updateUI();
                    if (topRemaining == 0 || bottomRemaining == 0) pauseClock();
                }
                lastTickTime = now;
                handler.postDelayed(this, 100);
            }
        };
        lastTickTime = System.currentTimeMillis();
        handler.post(tickRunnable);



        if (panelTop != null){
            panelTop.setOnClickListener(v -> onPlayerTap(true));
            panelTop.setEnabled(false);
        }
        if (panelBottom != null) {
            panelBottom.setOnClickListener(v -> onPlayerTap(false));
            panelBottom.setEnabled(false);
        }

        if (btnPause != null) btnPause.setOnClickListener(v -> {
            if (paused){
                panelTop.setEnabled(true);
                panelBottom.setEnabled(true);
                btnSettings.setEnabled(false);
                resumeClock();
            } else {
                pauseClock();
                panelTop.setEnabled(false);
                panelBottom.setEnabled(false);
                btnSettings.setEnabled(true);
            }
        });

        if (btnReset != null) btnReset.setOnClickListener(v -> {
            resetClocks();
            updateUI();
        });

        updatePanelColors();
    }


    private void onPlayerTap(boolean topTapped) {
        // If the game hasn’t started yet (both paused)
        if (paused && !topRunning && !bottomRunning) {
            // If top tapped first, start bottom’s clock
            if (topTapped) {
                startPlayer(false);
            } else {
                startPlayer(true);
            }
            return;
        }

        // Normal behavior once game is in progress
        if (topTapped && topRunning) {
            topRemaining += incrementPerMove;
            topMoves++;
            switchTo(false);
        } else if (!topTapped && bottomRunning) {
            bottomRemaining += incrementPerMove;
            bottomMoves++;
            switchTo(true);
        }
    }

    private void startPlayer(boolean top) {
        paused = false;
        if (btnPause != null) btnPause.setImageResource(android.R.drawable.ic_media_pause);
        topRunning = top;
        bottomRunning = !top;
        lastTickTime = System.currentTimeMillis();
        updatePanelColors();
        updateUI();
    }

    private void switchTo(boolean top) {
        paused = false;
        if (btnPause != null) btnPause.setImageResource(android.R.drawable.ic_media_pause);
        topRunning = top;
        bottomRunning = !top;
        lastTickTime = System.currentTimeMillis();
        updatePanelColors();
        updateUI();
    }

    private void pauseClock() {
        paused = true;
        topRunning = false;
        bottomRunning = false;
        if (btnPause != null) btnPause.setImageResource(android.R.drawable.ic_media_play);
        updatePanelColors();
    }

    private void resumeClock() {
        paused = false;
        if (btnPause != null) btnPause.setImageResource(android.R.drawable.ic_media_pause);
        if (topRemaining > 0 && bottomRemaining > 0) {
            topRunning = topRemaining < bottomRemaining;
            bottomRunning = !topRunning;
        } else if (topRemaining > 0) {
            topRunning = true; bottomRunning = false;
        } else if (bottomRemaining > 0) {
            bottomRunning = true; topRunning = false;
        }
        lastTickTime = System.currentTimeMillis();
        updatePanelColors();
    }

    private void resetClocks() {
        topRemaining = initialTimePerPlayer;
        bottomRemaining = initialTimePerPlayer;
        topRunning = false;
        bottomRunning = false;
        paused = true;
        topMoves = 0;
        bottomMoves = 0;
        if (btnPause != null) btnPause.setImageResource(android.R.drawable.ic_media_play);
        updatePanelColors();
        updateUI();
    }

    private void updateUI() {
        if (tvTopTime != null) tvTopTime.setText(formatTime(topRemaining));
        if (tvBottomTime != null) tvBottomTime.setText(formatTime(bottomRemaining));
        if (tvTopMoves != null) tvTopMoves.setText("Moves: " + topMoves);
        if (tvBottomMoves != null) tvBottomMoves.setText("Moves: " + bottomMoves);
    }

    private String formatTime(long millis) {
        if (millis <= 0) return "00:00";
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void updatePanelColors() {
//        final int activeBg = Color.parseColor("#6CC04A");
//        final int inactiveBg = Color.parseColor("#E6E6E6");
        final int activeBg = Color.parseColor("#1976D2");
        final int inactiveBg = Color.parseColor("#0A0A0A");
        final int activeText = Color.WHITE;
        final int inactiveText = Color.parseColor("#3A3A3C");
        final int timeoutText = Color.parseColor("#FF5C5C");

        if (panelTop != null) panelTop.setBackgroundColor(inactiveBg);
        if (panelBottom != null) panelBottom.setBackgroundColor(inactiveBg);

        if (tvTopTime != null) tvTopTime.setTextColor(inactiveText);
        if (tvBottomTime != null) tvBottomTime.setTextColor(inactiveText);
        if (tvTopName != null) tvTopName.setTextColor(inactiveText);
        if (tvBottomName != null) tvBottomName.setTextColor(inactiveText);
        if (tvTopMoves != null) tvTopMoves.setTextColor(inactiveText);
        if (tvBottomMoves != null) tvBottomMoves.setTextColor(inactiveText);
        if (tvTopPreset != null) tvTopPreset.setTextColor(inactiveText);
        if (tvBottomPreset != null) tvBottomPreset.setTextColor(inactiveText);

        int adjustVisible = paused ? View.VISIBLE : View.GONE;
        if (btnTopAdjust != null) btnTopAdjust.setVisibility(adjustVisible);
        if (btnBottomAdjust != null) btnBottomAdjust.setVisibility(adjustVisible);
        if (tvTopPreset != null) tvTopPreset.setVisibility(adjustVisible);
        if (tvBottomPreset != null) tvBottomPreset.setVisibility(adjustVisible);

        if (topRunning) {
            if (panelTop != null) panelTop.setBackgroundColor(activeBg);
            if (tvTopTime != null) tvTopTime.setTextColor(activeText);
            if (tvTopName != null) tvTopName.setTextColor(activeText);
            if (tvTopMoves != null) tvTopMoves.setTextColor(activeText);
            if (tvTopPreset != null) tvTopPreset.setTextColor(activeText);

            if (panelBottom != null) panelBottom.setBackgroundColor(inactiveBg);
            if (tvBottomTime != null) tvBottomTime.setTextColor(inactiveText);
            if (tvBottomName != null) tvBottomName.setTextColor(inactiveText);
            if (tvBottomMoves != null) tvBottomMoves.setTextColor(inactiveText);
            if (tvBottomPreset != null) tvBottomPreset.setTextColor(inactiveText);

        } else if (bottomRunning) {
            if (panelBottom != null) panelBottom.setBackgroundColor(activeBg);
            if (tvBottomTime != null) tvBottomTime.setTextColor(activeText);
            if (tvBottomName != null) tvBottomName.setTextColor(activeText);
            if (tvBottomMoves != null) tvBottomMoves.setTextColor(activeText);
            if (tvBottomPreset != null) tvBottomPreset.setTextColor(activeText);

            if (panelTop != null) panelTop.setBackgroundColor(inactiveBg);
            if (tvTopTime != null) tvTopTime.setTextColor(inactiveText);
            if (tvTopName != null) tvTopName.setTextColor(inactiveText);
            if (tvTopMoves != null) tvTopMoves.setTextColor(inactiveText);
            if (tvTopPreset != null) tvTopPreset.setTextColor(inactiveText);
        }

        if (topRemaining == 0 && tvTopTime != null) {
            tvTopTime.setTextColor(timeoutText);
            if (panelTop != null) panelTop.setAlpha(0.75f);
        } else if (panelTop != null) panelTop.setAlpha(1f);

        if (bottomRemaining == 0 && tvBottomTime != null) {
            tvBottomTime.setTextColor(timeoutText);
            if (panelBottom != null) panelBottom.setAlpha(0.75f);
        } else if (panelBottom != null) panelBottom.setAlpha(1f);
    }

    // ---- CUSTOM TIME dialog (updates BOTH preset time and increment) ----
    private void showCustomTimeDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext()); // CHANGE: Use requireContext()
        View v = inflater.inflate(R.layout.dialog_custom_time, null);

        // expected IDs in dialog_custom_time.xml:
        final EditText etTimeMin = v.findViewById(R.id.etTimeMin);
        final EditText etTimeSec = v.findViewById(R.id.etTimeSec);
        final EditText etIncMin = v.findViewById(R.id.etIncMin);
        final EditText etIncSec = v.findViewById(R.id.etIncSec);
        final Button btnCancel = v.findViewById(R.id.btnCustomCancelCustom);
        final Button btnSave = v.findViewById(R.id.btnCustomSaveCustom);

        // Prefill current preset values
        int curMin = (int) (initialTimePerPlayer / 1000 / 60);
        int curSec = (int) ((initialTimePerPlayer / 1000) % 60);
        int incMin = (int) (incrementPerMove / 1000 / 60);
        int incSec = (int) ((incrementPerMove / 1000) % 60);

        if (etTimeMin != null) etTimeMin.setText(String.format(Locale.getDefault(), "%02d", Math.min(curMin, 999)));
        if (etTimeSec != null) etTimeSec.setText(String.format(Locale.getDefault(), "%02d", curSec));
        if (etIncMin != null) etIncMin.setText(String.format(Locale.getDefault(), "%02d", Math.min(incMin, 999)));
        if (etIncSec != null) etIncSec.setText(String.format(Locale.getDefault(), "%02d", incSec));

        // keep edits white and numeric
        setupEditTextForDialog(etTimeMin);
        setupEditTextForDialog(etTimeSec);
        setupEditTextForDialog(etIncMin);
        setupEditTextForDialog(etIncSec);

        // Use a theme that is essentially invisible or transparent
        final AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogDarkTheme).setView(v).create(); // CHANGE: Use requireContext()

        if (btnCancel != null) btnCancel.setOnClickListener(x -> {
            if (etTimeMin != null) closeKeyboard(etTimeMin);
            dialog.dismiss();
        });

        if (btnSave != null) btnSave.setOnClickListener(x -> {
            int m = parseIntSafe(etTimeMin != null ? etTimeMin.getText().toString() : "0");
            int s = parseIntSafe(etTimeSec != null ? etTimeSec.getText().toString() : "0");
            int im = parseIntSafe(etIncMin != null ? etIncMin.getText().toString() : "0");
            int is = parseIntSafe(etIncSec != null ? etIncSec.getText().toString() : "0");

            if (s > 59) s = 59;
            if (is > 59) is = 59;
            if (m < 0) m = 0;
            if (im < 0) im = 0;

            long newMs = (m * 60L + s) * 1000L;
            long newIncMs = (im * 60L + is) * 1000L;

            if (newMs <= 0) {
                Toast.makeText(requireContext(), "Please set a non-zero time", Toast.LENGTH_SHORT).show(); // CHANGE: Use requireContext()
                return;
            }

            // **Important**: custom time updates both the preset base and increment (global)
            initialTimePerPlayer = newMs;
            incrementPerMove = newIncMs;

            // apply to both remaining clocks
            resetClocks(); // sets both remaining to initialTimePerPlayer and paused state
            updatePresetTexts();
            updateUI();

            if (etTimeMin != null) closeKeyboard(etTimeMin);
            dialog.dismiss();
        });

        dialog.setCancelable(true);
        dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels ),
                WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void setupEditTextForDialog(EditText et) {
        if (et == null) return;
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.WHITE);
        et.setSelectAllOnFocus(true);
        et.setCursorVisible(true);
        et.setOnFocusChangeListener((v, hasFocus) -> ((EditText)v).setTextColor(Color.WHITE));
    }

    private void closeKeyboard(View v) {
        if (v == null) return;
        // CHANGE: Use requireContext()
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void showAdjustTimeDialog(boolean isTop) {
        LayoutInflater inflater = LayoutInflater.from(requireContext()); // CHANGE: Use requireContext()
        View v = inflater.inflate(R.layout.dialog_adjust_time, null);

        final EditText etMinute = v.findViewById(R.id.npMinute);
        final EditText etSecond = v.findViewById(R.id.npSecond);
        final Button btnCancel = v.findViewById(R.id.btnCancelAdjust);
        final Button btnSave = v.findViewById(R.id.btnSaveAdjust);

        if (etMinute == null || etSecond == null) {
            Toast.makeText(requireContext(), "Adjust dialog missing time fields", Toast.LENGTH_SHORT).show(); // CHANGE: Use requireContext()
            return;
        }

        // Prefill current time for the selected player
        long currentTime = isTop ? topRemaining : bottomRemaining;
        int curMin = (int) (currentTime / 1000 / 60);
        int curSec = (int) ((currentTime / 1000) % 60);

        etMinute.setText(String.format(Locale.getDefault(), "%02d", curMin));
        etSecond.setText(String.format(Locale.getDefault(), "%02d", curSec));

        setupEditTextForDialog(etMinute);
        setupEditTextForDialog(etSecond);

        final AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(v).create(); // CHANGE: Use requireContext()

        btnCancel.setOnClickListener(x -> dialog.dismiss());

        btnSave.setOnClickListener(x -> {
            int m = parseIntSafe(etMinute.getText().toString());
            int s = parseIntSafe(etSecond.getText().toString());
            if (s > 59) s = 59;
            if (m < 0) m = 0;

            long newMs = (m * 60L + s) * 1000L;
            if (newMs <= 0) {
                Toast.makeText(requireContext(), "Please set a non-zero time", Toast.LENGTH_SHORT).show(); // CHANGE: Use requireContext()
                return;
            }

            if (isTop) {
                topRemaining = newMs;
                if (tvTopPreset != null)
                    tvTopPreset.setText(formatPresetText(newMs, incrementPerMove));
            } else {
                bottomRemaining = newMs;
                if (tvBottomPreset != null)
                    tvBottomPreset.setText(formatPresetText(newMs, incrementPerMove));
            }

            updateUI();
            dialog.dismiss();
        });

        dialog.setCancelable(true);
        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * .95),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
    }

    private void styleNumberPicker(NumberPicker np, int color, float sizeSp) {

        if (np == null) return;
        final int count = np.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = np.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                tv.setTextColor(color);
                tv.setTextSize(sizeSp);
            }
        }
//        try {
//            java.lang.reflect.Field selectorWheelPaintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
//            selectorWheelPaintField.setAccessible(true);
//            android.graphics.Paint paint = (android.graphics.Paint) selectorWheelPaintField.get(np);
//            if (paint != null) paint.setColor(color);
//            np.invalidate();
//        } catch (Exception ignored) {}
    }

    private void updatePresetTexts() {
        String preset = formatPresetText(initialTimePerPlayer, incrementPerMove);
        if (tvTopPreset != null) tvTopPreset.setText(preset);
        if (tvBottomPreset != null) tvBottomPreset.setText(preset);
    }

    private String formatPresetText(long initialMs, long incMs) {
        int min = (int) (initialMs / 1000 / 60);
        int sec = (int) ((initialMs / 1000) % 60);

        String base;
        if (min > 0 && sec > 0) base = String.format(Locale.getDefault(), "%d min %d sec", min, sec);
        else if (min > 0) base = String.format(Locale.getDefault(), "%d min", min);
        else base = String.format(Locale.getDefault(), "%d sec", sec);

        int incMin = (int) (incMs / 1000 / 60);
        int incSec = (int) ((incMs / 1000) % 60);

        String incPart = String.format(Locale.getDefault(), "%d min", incMin);
        if (incSec > 0) incPart = incPart + " " + String.format(Locale.getDefault(), "%d sec", incSec);

        return base + " | " + incPart;
    }

    private int parseIntSafe(String s) {
        if (s == null) return 0;
        s = s.trim();
        if (s.length() == 0) return 0;
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(tickRunnable);
        rootView = null;
    }
}