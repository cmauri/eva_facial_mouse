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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.PointF;

import com.crea_si.eviacam.Preferences;
import com.crea_si.eviacam.R;

import java.lang.Math;

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

    // speed multipliers (derived from axis_speed)
    private float mHorizontalSpeed, mVerticalSpeed;

    // pre-computed acceleration vector (derived from acceleration setting)
    private float mAccelArray[]= new float[ACCEL_ARRAY_SIZE];

    // filter weight (derived from motion_smoothing)
    private float mLowPassFilterWeight;

    // previous motion (needed for the motion filter)
    private PointF mPrevMotion = new PointF();

    // motion threshold in screen pixels
    private int mMotionThreshold;

    // pointer location in screen coordinates
    private PointF mPointerLocation= new PointF();

    // view to display the pointer
    private PointerLayerView mPointerLayerView;

    // preferences stuff
    private SharedPreferences mSharedPref;

    // constructor
    public PointerControl(Context c, PointerLayerView pv) {
        mPointerLayerView= pv;
       
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
        mSharedPref = Preferences.getSharedPreferences(c);
        
        // register preference change listener
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        
        updateSettings();
        
        reset();
    }
    
    private void updateSettings() {
        // get values from shared resources
        int xAxisSpeed= mSharedPref.getInt(Preferences.KEY_X_AXIS_SPEED, AXIS_SPEED_MIN);
        setXSpeed(xAxisSpeed);
        int yAxisSpeed= mSharedPref.getInt(Preferences.KEY_Y_AXIS_SPEED, AXIS_SPEED_MIN);
        setYSpeed(yAxisSpeed);
        int acceleration= mSharedPref.getInt(Preferences.KEY_ACCELERATION, ACCELERATION_MIN);
        setAcceleration(acceleration);
        int motionSmoothing= mSharedPref.getInt(Preferences.KEY_MOTION_SMOOTHING,
                MOTION_SMOOTHING_MIN);
        setMotionSmoothing(motionSmoothing);
        mMotionThreshold= mSharedPref.getInt(Preferences.KEY_MOTION_THRESHOLD,
                MOTION_THRESHOLD_MIN);
    }
    
    // clean-up object
    public void cleanup() {
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(Preferences.KEY_X_AXIS_SPEED) ||
            key.equals(Preferences.KEY_Y_AXIS_SPEED) ||
            key.equals(Preferences.KEY_ACCELERATION) ||
            key.equals(Preferences.KEY_MOTION_SMOOTHING) ||
            key.equals(Preferences.KEY_MOTION_THRESHOLD)) {
            updateSettings();
        }
    }
    
    private static float computeSpeedFactor(int speed) {
        return (float) Math.pow (6.0, speed / 6.0); 
    }
    
    private void setXSpeed(int value) {
        if (value >= AXIS_SPEED_MIN && value <= AXIS_SPEED_MAX) {
            mHorizontalSpeed = computeSpeedFactor(value);
        }
    }

    private void setYSpeed (int value) {
        if (value >= AXIS_SPEED_MIN && value <= AXIS_SPEED_MAX) {
            mVerticalSpeed = computeSpeedFactor(value);
        }
    }

    private void setRelAcceleration (int delta0, float factor0, int delta1, float factor1) {
        //assert (delta0> 2 && delta1> 2);
        //assert (factor0> 0.0f && factor1> 0.0f);
        
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
            case 1: setRelAcceleration (9, 1.5f); break;
            case 2: setRelAcceleration (7, 1.5f); break;
            case 3: setRelAcceleration (7, 1.5f, 14, 2.0f); break;
            case 4: setRelAcceleration (5, 1.5f, 10, 3.0f); break;
            case 5: setRelAcceleration (3, 1.5f,  8, 3.0f); break;
            default: throw new IllegalStateException("Wrong acceleration value");
        }
    }

    private void setMotionSmoothing(int smoothness) {
        if (smoothness< MOTION_SMOOTHING_MIN) smoothness= MOTION_SMOOTHING_MIN;
        else if (smoothness> MOTION_SMOOTHING_MAX) smoothness= MOTION_SMOOTHING_MAX;
        mLowPassFilterWeight= (float) Math.log10((double) smoothness + 1);
    }
    
    /**
     * Reset pointer location by centering it
     */
    public void reset () {
        mPointerLocation.x = mPointerLayerView.getWidth() / 2;
        mPointerLocation.y = mPointerLayerView.getHeight() / 2;
    }

    // current motion (avoid creating an instance for each updateMotion call)
    private PointF mCurrMotion = new PointF();

    /**
     * Called for each frame to update pointer position
     * @param vel motion vector in world coordinates (i.e. upright face coordinates)
     */
    public void updateMotion(PointF vel) {
        mCurrMotion.x= vel.x;
        mCurrMotion.y= vel.y;

        // multipliers
        mCurrMotion.x *= mHorizontalSpeed;
        mCurrMotion.y *= mVerticalSpeed;

        /*
            The following commented block implements a behaviour in which
            the speed multipliers are specific for each side (long / short)
            of the device. When the device is rotated, so are the multipliers.

            After some tests (not with users) I decided to keep the old behaviour.
            Now each speed multiplier is specific for each axis of the motion
            of the face in the real world (e.g. horizontal speed multiplier is
            always used for horizontal face motion).

            TODO: perhaps this needs some usability testing
         */
        /*
        OrientationManager om= OrientationManager.get();
        int naturalOrientation= om.getDeviceNaturalOrientation();
        int currentOrientation= om.getDeviceCurrentOrientation();

        if (naturalOrientation == Configuration.ORIENTATION_LANDSCAPE &&
            (currentOrientation== 0 || currentOrientation== 180) ||
            naturalOrientation == Configuration.ORIENTATION_PORTRAIT &&
            (currentOrientation== 90 || currentOrientation== 270)) {
            mCurrMotion.x *= mLongSideSpeed;
            mCurrMotion.y *= mShortSideSpeed;
        }
        else {
            mCurrMotion.x *= mShortSideSpeed;
            mCurrMotion.y *= mLongSideSpeed;
        }*/

        // low-pass filter
        mCurrMotion.x= mCurrMotion.x * (1.0f - mLowPassFilterWeight) +
                mPrevMotion.x * mLowPassFilterWeight;
        mCurrMotion.y= mCurrMotion.y * (1.0f - mLowPassFilterWeight) +
                mPrevMotion.y * mLowPassFilterWeight;
        mPrevMotion.x= mCurrMotion.x;
        mPrevMotion.y= mCurrMotion.y;

        // acceleration
        double distance= Math.sqrt (mCurrMotion.x * mCurrMotion.x + mCurrMotion.y * mCurrMotion.y);
        int iAccelArray= (int) (distance + 0.5f);
        if (iAccelArray>= ACCEL_ARRAY_SIZE) iAccelArray= ACCEL_ARRAY_SIZE - 1;
        mCurrMotion.x*= mAccelArray[iAccelArray];
        mCurrMotion.y*= mAccelArray[iAccelArray];

        // stop margin
        if (-mMotionThreshold < mCurrMotion.x &&
                mCurrMotion.x < mMotionThreshold) mCurrMotion.x= 0.0f;
        if (-mMotionThreshold < mCurrMotion.y &&
                mCurrMotion.y < mMotionThreshold) mCurrMotion.y= 0.0f;

        // apply rotation
        OrientationManager.get().fixVectorOrientation(mCurrMotion);

        // update pointer location
        mPointerLocation.x+= mCurrMotion.x;
        if (mPointerLocation.x< 0) {
            mPointerLocation.x= 0;
        }
        else {
            int width= mPointerLayerView.getWidth();
            if (mPointerLocation.x>= width)
                mPointerLocation.x= width - 1;
        }
         
        mPointerLocation.y+= mCurrMotion.y;
        if (mPointerLocation.y< 0) {
            mPointerLocation.y= 0;
        }
        else {
            int height= mPointerLayerView.getHeight();
            if (mPointerLocation.y>= height)
                mPointerLocation.y= height - 1;
        }
        
        mPointerLayerView.updatePosition(mPointerLocation);
    }
    
    PointF getPointerLocation() {
        return mPointerLocation;
    }
}
