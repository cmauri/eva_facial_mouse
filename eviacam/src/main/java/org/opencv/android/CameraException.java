package org.opencv.android;

/**
 * Thrown when the camera device could not be found, queried or opened.
 *
 * Note that since API level 21 a newer camera API is provided (android.hardware.camera2)
 * which includes its own exception (CameraAccessException). Since we target API level 16
 * or higher we do not use it.
 */
@SuppressWarnings("WeakerAccess")
public class CameraException extends java.lang.Exception {
    /**
     * The device has no cameras available
     */
    public static final int NO_CAMERAS_AVAILABLE = 1;

    /**
     * The camera is being used
     */
    public static final int CAMERA_IN_USE = 2;

    /**
     * The the device's cameras have been disabled for this user
     */
    public static final int CAMERA_DISABLED = 3;

    /**
     * General camera error
     */
    public static final int CAMERA_ERROR = -1;

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
