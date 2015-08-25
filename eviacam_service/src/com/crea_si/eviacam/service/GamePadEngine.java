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
import android.graphics.PointF;
import android.view.View;

public class GamePadEngine implements MotionProcessor {
    private AbsolutePad mAbsolutePad= new AbsolutePad();

    private AbsolutePadView mAbsolutePadView;

    // layer for drawing the pointer and the dwell click feedback
    private PointerLayerView mPointerLayer;

    public GamePadEngine(Context c, OverlayView ov) {
        /*
         * UI stuff 
         */
        mAbsolutePadView= new AbsolutePadView(c);
        
        // TODO
        mAbsolutePadView.setInnerRadiusRatio(mAbsolutePad.getInnerRadiusRatio());
        
        ov.addFullScreenLayer(mAbsolutePadView);

        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(c);
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
    }
}
