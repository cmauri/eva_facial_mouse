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
package com.crea_si.eviacam.a11yservice;

import android.content.Context;

import java.io.File;
import java.io.IOException;

/**
 * Quick and dirty lock file implementation to detect when the app closed uncleanly
 */

class LockFile {

    private static File getFile(Context c) {
        String FILE_NAME = "/eviacam.lock";
        return new File(c.getFilesDir().getAbsolutePath() + FILE_NAME);
    }

    /**
     * Check if the file exists
     *
     * @param c context
     * @return true if exists
     */
    static boolean exists(Context c) {
        return LockFile.getFile(c).exists();
    }

    /**
     * Create lock file
     */
    static void create(Context c) {
        try {
            //noinspection ResultOfMethodCallIgnored
            LockFile.getFile(c).createNewFile();
        } catch (IOException e) {
            // Do nothing
        }
    }

    /**
     * Remove lock file
     * @param c context
     *
     * Exceptions are ignored
     */
    static void delete(Context c) {
        //noinspection ResultOfMethodCallIgnored
        LockFile.getFile(c).delete();
    }
}
