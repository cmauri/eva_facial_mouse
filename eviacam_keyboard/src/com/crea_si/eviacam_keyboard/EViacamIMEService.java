package com.crea_si.eviacam_keyboard;

import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodSubtype;

public class EViacamIMEService extends InputMethodService implements
        OnKeyboardActionListener {

    private KeyboardView mKeyboardView;
    private Keyboard mKeyboard;
    private boolean mCaps = false;
//    private ServiceNotification mServiceNotification;
    private MessengerService mMessengerService;
    
    private static InputMethodService gInputMethodService;
    static public InputMethodService getInstance() {
        return gInputMethodService;
    }
    
    @Override
    public void onCreate() {
        gInputMethodService= this;
        EVIACAMIME.debug("onCreate");
        super.onCreate();
        android.os.Debug.waitForDebugger();
        //mServiceNotification= new ServiceNotification(this);
        EVIACAMIME.debug("Class name:" + MessengerService.class.getName()); 
        mMessengerService= new MessengerService();
    }
    
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        EVIACAMIME.debug("onStartCommand");
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onInitializeInterface () {
        EVIACAMIME.debug("onInitializeInterface");
    }
    
    @Override
    public View onCreateInputView() {
        EVIACAMIME.debug("onCreateInputView");
        mKeyboardView = (KeyboardView) getLayoutInflater().inflate(
                R.layout.keyboard, null);
        mKeyboard = new Keyboard(this, R.xml.qwerty);
        mKeyboardView.setKeyboard(mKeyboard);
        mKeyboardView.setOnKeyboardActionListener(this);
        
        
        mKeyboardView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {

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
        
        return mKeyboardView;
    }
    
    public void onBindInput () {
        EVIACAMIME.debug("onBindInput");
        super.onBindInput();
    }
    
    
    @Override
    public View onCreateCandidatesView() {
        EVIACAMIME.debug("onCreateCandidatesView");
        return super. onCreateCandidatesView();
    }
    
    @Override
    public void onStartInput (EditorInfo attribute, boolean restarting) {
        EVIACAMIME.debug("onStartInput");
        super.onStartInput(attribute, restarting);
    }
    
    @Override
    public void onStartInputView (EditorInfo info, boolean restarting) {
        EVIACAMIME.debug("onStartInputView");
        super.onStartInputView(info, restarting);
    }

    @Override
    protected void onCurrentInputMethodSubtypeChanged(InputMethodSubtype newSubtype) {
        EVIACAMIME.debug("onCurrentInputMethodSubtypeChanged");
        super.onCurrentInputMethodSubtypeChanged(newSubtype);
    }
    
    @Override
    public void onFinishInput () {
        EVIACAMIME.debug("onFinishInput");
        super.onFinishInput();
    }
    
    @Override
    public void onDestroy() {
        EVIACAMIME.debug("onDestroy");
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        EVIACAMIME.debug("onKey");
        
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
