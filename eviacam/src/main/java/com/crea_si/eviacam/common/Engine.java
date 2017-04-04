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

import android.app.Service;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Interface for the engine
 */
public interface Engine {
    /*
     * States of the engine
     */
    // not initialised
    int STATE_DISABLED= 0;

    // initialised but stopped
    int STATE_STOPPED= 1;

    // running
    int STATE_RUNNING= 2;

    // paused, capturing frames but not moving the pointer
    int STATE_PAUSED= 4;

    // similar to paused but capturing at low FPS rate just trying to
    // find a face (to return to STATE_RUNNING again)
    int STATE_STANDBY = 3;

    /**
     * Interface definition of a callback to be invoked indicating the completion
     * of the Engine initialization.
     */
    interface OnInitListener {
        int INIT_SUCCESS = 0;
        int INIT_ERROR = -1;

        /**
         * Called to signal the completion of the Engine initialization.
         *
         * @param status 0 if initialization completed successfully
         */
        void onInit(int status);
    }

    /**
     * Engine initialization
     *
     * @param s service to be used as context for the engine
     * @param l listener to be called when initialization finished
     * @return true if, at least, the first stage of the initialization went fine
     *         to know when the initialization is complete and everything went fine
     *         need to register a OnInitListener
     */
    boolean init(@NonNull Service s, @Nullable OnInitListener l);

    /**
     * Start the engine
     *
     * @return true if successfully started
     */
    boolean start();

    /**
     * Stop the engine
     */
    void stop();

    /**
     * Pause the engine
     */
    void pause();

    /**
     * Resume the engine if in paused or in standby
     */
    void resume();

    /**
     * Enter standby mode
     */
    void standby();

    /**
     * Cleanup (destructor)
     */
    void cleanup();

    /**
     * Return the state of the engine
     *
     * @return code of the current state of the engine which could be
     *         STATE_DISABLED, STATE_STOPPED, STATE_RUNNING, STATE_PAUSED, STATE_STANDBY
     */
    int getState();

    /**
     * Return whether the engine is ready (not in STATE_DISABLED)
     *
     * @return true when ready
     */
    boolean isReady();

    /**
     * Return elapsed time since last face detection
     *
     * @return elapsed time in ms or 0 if no detection
     */
    long getFaceDetectionElapsedTime();

    /**
     * Provide feedback for the face detector
     * @param fdc
     *
     * Safe to call from a secondary thread
     * TODO: FaceDetectionCountdown is quite ugly
     */
    void updateFaceDetectorStatus(FaceDetectionCountdown fdc);
}
