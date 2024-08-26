package com.thiago.calendarvertical.sample;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.thiago.calendarvertical.CalendarCellView;
import com.thiago.calendarvertical.DayViewAdapter;

import you.thiago.calendarvert.sample.R;

public class SampleDayViewAdapter implements DayViewAdapter {
  @Override public void makeCellView(CalendarCellView parent) {
    LayoutInflater inflater = LayoutInflater.from(parent.getContext());
    View layout = inflater.inflate(R.layout.day_view_custom, parent, false);
    parent.addView(layout);
    parent.setDayOfMonthTextView((TextView) layout.findViewById(R.id.day_view));
  }
}
