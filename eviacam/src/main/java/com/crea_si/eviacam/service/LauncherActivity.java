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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.crea_si.eviacam.Preferences;
import com.crea_si.eviacam.R;

/**
 * Launcher activity
 */

public class LauncherActivity extends Activity {

    private void openAccessibility() {
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(intent, 0);

        finish();
    }

    private void showLauncherHelp () {
        final Resources r= getResources();

        new AlertDialog.Builder(this)
        .setTitle(r.getText(R.string.app_name))
        .setMessage(r.getText(R.string.how_to_run))
        .setPositiveButton(r.getText(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        openAccessibility();
                    }
                })
        .setNeutralButton(r.getText(R.string.dont_remember_again),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Preferences.get().setShowLauncherHelp(false);
                        openAccessibility();
                    }
                })
        .show();
    }

    @Override
    protected void onStart () {
        super.onStart();

        if (Preferences.initForA11yService(this) == null) return;

        if (Preferences.get().getShowLauncherHelp()) {
            showLauncherHelp();
        }
        else {
            openAccessibility();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Preferences.get().cleanup();
    }
}
