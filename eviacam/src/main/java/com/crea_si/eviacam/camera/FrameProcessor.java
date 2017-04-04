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

package com.crea_si.eviacam.camera;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.opencv.core.Mat;

/**
 * Interface to implement a callback each time a camera frame is captured
 */
public interface FrameProcessor {
    /**
     * Process a captured frame
     * @param rgba captured frame
     */
    void processFrame(@NonNull Mat rgba);

    /**
     * Called when the camera is started
     */
    void onCameraStarted();

    /**
     * Called when the camera is stopped
     */
    void onCameraStopped();

    /**
     * Called when the camera initialization failed
     * @param error throwable that caused the error
     */
    void onCameraError(@NonNull Throwable error);
}
