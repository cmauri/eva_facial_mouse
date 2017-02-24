/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
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
package com.crea_si.eviacam.common;

import org.opencv.android.CameraException;
import org.opencv.core.Mat;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.camera.CameraLayerView;
import com.crea_si.eviacam.camera.CameraListener;
import com.crea_si.eviacam.camera.FrameProcessor;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.PointF;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.WindowManager;

/**
 * Provides an abstract implementation for the Engine interface. The class is in charge of:
 *
 * - engine initialization and state management
 * - camera and image processing to detect face and track motion
 * - UI: main overlay and camera viewer
 *
 */
public abstract class CoreEngine implements Engine, FrameProcessor,
        PowerManagement.OnScreenStateChangeListener {
    // stores when the last detection of a face occurred
    private final FaceDetectionCountdown mFaceDetectionCountdown = new FaceDetectionCountdown();

    // handler to run things on the main thread
    private final Handler mHandler= new Handler();

    // power management stuff
    private PowerManagement mPowerManagement;

    /* current engine state */
    private volatile int mCurrentState= STATE_DISABLED;
    @Override
    public int getState() {
        return mCurrentState;
    }

    // state before switching screen off
    private int mSaveState= -1;

    /* splash screen has been displayed? */
    private boolean mSplashDisplayed = false;

    /* listener to notify when the initialization is done */
    private OnInitListener mOnInitListener;

    /* reference to the service which started the engine */
    private Service mService;
    protected Service getService() { return mService; }

    /* root overlay view */
    private OverlayView mOverlayView;
    protected OverlayView getOverlayView() { return mOverlayView; }

    /* the camera viewer */
    private CameraLayerView mCameraLayerView;

    /* object in charge of capturing & processing frames */
    private CameraListener mCameraListener;

    /* object which encapsulates rotation and orientation logic */
    private OrientationManager mOrientationManager;
    protected OrientationManager getOrientationManager() { return mOrientationManager; }

    /* Last time a face has been detected */
    private volatile long mLastFaceDetectionTimeStamp;

    /* Abstract methods to be implemented by derived classes */

    /**
     * Called just before the initialization is finished
     *
     * @param service service which started the engine
     */
    protected abstract void onInit(Service service);

    /**
     * Called at the beginning of the cleanup sequence
     */
    protected abstract void onCleanup();

    /**
     * Called at the beginning of the start sequence
     *
     * @return should return false when something went wrong to abort start sequence
     */
    protected abstract boolean onStart();

    /**
     * Called at the end of the stop sequence
     */
    protected abstract void onStop();

    /**
     * Called at the end of the pause sequence
     */
    protected abstract void onPause();

    /**
     * Called at the end of the standby sequence
     */
    protected abstract void onStandby();

    /**
     * Called at the beginning of the resume sequence
     */
    protected abstract void onResume();

    /**
     * Called each time a frame is processed and the engine is in one of these states:
     *     STATE_RUNNING, STATE_PAUSED or STATE_STANDBY
     *
     * @param motion motion vector, could be (0, 0) if motion not detected or the engine is
     *               paused or in standby mode
     * @param faceDetected whether or not a face was detected for the last frame, note
     *                     not all frames are checked for the face detection algorithm
     * @param state current state of the engine
     */
    protected abstract void onFrame(PointF motion, boolean faceDetected, int state);


    @Override
    public boolean init(Service s, OnInitListener l) {
        if (mCurrentState != STATE_DISABLED) {
            // Already started, something went wrong
            throw new IllegalStateException();
        }

        /* Proceed with initialization. Store service and listener */
        mPowerManagement = new PowerManagement(s, this);
        mService= s;
        mOnInitListener= l;

        /* Show splash screen if not already shown. The splash screen is also used to
           request the user the required permissions to run this software.
           In the past, it was also used for OpenCV detection and installation.
           The engine initialization waits until the splash finishes. */
        if (mSplashDisplayed) return init2();
        else {
            /* Register receiver for splash finished */
            LocalBroadcastManager.getInstance(s).registerReceiver(
                    onSplashReady,
                    new IntentFilter(SplashActivity.FINISHED_INTENT_FILTER));

            /* Start splash activity */
            Intent dialogIntent = new Intent(mService, SplashActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mService.startActivity(dialogIntent);

            return true;
        }
    }

    /* Receiver which is called when the splash activity finishes */
    private BroadcastReceiver onSplashReady= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            EVIACAM.debug("onSplashReady: onReceive: called");

            /* Unregister receiver */
            LocalBroadcastManager.getInstance(mService).unregisterReceiver(onSplashReady);

            /* Resume initialization */
            if (mOnInitListener!= null) mOnInitListener.onInit(0);
            mSplashDisplayed = true;
            init2();
        }
    };

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

        // initialize specific motion processor(s)
        onInit(mService);

        mCurrentState= STATE_STOPPED;

        mSaveState= mCurrentState;

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

        /* At this point means that (mCurrentState== STATE_STOPPED) */

        if (!onStart()) return false;

        mFaceDetectionCountdown.start();

        mPowerManagement.lockFullPower();         // Screen always on
        mPowerManagement.setSleepEnabled(true);   // Enable sleep call

        /* show GUI elements */
        mOverlayView.requestLayout();
        mOverlayView.setVisibility(View.VISIBLE);

        mCameraLayerView.enableDetectionFeedback();
        
        // start processing frames
        mCameraListener.startCamera();

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

        mPowerManagement.unlockFullPower();

        onPause();

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

        mPowerManagement.unlockFullPower();
        mPowerManagement.setSleepEnabled(true);   // Enable sleep call

        Service s= getService();
        if (s!= null) {
            Resources res = s.getResources();
            String t = String.format(
                    res.getString(R.string.pointer_stopped_toast),
                    Preferences.get().getTimeWithoutDetectionEntryValue());
            EVIACAM.LongToast(s, t);
        }

        onStandby();

        mCurrentState= STATE_STANDBY;
    }

    @Override
    public void resume() {
        if (mCurrentState != STATE_PAUSED && mCurrentState!= STATE_STANDBY) return;

        onResume();

        //mCameraListener.setUpdateViewer(true);
        mCameraLayerView.enableDetectionFeedback();

        mPowerManagement.lockFullPower();         // Screen always on
        mPowerManagement.setSleepEnabled(true);   // Enable sleep call

        mFaceDetectionCountdown.start();

        // make sure that UI changes during pause (e.g. docking panel edge) are applied
        mOverlayView.requestLayout();

        mCurrentState= STATE_RUNNING;
    }    

    @Override
    public void stop() {
        if (mCurrentState == STATE_DISABLED || mCurrentState == STATE_STOPPED) return;

        mCameraListener.stopCamera();
        mOverlayView.setVisibility(View.INVISIBLE);

        mPowerManagement.unlockFullPower();
        mPowerManagement.setSleepEnabled(false);

        onStop();

        mCurrentState= STATE_STOPPED;
    }
    
    @Override
    public void cleanup() {
        if (mCurrentState == STATE_DISABLED) return;

        stop();

        onCleanup();

        mCameraListener= null;

        mOrientationManager.cleanup();
        mOrientationManager= null;

        mCameraLayerView= null;

        mOverlayView.cleanup();
        mOverlayView= null;

        mPowerManagement.cleanup();
        mPowerManagement = null;

        mCurrentState= STATE_DISABLED;

        mFaceDetectionCountdown.cleanup();
    }

    @Override
    public boolean isReady() {
        return (mCurrentState != STATE_DISABLED);
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

    /**
     * Called when screen goes ON or OFF
     */
    @Override
    public void onOnScreenStateChange() {
        if (mPowerManagement.getScreenOn()) {
            // Screen switched on
            if (mSaveState == Engine.STATE_RUNNING ||
                    mSaveState == Engine.STATE_STANDBY) start();
            else if (mSaveState == Engine.STATE_PAUSED) {
                start();
                pause();
            }
        }
        else {
            // Screen switched off
            mSaveState= getState();
            if (mSaveState!= Engine.STATE_STANDBY) stop();
        }
    }


    // avoid creating a new PointF for each frame
    private PointF mMotion= new PointF(0, 0);

    /**
     * Process incoming camera frames (called from a secondary thread)
     *
     * @param rgba opencv matrix with the captured image
     */
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

        // compensate mirror effect
        mMotion.x = -mMotion.x;

        onFrame(mMotion, faceDetected, mCurrentState);

        // States to be managed below: RUNNING, PAUSED, STANDBY

        if (faceDetected) mFaceDetectionCountdown.start();

        if (mCurrentState == STATE_STANDBY) {
            if (faceDetected) {
                // "Awake" from standby state
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
            else {
                // In standby reduce CPU cycles by sleeping but only if screen went off
                if (!mPowerManagement.getScreenOn()) mPowerManagement.sleep();
            }
        }
        else if (mCurrentState == STATE_RUNNING) {
            if (mFaceDetectionCountdown.hasFinished() && !mFaceDetectionCountdown.isDisabled()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        standby();
                    }
                });
            }
        }

        // Nothing more to do (state == Engine.STATE_PAUSED)
        updateFaceDetectorStatus(mFaceDetectionCountdown);
    }
}
