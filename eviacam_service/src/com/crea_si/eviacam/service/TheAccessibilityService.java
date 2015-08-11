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

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentCallbacks;
import android.content.Intent;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.view.accessibility.AccessibilityEvent;

/**
 * The Enable Viacam accessibility service
 */

public class TheAccessibilityService extends AccessibilityService implements ComponentCallbacks {
    
    /**
     * States of the accessibility service
     */
    private static final int STATE_STOP= 0;
    private static final int STATE_CHECKING_OPENCV= 1;
    private static final int STATE_RUNNING= 2;

    // static attribute which holds an instance to the service instance
    private static TheAccessibilityService sAccessibilityService;
    
    // reference to the engine
    private EViacamEngine mEngine;
    
    // reference to the notification management stuff
    private ServiceNotification mServiceNotification;
    
    // stores the states of the service (see comments on init() )
    private int mState= STATE_STOP;

    public TheAccessibilityService() {
        super();
        sAccessibilityService= this;
    }

    private void init() {
        // TODO:
        // http://stackoverflow.com/questions/28752238/accessibilityservice-onunbind-not-always-called-when-running-on-emulator
        
        // check if service has been already started
        // under certain circumstances onUnbind is not called (e.g. running
        // on an emulator happens quite often) and the service continues 
        // running although it shows it is disabled
        // this does not solve the issue but at least the service does not crash
        if (mState != STATE_STOP) {
            EVIACAM.debug("ALREADY RUNNING");
            //stopSelf();
            return;
        }
        
        // set default configuration values if the service is run for the first time
        PreferenceManager.setDefaultValues(this, R.xml.preference_fragment, true);
        
        /**
         * Display splash and detect OpenCV installation. The service from now on waits 
         * until the detection process finishes and a notification is received.
         */
        Intent dialogIntent = new Intent(this, SplashActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
        
        mState = STATE_CHECKING_OPENCV;
    }
    
    /** Called from splash activity to notify the openCV is properly installed */
    public static void initCVReady() {
        TheAccessibilityService s= TheAccessibilityService.sAccessibilityService;
        if (s != null) {
            s.startEngine();
        }
    }
    
    /** Finished the initialization process by starting the engine */
    private void startEngine() {
        if (mState == STATE_RUNNING) return;
        
        // start engine
        mEngine= new EViacamEngine(this);
        
        // add notification
        mServiceNotification= new ServiceNotification(this, mEngine);
        
        // set as foreground service
        startForeground(mServiceNotification.getNotificationId(), 
                mServiceNotification.getNotification(this));
                
        mState = STATE_RUNNING;
    }
    
    private void cleanup() {
        // TODO: handle exceptions properly
        if (mState == STATE_STOP) return;
        
        if (mState == STATE_RUNNING) {
            // stop being foreground service and remove notification
            stopForeground(true);
        
            mServiceNotification.cleanup();
            mServiceNotification= null;
        
            mEngine.cleanup();
            mEngine= null;
        }

        EVIACAM.debugCleanup();

        mState = STATE_STOP;
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
        if (mEngine != null) {
            mEngine.onAccessibilityEvent(event);
        }
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
