package com.crea_si.eviacam.service;

import android.content.Context;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.graphics.PixelFormat;

public class OverlayView extends RelativeLayout {

    OverlayView(Context c) {
        super(c);
        
        WindowManager.LayoutParams feedbackParams = new WindowManager.LayoutParams();
        
        feedbackParams.setTitle("FeedbackOverlay");
        
        // Transparent background
        feedbackParams.format = PixelFormat.TRANSLUCENT;    
        
        /*
         * Type of window- Create an always on top window
         * 
         * TYPE_PHONE: These are non-application windows providing user interaction with the
         *      phone (in particular incoming calls). These windows are normally placed above 
         *      all applications, but behind the status bar. In multiuser systems shows on all 
         *      users' windows.
         *      
         * TYPE_SYSTEM_OVERLAY: system overlay windows, which need to be displayed on top of 
         *      everything else. These windows must not take input focus, or they will interfere 
         *      with the keyguard. In multiuser systems shows only on the owning user's window
         *      
         * TODO: For future versions check TYPE_ACCESSIBILITY_OVERLAY
         */
        feedbackParams.type = WindowManager.LayoutParams.TYPE_PHONE | 
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
               
        /*
         * Type of window. Whole screen is covered (including status bar)
         * 
         * FLAG_NOT_FOCUSABLE: this window won't ever get key input focus, so the user can not 
         *      send key or other button events to it. It can use the full screen for its content 
         *      and cover the input method if needed
         *      
         * FLAG_LAYOUT_IN_SCREEN: place the window within the entire screen, ignoring decorations 
         *      around the border (such as the status bar)
         *             
         */
        feedbackParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        
        feedbackParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        feedbackParams.height = WindowManager.LayoutParams.MATCH_PARENT;
      
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(this, feedbackParams);
    }
   
    void cleanup() {
        WindowManager wm= (WindowManager) this.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.removeViewImmediate(this);

        EVIACAM.debug("finish destroyOverlay");
    }
    
    void addFullScreenLayer (View v) {
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(this.getWidth(), this.getHeight());
        lp.width= RelativeLayout.LayoutParams.MATCH_PARENT;
        lp.height= RelativeLayout.LayoutParams.MATCH_PARENT;
        
        v.setLayoutParams(lp);
        this.addView(v);
    }
}
