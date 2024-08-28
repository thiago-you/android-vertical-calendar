package you.thiago.calendarvertical;

import static androidx.core.text.TextUtilsCompat.getLayoutDirectionFromLocale;
import static java.util.Calendar.MONTH;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import you.thiago.calendarvert.R;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("unused")
public class MonthView extends LinearLayout {

    TextView title;
    CalendarGridView grid;
    View dayNamesHeaderRowView;

    private Listener listener;
    private List<CalendarCellDecorator> decorators;
    private boolean isRtl;
    private Locale locale;
    private boolean alwaysDigitNumbers;

    public static MonthView create(
            ViewGroup parent, LayoutInflater inflater,
            DateFormat weekdayNameFormat, Listener listener, Calendar today,
            int dayBackgroundResId, int dayTextColorResId, int titleTextStyle, boolean displayHeader,
            int headerTextColor, boolean showDayNamesHeaderRowView, boolean displayDayNamesAsCalendarHeader,
            Locale locale, boolean showAlwaysDigitNumbers, DayViewAdapter adapter
    ) {
        return create(parent, inflater, weekdayNameFormat, listener, today,
                      dayBackgroundResId, dayTextColorResId, titleTextStyle, displayHeader, headerTextColor,
                      showDayNamesHeaderRowView, displayDayNamesAsCalendarHeader, 
                      showAlwaysDigitNumbers, null, locale, adapter);
    }

    public static MonthView create(
            ViewGroup parent, LayoutInflater inflater,
            DateFormat weekdayNameFormat, Listener listener, Calendar today,
            int dayBackgroundResId, int dayTextColorResId, int titleTextStyle, boolean displayHeader,
            int headerTextColor, boolean displayDayNamesHeaderRowView, boolean displayDayNamesAsCalendarHeader,
            boolean showAlwaysDigitNumbers, List<CalendarCellDecorator> decorators,
            Locale locale, DayViewAdapter adapter
    ) {
        final MonthView view = (MonthView) inflater.inflate(R.layout.month, parent, false);

        // Set the views
        view.title = new TextView(new ContextThemeWrapper(view.getContext(), titleTextStyle));
        view.grid = view.findViewById(R.id.calendar_grid);
        view.dayNamesHeaderRowView = view.findViewById(R.id.day_names_header_row);

        // Add the month title as the first child of MonthView
        view.addView(view.title, 0);

        view.setDayViewAdapter(adapter);
        view.setDayTextColor(dayTextColorResId);
        view.setDayTextColor(dayTextColorResId);
        view.setDisplayHeader(displayHeader);
        view.setHeaderTextColor(headerTextColor);

        if (dayBackgroundResId != 0) {
            view.setDayBackground(dayBackgroundResId);
        }

        view.isRtl = getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL;
        view.locale = locale;
        view.alwaysDigitNumbers = showAlwaysDigitNumbers;
        int firstDayOfWeek = today.getFirstDayOfWeek();

        if (displayDayNamesHeaderRowView && !displayDayNamesAsCalendarHeader) {
            List<String> weekDaysNames = new ArrayList<>();
            final int originalDayOfWeek = today.get(Calendar.DAY_OF_WEEK);

            for (int offset = 0; offset < 7; offset++) {
                today.set(Calendar.DAY_OF_WEEK, getDayOfWeek(firstDayOfWeek, offset, view.isRtl));
                weekDaysNames.add(weekdayNameFormat.format(today.getTime()));
            }
            
            view.setWeekDaysNames(weekDaysNames);
            
            today.set(Calendar.DAY_OF_WEEK, originalDayOfWeek);
        } else {
            view.dayNamesHeaderRowView.setVisibility(View.GONE);
        }

        view.listener = listener;
        view.decorators = decorators;

        return view;
    }

    private static int getDayOfWeek(int firstDayOfWeek, int offset, boolean isRtl) {
        int dayOfWeek = firstDayOfWeek + offset;
        if (isRtl) {
            return 8 - dayOfWeek;
        }
        return dayOfWeek;
    }

    public MonthView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDecorators(List<CalendarCellDecorator> decorators) {
        this.decorators = decorators;
    }

    public List<CalendarCellDecorator> getDecorators() {
        return decorators;
    }

    public void init(
            MonthDescriptor month, List<List<MonthCellDescriptor>> cells,
            boolean displayOnly, Typeface titleTypeface, Typeface dateTypeface, List<String> monthTitles
    ) {
        updateMonthTitle(month, monthTitles);

        NumberFormat numberFormatter;
        if (alwaysDigitNumbers) {
            numberFormatter = NumberFormat.getInstance(Locale.US);
        } else {
            numberFormatter = NumberFormat.getInstance(locale);
        }

        final int numRows = cells.size();
        grid.setNumRows(numRows);

        for (int i = 0; i < 6; i++) {
            CalendarRowView weekRow = (CalendarRowView) grid.getChildAt(i + 1);
            weekRow.setListener(listener);

            if (i < numRows) {
                configCalendarRows(cells, displayOnly, weekRow, i, numberFormatter);
            } else {
                weekRow.setVisibility(GONE);
            }
        }

        if (titleTypeface != null) {
            title.setTypeface(titleTypeface);
        }
        if (dateTypeface != null) {
            grid.setTypeface(dateTypeface);
        }
    }

    public void updateMonthTitle(MonthDescriptor month, List<String> monthTitles) {
        if (monthTitles != null && !monthTitles.isEmpty() && monthTitles.size() >= month.getMonth()) {
            String monthTitle = monthTitles.get(month.getMonth());

            if (monthTitle != null && !monthTitle.trim().isEmpty()) {
                if (monthTitle.contains("%s")) {
                    title.setText(String.format(monthTitle, month.getYear()));
                } else {
                    title.setText(monthTitle);
                }
            }

            return;
        }

        String monthLabel = month.getLabel();
        monthLabel = monthLabel.substring(0, 1).toUpperCase() + monthLabel.substring(1);

        title.setText(monthLabel);
    }

    private void configCalendarRows(
            List<List<MonthCellDescriptor>> cells,
            boolean displayOnly,
            CalendarRowView weekRow,
            int i,
            NumberFormat numberFormatter
    ) {
        weekRow.setVisibility(VISIBLE);
        List<MonthCellDescriptor> week = cells.get(i);

        for (int c = 0; c < week.size(); c++) {
            MonthCellDescriptor cell = week.get(isRtl ? 6 - c : c);
            CalendarCellView cellView = (CalendarCellView) weekRow.getChildAt(c);

            configCalendarRowCellView(displayOnly, numberFormatter, cell, cellView);

            if (decorators != null) {
                for (CalendarCellDecorator decorator : decorators) {
                    decorator.decorate(cellView, cell.getDate());
                }
            }
        }
    }

    private void configCalendarRowCellView(
            boolean displayOnly,
            NumberFormat numberFormatter,
            MonthCellDescriptor cell,
            CalendarCellView cellView
    ) {
        String cellDate = numberFormatter.format(cell.getValue());
        if (!cellView.getDayOfMonthTextView().getText().equals(cellDate)) {
            cellView.getDayOfMonthTextView().setText(cellDate);
        }

        cellView.setEnabled(cell.isCurrentMonth());
        cellView.setClickable(!displayOnly);

        cellView.setSelectable(cell.isSelectable());
        cellView.setCurrentMonth(cell.isCurrentMonth());
        cellView.setSelected(cell.isSelected());
        cellView.setToday(cell.isToday());
        cellView.setRangeState(cell.getRangeState());
        cellView.setHighlighted(cell.isHighlighted());
        cellView.setTag(cell);

        if (cell.isSelected() && cell.getRangeState() != RangeState.MIDDLE) {
            cellView.getDayOfMonthTextView()
                    .setTypeface(ResourcesCompat.getFont(getContext(), R.font.calendar_days_semibold));
        } else {
            cellView.getDayOfMonthTextView()
                    .setTypeface(ResourcesCompat.getFont(getContext(), R.font.calendar_days_medium));
        }
        
        if (!cell.isCurrentMonth()) {
            cellView.setVisibility(View.INVISIBLE);
        } else {
            cellView.setVisibility(View.VISIBLE);
        }
    }

    public void setDayBackground(int resId) {
        grid.setDayBackground(resId);
    }

    public void setDayTextColor(int resId) {
        grid.setDayTextColor(resId);
    }

    public void setDayTextFont(int resId) {
        grid.setDayTextFont(resId);
    }

    public void setDayViewAdapter(DayViewAdapter adapter) {
        grid.setDayViewAdapter(adapter);
    }

    public void setDisplayHeader(boolean displayHeader) {
        grid.setDisplayHeader(displayHeader);
    }

    public void setHeaderTextColor(int color) {
        grid.setHeaderTextColor(color);
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
    
    public interface Listener {

        void handleClick(MonthCellDescriptor cell);
    }
}
