package com.crea_si.eviacam.service;

import android.graphics.PointF;

public class VisionPipeline {
    // initialize JNI part. must be called after initializing OpenCV and 
    // before processing frames 
    public static native void init (String cascadeName);
    
    // clean-up JNI part
    public static native void finish ();
    
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