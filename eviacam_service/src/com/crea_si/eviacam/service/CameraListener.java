package com.crea_si.eviacam.service;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.content.Context;
import android.graphics.PointF;
import android.view.SurfaceView;


public class CameraListener implements CvCameraViewListener2 {
    private PointerControl mPointerControl;
    private CameraBridgeViewBase mCameraView;
    
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
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    public CameraListener(PointerControl pa) {
        EVIACAM.debug("Create CameraListener");

        mPointerControl= pa;
        
        // TODO: detect if device has frontal camera or not
        // Create capture view directly
        mCameraView= new JavaCameraView(EViacamService.getInstance().getApplicationContext(), 
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
        if (mCameraView!= null)
            mCameraView.disableView();
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
        VisionPipeline.finish();
    }
     
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        PointF vel = new PointF(0, 0);
        VisionPipeline.processFrame(rgba.getNativeObjAddr(), vel);
        mPointerControl.updateMotion(vel);
        return rgba;
    }
}
