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

import android.graphics.PointF;

public class VisionPipeline {
    // initialize JNI part. must be called after initializing OpenCV and 
    // before processing frames 
    public static native void init (String cascadeName);
    
    // clean-up JNI part
    public static native void cleanup ();
    
    /*
     * entry point to process camera frames
     *
     * rotation: rotation (clockwise) in degrees that needs to be applied to the image
     *     before processing it so that the subject appears right.
     *     Valid values: 0, 90, 180, 270.
     *
     * vel is updated with the extracted motion for each axis 
     */
    public static native void processFrame (long matAddrGr, int rotation, PointF vel);
}