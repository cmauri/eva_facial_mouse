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
    private static final int SLEEP_DURATION = 500;
    private int mSleepIterations= 2;

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
    }

    /**
     * Method used to slowdown the secondary thread
     * To stop sleeping call setSleepEnabled with enabled= false
     */
    public void sleep() {
        try {
            for (int i= 0; mSleepEnabled && i< mSleepIterations; i++) {
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
