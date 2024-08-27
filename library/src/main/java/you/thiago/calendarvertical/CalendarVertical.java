package you.thiago.calendarvertical;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import you.thiago.calendarvert.R;

public class CalendarVertical extends LinearLayout {

    private final CalendarGridView grid;
    private final CalendarPickerView calendar;
    
    public CalendarVertical(Context context, AttributeSet attrs) {
        super(context, attrs);

        grid = findViewById(R.id.calendar_grid);
        
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarPickerView);
        
        boolean displayDayNamesHeaderRow =
                a.getBoolean(R.styleable.CalendarPickerView_calendarvert_displayDayNamesHeaderRow, true);
        
        boolean displayDayNamesAsCalendarHeader =
                a.getBoolean(R.styleable.CalendarPickerView_calendarvert_displayDayNamesAsCalendarHeader, false);
        
        
        if (!displayDayNamesHeaderRow && displayDayNamesAsCalendarHeader) {
            SimpleDateFormat weekdayNameFormat = new SimpleDateFormat(context.getString(R.string.day_name_format), Locale.getDefault());
            List<String> weekDaysNames = new ArrayList<>();

            for (int offset = 0; offset < 7; offset++) {
                weekDaysNames.add(weekdayNameFormat.format(Calendar.getInstance().getTime()));
            }

            setWeekDaysNames(weekDaysNames);
        } else {
            grid.setVisibility(View.GONE);
        }

        calendar = new CalendarPickerView(context, attrs);

        addView(calendar);
    }

    /**
     * Both date parameters must be non-null and their {@link Date#getTime()} must not return 0. Time
     * of day will be ignored.  For instance, if you pass in {@code minDate} as 11/16/2012 5:15pm and
     * {@code maxDate} as 11/16/2013 4:30am, 11/16/2012 will be the first selectable date and
     * 11/15/2013 will be the last selectable date ({@code maxDate} is exclusive).
     * <p>
     * This will implicitly set the {@link CalendarPickerView.SelectionMode} to {@link CalendarPickerView.SelectionMode#SINGLE}.  If you
     * want a different selection mode, use {@link CalendarPickerView.FluentInitializer#inMode(CalendarPickerView.SelectionMode)} on the
     * {@link CalendarPickerView.FluentInitializer} this method returns.
     * <p>
     * The calendar will be constructed using the default locale as returned by
     * {@link java.util.Locale#getDefault()} and default time zone as returned by
     * {@link java.util.TimeZone#getDefault()}. If you wish the calendar to be constructed using a
     * different locale or time zone, use
     * <p>
     * {@link CalendarPickerView#init(java.util.Date, java.util.Date, java.util.Locale)},
     * {@link CalendarPickerView#init(java.util.Date, java.util.Date, java.util.TimeZone)} or
     * {@link CalendarPickerView#init(java.util.Date, java.util.Date, java.util.TimeZone, java.util.Locale)}.
     * <p>
     * @param minDate Earliest selectable date, inclusive.  Must be earlier than {@code maxDate}.
     * @param maxDate Latest selectable date, exclusive.  Must be later than {@code minDate}.
     */
    public CalendarPickerView.FluentInitializer init(Date minDate, Date maxDate) {
        return calendar.init(minDate, maxDate);
    }

    public void setWeekDaysNames(List<String> weekDaysNames) {
        final CalendarRowView headerRow = (CalendarRowView) grid.getChildAt(0);

        if (weekDaysNames.size() != 7) {
            throw new IllegalArgumentException("Week days names must have 7 elements");
        }

        for (int i = 0; i < weekDaysNames.size(); i++) {
            final TextView textView = (TextView) headerRow.getChildAt(i);

            String dayName = weekDaysNames.get(i);

            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
            dayName = dayName.replace(".", "")
                             .replace(",", "")
                             .trim();

            textView.setText(dayName);
        }
    }
}
