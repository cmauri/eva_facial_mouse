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
import com.crea_si.eviacam.api.IGamepadEventListener;
import com.crea_si.eviacam.api.SlaveMode;

import android.content.Context;
import android.graphics.PointF;
import android.os.RemoteException;
import android.view.View;

public class GamepadEngine implements MotionProcessor {
    // time in ms after which the direction highlight is switched off
    private static final int TIME_OFF_MS= 100;
    
    // Absolute gamepad geometric logic
    private GamepadAbs mGamepadAbs= new GamepadAbs();
    
    private ShakeDetector mShakeDetectorX= new ShakeDetector();
    private ShakeDetector mShakeDetectorY= new ShakeDetector();
    
    private int mLastPressedButton= GamepadButtons.PAD_NONE;

    // Gamepad view
    private GamepadView mGamepadView;

    // layer for drawing the pointer
    private PointerLayerView mPointerLayer;

    // event listener
    private IGamepadEventListener mPadEventListener;

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

    public boolean registerListener(IGamepadEventListener l) {
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
    
    private void checkAndSendEvents(int button) {
        // Check and generate events
        if (button != mLastPressedButton) {
            IGamepadEventListener l= mPadEventListener;
            if (l!= null) {
                try {
                    if (mLastPressedButton!= GamepadButtons.PAD_NONE) {
                        // Release previous button
                        l.buttonReleased(mLastPressedButton);
                    }
                    if (button!= GamepadButtons.PAD_NONE) {
                        // Press new one
                        l.buttonPressed(button);
                    }
                }
                catch (RemoteException e) {
                    // Just log it and go on
                    EVIACAM.debug("RemoteException while sending gamepad event");
                }
            }
            mLastPressedButton= button;
        }
    }
    
    /**
     * Absolute gamepad motion processing
     */
    private PointF ptrLocation= new PointF();
    private void processMotionAbsoluteGamepad(PointF motion) {
        // update pointer location given face motion
        int button= mGamepadAbs.updateMotion(motion);

        // TODO: 
        mGamepadView.setInnerRadiusRatio(mGamepadAbs.getInnerRadiusRatio());

        // get new pointer location
        mGamepadView.toCanvasCoords(mGamepadAbs.getPointerLocationNorm(), ptrLocation);

        mGamepadView.setHighlightedButton(button);
        mGamepadView.postInvalidate();

        // update pointer position and click progress
        mPointerLayer.updatePosition(ptrLocation);
        mPointerLayer.postInvalidate();
        
        checkAndSendEvents(button);
    }
 
    /**
     * Relative gamepad motion processing
     */
    private long mLastHighlightTStamp = 0;
    private void processMotionRelativeGamepad(PointF motion) {
        //EVIACAM.debug("motion: (" + motion.x + ", " + motion.y + ")");
        
        long current= System.currentTimeMillis();
        int button= GamepadButtons.PAD_NONE;
        
        /** Y axis */
        int shakeY= mShakeDetectorY.update(motion.y);
        if (shakeY> 0) {
            button= GamepadButtons.PAD_DOWN;
            mGamepadView.setHighlightedButton(button);
            mGamepadView.postInvalidate();
            mLastHighlightTStamp= current;
        }
        else if (shakeY< 0) {
            button= GamepadButtons.PAD_UP;
            mGamepadView.setHighlightedButton(button);
            mGamepadView.postInvalidate();
            mLastHighlightTStamp= current;
        }
        
        /** X axis */
        if (shakeY== 0) {
            int shakeX= mShakeDetectorX.update(motion.x);
            if (shakeX> 0) {
                button= GamepadButtons.PAD_RIGHT;
                mGamepadView.setHighlightedButton(button);
                mGamepadView.postInvalidate();
                mLastHighlightTStamp= current;
            }
            else if (shakeX< 0) {
                button= GamepadButtons.PAD_LEFT;
                mGamepadView.setHighlightedButton(button);
                mGamepadView.postInvalidate();
                mLastHighlightTStamp= current;
            }
        }        
        
        /* Switch off highlighted direction when needed */
        if (current - mLastHighlightTStamp> TIME_OFF_MS) {
            mGamepadView.setHighlightedButton(GamepadButtons.PAD_NONE);
            mGamepadView.postInvalidate();
        }
        
        checkAndSendEvents(button);
    }
}
