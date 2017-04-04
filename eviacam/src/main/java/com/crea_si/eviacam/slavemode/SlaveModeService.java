/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.crea_si.eviacam.slavemode;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.api.IDockPanelEventListener;
import com.crea_si.eviacam.common.EVIACAM;
import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.api.IGamepadEventListener;
import com.crea_si.eviacam.api.IMouseEventListener;
import com.crea_si.eviacam.api.ISlaveMode;
import com.crea_si.eviacam.api.IReadyEventListener;
import com.crea_si.eviacam.common.Engine;
import com.crea_si.eviacam.EngineSelector;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

/**
 * EViacam slave mode service entry point
 * 
 * TODO: improve security
 */

public class SlaveModeService extends Service implements Engine.OnInitListener {
    // handler used to forward calls to the main thread 
    private final Handler mMainThreadHandler= new Handler();

    // reference to the engine to which incoming calls will be delegated
    private SlaveModeEngine mSlaveModeEngine;

    // Reference to notify when the service is ready to start receiving commands
    private IReadyEventListener mOnReadyListener;

    // binder stub, receives remote requests on a secondary thread
    private final ISlaveMode.Stub mBinder= new ISlaveMode.Stub() {
        /**
         * Triggers the initialization of the remote service. The initialization might take
         * an arbitrary amount of time (logo splash, user conditions agreement, etc.)
         * This method should be called ONLY ONCE, otherwise the behaviour is undefined.
         *
         * @param listener Listener that will be called once the initialization is completed.
         *                 This parameter is mandatory and cannot be null, in such a case, no
         *                 initialization will be performed.
         */
        @Override
        public void init(@NonNull final IReadyEventListener listener) throws RemoteException {
            Log.i(EVIACAM.TAG, "SlaveModeService.init: enter. Listener:" + listener);

            Runnable r= new Runnable() {
                @Override
                public void run() {
                    if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.init: runnable: enter");
                    /* No engine, initialization failed */
                    if (mSlaveModeEngine== null) {
                        try {
                            listener.onReadyEvent(false);
                        } catch (RemoteException e) {
                            // Nothing to do
                        }
                        return;
                    }

                    /* Already initialized, just run the callback */
                    if (mSlaveModeEngine.getState() != Engine.STATE_DISABLED) {
                        try {
                            listener.onReadyEvent(true);
                        } catch (RemoteException e) {
                            // Nothing to do
                        }
                        return;
                    }

                    /* Start the initialization sequence */
                    mOnReadyListener= listener;
                    if (!mSlaveModeEngine.init(SlaveModeService.this, SlaveModeService.this)) {
                        /*
                         * The engine manager initialization failed, this means that has been
                         * already started as accessibility service. Deny binding.
                        */
                        try {
                            listener.onReadyEvent(false);
                        } catch (RemoteException e) {
                            // Nothing to do
                        }
                    }
                    if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.init: runnable: finish");
                }
            };
            mMainThreadHandler.post(r);
        }

        @Override
        public boolean start() throws RemoteException {
            Log.i(EVIACAM.TAG, "SlaveModeService.start");
            FutureTask<Boolean> futureResult = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return mSlaveModeEngine != null && mSlaveModeEngine.start();
                }
            });

            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                return futureResult.get();
            } 
            catch (ExecutionException | InterruptedException e) {
                Log.e(EVIACAM.TAG, "SlaveModeService: exception: " + e.getMessage());
            }
            return false;
        }

        @Override
        public void stop() throws RemoteException {
            Log.i(EVIACAM.TAG, "SlaveModeService.stop");
            FutureTask<Void> futureResult = new FutureTask<>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (mSlaveModeEngine == null) return null;
                    mSlaveModeEngine.stop();
                    return null;
                }
            });

            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                futureResult.get();
            } 
            catch (ExecutionException | InterruptedException e) {
                Log.e(EVIACAM.TAG, "SlaveModeService: exception: " + e.getMessage());
            }
        }

        @Override
        public void setOperationMode(final int mode) throws RemoteException {
            FutureTask<Void> futureResult = new FutureTask<>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (mSlaveModeEngine == null) return null;
                    mSlaveModeEngine.setSlaveOperationMode(mode);
                    return null;
                }
            });

            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                futureResult.get();
            } 
            catch (ExecutionException | InterruptedException e) {
                Log.e(EVIACAM.TAG, "SlaveModeService: exception: " + e.getMessage());
            }
        }
        
        @Override
        public boolean registerGamepadListener(final IGamepadEventListener arg0)
                throws RemoteException {
            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.registerGamepadListener");

            FutureTask<Boolean> futureResult = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // TODO: if an exception is thrown, calling code always receive
                    // a RemoteException, it would be better to provide more information
                    // on the caller. See here:
                    // http://stackoverflow.com/questions/1800881/throw-a-custom-exception-from-a-service-to-an-activity
                    return mSlaveModeEngine != null &&
                           mSlaveModeEngine.registerGamepadListener(arg0);
                }
            });

            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                return futureResult.get();
            } 
            catch (ExecutionException | InterruptedException e) {
                Log.e(EVIACAM.TAG, "SlaveModeService: exception: " + e.getMessage());
            }
            return false;
        }

        @Override
        public void unregisterGamepadListener() throws RemoteException {
            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.unregisterGamepadListener");
            if (mSlaveModeEngine == null) return;

            Runnable r= new Runnable() {
                @Override
                public void run() {
                    mSlaveModeEngine.unregisterGamepadListener();
                }
            };
            mMainThreadHandler.post(r);
        }

        @Override
        public boolean registerMouseListener(final IMouseEventListener arg0)
                throws RemoteException {
            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.registerMouseListener");
            
            FutureTask<Boolean> futureResult = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // TODO: if an exception is thrown, calling code always receive
                    // a RemoteException, it would be better to provide more information
                    // on the caller. See here:
                    // http://stackoverflow.com/questions/1800881/throw-a-custom-exception-from-a-service-to-an-activity
                    return mSlaveModeEngine != null && mSlaveModeEngine.registerMouseListener(arg0);
                }
            });

            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                return futureResult.get();
            } 
            catch (ExecutionException | InterruptedException e) {
                Log.e(EVIACAM.TAG, "SlaveModeService: exception: " + e.getMessage());
            }
            return false;
        }

        @Override
        public void unregisterMouseListener() throws RemoteException {
            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.unregisterMouseListener");
            if (mSlaveModeEngine == null) return;

            Runnable r= new Runnable() {
                @Override
                public void run() {
                    mSlaveModeEngine.unregisterMouseListener();
                }
            };
            mMainThreadHandler.post(r);
        }

        @Override
        public boolean registerDockPanelListener(final IDockPanelEventListener arg0)
                throws RemoteException {
            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.registerDockPanelistener");

            FutureTask<Boolean> futureResult = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // TODO: if an exception is thrown, calling code always receive
                    // a RemoteException, it would be better to provide more information
                    // on the caller. See here:
                    // http://stackoverflow.com/questions/1800881/throw-a-custom-exception-from-a-service-to-an-activity
                    return mSlaveModeEngine != null && mSlaveModeEngine.registerDockPanelListener(arg0);
                }
            });

            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                return futureResult.get();
            }
            catch (ExecutionException | InterruptedException e) {
                Log.e(EVIACAM.TAG, "SlaveModeService: exception: " + e.getMessage());
            }
            return false;
        }

        @Override
        public void unregisterDockPanelListener() throws RemoteException {
            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.unregisterDockMenuListener");
            if (mSlaveModeEngine == null) return;

            Runnable r= new Runnable() {
                @Override
                public void run() {
                    mSlaveModeEngine.unregisterDockPanelListener();
                }
            };
            mMainThreadHandler.post(r);
        }
    };

    private void cleanup() {
        if (mSlaveModeEngine != null) {
            mSlaveModeEngine.cleanup();
            mSlaveModeEngine= null;
        }

        EngineSelector.releaseSlaveModeEngine();

        mOnReadyListener= null;

        if (Preferences.get() != null) {
            Preferences.get().cleanup();
        }
    }

    @Override
    public void onCreate () {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService: onCreate");
    }

    /** When binding to the service, we return an interface to the client */
    @Override
    public IBinder onBind(Intent intent) {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService.onBind: enter");
        if (mSlaveModeEngine!= null) {
            // Another client is connected. Do not allow.
            return null;
        }

        // Already initialized preferences, probably A11Y service running. Deny binding.
        if (Preferences.initForSlaveService(this) == null) return null;

        mSlaveModeEngine= EngineSelector.initSlaveModeEngine();
        if (mSlaveModeEngine== null) return null;

        return mBinder;
    }

    @Override
    public boolean onUnbind (Intent intent) {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService: onUnbind");
        cleanup();
        return false;
    }

    @Override
    public void onDestroy () {
        if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "SlaveModeService: onDestroy");
        cleanup();
    }

    /* Called when the engine finishes the initialization sequence */
    @Override
    public void onInit(int status) {
        Log.i(EVIACAM.TAG, "SlaveModeService.onInit(status= " + status + ")");
        IReadyEventListener listener= mOnReadyListener;
        if (listener== null) return;

        try {
            if (mSlaveModeEngine != null && status == 0) {
                // Initialization completed successfully
                listener.onReadyEvent(true);
            } else {
                listener.onReadyEvent(false);
            }
        }
        catch (RemoteException e) {
            Log.e(EVIACAM.TAG, "SlaveModeService: onInitException: " + e.getMessage());
        }
    }
}
