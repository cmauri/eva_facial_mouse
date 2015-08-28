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

import com.crea_si.eviacam.api.GamepadButtons;
import com.crea_si.eviacam.api.IPadEventListener;
import com.crea_si.eviacam.api.SlaveMode;

import android.content.Context;
import android.graphics.PointF;
import android.os.RemoteException;
import android.view.View;

public class GamepadEngine implements MotionProcessor {
    // Absolute gamepad geometric logic
    private GamepadAbs mGamepadAbs= new GamepadAbs();
    
    private int mLastPressed= GamepadButtons.PAD_NONE;

    // Gamepad view
    private GamepadView mGamepadView;

    // layer for drawing the pointer
    private PointerLayerView mPointerLayer;

    // event listener
    private IPadEventListener mPadEventListener;

    // operation mode
    private int mOperationMode= SlaveMode.GAMEPAD_ABSOLUTE;
    
    // is paused?
    private boolean isPaused= false;

    public GamepadEngine(Context c, OverlayView ov) {
        /*
         * UI stuff 
         */
        mGamepadView= new GamepadView(c);
        
        // TODO
        mGamepadView.setInnerRadiusRatio(mGamepadAbs.getInnerRadiusRatio());
        
        ov.addFullScreenLayer(mGamepadView);

        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(c);
        ov.addFullScreenLayer(mPointerLayer);
    }

    @Override
    public void pause() {
        mGamepadView.setVisibility(View.INVISIBLE);
        mPointerLayer.setVisibility(View.INVISIBLE);
        isPaused= true;
    }

    @Override
    public void resume() {
        if (mOperationMode== SlaveMode.GAMEPAD_ABSOLUTE) {
            mPointerLayer.setVisibility(View.VISIBLE);
        }
        mGamepadView.setVisibility(View.VISIBLE);
        isPaused= false;
    }
    
    @Override
    public void cleanup() {
        mPointerLayer.cleanup();
        mPointerLayer= null;
    }

    public boolean registerListener(IPadEventListener l) {
        if (mPadEventListener== null) {
            mPadEventListener= l;
            return true;
        }
        return false;
    }

    public void unregisterListener() {
        mPadEventListener= null;
    }
    
    public void setOperationMode(int mode) {
        if (mOperationMode== mode) return;

        if (mode== SlaveMode.GAMEPAD_ABSOLUTE && !isPaused) {
            mPointerLayer.setVisibility(View.VISIBLE);
        }
        else {
            mPointerLayer.setVisibility(View.INVISIBLE);
        }
        mGamepadView.setOperationMode(mode);

        mOperationMode= mode;
    }

    /*
     * process motion from the face
     * 
     * this method is called from a secondary thread 
     */
    
    @Override
    public void processMotion(PointF motion) {
        if (mOperationMode== SlaveMode.GAMEPAD_ABSOLUTE) {
            processMotionAbsoluteGamepad(motion);
        }
        else {
            processMotionRelativeGamepad(motion);
        }
    }
    
    /**
     * Absolute gamepad motion processing
     */
    private PointF ptrLocation= new PointF();
    private void processMotionAbsoluteGamepad(PointF motion) {
        // update pointer location given face motion
        int sector= mGamepadAbs.updateMotion(motion);

        // TODO: 
        mGamepadView.setInnerRadiusRatio(mGamepadAbs.getInnerRadiusRatio());

        // get new pointer location
        mGamepadView.toCanvasCoords(mGamepadAbs.getPointerLocationNorm(), ptrLocation);

        mGamepadView.setHighlightedSector(sector);
        mGamepadView.postInvalidate();

        // update pointer position and click progress
        mPointerLayer.updatePosition(ptrLocation);
        mPointerLayer.postInvalidate();
        
        // Check and generate events
        if (sector != mLastPressed) {
            IPadEventListener l= mPadEventListener;
            if (l!= null) {
                try {
                    if (mLastPressed!= GamepadButtons.PAD_NONE) {
                        // Release previous button
                        l.buttonReleased(mLastPressed);
                    }
                    if (sector!= GamepadButtons.PAD_NONE) {
                        // Press new one
                        l.buttonPressed(sector);
                    }
                }
                catch (RemoteException e) {
                    // Just log it and go on
                    EVIACAM.debug("RemoteException while sending gamepad event");
                }
            }
            mLastPressed= sector;
        }
    }
 
    /**
     * Relative gamepad motion processing
     */
    private void processMotionRelativeGamepad(PointF motion) {
        
    }
}
