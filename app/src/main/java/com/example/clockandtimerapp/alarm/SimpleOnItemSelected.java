package com.example.clockandtimerapp.alarm;

import android.view.View;
import android.widget.AdapterView;

public class SimpleOnItemSelected implements AdapterView.OnItemSelectedListener {
    public interface OnSelected { void run(int position); }
    private final OnSelected cb;
    public SimpleOnItemSelected(OnSelected cb){ this.cb = cb; }
    @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){ cb.run(pos); }
    @Override public void onNothingSelected(AdapterView<?> parent) {}
}
