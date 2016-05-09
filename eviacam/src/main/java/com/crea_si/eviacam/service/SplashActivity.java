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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.crea_si.eviacam.Eula;
import com.crea_si.eviacam.R;

/**
 * Displays an splash screen, and checks and guides the installation of the openCV manager.
 * Does it here (activity) so that installation dialog could be properly displayed.
 */
public class SplashActivity extends Activity implements Eula.Listener {
    public static final String IS_A11Y_SERVICE_PARAM= "isA11yService";

    /* Duration of the splash */
    private static final int SPLASH_DISPLAY_LENGTH = 2000;

    /*
     * Stores whether the splash screen has been displayed
     * is static to "survive" among different activity instantiations.
     */
    private static boolean sSplashShown = false;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.splash_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Eula.acceptEula(this, this);
    }

    @Override
    public void onAcceptEula() {
        // get is splash created from an accessibility service
        Intent i= SplashActivity.this.getIntent();
        boolean isA11yService= i.getBooleanExtra(SplashActivity.IS_A11Y_SERVICE_PARAM, true);

        if (sSplashShown) {
            // TODO: remove such ugly static method call
            MainEngine.splashReady(isA11yService);

            /**
             * New Handler to close this splash after some seconds.
             */
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run() {
                    SplashActivity.this.finish();
                }
            }, SPLASH_DISPLAY_LENGTH);
        }
        else {
            sSplashShown = true;

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
            dialogIntent.putExtra(IS_A11Y_SERVICE_PARAM, isA11yService);
            startActivity(dialogIntent);
        }
    }

    @Override
    public void onCancelEula() {
        finish();
    }
}
