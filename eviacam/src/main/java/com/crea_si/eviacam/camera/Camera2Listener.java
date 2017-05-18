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

package com.crea_si.eviacam.camera;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.acra.ACRA;
import org.opencv.android.CameraException;
import org.opencv.android.FpsMeter;
import org.opencv.android.MyCameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

import com.android.ex.camera2.blocking.BlockingCameraManager;
import com.android.ex.camera2.blocking.BlockingSessionCallback;
import com.android.ex.camera2.blocking.BlockingStateCallback;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;
import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.util.FlipDirection;

/**
 * Simple camera2 based interface for capturing camera image in real time for CV
 */
@TargetApi(21)
class Camera2Listener {
    private static final String TAG= "Camera2Listener";

    private final Context mContext;
    
    // callback to process frames
    private final FrameProcessor mFrameProcessor;

    // handler to run things on the main thread
    private final Handler mHandler= new Handler();
    
    // surface on which the image from the camera will be drawn
    private SurfaceView mCamera2View;
    SurfaceView getCameraSurface() {
        return mCamera2View;
    }

    // stores whether is supposed that the surface is ready to draw on it
    // note that this information is not authoritative, it just reflects
    // when is supposed that the camera is processing frames
    private boolean mSurfaceReady= false;

    /*
       Physical mounting rotation of the camera (i.e. whether the frame needs a flip
       operation). For instance, this is needed for those devices with rotating
       camera such as the Lenovo YT3-X50L)
     */
    private FlipDirection mCameraFlip= FlipDirection.NONE;
    FlipDirection getCameraFlip() { return mCameraFlip; }

    // physical orientation of the camera (0, 90, 180, 270)
    private int mCameraOrientation;
    int getCameraOrientation() { return mCameraOrientation; }

    // store the rotation needed to draw the picture in upwards position
    private int mPreviewRotation= 0;

    // capture size
    private Size mCaptureSize;

    // selected capture FPS range
    private Range<Integer> mTargetFPSRange;

    // captured frames count for debugging purposes
    private int mCapturedFrames;

    // camera device, null when the camera is closed
    private CameraDevice mCameraDevice;

    // facilities to run things on a secondary thread
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    // capture session
    private CameraCaptureSession mCaptureSession;

    // reader to extract camera frames
    private ImageReader mImageReader;

    // A {@link Semaphore} to prevent cleanup from exiting before closing the camera.
    private Semaphore mCameraCloseLock;

    // A {@link Semaphore} to prevent stopCamera from exiting before stopping the camera.
    private Semaphore mCameraStopLock;

    /* Cached images */
    private Mat mCacheImage;
    private Bitmap mCacheBitmap;

    private FpsMeter mFpsMeter = null;

    /**
     * Constructor
     * @param c context
     * @param fp object that will receive the camera callbacks
     */
    Camera2Listener(@NonNull Context c, @NonNull FrameProcessor fp) throws CameraException {
        mContext= c;
        mFrameProcessor= fp;

        // Pick best camera and get capture parameters
        String cameraId = setUpCameraParameters();

        // Start background thread
        startCameraThread();

        // View for drawing camera output
        mCamera2View= new SurfaceView(mContext);

        mCacheBitmap = Bitmap.createBitmap(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
                Bitmap.Config.ARGB_8888);

        mCacheImage= new Mat(mCaptureSize.getWidth(), mCaptureSize.getHeight(), CvType.CV_8UC4);

        /* Uncomment to enable the FPS meter for debugging */
        if (BuildConfig.DEBUG) {
            mFpsMeter = new FpsMeter();
            mFpsMeter.setResolution(mCaptureSize.getWidth(), mCaptureSize.getHeight());
        }

        openCamera(cameraId);
    }

    /**
     * Pick the best camera available for face tracking and set its flip, rotation and best
     * capture size
     *
     * @return ID if the camera
     * @throws CameraException when error
     */
    private String setUpCameraParameters()
            throws CameraException {
        /* Get camera manager */
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Cannot obtain camera manager");
            throw new CameraException(CameraException.NO_CAMERAS_AVAILABLE,
                    mContext.getResources().getString(R.string.service_camera_no_available));
        }

        /* Get available cameras */
        String[] cameraIdList;
        try {
            cameraIdList= manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot query camera id list");
            throw CameraAccessException2CameraException(mContext, e);
        }

        if (cameraIdList.length< 1) {
            Log.e(TAG, "No cameras available");
            throw new CameraException(CameraException.NO_CAMERAS_AVAILABLE,
                    mContext.getResources().getString(R.string.service_camera_no_available));
        }

        /* Detect and classify available cameras according to its lens facing */
        int frontCameraIdx= -1, backCameraIdx= -1, externalCameraIdx= -1;
        for (int i= 0; i< cameraIdList.length; i++) {
            CameraCharacteristics cameraCharacteristics;
            try {
                cameraCharacteristics= manager.getCameraCharacteristics(cameraIdList[i]);
            } catch (CameraAccessException e) {
                Log.e(TAG, "Cannot get camera characteristics: " + cameraIdList[i]);
                continue;
            }

            Integer lensFacing= cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing== null) {
                /* If fails to retrieve lens facing assume external camera */
                Log.i(TAG, "Cannot retrieve lens facing for camera: " + cameraIdList[i]);
                externalCameraIdx= i;
            }
            else {
                switch (lensFacing) {
                    case CameraMetadata.LENS_FACING_FRONT:
                        frontCameraIdx = i;
                        break;
                    case CameraMetadata.LENS_FACING_BACK:
                        backCameraIdx = i;
                        break;
                    case CameraMetadata.LENS_FACING_EXTERNAL:
                        externalCameraIdx = i;
                        break;
                }
            }
        }

        /*
         * Pick the best available camera according to its lens facing
         *
         * For some devices, notably the Lenovo YT3-X50L, have only one camera that can
         * be rotated to point to the user's face. In this case the camera is reported as
         * facing back (TODO: confirm that this is still true with the camera2 API).
         * Therefore, we try to detect all cameras of the device and pick
         * the facing front one, if any. Otherwise, we pick an external camera and finally
         * pick a facing back camera. In the latter case report that the image needs a
         * vertical flip before fixing the orientation.
         */
        Log.i(TAG, "Try front camera");
        int bestCamera= frontCameraIdx;
        if (bestCamera== -1) {
            Log.i(TAG, "Try external camera");
            bestCamera= externalCameraIdx;
        }
        if (bestCamera== -1) {
            Log.i(TAG, "Try back camera");
            bestCamera= backCameraIdx;
            mCameraFlip= FlipDirection.VERTICAL;
        }
        if (bestCamera== -1) {
            Log.e(TAG, "None of the cameras is suitable for the job. Aborting.");
            throw new CameraException(CameraException.CAMERA_ERROR,
                    mContext.getResources().getString(R.string.service_camera_error));
        }

        String cameraId= cameraIdList[bestCamera];

        CameraCharacteristics cameraCharacteristics;
        try {
            cameraCharacteristics= manager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Cannot get camera characteristics: " + cameraId);
            throw CameraAccessException2CameraException(mContext, e);
        }

        Log.d(TAG, "Supported hardware level: " +
                cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL));

        /*
         * The orientation of the camera is the angle that the camera image needs
         * to be rotated clockwise so it shows correctly on the display in its natural orientation.
         * It should be 0, 90, 180, or 270.
         */
        Integer cameraOrientation=
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (cameraOrientation== null) {
            Log.e(TAG, "Cannot get camera orientation");
            throw new CameraException(CameraException.CAMERA_ERROR,
                    mContext.getResources().getString(R.string.service_camera_error));
        }
        else {
            mCameraOrientation = cameraOrientation;
            Log.i(TAG, "Camera orientation: " + mCameraOrientation);
        }

        /*
         * Select the best camera preview size
         */
        StreamConfigurationMap map = cameraCharacteristics.
                get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map== null) {
            Log.e(TAG, "Cannot get camera map");
            throw new CameraException(CameraException.CAMERA_ERROR,
                    mContext.getResources().getString(R.string.service_camera_error));
        }

        Log.i(TAG, "StreamConfigurationMap" + map.toString());
        Log.i(TAG, "Preview sizes");
        Size[] outputSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
        if (outputSizes== null) {
            Log.e(TAG, "Cannot get output sizes");
            throw new CameraException(CameraException.CAMERA_ERROR,
                    mContext.getResources().getString(R.string.service_camera_error));
        }

        for (Size size : outputSizes) {
            Log.i(TAG, "(" + size.getWidth() + ", " + size.getHeight() + ")");
        }

        /* Helper class */
        class SizeAccessor implements MyCameraBridgeViewBase.ListItemAccessor {
            @Override
            public int getWidth(Object obj) {
                return ((Size) obj).getWidth();
            }

            @Override
            public int getHeight(Object obj) {
                return ((Size) obj).getHeight();
            }
        }

        org.opencv.core.Size size= MyCameraBridgeViewBase.calculateBestCameraFrameSize(
                Arrays.asList(outputSizes), new SizeAccessor(),
                Camera.DESIRED_CAPTURE_WIDTH, Camera.DESIRED_CAPTURE_HEIGHT);
        if (size.width<= 0 || size.height<= 0) {
            throw new CameraException(CameraException.CAMERA_ERROR,
                    mContext.getResources().getString(R.string.service_camera_error));
        }
        mCaptureSize= new Size((int) size.width, (int) size.height);

        /*
         * Tries to pick a frame rate higher or equal than 15 fps.
         *
         * CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES always returns a list of
         * supported preview fps ranges with at least one element.
         * Every element is an FPS range
         * TODO (still true?): The list is sorted from small to large (first by maximum fps and then
         * minimum fps).
         *
         * With the old API:
         * Nexus 7: the list has only one element (4000,60000)
         * Samsung Galaxy Nexus: (15000,15000),(15000,30000),(24000,30000)
         */
        Range<Integer>[] cameraTargetFPSRanges=
                cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        if (cameraTargetFPSRanges== null) {
            Log.w(TAG, "Cannot get camera target FPS ranges. Ignoring.");
        }
        else {
            int winner= cameraTargetFPSRanges.length-1;
            int maxLimit= cameraTargetFPSRanges[winner].getUpper();

            Log.i(TAG, "Camera FPS ranges");
            for (Range<Integer> r : cameraTargetFPSRanges) {
                Log.i(TAG, r.toString());
            }

            for (int i= winner-1; i>= 0; i--) {
                if (cameraTargetFPSRanges[i].getUpper()!= maxLimit ||
                        cameraTargetFPSRanges[i].getLower()< 15000) {
                    break;
                }
                winner= i;
            }
            mTargetFPSRange= cameraTargetFPSRanges[winner];
        }

        return cameraId;
    }

    /**
     * Open the camera device
     *
     * @param cameraId ID of the camera
     * @throws CameraException when error
     */
    private void openCamera(String cameraId) throws CameraException {
        BlockingStateCallback blockingCallback =
                new BlockingStateCallback(new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.i(TAG, "CameraDevice.StateCallback: onOpened");
                // Nothing to store. Device returned by the openCamera call
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                // TODO: not tested
                Log.w(TAG, "CameraDevice.StateCallback: onDisconnected");
                camera.close();
                mCameraDevice = null;

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFrameProcessor.onCameraError(
                                new CameraException(CameraException.CAMERA_ERROR,
                                        mContext.getString(R.string.camera_no_longer_available)));
                        mFrameProcessor.onCameraStopped();
                    }
                });
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.e(TAG, "CameraDevice.StateCallback: onError" + error);
                closeCamera();

                final int otherError;
                final String errorMessage;
                switch(error) {
                    case CameraDevice.StateCallback.ERROR_CAMERA_DEVICE:
                        Log.e(TAG, "The camera device has encountered a fatal error");
                        otherError= CameraException.CAMERA_ERROR;
                        errorMessage= mContext.getResources().getString(
                                R.string.service_camera_error);
                        break;
                    case CameraDevice.StateCallback.ERROR_CAMERA_DISABLED:
                        Log.e(TAG, "The camera device could not be opened due to a device policy");
                        otherError= CameraException.CAMERA_DISABLED;
                        errorMessage= mContext.getResources().getString(
                                R.string.service_camera_disabled_error);
                        break;
                    case CameraDevice.StateCallback.ERROR_CAMERA_IN_USE:
                        Log.e(TAG, "The camera device is in use already.");
                        otherError= CameraException.CAMERA_IN_USE;
                        errorMessage= mContext.getResources().getString(
                                R.string.service_camera_no_access);
                        break;
                    case CameraDevice.StateCallback.ERROR_CAMERA_SERVICE:
                        Log.e(TAG, "The camera service has encountered a fatal error.");
                        otherError= CameraException.CAMERA_ERROR;
                        errorMessage= mContext.getResources().getString(
                                R.string.service_camera_error);
                        break;
                    case CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE:
                        Log.e(TAG, "The camera device could not be opened because there are too many other open camera devices.");
                        otherError= CameraException.CAMERA_IN_USE;
                        errorMessage= mContext.getString(R.string.max_cameras_in_use);
                        break;
                    default:
                        otherError= CameraException.CAMERA_ERROR;
                        errorMessage= "";
                }

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFrameProcessor.onCameraError(new CameraException(otherError, errorMessage));
                        //mFrameProcessor.onCameraStopped();
                    }
                });
            }

            @Override
            public void onClosed (@NonNull CameraDevice camera) {
                Log.i(TAG, "CameraDevice.StateCallback: onClose");
                Semaphore sem= mCameraCloseLock;
                if (null != sem) {
                    sem.release();
                }
            }
        });

        /* Open camera */
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        BlockingCameraManager blockingManager = new BlockingCameraManager(manager);
        try {
            mCameraDevice = blockingManager.openCamera(cameraId, blockingCallback, mBackgroundHandler);
        } catch (BlockingCameraManager.BlockingOpenException|TimeoutRuntimeException e) {
            Log.e(TAG, "Timed out while trying to open the camera: " + e.getMessage());
            throw new CameraException(CameraException.CAMERA_ERROR, e.getMessage(), e);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error while trying to open the camera: " + e.getReason());
            throw CameraAccessException2CameraException(mContext, e);
        }
    }

    /**
     * Close camera device
     *
     * Block until the camera is fully closed.
     */
    private void closeCamera() {
        if (null != mCameraDevice) {
            mCameraCloseLock= new Semaphore(0);

            mCameraDevice.close();

            try {
                mCameraCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Log.e(TAG, "InterruptedException closing the camera: " + e.getLocalizedMessage());
            }

            mCameraCloseLock= null;
            mCameraDevice = null;

            mCaptureSession= null;
            mImageReader= null;
        }
    }

    /**
     * Start camera capture
     *
     * Once started, the client is notified using the onCameraStarted callback. When
     * error, the onCameraError is called.
     */
    void startCamera() {
        if (mCameraDevice == null) {
            Log.e(TAG, "Trying to start unopened camera");
            throw new IllegalStateException("Trying to start unopened camera");
        }

        try {
            createCaptureSession();

            mSurfaceReady= true;

            /* TODO: wait until the camera actually starts capturing frames */
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Notify: onCameraStarted");
                    mFrameProcessor.onCameraStarted();
                }
            });
        } catch (TimeoutRuntimeException|CameraAccessException e) {
            mFrameProcessor.onCameraError(e);
        }
    }

    /**
     * Create capture preview session
     *
     * TODO: set fixed shutter speed
     */
    private void createCaptureSession () throws CameraAccessException, TimeoutRuntimeException {
        if (mImageReader!= null || mCaptureSession!= null) {
            Log.i(TAG, "createCaptureSession: already created");
            return;
        }

        /* Create image reader. Need to be a member to avoid being garbage collected */
        mImageReader = ImageReader.newInstance(
                mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.YUV_420_888, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

        /* Now create the capture session */
        CaptureRequest.Builder previewRequestBuilder;
        try {
            previewRequestBuilder=
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(mImageReader.getSurface());
            mCameraDevice.createCaptureSession(Collections.singletonList(mImageReader.getSurface()),
                    mCameraCaptureSessionCallback, mBackgroundHandler);

            /* Wait for the session to complete */
            Log.d(TAG, "Waiting on session.");
            mCaptureSession = mCameraCaptureSessionCallback.waitAndGetSession(2500);

            // STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES

            if (null != mTargetFPSRange) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        mTargetFPSRange);
            }

            //previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
            // previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // Comment out the above and uncomment this to disable continuous autofocus and
            // instead set it to a fixed value of 20 diopters. This should make the picture
            // nice and blurry for denoised edge detection.
            // previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
            //		   CaptureRequest.CONTROL_AF_MODE_OFF);
            // previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 20.0f);
            // Finally, we start displaying the camera preview.

            mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                    mCaptureCallback, mBackgroundHandler);
            Log.d(TAG, "setRepeatingRequest done");
        } catch (CameraAccessException|TimeoutRuntimeException e) {
            Log.e(TAG, "createCaptureSession failed: " + e.getLocalizedMessage());
            if (mCaptureSession!= null) {
                mCaptureSession.close();
                mCaptureSession = null;
            }

            mImageReader.close();
            mImageReader= null;

            throw e;
        }
    }

    /* Listener that gets called when a new image is available */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener=
            new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (BuildConfig.DEBUG) Log.d(TAG,"onImageAvailable");
            Image image;

            try {
                image = reader.acquireLatestImage();
                if(image == null) {
                    Log.d(TAG,"onImageAvailable: null image");
                    return;
                }

                /* Informative log for debugging purposes */
                mCapturedFrames++;
                if (mCapturedFrames < 100) {
                    if ((mCapturedFrames % 10) == 0) {
                        Log.i(TAG, "onCameraFrame. Frame count:" + mCapturedFrames);
                    }
                }

                Mat yuv= imageToMat(image);
                Imgproc.cvtColor(yuv, mCacheImage, Imgproc.COLOR_YUV2BGRA_YV12);

                mFrameProcessor.processFrame(mCacheImage);

                Utils.matToBitmap(mCacheImage, mCacheBitmap);

                synchronized (this) {
                    if (mSurfaceReady) {
                        Canvas canvas = mCamera2View.getHolder().lockCanvas();
                        if (canvas != null) {
                            drawBitmap(canvas);
                            mCamera2View.getHolder().unlockCanvasAndPost(canvas);
                        }
                    }
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Image fmt:" + image.getFormat());
                    Log.d(TAG, "Size: " + image.getWidth() + "x" + image.getHeight());
                    Log.d(TAG, "Planes: " + image.getPlanes().length);
                    for (Image.Plane plane : image.getPlanes()) {
                        Log.d(TAG, plane.toString());
                    }
                    Log.d(TAG, "Crop rectangle: " + image.getCropRect().toString());
                    Log.d(TAG, "Mat type: " + yuv);
                    Log.d(TAG, "Bitmap type: " + mCacheBitmap.getWidth() + "*" +
                            mCacheBitmap.getHeight());
                }
            } catch (IllegalStateException e) {
                Log.w(TAG, "Too many images queued, dropping image");
                return;
            }
            image.close();
        }
    };

    /* Callback block for capture session state management */
    private BlockingSessionCallback mCameraCaptureSessionCallback =
            new BlockingSessionCallback(new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "CameraCaptureSession.StateCallback: onConfigured");
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "CameraCaptureSession.StateCallback: onConfigureFailed");
                }

                @Override
                public void onActive(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "CameraCaptureSession.StateCallback: onActive");
                    // Called when the camera starts capturing frames
                }

                @Override
                public void onClosed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "CameraCaptureSession.StateCallback: onClosed");
                }

                @Override
                public void onReady (@NonNull CameraCaptureSession session) {
                    /* Called when the session is ready. This happens asynchronously
                       once the session is initialized or just after stopping it.
                       In the latter case notify the completion of the operation
                       releasing a semaphore. */
                    Log.d(TAG, "CameraCaptureSession.StateCallback: onReady");
                    Semaphore sem= mCameraStopLock;
                    if (sem!= null) {
                        sem.release();
                    }
                }

                @Override
                public void onSurfacePrepared (@NonNull CameraCaptureSession session,
                                               @NonNull Surface surface) {
                    Log.d(TAG, "CameraCaptureSession.StateCallback: onSurfacePrepared");
                }
            });

    // Cached bitmap to avoid the allocation cost for each frame
    private Matrix mMatrixCached = new Matrix();

    /**
     * Draw image stored in mCachedBitmap to the canvas
     * @param canvas a canvas reference
     */
    private void drawBitmap(Canvas canvas) {
        /* Canvas size */
        final int canvasWidth= canvas.getWidth();
        final int canvasHeight= canvas.getHeight();
        if (0>= canvasWidth || 0>= canvasHeight) return;

        /* Bitmap (captured image) size */
        final int bitmapWidth= mCacheBitmap.getWidth();
        final int bitmapHeight= mCacheBitmap.getHeight();

        /*
         * Set rotation matrix
         */
        mMatrixCached.reset();

        if (mCameraFlip== FlipDirection.HORIZONTAL) {
            mMatrixCached.postScale(-1.0f, 1.0f);
            mMatrixCached.postTranslate(canvasWidth, 0.0f);
        }
        else if (mCameraFlip== FlipDirection.VERTICAL) {
            mMatrixCached.postScale(1.0f, -1.0f);
            mMatrixCached.postTranslate(0.0f, canvasHeight);
        }
        mMatrixCached.postRotate((float) mPreviewRotation, canvasWidth / 2, canvasHeight / 2);
        canvas.setMatrix(mMatrixCached);

        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

        if (canvasWidth!= bitmapWidth || canvasHeight != bitmapHeight) {
            // Need to scale captured image to draw
            float scale = Math.min((float) canvasHeight / (float) bitmapHeight,
                                   (float) canvasWidth / (float) bitmapWidth);

            canvas.drawBitmap(mCacheBitmap,
                    new Rect(0, 0, bitmapWidth, bitmapHeight),
                    new Rect((int)((canvasWidth - scale * bitmapWidth) / 2),
                            (int)((canvasHeight - scale * bitmapHeight) / 2),
                            (int)((canvasWidth - scale * bitmapWidth) / 2 + scale * bitmapWidth),
                            (int)((canvasHeight - scale * bitmapHeight) / 2 + scale * bitmapHeight)),
                    null);
        } else {
            canvas.drawBitmap(mCacheBitmap,
                    new Rect(0, 0, bitmapWidth, bitmapHeight),
                    new Rect((canvasWidth - bitmapWidth) / 2,
                            (canvasHeight - bitmapHeight) / 2,
                            (canvasWidth - bitmapWidth) / 2 + bitmapWidth,
                            (canvasHeight - bitmapHeight) / 2 + bitmapHeight), null);
        }

        if (mFpsMeter != null) {
            mFpsMeter.measure();
            mFpsMeter.draw(canvas, 20, 30);
        }
    }


    /**
     * Stop the camera capture.
     *
     * This method does not return until the capture has completely stop.
     */
    void stopCamera() {
        if (mCameraDevice == null) {
            Log.w(TAG, "Trying to stop closed camera");
            return;
        }
        Log.d(TAG, "stopCamera: enter");

        boolean wasRunning= false;  // avoid multiple callback notifications
        synchronized (this) {
            mSurfaceReady = false;

            if (null != mCaptureSession) {
                wasRunning= true;
                /* As stopRepeating is asynchronous use a semaphore
                   to lock until the operation completes */
                mCameraStopLock = new Semaphore(0);
                try {
                    mCaptureSession.stopRepeating();
                    mCameraStopLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
                } catch (InterruptedException|CameraAccessException e) {
                    // Ignore errors when stopping camera
                    Log.e(TAG, e.getLocalizedMessage());
                    ACRA.getErrorReporter().handleSilentException(e);
                }
                mCameraStopLock= null;
                mCaptureSession.close();
                mCaptureSession = null;
            }

            if (null != mImageReader) {
                wasRunning= true;
                mImageReader.close();
                mImageReader= null;
            }
        }

        /* Notify client (although is not necessary because the operation is synchronous) */
        if (wasRunning) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Notify: onCameraStopped");
                    mFrameProcessor.onCameraStopped();
                }
            });
        }

        Log.d(TAG, "stopCamera: completed");
    }

    /**
     * Sets the rotation to perform to the camera image before is displayed
     * in the preview surface
     *
     * @param rotation rotation to perform (clockwise) in degrees
     *                 legal values: 0, 90, 180, or 270
     */
    void setPreviewRotation (int rotation) {
        mPreviewRotation= rotation;
    }

    /**
     * Free resources
     */
    public void cleanup () {
        stopCamera();

        closeCamera();

        stopCameraThread();

        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
            mCacheBitmap= null;
        }

        if (mCacheImage != null) {
            mCacheImage.release();
            mCacheImage= null;
        }
    }

    /* Callback block for capture session capture management */
    private final CameraCaptureSession.CaptureCallback mCaptureCallback=
            new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session,
                                     @NonNull CaptureRequest request,
                                     long timestamp, long frameNumber) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "CameraCaptureSession.CaptureCallback: onCaptureStarted");
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "CameraCaptureSession.CaptureCallback: onCaptureCompleted");
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                    @NonNull CaptureRequest request,
                                    @NonNull CaptureFailure failure) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "CameraCaptureSession.CaptureCallback: onCaptureFailed");
        }
    };

    // TODO: provide a better implementation
    static private Mat imageToMat(Image image) {
        ByteBuffer buffer;
        int rowStride;
        int pixelStride;
        int width = image.getWidth();
        int height = image.getHeight();
        int offset = 0;

        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[image.getWidth() * image.getHeight() * 3 /2];// ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();
            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;
            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8;
                if (pixelStride == bytesPerPixel) {
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);

                    // Advance buffer the remainder of the row stride, unless on the last row.
                    // Otherwise, this will throw an IllegalArgumentException because the buffer
                    // doesn't include the last padding.
                    if (h - row != 1) {
                        buffer.position(buffer.position() + rowStride - length);
                    }
                    offset += length;
                } else {

                    // On the last row only read the width of the image minus the pixel stride
                    // plus one. Otherwise, this will throw a BufferUnderflowException because the
                    // buffer doesn't include the last padding.
                    if (h - row == 1) {
                        buffer.get(rowData, 0, width - pixelStride + 1);
                    } else {
                        buffer.get(rowData, 0, rowStride);
                    }

                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }
        }

        // Finally, create the Mat.
        Mat mat = new Mat(height + height / 2, width, CvType.CV_8UC1);
        mat.put(0, 0, data);

        return mat;
    }

    /**
     * Translate a CameraAccessException into a CameraException and throw it
     * @param e exception
     */
    static
    private CameraException CameraAccessException2CameraException (Context c,
                                                                   CameraAccessException e) {
        switch (e.getReason()) {
            case CameraAccessException.CAMERA_DISABLED:
                Log.e(TAG, "The device's cameras have been disabled for this user");
                return new CameraException(CameraException.CAMERA_DISABLED,
                        c.getResources().getString(R.string.service_camera_disabled_error));
            case CameraAccessException.CAMERA_DISCONNECTED:
                Log.e(TAG, "The camera device is no longer available");
                return new CameraException(CameraException.CAMERA_ERROR,
                        c.getString(R.string.camera_no_longer_available));
            case CameraAccessException.CAMERA_ERROR:
                Log.e(TAG, "The camera device is currently in the error state");
                return new CameraException(CameraException.CAMERA_ERROR,
                        c.getResources().getString(R.string.service_camera_error), e);
            case CameraAccessException.CAMERA_IN_USE:
                return new CameraException(CameraException.CAMERA_IN_USE,
                        c.getResources().getString(R.string.service_camera_no_access));
            case CameraAccessException.MAX_CAMERAS_IN_USE:
                return new CameraException(CameraException.CAMERA_IN_USE,
                        c.getString(R.string.max_cameras_in_use));
        }
        return new CameraException(CameraException.CAMERA_ERROR,
                c.getResources().getString(R.string.service_camera_error), e);
    }

    /**
     * Start background thread used for async notifications, including capturing frames
     */
    private void startCameraThread() {
        Log.i(TAG, "start CameraThread");
        stopCameraThread();
        mBackgroundThread = new HandlerThread("CameraThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stop background thread
     */
    private void stopCameraThread() {
        if(mBackgroundThread == null) return;

        Log.i(TAG, "stop CameraThread");
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "stop CameraThread");
        }
    }
}
