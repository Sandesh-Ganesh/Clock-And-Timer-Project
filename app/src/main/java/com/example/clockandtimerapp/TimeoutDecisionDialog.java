package com.example.clockandtimerapp;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

        // Initial States for buttons (Outlined/Unselected Look)
        rbTeamA.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        rbTeamA.setTextColor(colorPrimaryBlue);
        rbTeamA.setStrokeColor(ColorStateList.valueOf(colorPrimaryBlue));

        rbTeamB.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
        rbTeamB.setTextColor(colorPrimaryBlue);
        rbTeamB.setStrokeColor(ColorStateList.valueOf(colorPrimaryBlue));


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

        // Selection Logic for button-based selection
        View.OnClickListener selectionListener = v -> {
            boolean isTeamASelected = v.getId() == R.id.rb_team_a;

            // Reset styles for both buttons
            rbTeamA.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            rbTeamA.setTextColor(colorPrimaryBlue);
            rbTeamB.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            rbTeamB.setTextColor(colorPrimaryBlue);

            // Style the selected button (Solid Primary Blue background)
            MaterialButton selectedButton = isTeamASelected ? rbTeamA : rbTeamB;

            selectedButton.setBackgroundTintList(ColorStateList.valueOf(colorPrimaryBlue));
            selectedButton.setTextColor(colorWhite);

            // Enable Start Timeout button if a valid selection was made
            boolean hasTimeouts = (isTeamASelected && teamATimeoutsLeft > 0) || (!isTeamASelected && teamBTimeoutsLeft > 0);
            btnStartTimeout.setEnabled(hasTimeouts);
        };

        rbTeamA.setOnClickListener(selectionListener);
        rbTeamB.setOnClickListener(selectionListener);

        // Start Timeout Action
        btnStartTimeout.setOnClickListener(v -> {
            // Determine selection by checking which button has a solid background tint
            ColorStateList tintA = rbTeamA.getBackgroundTintList();
            boolean isTeamASelected = tintA != null && tintA.getDefaultColor() == colorPrimaryBlue;

            ColorStateList tintB = rbTeamB.getBackgroundTintList();
            boolean isTeamBSelected = tintB != null && tintB.getDefaultColor() == colorPrimaryBlue;


            if (listener != null) {
                if (isTeamASelected) {
                    listener.onTimeoutDecision(true);
                    dismiss();
                } else if (isTeamBSelected) {
                    listener.onTimeoutDecision(false);
                    dismiss();
                } else {
                    Toast.makeText(requireContext(), "Please select a team.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Cancel Action
        btnCancel.setOnClickListener(v -> dismiss());

        builder.setView(view).setTitle(null);

        setCancelable(false);

        return builder.create();
    }
}