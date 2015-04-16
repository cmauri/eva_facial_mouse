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
