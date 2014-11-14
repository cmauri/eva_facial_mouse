package com.crea_si.eviacam.service;

import java.util.Timer;
import java.util.TimerTask;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class EViacamService extends AccessibilityService {
    private static final String TAG = "EViacamService";
    private Timer mTimer;

    @Override
    public void onCreate() {
        // Called when the accessibility service is started
        super.onCreate();
        Log.i(TAG, "onCreate");

        mTimer = new Timer();
    }

    @Override
    public void onServiceConnected() {
        // Called every time the service is switched ON
        Log.i(TAG, "onServiceConnected");

        // Does not want any accessibility event. Cannot be removed directly from
        // @xml/accessibilityservice, otherwise onUnbind and onDestroy never get called
        setServiceInfo(new AccessibilityServiceInfo());

        Toast toast = Toast.makeText(this.getApplicationContext(),
                "onServiceConnected", Toast.LENGTH_SHORT);
        toast.show();

        mTimer.scheduleAtFixedRate(new mainTask(), 0, 5000);
    }

    private class mainTask extends TimerTask {
        public void run() {
            toastHandler.sendEmptyMessage(0);
        }
    }

    private final Handler toastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(), "test", Toast.LENGTH_SHORT)
                    .show();
        }
    };

    @Override
    public boolean onUnbind(Intent intent) {
        // Gets called when service is switched off
        mTimer.cancel();
        Log.i(TAG, "onUnbind");
        return false;
    }

    @Override
    public void onDestroy() {
        // Gets called when service is switched off after onUnbind
        super.onDestroy();
        mTimer.cancel();
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    /**
     * (required) This method is called back by the system when it detects an
     * AccessibilityEvent that matches the event filtering parameters specified
     * by your accessibility service.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        Log.i(TAG, "onAccessibilityEvent");
    }

    /**
     * (required) This method is called when the system wants to interrupt the
     * feedback your service is providing, usually in response to a user action
     * such as moving focus to a different control. This method may be called
     * many times over the lifecycle of your service.
     */
    @Override
    public void onInterrupt() {
        Log.i(TAG, "onInterrupt");
    }
}
