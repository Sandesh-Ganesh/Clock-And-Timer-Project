package com.example.clockandtimerapp;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;

public class NextRaidDialog extends DialogFragment {

    public interface NextRaidListener {
        void onRaidDecision(boolean isTeamA, boolean isSameTeam);
    }

    private NextRaidListener listener;
    private final String teamAName, teamBName;
    private final boolean lastRaidingTeamWasA;
    private final String currentRaiderName;
    private final String opponentName;

    public NextRaidDialog(String teamAName, String teamBName, boolean lastRaidingTeamWasA) {
        this.teamAName = teamAName;
        this.teamBName = teamBName;
        this.lastRaidingTeamWasA = lastRaidingTeamWasA;

        currentRaiderName = lastRaidingTeamWasA ? teamAName : teamBName;
        opponentName = lastRaidingTeamWasA ? teamBName : teamAName;
    }

    public void setNextRaidListener(NextRaidListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Uses the custom card dialog theme (must be defined in your styles.xml)
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomCardDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the custom themed layout (dialog_next_raid.xml)
        View view = inflater.inflate(R.layout.dialog_next_raid, container, false);

        MaterialButton btnReraid = view.findViewById(R.id.btn_re_raid);
        MaterialButton btnOpponentRaid = view.findViewById(R.id.btn_opponent_raid);
        TextView btnCancelPause = view.findViewById(R.id.btn_cancel_pause);

        // 1. Set Button Text
        btnReraid.setText(currentRaiderName + " Reraids");
        btnOpponentRaid.setText(opponentName + " Raids");

        // 2. Set Click Listeners

        // Reraid (Same team starts a new raid/re-raid)
        btnReraid.setOnClickListener(v -> {
            if (listener != null) {
                // true = re-raid (new raid for the current designated team)
                listener.onRaidDecision(lastRaidingTeamWasA, true);
            }
            dismiss();
        });

        // Opponent Raids (Change of possession)
        btnOpponentRaid.setOnClickListener(v -> {
            if (listener != null) {
                // false = change of possession (new raid for the opponent)
                listener.onRaidDecision(!lastRaidingTeamWasA, false);
            }
            dismiss();
        });

        // Cancel Pause (Match remains paused)
        btnCancelPause.setOnClickListener(v -> dismiss());

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Ensure background is transparent for rounded card corners
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }
}