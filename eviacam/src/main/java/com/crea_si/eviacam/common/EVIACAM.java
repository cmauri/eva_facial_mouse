/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.crea_si.eviacam.common;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.util.HeartBeat;

import org.acra.util.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;

/***
 * Constants and common stuff 
 */
public class EVIACAM {
    public static final String TAG = EVIACAM.class.getSimpleName();
    
    private static final boolean DEBUG = BuildConfig.DEBUG;
    
    private static final boolean ATTACH_DEBUGGER = DEBUG;
    
    @SuppressWarnings("unused")
    private static final boolean DEBUG_MESSAGES = DEBUG;
    
    private static HeartBeat sHeartBeat;

    private static final Handler sHandler= new Handler();

    /* Names of the process that are part of the application.
       The main process has no specific name */
    private static final String ACRA_PROCESS_NAME = ":acra";
    private static final String SOFTKEYBOARD_PROCESS_NAME = ":softkeyboard";

    /* Constants for each different available processes */
    private static final int UNKNOWN_PROCESS= 0;
    private static final int MAIN_PROCESS= 1;
    private static final int SOFTKEYBOARD_PROCESS= 2;
    private static final int ACRA_PROCESS= 3;

    // Store current running process
    private static int sCurrentProcess = UNKNOWN_PROCESS;
    
    @SuppressWarnings("unused")
    public static void debugInit(Context c) {
        if (!ATTACH_DEBUGGER) return;
        android.os.Debug.waitForDebugger();
        Toast.makeText(c, "onServiceConnected", Toast.LENGTH_SHORT).show();
        sHeartBeat = new HeartBeat(c, "eviacam alive");
        sHeartBeat.start();
    }
    
    @SuppressWarnings("unused")
    public static void debugCleanup() {
        if (sHeartBeat == null) return;
        sHeartBeat.stop();
        sHeartBeat= null;
    }
    
    private static void doToast (Context c, CharSequence t, int duration) {
        final Toast toast = Toast.makeText(c, t, duration);
        /* Try to increase the size of the text */
        try {
            ViewGroup group = (ViewGroup) toast.getView();
            TextView messageTextView = (TextView) group.getChildAt(0);
            messageTextView.setTextSize(25);
        }
        catch (ClassCastException ignored) { }
        toast.show();
    }

    @SuppressWarnings("WeakerAccess")
    public static void Toast (final Context c, final CharSequence t, final int duration) {
        if (Looper.myLooper()== Looper.getMainLooper()) {
            doToast(c, t, duration);
        }
        else {
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    doToast(c, t, duration);
                }
            });
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static void LongToast (Context c, CharSequence t) {
        Toast(c, t, Toast.LENGTH_LONG);
    }

    @SuppressWarnings("WeakerAccess")
    public static void LongToast (Context c, int id) {
        LongToast(c, c.getResources().getString(id));
    }

    @SuppressWarnings("WeakerAccess")
    public static void ShortToast (Context c, CharSequence t) {
        Toast(c, t, Toast.LENGTH_SHORT);
    }

    @SuppressWarnings("WeakerAccess")
    public static void ShortToast (Context c, int id) {
        ShortToast(c, c.getResources().getString(id));
    }

    /**
     * Retrieve the current process name
     *
     * @return process name, can be null
     */
    @Nullable
    private static String getCurrentProcessName() {
        try {
            return IOUtils.streamToString(new FileInputStream("/proc/self/cmdline")).trim();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Initialize the value of the sCurrentProcess field according to the current process name
     */
    private static void setCurrentProcess() {
        String processName= getCurrentProcessName();
        Log.i(EVIACAM.TAG, "Current process:" + processName);

        if (processName== null) return;

        if (processName.endsWith(ACRA_PROCESS_NAME)) {
            sCurrentProcess = ACRA_PROCESS;
        }
        else if (processName.endsWith(SOFTKEYBOARD_PROCESS_NAME)) {
            sCurrentProcess = SOFTKEYBOARD_PROCESS;
        }
        else {
            sCurrentProcess = MAIN_PROCESS;
        }
    }

    /**
     *
     * @return true is this is the main process
     */
    static boolean isMainProcess() {
        if (sCurrentProcess== UNKNOWN_PROCESS) {
            setCurrentProcess();
        }
        return sCurrentProcess== MAIN_PROCESS;
    }

    /**
     *
     * @return true is this is the ACRA reporting process
     */
    static boolean isACRAProcess() {
        if (sCurrentProcess== UNKNOWN_PROCESS) {
            setCurrentProcess();
        }
        return sCurrentProcess== ACRA_PROCESS;
    }

    /**
     *
     * @return true is this is the softkeyboard process
     */
    static boolean isSoftkeyboardProcess() {
        if (sCurrentProcess== UNKNOWN_PROCESS) {
            setCurrentProcess();
        }
        return sCurrentProcess== SOFTKEYBOARD_PROCESS;
    }
}
