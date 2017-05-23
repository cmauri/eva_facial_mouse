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

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.EngineSelector;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.common.Analytics;
import com.crea_si.eviacam.common.CrashRegister;
import com.crea_si.eviacam.common.Engine;
import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.wizard.WizardUtils;

import org.acra.ACRA;

/**
 * The Enable Viacam accessibility service
 */
public class TheAccessibilityService extends AccessibilityService
        implements ComponentCallbacks, Engine.OnInitListener {

    private static final String TAG = "TheAccessibilityService";

    private static TheAccessibilityService sTheAccessibilityService;

    // reference to the engine
    private AccessibilityServiceModeEngine mEngine;

    // stores whether the accessibility service was previously started (see comments on init())
    private boolean mServiceStarted = false;

    // reference to the notification management stuff
    private ServiceNotification mServiceNotification;

    // Receiver listener for the service notification
    private final BroadcastReceiver mServiceNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            int action = intent.getIntExtra(ServiceNotification.NOTIFICATION_ACTION_NAME, -1);

            if (action == ServiceNotification.NOTIFICATION_ACTION_STOP) {
                /* Ask for confirmation before stopping */
                AlertDialog ad = new AlertDialog.Builder(c)
                    .setMessage(c.getResources().getString(R.string.notification_stop_confirmation))
                    .setPositiveButton(c.getResources().getString(
                            R.string.notification_stop_confirmation_no), null)
                    .setNegativeButton(c.getResources().getString(
                            R.string.notification_stop_confirmation_yes),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    cleanupEngine();
                                    Preferences.get().setEngineWasRunning(false);
                                }
                            })
                   .create();
                //noinspection ConstantConditions
                ad.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                ad.show();
            } else if (action == ServiceNotification.NOTIFICATION_ACTION_START) {
                initEngine();
            } else {
                // ignore intent
                Log.i(TAG, "mServiceNotificationReceiver: Got unknown intent");
            }
        }
    };

    /**
     * Start the initialization sequence of the accessibility service.
     */
    private void init() {
        /*
         * Check if service has been already started.
         * Under certain circumstances onUnbind is not called (e.g. running
         * on an emulator happens quite often) and the service continues running
         * although it shows it is disabled. This does not solve the issue but at
         * least the service does not crash
         *
         * http://stackoverflow.com/questions/28752238/accessibilityservice-onunbind-not-always-
         * called-when-running-on-emulator
         */
        if (mServiceStarted) {
            Log.w(TAG, "Accessibility service already running! Stop here.");
            ACRA.getErrorReporter().handleException(new IllegalStateException(
                    "Accessibility service already running! Stop here."), true);
            return;
        }

        mServiceStarted= true;
        sTheAccessibilityService= this;

         /* When preferences are not properly initialized (i.e. is in slave mode)
           the call will return null. As is not possible to stop the accessibility
           service just take into account an avoid further actions. */
        if (Preferences.initForA11yService(this) == null) return;

        // Service notification
        mServiceNotification= new ServiceNotification(this, mServiceNotificationReceiver);
        mServiceNotification.init();

        /*
         * If crashed recently, abort initialization to avoid several crash messages in a row.
         * The user will need to enable it again through the notification icon.
         */
        if (CrashRegister.crashedRecently(this)) {
            Log.w(TAG, "Recent crash detected. Aborting initialization.");
            CrashRegister.clearCrash(this);
            mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_START);
            return;
        }

        /*
         * If the device has been rebooted and the engine was stopped before
         * such a reboot, do not start.
         */
        if (!Preferences.get().getEngineWasRunning()) {
            mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_START);
            return;
        }

        initEngine();
    }

    /**
     * Cleanup accessibility service before exiting completely
     */
    private void cleanup() {
        sTheAccessibilityService= null;

        cleanupEngine();

        if (Preferences.get() != null) {
            Preferences.get().cleanup();
        }

        if (mServiceNotification!= null) {
            mServiceNotification.cleanup();
            mServiceNotification= null;
        }

        mServiceStarted = false;
    }

    /**
     * Start engine initialization sequence. When finished, onInit is called
     */
    private void initEngine() {
        if (null != mEngine) {
            Log.i(TAG, "Engine already initialized. Ignoring.");
            return;
        }

        // During initialization cannot send new commands
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_NONE);

        /* Init the main engine */
        mEngine = EngineSelector.initAccessibilityServiceModeEngine();
        if (mEngine == null) {
            Log.e(TAG, "Cannot initialize CoreEngine in A11Y mode");
        } else {
            mEngine.init(this, this);
        }
    }

    /**
     * Callback for engine initialization completion
     * @param status 0 if initialization completed successfully
     */
    @Override
    public void onInit(int status) {
        if (status == Engine.OnInitListener.INIT_SUCCESS) {
            initEnginePhase2();
        }
        else {
            // Initialization failed
            Log.e(TAG, "Cannot initialize CoreEngine in A11Y mode");
            cleanupEngine();
            mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_START);
        }
    }

    /**
     * Completes the initialization of the engine
     */
    private void initEnginePhase2() {
        Analytics.get().trackStartService();

        Preferences.get().setEngineWasRunning(true);

        /* Start wizard or the full engine? */
        if (Preferences.get().getRunTutorial()) {
            // register notification receiver
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    this.mFinishWizardReceiver,
                    new IntentFilter(WizardUtils.WIZARD_CLOSE_EVENT_NAME));

            Intent dialogIntent = new Intent(this, com.crea_si.eviacam.wizard.WizardActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(dialogIntent);
        }
        else {
            mEngine.start();
        }
        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_STOP);
    }

    /**
     * Receiver listener for the event triggered when the wizard is finished
     */
    private final BroadcastReceiver mFinishWizardReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mEngine!= null) {
                mEngine.start();
                mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_STOP);
            }
            LocalBroadcastManager.getInstance(TheAccessibilityService.this).
                    unregisterReceiver(mFinishWizardReceiver);
        }
    };

    /**
     * Stop the engine and free resources
     */
    private void cleanupEngine() {
        if (null == mEngine) return;

        Analytics.get().trackStopService();

        if (mEngine!= null) {
            mEngine.cleanup();
            mEngine= null;
        }

        EngineSelector.releaseAccessibilityServiceModeEngine();

        mServiceNotification.update(ServiceNotification.NOTIFICATION_ACTION_START);
    }

    /**
     * Get the current instance of the accessibility service
     *
     * @return reference to the accessibility service or null
     */
    public static @Nullable TheAccessibilityService get() {
        return sTheAccessibilityService;
    }

    public void openNotifications() {
        performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
    }

    /**
     * Called when the accessibility service is started
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
    }

    /**
     * Called every time the service is switched ON
     */
    @Override
    public void onServiceConnected() {
        Log.i(TAG, "onServiceConnected");
        init();
    }

    /**
     * Called when service is switched off
     */
    @Override
    public boolean onUnbind(Intent intent) {
        /* TODO: it seems that, at this point, views are already destroyed
         * which might be related with the spurious crashes when switching
         * off the accessibility service. Tested on Nexus 7 Android 5.1.1
         */
        if (BuildConfig.DEBUG) Log.d(TAG, "onUnbind");
        cleanup();
        return false;
    }

    /**
     * Called when service is switched off after onUnbind
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) Log.d(TAG, "onDestroy");
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
        if (BuildConfig.DEBUG) Log.d(TAG, "onInterrupt");
    }
}
