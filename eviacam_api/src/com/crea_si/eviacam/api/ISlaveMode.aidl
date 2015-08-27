package com.crea_si.eviacam.api;

import com.crea_si.eviacam.api.IPadEventListener;

interface ISlaveMode {
    boolean start();
    void stop();
    void setOperationMode(int mode);
    boolean registerListener (IPadEventListener listener);
	void unregisterListener ();
}
