package com.crea_si.eviacam.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.inputmethod.InputMethodManager;

import com.crea_si.input_method_aidl.IClickableIME;

/**
 * Handles the communication with the IME
 */

class InputMethodAction implements ServiceConnection {
    
    private static final String REMOTE_ACTION= "com.crea_si.eviacam_keyboard.RemoteBinderService";
    
    // period (in milliseconds) to try to rebing again to the IME
    private static final int BIND_RETRY_PERIOD = 2000;
    
    private final Context mContext;
    
    private final InputMethodManager mInputMethodManager;
    
    // binder (proxy) with the remote input method service
    private IClickableIME mRemoteService;
    
    // time stamp of the last time the thread ran
    private long mLastBindAttempTimeStamp= 0;
    
    public InputMethodAction(Context c) {
        mContext= c;
        
        mInputMethodManager= (InputMethodManager) 
                c.getSystemService (Context.INPUT_METHOD_SERVICE);
        
        // attempt to bind with IME
        keepBindAlive();
    }
    
    public void cleanup() {
        if (mRemoteService == null) return;
        
        mContext.unbindService(this);
        mRemoteService= null;
    }
    
    /**
     * Bind to the remote IME when needed
     * 
     * @return true if the bind is alive, false otherwise
     * 
     * TODO: support multiple compatible IMEs
     * TODO: provide feedback to the user 
     */
    private void keepBindAlive() {
        if (mRemoteService != null) return;
        
        /**
         * no bind available, try to establish it if enough 
         * time passed since the last attempt
         */
        long tstamp= System.currentTimeMillis();
        
        if (tstamp - mLastBindAttempTimeStamp < BIND_RETRY_PERIOD) {
            return;
        }

        mLastBindAttempTimeStamp= tstamp;
        
        EVIACAM.debug("Attemp to bind to remote IME");
        if (!mContext.bindService(
                new Intent(REMOTE_ACTION), this, Context.BIND_AUTO_CREATE)) {
            EVIACAM.debug("Cannot bind remote IME");
        }
    }
    
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.
        EVIACAM.debug("remoteIME:onServiceConnected: " + className.toString());
        mRemoteService = IClickableIME.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        EVIACAM.debug("remoteIME:onServiceDisconnected");
        mContext.unbindService(this);
        mRemoteService = null;
        keepBindAlive();
    }
    
    public boolean click(int x, int y) {
        if (mRemoteService == null) {
            EVIACAM.debug("InputMethodAction: click: no remote service available");
            return false;
        }
        //if (!mInputMethodManager.isActive()) return false;
        
        try {
            return mRemoteService.click(x, y);
        } catch (RemoteException e) {
            return false;
        }
    }
    
    public void openIME() {
        if (mRemoteService == null) {
            EVIACAM.debug("InputMethodAction: openIME: no remote service available");
            keepBindAlive();
            return;
        }
        if (mInputMethodManager.isActive()) return; // already open
        try {
            mRemoteService.openIME();
        } catch (RemoteException e) {
            // Nothing to be done
            EVIACAM.debug("InputMethodAction: exception while trying to open IME");
        }
    }
}
