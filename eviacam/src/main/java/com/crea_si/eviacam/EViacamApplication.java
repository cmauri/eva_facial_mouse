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
/*
 * Support to catch unexpected exceptions
 * TODO: disable/halt service after an error
 */
package com.crea_si.eviacam;

import android.app.Application;
import android.os.Process;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

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

public class EViacamApplication extends Application {

    public void onCreate() {
        super.onCreate();

        Log.d(EVIACAM.TAG, "EVA application started");

        Analytics.init(this);

        // Raise priority to improve responsiveness
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);

        ACRA.init(this);
    }
}
