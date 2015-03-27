package com.crea_si.eviacam.service;

import org.opencv.core.Mat;

interface FrameProcessor {
    public void processFrame (Mat rgba);
}