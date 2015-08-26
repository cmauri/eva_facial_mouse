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

import com.crea_si.eviacam.api.IPadEventListener;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

/*
 * Provides the specific engine according to the intended
 * kind of use (i.e. as accessibility service or slave mode)
 */
public class EngineManager implements
    FrameProcessor, AccessibilityServiceModeEngine, SlaveModeEngine {
    /*
     * states of the engine
     */
    private static final int STATE_STOPPED= 0;
    private static final int STATE_CHECKING_OPENCV= 1;
    private static final int STATE_RUNNING= 2;
    private static final int STATE_PAUSED= 3;

    /*
     * modes of operation from the point of view of the service
     * that starts the engine
     */
    private static final int A11Y_SERVICE_MODE= 0;
    private static final int SLAVE_MODE= 1;

    // singleton instance
    private static EngineManager sEngineManager= null;

    // current engine state
    private int mCurrentState= STATE_STOPPED;

    // current engine mode
    private int mMode= -1;

    // slave mode submode
    private int mSlaveSubMode= -1;

    // reference to the service which started the engine
    private Service mService;

    // reference to the specific engine (motion processor)
    private MotionProcessor mMotionProcessor;

    // reference to the engine when running as mouse emulation
    private MouseEmulationEngine mMouseEmulationEngine;

    // reference to the engine for gamepad emulation
    private GamePadEngine mGamePadEngine;

    // root overlay view
    private OverlayView mOverlayView;

    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;

    // object which encapsulates rotation and orientation logic
    private OrientationManager mOrientationManager;

    // reference to the notification management stuff
    private ServiceNotification mServiceNotification;

    public static EngineManager getInstance() {
        if (sEngineManager== null) {
            sEngineManager= new EngineManager();
        }
        return sEngineManager;
    }
    
    private EngineManager() { }

    /**
     * Try to start the engine as a request from an accessibility service
     * 
     * @param as the reference to the accessibility service
     * @return a reference to the engine interface or null if cannot be started
     */
    public AccessibilityServiceModeEngine startAsAccessibilityService (AccessibilityService as) {
        if (mCurrentState != STATE_STOPPED) {
            // Already started, if was as accessibility service something went wrong
            if (mMode == A11Y_SERVICE_MODE) throw new IllegalStateException();
            
            // Otherwise assume that has been started in slave mode and just returns null
            return null;
        }

        mMode= A11Y_SERVICE_MODE;
        mSlaveSubMode= -1;
        mService= as;
        initStage1();
        return this;
    }

    /**
     * Try to start the engine in slave mode
     * 
     * @param s service which instantiates the engine
     * @param submode of operation, see {@link com.crea_si.eviacam.service.SlaveModeEngine} class
     * @return a reference to the engine interface or null if cannot be started
     */
    public SlaveModeEngine startInSlaveMode (Service s, int submode) {
        if (mCurrentState != STATE_STOPPED) {
            // Already started, if was in slave mode something went wrong
            if (mMode == SLAVE_MODE) throw new IllegalStateException();
         
            // Otherwise assume that has been started in accessibility service mode
            return null;
        }

        mMode= SLAVE_MODE;
        mSlaveSubMode= submode;
        mService= s;
        initStage1();
        return this;
    }    

    /** Called from splash activity to notify the openCV is properly installed */
    public static void initCVReady() {
        EngineManager ce= EngineManager.sEngineManager;
        if (ce == null) return;
        
        ce.initStage2();
    }
    
    private void initStage1 () {
        // set default configuration values if the service is run for the first time
        PreferenceManager.setDefaultValues(mService, R.xml.preference_fragment, true);
        
        /*
         * Create specific engine
         */
        if (mMode == A11Y_SERVICE_MODE || mSlaveSubMode== SlaveModeEngine.MOUSE) {
            // Start as accessibility service in mouse emulation mode
            // TODO: implement different behavior when (mSlaveSubMode== SlaveModeEngine.MOUSE)
            mMotionProcessor= mMouseEmulationEngine= new MouseEmulationEngine(
                    (AccessibilityService) mService);
        } 
        else {
            mMotionProcessor= mGamePadEngine= new GamePadEngine(mService);
        }
        
        /*
         * Display splash and detect OpenCV installation. The service from now on waits 
         * until the detection process finishes and a notification is received.
         */
        Intent dialogIntent = new Intent(mService, SplashActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mService.startActivity(dialogIntent);
        
        mCurrentState = STATE_CHECKING_OPENCV;
    }
    
    private void initStage2 () {
        if (mCurrentState!= STATE_CHECKING_OPENCV) return;

        /*
         * Create UI stuff: root overlay and camera view
         */
        mOverlayView= new OverlayView(mService);
        CameraLayerView cameraLayer= new CameraLayerView(mService);
        mOverlayView.addFullScreenLayer(cameraLayer);
     
        // Init the specific engine
        mMotionProcessor.init(mOverlayView);

        /*
         * camera and machine vision stuff
         */
        mCameraListener= new CameraListener(mService, this);
        cameraLayer.addCameraSurface(mCameraListener.getCameraSurface());

        // orientation manager
        mOrientationManager= new OrientationManager(mService,
                                                    mCameraListener.getCameraOrientation());
        // start processing frames
        mCameraListener.startCamera();

        /*
         * add notification and set as foreground service
         */
        mServiceNotification= new ServiceNotification(mService, this);
        mService.startForeground(mServiceNotification.getNotificationId(), 
                mServiceNotification.getNotification(mService));

        
        mCurrentState= STATE_RUNNING;
    }

    public void cleanup() {
        if (mCurrentState == STATE_STOPPED) return;
        
        if (mCurrentState == STATE_RUNNING) {
            /*
             *  stage 2 cleanup 
             */

            // stop being foreground service and remove notification
            mService.stopForeground(true);

            mServiceNotification.cleanup();
            mServiceNotification= null;
            
            EVIACAM.debug("before stopCamera");
            mCameraListener.stopCamera();
            EVIACAM.debug("after stopCamera");
            mCameraListener= null;

            mOrientationManager.cleanup();
            mOrientationManager= null;

            mMotionProcessor.cleanup();
            mMotionProcessor= null;
            mMouseEmulationEngine= null;
            mGamePadEngine= null;
            
            mOverlayView.cleanup();
            mOverlayView= null;
        }
        
        if (mCurrentState == STATE_RUNNING ||
            mCurrentState == STATE_CHECKING_OPENCV) {
            /*
             *  stage 1 cleanup 
             */
            mMotionProcessor= null;
            mMouseEmulationEngine= null;
            mGamePadEngine= null;

            sEngineManager= null;
        }

        mCurrentState= STATE_STOPPED;
    }

    /* Pauses (asynchronously) the engine */
    public void pause() {
        if (mCurrentState != STATE_RUNNING) return;
        mCurrentState= STATE_PAUSED;

        // pause specific engine
        if (mMotionProcessor!= null) {
            mMotionProcessor.pause();
        }
    }

    /* Resumes the engine */
    public void resume() {
        if (mCurrentState != STATE_PAUSED) return;

        // TODO: reset tracker internal state?

        // resume specific engine
        if (mMotionProcessor!= null) {
            mMotionProcessor.resume();
        }

        // make sure that changes during pause (e.g. docking panel edge) are applied
        mOverlayView.requestLayout();
        mCurrentState= STATE_RUNNING;
    }    

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mOrientationManager != null) mOrientationManager.onConfigurationChanged(newConfig);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mMouseEmulationEngine!= null && mMode == A11Y_SERVICE_MODE) {
            mMouseEmulationEngine.onAccessibilityEvent(event);
        }
    }

    @Override
    public boolean registerListener(IPadEventListener l) {
        return mGamePadEngine.registerListener(l);
    }

    @Override
    public void unregisterListener() {
        mGamePadEngine.unregisterListener();
    }

    /**
     * Process incoming camera frame 
     * 
     * This method is called from a secondary thread 
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
        mMotionProcessor.processMotion(motion);
    }
}
