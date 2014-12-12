package com.crea_si.eviacam.service;

import android.util.Log;

/***
 * Constants and common stuff 
 */
public class EVIACAM {
    public static final String TAG = EVIACAM.class.getSimpleName();
    
    public static final boolean DEBUG = true;
    
    public static void debug(String message) {
        if ( DEBUG ) Log.d(TAG, message);
    }
}
