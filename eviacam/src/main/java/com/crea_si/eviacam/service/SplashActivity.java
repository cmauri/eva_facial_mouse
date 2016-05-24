/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-16 Cesar Mauri Loba (CREA Software Systems)
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

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.crea_si.eviacam.Eula;
import com.crea_si.eviacam.R;

/**
 * Displays an splash screen, and checks and guides the installation of the openCV manager.
 * Does it here (activity) so that installation dialog could be properly displayed.
 */
public class SplashActivity extends Activity
        implements Eula.Listener, ActivityCompat.OnRequestPermissionsResultCallback {

    /* Parameters for the intents of this activity */
    public static final String IS_A11Y_SERVICE_PARAM= "isA11yService";
    private static final String IS_SECOND_RUN_PARAM = "isSecondRun";

    /* Request codes for permission request callback */
    private static final int CAMERA_PERMISSION_REQUEST= 10000;
    private static final int MANAGE_OVERLAY_PERMISSION_REQUEST= 10001;

    /* Duration of the splash */
    private static final int SPLASH_DISPLAY_LENGTH = 2000;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.splash_layout);

        if (isSecondRun()) {
            /**
             * Close this splash after some seconds.
             */
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run() {
                    SplashActivity.this.finish();
                }
            }, SPLASH_DISPLAY_LENGTH);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isSecondRun()) checkRequisites();
    }

    /**
     * Is the second time this activity is instantiated?
     *
     * @return
     */
    private boolean isSecondRun() {
        Intent i= SplashActivity.this.getIntent();
        return i.getBooleanExtra(SplashActivity.IS_SECOND_RUN_PARAM, false);
    }

    /**
     * Was initiated from the accessibility service?
     *
     * @return
     */
    private boolean isA11YService() {
        Intent i= SplashActivity.this.getIntent();
        return i.getBooleanExtra(SplashActivity.IS_A11Y_SERVICE_PARAM, false);
    }

    private void checkRequisites() {
        if (!Eula.wasAccepted(this)) {
            Eula.acceptEula(this, this);
            return;
        }

        if (checkPermissions()) {
            // If all permissions granted resume service initialization
            // TODO: remove such ugly static method call
            MainEngine.splashReady(isA11YService());

            /**
             * Restart this activity so that it does not show up in recents
             * nor when pressing back button
             */
            Intent dialogIntent = new Intent(this, SplashActivity.class);
            dialogIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK |
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS |
                            Intent.FLAG_ACTIVITY_NO_HISTORY);
            dialogIntent.putExtra(IS_A11Y_SERVICE_PARAM, isA11YService());
            dialogIntent.putExtra(IS_SECOND_RUN_PARAM, true);
            startActivity(dialogIntent);
        }
    }

    /**
     * Check permissions and ask user when necessary
     *
     * @return true if the user granted all permissions, false when some permissions are
     *         missing and therefore need to wait for the user
     */
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Nothing to do for versions below API 23
            return true;
        }

        // Camera permission
        final boolean hasCameraPerm=
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED;

        if (!hasCameraPerm) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            return false;
        }

        // Manage overlay permission
        final boolean hasManageOverlayPerm= Settings.canDrawOverlays(this);

        if (!hasManageOverlayPerm) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, MANAGE_OVERLAY_PERMISSION_REQUEST);
            return false;
        }



        return true;
    }

    @Override
    public void onAcceptEula() {
        checkRequisites();
    }

    @Override
    public void onCancelEula() {
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[],
                                           int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length== 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                finish();
            }
            else {
                checkRequisites();
            }
        }
    }

    @Override
    @TargetApi(23)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode== MANAGE_OVERLAY_PERMISSION_REQUEST) {
            if (!Settings.canDrawOverlays(this)) {
                finish();
            }
            else {
                checkRequisites();
            }
        }
    }
}
