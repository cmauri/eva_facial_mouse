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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.MyJavaCameraView;
import org.opencv.android.MyOpenCVLoader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;

/**
 * Displays an splash screen, and checks and guides the installation of the openCV manager.
 * Does it here (activity) so that installation dialog could be properly displayed.
 */

public class SplashActivity extends Activity {

    /** Duration of wait */
    private static final int SPLASH_DISPLAY_LENGTH = 4000;
    
    /** openCV capture & view facility */ 
    private CameraBridgeViewBase mCameraView;
    
    /** 
     * Stores whether openCV has been initialized. 
     * Is static to "survive" among different activity instantiations.
     */
    private static boolean sOpenCVReady = false;

    /** Callback for openCV initialization */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
            case LoaderCallbackInterface.SUCCESS:
                manageOpenCVInstallSucess();
                break;
            case LoaderCallbackInterface.INSTALL_CANCELED:
                manageOpenCVInstallCancel();
                break;
            default:
                super.onManagerConnected(status);
            }
        }
    };
    
    /** Handles the case when openCV has been properly installed */
    private void manageOpenCVInstallSucess() {
        EVIACAM.debug("SplashActivity: openCV loaded successfully");

        sOpenCVReady= true;

        /** 
         * Restart this activity so that it does not show up in recents
         * nor when pressing back button
         */
        Intent dialogIntent = new Intent(this, SplashActivity.class);
        dialogIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(dialogIntent);
    }
    
    /** Handles the case when openCV installation has been cancelled */
    private void manageOpenCVInstallCancel() {
        AlertDialog installCancelDlg = new AlertDialog.Builder(this).create();
        installCancelDlg.setTitle(getText(R.string.installation_cancelled));
        installCancelDlg.setMessage(getString(R.string.app_name) + " " + 
                                    getText(R.string.needs_opencv_retry));
        installCancelDlg.setCancelable(false); // This blocks the 'BACK' button
        installCancelDlg.setButton(AlertDialog.BUTTON_POSITIVE, "Retry", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MyOpenCVLoader.initAsync(MyOpenCVLoader.OPENCV_VERSION_2_4_9, 
                                         SplashActivity.this, mLoaderCallback);
            }
        });
        installCancelDlg.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SplashActivity.this.finish();
            }
        });
        
        installCancelDlg.show();
    }
    
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle bundle) {
        EVIACAM.debug("onCreate: SplashActivity");

        super.onCreate(bundle);
        setContentView(R.layout.splash_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        EVIACAM.debug("SplashActivity: onResume");
        
        if (sOpenCVReady) {
            EViacamService.initCVReady();
            
            /** 
             * New Handler to close this splash after some seconds. 
             */
            new Handler().postDelayed(new Runnable(){
                @Override
                public void run() {
                    EVIACAM.debug("SplashActivity: finish after timeout");
                    SplashActivity.this.finish();
                }
            }, SPLASH_DISPLAY_LENGTH);
        }
        else {

            /** Tries to initialize openCV */ 
            mCameraView= new MyJavaCameraView(this, CameraBridgeViewBase.CAMERA_ID_FRONT);
            MyOpenCVLoader.initAsync(MyOpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        EVIACAM.debug("SplashActivity: onPause");
        if (mCameraView != null) {
            mCameraView.disableView();
            mCameraView= null;
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        EVIACAM.debug("SplashActivity: onStop");
        if (mCameraView != null) {
            mCameraView.disableView();
            mCameraView= null;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        EVIACAM.debug("SplashActivity: onDestroy");
        if (mCameraView != null) {
            mCameraView.disableView();
            mCameraView= null;
        }
    }
}
