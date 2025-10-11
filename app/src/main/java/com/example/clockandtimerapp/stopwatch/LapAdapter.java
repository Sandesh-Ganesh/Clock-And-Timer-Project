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

public class LapAdapter extends RecyclerView.Adapter<LapAdapter.VH> {

    private final List<Lap> data = new ArrayList<>();

    public void submit(List<Lap> newData) {
        data.clear();
        data.addAll(newData);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lap, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        int labelNumber = data.size() - position;
        Lap lap = data.get(position);
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
            tvLapIndex = itemView.findViewById(R.id.tvLapIndex);
            tvLapTime = itemView.findViewById(R.id.tvLapTime);
        }
    }

    private static String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long hundredths = (ms % 1000) / 10;
        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths);
    }
}
