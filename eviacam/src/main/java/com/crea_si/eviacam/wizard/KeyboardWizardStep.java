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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.crea_si.eviacam.a11yservice.AccessibilityServiceModeEngine;
import com.crea_si.eviacam.common.InputMethodAction;
import com.crea_si.eviacam.R;

import org.codepond.wizardroid.WizardStep;

public class KeyboardWizardStep extends WizardStep {

    // You must have an empty constructor for every step
    public KeyboardWizardStep() {
    }

    private void checkUpdate (View v) {
        ImageView iv = (ImageView) v.findViewById(R.id.keyboardImage);
        Button b= (Button) v.findViewById(R.id.keyboardConfigureButton);
        if (InputMethodAction.isCustomKeyboardSelected(getActivity())) {
            iv.setImageResource(R.drawable.ic_correct);
            b.setEnabled(false);
        }
        else {
            iv.setImageResource(R.drawable.ic_wrong);
            b.setEnabled(true);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.wizard_step_keyboard, container, false);

        /* Different instruction for Lollipop */
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP) {
            TextView tv = (TextView) v.findViewById(R.id.wiz_set_keyboard);
            tv.setText(getResources().getText(R.string.wizard_set_keyboard_lollipop));
        }

        checkUpdate(v);
        Button b= (Button) v.findViewById(R.id.keyboardConfigureButton);
        b.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                startActivityForResult(intent, 0);
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkUpdate(getActivity().findViewById(android.R.id.content));
        WizardUtils.checkEngineAndFinishIfNeeded(getActivity());
    }

    @Override
    public void onEnter() {
        AccessibilityServiceModeEngine engine =
                WizardUtils.checkEngineAndFinishIfNeeded(getActivity());
        if (engine!= null) {
            engine.stop();
        }
    }
}
