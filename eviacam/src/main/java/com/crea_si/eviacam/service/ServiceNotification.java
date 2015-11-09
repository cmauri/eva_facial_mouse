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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.crea_si.eviacam.EVIACAM;
import com.crea_si.eviacam.R;

/**
 * Manage pause/resume notifications
 */
public class ServiceNotification {
    /**
     * Type of notification to display
     */
    public static final int NOTIFICATION_ACTION_PAUSE = 0;
    public static final int NOTIFICATION_ACTION_RESUME = 1;

    /*
     * constants for notifications
     */
    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_FILTER_ACTION = "ENABLE_DISABLE_EVIACAM";
    private static final String NOTIFICATION_ACTION_NAME = "action";

    private final Context mContext;

    // Engine which will be paused/resumed
    private final MainEngine mEngine;

    // Constructor
    public ServiceNotification(Context c, MainEngine e) {
        mContext = c;
        mEngine = e;
        
        /*
         * register notification receiver
         */
        IntentFilter iFilter = new IntentFilter(NOTIFICATION_FILTER_ACTION);
        c.registerReceiver(mMessageReceiver, iFilter);
    }

    public void cleanup() {
        mContext.unregisterReceiver(mMessageReceiver);
    }

    /**
     * Get the ID of the notification
     *
     * @return ID of the notification
     */
    public int getNotificationId() {
        return NOTIFICATION_ID;
    }

    /**
     * Set the notification to display
     *
     * @param type either NOTIFICATION_ACTION_PAUSE or NOTIFICATION_ACTION_RESUME
     * @return the notification
     */
    public Notification setNotification(int type) {
        Notification noti = createNotification(mContext, type);
        NotificationManager notificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, noti);
        return noti;
    }

    private static Notification createNotification(Context c, int action) {
        // notification initialization
        Intent intent = new Intent(NOTIFICATION_FILTER_ACTION);
        intent.putExtra(NOTIFICATION_ACTION_NAME, action);

        PendingIntent pIntent = PendingIntent.getBroadcast
                (c, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        CharSequence text;
        int iconId;
        if (action == NOTIFICATION_ACTION_PAUSE) {
            text = c.getText(R.string.running_click_to_pause);
            iconId = R.drawable.ic_notification_enabled;
        } else {
            text = c.getText(R.string.stopped_click_to_resume);
            iconId = R.drawable.ic_notification_disabled;
        }

        return new Notification.Builder(c)
                .setContentTitle(c.getText(R.string.app_name))
                .setContentText(text)
                .setSmallIcon(iconId)
                .setContentIntent(pIntent)
                .build();
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int action = intent.getIntExtra(NOTIFICATION_ACTION_NAME, -1);

            if (action == NOTIFICATION_ACTION_PAUSE) {
                mEngine.pause();
                //setNotification(NOTIFICATION_ACTION_RESUME);
                EVIACAM.debug("Got intent: PAUSE");
            } else if (action == NOTIFICATION_ACTION_RESUME) {
                mEngine.resume();
                //setNotification(NOTIFICATION_ACTION_PAUSE);
                EVIACAM.debug("Got intent: RESUME");
            } else {
                // ignore intent
                EVIACAM.debug("Got unknown intent");
            }
        }
    };
}
