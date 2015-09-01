package com.crea_si.eviacam.api;

oneway interface IGamepadEventListener {
	void buttonPressed (in int button);
	void buttonReleased (in int button);
}
