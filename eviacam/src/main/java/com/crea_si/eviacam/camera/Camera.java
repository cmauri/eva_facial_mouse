/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
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
package com.crea_si.eviacam.camera;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceView;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.common.VisionPipeline;
import com.crea_si.eviacam.util.FlipDirection;

import org.opencv.android.CameraException;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Camera interface class
 */
public class Camera {
    private static final String TAG= "camera.Camera";

    /* Desired capture size */
    static final int DESIRED_CAPTURE_WIDTH = 352;
    static final int DESIRED_CAPTURE_HEIGHT = 288;

    /* Camera API */
    private static final int CAMERA_API_LEGACY = 1;
    private static final int CAMERA_API_2 = 2;

    // legacy camera interface
    private CameraListener mCameraLegacy;

    // camera 2 interface
    private Camera2Listener mCamera2;

    /**
     * Constructs and detects the best camera available. Takes care of selecting the most
     * appropriate interface to acces the camera.
     *
     * @param c context
     * @param fp frame processor listener
     * @throws CameraException when error
     */
    public Camera (@NonNull Context c, @NonNull FrameProcessor fp) throws CameraException {
        /*
          In previous versions we used the OpenCV async helper, but we found
          problems with devices running Android arm64 (e.g. Huawei P8) due
          to missing OpenCV libraries. To avoid such problems we included the
          OpenCV binaries in the App apk
        */
        if (!OpenCVLoader.initDebug()) {
            throw new RuntimeException("Cannot initialize OpenCV");
        }

        Log.i(TAG, "OpenCV loaded successfully");

        // initialize JNI part
        System.loadLibrary("visionpipeline");

        /* Load haarcascade from resources */
        try {
            File f= resourceToTempFile (c, R.raw.haarcascade, "xml");
            VisionPipeline.init(f.getAbsolutePath());
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
        catch (IOException e) {
            Log.e(TAG, "Cannot write haar cascade temp file. Continuing anyway");
        }

        /*
            Select the best API to access the camera
        */
        if (whichCameraAPI()== CAMERA_API_LEGACY) {
            mCameraLegacy = new CameraListener(c, fp, DESIRED_CAPTURE_WIDTH, DESIRED_CAPTURE_HEIGHT);
            mCameraLegacy.setPreviewFlip(mCameraLegacy.getCameraFlip());
        }
        else {
            mCamera2= new Camera2Listener(c, fp);
        }
    }

    /**
     * Get if the current device provides camera2 API
     *
     * @return true when camera2 is available
     */
    public static boolean hasCamera2Support() {
        return (Build.VERSION.SDK_INT>= 21);
    }

    /**
     * Selects the best camera API to use
     *
     * @return camera API ID
     */
    private int whichCameraAPI () {
        if (!hasCamera2Support()) return CAMERA_API_LEGACY;

        switch (Preferences.get().getUseCamera2API()) {
            case NO: return CAMERA_API_LEGACY;
            case YES: return CAMERA_API_2;
        }

        /*
         *  This is the auto case
         */

        // This device has been reported to work only with camera2 API
        if (null!= Build.DEVICE && Build.DEVICE.equals("OnePlus3T") && Build.VERSION.SDK_INT>= 24) {
            return CAMERA_API_2;
        }
        return CAMERA_API_LEGACY;
    }

    /**
     * Cleanup
     */
    public void cleanup() {
        if (null != mCameraLegacy) {
            mCameraLegacy.stopCamera();
            mCameraLegacy= null;
        }

        if (null != mCamera2) {
            mCamera2.cleanup();
            mCamera2= null;
        }

        // finish JNI part
        VisionPipeline.cleanup();

        Log.d(TAG, "cleanup: completed");
    }

    /**
     * Return whether the frame needs a flip operation. For instance, this is needed for those
     * devices with rotating camera such as the Lenovo YT3-X50L which are reported as BACK camera.
     *
     * @return NONE, HORIZONTAL or VERTICAL
     */
    public @NonNull FlipDirection getCameraFlip() {
        if (null != mCameraLegacy) return mCameraLegacy.getCameraFlip();
        if (null != mCamera2) return mCamera2.getCameraFlip();
        return FlipDirection.NONE;
    }

    /**
     * Get the physical orientation of the camera
     * @return 0, 90, 180 or 270
     */
    public int getCameraOrientation() {
        if (null != mCameraLegacy) return mCameraLegacy.getCameraOrientation();
        if (null != mCamera2) return mCamera2.getCameraOrientation();
        return 0;
    }

    /**
     * Sets the rotation to perform to the camera image before is displayed
     * in the preview surface
     *
     * @param rotation rotation to perform (clockwise) in degrees
     *                 legal values: 0, 90, 180, or 270
     */
    public void setPreviewRotation (int rotation) {
        if (null != mCameraLegacy) mCameraLegacy.setPreviewRotation(rotation);
        else if (null != mCamera2) mCamera2.setPreviewRotation(rotation);
        else {
            throw new IllegalStateException("setPreviewRotation: Camera no initialized");
        }
    }

    /**
     * Start camera capture
     *
     * Once started, the client is notified using the onCameraStarted callback. When
     * error, the onCameraError is called.
     */
    public void startCamera() {
        if (null != mCameraLegacy) mCameraLegacy.startCamera();
        else if (null != mCamera2) mCamera2.startCamera();
        else {
            throw new IllegalStateException("startCamera: Camera no initialized");
        }
    }

    /**
     * Stop the camera capture.
     *
     * TODO: for legacy camera, wait until the camera has stopped completely
     */
    public void stopCamera() {
        if (null != mCameraLegacy) mCameraLegacy.stopCamera();
        else if (null != mCamera2) mCamera2.stopCamera();
        else {
            throw new IllegalStateException("stopCamera: Camera no initialized");
        }
    }

    /**
     * Return surface view on which camera image will be drawn
     *
     * @return the surface view or null
     */
    public @Nullable SurfaceView getCameraSurface() {
        if (null != mCameraLegacy) return mCameraLegacy.getCameraSurface();
        if (null != mCamera2) return mCamera2.getCameraSurface();
        return null;
    }

    /**
     * Load a resource into a temporary file
     *
     * @param c - context
     * @param rid - resource id
     * @param suffix - extension of the temporary file
     * @return a File object representing the temporary file
     * @throws IOException when failed
     */
    private static File resourceToTempFile (Context c, int rid, String suffix)
            throws IOException {
        InputStream is;
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
                //noinspection ResultOfMethodCallIgnored
                outFile.delete();
                outFile= null;
            }
            throw e;
        }
        finally {
            try {
                if (is != null) is.close();
            }
            catch (IOException e) {
                // Do nothing
            }
            try {
                if (os != null) os.close();
            }
            catch (IOException e) {
                // Do nothing
            }
        }

        return outFile;
    }
}
