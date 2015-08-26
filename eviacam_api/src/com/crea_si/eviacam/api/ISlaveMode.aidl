package com.crea_si.eviacam.api;

import com.crea_si.eviacam.api.IPadEventListener;

interface ISlaveMode {
    boolean registerListener (IPadEventListener listener);
	void unregisterListener ();
}
