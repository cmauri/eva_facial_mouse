package com.crea_si.eviacam.service;

import android.graphics.Point;
import android.view.View;

class ViewUtils {
    /**
     * Determines if given points are inside view
     * @param p - coordinates of point 
     * @param view - view object to compare
     * @return true if the points are within view bounds, false otherwise
     */
    public static boolean isPointInsideView(Point p, View view) {
        int[] location= new int[2];
        
        view.getLocationOnScreen(location);
        
        if (p.x< location[0] || p.y< location[1]) return false;

        if (location[0] + view.getWidth() < p.x || 
            location[1] + view.getHeight() < p.y) return false;
   
        return true;
    }
}
