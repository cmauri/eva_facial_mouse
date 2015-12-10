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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.crea_si.eviacam.R;

public class OverlayUtils {

    /**
     * Inflate a view given its id
     * @param c
     * @param id
     *
     * @return the new view
     *
     * Remarks: special version which "forces" the AppTheme
     */
    public static View inflate(Context c, int id, ViewGroup root) {
        /**
         * Quite weird but it seems that from a service, the inflater does not
         * take the theme from the context, it uses the default one [1].
         *
         * Setting the theme to the application context and using it to
         * get the inflater seems to work partially (the background color
         * is still fixed).
         *
         * [1] https://possiblemobile.com/2013/06/context/
         * [2] http://stackoverflow.com/questions/2118251/theme-style-is-not-applied-when-inflater-used-with-applicationcontext/2119625#2119625
         */
        final Context appContext= c.getApplicationContext();
        appContext.setTheme(R.style.AppTheme);
        LayoutInflater inflater = LayoutInflater.from(appContext);
        return inflater.inflate(id, root);
    }

    /**
     * Display a fullscreen view from a service
     *
     * @param v
     */

    /**
     * Create a fullscreen window and add a view that can be interacted
     *
     * @param v
     */
    public static void addInteractiveView (View v) {
        /**
         * Layout parameters
         */
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.setTitle("Touchable window");
        //layoutParams.format = PixelFormat.TRANSLUCENT; // Transparent background

        /**
         * Type of window. Create an always on top window.
         *
         * TYPE_PHONE: These are non-application windows providing user interaction with the
         *      phone (in particular incoming calls). These windows are normally placed above
         *      all applications, but behind the status bar. In multiuser systems shows on all
         *      users' windows.
         *
         * TYPE_SYSTEM_ERROR: appear on top of everything they can.
         */
        layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
                            //WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

        /**
         * Type of window. Whole screen is covered (including status bar)
         *
         * FLAG_NOT_FOCUSABLE: this window won't ever get key input focus, so the user can not
         *      send key or other button events to it. It can use the full screen for its content
         *      and cover the input method if needed
         *
         * FLAG_LAYOUT_IN_SCREEN: place the window within the entire screen, ignoring decorations
         *      around the border (such as the status bar)
         *
         */
        layoutParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        WindowManager wm= (WindowManager) v.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.addView(v, layoutParams);
    }

    /**
     * Remove view from the window manager
     *
     * @param v
     */
    public static void removeView (View v) {
        if (v== null) return;
        WindowManager wm=
                (WindowManager) v.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.removeViewImmediate(v);
    }
}
