package com.example.clockandtimerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class KabaddiSetupFragment extends Fragment {

    private TextInputEditText etTeamA, etTeamB, etMatchTime, etRaidTime;
    private RadioGroup rgFirstTeam;
    private RadioButton rbTeamA;
    private MaterialButton btnStartMatch;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_kabaddi_setup, container, false);

        // Initialize views
        etTeamA = view.findViewById(R.id.et_team_a);
        etTeamB = view.findViewById(R.id.et_team_b);
        etMatchTime = view.findViewById(R.id.et_match_time);
        etRaidTime = view.findViewById(R.id.et_raid_time);
        rgFirstTeam = view.findViewById(R.id.rg_first_team);
        rbTeamA = view.findViewById(R.id.rb_team_a);
        btnStartMatch = view.findViewById(R.id.btn_start_match);

        btnStartMatch.setOnClickListener(v -> startMatch());

        return view;
    }

    private void startMatch() {
        // Get input values
        String teamAName = etTeamA.getText().toString().trim();
        String teamBName = etTeamB.getText().toString().trim();
        String matchTimeStr = etMatchTime.getText().toString().trim();
        String raidTimeStr = etRaidTime.getText().toString().trim();

        // Validation
        if (teamAName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter Team A name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (teamBName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter Team B name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (matchTimeStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter match time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (raidTimeStr.isEmpty()) {
            Toast.makeText(getContext(), "Please enter raid time", Toast.LENGTH_SHORT).show();
            return;
        }

        int matchTime = Integer.parseInt(matchTimeStr);
        int raidTime = Integer.parseInt(raidTimeStr);

        if (matchTime <= 0) {
            Toast.makeText(getContext(), "Match time must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }
        if (raidTime <= 0) {
            Toast.makeText(getContext(), "Raid time must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get first team to raid
        boolean isTeamAFirst = rbTeamA.isChecked();

        // Create bundle with data
        Bundle bundle = new Bundle();
        bundle.putString("teamA", teamAName);
        bundle.putString("teamB", teamBName);
        bundle.putInt("matchTime", matchTime);
        bundle.putInt("raidTime", raidTime);
        bundle.putBoolean("isTeamAFirst", isTeamAFirst);

        // Navigate to Main Timer Fragment
        KabaddiMainFragment mainFragment = new KabaddiMainFragment();
        mainFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, mainFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}