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

package com.crea_si.eviacam.util;

import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.crea_si.eviacam.common.EVIACAM;

public class AccessibilityNodeDebug {
    static 
    public String getNodeInfo (AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        String result= "[";
        
        // actions
        if (node.isClickable()) result+= "CL.";
        else result+= "...";
        
        if (node.isLongClickable()) result+= "LC.";
        else result+= "...";
        
        if (node.isCheckable()) result+= "CH.";
        else result+= "...";
        
        if (node.isFocusable()) result+= "FO.";
        else result+= "...";
        
        if (node.isScrollable()) result+= "SC.";
        else result+= "...";
        
        if (node.isVisibleToUser()) result+= "VI]";
        else result+= "..]";
        
        result+= "; ";
        
        result+= node.getClassName();
        
        result+= "; ";

        result+= node.getText(); 
        
        result+= "; ";
        
        result+= node.getContentDescription();
                        
        result+= "; ";
       
        // TODO use getActions instead of getActionList because the latter crashes
        int actions= node.getActions();
        
        result+= "; [";

        if ((actions & AccessibilityNodeInfo.ACTION_CLICK) != 0) {
            result+= "ACTION_CLICK, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_FOCUS) != 0) {
            result+= "ACTION_FOCUS, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_CLEAR_FOCUS) != 0) {
            result+= "ACTION_CLEAR_FOCUS, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_SELECT) != 0) {
            result+= "ACTION_SELECT, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_CLEAR_SELECTION) != 0) {
            result+= "ACTION_CLEAR_SELECTION, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) != 0) {
            result+= "ACTION_ACCESSIBILITY_FOCUS, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS) != 0) {
            result+= "ACTION_CLEAR_ACCESSIBILITY_FOCUS, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_LONG_CLICK) != 0) {
            result+= "ACTION_LONG_CLICK, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY) != 0) {
            result+= "ACTION_NEXT_AT_MOVEMENT_GRANULARITY, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY) != 0) {
            result+= "ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT) != 0) {
            result+= "ACTION_NEXT_HTML_ELEMENT, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT) != 0) {
            result+= "ACTION_PREVIOUS_HTML_ELEMENT, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) != 0) {
            result+= "ACTION_SCROLL_FORWARD, ";
        }
        if ((actions & AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) != 0) {
            result+= "ACTION_SCROLL_BACKWARD, ";
        }
    
        result+= "]";

        //node.isPassword();
        
        /*
        Rect boundsInParent = new Rect();
        node.getBoundsInParent(boundsInParent);
        */
        Rect boundsInScreen = new Rect();
        node.getBoundsInScreen(boundsInScreen);

        result+= boundsInScreen.toString();
 
        return result;
    }
    
    @SuppressWarnings("unused")
    static
    public void displayFullTree (AccessibilityNodeInfo node) {
        Log.d(EVIACAM.TAG, "Accessibility tree dump:");
        
        displayFullTree0(node, "1");
    }
    
    static void displayFullTree0 (AccessibilityNodeInfo node, String prefix) {
        if (node == null) return;

        Log.d(EVIACAM.TAG, prefix + " " + getNodeInfo(node));
        
        // propagate calls to children
        for (int i= 0; i< node.getChildCount(); i++) {
            String newPrefix= " " + prefix + "." + Integer.toString(i + 1);
            displayFullTree0(node.getChild(i), newPrefix);
        }
    }
}
