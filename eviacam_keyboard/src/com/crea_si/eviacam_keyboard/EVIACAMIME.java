package com.crea_si.eviacam_keyboard;

import android.util.Log;

/***
 * Constants and common stuff 
 */
public class EVIACAMIME {
    public static final String TAG = EVIACAMIME.class.getSimpleName();
    
    private static final boolean DEBUG = BuildConfig.DEBUG;
    
    private static final boolean ATTACH_DEBUGGER = false;
    
    private static final boolean DEBUG_MESSAGES = DEBUG;
    
    public static void debugInit() {
        if (!ATTACH_DEBUGGER) return;
        android.os.Debug.waitForDebugger();
    }
    
    public static void debug(String message) {
        if (!DEBUG_MESSAGES) return;
        Log.d(TAG, message);
    }
    
    public static void warning(String message) {
        Log.w(TAG, message);
    }
}
