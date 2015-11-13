package com.crea_si.eviacam.wizard;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.crea_si.eviacam.Eula;
import com.crea_si.eviacam.R;

public class WizardActivity extends FragmentActivity implements Eula.Listener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wizard_activity);
        Eula.acceptEula(this, this);
    }

    @Override
    public void onAcceptEula() {
        // do nothing
    }

    @Override
    public void onCancelEula() {
        finish();
    }
}
