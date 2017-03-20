package com.crea_si.eviacam.api_demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

public class MyCanvas extends View {
    // Size of the pointer
    private static final float CURSOR_RADIUS = 10;

    // cached paint box
    private final Paint mPaintBox= new Paint();
    
    // the location where the pointer needs to be painted
    private final PointF mLocation= new PointF();
    
    public MyCanvas(Context c) {
        super(c);
        setWillNotDraw(false);
    }
    
    public MyCanvas (Context c, AttributeSet attrs) {
        super(c, attrs);
        setWillNotDraw(false);
    }
    
    public MyCanvas(Context c, AttributeSet attrs, int defStyleAttr) {
        super(c, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        
        mPaintBox.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(mLocation.x, mLocation.y, CURSOR_RADIUS, mPaintBox);
    }

    public void setPositionNorm(PointF p) {
        mLocation.x= ((p.x / 2.0f) * getWidth()) + getWidth() / 2;
        mLocation.y= ((p.y / 2.0f) * getHeight()) + getHeight() / 2;
        
        if (mLocation.x< 0) mLocation.x= 0;
        else if (mLocation.x>= getWidth()) mLocation.x= getWidth()-1;
        
        if (mLocation.y< 0) mLocation.y= 0;
        else if (mLocation.y>= getHeight()) mLocation.y= getHeight()-1;
    }

    public void setPosition(PointF p) {
        mLocation.set(p);
    }
}
