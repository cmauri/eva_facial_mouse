package com.crea_si.eviacam_keyboard;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ServiceNotification {

    /*
     * constants for notifications
     */
    private static final int NOTIFICATION_ID= 1;
    private static final String NOTIFICATION_FILTER_ACTION= "ENABLE_DISABLE_EVIACAM";
    private static final int NOTIFICATION_ACTION_PAUSE= 0;
    private static final int NOTIFICATION_ACTION_RESUME= 1;
    private static final String NOTIFICATION_ACTION_NAME= "action";
    

    
    public ServiceNotification (Context c) {

        
        /*
         * register notification receiver
         */
        IntentFilter iFilter= new IntentFilter(NOTIFICATION_FILTER_ACTION);
        c.registerReceiver(mMessageReceiver, iFilter);
    }
    
    // receiver for notifications
    private BroadcastReceiver mMessageReceiver= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // update notification
            int action= intent.getIntExtra(NOTIFICATION_ACTION_NAME, -1);
            //Notification noti;
            
            if (action == NOTIFICATION_ACTION_PAUSE) {
                //noti= getNotification(context, NOTIFICATION_ACTION_RESUME);
                EVIACAMIME.debug("IME: Got intent: PAUSE");
            }
            else if (action == NOTIFICATION_ACTION_RESUME) {
                //noti= getNotification(context, NOTIFICATION_ACTION_PAUSE);
                EVIACAMIME.debug("IME: Got intent: RESUME");
            }
            else {
                // ignore intent
                EVIACAMIME.debug("IME: Got unknown intent");
                return;
            }
            /*        
            NotificationManager notificationManager = 
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, noti);
            */
        }
    };

    private Notification getNotification(Context c, int action) {
        // notification initialization 
        Intent intent = new Intent(NOTIFICATION_FILTER_ACTION);
        intent.putExtra(NOTIFICATION_ACTION_NAME, action);
        
        PendingIntent pIntent= PendingIntent.getBroadcast
                (c, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        CharSequence text;
        if (action == NOTIFICATION_ACTION_PAUSE) {
            text= "IME PAUSE";
        }
        else {
            text= "IMA RESUME";
        }

        Notification noti= new Notification.Builder(c)
            .setContentTitle(c.getText(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pIntent)
            .build();
        
        return noti;
    }
    
    public Notification getNotification(Context c) {
        return getNotification(c, NOTIFICATION_ACTION_PAUSE);
    }
    
    public int getNotificationId() {
        return NOTIFICATION_ID;
    }
}
