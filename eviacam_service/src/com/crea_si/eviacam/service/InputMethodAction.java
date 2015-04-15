package com.crea_si.eviacam.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.inputmethod.InputMethodManager;

import com.crea_si.input_method_aidl.IClickableIME;

class InputMethodAction implements ServiceConnection {
    
    private static final String REMOTE_ACTION= "com.crea_si.eviacam_keyboard.RemoteBinderService";
    
    private final Context mContext;
    
    // flag indicating whether we have called bind on the service
    private boolean mBound;
    
    InputMethodManager mInputMethodManager;
    
    // binder with the remote input method service
    private IClickableIME mRemoteService; 

    public InputMethodAction(Context c) {
        mContext= c;
        
        mInputMethodManager= (InputMethodManager) 
                c.getSystemService (Context.INPUT_METHOD_SERVICE);
        
        // TODO: attempt to bind when IME open
        if (!c.bindService(new Intent(REMOTE_ACTION), this, Context.BIND_AUTO_CREATE)) {
            EVIACAM.debug("Cannot bind remote IME");
        }
    }
    
    public void cleanup() {
        if (mRemoteService == null) return;
        
        mContext.unbindService(this);
        mRemoteService= null;
    }
    
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.
        EVIACAM.debug("remoteIME:onServiceConnected: " + className.toString());
        mRemoteService = IClickableIME.Stub.asInterface(service);
        mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        EVIACAM.debug("remoteIME:onServiceDisconnected");
        mRemoteService = null;
        mBound = false;
    }
    
    public boolean click(float x, float y) {
        if (!mBound) return false;
        if (mRemoteService == null) return false;
        if (!mInputMethodManager.isActive()) return false;
        
        try {
            return mRemoteService.click(x, y);
        } catch (RemoteException e) {
            return false;
        }
    }
    
 
}
