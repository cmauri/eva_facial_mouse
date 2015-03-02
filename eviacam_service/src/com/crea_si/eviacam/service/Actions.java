package com.crea_si.eviacam.service;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.accessibility.AccessibilityNodeInfo;

class Actions {    
    public static void click (PointF p) {
        // get root
        final AccessibilityNodeInfo rootNode = 
                EViacamService.getInstance().getRootInActiveWindow();
        if (rootNode == null) return;
        final AccessibilityNodeInfoCompat rootCompat = 
                new AccessibilityNodeInfoCompat(rootNode);
        
        // find clickable node under (x, y)
        Point pInt= new Point();
        pInt.x= (int) p.x;
        pInt.y= (int) p.y;
        
        AccessibilityNodeInfoCompat node= findClickable(rootCompat, pInt); 
        if (node== null) return;
        
        // perform click
        node.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
    }
    
    
    private static AccessibilityNodeInfoCompat findClickable(
            AccessibilityNodeInfoCompat node, Point p) {

        // TODO: move the new outside the recursion
        Rect window = new Rect();
        node.getBoundsInScreen(window);
        if (!window.contains(window)) {
            // if window does not contain (x, y) stop recursion
            return null;
        }
        
        if (node.isClickable()) {
            // got it!
            EVIACAM.debug("Clickable found: (" + p.x + ", " + p.y + ")." + node.getText());
            return node;
        }
        
        // propagate calls to children
        for (int i= 0; i< node.getChildCount(); i++) {
            AccessibilityNodeInfoCompat descendant= findClickable(node.getChild(i), p);
            
            if (descendant != null) {
                return descendant;
            }
        }
        
        // not found
        return null;
    }
}
