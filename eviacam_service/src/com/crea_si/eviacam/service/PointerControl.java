package com.crea_si.eviacam.service;

import android.graphics.PointF;
import java.lang.Math;

class PointerControl {
    // constants
    private final int ACCEL_ARRAY_SIZE= 30;
    private final int DEFAULT_SPEED= 10;
    private final int MAX_SPEED= 25;
    private final int DEFAULT_ACCELERATION= 1;
    private final int MAX_ACCELERATION= 5;
    private final int DEFAULT_SMOOTHNESS= 1;
    private final int MAX_SMOOTHNESS= 8;
    private final int DEFAULT_STOP_MARGIN= 2;
    private final int MAX_STOP_MARGIN= 5;
    
    // configuration attributes
    private int mXSpeed= DEFAULT_SPEED, mYSpeed= DEFAULT_SPEED;
    private int mAcceleration= DEFAULT_ACCELERATION;
    private boolean mBeepOnClick= true;
    private float mLowPassFilterWeight;
    private int mStopMargin= DEFAULT_STOP_MARGIN;
    
    // internal status attributes
    private float mXMultiplier, mYMultiplier;
    private float mAccelArray[]= new float[ACCEL_ARRAY_SIZE];
    private float mDXPrevious, mDYPrevious; // previous values for the filter
    private PointF mPointerLocation= new PointF();
    private OverlayView mOverlayView;
    
    // methods
    public PointerControl(OverlayView ov){
        mOverlayView= ov;
        mXMultiplier= getSpeedFactor(mXSpeed);
        mYMultiplier= getSpeedFactor(mYSpeed);
        setSmoothness (DEFAULT_SMOOTHNESS);
        for (int i= 0; i< ACCEL_ARRAY_SIZE; i++) mAccelArray[i]= 1.0f;
    }
    
    private float getSpeedFactor(int speed) {
        return (float) Math.pow (6.0, speed / 6.0); 
    }
    
    public int getXSpeed() { return mXSpeed; }
    public void setXSpeed(int value) {
        if (value >= 0 && value <= MAX_SPEED) {
            mXSpeed= value;
            mXMultiplier= getSpeedFactor(mXSpeed);
        }
    }

    public int getYSpeed() { return mYSpeed; }
    public void setYSpeed (int value) {
        if (value >= 0 && value <= MAX_SPEED) {
            mYSpeed= value;
            mYMultiplier= getSpeedFactor(mYSpeed);
        }
    }

    
    private void setRelAcceleration (int delta0, float factor0, int delta1, float factor1) {
        assert (delta0> 2 && delta1> 2); 
        assert (factor0> 0.0f && factor1> 0.0f);
        
        if (delta0>= ACCEL_ARRAY_SIZE) delta0= ACCEL_ARRAY_SIZE;
        if (delta1>= ACCEL_ARRAY_SIZE) delta1= ACCEL_ARRAY_SIZE;
        
        int i;
        
        for (i= 0; i< delta0; i++) mAccelArray[i]= 1.0f;
        for (;i< delta1; i++) mAccelArray[i]= factor0;
        float j= 0;
        for (;i< ACCEL_ARRAY_SIZE; i++) {
            mAccelArray[i]= factor0 * factor1 + j;
            j+= 0.1f;
        }
    }
    
    private void setRelAcceleration (int delta0, float factor0) {
        setRelAcceleration (delta0, factor0, ACCEL_ARRAY_SIZE, 1.0f);
    }
    
    private void setRelAcceleration () {
        setRelAcceleration (ACCEL_ARRAY_SIZE, 1.0f, ACCEL_ARRAY_SIZE, 1.0f);
    }
    
    
    public int getAcceleration() { return mAcceleration; }
    public void setAcceleration(int acceleration) {
        if (acceleration> MAX_ACCELERATION) acceleration= MAX_ACCELERATION;

        switch (acceleration) {
            case 0: setRelAcceleration(); break;
            case 1: setRelAcceleration (7, 1.5f); break;
            case 2: setRelAcceleration (7, 2.0f); break;
            case 3: setRelAcceleration (7, 1.5f, 14, 2.0f); break;
            case 4: setRelAcceleration (7, 2.0f, 14, 1.5f); break;
            case 5: setRelAcceleration (7, 2.0f, 14, 2.0f); break;
            default: assert (false);
        }

        mAcceleration= acceleration;
    }

    public int getSmoothness() {
        return (int) (Math.pow (10.0, mLowPassFilterWeight) + 0.5f) - 1;
    }
    public void setSmoothness (int smoothness) {
        if (smoothness> MAX_SMOOTHNESS) smoothness= MAX_SMOOTHNESS;
        mLowPassFilterWeight= (float) Math.log10((double) smoothness + 1);
    }

    int getStopMargin() { return mStopMargin; } 
    void setStopMargin (int value) {
        if (value> MAX_STOP_MARGIN) value= MAX_STOP_MARGIN;
        mStopMargin= value;
    }

    boolean getBeepOnClick() { return mBeepOnClick; }
    void setBeepOnClick(boolean value) { mBeepOnClick = value; }
    
    public void updateMotion(PointF vel) {
        // multipliers
        float dx= vel.x * mXMultiplier;
        float dy= vel.y * mYMultiplier;
        
        // low-pass filter  
        dx= dx * (1.0f - mLowPassFilterWeight) + mDXPrevious * mLowPassFilterWeight;
        dy= dy * (1.0f - mLowPassFilterWeight) + mDYPrevious * mLowPassFilterWeight;
        mDXPrevious= dx;
        mDYPrevious= dy;

        // acceleration
        double distance= Math.sqrt (dx * dx + dy * dy);
        int iAccelArray= (int) (distance + 0.5f);
        if (iAccelArray>= ACCEL_ARRAY_SIZE) iAccelArray= ACCEL_ARRAY_SIZE - 1;
        dx*= mAccelArray[iAccelArray];
        dy*= mAccelArray[iAccelArray];
        
        // stop margin
        if (-mStopMargin < dx && dx < mStopMargin) dx= 0.0f;
        if (-mStopMargin < dy && dy < mStopMargin) dy= 0.0f;
        
        // update pointer location
        mPointerLocation.x+= dx;
        if (mPointerLocation.x< 0) {
            mPointerLocation.x= 0;
        }
        else {
            int width= mOverlayView.getWidth();
            if (mPointerLocation.x>= width)
                mPointerLocation.x= width - 1;
        }
         
        mPointerLocation.y+= dy;
        if (mPointerLocation.y< 0) {
            mPointerLocation.y= 0;
        }
        else {
            int height= mOverlayView.getHeight();
            if (mPointerLocation.y>= height)
                mPointerLocation.y= height - 1;
        }
        
        mOverlayView.updatePointerLocation(mPointerLocation);
    }
}