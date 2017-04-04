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
package com.crea_si.eviacam.wizard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.a11yservice.AccessibilityServiceModeEngine;
import com.crea_si.eviacam.EngineSelector;
import com.crea_si.eviacam.common.EVIACAM;

public class WizardUtils {
    public static final String WIZARD_CLOSE_EVENT_NAME= "wizard-closed-event";

    static void finishWizard(Activity a) {
        a.startActivity(new Intent(a, WizardActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                  Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        a.finish();
    }

    static void fullStartEngine(Context c) {
        AccessibilityServiceModeEngine engine= EngineSelector.getAccessibilityServiceModeEngine();
        if (engine!= null && engine.isReady()) {
            engine.enableAll();
            engine.start();

            // Notify AccessibilityServiceModeEngineImpl the wizard has finished
            Intent intent = new Intent(WIZARD_CLOSE_EVENT_NAME);
            LocalBroadcastManager.getInstance(c).sendBroadcast(intent);
        }
    }

    static AccessibilityServiceModeEngine checkEngineAndFinishIfNeeded (final Activity a) {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "checkEngineAndFinishIfNeeded");

        if (a== null) {
            // This means the activity does not exists anymore
            return null;
        }
        AccessibilityServiceModeEngine engine= EngineSelector.getAccessibilityServiceModeEngine();
        if (engine== null || !engine.isReady()) {
            /* Engine is not ready anymore */
            final Resources res = a.getResources();
            AlertDialog ad = new AlertDialog.Builder(a).create();
            ad.setCancelable(false); // This blocks the 'BACK' button
            ad.setTitle(res.getText(R.string.wizard_eva_not_running));
            ad.setMessage(res.getText(R.string.wizard_eva_not_running_summary));
            ad.setButton(
                    DialogInterface.BUTTON_POSITIVE, res.getText(R.string.close),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finishWizard(a);
                        }
                    });
            ad.show();

            return null;
        }

        return engine;
    }
}
