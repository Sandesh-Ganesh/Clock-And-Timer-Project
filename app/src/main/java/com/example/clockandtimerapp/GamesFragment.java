package com.example.clockandtimerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.card.MaterialCardView;

public class GamesFragment extends Fragment {

    private MaterialCardView cardKabaddi, cardChess;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_games, container, false);

        cardKabaddi = view.findViewById(R.id.card_kabaddi);
        cardChess = view.findViewById(R.id.card_chess);

        // Kabaddi Timer Click
        cardKabaddi.setOnClickListener(v -> {
            // Navigate to Kabaddi Setup Fragment
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new KabaddiSetupFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Chess Timer Click (Placeholder)
        cardChess.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chess Timer - Coming Soon!", Toast.LENGTH_SHORT).show();
        });

        return view;
    }
}
