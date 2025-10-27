package com.example.clockandtimerapp.worldclock;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clockandtimerapp.R;
import com.example.clockandtimerapp.utils.TimeFormatPreference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WorldClockAdapter extends RecyclerView.Adapter<WorldClockAdapter.ClockViewHolder> {

    private final Context context;
    private final List<TimezoneInfo> clockList;
    private final OnDeleteClickListener deleteListener;

    private final Handler updateHandler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getItemCount() > 0) {
                notifyDataSetChanged();
            }
            updateHandler.postDelayed(this, 60000);
        }
    };

    public interface OnDeleteClickListener {
        void onDeleteClicked(int position);
    }

    public WorldClockAdapter(Context context, List<TimezoneInfo> clockList, OnDeleteClickListener deleteListener) {
        this.context = context;
        this.clockList = clockList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ClockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_world_clock, parent, false);
        return new ClockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClockViewHolder holder, int position) {
        TimezoneInfo info = clockList.get(position);

        holder.textCityName.setText(info.getCityName());

        String timePattern = TimeFormatPreference.getHourFormatPattern(context);
        SimpleDateFormat timeFormat = new SimpleDateFormat(timePattern + " a", Locale.getDefault());
        TimeZone cityTimeZone = TimeZone.getTimeZone(info.getTimezoneId());
        timeFormat.setTimeZone(cityTimeZone);
        holder.textCityTime.setText(timeFormat.format(new Date()));

        String difference = formatTimeDifference(info.getDifferenceMinutes());
        holder.textTimeDifference.setText(difference);

        // FIX: Use getAdapterPosition() to get the correct, current position
        holder.iconDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                deleteListener.onDeleteClicked(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return clockList.size();
    }

    public void startUpdates() {
        updateHandler.post(updateRunnable);
    }

    public void stopUpdates() {
        updateHandler.removeCallbacks(updateRunnable);
    }

    private String formatTimeDifference(long differenceMins) {
        if (differenceMins == 0) {
            return "Same time as India Standard Time";
        }

        long hours = Math.abs(differenceMins / 60);
        long minutes = Math.abs(differenceMins % 60);

        String sign = differenceMins > 0 ? "ahead" : "behind";
        StringBuilder timePart = new StringBuilder();

        if (hours > 0) {
            timePart.append(hours).append(" hr");
        }
        if (minutes > 0) {
            if (timePart.length() > 0) {
                timePart.append(" ");
            }
            timePart.append(minutes).append(" min");
        }

        String dayText = "Today";
        if (differenceMins > 720) {
            dayText = "Tomorrow";
        } else if (differenceMins < -720) {
            dayText = "Yesterday";
        }

        return String.format(Locale.getDefault(), "%s, %s %s", dayText, timePart.toString(), sign);
    }

    static class ClockViewHolder extends RecyclerView.ViewHolder {
        TextView textCityName;
        TextView textTimeDifference;
        TextView textCityTime;
        ImageView iconDelete;

        public ClockViewHolder(@NonNull View itemView) {
            super(itemView);
            textCityName = itemView.findViewById(R.id.text_city_name);
            textTimeDifference = itemView.findViewById(R.id.text_time_difference);
            textCityTime = itemView.findViewById(R.id.text_city_time);
            iconDelete = itemView.findViewById(R.id.icon_delete);
        }
    }
}