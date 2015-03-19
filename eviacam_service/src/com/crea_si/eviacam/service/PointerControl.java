package com.crea_si.eviacam.service;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.PointF;
import android.preference.PreferenceManager;

import java.lang.Math;

@SuppressLint("Assert")
class PointerControl implements OnSharedPreferenceChangeListener {
    // constants
    private final int AXIS_SPEED_MIN;
    private final int AXIS_SPEED_MAX;
    private final int ACCELERATION_MIN;
    private final int ACCELERATION_MAX;
    private final int MOTION_SMOOTHING_MIN;
    private final int MOTION_SMOOTHING_MAX;
    private final int MOTION_THRESHOLD_MIN;
    private final int ACCEL_ARRAY_SIZE= 30;
    
    private static final String KEY_X_AXIS_SPEED= "x_axis_speed";
    private static final String KEY_Y_AXIS_SPEED= "y_axis_speed";
    private static final String KEY_ACCELERATION= "acceleration";
    private static final String KEY_MOTION_SMOOTHING= "motion_smoothing";
    private static final String KEY_MOTION_THRESHOLD= "motion_threshold";
    
    // internal status attributes
    private float mXMultiplier, mYMultiplier;   // derived from axis_speed
    private float mAccelArray[]= new float[ACCEL_ARRAY_SIZE]; // derived from acceleration
    private float mLowPassFilterWeight; // derived from motion_smoothing
    private float mDXPrevious, mDYPrevious; // previous values for the filter
    private int mMotionThreshold;
    private PointF mPointerLocation= new PointF();
    private PointerView mPointerView;
    private SharedPreferences mSharedPref;
    private DwellClick mDwellClick;
    
    // methods
    public PointerControl(PointerView pv, ControlsView cv) {
        mPointerView= pv;
        
        Context c= EViacamService.getInstance().getApplicationContext();
        
        // get constants from resources
        Resources r= c.getResources();
        AXIS_SPEED_MIN= r.getInteger(R.integer.axis_speed_min);                
        AXIS_SPEED_MAX= r.getInteger(R.integer.axis_speed_max);
        
        ACCELERATION_MIN= r.getInteger(R.integer.acceleration_min);
        ACCELERATION_MAX= r.getInteger(R.integer.acceleration_max);
        
        MOTION_SMOOTHING_MIN= r.getInteger(R.integer.motion_smoothing_min);
        MOTION_SMOOTHING_MAX= r.getInteger(R.integer.motion_smoothing_max);
        
        MOTION_THRESHOLD_MIN = r.getInteger(R.integer.motion_threshold_min);

        // shared preferences
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        
        // register preference change listener
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        
        readSettings();
        
        mDwellClick= new DwellClick(cv);
    }
    
    private void readSettings() {
        // get values from shared resources
        int xAxisSpeed= mSharedPref.getInt(KEY_X_AXIS_SPEED, AXIS_SPEED_MIN);
        setXSpeed(xAxisSpeed);
        int yAxisSpeed= mSharedPref.getInt(KEY_Y_AXIS_SPEED, AXIS_SPEED_MIN);
        setYSpeed(yAxisSpeed);
        int acceleration= mSharedPref.getInt(KEY_ACCELERATION, ACCELERATION_MIN);
        setAcceleration(acceleration);
        int motionSmoothing= mSharedPref.getInt(KEY_MOTION_SMOOTHING, MOTION_SMOOTHING_MIN);
        setMotionSmoothning (motionSmoothing);
        mMotionThreshold= mSharedPref.getInt(KEY_MOTION_THRESHOLD, MOTION_THRESHOLD_MIN);
    }
    
    // clean-up object
    public void cleanup() {
        mDwellClick.cleanup();
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(KEY_X_AXIS_SPEED) || key.equals(KEY_Y_AXIS_SPEED) ||
            key.equals(KEY_ACCELERATION) || key.equals(KEY_MOTION_SMOOTHING) ||
            key.equals(KEY_MOTION_THRESHOLD)) {
            readSettings();
        }
    }
    
    private static float computeSpeedFactor(int speed) {
        return (float) Math.pow (6.0, speed / 6.0); 
    }
    
    private void setXSpeed(int value) {
        if (value >= AXIS_SPEED_MIN && value <= AXIS_SPEED_MAX) {
            mXMultiplier= computeSpeedFactor(value);
        }
    }

    private void setYSpeed (int value) {
        if (value >= AXIS_SPEED_MIN && value <= AXIS_SPEED_MAX) {
            mYMultiplier= computeSpeedFactor(value);
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
    
    private void setAcceleration(int acceleration) {
        if (acceleration< ACCELERATION_MIN) acceleration= ACCELERATION_MIN;
        else if (acceleration> ACCELERATION_MAX) acceleration= ACCELERATION_MAX;

        switch (acceleration) {
            case 0: setRelAcceleration(); break;
            case 1: setRelAcceleration (7, 1.5f); break;
            case 2: setRelAcceleration (7, 2.0f); break;
            case 3: setRelAcceleration (7, 1.5f, 14, 2.0f); break;
            case 4: setRelAcceleration (7, 2.0f, 14, 1.5f); break;
            case 5: setRelAcceleration (7, 2.0f, 14, 2.0f); break;
            default: assert (false);
        }
    }

    private void setMotionSmoothning (int smoothness) {
        if (smoothness< MOTION_SMOOTHING_MIN) smoothness= MOTION_SMOOTHING_MIN;
        else if (smoothness> MOTION_SMOOTHING_MAX) smoothness= MOTION_SMOOTHING_MAX;
        mLowPassFilterWeight= (float) Math.log10((double) smoothness + 1);
    }
    
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
        if (-mMotionThreshold < dx && dx < mMotionThreshold) dx= 0.0f;
        if (-mMotionThreshold < dy && dy < mMotionThreshold) dy= 0.0f;
        
        // update pointer location
        mPointerLocation.x+= dx;
        if (mPointerLocation.x< 0) {
            mPointerLocation.x= 0;
        }
        else {
            int width= mPointerView.getWidth();
            if (mPointerLocation.x>= width)
                mPointerLocation.x= width - 1;
        }
         
        mPointerLocation.y+= dy;
        if (mPointerLocation.y< 0) {
            mPointerLocation.y= 0;
        }
        else {
            int height= mPointerView.getHeight();
            if (mPointerLocation.y>= height)
                mPointerLocation.y= height - 1;
        }
        
        mPointerView.updatePosition(mPointerLocation);
        mDwellClick.updatePointerLocation(mPointerLocation);
    }
}
