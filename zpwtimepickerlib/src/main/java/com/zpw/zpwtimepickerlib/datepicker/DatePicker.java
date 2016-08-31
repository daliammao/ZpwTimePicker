/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright 2015 Vikram Kakkar
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

package com.zpw.zpwtimepickerlib.datepicker;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.zpw.zpwtimepickerlib.BuildConfig;
import com.zpw.zpwtimepickerlib.R;
import com.zpw.zpwtimepickerlib.common.DateTimePatternHelper;
import com.zpw.zpwtimepickerlib.helpers.Options;
import com.zpw.zpwtimepickerlib.utilities.AccessibilityUtils;
import com.zpw.zpwtimepickerlib.utilities.SUtils;
import com.zpw.zpwtimepickerlib.utilities.TextColorHelper;

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

public class DatePicker extends FrameLayout {
    private static final String TAG = DatePicker.class.getSimpleName();

    private static final int UNINITIALIZED = -1;
    private static final int VIEW_MONTH_DAY = 0;
    private static final int VIEW_YEAR = 1;

    private static final int RANGE_ACTIVATED_NONE = 0;
    private static final int RANGE_ACTIVATED_START = 1;
    private static final int RANGE_ACTIVATED_END = 2;

    private static final int DEFAULT_START_YEAR = 1900;
    private static final int DEFAULT_END_YEAR = 2100;

    private Context mContext;

    private DateTimeFormatter mYearFormat;
    private DateTimeFormatter mMonthDayFormat;

    // Top-level container.
    private ViewGroup mContainer;

    // Header views.
    private LinearLayout llHeaderDateSingleCont;
    private TextView mHeaderYear;
    private TextView mHeaderMonthDay;
    private LinearLayout llHeaderDateRangeCont;
    private TextView tvHeaderDateStart;
    private TextView tvHeaderDateEnd;
    private ImageView ivHeaderDateReset;

    // Picker views.
    private ViewAnimator mAnimator;
    private DayPickerView mDayPickerView;
    private YearPickerView mYearPickerView;

    // Accessibility strings.
    private String mSelectDay;
    private String mSelectYear;

    private OnDateChangedListener mDateChangedListener;

    private int mCurrentView = UNINITIALIZED;

    private SelectedDate mCurrentDate;
    private LocalDate mTempDate;
    private LocalDate mMinDate;
    private LocalDate mMaxDate;

    private Options.PickerType mPickerType;

    private int mFirstDayOfWeek;

    private Locale mCurrentLocale;

    private DatePickerValidationCallback mValidationCallback;

    private int mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_NONE;

    private boolean mIsInLandscapeMode;

    public DatePicker(Context context) {
        this(context, null);
    }

    public DatePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spDatePickerStyle);
    }

    public DatePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeLayout(attrs, defStyleAttr, R.style.SublimeDatePickerStyle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DatePicker(Context context, AttributeSet attrs,
                      int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeLayout(attrs, defStyleAttr, defStyleRes);
    }

    private void initializeLayout(AttributeSet attrs,
                                  int defStyleAttr, int defStyleRes) {
        mContext = getContext();
        mIsInLandscapeMode = mContext.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        setCurrentLocale(Locale.getDefault());
        mCurrentDate = new SelectedDate(LocalDate.now());
        mTempDate = LocalDate.now();
        mMinDate = new LocalDate(DEFAULT_START_YEAR, Calendar.JANUARY + 1, 1);
        mMaxDate = new LocalDate(DEFAULT_END_YEAR, Calendar.DECEMBER + 1, 31);

        final Resources res = getResources();
        final TypedArray a = mContext.obtainStyledAttributes(attrs,
                R.styleable.SublimeDatePicker, defStyleAttr, defStyleRes);
        final LayoutInflater inflater
                = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final int layoutResourceId = R.layout.date_picker_layout;

        try {
            // Set up and attach container.
            mContainer = (ViewGroup) inflater.inflate(layoutResourceId, this, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        addView(mContainer);

        // Set up header views.
        final ViewGroup header = (ViewGroup) mContainer.findViewById(R.id.date_picker_header);
        llHeaderDateSingleCont = (LinearLayout) header.findViewById(R.id.ll_header_date_single_cont);
        mHeaderYear = (TextView) header.findViewById(R.id.date_picker_header_year);
        mHeaderYear.setOnClickListener(mOnHeaderClickListener);
        mHeaderMonthDay = (TextView) header.findViewById(R.id.date_picker_header_date);
        mHeaderMonthDay.setOnClickListener(mOnHeaderClickListener);

        llHeaderDateRangeCont = (LinearLayout) header.findViewById(R.id.ll_header_date_range_cont);
        tvHeaderDateStart = (TextView) header.findViewById(R.id.tv_header_date_start);
        tvHeaderDateStart.setOnClickListener(mOnHeaderClickListener);
        tvHeaderDateEnd = (TextView) header.findViewById(R.id.tv_header_date_end);
        tvHeaderDateEnd.setOnClickListener(mOnHeaderClickListener);
        ivHeaderDateReset = (ImageView) header.findViewById(R.id.iv_header_date_reset);
        ivHeaderDateReset.setOnClickListener(mOnHeaderClickListener);

        int iconColor, pressedStateBgColor;

        TypedArray typedArray = getContext().obtainStyledAttributes(R.styleable.SublimePicker);
        try {
            iconColor = typedArray.getColor(R.styleable.SublimePicker_spOverflowIconColor,
                    SUtils.COLOR_TEXT_PRIMARY_INVERSE);
            pressedStateBgColor = typedArray.getColor(R.styleable.SublimePicker_spOverflowIconPressedBgColor,
                    SUtils.COLOR_TEXT_PRIMARY);
        } finally {
            typedArray.recycle();
        }

        SUtils.setImageTintList(ivHeaderDateReset, ColorStateList.valueOf(iconColor));
        SUtils.setViewBackground(ivHeaderDateReset, SUtils.createOverflowButtonBg(pressedStateBgColor));

        ColorStateList headerTextColor
                = a.getColorStateList(R.styleable.SublimeDatePicker_spHeaderTextColor);

        if (headerTextColor == null) {
            headerTextColor = TextColorHelper.resolveMaterialHeaderTextColor();
        }

        if (headerTextColor != null) {
            mHeaderYear.setTextColor(headerTextColor);
            mHeaderMonthDay.setTextColor(headerTextColor);
        }

        // Set up header background, if available.
        if (SUtils.isApi_22_OrHigher()) {
            if (a.hasValueOrEmpty(R.styleable.SublimeDatePicker_spHeaderBackground)) {
                SUtils.setViewBackground(header,
                        a.getDrawable(R.styleable.SublimeDatePicker_spHeaderBackground));
            }
        } else {
            if (a.hasValue(R.styleable.SublimeDatePicker_spHeaderBackground)) {
                SUtils.setViewBackground(header, a.getDrawable(R.styleable.SublimeDatePicker_spHeaderBackground));
            }
        }

        int firstDayOfWeek = a.getInt(R.styleable.SublimeDatePicker_spFirstDayOfWeek,
                Calendar.getInstance(mCurrentLocale).getFirstDayOfWeek());

        final String minDate = a.getString(R.styleable.SublimeDatePicker_spMinDate);
        final String maxDate = a.getString(R.styleable.SublimeDatePicker_spMaxDate);

        // Set up min and max dates.
        LocalDate tempDate;

        try {
            tempDate = SUtils.parseDate(minDate);
        } catch (ParseException e) {
            tempDate = new LocalDate(DEFAULT_START_YEAR, Calendar.JANUARY + 1, 1);
        }

        final LocalDate minLocalDate = tempDate;

        try {
            tempDate = SUtils.parseDate(maxDate);
        } catch (ParseException e) {
            tempDate = new LocalDate(DEFAULT_END_YEAR, Calendar.DECEMBER + 1, 31);
        }

        final LocalDate maxLocalDate = tempDate;

        if (maxLocalDate.isBefore(minLocalDate)) {
            throw new IllegalArgumentException("maxDate must be >= minDate");
        }

        final long setDateMillis = SUtils.constrain(
                System.currentTimeMillis(), minLocalDate.toDate().getTime(), maxLocalDate.toDate().getTime());

        mMinDate = minLocalDate;
        mMaxDate = maxLocalDate;
        mCurrentDate.setTimeInMillis(setDateMillis);

        a.recycle();

        // Set up picker container.
        mAnimator = (ViewAnimator) mContainer.findViewById(R.id.animator);

        // Set up day picker view.
        mDayPickerView = (DayPickerView) mAnimator.findViewById(R.id.date_picker_day_picker);
        setFirstDayOfWeek(firstDayOfWeek);
        mDayPickerView.setMinDate(mMinDate);
        mDayPickerView.setMaxDate(mMaxDate);
        mDayPickerView.setDate(mCurrentDate);
        mDayPickerView.setProxyDaySelectionEventListener(mProxyDaySelectionEventListener);

        // Set up year picker view.
        mYearPickerView = (YearPickerView) mAnimator.findViewById(R.id.date_picker_year_picker);
        mYearPickerView.setRange(mMinDate, mMaxDate);
        mYearPickerView.setOnYearSelectedListener(mOnYearSelectedListener);

        // Set up content descriptions.
        mSelectDay = res.getString(R.string.select_day);
        mSelectYear = res.getString(R.string.select_year);

        // Initialize for current locale. This also initializes the date, so no
        // need to call onDateChanged.
        onLocaleChanged(mCurrentLocale);

        setCurrentView(VIEW_MONTH_DAY);
    }

    /**
     * Listener called when the user selects a day in the day picker view.
     */
    private final DayPickerView.ProxyDaySelectionEventListener mProxyDaySelectionEventListener
            = new DayPickerView.ProxyDaySelectionEventListener() {
        @Override
        public void onDaySelected(DayPickerView view, LocalDate day) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "tvHeaderDateStart is activated? " + tvHeaderDateStart.isActivated());
                Log.i(TAG, "tvHeaderDateEnd is activated? " + tvHeaderDateEnd.isActivated());
            }

            boolean goToPosition = true;

            if (llHeaderDateRangeCont.getVisibility() == View.VISIBLE) {
                // We're in Range selection mode
                if (tvHeaderDateStart.isActivated()) {
                    if (SelectedDate.compareDates(day, mCurrentDate.getEndDate()) > 0) {
                        mCurrentDate = new SelectedDate(day);
                    } else {
                        goToPosition = false;
                        mCurrentDate = new SelectedDate(day, mCurrentDate.getEndDate());
                    }
                } else if (tvHeaderDateEnd.isActivated()) {
                    if (SelectedDate.compareDates(day, mCurrentDate.getStartDate()) < 0) {
                        mCurrentDate = new SelectedDate(day);
                    } else {
                        goToPosition = false;
                        mCurrentDate = new SelectedDate(mCurrentDate.getStartDate(), day);
                    }
                } else { // Should never happen
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "onDaySelected: Neither tvDateStart, nor tvDateEnd is activated");
                    }
                }
            } else {
                mCurrentDate = new SelectedDate(day);
            }

            onDateChanged(true, false, goToPosition);
        }

        @Override
        public void onDateRangeSelectionStarted(@NonNull SelectedDate selectedDate) {
            mCurrentDate = new SelectedDate(selectedDate);
            onDateChanged(false, false, false);
        }

        @Override
        public void onDateRangeSelectionEnded(@Nullable SelectedDate selectedDate) {
            if (selectedDate != null) {
                mCurrentDate = new SelectedDate(selectedDate);
                onDateChanged(false, false, false);
            }
        }

        @Override
        public void onDateRangeSelectionUpdated(@NonNull SelectedDate selectedDate) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "onDateRangeSelectionUpdated: " + selectedDate.toString());
            }

            mCurrentDate = new SelectedDate(selectedDate);
            onDateChanged(false, false, false);
        }
    };

    /**
     * Listener called when the user selects a year in the year picker view.
     */
    private final YearPickerView.OnYearSelectedListener mOnYearSelectedListener
            = new YearPickerView.OnYearSelectedListener() {
        @Override
        public void onYearChanged(YearPickerView view, int year) {
            // If the newly selected month / year does not contain the
            // currently selected day number, change the selected day number
            // to the last day of the selected month or year.
            // e.g. Switching from Mar to Apr when Mar 31 is selected -> Apr 30
            // e.g. Switching from 2012 to 2013 when Feb 29, 2012 is selected -> Feb 28, 2013
            final int day = mCurrentDate.getStartDate().getDayOfMonth();
            final int month = mCurrentDate.getStartDate().getMonthOfYear();
            final int daysInMonth = SUtils.getDaysInMonth(month - 1, year);
            if (day > daysInMonth) {
                mCurrentDate.set(DateTimeFieldType.dayOfMonth(), daysInMonth);
            }

            mCurrentDate.set(DateTimeFieldType.year(), year);
            onDateChanged(true, true, true);

            // Automatically switch to day picker.
            setCurrentView(VIEW_MONTH_DAY);
        }
    };

    /**
     * Listener called when the user clicks on a header item.
     */
    private final OnClickListener mOnHeaderClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            SUtils.vibrateForDatePicker(DatePicker.this);

            if (v.getId() == R.id.date_picker_header_year) {
                setCurrentView(VIEW_YEAR);
            } else if (v.getId() == R.id.date_picker_header_date) {
                setCurrentView(VIEW_MONTH_DAY);
            } else if (v.getId() == R.id.tv_header_date_start) {
                mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_START;
                tvHeaderDateStart.setActivated(true);
                tvHeaderDateEnd.setActivated(false);
            } else if (v.getId() == R.id.tv_header_date_end) {
                mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_END;
                tvHeaderDateStart.setActivated(false);
                tvHeaderDateEnd.setActivated(true);
            } else if (v.getId() == R.id.iv_header_date_reset) {
                mCurrentDate = new SelectedDate(mCurrentDate.getStartDate());
                onDateChanged(true, false, true);
            }
        }
    };

    private void onLocaleChanged(Locale locale) {
        final TextView headerYear = mHeaderYear;
        if (headerYear == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return;
        }

        // Update the date formatter.
        String datePattern;

        if (SUtils.isApi_18_OrHigher()) {
            datePattern = android.text.format.DateFormat.getBestDateTimePattern(locale, "EMMMd");
        } else {
            datePattern = DateTimePatternHelper.getBestDateTimePattern(locale, DateTimePatternHelper.PATTERN_EMMMd);
        }

        mMonthDayFormat = DateTimeFormat.forPattern(datePattern);
        mYearFormat = DateTimeFormat.forPattern("y");

        // Update the header text.
        onCurrentDateChanged(false);
    }

    private void onCurrentDateChanged(boolean announce) {
        if (mHeaderYear == null) {
            // Abort, we haven't initialized yet. This method will get called
            // again later after everything has been set up.
            return;
        }

        final String year = mCurrentDate.getStartDate().toString(mYearFormat);
        mHeaderYear.setText(year);

        final String monthDay = mCurrentDate.getStartDate().toString(mMonthDayFormat);
        mHeaderMonthDay.setText(monthDay);

        final String yearStrStart = mCurrentDate.getStartDate().toString(mYearFormat);
        final String monthDayStrStart = mCurrentDate.getStartDate().toString(mMonthDayFormat);
        final String dateStrStart = yearStrStart + "\n" + monthDayStrStart;

        final String yearStrEnd = mCurrentDate.getEndDate().toString(mYearFormat);
        final String monthDayStrEnd = mCurrentDate.getEndDate().toString(mMonthDayFormat);
        final String dateStrEnd = yearStrEnd + "\n" + monthDayStrEnd;

        SpannableString spDateStart = new SpannableString(dateStrStart);
        // If textSize is 34dp for land, use 0.47f
        //spDateStart.setSpan(new RelativeSizeSpan(mIsInLandscapeMode ? 0.47f : 0.7f),
        spDateStart.setSpan(new RelativeSizeSpan(0.7f),
                0, yearStrStart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannableString spDateEnd = new SpannableString(dateStrEnd);
        spDateEnd.setSpan(new RelativeSizeSpan(0.7f),
                0, yearStrEnd.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // API <= 16
        if (!mIsInLandscapeMode && !SUtils.isApi_17_OrHigher()) {
            spDateEnd.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE),
                    0, dateStrEnd.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvHeaderDateStart.setText(spDateStart);
        tvHeaderDateEnd.setText(spDateEnd);

        // TODO: This should use live regions.
        if (announce) {
            final long millis = mCurrentDate.getStartDate().toDate().getTime();
            final int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR;
            final String fullDateText = DateUtils.formatDateTime(mContext, millis, flags);
            AccessibilityUtils.makeAnnouncement(mAnimator, fullDateText);
        }
    }

    private void setCurrentView(int viewIndex) {
        switch (viewIndex) {
            case VIEW_MONTH_DAY:
                mDayPickerView.setDate(mCurrentDate);

                if (mCurrentDate.getType() == SelectedDate.Type.SINGLE) {
                    switchToSingleDateView();
                } else if (mCurrentDate.getType() == SelectedDate.Type.RANGE) {
                    switchToDateRangeView();
                }

                if (mCurrentView != viewIndex) {
                    if (mAnimator.getDisplayedChild() != VIEW_MONTH_DAY) {
                        mAnimator.setDisplayedChild(VIEW_MONTH_DAY);
                    }
                    mCurrentView = viewIndex;
                }

                AccessibilityUtils.makeAnnouncement(mAnimator, mSelectDay);
                break;
            case VIEW_YEAR:
                if (mCurrentView != viewIndex) {
                    mHeaderMonthDay.setActivated(false);
                    mHeaderYear.setActivated(true);
                    mAnimator.setDisplayedChild(VIEW_YEAR);
                    mCurrentView = viewIndex;
                }

                AccessibilityUtils.makeAnnouncement(mAnimator, mSelectYear);
                break;
        }
    }

    /**
     * Initialize the state. If the provided values designate an inconsistent
     * date the values are normalized before updating the spinners.
     *
     * @param selectedDate   The initial date or date range.
     * @param pickerType type of date range selection
     * @param callback       How user is notified date is changed by
     *                       user, can be null.
     */
    //public void init(int year, int monthOfYear, int dayOfMonth, boolean canPickRange,
    public void init(SelectedDate selectedDate, Options.PickerType pickerType,
                     OnDateChangedListener callback) {
        mCurrentDate = new SelectedDate(selectedDate);
        mPickerType = pickerType;

        mDayPickerView.setCanPickRange(pickerType == Options.PickerType.BOTH);
        mDateChangedListener = callback;

        onDateChanged(false, false, true);
    }

    /**
     * Update the current date.
     *
     * @param year       The year.
     * @param month      The month which is <strong>starting from zero</strong>.
     * @param dayOfMonth The day of the month.
     */
    @SuppressWarnings("unused")
    public void updateDate(int year, int month, int dayOfMonth) {
        mCurrentDate.set(DateTimeFieldType.year(), year);
        mCurrentDate.set(DateTimeFieldType.monthOfYear(), month);
        mCurrentDate.set(DateTimeFieldType.dayOfMonth(), dayOfMonth);

        onDateChanged(false, true, true);
    }

    // callbackToClient is useless for now & gives us an unnecessary round-trip
    // by calling init(...)
    private void onDateChanged(boolean fromUser, boolean callbackToClient, boolean goToPosition) {
        final int year = mCurrentDate.getStartDate().getYear();

        if (callbackToClient && mDateChangedListener != null) {
            mDateChangedListener.onDateChanged(this, mCurrentDate);
        }

        updateHeaderViews();

        mDayPickerView.setDate(new SelectedDate(mCurrentDate), false, goToPosition);

        if (mCurrentDate.getType() == SelectedDate.Type.SINGLE) {
            mYearPickerView.setYear(year);
        }

        onCurrentDateChanged(fromUser);

        if (fromUser) {
            SUtils.vibrateForDatePicker(DatePicker.this);
        }
    }

    private void updateHeaderViews() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "updateHeaderViews(): First Date: "
                    + mCurrentDate.getStartDate().toString()
                    + " Second Date: "
                    + mCurrentDate.getEndDate().toString());
        }

        switch (mPickerType) {
            default:
            case SINGLE:
                switchToSingleDateView();
                break;
            case RANGE:
                switchToDateRangeView();
                break;
            case BOTH:
                if (mCurrentDate.getType() == SelectedDate.Type.SINGLE) {
                    switchToSingleDateView();
                } else if (mCurrentDate.getType() == SelectedDate.Type.RANGE) {
                    switchToDateRangeView();
                }
                break;
        }
    }

    private void switchToSingleDateView() {
        mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_NONE;

        ivHeaderDateReset.setVisibility(View.GONE);
        llHeaderDateRangeCont.setVisibility(View.INVISIBLE);
        llHeaderDateSingleCont.setVisibility(View.VISIBLE);

        mHeaderMonthDay.setActivated(true);
        mHeaderYear.setActivated(false);
    }

    private void switchToDateRangeView() {
        if (mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_NONE) {
            mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_START;
        }

        llHeaderDateSingleCont.setVisibility(View.INVISIBLE);
        ivHeaderDateReset.setVisibility(View.VISIBLE);
        llHeaderDateRangeCont.setVisibility(View.VISIBLE);

        tvHeaderDateStart.setActivated(mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_START);
        tvHeaderDateEnd.setActivated(mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_END);
    }

    public SelectedDate getSelectedDate() {
        return new SelectedDate(mCurrentDate);
    }

    /**
     * Sets the minimal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     *
     * @param minDate The minimal supported date.
     */
    public void setMinDate(long minDate) {
        mTempDate = new DateTime(minDate).toLocalDate();
        if (mTempDate.getYear() == mMinDate.getYear()
                && mTempDate.getDayOfYear() != mMinDate.getDayOfYear()) {
            return;
        }
        if (mCurrentDate.getStartDate().isBefore(mTempDate)) {
            mCurrentDate.setStartDate(mTempDate);
            onDateChanged(false, true, true);
        }
        mMinDate = mTempDate;
        mDayPickerView.setMinDate(mMinDate);
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }

    /**
     * Gets the minimal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     * Note: The default minimal date is 01/01/1900.
     *
     * @return The minimal supported date.
     */
    @SuppressWarnings("unused")
    public LocalDate getMinDate() {
        return mMinDate;
    }

    /**
     * Sets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     *
     * @param maxDate The maximal supported date.
     */
    public void setMaxDate(long maxDate) {
        mTempDate = new DateTime(maxDate).toLocalDate();
        if (mTempDate.getYear() == mMaxDate.getYear()
                && mTempDate.getDayOfYear() != mMaxDate.getDayOfWeek()) {
            return;
        }
        if (mCurrentDate.getEndDate().isAfter(mTempDate)) {
            mCurrentDate.setEndDate(mTempDate);
            onDateChanged(false, true, true);
        }
        mMaxDate = mTempDate;
        mDayPickerView.setMaxDate(mMaxDate);
        mYearPickerView.setRange(mMinDate, mMaxDate);
    }

    /**
     * Gets the maximal date supported by this {@link DatePicker} in
     * milliseconds since January 1, 1970 00:00:00 in
     * {@link java.util.TimeZone#getDefault()} time zone.
     * Note: The default maximal date is 12/31/2100.
     *
     * @return The maximal supported date.
     */
    @SuppressWarnings("unused")
    public LocalDate getMaxDate() {
        return mMaxDate;
    }

    public void setFirstDayOfWeek(int firstDayOfWeek) {
        if (firstDayOfWeek < Calendar.SUNDAY || firstDayOfWeek > Calendar.SATURDAY) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Provided `firstDayOfWeek` is invalid - it must be between 1 and 7. " +
                        "Given value: " + firstDayOfWeek + " Picker will use the default value for the given locale.");
            }

            firstDayOfWeek = Calendar.getInstance(mCurrentLocale).getFirstDayOfWeek();
        }

        mFirstDayOfWeek = firstDayOfWeek;
        mDayPickerView.setFirstDayOfWeek(firstDayOfWeek);
    }

    public int getFirstDayOfWeek() {
        return mFirstDayOfWeek;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isEnabled() == enabled) {
            return;
        }

        mContainer.setEnabled(enabled);
        mDayPickerView.setEnabled(enabled);
        mYearPickerView.setEnabled(enabled);
        mHeaderYear.setEnabled(enabled);
        mHeaderMonthDay.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return mContainer.isEnabled();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        setCurrentLocale(newConfig.locale);
    }

    private void setCurrentLocale(Locale locale) {
        if (!locale.equals(mCurrentLocale)) {
            mCurrentLocale = locale;
            onLocaleChanged(locale);
        }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        int listPosition = -1;
        int listPositionOffset = -1;

        if (mCurrentView == VIEW_MONTH_DAY) {
            listPosition = mDayPickerView.getMostVisiblePosition();
        } else if (mCurrentView == VIEW_YEAR) {
            listPosition = mYearPickerView.getFirstVisiblePosition();
            listPositionOffset = mYearPickerView.getFirstPositionOffset();
        }

        return new SavedState(superState, mCurrentDate, mMinDate.toDate().getTime(),
                mMaxDate.toDate().getTime(), mCurrentView, listPosition,
                listPositionOffset, mCurrentlyActivatedRangeItem);
    }

    @SuppressLint("NewApi")
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        LocalDate startDate = new LocalDate(ss.getSelectedYearStart(), ss.getSelectedMonthStart(), ss.getSelectedDayStart());
        LocalDate endDate = new LocalDate(ss.getSelectedYearEnd(), ss.getSelectedMonthEnd(), ss.getSelectedDayEnd());

        mCurrentDate.setStartDate(startDate);
        mCurrentDate.setEndDate(endDate);

        int currentView = ss.getCurrentView();
        mMinDate = new DateTime(ss.getMinDate()).toLocalDate();
        mMaxDate = new DateTime(ss.getMaxDate()).toLocalDate();

        mCurrentlyActivatedRangeItem = ss.getCurrentlyActivatedRangeItem();

        onCurrentDateChanged(false);
        setCurrentView(currentView);

        final int listPosition = ss.getListPosition();

        if (listPosition != -1) {
            if (currentView == VIEW_MONTH_DAY) {
                mDayPickerView.setPosition(listPosition);
            } else if (currentView == VIEW_YEAR) {
                final int listPositionOffset = ss.getListPositionOffset();
                mYearPickerView.setSelectionFromTop(listPosition, listPositionOffset);
            }
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        event.getText().add(mCurrentDate.getStartDate().toString());
    }

    public CharSequence getAccessibilityClassName() {
        return DatePicker.class.getName();
    }

    public void setValidationCallback(DatePickerValidationCallback callback) {
        mValidationCallback = callback;
    }

    @SuppressWarnings("unused")
    protected void onValidationChanged(boolean valid) {
        if (mValidationCallback != null) {
            mValidationCallback.onDatePickerValidationChanged(valid);
        }
    }

    /**
     * A callback interface for updating input validity when the date picker
     * when included into a dialog.
     */
    public interface DatePickerValidationCallback {
        void onDatePickerValidationChanged(boolean valid);
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends BaseSavedState {

        private final int mSelectedYearStart;
        private final int mSelectedMonthStart;
        private final int mSelectedDayStart;
        private final int mSelectedYearEnd;
        private final int mSelectedMonthEnd;
        private final int mSelectedDayEnd;
        private final long mMinDate;
        private final long mMaxDate;
        private final int mCurrentView;
        private final int mListPosition;
        private final int mListPositionOffset;
        private final int ssCurrentlyActivatedRangeItem;

        /**
         * Constructor called from {@link DatePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, SelectedDate selectedDate,
                           long minDate, long maxDate, int currentView, int listPosition,
                           int listPositionOffset, int currentlyActivatedRangeItem) {
            super(superState);
            mSelectedYearStart = selectedDate.getStartDate().getYear();
            mSelectedMonthStart = selectedDate.getStartDate().getMonthOfYear();
            mSelectedDayStart = selectedDate.getStartDate().getDayOfMonth();
            mSelectedYearEnd = selectedDate.getEndDate().getYear();
            mSelectedMonthEnd = selectedDate.getEndDate().getMonthOfYear();
            mSelectedDayEnd = selectedDate.getEndDate().getDayOfMonth();
            mMinDate = minDate;
            mMaxDate = maxDate;
            mCurrentView = currentView;
            mListPosition = listPosition;
            mListPositionOffset = listPositionOffset;
            ssCurrentlyActivatedRangeItem = currentlyActivatedRangeItem;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            mSelectedYearStart = in.readInt();
            mSelectedMonthStart = in.readInt();
            mSelectedDayStart = in.readInt();
            mSelectedYearEnd = in.readInt();
            mSelectedMonthEnd = in.readInt();
            mSelectedDayEnd = in.readInt();
            mMinDate = in.readLong();
            mMaxDate = in.readLong();
            mCurrentView = in.readInt();
            mListPosition = in.readInt();
            mListPositionOffset = in.readInt();
            ssCurrentlyActivatedRangeItem = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mSelectedYearStart);
            dest.writeInt(mSelectedMonthStart);
            dest.writeInt(mSelectedDayStart);
            dest.writeInt(mSelectedYearEnd);
            dest.writeInt(mSelectedMonthEnd);
            dest.writeInt(mSelectedDayEnd);
            dest.writeLong(mMinDate);
            dest.writeLong(mMaxDate);
            dest.writeInt(mCurrentView);
            dest.writeInt(mListPosition);
            dest.writeInt(mListPositionOffset);
            dest.writeInt(ssCurrentlyActivatedRangeItem);
        }

        public int getSelectedDayStart() {
            return mSelectedDayStart;
        }

        public int getSelectedMonthStart() {
            return mSelectedMonthStart;
        }

        public int getSelectedYearStart() {
            return mSelectedYearStart;
        }

        public int getSelectedDayEnd() {
            return mSelectedDayEnd;
        }

        public int getSelectedMonthEnd() {
            return mSelectedMonthEnd;
        }

        public int getSelectedYearEnd() {
            return mSelectedYearEnd;
        }

        public long getMinDate() {
            return mMinDate;
        }

        public long getMaxDate() {
            return mMaxDate;
        }

        public int getCurrentView() {
            return mCurrentView;
        }

        public int getListPosition() {
            return mListPosition;
        }

        public int getListPositionOffset() {
            return mListPositionOffset;
        }

        public int getCurrentlyActivatedRangeItem() {
            return ssCurrentlyActivatedRangeItem;
        }

        @SuppressWarnings("all")
        // suppress unused and hiding
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    /**
     * The callback used to indicate the user changed the date.
     */
    public interface OnDateChangedListener {

        /**
         * Called upon a date change.
         *
         * @param view         The view associated with this listener.
         * @param selectedDate The date that was set.
         */
        void onDateChanged(DatePicker view, SelectedDate selectedDate);
    }
}
