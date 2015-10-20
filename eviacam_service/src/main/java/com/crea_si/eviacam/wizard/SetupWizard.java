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
import android.content.res.Resources;

import com.crea_si.eviacam.service.R;

import org.codepond.wizardroid.WizardFlow;
import org.codepond.wizardroid.layouts.BasicWizardLayout;

public class SetupWizard extends BasicWizardLayout {

    /**
     * Note that initially BasicWizardLayout inherits from
     * {@link android.support.v4.app.Fragment}
     * and therefore you must have an empty constructor
     */
    public SetupWizard() {
        super();
    }

    //You must override this method and create a wizard flow by
    //using WizardFlow.Builder as shown in this example
    @Override
    public WizardFlow onSetup() {
        /* Labels of the buttons */
        Resources r= getResources();
        setNextButtonText(r.getString(R.string.next));
        setBackButtonText(r.getString(R.string.back));
        setFinishButtonText(r.getString(R.string.finish));

        /* Add your steps in the order you want them to appear and eventually
         * call create() to create the wizard flow.
         */
        return new WizardFlow.Builder()
                .addStep(WelcomeWizardStep.class)
                .addStep(WhatsIsWizardStep.class)
                .addStep(KeyboardWizardStep.class)
                .addStep(CameraViewerWizardStep.class)
                .addStep(DockMenuWizardStep.class)
                .addStep(ScrollButtonsWizardStep.class)
                .addStep(SettingsWizardStep.class)
                .addStep(LimitationsWizardStep.class)
                .addStep(RunWizardStep.class)
                .create();
    }

    @Override
    public void onWizardComplete() {
        super.onWizardComplete();
        Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, 0);
        getActivity().finish();
    }
}
