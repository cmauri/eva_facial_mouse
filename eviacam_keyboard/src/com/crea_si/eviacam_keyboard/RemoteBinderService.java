package com.crea_si.eviacam_keyboard;

import com.crea_si.input_method_aidl.IClickableIME;

import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.inputmethod.InputConnection;

public class RemoteBinderService extends Service {
    
    private final IClickableIME.Stub mBinder= new IClickableIME.Stub() {
        @Override
        public boolean click(float x, float y) throws RemoteException {
            EVIACAMIME.debug("click: (" + x + ", " + y + ")");

            InputMethodService ims= EViacamIMEService.getInstance();
            if (ims == null) return false;
            
            InputConnection ic = ims.getCurrentInputConnection();
            if (ic == null) return false;
            
            // TODO: perform click
            ic.commitText("!", 1);
            
            return true;
        }
    };
    
    @Override
    public void onCreate () {
        EVIACAMIME.debug("RemoteBinderService: onCreate");
    }
    
    /**
     * When binding to the service, we return an interface to the client
     */
    @Override
    public IBinder onBind(Intent intent) {
        EVIACAMIME.debug("onBind");
        return mBinder;
    }  
    
    @Override
    public void onDestroy () {
        EVIACAMIME.debug("RemoteBinderService: onDestroy");
    }
 }
