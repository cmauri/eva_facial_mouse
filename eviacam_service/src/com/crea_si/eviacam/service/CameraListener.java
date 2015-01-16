package com.crea_si.eviacam.service;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup; 
import android.view.SurfaceView;

public class CameraListener implements CvCameraViewListener2 {
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
       
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
        View v= inflater.inflate(R.layout.camera_view, null);
               
        mCameraView= (CameraBridgeViewBase) v.findViewById(R.id.camera_view);
        //mCameraView= new JavaCameraView(context, -1);
        //mCameraView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        mCameraView.setVisibility(SurfaceView.VISIBLE);
        mCameraView.setCvCameraViewListener(this);
        vg.addView(v);

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, context, mLoaderCallback);
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        EVIACAM.debug("onCameraViewStarted");
    }

    @Override
    public void onCameraViewStopped() {
        EVIACAM.debug("onCameraViewStopped");        
    }

    /*
    @Override
    public Mat onCameraFrame(Mat inputFrame) {
        EVIACAM.debug("onCameraFrame");
        return null;
    }
    */

    private int count= 0;
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        count++;
        if (count== 100) 
            mCameraView.disableView();

        EVIACAM.debug("onCameraFrame");
        return inputFrame.rgba();
    }
}
