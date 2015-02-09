package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PointF;
import android.widget.RelativeLayout;

public class OverlayView extends RelativeLayout {
	Paint mPaintBox;
	PointF mPointerLocation;
    
    public OverlayView(Context context) {
        super(context);
        
        mPaintBox = new Paint();
        setWillNotDraw(false);
        mPointerLocation= new PointF();
    }
    
    public void updatePointerLocation(PointF p) {
        mPointerLocation.x= p.x;
        mPointerLocation.y= p.y;
        this.postInvalidate();
    }
	
	public void onDraw(Canvas canvas){
	    super.onDraw(canvas);
	    
	    final int COLOR = Color.parseColor("#0099cc");
        final int ALPHA_EMPTY = 255;
        final int STROKE_WIDTH = 8;
        
        mPaintBox.setColor(COLOR);
        mPaintBox.setAlpha(ALPHA_EMPTY);
        mPaintBox.setStrokeWidth(STROKE_WIDTH);
        mPaintBox.setStyle(Style.STROKE);
        
	    // draw pointer
	    canvas.drawCircle(mPointerLocation.x, mPointerLocation.y, 2, mPaintBox);
	    
		// Draw something
	    //canvas.drawRect(10, 10, 100, 100, mPaintBox);
	}	
}

