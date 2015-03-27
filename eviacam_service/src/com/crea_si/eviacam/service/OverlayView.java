package com.crea_si.eviacam.service;

import android.content.Context;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.graphics.PixelFormat;

public class OverlayView extends RelativeLayout {
    private ControlsView mControlsView;
    private PointerView mPointerView;

    OverlayView(Context c) {
        super(c);
        
        WindowManager.LayoutParams feedbackParams = new WindowManager.LayoutParams();
        
        feedbackParams.setTitle("FeedbackOverlay");
        
        // Transparent background
        feedbackParams.format = PixelFormat.TRANSLUCENT;    
        
        // Create an always on top window
        feedbackParams.type = WindowManager.LayoutParams.TYPE_PHONE | 
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
               
        // Whole screen is covered (including status bar)
        feedbackParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        
        feedbackParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        feedbackParams.height = WindowManager.LayoutParams.MATCH_PARENT;
      
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(this, feedbackParams);
        
        /*
         * controls view layer 
         */
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(this.getWidth(), this.getHeight());
        lp.width= RelativeLayout.LayoutParams.MATCH_PARENT;
        lp.height= RelativeLayout.LayoutParams.MATCH_PARENT;
        
        mControlsView= new ControlsView(c);
        mControlsView.setLayoutParams(lp);
        this.addView(mControlsView);
        
        /*
         * pointer view layer 
         */
        mPointerView= new PointerView(c);
        mPointerView.setLayoutParams(lp);
        this.addView(mPointerView);
        
        EVIACAM.debug("finish createOverlay");
    }
   
    void cleanup() {
        WindowManager wm= (WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.removeViewImmediate(this);

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
