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

import org.opencv.core.Mat;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

public class EngineManager implements FrameProcessor, AccessibilityServiceModeEngine {
    /*
     * states of the engine
     */
    private static final int STATE_NONE= 0;
    private static final int STATE_CHECKING_OPENCV= 1;
    private static final int STATE_RUNNING= 2;
    private static final int STATE_PAUSED= 3;
    private static final int STATE_STOPPED= 4;

    // static reference to the single common engine instance
    private static EngineManager sEngineManager= null;

    // reference to the accessibility service
    private AccessibilityService mAccessibilityService;

    // current engine state
    private int mCurrentState= STATE_NONE;

    // root overlay view
    private OverlayView mOverlayView;
    
    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;
    
    // object which encapsulates rotation and orientation logic
    private OrientationManager mOrientationManager;
    
    // reference to the mouse emulation engine
    private AccessibilityServiceModeEngineImpl mAccessibilityServiceModeEngineImpl;
    
    // reference to the notification management stuff
    private ServiceNotification mServiceNotification;

    public static EngineManager getInstance() {
        if (sEngineManager== null) {
            sEngineManager= new EngineManager();
        }
        return sEngineManager;
    }
    
    private EngineManager() { }

    public AccessibilityServiceModeEngine startAsAccessibilityService (AccessibilityService as) {
        mAccessibilityService= as;
        initStage1(as);
        return this;
    }

    /*
    public SlaveModeEngine startInSlaveMode () {
        return null;
    }*/    

    /** Called from splash activity to notify the openCV is properly installed */
    public static void initCVReady() {
        EngineManager ce= EngineManager.sEngineManager;
        if (ce == null) return;
        
        ce.initStage2();
    }
    
    private void initStage1 (AccessibilityService as) {
        // set default configuration values if the service is run for the first time
        PreferenceManager.setDefaultValues(mAccessibilityService, R.xml.preference_fragment, true);
        
        /*
         * Display splash and detect OpenCV installation. The service from now on waits 
         * until the detection process finishes and a notification is received.
         */
        Intent dialogIntent = new Intent(mAccessibilityService, SplashActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mAccessibilityService.startActivity(dialogIntent);
        
        mCurrentState = STATE_CHECKING_OPENCV;
    }
    
    private void initStage2 () {
        if (mCurrentState== STATE_RUNNING) return;

        /*
         * Create UI stuff: root overlay and camera view
         */
        mOverlayView= new OverlayView(mAccessibilityService);
        CameraLayerView cameraLayer= new CameraLayerView(mAccessibilityService);
        mOverlayView.addFullScreenLayer(cameraLayer);

        /*
         * TODO: set up specific engine
         */
        mAccessibilityServiceModeEngineImpl= 
                new AccessibilityServiceModeEngineImpl(mAccessibilityService, mOverlayView);

        /*
         * camera and machine vision stuff
         */
        mCameraListener= new CameraListener(mAccessibilityService, this);
        cameraLayer.addCameraSurface(mCameraListener.getCameraSurface());

        // orientation manager
        mOrientationManager= new OrientationManager(mAccessibilityService,
                                                    mCameraListener.getCameraOrientation());
        // start processing frames
        mCameraListener.startCamera();

        /*
         * add notification and set as foreground service
         */
        mServiceNotification= new ServiceNotification(mAccessibilityService, this);
        mAccessibilityService.startForeground(mServiceNotification.getNotificationId(), 
                mServiceNotification.getNotification(mAccessibilityService));

        
        mCurrentState= STATE_RUNNING;
    }

    public void cleanup() {
        if (mCurrentState == STATE_STOPPED) return;
        
        if (mCurrentState == STATE_RUNNING) {
            /*
             *  stage 2 cleanup 
             */

            // stop being foreground service and remove notification
            mAccessibilityService.stopForeground(true);

            mServiceNotification.cleanup();
            mServiceNotification= null;
            
            mCameraListener.stopCamera();
            mCameraListener= null;
            
            mOrientationManager.cleanup();
            mOrientationManager= null;

            mAccessibilityServiceModeEngineImpl.cleanup();
            mAccessibilityServiceModeEngineImpl= null;
            
            mOverlayView.cleanup();
            mOverlayView= null;
        }
        
        if (mCurrentState == STATE_RUNNING ||
            mCurrentState == STATE_CHECKING_OPENCV) {
            /*
             *  stage 1 cleanup 
             */
            sEngineManager= null;
        }

        mCurrentState= STATE_STOPPED;
    }
   
    public void pause() {
        if (mCurrentState != STATE_RUNNING) return;
        mCurrentState= STATE_PAUSED;

        // pause specific engine
        if (mAccessibilityServiceModeEngineImpl!= null) {
            mAccessibilityServiceModeEngineImpl.pause();
        }
    }
    
    public void resume() {
        if (mCurrentState != STATE_PAUSED) return;

        // TODO: reset tracker internal state?

        // resume specific engine
        if (mAccessibilityServiceModeEngineImpl!= null) {
            mAccessibilityServiceModeEngineImpl.resume();
        }

        // make sure that changes during pause (e.g. docking panel edge) are applied
        mOverlayView.requestLayout();
        mCurrentState= STATE_RUNNING;
    }    

    public void onConfigurationChanged(Configuration newConfig) {
        if (mOrientationManager != null) mOrientationManager.onConfigurationChanged(newConfig);
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mAccessibilityServiceModeEngineImpl!= null) {
            mAccessibilityServiceModeEngineImpl.onAccessibilityEvent(event);
        }
    }

    /*
     * process incoming camera frame 
     * 
     * this method is called from a secondary thread 
     */
    @Override
    public void processFrame(Mat rgba) {
        if (mCurrentState != STATE_RUNNING) return;

        int phyRotation = mOrientationManager.getPictureRotation();

        // call jni part to track face
        PointF motion = new PointF(0, 0);
        VisionPipeline.processFrame(rgba.getNativeObjAddr(), phyRotation, motion);

        // compensate mirror effect
        motion.x= -motion.x;

        // fix motion orientation according to device rotation and screen orientation
        mOrientationManager.fixVectorOrientation(motion);

        // process motion on specific engine
        mAccessibilityServiceModeEngineImpl.processMotion(motion);
    }
}
