package com.crea_si.eviacam.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class SlaveMode implements ServiceConnection {
    /*
     * In slave mode there several possibilities when started
     */
    public static final int MOUSE= 0;
    public static final int ABSOLUTE_PAD= 1;
    public static final int RELATIVE_PAD= 2;

    private static final String TAG= "eviacam_api";
    
    private static final String REMOTE_PACKAGE= "com.crea_si.eviacam.service";
    private static final String REMOTE_ACTION= REMOTE_PACKAGE + ".SlaveModeService";
    
    private final Context mContext;
    private final SlaveModeConnection mSlaveModeConnection;
    
    // binder (proxy) with the remote input method service
    private ISlaveMode mSlaveMode;

    public static void initConnection(Context c, SlaveModeConnection callback) {
        Log.d(TAG, "Attemp to bind to EViacam API");
        Intent intent= new Intent(REMOTE_ACTION);
        intent.setPackage(REMOTE_PACKAGE);
        try {
            if (!c.bindService(intent, new SlaveMode(c, callback), Context.BIND_AUTO_CREATE)) {
                Log.d(TAG, "Cannot bind remote API");
            }
        }
        catch(SecurityException e) {
            Log.d(TAG, "Cannot bind remote API. Security exception.");
        }
    }

    private SlaveMode (Context c, SlaveModeConnection callback) {
        mContext= c;
        mSlaveModeConnection= callback;
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.
        Log.d(TAG, "EViacam API:onServiceConnected: " + name.toString());
        mSlaveMode = ISlaveMode.Stub.asInterface(service);
        mSlaveModeConnection.onConnected(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        Log.d(TAG, "EViacam API:onServiceDisconnected");
        mContext.unbindService(this);
        mSlaveModeConnection.onDisconnected();
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
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Stop eviacam
     */
    public void stop() {
        try {
            mSlaveMode.stop();
        } catch (RemoteException e) {
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
    void setOperationMode(int mode) {
        try {
            mSlaveMode.setOperationMode(mode);
        } catch (RemoteException e) {
        }
    }
    
    public boolean registerListener(IPadEventListener listener) {
        if (mSlaveMode== null) return false;
        try {
            return mSlaveMode.registerListener(new IPadEventListenerWrapper(listener));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void unregisterListener() {
        if (mSlaveMode== null) return;
        try {
            mSlaveMode.unregisterListener();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    
    public void disconnect() {
        mContext.unbindService(this);
        mSlaveMode = null;
    }

    private class IPadEventListenerWrapper extends IPadEventListener.Stub {
        private final IPadEventListener mListener;
        public IPadEventListenerWrapper(IPadEventListener l) {
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
}
