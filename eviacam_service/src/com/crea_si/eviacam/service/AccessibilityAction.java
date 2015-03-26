package com.crea_si.eviacam.service;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

class AccessibilityAction {
    
    // accessibility actions we are interested on when searching nodes
    static final int FULL_ACTION_MASK= 
            AccessibilityNodeInfo.ACTION_CLICK | 
            AccessibilityNodeInfo.ACTION_LONG_CLICK |
            AccessibilityNodeInfo.ACTION_COLLAPSE |
            AccessibilityNodeInfo.ACTION_COPY |
            AccessibilityNodeInfo.ACTION_CUT |
            AccessibilityNodeInfo.ACTION_DISMISS |
            AccessibilityNodeInfo.ACTION_EXPAND |
            AccessibilityNodeInfo.ACTION_PASTE |
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD |
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD; /* |
            AccessibilityNodeInfo.ACTION_SELECT;*/
    
    // reference to the view on which action menus are drawn
    ControlsView mControlsView;
    
    // tracks whether the contextual menu is open
    private boolean mContextMenuOpen= false;

    // node on which the action should be performed
    private AccessibilityNodeInfo mNode;
    
    public AccessibilityAction (ControlsView cv) {
        mControlsView= cv;
    }
    
    public void performAction (PointF p) {
        // TODO: consider making it an attribute
        Point pInt= new Point();
        pInt.x= (int) p.x;
        pInt.y= (int) p.y;
        
        if (mContextMenuOpen) {
            int action= mControlsView.testClick(pInt);
            mControlsView.hideActionsMenu();
            mContextMenuOpen= false;
            if (action != 0) mNode.performAction(action);
        }
        else {
            AccessibilityNodeInfo node= findActionable (pInt, FULL_ACTION_MASK);
            
            if (node == null) return;
            
            EVIACAM.debug("Actionable node found: (" + p.x + ", " + p.y + ")." + 
                    AccessibilityNodeDebug.getNodeInfo(node));
            
            int availableActions= FULL_ACTION_MASK & node.getActions();
            
            if (Integer.bitCount(availableActions)> 1) {
                mControlsView.showActionsMenu(pInt, availableActions);
                mContextMenuOpen= true;
                mNode= node;
            }
            else {
                node.performAction(availableActions);
            }
        }
    }
    
    static private class RecursionInfo {
        final public Point p;
        final public Rect tmp= new Rect();
        final public int actions;
        
        RecursionInfo (Point p, int actions) {
            this.p = p;
            this.actions= actions;
        }
    }
    
    /*
     * find recursively the node under (x, y) that accepts some or all 
     * actions encoded on the mask
     */
    private static AccessibilityNodeInfo findActionable (Point p, int actions) {
        // get root node
        final AccessibilityNodeInfo rootNode = 
                EViacamService.getInstance().getRootInActiveWindow();
        if (rootNode == null) return null;
        
        // TODO: consider making it an attribute to avoid creating it each time
        RecursionInfo ri= new RecursionInfo (p, actions);
        
        return findActionable0(rootNode, ri);
    }
    
    /*
     * 
     */
    private static AccessibilityNodeInfo findActionable0(
            AccessibilityNodeInfo node, RecursionInfo ri) {

        node.getBoundsInScreen(ri.tmp);
        if (!ri.tmp.contains(ri.p.x, ri.p.y)) {
            // if window does not contain (x, y) stop recursion
            return null;
        }

        AccessibilityNodeInfo result = null;

        if ((node.getActions() & ri.actions) != 0) {
            // this is a good candidate but continue exploring children
            // there are controls such as ListView which are clickable
            // but do not have an useful action associated
            result = node;
        }

        // propagate calls to children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = findActionable0(node.getChild(i), ri);

            if (child != null) result = child;
        }

        return result;
    }
}
