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

package com.daliammao.zpwtimepickerlib.helpers;

import com.daliammao.zpwtimepickerlib.datepicker.SelectedDate;
import com.daliammao.zpwtimepickerlib.datetimepicker.DateTimePicker;
import com.daliammao.zpwtimepickerlib.datetimepicker.SelectedDateTime;
import com.daliammao.zpwtimepickerlib.recurrencepicker.RecurrencePicker;

import org.joda.time.LocalTime;

public abstract class ListenerAdapter {
    /**
     * @param sublimeMaterialPicker SublimeMaterialPicker view
     * @param selectedDateTime      The datetime that was set.
     * @param recurrenceOption      One of the options defined in
     *                              SublimeRecurrencePicker.RecurrenceOption.
     *                              'recurrenceRule' will only be passed if
     *                              'recurrenceOption' is 'CUSTOM'.
     * @param recurrenceRule        The recurrence rule that was set. This will
     *                              be 'null' if 'recurrenceOption' is anything
     *                              other than 'CUSTOM'.
     */
    public abstract void onDateTimeRecurrenceSet(DateTimePicker sublimeMaterialPicker,
                                                 SelectedDateTime selectedDateTime,
                                                 RecurrencePicker.RecurrenceOption recurrenceOption,
                                                 String recurrenceRule);

    // Cancel button or icon clicked
    public abstract void onCancelled();

    /**
     * @param selectedDate The date(or range) that is selected.
     * @return Formatted date to display on `Switcher` button
     */
    @SuppressWarnings("UnusedParameters")
    public CharSequence formatDate(SelectedDate selectedDate) {
        return null;
    }

    /**
     * @param selectedTime The time of day that was set.
     * @return Formatted time to display on `Switcher` button
     */
    @SuppressWarnings("UnusedParameters")
    public CharSequence formatTime(LocalTime selectedTime) {
        return null;
    }
}
