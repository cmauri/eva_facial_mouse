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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.View;

import com.crea_si.eviacam.common.InputMethodAction;
import com.crea_si.eviacam.R;

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
        setNextButtonText(r.getString(R.string.wizard_next));
        setBackButtonText(r.getString(R.string.wizard_back));
        setFinishButtonText(r.getString(R.string.wizard_finish));

        /* Label of the activity */
        getActivity().setTitle(r.getString(R.string.app_name));

        /* Add your steps in the order you want them to appear and eventually
         * call create() to create the wizard flow.
         */
        return new WizardFlow.Builder()
                .addStep(WelcomeWizardStep.class)
                .addStep(WhatsIsWizardStep.class)
                .addStep(KeyboardWizardStep.class)
                .addStep(PositioningWizardStep.class, true)
                .addStep(SpeedSettingsWizardStep.class)
                .addStep(SettingsWizardStep.class)
                .addStep(PreClickWizardStep.class)
                .addStep(ClickWizardStep.class)
                .addStep(PreScrollButtonsWizardStep.class)
                .addStep(ScrollButtonsWizardStep.class)
                .addStep(LimitationsWizardStep.class)
                .addStep(DockMenuWizardStep.class)
                .addStep(NotificationIconWizardStep.class)
                .addStep(FinalWizardStep.class)
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

    @Override
    public void onClick(final View v) {
        if (wizard.getCurrentStepPosition() == 2 &&
                v.getId() == org.codepond.wizardroid.R.id.wizard_next_button) {

            boolean result= showKeyboardWarnDialog(getActivity(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    SetupWizard.super.onClick(v);
                }
            }, null);
            if (!result) return;
        }

        super.onClick(v);
    }

    private int mStepBefore= -1;
    @Override
    public void onStepChanged() {
        super.onStepChanged();
        int stepCurrent= wizard.getCurrentStepPosition();
        if (mStepBefore == 2 && stepCurrent== 3) {
            showKeyboardWarnDialog(getActivity(), null, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    wizard.goBack();
                }
            });
        }
        mStepBefore= stepCurrent;
    }

    private boolean showKeyboardWarnDialog (@NonNull Activity activity,
                                            DialogInterface.OnClickListener listenerPos,
                                            DialogInterface.OnClickListener listenerNeg) {
        if (InputMethodAction.isCustomKeyboardSelected(activity)) return true;

        mStepBefore= -1;

        DialogInterface.OnClickListener dummyListener= new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // do nothing
            }
        };

        if (listenerPos== null) listenerPos= dummyListener;
        if (listenerNeg== null) listenerNeg= dummyListener;

        Resources r= getResources();

        new AlertDialog.Builder(activity)
            .setTitle(r.getText(R.string.wizard_keyboard_not_configured))
            .setMessage(r.getText(R.string.wizard_keyboard_not_configured_confirm))
            .setPositiveButton(android.R.string.yes, listenerPos)
            .setNegativeButton(android.R.string.no, listenerNeg)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();

        return false;
    }
}
