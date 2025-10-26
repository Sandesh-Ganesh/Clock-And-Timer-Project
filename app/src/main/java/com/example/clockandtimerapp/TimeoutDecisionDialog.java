package com.example.clockandtimerapp;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable; // ADDED
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class TimeoutDecisionDialog extends DialogFragment {

    public interface TimeoutDecisionListener {
        void onTimeoutDecision(boolean isTeamA);
    }

    private TimeoutDecisionListener listener;
    private final String teamAName;
    private final String teamBName;
    private final int teamATimeoutsLeft;
    private final int teamBTimeoutsLeft;

    // State variable to track the selection
    private Boolean isTeamASelected = null;

    // UI Elements
    private MaterialButton rbTeamA, rbTeamB;
    private TextView tvTimeoutA, tvTimeoutB;
    private MaterialButton btnStartTimeout;
    private TextView btnCancel;

    public TimeoutDecisionDialog(String teamAName, String teamBName, int teamATimeoutsLeft, int teamBTimeoutsLeft) {
        this.teamAName = teamAName;
        this.teamBName = teamBName;
        this.teamATimeoutsLeft = teamATimeoutsLeft;
        this.teamBTimeoutsLeft = teamBTimeoutsLeft;
    }

    public void setTimeoutDecisionListener(TimeoutDecisionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_timeout_decision, null);

        // Map UI elements
        rbTeamA = view.findViewById(R.id.rb_team_a);
        rbTeamB = view.findViewById(R.id.rb_team_b);
        tvTimeoutA = view.findViewById(R.id.tv_timeout_a_count);
        tvTimeoutB = view.findViewById(R.id.tv_timeout_b_count);
        btnStartTimeout = view.findViewById(R.id.btn_start_timeout);
        btnCancel = view.findViewById(R.id.btn_cancel_pause);

        // Dynamic Text Setup
        rbTeamA.setText(String.format(Locale.getDefault(), "%s Timeout", teamAName));
        rbTeamB.setText(String.format(Locale.getDefault(), "%s Timeout", teamBName));
        tvTimeoutA.setText(String.format(Locale.getDefault(), "Available: %d", teamATimeoutsLeft));
        tvTimeoutB.setText(String.format(Locale.getDefault(), "Available: %d", teamBTimeoutsLeft));

        // Color variables for styling
        int colorDisabled = ContextCompat.getColor(requireContext(), R.color.icon_disabled_tint);
        int colorPrimaryBlue = ContextCompat.getColor(requireContext(), R.color.primary_blue);
        int colorWhite = ContextCompat.getColor(requireContext(), R.color.white);

        // --- Setup Initial/Unselected Styles ---

        // Unselected style: Transparent background, Blue text/stroke
        ColorStateList primaryBlueTint = ColorStateList.valueOf(colorPrimaryBlue);
        ColorStateList transparentTint = ColorStateList.valueOf(Color.TRANSPARENT);

        rbTeamA.setBackgroundTintList(transparentTint);
        rbTeamA.setTextColor(colorPrimaryBlue);
        rbTeamA.setStrokeColor(primaryBlueTint);

        rbTeamB.setBackgroundTintList(transparentTint);
        rbTeamB.setTextColor(colorPrimaryBlue);
        rbTeamB.setStrokeColor(primaryBlueTint);

        // Handle Team A availability
        if (teamATimeoutsLeft <= 0) {
            rbTeamA.setEnabled(false);
            rbTeamA.setStrokeColor(ColorStateList.valueOf(colorDisabled));
            rbTeamA.setTextColor(colorDisabled);
            tvTimeoutA.setTextColor(colorDisabled);
        }

        // Handle Team B availability
        if (teamBTimeoutsLeft <= 0) {
            rbTeamB.setEnabled(false);
            rbTeamB.setStrokeColor(ColorStateList.valueOf(colorDisabled));
            rbTeamB.setTextColor(colorDisabled);
            tvTimeoutB.setTextColor(colorDisabled);
        }

        // Start Timeout button is disabled until a valid choice is made
        btnStartTimeout.setEnabled(false);

        // Selection Logic for button-based selection
        View.OnClickListener selectionListener = v -> {
            boolean clickedTeamA = v.getId() == R.id.rb_team_a;

            // 1. Reset styles for both buttons (Unselected state)
            rbTeamA.setBackgroundTintList(transparentTint);
            rbTeamA.setTextColor(colorPrimaryBlue);
            rbTeamB.setBackgroundTintList(transparentTint);
            rbTeamB.setTextColor(colorPrimaryBlue);

            // 2. Style the selected button (Selected state: White background, Blue text)
            MaterialButton selectedButton = clickedTeamA ? rbTeamA : rbTeamB;

            selectedButton.setBackgroundTintList(ColorStateList.valueOf(colorWhite));
            selectedButton.setTextColor(colorPrimaryBlue);

            // 3. Update the tracked state
            isTeamASelected = clickedTeamA;

            // 4. Enable Start Timeout button if a valid selection was made
            boolean hasTimeouts = (clickedTeamA && teamATimeoutsLeft > 0) || (!clickedTeamA && teamBTimeoutsLeft > 0);
            btnStartTimeout.setEnabled(hasTimeouts);
        };

        rbTeamA.setOnClickListener(selectionListener);
        rbTeamB.setOnClickListener(selectionListener);

        // Start Timeout Action
        btnStartTimeout.setOnClickListener(v -> {
            // Determine selection using the tracked state variable
            if (isTeamASelected == null) {
                Toast.makeText(requireContext(), "Please select a team.", Toast.LENGTH_SHORT).show();
            } else if (isTeamASelected) {
                if (teamATimeoutsLeft > 0 && listener != null) {
                    listener.onTimeoutDecision(true);
                    dismiss();
                }
            } else { // isTeamBSelected
                if (teamBTimeoutsLeft > 0 && listener != null) {
                    listener.onTimeoutDecision(false);
                    dismiss();
                }
            }
        });

        // Cancel Action
        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view).setTitle(null);

        setCancelable(false);

        // Final creation of the dialog
        AlertDialog dialog = builder.create();

        // CRITICAL FIX: Robust Centering Logic
        Window window = dialog.getWindow();
        if (window != null) {
            // 1. Remove default background/padding inherited from theme
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 2. Set the dimensions and gravity
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }

        return dialog;
    }
}