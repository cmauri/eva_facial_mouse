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

import org.opencv.android.CameraException;
import org.opencv.core.Mat;

import com.crea_si.eviacam.EVIACAM;
import com.crea_si.eviacam.EViacamApplication;
import com.crea_si.eviacam.Preferences;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.api.IMouseEventListener;
import com.crea_si.eviacam.api.SlaveMode;
import com.crea_si.eviacam.api.IGamepadEventListener;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PointF;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
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
    private static final int STATE_RUNNING= 2;
    // Standby is when engine timed out after not detecting a face
    // for a while. It keeps running trying to detect a face.
    private static final int STATE_STANDBY = 3;
    // manually paused
    private static final int STATE_PAUSED= 4;

    /*
     * modes of operation from the point of view of the service
     * that starts the engine
     */
    private static final int A11Y_SERVICE_MODE= 0;
    private static final int SLAVE_MODE= 1;

    // singleton instance
    private static MainEngine sMainEngine = null;
    
    // splash screen has been displayed (in the past: openvc has been checked?)
    private static boolean sSplashDisplayed = false;

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
     * Try to init the engine as a request from an accessibility service
     * 
     * @param as the reference to the accessibility service
     * @return a reference to the engine interface or null if cannot be initiated
     */
    public AccessibilityServiceModeEngine initAccessibilityServiceModeEngine
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
     * Get an instance to the current accessibility mode engine
     *
     * @return a reference to the engine interface or null if not available
     */
    public AccessibilityServiceModeEngine getAccessibilityServiceModeEngine() {
        if (mMode== A11Y_SERVICE_MODE) return this;

        return null;
    }

    /**
     * Return the slave mode engine
     * 
     * @param s service which instantiates the engine
     * @return a reference to the engine interface or null if cannot be created (i.e. accessibility
     *           service engine already instantiated).
     */
    public SlaveModeEngine initSlaveModeEngine(Service s) {
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


    /**
     * Init phase 1: splash screen display (formerly used to OpenCV detection and install)
     */
    private void init() {
        if (sSplashDisplayed) init2();
        else {
            /*
             * Display splash. The engine from now on waits
             * until the detection process finishes and splashReady() is called.
             */
            Intent dialogIntent = new Intent(mService, SplashActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mService.startActivity(dialogIntent);
        }
    }

    /** Called from splash activity to notify the finished */
    public static void splashReady() {
        /* Was initialized previously? If so, just do nothing. */
        if (sSplashDisplayed) return;

        MainEngine ce= MainEngine.sMainEngine;
        if (ce == null) return;
        sSplashDisplayed = true;

        ce.init2();
    }

    /**
     * Init phase 2: common initialization stuff
     */
    private void init2() {
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
            // Init as accessibility service in mouse emulation mode
            mMotionProcessor= mMouseEmulationEngine=
                    new MouseEmulationEngine(mService, mOverlayView);
        }
        else {
            /*
             * Init in slave mode. Instantiate both gamepad and mouse emulation.
             */
            mMotionProcessor= mGamepadEngine= new GamepadEngine(mService, mOverlayView);
            mMouseEmulationEngine= new MouseEmulationEngine(mService, mOverlayView);
        }

        /*
         * camera and machine vision stuff
         */
        try {
            mCameraListener = new CameraListener(mService, this);
        }
        catch(CameraException e) {
            // TODO: throw more camera related exceptions inside MyCameraBridgeViewBase
            // and manage here accordingly.
            AlertDialog.Builder adb = new AlertDialog.Builder(mService);
            adb.setCancelable(false); // This blocks the 'BACK' button
            adb.setTitle(mService.getText(R.string.app_name));
            adb.setMessage(e.getMessage());
            adb.setPositiveButton(mService.getText(android.R.string.ok), null);

            AlertDialog ad= adb.create();
            ad.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            ad.show();

            return;  // abort initialization
        }
        mCameraLayerView.addCameraSurface(mCameraListener.getCameraSurface());

        /* flip the preview when needed */
        mCameraListener.setPreviewFlip(mCameraListener.getCameraFlip());

        // orientation manager
        OrientationManager.init(
                mService,
                mCameraListener.getCameraFlip(),
                mCameraListener.getCameraOrientation());
        mOrientationManager= OrientationManager.get();

        // Service notification listener
        mServiceNotification= new ServiceNotification(mService, this);

        // Face detection countdown
        mFaceDetectionCountdown= new FaceDetectionCountdown(mService);
        
        mCurrentState= STATE_STOPPED;

        /*
         * start things when needed
         */
        if (mMode == A11Y_SERVICE_MODE) {
            if (Preferences.getRunTutorial(mService)) {
                Intent dialogIntent = new Intent(mService,
                        com.crea_si.eviacam.wizard.WizardActivity.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mService.startActivity(dialogIntent);
            }
            else start();
        }
    }
    
    @Override
    public boolean start() {
        /*
         * Check and update current state
         */
        if (mCurrentState==STATE_RUNNING) return true;
        resume();
        if (mCurrentState!= STATE_STOPPED) return false;
        
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

        mCameraLayerView.enableDetectionFeedback();
        
        // start processing frames
        mCameraListener.startCamera();

        // add notification and set as foreground service
        mService.startForeground(mServiceNotification.getNotificationId(), 
                mServiceNotification.setNotification(
                        ServiceNotification.NOTIFICATION_ACTION_PAUSE));

        mFaceDetectionCountdown.reset();

        // start engine
        mMotionProcessor.start();

        mCurrentState= STATE_RUNNING;

        return true;
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

    private void standby() {
        if (mCurrentState != STATE_RUNNING) return;
        mCurrentState= STATE_STANDBY;

        doPause();
    }

    private void doPause() {
        mCameraLayerView.disableDetectionFeedback();

        // pause specific engine
        if (mMotionProcessor!= null) {
            mMotionProcessor.stop();
        }

        mServiceNotification.setNotification(ServiceNotification.NOTIFICATION_ACTION_RESUME);

        // TODO: disable surface updates when screen switched off to save some CPU cycles
        //mCameraListener.setUpdateViewer(false);
        mPowerManagement.unlockFullPower();
    }

    /* Resumes the engine */
    public void resume() {
        if (mCurrentState != STATE_PAUSED && mCurrentState!= STATE_STANDBY) return;

        mPowerManagement.lockFullPower();
        //mCameraListener.setUpdateViewer(true);

        mCameraLayerView.enableDetectionFeedback();

        // resume specific engine
        if (mMotionProcessor!= null) {
            mMotionProcessor.start();
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
            case STATE_STANDBY:
            case STATE_PAUSED:
                mService.stopForeground(true);

                // Disable sleep call
                mPowerManagement.setSleepEnabled(false);

                mCameraListener.stopCamera();

                // TODO: add sleep to see if removes spurious crashes on exit
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) { /* do nothing */ }

                mOverlayView.setVisibility(View.INVISIBLE);
                break;
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

        mPowerManagement.cleanup();
        mPowerManagement = null;

        EViacamApplication app= (EViacamApplication) mService.getApplicationContext();
        app.setSharedPreferences(null);

        sMainEngine = null;
    }

    @Override
    public void setOperationMode(int mode) {
        if (mSlaveOperationMode== mode) return;

        // Pause old engine & switch to new
        if (mSlaveOperationMode== SlaveMode.MOUSE) {
            mMouseEmulationEngine.stop();
            mMotionProcessor= mGamepadEngine;
        }
        else if (mode== SlaveMode.MOUSE){
            mGamepadEngine.stop();
            mMotionProcessor= mMouseEmulationEngine;
        }

        mSlaveOperationMode= mode;

        if (mode!= SlaveMode.MOUSE) {
            mGamepadEngine.setOperationMode(mode);
        }

        // Resume engine if needed
        if (mCurrentState != STATE_PAUSED) mMotionProcessor.start();
    }

    @Override
    public boolean isReady() {
        return (mCurrentState != STATE_DISABLED);
    }

    @Override
    public void enablePointer() {
        if (mMouseEmulationEngine != null) mMouseEmulationEngine.enablePointer();
    }

    @Override
    public void disablePointer() {
        if (mMouseEmulationEngine != null) mMouseEmulationEngine.disablePointer();
    }

    @Override
    public void enableClick() {
        if (mMouseEmulationEngine != null) mMouseEmulationEngine.enableClick();
    }

    @Override
    public void disableClick() {
        if (mMouseEmulationEngine != null) mMouseEmulationEngine.disableClick();
    }

    @Override
    public void enableDockPanel() {
        if (mMouseEmulationEngine != null) mMouseEmulationEngine.enableDockPanel();
    }

    @Override
    public void disableDockPanel() {
        if (mMouseEmulationEngine != null) mMouseEmulationEngine.disableDockPanel();
    }

    @Override
    public void enableScrollButtons() {
        if (mMouseEmulationEngine != null) mMouseEmulationEngine.enableScrollButtons();
    }

    @Override
    public void disableScrollButtons() {
        if (mMouseEmulationEngine != null) mMouseEmulationEngine.disableScrollButtons();
    }

    /**
     * Return elapsed time since last face detection
     *
     * @return elapsed time in ms or 0 if no detection
     */
    @Override
    public long getFaceDetectionElapsedTime() {
        if (mCurrentState != STATE_RUNNING) return 0;
        return mFaceDetectionCountdown.getElapsedTime();
    }

    @Override
    public void enableAll() {
        enablePointer();
        enableClick();
        enableDockPanel();
        enableScrollButtons();
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
        // For these states do nothing
        if (mCurrentState== STATE_DISABLED || mCurrentState== STATE_STOPPED) return;

        /*
         * When to screen is off make sure is working in standby mode and reduce CPU usage
         */
        if (!mPowerManagement.getScreenOn()) {
            if (mCurrentState!= STATE_PAUSED && mCurrentState!= STATE_STANDBY) {
                mHandler.post(new Runnable() {
                   @Override
                   public void run() { standby(); } }
                );
            }
            mPowerManagement.sleep();
        }

        /* Here is in RUNNING or in STANDBY state */

        int pictRotation = mOrientationManager.getPictureRotation();

        // set preview rotation
        mCameraListener.setPreviewRotation(pictRotation);

        if (mCurrentState== STATE_PAUSED) return;

        /*
         * call jni part to track face
         */
        mMotion.x= mMotion.y= 0.0f;
        boolean faceDetected=
                VisionPipeline.processFrame(
                        rgba.getNativeObjAddr(),
                        mOrientationManager.getPictureFlip().getValue(),
                        pictRotation,
                        mMotion);

        /*
         * Check whether need to pause/resume the engine according
         * to the face detection status
         */
        if (faceDetected) {
            mFaceDetectionCountdown.start();
            if (mCurrentState== STATE_STANDBY) {
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

        if (mCurrentState== STATE_STANDBY) return;

        if (mFaceDetectionCountdown.hasFinished() && !mFaceDetectionCountdown.isDisabled()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Resources res = mService.getResources();
                    String t = String.format(res.getString(R.string.pointer_stopped_toast),
                            Preferences.getTimeWithoutDetectionEntryValue(mService));
                    EVIACAM.LongToast(mService, t);

                    standby();
                }
            });
        }

        // Provide feedback through the camera viewer
        mCameraLayerView.updateFaceDetectorStatus(mFaceDetectionCountdown);

        // compensate mirror effect
        mMotion.x= -mMotion.x;

        // process motion on specific engine
        mMotionProcessor.processMotion(mMotion);
    }
}
