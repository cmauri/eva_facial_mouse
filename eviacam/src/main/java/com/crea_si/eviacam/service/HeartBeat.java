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

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;


public class HeartBeat {
    private final Timer mTimer;
    private final Context mContext;
    private final String mMessage;
    
    public HeartBeat(Context context, String msg) {
        mContext= context;
        mTimer= new Timer();
        mMessage= msg;
    }
    
    public void start() {
        mTimer.scheduleAtFixedRate(new mainTask(), 0, 5000);
    }
    
    public void stop() {
        mTimer.cancel();
    }
    
    private class mainTask extends TimerTask {
        public void run() {
            toastHandler.sendEmptyMessage(0);
        }
    }

    private final Handler toastHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(
                    mContext, mMessage, Toast.LENGTH_SHORT).show();
        }
    };
}
