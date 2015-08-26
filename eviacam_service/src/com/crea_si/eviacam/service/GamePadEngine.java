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

import android.content.Context;
import android.graphics.PointF;
import android.os.RemoteException;
import android.view.View;

public class GamePadEngine implements MotionProcessor {
    private final Context mContext;
    
    // Gamepad geometric logic
    private AbsolutePad mAbsolutePad= new AbsolutePad();
    
    private int mLastPressed= GamepadButtons.PAD_NONE;

    // Absolute gamepad view
    private AbsolutePadView mAbsolutePadView;

    // layer for drawing the pointer
    private PointerLayerView mPointerLayer;

    // event listener
    private IPadEventListener mPadEventListener;    

    public GamePadEngine(Context c) {
        mContext= c;
    }
    
    public void init (OverlayView ov) {
        /*
         * UI stuff 
         */
        mAbsolutePadView= new AbsolutePadView(mContext);
        
        // TODO
        mAbsolutePadView.setInnerRadiusRatio(mAbsolutePad.getInnerRadiusRatio());
        
        ov.addFullScreenLayer(mAbsolutePadView);

        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(mContext);
        ov.addFullScreenLayer(mPointerLayer);
    }

    @Override
    public void cleanup() {
        mPointerLayer.cleanup();
        mPointerLayer= null;
    }

    @Override
    public void pause() {
        mPointerLayer.setVisibility(View.INVISIBLE);
    }

    @Override
    public void resume() {
        mPointerLayer.setVisibility(View.VISIBLE);
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

    /*
     * process motion from the face
     * 
     * this method is called from a secondary thread 
     */
    private PointF ptrLocation= new PointF();
    @Override
    public void processMotion(PointF motion) {
        // update pointer location given face motion
        int sector= mAbsolutePad.updateMotion(motion);

        // TODO: 
        mAbsolutePadView.setInnerRadiusRatio(mAbsolutePad.getInnerRadiusRatio());

        // get new pointer location
        mAbsolutePadView.toCanvasCoords(mAbsolutePad.getPointerLocationNorm(), ptrLocation);

        mAbsolutePadView.setHighlightedSector(sector);
        mAbsolutePadView.postInvalidate();

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
}
