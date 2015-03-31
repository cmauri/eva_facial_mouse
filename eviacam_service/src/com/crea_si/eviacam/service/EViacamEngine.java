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
import android.view.View;

public class EViacamEngine implements FrameProcessor {

    /*
     * constants for notifications
     */
    private static final int NOTIFICATION_ID= 1;
    private static final String NOTIFICATION_FILTER_ACTION= "ENABLE_DISABLE_EVIACAM";
    private static final int NOTIFICATION_ACTION_PAUSE= 0;
    private static final int NOTIFICATION_ACTION_RESUME= 1;
    private static final String NOTIFICATION_ACTION_NAME= "action";
    
    /*
     * states of the engine
     */
    static private final int STATE_NONE= 0;
    static private final int STATE_RUNNING= 1;
    static private final int STATE_PAUSED= 2;
    static private final int STATE_STOPPED= 3;
    
    // root overlay view
    private OverlayView mOverlayView;
    
    // layer for drawing the pointer and the dwell click feedback
    private PointerLayerView mPointerLayer;
    
    // layer for drawing different controls
    private ControlsLayerView mControlsLayer;
    
    // object in charge of capturing & processing frames
    private CameraListener mCameraListener;
    
    // object which provides the logic for the pointer motion and actions 
    private PointerControl mPointerControl;
    
    // dwell clicking function
    private DwellClick mDwellClick;
    
    // perform actions on the UI using the accessibility API
    private AccessibilityAction mAccessibilityAction;
    
    // object which encapsulates rotation and orientation logic
    private OrientationManager mOrientationManager;
    
    // current engine state
    private int mCurrentState= STATE_NONE;
    
    // receiver for notifications
    private BroadcastReceiver mMessageReceiver= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // update notification
            int action= intent.getIntExtra(NOTIFICATION_ACTION_NAME, -1);
            Notification noti;
            
            if (action == NOTIFICATION_ACTION_PAUSE) {
                pause();
                noti= getNotification(context, NOTIFICATION_ACTION_RESUME);
                EVIACAM.debug("Got intent: PAUSE");
            }
            else if (action == NOTIFICATION_ACTION_RESUME) {
                resume();
                noti= getNotification(context, NOTIFICATION_ACTION_PAUSE);
                EVIACAM.debug("Got intent: RESUME");
            }
            else {
                // ignore intent
                EVIACAM.debug("Got unknown intent");
                return;
            }
                    
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, noti);
        }
    };

    private Notification getNotification(Context c, int action) {
        // notification initialization 
        Intent intent = new Intent(NOTIFICATION_FILTER_ACTION);
        intent.putExtra(NOTIFICATION_ACTION_NAME, action);
        
        PendingIntent pIntent= PendingIntent.getBroadcast
                (c, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        CharSequence text;
        if (action == NOTIFICATION_ACTION_PAUSE) {
            text= c.getText(R.string.running_click_to_pause);
        }
        else {
            text= c.getText(R.string.stopped_click_to_resume);
        }

        Notification noti= new Notification.Builder(c)
            .setContentTitle(c.getText(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pIntent)
            .build();
        
        return noti;
    }
    
    public Notification getNotification(Context c) {
        return getNotification(c, NOTIFICATION_ACTION_PAUSE);
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

        DockPanelLayerView dockPanelView= new DockPanelLayerView(c);
        mOverlayView.addFullScreenLayer(dockPanelView);
        
        mControlsLayer= new ControlsLayerView(c);
        mOverlayView.addFullScreenLayer(mControlsLayer);
        
        // pointer layer (should be the last one)
        mPointerLayer= new PointerLayerView(c);
        mOverlayView.addFullScreenLayer(mPointerLayer);
        
        /*
         * control stuff
         */
        
        mPointerControl= new PointerControl(c, mPointerLayer);
        
        mDwellClick= new DwellClick(c);
        
        mAccessibilityAction= new AccessibilityAction (mControlsLayer, dockPanelView);
        
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

        mCurrentState= STATE_RUNNING;
    }
   
    public void cleanup() {
        if (mCurrentState == STATE_STOPPED) return;
               
        mCameraListener.stopCamera();
        mCameraListener= null;
        
        mOrientationManager.cleanup();
        mOrientationManager= null;

        mDwellClick.cleanup();
        mDwellClick= null;
        
        mPointerControl.cleanup();
        mPointerControl= null;
        
        // nothing to be done for mPointerLayer
        
        mOverlayView.cleanup();
        mOverlayView= null;

        mCurrentState= STATE_STOPPED;
    }
   
    private void pause() {
        if (mCurrentState != STATE_RUNNING) return;
        
        // TODO: this is a basic method to pause the program
        // pause/resume should reset internal state of some objects 
        mCurrentState= STATE_PAUSED;
        mControlsLayer.setVisibility(View.INVISIBLE);
        mPointerLayer.setVisibility(View.INVISIBLE);
    }
    
    private void resume() {
        if (mCurrentState != STATE_PAUSED) return;
        
        // TODO: see comment on pause()
        mPointerLayer.setVisibility(View.VISIBLE);
        mControlsLayer.setVisibility(View.VISIBLE);        
        mCurrentState= STATE_RUNNING;
        
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
        if (mCurrentState != STATE_RUNNING) return;
        
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
