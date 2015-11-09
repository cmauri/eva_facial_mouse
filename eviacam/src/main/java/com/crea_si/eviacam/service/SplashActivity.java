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
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Displays an splash screen, and checks and guides the installation of the openCV manager.
 * Does it here (activity) so that installation dialog could be properly displayed.
 */
public class SplashActivity extends Activity implements OpenCVInstallHelper.Listener {
    /* Duration of the splash */
    private static final int SPLASH_DISPLAY_LENGTH = 2000;

    /*
     * Stores whether openCV has been initialized.
     * static to "survive" among different activity instantiations.
     */
    private static boolean sOpenCVReady = false;
    
    /* OpenCV installation helper */
    private OpenCVInstallHelper mHelper;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.splash_layout);
        //mHelper= new OpenCVInstallHelper(this, this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (sOpenCVReady) {
            MainEngine.initCVReady();

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
            mHelper= new OpenCVInstallHelper(this, this);
        }
    }

    @Override
    public void onOpenCVInstallSuccess() {
        sOpenCVReady= true;

        /**
         * Restart this activity so that it does not show up in recents
         * nor when pressing back button
         */
        Intent dialogIntent = new Intent(this, SplashActivity.class);
        dialogIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(dialogIntent);
    }

    @Override
    public void onOpenCVInstallCancel() {
        SplashActivity.this.finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHelper!= null) mHelper.cleanup();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        if (mHelper!= null) mHelper.cleanup();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mHelper!= null) mHelper.cleanup();
    }
}
