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

package com.crea_si.eviacam.a11yservice;

import android.support.annotation.NonNull;
import android.view.accessibility.AccessibilityEvent;

import com.crea_si.eviacam.common.Engine;

/**
 * Interface for the engine in accessibility service mode
 */
public interface AccessibilityServiceModeEngine extends Engine {
    /**
     * Enable/disable on-screen pointer
     */
    void enablePointer();
    void disablePointer();

    /**
     * Enable/disable dwell based click action
     */
    void enableClick();
    void disableClick();

    /**
     * Show/hide the docking menu panel
     */
    void enableDockPanel();
    void disableDockPanel();

    /**
     * Enable/disable scroll buttons
     */
    void enableScrollButtons();
    void disableScrollButtons();

    /**
     * Enable all above elements
     */
    void enableAll();

    /**
     * Method to route the accessibility events received by the accessibility service
     * @param event the accessibility event
     */
    void onAccessibilityEvent(@NonNull AccessibilityEvent event);
}
