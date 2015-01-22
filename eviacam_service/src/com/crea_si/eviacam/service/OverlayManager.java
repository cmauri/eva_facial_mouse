package com.crea_si.eviacam.service;

import android.content.Context;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.RelativeLayout;
import android.graphics.PixelFormat;

public class OverlayManager {
    private final int CAM_SURFACE_WIDTH= 88;
    private final int CAM_SURFACE_HEIGHT= 72;
    
    private Context mContext;
    private OverlayView mOverlayView;
    
    OverlayManager (Context context) {
        mContext=context;
    }
    
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
      
        mOverlayView = new OverlayView(mContext);
        WindowManager wm= (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mOverlayView, feedbackParams);

        EVIACAM.debug("finish createOverlay");
    }    
    
    void destroyOverlay() {
        if (mOverlayView == null) return;
        
        WindowManager wm= (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
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
}
