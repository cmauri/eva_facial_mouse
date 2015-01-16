package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.widget.LinearLayout;

public class FeedbackOverlayView extends LinearLayout {
	Paint mPaintBox;
    
    public FeedbackOverlayView(Context context) {
        super(context);
        
        mPaintBox = new Paint();
    }
	
	public void onDraw(Canvas canvas){
	    super.onDraw(canvas);
	    
		// Draw something
	    final int COLOR = Color.parseColor("#0099cc");
	    final int ALPHA_EMPTY = 255;
	    final int STROKE_WIDTH = 8;
        mPaintBox.setColor(COLOR);
        mPaintBox.setAlpha(ALPHA_EMPTY);
        mPaintBox.setStyle(Style.STROKE);
        mPaintBox.setStrokeWidth(STROKE_WIDTH);
	    canvas.drawRect(10, 10, 100, 100, mPaintBox);
	}	
}

