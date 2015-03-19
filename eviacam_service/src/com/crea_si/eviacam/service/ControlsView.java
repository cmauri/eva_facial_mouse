package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Point;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

public class ControlsView extends RelativeLayout {
    // camera viewer size
    private final int CAM_SURFACE_WIDTH= 320;
    private final int CAM_SURFACE_HEIGHT= 240;
    
    // reference to the view which draws buttons 
    ActionsButtonsView mActionsButtonsView;
        
    public ControlsView(Context context) {
        super(context);
        
        // create and add buttons. initially these are hidden
        mActionsButtonsView= new ActionsButtonsView(context);
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mActionsButtonsView.setLayoutParams(lp);
        mActionsButtonsView.setVisibility(View.GONE);
        addView(mActionsButtonsView);
    }
    
    public void addCameraSurface(SurfaceView v) {
        // set layout and add to parent
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(CAM_SURFACE_WIDTH, CAM_SURFACE_HEIGHT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        v.setLayoutParams(lp);

        this.addView(v);
    }
    
    public void showButtons(final Point p, final int actions) {
        this.post(new Runnable() {
            @Override
            public void run() {
                if (actions == 0) {
                    mActionsButtonsView.setVisibility(View.GONE);
                    return;
                }
                
                mActionsButtonsView.setVisibility(View.VISIBLE);
                mActionsButtonsView.updateButtons(actions);
                RelativeLayout.LayoutParams lp= (RelativeLayout.LayoutParams) mActionsButtonsView.getLayoutParams();
                lp.leftMargin= p.x;
                lp.topMargin= p.y;
                mActionsButtonsView.setLayoutParams(lp);
            }
        });
    }
}
