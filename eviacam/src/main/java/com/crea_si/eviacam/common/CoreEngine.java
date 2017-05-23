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

import org.acra.ACRA;
import org.opencv.android.CameraException;
import org.opencv.core.Mat;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.camera.Camera;
import com.crea_si.eviacam.camera.CameraLayerView;
import com.crea_si.eviacam.camera.FrameProcessor;

import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PointF;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayDeque;

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

    /* root overlay view */
    private OverlayView mOverlayView;
    protected OverlayView getOverlayView() { return mOverlayView; }

    /* the camera viewer */
    private CameraLayerView mCameraLayerView;

    /* object in charge of capturing & processing frames */
    private Camera mCamera;

    /* object which encapsulates rotation and orientation logic */
    private OrientationManager mOrientationManager;
    protected OrientationManager getOrientationManager() { return mOrientationManager; }

    /* Last time a face has been detected */
    private volatile long mLastFaceDetectionTimeStamp;

    /* When the engine is wating for the completion of some operation */
    private boolean mWaitState = false;

    /* Store requests when the engine is waiting for the completion of a previous one.
       These stored requests will be executed eventually in the order as arrived.
       This is needed because some operations (for instance, start or stop). */
    private ArrayDeque<Runnable> mPendingRequests= new ArrayDeque<>();

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
    protected abstract void onFrame(@NonNull PointF motion, boolean faceDetected, int state);


    @Override
    public boolean init(@NonNull Service s, @Nullable OnInitListener l) {
        if (mCurrentState != STATE_DISABLED) {
            // Already started, something went wrong
            throw new IllegalStateException();
        }

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
            boolean status= intent.getBooleanExtra(SplashActivity.KEY_STATUS, false);
            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "onSplashReady: onReceive: called");

            /* Unregister receiver */
            LocalBroadcastManager.getInstance(mService).unregisterReceiver(onSplashReady);

            if (status) {
                /* Resume initialization */
                mSplashDisplayed = true;
                init2();
            }
            else {
                /* Notify failed initialization */
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mOnInitListener!= null) {
                            mOnInitListener.onInit(OnInitListener.INIT_ERROR);
                        }
                    }
                });
            }
        }
    };

    /**
     * Init phase 2: actual initialization
     */
    // TODO: remove return value
    private boolean init2() {
        mPowerManagement = new PowerManagement(mService, this);
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
            mCamera = new Camera(mService, this);
        }
        catch(CameraException e) {
            manageCameraError(e);
            return false;  // abort initialization
        }
        mCameraLayerView.addCameraSurface(mCamera.getCameraSurface());

        // orientation manager
        mOrientationManager= new OrientationManager(
                mService,
                mCamera.getCameraFlip(),
                mCamera.getCameraOrientation());

        // initialize specific motion processor(s)
        onInit(mService);

        mCurrentState= STATE_STOPPED;

        mSaveState= mCurrentState;

        /* Notify successful initialization */
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mOnInitListener!= null) {
                    mOnInitListener.onInit(OnInitListener.INIT_SUCCESS);
                }
            }
        });
        // TODO: remove return value
        return true;
    }

    private boolean isInWaitState() {
        return mWaitState;
    }

    /**
     * Process requests in two steps. First, the request is added to
     * a queue. After that, if the engine is not waiting for the
     * completion of a previous requests, the queue is processed
     * @param request runnable with the request
     */
    protected void processRequest (Runnable request) {
        // Queue request
        mPendingRequests.add(request);
        dispatchRequests();
    }

    /**
     * Dispatch previously queued requests
     */
    private void dispatchRequests() {
        while (mPendingRequests.size()> 0 && !isInWaitState()) {
            Runnable request = mPendingRequests.remove();
            request.run();
        }
    }

    // TODO: remove return value
    @Override
    public boolean start() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.start");
        final Runnable request= new Runnable() {
            @Override
            public void run() {
                doStart();
            }
        };
        processRequest(request);
        return true;
    }

    private void doStart() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.doStart");
        // If not initialized just fail
        if (mCurrentState == STATE_DISABLED) {
            Log.e(EVIACAM.TAG, "Attempt to start DISABLED engine");
            return;
        }

        // If already running just return startup correct
        if (mCurrentState==STATE_RUNNING) {
            Log.i(EVIACAM.TAG, "Attempt to start already running engine");
            return;
        }

        // If paused or in standby, just resume
        if (mCurrentState == STATE_PAUSED || mCurrentState== STATE_STANDBY) {
            resume();
            return;
        }

        /* At this point means that (mCurrentState== STATE_STOPPED) */

        if (!onStart()) {
            Log.e(EVIACAM.TAG, "start.onStart failed");
            return;
        }

        mFaceDetectionCountdown.start();

        mPowerManagement.lockFullPower();         // Screen always on
        mPowerManagement.setSleepEnabled(true);   // Enable sleep call

        /* show GUI elements */
        mOverlayView.requestLayout();
        mOverlayView.setVisibility(View.VISIBLE);

        mCameraLayerView.enableDetectionFeedback();
        
        // start processing frames
        mCamera.startCamera();

        // set wait state until camera actually starts or error
        mWaitState= true;
    }

    @Override
    public void onCameraStarted() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.onCameraStarted");
        if (mWaitState && mCurrentState == STATE_STOPPED) {
            mWaitState= false;
            mCurrentState = STATE_RUNNING;
            dispatchRequests();
        }
        else {
            Log.e(EVIACAM.TAG, "onCameraStarted: inconsistent state (ignoring): " + mCurrentState);
        }
    }

    @Override
    public void pause() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.pause");
        final Runnable request= new Runnable() {
            @Override
            public void run() {
                doPause();
            }
        };
        processRequest(request);
    }

    private void doPause() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.doPause");
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
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.standby");
        final Runnable request= new Runnable() {
            @Override
            public void run() {
                doStandby();
            }
        };
        processRequest(request);
    }

    private void doStandby() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.doStandby");
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

        String t = String.format(
                mService.getResources().getString(R.string.service_toast_pointer_stopped_toast),
                Preferences.get().getTimeWithoutDetectionEntryValue());
        EVIACAM.LongToast(mService, t);

        onStandby();

        mCurrentState= STATE_STANDBY;
    }

    @Override
    public void resume() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.resume");
        final Runnable request= new Runnable() {
            @Override
            public void run() {
                doResume();
            }
        };
        processRequest(request);
    }

    private void doResume() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.doResume");
        if (mCurrentState != STATE_PAUSED && mCurrentState!= STATE_STANDBY) return;

        onResume();

        //mCamera.setUpdateViewer(true);
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
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.stop");
        final Runnable request= new Runnable() {
            @Override
            public void run() {
                doStop();
            }
        };
        processRequest(request);
    }

    private void doStop() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.doStop");
        if (mCurrentState == STATE_DISABLED || mCurrentState == STATE_STOPPED) return;

        mCamera.stopCamera();
        mOverlayView.setVisibility(View.INVISIBLE);

        mPowerManagement.unlockFullPower();
        mPowerManagement.setSleepEnabled(false);

        onStop();

        // TODO: consider adding STATE_WAIT_STOP
        mCurrentState= STATE_STOPPED;
    }

    @Override
    public void onCameraStopped() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.onCameraStopped");
        // TODO: consider adding STATE_WAIT_STOP
    }

    @Override
    public void cleanup() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.cleanup");
        if (mCurrentState == STATE_DISABLED) return;

        /* Stop engine immediately and purge pending requests queue */
        doStop();
        mWaitState= false;
        mPendingRequests.clear();

        // Call derived
        onCleanup();

        mCamera.cleanup();
        mCamera = null;

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

    /* Called during camera startup if something goes wrong */
    @Override
    public void onCameraError(@NonNull Throwable error) {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.onCameraError");
        manageCameraError(error);
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
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.onOnScreenStateChanged");
        final Runnable request= new Runnable() {
            @Override
            public void run() {
                doOnOnScreenStateChange();
            }
        };
        processRequest(request);
    }

    private void doOnOnScreenStateChange() {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "CoreEngine.doOnOnScreenStateChanged");
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
            mSaveState= mCurrentState;
            if (mSaveState!= Engine.STATE_STANDBY) stop();
        }
    }

    /**
     * Handle camera errors
     * @param error the error
     */
    private void manageCameraError(@NonNull Throwable error) {
        /* Cast into CameraException */
        CameraException cameraException;
        if (error.getClass().isAssignableFrom(CameraException.class)) {
            cameraException = (CameraException) error;
        }
        else {
            cameraException = new CameraException(CameraException.CAMERA_ERROR,
                    error.getLocalizedMessage(), error);
        }

        boolean allowRetry = false;

        if (mCurrentState == STATE_DISABLED) {
            /* Exception during initialization. Non recoverable. */
            cleanup();

            ACRA.getErrorReporter().handleSilentException(cameraException);

            /* Notify whoever requested the initialization */
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mOnInitListener!= null) {
                        mOnInitListener.onInit(OnInitListener.INIT_ERROR);
                    }
                }
            });
        }
        else if (mCurrentState == STATE_STOPPED && mWaitState &&
                cameraException.getProblem() == CameraException.CAMERA_IN_USE) {
            /* Exception during camera startup because is in use, allow to retry */
            allowRetry = true;
        }
        else {
            /* Other camera exceptions */
            if (null != mCamera) {
                mCamera.stopCamera();
            }

            if (cameraException.getProblem() != CameraException.CAMERA_DISABLED) {
                ACRA.getErrorReporter().handleSilentException(cameraException);
            }
        }


        final DialogInterface.OnClickListener okListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cleanup();
            }
        };

        AlertDialog.Builder adb = new AlertDialog.Builder(mService);
        adb.setCancelable(false); // This blocks the 'BACK' button
        adb.setTitle(mService.getText(R.string.app_name));
        adb.setMessage(cameraException.getLocalizedMessage());
        if (!allowRetry) {
            adb.setPositiveButton(mService.getText(android.R.string.ok), okListener);
        }
        else {
            adb.setNeutralButton(mService.getText(R.string.close), okListener);
            adb.setPositiveButton(mService.getText(R.string.opencv_retry),
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Try to start the camera again
                    mCamera.stopCamera();
                    mCamera.startCamera();
                }
            });
        }
        /*
        adb.setPositiveButton(mService.getText(R.string.send_report),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ACRA.getErrorReporter().handleException(error);
                    }});
        */
        AlertDialog ad= adb.create();
        //noinspection ConstantConditions
        ad.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        ad.show();
    }


    // avoid creating a new PointF for each frame
    private PointF mMotion= new PointF(0, 0);

    /**
     * Process incoming camera frames (called from a secondary thread)
     *
     * @param rgba opencv matrix with the captured image
     */
    @Override
    public void processFrame(@NonNull Mat rgba) {
        // For these states do nothing
        if (mCurrentState== STATE_DISABLED || mCurrentState== STATE_STOPPED ||
                isInWaitState()) return;

        /*
         * In STATE_RUNNING, STATE_PAUSED or STATE_STANDBY state.
         * Need to check if face detected
         */
        int pictRotation = mOrientationManager.getPictureRotation();

        // set preview rotation
        mCamera.setPreviewRotation(pictRotation);

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
