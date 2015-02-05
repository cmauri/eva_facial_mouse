package com.crea_si.eviacam.service;

import android.graphics.PointF;

public class VisionPipeline {
    // initialize JNI part. must be called after initializing OpenCV and 
    // before processing frames 
    public static native void init (String cascadeName);
    
    // clean-up JNI part
    public static native void finish ();
    
    // process a frame, parameter vel is updated with the extracted
    // motion for each axis 
    public static native void processFrame (long matAddrGr, PointF vel);
}