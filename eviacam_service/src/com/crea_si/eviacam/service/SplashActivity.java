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
import android.os.Bundle;
import android.view.View;

public class SplashActivity extends Activity {

    /** Duration of wait */
    private static final int SPLASH_DISPLAY_LENGTH = 4000;
    
    /** opencv capture&view facility */ 
    private CameraBridgeViewBase mCameraView;

    // callback for camera initialization
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS) {
                EVIACAM.debug("OpenCV loaded successfully (from activity)");
            }
            else {
                super.onManagerConnected(status);
            }
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle bundle) {
        EVIACAM.debug("onCreate: SplashActivity");

        super.onCreate(bundle);
        setContentView(R.layout.splash_layout);

        // Try to init camera. Does it here (activity) so that installation dialog
        // could be properly displayed
        mCameraView= new MyJavaCameraView(this, CameraBridgeViewBase.CAMERA_ID_FRONT);
        
        // Start OpenCV
        MyOpenCVLoader.initAsync(MyOpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
        
        /** New Handler to close this splash after some seconds.*/
        /*
        new Handler().postDelayed(new Runnable(){
            @Override
            public void run() {
                SplashActivity.this.finish();
            }
        }, SPLASH_DISPLAY_LENGTH);
        */
    }
    
    /** Close activity when clicked */
    public void onClick(View view) {
        this.finish();
    }
}
