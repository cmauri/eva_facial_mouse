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
package com.crea_si.eviacam.service;

import com.crea_si.eviacam.EVIACAM;
import com.crea_si.eviacam.Preferences;
import com.crea_si.eviacam.R;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PointF;
import android.os.Handler;
import android.view.accessibility.AccessibilityEvent;

/**
 * Engine implementation for the accessibility service which provides
 * a mouse emulation motion processor
 */
class AccessibilityServiceModeEngineImpl extends CoreEngine
        implements PowerManagement.OnScreenStateChangeListener,
        AccessibilityServiceModeEngine {

    // handler to run things on the main thread
    private final Handler mHandler= new Handler();

    // stores when the last detection of a face occurred
    // TODO: move to the base class???
    private final FaceDetectionCountdown mFaceDetectionCountdown = new FaceDetectionCountdown();

    // power management stuff
    private PowerManagement mPowerManagement;

    // reference to the notification management stuff
    private ServiceNotification mServiceNotification;

    // state before switching screen off
    private int mSaveState= -1;

    // reference to the engine when running as mouse emulation
    private MouseEmulation mMouseEmulation;

    @Override
    protected void onInit(Service service) {
        mMouseEmulation = new MouseEmulation(service, getOverlayView(), getOrientationManager());

        mPowerManagement = new PowerManagement(service, this);

        // Service notification
        mServiceNotification= new ServiceNotification(service, mServiceNotificationReceiver);
        mServiceNotification.init();
    }

    @Override
    protected void onCleanup() {
        mFaceDetectionCountdown.cleanup();

        if (mServiceNotification!= null) {
            mServiceNotification.cleanup();
            mServiceNotification= null;
        }

        if (mPowerManagement!= null) {
            mPowerManagement.cleanup();
            mPowerManagement = null;
        }

        if (mMouseEmulation != null) {
            mMouseEmulation.cleanup();
            mMouseEmulation = null;
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
                EVIACAM.debug("Got unknown intent");
            }
        }
    };

    @Override
    protected boolean onStart() {
        mPowerManagement.lockFullPower();         // Screen always on
        mPowerManagement.setSleepEnabled(true);   // Enable sleep call

        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_PAUSE);

        mFaceDetectionCountdown.start();

        mMouseEmulation.start();

        return true;
    }

    @Override
    protected void onStop() {
        mPowerManagement.unlockFullPower();
        mPowerManagement.setSleepEnabled(false);
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_NONE);
        mMouseEmulation.stop();
    }

    @Override
    protected void onPause() {
        mPowerManagement.unlockFullPower();
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_RESUME);
        mMouseEmulation.stop();
    }

    @Override
    protected void onStandby() {
        mPowerManagement.unlockFullPower();
        mPowerManagement.setSleepEnabled(true);   // Enable sleep call
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_RESUME);
        mMouseEmulation.stop();

        Service s= getService();
        if (s!= null) {
            Resources res = s.getResources();
            String t = String.format(
                    res.getString(R.string.pointer_stopped_toast),
                    Preferences.get().getTimeWithoutDetectionEntryValue());
            EVIACAM.LongToast(s, t);
        }
    }

    @Override
    protected void onResume() {
        mPowerManagement.lockFullPower();

        mFaceDetectionCountdown.start();

        mPowerManagement.setSleepEnabled(true);   // Enable sleep call

        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_PAUSE);

        mMouseEmulation.start();
    }

    /**
     * Called when screen goes on or off
     */
    @Override
    public void onOnScreenStateChange() {
        if (mPowerManagement.getScreenOn()) {
            // Screen switched on
            if (mSaveState == Engine.STATE_RUNNING ||
                mSaveState == Engine.STATE_STANDBY) start();
            else if (mSaveState == Engine.STATE_PAUSED) {
                start();
                pause();
            }
        }
        else {
            // Screen switched off
            mSaveState= getState();
            if (mSaveState!= Engine.STATE_STANDBY) stop();
        }
    }

    @Override
    protected void onFrame(PointF motion, boolean faceDetected, int state) {
        if (getState() == STATE_RUNNING) {
            mMouseEmulation.processMotion(motion);
        }

        // States to be managed: RUNNING, PAUSED, STANDBY

        if (faceDetected) mFaceDetectionCountdown.start();

        if (state == Engine.STATE_STANDBY) {
            if (faceDetected) {
                // "Awake" from standby state
                mHandler.post(new Runnable() {
                    @Override
                    public void run() { resume(); } }
                );
                /* Yield CPU to the main thread so that it has the opportunity
                 * to run and change the engine state before this thread continue
                 * running.
                 * Remarks: tried Thread.yield() without success
                 */
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) { /* do nothing */ }
            }
            else {
                // In standby reduce CPU cycles by sleeping but only if screen went off
                if (!mPowerManagement.getScreenOn()) mPowerManagement.sleep();
            }
        }
        else if (state == Engine.STATE_RUNNING) {
            if (mFaceDetectionCountdown.hasFinished() && !mFaceDetectionCountdown.isDisabled()) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        standby();
                    }
                });
            }
        }

        // Nothing more to do (state == Engine.STATE_PAUSED)
        updateFaceDetectorStatus(mFaceDetectionCountdown);
    }

    @Override
    public void enablePointer() {
        if (mMouseEmulation != null) mMouseEmulation.enablePointer();
    }

    @Override
    public void disablePointer() {
        if (mMouseEmulation != null) mMouseEmulation.disablePointer();
    }

    @Override
    public void enableClick() {
        if (mMouseEmulation != null) mMouseEmulation.enableClick();
    }

    @Override
    public void disableClick() {
        if (mMouseEmulation != null) mMouseEmulation.disableClick();
    }

    @Override
    public void enableDockPanel() {
        if (mMouseEmulation != null) mMouseEmulation.enableDockPanel();
    }

    @Override
    public void disableDockPanel() {
        if (mMouseEmulation != null) mMouseEmulation.disableDockPanel();
    }

    @Override
    public void enableScrollButtons() {
        if (mMouseEmulation != null) mMouseEmulation.enableScrollButtons();
    }

    @Override
    public void disableScrollButtons() {
        if (mMouseEmulation != null) mMouseEmulation.disableScrollButtons();
    }

    @Override
    public void enableAll() {
        enablePointer();
        enableClick();
        enableDockPanel();
        enableScrollButtons();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (mMouseEmulation != null) {
            mMouseEmulation.onAccessibilityEvent(event);
        }
    }
}
