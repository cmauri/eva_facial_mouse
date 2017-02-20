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

package com.crea_si.eviacam.slavemode;

import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.api.GamepadButtons;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.PointF;

/**
 * Logic for a on-screen virtual game pad.
 * 
 * Internally works as a circumference of radius 1 centered at (0,0).
 * Stores the location of a virtual pointer looked inside such
 * circumference. 
 */

class GamepadAbs implements OnSharedPreferenceChangeListener {
    // Current pointer position
    private PointF mPointerLocation= new PointF(0, 0);

    // Speed multiplier for the pointer control
    private float mPointerSpeed= 0.05f;

    GamepadAbs() {
        // shared preferences
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
        updateSettings();
    }
    
    public void cleanup() {
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }
    
    private void updateSettings() {
        // get values from shared resources
        mPointerSpeed= (float) Preferences.get().getGamepadAbsSpeed() / 100.0f;
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals(Preferences.KEY_GAMEPAD_ABS_SPEED)) {
            updateSettings();
        }
    }

    /**
     * Updates internal pointer location ensuring that
     * never goes outside the circumference 
     * 
     * @param motion motion vector
     * @return the sector (i.e. button) in which the pointer is
     */
    int updateMotion (PointF motion) {
        mPointerLocation.x+= motion.x * mPointerSpeed;
        mPointerLocation.y+= motion.y * mPointerSpeed;

        /*
         * Restrict pointer inside the circle
         */
        float dist_sq= mPointerLocation.x * mPointerLocation.x +
                       mPointerLocation.y * mPointerLocation.y;

        double alpha= Math.atan2(mPointerLocation.y, mPointerLocation.x);
        if (dist_sq> 1.0f) {
            /* 
             * Compute new pointer location
             */ 
            mPointerLocation.x= (float) Math.cos(alpha);
            mPointerLocation.y= (float) Math.sin(alpha);
            dist_sq= 1.0f;
        }

        /*
         * Get button 
         */
        int newButton= GamepadButtons.PAD_NONE;
        if (dist_sq> GamepadView.INNER_RADIUS_RATIO * GamepadView.INNER_RADIUS_RATIO) {
            // angle 0 points down
            alpha+= Math.PI / 8.0 - Math.PI / 2.0;
            if (alpha< 0.0) alpha+= Math.PI * 2.0;

            newButton= (int) (4 * alpha / Math.PI);
            if (newButton> 7) newButton= 7; // just in case
        }
        return newButton;
    }

    /**
     * Get internal pointer location. 
     *
     * @return reference to the point 
     */
    PointF getPointerLocationNorm() {
        return mPointerLocation;
    }
}
