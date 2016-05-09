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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.crea_si.eviacam.R;

/**
 * Manage pause/resume notifications
 */
public class ServiceNotification {
    /**
     * Type of notification to display
     */
    public static final int NOTIFICATION_ACTION_NONE = 0;
    public static final int NOTIFICATION_ACTION_PAUSE = 1;
    public static final int NOTIFICATION_ACTION_RESUME = 2;

    /*
     * constants for notifications
     */
    public static final int NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_FILTER_ACTION = "ENABLE_DISABLE_EVIACAM";
    public static final String NOTIFICATION_ACTION_NAME = "action";

    private final Service mService;

    private final BroadcastReceiver mBroadcastReceiver;

    private int mAction= NOTIFICATION_ACTION_NONE;

    private boolean mEnabled= false;

    /**
     * Constructor
     *
     * @param s  service
     * @param bc broadcast receiver which will be called when the notification is tapped
     */
    public ServiceNotification(Service s, BroadcastReceiver bc) {
        mService = s;
        mBroadcastReceiver = bc;
        
        /*
         * register notification receiver
         */
        IntentFilter iFilter = new IntentFilter(NOTIFICATION_FILTER_ACTION);
        s.registerReceiver(bc, iFilter);
    }

    public void cleanup() {
        disable();
        mService.unregisterReceiver(mBroadcastReceiver);
    }

    public void enable() {
        if (mEnabled) return;

        updateNotification();

        mEnabled= true;
    }

    public void disable() {
        if (!mEnabled) return;

        // Remove as foreground service
        mService.stopForeground(true);

        // Remove notification
        NotificationManager notificationManager =
                (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        mEnabled= false;
    }

    private void updateNotification () {
        // Create and register the notification
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
     *             NOTIFICATION_ACTION_PAUSE
     *             NOTIFICATION_ACTION_RESUME
     */
    void update (int action) {
        if (mAction == action) return;
        mAction= action;

        updateNotification ();
    }

    private static Notification createNotification(Context c, int action) {
        // notification initialization
        Intent intent = new Intent(NOTIFICATION_FILTER_ACTION);
        intent.putExtra(NOTIFICATION_ACTION_NAME, action);

        PendingIntent pIntent = PendingIntent.getBroadcast
                (c, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        CharSequence text;
        int iconId;
        if (action == NOTIFICATION_ACTION_NONE) {
            text = c.getText(R.string.app_name);
            iconId = R.drawable.ic_notification_enabled;
        } else if (action == NOTIFICATION_ACTION_PAUSE) {
            text = c.getText(R.string.running_click_to_pause);
            iconId = R.drawable.ic_notification_enabled;
        } else if (action == NOTIFICATION_ACTION_RESUME) {
            text = c.getText(R.string.stopped_click_to_resume);
            iconId = R.drawable.ic_notification_disabled;
        }
        else throw new IllegalStateException();

        return new Notification.Builder(c)
                .setContentTitle(c.getText(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(iconId)
                .setContentIntent(pIntent)
                .build();
    }
}
