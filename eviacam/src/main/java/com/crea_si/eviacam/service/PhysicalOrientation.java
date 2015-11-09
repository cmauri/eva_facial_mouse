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
import android.hardware.SensorManager;
import android.view.OrientationEventListener;

import com.crea_si.eviacam.EVIACAM;

class PhysicalOrientation extends OrientationEventListener {
    private int mCurrentOrientation = 0;
    
    public PhysicalOrientation(Context context) {
        super(context, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    /**
     * translates from degrees to 4 orientations
     * 
     * More info:
     * http://www.androidzeitgeist.com/2013/01/fixing-rotation-camera-picture.html
     */
    static private int normalize (int degrees) {
        if (degrees > 315 || degrees <= 45)  return 0;
        if (degrees > 45  && degrees <= 135) return 90;
        if (degrees > 135 && degrees <= 225) return 180;
        if (degrees > 225 && degrees <= 315) return 270;

        throw new RuntimeException("Abnormal orientation reported");
    }
   
    /**
     * callback 
     * 
     * with SensorManager.SENSOR_DELAY_NORMAL the period is ~60ms
     */
    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation != ORIENTATION_UNKNOWN) {
            int newOrientation= normalize(orientation);
            if (newOrientation != mCurrentOrientation) {
                EVIACAM.debug("onOrientationChanged: " + newOrientation + "Need to rotate: " + (270 - newOrientation));
            }
            mCurrentOrientation= newOrientation;
        }
    }

    public int getCurrentOrientation() {
        return mCurrentOrientation;
    }
}
