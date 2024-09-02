package you.thiago.calendarvertical;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.core.content.ContextCompat;

import static androidx.core.text.TextUtilsCompat.getLayoutDirectionFromLocale;

public class CalendarVertical extends LinearLayout {

    private final CalendarPickerView calendar;
    private final CalendarRowView rowViewWeekDaysHeader;
    
    public CalendarVertical(Context context, AttributeSet attrs) {
        super(context, attrs);

        final ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.calendar_vertical, this, false);
        
        addView(view);
        
        rowViewWeekDaysHeader = view.findViewById(R.id.day_names_header_row);
        
        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarVertical)) {
            final int bg = a.getColor(R.styleable.CalendarPickerView_android_background, ContextCompat.getColor(context, R.color.calendar_bg));
            
            int dayBackgroundResId = a.getResourceId(R.styleable.CalendarPickerView_calendarpicker_dayBackground, R.drawable.calendar_bg_selector);
            int dayTextColorResId = a.getResourceId(R.styleable.CalendarPickerView_calendarpicker_dayTextColor, R.color.calendar_text_selector);
            int titleTextStyle = a.getResourceId(R.styleable.CalendarPickerView_calendarpicker_titleTextStyle, R.style.CalendarTitle);
            int headerTextColor = a.getColor(R.styleable.CalendarPickerView_calendarpicker_headerTextColor, ContextCompat.getColor(context, R.color.calendar_text_active));
            boolean displayHeader = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_displayHeader, true);
            boolean displayDayNamesHeaderRow = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_displayDayNamesHeaderRow, false);
            boolean displayDayNamesAsCalendarHeader = a.getBoolean(R.styleable.CalendarVertical_calendarvert_displayDayNamesAsCalendarHeader, true);
            boolean displayAlwaysDigitNumbers = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_displayAlwaysDigitNumbers, false);
            boolean autoInit = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_autoInit, true);
            int initialMode = a.getInt(R.styleable.CalendarVertical_calendarvert_mode, 0);
            int monthsTitleResId = a.getResourceId(R.styleable.CalendarVertical_calendarvert_months_title, 0);
            
            List<String> monthsTitle = new ArrayList<>();
            if (monthsTitleResId != 0) {
                Collections.addAll(monthsTitle, getResources().getStringArray(monthsTitleResId));
            }
            
            setupWeekDaysHeader(context, displayDayNamesAsCalendarHeader);

            calendar = new CalendarPickerView(
                    this,
                    context,
                    attrs,
                    bg,
                    dayBackgroundResId,
                    dayTextColorResId,
                    titleTextStyle,
                    displayHeader,
                    headerTextColor,
                    displayDayNamesHeaderRow,
                    displayDayNamesAsCalendarHeader,
                    displayAlwaysDigitNumbers,
                    autoInit,
                    initialMode,
                    monthsTitle
            );
    
            view.addView(calendar);
        }
    }

    /**
     * Return CalendarPickerView fluent initializer instance (builder)
     */
    public CalendarPickerView getInstance() {
        return calendar;
    }

    /**
     * Return CalendarPickerView fluent initializer instance (builder)
     */
    public CalendarPickerView.FluentInitializer build() {
        return calendar.build();
    }

    /**
     * Return selected range dates (start and end date)
     * @return SelectedRange
     */
    @NotNull
    public CalendarPickerView.SelectedRange getSelectedRange() {
        return calendar.getSelectedRange();
    }

    public void setOnDateSelectedListener(CalendarPickerView.OnDateSelectedListener listener) {
        calendar.setOnDateSelectedListener(listener);
    }

    public void setOnRangeSelectionListener(CalendarPickerView.OnRangeDateSelectedListener listener) {
        calendar.setOnRangeSelectionListener(listener);
    }
    
    public void setWeekDaysNames(List<String> weekDaysNames) {
        if (weekDaysNames.size() != 7) {
            throw new IllegalArgumentException("Week days names must have 7 elements");
        }

        for (int i = 0; i < weekDaysNames.size(); i++) {
            final TextView textView = (TextView) rowViewWeekDaysHeader.getChildAt(i);

            String dayName = weekDaysNames.get(i);

            dayName = dayName.substring(0, 1).toUpperCase() + dayName.substring(1);
            dayName = dayName.replace(".", "")
                             .replace(",", "")
                             .trim();

            textView.setText(dayName);
        }
    }

    private void setupWeekDaysHeader(Context context, boolean displayDayNamesAsCalendarHeader) {
        if (displayDayNamesAsCalendarHeader) {
            SimpleDateFormat weekdayNameFormat = new SimpleDateFormat(context.getString(R.string.day_name_format), Locale.getDefault());
            List<String> weekDaysNames = new ArrayList<>();

            Calendar today = Calendar.getInstance();
            today.setTime(new Date());

            int firstDayOfWeek = today.getFirstDayOfWeek();
            boolean isRtl = getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;

            for (int offset = 0; offset < 7; offset++) {
                today.set(Calendar.DAY_OF_WEEK, getDayOfWeek(firstDayOfWeek, offset, isRtl));
                weekDaysNames.add(weekdayNameFormat.format(today.getTime()));
            }

            setWeekDaysNames(weekDaysNames);
        } else {
            rowViewWeekDaysHeader.setVisibility(View.GONE);
        }
    }
    
    private int getDayOfWeek(int firstDayOfWeek, int offset, boolean isRtl) {
        int dayOfWeek = firstDayOfWeek + offset;

        if (isRtl) {
            return 8 - dayOfWeek;
        }

        return dayOfWeek;
    }
}
