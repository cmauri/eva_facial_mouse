package com.crea_si.eviacam.service;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.util.Log;

public class EViacamService extends AccessibilityService {
	private static final String TAG = "EViacamService";
	
	@Override
	public void onCreate () {
		// Only called the first time the accessibility service is started 
		Log.i(TAG,"onCreate");
	}
	
	@Override
    public void onServiceConnected() {
		// Called every time the service is switched ON
		Log.i(TAG,"onServiceConnected");
		
		Toast toast = Toast.makeText(this.getApplicationContext(), "onServiceConnected", Toast.LENGTH_SHORT);
		toast.show();
    }
	
	@Override
	public boolean onUnbind (Intent intent) {
		// It seems that never gets called
		Log.i(TAG,"onUnbind");
		return false;
	}	
	
	@Override
	public void onDestroy() {
		// It seems that never gets called
		Log.i(TAG,"onDestroy");
		super.onDestroy();
	}

	
	/**
	 * (required) This method is called back by the system when it detects an AccessibilityEvent
	 * that matches the event filtering parameters specified by your accessibility service.
	 */
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		Log.i(TAG,"onAccessibilityEvent");
	}


	/**
	 * (required) This method is called when the system wants to interrupt the feedback your 
	 * service is providing, usually in response to a user action such as moving focus to a 
	 * different control. This method may be called many times over the lifecycle of your 
	 * service.
	 */
	@Override
	public void onInterrupt() {
		Log.i(TAG,"onInterrupt");
	}
	
	
	/*
	 * About other Service callbacks
	 * 
	 * NOT CALLED
	 * 	int onStartCommand(Intent intent, int flags, int startId)
	 *  boolean onUnbind(Intent intent)
	 * 
	 * NON OVERRIDABLE
	 * 	IBinder onBind(Intent intent)
	 */
}
