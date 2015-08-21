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
import android.view.accessibility.AccessibilityEvent;

/**
 * The Enable Viacam accessibility service
 */

public class TheAccessibilityService extends AccessibilityService implements ComponentCallbacks {
    // reference to the engine
    private AccessibilityServiceEngine mEngine;

    // stores whether it was previously initialized (see comments on init() )
    private boolean mInitialized= false;

    private void init() {
        /* TODO  
         * Check if service has been already started. 
         * Under certain circumstances onUnbind is not called (e.g. running
         * on an emulator happens quite often) and the service continues running
         * although it shows it is disabled this does not solve the issue but at
         * least the service does not crash
         *
         * http://stackoverflow.com/questions/28752238/accessibilityservice-onunbind-not-always-called-when-running-on-emulator
         */
        if (mInitialized) {
            EVIACAM.debug("ALREADY RUNNING");
            return;
        }

        EVIACAM.debugInit(this);

        mEngine= EngineManager.getInstance().startAsAccessibilityService(this);
        
        // When the engine is not properly initialized (i.e. is in slave mode)
        // the above call returns null. As is not possible to stop the accessibility
        // service just take into account an avoid further actions.

        mInitialized= true;
    }

    private void cleanup() {
        // TODO: handle exceptions properly
        if (!mInitialized) return;

        if (mEngine!= null) {
            mEngine.cleanup();
            mEngine= null;
        }

        EVIACAM.debugCleanup();

        mInitialized= false;
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
     * many times over the life cycle of your service.
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
