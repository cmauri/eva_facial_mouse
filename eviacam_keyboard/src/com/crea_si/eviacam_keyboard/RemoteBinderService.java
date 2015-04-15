package com.crea_si.eviacam_keyboard;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.crea_si.input_method_aidl.IClickableIME;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

/**
 * Listens to and dispatches remote click requests for this IME
 */

public class RemoteBinderService extends Service {

    // instance of this service
    // private static RemoteBinderService gInstance;
    
    // handler for the main thread, used to send 
    Handler mMainThreadHandler;
    
    // binder stub, receives remote requests on a secondary thread
    private final IClickableIME.Stub mBinder= new IClickableIME.Stub() {
        @Override
        public boolean click(float x, float y) throws RemoteException {
            //boolean isMainThread= Looper.myLooper() == Looper.getMainLooper();
            //EVIACAMIME.debug("click: (" + x + ", " + y + "). Is main thread:" + isMainThread);
            
            // pass the control to the main thread to facilitate implementation of the IME
            return click_main_thread(x, y);
        }
    };
    
    /** Calls click on the main thread and waits for the result */
    private boolean click_main_thread(final float x, final float y) {
        FutureTask<Boolean> futureResult = new FutureTask<Boolean>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                //boolean isMainThread= Looper.myLooper() == Looper.getMainLooper();
                //EVIACAMIME.debug("Inside callable (beegin):" + isMainThread);
                //Thread.sleep(2000);
                //EVIACAMIME.debug("Inside callable (end):");
                return EViacamIMEService.click(x, y);
            }
        });
        
        mMainThreadHandler.post(futureResult);

        // this block until the result is calculated!
        try {
            //EVIACAMIME.debug("futureResult.get(): before");
            boolean result= futureResult.get();
            //EVIACAMIME.debug("futureResult.get(): after");
            return result;
        } 
        catch (ExecutionException e) {
            EVIACAMIME.debug("click_main_thread has thrown an exception" + e.getMessage()); 
            return false;
        } catch (InterruptedException e) {
            EVIACAMIME.debug("click_main_thread has thrown an exception" + e.getMessage()); 
            return false;
        }
    }
    
    
    @Override
    public void onCreate () {
        EVIACAMIME.debug("RemoteBinderService: onCreate");
        mMainThreadHandler= new Handler();
        /*
        if (gInstance != null) {
            EVIACAMIME.warning("RemoteBinderService. onCreate. already existing instance!");
        }
        gInstance= this;
        */
    }
    
    /** When binding to the service, we return an interface to the client */
    @Override
    public IBinder onBind(Intent intent) {
        EVIACAMIME.debug("onBind");
        return mBinder;
    }
    
    @Override
    public boolean onUnbind (Intent intent) {
        EVIACAMIME.debug("onUnbind");
        //gInstance= null;
        return false;
    }
    
    @Override
    public void onDestroy () {
        EVIACAMIME.debug("RemoteBinderService: onDestroy");
        //gInstance= null;
    }
 }
