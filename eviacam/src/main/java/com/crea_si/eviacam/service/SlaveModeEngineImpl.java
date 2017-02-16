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
package com.crea_si.eviacam.service;

import android.app.Service;
import android.graphics.PointF;

import com.crea_si.eviacam.api.IGamepadEventListener;
import com.crea_si.eviacam.api.IMouseEventListener;
import com.crea_si.eviacam.api.SlaveMode;

class SlaveModeEngineImpl extends CoreEngine implements SlaveModeEngine {
    /* slave mode operation mode */
    private int mSlaveOperationMode= SlaveMode.GAMEPAD_ABSOLUTE;

    /* reference to the motion processor for mouse emulation */
    private MouseEmulationEngine mMouseEmulationEngine;

    /* reference to the motion processor for gamepad emulation */
    private GamepadEngine mGamepadEngine;

    /* generic referente to the current motion processor */
    private MotionProcessor mCurrentMotionProcessor;


    @Override
    protected void onInit(Service service) {
        /*
         * Init in slave mode. Instantiate both gamepad and mouse emulation.
         */

        // Set valid mode for gamepad engine
        final int mode= (mSlaveOperationMode!= SlaveMode.MOUSE?
                mSlaveOperationMode : SlaveMode.GAMEPAD_ABSOLUTE);

        // Create specific engines
        mGamepadEngine= new GamepadEngine(service, getOverlayView(), mode);
        mMouseEmulationEngine=
                new MouseEmulationEngine(service, getOverlayView(), getOrientationManager());

        // Select enabled engine
        if (mSlaveOperationMode== SlaveMode.MOUSE) {
            mCurrentMotionProcessor = mMouseEmulationEngine;
        }
        else {
            mCurrentMotionProcessor = mGamepadEngine;
        }
    }

    @Override
    protected void onCleanup() {
        if (mMouseEmulationEngine!= null) {
            mMouseEmulationEngine.cleanup();
            mMouseEmulationEngine= null;
        }
        if (mGamepadEngine!= null) {
            mGamepadEngine.cleanup();
            mGamepadEngine = null;
        }
        mCurrentMotionProcessor= null;
    }

    @Override
    public void setSlaveOperationMode(int mode) {
        if (mSlaveOperationMode== mode) return;

        // Pause old motion processor & switch to new
        if (mSlaveOperationMode== SlaveMode.MOUSE) {
            mMouseEmulationEngine.stop();
            mCurrentMotionProcessor = mGamepadEngine;
        }
        else if (mode== SlaveMode.MOUSE){
            mGamepadEngine.stop();
            mCurrentMotionProcessor = mMouseEmulationEngine;
        }

        mSlaveOperationMode= mode;

        if (mode!= SlaveMode.MOUSE) {
            mGamepadEngine.setOperationMode(mode);
        }

        // Resume engine if needed
        if (getState() == STATE_RUNNING) mCurrentMotionProcessor.start();
    }

    @Override
    protected boolean onStart() {
        if (mCurrentMotionProcessor!= null) mCurrentMotionProcessor.start();
        return true;
    }

    @Override
    protected void onStop() {
        if (mCurrentMotionProcessor!= null) mCurrentMotionProcessor.stop();
    }

    @Override
    protected void onPause() {
        if (mCurrentMotionProcessor!= null) mCurrentMotionProcessor.stop();
    }

    @Override
    protected void onStandby() {
        if (mCurrentMotionProcessor!= null) mCurrentMotionProcessor.stop();
    }

    @Override
    protected void onResume() {
        if (mCurrentMotionProcessor!= null) mCurrentMotionProcessor.start();
    }

    @Override
    public boolean registerGamepadListener(IGamepadEventListener l) {
        return mGamepadEngine.registerListener(l);
    }

    @Override
    public void unregisterGamepadListener() {
        mGamepadEngine.unregisterListener();
    }

    @Override
    public boolean registerMouseListener(IMouseEventListener l) {
        return mMouseEmulationEngine.registerListener(l);
    }

    @Override
    public void unregisterMouseListener() {
        mMouseEmulationEngine.unregisterListener();
    }

    @Override
    protected void onFrame(PointF motion, boolean faceDetected, int state) {
        if (mCurrentMotionProcessor!= null) {
            if (state == STATE_RUNNING) {
                mCurrentMotionProcessor.processMotion(motion);
            }
        }
    }
}
