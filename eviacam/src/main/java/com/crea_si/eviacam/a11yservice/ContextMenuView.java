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
package com.crea_si.eviacam.a11yservice;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.util.ViewUtils;

/**
 * Buttons view for the pointer contextual menu
 */
class ContextMenuView extends LinearLayout
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    private class ActionButton {
        final int action;
        final Button button;
        ActionButton (int a, Button b) { 
            action= a;
            button= b;
        }
    }
    
    // track last used mask to avoid unnecessary operations
    private int mActionsMask= 0;
    
    // references to actions and buttons pairs
    private List<ActionButton> mActionButtons= new ArrayList<>();

    // Buttons scale factor
    private float mScale = 1.0f;
    
    ContextMenuView(@NonNull Context c) {
        super(c);
        setOrientation(LinearLayout.VERTICAL);

        // shared preferences
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
        updateSettings();
    }

    void cleanup() {
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updateSettings() {
        // get values from shared resources
        mScale = Preferences.get().getUIElementsSize();

        /*
         * scale view according to size selected by the user
         */
        setScaleX(mScale);
        setScaleY(mScale);
        setPivotX(0.0f);
        setPivotY(0.0f);

        requestLayout();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(Preferences.KEY_UI_ELEMENTS_SIZE)) {
            updateSettings();
        }
    }
    
    /*
     * create button, make invisible (gone), add to layout and store in 
     * collection for further reference 
     */
    void populateAction (int action, int labelId) {
        Button b= new Button(getContext());
        b.setText(getResources().getString(labelId));
        b.setVisibility(View.GONE);

        addView(b);

        mActionButtons.add(new ActionButton(action, b));
    }
    
    void updateButtons (int actions) {
        if (actions == mActionsMask) return;
        
        for (ActionButton ab : mActionButtons) {
            if ((actions & ab.action) != 0) {
                ab.button.setVisibility(View.VISIBLE);
            }
            else {
                ab.button.setVisibility(View.GONE);                
            }
        }

        // measure how much space will need
        measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        mActionsMask= actions;
    }

    int testClick (@NonNull Point p)  {
        if (!ViewUtils.isPointInsideView(p, this, mScale)) return 0;

        for (ActionButton ab : mActionButtons) {
            if (ab.button.getVisibility() == View.VISIBLE &&
                ViewUtils.isPointInsideView(p, ab.button, mScale)) {
                return ab.action;
            }
        }
       
        return 0;
    }

    int getMeasuredWidthScaled() {
        return (int) (getMeasuredWidth() * mScale);
    }

    int getMeasuredHeightScaled() {
        return (int) (getMeasuredHeight() * mScale);
    }
}
