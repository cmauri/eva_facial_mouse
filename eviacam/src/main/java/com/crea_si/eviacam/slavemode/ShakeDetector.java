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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.crea_si.eviacam.common.Preferences;

/**
 * Detect (face) shakes 
 * 
 * It analyzes the progression of an input until its absolute 
 * value is larger than a specified threshold. After that filters
 * out motion in the opposite direction (i.e. bounce).  
 */
class ShakeDetector implements OnSharedPreferenceChangeListener {
    /*
     * States of the detector
     */
    private static final int NO_SHAKE= 0;
    private static final int ABOVE_THRESHOLD = 1;
    private static final int BOUNCE= 2;
    private static final int BOUNCE_2= 3;

    // The threshold
    private float mThreshold;

    // Previous value
    private float mLastValue;

    // Current state
    private int mCurrentState= NO_SHAKE;

    ShakeDetector () {
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
        /*
         * The following function is used:
         * 
         * y= A*e^(x*B)-A+C  (defined for x>= 0)
         *  
         * where
         * 
         * x is the input value (linear)
         * y is the output value (exp)
         * A is the "speed" constant (for large values the above function grows faster)
         * B and C are constants determined by two points the function should cross
         * 
         *  p1=(min_linear, min_exp)
         *  p2=(max_linear, max_exp)
         *  
         * where min_linear>= 0, max_linear>0 and max_exp> min_exp
         * 
         * For simplicity min_linear= 0. This yields the following solutions for
         * the equation system:
         *  C= min_exp
         *  B= (1/max_lin) * ln ((A+max_exp-min_exp)/A)
         *  
         * The inverse function is:
         *  x= (1/B) * ln ((y+A-C)/A)
         */

        // The A constant determined empirically
        final double A= 0.08;

        // Get values from shared resources
        final float x= (float) Preferences.get().getGamepadRelSensitivity();

        // Compute threshold
        mThreshold= (float) (A * Math.exp(x*0.1*Math.log((A+1.0)/A))-A);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals(Preferences.KEY_GAMEPAD_REL_SENSITIVITY)) {
            updateSettings();
        }
    }
    
    /**
     * Update the value
     * 
     * @param v new motion value
     * @return 0 if no shake detected
     *         1 if shake detected (positive direction)
     *        -1 if shake detected (negative direction)
     */
    public int update (final float v) {
        int result= 0;

        if (mCurrentState== NO_SHAKE) {
            if (Math.abs(v)>= mThreshold) {
                mCurrentState= ABOVE_THRESHOLD;
                // signal a shake
                if (v> 0) result= 1;
                else result= -1;
            }
        }
        else if (mCurrentState== ABOVE_THRESHOLD) {
            // Continue following until there is direction change
            if (mLastValue> 0) {
                if (mLastValue> v) mCurrentState= BOUNCE;
            }
            else {
                if (mLastValue< v) mCurrentState= BOUNCE;
            }
        }
        else if (mCurrentState== BOUNCE) {
            // Wait until crosses zero
            if (mLastValue== 0 || mLastValue*v< 0) {
                mCurrentState= BOUNCE_2;
            }
        }
        else if (mCurrentState== BOUNCE_2) {
            // Wait until crosses zero again
            if (mLastValue== 0 || mLastValue*v< 0) {
                mCurrentState= NO_SHAKE;
            }
        }

        mLastValue= v;

        return result;
    }
}
