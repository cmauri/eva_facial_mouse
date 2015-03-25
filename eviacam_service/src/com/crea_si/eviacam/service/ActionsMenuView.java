package com.crea_si.eviacam.service;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;

class ActionsMenuView extends LinearLayout {
    
    class ActionButton {
        int action;
        Button button;
        ActionButton (int a, int textResource, Context c) {
            action= a;
            Resources res= getResources();
            button= new Button (c);
            button.setText(res.getString(textResource));
            button.setVisibility(View.GONE);
        }
    }
    
    // track last used mask to avoid unnecessary operations
    private int mActionsMask= 0;
    
    // references to actions and buttons pairs
    ActionButton[] mActionButtons;
    
    public ActionsMenuView(Context c) {
        super(c);
        setOrientation(LinearLayout.VERTICAL);
        
        /*
         * create buttons, add to layout and make invisible (gone)
         */
    /*    
        AccessibilityNodeInfo.ACTION_CLICK | 
        AccessibilityNodeInfo.ACTION_LONG_CLICK |
        AccessibilityNodeInfo.ACTION_COLLAPSE |
        AccessibilityNodeInfo.ACTION_COPY |
        AccessibilityNodeInfo.ACTION_CUT |
        AccessibilityNodeInfo.ACTION_DISMISS |
        AccessibilityNodeInfo.ACTION_EXPAND |
        AccessibilityNodeInfo.ACTION_PASTE |
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD |
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD |
        AccessibilityNodeInfo.ACTION_SELECT;
      */  
        mActionButtons= new ActionButton[]{
            new ActionButton(AccessibilityNodeInfo.ACTION_CLICK, R.string.click, c),
            new ActionButton(AccessibilityNodeInfo.ACTION_LONG_CLICK, R.string.long_click, c)
        };
        
        for (int i= 0; i< mActionButtons.length; i++) {
            addView(mActionButtons[i].button);
        }
    }
    
    void updateButtons (int actions) {
        if (actions == mActionsMask) return;
        
        for (int i= 0; i< mActionButtons.length; i++) {
            if ((actions & mActionButtons[i].action) != 0) {
                mActionButtons[i].button.setVisibility(View.VISIBLE);
            }
            else {
                mActionButtons[i].button.setVisibility(View.GONE);                
            }
        }        
      
        // measure how much space will need
        measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        
        mActionsMask= actions;
    }

    static 
    private boolean testClick (View v, Point p) {
        int[] location= new int[2];
        
        v.getLocationOnScreen(location);
        
        if (p.x< location[0] || p.y< location[1]) return false;

        if (location[0] + v.getWidth() < p.x || 
            location[1] + v.getHeight() < p.y) return false;
        
        return true;
    }
    
    public int testClick (Point p)  {
        for (int i= 0; i< mActionButtons.length; i++) {
            if (mActionButtons[i].button.getVisibility() == View.VISIBLE && 
                testClick(mActionButtons[i].button, p)) {

                return mActionButtons[i].action;
            }
        }
       
        return 0;
    }
}
