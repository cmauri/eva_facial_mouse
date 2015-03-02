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
        
        // find clickable node under (x, y)
        Point pInt= new Point();
        pInt.x= (int) p.x;
        pInt.y= (int) p.y;
        
        AccessibilityNodeInfoCompat node= findClickable(rootCompat, pInt); 
        if (node== null) return;
        
        // perform click
        node.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
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
        
        if (node.isClickable()) {
            // got it!
            EVIACAM.debug("Clickable found: (" + p.x + ", " + p.y + ")." + node.getText());
            return node;
        }
        
        // propagate calls to children
        for (int i= 0; i< node.getChildCount(); i++) {
            AccessibilityNodeInfoCompat descendant= findClickable0(node.getChild(i), p, window);
            
            if (descendant != null) return descendant;
        }
        
        // not found
        return null;
    }
}
