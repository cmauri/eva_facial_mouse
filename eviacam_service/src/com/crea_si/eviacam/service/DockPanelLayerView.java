package com.crea_si.eviacam.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class DockPanelLayerView extends RelativeLayout 
    implements OnSharedPreferenceChangeListener {
    
    private static final String KEY_DOCKING_PANEL_EDGE= "docking_panel_edge";
    
    private static final int TOGGLE_BUTTON_SHORT_SIDE_DP= 15;
    private static final int TOGGLE_BUTTON_LONG_SIDE_DP= 24;
    
    private final int DOCKING_PANEL_EDGE_DEFAULT;
    private final int EDGE_RIGHT;
    private final int EDGE_TOP;
    private final int EDGE_BOTTOM;
    
    // reference to shared preferences pool
    private SharedPreferences mSharedPref;
    
    // the docking panel
    private LinearLayout mDockPanelView;
    
    // whether is expanded or not
    private boolean mIsExpanded= true;
    
    // arrow icon when the panel is expanded
    private Bitmap mToggleExpanded;
    
    // arrow icon when the panel is collapsed
    private Bitmap mToggleCollapsed;
    
    public DockPanelLayerView(Context context) {
        super(context);
       
        // get constants from resources
        Resources r= context.getResources();
        DOCKING_PANEL_EDGE_DEFAULT= r.getInteger(R.integer.docking_panel_edge_default);
        EDGE_RIGHT= Integer.parseInt(r.getString(R.string.docking_panel_edge_right_value));
        EDGE_TOP= Integer.parseInt(r.getString(R.string.docking_panel_edge_top_value));
        EDGE_BOTTOM=  Integer.parseInt(r.getString(R.string.docking_panel_edge_bottom_value));
        
        // shared preferences
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        
        // register preference change listener
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        
        readSettings();
    }
    
    public void cleanup() {
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);        
    }
    
    private void readSettings() {
        // get values from shared resources
        int dockingEdge= Integer.parseInt(mSharedPref.getString(
                KEY_DOCKING_PANEL_EDGE, Integer.toString(DOCKING_PANEL_EDGE_DEFAULT)));
                
        if (mDockPanelView != null) {
            removeView(mDockPanelView);
        }
        
        int gravity= Gravity.START;
        if (dockingEdge == EDGE_RIGHT)  gravity= Gravity.END;
        else if (dockingEdge == EDGE_TOP)    gravity= Gravity.TOP;
        else if (dockingEdge == EDGE_BOTTOM) gravity= Gravity.BOTTOM;
        
        createAndAddDockPanel (gravity);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(KEY_DOCKING_PANEL_EDGE)) {
            readSettings();
        }
    }
    
    /**
     * create container and set layout parameters
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
     * inflate contents view
     */
    private static View createPanelButtonsView (Context c, ViewGroup container, int gravity) {
        LayoutInflater inflater = LayoutInflater.from(c);
        LinearLayout contents= (LinearLayout) 
                inflater.inflate(R.layout.dock_panel_layout, container, false);
        
        // set layout direction
        if (gravity == Gravity.END || gravity == Gravity.START) {
            contents.setOrientation(LinearLayout.VERTICAL);
        }
        else {
            contents.setOrientation(LinearLayout.HORIZONTAL);
        }
        
        return contents;
    }
    
    private static boolean isVertical (int gravity) {
        return (gravity == Gravity.END || gravity == Gravity.START);
    }
    
    /**
     * create toggle button
     */
    private View createToggleButtonView (int gravity) {
        // first we need a relative layout for centering toggle
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
        
        // size of the button
        int longSide= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                TOGGLE_BUTTON_LONG_SIDE_DP, getResources().getDisplayMetrics());
        int shortSide= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
                TOGGLE_BUTTON_SHORT_SIDE_DP, getResources().getDisplayMetrics());
        
        // create the button
        ImageButton ib= new ImageButton(this.getContext());
        ib.setId(R.id.expand_collapse_dock_button);
        ib.setBackgroundColor(getResources().getColor(R.color.half_alpha));
        ib.setContentDescription(this.getContext().getText(R.string.dock_panel_button));
        
        // set layout params
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
        
        // get original bitmap (which is arrow left)
        Drawable d= this.getContext().getResources().getDrawable(R.drawable.arrow_left);
        Bitmap origBitmap= ((BitmapDrawable) d).getBitmap();
        
        // scale to the final size
        Bitmap arrowLeft= Bitmap.createScaledBitmap(origBitmap, shortSide, longSide, true);
        
        if (isVertical(gravity)) {
            // arrow right
            Matrix matrix = new Matrix(); 
            matrix.preScale(-1.0f, 1.0f); 
            Bitmap arrowRight = Bitmap.createBitmap(arrowLeft, 0, 0, arrowLeft.getWidth(), 
                    arrowLeft.getHeight(), matrix, false);
            
            if (gravity == Gravity.START) {
                mToggleExpanded= arrowLeft;
                mToggleCollapsed= arrowRight;
            }
            else { // END
                mToggleExpanded= arrowRight;
                mToggleCollapsed= arrowLeft;
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
                mToggleExpanded= arrowUp;
                mToggleCollapsed= arrowDown;
            }
            else { // BOTTOM
                mToggleExpanded= arrowDown;
                mToggleCollapsed= arrowUp;
            }
        }
        ib.setImageBitmap(mToggleExpanded);
        
        return buttonLayout;
    }
    
    private void createAndAddDockPanel (int gravity) {
    
        // create elements
        LinearLayout container= createContainerView (this.getContext(), gravity);
        
        View panelButtons= createPanelButtonsView (this.getContext(), container, gravity);
        
        View toggleButton= createToggleButtonView(gravity);
   
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
    }
    
    /**
     * Finds the ID of the view below the point
     * @param p - the point
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
     * @return true if action performed, false otherwise
     */
    public boolean performClick (int id) {
        if (id == R.id.expand_collapse_dock_button) {
            this.post(new Runnable() {
                @Override
                public void run() {
                   if (mIsExpanded) collapse();
                   else expand();
                }
            });
            
            return true;
        }
        
        return false;
    }
    
    private void expand() {
        if (mIsExpanded) return;
        
        View v= mDockPanelView.findViewById(R.id.collapsible_view);
        if (v != null) v.setVisibility(View.VISIBLE);
        
        ImageButton ib= (ImageButton)
                mDockPanelView.findViewById(R.id.expand_collapse_dock_button);
        if (ib != null) ib.setImageBitmap(mToggleExpanded);
                
        mIsExpanded= true;
    }
    
    private void collapse() {
        if (!mIsExpanded) return;
        
        View v= mDockPanelView.findViewById(R.id.collapsible_view);
        if (v != null) v.setVisibility(View.GONE);
        
        ImageButton ib= (ImageButton)
                mDockPanelView.findViewById(R.id.expand_collapse_dock_button);
        if (ib != null) ib.setImageBitmap(mToggleCollapsed);

        mIsExpanded= false;
    }
}
