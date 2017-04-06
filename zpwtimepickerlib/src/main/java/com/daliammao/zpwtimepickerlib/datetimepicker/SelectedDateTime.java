package com.daliammao.zpwtimepickerlib.datetimepicker;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import java.text.DateFormat;

/**
 * @author: zhoupengwei
 * @time: 16/7/26-下午3:40
 * @Email: 496946423@qq.com
 * @desc:
 */
public class SelectedDateTime {
    public enum Type {SINGLE, RANGE}

    private DateTime mFirstDateTime, mSecondDateTime;

    public SelectedDateTime(DateTime startDateTime, DateTime endDateTime) {
        mFirstDateTime = startDateTime;
        mSecondDateTime = endDateTime;
    }

    public SelectedDateTime(DateTime dateTime) {
        mFirstDateTime = mSecondDateTime = dateTime;
    }

    // TODO: Should be requiring Locale
    public SelectedDateTime(SelectedDateTime date) {
        mFirstDateTime = DateTime.now();
        mSecondDateTime = DateTime.now();

        if (date != null) {
            mFirstDateTime = new DateTime(date.getStartDateTime().getMillis());
            mSecondDateTime = new DateTime(date.getEndDateTime().getMillis());
        }
    }

    public void setDateTime(DateTime dateTime) {
        mFirstDateTime = dateTime;
        mSecondDateTime = dateTime;
    }

    public DateTime getStartDateTime() {
        return compareDates(mFirstDateTime, mSecondDateTime) == -1 ? mFirstDateTime : mSecondDateTime;
    }

    public DateTime getEndDateTime() {
        return compareDates(mFirstDateTime, mSecondDateTime) == 1 ? mFirstDateTime : mSecondDateTime;
    }

    public Type getType() {
        return compareDates(mFirstDateTime, mSecondDateTime) == 0 ? Type.SINGLE : Type.RANGE;
    }

    // a & b should never be null, so don't perform a null check here.
    // Let the source of error identify itself.
    public static int compareDates(DateTime a, DateTime b) {
        LocalDate aDate = a.toLocalDate();
        LocalDate bDate = b.toLocalDate();

        if (aDate.isBefore(bDate)) {
            return -1;
        } else if (aDate.isAfter(bDate)) {
            return 1;
        } else {
            int aMinuteOfDay = a.getMinuteOfDay();
            int bMinuteOfDay = b.getMinuteOfDay();

            if (aMinuteOfDay < bMinuteOfDay) {
                return -1;
            } else if (aMinuteOfDay > bMinuteOfDay) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public void setTimeInMillis(long timeInMillis) {
        mFirstDateTime = new DateTime(timeInMillis);
        mSecondDateTime = new DateTime(timeInMillis);
    }

    @Override
    public String toString() {
        StringBuilder toReturn = new StringBuilder();

        if (mFirstDateTime != null) {
            toReturn.append(DateFormat.getDateInstance().format(mFirstDateTime.getMillis()));
            toReturn.append("\n");
        }

        if (mSecondDateTime != null) {
            toReturn.append(DateFormat.getDateInstance().format(mSecondDateTime.getMillis()));
        }

        return toReturn.toString();
    }
}
