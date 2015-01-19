package com.crea_si.eviacam.service;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup; 
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;

public class CameraListener implements CvCameraViewListener {
    private Context mContext;
    private CameraBridgeViewBase mCameraView;
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(mContext) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    EVIACAM.debug("OpenCV loaded successfully");
                    mCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    public CameraListener(Context context, ViewGroup vg) {
        EVIACAM.debug("Create CameraListener");
        
        mContext= context;
       
        // Create capture view using layout
        //LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
        //View v= inflater.inflate(R.layout.camera_view, null);
        //mCameraView= (CameraBridgeViewBase) v.findViewById(R.id.camera_view);
        
        // Create capture view directly
        View v= mCameraView= new JavaCameraView(context, -1);
        
        // Set  CameraBridgeViewBase parameters
        
        // TODO: Damn! It seems that for certain resolutions (for instance 320x240 on a Galaxy Nexus)
        // crashes with a "Callback buffer was too small! error", it works at 352x288
        
        mCameraView.setMaxFrameSize(352, 288);
        mCameraView.enableFpsMeter(); // For testing
        mCameraView.setCvCameraViewListener(this);
        
        // Set View parameters
        mCameraView.setLayoutParams(new LayoutParams(352, 288));
        mCameraView.setVisibility(SurfaceView.VISIBLE);
        
        // Add view to parent
        vg.addView(v);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, context, mLoaderCallback);
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        EVIACAM.debug("onCameraViewStarted");
    }

    @Override
    public void onCameraViewStopped() {
        EVIACAM.debug("onCameraViewStopped");        
    }

    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        EVIACAM.debug("onCameraFrame");
        return inputFrame;
    }
     
    /*
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        EVIACAM.debug("onCameraFrame");
        return inputFrame.rgba();
    }
    */
}
