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
 
public class Settings {
     public static final String KEY_X_AXIS_SPEED= "x_axis_speed";
     public static final String KEY_Y_AXIS_SPEED= "y_axis_speed";
     public static final String KEY_ACCELERATION= "acceleration";
     public static final String KEY_MOTION_SMOOTHING= "motion_smoothing";
     public static final String KEY_MOTION_THRESHOLD= "motion_threshold";
     public static final String KEY_DWELL_TIME= "dwell_time";
     public static final String KEY_DWELL_AREA= "dwell_area";
     public static final String KEY_SOUND_ON_CLICK= "sound_on_click";
     public static final String KEY_CONSECUTIVE_CLIKCS= "consecutive_clicks";
     public static final String KEY_DOCKING_PANEL_EDGE= "docking_panel_edge";
     public static final String KEY_UI_ELEMENTS_SIZE= "ui_elements_size";
     public static final String KEY_TIME_WITHOUT_DETECTION= "time_without_detection";
     
     public static float getUIElementsSize(SharedPreferences sp) {
         return Float.parseFloat(sp.getString(Settings.KEY_UI_ELEMENTS_SIZE, null));
     }
     
     public static SharedPreferences getSharedPreferences(Context c) {
         return ((EViacamApplication) c.getApplicationContext()).getSharedPreferences();
     }
 }
 