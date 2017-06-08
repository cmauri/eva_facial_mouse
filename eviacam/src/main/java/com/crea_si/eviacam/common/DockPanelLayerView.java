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

package com.crea_si.eviacam.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.crea_si.eviacam.R;
import com.crea_si.eviacam.util.ViewUtils;

/**
 * Dock menu view
 */
public class DockPanelLayerView extends RelativeLayout 
    implements OnSharedPreferenceChangeListener {
    
    private static final int TOGGLE_BUTTON_SHORT_SIDE_DP= 18;
    private static final int TOGGLE_BUTTON_LONG_SIDE_DP= 30;
    private static final int TOGGLE_BUTTON_PADDING_DP= 2;

    private final int DOCKING_PANEL_EDGE_DEFAULT;
    private final int EDGE_RIGHT;
    private final int EDGE_TOP;
    private final int EDGE_BOTTOM;
    private static final float DEFAULT_ALPHA= 1.0f;
    private final float DISABLED_ALPHA;
    
    // the docking panel
    private LinearLayout mDockPanelView;
    
    // whether is expanded or not
    private boolean mIsExpanded= true;
    
    // arrow icon when the panel is expanded
    private Bitmap mToggleExpandedArrow;
    
    // arrow icon when the panel is collapsed
    private Bitmap mToggleCollapsedArrow;

    // rest mode icon when the panel is collapsed
    private Bitmap mToggleCollapsedRestMode;

    // status of the context menu
    private boolean mContextMenuEnabled= false;

    // status of the rest mode
    private boolean mRestModeEnabled= false;

    /**
     * Constructor
     * @param c context
     */
    public DockPanelLayerView(Context c) {
        super(c);
       
        // get constants from resources
        Resources r= c.getResources();
        DOCKING_PANEL_EDGE_DEFAULT= r.getInteger(R.integer.docking_panel_edge_default);
        EDGE_RIGHT= Integer.parseInt(r.getString(R.string.docking_panel_edge_right_value));
        EDGE_TOP= Integer.parseInt(r.getString(R.string.docking_panel_edge_top_value));
        EDGE_BOTTOM=  Integer.parseInt(r.getString(R.string.docking_panel_edge_bottom_value));
        DISABLED_ALPHA= (float) (ContextCompat.getColor(c, R.color.disabled_alpha) >> 24) / 255.0f;
        
        // shared preferences
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.registerOnSharedPreferenceChangeListener(this);
        updateSettings(sp);
    }
    
    public void cleanup() {
        SharedPreferences sp= Preferences.get().getSharedPreferences();
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }
    
    private void updateSettings(SharedPreferences sp) {
        // get values from shared resources
        int dockingEdge= Integer.parseInt(sp.getString(
                Preferences.KEY_DOCKING_PANEL_EDGE, Integer.toString(DOCKING_PANEL_EDGE_DEFAULT)));
        
        int gravity= Gravity.START;
        if (dockingEdge == EDGE_RIGHT)  gravity= Gravity.END;
        else if (dockingEdge == EDGE_TOP)    gravity= Gravity.TOP;
        else if (dockingEdge == EDGE_BOTTOM) gravity= Gravity.BOTTOM;
        
        float size = Preferences.get().getUIElementsSize();
        
        if (mDockPanelView != null) {
            removeView(mDockPanelView);
        }
        
        createAndAddDockPanel(gravity, size);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(Preferences.KEY_DOCKING_PANEL_EDGE) ||
            key.equals(Preferences.KEY_UI_ELEMENTS_SIZE)) {
            updateSettings(sharedPreferences);
        }
    }
    
    /**
     * Create container and set layout parameters
     */
    private static LinearLayout createContainerView (Context c, int gravity) {
        
        LinearLayout container= new LinearLayout(c);
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
 
        switch (gravity) {
        case Gravity.END:
            container.setOrientation(LinearLayout.HORIZONTAL);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            // no break
        case Gravity.START:
            lp.addRule(RelativeLayout.CENTER_VERTICAL);
            break;
        case Gravity.TOP:
            container.setOrientation(LinearLayout.VERTICAL);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            break;
        case Gravity.BOTTOM:
            container.setOrientation(LinearLayout.VERTICAL);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            break;
        }
        container.setLayoutParams(lp);
        
        return container;
    }
    
    /**
     * Inflate contents view and apply size
     *
     * @param c context
     * @param container into which add the buttons
     * @param gravity orientation of the layout
     * @param size multiplier to scale the size of the buttons
     * @return newly created view
     */
    private static View createPanelButtonsView (
            Context c, ViewGroup container, int gravity, float size) {
        LayoutInflater inflater = LayoutInflater.from(c);
        LinearLayout contents= (LinearLayout) 
                inflater.inflate(R.layout.dock_panel_layout, container, false);
        
        // Resize buttons as needed
        for (int i= 0; i< contents.getChildCount(); i++) {
            View v= contents.getChildAt(i);
            if (v instanceof ImageButton) {
                ImageButton ib = (ImageButton) v;
                ViewGroup.LayoutParams lp = ib.getLayoutParams();
                lp.width *= size;
                lp.height *= size;
                ib.setLayoutParams(lp);
            }
        }

        // set layout direction
        if (gravity == Gravity.END || gravity == Gravity.START) {
            contents.setOrientation(LinearLayout.VERTICAL);
        }
        else {
            contents.setOrientation(LinearLayout.HORIZONTAL);
        }
        
        return contents;
    }

    /**
     * Check if the menu should be draw in vertical orientation according to its gravity value
     * @param gravity gravity value
     * @return true when vertical
     */
    private static boolean isVertical (int gravity) {
        return (gravity == Gravity.END || gravity == Gravity.START);
    }
    
    /**
     * Create toggle button (i.e. button to show/hide the menu
     * @param gravity orientation of the layout
     * @param size multiplier to scale the size of the buttons
     * @return newly created view
     */
    private View createToggleButtonView (int gravity, float size) {
        /* first we need a relative layout for centering toggle */
        RelativeLayout buttonLayout= new RelativeLayout(this.getContext());
        if (isVertical(gravity)) {
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams (
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            buttonLayout.setLayoutParams(llp);
        }
        else {
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams (
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            buttonLayout.setLayoutParams(llp);
        }
        
        /* compute the size of the button */
        int longSide= (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TOGGLE_BUTTON_LONG_SIDE_DP, getResources().getDisplayMetrics()) * size);
        int shortSide= (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                TOGGLE_BUTTON_SHORT_SIDE_DP, getResources().getDisplayMetrics()) * size);
        
        /* create the button */
        ImageButton ib= new ImageButton(getContext());
        ib.setId(R.id.expand_collapse_dock_button);
        ib.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.half_alpha));
        ib.setContentDescription(this.getContext().getText(R.string.dock_menu_button));
        ib.setScaleType(ScaleType.FIT_CENTER);
        int padding_px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                TOGGLE_BUTTON_PADDING_DP, getResources().getDisplayMetrics());
        ib.setPadding(padding_px, padding_px, padding_px, padding_px);
        
        /* set layout params */
        if (isVertical(gravity)) {
            RelativeLayout.LayoutParams rlp= new RelativeLayout.LayoutParams(shortSide, longSide);
            rlp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
            ib.setLayoutParams(rlp);
        }
        else {
            RelativeLayout.LayoutParams rlp= new RelativeLayout.LayoutParams(longSide, shortSide);
            rlp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
            ib.setLayoutParams(rlp);
        }
        buttonLayout.addView(ib);
        
        /*
         * get and scale bitmaps
         */

        /* get arrow left icon and scale */
        Drawable d= ContextCompat.getDrawable(getContext(), R.drawable.arrow_left);
        Bitmap origBitmap= ((BitmapDrawable) d).getBitmap();
        Bitmap arrowLeft= Bitmap.createScaledBitmap(origBitmap, shortSide, longSide, true);

        /* get rest mode icon and scale */
        d= ContextCompat.getDrawable(getContext(), R.drawable.ic_rest_mode_enabled);
        origBitmap= ((BitmapDrawable) d).getBitmap();
        mToggleCollapsedRestMode= Bitmap.createScaledBitmap(origBitmap, longSide, shortSide, true);

        /* rotate arrow as needed */
        if (isVertical(gravity)) {
            // arrow right
            Matrix matrix = new Matrix(); 
            matrix.preScale(-1.0f, 1.0f); 
            Bitmap arrowRight = Bitmap.createBitmap(arrowLeft, 0, 0, arrowLeft.getWidth(), 
                    arrowLeft.getHeight(), matrix, false);
            
            if (gravity == Gravity.START) {
                mToggleExpandedArrow = arrowLeft;
                mToggleCollapsedArrow = arrowRight;
            }
            else { // END
                mToggleExpandedArrow = arrowRight;
                mToggleCollapsedArrow = arrowLeft;
            }
        }
        else {
            // arrow up
            Matrix matrix = new Matrix(); 
            matrix.setRotate(90); 
            Bitmap arrowUp = Bitmap.createBitmap(arrowLeft, 0, 0, arrowLeft.getWidth(), 
                    arrowLeft.getHeight(), matrix, false);
            
            // arrow down
            matrix.setScale(1.0f, -1.0f);
            Bitmap arrowDown = Bitmap.createBitmap(arrowUp, 0, 0, arrowUp.getWidth(), 
                    arrowUp.getHeight(), matrix, false);

            if (gravity == Gravity.TOP) {
                mToggleExpandedArrow = arrowUp;
                mToggleCollapsedArrow = arrowDown;
            }
            else { // BOTTOM
                mToggleExpandedArrow = arrowDown;
                mToggleCollapsedArrow = arrowUp;
            }
        }
        ib.setImageBitmap(mToggleExpandedArrow);
        
        return buttonLayout;
    }

    /**
     * Create and assemble the full menu
     * @param gravity orientation of the layout
     * @param size multiplier to scale the size of the buttons
     */
    private void createAndAddDockPanel (int gravity, float size) {

        // create elements
        LinearLayout container= createContainerView (getContext(), gravity);
        
        View panelButtons= createPanelButtonsView (getContext(), container, gravity, size);

        View toggleButton= createToggleButtonView(gravity, size);
   
        // assemble
        if (gravity == Gravity.START || gravity == Gravity.TOP) {
            container.addView(panelButtons);
            container.addView(toggleButton);
        }
        else {
            container.addView(toggleButton);
            container.addView(panelButtons);
        }
        
        mDockPanelView= container;
        addView(mDockPanelView);

        // update toggle buttons
        updateToggleButtons();

        // update rest mode appearance
        setRestModeAppearance();
    }
    
    /**
     * Finds the ID of the view below the point
     * @param p - the point in screen coordinates
     * @return id of the view, NO_ID otherwise
     */
    public int getViewIdBelowPoint (Point p) {
        View result= ViewUtils.findViewWithIdBelowPoint(p, mDockPanelView);
        if (result == null) return View.NO_ID;
        
        return result.getId();
    }
   
    /**
     * Gives an opportunity to process a click action for a given view
     *  
     * @param id - ID of the view on which to make click
     *
     * Remarks: called from a secondary thread
     */
    public void performClick (final int id) {
        if (id == R.id.expand_collapse_dock_button) {
            this.post(new Runnable() {
                @Override
                public void run() {
                    if (mIsExpanded) collapse();
                    else {
                        expand();
                        if (mRestModeEnabled) {
                            mRestModeEnabled = false;
                            updateToggleButtons ();
                            setRestModeAppearance();
                        }
                    }
                }
            });
        }
        else if (id == R.id.toggle_rest_mode) {
            mRestModeEnabled= !mRestModeEnabled;
            this.post(new Runnable() {
                @Override
                public void run() {
                    updateToggleButtons ();
                    setRestModeAppearance();
                    collapse();
                }
            });
        }
        else if (id == R.id.toggle_context_menu) {
            mContextMenuEnabled= !mContextMenuEnabled;
            this.post(new Runnable() {
                @Override
                public void run() {
                    updateToggleButtons ();
                }
            });
        }
    }

    /**
     * Update the bitmaps of toggle buttons
     */
    private void updateToggleButtons () {
        ImageButton ib= (ImageButton) mDockPanelView.findViewById(R.id.toggle_rest_mode);
        if (mRestModeEnabled) {
            ib.setImageResource(R.drawable.ic_rest_mode_enabled);
        }
        else {
            ib.setImageResource(R.drawable.ic_rest_mode_disabled);
        }

        ib= (ImageButton) mDockPanelView.findViewById(R.id.toggle_context_menu);
        if (mContextMenuEnabled) {
            ib.setImageResource(R.drawable.ic_context_menu_enabled);
        }
        else {
            ib.setImageResource(R.drawable.ic_context_menu_disabled);
        }
    }

    /**
     * Get if view state for rest mode
     *
     * @return true if view state is in rest mode
     */
    public boolean getRestModeEnabled () {
        return mRestModeEnabled;
    }

    /**
     * Get if view context menu is enabled
     *
     * @return true if the context menu is enabled
     */
    public boolean getContextMenuEnabled () {
        return mContextMenuEnabled;
    }

    /**
     * Check whether the point below the pointer is actionable
     * @param p point in screen coordinates
     * @return true when is actionable
     *
     * In rest mode, only a specific button in the dock panel works
     */
    public boolean isActionable(@NonNull Point p) {
        if (!mRestModeEnabled) return true;
        int id= getViewIdBelowPoint(p);

        return id == R.id.toggle_rest_mode || id == R.id.expand_collapse_dock_button;
    }

    /**
     * Expand the view so it becomes fully visible
     */
    private void expand() {
        if (mIsExpanded) return;
        
        View v= mDockPanelView.findViewById(R.id.collapsible_view);
        if (v != null) v.setVisibility(View.VISIBLE);
        
        ImageButton ib= (ImageButton)
                mDockPanelView.findViewById(R.id.expand_collapse_dock_button);
        if (ib != null) ib.setImageBitmap(mToggleExpandedArrow);
                
        mIsExpanded= true;
    }

    /**
     * Hide the menu leaving only a button to expand it again
     */
    private void collapse() {
        if (!mIsExpanded) return;
        
        View v= mDockPanelView.findViewById(R.id.collapsible_view);
        if (v != null) v.setVisibility(View.GONE);
        
        ImageButton ib= (ImageButton)
                mDockPanelView.findViewById(R.id.expand_collapse_dock_button);
        if (ib != null) {
            if (mRestModeEnabled) {
                ib.setImageBitmap(mToggleCollapsedRestMode);
            }
            else {
                ib.setImageBitmap(mToggleCollapsedArrow);
            }
        }

        mIsExpanded= false;
    }
    
    /**
     * Enable or disable faded appearance of the buttons 
     */
    private void setRestModeAppearance() {
        float alpha= (mRestModeEnabled? DISABLED_ALPHA : DEFAULT_ALPHA);

        setChildrenRestModeAppearance(mDockPanelView, alpha);
    }

    /**
     * Recursive function change alpha to all buttons except the rest mode one
     *
     * @param v view
     * @param alpha alpha channel value
     */
    private static void setChildrenRestModeAppearance(View v, float alpha) {
        if (v == null) return;

        if (v instanceof ImageButton) {
            if (v.getId() != R.id.toggle_rest_mode) v.setAlpha(alpha);
        }
        else if (v instanceof ViewGroup) {
            ViewGroup vg= (ViewGroup) v;
            for (int i= 0; i< vg.getChildCount(); i++) {
                setChildrenRestModeAppearance(vg.getChildAt(i), alpha);
            }
        }
    }

    /**
     * Make the context menu button blink
     */
    public void startFlashingContextMenuButton() {
        final Animation animation = new AlphaAnimation(1, 0); // Change alpha from visible to invisible
        animation.setDuration(400); // duration
        animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.REVERSE);

        ImageButton ib= (ImageButton) mDockPanelView.findViewById(R.id.toggle_context_menu);
        ib.startAnimation(animation);
    }

    /**
     * Stop blinking the context menu button
     */
    public void stopFlashingContextMenuButton() {
        ImageButton ib= (ImageButton) mDockPanelView.findViewById(R.id.toggle_context_menu);
        ib.clearAnimation();
    }
}
