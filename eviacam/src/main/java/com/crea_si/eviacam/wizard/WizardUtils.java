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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.service.AccessibilityServiceModeEngine;
import com.crea_si.eviacam.service.MainEngine;

class WizardUtils {
    static void finishWizard(Activity a) {
        a.startActivity(new Intent(a, WizardActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                  Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS));
        a.finish();
    }

    static void fullStartEngine() {
        AccessibilityServiceModeEngine engine=
                MainEngine.getInstance().getAccessibilityServiceModeEngine();
        if (engine!= null && engine.isReady()) {
            engine.enableAll();
            engine.start();
        }
    }

    static void checkEngineAndFinishIfNeeded (final Activity a) {
        AccessibilityServiceModeEngine engine=
                MainEngine.getInstance().getAccessibilityServiceModeEngine();
        if (engine== null || !engine.isReady()) {
            // Engine is not ready anymore
            final Resources res= a.getResources();
            AlertDialog ad = new AlertDialog.Builder(a).create();
            ad.setCancelable(false); // This blocks the 'BACK' button
            ad.setTitle(res.getText(R.string.eva_not_running));
            ad.setMessage(res.getText(R.string.eva_not_running_summary));
            ad.setButton(
                DialogInterface.BUTTON_NEUTRAL, res.getText(R.string.close),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishWizard(a);
                    }
                });
            ad.show();
        }
    }
}
