package com.crea_si.eviacam.service;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceChangeListener;

public class SlaveModePreferencesActivity extends Activity {
    
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
    
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Set preference file
            getPreferenceManager().setSharedPreferencesName(Settings.FILE_SLAVE_MODE);
            
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.gamepad_preference_fragment);
                        
            /**
             * Listeners for list preference entries
             */
            ListPreference lp = (ListPreference) findPreference(Settings.KEY_GAMEPAD_LOCATION);
            lp.setOnPreferenceChangeListener(new ListPreferenceUpdate(lp));
            
            lp = (ListPreference) findPreference(Settings.KEY_GAMEPAD_TRANSPARENCY);
            lp.setOnPreferenceChangeListener(new ListPreferenceUpdate(lp));
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
