package com.crea_si.eviacam.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentCallbacks;
import android.content.Intent;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

public class EViacamService extends AccessibilityService implements ComponentCallbacks {
    
    // static attribute which holds an instance to the service instance
    private static AccessibilityService sAccessibilityService;
    
    // for debugging, shows a toast at certain intervals so that we know 
    // the service is still alive
    private HeartBeat mHeartBeat;
    
    // reference to the engine
    private EViacamEngine mEngine;
    
    // reference to the notification management stuff
    private ServiceNotification mServiceNotification;
    
    // stores whether the service is running or not (see comments on init() )
    private boolean mRunning= false;

    public EViacamService() {
        super();
        sAccessibilityService= this;
    }
    
    public static AccessibilityService getInstance() {
        return sAccessibilityService;
    }
    
    private void init() {
        // TODO: handle exceptions properly
        
        // TODO:
        // http://stackoverflow.com/questions/28752238/accessibilityservice-onunbind-not-always-called-when-running-on-emulator
        
        // check if service has been already started
        // under certain circumstances onUnbind is not called (e.g. running
        // on an emulator happens quite often) and the service continues 
        // running although it shows it is disabled
        // this does not solve the issue but at least the service does not crash
        if (mRunning) {
            EVIACAM.debug("ALREADY RUNNING");
            //stopSelf();
            return;
        }
        
        /**
         * Unsubscribe all accessibility events. Cannot be removed directly from
         * @xml/accessibilityservice, otherwise onUnbind and onDestroy // never
         * get called
         */
        setServiceInfo(new AccessibilityServiceInfo());

        if (EVIACAM.DEBUG) {
            // debugging stuff
            android.os.Debug.waitForDebugger();
            Toast.makeText(getApplicationContext(), "onServiceConnected", Toast.LENGTH_SHORT).show();
            mHeartBeat = new HeartBeat(this);
            mHeartBeat.start();
        }
        
        // set default configuration values if the service is run for the first time
        PreferenceManager.setDefaultValues(this, R.xml.preference_fragment, false);
     
        // start engine
        mEngine= new EViacamEngine(this);
        
        // add notification
        mServiceNotification= new ServiceNotification(this, mEngine);
        
        // set as foreground service
        startForeground(mServiceNotification.getNotificationId(), 
                mServiceNotification.getNotification(this));
                
        mRunning= true;
    }
    
    private void cleanup() {
        // TODO: handle exceptions properly
        if (!mRunning) return;
        
        // stop being foreground service and remove notification
        stopForeground(true);
        
        mServiceNotification.cleanup();
        mServiceNotification= null;
        
        mEngine.cleanup();
        mEngine= null;
        
        if (EVIACAM.DEBUG) {
            mHeartBeat.stop();
            mHeartBeat= null;
        }
        
        mRunning= false;
    }
    
    /**
     * Called when the accessibility service is started
     */
    @Override
    public void onCreate() {
        super.onCreate();
        
        EVIACAM.debug("onCreate");
    }

    /**
     * Called every time the service is switched ON
     */
    @Override
    public void onServiceConnected() {
        EVIACAM.debug("onServiceConnected");
        
        init();
    }

    /**
     * Called when service is switched off
     */
    @Override
    public boolean onUnbind(Intent intent) {
        EVIACAM.debug("onUnbind");
        
        cleanup();
        
        return false;
    }

    /**
     * Called when service is switched off after onUnbind
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        EVIACAM.debug("onDestroy");

        cleanup();
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
    
    /*
     * Called by the system when the device configuration changes
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (mEngine != null) {
            mEngine.onConfigurationChanged(newConfig);
        }
    }
}
