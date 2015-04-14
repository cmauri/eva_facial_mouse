package com.crea_si.eviacam_keyboard;

import android.app.Service;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

public class MessengerService extends Service {
    /** Command to the service to display a message */
    static final int MSG_SAY_HELLO = 1;

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            EVIACAMIME.debug("handleMessage");
            switch (msg.what) {
                case MSG_SAY_HELLO:
                    Toast.makeText(getApplicationContext(), "hello!", Toast.LENGTH_SHORT).show();
                    EVIACAMIME.debug("handleMessage: hello message received!");
                    
                    
                    
                    InputMethodService ims= EViacamIMEService.getInstance();
                    if (ims == null) return;
                    
                    InputConnection ic = ims.getCurrentInputConnection();
                    if (ic == null) return;
                    
                    ic.commitText("VOILA", 1);
                    
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
  
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
        EVIACAMIME.debug("onBind");
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }
}