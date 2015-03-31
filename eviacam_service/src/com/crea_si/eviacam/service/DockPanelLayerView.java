package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

public class DockPanelLayerView extends RelativeLayout {
        
    private View mDockPanelView;
    
    public DockPanelLayerView(Context context) {
        super(context);
        
        LayoutInflater inflater = LayoutInflater.from(context);
        mDockPanelView= inflater.inflate(R.layout.dock_panel_layout, this, false);
        addView(mDockPanelView);
        //ViewUtils.dumpViewGroupHierarchy(mDockPanelView);
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
}
