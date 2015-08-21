/*
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
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

package com.crea_si.eviacam.service;

import com.crea_si.eviacam.api.IPadEventListener;
import com.crea_si.eviacam.api.ISlaveMode;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * 
 *
 * TODO: improve security
 * TODO: allow one client only
 */

public class SlaveModeService extends Service {

    // handler used to forward calls to the main thread 
    Handler mMainThreadHandler;

    // binder stub, receives remote requests on a secondary thread
    private final ISlaveMode.Stub mBinder= new ISlaveMode.Stub() {
        @Override
        public boolean registerListener(IPadEventListener arg0)
                throws RemoteException {
            EVIACAM.debug("SlaveModeService.registerListener");
            return false;
        }

        @Override
        public void unregisterListener(IPadEventListener arg0)
                throws RemoteException {
            EVIACAM.debug("SlaveModeService.unregisterListener");
            
        }
    };

    @Override
    public void onCreate () {
        EVIACAM.debug("SlaveModeService: onCreate");
        EVIACAM.debugInit(this);
        mMainThreadHandler= new Handler();
    }

    /** When binding to the service, we return an interface to the client */
    @Override
    public IBinder onBind(Intent intent) {
        EVIACAM.debug("SlaveModeService: onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind (Intent intent) {
        EVIACAM.debug("SlaveModeService: onUnbind");
        return false;
    }

    @Override
    public void onDestroy () {
        EVIACAM.debug("SlaveModeService: onDestroy");
    }
 }
