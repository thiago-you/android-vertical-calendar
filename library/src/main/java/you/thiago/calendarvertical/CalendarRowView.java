package you.thiago.calendarvertical;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import static android.view.View.MeasureSpec.AT_MOST;
import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.makeMeasureSpec;

/** TableRow that draws a divider between each cell. To be used with {@link CalendarGridView}. */
public class CalendarRowView extends ViewGroup implements View.OnClickListener {
  private boolean isHeaderRow;
  private MonthView.Listener listener;

  public CalendarRowView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override public void addView(View child, int index, ViewGroup.LayoutParams params) {
    child.setOnClickListener(this);
    super.addView(child, index, params);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int totalWidth = MeasureSpec.getSize(widthMeasureSpec);
    int rowHeight = 0;
    int cellHeightSpec = makeMeasureSpec(totalWidth, AT_MOST);
    for (int c = 0, numChildren = getChildCount(); c < numChildren; c++) {
      final View child = getChildAt(c);
      // Calculate width cells, making sure to cover totalWidth.
      int l = (c * totalWidth) / 7;
      int r = ((c + 1) * totalWidth) / 7;
      int cellSize = r - l;
      if (!isHeaderRow) {
        child.setMinimumHeight(cellSize);
      }
      int cellWidthSpec = makeMeasureSpec(cellSize, EXACTLY);
      child.measure(cellWidthSpec, cellHeightSpec);
      // The row height is the height of the tallest cell.
      if (child.getMeasuredHeight() > rowHeight) {
        rowHeight = child.getMeasuredHeight();
      }
    }

    final int widthWithPadding = totalWidth + getPaddingLeft() + getPaddingRight();
    final int heightWithPadding = rowHeight + getPaddingTop() + getPaddingBottom();

    setMeasuredDimension(widthWithPadding, heightWithPadding);
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    int cellHeight = bottom - top;
    int width = right - left;
    for (int c = 0, numChildren = getChildCount(); c < numChildren; c++) {
      final View child = getChildAt(c);
      int l = (c * width) / 7;
      int r = ((c + 1) * width) / 7;
      child.layout(l, 0, r, cellHeight);
    }
  }

  public void setIsHeaderRow(boolean isHeaderRow) {
    this.isHeaderRow = isHeaderRow;
  }

  @Override public void onClick(View v) {
    // Header rows don't have a click listener
    if (listener != null) {
      listener.handleClick((MonthCellDescriptor) v.getTag());
    }
  }

  public void setListener(MonthView.Listener listener) {
    this.listener = listener;
  }

  public void setDayViewAdapter(DayViewAdapter adapter) {
    for (int i = 0; i < getChildCount(); i++) {
      if (getChildAt(i) instanceof CalendarCellView) {
        CalendarCellView cell = ((CalendarCellView) getChildAt(i));
        cell.removeAllViews();
        adapter.makeCellView(cell);
      }
    }
  }

  public void setCellBackground(int resId) {
    for (int i = 0; i < getChildCount(); i++) {
      getChildAt(i).setBackgroundResource(resId);
    }
  }

  public void setCellTextColor(int resId) {
    for (int i = 0; i < getChildCount(); i++) {
      if (getChildAt(i) instanceof CalendarCellView) {
        ((CalendarCellView) getChildAt(i)).getDayOfMonthTextView().setTextColor(resId);
      } else {
        ((TextView) getChildAt(i)).setTextColor(resId);
      }
    }
  }

  public void setCellTextColor(ColorStateList colors) {
    for (int i = 0; i < getChildCount(); i++) {
      if (getChildAt(i) instanceof CalendarCellView) {
        ((CalendarCellView) getChildAt(i)).getDayOfMonthTextView().setTextColor(colors);
      } else {
        ((TextView) getChildAt(i)).setTextColor(colors);
      }
    }
  }

  public void setCellTextFont(Typeface typeFace) {
    for (int i = 0; i < getChildCount(); i++) {
      if (getChildAt(i) instanceof CalendarCellView) {
          ((CalendarCellView) getChildAt(i)).getDayOfMonthTextView().setTypeface(typeFace);
      } else {
        ((TextView) getChildAt(i)).setTypeface(typeFace);
      }
    }
  }

  public void setTypeface(Typeface typeface) {
    for (int i = 0; i < getChildCount(); i++) {
      if (getChildAt(i) instanceof CalendarCellView) {
        ((CalendarCellView) getChildAt(i)).getDayOfMonthTextView().setTypeface(typeface);
      } else {
        ((TextView) getChildAt(i)).setTypeface(typeface);
      }
    }
  }
}
