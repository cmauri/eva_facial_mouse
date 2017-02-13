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
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.service.AccessibilityServiceModeEngine;

import org.codepond.wizardroid.WizardStep;

public class PositioningWizardStep extends WizardStep implements Runnable {
    private TextView mTextViewDetection;
    private final Handler mHandler = new Handler();

    // You must have an empty constructor for every step
    public PositioningWizardStep() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v= inflater.inflate(R.layout.wizard_step_positioning, container, false);

        mTextViewDetection= (TextView) v.findViewById(R.id.textViewDetectionStatus);

        new Thread(this).start();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        WizardUtils.checkEngineAndFinishIfNeeded(getActivity());
    }

    @Override
    public void onEnter() {
        AccessibilityServiceModeEngine engine =
                WizardUtils.checkEngineAndFinishIfNeeded(getActivity());
        if (engine!= null) {
            engine.disableClick();
            engine.disableDockPanel();
            engine.disablePointer();
            engine.disableScrollButtons();
            engine.start();
        }
    }

    @Override
    public void run() {
        final long faceMaxElapsedTime = 1000;
        final int timeToBlink = 400;    // in milliseconds
        AccessibilityServiceModeEngine engine =
                WizardUtils.checkEngineAndFinishIfNeeded(getActivity());

        if (engine== null) return;

        while (engine.getFaceDetectionElapsedTime() == 0 ||
               engine.getFaceDetectionElapsedTime() > faceMaxElapsedTime) {
            try {
                Thread.sleep(timeToBlink);
            } catch (Exception e) { /* nothing to do */ }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mTextViewDetection.getVisibility() == View.VISIBLE) {
                        mTextViewDetection.setVisibility(View.INVISIBLE);
                    } else {
                        mTextViewDetection.setVisibility(View.VISIBLE);
                    }
                }
            });
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextViewDetection.setText(R.string.face_detected);
                mTextViewDetection.setVisibility(View.VISIBLE);
                notifyCompleted();
            }
        });
    }
}
