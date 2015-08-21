package com.crea_si.eviacam.api_demo;

import com.crea_si.eviacam.api.ISlaveMode;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity implements ServiceConnection {
    private static String TAG = "EVIACAM_API_DEMO";

    private static final String REMOTE_PACKAGE= "com.crea_si.eviacam.service";
    private static final String REMOTE_ACTION= REMOTE_PACKAGE + ".SlaveModeService";
    
    // binder (proxy) with the remote input method service
    private ISlaveMode mSlaveMode;
    
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
        if (id == R.id.action_test) {
            MyCanvas cv= (MyCanvas) findViewById(R.id.the_canvas);
            cv.setPosition(new PointF(0,0));
            cv.postInvalidate();
            return true;
        }
        else if (id == R.id.action_bind) {
            doBind();
        }
        else if (id == R.id.action_unbind) {
            doUnbind();
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void doBind() {
        Log.d(TAG, "Attemp to bind to EViacam API");
        Intent intent= new Intent(REMOTE_ACTION);
        intent.setPackage(REMOTE_PACKAGE);
        try {
            if (!bindService(intent, this, Context.BIND_AUTO_CREATE)) {
                Log.d(TAG, "Cannot bind remote API");
            }
        }
        catch(SecurityException e) {
            Log.d(TAG, "Cannot bind remote API. Security exception.");
        }
    }
    
    private void doUnbind() {
        unbindService(this);
        mSlaveMode = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.
        Log.d(TAG, "EViacam API:onServiceConnected: " + name.toString());
        mSlaveMode = ISlaveMode.Stub.asInterface(service);        
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        Log.d(TAG, "EViacam API:onServiceDisconnected");
        unbindService(this);
        mSlaveMode = null;
    }
}
