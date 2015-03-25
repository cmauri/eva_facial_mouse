package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Point;
import android.view.SurfaceView;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.RelativeLayout;

public class ControlsView extends RelativeLayout {
    // camera viewer size
    private final int CAM_SURFACE_WIDTH= 320;
    private final int CAM_SURFACE_HEIGHT= 240;
    
    // reference to the view which draws the actions context menu 
    ActionsMenuView mActionsMenuView;
        
    public ControlsView(Context context) {
        super(context);
        
        // create and add buttons. initially these are hidden.
        mActionsMenuView= new ActionsMenuView(context);
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        mActionsMenuView.setLayoutParams(lp);
        mActionsMenuView.setVisibility(View.GONE);
        addView(mActionsMenuView);
    }
    
    public void addCameraSurface(SurfaceView v) {
        // set layout and add to parent
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(CAM_SURFACE_WIDTH, CAM_SURFACE_HEIGHT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        v.setLayoutParams(lp);

        this.addView(v);
    }
    
    public void showActionsMenu(final Point p, final int actions) {
        // TODO: when the screen is rotated the menu vanishes
        
        this.post(new Runnable() {
            @Override
            public void run() {
                if (actions == 0) {
                    mActionsMenuView.setVisibility(View.GONE);
                    setBackgroundColor(0);
                    return;
                }
                
                // set actions menu view as visible
                mActionsMenuView.setVisibility(View.VISIBLE);
                
                // update buttons list that need to display
                mActionsMenuView.updateButtons(actions);
                      
                // dim background
                setBackgroundColor(0x80000000);
                
                /*
                 * compute position in which actions menu will be displayed
                 * 
                 * it first tries to display it below and at the left of the pointer.
                 * if this is not possible move to the other side
                 */
                RelativeLayout.LayoutParams lp= (RelativeLayout.LayoutParams) mActionsMenuView.getLayoutParams();
                
                // get actions menu expected width
                int expectedWidth= mActionsMenuView.getMeasuredWidth();
                
                // fits at the left of the pointer?
                if (p.x - expectedWidth> 0) lp.leftMargin= p.x - expectedWidth;
                else lp.leftMargin= p.x;
                
                // get actions menu expected height
                int expectedHeight= mActionsMenuView.getMeasuredWidth();
                
                // fits below the pointer?
                if (p.y + expectedHeight< getHeight()) lp.topMargin= p.y;
                else lp.topMargin= p.y - expectedHeight;

                // set menu position
                mActionsMenuView.setLayoutParams(lp);
            }
        });
    }
    
    public void hideActionsMenu() {
        showActionsMenu(null, 0);
    }
    
    /*
     * get action under point p
     */
    
    
    public int testClick (Point p)  {
        int[] location= new int[2];
        
        mActionsMenuView.getLocationOnScreen(location);
        
        if (p.x< location[0] || p.y< location[1]) return 0;

        if (location[0] + mActionsMenuView.getWidth() < p.x || 
            location[1] + mActionsMenuView.getHeight() < p.y) return 0;

        return mActionsMenuView.testClick(p);
    }
}
