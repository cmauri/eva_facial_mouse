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
import android.media.AudioManager;

import com.crea_si.eviacam.Preferences;
import com.crea_si.eviacam.R;

class DwellClick implements OnSharedPreferenceChangeListener {
    /**
     * Enums and constants
     */
    private enum State {
        RESET, POINTER_MOVING, COUNTDOWN_STARTED, CLICK_DONE
    }
    
    private final int DWELL_TIME_DEFAULT;
    private final int DWELL_AREA_DEFAULT;
    private final boolean SOUND_ON_CLICK_DEFAULT;
    private final boolean CONSECUTIVE_CLICKS;

    // delegate to measure elapsed time
    private Countdown mCountdown;
    
    // reference to shared preferences pool
    private SharedPreferences mSharedPref;
    
    // current dwell click state
    private State mState= State.RESET;

    // dwell area tolerance. stored squared to avoid sqrt 
    // for each updatePointerLocation call
    private float mDwellAreaSquared;    
    
    // whether to play a sound when action performed
    private boolean mSoundOnClick;
    
    // if true it keeps generating clicks whilst the pointer is stopped
    private boolean mConsecutiveClicks;
    
    // audio manager for FX notifications
    AudioManager mAudioManager;
    
    // to remember previous pointer location and measure traveled distance
    private PointF mPrevPointerLocation= new PointF();
    
    public DwellClick(Context c) {
        // get constants from resources
        Resources r= c.getResources();
        DWELL_TIME_DEFAULT= r.getInteger(R.integer.dwell_time_default) * 100;
        DWELL_AREA_DEFAULT= r.getInteger(R.integer.dwell_area_default);
        SOUND_ON_CLICK_DEFAULT= r.getBoolean(R.bool.sound_on_click_default);
        CONSECUTIVE_CLICKS= r.getBoolean(R.bool.consecutive_clicks_default);
        
        mCountdown= new Countdown(DWELL_TIME_DEFAULT);
        
        mAudioManager= (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        
        // shared preferences
        mSharedPref = Preferences.getSharedPreferences(c);

        // register preference change listener
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        
        updateSettings();
    }
    
    private void updateSettings() {
        // get values from shared resources
        int dwellTime= mSharedPref.getInt(Preferences.KEY_DWELL_TIME, DWELL_TIME_DEFAULT) * 100;
        mCountdown.setTimeToWait(dwellTime);
        int dwellArea= mSharedPref.getInt(Preferences.KEY_DWELL_AREA, DWELL_AREA_DEFAULT);
        mDwellAreaSquared= dwellArea * dwellArea;
        mSoundOnClick= mSharedPref.getBoolean(Preferences.KEY_SOUND_ON_CLICK, SOUND_ON_CLICK_DEFAULT);
        mConsecutiveClicks = mSharedPref.getBoolean(
                Preferences.KEY_CONSECUTIVE_CLICKS, CONSECUTIVE_CLICKS);
    }
    
    public void cleanup() {
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);        
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(Preferences.KEY_DWELL_TIME) || key.equals(Preferences.KEY_DWELL_AREA) ||
            key.equals(Preferences.KEY_SOUND_ON_CLICK) || key.equals(Preferences.KEY_CONSECUTIVE_CLICKS)) {
                updateSettings();
        }
    }
    
    private void playSound () {
        if (mSoundOnClick) {
            mAudioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
        }
    }

    private boolean movedAboveThreshold (PointF p1, PointF p2) {
        float dx= p1.x - p2.x;
        float dy= p1.y - p2.y;
        float dist= dx * dx + dy * dy;
        return (dist> mDwellAreaSquared);
    }

    /**
     * Reset dwell click internal state 
     */
    public void reset () {
        mState= State.RESET;
    }
    
    /**
     * Given the current position of the pointer calculates if needs to generate a click
     * 
     * @param pl - position of the pointer
     * @return true if click generated
     * 
     * this method is called from a secondary thread
     */
    public boolean updatePointerLocation (PointF pl) {
        boolean retval= false;
       
        // state machine
        if (mState == State.RESET) {
            /* Means previous pointer position is not valid. Change to
             * POINTER_MOVING state and allow pointer position update.
             */
            mState= State.POINTER_MOVING;
        }
        else if (mState == State.POINTER_MOVING) {
            if (!movedAboveThreshold (mPrevPointerLocation, pl)) {
                mState= State.COUNTDOWN_STARTED;
                mCountdown.reset();
            }
        }
        else if (mState == State.COUNTDOWN_STARTED) {
            if (movedAboveThreshold (mPrevPointerLocation, pl)) {
                mState= State.POINTER_MOVING;
            }
            else {
                if (mCountdown.hasFinished()) {
                    playSound ();
                    retval= true;
                    if (mConsecutiveClicks) {
                        mState= State.POINTER_MOVING;
                    }
                    else {
                        mState= State.CLICK_DONE;
                    }
                }
            }
        }
        else if (mState == State.CLICK_DONE) {
            if (movedAboveThreshold (mPrevPointerLocation, pl)) {
                mState= State.POINTER_MOVING;
            }
        }
        
        mPrevPointerLocation.set(pl);  // deep copy
        
        return retval;
    }
    
    /**
     * Get click progress percent
     * @return value in the range 0 to 100
     */
    public int getClickProgressPercent() {
        if (mState != State.COUNTDOWN_STARTED) return 0;
        
        return mCountdown.getElapsedPercent();
    }
}
