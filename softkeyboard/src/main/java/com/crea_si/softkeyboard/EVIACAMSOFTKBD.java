package com.crea_si.softkeyboard;

/***
 * Constants and common stuff
 */
class EVIACAMSOFTKBD {
    public static final String TAG = EVIACAMSOFTKBD.class.getSimpleName();

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private static final boolean ATTACH_DEBUGGER = DEBUG;

    private static final boolean DEBUG_MESSAGES = DEBUG;

    public static void debugInit() {
        if (!ATTACH_DEBUGGER) return;
        android.os.Debug.waitForDebugger();
    }
}
