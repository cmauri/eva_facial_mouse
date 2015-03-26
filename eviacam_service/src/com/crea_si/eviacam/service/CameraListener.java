package com.crea_si.eviacam.service;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.MyJavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.WindowManager;

@SuppressWarnings("deprecation")
public class CameraListener implements CvCameraViewListener2 {

    // opencv capture&view facility 
    private CameraBridgeViewBase mCameraView;
    
    // delegate to manage pointer
    private PointerControl mPointerControl;
    
    // physical orientation of the camera (0, 90, 180, 270)
    private int mCameraOrientation= 0;
    
    // orientation sensors listener. keeps updated the actual orientation of the
    // device (independently of the screen orientation)
    private PhysicalOrientation mPhysicalOrientation;
    
    // orientation of the screen
    private int mScreenOrientation= 0;
    
    // callback for camera initialization
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(
            EViacamService.getInstance().getApplicationContext()) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    EVIACAM.debug("OpenCV loaded successfully");
                    
                    // initialize JNI part
                    System.loadLibrary("visionpipeline");
                    
                    // TODO: get cascade path from apk resources
                    VisionPipeline.init("/mnt/sdcard/Download/haarcascade_frontalface_alt2.xml");
                    
                    // start camera capture
                    mCameraView.enableView();
                    
                    // enable sensor listener
                    mPhysicalOrientation.enable();
                } break;
                default:
                {
                    // TODO: manage errors
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    public CameraListener(PointerControl pa) {
        mPointerControl= pa;

        /**
         * The orientation of the camera image. The value is the angle that the camera image needs 
         * to be rotated clockwise so it shows correctly on the display in its natural orientation. 
         * It should be 0, 90, 180, or 270.
         * 
         * For example, suppose a device has a naturally tall screen. The back-facing camera sensor 
         * is mounted in landscape. You are looking at the screen. If the top side of the camera 
         * sensor is aligned with the right edge of the screen in natural orientation, the value 
         * should be 90. If the top side of a front-facing camera sensor is aligned with the right 
         * of the screen, the value should be 270.
         * 
         */
        // TODO: display error when no front camera detected
        Camera.CameraInfo cameraInfo= new CameraInfo();
        for (int i= 0; i<  Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo (i, cameraInfo);
            if (cameraInfo.facing== CameraInfo.CAMERA_FACING_FRONT) {
                mCameraOrientation= cameraInfo.orientation;
                EVIACAM.debug("Detected front camera. Orientation: " + mCameraOrientation);
            }
        }
        
        mScreenOrientation= getScreenOrientation();

        // create physical orientation manager
        mPhysicalOrientation= 
                new PhysicalOrientation(EViacamService.getInstance().getApplicationContext());
 
        // create capture view
        mCameraView= new MyJavaCameraView(EViacamService.getInstance().getApplicationContext(), 
                CameraBridgeViewBase.CAMERA_ID_FRONT);
        
        // set CameraBridgeViewBase parameters        
        // TODO: Damn! It seems that for certain resolutions (for instance 320x240 on a Galaxy Nexus)
        // crashes with a "Callback buffer was too small! error", it works at 352x288
        
        mCameraView.setMaxFrameSize(352, 288);
        mCameraView.enableFpsMeter(); // For testing
        mCameraView.setCvCameraViewListener(this);
        
        // Set View parameters
        mCameraView.setVisibility(SurfaceView.VISIBLE);
    }
    
    /**
     * Returns the rotation of the screen from its "natural" orientation. 
     * 
     * The returned value may be Surface.ROTATION_0 (no rotation), Surface.ROTATION_90, 
     * Surface.ROTATION_180, or Surface.ROTATION_270. For example, if a device has a 
     * naturally tall screen, and the user has turned it on its side to go into a 
     * landscape orientation, the value returned here may be either Surface.ROTATION_90 
     * or Surface.ROTATION_270 depending on the direction it was turned. 
     * 
     * The angle is the rotation of the drawn graphics on the screen, which is the opposite 
     * direction of the physical rotation of the device. For example, if the device is 
     * rotated 90 degrees counter-clockwise, to compensate rendering will be rotated by 
     * 90 degrees clockwise and thus the returned value here will be Surface.ROTATION_90.
     * 
     */
    static
    private int getScreenOrientation() {
        Context c= EViacamService.getInstance().getApplicationContext();
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d= wm.getDefaultDisplay();
        switch (d.getRotation()) {
        case Surface.ROTATION_0: return 0;
        case Surface.ROTATION_90: return 90;
        case Surface.ROTATION_180: return 180;
        case Surface.ROTATION_270: return 270;
        default:
            throw new RuntimeException("wrong screen orientation");
        }
    }
    
    public void StartCamera() {
        // Start OpenCV
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, 
                EViacamService.getInstance().getApplicationContext(), mLoaderCallback);
    }
    
    public void StopCamera() {
        mPhysicalOrientation.disable();
        mCameraView.disableView();
    }

    SurfaceView getCameraSurface(){
        return mCameraView;
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
     * 
     * @see org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2#onCameraFrame(org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame)
     */
    @Override
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        int phyRotation = mCameraOrientation - mPhysicalOrientation.getCurrentOrientation();
        if (phyRotation< 0) phyRotation+= 360;
        
        // TODO: refactor as attribute to avoid an object creation for each frame
        PointF vel = new PointF(0, 0);
        
        // call jni part to track face
        VisionPipeline.processFrame(rgba.getNativeObjAddr(), phyRotation, vel);
        
        // compensate mirror effect
        vel.x= -vel.x;
        
        // calculate equivalent physical device rotation for the current screen orientation
        int equivPhyRotation= 360 - mScreenOrientation;
        if (equivPhyRotation== 360) equivPhyRotation= 0;
       
        // when is a mismatch between physical rotation and screen orientation
        // need to cancel it out (e.g. activity that forces specific screen orientation
        // but the device has not been rotated)
        int diffRotation= equivPhyRotation -  mPhysicalOrientation.getCurrentOrientation();
        if (diffRotation< 0) diffRotation+= 360;
        switch (diffRotation) {
        case 0: 
            // Nothing to be done
            break;
        case 90: {
            float tmp= vel.x;
            vel.x= -vel.y;
            vel.y= tmp;
            break;
        }
        case 180:
            vel.x= -vel.x;
            vel.y= -vel.y;
            break;
        case 270: {
            float tmp= vel.x;
            vel.x= vel.y;
            vel.y= -tmp;
            break;
        }
        default:
            throw new RuntimeException("wrong diffRotation");
        }
             
        // send motion to pointer controller delegate
        mPointerControl.updateMotion(vel);
        
        return rgba;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        mScreenOrientation= getScreenOrientation();
        EVIACAM.debug("Screen rotation changed: " + mScreenOrientation);
    }
}
