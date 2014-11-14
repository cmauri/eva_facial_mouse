package com.crea_si.eviacam.service;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;


public class HeartBeat {
    private Timer mTimer;
    private Context mContext;
    
    public HeartBeat(Context context) {
        mContext= context;
        mTimer= new Timer();
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
            Toast.makeText(mContext, "test", Toast.LENGTH_SHORT)
                    .show();
        }
    };

}
