package com.crea_si.eviacam_keyboard;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.crea_si.input_method_aidl.IClickableIME;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * Listens to and dispatches remote click requests for this IME
 */

public class RemoteBinderService extends Service {
    
    // handler for the main thread, used to forward calls 
    Handler mMainThreadHandler;
    
    // binder stub, receives remote requests on a secondary thread
    private final IClickableIME.Stub mBinder= new IClickableIME.Stub() {
        @Override
        public boolean click(float x, float y) throws RemoteException {
            // pass the control to the main thread to facilitate implementation of the IME
            return click_main_thread(x, y);
        }
    };
    
    /** Calls click on the main thread and waits for the result */
    private boolean click_main_thread(final float x, final float y) {
        FutureTask<Boolean> futureResult = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return EViacamIMEService.click((int) x, (int) y);
            }
        });
        
        mMainThreadHandler.post(futureResult);
        
        try {
            // this block until the result is calculated!
            return futureResult.get();
        } 
        catch (ExecutionException e) {
            EVIACAMIME.debug("RemoteBinderService: exception: " + e.getMessage()); 
        } 
        catch (InterruptedException e) {
            EVIACAMIME.debug("RemoteBinderService: exception: " + e.getMessage()); 
        }
        return false;
    }

    @Override
    public void onCreate () {
        EVIACAMIME.debug("RemoteBinderService: onCreate");
        mMainThreadHandler= new Handler();
    }
    
    /** When binding to the service, we return an interface to the client */
    @Override
    public IBinder onBind(Intent intent) {
        EVIACAMIME.debug("RemoteBinderService: onBind");
        return mBinder;
    }
    
    @Override
    public boolean onUnbind (Intent intent) {
        EVIACAMIME.debug("RemoteBinderService: onUnbind");
        return false;
    }
    
    @Override
    public void onDestroy () {
        EVIACAMIME.debug("RemoteBinderService: onDestroy");
    }
 }
