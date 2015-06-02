/*
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
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

package com.crea_si.eviacam_keyboard;

import android.inputmethodservice.AbstractInputMethodService;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

public class EViacamIMEService extends InputMethodService implements
        OnKeyboardActionListener {

    private static EViacamIMEService sInstance;
    private IBinder mIdentifiyingToken;
    private KeyboardView mKeyboardView;
    private Keyboard mKeyboard;
    private boolean mCaps = false;
    private boolean mReadyForInput= false;

    @Override
    public void onCreate() {
        EVIACAMIME.debugInit();
        EVIACAMIME.debug("EViacamIMEService: onCreate");
        sInstance= this;
        super.onCreate();
    }
    
    @Override
    public void onDestroy() {
        EVIACAMIME.debug("EViacamIMEService: onDestroy");
        super.onDestroy();
        sInstance= null;
        mKeyboardView = null;
        mKeyboard= null;
    }
    
    @Override
    public View onCreateInputView() {
        EVIACAMIME.debug("EViacamIMEService: onCreateInputView");
        mKeyboardView = (KeyboardView) getLayoutInflater().inflate(
                R.layout.keyboard, null);
        mKeyboard = new Keyboard(this, R.xml.qwerty);
        mKeyboardView.setKeyboard(mKeyboard);
        mKeyboardView.setOnKeyboardActionListener(this);
        debugViewStateChanges(mKeyboardView);
        return mKeyboardView;
    }
    
    @Override
    public void onStartInputView (EditorInfo info, boolean restarting) {
        EVIACAMIME.debug("EViacamIMEService: onStartInputView");
        mReadyForInput= true;
        super.onStartInputView(info, restarting);
        
    }

    @Override
    public void onFinishInputView (boolean finishingInput) {
        EVIACAMIME.debug("EViacamIMEService: onFinishInputView");
        mReadyForInput= false;
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        EVIACAMIME.debug("EViacamIMEService: onKey");
        
        //debugDump();
        
        InputConnection ic = getCurrentInputConnection();
        playClick(primaryCode);
        switch (primaryCode) {
        case Keyboard.KEYCODE_DELETE:
            ic.deleteSurroundingText(1, 0);
            break;
        case Keyboard.KEYCODE_SHIFT:
            mCaps = !mCaps;
            mKeyboard.setShifted(mCaps);
            mKeyboardView.invalidateAllKeys();
            break;
        case Keyboard.KEYCODE_DONE:
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,
                    KeyEvent.KEYCODE_ENTER));
            break;
        default:
            char code = (char) primaryCode;
            if (Character.isLetter(code) && mCaps) {
                code = Character.toUpperCase(code);
            }
            ic.commitText(String.valueOf(code), 1);
        }
    }

    private void playClick(int keyCode) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        switch (keyCode) {
        case 32: // whitespace
            am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR);
            break;
        case Keyboard.KEYCODE_DONE:
        case 10: // intro
            am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN);
            break;
        case Keyboard.KEYCODE_DELETE:
            am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE);
            break;
        default:
            am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }
    
    /**
     * Performs a click on the location (x, y) when possible
     * @param x - abscissa coordinate of the point (relative to the screen)
     * @param y - ordinate coordinate of the point (relative to the screen)
     * @return true if the point is within view bounds of the IME, false otherwise
     * 
     * Needs to be static because is called from an external service
     */
    public static boolean click(int x, int y) {
        // is the IME has not been create just return false
        if (sInstance == null) return false;
        
        if (!sInstance.mReadyForInput) return false;
        
        InputConnection ic = sInstance.getCurrentInputConnection();
        if (ic == null) return false;
        
        // has clicked inside the keyboard?
        if (sInstance.mKeyboardView == null) return false;
        int coord[]= new int[2];
        sInstance.mKeyboardView.getLocationOnScreen(coord);
        if (x < coord[0] || y < coord[1]) return false;
        
        // adjust coordinates relative to the edge of the keyboard
        x= x - coord[0];
        y= y - coord[1];
        
        Keyboard.Key k= sInstance.getKeyBelow (x, y);
        if (k != null) {
            sInstance.onKey(k.codes[0], k.codes);
        }
        
        return true;
    }

    private Key getKeyBelow (int x, int y) {
        if (mKeyboard == null) return null;

        // keys near the given point
        int[] keys= mKeyboard.getNearestKeys ((int) x, (int) y);

        for (int i : keys) {
            Keyboard.Key k= mKeyboard.getKeys().get(i);
            if (k.isInside(x, y)) return k;
        }
        
        return null;
    }
    
    /**
     * Opens the IME
     *  
     * Needs to be static because is called from an external service
     */
    public static void openIME() {
        // is the IME has not been create just returns
        if (sInstance == null) return;
        
        // no identifying token? should not happen but just in case
        if (sInstance.mIdentifiyingToken == null) return;
        
        InputMethodManager imm= 
                (InputMethodManager) sInstance.getSystemService(INPUT_METHOD_SERVICE);
        
        imm.showSoftInputFromInputMethod(
                sInstance.mIdentifiyingToken, InputMethodManager.SHOW_FORCED);
    }

    /**
     * Closes the IME
     *
     * Needs to be static because is called from an external service
     */
    public static void closeIME() {
        // is the IME has not been create just returns
        if (sInstance == null) return;

        // no identifying token? should not happen but just in case
        if (sInstance.mIdentifiyingToken == null) return;

        InputMethodManager imm=
                (InputMethodManager) sInstance.getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromInputMethod(sInstance.mIdentifiyingToken, 0);
    }

    /** 
     * Trick to obtain the identifying token given to the input method when it is started 
     */
    @Override
    public AbstractInputMethodService.AbstractInputMethodImpl onCreateInputMethodInterface() {
        return new AbstractInputMethodHook(super.onCreateInputMethodInterface());
    }
    
    class AbstractInputMethodHook extends AbstractInputMethodService.AbstractInputMethodImpl {
        private AbstractInputMethodService.AbstractInputMethodImpl mBase;

        AbstractInputMethodHook(AbstractInputMethodService.AbstractInputMethodImpl impl) {
            super();
            this.mBase = impl;
        }
        
        /** Needed token is supplied here */
        @Override
        public void attachToken(IBinder token) {
            mBase.attachToken(token);
            mIdentifiyingToken= token;            
        }
        
        /** Following methods just delegate to base */
        @Override
        public void bindInput(InputBinding binding) {
            mBase.bindInput(binding);            
        }

        @Override
        public void unbindInput() {
            mBase.unbindInput();
        }

        @Override
        public void startInput(InputConnection inputConnection, EditorInfo info) {
            mBase.startInput(inputConnection, info);
        }

        @Override
        public void restartInput(InputConnection inputConnection,
                EditorInfo attribute) {
            mBase.restartInput(inputConnection, attribute);
        }

        @Override
        public void showSoftInput(int flags, ResultReceiver resultReceiver) {
            mBase.showSoftInput(flags, resultReceiver);
        }

        @Override
        public void hideSoftInput(int flags, ResultReceiver resultReceiver) {
            mBase.hideSoftInput(flags, resultReceiver);
        }

        @Override
        public void changeInputMethodSubtype(InputMethodSubtype subtype) {
            mBase.changeInputMethodSubtype(subtype);
        }
    }
    
    //////////////// DEBUG CODE
    private void debugViewStateChanges (View v) {
        v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                EVIACAMIME.debug("onViewAttachedToWindow");
                if (mKeyboardView == v) {
                    EVIACAMIME.debug("YEP!");
                }
                else {
                    EVIACAMIME.debug("NOPE!");
                }
                //debugDump();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                EVIACAMIME.debug("onViewDetachedFromWindow");
            }
        });
    }
    
    @Override
    public void onBindInput () {
        EVIACAMIME.debug("EViacamIMEService: onBindInput");
        super.onBindInput();
    }
    
    @Override
    public View onCreateCandidatesView() {
        EVIACAMIME.debug("EViacamIMEService: onCreateCandidatesView");
        return super. onCreateCandidatesView();
    }
    
    @Override
    public void onStartInput (EditorInfo attribute, boolean restarting) {
        EVIACAMIME.debug("EViacamIMEService: onStartInput");
        super.onStartInput(attribute, restarting);
    }
    
    @Override
    public void onFinishInput () {
        EVIACAMIME.debug("EViacamIMEService: onFinishInput");
        super.onFinishInput();
    }   
    
    @Override
    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        EVIACAMIME.debug("EViacamIMEService: onCurrentInputMethodSubtypeChanged");
        super.onCurrentInputMethodSubtypeChanged(newSubtype);
    }
    
    @Override
    public void onPress(int primaryCode) {
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeUp() {
    }
    
    private void debugDump() {
        AccessibilityNodeInfo node= mKeyboardView.createAccessibilityNodeInfo();
        AccessibilityNodeDebug.displayFullTree(node);
        
        node= AccessibilityNodeInfo.obtain(mKeyboardView);
        AccessibilityNodeDebug.displayFullTree(node);
        
        // this return null
        AccessibilityNodeProvider provider= mKeyboardView.getAccessibilityNodeProvider();
        EVIACAMIME.debug("provider: " + provider);
        
        ViewUtils.dumpViewGroupHierarchy(mKeyboardView);
        
        int coord[]= new int[2];
        mKeyboardView.getLocationOnScreen(coord);
        EVIACAMIME.debug("Coord: " + coord[0] + ", " + coord[1]);
        EVIACAMIME.debug("Size: " + mKeyboardView.getWidth() + " " + mKeyboardView.getHeight());
        
        EVIACAMIME.debug("Keyboard height: " + mKeyboard.getHeight());

        for (Key key : mKeyboard.getKeys()) {
            EVIACAMIME.debug("KEY: " + key.label+ " (" + key.x + " " + key.y + ")");
        }
    }
}
