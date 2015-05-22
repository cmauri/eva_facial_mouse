/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

 package com.crea_si.eviacam.service;

import org.opencv.core.Mat;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

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

    // layer for the scrolling user interface
    private ScrollLayerView mScrollLayerView;
    
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

        mScrollLayerView= new ScrollLayerView(c);
        mOverlayView.addFullScreenLayer(mScrollLayerView);
        
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
        
        mAccessibilityAction= new AccessibilityAction (
                mControlsLayer, mDockPanelView, mScrollLayerView);
        
        // create camera & machine vision part
        mCameraListener= new CameraListener(c, this);
        cameraLayer.addCameraSurface(mCameraListener.getCameraSurface());
        
        mOrientationManager= new OrientationManager(c, mCameraListener.getCameraOrientation());
        
        /*
         * start processing frames
         */
        mCameraListener.startCamera();

        mCurrentState= STATE_RUNNING;
    }
   
    public void cleanup() {
        if (mCurrentState == STATE_STOPPED) return;
               
        mCameraListener.stopCamera();
        mCameraListener= null;
        
        mOrientationManager.cleanup();
        mOrientationManager= null;
        
        mAccessibilityAction.cleanup();
        mAccessibilityAction= null;

        mDwellClick.cleanup();
        mDwellClick= null;
        
        mPointerControl.cleanup();
        mPointerControl= null;
        
        // nothing to be done for mScrollLayerView and mControlsLayer
        
        mDockPanelView.cleanup();
        mDockPanelView= null;
        
        mPointerLayer.cleanup();
        mPointerLayer= null;
        
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
        mScrollLayerView.setVisibility(View.INVISIBLE);
        mControlsLayer.setVisibility(View.INVISIBLE);
        mDockPanelView.setVisibility(View.INVISIBLE);
    }
    
    public void resume() {
        if (mCurrentState != STATE_PAUSED) return;
        
        // TODO: see comment on pause()
        mDockPanelView.setVisibility(View.VISIBLE);
        mControlsLayer.setVisibility(View.VISIBLE);
        mScrollLayerView.setVisibility(View.VISIBLE);
        mPointerLayer.setVisibility(View.VISIBLE);
        
        // make sure that changes during pause (e.g. docking panel edge) are applied        
        mOverlayView.requestLayout();
        mCurrentState= STATE_RUNNING;
    }    
    
    public void onConfigurationChanged(Configuration newConfig) {
        if (mOrientationManager != null) mOrientationManager.onConfigurationChanged(newConfig);
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mAccessibilityAction != null) mAccessibilityAction.onAccessibilityEvent(event);
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
        
        // update pointer position and click progress
        mPointerLayer.updatePosition(pointerLocation);
        mPointerLayer.updateClickProgress(mDwellClick.getClickProgressPercent());
        mPointerLayer.postInvalidate();
        
        // this needs to be called regularly
        mAccessibilityAction.refresh();
        
        // perform action when needed
        if (clickGenerated) { 
            mAccessibilityAction.performAction(pointerLocation);
        }
    }
}
