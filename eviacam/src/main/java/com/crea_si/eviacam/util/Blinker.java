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
package com.crea_si.eviacam.util;

/**
 * Implements a time based blinker with equal duration for the ON/OFF states
 */
public class Blinker {
    // Duration of the ON/OFF state
    private final long mDuration;

    // Time when the blinker was been started (0 means stopped)
    private long mStartTime= 0;

    /**
     * Constructor
     *
     * @param duration duration of the blinker
     */
    public Blinker(int duration) {
        mDuration = duration;
    }

    /**
     * Start the blinker. Do nothing if already started.
     */
    public void start() {
        if (mStartTime!= 0) return;
        mStartTime= System.currentTimeMillis();
    }

    /**
     * Stop the blinker
     */
    public void stop() {
        mStartTime= 0;
    }

    /**
     * Get the current state of the blinker
     *
     * @return true means ON state, false means OFF or disabled
     */
    public boolean getState() {
        return mStartTime == 0 || (((System.currentTimeMillis() - mStartTime) / mDuration) & 1)== 0;
    }
}
