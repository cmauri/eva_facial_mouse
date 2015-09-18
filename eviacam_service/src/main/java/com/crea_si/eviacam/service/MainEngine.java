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

import com.crea_si.eviacam.api.IMouseEventListener;
import com.crea_si.eviacam.api.SlaveMode;
import com.crea_si.eviacam.api.IGamepadEventListener;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

/*
 * Provides the specific engine according to the intended
 * kind of use (i.e. as accessibility service or slave mode)
 */
public class MainEngine implements
    FrameProcessor, AccessibilityServiceModeEngine, SlaveModeEngine {
    /*
     * states of the engine
     */
    private static final int STATE_DISABLED= 0;
    private static final int STATE_STOPPED= 1;
    private static final int STATE_CHECKING_OPENCV= 2;
    private static final int STATE_RUNNING= 3;
    private static final int STATE_NO_FACE_PAUSED= 4;
    private static final int STATE_PAUSED= 5;

    /*
     * modes of operation from the point of view of the service
     * that starts the engine
     */
    private static final int A11Y_SERVICE_MODE= 0;
    private static final int SLAVE_MODE= 1;

    // singleton instance
    private static MainEngine sMainEngine = null;
    
    // openvc has been checked?
    private static boolean sOpenCVReady= false;

    // handler to run things on the main thread
    private final Handler mHandler= new Handler();

    // current engine state
    private int mCurrentState= STATE_DISABLED;

    // current engine mode
    private int mMode= -1;

    // slave mode submode
    private int mSlaveOperationMode= SlaveMode.GAMEPAD_ABSOLUTE;

    // reference to the service which started the engine
    private Service mService;

    // reference to the specific engine (motion processor)
    private MotionProcessor mMotionProcessor;

    // reference to the engine when running as mouse emulation
    private MouseEmulationEngine mMouseEmulationEngine;

    // reference to the engine for gamepad emulation
    private GamepadEngine mGamepadEngine;

    // power management stuff
    private PowerManagement mPowerManagement;

    // root overlay view
    private OverlayView mOverlayView;

    // the camera viewer
    private CameraLayerView mCameraLayerView;

    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;

    // object which encapsulates rotation and orientation logic
    private OrientationManager mOrientationManager;

    // reference to the notification management stuff
    private ServiceNotification mServiceNotification;

    // stores when the last detection of a face occurred
    private FaceDetectionCountdown mFaceDetectionCountdown;

    public static MainEngine getInstance() {
        if (sMainEngine == null) {
            sMainEngine = new MainEngine();
        }
        return sMainEngine;
    }
    
    private MainEngine() { }

    /**
     * Try to start the engine as a request from an accessibility service
     * 
     * @param as the reference to the accessibility service
     * @return a reference to the engine interface or null if cannot be started
     */
    public AccessibilityServiceModeEngine getAccessibilityServiceModeEngine 
                                                        (AccessibilityService as) {
        if (mCurrentState != STATE_DISABLED) {
            // Already started, if was as accessibility service something went wrong
            if (mMode == A11Y_SERVICE_MODE) throw new IllegalStateException();
            
            // Otherwise assume that has been started in slave mode and just returns null
            return null;
        }

        mMode= A11Y_SERVICE_MODE;
        mService= as;
        
        init();
                
        return this;
    }

    /**
     * Return the slave mode engine
     * 
     * @param s service which instantiates the engine
     * @return a reference to the engine interface or null if cannot be created (i.e. accessibility
     *           service engine already instantiated).
     */
    public SlaveModeEngine getSlaveModeEngine (Service s) {
        if (mCurrentState != STATE_DISABLED) {
            // Already instantiated, if was in slave mode something went wrong
            if (mMode == SLAVE_MODE) throw new IllegalStateException();
         
            // Otherwise assume that has been started in accessibility service mode
            return null;
        }
        
        mMode= SLAVE_MODE;
        mService= s;
        
        init();
        
        return this;
    }    

    private void init() {
        /*
         * Preference related stuff
         */
        EViacamApplication app= (EViacamApplication) mService.getApplicationContext();

        // set default configuration values if the service is run for the first time
        if (mMode == A11Y_SERVICE_MODE) {
            // If accessibility service use the default preferences
            PreferenceManager.setDefaultValues(mService, R.xml.preference_fragment, true);
            // Set the default shared preferences
            app.setSharedPreferences(PreferenceManager.getDefaultSharedPreferences(mService));
        }
        else {
            // Otherwise use slave mode preferences. We first load default default
            // preferences and then update with slave mode ones
            PreferenceManager.setDefaultValues(mService, Preferences.FILE_SLAVE_MODE,
                                               Context.MODE_PRIVATE, 
                                               R.xml.preference_fragment, true);
            PreferenceManager.setDefaultValues(mService, Preferences.FILE_SLAVE_MODE,
                                               Context.MODE_PRIVATE,
                                               R.xml.gamepad_preference_fragment, true);
            // Set the slave mode shared preferences
            app.setSharedPreferences(mService.getSharedPreferences(Preferences.FILE_SLAVE_MODE,
                                                                   Context.MODE_PRIVATE));
        }

        /*
         * Power management
         */
        mPowerManagement = new PowerManagement(mService);

        /*
         * Create UI stuff: root overlay and camera view
         */
        mOverlayView= new OverlayView(mService);
        mOverlayView.setVisibility(View.INVISIBLE);
        
        mCameraLayerView= new CameraLayerView(mService);
        mOverlayView.addFullScreenLayer(mCameraLayerView);
     
        /*
         * Create specific engine
         */
        if (mMode == A11Y_SERVICE_MODE) {
            // Start as accessibility service in mouse emulation mode
            mMotionProcessor= mMouseEmulationEngine=
                    new MouseEmulationEngine(mService, mOverlayView);
        }
        else {
            /*
             * Start in slave mode. Instantiate both gamepad and mouse emulation.
             * Initially mouse emulation is disabled
             */
            mMotionProcessor= mGamepadEngine= new GamepadEngine(mService, mOverlayView);
            mMouseEmulationEngine= new MouseEmulationEngine(mService, mOverlayView);
            mMouseEmulationEngine.pause();
        }

        /*
         * camera and machine vision stuff
         */
        mCameraListener= new CameraListener(mService, this);
        mCameraLayerView.addCameraSurface(mCameraListener.getCameraSurface());

        // orientation manager
        mOrientationManager= new OrientationManager(mService,
                                                    mCameraListener.getCameraOrientation());

        // Service notification listener
        mServiceNotification= new ServiceNotification(mService, this);

        // Face detection countdown
        mFaceDetectionCountdown= new FaceDetectionCountdown(mService);
        
        mCurrentState= STATE_STOPPED;
    }
    
    @Override
    public boolean start() {
        /*
         * Check and update current state
         */
        if (mCurrentState== STATE_CHECKING_OPENCV || mCurrentState==STATE_RUNNING) {
            return true;
        }
        if (mCurrentState!= STATE_STOPPED) return false;
        
        mCurrentState = STATE_CHECKING_OPENCV;

        if (sOpenCVReady) startStage2 ();
        else {
            /*
             * Display splash and detect OpenCV installation. The engine from now on waits
             * until the detection process finishes and initCVReady() is called.
             */
            Intent dialogIntent = new Intent(mService, SplashActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mService.startActivity(dialogIntent);
            
            mCurrentState = STATE_CHECKING_OPENCV;
        }
        
        return true;
    }
    
    /** Called from splash activity to notify the openCV is properly installed */
    public static void initCVReady() {
        MainEngine ce= MainEngine.sMainEngine;
        if (ce == null) return;
        sOpenCVReady= true;
                
        ce.startStage2();
    }

    private void startStage2 () {
        if (mCurrentState!= STATE_CHECKING_OPENCV) return;

        /*
         * Power management
         */
        // Screen always on
        mPowerManagement.lockFullPower();
        // Enable sleep call
        mPowerManagement.setSleepEnabled(true);

        // show GUI elements
        mOverlayView.requestLayout();
        mOverlayView.setVisibility(View.VISIBLE);
        
        // start processing frames
        mCameraListener.startCamera();

        // add notification and set as foreground service
        mService.startForeground(mServiceNotification.getNotificationId(), 
                mServiceNotification.setNotification(
                        ServiceNotification.NOTIFICATION_ACTION_PAUSE));

        mFaceDetectionCountdown.reset();

        mCurrentState= STATE_RUNNING;
    }

    /**
     * Pauses (asynchronously) the engine
     *
     */
    public void pause() {
        if (mCurrentState != STATE_RUNNING) return;
        mCurrentState= STATE_PAUSED;

        doPause();
    }

    private void noFacePause() {
        if (mCurrentState != STATE_RUNNING) return;
        mCurrentState= STATE_NO_FACE_PAUSED;

        doPause();
    }

    private void doPause() {
        // pause specific engine
        if (mMotionProcessor!= null) {
            mMotionProcessor.pause();
        }

        mServiceNotification.setNotification(ServiceNotification.NOTIFICATION_ACTION_RESUME);

        mCameraListener.setUpdateViewer(false);
        mPowerManagement.unlockFullPower();
    }

    /* Resumes the engine */
    public void resume() {
        if (mCurrentState != STATE_PAUSED && mCurrentState!= STATE_NO_FACE_PAUSED) return;

        mPowerManagement.lockFullPower();
        mCameraListener.setUpdateViewer(true);

        // resume specific engine
        if (mMotionProcessor!= null) {
            mMotionProcessor.resume();
        }

        // make sure that UI changes during pause (e.g. docking panel edge) are applied
        mOverlayView.requestLayout();

        mFaceDetectionCountdown.reset();

        mServiceNotification.setNotification(ServiceNotification.NOTIFICATION_ACTION_PAUSE);

        mCurrentState= STATE_RUNNING;
    }    

    @Override
    public void stop() {
        switch (mCurrentState) {
            case STATE_DISABLED:
            case STATE_STOPPED:
                return;
            case STATE_RUNNING:
                mPowerManagement.unlockFullPower();
                // no break
            case STATE_NO_FACE_PAUSED:
            case STATE_PAUSED:
                mService.stopForeground(true);

                // Disable sleep call
                mPowerManagement.setSleepEnabled(false);

                mCameraListener.stopCamera();

                mOverlayView.setVisibility(View.INVISIBLE);
                break;
            case STATE_CHECKING_OPENCV:
                // do nothing
        }

        mCurrentState= STATE_STOPPED;
    }
    
    @Override
    public void cleanup() {
        if (mCurrentState == STATE_DISABLED) return;

        stop();

        mFaceDetectionCountdown.cleanup();
        mFaceDetectionCountdown= null;

        mServiceNotification.cleanup();
        mServiceNotification= null;
        
        mCameraListener= null;

        mOrientationManager.cleanup();
        mOrientationManager= null;

        mMotionProcessor.cleanup();
        mMotionProcessor= null;
        mMouseEmulationEngine= null;
        mGamepadEngine= null;

        mCameraLayerView= null;

        mOverlayView.cleanup();
        mOverlayView= null;
        
        mCurrentState= STATE_DISABLED;

        mPowerManagement = null;

        EViacamApplication app= (EViacamApplication) mService.getApplicationContext();
        app.setSharedPreferences(null);

        sMainEngine = null;
    }
  
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mOrientationManager != null) mOrientationManager.onConfigurationChanged(newConfig);
    }

    @Override
    public void setOperationMode(int mode) {
        if (mSlaveOperationMode== mode) return;

        // Pause old engine & switch to new
        if (mSlaveOperationMode== SlaveMode.MOUSE) {
            mMouseEmulationEngine.pause();
            mMotionProcessor= mGamepadEngine;
        }
        else if (mode== SlaveMode.MOUSE){
            mGamepadEngine.pause();
            mMotionProcessor= mMouseEmulationEngine;
        }

        mSlaveOperationMode= mode;

        if (mode!= SlaveMode.MOUSE) {
            mGamepadEngine.setOperationMode(mode);
        }

        // Resume engine if needed
        if (mCurrentState != STATE_PAUSED) mMotionProcessor.resume(); 
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mMouseEmulationEngine!= null && mMode == A11Y_SERVICE_MODE) {
            mMouseEmulationEngine.onAccessibilityEvent(event);
        }
    }

    @Override
    public boolean registerGamepadListener(IGamepadEventListener l) {
        return mGamepadEngine.registerListener(l);
    }

    @Override
    public void unregisterGamepadListener() {
        mGamepadEngine.unregisterListener();
    }

    @Override
    public boolean registerMouseListener(IMouseEventListener l) {
        return mMouseEmulationEngine.registerListener(l);
    }

    @Override
    public void unregisterMouseListener() {
        mMouseEmulationEngine.unregisterListener();
    }

    PointF mMotion= new PointF(0, 0); // avoid creating a new PointF for each frame

    /**
     * Process incoming camera frames
     *
     * Remarks: this method is called from a secondary thread
     *
     * @param rgba opencv matrix with the captured image
     */
    @Override
    public void processFrame(Mat rgba) {
        if (mCurrentState != STATE_RUNNING && mCurrentState != STATE_NO_FACE_PAUSED) {
            // reduce CPU load when not running
            mPowerManagement.sleep();
            return;
        }

        int pictRotation = mOrientationManager.getPictureRotation();

        /*
         * call jni part to track face
         */
        mMotion.x= mMotion.y= 0.0f;
        boolean faceDetected=
                VisionPipeline.processFrame(rgba.getNativeObjAddr(), pictRotation, mMotion);

        // set preview rotation
        mCameraListener.setPreviewRotation(pictRotation);

        /*
         * Check whether need to pause/resume the engine according
         * to the face detection status
         */
        if (faceDetected) {
            mFaceDetectionCountdown.reset();
            if (mCurrentState== STATE_NO_FACE_PAUSED) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() { resume(); } }
                );

                /* Yield CPU to the main thread so that it has the opportunity
                 * to run and change the engine state before this thread continue
                 * running.
                 * Remarks: tried Thread.yield() without success
                 */
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) { /* do nothing */ }
            }
        }
        else {
            if (mFaceDetectionCountdown.hasFinished() && !mFaceDetectionCountdown.isDisabled()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() { noFacePause(); } }
                );
            }
        }

        // No face paused? Don't continue processing
        if (mCurrentState == STATE_NO_FACE_PAUSED) {
            // reduce CPU load when not running
            mPowerManagement.sleep();
            return;
        }

        // Provide feedback through the camera viewer
        mCameraLayerView.updateFaceDetectorStatus(mFaceDetectionCountdown);

        // compensate mirror effect
        mMotion.x= -mMotion.x;

        // fix motion orientation according to device rotation and screen orientation
        mOrientationManager.fixVectorOrientation(mMotion);

        // process motion on specific engine
        mMotionProcessor.processMotion(mMotion);
    }
}
