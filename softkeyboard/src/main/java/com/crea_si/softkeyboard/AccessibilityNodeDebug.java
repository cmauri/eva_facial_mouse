package com.crea_si.softkeyboard;

import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

@SuppressWarnings("unused")
class AccessibilityNodeDebug {
    private static String getNodeInfo(AccessibilityNodeInfo node) {
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
       
        // use getActions instead of getActionList because the latter crashes 
        int actions= node.getActions();
        
        result+= "; [";
        
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
        
        Rect boundsInScreen = new Rect();
        node.getBoundsInScreen(boundsInScreen);
        */
 
        return result;
    }
    
    static
    public void displayFullTree (AccessibilityNodeInfo node) {
        Log.d(EVIACAMSOFTKBD.TAG, "Accessibility tree dump:");
        
        displayFullTree0(node, "1");
    }
    
    static private void displayFullTree0 (AccessibilityNodeInfo node, String prefix) {
        Log.d(EVIACAMSOFTKBD.TAG, prefix + " " + getNodeInfo(node));
        
        // propagate calls to children
        for (int i= 0; i< node.getChildCount(); i++) {
            String newPrefix= " " + prefix + "." + Integer.toString(i + 1);
            displayFullTree0(node.getChild(i), newPrefix);
        }
    }
}
