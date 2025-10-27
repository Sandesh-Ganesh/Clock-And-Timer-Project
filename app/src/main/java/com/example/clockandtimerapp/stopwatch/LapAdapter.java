package com.example.clockandtimerapp.stopwatch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockandtimerapp.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LapAdapter extends RecyclerView.Adapter<LapAdapter.VH> {

    // Adapter now holds Lap objects
    final List<Lap> data = new ArrayList<>();

    public LapAdapter(List<Lap> initialData) {
        if (initialData != null) {
            this.data.addAll(initialData);
        }
    }

    public void submit(List<Lap> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lap, parent, false); // Assuming R.layout.item_lap exists
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        // Since laps are stored newest-first, we calculate the label index from the end
        int labelNumber = data.size() - position;
        Lap lap = data.get(position);

        // The lap time is calculated by the Lap object itself
        holder.tvLapIndex.setText("Lap " + labelNumber);
        holder.tvLapTime.setText(formatTime(lap.getLapMs(position, data)));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLapIndex, tvLapTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvLapIndex = itemView.findViewById(R.id.tvLapIndex); // Assuming R.id.tvLapIndex exists
            tvLapTime = itemView.findViewById(R.id.tvLapTime);   // Assuming R.id.tvLapTime exists
        }
    }

    static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, hundredths);
    }
}