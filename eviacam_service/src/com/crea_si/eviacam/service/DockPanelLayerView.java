package com.crea_si.eviacam.service;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

public class DockPanelLayerView extends RelativeLayout {
        
    private View mDockPanelView;
    
    public DockPanelLayerView(Context context) {
        super(context);
        
        LayoutInflater inflater = LayoutInflater.from(context);
        mDockPanelView= inflater.inflate(R.layout.dock_panel_layout, this, true);
    }
}
