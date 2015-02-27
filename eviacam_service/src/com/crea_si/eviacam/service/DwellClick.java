package com.crea_si.eviacam.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.PointF;
import android.preference.PreferenceManager;

class DwellClick implements OnSharedPreferenceChangeListener {
    // constants
    private final int DWELL_TIME_DEFAULT;
    private final int DWELL_AREA_DEFAULT;
    private final boolean SOUND_ON_CLICK_DEFAULT;
    
    private static final String KEY_DWELL_TIME= "dwell_time";
    private static final String KEY_DWELL_AREA= "dwell_area";
    private static final String KEY_SOUND_ON_CLICK= "sound_on_click";
    
    // attributes
    private boolean mEnabled= true;
    private float mDwellAreaSquared;
    private Countdown mCountdown;
    private boolean mSoundOnClick;
    private PointF mPrevPointerLocation= null;
    private SharedPreferences mSharedPref;
    
    public DwellClick(Context c) {
        // get constants from resources
        Resources r= c.getResources();
        DWELL_TIME_DEFAULT= r.getInteger(R.integer.dwell_time_default) * 100;
        DWELL_AREA_DEFAULT= r.getInteger(R.integer.dwell_area_default);
        SOUND_ON_CLICK_DEFAULT= r.getBoolean(R.bool.sound_on_click_default);
        
        mCountdown= new Countdown(DWELL_TIME_DEFAULT);
        
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
       
    /*
    private void enable () {
        mPrevPointerLocation= null;
        mEnabled= true;
    }
    
    private void disable () {
        mEnabled= false;
    }
    */
    
    private void performClick () {
        EVIACAM.debug("Click performed");
    }
    
    public void updatePointerLocation (PointF pl) {
        if (!mEnabled) return;
        
        // compute cursor displacement
        if (mPrevPointerLocation != null) {
            float dx= pl.x - mPrevPointerLocation.x;
            float dy= pl.y - mPrevPointerLocation.y;
            
            double displacementSquared= dx * dx + dy * dy;
            
            if (displacementSquared> mDwellAreaSquared) {
                // pointer moving
                mCountdown.reset();
            }
            else {
                // pointer static
                if (mCountdown.hasFinishedOneShot()) {
                    performClick();
                }
            }
        }
        
        mPrevPointerLocation= pl;
    }
}
