/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-16 Cesar Mauri Loba (CREA Software Systems)
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


import android.accessibilityservice.AccessibilityService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;

import android.os.Handler;

import android.support.v4.content.LocalBroadcastManager;


/**
 * Control the main engine when running in A11Y mode
 *
 * TODO: implement as derived class of CoreEngine (which can be renamed as
 * CoreEngine) and add a EngineFactory class to provide the right instance
 * being this one for the accessibility service
 */
public class EngineControl
        implements Engine.OnFinishProcessFrame, PowerManagement.OnScreenStateChangeListener {

    public static final String WIZARD_CLOSE_EVENT_NAME= "wizard-closed-event";

    // handler to run things on the main thread
    private final Handler mHandler= new Handler();

    private final AccessibilityService mAccessibilityService;

    private final Engine mEngine;

    // power management stuff
    private final PowerManagement mPowerManagement;

    // reference to the notification management stuff
    private final ServiceNotification mServiceNotification;

    // stores when the last detection of a face occurred
    private final FaceDetectionCountdown mFaceDetectionCountdown = new FaceDetectionCountdown();

    // state before switching screen off
    private int mSaveState= -1;


    public EngineControl(AccessibilityService as, Engine e) {
        mAccessibilityService= as;
        mEngine= e;

        mEngine.setOnFinishProcessFrame(this);

        mPowerManagement = new PowerManagement(as, this);

        // Service notification
        mServiceNotification= new ServiceNotification(as, mServiceNotificationReceiver);
        mServiceNotification.init();

        // Start wizard or the full engine
        if (Preferences.get().getRunTutorial()) {
            // register notification receiver
            LocalBroadcastManager.getInstance(as).registerReceiver(
                    this.mFinishWizardReceiver,
                    new IntentFilter(WIZARD_CLOSE_EVENT_NAME));

            Intent dialogIntent = new Intent(as, com.crea_si.eviacam.wizard.WizardActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            as.startActivity(dialogIntent);
        }
        else start();
    }

    // Receiver listener for the event triggered when the wizard is finished
    private final BroadcastReceiver mFinishWizardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            start();
        }
    };

    // Receiver listener for the service notification
    private final BroadcastReceiver mServiceNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra(ServiceNotification.NOTIFICATION_ACTION_NAME, -1);

            if (action == ServiceNotification.NOTIFICATION_ACTION_PAUSE) {
                pause();
                EVIACAM.debug("Got intent: PAUSE");
            } else if (action == ServiceNotification.NOTIFICATION_ACTION_RESUME) {
                resume();
                EVIACAM.debug("Got intent: RESUME");
            } else {
                // ignore intent
                EVIACAM.debug("Got unknown intent");
            }
        }
    };

    public void cleanup() {
        LocalBroadcastManager.getInstance(mAccessibilityService).unregisterReceiver(
                mFinishWizardReceiver);

        mEngine.cleanup();

        mFaceDetectionCountdown.cleanup();

        mServiceNotification.cleanup();

        mPowerManagement.cleanup();
    }

    private void start() {
        mPowerManagement.lockFullPower();         // Screen always on
        mPowerManagement.setSleepEnabled(true);   // Enable sleep call

        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_PAUSE);

        mFaceDetectionCountdown.start();

        mEngine.start();
    }

    private void stop() {
        mPowerManagement.unlockFullPower();
        mPowerManagement.setSleepEnabled(false);
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_NONE);

        mEngine.stop();
    }

    private void pause() {
        mPowerManagement.unlockFullPower();
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_RESUME);

        mEngine.pause();
    }

    private void standby() {
        mPowerManagement.unlockFullPower();
        mPowerManagement.setSleepEnabled(true);   // Enable sleep call
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_RESUME);

        Resources res = mAccessibilityService.getResources();
        String t = String.format(
                res.getString(R.string.pointer_stopped_toast),
                Preferences.get().getTimeWithoutDetectionEntryValue());
        EVIACAM.LongToast(mAccessibilityService, t);

        mEngine.standby();
    }

    private void resume() {
        mPowerManagement.lockFullPower();

        mFaceDetectionCountdown.start();

        mPowerManagement.setSleepEnabled(true);   // Enable sleep call

        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_PAUSE);

        mEngine.resume();
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
            mSaveState= mEngine.getState();
            if (mSaveState!= Engine.STATE_STANDBY) stop();
        }
    }

    @Override
    public void onOnFinishProcessFrame(boolean faceDetected) {

        // States to be managed: RUNNING, PAUSED, STANDBY

        if (faceDetected) mFaceDetectionCountdown.start();

        int state= mEngine.getState();

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

        mEngine.updateFaceDetectorStatus(mFaceDetectionCountdown);
    }
}
