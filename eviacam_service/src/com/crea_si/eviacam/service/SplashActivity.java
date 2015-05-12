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
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Mat;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;

public class SplashActivity extends Activity implements CvCameraViewListener2 {

    /** Duration of wait */
    private static final int SPLASH_DISPLAY_LENGTH = 4000;
    
    /** opencv capture&view facility */ 
    private CameraBridgeViewBase mCameraView;

    // callback for camera initialization
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    EVIACAM.debug("OpenCV loaded successfully (from activity)");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle bundle) {
        EVIACAM.debug("onCreate: SplashActivity");

        super.onCreate(bundle);
        setContentView(R.layout.splash_layout);

        
        // create capture view
        mCameraView= new MyJavaCameraView(this, CameraBridgeViewBase.CAMERA_ID_FRONT);
        
        // set CameraBridgeViewBase parameters        
        // TODO: Damn! It seems that for certain resolutions (for instance 320x240 on a Galaxy Nexus)
        // crashes with a "Callback buffer was too small! error", it works at 352x288
        
        mCameraView.setMaxFrameSize(352, 288);
        //mCameraView.enableFpsMeter(); // For testing
        mCameraView.setCvCameraViewListener(this);
        
        // Set View parameters
        mCameraView.setVisibility(SurfaceView.VISIBLE);
        
        LinearLayout l = (LinearLayout) this.findViewById(R.id.camera_view);
        l.addView(mCameraView);
        
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

    @Override
    public void onCameraViewStarted(int width, int height) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onCameraViewStopped() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        // TODO Auto-generated method stub
        return null;
    }
}
