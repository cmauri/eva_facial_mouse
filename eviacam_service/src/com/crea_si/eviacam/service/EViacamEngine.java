package com.crea_si.eviacam.service;

import org.opencv.core.Mat;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class EViacamEngine implements FrameProcessor {

    // object which manages the overlay windows
    private OverlayManager mOverlayManager;
    
    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    // orientation sensors listener. keeps updated the actual orientation of the
    // device (independently of the screen orientation)
    private PhysicalOrientation mPhysicalOrientation;
    
    // orientation of the screen
    private int mScreenOrientation= 0;
    final int mCameraOrientation;
    
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
        
        // start processing frames
        mCameraListener.startCamera();
        
        // create physical orientation manager
        mPhysicalOrientation= new PhysicalOrientation(c);
        // enable sensor listener
        mPhysicalOrientation.enable();
        
        mScreenOrientation= getScreenOrientation();
        mCameraOrientation= mCameraListener.getCameraOrientation();
        
        mRunning= true;
    }
   
    public void cleanup() {
        if (!mRunning) return;
        
        mPhysicalOrientation.disable();
        
        mCameraListener.stopCamera();
        mCameraListener= null;

        mPointerControl.cleanup();
        mPointerControl= null;
        
        mOverlayManager.destroyOverlay();
        mOverlayManager= null;

        mRunning= false;
    }
   
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
     * 
     */
    static
    private int getScreenOrientation() {
        Context c= EViacamService.getInstance().getApplicationContext();
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d= wm.getDefaultDisplay();
        switch (d.getRotation()) {
        case Surface.ROTATION_0: return 0;
        case Surface.ROTATION_90: return 90;
        case Surface.ROTATION_180: return 180;
        case Surface.ROTATION_270: return 270;
        default:
            throw new RuntimeException("wrong screen orientation");
        }
    }
    
    /*
     * Called by the system when the device configuration changes
     */
    public void onConfigurationChanged(Configuration newConfig) {
        mScreenOrientation= getScreenOrientation();
        EVIACAM.debug("Screen rotation changed: " + mScreenOrientation);
    }

    @Override
    public void processFrame(Mat rgba) {
        int phyRotation = mCameraOrientation - mPhysicalOrientation.getCurrentOrientation();
        if (phyRotation< 0) phyRotation+= 360;
        
        // TODO: refactor as attribute to avoid an object creation for each frame
        PointF vel = new PointF(0, 0);
        
        // call jni part to track face
        VisionPipeline.processFrame(rgba.getNativeObjAddr(), phyRotation, vel);
        
        // compensate mirror effect
        vel.x= -vel.x;
        
        // calculate equivalent physical device rotation for the current screen orientation
        int equivPhyRotation= 360 - mScreenOrientation;
        if (equivPhyRotation== 360) equivPhyRotation= 0;
       
        // when is a mismatch between physical rotation and screen orientation
        // need to cancel it out (e.g. activity that forces specific screen orientation
        // but the device has not been rotated)
        int diffRotation= equivPhyRotation -  mPhysicalOrientation.getCurrentOrientation();
        if (diffRotation< 0) diffRotation+= 360;
        switch (diffRotation) {
        case 0: 
            // Nothing to be done
            break;
        case 90: {
            float tmp= vel.x;
            vel.x= -vel.y;
            vel.y= tmp;
            break;
        }
        case 180:
            vel.x= -vel.x;
            vel.y= -vel.y;
            break;
        case 270: {
            float tmp= vel.x;
            vel.x= vel.y;
            vel.y= -tmp;
            break;
        }
        default:
            throw new RuntimeException("wrong diffRotation");
        }
             
        // send motion to pointer controller delegate
        mPointerControl.updateMotion(vel);
    }
}
