/*
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crea_si.softkeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

class InputViewManager {
    /*
     * Layout types
     */
    private static final int NONE_LAYOUT = 0;
    static final int QWERTY_LAYOUT = 1;
    static final int SYMBOLS_LAYOUT = 2;
    private static final int SYMBOLS_SHIFT_LAYOUT = 3;
    static final int NAVIGATION_LAYOUT = 4;
    
    /*
     * Qwerty layout subtypes
     */
    private static final int QWERTY_NONE = 0;
    private static final int QWERTY_EN = 1;
    private static final int QWERTY_ES = 2;
    private static final int QWERTY_CA = 3;
    private static final int QWERTY_DE = 4;

    /* Current layout key */
    private static final String CURRENT_LAYOUT = "current_layout";

    final private InputMethodService mIMEService;
    final private InputMethodManager mInputMethodManager;

    private final Handler mHandler= new Handler();

    private LatinKeyboardView mInputView;

    private int mCurrentLayout= NONE_LAYOUT;
    private int mCurrentQwertySubtype = QWERTY_NONE;

    private LatinKeyboard mQwertyKeyboard;
    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;    
    private LatinKeyboard mNavigationKeyboard;

    private boolean mCapsLock;
    private long mLastShiftTime= 0;

    InputViewManager(InputMethodService ime) {
        mIMEService= ime;
        mInputMethodManager = (InputMethodManager) ime.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * Select the layout that will be enabled when required calling 
     * enableSelectedLayout
     * 
     * @param type type of layout
     */
    void selectLayout(int type) {
        if (type == mCurrentLayout) return;

        switch (type) {
        case QWERTY_LAYOUT:
            if (mQwertyKeyboard== null) {
                selectSubtype (mInputMethodManager.getCurrentInputMethodSubtype());
            }
            mCurrentLayout= QWERTY_LAYOUT;
            break;
        case SYMBOLS_LAYOUT:
            if (mSymbolsKeyboard== null) {
                mSymbolsKeyboard = new LatinKeyboard(mIMEService, R.xml.symbols);
            }
            mSymbolsKeyboard.setShifted(false);
            mCurrentLayout= SYMBOLS_LAYOUT;
            break;
        case SYMBOLS_SHIFT_LAYOUT:
            if (mSymbolsShiftedKeyboard== null) {
                mSymbolsShiftedKeyboard = new LatinKeyboard(mIMEService, R.xml.symbols_shift);
            }
            mSymbolsShiftedKeyboard.setShifted(true);
            mCurrentLayout= SYMBOLS_SHIFT_LAYOUT;
            break;
        case NAVIGATION_LAYOUT:
            if (mNavigationKeyboard== null) {
                mNavigationKeyboard = new LatinKeyboard(mIMEService, R.xml.navigation);
            }
            mCurrentLayout= NAVIGATION_LAYOUT;
            break;
        default:
            throw new IllegalArgumentException();
        }
    }
    
    int getSelectedLayout() {
        return mCurrentLayout;
    }
    
    /**
     * Select qwerty keyboard according of a specific subtype (language)
     *
     * TODO: there is probably a better way to choose between different
     * keyboard layouts when changing the subtype (e.g. when changing language)
     */
    void selectSubtype(@Nullable InputMethodSubtype subtype) {
        if (subtype== null) return; // prevent crash
        final String locale= subtype.getLocale();
        if (locale.compareTo("es")== 0) {
            if (mCurrentQwertySubtype != QWERTY_ES) {
                mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty_es);
                mCurrentQwertySubtype= QWERTY_ES;
            }
        }
        else if (locale.compareTo("ca")== 0) {
            if (mCurrentQwertySubtype != QWERTY_CA) {
                mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty_ca);
                mCurrentQwertySubtype= QWERTY_CA;
            }
        }
        else if (locale.compareTo("de")== 0) {
            if (mCurrentQwertySubtype != QWERTY_DE) {
                mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty_de);
                mCurrentQwertySubtype= QWERTY_DE;
            }
        }
        else if (mCurrentQwertySubtype != QWERTY_EN) {
            mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty);
            mCurrentQwertySubtype= QWERTY_EN;
        }
    }

    /**
     * Create the KeyboardView
     * 
     * @param listener the listener
     * @return the view
     */
    View createView(KeyboardView.OnKeyboardActionListener listener) {
        mInputView= (LatinKeyboardView)
                mIMEService.getLayoutInflater().inflate(R.layout.input, null);
        mInputView.setOnKeyboardActionListener(listener);
        return mInputView;
    }

    /**
     * Retrieve previously saved layout from settings and enable it
     */
    void selectSavedLayout() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mIMEService);
        int layout = settings.getInt(CURRENT_LAYOUT, NONE_LAYOUT);
        if (NONE_LAYOUT == layout) return;
        selectLayout(layout);
    }

    /**
     * Save currently selected layout to preferences
     */
    void saveCurrentLayout() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mIMEService);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(CURRENT_LAYOUT, mCurrentLayout);
        editor.apply();
    }

    /*
     * Helper to get the currently selected keyboard 
     */
    private Keyboard getSelectedKeyboard() {
        switch (mCurrentLayout) {
        case QWERTY_LAYOUT:         return mQwertyKeyboard;
        case SYMBOLS_LAYOUT:        return mSymbolsKeyboard;
        case SYMBOLS_SHIFT_LAYOUT:  return mSymbolsShiftedKeyboard;
        case NAVIGATION_LAYOUT:     return mNavigationKeyboard;
        default:                    return null;
        }
    }
    
    /**
     * Enable the selected keyboard layout & subtype
     * 
     * @param subtype (can be null)
     */
    void enableSelected(InputMethodSubtype subtype) {
        if (mInputView== null) return;
        mInputView.setKeyboard(getSelectedKeyboard());
        mInputView.closing();
        if (subtype == null) {
            subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        }
        mInputView.setSubtype(subtype);
    }

    /*
     * Cleanup when closing the keyboard
     */
    void closing() {
        if (mInputView== null) return;
        mInputView.closing();
    }
    
    /*
     * Handle backspace key
     */
    boolean handleBack() {
        return mInputView != null && mInputView.handleBack();
    }
    
    /*
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    void updateShiftKeyState(EditorInfo attr) {
        if (mInputView == null) return;

        // Applicable only to the qwerty keyboard
        if (mCurrentLayout != QWERTY_LAYOUT) return;
        if (attr == null) attr = mIMEService.getCurrentInputEditorInfo();
        
        int caps = 0;
        if (attr.inputType != InputType.TYPE_NULL) {
            caps = mIMEService.getCurrentInputConnection().getCursorCapsMode(attr.inputType);
        }
        mInputView.setShifted(mCapsLock || caps != 0);
    }
    
    /*
     * Update label for the enter key according to what editor says
     */
    void updateEnterLabel(EditorInfo attr) {
        if (mInputView == null) return;
        LatinKeyboard current= (LatinKeyboard) mInputView.getKeyboard();
        current.setImeOptions(mIMEService.getResources(), attr.imeOptions);
    }
    
    /*
     * Handle when user press shift key
     */
    void handleShift() {
        if (mInputView == null) return;
        
        if (mCurrentLayout == QWERTY_LAYOUT) {
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !isShifted());
        } else if (mCurrentLayout == SYMBOLS_LAYOUT) {
            selectLayout(SYMBOLS_SHIFT_LAYOUT);
            enableSelected(null);
        } else if (mCurrentLayout == SYMBOLS_SHIFT_LAYOUT) {
            selectLayout(SYMBOLS_LAYOUT);
            enableSelected(null);
        }
    }
    
    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }
    
    int[] getKeyboardLocationOnScreen() {
        if (mInputView == null) return null;
        int coord[]= new int[2];
        mInputView.getLocationOnScreen(coord);
        
        return coord;
    }
    
    /**
     * Perform a click on the keyboard
     * @param x - abscissa coordinate relative to the view of the keyboard
     * @param y - ordinate coordinate relative to the view of the keyboard
     * @return - true if click performed
     */
    boolean performClick(int x, int y) {
        // has clicked inside the keyboard?
        if (mInputView == null) return false;

        long time= SystemClock.uptimeMillis();
        MotionEvent down= MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0);

        // dispatch down event
        mInputView.dispatchTouchEvent(down);

        // program up event after some ms
        mDelayedX= x;
        mDelayedY= y;
        mHandler.postDelayed(mDelayedEvent, 150);

        return true;
    }

    /* Runnable and parameters to send the UP event */
    private int mDelayedX, mDelayedY;
    private Runnable mDelayedEvent= new Runnable() {
        @Override
        public void run() {
            long time= SystemClock.uptimeMillis();
            MotionEvent up=
                    MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, mDelayedX, mDelayedY, 0);
            if (mInputView!= null) mInputView.dispatchTouchEvent(up);
        }
    };
    
    /*
     * Return whether the qwerty layout is shifted
     */
    boolean isShifted() {
        return mCurrentLayout == QWERTY_LAYOUT && mInputView != null && mInputView.isShifted();
    }
    
    /*
     * Handle layout change
     */
    void handleModeChange() {
        if (mInputView == null) return;

        if (mCurrentLayout == QWERTY_LAYOUT) {
            selectLayout(SYMBOLS_LAYOUT);
        } else {
            selectLayout(QWERTY_LAYOUT);
        }
        enableSelected(null);
    }
    
    /*
     * Enable navigation keyboard
     */
    void setNavigationKeyboard() {
        if (mInputView == null) return;
        if (mCurrentLayout == NAVIGATION_LAYOUT) return;

        selectLayout(NAVIGATION_LAYOUT);
        enableSelected(null);
    }
}
