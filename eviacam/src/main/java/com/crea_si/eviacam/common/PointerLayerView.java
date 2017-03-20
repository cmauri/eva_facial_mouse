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

package com.crea_si.eviacam.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;
import android.view.View;

import com.crea_si.eviacam.R;

/**
 * Layout to draw click progress feedback and pointer
 * 
 */

public class PointerLayerView extends View implements OnSharedPreferenceChangeListener {

    // Size of the long side of the pointer for normal size (in DIP)
    private static final float CURSOR_LONG_SIDE_DIP = 30;
    
    // Radius of the visual progress feedback (in DIP)
    private static final float PROGRESS_INDICATOR_RADIUS_DIP = 30;
    
    // default alpha value
    private static final int DEFAULT_ALPHA= 255;
    private final int DISABLED_ALPHA;
    
    // cached paint box
    private final Paint mPaintBox;
    
    // the location where the pointer needs to be painted
    private PointF mPointerLocation;
    
    // alpha value for drawing pointer
    private int mAlphaPointer= DEFAULT_ALPHA;
    
    // bitmap of the (mouse) pointer
    private Bitmap mPointerBitmap;
    
    // progress indicator radius in px
    private float mProgressIndicatorRadius;
    
    // click progress percent so far (0 disables)
    private int mClickProgressPercent= 0;
    
    public PointerLayerView(Context c) {
        super(c);
        
        mPaintBox = new Paint();
        setWillNotDraw(false);
        mPointerLocation= new PointF();

        DISABLED_ALPHA= c.getResources().getColor(R.color.disabled_alpha) >> 24;
        
        // preferences
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
        updateSettings();
    }
    
    public void cleanup() {
        Preferences.get().getSharedPreferences().
            unregisterOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals(Preferences.KEY_UI_ELEMENTS_SIZE) ||
            key.equals(Preferences.KEY_GAMEPAD_TRANSPARENCY)) {
            updateSettings();
        }
    }
    
    private void updateSettings() {
        float size= Preferences.get().getUIElementsSize();

        mAlphaPointer= (255 * Preferences.get().getGamepadTransparency()) / 100;
        
        // re-scale pointer accordingly
        BitmapDrawable bd = (BitmapDrawable)
                getContext().getResources().getDrawable(R.drawable.pointer);
        Bitmap origBitmap= bd.getBitmap();
        origBitmap.setDensity(Bitmap.DENSITY_NONE);
        
        // desired long side in pixels of the pointer for this screen density
        float longSide= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                CURSOR_LONG_SIDE_DIP, getResources().getDisplayMetrics()) * size;
        float scaling = longSide / (float) bd.getIntrinsicHeight();
        float shortSide= scaling * bd.getIntrinsicWidth();

        mPointerBitmap= Bitmap.createScaledBitmap(origBitmap, (int) shortSide, (int) longSide, true);
        mPointerBitmap.setDensity(Bitmap.DENSITY_NONE);
        
        // compute radius of progress indicator in px
        mProgressIndicatorRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                PROGRESS_INDICATOR_RADIUS_DIP, getResources().getDisplayMetrics()) * size;
    }
    
    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        
        // draw progress indicator
        if (mClickProgressPercent> 0) {
            float radius= ((float)
                    (100 - mClickProgressPercent) * mProgressIndicatorRadius) / 100.0f;

            mPaintBox.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaintBox.setColor(0x80000000);
            canvas.drawCircle(mPointerLocation.x, mPointerLocation.y, radius, mPaintBox);

            mPaintBox.setStyle(Paint.Style.STROKE);
            mPaintBox.setColor(0x80FFFFFF);
            canvas.drawCircle(mPointerLocation.x, mPointerLocation.y, radius, mPaintBox);
        }
        
        // draw pointer
        mPaintBox.setAlpha(mAlphaPointer);
        canvas.drawBitmap(mPointerBitmap, mPointerLocation.x, mPointerLocation.y, mPaintBox);
    }

    public void updatePosition(PointF p) {
        mPointerLocation.x= p.x;
        mPointerLocation.y= p.y;
    }

    public void updateClickProgress(int percent) {
        mClickProgressPercent= percent;
    }
    
    /**
     * Enable or disable faded appearance of the pointer for the rest mode
     * 
     * @param value true for faded appearance
     */
    public void setRestModeAppearance(boolean value) {
        mAlphaPointer= (value? DISABLED_ALPHA : DEFAULT_ALPHA);
    }
}
