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

public class Blink {
    private final long mPeriod;
    private long mStartTime= 0;

    Blink(int period) {
        mPeriod= period;
    }

    /**
     * Start. Do nothing if already started.
     */
    public void start() {
        if (mStartTime!= 0) return;
        mStartTime= System.currentTimeMillis();
    }

    public void stop() {
        mStartTime= 0;
    }

    public boolean getState() {
        if (mStartTime== 0) return true;
        return (((System.currentTimeMillis() - mStartTime) / mPeriod) & 1) == 0;
    }
}
