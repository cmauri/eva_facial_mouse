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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Manage uncaught exceptions
 *
 * TODO: disable/halt service after an error and stop a11y service for Android 7.0+
 */
class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    private final Context mContext;

    static public void init(@NonNull Context c) {
        new UncaughtExceptionHandler(c);
    }

    /**
     * Private constructor
     *
     * Try to install a uncaught exception handler.
     */
    private UncaughtExceptionHandler(@NonNull Context c) {
        mContext= c;
        Thread.UncaughtExceptionHandler defaultExceptionHandler=
                Thread.getDefaultUncaughtExceptionHandler();

        if (defaultExceptionHandler!= null &&
            defaultExceptionHandler.getClass().isAssignableFrom(UncaughtExceptionHandler.class)) {
            // Already installed
            Log.i(EVIACAM.TAG, "UncaughtExceptionHandler already installed. Ignore request");
            mDefaultExceptionHandler= null;
        }
        else {
            mDefaultExceptionHandler = defaultExceptionHandler;
            Thread.setDefaultUncaughtExceptionHandler(this);
        }
    }

    @Override
    public void uncaughtException(@Nullable Thread thread, @NonNull Throwable ex) {
        Class<?> cls= ex.getClass();
        Log.i(EVIACAM.TAG, "Uncaught exception:" + cls.getName());

        /* Suppress spurious IllegalArgumentException at
           android.view.Surface.nativeUnlockCanvasAndPost */
        if (EVIACAM.isMainProcess() && cls.isAssignableFrom(IllegalArgumentException.class)) {
            StackTraceElement[] stackTrace= ex.getStackTrace();
            StackTraceElement stackTraceElem= null;
            if (stackTrace!= null && stackTrace.length> 0) {
                stackTraceElem= stackTrace[0];
            }

            String className= null;
            if (stackTraceElem!= null) {
                className= stackTraceElem.getClassName();
            }

            if (className!= null && className.equals("android.view.Surface")) {
                Log.i(EVIACAM.TAG, "Uncaught exception filtered");
                endApplication();
                return;
            }
        }

        if (EVIACAM.isMainProcess()) {
            CrashRegister.recordCrash(mContext);
        }

        /* Call default exception handler (if any) */
        if (mDefaultExceptionHandler!= null) {
            mDefaultExceptionHandler.uncaughtException(thread, ex);
        }
    }

    /**
     * Finish the application
     */
    private static void endApplication() {
        // TODO: yes, this is ugly but works
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(-1);
    }
}

