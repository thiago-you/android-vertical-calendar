package you.thiago.calendarvertical.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import you.thiago.calendarvertical.CalendarPickerView;
import you.thiago.calendarvertical.CalendarVertical;
import you.thiago.calendarvertical.DefaultDayViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import you.thiago.calendarvert.sample.R;

import static android.widget.Toast.LENGTH_SHORT;

public class SampleCalendarVertActivity extends Activity {

    private static final String TAG = "SampleCalendarVert";
    private CalendarPickerView calendar;
    private AlertDialog theDialog;
    private CalendarPickerView dialogView;
    private final Set<Button> modeButtons = new LinkedHashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_calendar_picker);

        final Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);

        final Calendar lastYear = Calendar.getInstance();
        lastYear.add(Calendar.YEAR, -1);

        calendar = ((CalendarVertical) findViewById(R.id.calendar_vertical)).getInstance();

        Calendar today = Calendar.getInstance();
        ArrayList<Date> dates = new ArrayList<>();
        
        today.add(Calendar.DATE, 3);
        dates.add(today.getTime());
        
        today.add(Calendar.DATE, 5);
        dates.add(today.getTime());
        
        calendar.build()
                .withSelectedDates(dates);

        initButtonListeners(nextYear, lastYear);
    }

    private void initButtonListeners(final Calendar nextYear, final Calendar lastYear) {
        final Button single = findViewById(R.id.button_single);
        final Button multi = findViewById(R.id.button_multi);
        final Button highlight = findViewById(R.id.button_highlight);
        final Button range = findViewById(R.id.button_range);
        final Button displayOnly = findViewById(R.id.button_display_only);
        final Button dialog = findViewById(R.id.button_dialog);
        final Button customized = findViewById(R.id.button_customized);
        final Button decorator = findViewById(R.id.button_decorator);
        final Button hebrew = findViewById(R.id.button_hebrew);
        final Button arabic = findViewById(R.id.button_arabic);
        final Button arabicDigits = findViewById(R.id.button_arabic_with_digits);
        final Button customView = findViewById(R.id.button_custom_view);

        modeButtons.addAll(Arrays.asList(single, multi, range, displayOnly, decorator, customView));

        range.setOnClickListener(v -> {
            setButtonsEnabled(range);

            calendar.setCustomDayView(new DefaultDayViewAdapter());
            Calendar today = Calendar.getInstance();
            ArrayList<Date> dates = new ArrayList<>();
            today.add(Calendar.DATE, 3);
            dates.add(today.getTime());
            today.add(Calendar.DATE, 5);
            dates.add(today.getTime());
            calendar.setDecorators(Collections.emptyList());
            calendar.init(new Date(), nextYear.getTime()) //
                    .inMode(CalendarPickerView.SelectionMode.RANGE) //
                    .withSelectedDates(dates);
        });
        
        single.setOnClickListener(v -> {
            setButtonsEnabled(single);

            calendar.setCustomDayView(new DefaultDayViewAdapter());
            calendar.setDecorators(Collections.emptyList());
            calendar.init(lastYear.getTime(), nextYear.getTime()) //
                    .inMode(CalendarPickerView.SelectionMode.SINGLE) //
                    .withSelectedDate(new Date());
        });

        multi.setOnClickListener(v -> {
            setButtonsEnabled(multi);

            calendar.setCustomDayView(new DefaultDayViewAdapter());
            Calendar today = Calendar.getInstance();
            ArrayList<Date> dates = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                today.add(Calendar.DAY_OF_MONTH, 3);
                dates.add(today.getTime());
            }
            calendar.setDecorators(Collections.emptyList());
            calendar.init(new Date(), nextYear.getTime()) //
                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE) //
                    .withSelectedDates(dates);
        });

        highlight.setOnClickListener(view -> {
            setButtonsEnabled(highlight);

            Calendar c = Calendar.getInstance();
            c.setTime(new Date());

            Calendar minRange = Calendar.getInstance();
            minRange.setTime(new Date());
            minRange.add(Calendar.YEAR, -1);

            Calendar maxRange = Calendar.getInstance();
            maxRange.setTime(new Date());
            maxRange.add(Calendar.YEAR, 1);

            calendar.setCustomDayView(new DefaultDayViewAdapter());
            calendar.setDecorators(Collections.emptyList());

            calendar.init(minRange.getTime(), maxRange.getTime())
                    .inMode(CalendarPickerView.SelectionMode.SINGLE)
                    .withSelectedDate(c.getTime());

            calendar.highlightDates(getHighlightedDaysForMonth(c.get(Calendar.MONTH)));
        });

        displayOnly.setOnClickListener(v -> {
            setButtonsEnabled(displayOnly);

            calendar.setCustomDayView(new DefaultDayViewAdapter());
            calendar.setDecorators(Collections.emptyList());
            calendar.init(new Date(), nextYear.getTime()) //
                    .inMode(CalendarPickerView.SelectionMode.SINGLE) //
                    .withSelectedDate(new Date()) //
                    .displayOnly();
        });

        dialog.setOnClickListener(view -> {
            String title = "I'm a dialog!";
            showCalendarInDialog(title, R.layout.dialog);
            dialogView.init(lastYear.getTime(), nextYear.getTime()) //
                      .withSelectedDate(new Date());
        });

        customized.setOnClickListener(view -> {
            showCalendarInDialog("Pimp my calendar!", R.layout.dialog_customized);
            dialogView.init(lastYear.getTime(), nextYear.getTime()).withSelectedDate(new Date());
        });

        decorator.setOnClickListener(v -> {
            setButtonsEnabled(decorator);

            calendar.setCustomDayView(new DefaultDayViewAdapter());
            calendar.setDecorators(List.of(new SampleDecorator()));
            calendar.init(lastYear.getTime(), nextYear.getTime()) //
                    .inMode(CalendarPickerView.SelectionMode.SINGLE) //
                    .withSelectedDate(new Date());
        });

        hebrew.setOnClickListener(view -> {
            showCalendarInDialog("I'm Hebrew!", R.layout.dialog);
            dialogView.init(lastYear.getTime(), nextYear.getTime(), new Locale("iw", "IL")) //
                      .withSelectedDate(new Date());
        });

        arabic.setOnClickListener(view -> {
            showCalendarInDialog("I'm Arabic!", R.layout.dialog);
            dialogView.init(lastYear.getTime(), nextYear.getTime(), new Locale("ar", "EG")) //
                      .withSelectedDate(new Date());
        });

        arabicDigits.setOnClickListener(view -> {
            showCalendarInDialog("I'm Arabic with Digits!", R.layout.dialog_digits);
            dialogView.init(lastYear.getTime(), nextYear.getTime(), new Locale("ar", "EG")) //
                      .withSelectedDate(new Date());
        });

        customView.setOnClickListener(view -> {
            setButtonsEnabled(customView);

            calendar.setDecorators(Collections.emptyList());
            calendar.setCustomDayView(new SampleDayViewAdapter());
            calendar.init(lastYear.getTime(), nextYear.getTime())
                    .inMode(CalendarPickerView.SelectionMode.SINGLE)
                    .withSelectedDate(new Date());
        });

        findViewById(R.id.done_button).setOnClickListener(view -> {
            Log.d(TAG, "Selected time in millis: " + calendar.getSelectedDate().getTime());
            String toast = "Selected: " + calendar.getSelectedDate().getTime();
            Toast.makeText(SampleCalendarVertActivity.this, toast, LENGTH_SHORT).show();
        });
    }

    private void showCalendarInDialog(String title, int layoutResId) {
        CalendarVertical calendarVertical = (CalendarVertical) getLayoutInflater().inflate(layoutResId, null, false);


        dialogView = calendarVertical.getInstance();
        
        theDialog = new AlertDialog.Builder(this) //
                                                  .setTitle(title)
                                                  .setView(calendarVertical)
                                                  .setNeutralButton("Dismiss",
                                                                    (dialogInterface, i) -> dialogInterface.dismiss())
                                                  .create();
        theDialog.setOnShowListener(dialogInterface -> {
            Log.d(TAG, "onShow: fix the dimens!");
            //dialogView.fixDialogDimens();
        });
        theDialog.show();
    }

    private void setButtonsEnabled(Button currentButton) {
        for (Button modeButton : modeButtons) {
            modeButton.setEnabled(modeButton != currentButton);
        }
    }
    
    private Date getDateWithYearAndMonthForDay(int year, int month, int day) {
        final Calendar cal = Calendar.getInstance();

        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    private List<Date> getHighlightedDaysForMonth(int month) {
        List<Date> dateList = new ArrayList<>();

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());

        for (int j = 0; j < 10; j++) {
            dateList.add(getDateWithYearAndMonthForDay(c.get(Calendar.YEAR), month, j + 1));
        }

        return dateList;
    }

    @Override
    public void onConfigurationChanged(@NotNull Configuration newConfig) {
        boolean applyFixes = theDialog != null && theDialog.isShowing();
        if (applyFixes) {
            Log.d(TAG, "Config change: unfix the dimens so I'll get remeasured!");
            dialogView.unfixDialogDimens();
        }
        
        super.onConfigurationChanged(newConfig);
        
        if (applyFixes) {
            dialogView.post(() -> {
                Log.d(TAG, "Config change done: re-fix the dimens!");
                dialogView.fixDialogDimens();
            });
        }
    }
}
