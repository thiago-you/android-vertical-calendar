package you.thiago.calendarvertical.sample;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import you.thiago.calendarvertical.CalendarCellDecorator;
import you.thiago.calendarvertical.CalendarCellView;

import java.util.Calendar;
import java.util.Date;

public class SampleDecorator implements CalendarCellDecorator {
  @Override
  public void decorate(CalendarCellView cellView, Date date) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    
    String dateString = Integer.toString(cal.get(Calendar.DATE));
    SpannableString string = new SpannableString(dateString + "\ntitle");
    string.setSpan(new RelativeSizeSpan(0.5f), 0, dateString.length(),
        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
    cellView.getDayOfMonthTextView().setText(string);
  }
}
