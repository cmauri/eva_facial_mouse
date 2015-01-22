package com.crea_si.eviacam.service;

public class VisionPipeline {
    public static native void ProcessFrame (long matAddrGr, long matAddrRgba);
}