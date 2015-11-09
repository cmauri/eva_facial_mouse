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
package com.crea_si.eviacam.service;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Process;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes( 
    mailTo = "cesar@crea-si.com",
    mode = ReportingInteractionMode.DIALOG,
    resToastText = R.string.crash_toast_text,
    resDialogText = R.string.crash_dialog_text,
    resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
    resDialogOkToast = R.string.crash_dialog_ok_toast
)

public class EViacamApplication extends Application {
    private SharedPreferences mSharedPreferences;
    
    public void onCreate() {
        super.onCreate();

        // Raise priority to improve responsiveness
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);

        ACRA.init(this);
    }

    public SharedPreferences getSharedPreferences() {
        return mSharedPreferences;
    }

    public void setSharedPreferences(SharedPreferences sp) {
        mSharedPreferences= sp;
    }
}
