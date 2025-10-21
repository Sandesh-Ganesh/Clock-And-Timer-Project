package com.example.clockandtimerapp.worldclock;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class AddClockAdapter extends RecyclerView.Adapter<AddClockAdapter.CityViewHolder> {

    private final Context context;
    private List<TimezoneInfo> clockList;
    private final OnCitySelectListener selectListener;

    // Handler to update the city times in the search list every minute
    private final Handler updateHandler = new Handler();
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getItemCount() > 0) {
                notifyDataSetChanged();
            }
            updateHandler.postDelayed(this, 60000); // Update every 1 minute
        }
    };

    public interface OnCitySelectListener {
        void onCitySelected(TimezoneInfo selectedInfo);
    }

    public AddClockAdapter(Context context, List<TimezoneInfo> clockList, OnCitySelectListener selectListener) {
        this.context = context;
        this.clockList = clockList;
        this.selectListener = selectListener;
        updateHandler.post(updateRunnable); // Start updates immediately
    }

    public void updateList(List<TimezoneInfo> newList) {
        this.clockList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Uses the same layout card style as item_world_clock but with a different internal structure
        View view = LayoutInflater.from(context).inflate(R.layout.item_add_clock_city, parent, false);
        return new CityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CityViewHolder holder, int position) {
        TimezoneInfo info = clockList.get(position);

        holder.textCountryName.setText(info.getCountryName());
        holder.textCityName.setText(info.getCityName());

        // Calculate and Set Time (uses current time and saved format preference)
        String timePattern = TimeFormatPreference.getHourFormatPattern(context);
        SimpleDateFormat timeFormat = new SimpleDateFormat(timePattern + " a", Locale.getDefault());
        TimeZone cityTimeZone = TimeZone.getTimeZone(info.getTimezoneId());
        timeFormat.setTimeZone(cityTimeZone);
        holder.textCityTime.setText(timeFormat.format(new Date()));

        // Set click listener to select the city
        holder.itemView.setOnClickListener(v -> selectListener.onCitySelected(info));
    }

    @Override
    public int getItemCount() {
        return clockList.size();
    }

    // Stop updates when fragment is destroyed
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        updateHandler.removeCallbacks(updateRunnable);
        super.onDetachedFromRecyclerView(recyclerView);
    }

    static class CityViewHolder extends RecyclerView.ViewHolder {
        TextView textCountryName;
        TextView textCityName;
        TextView textCityTime;

        public CityViewHolder(@NonNull View itemView) {
            super(itemView);
            textCountryName = itemView.findViewById(R.id.text_country_name);
            textCityName = itemView.findViewById(R.id.text_city_name);
            textCityTime = itemView.findViewById(R.id.text_city_time);
        }
    }
}