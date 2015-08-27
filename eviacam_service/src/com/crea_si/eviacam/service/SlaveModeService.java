/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
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

package com.crea_si.eviacam.service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.crea_si.eviacam.api.IPadEventListener;
import com.crea_si.eviacam.api.ISlaveMode;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * EViacam slave mode service entry point
 * 
 * TODO: improve security
 */

public class SlaveModeService extends Service {
    // handler used to forward calls to the main thread 
    private final Handler mMainThreadHandler= new Handler();
    
    private SlaveModeEngine mSlaveModeEngine;

    // binder stub, receives remote requests on a secondary thread
    private final ISlaveMode.Stub mBinder= new ISlaveMode.Stub() {
        @Override
        public boolean start() throws RemoteException {
            FutureTask<Boolean> futureResult = new FutureTask<Boolean>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    if (mSlaveModeEngine == null) return false;
                    return mSlaveModeEngine.start();
                }
            });
           
            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                return futureResult.get();
            } 
            catch (ExecutionException e) {
                EVIACAM.debug("SlaveModeService: exception: " + e.getMessage()); 
            } 
            catch (InterruptedException e) {
                EVIACAM.debug("SlaveModeService: exception: " + e.getMessage()); 
            }
            return false;
        }

        @Override
        public void stop() throws RemoteException {
            FutureTask<Void> futureResult = new FutureTask<Void>(new Callable<Void>() {
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
            catch (ExecutionException e) {
                EVIACAM.debug("SlaveModeService: exception: " + e.getMessage()); 
            } 
            catch (InterruptedException e) {
                EVIACAM.debug("SlaveModeService: exception: " + e.getMessage()); 
            }
        }

        @Override
        public void setOperationMode(final int mode) throws RemoteException {
            FutureTask<Void> futureResult = new FutureTask<Void>(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    if (mSlaveModeEngine == null) return null;
                    mSlaveModeEngine.setOperationMode(mode);
                    return null;
                }
            });
           
            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                futureResult.get();
            } 
            catch (ExecutionException e) {
                EVIACAM.debug("SlaveModeService: exception: " + e.getMessage()); 
            } 
            catch (InterruptedException e) {
                EVIACAM.debug("SlaveModeService: exception: " + e.getMessage()); 
            }
            
        }
        
        @Override
        public boolean registerListener(final IPadEventListener arg0)
                throws RemoteException {
            EVIACAM.debug("SlaveModeService.registerListener");
            
            FutureTask<Boolean> futureResult = new FutureTask<Boolean>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    // TODO: if an exception is thrown, calling code always receive
                    // a RemoteException, it would be better to provide more information
                    // on the caller. See here:
                    // http://stackoverflow.com/questions/1800881/throw-a-custom-exception-from-a-service-to-an-activity
                    if (mSlaveModeEngine== null) return false;
                    return mSlaveModeEngine.registerListener(arg0);
                }
            });
            
            mMainThreadHandler.post(futureResult);

            try {
                // this block until the result is calculated
                return futureResult.get();
            } 
            catch (ExecutionException e) {
                EVIACAM.debug("SlaveModeService: exception: " + e.getMessage()); 
            } 
            catch (InterruptedException e) {
                EVIACAM.debug("SlaveModeService: exception: " + e.getMessage()); 
            }
            return false;
        }

        @Override
        public void unregisterListener() throws RemoteException {
            EVIACAM.debug("SlaveModeService.unregisterListener");
            if (mSlaveModeEngine == null) return;

            Runnable r= new Runnable() {
                @Override
                public void run() {
                    mSlaveModeEngine.unregisterListener();
                    mSlaveModeEngine.cleanup();
                    mSlaveModeEngine= null;
                }
            };
            mMainThreadHandler.post(r);
        }
    };

    @Override
    public void onCreate () {
        EVIACAM.debug("SlaveModeService: onCreate");
        EVIACAM.debugInit(this);
    }

    /** When binding to the service, we return an interface to the client */
    @Override
    public IBinder onBind(Intent intent) {
        EVIACAM.debug("SlaveModeService: onBind");
        if (mSlaveModeEngine!= null) {
            // Another client is connected. Do not allow.
            return null;
        }

        mSlaveModeEngine= 
            EngineManager.getInstance().getSlaveModeEngine(this);

        if (mSlaveModeEngine== null) {
            /* 
             * The engine manager returned null, this means that has been
             * already started as accessibility service. Deny binding.
             */
            return null;
        }
        
        return mBinder;
    }

    @Override
    public boolean onUnbind (Intent intent) {
        EVIACAM.debug("SlaveModeService: onUnbind");
        if (mSlaveModeEngine != null) {
            //mSlaveModeEngine.unregisterListener();
            mSlaveModeEngine.cleanup();
            mSlaveModeEngine= null;
        }
        return false;
    }

    @Override
    public void onDestroy () {
        EVIACAM.debug("SlaveModeService: onDestroy");
        EVIACAM.debugCleanup();
    }
 }
