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

 package com.crea_si.eviacam.service;

import java.util.ArrayList;
import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

class AccessibilityAction {

    /*
     * class to put together an accessibility action 
     * and the label to display to the user 
     */
    private static class ActionLabel {
        int action;
        int labelId;
        ActionLabel(int a, int l) {
            action= a;
            labelId= l;
        }
    }
    
    // store the pairs (accessibility action, label) of supported actions
    private static final ActionLabel[] mActionLabels = new ActionLabel[] {
        new ActionLabel(AccessibilityNodeInfo.ACTION_CLICK, R.string.click),
        new ActionLabel(AccessibilityNodeInfo.ACTION_LONG_CLICK, R.string.long_click),            
        new ActionLabel(AccessibilityNodeInfo.ACTION_COLLAPSE, R.string.collapse),
        new ActionLabel(AccessibilityNodeInfo.ACTION_COPY, R.string.copy),
        new ActionLabel(AccessibilityNodeInfo.ACTION_CUT, R.string.cut),            
        new ActionLabel(AccessibilityNodeInfo.ACTION_DISMISS, R.string.dismiss),            
        new ActionLabel(AccessibilityNodeInfo.ACTION_EXPAND, R.string.expand),
        new ActionLabel(AccessibilityNodeInfo.ACTION_PASTE, R.string.paste),
        new ActionLabel(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, R.string.scroll_backward),
        new ActionLabel(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, R.string.scroll_forward)
    };

    // accessibility actions we are interested on when searching nodes
    private final int FULL_ACTION_MASK;

    // layer view for context menu
    private ControlsLayerView mControlsLayerView;
    
    // layer view for docking panel
    private DockPanelLayerView mDockPanelLayerView;

    // layer for the scrolling user interface
    private ScrollLayerView mScrollLayerView;

    // delegate to manage input method interaction
    private final InputMethodAction mInputMethodAction;
    
    // tracks whether the contextual menu is open
    private boolean mContextMenuOpen= false;

    // node on which the action should be performed
    private AccessibilityNodeInfo mNode;
    
    public AccessibilityAction (
            ControlsLayerView cv, DockPanelLayerView dplv, ScrollLayerView slv) {
        mControlsLayerView= cv;
        mDockPanelLayerView= dplv;
        mScrollLayerView= slv;
        
        mInputMethodAction= new InputMethodAction (cv.getContext());
        
        // populate actions to view & compute action mask
        int full_action_mask= 0;
        for (ActionLabel al : mActionLabels) {
            mControlsLayerView.populateContextMenu(al.action, al.labelId);
            full_action_mask|= al.action;
        }
        
        FULL_ACTION_MASK= full_action_mask;
    }
    
    public void cleanup () {
        mInputMethodAction.cleanup();
    }

    /** Manages global actions, return false if action not generated */
    private boolean manageGlobalActions (Point p) {
        int idDockPanelAction= mDockPanelLayerView.getViewIdBelowPoint(p);
        if (idDockPanelAction == View.NO_ID) return false;
        
        if (mDockPanelLayerView.performClick(idDockPanelAction)) return true;
        
        AccessibilityService s= EViacamService.getInstance();
        
        switch (idDockPanelAction) {
        case R.id.back_button:
            s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
            break;
        case R.id.home_button:
            s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            break;
        case R.id.recents_button:
            s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
            break;
        default:
            return false;
        }
        
        return true;
    }
    
    private void performActionOnNode(AccessibilityNodeInfo node, int action) {
        if (action == 0) return;
        // TODO: currently only checks for EditText instances, check with EditText subclasses
        if ((action & AccessibilityNodeInfo.ACTION_CLICK) != 0 &&
                node.getClassName().toString().equalsIgnoreCase("android.widget.EditText")) {
            mInputMethodAction.openIME();
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        }
        node.performAction(action);
    }
    
    public void performAction (PointF p) {
        Point pInt= new Point();
        pInt.x= (int) p.x;
        pInt.y= (int) p.y;
        
        if (mContextMenuOpen) {
            int action= mControlsLayerView.testClick(pInt);
            mControlsLayerView.hideContextMenu();
            mContextMenuOpen= false;
            performActionOnNode(mNode, action);
        }
        else {
           
            // manage clicks on global actions menu
            if (manageGlobalActions(pInt)) return;
            
            // manage actions for the IME
            if (mInputMethodAction.click(pInt.x, pInt.y)) return;
            
            /**
             * Find node under (x, y) and its available actions
             */
            AccessibilityNodeInfo node= findActionable (pInt, FULL_ACTION_MASK);
            
            if (node == null) return;
            
            EVIACAM.debug("Actionable node found: (" + pInt.x + ", " + pInt.y + ")." + 
                    AccessibilityNodeDebug.getNodeInfo(node));
            
            int availableActions= FULL_ACTION_MASK & node.getActions();
            
            if (Integer.bitCount(availableActions)> 1) {
                mControlsLayerView.showContextMenu(pInt, availableActions);
                mContextMenuOpen= true;
                mNode= node;
            }
            else {
                performActionOnNode(node, availableActions);
            }
        }
    }
    
    /**
     * Process events from accessiility service
     */
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // TODO: TYPE_WINDOW_CONTENT_CHANGED events come in short bursts,
        // filter repetitive events
        
        if (event == null) {
            // Called during the initialization
            // TODO: handle this case
            return;
        }
        
        AccessibilityNodeInfo node= event.getSource();
        
        switch (event.getEventType()) {
        case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            EVIACAM.debug("WINDOW_STATE_CHANGED: " + AccessibilityNodeDebug.getNodeInfo(node));
            break;
        case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                switch (event.getContentChangeTypes ()) {
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION:
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT:
                    EVIACAM.debug("WINDOW_CONTENT_TEXT|CONTENT_DESC_CHANGED: ignored");
                    return;  // just ignore these events
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE:
                    EVIACAM.debug("WINDOW_CONTENT_CHANGED_SUBTREE: " + AccessibilityNodeDebug.getNodeInfo(node));
                    break;
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED:
                    EVIACAM.debug("WINDOW_CONTENT_CHANGED_UNDEFINED: " + AccessibilityNodeDebug.getNodeInfo(node));
                }
                break;
            }
            else {
                EVIACAM.debug("WINDOW_CONTENT_CHANGED: " + AccessibilityNodeDebug.getNodeInfo(node));
            }
        case AccessibilityEvent.TYPE_VIEW_SCROLLED:
            EVIACAM.debug("VIEW_SCROLLED: " + AccessibilityNodeDebug.getNodeInfo(node));
            break;
        }
        
        List<AccessibilityNodeInfo> nodes= findNodes (
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD |
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);

        mScrollLayerView.clearScrollAreas();
        
        for (AccessibilityNodeInfo n : nodes) {
            mScrollLayerView.addScrollArea(n);
        }
    } 

    /**
     * class to store information across recursive calls
     */
    private static class RecursionInfo {
        final public Point p;
        final public Rect tmp= new Rect();
        final public int actions;
        
        RecursionInfo (Point p, int actions) {
            this.p = p;
            this.actions= actions;
        }
    }
 
    /**
     * Find recursively the node under (x, y) that accepts some or all
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
    
    /** Actual recursive call for findActionable */
    private static AccessibilityNodeInfo findActionable0(
            AccessibilityNodeInfo node, RecursionInfo ri) {

        // sometimes, during the recursion, getChild() might return null
        // check here and abort recursion in that case
        if (node == null) return null;
        
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

    /**
     * Find recursively all nodes that support certain actions
     *
     * @param actions - bitmask of actions
     * @return - list with the node, may be void
     */
    public static List<AccessibilityNodeInfo> findNodes (int actions) {
        final List<AccessibilityNodeInfo> result= new ArrayList<AccessibilityNodeInfo>();
        // get root node
        final AccessibilityNodeInfo rootNode =
                EViacamService.getInstance().getRootInActiveWindow();

        findNodes0 (actions, rootNode, result);

        return result;
    }

    /** Actual recursive call for findNode */
    private static void findNodes0 (final int actions, final AccessibilityNodeInfo node,
            final List<AccessibilityNodeInfo> result) {

        if (node == null) return;

        if ((node.getActions() & actions) != 0) {
            result.add(node);
        }

        // propagate calls to children
        for (int i = 0; i < node.getChildCount(); i++) {
            findNodes0 (actions, node.getChild(i), result);
        }
    }
}
