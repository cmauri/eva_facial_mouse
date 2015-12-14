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

package com.crea_si.eviacam;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

public class Preferences {
    /**
     * Preference file names
     */
    public static final String FILE_SLAVE_MODE= 
            Preferences.class.getPackage().getName() + ".slave_mode";

    /**
     * Preference keys
     */
    public static final String KEY_HORIZONTAL_SPEED = "horizontal_speed";
    public static final String KEY_VERTICAL_SPEED = "vertical_speed";
    public static final String KEY_ACCELERATION= "acceleration";
    public static final String KEY_MOTION_SMOOTHING= "motion_smoothing";
    public static final String KEY_MOTION_THRESHOLD= "motion_threshold";
    public static final String KEY_DWELL_TIME= "dwell_time";
    public static final String KEY_DWELL_AREA= "dwell_area";
    public static final String KEY_SOUND_ON_CLICK= "sound_on_click";
    public static final String KEY_CONSECUTIVE_CLICKS = "consecutive_clicks";
    public static final String KEY_DOCKING_PANEL_EDGE= "docking_panel_edge";
    public static final String KEY_UI_ELEMENTS_SIZE= "ui_elements_size";
    public static final String KEY_TIME_WITHOUT_DETECTION= "time_without_detection";
    public static final String KEY_GAMEPAD_LOCATION= "gamepad_location";
    public static final String KEY_GAMEPAD_TRANSPARENCY= "gamepad_transparency";
    public static final String KEY_GAMEPAD_ABS_SPEED= "gamepad_abs_speed";
    public static final String KEY_GAMEPAD_REL_SENSITIVITY= "gamepad_rel_sensitivity";
    public static final String KEY_MOUSE_CALIBRATION_PERFORMED= "mouse_calibration_performed";

    /**
        Run-time constants
     */
    private static boolean sRuntimeConstantsLoaded= false;
    private static int AXIS_SPEED_DEFAULT;
    private static int AXIS_SPEED_MIN;
    private static int AXIS_SPEED_MAX;
    private static int ACCELERATION_DEFAULT;
    private static int ACCELERATION_MIN;
    private static int ACCELERATION_MAX;
    private static int MOTION_SMOOTHING_DEFAULT;
    private static int MOTION_SMOOTHING_MIN;
    private static int MOTION_SMOOTHING_MAX;
    private static int MOTION_THRESHOLD_DEFAULT;
    private static int MOTION_THRESHOLD_MIN;
    private static int MOTION_THRESHOLD_MAX;
    private static boolean SOUND_ON_CLICK_DEFAULT;


    public static SharedPreferences getSharedPreferences(Context c) {
        return ((EViacamApplication) c.getApplicationContext()).getSharedPreferences();
    }

    private static void loadRuntimeConstants (Context c) {
        if (sRuntimeConstantsLoaded) return;
        final Resources r= c.getResources();

        AXIS_SPEED_DEFAULT= r.getInteger(R.integer.axis_speed_default);
        AXIS_SPEED_MIN= r.getInteger(R.integer.axis_speed_min);
        AXIS_SPEED_MAX= r.getInteger(R.integer.axis_speed_max);
        ACCELERATION_DEFAULT= r.getInteger(R.integer.acceleration_default);
        ACCELERATION_MIN= r.getInteger(R.integer.acceleration_min);
        ACCELERATION_MAX= r.getInteger(R.integer.acceleration_max);
        MOTION_SMOOTHING_DEFAULT= r.getInteger(R.integer.motion_smoothing_default);
        MOTION_SMOOTHING_MIN= r.getInteger(R.integer.motion_smoothing_min);
        MOTION_SMOOTHING_MAX= r.getInteger(R.integer.motion_smoothing_max);
        MOTION_THRESHOLD_DEFAULT= r.getInteger(R.integer.motion_threshold_default);
        MOTION_THRESHOLD_MIN= r.getInteger(R.integer.motion_threshold_min);
        MOTION_THRESHOLD_MAX= r.getInteger(R.integer.motion_threshold_max);
        SOUND_ON_CLICK_DEFAULT= r.getBoolean(R.bool.sound_on_click_default);

        sRuntimeConstantsLoaded= true;
    }

    private static int constraint (int v, int min, int max) {
        if (v< min) return min;
        if (v> max) return max;
        return v;
    }

    public static int getHorizontalSpeed(Context c) {
        loadRuntimeConstants(c);
        int v= getSharedPreferences(c).getInt(Preferences.KEY_HORIZONTAL_SPEED, AXIS_SPEED_DEFAULT);
        return constraint (v, AXIS_SPEED_MIN, AXIS_SPEED_MAX);
    }

    public static int setHorizontalSpeed(Context c, int v) {
        loadRuntimeConstants(c);
        v= constraint (v, AXIS_SPEED_MIN, AXIS_SPEED_MAX);

        SharedPreferences.Editor spe= getSharedPreferences(c).edit();
        spe.putInt(Preferences.KEY_HORIZONTAL_SPEED, v);
        spe.apply();

        return v;
    }

    public static int getVerticalSpeed(Context c) {
        loadRuntimeConstants(c);
        int v= getSharedPreferences(c).getInt(Preferences.KEY_VERTICAL_SPEED, AXIS_SPEED_DEFAULT);
        return constraint(v, AXIS_SPEED_MIN, AXIS_SPEED_MAX);
    }

    public static int setVerticalSpeed(Context c, int v) {
        loadRuntimeConstants(c);
        v= constraint (v, AXIS_SPEED_MIN, AXIS_SPEED_MAX);

        SharedPreferences.Editor spe= getSharedPreferences(c).edit();
        spe.putInt(Preferences.KEY_VERTICAL_SPEED, v);
        spe.apply();

        return v;
    }

    public static int getAcceleration(Context c) {
        loadRuntimeConstants(c);
        int v= getSharedPreferences(c).getInt(Preferences.KEY_ACCELERATION, ACCELERATION_DEFAULT);
        return constraint (v, ACCELERATION_MIN, ACCELERATION_MAX);
    }

    public static int getMotionSmoothing(Context c) {
        loadRuntimeConstants(c);
        int v= getSharedPreferences(c).getInt(
                Preferences.KEY_MOTION_SMOOTHING, MOTION_SMOOTHING_DEFAULT);
        return constraint (v, MOTION_SMOOTHING_MIN, MOTION_SMOOTHING_MAX);
    }

    public static int getMotionThreshold(Context c) {
        loadRuntimeConstants(c);
        int v= getSharedPreferences(c).getInt(
                Preferences.KEY_MOTION_THRESHOLD, MOTION_THRESHOLD_DEFAULT);
        return constraint (v, MOTION_THRESHOLD_MIN, MOTION_THRESHOLD_MAX);
    }

    public static boolean getSoundOnClick(Context c) {
        loadRuntimeConstants(c);
        return getSharedPreferences(c).getBoolean(
                Preferences.KEY_SOUND_ON_CLICK, SOUND_ON_CLICK_DEFAULT);
    }

    public static float getUIElementsSize(SharedPreferences sp) {
        return Float.parseFloat(sp.getString(KEY_UI_ELEMENTS_SIZE, null));
    }

    public static int getTimeWithoutDetection(SharedPreferences sp) {
        return Integer.parseInt(sp.getString(KEY_TIME_WITHOUT_DETECTION, null));
    }

    public static String getTimeWithoutDetectionEntryValue(Context c) {
        // current value
        int value= getTimeWithoutDetection(getSharedPreferences(c));

        // search value in array entries
        Resources res= c.getResources();
        String[] entries= res.getStringArray(R.array.time_without_detection_values);
        int pos;
        for (pos= 0; pos< entries.length; pos++) {
            if (entries[pos].contentEquals(String.valueOf(value))) break;
        }

        /*
         * if found, pick the entry value. inside a try/catch block in case
         * there is a size mismatch between entries and values arrays
         */
        try {
            if (pos< entries.length) {
                String[] values= res.getStringArray(R.array.time_without_detection_entries);
                return values[pos];
            }
        }
        catch (Exception e) { /* do nothing */ }

        // fallback path, should never happen
        return String.valueOf(value);
    }

    public static boolean getMouseCalibrationPerformed(Context c) {
        return getSharedPreferences(c).getBoolean(KEY_MOUSE_CALIBRATION_PERFORMED, false);
    }

    public static void setMouseCalibrationPerformed(Context c, boolean value) {
        SharedPreferences.Editor spe= getSharedPreferences(c).edit();
        spe.putBoolean(KEY_MOUSE_CALIBRATION_PERFORMED, value);
        spe.apply();
    }

    /**
     * Gamepad locations
     */
    public static final int LOCATION_GAMEPAD_TOP_LEFT= 0;
    public static final int LOCATION_GAMEPAD_BOTTOM_LEFT= 1;
    public static final int LOCATION_GAMEPAD_TOP_CENTER= 2;
    public static final int LOCATION_GAMEPAD_BOTTOM_CENTER= 3;
    public static final int LOCATION_GAMEPAD_TOP_RIGHT= 4;
    public static final int LOCATION_GAMEPAD_BOTTOM_RIGHT= 5;

    public static int getGamepadLocation(SharedPreferences sp) {
        return Integer.parseInt(sp.getString(KEY_GAMEPAD_LOCATION, null));
    }

    public static int getGamepadTransparency(SharedPreferences sp) {
        return Integer.parseInt(sp.getString(KEY_GAMEPAD_TRANSPARENCY, "100"));
    }

    public static int getGamepadAbsSpeed(SharedPreferences sp) {
        return sp.getInt(KEY_GAMEPAD_ABS_SPEED, 0);
    }

    public static int getGamepadRelSensitivity(SharedPreferences sp) {
        return sp.getInt(KEY_GAMEPAD_REL_SENSITIVITY, 0);
    }
 }
 