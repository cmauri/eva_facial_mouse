package com.crea_si.eviacam.service;

import org.opencv.core.Mat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PointF;

public class EViacamEngine implements FrameProcessor {

    static private final int NOTIFICATION_ID= 1;
    static private final String NOTIFICATION_FILTER_ACTION= "ENABLE_DISABLE_EVIACAM";
    
    // root overlay view
    private OverlayView mOverlayView;
    
    // layer for drawing the pointer and the dwell click feedback
    PointerLayerView mPointerLayer;
    
    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    // dwell clicking function
    private DwellClick mDwellClick;
    
    // perform actions on the UI using the accessibility API
    AccessibilityAction mAccessibilityAction;
    
    // object which encapsulates rotation and orientation logic
    OrientationManager mOrientationManager;
        
    boolean mRunning= false;
    
    // receiver for notifications
    private BroadcastReceiver mMessageReceiver= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // update notification
            // see here for details: http://stackoverflow.com/a/20142620/3519813
            // TODO: do useful work
            Notification noti= 
                    getNotification(context, context.getText(R.string.stopped));
            
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, noti);
            
            String message = intent.getStringExtra("message");            
            EVIACAM.debug("Got message: " + message);
        }
    };

    private Notification getNotification(Context c, CharSequence text) {
        // notification initialization 
        Intent intent = new Intent(NOTIFICATION_FILTER_ACTION);
        PendingIntent pIntent= PendingIntent.getBroadcast
                (c, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (text == null) text= c.getText(R.string.running);

        Notification noti= new Notification.Builder(c)
            .setContentTitle(c.getText(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pIntent)
            .build();
        
        return noti;
    }
    
    public Notification getNotification(Context c) {
        return getNotification(c, null);
    }
    
    public int getNotificationId() {
        return NOTIFICATION_ID;
    }
    
    public EViacamEngine(Context c) {
        /*
         * UI stuff 
         */

        // create overlay root layer
        mOverlayView= new OverlayView(c);
        
        CameraLayerView cameraLayer= new CameraLayerView(c);
        mOverlayView.addFullScreenLayer(cameraLayer);

        ControlsLayerView controlsLayer= new ControlsLayerView(c);
        mOverlayView.addFullScreenLayer(controlsLayer);
        
        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(c);
        mOverlayView.addFullScreenLayer(mPointerLayer);
        
        /*
         * control stuff
         */
        
        mPointerControl= new PointerControl(c, mPointerLayer);
        
        mDwellClick= new DwellClick(c);
        
        mAccessibilityAction= new AccessibilityAction (controlsLayer);
        
        // create camera & machine vision part
        mCameraListener= new CameraListener(c, this);
        cameraLayer.addCameraSurface(mCameraListener.getCameraSurface());
        
        mOrientationManager= new OrientationManager(c, mCameraListener.getCameraOrientation());
        
        /*
         * register notification receiver
         */
        IntentFilter iFilter= new IntentFilter(NOTIFICATION_FILTER_ACTION);
        c.registerReceiver(mMessageReceiver, iFilter);
        
        /*
         * start processing frames
         */
        mCameraListener.startCamera();

        mRunning= true;
    }
   
    public void cleanup() {
        if (!mRunning) return;
               
        mCameraListener.stopCamera();
        mCameraListener= null;
        
        mOrientationManager.cleanup();
        mOrientationManager= null;

        mDwellClick.cleanup();
        mDwellClick= null;
        
        mPointerControl.cleanup();
        mPointerControl= null;
        
        mOverlayView.cleanup();
        mOverlayView= null;

        mRunning= false;
    }
   
    public void onConfigurationChanged(Configuration newConfig) {
        if (mOrientationManager != null) mOrientationManager.onConfigurationChanged(newConfig);
    }
    
    /*
     * process incoming camera frame 
     * 
     * this method is called from a secondary thread 
     */
    @Override
    public void processFrame(Mat rgba) {
        int phyRotation = mOrientationManager.getPictureRotation();
        
        // call jni part to track face
        PointF motion = new PointF(0, 0);
        VisionPipeline.processFrame(rgba.getNativeObjAddr(), phyRotation, motion);
        
        // compensate mirror effect
        motion.x= -motion.x;
        
        // fix motion orientation according to device rotation and screen orientation 
        mOrientationManager.fixVectorOrientation(motion);
             
        // update pointer location given face motion
        mPointerControl.updateMotion(motion);
        
        // get new pointer location
        PointF pointerLocation= mPointerControl.getPointerLocation();
        
        // dwell clicking update
        boolean clickGenerated= 
                mDwellClick.updatePointerLocation(pointerLocation);
        
        // redraw pointer layer
        mPointerLayer.postInvalidate();
        
        // perform action when needed
        if (clickGenerated) { 
            mAccessibilityAction.performAction(pointerLocation);
        }
    }
}
