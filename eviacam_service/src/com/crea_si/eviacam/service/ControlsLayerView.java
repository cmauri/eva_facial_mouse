package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Point;
import android.view.View;
import android.widget.RelativeLayout;

public class ControlsLayerView extends RelativeLayout {
    // view of the pointer context menu 
    ContextMenuView mContextMenuView;
        
    public ControlsLayerView(Context context) {
        super(context);
        
        // create and add buttons. initially these are hidden.
        mContextMenuView= new ContextMenuView(context);
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, 
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        mContextMenuView.setLayoutParams(lp);
        mContextMenuView.setVisibility(View.GONE);
        addView(mContextMenuView);
    }
    
    public void showContextMenu(final Point p, final int actions) {
        // TODO: when the screen is rotated the menu vanishes
        
        this.post(new Runnable() {
            @Override
            public void run() {
                if (actions == 0) {
                    mContextMenuView.setVisibility(View.GONE);
                    setBackgroundColor(0);
                    return;
                }
                
                // set context menu view as visible
                mContextMenuView.setVisibility(View.VISIBLE);
                
                // update buttons list that need to display
                mContextMenuView.updateButtons(actions);
                      
                // dim background
                setBackgroundColor(0x80000000);
                
                /*
                 * compute position in which actions menu will be displayed
                 * 
                 * it first tries to display it below and at the left of the pointer.
                 * if this is not possible move to the other side
                 */
                RelativeLayout.LayoutParams lp= (RelativeLayout.LayoutParams) 
                        mContextMenuView.getLayoutParams();
                
                // get actions menu expected width
                int expectedWidth= mContextMenuView.getMeasuredWidth();
                
                // fits at the left of the pointer?
                if (p.x - expectedWidth> 0) lp.leftMargin= p.x - expectedWidth;
                else lp.leftMargin= p.x;
                
                // get actions menu expected height
                int expectedHeight= mContextMenuView.getMeasuredHeight();
                
                // fits below the pointer?
                if (p.y + expectedHeight< getHeight()) lp.topMargin= p.y;
                else lp.topMargin= p.y - expectedHeight;

                // set menu position
                mContextMenuView.setLayoutParams(lp);
            }
        });
    }
    
    public void hideContextMenu() {
        showContextMenu(null, 0);
    }
    
    /*
     * test if one button has been clicked
     */
    public int testClick (Point p)  {
        int[] location= new int[2];
        
        mContextMenuView.getLocationOnScreen(location);
        
        if (p.x< location[0] || p.y< location[1]) return 0;

        if (location[0] + mContextMenuView.getWidth() < p.x || 
            location[1] + mContextMenuView.getHeight() < p.y) return 0;

        return mContextMenuView.testClick(p);
    }
    
    public void populateContextMenu (int action, int labelId) {
        mContextMenuView.populateAction(action, labelId);
    }
}
