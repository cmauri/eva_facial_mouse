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

/**
 * Interface for listening the status of the slave mode service
 */
public interface SlaveModeStatusListener {
    /***
     * Called when the remote EVA service is connected and ready
     * @param sm reference to the service facade or null if null if something went wrong
     */
    void onReady(SlaveMode sm);
    void onDisconnected();
}
