package com.crea_si.eviacam.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.MyJavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.SurfaceView;


@SuppressWarnings("deprecation")
public class CameraListener implements CvCameraViewListener2 {

    Context mContext;
    
    // callback to process frames
    FrameProcessor mFrameProcessor;
    
    // opencv capture&view facility 
    private CameraBridgeViewBase mCameraView;
    
    // physical orientation of the camera (0, 90, 180, 270)
    final private int mCameraOrientation;
   
    // callback for camera initialization
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(
            EViacamService.getInstance().getApplicationContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    EVIACAM.debug("OpenCV loaded successfully");
                    
                    // initialize JNI part
                    System.loadLibrary("visionpipeline");
                    
                    /** Load haarcascade from resources */
                    try {
                        File f= resourceToTempFile (mContext, R.raw.haarcascade, "xml");
                        VisionPipeline.init(f.getAbsolutePath());
                        f.delete();
                    }
                    catch (IOException e) {
                        EVIACAM.debug("Cannot write haarcascade temp file. Continuing anyway");
                    }

                    // start camera capture
                    mCameraView.enableView();
                } break;
                default:
                {
                    // TODO: manage errors
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    /** 
     * Load a resource into a temporary file
     * 
     * @param c - context
     * @param rid - resource id
     * @param suffix - extension of the temporary file
     * @return a File object representing the temporary file
     * @throws IOException
     */
    private static File resourceToTempFile (Context c, int rid, String suffix) 
            throws IOException {
        InputStream is= null;
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
                outFile.delete();
                outFile= null;
            }
            throw e;
        }
        finally {
            try {
                if (is != null) is.close();
                if (os != null) os.close();
            } catch (IOException e) {
                throw e;
            }
        }

        return outFile;
    }

    public CameraListener(Context c, FrameProcessor fp) {
        mContext= c;
        mFrameProcessor= fp;

        /**
         * The orientation of the camera image. The value is the angle that the camera image needs 
         * to be rotated clockwise so it shows correctly on the display in its natural orientation. 
         * It should be 0, 90, 180, or 270.
         * 
         * For example, suppose a device has a naturally tall screen. The back-facing camera sensor 
         * is mounted in landscape. You are looking at the screen. If the top side of the camera 
         * sensor is aligned with the right edge of the screen in natural orientation, the value 
         * should be 90. If the top side of a front-facing camera sensor is aligned with the right 
         * of the screen, the value should be 270.
         * 
         */
        // TODO: display error when no front camera detected
        int cameraOrientation= 0;
        Camera.CameraInfo cameraInfo= new CameraInfo();
        for (int i= 0; i<  Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo (i, cameraInfo);
            if (cameraInfo.facing== CameraInfo.CAMERA_FACING_FRONT) {
                cameraOrientation= cameraInfo.orientation;
                EVIACAM.debug("Detected front camera. Orientation: " + cameraOrientation);
            }
        }
        mCameraOrientation= cameraOrientation;
    
        // create capture view
        mCameraView= new MyJavaCameraView(mContext, CameraBridgeViewBase.CAMERA_ID_FRONT);
        
        // set CameraBridgeViewBase parameters        
        // TODO: Damn! It seems that for certain resolutions (for instance 320x240 on a Galaxy Nexus)
        // crashes with a "Callback buffer was too small! error", it works at 352x288
        
        mCameraView.setMaxFrameSize(352, 288);
        //mCameraView.enableFpsMeter(); // For testing
        mCameraView.setCvCameraViewListener(this);
        
        // Set View parameters
        mCameraView.setVisibility(SurfaceView.VISIBLE);
    }
    
    public void startCamera() {
        // Start OpenCV
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, mContext, mLoaderCallback);
    }
    
    public void stopCamera() {
        mCameraView.disableView();
    }

    SurfaceView getCameraSurface(){
        return mCameraView;
    }

    int getCameraOrientation() {
        return mCameraOrientation;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        EVIACAM.debug("onCameraViewStarted");
    }

    @Override
    public void onCameraViewStopped() {
        EVIACAM.debug("onCameraViewStopped");
        
        // finish JNI part
        VisionPipeline.cleanup();
    }
     
    /*
     * called each time new frame is grabbed 
     * 
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
     */
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        
        mFrameProcessor.processFrame(rgba);
        
        return rgba;
    }
}
