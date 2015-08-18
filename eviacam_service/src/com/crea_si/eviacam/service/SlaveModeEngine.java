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
import android.view.accessibility.AccessibilityEvent;

public class SlaveModeEngine {
    
    private AbsolutePadView mAbsolutePadView;
    
    // layer for drawing the pointer and the dwell click feedback
    private PointerLayerView mPointerLayer;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    public SlaveModeEngine(Context c, OverlayView ov) {
        /*
         * UI stuff 
         */
        mAbsolutePadView= new AbsolutePadView(c);
        ov.addFullScreenLayer(mAbsolutePadView);

        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(c);
        ov.addFullScreenLayer(mPointerLayer);

        /*
         * control stuff
         */
        mPointerControl= new PointerControl(c, mPointerLayer);
    }
   
    public void cleanup() {
        mPointerControl.cleanup();
        mPointerControl= null;

        mPointerLayer.cleanup();
        mPointerLayer= null;
    }
   
    public void pause() {
        mPointerLayer.setVisibility(View.INVISIBLE);
    }
    
    public void resume() {
        mPointerControl.reset();
        mPointerLayer.setVisibility(View.VISIBLE);
    }    

    public void onAccessibilityEvent(AccessibilityEvent event) {
        // do nothing
    } 

    /*
     * process incoming camera frame 
     * 
     * this method is called from a secondary thread 
     */
    public void processMotion(PointF motion) {
        // update pointer location given face motion
        mPointerControl.updateMotion(motion);
        
        // get new pointer location
        PointF pointerLocation= mPointerControl.getPointerLocation();
        
        /*
        Point pInt= new Point();
        pInt.x= (int) pointerLocation.x;
        pInt.y= (int) pointerLocation.y;
        */
        
        mAbsolutePadView.postInvalidate();
        
        // update pointer position and click progress
        mPointerLayer.updatePosition(pointerLocation);
        mPointerLayer.postInvalidate();
    }
}
