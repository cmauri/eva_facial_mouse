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
    // remaining time below which need to start blinking (in milliseconds)
    private static final int BLINK_TIMEOUT= 3200;

    // (half) period of the blinking
    private static final int BLINK_INTERVAL = 400;

    // blinking management
    private final Blink mBlink= new Blink(BLINK_INTERVAL);

    // show detection feeback?
    private boolean mShowDetectionFeedback= true;

    /*
     * camera viewer size
     */
    private static final int CAM_SURFACE_WIDTH= 80;
    private static final int CAM_SURFACE_HEIGHT= 60;

    /*
     * Constants for the detector status
     */
    private static final int BORDER_SIZE= 4;
    private static final int BEGIN_COLOR= 0xffa5151f;
    private static final int END_COLOR= 0xffffffff; //f5afb4;
    private int mPaintColor= END_COLOR;
    private static final long FADE_TIME= 5000;

    // the camera surface view
    private SurfaceView mCameraSurfaceView;

    private final View mBorderDrawer;

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
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(BORDER_SIZE);
        }

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            /* Draw a border around the camera surface */
            if (mCameraSurfaceView!= null && mShowDetectionFeedback) {
                float x= mCameraSurfaceView.getX();
                float y= mCameraSurfaceView.getY();

                if (mBlink.getState())
                    mPaint.setColor(mPaintColor);
                else
                    mPaint.setColor(Color.BLACK);

                canvas.drawRect(
                        x - BORDER_SIZE / 2,
                        y,
                        x + CAM_SURFACE_WIDTH + BORDER_SIZE / 2,
                        y + CAM_SURFACE_HEIGHT + BORDER_SIZE / 2, mPaint);
            }
        }
    }

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
     * @param countdown a Countdown object
     */
    public void updateFaceDetectorStatus(FaceDetectionCountdown countdown) {
        /*
         * Color of the border. Fade BEGIN_COLOR into END_COLOR throughout FADE_TIME
         */
        // Elapsed time since last face detection
        long elapsed= countdown.getElapsedTime();
        if (elapsed>= FADE_TIME) elapsed= FADE_TIME;
        int perc= (int) ((elapsed * 100) / FADE_TIME);
        int r = (Color.red(BEGIN_COLOR) * (100 - perc) + Color.red(END_COLOR) * perc) / 100;
        int g = (Color.green(BEGIN_COLOR) * (100 - perc) + Color.green(END_COLOR) * perc) / 100;
        int b = (Color.blue(BEGIN_COLOR) * (100 - perc) + Color.blue(END_COLOR) * perc) / 100;
        mPaintColor = Color.argb(0xff, r, g, b);

        /*
         * Blinking control. When countdown is disable means that there is no
         * no auto-off feature, this blinking is not used
         */
        if (countdown.isDisabled() || countdown.getElapsedPercent() == 100 ||
            countdown.getRemainingTime() > BLINK_TIMEOUT) {
            mBlink.stop();
        } else {
            mBlink.start();
        }

        // redraw. called from a secondary thread
        mBorderDrawer.postInvalidate();
    }

    public void showDetectionFeedback() { mShowDetectionFeedback= true; }

    public void hideDetectionFeedback() { mShowDetectionFeedback= false; }
}
