package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

/**
 * Layout to draw click progress feedback and pointer
 * 
 */

public class PointerLayerView extends View {
    
    // cached paint box
    private final Paint mPaintBox;
    
    // the location where the pointer needs to be painted
    private PointF mPointerLocation;
    
    // bitmap of the (mouse) pointer
    private Bitmap mPointerBitmap;
    
    // click progress percent so far (0 disables)
    private int mClickProgressPercent= 0;
    
    public PointerLayerView(Context context) {
        super(context);
        
        mPaintBox = new Paint();
        setWillNotDraw(false);
        mPointerLocation= new PointF();
        Drawable d = context.getResources().getDrawable(R.drawable.pointer);
        mPointerBitmap =((BitmapDrawable) d).getBitmap();
    }
    
    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        
        // draw progress indicator
        if (mClickProgressPercent> 0) {
            // TODO: use device independent pixels and improve esthetics
            float radius= ((float) (100 - mClickProgressPercent) / 100.0f) * 30;
            mPaintBox.setAlpha(127);
            canvas.drawCircle(mPointerLocation.x, mPointerLocation.y, radius, mPaintBox);
            mPaintBox.setAlpha(255);
        }
        
        // draw pointer
        canvas.drawBitmap(mPointerBitmap, mPointerLocation.x, mPointerLocation.y, mPaintBox);
    }

    public void updatePosition(PointF p) {
        mPointerLocation.x= p.x;
        mPointerLocation.y= p.y;
    }

    public void updateClickProgress(int percent) {
        mClickProgressPercent= percent;
    }	
}
