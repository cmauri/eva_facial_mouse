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

package com.crea_si.softkeyboard;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.crea_si.input_method_aidl.IClickableIME;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.acra.ACRA;

/**
 * Listens to and dispatches remote requests
 */
public class RemoteBinderService extends Service {

    // handler used to forward calls to the main thread 
    Handler mMainThreadHandler;

    // binder stub, receives remote requests on a secondary thread
    private final IClickableIME.Stub mBinder= new IClickableIME.Stub() {
        @Override
        public boolean click(int x, int y) throws RemoteException {
            // pass the control to the main thread to facilitate implementation of the IME
            if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "RemoteBinderService: click");
            return click_main_thread(x, y);
        }

        @Override
        public void openIME() throws RemoteException {
            Runnable r= new Runnable() {
                @Override
                public void run() {
                    if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "RemoteBinderService: openIME");
                    SoftKeyboard.openIME();
                }
            };
            mMainThreadHandler.post(r);
        }

        @Override
        public void closeIME() throws RemoteException {
            Runnable r= new Runnable() {
                @Override
                public void run() {
                    if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "RemoteBinderService: closeIME");
                    SoftKeyboard.closeIME();
                }
            };
            mMainThreadHandler.post(r);
        }

        @Override
        public void toggleIME() throws RemoteException {
            Runnable r= new Runnable() {
                @Override
                public void run() {
                    if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "RemoteBinderService: closeIME");
                    SoftKeyboard.toggleIME();
                }
            };
            mMainThreadHandler.post(r);
        }
    };

    /** Calls click on the main thread and waits for the result */
    private boolean click_main_thread(final int x, final int y) {
        FutureTask<Boolean> futureResult = new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    return SoftKeyboard.click(x, y);
                }
                catch(Exception e) {
                    /* In case of exception, return that the operation
                       did not work and report the error */
                    ACRA.getErrorReporter().handleException(e);
                    return false;
                }
            }
        });

        mMainThreadHandler.post(futureResult);

        try {
            // this block until the result is calculated
            return futureResult.get();
        } 
        catch (Exception e) {
            ACRA.getErrorReporter().handleException(e);
        }
        return false;
    }

    @Override
    public void onCreate () {
        if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "RemoteBinderService: onCreate");
        mMainThreadHandler= new Handler();
    }

    /** When binding to the service, we return an interface to the client */
    @Override
    public IBinder onBind(Intent intent) {
        if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "RemoteBinderService: onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind (Intent intent) {
        if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "RemoteBinderService: onUnbind");
        return false;
    }

    @Override
    public void onDestroy () {
        if (BuildConfig.DEBUG) Log.d(EVIACAMSOFTKBD.TAG, "RemoteBinderService: onDestroy");
    }
 }
