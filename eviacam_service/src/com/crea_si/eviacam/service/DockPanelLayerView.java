package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class DockPanelLayerView extends RelativeLayout {
        
    private LinearLayout mDockPanelView;
    
    private boolean mIsExpanded= true;
    
    public DockPanelLayerView(Context context) {
        super(context);
        
        LayoutInflater inflater = LayoutInflater.from(context);
        mDockPanelView= (LinearLayout) inflater.inflate(R.layout.dock_panel_layout, this, false);
        
        RelativeLayout.LayoutParams lp= (RelativeLayout.LayoutParams) mDockPanelView.getLayoutParams();
        lp.addRule(RelativeLayout.CENTER_VERTICAL);
        mDockPanelView.setLayoutParams(lp);
        
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
    
    private void expand() {
        if (mIsExpanded) return;
        
        View v= mDockPanelView.findViewById(R.id.collapsible_view);
        if (v != null) v.setVisibility(View.VISIBLE);
        mIsExpanded= true;
    }
    
    private void collapse() {
        if (!mIsExpanded) return;
        
        View v= mDockPanelView.findViewById(R.id.collapsible_view);
        if (v != null) v.setVisibility(View.GONE);
        mIsExpanded= false;
    }
    
    /**
     * Gives an opportunity to process a click action for a given view
     *  
     * @param id - ID of the view on which to make click
     * @return true if action performed, false otherwise
     */
    public boolean performClick (int id) {
        if (id == R.id.expand_collapse_dock_panel) {
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
}
