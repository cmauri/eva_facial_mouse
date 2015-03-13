package com.crea_si.eviacam.service;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.MyJavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.OrientationEventListener;

public class CameraListener extends OrientationEventListener implements CvCameraViewListener2 {
    private PointerControl mPointerControl;
    private CameraBridgeViewBase mCameraView;
    private boolean mOrientationLandscape= true;
    
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
                    // TODO: get cascade path from apk resources
                    VisionPipeline.init("/mnt/sdcard/Download/haarcascade_profileface.xml");
                    
                    // start camera
                    mCameraView.enableView();
                    
                    // enable sensor listener
                    CameraListener.this.enable();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    public CameraListener(PointerControl pa) {
        super(EViacamService.getInstance().getApplicationContext());
        EVIACAM.debug("Create CameraListener");

        mPointerControl= pa;
        
        // TODO: detect if device has frontal camera or not
        // Create capture view directly
        cameraInfo();
        mCameraView= new MyJavaCameraView(EViacamService.getInstance().getApplicationContext(), 
                CameraBridgeViewBase.CAMERA_ID_FRONT);
        
        // Set CameraBridgeViewBase parameters        
        // TODO: Damn! It seems that for certain resolutions (for instance 320x240 on a Galaxy Nexus)
        // crashes with a "Callback buffer was too small! error", it works at 352x288
        
        mCameraView.setMaxFrameSize(352, 288);
        mCameraView.enableFpsMeter(); // For testing
        mCameraView.setCvCameraViewListener(this);
        
        // Set View parameters
        mCameraView.setVisibility(SurfaceView.VISIBLE);
        
    }
    
    public void StartCamera() {
        // Start OpenCV
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, 
                EViacamService.getInstance().getApplicationContext(), mLoaderCallback);
    }
    
    public void StopCamera() {
        if (mCameraView!= null) {
            this.disable();
            mCameraView.disableView();
        }
    }

    SurfaceView getCameraSurface(){
        return mCameraView;
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
     
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        PointF vel = new PointF(0, 0);
        VisionPipeline.processFrame(rgba.getNativeObjAddr(), 0, vel);
        if (!mOrientationLandscape) {
            float tmp= vel.x;
            vel.x= -vel.y;
            vel.y= tmp;
        }
        mPointerControl.updateMotion(vel);
        return rgba;
    }
    
    public void cameraInfo() {
        int camNum= Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo= new CameraInfo();
        for (int i= 0; i< camNum; i++) {
            Camera.getCameraInfo (i, cameraInfo);
            EVIACAM.debug("Camera: " + i);
            EVIACAM.debug("cameraInfo.canDisableShutterSound: " + cameraInfo.canDisableShutterSound);
            if (cameraInfo.facing== CameraInfo.CAMERA_FACING_BACK) {
                EVIACAM.debug("cameraInfo.facing: CAMERA_FACING_BACK");
            }
            else if (cameraInfo.facing== CameraInfo.CAMERA_FACING_FRONT) {
                EVIACAM.debug("cameraInfo.facing: CAMERA_FACING_FRONT");
            }
            else {
                EVIACAM.debug("cameraInfo.facing: UNKNOWN");
            }
            
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
            
            EVIACAM.debug("cameraInfo.orientation: " + cameraInfo.orientation);
        }
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        
        // Check this
        // http://www.androidzeitgeist.com/2013/01/fixing-rotation-camera-picture.html
        
        Context c= EViacamService.getInstance().getApplicationContext();
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d= wm.getDefaultDisplay();
        int rotation= d.getRotation();
        
        /**
         * Returns the rotation of the screen from its "natural" orientation. 
         * 
         * The returned value may be Surface.ROTATION_0 (no rotation), Surface.ROTATION_90, 
         * Surface.ROTATION_180, or Surface.ROTATION_270. For example, if a device has a 
         * naturally tall screen, and the user has turned it on its side to go into a 
         * landscape orientation, the value returned here may be either Surface.ROTATION_90 
         * or Surface.ROTATION_270 depending on the direction it was turned. 
         * 
         * The angle is the rotation of the drawn graphics on the screen, which is the opposite 
         * direction of the physical rotation of the device. For example, if the device is 
         * rotated 90 degrees counter-clockwise, to compensate rendering will be rotated by 
         * 90 degrees clockwise and thus the returned value here will be Surface.ROTATION_90.
         */
        
        switch (rotation) {
        case Surface.ROTATION_0:
            EVIACAM.debug("Rotation: ROTATION_0");
            break;
        case Surface.ROTATION_90:
            EVIACAM.debug("Rotation: ROTATION_90");
            break;           
        case Surface.ROTATION_180:
            EVIACAM.debug("Rotation: ROTATION_180");
            break;
        case Surface.ROTATION_270:
            EVIACAM.debug("Rotation: ROTATION_270");
            break;
        default:
            EVIACAM.debug("Rotation unknown: " + rotation);
        }
        
        
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {           
            mOrientationLandscape= true;
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            mOrientationLandscape= false;
        }
        else
            EVIACAM.debug("onConfigurationChanged: " + newConfig.toString());
    }

    @Override
    public void onOrientationChanged(int orientation) {
       // EVIACAM.debug("onOrientationChanged: " + orientation);
        
    }
}
