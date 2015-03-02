package com.crea_si.eviacam.service;

import android.content.Context;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.RelativeLayout;
import android.graphics.PixelFormat;

public class OverlayManager {
    private final int CAM_SURFACE_WIDTH= 320;
    private final int CAM_SURFACE_HEIGHT= 240;

    private OverlayView mOverlayView;

    /***
     *  // Set overlay window to provide visual feedback
     */
    void createOverlay() {        
        if (mOverlayView != null) return;
        
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
        mOverlayView = new OverlayView(c);
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mOverlayView, feedbackParams);

        EVIACAM.debug("finish createOverlay");
    }    
    
    void destroyOverlay() {
        if (mOverlayView == null) return;
        
        Context c= EViacamService.getInstance().getApplicationContext();
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.removeViewImmediate(mOverlayView);
        mOverlayView = null;
        EVIACAM.debug("finish destroyOverlay");
    }
    
    void addCameraSurface(SurfaceView v) {
        // Set layout and add to parent
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(CAM_SURFACE_WIDTH, CAM_SURFACE_HEIGHT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        v.setLayoutParams(lp);

        mOverlayView.addView(v);
    }
    
    OverlayView getOverlayView() {
        return mOverlayView;
    }
}
