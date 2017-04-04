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

package com.crea_si.eviacam.util;

import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.crea_si.eviacam.common.EVIACAM;

public class ViewUtils {
    /**
     * Determines if given point is inside view
     * @param p - coordinates of point 
     * @param view - view object to compare
     * @param scale - scale factor in X and Y, assumes pivot is in (0, 0)
     * @return true if the point is within view bounds, false otherwise
     */
    public static boolean isPointInsideView(Point p, View view, float scale) {
        if (view == null) return false;

        int[] location = new int[2];

        view.getLocationOnScreen(location);

        return !(p.x < location[0] || p.y < location[1]) &&
                !(location[0] + view.getWidth() * scale < p.x ||
                  location[1] + view.getHeight() * scale < p.y);
    }

    /**
     * Determines if given point is inside view
     * @param p - coordinates of point
     * @param view - view object to compare
     * @return true if the point is within view bounds, false otherwise
     */
    public static boolean isPointInsideView(Point p, View view) {
        return isPointInsideView(p, view, 1.0f);
    }
    
    /**
     * Given a view, finds recursively a view with the point inside and which have ID
     * @param p - coordinates of point 
     * @param v - view object to start search
     * @return the view which meets these conditions, null otherwise
     */
    public static View findViewWithIdBelowPoint(Point p, View v) {
        if (v.getVisibility() != View.VISIBLE) return null;
        if (!isPointInsideView(p, v)) return null;
        if (!(v instanceof ViewGroup)) {
            if (v.getId() != View.NO_ID) return v;
            return null; 
        }
        
        // is a ViewGroup, iterate children
        ViewGroup vg= (ViewGroup) v;
        
        int childCount= vg.getChildCount();
        
        for (int i= 0; i< childCount; i++) {            
            View result = findViewWithIdBelowPoint(p, vg.getChildAt(i));
            if (result != null) return result;
        }

        return null;
    }
    
    /**
     * Dump view group hierarchy for debugging
     * @param v view
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public static void dumpViewGroupHierarchy (View v) {
        if (v == null) return;
        
        
        if (v.getId() == View.NO_ID) {
            Log.d(EVIACAM.TAG, "Processing NO_ID View: " + v.toString());
        }
        else {
            Log.d(EVIACAM.TAG, "Processing (id:" + v.getId() + ") " + v.toString());
        }
        
        if (!(v instanceof ViewGroup)) return; 
        
        ViewGroup vg= (ViewGroup) v;
        
        // iterate children
        
        int childCount= vg.getChildCount();
                
        for (int i= 0; i< childCount; i++) {
            dumpViewGroupHierarchy(vg.getChildAt(i));
        }
    }
}
