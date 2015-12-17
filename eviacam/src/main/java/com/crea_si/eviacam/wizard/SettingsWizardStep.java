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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.service.AccessibilityServiceModeEngine;
import com.crea_si.eviacam.service.MainEngine;
import com.crea_si.eviacam.service.SplashActivity;

import org.codepond.wizardroid.WizardStep;

public class SettingsWizardStep extends WizardStep {

    // You must have an empty constructor for every step
    public SettingsWizardStep() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wizard_step_settings, container, false);
        return v;
    }

    @Override
    public void onExit(int exitCode) {
        AccessibilityServiceModeEngine engine =
                MainEngine.getInstance().getAccessibilityServiceModeEngine();
        if (exitCode== WizardStep.EXIT_PREVIOUS) {
            engine.enablePointer();
        }
        else {
            //
        }
    }
}
