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
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import com.crea_si.eviacam.BuildConfig;
/**
 * Customized application class
 */

/* Annotation for ACRA */
@ReportsCrashes(
        formUri = "http://eva.crea-si.com/submit.php",
        mode = ReportingInteractionMode.TOAST,
        customReportContent = {
                ReportField.APP_VERSION_CODE,
                ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION,
                ReportField.PACKAGE_NAME,
                ReportField.REPORT_ID,
                ReportField.BUILD,
                ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.LOGCAT },
        logcatArguments = { "-t", "100", "-v", "time" },
        resToastText = com.crea_si.eviacam.R.string.crash_toast_text,
        resDialogText = com.crea_si.eviacam.R.string.crash_dialog_text,
        resDialogCommentPrompt = com.crea_si.eviacam.R.string.crash_dialog_comment_prompt,
        resDialogOkToast = com.crea_si.eviacam.R.string.crash_dialog_ok_toast
)

public class EViacamApplication extends Application {

    public void onCreate() {
        super.onCreate();

        Log.i(EVIACAM.TAG, "Application EVA Facial Mouse started");

        if (EVIACAM.isACRAProcess()) {
            /* ACRA crash report process. Nothing else to do */
            Log.i(EVIACAM.TAG, "ACRA crash report process");
            return;
        }

        /*
         * Regular application start up. Register uncaught exception handlers. First the one
         * provided by ACRA and after that our own handler. In case of uncaught exception our
         * handler is called first so that it can filter some exceptions.
         */
        ACRA.init(this);
        ACRA.getErrorReporter().putCustomData("BUILD_ORIGIN", BuildConfig.BUILD_ORIGIN);
        UncaughtExceptionHandler.init(this);

        if (EVIACAM.isSoftkeyboardProcess()) {
            /* Softkeyboard started. Nothing else to do */
            Log.i(EVIACAM.TAG, "Softkeyboard process");
            return;
        }

        /*
         * EVA service regular start up (main process)
         */

        Analytics.init(this);

        // Raise priority to improve responsiveness
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);
    }
}
