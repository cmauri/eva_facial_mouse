/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
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

 package com.crea_si.eviacam.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/***
 * Constants and common stuff 
 */
public class EVIACAM {
    public static final String TAG = EVIACAM.class.getSimpleName();
    
    private static final boolean DEBUG = false;
    
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
