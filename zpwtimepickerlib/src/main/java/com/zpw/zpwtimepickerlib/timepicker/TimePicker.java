package com.zpw.zpwtimepickerlib.timepicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.TtsSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zpw.zpwtimepickerlib.R;
import com.zpw.zpwtimepickerlib.common.DateTimePatternHelper;
import com.zpw.zpwtimepickerlib.utilities.AccessibilityUtils;
import com.zpw.zpwtimepickerlib.utilities.SUtils;

import org.joda.time.Chronology;
import org.joda.time.LocalTime;
import org.joda.time.chrono.GJChronology;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * @author: zhoupengwei
 * @time: 16/7/18-上午11:40
 * @Email: zhoupengwei@qccr.com
 * @desc: 时间选择器
 */
public class TimePicker extends FrameLayout {
    private static final String TAG = TimePicker.class.getSimpleName();

    private static final int RANGE_ACTIVATED_NONE = 0;
    private static final int RANGE_ACTIVATED_START = 1;
    private static final int RANGE_ACTIVATED_END = 2;

    // Index used by RadialPickerLayout
    public static final int HOUR_INDEX = 0;
    public static final int MINUTE_INDEX = 1;

    // LayoutLib relies on these constants. Change TimePickerClockDelegate_Delegate if
    // modifying these.
    private static final int AM = 0;
    private static final int PM = 1;

    private static final int HOURS_IN_HALF_DAY = 12;

    private Context mContext;
    private Locale mCurrentLocale;

    private View mHeaderView;

    private RelativeLayout mRlHeaderTimeSingleCont;
    private TextView mHourView;
    private TextView mMinuteView;
    private View mAmPmLayout;
    private CheckedTextView mAmLabel;
    private CheckedTextView mPmLabel;
    private TextView mSeparatorView;

    private RelativeLayout mRlHeaderTimeRangeCont;
    private TextView mTimeStart;
    private View mAmPmLayoutStart;
    private CheckedTextView mAmLabelStart;
    private CheckedTextView mPmLabelStart;
    private TextView mTimeEnd;
    private View mAmPmLayoutEnd;
    private CheckedTextView mAmLabelEnd;
    private CheckedTextView mPmLabelEnd;

    private ImageView ivHeaderTimeReset;

    private RadialTimePickerView mRadialTimePickerView;

    private String mAmText;
    private String mPmText;

    private boolean mIsEnabled = true;
    private boolean mAllowAutoAdvance;
    private SelectedTime mCurrentTime;
    private boolean mIs24HourView;
    private boolean mIsAmPmAtStart;

    // Accessibility strings.
    private String mSelectHours;
    private String mSelectMinutes;

    // Most recent time announcement values for accessibility.
    private CharSequence mLastAnnouncedText;
    private boolean mLastAnnouncedIsHour;

    private Calendar mTempCalendar;

    // Callbacks
    private OnTimeChangedListener mOnTimeChangedListener;

    private int mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_NONE;

    public TimePicker(Context context) {
        this(context, null);
    }

    public TimePicker(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.spTimePickerStyle);
    }

    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spTimePickerStyle,
                R.style.SublimeTimePickerStyle), attrs, defStyleAttr);
        initializeLayout();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TimePicker(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(SUtils.createThemeWrapper(context, R.attr.sublimePickerStyle,
                R.style.SublimePickerStyleLight, R.attr.spTimePickerStyle,
                R.style.SublimeTimePickerStyle), attrs, defStyleAttr, defStyleRes);
        initializeLayout();
    }

    private void initializeLayout() {
        mContext = getContext();
        setCurrentLocale(Locale.getDefault());

        // process style attributes
        final TypedArray a = mContext.obtainStyledAttributes(R.styleable.SublimeTimePicker);
        final LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final Resources res = mContext.getResources();

        mSelectHours = res.getString(R.string.select_hours);
        mSelectMinutes = res.getString(R.string.select_minutes);

        DateFormatSymbols dfs = DateFormatSymbols.getInstance(mCurrentLocale);
        String[] amPmStrings = dfs.getAmPmStrings();/*{"AM", "PM"}*/

        if (amPmStrings.length == 2
                && !TextUtils.isEmpty(amPmStrings[0]) && !TextUtils.isEmpty(amPmStrings[1])) {
            mAmText = amPmStrings[0].length() > 2 ?
                    amPmStrings[0].substring(0, 2) : amPmStrings[0];
            mPmText = amPmStrings[1].length() > 2 ?
                    amPmStrings[1].substring(0, 2) : amPmStrings[1];
        } else {
            // Defaults
            mAmText = "AM";
            mPmText = "PM";
        }

        final int layoutResourceId = R.layout.time_picker_layout;
        final View mainView = inflater.inflate(layoutResourceId, this);

        mHeaderView = mainView.findViewById(R.id.time_header);

        ivHeaderTimeReset = (ImageView) mainView.findViewById(R.id.iv_header_time_reset);

        mRlHeaderTimeSingleCont = (RelativeLayout)mainView.findViewById(R.id.rl_header_time_single_cont);
        // Set up hour/minute labels.
        mHourView = (TextView) mainView.findViewById(R.id.hours);
        mHourView.setOnClickListener(mClickListener);

        ViewCompat.setAccessibilityDelegate(mHourView, new ClickActionDelegate(mContext, R.string.select_hours));

        mSeparatorView = (TextView) mainView.findViewById(R.id.separator);

        mMinuteView = (TextView) mainView.findViewById(R.id.minutes);
        mMinuteView.setOnClickListener(mClickListener);

        ViewCompat.setAccessibilityDelegate(mMinuteView, new ClickActionDelegate(mContext, R.string.select_minutes));

        // Now that we have text appearances out of the way, make sure the hour
        // and minute views are correctly sized.
        mHourView.setMinWidth(computeStableWidth(mHourView, 24));
        mMinuteView.setMinWidth(computeStableWidth(mMinuteView, 60));

        // Set up AM/PM labels.
        mAmPmLayout = mainView.findViewById(R.id.ampm_layout);
        mAmLabel = (CheckedTextView) mAmPmLayout.findViewById(R.id.am_label);
        mAmLabel.setText(obtainVerbatim(amPmStrings[0]));
        mAmLabel.setOnClickListener(mClickListener);
        mPmLabel = (CheckedTextView) mAmPmLayout.findViewById(R.id.pm_label);
        mPmLabel.setText(obtainVerbatim(amPmStrings[1]));
        mPmLabel.setOnClickListener(mClickListener);

        mRlHeaderTimeRangeCont = (RelativeLayout)mainView.findViewById(R.id.rl_header_time_range_cont);

        mTimeStart = (TextView) mainView.findViewById(R.id.time_start);
        // Set up AM/PM labels.
        mAmPmLayoutStart = mainView.findViewById(R.id.ampm_layout_start);
        mAmLabelStart = (CheckedTextView) mAmPmLayoutStart.findViewById(R.id.am_label_start);
        mAmLabelStart.setText(obtainVerbatim(amPmStrings[0]));
        mAmLabelStart.setOnClickListener(mClickListener);
        mPmLabelStart = (CheckedTextView) mAmPmLayoutStart.findViewById(R.id.pm_label_start);
        mPmLabelStart.setText(obtainVerbatim(amPmStrings[1]));
        mPmLabelStart.setOnClickListener(mClickListener);

        mTimeEnd = (TextView) mainView.findViewById(R.id.time_end);
        // Set up AM/PM labels.
        mAmPmLayoutEnd = mainView.findViewById(R.id.ampm_layout_end);
        mAmLabelEnd = (CheckedTextView) mAmPmLayoutEnd.findViewById(R.id.am_label_end);
        mAmLabelEnd.setText(obtainVerbatim(amPmStrings[0]));
        mAmLabelEnd.setOnClickListener(mClickListener);
        mPmLabelEnd = (CheckedTextView) mAmPmLayoutEnd.findViewById(R.id.pm_label_end);
        mPmLabelEnd.setText(obtainVerbatim(amPmStrings[1]));
        mPmLabelEnd.setOnClickListener(mClickListener);

        int iconColor, pressedStateBgColor;

        iconColor = a.getColor(R.styleable.SublimePicker_spOverflowIconColor,
                SUtils.COLOR_TEXT_PRIMARY_INVERSE);
        pressedStateBgColor = a.getColor(R.styleable.SublimePicker_spOverflowIconPressedBgColor,
                SUtils.COLOR_TEXT_PRIMARY);
        ColorStateList headerTextColor = a.getColorStateList(R.styleable.SublimeTimePicker_spHeaderTextColor);

        SUtils.setImageTintList(ivHeaderTimeReset, ColorStateList.valueOf(iconColor));
        SUtils.setViewBackground(ivHeaderTimeReset, SUtils.createOverflowButtonBg(pressedStateBgColor));

        if (headerTextColor != null) {
            mHourView.setTextColor(headerTextColor);
            mSeparatorView.setTextColor(headerTextColor);
            mMinuteView.setTextColor(headerTextColor);
            mAmLabel.setTextColor(headerTextColor);
            mPmLabel.setTextColor(headerTextColor);

            mTimeStart.setTextColor(headerTextColor);
            mAmLabelStart.setTextColor(headerTextColor);
            mPmLabelStart.setTextColor(headerTextColor);

            mTimeEnd.setTextColor(headerTextColor);
            mAmLabelEnd.setTextColor(headerTextColor);
            mPmLabelEnd.setTextColor(headerTextColor);
        }

        // Set up header background, if available.
        if (SUtils.isApi_22_OrHigher()) {
            if (a.hasValueOrEmpty(R.styleable.SublimeTimePicker_spHeaderBackground)) {
                SUtils.setViewBackground(mHeaderView,
                        a.getDrawable(R.styleable.SublimeTimePicker_spHeaderBackground));
            }
        } else {
            if (a.hasValue(R.styleable.SublimeTimePicker_spHeaderBackground)) {
                SUtils.setViewBackground(mHeaderView,
                        a.getDrawable(R.styleable.SublimeTimePicker_spHeaderBackground));
            }
        }

        a.recycle();

        mRadialTimePickerView = (RadialTimePickerView) mainView.findViewById(R.id.radial_picker);

        setupListeners();

        mAllowAutoAdvance = true;

        // Initialize with current time
        final Calendar calendar = Calendar.getInstance(mCurrentLocale);
        mCurrentTime = new SelectedTime(LocalTime.fromCalendarFields(calendar));
        mIs24HourView = false;
        init(mCurrentTime /* 12h */, HOUR_INDEX);
    }

    RadialTimePickerView.OnValueSelectedListener mOnValueSelectedListener
            = new RadialTimePickerView.OnValueSelectedListener(){
        /**
         * Called by the picker for updating the header display.
         */
        @Override
        public void onValueSelected(int pickerIndex, int newValue, boolean autoAdvance) {
            switch (pickerIndex) {
                case HOUR_INDEX:
                    if (mAllowAutoAdvance && autoAdvance) {
                        updateHeaderHour(newValue, false);
                        setCurrentItemShowing(MINUTE_INDEX, true, false);
                        AccessibilityUtils.makeAnnouncement(TimePicker.this, newValue + ". " + mSelectMinutes);
                    } else {
                        updateHeaderHour(newValue, true);
                    }
                    break;
                case MINUTE_INDEX:
                    updateHeaderMinute(newValue, true);
                    break;
            }

            if (mOnTimeChangedListener != null) {
                mOnTimeChangedListener.onTimeChanged(TimePicker.this, getCurrentHour(), getCurrentMinute());
            }
        }
    };


    private final OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.am_label) {
                setAmOrPm(AM);
            } else if (v.getId() == R.id.pm_label) {
                setAmOrPm(PM);
            } else if (v.getId() == R.id.hours) {
                setCurrentItemShowing(HOUR_INDEX, true, true);
            } else if (v.getId() == R.id.minutes) {
                setCurrentItemShowing(MINUTE_INDEX, true, true);
            } else {
                // Failed to handle this click, don't vibrate.
                return;
            }

            SUtils.vibrateForTimePicker(TimePicker.this);
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private CharSequence obtainVerbatim(String text) {
        return (SUtils.isApi_21_OrHigher()) ?
                new SpannableStringBuilder().append(text,
                        new TtsSpan.VerbatimBuilder(text).build(), 0)
                : text;
    }

    private static class ClickActionDelegate extends AccessibilityDelegateCompat {
        private final AccessibilityNodeInfoCompat.AccessibilityActionCompat mClickAction;

        public ClickActionDelegate(Context context, int resId) {
            CharSequence label = context.getString(resId);
            mClickAction = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(AccessibilityNodeInfoCompat.ACTION_CLICK,
                    label);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.addAction(mClickAction);
        }
    }

    private int computeStableWidth(TextView v, int maxNumber) {
        int maxWidth = 0;

        for (int i = 0; i < maxNumber; i++) {
            final String text = String.format("%02d", i);
            v.setText(text);
            v.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);

            final int width = v.getMeasuredWidth();
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        return maxWidth;
    }

    public void init(SelectedTime selectedTime, int index) {
        mCurrentTime = selectedTime;
        updateUI(index);
    }

    private void setupListeners() {
        mHeaderView.setFocusable(true);

        mRadialTimePickerView.setOnValueSelectedListener(mOnValueSelectedListener);
    }

    private void updateUI(int index) {
        // Update RadialPicker values
        updateRadialPicker(index);
        // Enable or disable the AM/PM view.
        updateHeaderAmPm();
        // Update Hour and Minutes
        updateHeaderHour(mCurrentTime, false);
        // Update time separator
        updateHeaderSeparator();
        // Update Minutes
        updateHeaderMinute(mCurrentTime, false);
        // Invalidate everything
        invalidate();
    }

    private void switchToSingleDateView() {
        mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_NONE;

        ivHeaderTimeReset.setVisibility(View.GONE);
        mRlHeaderTimeRangeCont.setVisibility(View.INVISIBLE);
        mRlHeaderTimeSingleCont.setVisibility(View.VISIBLE);
    }

    private void switchToDateRangeView() {
        if (mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_NONE) {
            mCurrentlyActivatedRangeItem = RANGE_ACTIVATED_START;
        }

        mRlHeaderTimeSingleCont.setVisibility(View.INVISIBLE);
        ivHeaderTimeReset.setVisibility(View.VISIBLE);
        mRlHeaderTimeRangeCont.setVisibility(View.VISIBLE);

        mTimeStart.setActivated(mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_START);
        mTimeEnd.setActivated(mCurrentlyActivatedRangeItem == RANGE_ACTIVATED_END);
    }

    private void updateRadialPicker(int index) {
        mRadialTimePickerView.initialize(mInitialHourOfDay, mInitialMinute, mIs24HourView);
        setCurrentItemShowing(index, false, true);
    }

    private void updateHeaderAmPm() {
        if (mIs24HourView) {
            mAmPmLayout.setVisibility(View.GONE);
        } else {
            // Ensure that AM/PM layout is in the correct position.
            String timePattern;

            // Available on API >= 18
            if (SUtils.isApi_18_OrHigher()) {
                timePattern = DateFormat.getBestDateTimePattern(mCurrentLocale, "hm");
            } else {
                timePattern = DateTimePatternHelper.getBestDateTimePattern(mCurrentLocale,
                        DateTimePatternHelper.PATTERN_hm);
            }

            final boolean isAmPmAtStart = timePattern.startsWith("a");
            setAmPmAtStart(isAmPmAtStart);

            updateAmPmLabelStates(mInitialHourOfDay < 12 ? AM : PM);
        }
    }

    private void setAmPmAtStart(boolean isAmPmAtStart) {
        if (mIsAmPmAtStart != isAmPmAtStart) {
            mIsAmPmAtStart = isAmPmAtStart;

            final RelativeLayout.LayoutParams params =
                    (RelativeLayout.LayoutParams) mAmPmLayout.getLayoutParams();
            int[] rules = params.getRules();

            if (rules[RelativeLayout.RIGHT_OF] != 0 ||
                    rules[RelativeLayout.LEFT_OF] != 0) {
                if (isAmPmAtStart) {
                    params.addRule(RelativeLayout.RIGHT_OF, 0);
                    params.addRule(RelativeLayout.LEFT_OF, mHourView.getId());
                } else {
                    params.addRule(RelativeLayout.LEFT_OF, 0);
                    params.addRule(RelativeLayout.RIGHT_OF, mMinuteView.getId());
                }
            }

            mAmPmLayout.setLayoutParams(params);
        }
    }

    /**
     * Set the current hour.
     */
    public void setCurrentHour(int currentHour) {
        if (mInitialHourOfDay == currentHour) {
            return;
        }
        mInitialHourOfDay = currentHour;
        updateHeaderHour(currentHour, true);
        updateHeaderAmPm();
        mRadialTimePickerView.setCurrentHour(currentHour);
        mRadialTimePickerView.setAmOrPm(mInitialHourOfDay < 12 ? AM : PM);
        invalidate();
        onTimeChanged();
    }

    /**
     * @return The current hour in the range (0-23).
     */
    public int getCurrentHour() {
        int currentHour = mRadialTimePickerView.getCurrentHour();
        if (mIs24HourView) {
            return currentHour;
        } else {
            switch (mRadialTimePickerView.getAmOrPm()) {
                case PM:
                    return (currentHour % HOURS_IN_HALF_DAY) + HOURS_IN_HALF_DAY;
                case AM:
                default:
                    return currentHour % HOURS_IN_HALF_DAY;
            }
        }
    }

    /**
     * Set the current minute (0-59).
     */
    public void setCurrentMinute(int currentMinute) {
        if (mInitialMinute == currentMinute) {
            return;
        }
        mInitialMinute = currentMinute;
        updateHeaderMinute(currentMinute, true);
        mRadialTimePickerView.setCurrentMinute(currentMinute);
        invalidate();
        onTimeChanged();
    }

    /**
     * @return The current minute.
     */
    public int getCurrentMinute() {
        return mRadialTimePickerView.getCurrentMinute();
    }

    /**
     * Set whether in 24 hour or AM/PM mode.
     *
     * @param is24HourView True = 24 hour mode. False = AM/PM.
     */
    public void setIs24HourView(boolean is24HourView) {
        if (is24HourView == mIs24HourView) {
            return;
        }
        mIs24HourView = is24HourView;
        int hour = mRadialTimePickerView.getCurrentHour();
        mInitialHourOfDay = hour;
        updateHeaderHour(hour, false);
        updateHeaderAmPm();
        updateRadialPicker(mRadialTimePickerView.getCurrentItemShowing());
        invalidate();
    }

    /**
     * @return true if this is in 24 hour view else false.
     */
    public boolean is24HourView() {
        return mIs24HourView;
    }

    @SuppressWarnings("unused")
    public void setOnTimeChangedListener(OnTimeChangedListener callback) {
        mOnTimeChangedListener = callback;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mHourView.setEnabled(enabled);
        mMinuteView.setEnabled(enabled);
        mAmLabel.setEnabled(enabled);
        mPmLabel.setEnabled(enabled);
        mRadialTimePickerView.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Override
    public int getBaseline() {
        // does not support baseline alignment
        return -1;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updateUI(mRadialTimePickerView.getCurrentItemShowing());
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return new SavedState(super.onSaveInstanceState(), getCurrentHour(), getCurrentMinute(),
                is24HourView(), getCurrentItemShowing(),mCurrentlyActivatedRangeItem);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        BaseSavedState bss = (BaseSavedState) state;
        super.onRestoreInstanceState(bss.getSuperState());
        SavedState ss = (SavedState) bss;
        setInKbMode(ss.inKbMode());
        setTypedTimes(ss.getTypesTimes());
        init(ss.getHour(), ss.getMinute(), ss.is24HourMode(), ss.getCurrentItemShowing());
        mRadialTimePickerView.invalidate();
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        onPopulateAccessibilityEvent(event);
        return true;
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        int flags = DateUtils.FORMAT_SHOW_TIME;

        // The deprecation status does not show up in the documentation and
        // source code does not outline the alternative.
        // Leaving this as is for now.
        if (mIs24HourView) {
            //noinspection deprecation
            flags |= DateUtils.FORMAT_24HOUR;
        } else {
            //noinspection deprecation
            flags |= DateUtils.FORMAT_12HOUR;
        }
        mTempCalendar.set(Calendar.HOUR_OF_DAY, getCurrentHour());
        mTempCalendar.set(Calendar.MINUTE, getCurrentMinute());
        String selectedDate = DateUtils.formatDateTime(mContext,
                mTempCalendar.getTimeInMillis(), flags);
        event.getText().add(selectedDate);
    }

    /**
     * @return the index of the current item showing
     */
    private int getCurrentItemShowing() {
        return mRadialTimePickerView.getCurrentItemShowing();
    }

    /**
     * Propagate the time change
     */
    private void onTimeChanged() {
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
        if (mOnTimeChangedListener != null) {
            mOnTimeChangedListener.onTimeChanged(this,
                    getCurrentHour(), getCurrentMinute());
        }
    }

    /**
     * Used to save / restore state of time picker
     */
    private static class SavedState extends BaseSavedState {

        private final int mHour;
        private final int mMinute;
        private final boolean mIs24HourMode;
        private final int mCurrentItemShowing;
        private final int mActivatedRangeItem;

        private SavedState(Parcelable superState, int hour, int minute, boolean is24HourMode,
                           int currentItemShowing ,int activatedRangeItem) {
            super(superState);
            mHour = hour;
            mMinute = minute;
            mIs24HourMode = is24HourMode;
            mCurrentItemShowing = currentItemShowing;
            mActivatedRangeItem = activatedRangeItem;
        }

        private SavedState(Parcel in) {
            super(in);
            mHour = in.readInt();
            mMinute = in.readInt();
            mIs24HourMode = (in.readInt() == 1);
            //noinspection unchecked
            mCurrentItemShowing = in.readInt();
            mActivatedRangeItem = in.readInt();
        }

        public int getHour() {
            return mHour;
        }

        public int getMinute() {
            return mMinute;
        }

        public boolean is24HourMode() {
            return mIs24HourMode;
        }

        public int getCurrentItemShowing() {
            return mCurrentItemShowing;
        }

        public int getActivatedRangeItem() {
            return mActivatedRangeItem;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mHour);
            dest.writeInt(mMinute);
            dest.writeInt(mIs24HourMode ? 1 : 0);
            dest.writeInt(mCurrentItemShowing);
            dest.writeInt(mActivatedRangeItem);
        }

        @SuppressWarnings({"unused", "hiding"})
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private void updateAmPmLabelStates(int amOrPm) {
        final boolean isAm = amOrPm == AM;
        mAmLabel.setActivated(isAm);
        mAmLabel.setChecked(isAm);

        final boolean isPm = amOrPm == PM;
        mPmLabel.setActivated(isPm);
        mPmLabel.setChecked(isPm);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void updateHeaderHour(int value, boolean announce) {
        String timePattern;

        if (SUtils.isApi_18_OrHigher()) {
            timePattern = DateFormat.getBestDateTimePattern(mCurrentLocale,
                    (mIs24HourView) ? "Hm" : "hm");
        } else {
            timePattern = DateTimePatternHelper.getBestDateTimePattern(mCurrentLocale,
                    (mIs24HourView) ? DateTimePatternHelper.PATTERN_Hm
                            : DateTimePatternHelper.PATTERN_hm);
        }

        final int lengthPattern = timePattern.length();
        boolean hourWithTwoDigit = false;
        char hourFormat = '\0';
        // Check if the returned pattern is single or double 'H', 'h', 'K', 'k'. We also save
        // the hour format that we found.
        for (int i = 0; i < lengthPattern; i++) {
            final char c = timePattern.charAt(i);
            if (c == 'H' || c == 'h' || c == 'K' || c == 'k') {
                hourFormat = c;
                if (i + 1 < lengthPattern && c == timePattern.charAt(i + 1)) {
                    hourWithTwoDigit = true;
                }
                break;
            }
        }
        final String format;
        if (hourWithTwoDigit) {
            format = "%02d";
        } else {
            format = "%d";
        }
        if (mIs24HourView) {
            // 'k' means 1-24 hour
            if (hourFormat == 'k' && value == 0) {
                value = 24;
            }
        } else {
            // 'K' means 0-11 hour
            value = modulo12(value, hourFormat == 'K');
        }
        CharSequence text = String.format(format, value);
        mHourView.setText(text);
        if (announce) {
            tryAnnounceForAccessibility(text, true);
        }
    }

    private void tryAnnounceForAccessibility(CharSequence text, boolean isHour) {
        if (mLastAnnouncedIsHour != isHour || !text.equals(mLastAnnouncedText)) {
            // TODO: Find a better solution, potentially live regions?
            AccessibilityUtils.makeAnnouncement(this, text);
            mLastAnnouncedText = text;
            mLastAnnouncedIsHour = isHour;
        }
    }

    private static int modulo12(int n, boolean startWithZero) {
        int value = n % 12;
        if (value == 0 && !startWithZero) {
            value = 12;
        }
        return value;
    }

    /**
     * The time separator is defined in the Unicode CLDR and cannot be supposed to be ":".
     * <p/>
     * See http://unicode.org/cldr/trac/browser/trunk/common/main
     * <p/>
     * We pass the correct "skeleton" depending on 12 or 24 hours view and then extract the
     * separator as the character which is just after the hour marker in the returned pattern.
     */
    private void updateHeaderSeparator() {
        String timePattern;

        // Available on API >= 18
        if (SUtils.isApi_18_OrHigher()) {
            timePattern = DateFormat.getBestDateTimePattern(mCurrentLocale,
                    (mIs24HourView) ? "Hm" : "hm");
        } else {
            timePattern = DateTimePatternHelper.getBestDateTimePattern(mCurrentLocale,
                    (mIs24HourView) ? DateTimePatternHelper.PATTERN_Hm
                            : DateTimePatternHelper.PATTERN_hm);
        }

        final String separatorText;
        // See http://www.unicode.org/reports/tr35/tr35-dates.html for hour formats
        final char[] hourFormats = {'H', 'h', 'K', 'k'};
        int hIndex = lastIndexOfAny(timePattern, hourFormats);
        if (hIndex == -1) {
            // Default case
            separatorText = ":";
        } else {
            separatorText = Character.toString(timePattern.charAt(hIndex + 1));
        }
        mSeparatorView.setText(separatorText);
    }

    static private int lastIndexOfAny(String str, char[] any) {
        final int lengthAny = any.length;
        if (lengthAny > 0) {
            for (int i = str.length() - 1; i >= 0; i--) {
                char c = str.charAt(i);
                for (char anyChar : any) {
                    if (c == anyChar) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private void updateHeaderMinute(int value, boolean announceForAccessibility) {
        if (value == 60) {
            value = 0;
        }
        final CharSequence text = String.format(mCurrentLocale, "%02d", value);
        mMinuteView.setText(text);
        if (announceForAccessibility) {
            tryAnnounceForAccessibility(text, false);
        }
    }

    /**
     * Show either Hours or Minutes.
     */
    private void setCurrentItemShowing(int index, boolean animateCircle, boolean announce) {
        mRadialTimePickerView.setCurrentItemShowing(index, animateCircle);

        if (index == HOUR_INDEX) {
            if (announce) {
                AccessibilityUtils.makeAnnouncement(this, mSelectHours);
            }
        } else {
            if (announce) {
                AccessibilityUtils.makeAnnouncement(this, mSelectMinutes);
            }
        }

        mHourView.setActivated(index == HOUR_INDEX);
        mMinuteView.setActivated(index == MINUTE_INDEX);
    }

    private void setAmOrPm(int amOrPm) {
        updateAmPmLabelStates(amOrPm);
        mRadialTimePickerView.setAmOrPm(amOrPm);
    }

    public void setCurrentLocale(Locale locale) {
        if (locale.equals(mCurrentLocale)) {
            return;
        }
        mCurrentLocale = locale;

        mTempCalendar = Calendar.getInstance(locale);
    }

    /**
     * The callback interface used to indicate the time has been adjusted.
     */
    public interface OnTimeChangedListener {

        /**
         * @param view      The view associated with this listener.
         * @param selectedTime The current hour.
         */
        void onTimeChanged(TimePicker view, SelectedTime selectedTime);
    }

    /**
     * A callback interface for updating input validity when the TimePicker
     * when included into a Dialog.
     */
    public interface TimePickerValidationCallback {
        void onTimePickerValidationChanged(boolean valid);
    }
}
