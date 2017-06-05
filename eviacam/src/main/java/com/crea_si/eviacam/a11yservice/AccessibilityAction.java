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

package com.crea_si.eviacam.a11yservice;

import java.util.ArrayList;
import java.util.List;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.crea_si.eviacam.BuildConfig;
import com.crea_si.eviacam.common.DockPanelLayerView;
import com.crea_si.eviacam.common.EVIACAM;
import com.crea_si.eviacam.R;
import com.crea_si.eviacam.common.InputMethodAction;
import com.crea_si.eviacam.common.Preferences;
import com.crea_si.eviacam.util.AccessibilityNodeDebug;

 /**
  * Manages actions relative to the Android accessibility API
  *
  * TODO: refactor this class
  */

 @TargetApi(Build.VERSION_CODES.KITKAT)
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
    
    /*
     * List the (accessibility action, label) pairs of supported actions,
     * the order of the items is relevant for selecting a default action.
     */
    private static final ActionLabel[] mActionLabels = new ActionLabel[] {
        new ActionLabel(AccessibilityNodeInfo.ACTION_CLICK, R.string.action_click),
        new ActionLabel(AccessibilityNodeInfo.ACTION_LONG_CLICK, R.string.action_long_click),
        new ActionLabel(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, R.string.action_scroll_backward),
        new ActionLabel(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, R.string.action_scroll_forward),
        new ActionLabel(AccessibilityNodeInfo.ACTION_COLLAPSE, R.string.action_collapse),
        new ActionLabel(AccessibilityNodeInfo.ACTION_COPY, R.string.action_copy),
        new ActionLabel(AccessibilityNodeInfo.ACTION_CUT, R.string.action_cut),
        new ActionLabel(AccessibilityNodeInfo.ACTION_DISMISS, R.string.action_dismiss),
        new ActionLabel(AccessibilityNodeInfo.ACTION_EXPAND, R.string.action_expand),
        new ActionLabel(AccessibilityNodeInfo.ACTION_PASTE, R.string.action_paste),
    };

    private final AccessibilityService mAccessibilityService;

    // layer view for context menu
    private final ContextMenuLayerView mContextMenuLayerView;
    
    // layer view for docking panel
    private final DockPanelLayerView mDockPanelLayerView;

    // layer view for the scrolling controls
    private final ScrollLayerView mScrollLayerView;

    // handler to execute in the main thread
    private final Handler mHandler;

    // delegate to manage input method interaction
    private final InputMethodAction mInputMethodAction;

    // accessibility actions we are interested on when searching nodes
    private final int FULL_ACTION_MASK;

    // tracks whether the contextual menu is open
    private boolean mContextMenuOpen= false;

    // tracks whether the contextual menu help dialog is open
    private boolean mContextMenuHelpOpen= false;

    // node on which the action should be performed when context menu open
    private AccessibilityNodeInfo mNode;

    // scroll buttons exploration enabled?
    private boolean mScrollingScanEnabled= true;

    // time stamp at which scrolling scan need to execute
    private volatile long mRunScrollingScanTStamp = 0;
    
    // is set to true when scrolling scan needs to be run
    private volatile boolean mNeedToRunScrollingScan = false;

    // the current node tree contains a web view?
    private boolean mContainsWebView = false;

    // navigation keyboard advice shown?
    private boolean mNavigationKeyboardAdviceShown= false;

    AccessibilityAction (@NonNull AccessibilityService as, @NonNull ContextMenuLayerView cv,
                         @NonNull DockPanelLayerView dplv, @NonNull ScrollLayerView slv) {
        mAccessibilityService= as;
        mContextMenuLayerView = cv;
        mDockPanelLayerView= dplv;
        mScrollLayerView= slv;
        
        mHandler = new Handler();
        
        mInputMethodAction= new InputMethodAction (cv.getContext());
        
        // populate actions to view & compute action mask
        int full_action_mask= 0;
        for (ActionLabel al : mActionLabels) {
            mContextMenuLayerView.populateContextMenu(al.action, al.labelId);
            full_action_mask|= al.action;
        }

        FULL_ACTION_MASK= full_action_mask;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AccessibilityServiceInfo asi= mAccessibilityService.getServiceInfo();
            /* This call should not return null under normal circumstances,
               just to avoid spurious crashes */
            if (asi!= null) {
                asi.flags |= AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
                mAccessibilityService.setServiceInfo(asi);
            }
        }
    }

    @NonNull
    private Context getContext() {
        return mContextMenuLayerView.getContext();
    }
    
    public void cleanup () {
        mInputMethodAction.cleanup();
    }

    void enableScrollingScan() {
        mScrollingScanEnabled= true;
        mNeedToRunScrollingScan= true;
    }
    void disableScrollingScan() { mScrollingScanEnabled= false; }

    /** Manages global actions, return false if action not generated */
    private boolean manageGlobalActions (@NonNull Point p) {
        /*
         * Is the action for the dock panel?
         */
        int idDockPanelAction= mDockPanelLayerView.getViewIdBelowPoint(p);
        if (idDockPanelAction == View.NO_ID) return false;

        /*  Process action by the view */
        mDockPanelLayerView.performClick(idDockPanelAction);

        /*
         * Process action here
         */
        final AccessibilityService s= mAccessibilityService;

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
        case R.id.softkeyboard_button:
            mInputMethodAction.dockMenuKeyboardSequence();
            break;
        case R.id.toggle_context_menu:
            if (mDockPanelLayerView.getContextMenuEnabled()) {
                EVIACAM.ShortToast(getContext(), R.string.service_toast_context_menu_enabled);
            }
            else {
                EVIACAM.ShortToast(getContext(), R.string.service_toast_context_menu_disabled);
            }
            break;
        case R.id.toggle_rest_mode:
            if (mDockPanelLayerView.getRestModeEnabled()) {
                EVIACAM.ShortToast(getContext(), R.string.service_toast_rest_mode_enabled);
                refreshScrollingButtons();
            }
            else {
                EVIACAM.ShortToast(getContext(), R.string.service_toast_rest_mode_disabled);
            }
            break;
        }
        
        return true;
    }
    
    /** Checks and run scrolling actions */
    private boolean manageScrollActions(@NonNull Point p) {
        ScrollLayerView.NodeAction na= mScrollLayerView.getContaining(p);
        if (na == null) return false;
        
        /*
         * Workaround: give focus to the node to scroll. 
         * 
         * We had to do so because when scrolling some listview control
         * focus seems to be on the first element (but actually is on the
         * listview itself) and scrolling gets stuck.
         *
         * Edit: do not give focus anymore, that was part of the problem
         * 
         */
        //na.node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        na.node.performAction(na.actions);
        
        return true;
    }
    
    /** Perform an action to a node focusing it when necessary */
    private void performActionOnNode(@NonNull AccessibilityNodeInfo node, int action) {
        if (action == 0) return;
        
        /*
          Focus the node.

          REMARKS: tried to see whether it solved the problem with the icon panel of WhatsApp.
          but it did not work (see comments in AccessibilityAction.performAction method). We
          leave here because it seems reasonable to focus the node on which the action is performed

          EDIT: focus only EditText elements, focusing arbitrary widgets cause scrolling issues
         */
        // TODO: currently only checks for EditText instances, check with EditText subclasses
       CharSequence clsName;
       if ((action & AccessibilityNodeInfo.ACTION_CLICK) != 0 &&
                (node.isEditable() ||
                        (((clsName= node.getClassName())!= null) &&
                           clsName.toString().equalsIgnoreCase("android.widget.EditText")))) {
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            mInputMethodAction.textViewFocusedSequence();
        }

        /*
          Here we tried to check whether for Kitkat and higher versions canOpenPopup() allows
          to know if the node will actually open a popup and thus IME could be hidden. However
          after some test with menu options with popups it seems that this function always
          return false.
         */
        node.performAction(action);
    }

    /**
     * Reset internal state
     */
    public void reset () {
        if (mContextMenuOpen) {
            mContextMenuLayerView.hideContextMenu();
            mContextMenuOpen= false;
        }
    }
    
    /**
     * Check if the point is over an element which is actionable
     * 
     * @param p point in screen coordinates
     * @return true if the element below is actionable
     * 
     * Remarks: it is used to implement a button to disable/enable
     * rest mode (clicking function disabled). It does not check
     * if the node below the pointer is actually actionable.
     */
    boolean isActionable(@NonNull Point p) {
        if (!mDockPanelLayerView.getRestModeEnabled()) return true;

        /* In rest mode check for active areas of the Dock Menu */
        return mDockPanelLayerView.isActionable(p);
    }

    /**
     * Performs action (click) on a specific location of the screen
     * 
     * @param pInt - point in screen coordinates
     */
    void performAction(@NonNull Point pInt) {
        if (mContextMenuOpen && !mContextMenuHelpOpen) {
            /* When context menu open only check it */
            int action= mContextMenuLayerView.testClick(pInt);
            mContextMenuLayerView.hideContextMenu();
            mContextMenuOpen= false;
            performActionOnNode(mNode, action);
        }
        else {
            // Manages clicks on global actions menu
            if (manageGlobalActions(pInt)) return;
            
            // Manages clicks for scrolling buttons
            if (manageScrollActions(pInt)) return;
            
            AccessibilityNodeInfo root= null;
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                /*
                  According to the documentation [1]: "This method returns only the windows
                  that a sighted user can interact with, as opposed to all windows.".
                  Unfortunately this does not seem to be the actual behavior. On Lollipop
                  API 21 (Nexus 7) the windows of the IME is not returned. With API 22
                  the window of the IME is only returned for the google keyboard (and allows
                  to interact with it). For other keyboards (e.g. AnySoftKeyboard or
                  Go Keyboard 2015) does not work.

                  UPDATE: this does not always happens, sometimes an IME window is reported,
                  needs more work (tested under API 22 and bundled eviacam IME and
                  Go Keyboard 2015).

                  logcat excerpt
                  W3 AccessibilityWindowInfo[id=1544, type=TYPE_INPUT_METHOD, layer=21040, bounds=Rect(0, 33 - 1280, 800), focused=false, active=false, hasParent=false, hasChildren=false]
                  W3.1 [...............VI]; android.widget.FrameLayout; null; null; ; [ACTION_SELECT, ACTION_CLEAR_SELECTION, ACTION_ACCESSIBILITY_FOCUS, ]Rect(0, 808 - 800, 1280)
                  W4 AccessibilityWindowInfo[id=1516, type=TYPE_APPLICATION, layer=21035, bounds=Rect(0, 0 - 1280, 800), focused=true, active=true, hasParent=false, hasChildren=false]
                  W4.1 [...............VI]; android.widget.FrameLayout; null; null; ; [ACTION_SELECT, ACTION_CLEAR_SELECTION, ACTION_ACCESSIBILITY_FOCUS, ]Rect(0, 0 - 800, 1280)

                  Further tests with WhatsApp client show that if the input text of a chat
                  is not focused the window containing the emoticons is not reported (although
                  is visible and the user can interact with it).

                  [1] http://developer.android.com/reference/android/accessibilityservice/AccessibilityService.html#getWindows()
                 */
                List<AccessibilityWindowInfo> l= mAccessibilityService.getWindows();
                
                Rect bounds = new Rect();
                for (AccessibilityWindowInfo awi : l) {
                    awi.getBoundsInScreen(bounds);
                    if (bounds.contains(pInt.x, pInt.y)) {
                        AccessibilityNodeInfo rootCandidate= awi.getRoot();
                        if (rootCandidate == null) continue;
                        /*
                          Check bounds for the candidate root node. Sometimes windows bounds
                           are larger than root bounds
                         */
                        rootCandidate.getBoundsInScreen(bounds);
                        if (bounds.contains(pInt.x, pInt.y)) {
                            root = rootCandidate;
                            break;
                        }
                    }
                }
                
                /*
                  Give an opportunity to the bundled eviacam keyboard
                  TODO: check whether this is really needed (e.g. checking which IME is
                  currently active, if the node is already an IME)
                 */
                if (mInputMethodAction.click(pInt.x, pInt.y)) return;
            }
            else {
                /*
                  Manages actions for the IME.

                  LIMITATIONS: when a pop up or dialog is covering the IME there is no way to
                  know (at least for API < 21) such circumstance. Therefore, we give preference
                  to the IME. This may lead to situations where the pop up is not accessible.
                 */
                if (mInputMethodAction.click(pInt.x, pInt.y)) return;
            }

            /* Manages actions on an arbitrary position of the screen  */

            // Finds node under (x, y) and its available actions
            AccessibilityNodeInfo node= findActionable (pInt, FULL_ACTION_MASK, root);

            if (node == null) return;

            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "Actionable node found: (" + pInt.x + ", " +
                    pInt.y + ")." + AccessibilityNodeDebug.getNodeInfo(node));

            int availableActions= FULL_ACTION_MASK & node.getActions();

            if (Integer.bitCount(availableActions)> 1) {
                /* Multiple actions available, need to show the context menu? */
                if (mDockPanelLayerView.getContextMenuEnabled()) {
                    mContextMenuLayerView.showContextMenu(pInt, availableActions);
                    mContextMenuOpen = true;
                    mNode = node;
                }
                else {
                    /* Need to display help dialog. Run in the main thread */
                    if (Preferences.get().getShowContextMenuHelp() &&
                            mDockPanelLayerView.getVisibility() == View.VISIBLE) {
                        mContextMenuHelpOpen= true;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                showContextMenuHelp();
                            }
                        });
                    }

                    /* Pick the default action */
                    for (ActionLabel al : mActionLabels) {
                        if ((al.action & availableActions)!= 0) {
                            performActionOnNode(node, al.action);
                            break;
                        }
                    }
                }
            }
            else {
                // One action, goes ahead
                performActionOnNode(node, availableActions);
            }
        }
    }

    /**
     * Show the context menu help dialog
     */
    private void showContextMenuHelp() {
        mDockPanelLayerView.startFlashingContextMenuButton();

        final Context c= mDockPanelLayerView.getContext();
        View checkBoxView = View.inflate(c, R.layout.context_menu_help, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.get().setShowContextMenuHelp(!isChecked);
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle(c.getText(R.string.app_name));
        builder.setMessage(c.getText(R.string.service_dialog_context_menu_help_msg));
        builder.setView(checkBoxView);
        builder.setPositiveButton(c.getText(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mContextMenuHelpOpen= false;
                        mDockPanelLayerView.stopFlashingContextMenuButton();
                    }
                });
        AlertDialog ad = builder.create();

        //noinspection ConstantConditions
        ad.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        ad.show();
    }

    /**
     * Needs to be called at regular intervals
     * 
     * Remarks: checks whether needs to start a scrolling nodes exploration
     */
    void refresh() {
        if (mDockPanelLayerView.getRestModeEnabled()) return;
        if (!mNeedToRunScrollingScan) return;
        if (System.currentTimeMillis()< mRunScrollingScanTStamp) return;
        mNeedToRunScrollingScan = false;
        refreshScrollingButtons();
    }

    private Runnable mRefreshScrollingRunnable = new Runnable() {
        List<AccessibilityNodeInfo> scrollableNodes = new ArrayList<>();

        @Override
        public void run() {
            mScrollLayerView.clearScrollAreas();

            if (mScrollingScanEnabled && !mDockPanelLayerView.getRestModeEnabled()) {
                scrollableNodes.clear();
                mContainsWebView= findNodes (scrollableNodes,
                        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD |
                                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD,
                        "android.webkit.WebView");
                if (mContainsWebView && !mNavigationKeyboardAdviceShown) {
                    EVIACAM.LongToast(getContext(), R.string.service_toast_navigation_keyboard_advice);
                    mNavigationKeyboardAdviceShown= true;
                }
                if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "Scroll areas refresh done. Found:" +
                        scrollableNodes.size());
                for (AccessibilityNodeInfo n : scrollableNodes) {
                    mScrollLayerView.addScrollArea(n);
                }
            }
        }
    };

    private void refreshScrollingButtons() {
        /* Interaction with the UI needs to be done in the main thread */
        mHandler.post(mRefreshScrollingRunnable);
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
    void onAccessibilityEvent(@NonNull AccessibilityEvent event) {
        switch (event.getEventType()) {
        case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            break;

        case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:

            // If contains a WebView stop processing
            if (mContainsWebView) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                switch (event.getContentChangeTypes ()) {
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION:
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT:
                    return;  // just ignore these events
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE:
                    break;
                case AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED:
                }
            }
            break;

        case AccessibilityEvent.TYPE_VIEW_SCROLLED:
            // If contains a WebView stop processing
            if (mContainsWebView) return;

            break;

        default:
            if (BuildConfig.DEBUG) Log.d(EVIACAM.TAG, "UNKNOWN EVENT: IGNORED");
            return;
        }

        /* Schedule scrolling nodes scanning after SCROLLING_SCAN_RUN_DELAY ms */
        mRunScrollingScanTStamp= System.currentTimeMillis() + SCROLLING_SCAN_RUN_DELAY;
        mNeedToRunScrollingScan= true;
    } 

    /**
     * Class to store information across recursive calls
     */
    private static class RecursionInfo {
        final public Point p;
        final Rect tmp= new Rect();
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
    private AccessibilityNodeInfo findActionable (@NonNull Point p, int actions,
                                                  @Nullable AccessibilityNodeInfo root) {
        // get root node
        if (root == null) { 
            root = mAccessibilityService.getRootInActiveWindow();
            if (root == null) return null;
        }
        
        RecursionInfo ri= new RecursionInfo (p, actions);

        //AccessibilityNodeDebug.displayFullTree(rootNode);
        
        return findActionable0(root, ri);
    }
    
    /** Actual recursive call for findActionable */
    private static AccessibilityNodeInfo findActionable0(
            @Nullable AccessibilityNodeInfo node, @NonNull RecursionInfo ri) {

        // sometimes, during the recursion, getChild() might return null
        // check here and abort recursion in that case
        if (node == null) return null;
        
        node.getBoundsInScreen(ri.tmp);
        if (!ri.tmp.contains(ri.p.x, ri.p.y)) {
            /*
              If node does not contain (x, y) stop recursion. It seems that, when part
              of the view is covered by another window (e.g. IME), reported bounds
              EXCLUDE the area covered by such a window. Unfortunately, this does not
              always works, for instance, when a extracted view is shown (e.g. usually
              in landscape mode). This behavior can be changed (see [1]) in the IME
              but perhaps this is not the best approach.

              [1] http://stackoverflow.com/questions/14252184/how-can-i-make-my-custom-keyboard-to-show-in-fullscreen-mode-always
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
        final int child_count = node.getChildCount();
        for (int i = 0; i < child_count; i++) {
            AccessibilityNodeInfo child = findActionable0(node.getChild(i), ri);

            if (child != null) result = child;
        }

        return result;
    }

    /**
     * Finds recursively all nodes that support certain actions
     *
     * @param result - list where results will be stored
     * @param actions - bitmask of actions
     * @param exclude - class name to exclude from the search, null to not exclude anything
     * @return if some node has been excluded
     */
    private boolean findNodes(List<AccessibilityNodeInfo> result,
                              int actions, String exclude) {
        // get root node
        final AccessibilityNodeInfo rootNode = mAccessibilityService.getRootInActiveWindow();

        return findNodes0 (result, actions, exclude, rootNode);
    }

    /** Actual recursive call for findNode */
    private static boolean findNodes0 (
            List<AccessibilityNodeInfo> result, int actions,
            String exclude, final AccessibilityNodeInfo node) {

        if (node == null || !node.isVisibleToUser()) return false;
        if (exclude!= null) {
            CharSequence className= node.getClassName();
            if (className!= null && className.toString().equals(exclude)) return true;
        }
        if ((node.getActions() & actions) != 0) {
            result.add(node);
        }

        // propagate calls to children
        final int child_count = node.getChildCount();
        boolean excluded= false;
        for (int i = 0; i < child_count; i++) {
            excluded= findNodes0 (result, actions, exclude, node.getChild(i)) || excluded;
        }

        return excluded;
    }
}
