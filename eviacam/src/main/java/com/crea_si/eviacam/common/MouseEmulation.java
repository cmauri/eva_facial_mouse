/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
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
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.view.View;

public class MouseEmulation implements MotionProcessor {
    /*
     * states of the engine
     */
    private static final int STATE_STOPPED= 0;
    private static final int STATE_RUNNING= 1;

    // click manager
    private final MouseEmulationCallbacks mMouseEmulationCallbacks;

    // current state
    private volatile int mState= STATE_STOPPED;

    // layer for drawing the pointer and the dwell click feedback
    private PointerLayerView mPointerLayer;

    // pointer is enabled?
    private volatile boolean mPointerEnabled= true;
    
    // object which provides the logic for the pointer motion and actions
    private PointerControl mPointerControl;
    
    // dwell clicking function
    private DwellClick mDwellClick;

    // click enabled?
    private volatile boolean mClickEnabled= true;

    // resting mode
    private volatile boolean mRestingModeEnabled= false;
    public void setRestMode(boolean enabled) { mRestingModeEnabled= enabled; }

    /**
     * Constructor
     * @param c context
     * @param ov layout to which add other (transparent) views
     * @param om reference to the orientation manager instance
     * @param cm listener which will be called back
     */
    public MouseEmulation(Context c, OverlayView ov, OrientationManager om,
                          MouseEmulationCallbacks cm) {
        mMouseEmulationCallbacks = cm;

        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(c);
        ov.addFullScreenLayer(mPointerLayer);

        /*
         * control stuff
         */
        mPointerControl= new PointerControl(mPointerLayer, om);
        mDwellClick= new DwellClick(c);
    }

    /**
     * Enable the pointer function
     */
    public void enablePointer() {
        if (!mPointerEnabled) {
            mPointerControl.reset();
            mPointerLayer.setVisibility(View.VISIBLE);

            mPointerEnabled= true;
        }
    }

    /**
     * Disable the pointer function
     */
    public void disablePointer() {
        if (mPointerEnabled) {
            mPointerLayer.setVisibility(View.INVISIBLE);

            mPointerEnabled= false;
        }
    }

    /**
     * Enable the pointer function
     */
    public void enableClick() {
        if (!mClickEnabled) {
            mDwellClick.reset();

            mClickEnabled= true;
        }
    }

    /**
     * Disable the pointer function
     */
    public void disableClick() {
        mClickEnabled= false;
    }

    @Override
    public void start() {
        if (mState == STATE_RUNNING) return;

        /* Pointer layer */
        mPointerControl.reset();
        mPointerLayer.setVisibility(mPointerEnabled? View.VISIBLE : View.INVISIBLE);

        /* Click */
        mDwellClick.reset();

        mState = STATE_RUNNING;
    }

    @Override
    public void stop() {
        if (mState != STATE_RUNNING) return;

        mPointerLayer.setVisibility(View.INVISIBLE);

        mState = STATE_STOPPED;
    }

    @Override
    public void cleanup() {
        stop();

        if (mDwellClick!= null) {
            mDwellClick.cleanup();
            mDwellClick = null;
        }

        if (mPointerControl!= null) {
            mPointerControl.cleanup();
            mPointerControl = null;
        }

        if (mPointerLayer!= null) {
            mPointerLayer.cleanup();
            mPointerLayer = null;
        }
    }

    /**
     * Process incoming motion
     *
     * @param motion motion vector
     *
     * NOTE: this method can be called from a secondary thread
     */
    @Override
    public void processMotion(@NonNull PointF motion) {
        if (mState != STATE_RUNNING) return;

        // update pointer location given motion
        mPointerControl.updateMotion(motion);
        
        // get new pointer location
        PointF pointerLocation= mPointerControl.getPointerLocation();

        /* check if click generated */
        boolean clickGenerated= false;
        if (mClickEnabled && mMouseEmulationCallbacks.isClickable(pointerLocation)) {
            clickGenerated= mDwellClick.updatePointerLocation(pointerLocation);
        }
        else {
            mDwellClick.reset();
        }
        
        // update pointer position and click progress
        mPointerLayer.updatePosition(pointerLocation);

        // update resting mode appearance if needed
        mPointerLayer.setRestModeAppearance(mRestingModeEnabled);

        /* update dwell click progress */
        if (mClickEnabled) {
            mPointerLayer.updateClickProgress(mDwellClick.getClickProgressPercent());
        }
        else {
            mPointerLayer.updateClickProgress(0);
        }

        // make sure visible changes are updated
        mPointerLayer.postInvalidate();

        mMouseEmulationCallbacks.onMouseEvent(pointerLocation, clickGenerated);
    }
}
