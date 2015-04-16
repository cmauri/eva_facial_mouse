package com.crea_si.eviacam.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/***
 * Constants and common stuff 
 */
public class EVIACAM {
    public static final String TAG = EVIACAM.class.getSimpleName();
    
    private static final boolean DEBUG = BuildConfig.DEBUG;
    
    private static final boolean ATTACH_DEBUGGER = DEBUG;
    
    private static final boolean DEBUG_MESSAGES = DEBUG;
    
    private static HeartBeat sHeartBeat;
    
    public static void debugInit(Context c) {
        if (!ATTACH_DEBUGGER) return;
        android.os.Debug.waitForDebugger();
        Toast.makeText(c, "onServiceConnected", Toast.LENGTH_SHORT).show();
        sHeartBeat = new HeartBeat(c, "eviacam alive");
        sHeartBeat.start();
    }
    
    public static void debugCleanup() {
        if (sHeartBeat == null) return;
        sHeartBeat.stop();
        sHeartBeat= null;
    }
    
    public static void debug(String message) {
        if (!DEBUG_MESSAGES) return;
        Log.d(TAG, message);
    }
}
