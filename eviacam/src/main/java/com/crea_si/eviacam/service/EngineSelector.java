/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.crea_si.eviacam.service;

import com.crea_si.eviacam.slavemode.SlaveModeEngine;
import com.crea_si.eviacam.slavemode.SlaveModeEngineImpl;

/**
 * Class which allows to select among of the available engine modes. Currently:
 *   * Accessibility service mode: this is the main mode for EVA and can only be started from
 *     an accessibility service
 *   * Slave mode: in this mode, EVA can be run using its API
 *
 * Both modes CANNOT run simultaneously
 */
public class EngineSelector {
    /*
    * Modes of operation from the point of view of the service
    * that starts the engine
    */
    private static final int NONE_MODE= -1;
    private static final int A11Y_SERVICE_MODE= 0;
    private static final int SLAVE_MODE= 1;

    /* singleton instances */
    private static AccessibilityServiceModeEngine sAccessibilityServiceModeEngine = null;
    private static SlaveModeEngineImpl sSlaveModeEngine = null;

    /* mode in use */
    private static int mModeInUse= NONE_MODE;

    /**
     * Get an instance to the current accessibility mode engine
     *
     * @return a reference to the engine interface or null if not available
     */
    public static AccessibilityServiceModeEngine getAccessibilityServiceModeEngine() {
        if (mModeInUse == SLAVE_MODE) return null;

        mModeInUse= A11Y_SERVICE_MODE;

        if (sAccessibilityServiceModeEngine == null) {
            sAccessibilityServiceModeEngine= new AccessibilityServiceModeEngineImpl();
        }

        return sAccessibilityServiceModeEngine;
    }

    /**
     * Release accessibility service mode if previously requested
     */
    public static void releaseAccessibilityServiceModeEngine() {
        if (mModeInUse== A11Y_SERVICE_MODE) mModeInUse= NONE_MODE;
    }

    /**
     * Get an instance to the current accessibility mode engine
     *
     * @return a reference to the engine interface or null if not available
     */
    public static SlaveModeEngine getSlaveModeEngine() {
        if (mModeInUse == A11Y_SERVICE_MODE) return null;

        mModeInUse= SLAVE_MODE;

        if (sSlaveModeEngine == null) {
            sSlaveModeEngine= new SlaveModeEngineImpl();
        }

        return sSlaveModeEngine;
    }

    /**
     * Release accessibility service mode if previously requested
     */
    public static void releaseSlaveModeEngine() {
        if (mModeInUse== SLAVE_MODE) mModeInUse= NONE_MODE;
    }
}
