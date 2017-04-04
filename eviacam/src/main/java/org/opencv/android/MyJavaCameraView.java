package org.opencv.android;

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Build;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;

import com.crea_si.eviacam.R;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * This class is an implementation of the Bridge View between OpenCV and Java Camera.
 * This class relays on the functionality available in base class and only implements
 * required functions:
 * connectCamera - opens Java camera and sets the PreviewCallback to be delivered.
 * disconnectCamera - closes the camera and stops preview.
 * When frame is delivered via callback from Camera - it processed via OpenCV to be
 * converted to RGBA32 and then passed to the external callback for modifications if required.
 */
public class MyJavaCameraView extends MyCameraBridgeViewBase implements PreviewCallback {

    private static final int MAGIC_TEXTURE_ID = 10;
    private static final String TAG = "MyJavaCameraView";

    private byte mBuffer[];
    private Mat[] mFrameChain;
    private int mChainIdx = 0;
    private Thread mThread;
    private boolean mStopThread;
    private boolean mWhiteBalanceLockTried= false;

    protected Camera mCamera;
    protected JavaCameraFrame[] mCameraFrame;
    private SurfaceTexture mSurfaceTexture;

    public static class JavaCameraSizeAccessor implements ListItemAccessor {

        @Override
        public int getWidth(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.width;
        }

        @Override
        public int getHeight(Object obj) {
            Camera.Size size = (Camera.Size) obj;
            return size.height;
        }
    }

    public MyJavaCameraView(Context context, int cameraId) {
        super(context, cameraId);
    }

    public MyJavaCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void initializeCamera(int width, int height) throws CameraException {
        Log.d(TAG, "Initialize java camera");

        synchronized (this) {
            mCamera = null;

            if (mCameraIndex == CAMERA_ID_ANY) {
                Log.d(TAG, "Trying to open camera with old open()");
                try {
                    mCamera = Camera.open();
                }
                catch (Exception e){
                    Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getLocalizedMessage());
                }

                if(mCamera == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    boolean connected = false;
                    for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(camIdx) + ")");
                        try {
                            mCamera = Camera.open(camIdx);
                            connected = true;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + camIdx + "failed to open: " + e.getLocalizedMessage());
                        }
                        if (connected) break;
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    int localCameraIndex = mCameraIndex;
                    if (mCameraIndex == CAMERA_ID_BACK) {
                        Log.i(TAG, "Trying to open back camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo( camIdx, cameraInfo );
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    } else if (mCameraIndex == CAMERA_ID_FRONT) {
                        Log.i(TAG, "Trying to open front camera");
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        for (int camIdx = 0; camIdx < Camera.getNumberOfCameras(); ++camIdx) {
                            Camera.getCameraInfo( camIdx, cameraInfo );
                            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                localCameraIndex = camIdx;
                                break;
                            }
                        }
                    }
                    if (localCameraIndex == CAMERA_ID_BACK) {
                        Log.e(TAG, "Back camera not found!");
                        throw new CameraException(CameraException.CAMERA_ERROR, "Back camera not found!");
                    } else if (localCameraIndex == CAMERA_ID_FRONT) {
                        Log.e(TAG, "Front camera not found!");
                        throw new CameraException(CameraException.CAMERA_ERROR, "Front camera not found!");
                    } else {
                        Log.d(TAG, "Trying to open camera with new open(" + Integer.valueOf(localCameraIndex) + ")");
                        try {
                            mCamera = Camera.open(localCameraIndex);
                        } catch (RuntimeException e) {
                            Log.e(TAG, "Camera #" + localCameraIndex + "failed to open: " + e.getLocalizedMessage());
                        }
                    }
                }
            }

            if (mCamera == null) {
                /* Check if the camera is disabled */
                DevicePolicyManager dpm = (DevicePolicyManager)
                        getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
                if (dpm.getCameraDisabled(null)) {
                    Log.e(TAG, "The device's cameras have been disabled for this user");
                    throw new CameraException(CameraException.CAMERA_DISABLED,
                            getResources().getString(R.string.service_camera_disabled_error));
                }

                /* Otherwise the camera is already in use */
                Log.e(TAG, "Camera already in use");
                throw new CameraException(CameraException.CAMERA_IN_USE,
                        getResources().getString(R.string.service_camera_no_access));
            }

            /* Now set camera parameters */
            Camera.Parameters params = mCamera.getParameters();
            Log.d(TAG, params.flatten());

            List<android.hardware.Camera.Size> sizes = params.getSupportedPreviewSizes();

            if (sizes != null && sizes.size()> 0) {
                /* Select the size that fits surface considering maximum size allowed */
                Size frameSize = calculateBestCameraFrameSize(sizes, new JavaCameraSizeAccessor(), mMaxWidth, mMaxHeight);

                Log.d(TAG, "Set preview size to " + Integer.valueOf((int)frameSize.width) +
                        "x" + Integer.valueOf((int)frameSize.height));

                params.setPreviewFormat(ImageFormat.NV21);
                params.setPreviewSize((int)frameSize.width, (int)frameSize.height);
                mCamera.setParameters(params);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                        !android.os.Build.MODEL.equals("GT-I9100")) {
                    params.setRecordingHint(true);
                    mCamera.setParameters(params);
                }

                List<String> FocusModes = params.getSupportedFocusModes();
                if (FocusModes != null &&
                        FocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    mCamera.setParameters(params);
                }

                /*
                 * Disable stabilization to save some CPU cycles
                 */
                if (params.isVideoStabilizationSupported()) {
                    params.setVideoStabilization(false);
                    mCamera.setParameters(params);
                }

                /*
                 * Tries to set a frame rate higher or equal than 15 fps.
                 *
                 * getSupportedPreviewFpsRange always returns a list of supported preview fps ranges with
                 * at least one element. Every element is an int array of two values - minimum fps and
                 * maximum fps. The list is sorted from small to large (first by maximum fps and then
                 * minimum fps).
                 *
                 * Nexus 7: the list has only one element (4000,60000)
                 * Samsung Galaxy Nexus: (15000,15000),(15000,30000),(24000,30000)
                 */
                List<int[]> ranges= params.getSupportedPreviewFpsRange ();
                Log.d(TAG, ranges.toString());

                int winner= ranges.size()-1;
                int maxLimit= ranges.get(ranges.size()-1)[1];

                for (int i= ranges.size()-2; i>= 0; i--) {
                    if (ranges.get(i)[1]!= maxLimit || ranges.get(i)[0]< 15000) {
                        break;
                    }
                    winner= i;
                }
                params.setPreviewFpsRange(ranges.get(winner)[0], ranges.get(winner)[1]);

                mCamera.setParameters(params);
                params = mCamera.getParameters();

                mFrameWidth = params.getPreviewSize().width;
                mFrameHeight = params.getPreviewSize().height;

                if ((getLayoutParams().width == LayoutParams.MATCH_PARENT) && (getLayoutParams().height == LayoutParams.MATCH_PARENT) ||
                     width != mFrameWidth || height != mFrameHeight) {
                    mScale = Math.min(((float) height) / mFrameHeight, ((float) width) / mFrameWidth);
                }
                else {
                    mScale = 0;
                }

                if (mFpsMeter != null) {
                    mFpsMeter.setResolution(mFrameWidth, mFrameHeight);
                }

                int size = mFrameWidth * mFrameHeight;
                size  = size * ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8;
                mBuffer = new byte[size];

                mCamera.addCallbackBuffer(mBuffer);
                mCamera.setPreviewCallbackWithBuffer(this);

                mFrameChain = new Mat[2];
                mFrameChain[0] = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);
                mFrameChain[1] = new Mat(mFrameHeight + (mFrameHeight/2), mFrameWidth, CvType.CV_8UC1);

                AllocateCache();

                mCameraFrame = new JavaCameraFrame[2];
                mCameraFrame[0] = new JavaCameraFrame(mFrameChain[0], mFrameWidth, mFrameHeight);
                mCameraFrame[1] = new JavaCameraFrame(mFrameChain[1], mFrameWidth, mFrameHeight);

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        mSurfaceTexture = new SurfaceTexture(MAGIC_TEXTURE_ID);
                        mCamera.setPreviewTexture(mSurfaceTexture);
                    } else
                        mCamera.setPreviewDisplay(null);
                }
                catch (java.io.IOException e) {
                    Log.e(TAG, "setPreviewTexture failed with an IOException");
                    throw new CameraException(CameraException.CAMERA_ERROR,
                            "IO error while settings camera parameters", e);
                }

                /* Finally we are ready to start the preview */
                Log.d(TAG, "startPreview");
                try {
                    mCamera.startPreview();
                }
                catch (RuntimeException e) {
                    Log.e(TAG, "startPreview failed with a RuntimeException");
                    throw new CameraException(CameraException.CAMERA_ERROR,
                            getResources().getString(R.string.service_camera_error), e);
                }
            }
            else {
                Log.e(TAG, "Cannot retrieve sizes");
                throw new CameraException(CameraException.CAMERA_ERROR,
                        "Camera error: cannot retrieve sizes");
            }
        }
    }

    protected void releaseCamera() {
        synchronized (this) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);

                mCamera.release();
            }
            mCamera = null;
            if (mFrameChain != null) {
                mFrameChain[0].release();
                mFrameChain[1].release();
            }
            if (mCameraFrame != null) {
                mCameraFrame[0].release();
                mCameraFrame[1].release();
            }
        }
    }

    private boolean mCameraFrameReady = false;

    @Override
    protected void connectCamera(int width, int height) throws CameraException {

        /* 1. We need to instantiate camera
         * 2. We need to start thread which will be getting frames
         */
        /* First step - initialize camera connection */
        Log.d(TAG, "Connecting to camera");
        initializeCamera(width, height);

        mCameraFrameReady = false;

        /* now we can start update thread */
        Log.d(TAG, "Starting processing thread");
        mStopThread = false;
        mThread = new Thread(new CameraWorker());
        mThread.start();
    }

    @Override
    protected void disconnectCamera() {
        /* 1. We need to stop thread which updating the frames
         * 2. Stop camera and release it
         */
        Log.d(TAG, "Disconnecting from camera");
        try {
            mStopThread = true;
            Log.d(TAG, "Notify thread");
            synchronized (this) {
                this.notify();
            }
            Log.d(TAG, "Waiting for thread");
            if (mThread != null)
                mThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThread =  null;
        }

        /* Now release camera */
        releaseCamera();

        mCameraFrameReady = false;
    }

    @Override
    public void onPreviewFrame(byte[] frame, Camera arg1) {
        synchronized (this) {
            mFrameChain[mChainIdx].put(0, 0, frame);
            mCameraFrameReady = true;
            this.notify();
            /*
             * Disable auto white balance to save some CPU cycles
             * Do here to allow to run the white balance algorithm once
             */
            if (!mWhiteBalanceLockTried && mCamera != null) {
                Camera.Parameters params = mCamera.getParameters();
                if (params.isAutoWhiteBalanceLockSupported()) {
                    params.setAutoWhiteBalanceLock(true);
                }
                mWhiteBalanceLockTried= true;
            }
        }
        if (mCamera != null)
            mCamera.addCallbackBuffer(mBuffer);
    }

    private class JavaCameraFrame implements CvCameraViewFrame {
        @Override
        public Mat gray() {
            return mYuvFrameData.submat(0, mHeight, 0, mWidth);
        }

        @Override
        public Mat rgba() {
            Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            return mRgba;
        }

        public JavaCameraFrame(Mat Yuv420sp, int width, int height) {
            super();
            mWidth = width;
            mHeight = height;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        public void release() {
            mRgba.release();
        }

        private Mat mYuvFrameData;
        private Mat mRgba;
        private int mWidth;
        private int mHeight;
    };

    private class CameraWorker implements Runnable {

        @Override
        public void run() {

            // Raise priority to improve responsiveness
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);

            do {
                boolean hasFrame = false;
                synchronized (MyJavaCameraView.this) {
                    try {
                        while (!mCameraFrameReady && !mStopThread) {
                            MyJavaCameraView.this.wait();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mCameraFrameReady)
                    {
                        mChainIdx = 1 - mChainIdx;
                        mCameraFrameReady = false;
                        hasFrame = true;
                    }
                }

                if (!mStopThread && hasFrame) {
                    if (!mFrameChain[1 - mChainIdx].empty())
                        deliverAndDrawFrame(mCameraFrame[1 - mChainIdx]);
                }
            } while (!mStopThread);
            Log.d(TAG, "Finish processing thread");
        }
    }
}
