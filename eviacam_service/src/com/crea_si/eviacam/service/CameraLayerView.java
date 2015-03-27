package com.crea_si.eviacam.service;

import android.content.Context;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

public class CameraLayerView extends RelativeLayout {
    // camera viewer size
    private final int CAM_SURFACE_WIDTH= 320;
    private final int CAM_SURFACE_HEIGHT= 240;
        
    public CameraLayerView(Context context) {
        super(context);
    }
    
    public void addCameraSurface(SurfaceView v) {
        // set layout and add to parent
        RelativeLayout.LayoutParams lp= 
                new RelativeLayout.LayoutParams(CAM_SURFACE_WIDTH, CAM_SURFACE_HEIGHT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        v.setLayoutParams(lp);

        this.addView(v);
    }
}
