package com.crea_si.eviacam.service;

import android.annotation.SuppressLint;

@SuppressLint("Assert")
class Countdown {
    private long mLastTimeStamp;
    private long mTimeToWait;
    private boolean mOneShotFinished;
    
    Countdown (long timeToWait) {
        setTimeToWait (timeToWait);
        reset();
    }    
    
    public void reset () {
        mTimeToWait= System.currentTimeMillis();
        mOneShotFinished= false;
    }
    
    public void setTimeToWait (long timeToWait) {
        assert(timeToWait>= 0);
        mTimeToWait= timeToWait;
    }
    
    public int getElapsedPercent() {
        if (mTimeToWait== 0) return 100;

        long elapsed= System.currentTimeMillis() - mLastTimeStamp;
        
        if (elapsed> mTimeToWait) return 100;
        
        return (int) ((100 * elapsed) / mTimeToWait);        
    }

    public boolean hasFinished () {
        return (System.currentTimeMillis() - mLastTimeStamp> mTimeToWait);
    }

    // if countdown finished returns true once and false for all subsequent calls
    public boolean hasFinishedOneShot () {
        if (mOneShotFinished) return false;
        if (hasFinished()) {
            mOneShotFinished= true;
            return true;
        }
        return false;
    }
}
