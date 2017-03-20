/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
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
package com.crea_si.eviacam.a11yservice;

import com.crea_si.eviacam.common.CoreEngine;
import com.crea_si.eviacam.common.EVIACAM;
import com.crea_si.eviacam.common.MouseEmulation;

import android.accessibilityservice.AccessibilityService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Engine implementation for the accessibility service which provides
 * a mouse emulation motion processor
 */
public class AccessibilityServiceModeEngineImpl extends CoreEngine
        implements AccessibilityServiceModeEngine {

    // object which decides what to do with a new click
    private ClickDispatcher mClickDispatcher;

    // mouse emulation subsystem
    private MouseEmulation mMouseEmulation;

    // reference to the notification management stuff
    private ServiceNotification mServiceNotification;

    @Override
    protected void onInit(@NonNull Service service) {
        mClickDispatcher= new ClickDispatcher((AccessibilityService) service, getOverlayView());

        /* mouse emulation subsystem, should be the last one to add visible layers
           so that the pointer is drawn on top of everything else */
        mMouseEmulation = new MouseEmulation(service, getOverlayView(),
                getOrientationManager(), mClickDispatcher);

        // Service notification
        mServiceNotification= new ServiceNotification(service, mServiceNotificationReceiver);
        mServiceNotification.init();
    }

    @Override
    protected void onCleanup() {
        if (mServiceNotification!= null) {
            mServiceNotification.cleanup();
            mServiceNotification= null;
        }

        if (mMouseEmulation != null) {
            mMouseEmulation.cleanup();
            mMouseEmulation = null;
        }

        if (mClickDispatcher!= null) {
            mClickDispatcher.cleanup();
            mClickDispatcher= null;
        }
    }

    // Receiver listener for the service notification
    private final BroadcastReceiver mServiceNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra(ServiceNotification.NOTIFICATION_ACTION_NAME, -1);

            if (action == ServiceNotification.NOTIFICATION_ACTION_PAUSE) {
                pause();
            } else if (action == ServiceNotification.NOTIFICATION_ACTION_RESUME) {
                resume();
            } else {
                // ignore intent
                Log.i(EVIACAM.TAG, "mServiceNotificationReceiver: Got unknown intent");
            }
        }
    };

    @Override
    protected final boolean onStart() {
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_PAUSE);
        mMouseEmulation.start();
        mClickDispatcher.start();

        return true;
    }

    @Override
    protected void onStop() {
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_NONE);
        mMouseEmulation.stop();
        mClickDispatcher.stop();
    }

    @Override
    protected void onPause() {
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_RESUME);
        mMouseEmulation.stop();
        mClickDispatcher.stop();
    }

    @Override
    protected void onStandby() {
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_RESUME);
        mMouseEmulation.stop();
        mClickDispatcher.stop();
    }

    @Override
    protected void onResume() {
        onStart();
    }

    /**
     * Called for each processed camera frame
     *
     * @param motion motion vector, could be (0, 0) if motion not detected or the engine is
     *               paused or in standby mode
     * @param faceDetected whether or not a face was detected for the last frame, note
     *                     not all frames are checked for the face detection algorithm
     * @param state current state of the engine
     *
     * NOTE: called from a secondary thread
     */
    @Override
    protected void onFrame(@NonNull PointF motion, boolean faceDetected, int state) {
        if (getState() == STATE_RUNNING) {
            mMouseEmulation.processMotion(motion);
            mClickDispatcher.refresh();
            mMouseEmulation.setRestMode(mClickDispatcher.getRestModeEnabled());
        }
    }

    @Override
    public void enablePointer() {
        if (mMouseEmulation != null) mMouseEmulation.enablePointer();
    }

    @Override
    public void disablePointer() {
        if (mMouseEmulation != null) mMouseEmulation.disablePointer();
        if (mClickDispatcher!= null) mClickDispatcher.reset();
    }

    @Override
    public void enableClick() {
        if (mMouseEmulation != null) mMouseEmulation.enableClick();
    }

    @Override
    public void disableClick() {
        if (mMouseEmulation != null) mMouseEmulation.disableClick();
        if (mClickDispatcher!= null) mClickDispatcher.reset();
    }

    @Override
    public void enableDockPanel() {
        if (mClickDispatcher!= null) mClickDispatcher.enableDockPanel();
    }

    @Override
    public void disableDockPanel() {
        if (mClickDispatcher!= null) mClickDispatcher.disableDockPanel();
    }

    @Override
    public void enableScrollButtons() {
        if (mClickDispatcher!= null) mClickDispatcher.enableScrollButtons();
    }

    @Override
    public void disableScrollButtons() {
        if (mClickDispatcher!= null) mClickDispatcher.disableScrollButtons();
    }

    @Override
    public void enableAll() {
        enablePointer();
        enableClick();
        enableDockPanel();
        enableScrollButtons();
    }

    @Override
    public void onAccessibilityEvent(@NonNull AccessibilityEvent event) {
        if (mClickDispatcher != null) {
            mClickDispatcher.onAccessibilityEvent(event);
        }
    }
}
