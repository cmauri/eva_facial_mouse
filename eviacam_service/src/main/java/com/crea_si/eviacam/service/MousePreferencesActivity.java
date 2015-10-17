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

package com.crea_si.eviacam.service;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

/**
 * The main preferences activity. It is also used for the mouse specific
 * settings when working in slave mode.
 */
public class MousePreferencesActivity extends Activity {
    /*
     * Listener for list preferences
     */
    private static class ListPreferenceUpdate implements OnPreferenceChangeListener {

        private final ListPreference mListPreference;

        public ListPreferenceUpdate(ListPreference lp) {
            mListPreference = lp;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // Set the value as the new value
            mListPreference.setValue(newValue.toString());                    
            // Get the entry which corresponds to the current value and set as summary
            preference.setSummary(mListPreference.getEntry());
            return true;
        }
    }
    
    /*
     * The settings fragment
     */
    public static class SettingsFragment extends PreferenceFragment {
        // stored whether the activity has been started in slave mode
        private final boolean mSlaveMode;

        public SettingsFragment () {
            mSlaveMode= false;
        }

        SettingsFragment (boolean slaveMode) { mSlaveMode= slaveMode; }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);

            if (mSlaveMode) {
                // In slave mode use different preference file
                getPreferenceManager().setSharedPreferencesName(Preferences.FILE_SLAVE_MODE);
            }

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference_fragment);

            if (mSlaveMode) {
                /*
                 * Remove preferences not applicable in slave mode
                 */
                PreferenceGroup cat= (PreferenceGroup) getPreferenceScreen().
                        findPreference("interface_settings");
                Preference p= getPreferenceScreen().
                        findPreference(Preferences.KEY_DOCKING_PANEL_EDGE);
                cat.removePreference(p);
            }

            /**
             * Listeners for list preference entries
             */
            ListPreference lp;
            if (!mSlaveMode) {
                lp = (ListPreference) findPreference(Preferences.KEY_DOCKING_PANEL_EDGE);
                lp.setOnPreferenceChangeListener(new ListPreferenceUpdate(lp));
            }

            lp = (ListPreference) findPreference(Preferences.KEY_TIME_WITHOUT_DETECTION);
            lp.setOnPreferenceChangeListener(new ListPreferenceUpdate(lp));

            lp = (ListPreference) findPreference(Preferences.KEY_UI_ELEMENTS_SIZE);
            lp.setOnPreferenceChangeListener(new ListPreferenceUpdate(lp));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Check is started for slave mode
         */
        String value= getIntent().getDataString();
        boolean slaveMode= value!= null && value.compareTo("slave_mode")== 0;
        Bundle extras= getIntent().getExtras();
        if (extras!= null) {
            slaveMode= slaveMode || extras.getBoolean("slave_mode", false);
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment(slaveMode))
                .commit();

        if (slaveMode) {
            setTitle(getResources().getText(R.string.mouse_preferences));
        }
    }
}
