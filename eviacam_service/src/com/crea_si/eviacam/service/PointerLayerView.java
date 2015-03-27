package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

public class PointerLayerView extends View {
    Paint mPaintBox;
    PointF mPointerLocation;
    Bitmap mPointerBitmap;
    
    public PointerLayerView(Context context) {
        super(context);
        
        mPaintBox = new Paint();
        setWillNotDraw(false);
        mPointerLocation= new PointF();
        Drawable d = context.getResources().getDrawable(R.drawable.pointer);
        mPointerBitmap =((BitmapDrawable) d).getBitmap();
        
        EVIACAM.debug("Bitmap size: " + mPointerBitmap.getWidth() + ", " + mPointerBitmap.getHeight());
    }
    
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        
        // draw pointer
        canvas.drawBitmap(mPointerBitmap, mPointerLocation.x, mPointerLocation.y, mPaintBox);
    }

    public void updatePosition(PointF p) {
        mPointerLocation.x= p.x;
        mPointerLocation.y= p.y;
        this.postInvalidate();
    }

    public void updateCountdown(int percent) {
        // TODO Auto-generated method stub
        
    }	
}
