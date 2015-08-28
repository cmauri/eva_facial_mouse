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

public class ShakeDetector {
    private static final int NO_SHAKE= 0;
    private static final int ABOVE_THESHOLD= 1;
    private static final int BOUNCE= 2;
    private static final int BOUNCE_2= 3;
    
    private static final float THRESHOLD= 5.0f;
    
    private float mLastValue;
    
    private int mCurrentState= NO_SHAKE;
    
    public int update (float v) {
        int result= 0;

        if (mCurrentState== NO_SHAKE) {
            if (Math.abs(v)>= THRESHOLD) {
                mCurrentState= ABOVE_THESHOLD;
                // signal a shake
                if (v> 0) result= 1;
                else result= -1;
            }
        }
        else if (mCurrentState== ABOVE_THESHOLD) {
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
