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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.R;

import org.codepond.wizardroid.WizardStep;

public class WelcomeWizardStep extends WizardStep {
    // You must have an empty constructor for every step
    public WelcomeWizardStep() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v= inflater.inflate(R.layout.wizard_step_welcome, container, false);

        final Resources res= getResources();
        final CheckBox skipWizardCheckBox= (CheckBox) v.findViewById(R.id.checkBoxSkipWizard);

        skipWizardCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog ad = new AlertDialog.Builder(getActivity()).create();
                ad.setCancelable(false); // This blocks the 'BACK' button
                ad.setMessage(res.getText(R.string.wizard_close_wizard_question));
                ad.setButton(
                    DialogInterface.BUTTON_POSITIVE, res.getText(android.R.string.yes),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            WizardUtils.fullStartEngine(getContext());
                            Preferences.get().setRunTutorial(false);
                            WizardUtils.finishWizard(getActivity());
                        }
                    });
                ad.setButton(
                    DialogInterface.BUTTON_NEGATIVE, res.getText(android.R.string.no),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            skipWizardCheckBox.setChecked(false);
                        }
                    });
                ad.show();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        WizardUtils.checkEngineAndFinishIfNeeded(getActivity());
    }
}
