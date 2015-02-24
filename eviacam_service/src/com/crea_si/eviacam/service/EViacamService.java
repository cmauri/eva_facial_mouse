package com.crea_si.eviacam.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class EViacamService extends AccessibilityService {
    private HeartBeat mHeartBeat;
    private OverlayManager mOverlayManager;
    private CameraListener mCameraListener;

    /**
     * Called when the accessibility service is started
     */
    @Override
    public void onCreate() {
        super.onCreate();
        
        if (EVIACAM.DEBUG) android.os.Debug.waitForDebugger();
        EVIACAM.debug("onCreate");

        mHeartBeat = new HeartBeat(this);
    }

    /**
     * Called every time the service is switched ON
     */
    @Override
    public void onServiceConnected() {
        EVIACAM.debug("onServiceConnected");

        /**
         * Unsubscribe all accessibility events. Cannot be removed directly from
         * @xml/accessibilityservice, otherwise onUnbind and onDestroy // never
         * get called
         */
        setServiceInfo(new AccessibilityServiceInfo());

        if (EVIACAM.DEBUG) {
            // DEBUGGING MESSAGES
            Toast.makeText(this.getApplicationContext(), "onServiceConnected", Toast.LENGTH_SHORT).show();
            mHeartBeat.start();
        }
        
        // set default configuration values if the service is run for the first time
        PreferenceManager.setDefaultValues(this, R.xml.preference_fragment, false);
        
        // Create overlay
        mOverlayManager= new OverlayManager(this.getApplicationContext());
        mOverlayManager.createOverlay();
        
        // create pointer action object
        PointerControl p= new PointerControl(mOverlayManager.getOverlayView(), getApplicationContext());
        
        // Create camera
        mCameraListener= new CameraListener(this, p);
        mOverlayManager.addCameraSurface(mCameraListener.getCameraSurface());
        mCameraListener.StartCamera();
    }

    /**
     * Called when service is switched off
     */
    @Override
    public boolean onUnbind(Intent intent) {
        EVIACAM.debug("onUnbind");
        
        mCameraListener.StopCamera();

        mOverlayManager.destroyOverlay();
        mOverlayManager= null;        
        
        if (EVIACAM.DEBUG) mHeartBeat.stop();
        
        return false;
    }

    /**
     * Called when service is switched off after onUnbind
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        EVIACAM.debug("onDestroy");

        mHeartBeat.stop();

    }

    /**
     * (required) This method is called back by the system when it detects an
     * AccessibilityEvent that matches the event filtering parameters specified
     * by your accessibility service.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        EVIACAM.debug("onAccessibilityEvent");
    }

    /**
     * (required) This method is called when the system wants to interrupt the
     * feedback your service is providing, usually in response to a user action
     * such as moving focus to a different control. This method may be called
     * many times over the lifecycle of your service.
     */
    @Override
    public void onInterrupt() {
        EVIACAM.debug("onInterrupt");
    }
}
