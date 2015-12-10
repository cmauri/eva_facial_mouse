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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.crea_si.eviacam.Preferences;
import com.crea_si.eviacam.R;

public class OverlayFocusableView {

    private View mView;

    OverlayFocusableView(final Context c) {

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
        mView = inflater.inflate(R.layout.speed_settings_layout, null);

        /**
         * DONE button
         */
        Button doneButton= (Button) mView.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cleanup();
            }
        });

        /**
         * Horizontal speed setting
         */
        final Button hSpeedMinus= (Button) mView.findViewById(R.id.hspeed_minus_button);
        final Button hSpeedPlus= (Button) mView.findViewById(R.id.hspeed_plus_button);
        final TextView hSpeedTextView= (TextView) mView.findViewById(R.id.hspeed_textView);

        hSpeedTextView.setText(Integer.toString(Preferences.getHorizontalSpeed(c)));
        hSpeedMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val = Preferences.getHorizontalSpeed(c);
                val = Preferences.setHorizontalSpeed(c, --val);
                hSpeedTextView.setText(Integer.toString(val));
            }
        });

        hSpeedPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val= Preferences.getHorizontalSpeed(c);
                val= Preferences.setHorizontalSpeed(c, ++val);
                hSpeedTextView.setText(Integer.toString(val));
            }
        });

        /**
         * Vertical speed setting
         */
        final Button vSpeedMinus= (Button) mView.findViewById(R.id.vspeed_minus_button);
        final Button vSpeedPlus= (Button) mView.findViewById(R.id.vspeed_plus_button);
        final TextView vSpeedTextView= (TextView) mView.findViewById(R.id.vspeed_textView);

        // Current value
        vSpeedTextView.setText(Integer.toString(Preferences.getVerticalSpeed(c)));

        vSpeedMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val = Preferences.getVerticalSpeed(c);
                val = Preferences.setVerticalSpeed(c, --val);
                vSpeedTextView.setText(Integer.toString(val));
            }
        });

        vSpeedPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val = Preferences.getVerticalSpeed(c);
                val = Preferences.setVerticalSpeed(c, ++val);
                vSpeedTextView.setText(Integer.toString(val));
            }
        });


        /**
         * Layout parameters
         */
        WindowManager.LayoutParams feedbackParams = new WindowManager.LayoutParams();
        feedbackParams.setTitle("FeedbackFocusableOverlay");
        //feedbackParams.format = PixelFormat.TRANSLUCENT; // Transparent background

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
        feedbackParams.type = WindowManager.LayoutParams.TYPE_PHONE;
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
        feedbackParams.flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        feedbackParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        feedbackParams.height = WindowManager.LayoutParams.MATCH_PARENT;

        WindowManager wm= (WindowManager) appContext.getSystemService(Context.WINDOW_SERVICE);
        wm.addView(mView, feedbackParams);
    }
   
    void cleanup() {
        if (mView== null) return;
        WindowManager wm=
                (WindowManager) mView.getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.removeViewImmediate(mView);
        mView= null;
    }
}
