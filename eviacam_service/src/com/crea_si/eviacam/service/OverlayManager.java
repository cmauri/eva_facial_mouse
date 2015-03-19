package com.crea_si.eviacam.service;

import android.content.Context;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.RelativeLayout;
import android.graphics.PixelFormat;

public class OverlayManager {
    private RelativeLayout mRootView;
    private ControlsView mControlsView;
    private PointerView mPointerView;

    /***
     *  // Set overlay window to provide visual feedback
     */
    void createOverlay() {        
        if (mRootView != null) return;
        
        LayoutParams feedbackParams = new LayoutParams();
        
        feedbackParams.setTitle("FeedbackOverlay");
        
        // Transparent background
        feedbackParams.format = PixelFormat.TRANSLUCENT;    
        
        // Create an always on top window
        feedbackParams.type = LayoutParams.TYPE_PHONE | LayoutParams.TYPE_SYSTEM_OVERLAY;
               
        // Whole screen is covered (including status bar)
        feedbackParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        
        feedbackParams.width = LayoutParams.MATCH_PARENT;
        feedbackParams.height = LayoutParams.MATCH_PARENT;
      
        Context c= EViacamService.getInstance().getApplicationContext();
        mRootView = new RelativeLayout(c);
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mRootView, feedbackParams);

        /*
         * controls view layer 
         */
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(mRootView.getWidth(), mRootView.getHeight());
        lp.width= RelativeLayout.LayoutParams.MATCH_PARENT;
        lp.height= RelativeLayout.LayoutParams.MATCH_PARENT;
        
        mControlsView= new ControlsView(c);
        mControlsView.setLayoutParams(lp);
        mRootView.addView(mControlsView);
        
        /*
         * pointer view layer 
         */
        mPointerView= new PointerView(c);
        mPointerView.setLayoutParams(lp);
        mRootView.addView(mPointerView);
        
        EVIACAM.debug("finish createOverlay");
    }    
    
    void destroyOverlay() {
        if (mRootView == null) return;
        
        Context c= EViacamService.getInstance().getApplicationContext();
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.removeViewImmediate(mRootView);
        mRootView = null;
        EVIACAM.debug("finish destroyOverlay");
    }
    
    void addCameraSurface(SurfaceView v) {
        mControlsView.addCameraSurface(v);
    }
    
    public PointerView getPointerView() {
        return mPointerView;
    }
    
    public ControlsView getControlsView() {
        return mControlsView;
    }
}
