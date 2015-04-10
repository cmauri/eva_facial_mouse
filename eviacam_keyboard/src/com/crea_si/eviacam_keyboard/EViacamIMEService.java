package com.crea_si.eviacam_keyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

public class EViacamIMEService extends InputMethodService implements
        OnKeyboardActionListener {

    private KeyboardView mKeyboardView;
    private Keyboard mKeyboard;
    private boolean mCaps = false;

    @Override
    public void onCreate() {
        EVIACAMIME.debug("onCreate");
        super.onCreate();
        android.os.Debug.waitForDebugger();
    }

    @Override
    public View onCreateInputView() {
        EVIACAMIME.debug("onCreateInputView");
        mKeyboardView = (KeyboardView) getLayoutInflater().inflate(
                R.layout.keyboard, null);
        mKeyboard = new Keyboard(this, R.xml.qwerty);
        mKeyboardView.setKeyboard(mKeyboard);
        mKeyboardView.setOnKeyboardActionListener(this);
        return mKeyboardView;
    }

    @Override
    public void onDestroy() {
        EVIACAMIME.debug("onDestroy");
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
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
}
