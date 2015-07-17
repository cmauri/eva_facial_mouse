package com.crea_si.softkeyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.view.View;
import android.view.inputmethod.InputMethodSubtype;

public class InputViewManager {
    public static final int QWERTY_LAYOUT = 1;
    public static final int SYMBOLS_LAYOUT = 2;
    public static final int SYMBOLS_SHIFT_LAYOUT = 3;
    public static final int NAVIGATION_LAYOUT = 4;

    final private InputMethodService mIMEService;
    private LatinKeyboardView mInputView;
    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;
    private LatinKeyboard mQwertyKeyboard;
    private LatinKeyboard mNavigationKeyboard;
    private LatinKeyboard mCurKeyboard;
    private int mLastDisplayWidth;
    private boolean mCapsLock;

    public InputViewManager(InputMethodService ime) {
        mIMEService= ime;

        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = mIMEService.getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        createKeyboards (subtype);
    }
    
    /**
     * Create the keyboards according to the current subtype
     *
     * TODO: there is probably a better way to choose between different
     * keyboard layouts when changing the subtype (e.g. when changing language)
     */
    private void createKeyboards (InputMethodSubtype subtype) {
        boolean needUpdateCurrent= false;
        if (mCurKeyboard != null && mCurKeyboard == mQwertyKeyboard) {
            needUpdateCurrent= true;
        }

        final String locale= subtype.getLocale();
        if (locale.compareTo("es")== 0) {
            mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty_es);
        }
        else if (locale.compareTo("ca")== 0) {
            mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty_ca);
        }
        else {
            mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty);
        }

        //mSymbolsKeyboard = new LatinKeyboard(this, R.xml.symbols);
        mSymbolsKeyboard = new LatinKeyboard(mIMEService, R.xml.symbols);
        mSymbolsShiftedKeyboard = new LatinKeyboard(mIMEService, R.xml.symbols_shift);
        mNavigationKeyboard = new LatinKeyboard(mIMEService, R.xml.navigation);

        if (needUpdateCurrent) mCurKeyboard = mQwertyKeyboard;
    }
    
    public View createView(KeyboardView.OnKeyboardActionListener listener) {
        mInputView = (LatinKeyboardView) mIMEService.getLayoutInflater().inflate(
                R.layout.input, null);
        mInputView.setOnKeyboardActionListener(listener);
        mInputView.setKeyboard(mQwertyKeyboard);
        return mInputView;
    }
    
    public void selectLayout(int type) {
        
    }
    
    public void finishInput() {
        if (mInputView != null) {
            mInputView.closing();
        }
    }
}