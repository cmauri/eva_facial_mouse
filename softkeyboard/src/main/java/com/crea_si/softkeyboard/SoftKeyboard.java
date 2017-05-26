/*
 * Copyright (C) 2008-2009 The Android Open Source Project
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
 *
 *
 * Modified by Cesar Mauri (CREA) 2015-17 for the Enable Viacam for Android project
 */
package com.crea_si.softkeyboard;

import android.app.Dialog;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.IBinder;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic working IME which works in conjunction with Enable Viacam service
 */
public class SoftKeyboard extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {
    /**
     * This boolean indicates the optional example code for performing
     * processing of hard keys in addition to regular text generation
     * from on-screen interaction.  It would be used for input methods that
     * perform language translations (such as converting text entered on
     * a QWERTY keyboard to Chinese), but may not be used for input methods
     * that are primarily intended to be used for on-screen text entry.
     */
    private static final boolean PROCESS_HARD_KEYS = false;
    private static SoftKeyboard sInstance;
    private static final int SWITCH_LANGUAGE_KEYCODE = -101;
    private static final int SWITCH_NAVIGATION = -102;
    private static final int IME_PICKER = -103;

    private StringBuilder mComposing = new StringBuilder();
    private long mMetaState = 0;
    private boolean mReadyForInput = false;

    private InputMethodManager mInputMethodManager;
    private String mWordSeparators;
    private InputViewManager mInputViewManager;

    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;
    private boolean mPredictionOn;
    private boolean mCompletionOn;

    /**
     * Main initialization of the input method component
     */
    @Override
    public void onCreate() {
        if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "SoftKeyboard: onCreate");
        super.onCreate();

        sInstance = this;
        mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.kbd_word_separators);
    }

    /**
     * UI initialization.  It is called after creation and any configuration change.
     */
    @Override
    public void onInitializeInterface() {
        mInputViewManager = new InputViewManager(this);
    }

    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time the input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override
    public View onCreateInputView() {
        if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "onInitializeInterface");
        View v= mInputViewManager.createView(this);
        mInputViewManager.selectSavedLayout();
        return v;
    }

    /**
     * Called by the framework when the view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override
    public View onCreateCandidatesView() {
        // Currently disabled
        return null;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);

        // When navigation layout enabled do not change automatically 
        if (mInputViewManager.getSelectedLayout() == InputViewManager.NAVIGATION_LAYOUT) {
            return;
        }

        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        updateCandidates();

        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }

        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;

        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
            case InputType.TYPE_CLASS_PHONE:
                // Numbers and dates default to the symbols keyboard, with no extra features.
                mInputViewManager.selectLayout(InputViewManager.SYMBOLS_LAYOUT);
                break;

            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing. We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mInputViewManager.selectLayout(InputViewManager.QWERTY_LAYOUT);
                mPredictionOn = true;

                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }

                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS ||
                        variation == InputType.TYPE_TEXT_VARIATION_URI ||
                        variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }

                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }

                // We also want to look at the current state of the editor to 
                // decide whether our alphabetic keyboard should start out shifted.
                mInputViewManager.updateShiftKeyState(attribute);
                break;

            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mInputViewManager.selectLayout(InputViewManager.QWERTY_LAYOUT);
                mInputViewManager.updateShiftKeyState(attribute);
        }
    }

    /*
     * Called when the input view is being shown and input has started on a new editor.
     * This will always be called after onStartInput(EditorInfo, boolean), allowing you
     * to do your general setup there and just view-specific setup here. You are 
     * guaranteed that onCreateInputView() will have been called some time before this 
     * function is called. 
     */
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        mInputViewManager.enableSelected(null);
        mInputViewManager.updateEnterLabel(attribute);
        mReadyForInput = true;
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "onFinishInputView");
        mReadyForInput = false;
        super.onFinishInputView(finishingInput);
        mInputViewManager.saveCurrentLayout();
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override
    public void onFinishInput() {
        super.onFinishInput();

        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();

        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);

        mInputViewManager.closing();
    }

    /**
     * Called by the system to notify a Service that it is no longer used
     * and is being removed.
     */
    @Override
    public void onDestroy() {
        if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "SoftKeyboard: onDestroy");
        sInstance = null;
        super.onDestroy();
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        /*
         * When subtype changes we need to recreate the keyboard layouts and apply them
         *
         */
        mInputViewManager.selectSubtype(subtype);
        mInputViewManager.enableSelected(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd,
                                  int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);

        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override
    public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }

            List<String> stringList = new ArrayList<>();
            for (CompletionInfo ci : completions) {
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }

    /**
     * This translates incoming hard key events in to edit operations on an
     * InputConnection.  It is only needed when using the
     * PROCESS_HARD_KEYS option.
     */
    private boolean translateKeyDown(int keyCode, KeyEvent event) {
        mMetaState = MetaKeyKeyListener.handleKeyDown(mMetaState,
                keyCode, event);
        int c = event.getUnicodeChar(MetaKeyKeyListener.getMetaState(mMetaState));
        mMetaState = MetaKeyKeyListener.adjustMetaAfterKeypress(mMetaState);
        InputConnection ic = getCurrentInputConnection();
        if (c == 0 || ic == null) {
            return false;
        }

        boolean dead = false;

        if ((c & KeyCharacterMap.COMBINING_ACCENT) != 0) {
            dead = true;
            c = c & KeyCharacterMap.COMBINING_ACCENT_MASK;
        }

        if (mComposing.length() > 0) {
            char accent = mComposing.charAt(mComposing.length() - 1);
            int composed = KeyEvent.getDeadChar(accent, c);

            if (composed != 0) {
                c = composed;
                mComposing.setLength(mComposing.length() - 1);
            }
        }

        onKey(c, null);

        return true;
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0) {
                    if (mInputViewManager.handleBack()) {
                        return true;
                    }
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;

            default:
                // For all other keys, if we want to do transformations on
                // text being entered with a hard keyboard, we need to process
                // it and do the appropriate action.
                if (PROCESS_HARD_KEYS) {
                    if (keyCode == KeyEvent.KEYCODE_SPACE
                            && (event.getMetaState() & KeyEvent.META_ALT_ON) != 0) {
                        // A silly example: in our input method, Alt+Space
                        // is a shortcut for 'android' in lower case.
                        InputConnection ic = getCurrentInputConnection();
                        if (ic != null) {
                            // First, tell the editor that it is no longer in the
                            // shift state, since we are consuming this.
                            ic.clearMetaKeyStates(KeyEvent.META_ALT_ON);
                            keyDownUp(KeyEvent.KEYCODE_A, ic);
                            keyDownUp(KeyEvent.KEYCODE_N, ic);
                            keyDownUp(KeyEvent.KEYCODE_D, ic);
                            keyDownUp(KeyEvent.KEYCODE_R, ic);
                            keyDownUp(KeyEvent.KEYCODE_O, ic);
                            keyDownUp(KeyEvent.KEYCODE_I, ic);
                            keyDownUp(KeyEvent.KEYCODE_D, ic);
                            // And we consume this event.
                            return true;
                        }
                    }
                    if (mPredictionOn && translateKeyDown(keyCode, event)) {
                        return true;
                    }
                }
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // If we want to do transformations on text being entered with a hard
        // keyboard, we need to process the up events to update the meta key
        // state we are tracking.
        if (PROCESS_HARD_KEYS) {
            if (mPredictionOn) {
                mMetaState = MetaKeyKeyListener.handleKeyUp(mMetaState,
                        keyCode, event);
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            inputConnection.commitText(mComposing, mComposing.length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        return Character.isLetter(code);
    }

    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode, InputConnection ic) {
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode, InputConnection ic) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER, ic);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0, ic);
                } else {
                    ic.commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (primaryCode >= KeyEvent.KEYCODE_DPAD_UP &&
                primaryCode <= KeyEvent.KEYCODE_DPAD_CENTER ||
                primaryCode == KeyEvent.KEYCODE_PAGE_UP ||
                primaryCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                primaryCode == KeyEvent.KEYCODE_TAB) {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            keyDownUp(primaryCode, ic);
        } else if (primaryCode == -KeyEvent.KEYCODE_MOVE_HOME ||
                primaryCode == -KeyEvent.KEYCODE_MOVE_END ||
                primaryCode == -KeyEvent.KEYCODE_FORWARD_DEL) {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;
            keyDownUp(-primaryCode, ic);
        } else if (isWordSeparator(primaryCode)) {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;

            // Handle separator
            if (mComposing.length() > 0) {
                commitTyped(ic);
            }
            sendKey(primaryCode, ic);
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            mInputViewManager.handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
        } else if (primaryCode == LatinKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or something
        } else if (primaryCode == SWITCH_LANGUAGE_KEYCODE) {
            mInputMethodManager.switchToNextInputMethod(getToken(), true);
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE) {
            mInputViewManager.handleModeChange();
        } else if (primaryCode == SWITCH_NAVIGATION) {
            mInputViewManager.setNavigationKeyboard();
        } else if (primaryCode == IME_PICKER) {
            mInputMethodManager.showInputMethodPicker();
            closeIME();
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    /**
     * Performs a click on the location (x, y) when possible
     *
     * @param x - abscissa coordinate of the point (relative to the screen)
     * @param y - ordinate coordinate of the point (relative to the screen)
     * @return true if the point is within view bounds of the IME, false otherwise
     * <p>
     * Needs to be static because is called from an external service
     */
    public static boolean click(int x, int y) {
        // is the IME has not been create just return false
        if (sInstance == null) return false;

        if (!sInstance.mReadyForInput) return false;

        InputConnection ic = sInstance.getCurrentInputConnection();
        if (ic == null) return false;

        // has clicked inside the keyboard?
        int[] coord = sInstance.mInputViewManager.getKeyboardLocationOnScreen();
        if (coord == null || x < coord[0] || y < coord[1]) return false;

        // adjust coordinates relative to the edge of the keyboard
        x = x - coord[0];
        y = y - coord[1];

        return sInstance.mInputViewManager.performClick(x, y);
    }

    @Override
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        mInputViewManager.updateShiftKeyState(null);
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                ArrayList<String> list = new ArrayList<>();
                list.add(mComposing.toString());
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }

    public void setSuggestions(List<String> suggestions, boolean completions,
                               boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(suggestions, completions, typedWordValid);
        }
    }

    private void handleBackspace() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            ic.setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            mComposing.setLength(0);
            ic.commitText("", 0);
            updateCandidates();
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL, ic);
        }
        mInputViewManager.updateShiftKeyState(null);
    }


    private void handleCharacter(int primaryCode, int[] keyCodes) {
        if (isInputViewShown()) {
            if (mInputViewManager.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (isAlphabet(primaryCode) && mPredictionOn) {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            mInputViewManager.updateShiftKeyState(null);
            updateCandidates();
        } else {
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
        }
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputViewManager.closing();
    }

    public boolean isWordSeparator(int code) {
        return mWordSeparators.contains(String.valueOf((char) code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }

    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            mInputViewManager.updateShiftKeyState(null);
        } else if (mComposing.length() > 0) {
            // If we were generating candidate suggestions for the current
            // text, we would commit one of them here.  But for this sample,
            // we will just commit the current text.
            commitTyped(getCurrentInputConnection());
        }
    }

    /**
     * Opens the IME
     * <p>
     * Needs to be static because is called from an external service
     */
    public static void openIME() {
        // the IME has not been created, just returns
        if (sInstance == null) return;

        IBinder token= sInstance.getToken();

        // no identifying token? should not happen but just in case
        if (token == null) return;

        InputMethodManager imm =
                (InputMethodManager) sInstance.getSystemService(INPUT_METHOD_SERVICE);

        imm.showSoftInputFromInputMethod(token, InputMethodManager.SHOW_FORCED);
    }

    /**
     * Closes the IME
     * <p>
     * Needs to be static because is called from an external service
     */
    public static void closeIME() {
        // is the IME has not been create just returns
        if (sInstance == null) return;

        IBinder token= sInstance.getToken();

        // no identifying token? should not happen but just in case
        if (token == null) return;

        InputMethodManager imm =
                (InputMethodManager) sInstance.getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromInputMethod(token, 0);
    }

    /**
     * Toggle the IME
     * <p>
     * Needs to be static because is called from an external service
     */
    public static void toggleIME() {
        if (sInstance == null) return;

        if (!sInstance.mReadyForInput) openIME();
        else closeIME();
    }

    /**
     * Get the identifying token given to the input method
     *
     * @return the token or null
     */
    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

    @Override
    public void swipeRight() {
        if (mCompletionOn) {
            pickDefaultCandidate();
        }
    }

    @Override
    public void onUpdateExtractingVisibility(EditorInfo ei) {
        setExtractViewShown(false);
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void swipeLeft() {
        handleBackspace();
    }

    @Override
    public void swipeDown() {
        handleClose();
    }

    @Override
    public void swipeUp() {
    }

    @Override
    public void onPress(int primaryCode) {
    }

    @Override
    public void onRelease(int primaryCode) {
    }
}
