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

package com.crea_si.eviacam.service;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

public class ScrollLayerView extends RelativeLayout {
    
    /** */
    public class NodeAction {
        public AccessibilityNodeInfo node;
        public int actions;
        
        NodeAction(AccessibilityNodeInfo n, int a) {
            this.node= n;
            this.actions= a;
        }
    }
    
    /** Class to store button with a NodeAction */
    private class ButtonNodeAction {
        public AccessibilityNodeInfo node;
        public ImageButton buttonForward;
        public ImageButton buttonBackward;
    }
    
    /** Button size in device pixels */
    private static final int SCROLL_BUTTON_WIDTH_DP= 28;
    private static final int SCROLL_BUTTON_HEIGHT_DP= 28;
    
    /** Button size in pixels */
    private final int SCROLL_BUTTON_WIDTH;
    private final int SCROLL_BUTTON_HEIGHT;
    
    /** Icon bitmaps */
    private Bitmap mScrollBackwardIcon;
    private Bitmap mScrollForwardIcon;
    
    // scroll areas
    private List<ButtonNodeAction> mScrollAreas = new ArrayList<ButtonNodeAction>();
    private int mScrollAreasCount = 0;
    
    public ScrollLayerView(Context context) {
        super(context);
        
        // size of icons
        SCROLL_BUTTON_WIDTH= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                SCROLL_BUTTON_WIDTH_DP, getResources().getDisplayMetrics());
        SCROLL_BUTTON_HEIGHT= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                SCROLL_BUTTON_HEIGHT_DP, getResources().getDisplayMetrics());
        
        // scroll backward icon
        Drawable d= context.getResources().getDrawable(R.drawable.scrollback_icon);
        mScrollBackwardIcon= Bitmap.createScaledBitmap(
                ((BitmapDrawable) d).getBitmap(), SCROLL_BUTTON_WIDTH, SCROLL_BUTTON_HEIGHT, true);

        // scroll forward icon
        d= context.getResources().getDrawable(R.drawable.scrollfor_icon);
        mScrollForwardIcon= Bitmap.createScaledBitmap(
                ((BitmapDrawable) d).getBitmap(), SCROLL_BUTTON_WIDTH, SCROLL_BUTTON_HEIGHT, true);
    }
    
    /**
     * Gets the NodeAction pair containing a given point
     * 
     * @param p - the point in screen coordinates
     * @return a NodeAction object if found, null otherwise
     * 
     * Remarks: this method might be called safely from a secondary thread 
     */
    public synchronized NodeAction getContaining(Point p)  {
        for (int i= 0; i< mScrollAreasCount; i++) {
            ButtonNodeAction bna= mScrollAreas.get(i);
            if ((bna.buttonBackward.getVisibility() == View.VISIBLE) &&
                    ViewUtils.isPointInsideView(p, bna.buttonBackward)) {
                return new NodeAction(bna.node, AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            }
            if ((bna.buttonForward.getVisibility() == View.VISIBLE) &&
                    ViewUtils.isPointInsideView(p, bna.buttonForward)) {
                return new NodeAction(bna.node, AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            }
        }

        return null;
    }
    
    /** Remove all scrollable areas */
    public synchronized void clearScrollAreas() {
        // Do not erase buttons, just hide them for reuse
        for (int i= 0; i< mScrollAreasCount; i++) {
            ButtonNodeAction bna= mScrollAreas.get(i);
            bna.node = null;
            bna.buttonBackward.setVisibility(View.GONE);
            bna.buttonForward.setVisibility(View.GONE);
        }
        
        mScrollAreasCount= 0;
    }
    
    /**
     * Add a scrollable area
     *  
     * @param node - the node of the scrollable area
     */
    public synchronized void addScrollArea(AccessibilityNodeInfo node) {
        if (node == null || !node.isScrollable()) return;

        /** Grow the list when necessary */
        if (mScrollAreasCount>= mScrollAreas.size()) {
            mScrollAreas.add(new ButtonNodeAction());
        }
        
        // Pick last element from the list
        ButtonNodeAction bna= mScrollAreas.get(mScrollAreasCount++);
        
        bna.node= node;
        
        /** Create buttons if needed */        
        if (bna.buttonBackward == null) {
            bna.buttonBackward = new ImageButton(this.getContext());
            bna.buttonBackward.setBackgroundColor(
                    getResources().getColor(R.color.half_alpha));
            bna.buttonBackward.setContentDescription(
                    getContext().getText(R.string.scroll_backward));
            RelativeLayout.LayoutParams rlp= new RelativeLayout.LayoutParams(
                    SCROLL_BUTTON_WIDTH, SCROLL_BUTTON_HEIGHT);
            bna.buttonBackward.setLayoutParams(rlp);
            bna.buttonBackward.setImageBitmap(mScrollBackwardIcon);
            this.addView(bna.buttonBackward);
        }
        if (bna.buttonForward == null) {
            bna.buttonForward = new ImageButton(this.getContext());
            bna.buttonForward.setBackgroundColor(
                    getResources().getColor(R.color.half_alpha));
            bna.buttonForward.setContentDescription(
                    getContext().getText(R.string.scroll_forward));
            RelativeLayout.LayoutParams rlp= new RelativeLayout.LayoutParams(
                    SCROLL_BUTTON_WIDTH, SCROLL_BUTTON_HEIGHT);
            bna.buttonForward.setLayoutParams(rlp);
            bna.buttonForward.setImageBitmap(mScrollForwardIcon);
            this.addView(bna.buttonForward);
        }
        
        /** Set visibility and position */
        Rect tmpRect = new Rect();
        node.getBoundsInScreen(tmpRect);
        
        if ((node.getActions() & AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) != 0) {
            RelativeLayout.LayoutParams rlp= (RelativeLayout.LayoutParams) 
                    bna.buttonBackward.getLayoutParams();
            rlp.leftMargin = tmpRect.left;
            rlp.topMargin = tmpRect.top;
            bna.buttonBackward.setLayoutParams(rlp);
            bna.buttonBackward.setVisibility(View.VISIBLE);
        }
        else {
            bna.buttonBackward.setVisibility(View.GONE);
        }
        
        if ((node.getActions() & AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) != 0) {
            RelativeLayout.LayoutParams rlp= (RelativeLayout.LayoutParams) 
                    bna.buttonForward.getLayoutParams();
            rlp.leftMargin = tmpRect.left + tmpRect.width() - SCROLL_BUTTON_WIDTH;
            rlp.topMargin = tmpRect.top + tmpRect.height() - SCROLL_BUTTON_HEIGHT;
            bna.buttonForward.setLayoutParams(rlp);
            bna.buttonForward.setVisibility(View.VISIBLE);
        }
        else {
            bna.buttonForward.setVisibility(View.GONE);
        }
    }
}
