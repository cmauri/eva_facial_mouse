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

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

public class AccessibilityServiceModeEngineImpl {
    // layer for drawing the pointer and the dwell click feedback
    private PointerLayerView mPointerLayer;
    
    // layer for drawing the docking panel
    private DockPanelLayerView mDockPanelView;

    // layer for the scrolling user interface
    private ScrollLayerView mScrollLayerView;
    
    // layer for drawing different controls
    private ControlsLayerView mControlsLayer;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    // dwell clicking function
    private DwellClick mDwellClick;
    
    // perform actions on the UI using the accessibility API
    private AccessibilityAction mAccessibilityAction;
    
    public AccessibilityServiceModeEngineImpl(AccessibilityService as, OverlayView ov) {
        /*
         * UI stuff 
         */
        mDockPanelView= new DockPanelLayerView(as);
        ov.addFullScreenLayer(mDockPanelView);

        mScrollLayerView= new ScrollLayerView(as);
        ov.addFullScreenLayer(mScrollLayerView);
        
        mControlsLayer= new ControlsLayerView(as);
        ov.addFullScreenLayer(mControlsLayer);
        
        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(as);
        ov.addFullScreenLayer(mPointerLayer);

        /*
         * control stuff
         */
        mPointerControl= new PointerControl(as, mPointerLayer);
        
        mDwellClick= new DwellClick(as);
        
        mAccessibilityAction= new AccessibilityAction (
                as, mControlsLayer, mDockPanelView, mScrollLayerView);
    }
   
    public void cleanup() {
        mAccessibilityAction.cleanup();
        mAccessibilityAction= null;

        mDwellClick.cleanup();
        mDwellClick= null;

        mPointerControl.cleanup();
        mPointerControl= null;

        // nothing to be done for mScrollLayerView and mControlsLayer
        
        mDockPanelView.cleanup();
        mDockPanelView= null;
        
        mPointerLayer.cleanup();
        mPointerLayer= null;
    }
   
    public void pause() {
        mPointerLayer.setVisibility(View.INVISIBLE);
        mScrollLayerView.setVisibility(View.INVISIBLE);
        mControlsLayer.setVisibility(View.INVISIBLE);
        mDockPanelView.setVisibility(View.INVISIBLE);
    }
    
    public void resume() {
        mPointerControl.reset();
        mDwellClick.reset();
        mAccessibilityAction.reset();

        mDockPanelView.setVisibility(View.VISIBLE);
        mControlsLayer.setVisibility(View.VISIBLE);
        mScrollLayerView.setVisibility(View.VISIBLE);
        mPointerLayer.setVisibility(View.VISIBLE);
    }    

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mAccessibilityAction != null) mAccessibilityAction.onAccessibilityEvent(event);
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
        
        Point pInt= new Point();
        pInt.x= (int) pointerLocation.x;
        pInt.y= (int) pointerLocation.y;
        
        boolean clickGenerated= false;
        if (mAccessibilityAction.isActionable(pInt)) {
            // dwell clicking update
            clickGenerated= mDwellClick.updatePointerLocation(pointerLocation);
        }
        else {
            mDwellClick.reset();
        }
        
        // update pointer position and click progress
        mPointerLayer.updatePosition(pointerLocation);
        mPointerLayer.setClickDisabledAppearance(mAccessibilityAction.getClickDisabled());
        mPointerLayer.updateClickProgress(mDwellClick.getClickProgressPercent());
        mPointerLayer.postInvalidate();
        
        // this needs to be called regularly
        mAccessibilityAction.refresh();
        
        // perform action when needed
        if (clickGenerated) {
            mAccessibilityAction.performAction(pInt);
        }
    }
}
