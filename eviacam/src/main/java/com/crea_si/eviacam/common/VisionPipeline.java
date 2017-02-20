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

import android.graphics.PointF;

public class VisionPipeline {
    /**
     * Initialize JNI part.
     * Must be called after initializing OpenCV and before start processing frames
     * @param cascadeName file path where the haar-cascade file resides
     */
    public static native void init (String cascadeName);

    /**
     * Clean-up JNI part
     */
    public static native void cleanup ();

    /**
     * Entry point to process camera frames
     *
     * @param matAddrGr OpenCV image pointer (mat.getNativeObjAddr())
     * @param rotation rotation (clockwise) in degrees that needs to be applied to the image
     *     before processing it so that the subject appears right.
     *     Valid values: 0, 90, 180, 270.
     * @param flip flip operation before rotation
     *     NONE       (0): no flip
     *     VERTICAL   (1): vertical flip (around X-axis)
     *     HORIZONTAL (2): horizontal flip (around Y-axis)
     * @param vel is updated with the extracted motion for each axis
     * @return true if face detected in the last frame (or few frames ago)
     */
    public static native boolean processFrame (long matAddrGr, int flip, int rotation, PointF vel);
}
