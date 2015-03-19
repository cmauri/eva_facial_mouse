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
    
    // reference to the view which draws the actions context menu 
    ActionsButtonsView mActionsButtonsView;
        
    public ControlsView(Context context) {
        super(context);
        
        // create and add buttons. initially these are hidden.
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
        // TODO: when the screen is rotated the menu vanishes
        
        this.post(new Runnable() {
            @Override
            public void run() {
                if (actions == 0) {
                    mActionsButtonsView.setVisibility(View.GONE);
                    return;
                }
                
                // set actions menu view as visible
                mActionsButtonsView.setVisibility(View.VISIBLE);
                
                // update buttons list that need to display
                mActionsButtonsView.updateButtons(actions);
                        
                /*
                 * compute position in which actions menu will be displayed
                 * 
                 * it first tries to display it below and at the left of the pointer.
                 * if this is not possible move to the other side
                 */
                RelativeLayout.LayoutParams lp= (RelativeLayout.LayoutParams) mActionsButtonsView.getLayoutParams();
                
                // get actions menu expected width
                int expectedWidth= mActionsButtonsView.getMeasuredWidth();
                
                // fits at the left of the pointer?
                if (p.x - expectedWidth> 0) lp.leftMargin= p.x - expectedWidth;
                else lp.leftMargin= p.x;
                
                // get actions menu expected height
                int expectedHeight= mActionsButtonsView.getMeasuredWidth();
                
                // fits below the pointer?
                if (p.y + expectedHeight< getHeight()) lp.topMargin= p.y;
                else lp.topMargin= p.y - expectedHeight;

                // set menu position
                mActionsButtonsView.setLayoutParams(lp);
            }
        });
    }
}
