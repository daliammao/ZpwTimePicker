package com.zpw.zpwtimepickerlib.datepicker;

import com.zpw.zpwtimepickerlib.datetimepicker.SelectedDateTime;

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

/**
 * Created by Admin on 25/02/2016.
 */
public class
SelectedDate {
    public enum Type {SINGLE, RANGE}

    private LocalDate mFirstDate, mSecondDate;

    public SelectedDate(LocalDate startDate, LocalDate endDate) {
        mFirstDate = startDate;
        mSecondDate = endDate;
    }

    public SelectedDate(LocalDate date) {
        mFirstDate = mSecondDate = date;
    }

    // TODO: Should be requiring Locale
    public SelectedDate(SelectedDate date) {
        mFirstDate = LocalDate.now();
        mSecondDate = LocalDate.now();

        if (date != null) {
            mFirstDate = date.getStartDate();
            mSecondDate = date.getEndDate();
        }
    }

    public void setDate(LocalDate date) {
        mFirstDate = date;
        mSecondDate = date;
    }

    public LocalDate getStartDate() {
        return compareDates(mFirstDate, mSecondDate) == -1 ? mFirstDate : mSecondDate;
    }

    public void setStartDate(LocalDate startDate) {
        if (compareDates(mFirstDate, mSecondDate) == -1) {
            mFirstDate = startDate;
        } else {
            mSecondDate = startDate;
        }
    }

    public LocalDate getEndDate() {
        return compareDates(mFirstDate, mSecondDate) == 1 ? mFirstDate : mSecondDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (compareDates(mFirstDate, mSecondDate) == 1) {
            mFirstDate = endDate;
        } else {
            mSecondDate = endDate;
        }
    }

    public Type getType() {
        return compareDates(mFirstDate, mSecondDate) == 0 ? Type.SINGLE : Type.RANGE;
    }

    // a & b should never be null, so don't perform a null check here.
    // Let the source of error identify itself.
    public static int compareDates(LocalDate a, LocalDate b) {
        if (a.isBefore(b)) {
            return -1;
        } else if (a.isAfter(b)) {
            return 1;
        } else {
            return 0;
        }
    }

    public void setTimeInMillis(long timeInMillis) {
        mFirstDate = new DateTime(timeInMillis).toLocalDate();
        mSecondDate = new DateTime(timeInMillis).toLocalDate();
    }

    public void set(DateTimeFieldType field, int value) {
        mFirstDate = mFirstDate.withField(field,value);
        mSecondDate = mSecondDate.withField(field,value);
    }

    public SelectedDateTime toSelectedDateTime(LocalTime firstTime, LocalTime secondTime) {
        return new SelectedDateTime(mFirstDate.toDateTime(firstTime), mSecondDate.toDateTime(secondTime));
    }

    @Override
    public String toString() {
        StringBuilder toReturn = new StringBuilder();

        if (mFirstDate != null) {
            toReturn.append(mFirstDate.toString());
            toReturn.append("\n");
        }

        if (mSecondDate != null) {
            toReturn.append(mSecondDate.toString());
        }

        return toReturn.toString();
    }
}
