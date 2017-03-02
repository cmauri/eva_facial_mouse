/*
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crea_si.eviacam.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;

/**
 * eviacam slave mode
 * 
 * TODO: improve service security
 */
public class SlaveMode implements ServiceConnection, IReadyEventListener {
    /**
     * In slave mode there several possibilities when started
     */
    public static final int MOUSE= 0;
    public static final int GAMEPAD_ABSOLUTE= 1;
    public static final int GAMEPAD_RELATIVE= 2;

    private static final String TAG= "eviacam_api";

    // This is the Android package name for the eViacam app
    private static final String APP_PACKAGE_NAME = "com.crea_si.eviacam.service";

    // Action to start the slave mode service
    private static final String SLAVE_MODE_SERVICE_ACTION=
            "com.crea_si.eviacam.slavemode.SlaveModeService";

    // Class to open the general slave mode preferences activity
    private static final String PREFERENCES_ACTIVITY_CLS =
            "com.crea_si.eviacam.slavemode.SlaveModePreferencesActivity";

    // Class to open gamepad preferences activity
    private static final String GAMEPAD_PREFERENCE_ACTIVITY_CLS =
            "com.crea_si.eviacam.slavemode.GamepadPreferencesActivity";

    // Class to open mouse preferences activity
    private static final String MOUSE_PREFERENCE_ACTIVITY_CLS =
            "com.crea_si.eviacam.common.MousePreferencesActivity";
    
    private final Context mContext;
    private final SlaveModeStatusListener mSlaveModeStatusListener;
    
    // binder (proxy) with the remote input method service
    private ISlaveMode mSlaveMode;

    /**
     * Connect to the remote eviacam slave mode service
     * 
     * @param c context
     * @param callback which will receive the instance of a SlaveMode class
     */
    public static void connect(Context c, SlaveModeStatusListener callback) {
        Log.d(TAG, "Attempt to bind to EViacam API");

        if (callback== null) throw new NullPointerException();

        Intent intent= new Intent(SLAVE_MODE_SERVICE_ACTION);
        intent.setPackage(APP_PACKAGE_NAME);
        try {
            if (!c.bindService(intent, new SlaveMode(c, callback), Context.BIND_AUTO_CREATE)) {
                Log.d(TAG, "Cannot bind remote API");
            }
        }
        catch(SecurityException e) {
            Log.d(TAG, "Cannot bind remote API. Security exception.");
        }
    }
    
    /**
     * Disconnect from eviacam slave mode service
     */
    public void disconnect() {
        mContext.unbindService(this);
        mSlaveMode = null;
    }

    private SlaveMode (Context c, SlaveModeStatusListener callback) {
        mContext= c;
        mSlaveModeStatusListener = callback;
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.
        Log.d(TAG, "EViacam API:onServiceConnected: " + name.toString());
        mSlaveMode = ISlaveMode.Stub.asInterface(service);
        try {
            mSlaveMode.init(new IReadyEventListenerWrapper(this));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReadyEvent(boolean ready) throws RemoteException {
        if (!ready) {
            // Not ready, something went wrong, disconnect
            disconnect();
        }
        else if (mSlaveModeStatusListener!= null) {
            mSlaveModeStatusListener.onReady(this);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        Log.d(TAG, "EViacam API:onServiceDisconnected");
        mContext.unbindService(this);
        mSlaveModeStatusListener.onDisconnected();
        mSlaveMode = null;
    }
    
    /**
     * Starts eviacam in slave mode
     */
    public boolean start() {
        if (mSlaveMode== null) return false;
        try {
            return mSlaveMode.start();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.start: exception: " + e.getMessage()); 
        }
        return false;
    }
    
    /**
     * Stops eviacam slave mode
     */
    public void stop() {
        try {
            mSlaveMode.stop();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.stop: exception: " + e.getMessage());
        }
    }
    
    /**
     * Set operation mode
     * 
     * @param mode
     *  MOUSE
     *  ABSOLUTE_PAD
     *  RELATIVE_PAD
     */
    public void setOperationMode(int mode) {
        try {
            if (mode< MOUSE || mode> GAMEPAD_RELATIVE) return;
            mSlaveMode.setOperationMode(mode);
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.setOperationMode: exception: " + e.getMessage());
        }
    }
    
    /**
     * Register the listener for gamepad events
     * 
     * @param listener the listener
     * @return true if registration succeeded, false otherwise
     */
    public boolean registerGamepadListener(IGamepadEventListener listener) {
        if (mSlaveMode== null) return false;
        try {
            return mSlaveMode.registerGamepadListener(new IGamepadEventListenerWrapper(listener));
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.registerGamepadListener: exception: " + e.getMessage());
        }
        return false;
    }

    /**
     * Unregister the listener for gamepad events (if any)
     */
    public void unregisterGamepadListener() {
        if (mSlaveMode== null) return;
        try {
            mSlaveMode.unregisterGamepadListener();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.unregisterGamepadListener: exception: " + e.getMessage());
        }
    }
    
    /**
     * Register the listener for mouse events
     * 
     * @param listener the listener
     * @return true if registration succeeded, false otherwise
     */
    public boolean registerMouseListener(IMouseEventListener listener) {
        if (mSlaveMode== null) return false;
        try {
            return mSlaveMode.registerMouseListener(new IMouseEventListenerWrapper(listener));
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.registerMouseListener: exception: " + e.getMessage());
        }
        return false;
    }

    /**
     * Unregister the listener for mouse events (if any)
     */
    public void unregisterMouseListener() {
        if (mSlaveMode== null) return;
        try {
            mSlaveMode.unregisterMouseListener();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.unregisterMouseListener: exception: " + e.getMessage());
        }
    }

    /**
     * Register the listener for menu events
     *
     * @param listener the listener
     * @return true if registration succeeded, false otherwise
     */
    public boolean registerDockPanelListener(IDockPanelEventListener listener) {
        if (mSlaveMode== null) return false;
        try {
            return mSlaveMode.registerDockPanelListener(new IDockPanelListenerWrapper(listener));
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.registerDockPanelListener: exception: " + e.getMessage());
        }
        return false;
    }

    /**
     * Unregister the listener for mouse events (if any)
     */
    public void unregisterDockPanelListener() {
        if (mSlaveMode== null) return;
        try {
            mSlaveMode.unregisterDockPanelListener();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.nregisterDockPanelListener: exception: " + e.getMessage());
        }
    }
    
    /**
     * Open the root preferences activity for the slave mode
     */
    public static void openSettingsActivity(Context c) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(APP_PACKAGE_NAME, PREFERENCES_ACTIVITY_CLS));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(intent);
    }
    
    /**
     * Open gamepad preferences activity
     */
    public static void openGamepadSettingsActivity(Context c) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(APP_PACKAGE_NAME, GAMEPAD_PREFERENCE_ACTIVITY_CLS));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(intent);
    }

    /**
     * Open mouse preferences activity
     */
    public static void openMouseSettingsActivity(Context c) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(APP_PACKAGE_NAME, MOUSE_PREFERENCE_ACTIVITY_CLS));
        intent.putExtra("slave_mode", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        c.startActivity(intent);
    }

    @Override
    public IBinder asBinder() {
        return null;
    }

    /*
     * Stub implementation for ready event listener
     */
    private class IReadyEventListenerWrapper extends IReadyEventListener.Stub {
        private final IReadyEventListener mListener;
        IReadyEventListenerWrapper(IReadyEventListener l) {
            mListener= l;
        }

        @Override
        public void onReadyEvent(boolean ready) throws RemoteException {
            mListener.onReadyEvent(ready);
        }
    }

    /*
     * Stub implementation for gamepad event listener
     */
    private class IGamepadEventListenerWrapper extends IGamepadEventListener.Stub {
        private final IGamepadEventListener mListener;
        IGamepadEventListenerWrapper(IGamepadEventListener l) {
            mListener= l;
        }
        
        @Override
        public void buttonPressed(int button) throws RemoteException {
            mListener.buttonPressed(button);
        }

        @Override
        public void buttonReleased(int button) throws RemoteException {
            mListener.buttonReleased(button);    
        }        
    }

    /*
     * Stub implementation for mouse event listener
     */
    private class IMouseEventListenerWrapper extends IMouseEventListener.Stub {
        private final IMouseEventListener mListener;
        IMouseEventListenerWrapper(IMouseEventListener l) {
            mListener= l;
        }

        @Override
        public void onMouseEvent(MotionEvent e) throws RemoteException {
            mListener.onMouseEvent(e);
        }
    }

    /*
     * Stub implementation for mouse event listener
     */
    private class IDockPanelListenerWrapper extends IDockPanelEventListener.Stub {
        private final IDockPanelEventListener mListener;
        IDockPanelListenerWrapper(IDockPanelEventListener l) {
            mListener= l;
        }

        @Override
        public void onDockMenuOption (int option) throws RemoteException {
            mListener.onDockMenuOption(option);
        }
    }
}
