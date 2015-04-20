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
import android.content.res.Configuration;
import android.graphics.PointF;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

class OrientationManager {
    // orientation sensors listener. keeps updated the actual orientation of the
    // device (independently of the screen orientation)
    private PhysicalOrientation mPhysicalOrientation;
    
    // orientation of the screen
    private int mScreenOrientation= 0;
    
    // the orientation of the camera
    final int mCameraOrientation;
    
    OrientationManager(Context c, int cameraOrientation) {
        // create physical orientation manager
        mPhysicalOrientation= new PhysicalOrientation(c);
        
        // enable sensor listener
        mPhysicalOrientation.enable();
        
        mScreenOrientation= getScreenOrientation();
        
        mCameraOrientation= cameraOrientation;
    }
    
    public void cleanup() {
        mPhysicalOrientation.disable();
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
    static
    private int getScreenOrientation() {
        Context c= EViacamService.getInstance().getApplicationContext();
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d= wm.getDefaultDisplay();
        switch (d.getRotation()) {
        case Surface.ROTATION_0: return 0;
        case Surface.ROTATION_90: return 90;
        case Surface.ROTATION_180: return 180;
        case Surface.ROTATION_270: return 270;
        default:
            throw new RuntimeException("wrong screen orientation");
        }
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        mScreenOrientation= getScreenOrientation();
        EVIACAM.debug("Screen rotation changed: " + mScreenOrientation);
    }
    
    /*
     * given the physical orientation of the device and the mounting orientation of 
     * the camera returns the rotation (clockwise) in degrees that needs to be applied 
     * to the image so that the subject appears upright
     */
    public int getPictureRotation() {
        int phyRotation = mCameraOrientation - mPhysicalOrientation.getCurrentOrientation();
        if (phyRotation< 0) phyRotation+= 360;
        
        return phyRotation;
    }
    
    /*
     * given the screen orientation and the physical orientation of the device 
     * modifies a given a motion vector so that the physical motion of the subject
     * matches the motion of the pointer on the screen
     */
    public void fixVectorOrientation(PointF motion) {
        // calculate equivalent physical device rotation for the current screen orientation
        int equivPhyRotation= 360 - mScreenOrientation;
        if (equivPhyRotation== 360) equivPhyRotation= 0;
       
        // when is a mismatch between physical rotation and screen orientation
        // need to cancel it out (e.g. activity that forces specific screen orientation
        // but the device has not been rotated)
        int diffRotation= equivPhyRotation -  mPhysicalOrientation.getCurrentOrientation();
        if (diffRotation< 0) diffRotation+= 360;
        switch (diffRotation) {
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
            motion.x= motion.y;
            motion.y= -tmp;
            break;
        }
        default:
            throw new RuntimeException("wrong diffRotation");
        }
    }
}
