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
package com.crea_si.eviacam.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;

import com.crea_si.eviacam.common.EVIACAM;

/**
 * Power management stuff
 */
public class PowerManagement extends BroadcastReceiver {
    interface OnScreenStateChangeListener {
        void onOnScreenStateChange ();
    }

    private final Context mContext;

    private final OnScreenStateChangeListener mOnScreenStateChangeListener;

    // power management lock
    private PowerManager.WakeLock mWakeLook;

    // condition to interrupt sleep
    private volatile boolean mSleepEnabled= true;

    // screen status (we assume the screen is on at startup)
    private boolean mScreenOn;

    // constructor
    public PowerManagement(Context c, OnScreenStateChangeListener l) {
        mContext= c;
        mOnScreenStateChangeListener= l;

        final PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);

        // Lock for the screen to avoid switching off
        mWakeLook = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP |
                PowerManager.ON_AFTER_RELEASE , EVIACAM.TAG);

        // Current screen status
        mScreenOn = pm.isScreenOn();

        /*
         * Register screen power broadcast receiver
         */
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        c.registerReceiver(this, filter);
    }

    public void cleanup() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            mScreenOn = false;

        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            mScreenOn = true;
        }
        if (mOnScreenStateChangeListener != null) {
            mOnScreenStateChangeListener.onOnScreenStateChange();
        }
    }

    public boolean getScreenOn () { return mScreenOn; }

    /**
     * Lock full power mode
     */
    public void lockFullPower() {
        if (!mWakeLook.isHeld()) mWakeLook.acquire();

        // Set here to true to avoid the delay of the broadcast receiver
        mScreenOn= true;
    }

    /**
     * Unlock full power mode
     */
    public void unlockFullPower() {
        if (mWakeLook.isHeld()) mWakeLook.release();
    }

    /**
     * Method used to slowdown a secondary thread
     *
     * To exit this call (in a reasonable time) call setSleepEnabled(false)
     */
    public void sleep() {
        final int SLEEP_DURATION = 500;
        final int SLEEP_ITERATIONS_MAX= 10;

        try {
            for (int i= 0; mSleepEnabled && i< SLEEP_ITERATIONS_MAX; i++) {
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
