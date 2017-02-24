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
import android.support.v4.app.FragmentActivity;

import com.crea_si.eviacam.common.Eula;
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
