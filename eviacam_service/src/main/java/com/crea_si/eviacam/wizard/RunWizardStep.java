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
package com.crea_si.eviacam.wizard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.crea_si.eviacam.service.Preferences;
import com.crea_si.eviacam.service.R;

import org.codepond.wizardroid.WizardStep;

public class RunWizardStep extends WizardStep {

    // You must have an empty constructor for every step
    public RunWizardStep() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v=  inflater.inflate(R.layout.wizard_step_run, container, false);

        /*
         * Check box
         */
        final CheckBox checkBox= (CheckBox) v.findViewById(R.id.runAtStartupCheckBox);
        final SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(getActivity());
        checkBox.setChecked(Preferences.getRunWizardAtStartup(sp));
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Preferences.getRunWizardAtStartup(sp, checkBox.isChecked());
            }
        });

        return v;
    }
}
