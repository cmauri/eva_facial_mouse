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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.acra.ACRA;
import org.opencv.android.CameraException;
import org.opencv.android.MyCameraBridgeViewBase;
import org.opencv.android.MyCameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.MyJavaCameraView;
import org.opencv.android.MyCameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
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
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.RelativeLayout;

import com.android.ex.camera2.blocking.BlockingCameraManager;
import com.android.ex.camera2.blocking.BlockingSessionCallback;
import com.android.ex.camera2.blocking.BlockingStateCallback;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;
import com.crea_si.eviacam.common.EVIACAM;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.common.VisionPipeline;
import com.crea_si.eviacam.util.FlipDirection;

/**
 * Provides a simple camera interface for initializing, starting and stopping it.
 */

public class CameraListener implements CvCameraViewListener2 {
    private final Context mContext;
    
    // callback to process frames
    private final FrameProcessor mFrameProcessor;
    
    // OpenCV capture&view facility
    private MyCameraBridgeViewBase mCameraView;
    public SurfaceView getCameraSurface(){
        return mCameraView;
    }

    /*
       Physical mounting rotation of the camera (i.e. whether the frame needs a flip
       operation). For instance, this is needed for those devices with rotating
       camera such as the Lenovo YT3-X50L)
     */
    private FlipDirection mCameraFlip= FlipDirection.NONE;
    public FlipDirection getCameraFlip() { return mCameraFlip; }

    // physical orientation of the camera (0, 90, 180, 270)
    private int mCameraOrientation;
    public int getCameraOrientation() { return mCameraOrientation; }

    // captured frames count
    private int mCapuredFrames;

    /** 
     * Load a resource into a temporary file
     * 
     * @param c - context
     * @param rid - resource id
     * @param suffix - extension of the temporary file
     * @return a File object representing the temporary file
     * @throws IOException when failed
     */
    private static File resourceToTempFile (Context c, int rid, String suffix) 
            throws IOException {
        InputStream is;
        OutputStream os= null;
        File outFile = null;
        
        is= c.getResources().openRawResource(rid);
        try {
            outFile = File.createTempFile("tmp", suffix, c.getCacheDir());
            os= new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            if (outFile != null) {
                //noinspection ResultOfMethodCallIgnored
                outFile.delete();
                outFile= null;
            }
            throw e;
        }
        finally {
            try {
                if (is != null) is.close();
            }
            catch (IOException e) {
                // Do nothing
            }
            try {
                if (os != null) os.close();
            }
            catch (IOException e) {
                // Do nothing
            }
        }

        return outFile;
    }

    /**
     * Constructor
     * @param c context
     * @param fp object that will receive the camera callbacks
     */
    public CameraListener(@NonNull Context c, @NonNull FrameProcessor fp) throws CameraException {
        mContext= c;
        mFrameProcessor= fp;
        //initLegacyCamera();
        initCamera2();
    }

    private void initLegacyCamera() throws CameraException {
        /*
         * For some devices, notably the Lenovo YT3-X50L, have only one camera that can
         * be rotated to frame the user's face. In this case the camera is reported as
         * facing back. Therefore, we try to detect all cameras of the device and pick
         * the facing front one, if any. Otherwise, we pick the first facing back camera
         * and report that the image needs a vertical flip before fixing the orientation.
         *
         * The orientation of the camera is the angle that the camera image needs
         * to be rotated clockwise so it shows correctly on the display in its natural orientation.
         * It should be 0, 90, 180, or 270.
         *
         * For example, suppose a device has a naturally tall screen. The back-facing camera sensor
         * is mounted in landscape. You are looking at the screen. If the top side of the camera
         * sensor is aligned with the right edge of the screen in natural orientation, the value
         * should be 90. If the top side of a front-facing camera sensor is aligned with the right
         * of the screen, the value should be 270.
         */
        final int numCameras= Camera.getNumberOfCameras();
        if (numCameras< 1) {
            Log.e(EVIACAM.TAG, "No cameras available");
            throw new CameraException(CameraException.NO_CAMERAS_AVAILABLE,
                    mContext.getResources().getString(R.string.service_camera_no_available));
        }

        // Pick the best available camera
        int bestCamera= 0;  // pick the first one if no facing front camera available
        Camera.CameraInfo cameraInfo= new CameraInfo();
        for (int i= 0; i< numCameras; i++) {
            Camera.getCameraInfo (i, cameraInfo);
            if (cameraInfo.facing== CameraInfo.CAMERA_FACING_FRONT) {
                bestCamera= i;
                break;
            }
        }

        // Get camera features
        Camera.getCameraInfo (bestCamera, cameraInfo);
        FlipDirection flip= FlipDirection.NONE;                // no flip needed by default
        int cameraId = MyCameraBridgeViewBase.CAMERA_ID_FRONT; // front camera by default

        if (cameraInfo.facing== CameraInfo.CAMERA_FACING_BACK) {
            // When the best available camera faces back
            flip= FlipDirection.VERTICAL;
            cameraId= MyCameraBridgeViewBase.CAMERA_ID_BACK;

            Log.i(EVIACAM.TAG, "Back camera detected. Orientation: " + cameraInfo.orientation);
        }
        else {
            Log.i(EVIACAM.TAG, "Front camera detected. Orientation: " + cameraInfo.orientation);
        }

        mCameraOrientation= cameraInfo.orientation;
        mCameraFlip= flip;

        /*
         * Create a capture view which carries the responsibilities of
         * capturing and displaying frames.
         */
        mCameraView= new MyJavaCameraView(mContext, cameraId);

        mCameraView.setCvCameraViewListener(this);

        // We first attempted to work at 320x240, but for some devices such as the
        // Galaxy Nexus crashes with a "Callback buffer was too small!" error.
        // However, at 352x288 works for all devices tried so far.
        mCameraView.setMaxFrameSize(352, 288);

        //mCameraView.enableFpsMeter();  // remove comment for testing

        mCameraView.setVisibility(SurfaceView.VISIBLE);
    }

    public void startCamera() {
        /*
          In previous versions we used the OpenCV async helper, but we found
          problems with devices running Android arm64 (e.g. Huawei P8) due
          to missing OpenCV libraries. To avoid such problems we included the
          OpenCV binaries in the App apk
         */
        if (!OpenCVLoader.initDebug()) {
            throw new RuntimeException("Cannot initialize OpenCV");
        }

        Log.i(EVIACAM.TAG, "OpenCV loaded successfully");

        // initialize JNI part
        System.loadLibrary("visionpipeline");

        /* Load haarcascade from resources */
        try {
            File f= resourceToTempFile (mContext, R.raw.haarcascade, "xml");
            VisionPipeline.init(f.getAbsolutePath());
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
        catch (IOException e) {
            Log.e(EVIACAM.TAG, "Cannot write haarcascade temp file. Continuing anyway");
        }

        // start camera capture
        try {
            mCameraView.enableView();
        }
        catch(Exception error) {
            mFrameProcessor.onCameraError(error);
        }
    }
    
    public void stopCamera() {
        try {
            mCameraView.disableView();
        }
        catch (Exception error) {
            // Ignore errors when stopping camera
            Log.e(EVIACAM.TAG, error.getLocalizedMessage());
            ACRA.getErrorReporter().handleSilentException(error);
        }
    }

    /**
     * Sets the flip operation to perform to the frame before is applied a rotation
     *
     * @param flip FlipDirection.NONE, FlipDirection.VERTICAL or FlipDirection.HORIZONTAL
     */
    public void setPreviewFlip(FlipDirection flip) {
        switch (flip) {
            case NONE:
                mCameraView.setPreviewFlip(MyCameraBridgeViewBase.FlipDirection.NONE);
                break;
            case VERTICAL:
                mCameraView.setPreviewFlip(MyCameraBridgeViewBase.FlipDirection.VERTICAL);
                break;
            case HORIZONTAL:
                mCameraView.setPreviewFlip(MyCameraBridgeViewBase.FlipDirection.HORIZONTAL);
                break;
        }
    }

    /**
     * Sets the rotation to perform to the camera image before is displayed
     * in the preview surface
     *
     * @param rotation rotation to perform (clockwise) in degrees
     *                 legal values: 0, 90, 180, or 270
     */
    public void setPreviewRotation (int rotation) {
        mCameraView.setPreviewRotation(rotation);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(EVIACAM.TAG, "onCameraViewStarted");
        mFrameProcessor.onCameraStarted();
    }

    @Override
    public void onCameraViewStopped() {
        Log.i(EVIACAM.TAG, "onCameraViewStopped. Frame count:" + mCapuredFrames);
        
        // finish JNI part
        VisionPipeline.cleanup();

        mFrameProcessor.onCameraStopped();
    }

    @Override
    public void onCameraViewError(@NonNull Throwable error) {
        mFrameProcessor.onCameraError(error);
    }

    /**
     * Called each time new frame is captured
     */
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        /* Informative log for debug purposes */
        mCapuredFrames++;
        if (mCapuredFrames< 100) {
            if ((mCapuredFrames % 10) == 0) {
                Log.i(EVIACAM.TAG, "onCameraFrame. Frame count:" + mCapuredFrames);
            }
        }

        Mat rgba = inputFrame.rgba();
        
        mFrameProcessor.processFrame(rgba);
        
        return rgba;
    }

    /**
     * Enable or disable camera viewer refresh to save CPU cycles
     * @param v true to enable update, false to disable
     */
    @SuppressWarnings("unused")
    public void setUpdateViewer(boolean v) {
        if (mCameraView!= null) mCameraView.setUpdateViewer(v);
    }








    private CameraDevice mCameraDevice;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Semaphore mCameraLock = new Semaphore(1);
    private CameraCaptureSession mCaptureSession;

    /**
     * Initialize camera using camera2 API
     * @throws CameraException when error
     */
    @TargetApi(21)
    private void initCamera2() throws CameraException {
        /* Get camera manager */
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) {
            Log.e(EVIACAM.TAG, "Cannot obtain camera manager");
            throw new CameraException(CameraException.NO_CAMERAS_AVAILABLE,
                    mContext.getResources().getString(R.string.service_camera_no_available));
        }

        /* Get available cameras */
        String[] cameraIdList;
        try {
            cameraIdList= manager.getCameraIdList();
        } catch (CameraAccessException e) {
            Log.e(EVIACAM.TAG, "Cannot query camera id list");
            throw CameraAccessException2CameraException(e);
        }

        if (cameraIdList.length< 1) {
            Log.e(EVIACAM.TAG, "No cameras available");
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
                Log.e(EVIACAM.TAG, "Cannot get camera characteristics: " + cameraIdList[i]);
                continue;
            }

            Integer lensFacing= cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing== null) {
                /* If fails to retrieve lens facing assume external camera */
                Log.i(EVIACAM.TAG, "Cannot retrieve lens facing for camera: " + cameraIdList[i]);
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
        Log.i(EVIACAM.TAG, "Try front camera");
        int bestCamera= frontCameraIdx;
        if (bestCamera== -1) {
            Log.i(EVIACAM.TAG, "Try external camera");
            bestCamera= externalCameraIdx;
        }
        if (bestCamera== -1) {
            Log.i(EVIACAM.TAG, "Try back camera");
            bestCamera= backCameraIdx;
            mCameraFlip= FlipDirection.VERTICAL;
        }
        if (bestCamera== -1) {
            Log.e(EVIACAM.TAG, "None of the cameras is suitable for the job. Aborting.");
            throw new CameraException(CameraException.CAMERA_ERROR,
                    mContext.getResources().getString(R.string.service_camera_error));
        }
        String cameraId= cameraIdList[bestCamera];


        CameraCharacteristics cameraCharacteristics;
        try {
            cameraCharacteristics= manager.getCameraCharacteristics(cameraId);
        } catch (CameraAccessException e) {
            Log.e(EVIACAM.TAG, "Cannot get camera characteristics: " + cameraId);
            throw CameraAccessException2CameraException(e);
        }

        /*
         * The orientation of the camera is the angle that the camera image needs
         * to be rotated clockwise so it shows correctly on the display in its natural orientation.
         * It should be 0, 90, 180, or 270.
         */

        Integer cameraOrientation=
                cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if (cameraOrientation== null) {
            Log.e(EVIACAM.TAG, "Cannot get camera orientation");
            throw new CameraException(CameraException.CAMERA_ERROR,
                    mContext.getResources().getString(R.string.service_camera_error));
        }
        else {
            mCameraOrientation = cameraOrientation;
            Log.i(EVIACAM.TAG, "Camera orientation: " + mCameraOrientation);
        }

        StreamConfigurationMap map = cameraCharacteristics.
                get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        Log.i(EVIACAM.TAG, "Preview sizes");
        Size[] outputSizes= null;
        if (map!= null) {
            // TODO: perhaps use ImageFormat.YUV_420_888
            outputSizes = map.getOutputSizes(ImageFormat.YUV_420_888); //SurfaceTexture.class);
        }
        if (outputSizes!= null) {
            for (Size psize : outputSizes) {
                Long stallDuration = map.getOutputStallDuration(SurfaceTexture.class, psize);
                Log.i(EVIACAM.TAG, "(" + psize.getWidth() + ", " + psize.getHeight() + ") Stall: " + stallDuration);
            }
        }




        /*
        initSurfaceTexture();
        openCamera(mCameraIndex);
        mIsStarted = true;
        if(mCameraWidth>0 && mCameraHeight>0)
            setPreviewSize(mCameraWidth, mCameraHeight); // start preview and call listener.onCameraViewStarted()
        */
        // Start background thread
        startCameraThread();

        /* Open camera */
        /*
        try {
            if (!mCameraLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(cameraId, mStateCallback, mBackgroundHandler);

        } catch (InterruptedException e) {
            // TODO
        } catch (SecurityException e) {
            // TODO
        } catch (CameraAccessException e) {
            // TODO
            //showToast("Failed to open camera."); // failed immediately.
        }
        */

        try {
            CameraDevice.StateCallback callback= new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    // Nothing to do. Device returned by the openCamera call
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    mCameraDevice = null;
                    // TODO: handle this
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    camera.close();
                    mCameraDevice = null;
                    // TODO: handle this
                }
            };
            BlockingStateCallback blockingCallback = new BlockingStateCallback(callback);

            BlockingCameraManager blockingManager = new BlockingCameraManager(manager);
            mCameraDevice = blockingManager.openCamera(cameraId, blockingCallback, mBackgroundHandler);
        } catch (BlockingCameraManager.BlockingOpenException |TimeoutRuntimeException e) {
            e.printStackTrace();
            // TODO: handle this
            //showToast("Timed out opening camera.");
        } catch (CameraAccessException e) {
            e.printStackTrace();
            // TODO: handle this
            //showToast("Failed to open camera."); // failed immediately.
        }


            /*
            SurfaceTexture texture = mSurfaceView.getSurfaceTexture();
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            mSurface = new Surface(texture);

            Log.d(TAG, "bob createCameraPreviewSession ========================");
            */

            // TODO: calculate best preview size
        try {
            ImageReader imageReader;
            imageReader = ImageReader.newInstance(352, 288, ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);




            // We set up a CaptureRequest.Builder with the output Surface.
            CaptureRequest.Builder previewRequestBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(imageReader.getSurface());

            List<Surface> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(imageReader.getSurface());

            BlockingSessionCallback sessionCallback = new BlockingSessionCallback();
            mCameraDevice.createCaptureSession(outputSurfaces, sessionCallback, mBackgroundHandler);

            try {
                Log.d(EVIACAM.TAG, "Waiting on session.");
                mCaptureSession = sessionCallback.waitAndGetSession(2500);
                try {
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                    // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                    // Comment out the above and uncomment this to disable continuous autofocus and
                    // instead set it to a fixed value of 20 diopters. This should make the picture
                    // nice and blurry for denoised edge detection.
                    // mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    //		   CaptureRequest.CONTROL_AF_MODE_OFF);
                    // mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, 20.0f);
                    // Finally, we start displaying the camera preview.

                    mCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                            mCaptureCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            } catch (TimeoutRuntimeException e) {
                //showToast("Failed to configure capture session.");
                e.printStackTrace();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }




//        mCameraDevice.createCaptureSession(outputSurfaces, sessionCallback, mBackgroundHandler);

        //mImageReader.get().setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);


        /*
        // set layout and add to parent
        // TODO:
        SurfaceView surfaceView= new SurfaceView(mContext);
        RelativeLayout.LayoutParams lp=
                new RelativeLayout.LayoutParams(80, 60);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        surfaceView.setLayoutParams(lp);

        SurfaceTexture texture= surfaceView
        */



        // STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES
        // CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES
        // CONTROL_AE_TARGET_FPS_RANGE

        /*
         * Create a capture view which carries the responsibilities of
         * capturing and displaying frames.
         */
        /*
        mCameraView= new MyJavaCameraView(mContext, cameraId);

        mCameraView.setCvCameraViewListener(this);

        // We first attempted to work at 320x240, but for some devices such as the
        // Galaxy Nexus crashes with a "Callback buffer was too small!" error.
        // However, at 352x288 works for all devices tried so far.
        mCameraView.setMaxFrameSize(352, 288);

        //mCameraView.enableFpsMeter();  // remove comment for testing

        mCameraView.setVisibility(SurfaceView.VISIBLE);
        */
    }


    // TODO: perhaps need to use BlockingCaptureCallback
    private final CameraCaptureSession.CaptureCallback mCaptureCallback= new CaptureCallback();
    @TargetApi(21)
    private class CaptureCallback extends CameraCaptureSession.CaptureCallback {
        //@Override
        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                     long timestamp, long frameNumber) {
            Log.d(EVIACAM.TAG, "onCaptureStarted");

        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            // bob
            Log.d(EVIACAM.TAG, "onCaptureCompleted");
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                    @NonNull CaptureFailure failure) {
            // bob
            Log.d(EVIACAM.TAG, "onCaptureFailed");
            //showToast("Capture failed!");
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener= new OnImageAvailableListener();

    @TargetApi(21)
    private class OnImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(EVIACAM.TAG,"onImageAvailable");
            Image image;
            //String result;

            try {
                image = reader.acquireLatestImage();
                if(image == null) {
                    return;
                }
                int fmt = reader.getImageFormat();
                Log.d(EVIACAM.TAG,"Image fmt:" + fmt);

                // TODO: JNI call to process image

                //JNIUtils.blitraw(image, mSurface);
            } catch (IllegalStateException e) {
                Log.e(EVIACAM.TAG, "Too many images queued for saving, dropping image for request: ");
//                        entry.getKey());
//                pendingQueue.remove(entry.getKey());
                return;
            }
            image.close();
        }
    }


    /**
     * Translate a CameraAccessException into a CameraException and throw it
     * @param e exception
     */
    @TargetApi(21)
    private CameraException CameraAccessException2CameraException (CameraAccessException e) {
        switch (e.getReason()) {
            case CameraAccessException.CAMERA_DISABLED:
                Log.e(EVIACAM.TAG, "The device's cameras have been disabled for this user");
                return new CameraException(CameraException.CAMERA_DISABLED,
                        mContext.getResources().getString(R.string.service_camera_disabled_error));
            case CameraAccessException.CAMERA_DISCONNECTED:
                Log.e(EVIACAM.TAG, "The camera device is no longer available");
                return new CameraException(CameraException.CAMERA_ERROR,
                        mContext.getString(R.string.camera_no_longer_available));
            case CameraAccessException.CAMERA_ERROR:
                Log.e(EVIACAM.TAG, "The camera device is currently in the error state");
                return new CameraException(CameraException.CAMERA_ERROR,
                        mContext.getResources().getString(R.string.service_camera_error), e);
            case CameraAccessException.CAMERA_IN_USE:
                return new CameraException(CameraException.CAMERA_IN_USE,
                        mContext.getResources().getString(R.string.service_camera_no_access));
            case CameraAccessException.MAX_CAMERAS_IN_USE:
                return new CameraException(CameraException.CAMERA_IN_USE,
                        mContext.getString(R.string.max_cameras_in_use));
        }
        return new CameraException(CameraException.CAMERA_ERROR,
                mContext.getResources().getString(R.string.service_camera_error), e);
    }

    private void startCameraThread() {
        Log.i(EVIACAM.TAG, "start CameraThread");
        stopCameraThread();
        mBackgroundThread = new HandlerThread("CameraThread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    @TargetApi(21)
    private void stopCameraThread() {
        if(mBackgroundThread == null) return;

        Log.i(EVIACAM.TAG, "stop CameraThread");
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(EVIACAM.TAG, "stop CameraThread");
        }
    }

    /*
    private final CameraDevice.StateCallback mStateCallback = new CameraCallback();

    @TargetApi(21)
    private class CameraCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            mCameraLock.release();
            //createCameraPreviewSession();
            // TODO: finish initialization
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDevice.close();
            mCameraDevice = null;
            mCameraLock.release();
            // TODO: manage state
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDevice.close();
            mCameraDevice = null;
            mCameraLock.release();
            // TODO: report error
        }
    }
    */
}
