/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
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
package com.crea_si.eviacam.slavemode;

import android.support.annotation.Nullable;

import com.crea_si.eviacam.api.IDockPanelEventListener;
import com.crea_si.eviacam.api.IGamepadEventListener;
import com.crea_si.eviacam.api.IMouseEventListener;
import com.crea_si.eviacam.common.Engine;

/**
 * Interface for slave mode service engine 
 */
public interface SlaveModeEngine extends Engine {
    void setSlaveOperationMode(int mode);
    boolean registerGamepadListener(@Nullable IGamepadEventListener l);
    void unregisterGamepadListener();
    boolean registerMouseListener(@Nullable IMouseEventListener l);
    void unregisterMouseListener();
    void unregisterDockPanelListener();
    boolean registerDockPanelListener(@Nullable IDockPanelEventListener l);
}
