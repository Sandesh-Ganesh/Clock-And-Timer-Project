package com.example.clockandtimerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.card.MaterialCardView;

// Import the new ChessTimerFragment
import com.example.clockandtimerapp.chess.ChessTimerFragment;

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

        // Chess Timer Click (UPDATED)
        cardChess.setOnClickListener(v -> {
            // Navigate to Chess Timer Fragment
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new ChessTimerFragment());
            transaction.addToBackStack("Chess Timer"); // Adds the Chess screen to the back stack
            transaction.commit();
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MenuProvider menuProvider = new MenuProvider() {

            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear(); // Clear existing menu items
                menuInflater.inflate(R.menu.toolbar_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                // Return false to let the Activity handle the menu item click (e.g., Settings)
                return false;
            }
        };

        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner());
    }
}