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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ImageView.ScaleType;

/**
 * Layer to draw scrolling buttons
 *  
 */
public class ScrollLayerView extends RelativeLayout implements OnSharedPreferenceChangeListener {
    
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
    private static final int SCROLL_BUTTON_WIDTH_DP= 30;
    private static final int SCROLL_BUTTON_HEIGHT_DP= 30;
    private static final int SCROLL_BUTTON_PADDING_DP= 2;
    
    /** Factor to scale the size of the buttons */
    private float mSizeMultiplier = 1.0f;
    
    // scroll areas
    private List<ButtonNodeAction> mScrollAreas = new ArrayList<ButtonNodeAction>();
    private int mScrollAreasCount = 0;
    
    public ScrollLayerView(Context c) {
        super(c);
        
        // Preferences
        SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(c);
        sp.registerOnSharedPreferenceChangeListener(this);
        updateSettings(sp);
    }
    
    private synchronized void updateSettings(SharedPreferences sp) {
        mSizeMultiplier= Settings.getUIElementsSize(sp);

        // Force buttons full refresh
        clearScrollAreas();
        mScrollAreas.clear();
    }
    
    public void cleanup() {
        PreferenceManager.getDefaultSharedPreferences(this.getContext()).
            unregisterOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals(Settings.KEY_UI_ELEMENTS_SIZE)) {
            updateSettings(sp);
        }
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
    
    private int getScrollButtonWidth() {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                SCROLL_BUTTON_WIDTH_DP, getResources().getDisplayMetrics()) * mSizeMultiplier);
    }
    
    private int getScrollButtonHeight() {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                SCROLL_BUTTON_HEIGHT_DP, getResources().getDisplayMetrics()) * mSizeMultiplier);
    }
    
    /** Create a scrolling button */
    private ImageButton createScrollButton(Drawable d) {
        ImageButton b= new ImageButton(getContext());
        b.setBackgroundColor(getResources().getColor(R.color.half_alpha));
        b.setContentDescription(getContext().getText(R.string.scroll_backward));
        
        int padding_px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                SCROLL_BUTTON_PADDING_DP, getResources().getDisplayMetrics());
        
        RelativeLayout.LayoutParams rlp= new 
                RelativeLayout.LayoutParams(getScrollButtonWidth(), getScrollButtonHeight());
        b.setLayoutParams(rlp);
        b.setScaleType(ScaleType.FIT_CENTER);
        b.setPadding(padding_px, padding_px, padding_px, padding_px);
        b.setImageDrawable(d);
        
        return b;
    }
    
    /** Check if two rectangles overlap */
    static private boolean overlaps (int x1, int x2, int y1, int y2, int width, int height) {
        return y1 < y2 + height && y1 + height > y2 && x1 < x2 + width && x1 + width > x2;
    }
    
    /** Set the position of a button */
    private void setButtonPosition(ImageButton b, int x, int y) {
        RelativeLayout.LayoutParams rlp= (RelativeLayout.LayoutParams) b.getLayoutParams();
        rlp.leftMargin = x;
        rlp.topMargin = y;
        b.setLayoutParams(rlp);
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
        final ButtonNodeAction bna= mScrollAreas.get(mScrollAreasCount++);
        bna.node= node;
        
        /** Create buttons if needed */        
        if (bna.buttonBackward == null) {
            Drawable d= getContext().getResources().getDrawable(R.drawable.scrollback_icon);
            bna.buttonBackward= createScrollButton(d);
            addView(bna.buttonBackward);
        }
        if (bna.buttonForward == null) {
            Drawable d= getContext().getResources().getDrawable(R.drawable.scrollfor_icon);
            bna.buttonForward= createScrollButton(d);
            addView(bna.buttonForward);
        }
        
        /** 
         * Set visibility and position of the scroll buttons
         * 
         * We take into account whether they are really needed (i.e. support the corresponding
         * action) and the position of the buttons placed previously. We just move the button
         * down (or up) if it overlaps with previously placed buttons. This does not guarantee
         * that buttons will never overlap, but will probably work in most cases.
         */
        final Rect tmpRect = new Rect();
        node.getBoundsInScreen(tmpRect);
        
        final int width = getScrollButtonWidth();
        final int height = getScrollButtonHeight();
        
        final int actions = node.getActions();
        
        /** Scroll backward buttons */
        if ((actions & AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) != 0) {
            final int x = tmpRect.left;
            int y = tmpRect.top;

            for (int i= 0; i< mScrollAreasCount; i++) {
                ImageButton other_button= mScrollAreas.get(i).buttonBackward;
                if (other_button.getVisibility() != View.VISIBLE) continue;
                if (overlaps(x, other_button.getLeft(), y, other_button.getTop(), width, height)) {
                    y+= height; // move down
                }
            }

            setButtonPosition(bna.buttonBackward, x, y);
            bna.buttonBackward.setVisibility(View.VISIBLE);
        }
        else {
            bna.buttonBackward.setVisibility(View.GONE);
        }
        
        /** Scroll forward buttons */
        if ((actions & AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) != 0) {
            final int x = tmpRect.right - width;
            int y = tmpRect.bottom - height;
        
            for (int i= 0; i< mScrollAreasCount; i++) {
                ImageButton other_button= mScrollAreas.get(i).buttonForward;
                if (other_button.getVisibility() != View.VISIBLE) continue;
                if (overlaps(x, other_button.getLeft(), y, other_button.getTop(), width, height)) {
                    y-= height; // move up
                }
            }
            
            setButtonPosition(bna.buttonForward, x, y);
            bna.buttonForward.setVisibility(View.VISIBLE);
        }
        else {
            bna.buttonForward.setVisibility(View.GONE);
        }
    }
}
