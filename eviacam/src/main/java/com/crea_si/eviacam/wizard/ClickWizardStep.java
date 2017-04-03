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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.a11yservice.AccessibilityServiceModeEngine;

import org.codepond.wizardroid.WizardStep;

public class ClickWizardStep extends WizardStep {
    private boolean mClickDone= false;
    private boolean mLongClickDone= false;

    // You must have an empty constructor for every step
    public ClickWizardStep() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.wizard_step_click, container, false);

        final Button b= (Button) v.findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                b.setText(R.string.action_click);
                mClickDone= true;

                if (mLongClickDone) notifyCompleted();
            }
        });

        b.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                b.setText(R.string.action_long_click);
                mLongClickDone= true;
                if (mClickDone) notifyCompleted();
                return true;
            }
        });

        return v;
    }

    @Override
    public void onEnter() {
        AccessibilityServiceModeEngine engine =
                WizardUtils.checkEngineAndFinishIfNeeded(getActivity());
        if (engine!= null) {
            engine.enableClick();
            engine.disableDockPanel();
            engine.enablePointer();
            engine.disableScrollButtons();
            engine.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        WizardUtils.checkEngineAndFinishIfNeeded(getActivity());
    }
}
