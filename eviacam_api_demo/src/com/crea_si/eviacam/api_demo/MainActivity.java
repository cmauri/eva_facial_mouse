package com.crea_si.eviacam.api_demo;

import com.crea_si.eviacam.api.IPadEventListener;
import com.crea_si.eviacam.api.GamepadButtons;
import com.crea_si.eviacam.api.SlaveMode;
import com.crea_si.eviacam.api.SlaveModeConnection;

import android.app.Activity;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity implements 
    SlaveModeConnection, IPadEventListener {
    
    private static float INC= 0.05f;
    
    // binder (proxy) with the remote input method service
    private SlaveMode mSlaveMode;

    private PointF mPoint= new PointF(0, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_bind) {
            SlaveMode.initConnection(this, this);
        }
        else if (id == R.id.action_unbind) {
            if (mSlaveMode!= null) mSlaveMode.disconnect();
        }
        else if (id == R.id.action_register_events) {
            if (mSlaveMode!= null) mSlaveMode.registerListener(this);
        }
        else if (id == R.id.action_unregister_events) {
            if (mSlaveMode!= null) {
                mSlaveMode.unregisterListener();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /* This is called from a secondary thread */
    @Override
    public void buttonPressed(int arg0) throws RemoteException {
        switch (arg0) {
        case GamepadButtons.PAD_DOWN:
            mPoint.y+= INC;
            break;
        case GamepadButtons.PAD_DOWN_LEFT:
            mPoint.x-= INC;
            mPoint.y+= INC;
            break;
        case GamepadButtons.PAD_LEFT:
            mPoint.x-= INC;
            break;
        case GamepadButtons.PAD_UP_LEFT:
            mPoint.x-= INC;
            mPoint.y-= INC;
            break;
        case GamepadButtons.PAD_UP:
            mPoint.y-= INC;
            break;
        case GamepadButtons.PAD_UP_RIGHT:
            mPoint.x+= INC;
            mPoint.y-= INC;
            break;
        case GamepadButtons.PAD_RIGHT:
            mPoint.x+= INC;
            break;
        case GamepadButtons.PAD_DOWN_RIGHT:
            mPoint.x+= INC;
            mPoint.y+= INC;
            break;
        }
        
        if (mPoint.x< -1.0f) mPoint.x= -1.0f;
        else if (mPoint.x> 1.0f) mPoint.x= 1.0f;
        if (mPoint.y< -1.0f) mPoint.y= -1.0f;
        else if (mPoint.y> 1.0f) mPoint.y= 1.0f;
        
        MyCanvas cv= (MyCanvas) findViewById(R.id.the_canvas);
        cv.setPosition(mPoint);
        cv.postInvalidate();
    }

    /* This is called from a secondary thread */
    @Override
    public void buttonReleased(int arg0) throws RemoteException {
    }

    @Override
    public void onConnected(SlaveMode connection) {
        mSlaveMode= connection;
        
        // Uncomment if you wish to start listening to events ASAP
        //if (mSlaveMode!= null) mSlaveMode.registerListener(this);
    }

    @Override
    public void onDisconnected() {
        mSlaveMode = null;
    }

    // TODO: remove
    @Override
    public IBinder asBinder() {
        return null;
    }
}
