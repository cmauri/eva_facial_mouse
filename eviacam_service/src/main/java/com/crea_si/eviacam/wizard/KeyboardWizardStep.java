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
import android.widget.ImageView;

import com.crea_si.eviacam.service.InputMethodAction;
import com.crea_si.eviacam.service.R;

import org.codepond.wizardroid.WizardStep;

public class KeyboardWizardStep extends WizardStep {

    // You must have an empty constructor for every step
    public KeyboardWizardStep() {
    }

    private void checkUpdate (View v) {
        ImageView iv = (ImageView) v.findViewById(R.id.keyboardImage);
        if (InputMethodAction.isEnabledCustomKeyboard(getActivity())) {
            iv.setImageResource(R.drawable.ic_correct);
        }
        else {
            iv.setImageResource(R.drawable.ic_wrong);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.wizard_step_keyboard, container, false);

        checkUpdate(v);
        Button b= (Button) v.findViewById(R.id.keyboardConfigureButton);
        b.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) { checkUpdate(v); }
        });
        return v;
    }
}
