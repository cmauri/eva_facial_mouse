/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

 // http://stackoverflow.com/questions/20758986/android-preferenceactivity-dialog-with-number-mPicker
// http://stackoverflow.com/questions/2695646/declaring-a-custom-android-ui-element-using-xml
// https://github.com/TonicArtos/Otago-Linguistics-Experiments/blob/master/SPRE/src/com/michaelnovakjr/numberpicker/NumberPickerPreference.java
// https://gist.github.com/thom-nic/959884

package com.crea_si.eviacam.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

import com.crea_si.eviacam.R;

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
    private CharSequence mTitle;

    @SuppressWarnings("WeakerAccess")
    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        if (attrs== null) return;
        
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.NumberPickerPreference);
        mMinValue= arr.getInteger(R.styleable.NumberPickerPreference_minValue, DEFAULT_MIN_VALUE);
        mMaxValue= arr.getInteger(R.styleable.NumberPickerPreference_maxValue, DEFAULT_MAX_VALUE);
        arr.recycle();
        
        mTitle= this.getTitle();
    }
    
    @SuppressWarnings("unused")
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
            mPicker.clearFocus();
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
        Preference.OnPreferenceChangeListener listener= getOnPreferenceChangeListener();
        boolean update= true;
        if (null != listener) {
            update= listener.onPreferenceChange(this, value);
        }
        if (update) {
            mValue = value;
            setTitle(mTitle + ": " + Integer.toString(value));
            persistInt(mValue);
        }
    }

    public int getValue() {
        return mValue;
    }
}