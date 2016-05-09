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

import com.crea_si.eviacam.EVIACAM;
import com.crea_si.eviacam.Preferences;
import com.crea_si.eviacam.api.IMouseEventListener;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.AudioManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

class MouseEmulationEngine implements
        MotionProcessor,
        SharedPreferences.OnSharedPreferenceChangeListener {
    /*
     * states of the engine
     */
    private static final int STATE_STOPPED= 0;
    private static final int STATE_RUNNING= 1;

    // current state
    private int mState= STATE_STOPPED;

    // layer for drawing the pointer and the dwell click feedback
    private PointerLayerView mPointerLayer;

    // pointer is enabled?
    private boolean mPointerEnabled= true;
    
    // layer for drawing the docking panel
    private DockPanelLayerView mDockPanelView;

    // docking panel enabled?
    private boolean mDockPanelEnabled= true;

    // layer for the scrolling user interface
    private ScrollLayerView mScrollLayerView;

    // scroll buttons enabled?
    private boolean mScrollEnabled= true;
    
    // layer for drawing different controls
    private ControlsLayerView mContextMenuView;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    // dwell clicking function
    private DwellClick mDwellClick;

    // click enabled?
    private boolean mClickEnabled= true;

    // whether to play a sound when action performed
    private boolean mSoundOnClick;

    // audio manager for FX notifications
    private final AudioManager mAudioManager;
    
    // perform actions on the UI using the accessibility API
    private AccessibilityAction mAccessibilityAction;
    
    // event listener
    private IMouseEventListener mMouseEventListener;

    // constructor
    public MouseEmulationEngine(Service s, OverlayView ov, OrientationManager om) {

        /*
         * Final stuff
         */
        mAudioManager= (AudioManager) s.getSystemService(Context.AUDIO_SERVICE);

        /*
         * UI stuff
         */
        if (s instanceof AccessibilityService) {
            mDockPanelView= new DockPanelLayerView(s);
            ov.addFullScreenLayer(mDockPanelView);
        }

        mScrollLayerView= new ScrollLayerView(s);
        ov.addFullScreenLayer(mScrollLayerView);

        mContextMenuView = new ControlsLayerView(s);
        ov.addFullScreenLayer(mContextMenuView);

        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(s);
        ov.addFullScreenLayer(mPointerLayer);

        /*
         * control stuff
         */
        mPointerControl= new PointerControl(mPointerLayer, om);

        mDwellClick= new DwellClick(s);

        if (s instanceof AccessibilityService) {
            mAccessibilityAction= new AccessibilityAction ((AccessibilityService) s,
                    mContextMenuView, mDockPanelView, mScrollLayerView);
        }

        // register preference change listener
        Preferences.get().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        updateSettings();
    }

    private void updateSettings() {
        // get values from shared resources
        mSoundOnClick= Preferences.get().getSoundOnClick();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.KEY_SOUND_ON_CLICK)) updateSettings();
    }

    private void playSound () {
        if (mSoundOnClick) {
            mAudioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
        }
    }

    public void enablePointer() {
        if (!mPointerEnabled) {
            mPointerControl.reset();
            mPointerLayer.setVisibility(View.VISIBLE);

            // Reset context menu
            //if (mAccessibilityAction!= null) { mAccessibilityAction.reset(); }

            mPointerEnabled= true;
        }
    }

    public void disablePointer() {
        if (mPointerEnabled) {
            mPointerLayer.setVisibility(View.INVISIBLE);

            // Reset (remove) context menu
            if (mAccessibilityAction!= null) { mAccessibilityAction.reset(); }

            mPointerEnabled= false;
        }
    }

    public void enableClick() {
        if (!mClickEnabled) {
            // Reset context menu
            //if (mAccessibilityAction!= null) { mAccessibilityAction.reset(); }
            //mContextMenuView.setVisibility(View.VISIBLE);

            mDwellClick.reset();

            mClickEnabled= true;
        }
    }

    public void disableClick() {
        if (mClickEnabled) {
            // Reset (remove) context menu
            if (mAccessibilityAction!= null) { mAccessibilityAction.reset(); }
            //mContextMenuView.setVisibility(View.INVISIBLE);

            mClickEnabled= false;
        }
    }

    public void enableDockPanel() {
        if (!mDockPanelEnabled) {
            if (mDockPanelView != null) mDockPanelView.setVisibility(View.VISIBLE);
            mDockPanelEnabled= true;
        }
    }

    public void disableDockPanel() {
        if (mDockPanelEnabled) {
            if (mDockPanelView != null) mDockPanelView.setVisibility(View.INVISIBLE);
            mDockPanelEnabled= false;
        }
    }

    public void enableScrollButtons() {
        if (!mScrollEnabled) {
            if (mAccessibilityAction!= null) { mAccessibilityAction.enableScrollingScan(); }
            mScrollEnabled= true;
        }
    }

    public void disableScrollButtons() {
        if (mScrollEnabled) {
            if (mAccessibilityAction!= null) { mAccessibilityAction.disableScrollingScan(); }
            mScrollEnabled= false;
        }
    }

    //@Override
    public void start() {
        if (mState == STATE_RUNNING) return;

        /* Pointer layer */
        mPointerControl.reset();
        mPointerLayer.setVisibility(mPointerEnabled? View.VISIBLE : View.INVISIBLE);

        /* Click */
        // Reset context menu when needed if previously shown
        if (mAccessibilityAction!= null) mAccessibilityAction.reset();
        mContextMenuView.setVisibility(View.VISIBLE);
        mDwellClick.reset();

        /* Dock panel */
        if (mDockPanelView != null) {
            mDockPanelView.setVisibility(mDockPanelEnabled ? View.VISIBLE : View.INVISIBLE);
        }

        /* Scroll buttons */
        if (mAccessibilityAction!= null) {
            if (mScrollEnabled) mAccessibilityAction.enableScrollingScan ();
            else mAccessibilityAction.disableScrollingScan();
        }
        mScrollLayerView.setVisibility(View.VISIBLE);

        mState = STATE_RUNNING;
    }

    //@Override
    public void stop() {
        if (mState != STATE_RUNNING) return;

        /* Pointer layer */
        mPointerLayer.setVisibility(View.INVISIBLE);

        /* Click */
        // Reset context menu when needed if previously shown
        if (mAccessibilityAction!= null) mAccessibilityAction.reset();
        mContextMenuView.setVisibility(View.INVISIBLE);

        /* Dock panel */
        if (mDockPanelView != null) {
            mDockPanelView.setVisibility(View.INVISIBLE);
        }

        /* Scroll buttons */
        if (mAccessibilityAction!= null) { mAccessibilityAction.disableScrollingScan(); }
        mScrollLayerView.setVisibility(View.INVISIBLE);

        mState = STATE_STOPPED;
    }

    @Override
    public void cleanup() {
        Preferences.get().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        if (mAccessibilityAction!= null) {
            mAccessibilityAction.cleanup();
            mAccessibilityAction= null;
        }

        if (mDwellClick!= null) {
            mDwellClick.cleanup();
            mDwellClick = null;
        }

        if (mPointerControl!= null) {
            mPointerControl.cleanup();
            mPointerControl = null;
        }

        // nothing to be done for mScrollLayerView

        if (mDockPanelView!= null) {
            mDockPanelView.cleanup();
            mDockPanelView= null;
        }

        if (mContextMenuView != null) {
            mContextMenuView.cleanup();
            mContextMenuView = null;
        }

        if (mPointerLayer!= null) {
            mPointerLayer.cleanup();
            mPointerLayer = null;
        }
    }

    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mAccessibilityAction != null) mAccessibilityAction.onAccessibilityEvent(event);
    }

    public boolean registerListener(IMouseEventListener l) {
        if (mMouseEventListener== null) {
            mMouseEventListener= l;
            return true;
        }
        return false;
    }

    public void unregisterListener() {
        mMouseEventListener= null;
    }

    /*
     * Last values for checkAndSendEvents
     */
    private Point mLastPos= new Point();
    private boolean mLastClicked= false;

    /*
     * Sent mouse events when needed
     */
    private void checkAndSendEvents(Point pos, boolean clicked) {
        final float DEFAULT_SIZE = 1.0f;
        final int DEFAULT_META_STATE = 0;
        final float DEFAULT_PRECISION_X = 1.0f;
        final float DEFAULT_PRECISION_Y = 1.0f;
        final int DEFAULT_DEVICE_ID = 0;
        final int DEFAULT_EDGE_FLAGS = 0;

        // Check and generate events
        IMouseEventListener l= mMouseEventListener;
        if (l== null) return;

        try {
            long now = SystemClock.uptimeMillis();

            if (!pos.equals(mLastPos)) {
                MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_MOVE,
                    pos.x, pos.y, 0.0f, DEFAULT_SIZE, DEFAULT_META_STATE,
                    DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y, DEFAULT_DEVICE_ID,
                    DEFAULT_EDGE_FLAGS);
                event.setSource(InputDevice.SOURCE_CLASS_POINTER);
                l.onMouseEvent(event);
            }
            if (mLastClicked && !clicked) {
                MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP,
                        pos.x, pos.y, 0.0f, DEFAULT_SIZE, DEFAULT_META_STATE,
                        DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y, DEFAULT_DEVICE_ID,
                        DEFAULT_EDGE_FLAGS);
                event.setSource(InputDevice.SOURCE_CLASS_POINTER);
                l.onMouseEvent(event);
            }
            else if (!mLastClicked && clicked) {
                MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
                        pos.x, pos.y, 0.0f, DEFAULT_SIZE, DEFAULT_META_STATE,
                        DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y, DEFAULT_DEVICE_ID,
                        DEFAULT_EDGE_FLAGS);
                event.setSource(InputDevice.SOURCE_CLASS_POINTER);
                l.onMouseEvent(event);
            }
        }
        catch (RemoteException e) {
            // Just log it and go on
            EVIACAM.debug("RemoteException while sending mouse event");
        }
        mLastPos.set(pos.x, pos.y);
        mLastClicked= clicked;
    }

    /*
     * Process incoming motion
     * 
     * this method is called from a secondary thread 
     */
    @Override
    public void processMotion(PointF motion) {
        if (mState != STATE_RUNNING) return;

        // If pointer nor enabled just refresh scrolling buttons and exit
        if (!mPointerEnabled) {
            if (mAccessibilityAction != null) {
                mAccessibilityAction.refresh();
            }
            return;
        }

        // update pointer location given face motion
        mPointerControl.updateMotion(motion);
        
        // get new pointer location
        PointF pointerLocation= mPointerControl.getPointerLocation();
        
        Point pInt= new Point();
        pInt.x= (int) pointerLocation.x;
        pInt.y= (int) pointerLocation.y;
        
        boolean clickGenerated= false;
        if (mAccessibilityAction== null || mAccessibilityAction.isActionable(pInt)) {
            // dwell clicking update
            clickGenerated= mDwellClick.updatePointerLocation(pointerLocation);
        }
        else {
            mDwellClick.reset();
        }
        
        // update pointer position and click progress
        mPointerLayer.updatePosition(pointerLocation);

        if (mAccessibilityAction!= null) {
            mPointerLayer.setRestModeAppearance(mAccessibilityAction.getRestModeEnabled());
        }
        if (mClickEnabled) {
            mPointerLayer.updateClickProgress(mDwellClick.getClickProgressPercent());
        }
        else {
            mPointerLayer.updateClickProgress(0);
        }
        mPointerLayer.postInvalidate();
        
        // this needs to be called regularly
        if (mAccessibilityAction!= null) {
            mAccessibilityAction.refresh();
                
            // perform action when needed
            if (clickGenerated && mClickEnabled) {
                mAccessibilityAction.performAction(pInt);
                if (mSoundOnClick) playSound ();
            }
        }

        checkAndSendEvents(pInt, clickGenerated);
    }
}
