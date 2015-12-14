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
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crea_si.eviacam.Preferences;
import com.crea_si.eviacam.R;

/**
 * View to set up the pointer speed
 */
class SpeedSettingsView extends LinearLayout {

    public interface OnDoneListener {
        void onDone();
    }

    private OnDoneListener mOnDoneListener;

    SpeedSettingsView(final Context c) {
        super(c);

        View v= OverlayUtils.inflate(c, R.layout.speed_settings_layout, this);
        
        /**
         * DONE button
         */

        Button doneButton= (Button) v.findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mOnDoneListener != null) {
                    mOnDoneListener.onDone();
                }
            }
        });

        /**
         * Horizontal speed setting
         */
        final Button hSpeedMinus= (Button) v.findViewById(R.id.hspeed_minus_button);
        final Button hSpeedPlus= (Button) v.findViewById(R.id.hspeed_plus_button);
        final TextView hSpeedTextView= (TextView) v.findViewById(R.id.hspeed_textView);

        // Current value
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
        final Button vSpeedMinus= (Button) v.findViewById(R.id.vspeed_minus_button);
        final Button vSpeedPlus= (Button) v.findViewById(R.id.vspeed_plus_button);
        final TextView vSpeedTextView= (TextView) v.findViewById(R.id.vspeed_textView);

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
    }

    void setOnDoneListener(OnDoneListener listener) {
        mOnDoneListener= listener;
    }
}
