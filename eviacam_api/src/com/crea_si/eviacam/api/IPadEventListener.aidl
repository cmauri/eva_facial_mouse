package com.crea_si.eviacam.api;

oneway interface IPadEventListener {
	void buttonPressed (in int button);
	void buttonReleased (in int button);
}
