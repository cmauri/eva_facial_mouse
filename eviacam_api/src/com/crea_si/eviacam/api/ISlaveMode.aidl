package com.crea_si.eviacam.api;

import com.crea_si.eviacam.api.IGamepadEventListener;

interface ISlaveMode {
    boolean start();
    void stop();
    void setOperationMode(int mode);
    boolean registerGamepadListener (IGamepadEventListener listener);
	void unregisterGamepadListener ();
}
