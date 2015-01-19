package com.crea_si.eviacam.service;

import android.content.Context;
import android.widget.LinearLayout;

public class MaxWidthLinearLayout extends LinearLayout {
    private int mMaxWidth;
    private int mMaxHeight;

    public MaxWidthLinearLayout(Context context) {
        super(context);
        mMaxWidth = 0;
    }

    void setMaxWidth(int maxWidth) {
        mMaxWidth= maxWidth;
    }
    
    void setMaxHeight(int maxHeight) {
        mMaxHeight= maxHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (mMaxWidth > 0 && mMaxWidth < measuredWidth) {
            int measureMode = MeasureSpec.getMode(widthMeasureSpec);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, measureMode);
        }
        
        
        // Adjust height as necessary
        int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);
        if(mMaxHeight > 0 && mMaxHeight < measuredHeight) {
            int measureMode = MeasureSpec.getMode(heightMeasureSpec);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, measureMode);
        }

        
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}