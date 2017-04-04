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
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.a11yservice.AccessibilityServiceModeEngine;

import org.codepond.wizardroid.WizardStep;

public class PositioningWizardStep extends WizardStep {
    private static final int TIME_TO_BLINK = 400;    // in milliseconds
    private static final long FACE_MAX_ELAPSED_TIME = 1000;

    private TextView mTextViewDetection;
    private final Handler mHandler = new Handler();

    // You must have an empty constructor for every step
    public PositioningWizardStep() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v= inflater.inflate(R.layout.wizard_step_positioning, container, false);

        mTextViewDetection= (TextView) v.findViewById(R.id.textViewDetectionStatus);

        // TODO: start here the text blink because this View is created several times
        mHandler.postDelayed(mRunnable, TIME_TO_BLINK);

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

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            Activity activity= getActivity();

            AccessibilityServiceModeEngine engine =
                    WizardUtils.checkEngineAndFinishIfNeeded(activity);

            // This can be ran after the activity is closed and so activity could be null
            if (activity== null || engine== null) {
                mHandler.removeCallbacks(mRunnable);
                return;
            }

            if (mTextViewDetection.getVisibility() == View.VISIBLE) {
                mTextViewDetection.setVisibility(View.INVISIBLE);
            } else {
                mTextViewDetection.setVisibility(View.VISIBLE);
            }

            if (engine.getFaceDetectionElapsedTime() == 0 ||
                    engine.getFaceDetectionElapsedTime() > FACE_MAX_ELAPSED_TIME) {
                mHandler.postDelayed(this, TIME_TO_BLINK);
            }
            else {
                mTextViewDetection.setText(R.string.wizard_face_detected);
                mTextViewDetection.setVisibility(View.VISIBLE);
                mHandler.removeCallbacks(mRunnable);
                notifyCompleted();
            }
        }
    };
}
