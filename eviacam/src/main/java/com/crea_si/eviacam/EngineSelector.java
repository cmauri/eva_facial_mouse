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
package com.crea_si.eviacam;

import com.crea_si.eviacam.a11yservice.AccessibilityServiceModeEngine;
import com.crea_si.eviacam.a11yservice.AccessibilityServiceModeEngineImpl;
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
    /* singleton instances */
    private static AccessibilityServiceModeEngine sAccessibilityServiceModeEngine = null;
    private static SlaveModeEngineImpl sSlaveModeEngine = null;

    /**
     * Get an instance to the current accessibility mode engine
     *
     * @return a reference to the engine interface or null if not available
     */
    public static AccessibilityServiceModeEngine initAccessibilityServiceModeEngine() {
        if (null != sSlaveModeEngine) return null;

        if (null != sAccessibilityServiceModeEngine) {
            throw new IllegalStateException(
                    "initAccessibilityServiceModeEngine: already initialized");
        }
        sAccessibilityServiceModeEngine= new AccessibilityServiceModeEngineImpl();

        return sAccessibilityServiceModeEngine;
    }

    public static AccessibilityServiceModeEngine getAccessibilityServiceModeEngine() {
        return sAccessibilityServiceModeEngine;
    }

    /**
     * Release accessibility service mode if previously requested
     */
    public static void releaseAccessibilityServiceModeEngine() {
        if (null != sAccessibilityServiceModeEngine) {
            sAccessibilityServiceModeEngine.cleanup();
            sAccessibilityServiceModeEngine = null;
        }
    }

    /**
     * Get an instance to the current accessibility mode engine
     *
     * @return a reference to the engine interface or null if not available
     */
    public static SlaveModeEngine initSlaveModeEngine() {
        if (null != sAccessibilityServiceModeEngine) return null;

        if (null != sSlaveModeEngine) {
            throw new IllegalStateException("initSlaveModeEngine: already initialized");
        }

        sSlaveModeEngine= new SlaveModeEngineImpl();

        return sSlaveModeEngine;
    }

    /**
     * Release accessibility service mode if previously requested
     */
    public static void releaseSlaveModeEngine() {
        if (null != sSlaveModeEngine) {
            sSlaveModeEngine.cleanup();
            sSlaveModeEngine = null;
        }
    }
}
