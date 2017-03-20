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
package com.crea_si.eviacam.common;

import android.graphics.PointF;
import android.support.annotation.NonNull;

/**
 * Interface for mouse emulation callbacks
 */

public interface MouseEmulationCallbacks {

    /**
     * Called each time a mouse event is generated, either motion event and/or click event
     * @param location location of the pointer is screen coordinates
     * @param click true when click generated
     */
    void onMouseEvent(@NonNull PointF location, boolean click);

    /**
     * Called to ask whether a certain location of the screen is clickable.
     * It is used to enable/disable the dwell click countdown
     *
     * @param location location of the pointer is screen coordinates
     * @return true if the point is clickable
     */
    boolean isClickable(@NonNull PointF location);
}
