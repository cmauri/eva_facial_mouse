/*
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crea_si.eviacam.api;

import com.crea_si.eviacam.api.IReadyEventListener;
import com.crea_si.eviacam.api.IGamepadEventListener;
import com.crea_si.eviacam.api.IMouseEventListener;
import com.crea_si.eviacam.api.IDockPanelEventListener;

/**
 * AIDL main interface for the eviacam slave mode
 */
interface ISlaveMode {
    /**
     * Triggers the initialization of the remote service. The initialization might take
     * an arbitrary amount of time (logo splash, user conditions agreement, etc.)
     * This method should be called ONLY ONCE, otherwise the behaviour is undefined.
     *
     * @param listener Listener that will be called once the initialization is completed.
     *                 This parameter is mandatory and cannot be null, in such a case, no
     *                 initialization will be performed.
     */
    void init(in IReadyEventListener listener);
    boolean start();
    void stop();

    void setOperationMode(in int mode);

    boolean registerGamepadListener (in IGamepadEventListener listener);
    void unregisterGamepadListener ();

    boolean registerMouseListener (in IMouseEventListener listener);
    void unregisterMouseListener ();

    boolean registerDockPanelListener (in IDockPanelEventListener listener);
    void unregisterDockPanelListener ();
}
