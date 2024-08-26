package com.thiago.calendarvertical.sample;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import com.thiago.calendarvertical.CalendarCellDecorator;
import com.thiago.calendarvertical.CalendarCellView;

import java.util.Date;

public class SampleDecorator implements CalendarCellDecorator {
  @Override
  public void decorate(CalendarCellView cellView, Date date) {
    String dateString = Integer.toString(date.getDate());
    SpannableString string = new SpannableString(dateString + "\ntitle");
    string.setSpan(new RelativeSizeSpan(0.5f), 0, dateString.length(),
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    cellView.getDayOfMonthTextView().setText(string);
  }
}
