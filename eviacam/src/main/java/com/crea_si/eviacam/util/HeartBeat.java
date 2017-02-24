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
package com.crea_si.eviacam.util;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

/**
 * Intermittent toast message
 */
public class HeartBeat {
    private final Timer mTimer;
    private final Handler mToastHandler;

    /* Declare handler as static inner class to avoid memory leaks. See:
        http://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
     */
    private static class HeartBeatHandler extends Handler {
        private final WeakReference<Context> mContext;
        private final String mMessage;

        HeartBeatHandler(Context c, String msg) {
            mContext= new WeakReference<>(c);
            mMessage= msg;
        }

        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(mContext.get(), mMessage, Toast.LENGTH_SHORT).show();
        }
    }

    public HeartBeat(final Context context, String msg) {
        mTimer= new Timer();
        mToastHandler = new HeartBeatHandler(context, msg);
    }
    
    public void start() {
        mTimer.scheduleAtFixedRate(new mainTask(), 0, 5000);
    }
    
    public void stop() {
        mTimer.cancel();
    }
    
    private class mainTask extends TimerTask {
        public void run() {
            mToastHandler.sendEmptyMessage(0);
        }
    }
}
