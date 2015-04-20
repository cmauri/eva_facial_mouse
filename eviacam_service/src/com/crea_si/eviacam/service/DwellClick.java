package com.crea_si.eviacam.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.PointF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.preference.PreferenceManager;

class DwellClick implements OnSharedPreferenceChangeListener {
    /**
     * Enums and constants
     */
    private enum State {
        DISABLED, POINTER_MOVING, COUNTDOWN_STARTED, CLICK_DONE
    }
    
    private final int DWELL_TIME_DEFAULT;
    private final int DWELL_AREA_DEFAULT;
    private final boolean SOUND_ON_CLICK_DEFAULT;
    
    private static final String KEY_DWELL_TIME= "dwell_time";
    private static final String KEY_DWELL_AREA= "dwell_area";
    private static final String KEY_SOUND_ON_CLICK= "sound_on_click";
  
    // delegate to measure elapsed time
    private Countdown mCountdown;
    
    // reference to shared preferences pool
    private SharedPreferences mSharedPref;
    
    // current dwell click state
    private State mState= State.POINTER_MOVING;

    // modified from the main thread (enable/disable methods)
    // used to modify state without using synchronization
    private boolean mRequestEnabled= true;
    
    // dwell area tolerance. stored squared to avoid sqrt 
    // for each updatePointerLocation call
    private float mDwellAreaSquared;    
    
    // whether to play a sound when action performed
    private boolean mSoundOnClick;
    
    // audio manager for FX notifications
    AudioManager mAudioManager;
    
    // to remember previous pointer location and measure travelled distance
    private PointF mPrevPointerLocation= null;

    
    public DwellClick(Context c) {
        // get constants from resources
        Resources r= c.getResources();
        DWELL_TIME_DEFAULT= r.getInteger(R.integer.dwell_time_default) * 100;
        DWELL_AREA_DEFAULT= r.getInteger(R.integer.dwell_area_default);
        SOUND_ON_CLICK_DEFAULT= r.getBoolean(R.bool.sound_on_click_default);
        
        mCountdown= new Countdown(DWELL_TIME_DEFAULT);
        
        mAudioManager= (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        
        // shared preferences
        mSharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        
        // register preference change listener
        mSharedPref.registerOnSharedPreferenceChangeListener(this);
        
        readSettings();
    }
    
    private void readSettings() {
        // get values from shared resources
        int dwellTime= mSharedPref.getInt(KEY_DWELL_TIME, DWELL_TIME_DEFAULT) * 100;
        mCountdown.setTimeToWait(dwellTime);
        int dwellArea= mSharedPref.getInt(KEY_DWELL_AREA, DWELL_AREA_DEFAULT);
        mDwellAreaSquared= dwellArea * dwellArea;
        mSoundOnClick= mSharedPref.getBoolean(KEY_SOUND_ON_CLICK, SOUND_ON_CLICK_DEFAULT);
    }
    
    public void cleanup() {
        mSharedPref.unregisterOnSharedPreferenceChangeListener(this);        
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key.equals(KEY_DWELL_TIME) || key.equals(KEY_DWELL_AREA) ||
            key.equals(KEY_SOUND_ON_CLICK)) {
                readSettings();
        }
    }
       
    public void enable () {
        mRequestEnabled= true;
    }
    
    public void disable () {
        mRequestEnabled= false;
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
     * Given the current position of the pointer calculates if needs to generate a click
     * 
     * @param pl - position of the pointer
     * @return true if click generated
     * 
     * this method is called from a secondary thread
     */
    public boolean updatePointerLocation (PointF pl) {
        boolean retval= false;
        
        if (mPrevPointerLocation== null) {
            mPrevPointerLocation= new PointF();
            mPrevPointerLocation.set(pl);
            return retval;
        }
       
        // check if need to enable/disable 
        if (mState == State.DISABLED) {
            if (mRequestEnabled) {
                mState = State.POINTER_MOVING;
            }
        }
        else {
            if (!mRequestEnabled) {
                mState = State.DISABLED;
            }
        }
       
        // state machine
        if (mState == State.POINTER_MOVING) {
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
                    mState= State.CLICK_DONE;
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
