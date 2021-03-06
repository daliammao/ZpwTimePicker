/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.daliammao.zpwtimepickerlib.datepicker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daliammao.zpwtimepickerlib.BuildConfig;
import com.daliammao.zpwtimepickerlib.R;
import com.daliammao.zpwtimepickerlib.utilities.SUtils;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.Calendar;
import java.util.Locale;

/**
 * An adapter for a list of {@link MonthView} items.
 */
class DayPickerPagerAdapter extends PagerAdapter {

    private static final String TAG = DayPickerPagerAdapter.class.getSimpleName();

    private static final int MONTHS_IN_YEAR = 12;

    private LocalDate mMinDate = LocalDate.now();
    private LocalDate mMaxDate = LocalDate.now();

    private final SparseArray<ViewHolder> mItems = new SparseArray<>();

    private final LayoutInflater mInflater;
    private final int mLayoutResId;
    private final int mCalendarViewId;

    private SelectedDate mSelectedDay = null;

    private int mMonthTextAppearance;
    private int mDayOfWeekTextAppearance;
    private int mDayTextAppearance;

    private ColorStateList mCalendarTextColor;
    private ColorStateList mDaySelectorColor;
    private final ColorStateList mDayHighlightColor;

    private DaySelectionEventListener mDaySelectionEventListener;

    private int mCount;
    private int mFirstDayOfWeek;

    // used in resolving start/end dates during range selection
    private final SelectedDate mTempSelectedDay = new SelectedDate(LocalDate.now());

    public DayPickerPagerAdapter(@NonNull Context context, @LayoutRes int layoutResId,
                                 @IdRes int calendarViewId) {
        mInflater = LayoutInflater.from(context);
        mLayoutResId = layoutResId;
        mCalendarViewId = calendarViewId;

        final TypedArray ta = context.obtainStyledAttributes(new int[]{
                R.attr.colorControlHighlight});
        mDayHighlightColor = ta.getColorStateList(0);
        ta.recycle();
    }

    public void setRange(@NonNull LocalDate min, @NonNull LocalDate max) {
        mMinDate = min;
        mMaxDate = max;

        final int diffYear = mMaxDate.getYear() - mMinDate.getYear();
        final int diffMonth = mMaxDate.getMonthOfYear() - mMinDate.getMonthOfYear();
        mCount = diffMonth + MONTHS_IN_YEAR * diffYear + 1;

        // Positions are now invalid, clear everything and start over.
        notifyDataSetChanged();
    }

    /**
     * Sets the first day of the week.
     *
     * @param weekStart which day the week should start on, valid values are
     *                  {@link Calendar#SUNDAY} through {@link Calendar#SATURDAY}
     */
    public void setFirstDayOfWeek(int weekStart) {
        mFirstDayOfWeek = weekStart;

        // Update displayed views.
        final int count = mItems.size();
        for (int i = 0; i < count; i++) {
            final MonthView monthView = mItems.valueAt(i).calendar;
            monthView.setFirstDayOfWeek(weekStart);
        }
    }

    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek;
    }

    /**
     * Sets the selected day.
     *
     * @param day the selected day
     */
    public void setSelectedDay(@Nullable SelectedDate day) {
        final int[] oldPosition = getPositionsForDay(mSelectedDay);
        final int[] newPosition = getPositionsForDay(day);

        boolean shouldClearOldPosition = oldPosition != null;

        // Clear the old position if necessary.
        if (shouldClearOldPosition) {
            for (int i = oldPosition[0]; i <= oldPosition[oldPosition.length - 1]; i++) {
                final ViewHolder oldMonthView = mItems.get(i, null);
                if (oldMonthView != null) {
                    oldMonthView.calendar.setSelectedDays(-1, -1, SelectedDate.Type.SINGLE);
                }
            }
        }

        // Set the new position.
        if (newPosition != null) {
            if (newPosition.length == 1) {
                final ViewHolder newMonthView = mItems.get(newPosition[0], null);
                if (newMonthView != null) {
                    final int dayOfMonth = day.getStartDate().getDayOfMonth();
                    newMonthView.calendar.setSelectedDays(dayOfMonth, dayOfMonth, SelectedDate.Type.SINGLE);
                }
            } else if (newPosition.length == 2) {
                boolean rangeIsInSameMonth = newPosition[0] == newPosition[1];

                if (rangeIsInSameMonth) {
                    final ViewHolder newMonthView = mItems.get(newPosition[0], null);
                    if (newMonthView != null) {
                        final int startDayOfMonth = day.getStartDate().getDayOfMonth();
                        final int endDayOfMonth = day.getEndDate().getDayOfMonth();

                        newMonthView.calendar.setSelectedDays(startDayOfMonth, endDayOfMonth, SelectedDate.Type.RANGE);
                    }
                } else {
                    // Deal with starting month
                    final ViewHolder newMonthViewStart = mItems.get(newPosition[0], null);
                    if (newMonthViewStart != null) {
                        final int startDayOfMonth = day.getStartDate().getDayOfMonth();
                        // TODO: Check this
                        final int endDayOfMonth = day.getStartDate()
                                .toDateTime(LocalTime.now())
                                .toCalendar(Locale.getDefault())
                                .getActualMaximum(Calendar.DATE);

                        newMonthViewStart.calendar.setSelectedDays(startDayOfMonth, endDayOfMonth, SelectedDate.Type.RANGE);
                    }

                    for (int i = newPosition[0] + 1; i < newPosition[1]; i++) {
                        final ViewHolder newMonthView = mItems.get(i, null);
                        if (newMonthView != null) {
                            newMonthView.calendar.selectAllDays();
                        }
                    }

                    // Deal with ending month
                    final ViewHolder newMonthViewEnd = mItems.get(newPosition[1], null);
                    if (newMonthViewEnd != null) {
                        final int startDayOfMonth = Calendar.getInstance().getMinimum(Calendar.DATE);
                        // TODO: Check this
                        final int endDayOfMonth = day.getEndDate().getDayOfMonth();

                        newMonthViewEnd.calendar.setSelectedDays(startDayOfMonth, endDayOfMonth, SelectedDate.Type.RANGE);
                    }
                }
            }
        }

        mSelectedDay = day;
    }

    /**
     * Sets the listener to call when the user selects a day.
     *
     * @param listener The listener to call.
     */
    public void setDaySelectionEventListener(DaySelectionEventListener listener) {
        mDaySelectionEventListener = listener;
    }

    @SuppressWarnings("unused")
    void setCalendarTextColor(ColorStateList calendarTextColor) {
        mCalendarTextColor = calendarTextColor;
    }

    void setDaySelectorColor(ColorStateList selectorColor) {
        mDaySelectorColor = selectorColor;
    }

    void setMonthTextAppearance(int resId) {
        mMonthTextAppearance = resId;
    }

    void setDayOfWeekTextAppearance(int resId) {
        mDayOfWeekTextAppearance = resId;
    }

    int getDayOfWeekTextAppearance() {
        return mDayOfWeekTextAppearance;
    }

    void setDayTextAppearance(int resId) {
        mDayTextAppearance = resId;
    }

    int getDayTextAppearance() {
        return mDayTextAppearance;
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        final ViewHolder holder = (ViewHolder) object;
        return view == holder.container;
    }

    private int getMonthForPosition(int position) {
        return (position + mMinDate.getMonthOfYear() - 1) % MONTHS_IN_YEAR + 1;
    }

    private int getYearForPosition(int position) {
        final int yearOffset = (position + mMinDate.getMonthOfYear() - 1) / MONTHS_IN_YEAR;
        return yearOffset + mMinDate.getYear();
    }

    @SuppressWarnings("unused")
    private int getPositionForDay(@Nullable LocalDate day) {
        if (day == null) {
            return -1;
        }

        final int yearOffset = day.getYear() - mMinDate.getYear();
        final int monthOffset = day.getMonthOfYear() - mMinDate.getMonthOfYear();
        return (yearOffset * MONTHS_IN_YEAR + monthOffset);
    }

    private int[] getPositionsForDay(@Nullable SelectedDate day) {
        if (day == null) {
            return null;
        }

        SelectedDate.Type typeOfDay = day.getType();
        int[] positions = null;

        if (typeOfDay == SelectedDate.Type.SINGLE) {
            positions = new int[1];
            positions[0] = getPositionForDay(day.getStartDate());
        } else if (typeOfDay == SelectedDate.Type.RANGE) {
            positions = new int[2];
            positions[0] = getPositionForDay(day.getStartDate());
            positions[1] = getPositionForDay(day.getEndDate());
        }

        return positions;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        final View itemView = mInflater.inflate(mLayoutResId, container, false);

        final MonthView v = (MonthView) itemView.findViewById(mCalendarViewId);
        v.setOnDayClickListener(mOnDayClickListener);
        v.setMonthTextAppearance(mMonthTextAppearance);
        v.setDayOfWeekTextAppearance(mDayOfWeekTextAppearance);
        v.setDayTextAppearance(mDayTextAppearance);

        if (mDaySelectorColor != null) {
            v.setDaySelectorColor(mDaySelectorColor);
        }

        if (mDayHighlightColor != null) {
            v.setDayHighlightColor(mDayHighlightColor);
        }

        if (mCalendarTextColor != null) {
            v.setMonthTextColor(mCalendarTextColor);
            v.setDayOfWeekTextColor(mCalendarTextColor);
            v.setDayTextColor(mCalendarTextColor);
        }

        final int month = getMonthForPosition(position);
        final int year = getYearForPosition(position);

        final int[] selectedDay = resolveSelectedDayBasedOnType(month, year);

        final int enabledDayRangeStart;
        if (mMinDate.getMonthOfYear() == month && mMinDate.getYear() == year) {
            enabledDayRangeStart = mMinDate.getDayOfMonth();
        } else {
            enabledDayRangeStart = 1;
        }

        final int enabledDayRangeEnd;
        if (mMaxDate.getMonthOfYear() == month && mMaxDate.getYear() == year) {
            enabledDayRangeEnd = mMaxDate.getDayOfMonth();
        } else {
            enabledDayRangeEnd = 31;
        }

        if (BuildConfig.DEBUG) {
            Log.i(TAG, "mSelectedDay.getType(): " + (mSelectedDay != null ? mSelectedDay.getType() : null));
        }

        // MonthView内部计算使用Calendar,与joda time 月份索引相差1
        v.setMonthParams(month - 1, year, mFirstDayOfWeek,
                enabledDayRangeStart, enabledDayRangeEnd, selectedDay[0], selectedDay[1],
                mSelectedDay != null ? mSelectedDay.getType() : null);

        final ViewHolder holder = new ViewHolder(position, itemView, v);
        mItems.put(position, holder);

        container.addView(itemView);

        return holder;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        final ViewHolder holder = (ViewHolder) object;
        container.removeView(holder.container);

        mItems.remove(position);
    }

    @Override
    public int getItemPosition(Object object) {
        final ViewHolder holder = (ViewHolder) object;
        return holder.position;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        final MonthView v = mItems.get(position).calendar;
        if (v != null) {
            return v.getTitle();
        }
        return null;
    }

    private final MonthView.OnDayClickListener mOnDayClickListener = new MonthView.OnDayClickListener() {
        @Override
        public void onDayClick(MonthView view, Calendar day) {
            if (day != null) {
                if (mDaySelectionEventListener != null) {
                    mDaySelectionEventListener.onDaySelected(DayPickerPagerAdapter.this, LocalDate.fromCalendarFields(day));
                }
            }
        }
    };

    private static class ViewHolder {
        public final int position;
        public final View container;
        public final MonthView calendar;

        public ViewHolder(int position, View container, MonthView calendar) {
            this.position = position;
            this.container = container;
            this.calendar = calendar;
        }
    }

    public SelectedDate resolveStartDateForRange(int x, int y, int position) {
        if (position >= 0) {
            final ViewHolder newMonthView = mItems.get(position, null);
            if (newMonthView != null) {
                final int dayOfMonth = newMonthView.calendar.getDayAtLocation(x, y);
                LocalDate selectedDayStart = LocalDate.fromCalendarFields(newMonthView.calendar.composeDate(dayOfMonth));
                mTempSelectedDay.setDate(selectedDayStart);
                return mTempSelectedDay;
            }
        }

        return null;
    }

    public SelectedDate resolveEndDateForRange(int x, int y, int position, boolean updateIfNecessary) {
        if (position >= 0) {
            final ViewHolder newMonthView = mItems.get(position, null);
            if (newMonthView != null) {
                final int dayOfMonth = newMonthView.calendar.getDayAtLocation(x, y);
                LocalDate selectedDayEnd = LocalDate.fromCalendarFields(newMonthView.calendar.composeDate(dayOfMonth));

                if (!updateIfNecessary || !mSelectedDay.getEndDate().isEqual(selectedDayEnd)) {
                    mTempSelectedDay.setEndDate(selectedDayEnd);
                    return mTempSelectedDay;
                }
            }
        }

        return null;
    }

    private int[] resolveSelectedDayBasedOnType(int month, int year) {
        if (mSelectedDay == null) {
            return new int[]{-1, -1};
        }

        if (mSelectedDay.getType() == SelectedDate.Type.SINGLE) {
            return resolveSelectedDayForTypeSingle(month, year);
        } else if (mSelectedDay.getType() == SelectedDate.Type.RANGE) {
            return resolveSelectedDayForTypeRange(month, year);
        }

        return new int[]{-1, -1};
    }

    private int[] resolveSelectedDayForTypeSingle(int month, int year) {
        if (mSelectedDay.getStartDate().getMonthOfYear() == month
                && mSelectedDay.getStartDate().getYear() == year) {
            int resolvedDay = mSelectedDay.getStartDate().getDayOfMonth();
            return new int[]{resolvedDay, resolvedDay};
        }

        return new int[]{-1, -1};
    }

    private int[] resolveSelectedDayForTypeRange(int month, int year) {
        // Quan: "year.month" Eg: Feb, 2015 ==> 2015.02, Dec, 2000 ==> 2000.12
        float startDateQuan = mSelectedDay.getStartDate().getYear()
                + mSelectedDay.getStartDate().getMonthOfYear() / 100f;
        float endDateQuan = mSelectedDay.getEndDate().getYear()
                + mSelectedDay.getEndDate().getMonthOfYear() / 100f;

        float dateQuan = year + month / 100f;

        if (dateQuan >= startDateQuan && dateQuan <= endDateQuan) {
            int startDay, endDay;
            if (dateQuan == startDateQuan) {
                startDay = mSelectedDay.getStartDate().getDayOfMonth();
            } else {
                startDay = 1;
            }

            if (dateQuan == endDateQuan) {
                endDay = mSelectedDay.getEndDate().getDayOfMonth();
            } else {
                endDay = SUtils.getDaysInMonth(month - 1, year);
            }

            return new int[]{startDay, endDay};
        }

        return new int[]{-1, -1};
    }

    public void onDateRangeSelectionStarted(SelectedDate selectedDate) {
        if (mDaySelectionEventListener != null) {
            mDaySelectionEventListener.onDateRangeSelectionStarted(selectedDate);
        }
    }

    public void onDateRangeSelectionEnded(SelectedDate selectedDate) {
        if (mDaySelectionEventListener != null) {
            mDaySelectionEventListener.onDateRangeSelectionEnded(selectedDate);
        }
    }

    public void onDateRangeSelectionUpdated(SelectedDate selectedDate) {
        if (mDaySelectionEventListener != null) {
            mDaySelectionEventListener.onDateRangeSelectionUpdated(selectedDate);
        }
    }

    public interface DaySelectionEventListener {
        void onDaySelected(DayPickerPagerAdapter view, LocalDate day);

        void onDateRangeSelectionStarted(@NonNull SelectedDate selectedDate);

        void onDateRangeSelectionEnded(@Nullable SelectedDate selectedDate);

        void onDateRangeSelectionUpdated(@NonNull SelectedDate selectedDate);
    }
}
