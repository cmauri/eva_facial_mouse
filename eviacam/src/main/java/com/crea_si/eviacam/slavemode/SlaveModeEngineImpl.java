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
package com.crea_si.eviacam.slavemode;

import android.app.Service;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.api.IGamepadEventListener;
import com.crea_si.eviacam.api.IMouseEventListener;
import com.crea_si.eviacam.api.IDockPanelEventListener;
import com.crea_si.eviacam.api.SlaveMode;
import com.crea_si.eviacam.common.DockPanelLayerView;
import com.crea_si.eviacam.common.MouseEmulationCallbacks;
import com.crea_si.eviacam.common.CoreEngine;
import com.crea_si.eviacam.common.EVIACAM;
import com.crea_si.eviacam.common.MotionProcessor;
import com.crea_si.eviacam.common.MouseEmulation;

public class SlaveModeEngineImpl extends CoreEngine implements SlaveModeEngine, MouseEmulationCallbacks {
    // slave mode operation mode
    private int mSlaveOperationMode= SlaveMode.GAMEPAD_ABSOLUTE;

    // mouse emulation subsystem
    private MouseEmulation mMouseEmulation;

    // gamepad emulation subsystem
    private Gamepad mGamepad;

    // reference to the current motion processor (could be mouse or gamepad)
    private MotionProcessor mCurrentMotionProcessor;

    // layer for drawing the docking panel
    private DockPanelLayerView mDockPanelLayerView;

    // reference to the listener for mouse events
    private IMouseEventListener mMouseEventListener;

    // reference to the listener for menu events
    private IDockPanelEventListener mDockPanelEventListener;

    @Override
    protected void onInit(Service service) {
        /*
         * Init slave mode. Instantiate both gamepad and mouse emulator
         */

        // Set initial valid mode for gamepad engine
        final int mode= (mSlaveOperationMode!= SlaveMode.MOUSE?
                mSlaveOperationMode : SlaveMode.GAMEPAD_ABSOLUTE);

        /* dockable menu view */
        mDockPanelLayerView = new DockPanelLayerView(service);
        mDockPanelLayerView.setVisibility(View.INVISIBLE);
        getOverlayView().addFullScreenLayer(mDockPanelLayerView);

        // Create specific emulation subsystems
        mGamepad = new Gamepad(service, getOverlayView(), mode);
        mMouseEmulation =
                new MouseEmulation(service, getOverlayView(), getOrientationManager(), this);

        // Select currently enabled subsystem
        if (mSlaveOperationMode== SlaveMode.MOUSE) {
            mCurrentMotionProcessor = mMouseEmulation;
        }
        else {
            mCurrentMotionProcessor = mGamepad;
        }
    }

    @Override
    protected void onCleanup() {
        if (mMouseEmulation != null) {
            mMouseEmulation.cleanup();
            mMouseEmulation = null;
        }

        if (mGamepad != null) {
            mGamepad.cleanup();
            mGamepad = null;
        }

        if (mDockPanelLayerView != null) {
            mDockPanelLayerView.cleanup();
            mDockPanelLayerView = null;
        }

        mCurrentMotionProcessor= null;
        mMouseEventListener= null;
    }

    @Override
    public void setSlaveOperationMode(final int mode) {
        Runnable request = new Runnable() {
            @Override
            public void run() {
                doSetSlaveOperationMode(mode);
            }
        };
        processRequest(request);
    }

    private void doSetSlaveOperationMode(int mode) {
        if (mSlaveOperationMode== mode) return;

        // Pause old motion processor & switch to new one
        if (mSlaveOperationMode== SlaveMode.MOUSE) {
            mMouseEmulation.stop();
            mDockPanelLayerView.setVisibility(View.INVISIBLE);
            mCurrentMotionProcessor = mGamepad;
        }
        else if (mode== SlaveMode.MOUSE){
            mGamepad.stop();
            mDockPanelLayerView.setVisibility(View.VISIBLE);
            mCurrentMotionProcessor = mMouseEmulation;
        }

        mSlaveOperationMode= mode;

        if (mode!= SlaveMode.MOUSE) {
            mGamepad.setOperationMode(mode);
        }

        // Resume engine if needed
        if (getState() == STATE_RUNNING) mCurrentMotionProcessor.start();
    }

    @Override
    protected boolean onStart() {
        if (mCurrentMotionProcessor!= null) {
            if (mSlaveOperationMode== SlaveMode.MOUSE) {
                mDockPanelLayerView.setVisibility(View.VISIBLE);
            }
            mCurrentMotionProcessor.start();
        }
        return true;
    }

    @Override
    protected void onStop() {
        if (mCurrentMotionProcessor!= null) {
            if (mSlaveOperationMode== SlaveMode.MOUSE) {
                mDockPanelLayerView.setVisibility(View.INVISIBLE);
            }
            mCurrentMotionProcessor.stop();
        }
    }

    @Override
    protected void onPause() {
        onStop();
    }

    @Override
    protected void onStandby()  {
        onStop();
    }

    @Override
    protected void onResume()  {
        onStart();
    }

    @Override
    public boolean registerGamepadListener(IGamepadEventListener l) {
        return mGamepad.registerListener(l);
    }

    @Override
    public void unregisterGamepadListener() {
        mGamepad.unregisterListener();
    }

    @Override
    public boolean registerMouseListener(IMouseEventListener l) {
        if (mMouseEventListener== null) {
            mMouseEventListener= l;
            return true;
        }
        return false;
    }

    @Override
    public void unregisterDockPanelListener() {
        mDockPanelEventListener= null;
    }

    @Override
    public boolean registerDockPanelListener(IDockPanelEventListener l) {
        if (mDockPanelEventListener== null) {
            mDockPanelEventListener= l;
            return true;
        }
        return false;
    }

    @Override
    public void unregisterMouseListener() {
        mMouseEventListener= null;
    }

    /*
     * Last values for checkAndSendMouseEvents
     */
    private Point mLastPos= new Point();
    private boolean mLastClicked= false;

    /**
     * Send mouse events when needed
     *
     * @param pos pointer location in screen coordinates
     * @param clicked true if click performed
     */
    private void checkAndSendMouseEvents(Point pos, boolean clicked) {
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
            Log.e(EVIACAM.TAG, "RemoteException while sending mouse event");
        }
        mLastPos.set(pos.x, pos.y);
        mLastClicked= clicked;
    }

    /**
     * Process motion events
     *
     * @param motion motion vector, could be (0, 0) if motion not detected or the engine is
     *               paused or in standby mode
     * @param faceDetected whether or not a face was detected for the last frame, note
     *                     not all frames are checked for the face detection algorithm
     * @param state current state of the engine
     */
    @Override
    protected void onFrame(@NonNull PointF motion, boolean faceDetected, int state) {
        if (mCurrentMotionProcessor!= null) {
            if (state == STATE_RUNNING) {
                mCurrentMotionProcessor.processMotion(motion);
            }
        }
    }

    // Avoid creating a new Point for each onMouseEvent call
    private Point mPointInt= new Point();

    /**
     * Process mouse pointer events
     *
     * @param location location of the pointer is screen coordinates
     * @param click true when click generated
     */
    @Override
    public void onMouseEvent(@NonNull PointF location, boolean click) {
        mPointInt.x= (int) location.x;
        mPointInt.y= (int) location.y;

        mMouseEmulation.setRestMode(mDockPanelLayerView.getRestModeEnabled());

        if (!click) {
            // No click, send event and finish
            checkAndSendMouseEvents(mPointInt, false);
            return;
        }

        /* Click on the dock panel? */
        int idDockPanelAction= mDockPanelLayerView.getViewIdBelowPoint(mPointInt);
        if (idDockPanelAction == View.NO_ID) {
            // No, send regular click event and finish
            checkAndSendMouseEvents(mPointInt, true);
            return;
        }

        /*
         * Process click on dock menu
         */
        // Process action by the view
        mDockPanelLayerView.performClick(idDockPanelAction);

        /* Translate menu entry to option code */
        int option= 0;
        switch (idDockPanelAction) {
            case R.id.back_button:
                option= IDockPanelEventListener.BACK;
                break;
            case R.id.home_button:
                option= IDockPanelEventListener.HOME;
                break;
            case R.id.recents_button:
                option= IDockPanelEventListener.RECENTS;
                break;
            case R.id.notifications_button:
                option= IDockPanelEventListener.NOTIFICATIONS;
                break;
            case R.id.softkeyboard_button:
                option= IDockPanelEventListener.KEYBOARD;
                break;
            case R.id.toggle_context_menu:
                option= IDockPanelEventListener.CONTEXT_MENU;
                break;
            case R.id.toggle_rest_mode:
                option= IDockPanelEventListener.REST_MODE;
                break;
        }

        IDockPanelEventListener l= mDockPanelEventListener;
        if (l!= null && option!= 0) {
            try {
                l.onDockMenuOption(option);
            } catch (RemoteException e) {
                // Just ignore exception if any
            }
        }
    }

    /**
     *
     * @param location location of the pointer is screen coordinates
     * @return always true
     */
    @Override
    public boolean isClickable(@NonNull PointF location) {
        if (!mDockPanelLayerView.getRestModeEnabled()) return true;

        mPointInt.x= (int) location.x;
        mPointInt.y= (int) location.y;

        // Rest mode, only specific button in the dock panel works
        return mDockPanelLayerView.isActionable(mPointInt);
    }
}
