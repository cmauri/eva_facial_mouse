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
import android.accessibilityservice.AccessibilityServiceInfo;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

/**
 * Manages actions relative to the Android accessibility API 
 */

class AccessibilityAction {

    // delay after which an accessibility event is processed
    private static final long SCROLLING_SCAN_RUN_DELAY= 700;
    
    /**
     * Class to put together an accessibility action 
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

    // layer view for the scrolling controls
    private ScrollLayerView mScrollLayerView;

    // delegate to manage input method interaction
    private final InputMethodAction mInputMethodAction;
    
    // tracks whether the contextual menu is open
    private boolean mContextMenuOpen= false;

    // node on which the action should be performed when context menu open
    private AccessibilityNodeInfo mNode;
    
    // handler to execute in the main thread
    private final Handler mHandler;
    
    // time stamp at which scrolling scan need to execute
    private long mRunScrollingScanTStamp = 0;
    
    // is set to true when scrolling scan needs to be run
    private boolean mNeedToRunScrollingScan = false;
    
    public AccessibilityAction (
            ControlsLayerView cv, DockPanelLayerView dplv, ScrollLayerView slv) {
        mControlsLayerView= cv;
        mDockPanelLayerView= dplv;
        mScrollLayerView= slv;
        
        mHandler = new Handler();
        
        mInputMethodAction= new InputMethodAction (cv.getContext());
        
        // populate actions to view & compute action mask
        int full_action_mask= 0;
        for (ActionLabel al : mActionLabels) {
            mControlsLayerView.populateContextMenu(al.action, al.labelId);
            full_action_mask|= al.action;
        }
        
        FULL_ACTION_MASK= full_action_mask;
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AccessibilityService s= EViacamService.getInstance();
            AccessibilityServiceInfo asi= s.getServiceInfo();
            asi.flags|= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            s.setServiceInfo(asi);
        }
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
            mInputMethodAction.closeIME();
            s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
            break;
        case R.id.recents_button:
            mInputMethodAction.closeIME();
            s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
            break;
        case R.id.notifications_button:
            mInputMethodAction.closeIME();
            s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
            break;
        default:
            return false;
        }
        
        return true;
    }
    
    /** Checks and run scrolling actions */
    private boolean manageScrollActions(Point p) {
        ScrollLayerView.NodeAction na= mScrollLayerView.getContaining(p);
        if (na == null) return false;
        
        na.node.performAction(na.actions);
        
        return true;
    }
    
    /** Perform an action to a node focusing it when necessary */
    private void performActionOnNode(AccessibilityNodeInfo node, int action) {
        if (action == 0) return;
        // TODO: currently only checks for EditText instances, check with EditText subclasses
        if ((action & AccessibilityNodeInfo.ACTION_CLICK) != 0 &&
                node.getClassName().toString().equalsIgnoreCase("android.widget.EditText")) {
            mInputMethodAction.openIME();
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        }
        
        // Here we tried to check whether for Kitkat and higher versions canOpenPopup() allows
        // to know if the node will actually open a popup and thus IME could be hidden. However
        // after some test with menu options with popups it seems that this function always
        // return false.
        
        node.performAction(action);
    }
    
    /**
     * Performs action (click) on a specific location of the screen
     * 
     * @param p - point in screen coordinates
     */
    public void performAction (PointF p) {
        Point pInt= new Point();
        pInt.x= (int) p.x;
        pInt.y= (int) p.y;
        
        if (mContextMenuOpen) {
            /** When context menu open only check it */
            int action= mControlsLayerView.testClick(pInt);
            mControlsLayerView.hideContextMenu();
            mContextMenuOpen= false;
            performActionOnNode(mNode, action);
        }
        else {
            // Manages clicks on global actions menu
            if (manageGlobalActions(pInt)) return;
            
            // Manages clicks for scrolling buttons
            if (manageScrollActions(pInt)) return;
            
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AccessibilityService s= EViacamService.getInstance();
                
                List<AccessibilityWindowInfo> l= s.getWindows();
                AccessibilityWindowDebug.displayFullWindowTree (l);
            }
            
            /**
             * Manages actions for the IME.
             * 
             * LIMITATIONS: when a pop up or dialog is covering the IME there is no way to
             * know (at least for API < 21) such circumstance. Therefore, we give preference
             * to the IME. This may lead to situations where the pop up is not accessible.
             * 
             * TODO: for Lollipop: check getWindows()
             * TODO: add an option to open/close IME
             */
            if (mInputMethodAction.click(pInt.x, pInt.y)) return;
            
            /** Manages actions on an arbitrary position of the screen  */
            
            // Finds node under (x, y) and its available actions
            AccessibilityNodeInfo node= findActionable (pInt, FULL_ACTION_MASK);
            
            if (node == null) return;

            EVIACAM.debug("Actionable node found: (" + pInt.x + ", " + pInt.y + ")." + 
                    AccessibilityNodeDebug.getNodeInfo(node));
            
            int availableActions= FULL_ACTION_MASK & node.getActions();
            
            if (Integer.bitCount(availableActions)> 1) {
                /** Multiple actions available, shows context menu */
                mControlsLayerView.showContextMenu(pInt, availableActions);
                mContextMenuOpen= true;
                mNode= node;
            }
            else {
                // One action, goes ahead
                performActionOnNode(node, availableActions);
            }
        }
    }
    
    /**
     * Needs to be called at regular intervals
     * 
     * Remarks: checks whether needs to start a scrolling nodes exploration
     */
    public void refresh() {
        if (!mNeedToRunScrollingScan) return;
        if (System.currentTimeMillis()< mRunScrollingScanTStamp) return;
        mNeedToRunScrollingScan = false;
        
        /** Need to run scrolling scan */
        EVIACAM.debug("Scanning for scrollables");
        final List<AccessibilityNodeInfo> nodes= findNodes (
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD |
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
       
        /** Interaction with the UI needs to be done in the main thread */
        Runnable r = new Runnable() {
            @Override
            public void run() {
                mScrollLayerView.clearScrollAreas();

                for (AccessibilityNodeInfo n : nodes) {
                    mScrollLayerView.addScrollArea(n);
                }
            }
        };
        mHandler.post(r);
    }

    
    /**
     * Process events from accessibility service to refresh scrolling controls
     * 
     * @param event - the event
     * 
     * Expects three types of events: 
     *  TYPE_WINDOW_STATE_CHANGED
     *  TYPE_WINDOW_CONTENT_CHANGED
     *  TYPE_VIEW_SCROLLED
     * 
     * Remarks: it seems that events come in short bursts so tries to save CPU 
     * time and improve responsiveness by delaying the execution of the scan
     * so that consecutive events only fire an actual scan. 
     */
    public void onAccessibilityEvent(AccessibilityEvent event) {
        switch (event.getEventType()) {
        case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            EVIACAM.debug("WINDOW_STATE_CHANGED");
            break;
        case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                switch (event.getContentChangeTypes ()) {
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION:
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT:
                    EVIACAM.debug("WINDOW_CONTENT_TEXT|CONTENT_DESC_CHANGED: IGNORED");
                    return;  // just ignore these events
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE:
                    EVIACAM.debug("WINDOW_CONTENT_CHANGED_SUBTREE");
                    break;
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED:
                    EVIACAM.debug("WINDOW_CONTENT_CHANGED_UNDEFINED");
                }
                break;
            }
            else {
                EVIACAM.debug("WINDOW_CONTENT_CHANGED");
            }
        case AccessibilityEvent.TYPE_VIEW_SCROLLED:
            EVIACAM.debug("VIEW_SCROLLED");
            break;
        default:
            EVIACAM.debug("UNKNOWN EVENT: IGNORED");
            return;
        }

        /** Schedule scrolling nodes scanning after SCROLLING_SCAN_RUN_DELAY ms */
        mRunScrollingScanTStamp= System.currentTimeMillis() + SCROLLING_SCAN_RUN_DELAY;
        mNeedToRunScrollingScan= true;
    } 

    /**
     * Class to store information across recursive calls
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
        
        RecursionInfo ri= new RecursionInfo (p, actions);

        AccessibilityNodeDebug.displayFullTree(rootNode);
        
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
            /**
             * If node does not contain (x, y) stop recursion. It seems that, when part
             * of the view is covered by another window (e.g. IME), reported bounds 
             * EXCLUDE the area covered by such a window. Unfortunately, this does not
             * always works, for instance, when a extracted view is shown (e.g. usually
             * in landscape mode). This behavior can be changed (see [1]) in the IME
             * but perhaps this is not the best approach.
             * 
             * [1] http://stackoverflow.com/questions/14252184/how-can-i-make-my-custom-keyboard-to-show-in-fullscreen-mode-always
             */
            return null;
        }

        // Although it seems that is not needed, we check and give out if the
        // node is not visible. Just in case.
        if (!node.isVisibleToUser()) return null;

        AccessibilityNodeInfo result = null;

        if ((node.getActions() & ri.actions) != 0) {
            // this is a good candidate but continues exploring children
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
     * Finds recursively all nodes that support certain actions
     *
     * @param actions - bitmask of actions
     * @return - list with the node, may be void
     */
    private static List<AccessibilityNodeInfo> findNodes (int actions) {
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
