/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.crea_si.eviacam.a11yservice;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Fullscreen view for the pointer contextual (pop up) menu
 */
public class ContextMenuLayerView extends RelativeLayout {
    // view of the pointer context menu 
    private ContextMenuView mContextMenuView;
        
    public ContextMenuLayerView(@NonNull Context context) {
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

    public void cleanup() {
        mContextMenuView.cleanup();
        mContextMenuView= null;
    }
    
    public void showContextMenu(@Nullable final Point p, final int actions) {
        // TODO: when the screen is rotated the menu vanishes
        
        this.post(new Runnable() {
            @Override
            public void run() {
                // Prevent crash due to a race condition on closing
                if (mContextMenuView== null) return;

                if (actions == 0 || p == null) {
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
                int expectedWidth= mContextMenuView.getMeasuredWidthScaled();
                
                // fits at the left of the pointer?
                if (p.x - expectedWidth> 0) lp.leftMargin= p.x - expectedWidth;
                else lp.leftMargin= p.x;
                
                // get actions menu expected height
                int expectedHeight= mContextMenuView.getMeasuredHeightScaled();
                
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
    
    /**
     * Test if one button has been clicked
     */
    public int testClick (@NonNull Point p)  {
        return mContextMenuView.testClick(p);
    }
    
    public void populateContextMenu (int action, int labelId) {
        mContextMenuView.populateAction(action, labelId);
    }
}
