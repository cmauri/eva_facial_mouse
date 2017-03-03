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
package com.crea_si.eviacam.common;

import android.app.Application;
import android.os.Process;
import android.support.annotation.Nullable;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.util.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Annotation for ACRA
 */
@ReportsCrashes(
        mailTo = "eva.facial.mouse@gmail.com",
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.LOGCAT },
        logcatArguments = { "-t", "100", "-v", "time" },
        mode = ReportingInteractionMode.DIALOG,
        resToastText = com.crea_si.eviacam.R.string.crash_toast_text,
        resDialogText = com.crea_si.eviacam.R.string.crash_dialog_text,
        resDialogCommentPrompt = com.crea_si.eviacam.R.string.crash_dialog_comment_prompt,
        resDialogOkToast = com.crea_si.eviacam.R.string.crash_dialog_ok_toast
)

/**
 * Customized application class
 */
@SuppressWarnings("FieldCanBeLocal")
public class EViacamApplication extends Application {

    /* Names of the process that are part of the application.
       The main process has no specific name */
    private static String ACRA_PROC = ":acra";
    private static String SOFTKEYBOARD_PROC = ":softkeyboard";

    public void onCreate() {
        super.onCreate();

        /* Get the name of the process in which this Application instance is started */
        String processName= getCurrentProcessName();
        Log.d(EVIACAM.TAG, "Application EVA Facial Mouse started. Process: " + processName);

        if (processName!= null && processName.endsWith(ACRA_PROC)) {
            /* ACRA crash report process. Nothing else to do */
            return;
        }

        /*
         * Regular application start up. Register uncaught exception handlers. First the one
         * provided by ACRA and after that our own handler. In case of uncaught exception our
         * handler is called first so that it can filter some exceptions.
         */
        ACRA.init(this);
        UncaughtExceptionHandler.init();

        if (processName!= null && processName.endsWith(SOFTKEYBOARD_PROC)) {
            /* Softkeyboard started. Nothing else to do */
            return;
        }

        /*
         * EVA service regular start up
         */

        Analytics.init(this);

        // Raise priority to improve responsiveness
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
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
}
