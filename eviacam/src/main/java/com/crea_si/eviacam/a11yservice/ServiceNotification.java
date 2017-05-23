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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.common.MousePreferencesActivity;

/**
 * Manage pause/resume notifications
 */
class ServiceNotification {
    /**
     * Type of notification to display
     */
    static final int NOTIFICATION_ACTION_NONE = 0;
    static final int NOTIFICATION_ACTION_STOP = 1;
    static final int NOTIFICATION_ACTION_START = 2;

    /*
     * constants for notifications
     */
    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_FILTER_ACTION = "ENABLE_DISABLE_EVIACAM";
    static final String NOTIFICATION_ACTION_NAME = "action";

    private final Service mService;

    private final BroadcastReceiver mBroadcastReceiver;

    private int mAction= NOTIFICATION_ACTION_NONE;

    private boolean mInitDone = false;

    /**
     * Constructor
     *
     * @param s  service
     * @param bc broadcast receiver which will be called when the notification is tapped
     */
    ServiceNotification(@NonNull Service s, @NonNull BroadcastReceiver bc) {
        mService = s;
        mBroadcastReceiver = bc;
    }

    public void init() {
        if (mInitDone) return;

        /* register notification receiver */
        IntentFilter iFilter = new IntentFilter(NOTIFICATION_FILTER_ACTION);
        mService.registerReceiver(mBroadcastReceiver, iFilter);

        updateNotification();

        mInitDone = true;
    }

    public void cleanup() {
        if (!mInitDone) return;

        // Remove as foreground service
        mService.stopForeground(true);

        // Remove notification
        NotificationManager notificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        mService.unregisterReceiver(mBroadcastReceiver);

        mInitDone = false;
    }

    /**
     * Create and register the notification as foreground service
     */
    private void updateNotification () {
        Notification noti = createNotification(mService, mAction);
        NotificationManager notificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, noti);

        // Register as foreground service
        mService.startForeground(ServiceNotification.NOTIFICATION_ID, noti);
    }

    /**
     * Set the action of the notification and update accordingly
     * @param action of the notification
     *             NOTIFICATION_ACTION_NONE
     *             NOTIFICATION_ACTION_STOP
     *             NOTIFICATION_ACTION_START
     */
    void update (int action) {
        if (mAction == action) return;
        mAction= action;

        updateNotification ();
    }

    /**
     * Create the notification
     * @param c context
     * @param action code of the action
     * @return return notification object
     */
    private static Notification createNotification(@NonNull Context c, int action) {
        Intent intent;

        /* Pending intent to notify the a11y service */
        intent = new Intent(NOTIFICATION_FILTER_ACTION);
        intent.putExtra(NOTIFICATION_ACTION_NAME, action);
        PendingIntent pIntentAction = PendingIntent.getBroadcast
                (c, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* Pending intent to open settings activity */
        intent = new Intent(c, MousePreferencesActivity.class);
        PendingIntent pOpenSettings= PendingIntent.getActivity(c, 0, intent, 0);

        /* Choose right text message */
        CharSequence text;
        int iconId= R.drawable.ic_notification_enabled;
        if (action == NOTIFICATION_ACTION_NONE) {
            text = c.getText(R.string.app_name);
        } else if (action == NOTIFICATION_ACTION_STOP) {
            text = c.getText(R.string.notification_running_click_to_stop);
        } else if (action == NOTIFICATION_ACTION_START) {
            text = c.getText(R.string.notification_stopped_click_to_start);
        }
        else throw new IllegalStateException();

        NotificationCompat.Builder builder= new NotificationCompat.Builder(c)
            .setSmallIcon(iconId)
            .setLargeIcon(BitmapFactory.decodeResource(c.getResources(), R.drawable.ic_launcher))
            .setContentTitle(c.getText(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pIntentAction)
            .setPriority(Notification.PRIORITY_MAX)
            .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(text))
            .addAction(android.R.drawable.ic_menu_preferences,
                    c.getResources().getString(R.string.notification_settings_label), pOpenSettings);
        return builder.build();
    }
}
