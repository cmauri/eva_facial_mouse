package com.crea_si.eviacam.service;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;

class ActionsButtonsView extends LinearLayout {
    // track last used mask to avoid unnecessary operations
    private int mActionsMask= 0;
    
    // references to individual buttons
    private View mButtonClick;
    private View mButtonLongClick;
    
    public ActionsButtonsView(Context context) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        
        /*
         * create buttons, add to layout and make invisible (gone)
         */
        Resources res= getResources();
        mButtonClick= createButton(res.getString(R.string.click));
        mButtonLongClick= createButton(res.getString(R.string.long_click));
        
        addView(mButtonClick);
        addView(mButtonLongClick);
        
        mButtonClick.setVisibility(View.GONE);
        mButtonLongClick.setVisibility(View.GONE);
    }
    
    private View createButton(CharSequence text) {
        Button b= new Button (this.getContext());
        b.setText(text);
        return b;        
    }
    
    void updateButtons (int actions) {
        if (actions == mActionsMask) return;
        
        if ((actions & AccessibilityNodeInfo.ACTION_CLICK) != 0) {
            mButtonClick.setVisibility(View.VISIBLE);
        }
        else {
            mButtonClick.setVisibility(View.GONE);
        }
        
        if ((actions & AccessibilityNodeInfo.ACTION_LONG_CLICK) != 0) {
            mButtonLongClick.setVisibility(View.VISIBLE);
        }
        else {
            mButtonLongClick.setVisibility(View.GONE);
        }
        
        // measure how much space will need
        measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        
        mActionsMask= actions;
    }
}
