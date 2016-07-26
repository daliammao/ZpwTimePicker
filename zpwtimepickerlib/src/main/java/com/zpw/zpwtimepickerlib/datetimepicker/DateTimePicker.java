/*
 * Copyright 2016 Vikram Kakkar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zpw.zpwtimepickerlib.datetimepicker;

import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.zpw.zpwtimepickerlib.R;
import com.zpw.zpwtimepickerlib.common.ButtonHandler;
import com.zpw.zpwtimepickerlib.datepicker.DatePicker;
import com.zpw.zpwtimepickerlib.datepicker.SelectedDate;
import com.zpw.zpwtimepickerlib.drawables.OverflowDrawable;
import com.zpw.zpwtimepickerlib.helpers.Options;
import com.zpw.zpwtimepickerlib.helpers.ListenerAdapter;
import com.zpw.zpwtimepickerlib.recurrencepicker.RecurrencePicker;
import com.zpw.zpwtimepickerlib.timepicker.TimePicker;
import com.zpw.zpwtimepickerlib.utilities.SUtils;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A customizable view that provisions picking of a date,
 * time and recurrence option, all from a single user-interface.
 * You can also view 'SublimePicker' as a collection of
 * material-styled (API 23) DatePicker, TimePicker
 * and RecurrencePicker, backported to API 14.
 * You can opt for any combination of these three Pickers.
 */
public class DateTimePicker extends FrameLayout implements DatePicker.OnDateChangedListener,
        DatePicker.DatePickerValidationCallback,
        TimePicker.TimePickerValidationCallback {
    private static final String TAG = DateTimePicker.class.getSimpleName();

    // Used for formatting date range
    private static final long MONTH_IN_MILLIS = DateUtils.YEAR_IN_MILLIS / 12;

    // Container for 'SublimeDatePicker' & 'SublimeTimePicker'
    private LinearLayout llMainContentHolder;

    // For access to 'SublimeRecurrencePicker'
    private ImageView ivRecurrenceOptionsDP, ivRecurrenceOptionsTP;

    // Recurrence picker options
    private RecurrencePicker mSublimeRecurrencePicker;
    private RecurrencePicker.RecurrenceOption mCurrentRecurrenceOption
            = RecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT;
    private String mRecurrenceRule;

    // Keeps track which picker is showing
    private Options.Picker mCurrentPicker, mHiddenPicker;

    // Date picker
    private DatePicker mDatePicker;

    // Time picker
    private TimePicker mTimePicker;

    // Callback
    private ListenerAdapter mListener;

    // Client-set options
    private Options mOptions;

    // Ok, cancel & switch button handler
    private ButtonHandler mButtonLayout;

    // Flags set based on client-set options {Options}
    private boolean mDatePickerValid = true, mTimePickerValid = true,
            mDatePickerEnabled, mTimePickerEnabled, mRecurrencePickerEnabled,
            mDatePickerSyncStateCalled;

    // Used if listener returns
    // null/invalid(zero-length, empty) string
    private DateFormat mDefaultDateFormatter, mDefaultTimeFormatter;

    // Listener for recurrence picker
    private final RecurrencePicker.OnRepeatOptionSetListener mRepeatOptionSetListener = new RecurrencePicker.OnRepeatOptionSetListener() {
        @Override
        public void onRepeatOptionSet(RecurrencePicker.RecurrenceOption option, String recurrenceRule) {
            mCurrentRecurrenceOption = option;
            mRecurrenceRule = recurrenceRule;
            onDone();
        }

        @Override
        public void onDone() {
            if (mDatePickerEnabled || mTimePickerEnabled) {
                updateCurrentPicker();
                updateDisplay();
            } else { /* No other picker is activated. Dismiss. */
                mButtonLayoutCallback.onOkay();
            }
        }
    };

    // Handle ok, cancel & switch button click events
    private final ButtonHandler.Callback mButtonLayoutCallback = new ButtonHandler.Callback() {
        @Override
        public void onOkay() {
            SelectedDateTime selectedDate = null;

            if (mDatePickerEnabled) {
                selectedDate = mDatePicker.getSelectedDate();
            }

            int hour = -1, minute = -1;

            if (mTimePickerEnabled) {
                hour = mTimePicker.getCurrentHour();
                minute = mTimePicker.getCurrentMinute();
            }

            RecurrencePicker.RecurrenceOption recurrenceOption
                    = RecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT;
            String recurrenceRule = null;

            if (mRecurrencePickerEnabled) {
                recurrenceOption = mCurrentRecurrenceOption;

                if (recurrenceOption == RecurrencePicker.RecurrenceOption.CUSTOM) {
                    recurrenceRule = mRecurrenceRule;
                }
            }

            mListener.onDateTimeRecurrenceSet(DateTimePicker.this,
                    // DatePicker
                    selectedDate,
                    // TimePicker
                    hour, minute,
                    // RecurrencePicker
                    recurrenceOption, recurrenceRule);
        }

        @Override
        public void onCancel() {
            mListener.onCancelled();
        }

        @Override
        public void onSwitch() {
            mCurrentPicker = mCurrentPicker == Options.Picker.DATE_PICKER ?
                    Options.Picker.TIME_PICKER
                    : Options.Picker.DATE_PICKER;

            updateDisplay();
        }
    };

    public DateTimePicker(Context context) {
        this(context, null);
    }

    public DateTimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.sublimePickerStyle);
    }

    public DateTimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(createThemeWrapper(context), attrs, defStyleAttr);
        initializeLayout();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DateTimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(createThemeWrapper(context), attrs, defStyleAttr, defStyleRes);
        initializeLayout();
    }

    private static ContextThemeWrapper createThemeWrapper(Context context) {
        final TypedArray forParent = context.obtainStyledAttributes(
                new int[]{R.attr.sublimePickerStyle});
        int parentStyle = forParent.getResourceId(0, R.style.SublimePickerStyleLight);
        forParent.recycle();

        return new ContextThemeWrapper(context, parentStyle);
    }

    private void initializeLayout() {
        Context context = getContext();
        SUtils.initializeResources(context);

        LayoutInflater.from(context).inflate(R.layout.sublime_picker_view_layout,
                this, true);

        mDefaultDateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM,
                Locale.getDefault());
        mDefaultTimeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT,
                Locale.getDefault());
        mDefaultTimeFormatter.setTimeZone(TimeZone.getTimeZone("GMT+0"));

        llMainContentHolder = (LinearLayout) findViewById(R.id.llMainContentHolder);
        mButtonLayout = new ButtonHandler(this);
        initializeRecurrencePickerSwitch();

        mDatePicker = (DatePicker) findViewById(R.id.datePicker);
        mTimePicker = (TimePicker) findViewById(R.id.timePicker);
        mSublimeRecurrencePicker = (RecurrencePicker)
                findViewById(R.id.repeat_option_picker);
    }

    public void initializePicker(Options options, ListenerAdapter listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null.");
        }

        if (options != null) {
            options.verifyValidity();
        } else {
            options = new Options();
        }

        mOptions = options;
        mListener = listener;

        processOptions();
        updateDisplay();
    }

    // Called before 'RecurrencePicker' is shown
    private void updateHiddenPicker() {
        if (mDatePickerEnabled && mTimePickerEnabled) {
            mHiddenPicker = mDatePicker.getVisibility() == View.VISIBLE ?
                    Options.Picker.DATE_PICKER : Options.Picker.TIME_PICKER;
        } else if (mDatePickerEnabled) {
            mHiddenPicker = Options.Picker.DATE_PICKER;
        } else if (mTimePickerEnabled) {
            mHiddenPicker = Options.Picker.TIME_PICKER;
        } else {
            mHiddenPicker = Options.Picker.INVALID;
        }
    }

    // 'mHiddenPicker' retains the Picker that was active
    // before 'RecurrencePicker' was shown. On its dismissal,
    // we have an option to show either 'DatePicker' or 'TimePicker'.
    // 'mHiddenPicker' helps identify the correct option.
    private void updateCurrentPicker() {
        if (mHiddenPicker != Options.Picker.INVALID) {
            mCurrentPicker = mHiddenPicker;
        } else {
            throw new RuntimeException("Logic issue: No valid option for mCurrentPicker");
        }
    }

    private void updateDisplay() {
        CharSequence switchButtonText;

        if (mCurrentPicker == Options.Picker.DATE_PICKER) {

            if (mTimePickerEnabled) {
                mTimePicker.setVisibility(View.GONE);
            }

            if (mRecurrencePickerEnabled) {
                mSublimeRecurrencePicker.setVisibility(View.GONE);
            }

            mDatePicker.setVisibility(View.VISIBLE);
            llMainContentHolder.setVisibility(View.VISIBLE);

            if (mButtonLayout.isSwitcherButtonEnabled()) {
                Date toFormat = new Date(mTimePicker.getCurrentHour() * DateUtils.HOUR_IN_MILLIS
                        + mTimePicker.getCurrentMinute() * DateUtils.MINUTE_IN_MILLIS);

                switchButtonText = mListener.formatTime(toFormat);

                if (TextUtils.isEmpty(switchButtonText)) {
                    switchButtonText = mDefaultTimeFormatter.format(toFormat);
                }

                mButtonLayout.updateSwitcherText(Options.Picker.DATE_PICKER, switchButtonText);
            }

            if (!mDatePickerSyncStateCalled) {
                mDatePickerSyncStateCalled = true;
            }
        } else if (mCurrentPicker == Options.Picker.TIME_PICKER) {
            if (mDatePickerEnabled) {
                mDatePicker.setVisibility(View.GONE);
            }

            if (mRecurrencePickerEnabled) {
                mSublimeRecurrencePicker.setVisibility(View.GONE);
            }

            mTimePicker.setVisibility(View.VISIBLE);
            llMainContentHolder.setVisibility(View.VISIBLE);

            if (mButtonLayout.isSwitcherButtonEnabled()) {
                SelectedDate selectedDate = mDatePicker.getSelectedDate();
                switchButtonText = mListener.formatDate(selectedDate);

                if (TextUtils.isEmpty(switchButtonText)) {
                    if (selectedDate.getType() == SelectedDate.Type.SINGLE) {
                        Date toFormat = new Date(mDatePicker.getSelectedDateInMillis());
                        switchButtonText = mDefaultDateFormatter.format(toFormat);
                    } else if (selectedDate.getType() == SelectedDate.Type.RANGE) {
                        switchButtonText = formatDateRange(selectedDate);
                    }
                }

                mButtonLayout.updateSwitcherText(Options.Picker.TIME_PICKER, switchButtonText);
            }
        } else if (mCurrentPicker == Options.Picker.REPEAT_OPTION_PICKER) {
            updateHiddenPicker();
            mSublimeRecurrencePicker.updateView();

            if (mDatePickerEnabled || mTimePickerEnabled) {
                llMainContentHolder.setVisibility(View.GONE);
            }

            mSublimeRecurrencePicker.setVisibility(View.VISIBLE);
        }
    }

    private String formatDateRange(SelectedDate selectedDate) {
        Calendar startDate = selectedDate.getStartDate();
        Calendar endDate = selectedDate.getEndDate();

        startDate.set(Calendar.MILLISECOND, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.HOUR, 0);

        endDate.set(Calendar.MILLISECOND, 0);
        endDate.set(Calendar.SECOND, 0);
        endDate.set(Calendar.MINUTE, 0);
        endDate.set(Calendar.HOUR, 0);
        // Move to next day since we are nulling out the time fields
        endDate.add(Calendar.DAY_OF_MONTH, 1);

        float elapsedTime = endDate.getTimeInMillis() - startDate.getTimeInMillis();

        if (elapsedTime >= DateUtils.YEAR_IN_MILLIS) {
            final float years = elapsedTime / DateUtils.YEAR_IN_MILLIS;

            boolean roundUp = years - (int) years > 0.5f;
            final int yearsVal = roundUp ? (int) (years + 1) : (int) years;

            return "~" + yearsVal + " " + (yearsVal == 1 ? "year" : "years");
        } else if (elapsedTime >= MONTH_IN_MILLIS) {
            final float months = elapsedTime / MONTH_IN_MILLIS;

            boolean roundUp = months - (int) months > 0.5f;
            final int monthsVal = roundUp ? (int) (months + 1) : (int) months;

            return "~" + monthsVal + " " + (monthsVal == 1 ? "month" : "months");
        } else {
            final float days = elapsedTime / DateUtils.DAY_IN_MILLIS;

            boolean roundUp = days - (int) days > 0.5f;
            final int daysVal = roundUp ? (int) (days + 1) : (int) days;

            return "~" + daysVal + " " + (daysVal == 1 ? "day" : "days");
        }
    }

    private void initializeRecurrencePickerSwitch() {
        ivRecurrenceOptionsDP = (ImageView) findViewById(R.id.ivRecurrenceOptionsDP);
        ivRecurrenceOptionsTP = (ImageView) findViewById(R.id.ivRecurrenceOptionsTP);

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

        ivRecurrenceOptionsDP.setImageDrawable(
                new OverflowDrawable(getContext(), iconColor));
        SUtils.setViewBackground(ivRecurrenceOptionsDP,
                SUtils.createOverflowButtonBg(pressedStateBgColor));

        ivRecurrenceOptionsTP.setImageDrawable(
                new OverflowDrawable(getContext(), iconColor));
        SUtils.setViewBackground(ivRecurrenceOptionsTP,
                SUtils.createOverflowButtonBg(pressedStateBgColor));

        ivRecurrenceOptionsDP.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentPicker = Options.Picker.REPEAT_OPTION_PICKER;
                updateDisplay();
            }
        });

        ivRecurrenceOptionsTP.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentPicker = Options.Picker.REPEAT_OPTION_PICKER;
                updateDisplay();
            }
        });
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), mCurrentPicker, mHiddenPicker,
                mCurrentRecurrenceOption, mRecurrenceRule);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;

        mCurrentPicker = ss.getCurrentPicker();
        mCurrentRecurrenceOption = ss.getCurrentRepeatOption();
        mRecurrenceRule = ss.getRecurrenceRule();

        mHiddenPicker = ss.getHiddenPicker();
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        super.dispatchRestoreInstanceState(container);
        updateDisplay();
    }

    /**
     * Class for managing state storing/restoring.
     */
    private static class SavedState extends BaseSavedState {

        private final Options.Picker sCurrentPicker, sHiddenPicker /*One of DatePicker/TimePicker*/;
        private final RecurrencePicker.RecurrenceOption sCurrentRecurrenceOption;
        private final String sRecurrenceRule;

        /**
         * Constructor called from {@link DateTimePicker#onSaveInstanceState()}
         */
        private SavedState(Parcelable superState, Options.Picker currentPicker,
                           Options.Picker hiddenPicker,
                           RecurrencePicker.RecurrenceOption recurrenceOption,
                           String recurrenceRule) {
            super(superState);

            sCurrentPicker = currentPicker;
            sHiddenPicker = hiddenPicker;
            sCurrentRecurrenceOption = recurrenceOption;
            sRecurrenceRule = recurrenceRule;
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);

            sCurrentPicker = Options.Picker.valueOf(in.readString());
            sHiddenPicker = Options.Picker.valueOf(in.readString());
            sCurrentRecurrenceOption = RecurrencePicker.RecurrenceOption.valueOf(in.readString());
            sRecurrenceRule = in.readString();
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            dest.writeString(sCurrentPicker.name());
            dest.writeString(sHiddenPicker.name());
            dest.writeString(sCurrentRecurrenceOption.name());
            dest.writeString(sRecurrenceRule);
        }

        public Options.Picker getCurrentPicker() {
            return sCurrentPicker;
        }

        public Options.Picker getHiddenPicker() {
            return sHiddenPicker;
        }

        public RecurrencePicker.RecurrenceOption getCurrentRepeatOption() {
            return sCurrentRecurrenceOption;
        }

        public String getRecurrenceRule() {
            return sRecurrenceRule;
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

    private void processOptions() {
        if (mOptions.animateLayoutChanges()) {
            // Basic Layout Change Animation(s)
            LayoutTransition layoutTransition = new LayoutTransition();
            if (SUtils.isApi_16_OrHigher()) {
                layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
            }
            setLayoutTransition(layoutTransition);
        } else {
            setLayoutTransition(null);
        }

        mDatePickerEnabled = mOptions.isDatePickerActive();
        mTimePickerEnabled = mOptions.isTimePickerActive();
        mRecurrencePickerEnabled = mOptions.isRecurrencePickerActive();

        if (mDatePickerEnabled) {
            //int[] dateParams = mOptions.getDateParams();
            //mDatePicker.init(dateParams[0] /* year */,
            //        dateParams[1] /* month of year */,
            //        dateParams[2] /* day of month */,
            //        mOptions.canPickDateRange(),
            //        this);
            mDatePicker.init(mOptions.getDateParams(), mOptions.canPickDateRange(), this);

            long[] dateRange = mOptions.getDateRange();

            if (dateRange[0] /* min date */ != Long.MIN_VALUE) {
                mDatePicker.setMinDate(dateRange[0]);
            }

            if (dateRange[1] /* max date */ != Long.MIN_VALUE) {
                mDatePicker.setMaxDate(dateRange[1]);
            }

            mDatePicker.setValidationCallback(this);

            ivRecurrenceOptionsDP.setVisibility(mRecurrencePickerEnabled ?
                    View.VISIBLE : View.GONE);
        } else {
            llMainContentHolder.removeView(mDatePicker);
            mDatePicker = null;
        }

        if (mTimePickerEnabled) {
            int[] timeParams = mOptions.getTimeParams();
            mTimePicker.setCurrentHour(timeParams[0] /* hour of day */);
            mTimePicker.setCurrentMinute(timeParams[1] /* minute */);
            mTimePicker.setIs24HourView(mOptions.is24HourView());
            mTimePicker.setValidationCallback(this);

            ivRecurrenceOptionsTP.setVisibility(mRecurrencePickerEnabled ?
                    View.VISIBLE : View.GONE);
        } else {
            llMainContentHolder.removeView(mTimePicker);
            mTimePicker = null;
        }

        if (mDatePickerEnabled && mTimePickerEnabled) {
            mButtonLayout.applyOptions(true /* show switch button */,
                    mButtonLayoutCallback);
        } else {
            mButtonLayout.applyOptions(false /* hide switch button */,
                    mButtonLayoutCallback);
        }

        if (!mDatePickerEnabled && !mTimePickerEnabled) {
            removeView(llMainContentHolder);
            llMainContentHolder = null;
            mButtonLayout = null;
        }

        mCurrentRecurrenceOption = mOptions.getRecurrenceOption();
        mRecurrenceRule = mOptions.getRecurrenceRule();

        if (mRecurrencePickerEnabled) {
            Calendar cal = mDatePickerEnabled ?
                    mDatePicker.getSelectedDate().getStartDate()
                    : SUtils.getCalendarForLocale(null, Locale.getDefault());

            mSublimeRecurrencePicker.initializeData(mRepeatOptionSetListener,
                    mCurrentRecurrenceOption, mRecurrenceRule,
                    cal.getTimeInMillis());
        } else {
            removeView(mSublimeRecurrencePicker);
            mSublimeRecurrencePicker = null;
        }

        mCurrentPicker = mOptions.getPickerToShow();
        // Updated from 'updateDisplay()' when 'RecurrencePicker' is chosen
        mHiddenPicker = Options.Picker.INVALID;
    }

    private void reassessValidity() {
        mButtonLayout.updateValidity(mDatePickerValid && mTimePickerValid);
    }

    @Override
    public void onDateChanged(DatePicker view, SelectedDate selectedDate) {
        // TODO: Consider removing this propagation of date change event altogether
        //mDatePicker.init(selectedDate.getStartDate().get(Calendar.YEAR),
                //selectedDate.getStartDate().get(Calendar.MONTH),
                //selectedDate.getStartDate().get(Calendar.DAY_OF_MONTH),
                //mOptions.canPickDateRange(), this);
        mDatePicker.init(selectedDate, mOptions.canPickDateRange(), this);
    }

    @Override
    public void onDatePickerValidationChanged(boolean valid) {
        mDatePickerValid = valid;
        reassessValidity();
    }

    @Override
    public void onTimePickerValidationChanged(boolean valid) {
        mTimePickerValid = valid;
        reassessValidity();
    }
}
