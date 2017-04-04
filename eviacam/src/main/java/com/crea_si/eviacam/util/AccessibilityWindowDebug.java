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
package com.crea_si.eviacam.util;

import java.util.List;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import com.crea_si.eviacam.common.EVIACAM;

/**
 * Debugging code for AccessibilityWindowInfo (API +21)
 */
@SuppressWarnings("unused")
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class AccessibilityWindowDebug {
    static
    public void displayFullWindowTree (List<AccessibilityWindowInfo> l) {
        Log.d(EVIACAM.TAG, "Accessibility window tree dump:");
        int i= 1;
                
        for (AccessibilityWindowInfo w : l) {
            displayFullWindowTree0(w, "W" + Integer.toString(i++));
        }
    }
    
    static private void displayFullWindowTree0 (AccessibilityWindowInfo win, String prefix) {
        Log.d(EVIACAM.TAG, prefix + " " + win.toString());

        // has nodes?
        AccessibilityNodeInfo node= win.getRoot();
        if (node != null) {
            AccessibilityNodeDebug.displayFullTree0 (node, prefix + ".1");
        }
        
        // propagate calls to children
        for (int i= 0; i< win.getChildCount(); i++) {
            String newPrefix= " " + prefix + "." + Integer.toString(i + 1);
            displayFullWindowTree0(win.getChild(i), newPrefix);
        }
    }
}
