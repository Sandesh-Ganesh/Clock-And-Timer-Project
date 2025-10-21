package com.example.clockandtimerapp.alarm.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

// CORRECTED R IMPORT: References the main application's resource file
import com.example.clockandtimerapp.R;
// CORRECTED MODEL IMPORT: References the Alarm class from its new location
import com.example.clockandtimerapp.alarm.model.Alarm;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.text.SimpleDateFormat;

public class AlarmAdapter extends ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder> {

    public interface Callbacks {
        void onToggle(int position, boolean on);
        void onDelete(int position);
        void onEdit(int position);
    }

    private final Callbacks callbacks;

    public AlarmAdapter(Callbacks callbacks) {
        super(DIFF_CALLBACK);
        this.callbacks = callbacks;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // R.layout.item_alarm is now correctly resolved
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = getItem(position);
        holder.bind(alarm, position);
    }

    public void submit(List<Alarm> list) {
        super.submitList(list);
    }

    public class AlarmViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTime;
        private final TextView tvLabel;
        private final TextView tvDay;
        private final MaterialSwitch switchToggle;
        private final ImageButton btnDelete;
        // NOTE: btnEdit is removed/handled by itemView click since it's not a separate ID in item_alarm.xml

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);

            // CORRECTED ID MAPPING based on item_alarm.xml:
            tvTime = itemView.findViewById(R.id.tvTime);           // Matches XML ID: tvTime
            tvLabel = itemView.findViewById(R.id.tvLabel);         // Matches XML ID: tvLabel
            tvDay = itemView.findViewById(R.id.tvDay);             // Matches XML ID: tvDay
            switchToggle = itemView.findViewById(R.id.switchOn);   // Matches XML ID: switchOn
            btnDelete = itemView.findViewById(R.id.btnDelete);     // Matches XML ID: btnDelete
        }

        public void bind(Alarm alarm, int position) {
            Context context = itemView.getContext();

            // 1. FIX: Display AM/PM (Time Formatting)
            try {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, alarm.hour24);
                c.set(Calendar.MINUTE, alarm.minute);

                // Format to "hh:mm a" (e.g., "04:30 AM")
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                String timeFormatted = sdf.format(c.getTime());
                tvTime.setText(timeFormatted);
            } catch (Exception e) {
                // Fallback in case of formatting error
                tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d", alarm.hour24, alarm.minute));
            }

            // Label
            tvLabel.setText(alarm.label);

            // Day of Week
            tvDay.setText(mapCalendarToDayString(alarm.dayOfWeek));

            // 2. FIX: Prevent infinite loop and Schedule immediately if enabled

            // Crucial: Remove listener before setting state to prevent infinite loop
            switchToggle.setOnCheckedChangeListener(null);
            switchToggle.setChecked(alarm.enabled);

            // Logic: If the alarm is enabled but was just created/edited, we rely on
            // the fragment's ActivityResultLauncher to handle the *initial* scheduling.
            // However, if the toggle is clicked *by the user*, we update the state and schedule.

            // Set Toggle Listener
            switchToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // This block runs ONLY when the user clicks the switch
                if (callbacks != null) {
                    callbacks.onToggle(position, isChecked);
                    String status = isChecked ? "Alarm ON" : "Alarm OFF";
                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
                }
            });

            // Delete Button Listener
            btnDelete.setOnClickListener(v -> {
                if (callbacks != null) {
                    callbacks.onDelete(position);
                    Toast.makeText(context, "Alarm Deleted", Toast.LENGTH_SHORT).show();
                }
            });

            // Edit Listener: The entire item view handles the edit action
            itemView.setOnClickListener(v -> {
                if (callbacks != null) {
                    callbacks.onEdit(position);
                }
            });
        }
    }

    private static final DiffUtil.ItemCallback<Alarm> DIFF_CALLBACK = new DiffUtil.ItemCallback<Alarm>() {
        @Override
        public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {

            boolean primitivesSame = oldItem.hour24 == newItem.hour24 &&
                    oldItem.minute == newItem.minute &&
                    oldItem.enabled == newItem.enabled &&
                    oldItem.vibrate == newItem.vibrate &&
                    oldItem.dayOfWeek == newItem.dayOfWeek;

            // Check String fields using null-safe comparison
            // FIX: Use Objects.equals for null-safe comparison
            boolean labelSame = Objects.equals(oldItem.label, newItem.label);
            boolean ringtoneSame = Objects.equals(oldItem.ringtone, newItem.ringtone);

            return primitivesSame && labelSame && ringtoneSame;
        }
    };

    private String mapCalendarToDayString(int dayOfWeek){
        switch (dayOfWeek){
            case Calendar.SUNDAY: return "Sunday";
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            default: return "Daily";
        }
    }
}