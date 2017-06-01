/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
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

package com.crea_si.eviacam.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.camera.Camera;
import com.crea_si.eviacam.util.NumberPickerPreference;

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

        ListPreferenceUpdate(ListPreference lp) {
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
        // Factory method to pass initial arguments using the bundle
        public static SettingsFragment newInstance (boolean slaveMode) {
            SettingsFragment sf= new SettingsFragment();

            Bundle args= new Bundle();
            args.putBoolean("slaveMode", slaveMode);
            sf.setArguments(args);

            return sf;
        }

        // return whether the fragment has been started in slave mode
        private boolean getSlaveMode() {
            return getArguments().getBoolean("slaveMode");
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);

            final boolean slaveMode= getSlaveMode();

            if (slaveMode) {
                // In slave mode use different preference file
                getPreferenceManager().setSharedPreferencesName(Preferences.FILE_SLAVE_MODE);
            }

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preference_fragment);

            /*
             * Lock speed settings checkbox
             */

            /* Disable vertical speed if needed */
            final NumberPickerPreference vSpeedPreference = (NumberPickerPreference)
                    getPreferenceScreen().findPreference("vertical_speed");
            final NumberPickerPreference hSpeedPreference = (NumberPickerPreference)
                    getPreferenceScreen().findPreference("horizontal_speed");

            CheckBoxPreference lockSpeedPreferences =
                    (CheckBoxPreference) getPreferenceScreen().findPreference("lock_speeds");
            final boolean lockSpeeds = lockSpeedPreferences.isChecked();
            if (lockSpeeds) {
                vSpeedPreference.setValue(hSpeedPreference.getValue());
                vSpeedPreference.setEnabled(false);
            }

            lockSpeedPreferences.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if ((Boolean) newValue) {
                        // Lock speed preferences
                        vSpeedPreference.setValue(hSpeedPreference.getValue());
                        vSpeedPreference.setEnabled(false);
                    } else {
                        vSpeedPreference.setEnabled(true);
                    }
                    return true;
                }
            });

            hSpeedPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (lockSpeeds) {
                        vSpeedPreference.setValue((int) newValue);
                    }
                    return true;
                }
            });

            /*
             * Remove camera2 related preferences if not supported by the device
             */
            if (!Camera.hasCamera2Support()) {
                PreferenceCategory advanced = (PreferenceCategory) findPreference("advanced");
                if (null != advanced) {
                    getPreferenceScreen().removePreference(advanced);
                }
            }
            else {
                ListPreference lp = (ListPreference) findPreference(Preferences.KEY_USE_CAMERA2_API);
                lp.setOnPreferenceChangeListener(new ListPreferenceUpdate(lp));
            }

            /*
             * Remove preferences not applicable in slave mode
            */
            if (slaveMode) {
                PreferenceGroup cat = (PreferenceGroup) getPreferenceScreen().
                        findPreference("interface_settings");
                Preference p = getPreferenceScreen().
                        findPreference(Preferences.KEY_DOCKING_PANEL_EDGE);
                cat.removePreference(p);
            }
            else {
                ListPreference lp = (ListPreference) findPreference(Preferences.KEY_DOCKING_PANEL_EDGE);
                lp.setOnPreferenceChangeListener(new ListPreferenceUpdate(lp));
            }

            /*
             * Wizard button
             */
            Preference wizPreference = getPreferenceScreen().findPreference("wizard");
            wizPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // If cannot init preferences just ignore silently
                    if (Preferences.initForA11yService(getActivity()) == null) return true;
                    Preferences.get().setRunTutorial(true);
                    Preferences.get().cleanup();

                    new AlertDialog.Builder(getActivity())
                            .setMessage(R.string.settings_wizard_will_run)
                            .setPositiveButton(android.R.string.ok, null)
                            .create().show();
                    return true;
                }
            });

            /*
             * Version button
             */
            Preference p = getPreferenceScreen().findPreference("version");
            p.setSummary(getResources().getText(R.string.app_name) +
                    " " + BuildConfig.VERSION_NAME);
            p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                int clickCount;

                @Override
                public boolean onPreferenceClick(Preference preference) {
                    clickCount++;
                    if (clickCount >= 4) {
                        clickCount = 0;
                        Intent i = new Intent(getActivity(), TechInfoActivity.class);
                        startActivity(i);
                    }
                    return true;
                }
            });

            /*
             * Listeners for list preference entries
             */
            ListPreference lp = (ListPreference) findPreference(Preferences.KEY_TIME_WITHOUT_DETECTION);
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
                .replace(android.R.id.content, SettingsFragment.newInstance(slaveMode))
                .commit();

        if (slaveMode) {
            setTitle(getResources().getText(R.string.slave_settings_mouse_preferences));
        }
    }
}
