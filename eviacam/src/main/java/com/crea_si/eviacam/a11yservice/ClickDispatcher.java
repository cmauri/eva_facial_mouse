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

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PointF;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import com.crea_si.eviacam.common.MouseEmulationCallbacks;
import com.crea_si.eviacam.common.DockPanelLayerView;
import com.crea_si.eviacam.common.OverlayView;
import com.crea_si.eviacam.common.Preferences;

/**
 * Decides what to do when the user performs a click
 */
class ClickDispatcher implements MouseEmulationCallbacks,
        SharedPreferences.OnSharedPreferenceChangeListener {

    // audio manager for FX notifications
    private AudioManager mAudioManager;

    // layer for drawing the docking panel
    private DockPanelLayerView mDockPanelView;

    // docking panel enabled?
    private boolean mDockPanelEnabled= true;

    // layer for the scrolling user interface
    private ScrollLayerView mScrollLayerView;

    // scroll buttons enabled?
    private boolean mScrollEnabled= true;

    // layer for drawing the pointer context menus
    private ContextMenuLayerView mContextMenuView;

    // whether to play a sound when action performed
    private volatile boolean mSoundOnClick;

    // perform actions on the UI using the accessibility API
    private AccessibilityAction mAccessibilityAction;

    ClickDispatcher(@NonNull AccessibilityService s, @NonNull OverlayView ov) {
        mAudioManager= (AudioManager) s.getSystemService(Context.AUDIO_SERVICE);

        /* dockable menu view */
        mDockPanelView= new DockPanelLayerView(s);
        mDockPanelView.setVisibility(View.INVISIBLE);
        ov.addFullScreenLayer(mDockPanelView);

        /* view for scrolling buttons */
        mScrollLayerView= new ScrollLayerView(s);
        mScrollLayerView.setVisibility(View.INVISIBLE);
        ov.addFullScreenLayer(mScrollLayerView);

        /* context menu view */
        mContextMenuView = new ContextMenuLayerView(s);
        mContextMenuView.setVisibility(View.INVISIBLE);
        ov.addFullScreenLayer(mContextMenuView);

        mAccessibilityAction= new AccessibilityAction (s, mContextMenuView,
                mDockPanelView, mScrollLayerView);

        // register preference change listener
        Preferences.get().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        updateSettings();
    }

    private void updateSettings() {
        // get values from shared resources
        mSoundOnClick= Preferences.get().getSoundOnClick();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Preferences.KEY_SOUND_ON_CLICK)) updateSettings();
    }

    void cleanup() {
        stop();

        Preferences.get().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

        if (mAccessibilityAction!= null) {
            mAccessibilityAction.cleanup();
            mAccessibilityAction= null;
        }

        if (mContextMenuView != null) {
            mContextMenuView.cleanup();
            mContextMenuView = null;
        }

        if (mScrollLayerView!= null) {
            mScrollLayerView.cleanup();
            mScrollLayerView= null;
        }

        if (mDockPanelView!= null) {
            mDockPanelView.cleanup();
            mDockPanelView= null;
        }

        mAudioManager= null;
    }

    void start() {
        if (mAccessibilityAction!= null) mAccessibilityAction.reset();

        if (mDockPanelView!= null)
            mDockPanelView.setVisibility(mDockPanelEnabled? View.VISIBLE : View.INVISIBLE);

        if (mScrollLayerView!= null)
            mScrollLayerView.setVisibility(mScrollEnabled? View.VISIBLE : View.INVISIBLE);

        if (mContextMenuView!= null) {
            mContextMenuView.setVisibility(View.VISIBLE);
        }

        if (mAccessibilityAction!= null) {
            if (mScrollEnabled) mAccessibilityAction.enableScrollingScan();
            else mAccessibilityAction.disableScrollingScan();
        }
    }

    void stop() {
        if (mAccessibilityAction!= null) mAccessibilityAction.reset();

        if (mContextMenuView!= null) mContextMenuView.setVisibility(View.INVISIBLE);

        if (mDockPanelView != null) mDockPanelView.setVisibility(View.INVISIBLE);

        /* Scroll buttons */
        if (mAccessibilityAction!= null) { mAccessibilityAction.disableScrollingScan(); }
        if (mScrollLayerView!= null) mScrollLayerView.setVisibility(View.INVISIBLE);
    }

    /**
     * Reset internal state
     */
    void reset() {
        // Reset (remove) context menu
        if (mAccessibilityAction!= null) mAccessibilityAction.reset();
    }

    void refresh() {
        if (mAccessibilityAction != null) mAccessibilityAction.refresh();
    }

    private void playSound () {
        if (mSoundOnClick) {
            mAudioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
        }
    }

    void enableDockPanel() {
        if (!mDockPanelEnabled) {
            if (mDockPanelView != null) mDockPanelView.setVisibility(View.VISIBLE);
            mDockPanelEnabled= true;
        }
    }

    public void disableDockPanel() {
        if (mDockPanelEnabled) {
            if (mDockPanelView != null) mDockPanelView.setVisibility(View.INVISIBLE);
            mDockPanelEnabled= false;
        }
    }

    void enableScrollButtons() {
        if (!mScrollEnabled) {
            if (mScrollLayerView!= null) mScrollLayerView.setVisibility(View.VISIBLE);
            if (mAccessibilityAction!= null) { mAccessibilityAction.enableScrollingScan(); }
            mScrollEnabled= true;
        }
    }

    public void disableScrollButtons() {
        if (mScrollEnabled) {
            if (mScrollLayerView!= null) mScrollLayerView.setVisibility(View.INVISIBLE);
            if (mAccessibilityAction!= null) { mAccessibilityAction.disableScrollingScan(); }
            mScrollEnabled= false;
        }
    }

    /**
     * Get if view state for rest mode
     *
     * @return true if view state is in rest mode
     */
    boolean getRestModeEnabled() {
        return mDockPanelView.getRestModeEnabled();
    }

    void onAccessibilityEvent(AccessibilityEvent event) {
        if (mAccessibilityAction != null) mAccessibilityAction.onAccessibilityEvent(event);
    }

    // avoid creating an object for each onMouseEvent call
    // Does not need to be volatile as is only accessed from the 2ond thread
    private Point mPointInt= new Point();

    /**
     * Called each time a mouse event is generated
     *
     * @param location location of the pointer is screen coordinates
     * @param click true when click generated
     *
     * NOTE: this method is called from a secondary thread
     */
    @Override
    public void onMouseEvent(@NonNull PointF location, boolean click) {
        mPointInt.x= (int) location.x;
        mPointInt.y= (int) location.y;

        AccessibilityAction aa= mAccessibilityAction;
        if (aa!= null) {
            // this needs to be called regularly
            aa.refresh();

            // perform action when needed
            if (click) {
                aa.performAction(mPointInt);
                if (mSoundOnClick) playSound ();
            }
        }
    }

    /**
     *
     * @param location location of the pointer is screen coordinates
     * @return true when the location supports some action
     */
    @Override
    public boolean isClickable(@NonNull PointF location) {
        mPointInt.x= (int) location.x;
        mPointInt.y= (int) location.y;
        return mAccessibilityAction!= null && mAccessibilityAction.isActionable(mPointInt);
    }
}
