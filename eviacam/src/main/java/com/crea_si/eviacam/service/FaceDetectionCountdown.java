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
package com.crea_si.eviacam.service;

import android.content.Context;
import android.content.SharedPreferences;

import com.crea_si.eviacam.Preferences;

/**
 * Manages the time elapsed since last face detection.
 *
 * It is basically a countdown with a preference listener.
 */
class FaceDetectionCountdown extends Countdown
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final Context mContext;

    /**
     * Constructor
     * @param c the context
     */
    FaceDetectionCountdown(Context c) {
        super(0);
        mContext= c;

        // preferences
        SharedPreferences sp= Preferences.getSharedPreferences(c);
        sp.registerOnSharedPreferenceChangeListener(this);
        updateSettings(sp);
    }

    public void cleanup() {
        Preferences.getSharedPreferences(mContext).
                unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (key.equals(Preferences.KEY_TIME_WITHOUT_DETECTION)) {
            updateSettings(sp);
        }
    }

    private void updateSettings(SharedPreferences sp) {
        setTimeToWait(Preferences.getTimeWithoutDetection(sp) * 1000);
    }

    public boolean isDisabled() {
        return getTimeToWait() == 0;
    }
}
