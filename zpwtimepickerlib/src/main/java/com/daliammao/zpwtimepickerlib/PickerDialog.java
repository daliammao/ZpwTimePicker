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

package com.daliammao.zpwtimepickerlib;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.daliammao.zpwtimepickerlib.datetimepicker.DateTimePicker;
import com.daliammao.zpwtimepickerlib.datetimepicker.SelectedDateTime;
import com.daliammao.zpwtimepickerlib.helpers.ListenerAdapter;
import com.daliammao.zpwtimepickerlib.helpers.Options;
import com.daliammao.zpwtimepickerlib.recurrencepicker.RecurrencePicker;

public class PickerDialog extends DialogFragment {

    // Picker
    DateTimePicker mDateTimePicker;

    // Callback to activity
    Callback mCallback;

    ListenerAdapter mListener = new ListenerAdapter() {
        @Override
        public void onCancelled() {
            if (mCallback != null) {
                mCallback.onCancelled();
            }

            // Should actually be called by activity inside `Callback.onCancelled()`
            dismiss();
        }

        @Override
        public void onDateTimeRecurrenceSet(DateTimePicker sublimeMaterialPicker,
                                            SelectedDateTime selectedDateTime,
                                            RecurrencePicker.RecurrenceOption recurrenceOption,
                                            String recurrenceRule) {
            if (mCallback != null) {
                mCallback.onDateTimeRecurrenceSet(selectedDateTime, recurrenceOption, recurrenceRule);
            }

            // Should actually be called by activity inside `Callback.onCancelled()`
            dismiss();
        }
// You can also override 'formatDate(Date)' & 'formatTime(Date)'
        // to supply custom formatters.
    };

    // Set activity callback
    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mDateTimePicker = (DateTimePicker) getActivity()
                .getLayoutInflater().inflate(R.layout.sublime_picker, container);

        // Retrieve Options
        Bundle arguments = getArguments();
        Options options = null;

        // Options can be null, in which case, default
        // options are used.
        if (arguments != null) {
            options = arguments.getParcelable("SUBLIME_OPTIONS");
        }

        mDateTimePicker.initializePicker(options, mListener);
        return mDateTimePicker;
    }

    // For communicating with the activity
    public interface Callback {
        void onCancelled();

        void onDateTimeRecurrenceSet(SelectedDateTime selectedDateTime,
                                     RecurrencePicker.RecurrenceOption recurrenceOption,
                                     String recurrenceRule);
    }
}
