/*
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crea_si.eviacam.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * AIDL for the gamepad mode params
 */
public final class GamepadParams implements Parcelable {
    public int mData;

    public int describeContents() { 
        return 0;
    } 

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mData);
    } 

    public static final Parcelable.Creator<GamepadParams> CREATOR
            = new Parcelable.Creator<GamepadParams>() {
        public GamepadParams createFromParcel(Parcel in) {
            return new GamepadParams(in);
        } 

        public GamepadParams[] newArray(int size) {
            return new GamepadParams[size];
        } 
    }; 
     
    private GamepadParams(Parcel in) {
        mData = in.readInt();
    }
    
    public GamepadParams() { }
}
