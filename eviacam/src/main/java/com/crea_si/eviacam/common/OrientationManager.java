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

package com.crea_si.eviacam.common;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.view.Surface;
import android.view.WindowManager;

import com.crea_si.eviacam.util.FlipDirection;
import com.crea_si.eviacam.util.PhysicalOrientation;

/**
 * Encapsulates the all the orientation related stuff
 */
class OrientationManager {

    private final WindowManager mWindowManager;

    // Whether the captured frame needs to be flipped before rotating (depends on hardware)
    private final FlipDirection mCameraFlip;

    // The physical orientation of the camera (depends on hardware)
    private final int mCameraOrientation;

    // Receive notifications from the SensorManager and keep updated the physical
    // orientation of the device (which is different from the screen orientation)
    private PhysicalOrientation mPhysicalOrientation;

    // constructor
    OrientationManager(@NonNull Context c, @NonNull FlipDirection flip, int cameraOrientation) {
        mWindowManager= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);

        mCameraFlip= flip;

        mCameraOrientation= cameraOrientation;

        // create physical orientation manager and start listening sensors
        mPhysicalOrientation= new PhysicalOrientation(c);
        mPhysicalOrientation.enable();
    }

    public void cleanup() {
        if (mPhysicalOrientation!= null) {
            mPhysicalOrientation.disable();
            mPhysicalOrientation = null;
        }
    }

    /**
     * Returns the rotation of the screen from its "natural" orientation. 
     * 
     * The returned value may be Surface.ROTATION_0 (no rotation), Surface.ROTATION_90, 
     * Surface.ROTATION_180, or Surface.ROTATION_270. For example, if a device has a 
     * naturally tall screen, and the user has turned it on its side to go into a 
     * landscape orientation, the value returned here may be either Surface.ROTATION_90 
     * or Surface.ROTATION_270 depending on the direction it was turned. 
     * 
     * The angle is the rotation of the drawn graphics on the screen, which is the opposite 
     * direction of the physical rotation of the device. For example, if the device is 
     * rotated 90 degrees counter-clockwise, to compensate rendering will be rotated by 
     * 90 degrees clockwise and thus the returned value here will be Surface.ROTATION_90.
     * 
     */
    private int getScreenOrientation() {
        switch (mWindowManager.getDefaultDisplay().getRotation()) {
        case Surface.ROTATION_0: return 0;
        case Surface.ROTATION_90: return 90;
        case Surface.ROTATION_180: return 180;
        case Surface.ROTATION_270: return 270;
        default:
            throw new RuntimeException("wrong screen orientation");
        }
    }

    /*
     * (Method removed, comments left for future reference only)
     *
     * In theory, this method is called according to android:configChanges which should be
     *
     *     android:configChanges="orientation|screenSize"
     *
     * (see: http://developer.android.com/intl/es/guide/topics/manifest/activity-element.html)
     *
     * HOWEVER, in our experience this method is not always called when "The screen orientation
     * has changed — the user has rotated the device.", in particular when you flip the device
     * (note that screen size does not change). Therefore, we cannot rely on this listener
     * to keep the screen orientation value up to date and finally we need to query the screen
     * orientation each time is needed.
     */
    /*
    public void onConfigurationChanged(Configuration newConfig) {
        mScreenOrientation= getScreenOrientation(mContext);
    }*/

    /**
     * Some devices, such as the Lenovo YT3-X50L, have a rotating camera. For those devices
     * we need to flip the image before is rotated.
     *
     * @return FlipDirection reference
     */
    @NonNull
    FlipDirection getPictureFlip() {
        return mCameraFlip;
    }

    /**
     * Given the physical orientation of the device and the mounting orientation of
     * the camera compute rotation.
     *
     * @return the rotation (clockwise) in degrees that needs to be applied
     * to the image so that the subject appears upright
     */
    int getPictureRotation() {
        int phyRotation = mCameraOrientation - mPhysicalOrientation.getCurrentOrientation();
        if (phyRotation< 0) phyRotation+= 360;
        
        return phyRotation;
    }

    /**
     * Given the screen orientation and the physical orientation of the device,
     * return the rotation that needs to be applied to a motion vector so that the
     * physical motion of the subject matches the motion of the pointer on the screen
     */
    private int getDiffRotation () {
        // calculate equivalent physical device rotation for the current screen orientation
        int equivPhyRotation= 360 - getScreenOrientation();
        if (equivPhyRotation== 360) equivPhyRotation= 0;

        // when is a mismatch between physical rotation and screen orientation
        // need to cancel it out (e.g. activity that forces specific screen orientation
        // but the device has not been rotated)
        int diffRotation= equivPhyRotation -  mPhysicalOrientation.getCurrentOrientation();
        if (diffRotation< 0) diffRotation+= 360;

        return diffRotation;
    }

    /**
     * Given the screen orientation and the physical orientation of the device
     * modifies a given a motion vector so that the physical motion of the subject
     * matches the motion of the pointer on the screen
     */
    void fixVectorOrientation(@NonNull PointF motion) {
        switch (getDiffRotation()) {
        case 0: 
            // Nothing to be done
            break;
        case 90: {
            float tmp= motion.x;
            motion.x= -motion.y;
            motion.y= tmp;
            break;
        }
        case 180:
            motion.x= -motion.x;
            motion.y= -motion.y;
            break;
        case 270: {
            float tmp= motion.x;
            //noinspection SuspiciousNameCombination
            motion.x= motion.y;
            motion.y= -tmp;
            break;
        }
        default:
            throw new RuntimeException("wrong diffRotation");
        }
    }
}
