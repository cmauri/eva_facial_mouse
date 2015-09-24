package com.crea_si.eviacam.wizard;

import org.codepond.wizardroid.WizardFlow;
import org.codepond.wizardroid.layouts.BasicWizardLayout;

public class SetupWizard extends BasicWizardLayout {

    /**
     * Note that initially BasicWizardLayout inherits from {@link android.support.v4.app.Fragment}
     * and therefore you must have an empty constructor
     */
    public SetupWizard() {
        super();
    }

    //You must override this method and create a wizard flow by
    //using WizardFlow.Builder as shown in this example
    @Override
    public WizardFlow onSetup() {
        /* Optionally, you can set different labels for the control buttons
        setNextButtonLabel("Advance");
        setBackButtonLabel("Return");
        setFinishButtonLabel("Finalize"); */

        return new WizardFlow.Builder()
                .addStep(com.crea_si.eviacam.wizard.TutorialStep1.class)           //Add your steps in the order you want them
                .addStep(com.crea_si.eviacam.wizard.TutorialStep2.class)           //to appear and eventually call create()
                .create();                              //to create the wizard flow.
    }
}