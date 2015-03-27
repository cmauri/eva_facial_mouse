package com.crea_si.eviacam.service;

import android.content.Context;
import android.content.res.Configuration;

public class EViacamEngine  {

    // object which manages the overlay windows
    private OverlayManager mOverlayManager;
    
    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    boolean mRunning= false;

    public EViacamEngine(Context c) {
        init();
    }
        
    private void init() {
        // create overlay
        mOverlayManager= new OverlayManager();
        mOverlayManager.createOverlay();
        
        // create pointer control object
        mPointerControl= new PointerControl(
                mOverlayManager.getPointerView(), mOverlayManager.getControlsView());
        
        // create camera & machine vision part
        mCameraListener= new CameraListener(mPointerControl);
        mOverlayManager.addCameraSurface(mCameraListener.getCameraSurface());
        
        // start processing frames
        mCameraListener.StartCamera();
        
        mRunning= true;
    }
    
    public void cleanup() {
        if (!mRunning) return;
        
        mCameraListener.StopCamera();
        mCameraListener= null;

        mPointerControl.cleanup();
        mPointerControl= null;
        
        mOverlayManager.destroyOverlay();
        mOverlayManager= null;

        mRunning= false;
    }
    
    /*
     * Called by the system when the device configuration changes
     */
    public void onConfigurationChanged(Configuration newConfig) {
        if (mCameraListener != null) {
            mCameraListener.onConfigurationChanged(newConfig);
        }
    }
}
