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
import android.os.PowerManager;

/**
 * Power management stuff
 */
class PowerManagement {

    //private int mSleepIterations= SLEEP_ITERATIONS_MIN;

    // time since full power in not locked (i.e. in low energy mode)
    private long mWakeUnlockTStamp= 0;

    // power management lock
    private PowerManager.WakeLock mWakeLook;

    // condition to interrupt sleep
    private boolean mSleepEnabled= true;

    // constructor
    public PowerManagement(Context c) {
        /*
         * Make sure the screen does not switch off
         */
        PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
        mWakeLook = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE , EVIACAM.TAG);
    }

    /**
     * Lock full power mode
     */
    public void lockFullPower() {
        if (!mWakeLook.isHeld()) mWakeLook.acquire();
    }

    /**
     * Unlock full power mode
     */
    public void unlockFullPower() {
        if (mWakeLook.isHeld()) mWakeLook.release();
        mWakeUnlockTStamp= System.currentTimeMillis();
    }

    /**
     * Method used to slowdown a secondary thread
     *
     * To exit this call (in a reasonable time) call setSleepEnabled(false)
     */
    public void sleep() {
        final int SLEEP_DURATION = 500;
        final int SLEEP_ITERATIONS_MIN= 2;
        final int SLEEP_ITERATIONS_MAX= 10;
        final long RAMP_BEGIN_MS= 30000; // 30 seconds
        final long RAMP_END_MS= 60000;   // 60 seconds

        // no need to sleep if running at full power
        if (mWakeLook.isHeld()) return;

        /* After RAMP_BEGIN_MS ms start increasing the number of iterations
         * until RAMP_END_MS ms when the number of iterations it is set to
         * the maximum value.
         */
        long elapsed= System.currentTimeMillis() - mWakeUnlockTStamp;
        int waitIterations;
        if (elapsed< RAMP_BEGIN_MS) {
            waitIterations= SLEEP_ITERATIONS_MIN;
        }
        else if (elapsed< RAMP_END_MS) {
            waitIterations= (int) ((elapsed - RAMP_BEGIN_MS) *
                                   (SLEEP_ITERATIONS_MAX - SLEEP_ITERATIONS_MIN) /
                                   (RAMP_END_MS - RAMP_BEGIN_MS)) + SLEEP_ITERATIONS_MIN;
        }
        else {
            waitIterations= SLEEP_ITERATIONS_MAX;
        }

        try {
            for (int i= 0; mSleepEnabled && i< waitIterations; i++) {
                Thread.sleep(SLEEP_DURATION);
            }
        } catch (InterruptedException e) { /* do nothing */ }
    }

    /**
     * Allows the sleep call to finish
     *
     * @param enabled false to finish sleep call as soon as possible
     */
    public void setSleepEnabled (boolean enabled) {
        mSleepEnabled= enabled;
    }
}
