package com.crea_si.eviacam.service;

import android.content.Context;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.graphics.PixelFormat;

public class LayoutManager {
    private Context mContext;
    private FeedbackOverlayView mFOV;
    
    LayoutManager (Context context) {
        mContext=context;
    }
    
    /***
     *  // Set overlay window to provide visual feedback
     */
    void createFeedbackOverlay() {        
        if (mFOV != null) return;
        
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
      
        mFOV = new FeedbackOverlayView(mContext);
        WindowManager wm= (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mFOV, feedbackParams);

        EVIACAM.debug("finish createFeedbackOverlay");
    }    
    
    void destroyFeedbackOverlay() {
        if (mFOV == null) return;
        
        WindowManager wm= (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        wm.removeViewImmediate(mFOV);
        mFOV = null;
        EVIACAM.debug("finish destroyFeedbackOverlay");
    }
    
    FeedbackOverlayView getViewGroup() {
        return mFOV;
    }
}
