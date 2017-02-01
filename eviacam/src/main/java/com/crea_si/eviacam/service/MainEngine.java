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

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.api.IMouseEventListener;
import com.crea_si.eviacam.api.SlaveMode;
import com.crea_si.eviacam.api.IGamepadEventListener;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Intent;
import android.graphics.PointF;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

/*
 * Provides the specific engine according to the intended
 * kind of use (i.e. as accessibility service or slave mode)
 */
public class MainEngine implements
    FrameProcessor, AccessibilityServiceModeEngine, SlaveModeEngine {

    /* current engine state */
    private int mCurrentState= STATE_DISABLED;

    /*
     * Modes of operation from the point of view of the service
     * that starts the engine
     */
    private static final int A11Y_SERVICE_MODE= 0;
    private static final int SLAVE_MODE= 1;

    // current engine mode
    private final int mMode;

    // slave mode submode
    private int mSlaveOperationMode= SlaveMode.GAMEPAD_ABSOLUTE;

    // singleton instances
    private static MainEngine sAccessibilityServiceModeEngine = null;
    private static MainEngine sSlaveModeEngine = null;

    // splash screen has been displayed (in the past: openvc has been checked?)
    private boolean mSplashDisplayed = false;

    private OnInitListener mOnInitListener;

    private OnFinishProcessFrame mOnFinishProcessFrame;

    // reference to the service which started the engine
    private Service mService;

    // reference to the specific engine (motion processor)
    private MotionProcessor mMotionProcessor;

    // reference to the engine when running as mouse emulation
    private MouseEmulationEngine mMouseEmulationEngine;

    // reference to the engine for gamepad emulation
    private GamepadEngine mGamepadEngine;

    // root overlay view
    private OverlayView mOverlayView;

    // the camera viewer
    private CameraLayerView mCameraLayerView;

    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;

    // object which encapsulates rotation and orientation logic
    private OrientationManager mOrientationManager;

    // Last time a face has been detected
    private long mLastFaceDetectionTimeStamp;

    private MainEngine(int mode) {
        mMode= mode;
    }

    /**
     * Get an instance to the current accessibility mode engine
     *
     * @return a reference to the engine interface or null if not available
     */
    public static AccessibilityServiceModeEngine getAccessibilityServiceModeEngine() {
        if (sAccessibilityServiceModeEngine == null) {
            sAccessibilityServiceModeEngine= new MainEngine(A11Y_SERVICE_MODE);
        }

        return sAccessibilityServiceModeEngine;
    }

    /**
     * Get an instance to the current accessibility mode engine
     *
     * @return a reference to the engine interface or null if not available
     */
    public static SlaveModeEngine getSlaveModeEngine() {
        if (sSlaveModeEngine == null) {
            sSlaveModeEngine= new MainEngine(SLAVE_MODE);
        }

        return sSlaveModeEngine;
    }

    @Override
    public boolean init(Service s, OnInitListener l) {
        if (mCurrentState != STATE_DISABLED) {
            // Already started, something went wrong
            throw new IllegalStateException();
        }

        final Engine other;
        if (mMode == A11Y_SERVICE_MODE) other= sSlaveModeEngine;
        else if (mMode == SLAVE_MODE) other= sAccessibilityServiceModeEngine;
        else throw new IllegalStateException();

        if (other!= null && other.getState() != STATE_DISABLED) {
            // Engine started in the other working mode. Abort init.
            if (l!= null) l.onInit(-1);
            return false;
        }

        /**
         * Proceed with initialization. Store service and listener
         */
        mService= s;
        mOnInitListener= l;

        /* Show splash screen if not already shown. The splash screen is also used to
           request the user the required permissions to run this software.
           In the past, it was also used for OpenCV detection and installation.
           The engine initialization waits until the splash finishes. */
        if (mSplashDisplayed) return init2();
        else {
            Intent dialogIntent = new Intent(mService, SplashActivity.class);
            dialogIntent.putExtra(
                    SplashActivity.IS_A11Y_SERVICE_PARAM,
                    (mMode == A11Y_SERVICE_MODE));
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mService.startActivity(dialogIntent);
            return true;
        }
    }

    /** Called from splash activity to notify that finished */
    public static void splashReady(boolean isA11yService) {

        // Recover which instance started the splash screen
        MainEngine current= (isA11yService? sAccessibilityServiceModeEngine : sSlaveModeEngine);

        /* Was initialized previously? If so, just do nothing. */
        if (current.mSplashDisplayed) {
            final Engine.OnInitListener listener= current.mOnInitListener;
            if (listener!= null) listener.onInit(0);
        }
        else {
            current.mSplashDisplayed = true;
            current.init2();
        }
    }

    /**
     * Init phase 2: actual initialization
     */
    private boolean init2() {
        /*
         * Create UI stuff: root overlay and camera view
         */
        mOverlayView= new OverlayView(mService);
        mOverlayView.setVisibility(View.INVISIBLE);
        
        mCameraLayerView= new CameraLayerView(mService);
        mOverlayView.addFullScreenLayer(mCameraLayerView);

        /*
         * camera and machine vision stuff
         */
        try {
            mCameraListener = new CameraListener(mService, this);
        }
        catch(CameraException e) {
            AlertDialog.Builder adb = new AlertDialog.Builder(mService);
            adb.setCancelable(false); // This blocks the 'BACK' button
            adb.setTitle(mService.getText(R.string.app_name));
            adb.setMessage(e.getMessage());
            adb.setPositiveButton(mService.getText(android.R.string.ok), null);

            AlertDialog ad= adb.create();
            ad.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            ad.show();

            if (mOnInitListener!= null) mOnInitListener.onInit(-1);

            return false;  // abort initialization
        }
        mCameraLayerView.addCameraSurface(mCameraListener.getCameraSurface());

        /* flip the preview when needed */
        mCameraListener.setPreviewFlip(mCameraListener.getCameraFlip());

        // orientation manager
        mOrientationManager= new OrientationManager(
                mService,
                mCameraListener.getCameraFlip(),
                mCameraListener.getCameraOrientation());

        /*
         * Create specific engine
         */
        if (mMode == A11Y_SERVICE_MODE) {
            // Init as accessibility service in mouse emulation mode
            mMotionProcessor= mMouseEmulationEngine=
                    new MouseEmulationEngine(mService, mOverlayView, mOrientationManager);
        }
        else {
            /*
             * Init in slave mode. Instantiate both gamepad and mouse emulation.
             */

            // Set valid mode for gamepad engine
            final int mode= (mSlaveOperationMode!= SlaveMode.MOUSE?
                    mSlaveOperationMode : SlaveMode.GAMEPAD_ABSOLUTE);

            // Create specific engines
            mGamepadEngine= new GamepadEngine(mService, mOverlayView, mode);
            mMouseEmulationEngine=
                    new MouseEmulationEngine(mService, mOverlayView, mOrientationManager);

            // Select enabled engine
            if (mSlaveOperationMode== SlaveMode.MOUSE) {
                mMotionProcessor= mMouseEmulationEngine;
            }
            else {
                mMotionProcessor= mGamepadEngine;
            }
        }

        mCurrentState= STATE_STOPPED;

        // Notify successful initialization
        if (mOnInitListener!= null) mOnInitListener.onInit(0);

        return true;
    }

    @Override
    public boolean start() {
        // If not initialized just fail
        if (mCurrentState == STATE_DISABLED) return false;

        // If already initialized just return startup correct
        if (mCurrentState==STATE_RUNNING) return true;

        // If paused or in standby, just resume
        if (mCurrentState == STATE_PAUSED || mCurrentState!= STATE_STANDBY) {
            resume();
        }

        /*
         * At this point means that (mCurrentState== STATE_STOPPED)
         */

        // show GUI elements
        mOverlayView.requestLayout();
        mOverlayView.setVisibility(View.VISIBLE);

        mCameraLayerView.enableDetectionFeedback();
        
        // start processing frames
        mCameraListener.startCamera();

        // start sub-engine
        mMotionProcessor.start();

        mCurrentState= STATE_RUNNING;

        return true;
    }

    @Override
    public void pause() {
        // If not initialized, stopped or already paused, just stop here
        if (mCurrentState == STATE_DISABLED ||
            mCurrentState == STATE_PAUSED   ||
            mCurrentState == STATE_STOPPED) return;

        /*
         * If STATE_RUNNING or STATE_STANDBY
         */
        mCameraLayerView.disableDetectionFeedback();
        if (mMotionProcessor!= null) mMotionProcessor.stop();

        mCurrentState= STATE_PAUSED;
    }

    @Override
    public void standby() {
        // If not initialized, stopped or already standby, just stop here
        if (mCurrentState == STATE_DISABLED ||
            mCurrentState == STATE_STANDBY   ||
            mCurrentState == STATE_STOPPED) return;

        /*
         * If STATE_RUNNING or STATE_PAUSED
         */
        mCameraLayerView.disableDetectionFeedback();
        if (mMotionProcessor!= null) mMotionProcessor.stop();

        mCurrentState= STATE_STANDBY;
    }

    @Override
    public void resume() {
        if (mCurrentState != STATE_PAUSED && mCurrentState!= STATE_STANDBY) return;

        //mCameraListener.setUpdateViewer(true);
        mCameraLayerView.enableDetectionFeedback();

        // resume specific engine
        if (mMotionProcessor!= null) mMotionProcessor.start();

        // make sure that UI changes during pause (e.g. docking panel edge) are applied
        mOverlayView.requestLayout();

        mCurrentState= STATE_RUNNING;
    }    

    @Override
    public void stop() {
        if (mCurrentState == STATE_DISABLED || mCurrentState == STATE_STOPPED) return;

        mCameraListener.stopCamera();
        mOverlayView.setVisibility(View.INVISIBLE);
        if (mMotionProcessor!= null) mMotionProcessor.stop();

        mCurrentState= STATE_STOPPED;
    }
    
    @Override
    public void cleanup() {
        if (mCurrentState == STATE_DISABLED) return;

        stop();

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
    }

    @Override
    public int getState() {
        return mCurrentState;
    }

    @Override
    public void setSlaveOperationMode(int mode) {
        if (mMode != SLAVE_MODE) throw new IllegalStateException();

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
        if (mCurrentState == STATE_RUNNING) mMotionProcessor.start();
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

    @Override
    public long getFaceDetectionElapsedTime() {
        if (mCurrentState == STATE_DISABLED || mCurrentState == STATE_STOPPED) return 0;
        return System.currentTimeMillis() - mLastFaceDetectionTimeStamp;
    }

    @Override
    public void updateFaceDetectorStatus(FaceDetectionCountdown fdc) {
        mCameraLayerView.updateFaceDetectorStatus(fdc);
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
    public void setOnFinishProcessFrame(OnFinishProcessFrame l) {
        mOnFinishProcessFrame= l;
    }

    @Override
    public void unregisterMouseListener() {
        mMouseEmulationEngine.unregisterListener();
    }

    /**
     * Process incoming camera frames (called from a secondary thread)
     *
     * @param rgba opencv matrix with the captured image
     */
    PointF mMotion= new PointF(0, 0); // avoid creating a new PointF for each frame
    @Override
    public void processFrame(Mat rgba) {
        // For these states do nothing
        if (mCurrentState== STATE_DISABLED || mCurrentState== STATE_STOPPED) return;

        /*
         * In STATE_RUNNING, STATE_PAUSED or STATE_STANDBY state.
         * Need to check if faced detected
         */
        int pictRotation = mOrientationManager.getPictureRotation();

        // set preview rotation
        mCameraListener.setPreviewRotation(pictRotation);

        // call jni part to detect and track face
        mMotion.x= mMotion.y= 0.0f;
        boolean faceDetected=
                VisionPipeline.processFrame(
                        rgba.getNativeObjAddr(),
                        mOrientationManager.getPictureFlip().getValue(),
                        pictRotation,
                        mMotion);

        if (faceDetected) mLastFaceDetectionTimeStamp= System.currentTimeMillis();

        if (mCurrentState == STATE_RUNNING) {
            // TODO: slave mode feedback
            //mCameraLayerView.updateFaceDetectorStatus(mFaceDetectionCountdown);

            // compensate mirror effect
            mMotion.x = -mMotion.x;

            // process motion on specific engine
            mMotionProcessor.processMotion(mMotion);
        }

        // Nothing when (mCurrentState == STATE_PAUSED || mCurrentState== STATE_STANDBY)

        if (mOnFinishProcessFrame != null) {
            mOnFinishProcessFrame.onOnFinishProcessFrame(faceDetected);
        }
    }
}
