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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.CheckBox;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.R;

/**
 * Utilities to check if EULA has been accepted and display it
 */
public class Eula {

    public interface Listener {
        void onAcceptEula();
        void onCancelEula();
    }
    private static final String EULA_PREFIX = "eula_";

    // the EULA_KEY changes every time you increment the version number
    private static final String EULA_KEY = EULA_PREFIX + BuildConfig.VERSION_CODE;

    static boolean wasAccepted(@NonNull final Activity a) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
        return prefs.getBoolean(EULA_KEY, false);
    }

    static
    public void acceptEula (final Activity a, final Listener l) {
        if (Eula.wasAccepted(a)) {
            l.onAcceptEula();
            return;
        }

        View eulaView = View.inflate(a, R.layout.eula, null);
        final CheckBox checkBox = (CheckBox) eulaView.findViewById(R.id.checkbox);

        // Show the Eula
        String title = a.getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME;

        Dialog dialog= new AlertDialog.Builder(a)
            .setTitle(title)
            .setView(eulaView)
            .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Mark this version as read.
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(EULA_KEY, true);
                    editor.apply();
                    dialogInterface.dismiss();
                    Preferences.get().setACRAEnabled(checkBox.isChecked());
                    l.onAcceptEula();
                }
            })
            .setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    l.onCancelEula();
                }
            })
            .create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
