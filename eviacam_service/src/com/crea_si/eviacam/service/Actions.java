package com.crea_si.eviacam.service;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityNodeInfo;

class Actions {    
    /*
     * Perform click under the element under p
     */
    public static void click (PointF p) {
        // get root
        final AccessibilityNodeInfo rootNode = 
                EViacamService.getInstance().getRootInActiveWindow();
        if (rootNode == null) return;
        final AccessibilityNodeInfoCompat rootCompat = 
                new AccessibilityNodeInfoCompat(rootNode);
    
        //displayFullTree(rootCompat);
        
        // find clickable node under (x, y)
        Point pInt= new Point();
        pInt.x= (int) p.x;
        pInt.y= (int) p.y;
        
        AccessibilityNodeInfoCompat node= findClickable(rootCompat, pInt); 
        if (node== null) return;
        
        // perform click
        node.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
        
        EVIACAM.debug("Clickable found: (" + p.x + ", " + p.y + ")." + node.getText());        
        EVIACAM.debug("Clicked node: " + getNodeInfo(node));
    }
    
    /*
     * Stub used to avoid creating a Rect instance for each call
     */
    private static AccessibilityNodeInfoCompat findClickable(
            AccessibilityNodeInfoCompat node, Point p) {
        Rect window = new Rect();
        
        return findClickable0(node, p, window);
    }
    
    /*
     * Find recursively the node under (x, y)
     */
    private static AccessibilityNodeInfoCompat findClickable0(
            AccessibilityNodeInfoCompat node, Point p, Rect window) {

        node.getBoundsInScreen(window);
        if (!window.contains(p.x, p.y)) {                
            // if window does not contain (x, y) stop recursion
            return null;
        }
        
        AccessibilityNodeInfoCompat result = null;
        
        if (node.isClickable()) {
            // this is a good candidate but continue exploring children
            // there are controls such as ListView which are clickable
            // but do not have an useful action associated
            result= node;
        }
        
        // propagate calls to children
        for (int i= 0; i< node.getChildCount(); i++) {
            AccessibilityNodeInfoCompat child= findClickable0(node.getChild(i), p, window);
            
            if (child != null) result= child;
        }
        
        return result;
    }
    
    /*
     * DEBUGGING CODE
     * 
     *  Code below is only intended for debugging
     */
    
    
    static String getNodeInfo (AccessibilityNodeInfoCompat node) {
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
    void displayFullTree (AccessibilityNodeInfoCompat node) {
        EVIACAM.debug("Accesibility tree dump:");
        
        displayFullTree0(node, "1");
    }
    
    static
    void displayFullTree0 (AccessibilityNodeInfoCompat node, String prefix) {
        EVIACAM.debug(prefix + " " + getNodeInfo(node));
        
        // propagate calls to children
        for (int i= 0; i< node.getChildCount(); i++) {
            String newPrefix= " " + prefix + "." + Integer.toString(i + 1);
            displayFullTree0(node.getChild(i), newPrefix);
        }
    }
}
