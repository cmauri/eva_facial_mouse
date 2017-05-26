/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Modified by Cesar Mauri (CREA) 2015 for the Enable Viacam for Android project
 */
package com.crea_si.softkeyboard;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.android.inputmethodcommon.InputMethodSettingsFragment;

/**
 * Displays the IME preferences inside the input method setting.
 */
public class ImePreferences extends PreferenceActivity {
    @Override
    public Intent getIntent() {
        final Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, Settings.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We overwrite the title of the activity, as the default one is "Voice Search".
        setTitle(R.string.kbd_settings_name);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return ImePreferences.Settings.class.getName().equals(fragmentName);
    }

    public static class Settings extends InputMethodSettingsFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setInputMethodSettingsCategoryTitle(R.string.kbd_language_selection_title);
            setSubtypeEnablerTitle(R.string.kbd_select_language);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.ime_preferences);
        }
    }
}
