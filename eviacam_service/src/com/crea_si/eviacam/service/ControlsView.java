package com.crea_si.eviacam.service;

import android.content.Context;
import android.graphics.Paint;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

public class ControlsView extends RelativeLayout {
    private final int CAM_SURFACE_WIDTH= 320;
    private final int CAM_SURFACE_HEIGHT= 240;
    
    Paint mPaintBox;
    Button mButton;
        
    public ControlsView(Context context) {
        super(context);
        
        mPaintBox = new Paint();
        //setWillNotDraw(false);
    }
    
    public void addCameraSurface(SurfaceView v) {
        // Set layout and add to parent
        RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(CAM_SURFACE_WIDTH, CAM_SURFACE_HEIGHT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        v.setLayoutParams(lp);

        this.addView(v);
    }
    
    public void showButtons() {
        this.post(new Runnable() {
            @Override
            public void run() {
                if (mButton== null) {
                    RelativeLayout.LayoutParams lp= new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    lp.leftMargin= 100;
                    lp.topMargin= 200;
                    mButton= new Button(getContext());
                    mButton.setText("AKI");
                    mButton.setLayoutParams(lp);
                    addView(mButton);
                    
                    mButton.setOnClickListener(new Button.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EVIACAM.debug("Button clicked!!!");
                        }
                    });
                }
                else 
                    hideButtons();
            }
        });
    }
    
    public void hideButtons() {
        if (mButton!= null) {
            this.removeView(mButton);
            mButton= null;
        }
    }
}
