package com.example.clockandtimerapp.worldclock;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.clockandtimerapp.R;

public class DeleteConfirmationDialog extends DialogFragment {

    private final String cityName;
    private final Runnable onDeleteConfirmed;

    public DeleteConfirmationDialog(String cityName, Runnable onDeleteConfirmed) {
        this.cityName = cityName;
        this.onDeleteConfirmed = onDeleteConfirmed;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Style to show the dialog full screen or without title bar
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.CustomDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the custom layout for the dialog
        View view = inflater.inflate(R.layout.dialog_delete_clock, container, false);

        TextView title = view.findViewById(R.id.dialog_title);
        TextView message = view.findViewById(R.id.dialog_message);
        Button btnNo = view.findViewById(R.id.button_no);
        Button btnYes = view.findViewById(R.id.button_yes);

        title.setText(getString(R.string.delete_clock_title));
        // Use the string resource with a placeholder for the city name
        message.setText(getString(R.string.delete_clock_message, cityName));

        btnNo.setOnClickListener(v -> dismiss());

        btnYes.setOnClickListener(v -> {
            if (onDeleteConfirmed != null) {
                onDeleteConfirmed.run();
            }
            dismiss();
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // Make background transparent to see the corner radius set in the XML layout
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }
}