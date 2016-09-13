package com.zpw.zpwtimepickerlib.timepicker;

import com.zpw.zpwtimepickerlib.datetimepicker.SelectedDateTime;

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * @author: zhoupengwei
 * @time: 16/8/31-上午11:40
 * @Email: zhoupengwei@qccr.com
 * @desc:
 */
public class SelectedTime {
    public enum Type {SINGLE, RANGE}

    private LocalTime mFirstTime, mSecondTime;

    public SelectedTime(LocalTime startTime, LocalTime endTime) {
        this.mFirstTime = startTime;
        this.mSecondTime = endTime;
    }

    public SelectedTime(LocalTime time) {
        mFirstTime = mSecondTime = time;
    }

    // TODO: Should be requiring Locale
    public SelectedTime(SelectedTime time) {
        mFirstTime = LocalTime.now();
        mSecondTime = LocalTime.now();

        if (time != null) {
            mFirstTime = time.getStartTime();
            mSecondTime = time.getEndTime();
        }
    }

    public void setTime(LocalTime time) {
        mFirstTime = time;
        mSecondTime = time;
    }

    public LocalTime getStartTime() {
        return compareTimes(mFirstTime, mSecondTime) == -1 ? mFirstTime : mSecondTime;
    }

    public void setStartTime(LocalTime startTime) {
        if (compareTimes(mFirstTime, mSecondTime) == -1) {
            mFirstTime = startTime;
        } else {
            mSecondTime = startTime;
        }
    }

    public LocalTime getEndTime() {
        return compareTimes(mFirstTime, mSecondTime) == 1 ? mFirstTime : mSecondTime;
    }

    public void setEndTime(LocalTime endTime) {
        if (compareTimes(mFirstTime, mSecondTime) == 1) {
            mFirstTime = endTime;
        } else {
            mSecondTime = endTime;
        }
    }

    public Type getType() {
        return compareTimes(mFirstTime, mSecondTime) == 0 ? Type.SINGLE : Type.RANGE;
    }

    // a & b should never be null, so don't perform a null check here.
    // Let the source of error identify itself.
    public static int compareTimes(LocalTime a, LocalTime b) {
        if (a.isBefore(b)) {
            return -1;
        } else if (a.isAfter(b)) {
            return 1;
        } else {
            return 0;
        }
    }

    public void setTimeInMillis(long timeInMillis) {
        mFirstTime = new DateTime(timeInMillis).toLocalTime();
        mSecondTime = new DateTime(timeInMillis).toLocalTime();
    }

    public void set(DateTimeFieldType field, int value) {
        mFirstTime = mFirstTime.withField(field, value);
        mSecondTime = mSecondTime.withField(field, value);
    }

    public void setStart(DateTimeFieldType field, int value){
        setStartTime(getStartTime().withField(field, value));
    }

    public void setEnd(DateTimeFieldType field, int value){
        setEndTime(getEndTime().withField(field, value));
    }

    public SelectedDateTime toSelectedDateTime(LocalDate firstDate, LocalDate secondDate) {
        return new SelectedDateTime(firstDate.toDateTime(mFirstTime), secondDate.toDateTime(mSecondTime));
    }

    @Override
    public String toString() {
        StringBuilder toReturn = new StringBuilder();

        if (mFirstTime != null) {
            toReturn.append(mFirstTime.toString());
            toReturn.append("\n");
        }

        if (mSecondTime != null) {
            toReturn.append(mSecondTime.toString());
        }

        return toReturn.toString();
    }
}
