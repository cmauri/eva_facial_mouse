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

package com.crea_si.eviacam.slavemode;

import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.api.GamepadButtons;
import com.crea_si.eviacam.api.SlaveMode;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

/**
 * Layout for the absolute pad game controller
 */

public class GamepadView extends View implements OnSharedPreferenceChangeListener {
    // Ratio of the internal radius
    public static final float INNER_RADIUS_RATIO= 0.4f;
    
    private static final float OUTER_RADIUS_NORM_DEFAULT= 0.15f;

    private static final float BITMAP_RELATIVE_SIZE = 0.4f;

    /*
     * Main values for drawing gamepad (normalized to the shortest 
     * side of the canvas)
     */
    private PointF mPadCenterNorm= new PointF();
    private float mOuterRadiusNorm= OUTER_RADIUS_NORM_DEFAULT;

    // Transparency for painting
    private int mTransparency= 255;
    
    /*
     * Cached values
     */
    private float mCanvasWidth= 0;
    private float mCanvasHeight= 0;
    private float mPadCenterX= 0;
    private float mPadCenterY= 0;
    private float mOuterRadius= 0;
    private float mInnerRadius= 0;
    private Bitmap[] mPadArrows= new Bitmap[8];
    private Bitmap[] mPadArrowsPressed= new Bitmap[8];
    private PointF[] mPadArrowsLocation= new PointF[8];

    // When cache needs refresh
    private boolean mCacheNeedRefresh= true;
    
    // Cached paint box
    private final Paint mPaintBox;
    
    // Currently highlighted button
    private int mHighlightedButton= GamepadButtons.PAD_NONE;

    // Current operation mode
    private int mOperationMode;

    public GamepadView(Context c, int mode) {
        super(c);
        mOperationMode= mode;

        mPaintBox = new Paint();
        setWillNotDraw(false);

        // shared preferences
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
        updateSettings();
    }

    public void cleanup() {
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateSettings() {
        /*
         * Gamepad location
         */
        int l= Preferences.get().getGamepadLocation();

        if (l== Preferences.LOCATION_GAMEPAD_TOP_LEFT ||
            l== Preferences.LOCATION_GAMEPAD_TOP_CENTER ||
            l== Preferences.LOCATION_GAMEPAD_TOP_RIGHT) {
            mPadCenterNorm.y= 1.0f * (1.0f / 3.0f);
        }
        else {
            mPadCenterNorm.y= 2.0f * (1.0f / 3.0f);
        }
        
        if (l== Preferences.LOCATION_GAMEPAD_BOTTOM_LEFT ||
            l== Preferences.LOCATION_GAMEPAD_TOP_LEFT) {
            mPadCenterNorm.x= 1.0f * (1.0f / 4.0f);
        }
        else if (l== Preferences.LOCATION_GAMEPAD_BOTTOM_CENTER ||
                 l== Preferences.LOCATION_GAMEPAD_TOP_CENTER) {
            mPadCenterNorm.x= 2.0f * (1.0f / 4.0f);
        }
        else {
            mPadCenterNorm.x= 3.0f * (1.0f / 4.0f);
        }
        
        /*
         * Gamepad size
         */
        mOuterRadiusNorm= Preferences.get().getUIElementsSize() * OUTER_RADIUS_NORM_DEFAULT;
                
        /*
         * Gamepad alpha
         */
        mTransparency= (255 * Preferences.get().getGamepadTransparency()) / 100;
        
        mCacheNeedRefresh= true;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals(Preferences.KEY_GAMEPAD_LOCATION) ||
            key.equals(Preferences.KEY_GAMEPAD_TRANSPARENCY) ||
            key.equals(Preferences.KEY_UI_ELEMENTS_SIZE)) {
            updateSettings();
        }
    }

    private void updateCachedValues(Canvas canvas) {
        mCanvasWidth= canvas.getWidth();
        mCanvasHeight= canvas.getHeight();
        mPadCenterX= mCanvasWidth * mPadCenterNorm.x;
        mPadCenterY= mCanvasHeight * mPadCenterNorm.y;
        final float canvasShortSize= (mCanvasWidth< mCanvasHeight? mCanvasWidth : mCanvasHeight);
        mOuterRadius= canvasShortSize * mOuterRadiusNorm;
        mInnerRadius= canvasShortSize * mOuterRadiusNorm * INNER_RADIUS_RATIO;

        /*
         * Read original bitmaps from resources
         */
        BitmapDrawable bd = (BitmapDrawable) getContext().getResources().
                getDrawable(R.drawable.ic_pad_arrow_down);
        Bitmap downArrowOrig= bd.getBitmap();
        downArrowOrig.setDensity(Bitmap.DENSITY_NONE);

        bd = (BitmapDrawable) getContext().getResources().
                getDrawable(R.drawable.ic_pad_arrow_down_pressed);
        Bitmap downArrowPressedOrig= bd.getBitmap();
        downArrowPressedOrig.setDensity(Bitmap.DENSITY_NONE);

        /*
         * Compute radius and size of the bitmaps (assume bitmap width > length)
         */
        final float shortSide= (mOuterRadius - mInnerRadius) * BITMAP_RELATIVE_SIZE;
        final float scaling = shortSide / (float) bd.getIntrinsicHeight();
        final float longSide= scaling * bd.getIntrinsicWidth();
        final float bmpCenterRadius = mInnerRadius + (mOuterRadius - mInnerRadius) / 2.0f;

        /*
         * Scale to the desired size
         */
        Bitmap downArrowSized= Bitmap.createScaledBitmap(downArrowOrig, (int) longSide, (int) shortSide, true);
        downArrowSized.setDensity(Bitmap.DENSITY_NONE);

        Bitmap downArrowPressedSized= Bitmap.createScaledBitmap(downArrowPressedOrig, (int) longSide, (int) shortSide, true);
        downArrowPressedSized.setDensity(Bitmap.DENSITY_NONE);

        /*
         * Initialize arrays (position 0 is arrow down)
         */
        mPadArrows[0]= downArrowSized;
        mPadArrowsPressed[0]= downArrowPressedSized;
        mPadArrowsLocation[0]= new PointF(
                mPadCenterX - (float) downArrowSized.getWidth()/2.0f,
                mPadCenterY + bmpCenterRadius - (float) downArrowSized.getHeight()/2.0f);

        double alpha= Math.PI / 2.0 + Math.PI / 4.0;
        Matrix matrix = new Matrix();
        int rotate= 45;
        for (int i= 1; i< 8; i++) {
            matrix.reset();
            matrix.setRotate(rotate);

            Bitmap tb= Bitmap.createBitmap(downArrowSized, 0, 0, downArrowSized.getWidth(), downArrowSized.getHeight(), matrix, false);
            tb.setDensity(Bitmap.DENSITY_NONE);
            mPadArrows[i]= tb;

            tb= Bitmap.createBitmap(downArrowPressedSized, 0, 0, downArrowPressedSized.getWidth(), downArrowPressedSized.getHeight(), matrix, false);
            tb.setDensity(Bitmap.DENSITY_NONE);
            mPadArrowsPressed[i]= tb;

            mPadArrowsLocation[i]= new PointF(
                    mPadCenterX + bmpCenterRadius * (float) Math.cos(alpha) - (float) tb.getWidth() / 2.0f,
                    mPadCenterY + bmpCenterRadius * (float) Math.sin(alpha) - (float) tb.getHeight() / 2.0f);

            rotate+= 45;
            alpha+= Math.PI / 4.0;
        }

        mCacheNeedRefresh= false;
    }

    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);

        /*
         * Check whether the canvas has been resized and update cached values if so 
         */
        if (mCacheNeedRefresh || mCanvasWidth!= canvas.getWidth() || mCanvasHeight!= canvas.getHeight()) {
            updateCachedValues(canvas);
        }

        /*
         * Draw buttons (i.e. circle sectors)
         */
        mPaintBox.setAlpha(mTransparency);
        mPaintBox.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(mPadCenterX, mPadCenterY, mOuterRadius, mPaintBox);
        if (mOperationMode== SlaveMode.GAMEPAD_ABSOLUTE) {
            canvas.drawCircle(mPadCenterX, mPadCenterY, mInnerRadius, mPaintBox);
        }

        if (mOperationMode== SlaveMode.GAMEPAD_ABSOLUTE) {
            double alpha = Math.PI / 8.0;
            for (int i= 0; i< 2; i++) {
                final float cosAlpha = (float) Math.cos(alpha);
                final float sinAlpha = (float) Math.sin(alpha);
    
                final float startX= mInnerRadius * cosAlpha;
                final float stopX= mOuterRadius * cosAlpha;
                final float startY= mInnerRadius * sinAlpha;
                final float stopY= mOuterRadius * sinAlpha;
    
                canvas.drawLine(mPadCenterX + startX, mPadCenterY + startY,
                                mPadCenterX + stopX, mPadCenterY + stopY, mPaintBox);
                canvas.drawLine(mPadCenterX - startX, mPadCenterY + startY,
                                mPadCenterX - stopX, mPadCenterY + stopY, mPaintBox);
                canvas.drawLine(mPadCenterX + startX, mPadCenterY - startY,
                                mPadCenterX + stopX, mPadCenterY - stopY, mPaintBox);
                canvas.drawLine(mPadCenterX - startX, mPadCenterY - startY,
                                mPadCenterX - stopX, mPadCenterY - stopY, mPaintBox);
    
                alpha+= Math.PI / 4.0;
            }
        }

        /*
         * Draw arrows
         */
        for (int i= 0; i< 8; i++) {
            if (mHighlightedButton== i) {
                canvas.drawBitmap(mPadArrowsPressed[i], mPadArrowsLocation[i].x, mPadArrowsLocation[i].y, mPaintBox);
            }
            else {
                canvas.drawBitmap(mPadArrows[i], mPadArrowsLocation[i].x, mPadArrowsLocation[i].y, mPaintBox);
            }
            if (mOperationMode== SlaveMode.GAMEPAD_RELATIVE) i++; // 4 directions only
        }
    }

    /**
     * Convert relative pointer coordinates (in range [-1, 1]) to coordinates
     * relative to the size of the dpad and the overall canvas 
     * 
     * @param src source point
     * @param dst destination point (the value is modified)
     */
    void toCanvasCoords(PointF src, PointF dst) {
        dst.x= mOuterRadius * src.x + mPadCenterX;
        dst.y= mOuterRadius * src.y + mPadCenterY;
    }
    
    public void setOperationMode(int mode) {
        mOperationMode= mode;
        postInvalidate();
    }
    
    /**
     * Set the button which will drawn as highlighted (i.e. pressed)
     * 
     * @param button the id of the button (see GamepadAbs class)
     */
    void setHighlightedButton (int button) {
        mHighlightedButton= button;
    }
}
