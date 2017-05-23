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
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.a11yservice.TheAccessibilityService;

/**
 * Launcher activity
 */

public class LauncherActivity extends Activity {

    /* Keep references to the dialogs to properly dismiss them. See:
       http://stackoverflow.com/questions/2850573/activity-has-leaked-window-that-was-originally-added */
    Dialog mHelpDialog;
    Dialog mNoA11ySettingsDialog;

    /**
     * Display message for no accessibility settings available
     */
    private void noAccessibilitySettingsAlert() {
        final Resources r= getResources();
        mNoA11ySettingsDialog= new AlertDialog.Builder(this)
            .setMessage(r.getText(R.string.launcher_no_accessibility_settings))
            .setPositiveButton(r.getText(R.string.launcher_done),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            finish();
                        }
                    })
            .create();
        mNoA11ySettingsDialog.setCancelable(false);
        mNoA11ySettingsDialog.setCanceledOnTouchOutside(false);
        mNoA11ySettingsDialog.show();
    }

    /**
     * Open the accessibility settings screen
     *
     * @return true on success
     */
    private boolean openAccessibility() {
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            startActivity(intent, null);
        }
        catch(ActivityNotFoundException e) {
            return false;
        }
        return true;
    }

    private void showLauncherHelp () {
        View checkBoxView = View.inflate(this, R.layout.launcher_help, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.get().setShowLauncherHelp(!isChecked);
            }
        });

        Resources r= getResources();
        mHelpDialog= new AlertDialog.Builder(this)
        .setMessage(r.getText(R.string.launcher_how_to_run))
        .setView(checkBoxView)
        .setPositiveButton(r.getText(R.string.launcher_open_a11y_settings),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        if (!openAccessibility()) noAccessibilitySettingsAlert();
                    }
                })
        .create();
        mHelpDialog.setCancelable(false);
        mHelpDialog.setCanceledOnTouchOutside(false);
        mHelpDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Preferences.initForA11yService(this) == null) return;

        TheAccessibilityService service= TheAccessibilityService.get();
        if (null != service) {
            /* Engine running, open notifications */
            service.openNotifications();
            finish();
        }
        else {
            if (Preferences.get().getShowLauncherHelp()) {
                showLauncherHelp();
            } else {
                if (!openAccessibility()) noAccessibilitySettingsAlert();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mHelpDialog!= null) {
            mHelpDialog.dismiss();
            mHelpDialog= null;
        }

        if (mNoA11ySettingsDialog!= null) {
            mNoA11ySettingsDialog.dismiss();
            mNoA11ySettingsDialog= null;
        }

        Preferences.get().cleanup();

        finish();
    }
}
