package com.crea_si.eviacam.service;

import org.opencv.core.Mat;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;

public class EViacamEngine implements FrameProcessor {

    // object which manages the overlay windows
    private OverlayManager mOverlayManager;
    
    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    // object which encapsulated rotation and orientation logic
    OrientationManager mOrientationManager;
        
    boolean mRunning= false;

    public EViacamEngine(Context c) {
        // create overlay
        mOverlayManager= new OverlayManager();
        mOverlayManager.createOverlay();
        
        // create pointer control object
        mPointerControl= new PointerControl(
                mOverlayManager.getPointerView(), mOverlayManager.getControlsView());
        
        // create camera & machine vision part
        mCameraListener= new CameraListener(c, this);
        mOverlayManager.addCameraSurface(mCameraListener.getCameraSurface());
        
        mOrientationManager= new OrientationManager(c, mCameraListener.getCameraOrientation());
        
        // start processing frames
        mCameraListener.startCamera();

        mRunning= true;
    }
   
    public void cleanup() {
        if (!mRunning) return;
               
        mCameraListener.stopCamera();
        mCameraListener= null;
        
        mOrientationManager.cleanup();
        mOrientationManager= null;

        mPointerControl.cleanup();
        mPointerControl= null;
        
        mOverlayManager.destroyOverlay();
        mOverlayManager= null;

        mRunning= false;
    }
   
    public void onConfigurationChanged(Configuration newConfig) {
        if (mOrientationManager != null) mOrientationManager.onConfigurationChanged(newConfig);
    }

    @Override
    public void processFrame(Mat rgba) {
        int phyRotation = mOrientationManager.getPictureRotation();
        
        // TODO: refactor as attribute to avoid an object creation for each frame
        PointF motion = new PointF(0, 0);
        
        // call jni part to track face
        VisionPipeline.processFrame(rgba.getNativeObjAddr(), phyRotation, motion);
        
        // compensate mirror effect
        motion.x= -motion.x;
        
        // fix motion orientation according to device rotation and screen orientation 
        mOrientationManager.fixVectorOrientation(motion);
             
        // send motion to pointer controller delegate
        mPointerControl.updateMotion(motion);
    }
}
