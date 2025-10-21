package com.example.clockandtimerapp.alarm;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.clockandtimerapp.R;
import com.example.clockandtimerapp.alarm.utils.DayOfWeekHelper;
import com.example.clockandtimerapp.alarm.model.Alarm;
import com.google.android.material.card.MaterialCardView; // IMPORT ADDED

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditAlarmActivity extends AppCompatActivity {

    private TimePicker timePicker;
    private Spinner spDay;
    private EditText etLabel;
    private Switch swVibrate;
    // TYPE CORRECTED: Changed from LinearLayout to MaterialCardView
    private MaterialCardView rowPickRingtone;
    private TextView tvPickTone;

    private int hour24 = 6;
    private int minute = 30;
    private String pickedRingtone = null;

    private int editId = -1;
    private boolean isEditMode = false;

    private final ActivityResultLauncher<Intent> ringtonePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), res -> {
                if (res.getResultCode() == RESULT_OK && res.getData() != null) {
                    Uri uri = res.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        pickedRingtone = uri.toString();
                        updateRingtoneTitle();
                    }
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_ClockDark);
        setContentView(R.layout.activity_edit_alarm);

        // --- View Initialization ---
        ImageButton btnClose = findViewById(R.id.btnClose);
        ImageButton btnSave  = findViewById(R.id.btnSave);

        // CORRECTION: Assign TimePicker
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(false);

        // Removed tvHour, tvMinute, spAmPm declarations as they are obsolete
        // with the TimePicker widget used in the new XML.

        etLabel  = findViewById(R.id.etLabel);
        spDay    = findViewById(R.id.spDay);
        swVibrate = findViewById(R.id.swVibrate);

        // CORRECTION: Find the MaterialCardView by its ID
        rowPickRingtone = findViewById(R.id.rowPickRingtone); // THIS LINE IS NOW SAFE

        tvPickTone = findViewById(R.id.tvPickTone);

        // --- Day Adapter Setup (Dynamic) ---
        List<String> displayDays = DayOfWeekHelper.getRelativeDayList();

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                displayDays
        );
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDay.setAdapter(dayAdapter);

        // --- Event Handlers ---
        rowPickRingtone.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
            if (pickedRingtone != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(pickedRingtone));
            }
            ringtonePicker.launch(intent);
        });

        btnClose.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveAndFinish());

        // --- Pre-fill Logic (Edit Mode or New Alarm) ---
        Intent in = getIntent();
        if (in != null && "edit".equalsIgnoreCase(in.getStringExtra("mode"))) {
            isEditMode = true;
            editId = in.getIntExtra("editId", -1);

            int h = in.getIntExtra("hour24", hour24);
            int m = in.getIntExtra("minute", minute);
            String label = in.getStringExtra("label");
            boolean vibrate = in.getBooleanExtra("vibrate", false);
            String ringtone = in.getStringExtra("ringtone");
            String dayString = in.getStringExtra("day");

            // Update internal state and TimePicker
            hour24 = h;
            minute = m;
            timePicker.setHour(h);
            timePicker.setMinute(m);

            if (label != null) etLabel.setText(label);
            swVibrate.setChecked(vibrate);
            if (ringtone != null) pickedRingtone = ringtone;
            updateRingtoneTitle();

            // Set Day Selector
            if (dayString != null) {
                int pos = findDayPosition(dayString, displayDays);
                if (pos >= 0) spDay.setSelection(pos);
            }
        } else {
            // New alarm: Set current time
            Calendar c = Calendar.getInstance();
            hour24 = c.get(Calendar.HOUR_OF_DAY);
            minute = c.get(Calendar.MINUTE);

            timePicker.setHour(hour24);
            timePicker.setMinute(minute);

            swVibrate.setChecked(false);
            updateRingtoneTitle();
        }
    }

    private void saveAndFinish() {
        // Retrieve final time from TimePicker
        hour24 = timePicker.getHour();
        minute = timePicker.getMinute();

        Intent data = new Intent();
        data.putExtra("hour24", hour24);
        data.putExtra("minute", minute);

        String label = etLabel.getText().toString().trim();
        if (label.isEmpty()) label = "Alarm";
        data.putExtra("label", label);

        // Extract the original day name from the selected item string
        String selectedItem = spDay.getSelectedItem().toString();
        String simpleDayName = extractSimpleDayName(selectedItem);
        data.putExtra("day", simpleDayName);

        data.putExtra("ringtone", pickedRingtone);
        data.putExtra("vibrate", swVibrate.isChecked());

        if (isEditMode && editId != -1) {
            data.putExtra("editId", editId);
        }
        setResult(RESULT_OK, data);
        finish();
    }

    private int findDayPosition(String targetSimpleDay, List<String> displayDays){
        if (targetSimpleDay == null) return -1;

        for (int i = 0; i < displayDays.size(); i++) {
            String fullDayName = extractSimpleDayName(displayDays.get(i));
            if (targetSimpleDay.equalsIgnoreCase(fullDayName)) return i;
        }
        return -1;
    }

    private String extractSimpleDayName(String complexDayString) {
        int start = complexDayString.indexOf('(');
        int end = complexDayString.indexOf(')');
        if (start != -1 && end != -1) {
            // Found "(DayName)": return "DayName"
            return complexDayString.substring(start + 1, end).trim();
        } else {
            // Not a complex string: return as is (should be a simple day name)
            return complexDayString;
        }
    }

    private void updateRingtoneTitle() {
        if (tvPickTone == null) return;
        try {
            if (pickedRingtone == null || pickedRingtone.isEmpty()) {
                tvPickTone.setText("Pick Ringtone");
                return;
            }
            Uri uri = Uri.parse(pickedRingtone);
            Ringtone r = RingtoneManager.getRingtone(this, uri);
            String title = (r != null) ? r.getTitle(this) : null;
            tvPickTone.setText((title == null || title.trim().isEmpty()) ? "Pick Ringtone" : title);
        } catch (Exception e) {
            tvPickTone.setText("Pick Ringtone");
        }
    }
}