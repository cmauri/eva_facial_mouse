/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-16 Cesar Mauri Loba (CREA Software Systems)
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

import android.content.Context;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;

/**
 * Class to receive notifications from the SensorManager and keep updated the
 * physical orientation of the device.
 *
 * It translates from a range from 0 to 359 degrees, to a four main orientation
 * values: 0, 90, 180 and 270. Orientation is 0 degrees when the device is oriented
 * in its natural position, 90 degrees when its left side is at the top, 180 degrees
 * when it is upside down, and 270 degrees when its right side is to the top.
 */
public class PhysicalOrientation extends OrientationEventListener {
    private int mCurrentOrientation = 0;
    
    public PhysicalOrientation(Context context) {
        super(context, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
    /**
     * It translates from a range from 0 to 359 degrees, to a four main orientation
     * values
     * 
     * See: http://www.androidzeitgeist.com/2013/01/fixing-rotation-camera-picture.html
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
            mCurrentOrientation= normalize(orientation);
        }
    }

    public int getCurrentOrientation() {
        return mCurrentOrientation;
    }
}
