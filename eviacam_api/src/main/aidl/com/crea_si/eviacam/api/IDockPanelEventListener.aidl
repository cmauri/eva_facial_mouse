/*
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
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

import android.view.MotionEvent;

/**
 * AIDL interface for the dock panel options
 */
oneway interface IDockPanelEventListener {
    /**
     * Options for the docking menu
     */
    const int BACK = 1;
    const int HOME = 2;
    const int RECENTS = 3;
    const int NOTIFICATIONS = 4;
    const int KEYBOARD = 5;
    const int CONTEXT_MENU = 6;
    const int REST_MODE = 7;

    void onDockMenuOption (in int option);
}
