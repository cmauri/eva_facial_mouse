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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.MyCameraBridgeViewBase;
import org.opencv.android.MyCameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.MyJavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.MyCameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.MyOpenCVLoader;
import org.opencv.core.Mat;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.SurfaceView;

import com.crea_si.eviacam.EVIACAM;
import com.crea_si.eviacam.R;


@SuppressWarnings("deprecation")
public class CameraListener implements CvCameraViewListener2 {

    private final Context mContext;
    
    // callback to process frames
    private final FrameProcessor mFrameProcessor;
    
    // opencv capture&view facility 
    private final MyCameraBridgeViewBase mCameraView;
    
    // physical orientation of the camera (0, 90, 180, 270)
    private final int mCameraOrientation;

    // callback for camera initialization
    private final BaseLoaderCallback mLoaderCallback;
    
    /** 
     * Load a resource into a temporary file
     * 
     * @param c - context
     * @param rid - resource id
     * @param suffix - extension of the temporary file
     * @return a File object representing the temporary file
     * @throws IOException
     */
    private static File resourceToTempFile (Context c, int rid, String suffix) 
            throws IOException {
        InputStream is= null;
        OutputStream os= null;
        File outFile = null;
        
        is= c.getResources().openRawResource(rid);
        try {
            outFile = File.createTempFile("tmp", suffix, c.getCacheDir());
            os= new FileOutputStream(outFile);
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            if (outFile != null) {
                outFile.delete();
                outFile= null;
            }
            throw e;
        }
        finally {
            if (is != null) is.close();
            if (os != null) os.close();
        }

        return outFile;
    }

    // Constructor
    public CameraListener(Context c, FrameProcessor fp) {
        mContext= c;
        mFrameProcessor= fp;
        mLoaderCallback = new BaseLoaderCallback(c) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        EVIACAM.debug("OpenCV loaded successfully");
                        
                        // initialize JNI part
                        System.loadLibrary("visionpipeline");
                        
                        /** Load haarcascade from resources */
                        try {
                            File f= resourceToTempFile (mContext, R.raw.haarcascade, "xml");
                            VisionPipeline.init(f.getAbsolutePath());
                            f.delete();
                        }
                        catch (IOException e) {
                            EVIACAM.debug("Cannot write haarcascade temp file. Continuing anyway");
                        }

                        // start camera capture
                        mCameraView.enableView();
                    } break;
                    default:
                    {
                        // TODO: manage errors
                        super.onManagerConnected(status);
                    } break;
                }
            }};

        /*
         * The orientation of the camera image. The value is the angle that the camera image needs 
         * to be rotated clockwise so it shows correctly on the display in its natural orientation. 
         * It should be 0, 90, 180, or 270.
         * 
         * For example, suppose a device has a naturally tall screen. The back-facing camera sensor 
         * is mounted in landscape. You are looking at the screen. If the top side of the camera 
         * sensor is aligned with the right edge of the screen in natural orientation, the value 
         * should be 90. If the top side of a front-facing camera sensor is aligned with the right 
         * of the screen, the value should be 270.
         */
        // TODO: display error when no front camera detected
        int cameraOrientation= 0;
        Camera.CameraInfo cameraInfo= new CameraInfo();
        for (int i= 0; i<  Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo (i, cameraInfo);
            if (cameraInfo.facing== CameraInfo.CAMERA_FACING_FRONT) {
                cameraOrientation= cameraInfo.orientation;
                EVIACAM.debug("Detected front camera. Orientation: " + cameraOrientation);
            }
        }
        mCameraOrientation= cameraOrientation;
    
        // create capture view
        mCameraView= new MyJavaCameraView(mContext, MyCameraBridgeViewBase.CAMERA_ID_FRONT);
        
        // set CameraBridgeViewBase parameters        
        // TODO: Damn! It seems that for certain resolutions (for instance 320x240 on a Galaxy Nexus)
        // crashes with a "Callback buffer was too small! error", it works at 352x288
        
        mCameraView.setMaxFrameSize(352, 288);
        //mCameraView.enableFpsMeter(); // For testing
        mCameraView.setCvCameraViewListener(this);
        
        // Set View parameters
        mCameraView.setVisibility(SurfaceView.VISIBLE);
    }
    
    public void startCamera() {
        // Start OpenCV
        MyOpenCVLoader.initAsync(MyOpenCVLoader.OPENCV_VERSION_2_4_9, mContext, mLoaderCallback);
    }
    
    public void stopCamera() {
        mCameraView.disableView();
    }

    SurfaceView getCameraSurface(){
        return mCameraView;
    }

    int getCameraOrientation() {
        return mCameraOrientation;
    }

    /**
     * Sets the rotation to perfom to the camera image before is displayed
     * in the preview surface
     *
     * @param rotation rotation to perform (clockwise) in degrees
     *                 legal values: 0, 90, 180, or 270
     */
    public void setPreviewRotation (int rotation) {
        mCameraView.setPreviewRotation(rotation);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        EVIACAM.debug("onCameraViewStarted");
    }

    @Override
    public void onCameraViewStopped() {
        EVIACAM.debug("onCameraViewStopped");
        
        // finish JNI part
        VisionPipeline.cleanup();
    }
     
    /*
     * called each time new frame is grabbed 
     */
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        
        mFrameProcessor.processFrame(rgba);
        
        return rgba;
    }

    /**
     * Enable or disable camera viewer refresh to save CPU cycles
     * @param v true to enable update, false to disable
     */
    public void setUpdateViewer(boolean v) {
        if (mCameraView!= null) mCameraView.setUpdateViewer(v);
    }
}
