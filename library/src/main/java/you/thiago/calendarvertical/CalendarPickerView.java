package you.thiago.calendarvertical;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import static java.util.Calendar.DATE;
import static java.util.Calendar.DAY_OF_MONTH;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.HOUR_OF_DAY;
import static java.util.Calendar.MILLISECOND;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.MONTH;
import static java.util.Calendar.SECOND;
import static java.util.Calendar.YEAR;

/**
 * Android component to allow picking a date from a calendar view (a list of months).  Must be
 * initialized after inflation with {@link #init(Date, Date)} and can be customized with any of the
 * {@link FluentInitializer} methods returned.  The currently selected date can be retrieved with
 * {@link #getSelectedDate()}.
 */
@SuppressWarnings("unused")
public class CalendarPickerView extends ListView {

    public enum SelectionMode {
        /**
         * Allows you to select a date range.  Previous selections are cleared when you either:
         * <ul>
         * <li>Have a range selected and select another date (even if it's in the current range).</li>
         * <li>Have one date selected and then select an earlier date.</li>
         * </ul>
         */
        RANGE,
        /**
         * Only one date will be selectable.  If there is already a selected date and you select a new
         * one, the old date will be unselected.
         */
        SINGLE,
        /** Multiple dates will be selectable.  Selecting an already-selected date will un-select it. */
        MULTIPLE
    }

    // List of languages that require manually creation of YYYY MMMM date format
    private static final ArrayList<String> explicitlyNumericYearLocaleLanguages =
            new ArrayList<>(Arrays.asList("ar", "my"));

    private CalendarVertical calendarVertical = null;
    
    private final CalendarPickerView.MonthAdapter adapter;
    private final IndexedLinkedHashMap<String, List<List<MonthCellDescriptor>>> cells =
            new IndexedLinkedHashMap<>();
    final MonthView.Listener listener = new CellClickedListener();
    final List<MonthDescriptor> months = new ArrayList<>();
    final List<MonthCellDescriptor> selectedCells = new ArrayList<>();
    final List<MonthCellDescriptor> highlightedCells = new ArrayList<>();
    final List<Calendar> selectedCals = new ArrayList<>();
    final List<Calendar> highlightedCals = new ArrayList<>();
    private Locale locale;
    private TimeZone timeZone;
    private DateFormat weekdayNameFormat;
    private DateFormat fullDateFormat;
    private Calendar minCal;
    private Calendar maxCal;
    private Calendar monthCounter;
    private boolean displayOnly;
    SelectionMode selectionMode;
    Calendar today;
    private int dayBackgroundResId;
    private int dayTextColorResId;
    private int titleTextStyle;
    private boolean displayHeader;
    private int headerTextColor;
    private boolean displayDayNamesHeaderRow;
    private boolean displayDayNamesAsCalendarHeader;
    private boolean displayAlwaysDigitNumbers;
    private boolean autoInit;
    private int initialMode;
    private Typeface titleTypeface;
    private Typeface dateTypeface;
    private List<String> monthsTitle;

    private OnDateSelectedListener dateListener;
    private OnRangeDateSelectedListener rangeDateListener;
    private DateSelectableFilter dateConfiguredListener;
    private OnInvalidDateSelectedListener invalidDateListener =
            new DefaultOnInvalidDateSelectedListener();
    private CellClickInterceptor cellClickInterceptor;
    private List<CalendarCellDecorator> decorators;
    private DayViewAdapter dayViewAdapter = new DefaultDayViewAdapter();

    private boolean monthsReverseOrder;
    private boolean isSelectingNext = false;

    private final StringBuilder monthBuilder = new StringBuilder(50);
    private Formatter monthFormatter;

    private FluentInitializer builderInstance;
    
    public void setDecorators(List<CalendarCellDecorator> decorators) {
        this.decorators = decorators;
        if (null != adapter) {
            adapter.notifyDataSetChanged();
        }
    }

    public List<CalendarCellDecorator> getDecorators() {
        return decorators;
    }

    public CalendarPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        try (TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CalendarPickerView)) {
            final int bg = a.getColor(R.styleable.CalendarPickerView_android_background, ContextCompat.getColor(context, R.color.calendar_bg));
            
            dayBackgroundResId = a.getResourceId(R.styleable.CalendarPickerView_calendarpicker_dayBackground, R.drawable.calendar_bg_selector);
            dayTextColorResId = a.getResourceId(R.styleable.CalendarPickerView_calendarpicker_dayTextColor, R.color.calendar_text_selector);
            titleTextStyle = a.getResourceId(R.styleable.CalendarPickerView_calendarpicker_titleTextStyle, R.style.CalendarTitle);
            headerTextColor = a.getColor(R.styleable.CalendarPickerView_calendarpicker_headerTextColor, ContextCompat.getColor(context, R.color.calendar_text_active));
            displayHeader = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_displayHeader, true);
            displayDayNamesHeaderRow = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_displayDayNamesHeaderRow, true);
            displayDayNamesAsCalendarHeader = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_displayDayNamesAsCalendarHeader, false);
            displayAlwaysDigitNumbers = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_displayAlwaysDigitNumbers, false);
            autoInit = a.getBoolean(R.styleable.CalendarPickerView_calendarpicker_autoInit, false);
            initialMode = a.getInt(R.styleable.CalendarPickerView_calendarpicker_mode, 0);

            int monthsTitleResId = a.getResourceId(R.styleable.CalendarVertical_calendarvert_months_title, 0);

            monthsTitle = new ArrayList<>();

            if (monthsTitleResId != 0) {
                Collections.addAll(monthsTitle, getResources().getStringArray(monthsTitleResId));
            }

            adapter = new MonthAdapter();
        
            setupView(context, bg);
        }
    }

    public CalendarPickerView(
            CalendarVertical calendarVertical, Context context, AttributeSet attrs, int bg, int dayBackgroundResId,
            int dayTextColorResId, int titleTextStyle, boolean displayHeader, int headerTextColor,
            boolean displayDayNamesHeaderRow, boolean displayDayNamesAsCalendarHeader,
            boolean displayAlwaysDigitNumbers, boolean autoInit, int initialMode, List<String> monthsTitle
    ) {
        super(context, attrs);
        
        this.calendarVertical = calendarVertical;
        this.dayBackgroundResId = dayBackgroundResId;
        this.dayTextColorResId = dayTextColorResId;
        this.titleTextStyle = titleTextStyle;
        this.displayHeader = displayHeader;
        this.headerTextColor = headerTextColor;
        this.displayDayNamesHeaderRow = displayDayNamesHeaderRow;
        this.displayDayNamesAsCalendarHeader = displayDayNamesAsCalendarHeader;
        this.displayAlwaysDigitNumbers = displayAlwaysDigitNumbers;
        this.autoInit = autoInit;
        this.initialMode = initialMode;
        this.monthsTitle = monthsTitle;

        adapter = new MonthAdapter();

        setupView(context, bg);
    }

    public void setup(
            CalendarVertical calendarVertical, Context context, AttributeSet attrs, int bg, int dayBackgroundResId,
            int dayTextColorResId, int titleTextStyle, boolean displayHeader, int headerTextColor,
            boolean displayDayNamesHeaderRow, boolean displayDayNamesAsCalendarHeader,
            boolean displayAlwaysDigitNumbers, boolean autoInit, int initialMode, List<String> monthsTitle
    ) {
        this.calendarVertical = calendarVertical;
        this.dayBackgroundResId = dayBackgroundResId;
        this.dayTextColorResId = dayTextColorResId;
        this.titleTextStyle = titleTextStyle;
        this.displayHeader = displayHeader;
        this.headerTextColor = headerTextColor;
        this.displayDayNamesHeaderRow = displayDayNamesHeaderRow;
        this.displayDayNamesAsCalendarHeader = displayDayNamesAsCalendarHeader;
        this.displayAlwaysDigitNumbers = displayAlwaysDigitNumbers;
        this.autoInit = autoInit;
        this.initialMode = initialMode;
        this.monthsTitle = monthsTitle;
        
        setupView(context, bg);
    }
    
    private void setupView(Context context, int bg) {
        setDivider(null);
        setDividerHeight(0);
        setBackgroundColor(bg);
        setCacheColorHint(bg);
        
        timeZone = TimeZone.getDefault();
        locale = Locale.getDefault();
        today = Calendar.getInstance(timeZone, locale);
        minCal = Calendar.getInstance(timeZone, locale);
        maxCal = Calendar.getInstance(timeZone, locale);
        monthCounter = Calendar.getInstance(timeZone, locale);
        weekdayNameFormat = new SimpleDateFormat(context.getString(R.string.day_name_format), locale);
        weekdayNameFormat.setTimeZone(timeZone);
        fullDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        fullDateFormat.setTimeZone(timeZone);

        if (autoInit) {
            if (monthsTitle != null && !monthsTitle.isEmpty()) {
                adapter.setMonthsTitle(monthsTitle);
            }
            
            Calendar nextYear = Calendar.getInstance(timeZone, locale);
            nextYear.add(Calendar.YEAR, 1);

            FluentInitializer initializer = init(new Date(), nextYear.getTime())
                    .inMode(SelectionMode.values()[initialMode]);
        }
    }

    /**
     * Return selected range dates (start and end date)
     * @return SelectedRange
     */
    @NotNull
    public SelectedRange getSelectedRange() {
        Date date1 = null;
        Date date2 = null;

        if (!selectedCals.isEmpty()) {
            date1 = selectedCals.get(0).getTime();
        }
        if (selectedCals.size() > 1) {
            date2 = selectedCals.get(1).getTime();
        }
        
        return new SelectedRange(date1, date2);
    }
    
    /**
     * Return CalendarPickerView fluent initializer instance (builder)
     */
    public FluentInitializer build() {
        if (builderInstance == null) {
            builderInstance = new FluentInitializer();
        }

        return builderInstance;
    }
    
    /**
     * Both date parameters must be non-null and their {@link Date#getTime()} must not return 0. Time
     * of day will be ignored.  For instance, if you pass in {@code minDate} as 11/16/2012 5:15pm and
     * {@code maxDate} as 11/16/2013 4:30am, 11/16/2012 will be the first selectable date and
     * 11/15/2013 will be the last selectable date ({@code maxDate} is exclusive).
     * <p>
     * This will implicitly set the {@link SelectionMode} to {@link SelectionMode#SINGLE}.  If you
     * want a different selection mode, use {@link FluentInitializer#inMode(SelectionMode)} on the
     * {@link FluentInitializer} this method returns.
     * <p>
     * The calendar will be constructed using the given time zone and the given locale. This means
     * that all dates will be in given time zone, all names (months, days) will be in the language
     * of the locale and the weeks start with the day specified by the locale.
     *
     * @param minDate Earliest selectable date, inclusive.  Must be earlier than {@code maxDate}.
     * @param maxDate Latest selectable date, exclusive.  Must be later than {@code minDate}.
     */
    public FluentInitializer init(Date minDate, Date maxDate, TimeZone timeZone, Locale locale) {
        if (minDate == null || maxDate == null) {
            throw new IllegalArgumentException(
                    "minDate and maxDate must be non-null.  " + dbg(minDate, maxDate));
        }
        if (minDate.after(maxDate)) {
            throw new IllegalArgumentException(
                    "minDate must be before maxDate.  " + dbg(minDate, maxDate));
        }
        if (locale == null) {
            throw new IllegalArgumentException("Locale is null.");
        }
        if (timeZone == null) {
            throw new IllegalArgumentException("Time zone is null.");
        }

        // Make sure that all calendar instances use the same time zone and locale.
        this.timeZone = timeZone;
        this.locale = locale;
        today = Calendar.getInstance(timeZone, locale);
        minCal = Calendar.getInstance(timeZone, locale);
        maxCal = Calendar.getInstance(timeZone, locale);
        monthCounter = Calendar.getInstance(timeZone, locale);
        for (MonthDescriptor month : months) {
            month.setLabel(formatMonthDate(month.getDate()));
        }
        weekdayNameFormat =
                new SimpleDateFormat(getContext().getString(R.string.day_name_format), locale);
        weekdayNameFormat.setTimeZone(timeZone);
        fullDateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        fullDateFormat.setTimeZone(timeZone);
        monthFormatter = new Formatter(monthBuilder, locale);

        this.selectionMode = SelectionMode.SINGLE;
        // Clear out any previously-selected dates/cells.
        selectedCals.clear();
        selectedCells.clear();
        highlightedCals.clear();
        highlightedCells.clear();

        // Clear previous state.
        cells.clear();
        months.clear();
        minCal.setTime(minDate);
        maxCal.setTime(maxDate);
        setMidnight(minCal);
        setMidnight(maxCal);
        displayOnly = false;

        // maxDate is exclusive: bump back to the previous day so if maxDate is the first of a month,
        // we don't accidentally include that month in the view.
        maxCal.add(MINUTE, -1);

        // Now iterate between minCal and maxCal and build up our list of months to show.
        monthCounter.setTime(minCal.getTime());
        
        final int maxMonth = maxCal.get(MONTH);
        final int maxYear = maxCal.get(YEAR);
        
        while ((monthCounter.get(MONTH) <= maxMonth // Up to, including the month.
                || monthCounter.get(YEAR) < maxYear) // Up to the year.
               && monthCounter.get(YEAR) < maxYear + 1) { // But not > next yr.
            
            Date date = monthCounter.getTime();
            
            MonthDescriptor month = new MonthDescriptor(monthCounter.get(MONTH), monthCounter.get(YEAR), date, formatMonthDate(date));
            cells.put(monthKey(month), getMonthCells(month, monthCounter));
            
            months.add(month);
            monthCounter.add(MONTH, 1);
        }

        validateAndUpdate();

        return build();
    }

    /**
     * Both date parameters must be non-null and their {@link Date#getTime()} must not return 0. Time
     * of day will be ignored.  For instance, if you pass in {@code minDate} as 11/16/2012 5:15pm and
     * {@code maxDate} as 11/16/2013 4:30am, 11/16/2012 will be the first selectable date and
     * 11/15/2013 will be the last selectable date ({@code maxDate} is exclusive).
     * <p>
     * This will implicitly set the {@link SelectionMode} to {@link SelectionMode#SINGLE}.  If you
     * want a different selection mode, use {@link FluentInitializer#inMode(SelectionMode)} on the
     * {@link FluentInitializer} this method returns.
     * <p>
     * The calendar will be constructed using the default locale as returned by
     * {@link java.util.Locale#getDefault()} and default time zone as returned by
     * {@link java.util.TimeZone#getDefault()}. If you wish the calendar to be constructed using a
     * different locale or time zone, use
     * {@link #init(java.util.Date, java.util.Date, java.util.Locale)},
     * {@link #init(java.util.Date, java.util.Date, java.util.TimeZone)} or
     * {@link #init(java.util.Date, java.util.Date, java.util.TimeZone, java.util.Locale)}.
     *
     * @param minDate Earliest selectable date, inclusive.  Must be earlier than {@code maxDate}.
     * @param maxDate Latest selectable date, exclusive.  Must be later than {@code minDate}.
     */
    public FluentInitializer init(Date minDate, Date maxDate) {
        return init(minDate, maxDate, TimeZone.getDefault(), Locale.getDefault());
    }

    /**
     * Both date parameters must be non-null and their {@link Date#getTime()} must not return 0. Time
     * of day will be ignored.  For instance, if you pass in {@code minDate} as 11/16/2012 5:15pm and
     * {@code maxDate} as 11/16/2013 4:30am, 11/16/2012 will be the first selectable date and
     * 11/15/2013 will be the last selectable date ({@code maxDate} is exclusive).
     * <p>
     * This will implicitly set the {@link SelectionMode} to {@link SelectionMode#SINGLE}.  If you
     * want a different selection mode, use {@link FluentInitializer#inMode(SelectionMode)} on the
     * {@link FluentInitializer} this method returns.
     * <p>
     * The calendar will be constructed using the given time zone and the default locale as returned
     * by {@link java.util.Locale#getDefault()}. This means that all dates will be in given time zone.
     * If you wish the calendar to be constructed using a different locale, use
     * {@link #init(java.util.Date, java.util.Date, java.util.Locale)} or
     * {@link #init(java.util.Date, java.util.Date, java.util.TimeZone, java.util.Locale)}.
     *
     * @param minDate Earliest selectable date, inclusive.  Must be earlier than {@code maxDate}.
     * @param maxDate Latest selectable date, exclusive.  Must be later than {@code minDate}.
     */
    public FluentInitializer init(Date minDate, Date maxDate, TimeZone timeZone) {
        return init(minDate, maxDate, timeZone, Locale.getDefault());
    }

    /**
     * Both date parameters must be non-null and their {@link Date#getTime()} must not return 0. Time
     * of day will be ignored.  For instance, if you pass in {@code minDate} as 11/16/2012 5:15pm and
     * {@code maxDate} as 11/16/2013 4:30am, 11/16/2012 will be the first selectable date and
     * 11/15/2013 will be the last selectable date ({@code maxDate} is exclusive).
     * <p>
     * This will implicitly set the {@link SelectionMode} to {@link SelectionMode#SINGLE}.  If you
     * want a different selection mode, use {@link FluentInitializer#inMode(SelectionMode)} on the
     * {@link FluentInitializer} this method returns.
     * <p>
     * The calendar will be constructed using the given locale. This means that all names
     * (months, days) will be in the language of the locale and the weeks start with the day
     * specified by the locale.
     * <p>
     * The calendar will be constructed using the given locale and the default time zone as returned
     * by {@link java.util.TimeZone#getDefault()}. This means that all names (months, days) will be
     * in the language of the locale and the weeks start with the day specified by the locale.
     * If you wish the calendar to be constructed using a different time zone, use
     * {@link #init(java.util.Date, java.util.Date, java.util.TimeZone)} or
     * {@link #init(java.util.Date, java.util.Date, java.util.TimeZone, java.util.Locale)}.
     *
     * @param minDate Earliest selectable date, inclusive.  Must be earlier than {@code maxDate}.
     * @param maxDate Latest selectable date, exclusive.  Must be later than {@code minDate}.
     */
    public FluentInitializer init(Date minDate, Date maxDate, Locale locale) {
        return init(minDate, maxDate, TimeZone.getDefault(), locale);
    }

    public class FluentInitializer {

        /** Override the {@link SelectionMode} from the default ({@link SelectionMode#SINGLE}). */
        public FluentInitializer inMode(SelectionMode mode) {
            selectionMode = mode;
            validateAndUpdate();
            return this;
        }

        /**
         * Set an initially-selected date.  The calendar will scroll to that date if it's not already
         * visible.
         */
        public FluentInitializer withSelectedDate(Date selectedDates) {
            return withSelectedDates(Collections.singletonList(selectedDates));
        }

        /**
         * Set an initially-selected date.  The calendar will scroll to that date if it's not already
         * visible.
         */
        public FluentInitializer withSelectedDate(Date selectedDates, boolean selectingNext) {
            isSelectingNext = selectingNext;
            return withSelectedDates(Collections.singletonList(selectedDates));
        }

        /**
         * Set an initially-selected date.  The calendar will scroll to that date if it's not already
         * visible.
         */
        public FluentInitializer setWeekDaysHeader(List<String> weekDays) {
            if (calendarVertical != null) {
                calendarVertical.setWeekDaysNames(weekDays);
            }

            return this;
        }

        public FluentInitializer setSelectionToLastRangeDate(boolean state) {
            isSelectingNext = state;
            return this;
        }
        
        /**
         * Set multiple selected dates.  This will throw an {@link IllegalArgumentException} if you
         * pass in multiple dates and haven't already called {@link #inMode(SelectionMode)}.
         */
        public FluentInitializer withSelectedDates(Collection<Date> selectedDates) {
            if (selectionMode == SelectionMode.SINGLE && selectedDates.size() > 1) {
                throw new IllegalArgumentException("SINGLE mode can't be used with multiple selectedDates");
            }
            if (selectionMode == SelectionMode.RANGE && selectedDates.size() > 2) {
                throw new IllegalArgumentException(
                        "RANGE mode only allows two selectedDates.  You tried to pass " + selectedDates.size());
            }
            if (selectedDates != null) {
                for (Date date : selectedDates) {
                    selectDate(date);
                }
            }
            scrollToSelectedDates();

            validateAndUpdate();
            return this;
        }

        public FluentInitializer withHighlightedDates(Collection<Date> dates) {
            highlightDates(dates);
            return this;
        }

        public FluentInitializer withHighlightedDate(Date date) {
            return withHighlightedDates(Collections.singletonList(date));
        }

        @SuppressLint("SimpleDateFormat")
        public FluentInitializer setShortWeekdays(String[] newShortWeekdays) {
            DateFormatSymbols symbols = new DateFormatSymbols(locale);
            symbols.setShortWeekdays(newShortWeekdays);
            weekdayNameFormat =
                    new SimpleDateFormat(getContext().getString(R.string.day_name_format), symbols);
            return this;
        }

        public FluentInitializer displayOnly() {
            displayOnly = true;
            return this;
        }

        public FluentInitializer withMonthsReverseOrder(boolean monthsRevOrder) {
            monthsReverseOrder = monthsRevOrder;
            return this;
        }

        public FluentInitializer withMonthsTitle(List<String> titles) {
            adapter.setMonthsTitle(titles);
            validateAndUpdate();
            return this;
        }
    }

    private void validateAndUpdate() {
        if (getAdapter() == null) {
            setAdapter(adapter);
        }
        adapter.notifyDataSetChanged();
    }

    private void scrollToSelectedMonth(final int selectedIndex) {
        scrollToSelectedMonth(selectedIndex, false);
    }

    private void scrollToSelectedMonth(final int selectedIndex, final boolean smoothScroll) {
        post(() -> {
            if (smoothScroll) {
                smoothScrollToPosition(selectedIndex);
            } else {
                setSelection(selectedIndex);
            }
        });
    }

    private void scrollToSelectedDates() {
        Integer selectedIndex = null;
        Integer todayIndex = null;
        Calendar today = Calendar.getInstance(timeZone, locale);

        for (int c = 0; c < months.size(); c++) {
            MonthDescriptor month = months.get(c);
            if (selectedIndex == null) {
                for (Calendar selectedCal : selectedCals) {
                    if (sameMonth(selectedCal, month)) {
                        selectedIndex = c;
                        break;
                    }
                }

                if (selectedIndex == null && todayIndex == null && sameMonth(today, month)) {
                    todayIndex = c;
                }
            }
        }

        if (selectedIndex != null) {
            scrollToSelectedMonth(selectedIndex);
        } else if (todayIndex != null) {
            scrollToSelectedMonth(todayIndex);
        }
    }

    public boolean scrollToDate(Date date) {
        Integer selectedIndex = null;

        Calendar cal = Calendar.getInstance(timeZone, locale);
        cal.setTime(date);

        for (int c = 0; c < months.size(); c++) {
            MonthDescriptor month = months.get(c);
            if (sameMonth(cal, month)) {
                selectedIndex = c;
                break;
            }
        }

        if (selectedIndex != null) {
            scrollToSelectedMonth(selectedIndex);
            return true;
        }

        return false;
    }

    /**
     * This method should only be called if the calendar is contained in a dialog, and it should only
     * be called once, right after the dialog is shown (using
     * {@link android.content.DialogInterface.OnShowListener} or
     * {@link android.app.DialogFragment#onStart()}).
     */
    public void fixDialogDimens() {
        // Fix the layout height/width after the dialog has been shown.
        getLayoutParams().height = getMeasuredHeight();
        getLayoutParams().width = getMeasuredWidth();

        // Post this runnable so it runs _after_ the dimen changes have been applied/re-measured.
        post(this::scrollToSelectedDates);
    }

    /**
     * Set the typeface to be used for month titles.
     */
    public void setTitleTypeface(Typeface titleTypeface) {
        this.titleTypeface = titleTypeface;
        validateAndUpdate();
    }

    /**
     * Sets the typeface to be used within the date grid.
     */
    public void setDateTypeface(Typeface dateTypeface) {
        this.dateTypeface = dateTypeface;
        validateAndUpdate();
    }

    /**
     * Sets the typeface to be used for all text within this calendar.
     */
    public void setTypeface(Typeface typeface) {
        setTitleTypeface(typeface);
        setDateTypeface(typeface);
    }

    /**
     * This method should only be called if the calendar is contained in a dialog, and it should only
     * be called when the screen has been rotated and the dialog should be re-measured.
     */
    public void unfixDialogDimens() {
        // Fix the layout height/width after the dialog has been shown.
        getLayoutParams().height = LayoutParams.MATCH_PARENT;
        getLayoutParams().width = LayoutParams.MATCH_PARENT;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (months.isEmpty()) {
            throw new IllegalStateException("Must have at least one month to display.  Did you forget to call init()?");
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public Date getSelectedDate() {
        return (selectedCals.size() > 0 ? selectedCals.get(0).getTime() : null);
    }

    public List<Date> getSelectedDates() {
        List<Date> selectedDates = new ArrayList<>();
        for (MonthCellDescriptor cal : selectedCells) {
            selectedDates.add(cal.getDate());
        }
        Collections.sort(selectedDates);
        return selectedDates;
    }

    /** Returns a string summarizing what the client sent us for init() params. */
    private static String dbg(Date minDate, Date maxDate) {
        return "minDate: " + minDate + "\nmaxDate: " + maxDate;
    }

    /** Clears out the hours/minutes/seconds/millis of a Calendar. */
    static void setMidnight(Calendar cal) {
        cal.set(HOUR_OF_DAY, 0);
        cal.set(MINUTE, 0);
        cal.set(SECOND, 0);
        cal.set(MILLISECOND, 0);
    }

    private class CellClickedListener implements MonthView.Listener {

        @Override
        public void handleClick(MonthCellDescriptor cell) {
            Date clickedDate = cell.getDate();

            if (cellClickInterceptor != null && cellClickInterceptor.onCellClicked(clickedDate)) {
                return;
            }
            if (!betweenDates(clickedDate, minCal, maxCal) || !isDateSelectable(clickedDate)) {
                if (invalidDateListener != null) {
                    invalidDateListener.onInvalidDateSelected(clickedDate);
                }
            } else {
                boolean wasSelected = doSelectDate(clickedDate, cell);

                if (dateListener != null) {
                    if (wasSelected) {
                        dateListener.onDateSelected(clickedDate);
                    } else {
                        dateListener.onDateUnselected(clickedDate);
                    }
                }
                if (rangeDateListener != null) {
                    Date date1 = null;
                    Date date2 = null;
                    
                    if (!selectedCals.isEmpty()) {
                        date1 = selectedCals.get(0).getTime();
                    }
                    if (selectedCals.size() > 1) {
                        date2 = selectedCals.get(1).getTime();
                    }
                    
                    rangeDateListener.onRangeSelected(date1, date2);
                }
            }
        }
    }

    /**
     * Select a new date.  Respects the {@link SelectionMode} this CalendarPickerView is configured
     * with: if you are in {@link SelectionMode#SINGLE}, the previously selected date will be
     * un-selected.  In {@link SelectionMode#MULTIPLE}, the new date will be added to the list of
     * selected dates.
     * <p>
     * If the selection was made (selectable date, in range), the view will scroll to the newly
     * selected date if it's not already visible.
     *
     * @return - whether we were able to set the date
     */
    public boolean selectDate(Date date) {
        return selectDate(date, false);
    }

    /**
     * Select a new date.  Respects the {@link SelectionMode} this CalendarPickerView is configured
     * with: if you are in {@link SelectionMode#SINGLE}, the previously selected date will be
     * un-selected.  In {@link SelectionMode#MULTIPLE}, the new date will be added to the list of
     * selected dates.
     * <p>
     * If the selection was made (selectable date, in range), the view will scroll to the newly
     * selected date if it's not already visible.
     *
     * @return - whether we were able to set the date
     */
    public boolean selectDate(Date date, boolean smoothScroll) {
        validateDate(date);

        MonthCellWithMonthIndex monthCellWithMonthIndex = getMonthCellWithIndexByDate(date);
        if (monthCellWithMonthIndex == null || !isDateSelectable(date)) {
            return false;
        }
        boolean wasSelected = doSelectDate(date, monthCellWithMonthIndex.cell);
        if (wasSelected) {
            scrollToSelectedMonth(monthCellWithMonthIndex.monthIndex, smoothScroll);
        }
        return wasSelected;
    }

    /**
     * Use {@link DateUtils} to format the dates.
     *
     * @see DateUtils
     */
    private String formatMonthDate(Date date) {
        int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                    | DateUtils.FORMAT_NO_MONTH_DAY;

        // Save default Locale
        Locale defaultLocale = Locale.getDefault();

        // Set new default Locale, the reason to do that is DateUtils.formatDateTime uses
        // internally this method DateIntervalFormat.formatDateRange to format the date. And this
        // method uses the default locale.
        //
        // More details about the methods:
        // - DateUtils.formatDateTime: https://goo.gl/3YW52Q
        // - DateIntervalFormat.formatDateRange: https://goo.gl/RRmfK7
        Locale.setDefault(locale);

        String dateFormatted;
        if (displayAlwaysDigitNumbers
            && explicitlyNumericYearLocaleLanguages.contains(locale.getLanguage())) {
            StringBuilder sb = new StringBuilder();
            SimpleDateFormat sdfMonth = new SimpleDateFormat(getContext()
                                                                     .getString(R.string.month_only_name_format),
                                                             locale);
            SimpleDateFormat sdfYear = new SimpleDateFormat(getContext()
                                                                    .getString(R.string.year_only_format),
                                                            Locale.ENGLISH);
            dateFormatted = sb.append(sdfMonth.format(date.getTime())).append(" ")
                              .append(sdfYear.format(date.getTime())).toString();
        } else {
            // Format date using the new Locale
            dateFormatted = DateUtils.formatDateRange(getContext(), monthFormatter,
                                                      date.getTime(), date.getTime(), flags, timeZone.getID())
                                     .toString();
        }
        // Call setLength(0) on StringBuilder passed to the Formatter constructor to not accumulate
        // the results
        monthBuilder.setLength(0);

        // Restore default Locale to avoid generating any side effects
        Locale.setDefault(defaultLocale);

        return dateFormatted;
    }

    private void validateDate(Date date) {
        if (date == null) {
            throw new IllegalArgumentException("Selected date must be non-null.");
        }
        if (date.before(minCal.getTime()) || date.after(maxCal.getTime())) {
            throw new IllegalArgumentException(String.format(
                    "SelectedDate must be between minDate and maxDate."
                    + "%nminDate: %s%nmaxDate: %s%nselectedDate: %s", minCal.getTime(), maxCal.getTime(),
                    date));
        }
    }

    private boolean doSelectDate(Date date, MonthCellDescriptor cell) {
        Calendar newlySelectedCal = Calendar.getInstance(timeZone, locale);
        newlySelectedCal.setTime(date);
        // Sanitize input: clear out the hours/minutes/seconds/millis.
        setMidnight(newlySelectedCal);

        // Clear any remaining range state.
        for (MonthCellDescriptor selectedCell : selectedCells) {
            selectedCell.setRangeState(RangeState.NONE);
        }

        switch (selectionMode) {
            case RANGE:
                if (selectedCals.size() <= 1) {
                    isSelectingNext = false;
                }

                if (selectedCals.size() > 1) {
                    MonthCellDescriptor cell1 = selectedCells.get(0);
                    Calendar cal1 = selectedCals.get(0);
                    
                    // We've already got a range selected: clear the old one.
                    clearOldSelections();
                    
                    if (isSelectingNext) {
                        isSelectingNext = false;
                        cell1.setSelected(true);

                        selectedCells.add(cell1);
                        selectedCals.add(cal1);
                    }
                }
                break;

            case MULTIPLE:
                date = applyMultiSelect(date, newlySelectedCal);
                break;

            case SINGLE:
                clearOldSelections();
                break;
            default:
                throw new IllegalStateException("Unknown selectionMode " + selectionMode);
        }

        if (date != null) {
            // Select a new cell.
            if (selectedCals.size() == 1 && newlySelectedCal.before(selectedCals.get(0))) {
                // We're moving the start of the range back in time: set next date as first date range
                if (selectedCells.isEmpty() || !selectedCells.get(0).equals(cell)) {
                    selectedCells.add(0, cell);
                    cell.setSelected(true);
                }
                
                selectedCals.add(0, newlySelectedCal);
            } else {
                if (selectedCells.isEmpty() || !selectedCells.get(0).equals(cell)) {
                    selectedCells.add(cell);
                    cell.setSelected(true);
                }
                
                selectedCals.add(newlySelectedCal);
            }
            
            if (selectionMode == SelectionMode.RANGE && selectedCells.size() > 1) {
                // Select all days in between start and end.
                Date start = selectedCells.get(0).getDate();
                Date end = selectedCells.get(1).getDate();
                selectedCells.get(0).setRangeState(RangeState.FIRST);
                selectedCells.get(1).setRangeState(RangeState.LAST);

                int startMonthIndex = cells.getIndexOfKey(monthKey(selectedCals.get(0)));
                int endMonthIndex = cells.getIndexOfKey(monthKey(selectedCals.get(1)));
                for (int monthIndex = startMonthIndex; monthIndex <= endMonthIndex; monthIndex++) {
                    List<List<MonthCellDescriptor>> month = cells.getValueAtIndex(monthIndex);
                    for (List<MonthCellDescriptor> week : month) {
                        for (MonthCellDescriptor singleCell : week) {
                            if (singleCell.getDate().after(start)
                                && singleCell.getDate().before(end)
                                && singleCell.isSelectable()) {
                                singleCell.setSelected(true);
                                singleCell.setRangeState(RangeState.MIDDLE);
                                selectedCells.add(singleCell);
                            }
                        }
                    }
                }
            }
        }

        // Update the adapter.
        validateAndUpdate();
        return date != null;
    }

    private String monthKey(Calendar cal) {
        return cal.get(YEAR) + "-" + cal.get(MONTH);
    }

    private String monthKey(MonthDescriptor month) {
        return month.getYear() + "-" + month.getMonth();
    }

    private void clearOldSelections() {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            // De-select the currently-selected cell.
            selectedCell.setSelected(false);

            if (dateListener != null) {
                Date selectedDate = selectedCell.getDate();

                if (selectionMode == SelectionMode.RANGE) {
                    int index = selectedCells.indexOf(selectedCell);
                    if (index == 0 || index == selectedCells.size() - 1) {
                        dateListener.onDateUnselected(selectedDate);
                    }
                } else {
                    dateListener.onDateUnselected(selectedDate);
                }
            }
        }
        selectedCells.clear();
        selectedCals.clear();
    }

    private Date applyMultiSelect(Date date, Calendar selectedCal) {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            if (selectedCell.getDate().equals(date)) {
                // De-select the currently-selected cell.
                selectedCell.setSelected(false);
                selectedCells.remove(selectedCell);
                date = null;
                break;
            }
        }
        for (Calendar cal : selectedCals) {
            if (sameDate(cal, selectedCal)) {
                selectedCals.remove(cal);
                break;
            }
        }
        return date;
    }

    public void highlightDates(Collection<Date> dates) {
        for (Date date : dates) {
            validateDate(date);

            MonthCellWithMonthIndex monthCellWithMonthIndex = getMonthCellWithIndexByDate(date);
            if (monthCellWithMonthIndex != null) {
                Calendar newlyHighlightedCal = Calendar.getInstance(timeZone, locale);
                newlyHighlightedCal.setTime(date);
                MonthCellDescriptor cell = monthCellWithMonthIndex.cell;

                highlightedCells.add(cell);
                highlightedCals.add(newlyHighlightedCal);
                cell.setHighlighted(true);
            }
        }

        validateAndUpdate();
    }

    public void clearSelectedDates() {
        for (MonthCellDescriptor selectedCell : selectedCells) {
            selectedCell.setRangeState(RangeState.NONE);
        }

        clearOldSelections();
        validateAndUpdate();
    }

    public void clearHighlightedDates() {
        for (MonthCellDescriptor cal : highlightedCells) {
            cal.setHighlighted(false);
        }
        highlightedCells.clear();
        highlightedCals.clear();

        validateAndUpdate();
    }

    /** Hold a cell with a month-index. */
    private static class MonthCellWithMonthIndex {

        MonthCellDescriptor cell;
        int monthIndex;

        MonthCellWithMonthIndex(MonthCellDescriptor cell, int monthIndex) {
            this.cell = cell;
            this.monthIndex = monthIndex;
        }
    }

    /** Return cell and month-index (for scrolling) for a given Date. */
    private MonthCellWithMonthIndex getMonthCellWithIndexByDate(Date date) {
        Calendar searchCal = Calendar.getInstance(timeZone, locale);
        searchCal.setTime(date);
        String monthKey = monthKey(searchCal);
        Calendar actCal = Calendar.getInstance(timeZone, locale);

        int index = cells.getIndexOfKey(monthKey);
        List<List<MonthCellDescriptor>> monthCells = cells.get(monthKey);
        for (List<MonthCellDescriptor> weekCells : monthCells) {
            for (MonthCellDescriptor actCell : weekCells) {
                actCal.setTime(actCell.getDate());
                if (sameDate(actCal, searchCal) && actCell.isSelectable()) {
                    return new MonthCellWithMonthIndex(actCell, index);
                }
            }
        }
        return null;
    }

    private class MonthAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private List<String> monthsTitle;

        private MonthAdapter() {
            inflater = LayoutInflater.from(getContext());
        }

        @Override
        public boolean isEnabled(int position) {
            // Disable selectability: each cell will handle that itself.
            return false;
        }

        @Override
        public int getCount() {
            return months.size();
        }

        @Override
        public Object getItem(int position) {
            return months.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setMonthsTitle(List<String> monthsTitle) {
            this.monthsTitle = monthsTitle;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MonthView monthView = (MonthView) convertView;

            if (monthView == null //
                || !monthView.getTag(R.id.day_view_adapter_class).equals(dayViewAdapter.getClass())) {
                monthView =
                        MonthView.create(parent, inflater, weekdayNameFormat, listener, today,
                                         dayBackgroundResId, dayTextColorResId, titleTextStyle, displayHeader,
                                         headerTextColor, displayDayNamesHeaderRow, displayDayNamesAsCalendarHeader,
                                         displayAlwaysDigitNumbers, decorators, locale, dayViewAdapter);
                monthView.setTag(R.id.day_view_adapter_class, dayViewAdapter.getClass());
            } else {
                monthView.setDecorators(decorators);
            }

            if (monthsReverseOrder) {
                position = months.size() - position - 1;
            }
            
            monthView.init(
                    months.get(position),
                    cells.getValueAtIndex(position),
                    displayOnly,
                    titleTypeface,
                    dateTypeface,
                    monthsTitle
            );
            
            return monthView;
        }
    }

    List<List<MonthCellDescriptor>> getMonthCells(MonthDescriptor month, Calendar startCal) {
        Calendar cal = Calendar.getInstance(timeZone, locale);
        cal.setTime(startCal.getTime());
        List<List<MonthCellDescriptor>> cells = new ArrayList<>();
        cal.set(DAY_OF_MONTH, 1);
        int firstDayOfWeek = cal.get(DAY_OF_WEEK);
        int offset = cal.getFirstDayOfWeek() - firstDayOfWeek;
        if (offset > 0) {
            offset -= 7;
        }
        cal.add(Calendar.DATE, offset);

        Calendar minSelectedCal = minDate(selectedCals);
        Calendar maxSelectedCal = maxDate(selectedCals);

        while ((cal.get(MONTH) < month.getMonth() + 1 || cal.get(YEAR) < month.getYear()) //
               && cal.get(YEAR) <= month.getYear()) {
            List<MonthCellDescriptor> weekCells = new ArrayList<>();
            cells.add(weekCells);
            for (int c = 0; c < 7; c++) {
                Date date = cal.getTime();
                @SuppressWarnings("MagicConstant")
                boolean isCurrentMonth = cal.get(MONTH) == month.getMonth();
                boolean isSelected = isCurrentMonth && containsDate(selectedCals, cal);
                boolean isSelectable =
                        isCurrentMonth && betweenDates(cal, minCal, maxCal) && isDateSelectable(date);
                boolean isToday = sameDate(cal, today);
                boolean isHighlighted = containsDate(highlightedCals, cal);
                int value = cal.get(DAY_OF_MONTH);

                RangeState rangeState = RangeState.NONE;
                if (selectedCals.size() > 1) {
                    if (sameDate(minSelectedCal, cal)) {
                        rangeState = RangeState.FIRST;
                    } else if (sameDate(maxDate(selectedCals), cal)) {
                        rangeState = RangeState.LAST;
                    } else if (betweenDates(cal, minSelectedCal, maxSelectedCal)) {
                        rangeState = RangeState.MIDDLE;
                    }
                }

                weekCells.add(
                        new MonthCellDescriptor(date, isCurrentMonth, isSelectable, isSelected, isToday,
                                                isHighlighted, value, rangeState));
                cal.add(DATE, 1);
            }
        }
        return cells;
    }

    private boolean containsDate(List<Calendar> selectedCals, Date date) {
        Calendar cal = Calendar.getInstance(timeZone, locale);
        cal.setTime(date);
        return containsDate(selectedCals, cal);
    }

    private static boolean containsDate(List<Calendar> selectedCals, Calendar cal) {
        for (Calendar selectedCal : selectedCals) {
            if (sameDate(cal, selectedCal)) {
                return true;
            }
        }
        return false;
    }

    private static Calendar minDate(List<Calendar> selectedCals) {
        if (selectedCals == null || selectedCals.size() == 0) {
            return null;
        }
        Collections.sort(selectedCals);
        return selectedCals.get(0);
    }

    private static Calendar maxDate(List<Calendar> selectedCals) {
        if (selectedCals == null || selectedCals.size() == 0) {
            return null;
        }
        Collections.sort(selectedCals);
        return selectedCals.get(selectedCals.size() - 1);
    }

    private static boolean sameDate(Calendar cal, Calendar selectedDate) {
        return cal.get(MONTH) == selectedDate.get(MONTH)
               && cal.get(YEAR) == selectedDate.get(YEAR)
               && cal.get(DAY_OF_MONTH) == selectedDate.get(DAY_OF_MONTH);
    }

    private static boolean betweenDates(Calendar cal, Calendar minCal, Calendar maxCal) {
        final Date date = cal.getTime();
        return betweenDates(date, minCal, maxCal);
    }

    static boolean betweenDates(Date date, Calendar minCal, Calendar maxCal) {
        final Date min = minCal.getTime();
        return (date.equals(min) || date.after(min)) // >= minCal
               && date.before(maxCal.getTime()); // && < maxCal
    }

    @SuppressWarnings("MagicConstant")
    private static boolean sameMonth(Calendar cal, MonthDescriptor month) {
        return (cal.get(MONTH) == month.getMonth() && cal.get(YEAR) == month.getYear());
    }

    private boolean isDateSelectable(Date date) {
        return dateConfiguredListener == null || dateConfiguredListener.isDateSelectable(date);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        dateListener = listener;
    }

    public void setOnRangeSelectionListener(OnRangeDateSelectedListener listener) {
        rangeDateListener = listener;
    }

    /**
     * Set a listener to react to user selection of a disabled date.
     *
     * @param listener the listener to set, or null for no reaction
     */
    public void setOnInvalidDateSelectedListener(OnInvalidDateSelectedListener listener) {
        invalidDateListener = listener;
    }

    /**
     * Set a listener used to discriminate between selectable and unselectable dates. Set this to
     * disable arbitrary dates as they are rendered.
     * <p>
     * Important: set this before you call {@link #init(Date, Date)} methods.  If called afterwards,
     * it will not be consistently applied.
     */
    public void setDateSelectableFilter(DateSelectableFilter listener) {
        dateConfiguredListener = listener;
    }

    /**
     * Set an adapter used to initialize {@link CalendarCellView} with custom layout.
     * <p>
     * Important: set this before you call {@link #init(Date, Date)} methods.  If called afterwards,
     * it will not be consistently applied.
     */
    public void setCustomDayView(DayViewAdapter dayViewAdapter) {
        this.dayViewAdapter = dayViewAdapter;
        if (null != adapter) {
            adapter.notifyDataSetChanged();
        }
    }

    /** Set a listener to intercept clicks on calendar cells. */
    public void setCellClickInterceptor(CellClickInterceptor listener) {
        cellClickInterceptor = listener;
    }

    /**
     * Interface to be notified when a new date is selected or unselected. This will only be called
     * when the user initiates the date selection.  If you call {@link #selectDate(Date)} this
     * listener will not be notified.
     *
     * @see #setOnDateSelectedListener(OnDateSelectedListener)
     */
    public interface OnDateSelectedListener {

        void onDateSelected(Date date);

        void onDateUnselected(Date date);
    }

    /**
     * Interface to be notified when a new date range is selected. This will only be called
     * when the user initiates the date selection.  If you call {@link #selectDate(Date)} this
     * listener will not be notified.
     *
     * @see #setOnRangeSelectionListener(OnRangeDateSelectedListener)
     */
    public interface OnRangeDateSelectedListener {
        void onRangeSelected(Date date1, Date date2);
    }

    /**
     * Interface to be notified when an invalid date is selected by the user. This will only be
     * called when the user initiates the date selection. If you call {@link #selectDate(Date)} this
     * listener will not be notified.
     *
     * @see #setOnInvalidDateSelectedListener(OnInvalidDateSelectedListener)
     */
    public interface OnInvalidDateSelectedListener {

        void onInvalidDateSelected(Date date);
    }

    /**
     * Interface used for determining the selectability of a date cell when it is configured for
     * display on the calendar.
     *
     * @see #setDateSelectableFilter(DateSelectableFilter)
     */
    public interface DateSelectableFilter {

        boolean isDateSelectable(Date date);
    }

    /**
     * Interface to be notified when a cell is clicked and possibly intercept the click.  Return true
     * to intercept the click and prevent any selections from changing.
     *
     * @see #setCellClickInterceptor(CellClickInterceptor)
     */
    public interface CellClickInterceptor {
        boolean onCellClicked(Date date);
    }

    private static class DefaultOnInvalidDateSelectedListener implements OnInvalidDateSelectedListener {
        @Override
        public void onInvalidDateSelected(Date date) {
            // No default behavior. Nothing happens.
        }
    }
    
    public static class SelectedRange {
        @Nullable
        private Date dateStart;
        
        @Nullable
        private Date dateEnd;
        
        public SelectedRange(@Nullable Date dateStart, @Nullable Date dateEnd) {
            this.dateStart = dateStart;
            this.dateEnd = dateEnd;
        }
       
        @Nullable
        public Date getDateStart() {
            return dateStart;
        }
        
        @Nullable
        public Date getDateEnd() {
            return dateEnd;
        }
    }
}
