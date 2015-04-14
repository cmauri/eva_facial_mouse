package com.crea_si.eviacam.service;

import org.opencv.core.Mat;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.os.IBinder;
import android.os.Messenger;
import android.view.View;

public class EViacamEngine implements FrameProcessor {

    /*
     * states of the engine
     */
    private static final int STATE_NONE= 0;
    private static final int STATE_RUNNING= 1;
    private static  final int STATE_PAUSED= 2;
    private static final int STATE_STOPPED= 3;
    
    // root overlay view
    private OverlayView mOverlayView;
    
    // layer for drawing the pointer and the dwell click feedback
    private PointerLayerView mPointerLayer;
    
    // layer for drawing the docking panel
    private DockPanelLayerView mDockPanelView;
    
    // layer for drawing different controls
    private ControlsLayerView mControlsLayer;
    
    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    // dwell clicking function
    private DwellClick mDwellClick;
    
    // perform actions on the UI using the accessibility API
    private AccessibilityAction mAccessibilityAction;
    
    // object which encapsulates rotation and orientation logic
    private OrientationManager mOrientationManager;
    
    // current engine state
    private int mCurrentState= STATE_NONE;
    
    
    /** Messenger for communicating with the service. */
    Messenger mService = null;

    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            EVIACAM.debug("remoteIME:onServiceConnected");
            mService = new Messenger(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            EVIACAM.debug("remoteIME:onServiceDisconnected");
            mService = null;
            mBound = false;
        }
    };
    
    
    public EViacamEngine(Context c) {
        /*
         * UI stuff 
         */

        // create overlay root layer
        mOverlayView= new OverlayView(c);
        
        CameraLayerView cameraLayer= new CameraLayerView(c);
        mOverlayView.addFullScreenLayer(cameraLayer);

        mDockPanelView= new DockPanelLayerView(c);
        mOverlayView.addFullScreenLayer(mDockPanelView);
        
        mControlsLayer= new ControlsLayerView(c);
        mOverlayView.addFullScreenLayer(mControlsLayer);
        
        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(c);
        mOverlayView.addFullScreenLayer(mPointerLayer);
        
        /*
         * control stuff
         */
        
        mPointerControl= new PointerControl(c, mPointerLayer);
        
        mDwellClick= new DwellClick(c);
        
        mAccessibilityAction= new AccessibilityAction (mControlsLayer, mDockPanelView);
        
        // create camera & machine vision part
        mCameraListener= new CameraListener(c, this);
        cameraLayer.addCameraSurface(mCameraListener.getCameraSurface());
        
        mOrientationManager= new OrientationManager(c, mCameraListener.getCameraOrientation());
        
        /*
         * start processing frames
         */
        mCameraListener.startCamera();

        
        try {
            boolean retval= c.bindService(new Intent("com.crea_si.eviacam_keyboard.MessengerService"), mConnection, Context.BIND_AUTO_CREATE);
            EVIACAM.debug("bindService returned:" + retval);
        }
        catch (Exception e) {
            EVIACAM.debug("EXCEPTION:" + e.getMessage());
        }
        
        
        
        mCurrentState= STATE_RUNNING;
    }
   
    public void cleanup() {
        if (mCurrentState == STATE_STOPPED) return;
               
        mCameraListener.stopCamera();
        mCameraListener= null;
        
        mOrientationManager.cleanup();
        mOrientationManager= null;

        mDwellClick.cleanup();
        mDwellClick= null;
        
        mPointerControl.cleanup();
        mPointerControl= null;
        
        // nothing to be done for mPointerLayer and mControlsLayer
        
        mDockPanelView.cleanup();
        mDockPanelView= null;
        
        mOverlayView.cleanup();
        mOverlayView= null;

        mCurrentState= STATE_STOPPED;
    }
   
    public void pause() {
        if (mCurrentState != STATE_RUNNING) return;
        
        // TODO: this is a basic method to pause the program
        // pause/resume should reset internal state of some objects 
        mCurrentState= STATE_PAUSED;
        mPointerLayer.setVisibility(View.INVISIBLE);
        mControlsLayer.setVisibility(View.INVISIBLE);
        mDockPanelView.setVisibility(View.INVISIBLE);
    }
    
    public void resume() {
        if (mCurrentState != STATE_PAUSED) return;
        
        // TODO: see comment on pause()
        mDockPanelView.setVisibility(View.VISIBLE);
        mControlsLayer.setVisibility(View.VISIBLE);
        mPointerLayer.setVisibility(View.VISIBLE);
        
        // make sure that changes during pause (e.g. docking panel edge) are applied        
        mOverlayView.requestLayout();
        mCurrentState= STATE_RUNNING;
    }    
    
    public void onConfigurationChanged(Configuration newConfig) {
        if (mOrientationManager != null) mOrientationManager.onConfigurationChanged(newConfig);
    }
    
    /*
     * process incoming camera frame 
     * 
     * this method is called from a secondary thread 
     */
    @Override
    public void processFrame(Mat rgba) {
        if (mCurrentState != STATE_RUNNING) return;
        
        int phyRotation = mOrientationManager.getPictureRotation();
        
        // call jni part to track face
        PointF motion = new PointF(0, 0);
        VisionPipeline.processFrame(rgba.getNativeObjAddr(), phyRotation, motion);
        
        // compensate mirror effect
        motion.x= -motion.x;
        
        // fix motion orientation according to device rotation and screen orientation 
        mOrientationManager.fixVectorOrientation(motion);
             
        // update pointer location given face motion
        mPointerControl.updateMotion(motion);
        
        // get new pointer location
        PointF pointerLocation= mPointerControl.getPointerLocation();
        
        // dwell clicking update
        boolean clickGenerated= 
                mDwellClick.updatePointerLocation(pointerLocation);
        
        // redraw pointer layer
        mPointerLayer.postInvalidate();
        
        // perform action when needed
        if (clickGenerated) { 
            mAccessibilityAction.performAction(pointerLocation);
        }
    }
}
