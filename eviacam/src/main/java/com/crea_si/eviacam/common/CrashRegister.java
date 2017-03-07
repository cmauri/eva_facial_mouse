/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015-17 Cesar Mauri Loba (CREA Software Systems)
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
package com.crea_si.eviacam.common;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Utility to record the last crash event
 */
public class CrashRegister {

    @SuppressWarnings("FieldCanBeLocal")
    private static long RECENT_CRASH_TIME= 10000; // milliseconds

    /**
     * Return the time since the last app crash was recorded
     * @param c context
     * @return time since last crash (in milliseconds) or -1 if no crash recorded
     */
    private static long timeSinceLastCrash(@NonNull Context c) {
        File f= getFile(c);

        if (!f.exists()) return -1;

        long fileTime= f.lastModified();
        Date nowDate= new Date();
        long nowTime= nowDate.getTime();
        return nowTime - fileTime;
    }

    /**
     * Check if the app crashed recently
     *
     * @param c context
     * @return true if crash moments ago (defined by an internal constant)
     */
    static public boolean crashedRecently(@NonNull Context c) {
        long t= timeSinceLastCrash(c);
        return (t>-1 && t<RECENT_CRASH_TIME);
    }

    /**
     * Record the app crash event
     *
     * @param c context
     */
    static void recordCrash(@NonNull Context c) {
        try {
            clearCrash(c);
            //noinspection ResultOfMethodCallIgnored
            getFile(c).createNewFile();
        } catch (IOException e) {
            // Do nothing
        }
    }

    /**
     * Remove lock file
     * @param c context
     */
    public static void clearCrash(@NonNull Context c) {
        //noinspection ResultOfMethodCallIgnored
        getFile(c).delete();
    }

    /**
     * Return the File to record crashes
     * @param c context
     * @return the file object
     */
    @NonNull
    private static File getFile(@NonNull Context c) {
        String FILE_NAME = "/eviacam.crashed";
        return new File(c.getFilesDir().getAbsolutePath() + FILE_NAME);
    }
}
