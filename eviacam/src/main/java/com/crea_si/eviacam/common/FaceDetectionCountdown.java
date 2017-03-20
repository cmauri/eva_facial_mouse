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
package com.crea_si.eviacam.common;

import android.content.SharedPreferences;

import com.crea_si.eviacam.util.Countdown;

/**
 * Manages the time elapsed since last face detection.
 *
 * It is basically a countdown with a preference listener.
 *
 * TODO: review this class because is used as a countdown but
 * also to notify face tracking feedback
 */
public class FaceDetectionCountdown extends Countdown
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Constructor
     */
    FaceDetectionCountdown() {
        super(0);

        // preferences
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
        updateSettings();
    }

    public void cleanup() {
        Preferences.get().getSharedPreferences().
                unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals(Preferences.KEY_TIME_WITHOUT_DETECTION)) {
            updateSettings();
        }
    }

    private void updateSettings() {
        setTimeToWait(Preferences.get().getTimeWithoutDetection() * 1000);
    }

    public boolean isDisabled() {
        return getTimeToWait() == 0;
    }
}
