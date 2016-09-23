/*
 * Copyright 2015 Vikram Kakkar
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

package com.zpw.zpwtimepickerlib.helpers;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.zpw.zpwtimepickerlib.datepicker.SelectedDate;
import com.zpw.zpwtimepickerlib.datetimepicker.SelectedDateTime;
import com.zpw.zpwtimepickerlib.recurrencepicker.RecurrencePicker;
import com.zpw.zpwtimepickerlib.timepicker.SelectedTime;
import com.zpw.zpwtimepickerlib.utilities.SUtils;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.Calendar;
import java.util.Locale;

/**
 * Options to initialize 'SublimePicker'
 */
public class Options implements Parcelable {
    public enum Picker {DATE_PICKER, TIME_PICKER, REPEAT_OPTION_PICKER, INVALID}

    // 日期选择的展现类型
    public enum PickerType {
        SINGLE, RANGE, BOTH, INVALID
    }

    // make DatePicker available
    public final static int ACTIVATE_DATE_PICKER = 0x01;

    // make TimePicker available
    public final static int ACTIVATE_TIME_PICKER = 0x02;

    // make RecurrencePicker available
    public final static int ACTIVATE_RECURRENCE_PICKER = 0x04;

    private int mDisplayOptions =
            ACTIVATE_DATE_PICKER | ACTIVATE_TIME_PICKER | ACTIVATE_RECURRENCE_PICKER;

    // Date & Time params
    private int mStartYear = -1, mStartMonth = -1, mStartDayOfMonth = -1,
            mEndYear = -1, mEndMonth = -1, mEndDayOfMonth = -1,
            mStartHour = -1, mStartMinute = -1,
            mEndHour = -1, mEndMinute = -1;
    private long mMinDate = Long.MIN_VALUE, mMaxDate = Long.MIN_VALUE;
    private boolean mAnimateLayoutChanges, mIs24HourView = true;

    private RecurrencePicker.RecurrenceOption mRecurrenceOption
            = RecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT;
    private String mRecurrenceRule = "";

    // Defaults
    private Picker mPickerToShow = Picker.DATE_PICKER;

    // 根据mDisplayOptions的值会有不同的效果
    private PickerType mPickerType = PickerType.SINGLE;

    public Options() {
        // Nothing
    }

    private Options(Parcel in) {
        readFromParcel(in);
    }

    // Use 'LayoutTransition'
    @SuppressWarnings("unused")
    public Options setAnimateLayoutChanges(boolean animateLayoutChanges) {
        mAnimateLayoutChanges = animateLayoutChanges;
        return this;
    }

    public boolean animateLayoutChanges() {
        return mAnimateLayoutChanges;
    }

    // Set the Picker that will be shown
    // when 'SublimePicker' is displayed
    public Options setPickerToShow(Picker picker) {
        mPickerToShow = picker;
        return this;
    }

    private boolean isPickerActive(Picker picker) {
        switch (picker) {
            case DATE_PICKER:
                return isDatePickerActive();
            case TIME_PICKER:
                return isTimePickerActive();
            case REPEAT_OPTION_PICKER:
                return isRecurrencePickerActive();
        }

        return false;
    }

    public Picker getPickerToShow() {
        return mPickerToShow;
    }

    // Activate pickers
    public Options setDisplayOptions(int displayOptions) {
        if (!areValidDisplayOptions(displayOptions)) {
            throw new IllegalArgumentException("Invalid display options.");
        }

        mDisplayOptions = displayOptions;
        return this;
    }

    private boolean areValidDisplayOptions(int displayOptions) {
        int flags = ACTIVATE_DATE_PICKER | ACTIVATE_TIME_PICKER | ACTIVATE_RECURRENCE_PICKER;
        return (displayOptions & ~flags) == 0;
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public Options setDateParams(int year, int month, int dayOfMonth) {
        return setDateParams(year, month, dayOfMonth, year, month, dayOfMonth);
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public Options setDateParams(int startYear, int startMonth, int startDayOfMonth,
                                 int endYear, int endMonth, int endDayOfMonth) {
        mStartYear = startYear;
        mStartMonth = startMonth;
        mStartDayOfMonth = startDayOfMonth;

        mEndYear = endYear;
        mEndMonth = endMonth;
        mEndDayOfMonth = endDayOfMonth;

        return this;
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public Options setDateParams(@NonNull Calendar calendar) {
        return setDateParams(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public Options setDateParams(@NonNull Calendar startCal, @NonNull Calendar endCal) {
        return setDateParams(startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH),
                endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH),
                endCal.get(Calendar.DAY_OF_MONTH));
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public Options setDateParams(@NonNull LocalDate localDate) {
        return setDateParams(localDate.getYear(), localDate.getMonthOfYear(),
                localDate.getDayOfMonth(),
                localDate.getYear(), localDate.getMonthOfYear(),
                localDate.getDayOfMonth());
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public Options setDateParams(@NonNull LocalDate startLD, @NonNull LocalDate endLD) {
        return setDateParams(startLD.getYear(), startLD.getMonthOfYear(),
                startLD.getDayOfMonth(),
                endLD.getYear(), endLD.getMonthOfYear(),
                endLD.getDayOfMonth());
    }

    // Provide initial date parameters
    @SuppressWarnings("unused")
    public Options setDateParams(@NonNull SelectedDate selectedDate) {
        return setDateParams(selectedDate.getStartDate(), selectedDate.getEndDate());
    }


    // Set date range
    // Pass '-1L' for 'minDate'/'maxDate' for default
    @SuppressWarnings("unused")
    public Options setDateRange(long minDate, long maxDate) {
        mMinDate = minDate;
        mMaxDate = maxDate;
        return this;
    }

    // Provide initial time parameters
    @SuppressWarnings("unused")
    public Options setTimeParams(int hourOfDay, int minute, boolean is24HourView) {
        mStartHour = mEndHour = hourOfDay;
        mStartMinute = mEndMinute = minute;
        mIs24HourView = is24HourView;
        return this;
    }

    // Provide initial time parameters
    @SuppressWarnings("unused")
    public Options setTimeParams(int startHourOfDay, int startMinute, int endHourOfDay, int endMinute, boolean is24HourView) {
        mStartHour = startHourOfDay;
        mStartMinute = startMinute;
        mEndHour = endHourOfDay;
        mEndMinute = endMinute;
        mIs24HourView = is24HourView;
        return this;
    }

    // Provide initial time parameters
    @SuppressWarnings("unused")
    public Options setTimeParams(LocalTime localTime, boolean is24HourView) {
        mStartHour = mEndHour = localTime.getHourOfDay();
        mStartMinute = mEndMinute = localTime.getMinuteOfHour();
        mIs24HourView = is24HourView;
        return this;
    }

    // Provide initial time parameters
    @SuppressWarnings("unused")
    public Options setTimeParams(LocalTime startLT, LocalTime endLT, boolean is24HourView) {
        mStartHour = startLT.getHourOfDay();
        mStartMinute = startLT.getMinuteOfHour();
        mEndHour = endLT.getHourOfDay();
        mEndMinute = endLT.getMinuteOfHour();
        mIs24HourView = is24HourView;
        return this;
    }

    // Provide initial Recurrence-rule
    @SuppressWarnings("unused")
    public Options setRecurrenceParams(RecurrencePicker.RecurrenceOption recurrenceOption, String recurrenceRule) {

        // If passed recurrence option is null, take it as the does_not_repeat option.
        // If passed recurrence option is custom, but the passed recurrence rule is null/empty,
        // take it as the does_not_repeat option.
        // If passed recurrence option is not custom, nullify the recurrence rule.
        if (recurrenceOption == null
                || (recurrenceOption == RecurrencePicker.RecurrenceOption.CUSTOM
                && TextUtils.isEmpty(recurrenceRule))) {
            recurrenceOption = RecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT;
            recurrenceRule = null;
        } else if (recurrenceOption != RecurrencePicker.RecurrenceOption.CUSTOM) {
            recurrenceRule = null;
        }

        mRecurrenceOption = recurrenceOption;
        mRecurrenceRule = recurrenceRule;
        return this;
    }

    @SuppressWarnings("unused")
    public String getRecurrenceRule() {
        return mRecurrenceRule == null ?
                "" : mRecurrenceRule;
    }

    @SuppressWarnings("unused")
    public RecurrencePicker.RecurrenceOption getRecurrenceOption() {
        return mRecurrenceOption == null ?
                RecurrencePicker.RecurrenceOption.DOES_NOT_REPEAT : mRecurrenceOption;
    }

    public boolean isDatePickerActive() {
        return (mDisplayOptions & ACTIVATE_DATE_PICKER) == ACTIVATE_DATE_PICKER;
    }

    public boolean isTimePickerActive() {
        return (mDisplayOptions & ACTIVATE_TIME_PICKER) == ACTIVATE_TIME_PICKER;
    }

    public boolean isRecurrencePickerActive() {
        return (mDisplayOptions & ACTIVATE_RECURRENCE_PICKER) == ACTIVATE_RECURRENCE_PICKER;
    }

    public SelectedDate getDateParams() {
        LocalDate startCal = SUtils.getCalendarForLocale(null, Locale.getDefault());
        if (mStartYear == -1 || mStartMonth == -1 || mStartDayOfMonth == -1) {
            mStartYear = startCal.getYear();
            mStartMonth = startCal.getMonthOfYear();
            mStartDayOfMonth = startCal.getDayOfMonth();
        } else {
            startCal = new LocalDate(mStartYear, mStartMonth, mStartDayOfMonth);
        }

        LocalDate endCal = SUtils.getCalendarForLocale(null, Locale.getDefault());
        if (mEndYear == -1 || mEndMonth == -1 || mEndDayOfMonth == -1) {
            mEndYear = endCal.getYear();
            mEndMonth = endCal.getMonthOfYear();
            mEndDayOfMonth = endCal.getDayOfMonth();
        } else {
            endCal = new LocalDate(mEndYear, mEndMonth, mEndDayOfMonth);
        }

        return new SelectedDate(startCal, endCal);
    }

    public SelectedTime getTimeParams() {
        LocalTime startLt = LocalTime.now();
        if (mStartHour == -1 || mStartMinute == -1) {
            mStartHour = startLt.getHourOfDay();
            mStartMinute = startLt.getMinuteOfHour();
        } else {
            startLt = new LocalTime(mStartHour, mStartMinute);
        }

        LocalTime endLt = LocalTime.now();
        if (mEndHour == -1 || mEndMinute == -1) {
            mEndHour = endLt.getHourOfDay();
            mEndMinute = endLt.getMinuteOfHour();
        } else {
            endLt = new LocalTime(mEndHour, mEndMinute);
        }

        return new SelectedTime(startLt, endLt);
    }

    public SelectedDateTime getDateTimeParams() {
        DateTime startDT = DateTime.now();
        if (mStartYear == -1 || mStartMonth == -1 || mStartDayOfMonth == -1 || mStartHour == -1 || mEndMinute == -1) {
            mStartYear = startDT.getYear();
            mStartMonth = startDT.getMonthOfYear();
            mStartDayOfMonth = startDT.getDayOfMonth();
            mStartHour = startDT.getHourOfDay();
            mStartMinute = startDT.getMinuteOfHour();
        } else {
            startDT = new DateTime(mStartYear, mStartMonth, mStartDayOfMonth, mStartHour, mStartMinute);
        }

        DateTime endDT = DateTime.now();
        if (mEndYear == -1 || mEndMonth == -1 || mEndDayOfMonth == -1 || mEndHour == -1 || mEndMinute == -1) {
            mEndYear = endDT.getYear();
            mEndMonth = endDT.getMonthOfYear();
            mEndDayOfMonth = endDT.getDayOfMonth();
            mEndHour = endDT.getHourOfDay();
            mEndMinute = endDT.getMinuteOfHour();
        } else {
            endDT = new DateTime(mEndYear, mEndMonth, mEndDayOfMonth, mEndHour, mEndMinute);
        }

        return new SelectedDateTime(startDT, endDT);
    }

    public long[] getDateRange() {
        return new long[]{mMinDate, mMaxDate};
    }

    public boolean is24HourView() {
        return mIs24HourView;
    }

    // Verifies if the supplied options are valid
    public void verifyValidity() {
        if (mPickerToShow == null || mPickerToShow == Picker.INVALID) {
            throw new InvalidOptionsException("The picker set using setPickerToShow(Picker) " +
                    "cannot be null or Picker.INVALID.");
        }

        if (!isPickerActive(mPickerToShow)) {
            throw new InvalidOptionsException("The picker you have " +
                    "requested to show(" + mPickerToShow.name() + ") is not activated. " +
                    "Use setDisplayOptions(int) " +
                    "to activate it, or use an activated Picker with setPickerToShow(Picker).");
        }

        // TODO: Validation? mMinDate < mMaxDate
    }

    public Options setPickerType(PickerType type) {
        mPickerType = type;
        return this;
    }

    public PickerType getPickerType() {
        return mPickerType;
    }

    public PickerType getPickerTypeForDateTime() {
        if (isDatePickerActive() && isTimePickerActive()) {
            if (mPickerType == PickerType.INVALID) {
                return PickerType.SINGLE;
            } else {
                return mPickerType;
            }
        } else {
            return PickerType.INVALID;
        }
    }

    public PickerType getPickerTypeForDate() {
        if (isDatePickerActive()) {
            if (mPickerType == PickerType.INVALID) {
                return PickerType.SINGLE;
            } else {
                return mPickerType;
            }
        } else {
            return PickerType.INVALID;
        }
    }

    public PickerType getPickerTypeForTime() {
        if (isTimePickerActive()) {
            //如果日期和时间都可以选择,则时间选择只能是SINGLE,不支持不同日期有不同的时间段
            if (mPickerType == PickerType.INVALID || isDatePickerActive()) {
                return PickerType.SINGLE;
            } else if (mPickerType == PickerType.BOTH) {
                //现在不支持
                return PickerType.SINGLE;
            } else {
                return mPickerType;
            }
        } else {
            return PickerType.INVALID;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void readFromParcel(Parcel in) {
        mAnimateLayoutChanges = in.readByte() != 0;
        mPickerToShow = Picker.valueOf(in.readString());
        mDisplayOptions = in.readInt();
        mStartYear = in.readInt();
        mStartMonth = in.readInt();
        mStartDayOfMonth = in.readInt();
        mEndYear = in.readInt();
        mEndMonth = in.readInt();
        mEndDayOfMonth = in.readInt();
        mStartHour = in.readInt();
        mStartMinute = in.readInt();
        mEndHour = in.readInt();
        mEndMinute = in.readInt();
        mIs24HourView = in.readByte() != 0;
        mRecurrenceRule = in.readString();
        mPickerType = PickerType.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mAnimateLayoutChanges ? 1 : 0));
        dest.writeString(mPickerToShow.name());
        dest.writeInt(mDisplayOptions);
        dest.writeInt(mStartYear);
        dest.writeInt(mStartMonth);
        dest.writeInt(mStartDayOfMonth);
        dest.writeInt(mEndYear);
        dest.writeInt(mEndMonth);
        dest.writeInt(mEndDayOfMonth);
        dest.writeInt(mStartHour);
        dest.writeInt(mStartMinute);
        dest.writeInt(mEndHour);
        dest.writeInt(mEndMinute);
        dest.writeByte((byte) (mIs24HourView ? 1 : 0));
        dest.writeString(mRecurrenceRule);
        dest.writeString(mPickerType.name());
    }

    public static final Creator<Options> CREATOR = new Creator<Options>() {
        public Options createFromParcel(Parcel in) {
            return new Options(in);
        }

        public Options[] newArray(int size) {
            return new Options[size];
        }
    };

    // Thrown if supplied 'Options' are not valid
    public class InvalidOptionsException extends RuntimeException {
        public InvalidOptionsException(String detailMessage) {
            super(detailMessage);
        }
    }
}
