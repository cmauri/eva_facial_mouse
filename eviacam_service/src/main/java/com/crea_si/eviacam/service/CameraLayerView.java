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

 package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;

public class CameraLayerView extends RelativeLayout {
    /*
     * Class to draw the border. Done this way so that
     * only this view needs to be invalidated
     */
    private class BorderDrawer extends View {
        // cached paint box
        private final Paint mPaint;

        public BorderDrawer(Context context) {
            super(context);

            mPaint= new Paint();
            mPaint.setColor(BEGIN_COLOR);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(BORDER_SIZE);
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            /* Draw a border around the camera surface */
            if (mCameraSurfaceView!= null) {
                float x= mCameraSurfaceView.getX();
                float y= mCameraSurfaceView.getY();

                mPaint.setColor(mPaintColor);
                canvas.drawRect(
                        x-BORDER_SIZE/2,
                        y,
                        x+CAM_SURFACE_WIDTH+BORDER_SIZE/2,
                        y+CAM_SURFACE_HEIGHT+BORDER_SIZE/2, mPaint);
            }
        }
    }

    /*
     * camera viewer size
     */
    private static final int CAM_SURFACE_WIDTH= 64;
    private static final int CAM_SURFACE_HEIGHT= 48;

    /*
     * Constants for the detector status
     */
    private static final int BORDER_SIZE= 4;
    private static final int BEGIN_COLOR= 0xffa5151f;
    private static final int END_COLOR= 0xffffffff; //f5afb4;
    private int mPaintColor= BEGIN_COLOR;

    // the camera surface view
    private SurfaceView mCameraSurfaceView;

    private final View mBorderDrawer;

    // constructor
    public CameraLayerView(Context context) {
        super(context);
        mBorderDrawer= new BorderDrawer(context);
        addView(mBorderDrawer);
    }

    /**
     * Add a surface in wich the image from the camera will be displayed
     *
     * @param v a surface view
     */
    public void addCameraSurface(SurfaceView v) {
        mCameraSurfaceView= v;

        // set layout and add to parent
        RelativeLayout.LayoutParams lp= 
                new RelativeLayout.LayoutParams(CAM_SURFACE_WIDTH, CAM_SURFACE_HEIGHT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        v.setLayoutParams(lp);

        this.addView(v);
    }

    /**
     * Set the value since last detection of the face to provide feedback to the user
     *
     * @param v a value between 0 and 100
     *                0: a face has just been detected
     *              100: detection time has expired
     */
    public void updateFaceDetectorStatus(int v) {
        int r= (Color.red(BEGIN_COLOR) * (100 - v) + Color.red(END_COLOR) * v) / 100;
        int g= (Color.green(BEGIN_COLOR) * (100 - v) + Color.green(END_COLOR) * v) / 100;
        int b= (Color.blue(BEGIN_COLOR) * (100 - v) + Color.blue(END_COLOR) * v) / 100;

        mPaintColor= Color.argb(0xff, r, g ,b);
        mBorderDrawer.postInvalidate();     // called from a secondary thread
    }
}
