package org.opencv.android;

/**
 * Thrown when the camera device could not be found, queried or opened.
 *
 * Note that since API level 21 a newer camera API is provided (android.hardware.camera2)
 * which includes its own exception (CameraAccessException). Since we target API level 16
 * or higher we do not use it.
 */
public class CameraException extends java.lang.Exception {
    /**
     * There is no available cameras.
     */
    public static final int NO_CAMERAS_AVAILABLE= 0x1;
    /**
     * An error occurred when opening the camera.
     */
    public static final int CAMERA_ERROR= 0x2;

    // the problem id
    private final int mProblem;

    public CameraException (int problem) {
        super();
        mProblem= problem;
    }

    public CameraException (int problem, String detailMessage) {
        super(detailMessage);
        mProblem= problem;
    }

    public CameraException (int problem, String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
        mProblem= problem;
    }

    public int getProblem() { return mProblem; }
}
