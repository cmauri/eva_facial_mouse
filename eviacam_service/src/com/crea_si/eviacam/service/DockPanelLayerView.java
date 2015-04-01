package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class DockPanelLayerView extends RelativeLayout {
    
    // the docking panel
    private LinearLayout mDockPanelView;
    
    // whether is expanded or not
    private boolean mIsExpanded= true;
    
    // arrow facing left
    private Bitmap mArrowLeft;
    
    // arrow facing right
    private Bitmap mArrowRight;
    
    public DockPanelLayerView(Context context) {
        super(context);
        
        // inflate docking panel
        LayoutInflater inflater = LayoutInflater.from(context);
        mDockPanelView= (LinearLayout) inflater.inflate(R.layout.dock_panel_layout, this, false);
        
        addPanel(Gravity.START);
        
        // measure the size of the expand/collapse button
        mDockPanelView.measure(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        ImageButton ib= (ImageButton) mDockPanelView.findViewById(R.id.expand_collapse_dock_button);
        int width= ib.getMeasuredWidth();
        int height= ib.getMeasuredHeight();
        
        // get arrow left
        Drawable d= context.getResources().getDrawable(R.drawable.arrow_left);
        Bitmap origBitmap= ((BitmapDrawable) d).getBitmap();
        mArrowLeft= Bitmap.createScaledBitmap(origBitmap, width, height, true);
        ib.setImageBitmap(mArrowLeft);

        // get arrow right
        Matrix matrix = new Matrix(); 
        matrix.preScale(-1.0f, 1.0f); 
        mArrowRight = Bitmap.createBitmap(mArrowLeft, 0, 0, mArrowLeft.getWidth(), 
                mArrowLeft.getHeight(), matrix, false);
    }
  
    private void addPanel(int gravity) {
        RelativeLayout.LayoutParams lp= 
                (RelativeLayout.LayoutParams) mDockPanelView.getLayoutParams();
        
        switch (gravity) {
        case Gravity.END:
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            // no break
        case Gravity.START:
            lp.addRule(RelativeLayout.CENTER_VERTICAL);
            break;
        case Gravity.TOP:
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            break;
        case Gravity.BOTTOM:
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
            break;
        }

        mDockPanelView.setLayoutParams(lp);
                
        // add to this view
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
        
        ImageButton ib= (ImageButton)
                mDockPanelView.findViewById(R.id.expand_collapse_dock_button);
        if (ib != null) ib.setImageBitmap(mArrowLeft);
                
        mIsExpanded= true;
    }
    
    private void collapse() {
        if (!mIsExpanded) return;
        
        View v= mDockPanelView.findViewById(R.id.collapsible_view);
        if (v != null) v.setVisibility(View.GONE);
        
        ImageButton ib= (ImageButton)
                mDockPanelView.findViewById(R.id.expand_collapse_dock_button);
        if (ib != null) ib.setImageBitmap(mArrowRight);

        mIsExpanded= false;
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
}
