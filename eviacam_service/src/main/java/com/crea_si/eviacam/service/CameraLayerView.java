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

import android.content.Context;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

public class CameraLayerView extends RelativeLayout {
    // camera viewer size
    private final int CAM_SURFACE_WIDTH= 64;
    private final int CAM_SURFACE_HEIGHT= 48;
        
    public CameraLayerView(Context context) {
        super(context);
    }
    
    public void addCameraSurface(SurfaceView v) {
        // set layout and add to parent
        RelativeLayout.LayoutParams lp= 
                new RelativeLayout.LayoutParams(CAM_SURFACE_WIDTH, CAM_SURFACE_HEIGHT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        v.setLayoutParams(lp);

        this.addView(v);
    }
}
