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

package com.crea_si.eviacam.util;

/**
 * Time based countdown.
 *
 * Precision of milliseconds.
 */
public class Countdown {
    // last stored value of the millis counter
    private long mLastTimeStamp;

    // time to wait in milliseconds
    private long mTimeToWait;

    /**
     * Constructor
     * @param timeToWait time of the countdown in milliseconds
     */
    public Countdown (long timeToWait) {
        setTimeToWait (timeToWait);
        reset();
    }

    /**
     * Reset the countdown
     */
    public void reset () { mLastTimeStamp= 0; }

    /**
     * Start the countdown
     */
    public void start () {
        mLastTimeStamp= System.currentTimeMillis();
    }

    /**
     * Set the time the countdown will last
     *
     * This call does not restart the countdown
     *
     * @param timeToWait time of the countdown in milliseconds
     */
    public void setTimeToWait (long timeToWait) {
        if (timeToWait < 0) throw new AssertionError();
        mTimeToWait= timeToWait;
    }

    /**
     * Get the time the countdown will last
     *
     * @return time in milliseconds
     */
    @SuppressWarnings("WeakerAccess")
    public long getTimeToWait() {
        return mTimeToWait;
    }

    /**
     * Get the elapsed time since the countdown was restarted
     *
     * @return elapsed time in milliseconds
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - mLastTimeStamp;
    }

    /**
     * Get the remaining for the countdown to expire
     *
     * @return remaining time in milliseconds, 0 if countdown expired
     */
    public long getRemainingTime() {
        long remaining= mTimeToWait - getElapsedTime();
        if (remaining< 0) remaining= 0;
        return remaining;
    }

    /**
     * Get the percent of the countdown completed
     *
     * @return a value between 0 and 100. 0 meaning that the countdown has just
     *         been restarted and 100 that has finished
     */
    public int getElapsedPercent() {
        if (mTimeToWait== 0) return 100;

        long elapsed= System.currentTimeMillis() - mLastTimeStamp;
        
        if (elapsed> mTimeToWait) return 100;
        
        return (int) ((100 * elapsed) / mTimeToWait);        
    }

    /**
     * Check if the countdown has finished
     *
     * @return true is the countdown has finished
     */
    public boolean hasFinished () {
        return (System.currentTimeMillis() - mLastTimeStamp> mTimeToWait);
    }
}
