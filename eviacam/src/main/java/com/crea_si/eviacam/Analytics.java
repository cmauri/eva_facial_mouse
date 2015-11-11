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
package com.crea_si.eviacam;

import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

/**
 * Google analytics stuff
 */
public class Analytics {

    private static final String SERVICE_CATEGORY = "Service";
    private static final String TRACKING_ID = "UA-69958057-1";

    /* Singleton instance */
    private static Analytics sInstance;

    private final Context mContext;
    private final GoogleAnalytics mAnalytics;

    /*
        Trackers
     */
    private final Tracker mServiceTracker;
    private final Tracker mUITracker;

    /* Used to compute time between events */
    private long mStartTime;

    /* Create the Analytics instance. Should be called only once */
    static void init (Context c) {
        sInstance= new Analytics(c);
    }

    /* Get the singleton instance. Might return null */
    public static Analytics get() { return sInstance; }

    public void trackStartService() {
        mStartTime= System.currentTimeMillis();
        mServiceTracker.send(new HitBuilders.EventBuilder()
            .setCategory(SERVICE_CATEGORY)
            .setAction("start")
            .setLabel("service")
            .build());
    }

    // Reference: http://stackoverflow.com/questions/30239406/googleanalytics-track-time-between-events
    public void trackStopService() {
        final long elapsed= System.currentTimeMillis() - mStartTime;
        mServiceTracker.send(new HitBuilders.EventBuilder()
                .setCategory(SERVICE_CATEGORY)
                .setAction("stop")
                .setLabel("service")
                .setValue(elapsed / 1000)
                .setCustomMetric(1, elapsed / 1000)
                .build());
    }

    /* Constructor */
    private Analytics(Context c) {
        mContext= c;

        mAnalytics= GoogleAnalytics.getInstance(c);
        mAnalytics.setDryRun(false);
        mAnalytics.setLocalDispatchPeriod((BuildConfig.DEBUG ? 15 : 1800));

        /* Uncomment to avoid sending hits to analytics in debug mode */
        //if (BuildConfig.DEBUG) { mAnalytics.setAppOptOut(true); }


        /*
         *  UI auto activity tracker
         */
        mUITracker = mAnalytics.newTracker(TRACKING_ID);
        mUITracker.enableExceptionReporting(false);
        mUITracker.enableAdvertisingIdCollection(false);
        mUITracker.enableAutoActivityTracking(true);

        /*
         *  Service tracker
         */
        mServiceTracker = mAnalytics.newTracker(TRACKING_ID);
        mServiceTracker.enableExceptionReporting(true);
        mServiceTracker.enableAdvertisingIdCollection(false);
        mServiceTracker.enableAutoActivityTracking(false);
        /*
        String metricValue = SOME_METRIC_VALUE_SUCH_AS_123_AS_STRING;
        mServiceTracker.set(Fields.customMetric(1), metricValue);
        */
    }
}
