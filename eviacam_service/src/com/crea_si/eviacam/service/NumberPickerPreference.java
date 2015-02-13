// http://stackoverflow.com/questions/20758986/android-preferenceactivity-dialog-with-number-mPicker
// http://stackoverflow.com/questions/2695646/declaring-a-custom-android-ui-element-using-xml
// https://github.com/TonicArtos/Otago-Linguistics-Experiments/blob/master/SPRE/src/com/michaelnovakjr/numberpicker/NumberPickerPreference.java
// https://gist.github.com/thom-nic/959884

package com.crea_si.eviacam.service;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

/**
 * A {@link android.preference.Preference} that displays a number picker as a dialog.
 */
public class NumberPickerPreference extends DialogPreference {
    private static final int DEFAULT_MIN_VALUE = 0;
    private static final int DEFAULT_MAX_VALUE = 100;

    private NumberPicker mPicker;
    private int mValue;
    private int mMinValue;
    private int mMaxValue;
    // private int mDefaultValue;   // Not needed in principle

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        if (attrs== null) return;
        
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference);
        mMinValue= arr.getInteger(R.styleable.NumberPickerPreference_minValue, DEFAULT_MIN_VALUE);
        mMaxValue= arr.getInteger(R.styleable.NumberPickerPreference_maxValue, DEFAULT_MAX_VALUE);
        //mDefaultValue= arr.getInteger(R.styleable.NumberPickerPreference_defaultValue, DEFAULT_MIN_VALUE);
        arr.recycle();
    }
    
    public NumberPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.dialogPreferenceStyle);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mPicker.setMinValue(mMinValue);
        mPicker.setMaxValue(mMaxValue);
        mPicker.setValue(getValue());
    }
    
    @Override
    protected View onCreateDialogView() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;

        mPicker = new NumberPicker(getContext());
        mPicker.setLayoutParams(layoutParams);

        FrameLayout dialogView = new FrameLayout(getContext());
        dialogView.addView(mPicker);

        return dialogView;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            setValue(mPicker.getValue());
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, DEFAULT_MIN_VALUE);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue)
            setValue(getPersistedInt(DEFAULT_MIN_VALUE));
        else
            setValue((Integer) defaultValue);
    }

    public void setValue(int value) {
        mValue = value;
        persistInt(mValue);
    }

    public int getValue() {
        return mValue;
    }
}