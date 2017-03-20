/*
* Enable Viacam for Android, a camera based mouse emulator
*
* Copyright (getActivity()) 2015 Cesar Mauri Loba (CREA Software Systems)
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
package com.crea_si.eviacam.wizard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.a11yservice.AccessibilityServiceModeEngine;

import org.codepond.wizardroid.WizardStep;

public class SpeedSettingsWizardStep extends WizardStep {

    // You must have an empty constructor for every step
    public SpeedSettingsWizardStep() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wizard_step_speed_settings, container, false);

        /*
          Horizontal speed setting
         */
        final Button hSpeedMinus= (Button) v.findViewById(R.id.hspeed_minus_button);
        final Button hSpeedPlus= (Button) v.findViewById(R.id.hspeed_plus_button);
        final TextView hSpeedTextView= (TextView) v.findViewById(R.id.hspeed_textView);

        // Current value
        hSpeedTextView.setText(Integer.toString(Preferences.get().getHorizontalSpeed()));

        hSpeedMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val = Preferences.get().getHorizontalSpeed();
                val = Preferences.get().setHorizontalSpeed(--val);
                hSpeedTextView.setText(Integer.toString(val));
            }
        });

        hSpeedPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val= Preferences.get().getHorizontalSpeed();
                val= Preferences.get().setHorizontalSpeed(++val);
                hSpeedTextView.setText(Integer.toString(val));
            }
        });

        /*
          Vertical speed setting
         */
        final Button vSpeedMinus= (Button) v.findViewById(R.id.vspeed_minus_button);
        final Button vSpeedPlus= (Button) v.findViewById(R.id.vspeed_plus_button);
        final TextView vSpeedTextView= (TextView) v.findViewById(R.id.vspeed_textView);

        // Current value
        vSpeedTextView.setText(Integer.toString(Preferences.get().getVerticalSpeed()));

        vSpeedMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val = Preferences.get().getVerticalSpeed();
                val = Preferences.get().setVerticalSpeed(--val);
                vSpeedTextView.setText(Integer.toString(val));
            }
        });

        vSpeedPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int val = Preferences.get().getVerticalSpeed();
                val = Preferences.get().setVerticalSpeed(++val);
                vSpeedTextView.setText(Integer.toString(val));
            }
        });
        
        return v;
    }

    @Override
    public void onEnter() {
        AccessibilityServiceModeEngine engine =
                WizardUtils.checkEngineAndFinishIfNeeded(getActivity());
        if (engine!= null) {
            engine.disableClick();
            engine.disableDockPanel();
            engine.enablePointer();
            engine.disableScrollButtons();
        }
    }
}
