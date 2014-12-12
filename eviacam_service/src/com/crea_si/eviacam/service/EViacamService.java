package com.crea_si.eviacam.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.util.Log;

public class EViacamService extends AccessibilityService {
    private static final String TAG = "EViacamService";
    private HeartBeat mHeartBeat;
    private LayoutManager mLayoutManager;

    /**
     * Called when the accessibility service is started
     */
    @Override
    public void onCreate() {
        super.onCreate();
        android.os.Debug.waitForDebugger();
        Log.v(TAG, "onCreate");

        mHeartBeat = new HeartBeat(this);
    }

    /**
     * Called every time the service is switched ON
     */
    @Override
    public void onServiceConnected() {
        Log.v(TAG, "onServiceConnected");

        /**
         * Unsubscribe all accessibility events. Cannot be removed directly from
         * @xml/accessibilityservice, otherwise onUnbind and onDestroy // never
         * get called
         */
        setServiceInfo(new AccessibilityServiceInfo());

        Toast.makeText(this.getApplicationContext(), "onServiceConnected", Toast.LENGTH_SHORT).show();
        mHeartBeat.start();
        
        mLayoutManager= new LayoutManager(this.getApplicationContext());
        mLayoutManager.createFeedbackOverlay();
    }
    

    /**
     * Called when service is switched off
     */
    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind");

        mLayoutManager.destroyFeedbackOverlay();
        mLayoutManager= null;        
        
        mHeartBeat.stop();
        
        return false;
    }

    /**
     * Called when service is switched off after onUnbind
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.v(TAG, "onDestroy");

        mHeartBeat.stop();

    }

    /**
     * (required) This method is called back by the system when it detects an
     * AccessibilityEvent that matches the event filtering parameters specified
     * by your accessibility service.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "onAccessibilityEvent");
    }

    /**
     * (required) This method is called when the system wants to interrupt the
     * feedback your service is providing, usually in response to a user action
     * such as moving focus to a different control. This method may be called
     * many times over the lifecycle of your service.
     */
    @Override
    public void onInterrupt() {
        Log.i(TAG, "onInterrupt");
    }
}
