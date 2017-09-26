/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.content.Context;
import android.util.Log;

/**
 * Suleman add
 */
public final class DiscoverableEnablerManager {
    private static final String TAG = "DiscoverableEnablerManager";

    /** Singleton instance. */
    private static DiscoverableEnablerManager sInstance;
    private BluetoothDiscoverableEnabler  mDiscoverableEnabler;
    private final Context mContext;


    public static synchronized DiscoverableEnablerManager getInstance(Context context) {
         if (sInstance == null) {
            
            // This will be around as long as this process is
            Context appContext = context.getApplicationContext();
            sInstance = new DiscoverableEnablerManager(appContext);
        }

        return sInstance;
    }

    private DiscoverableEnablerManager(Context context) {
        mContext = context;
    }

    public void setDiscoverableEnabler(BluetoothDiscoverableEnabler discoverableEnabler) {
        mDiscoverableEnabler = discoverableEnabler;
    }

    public BluetoothDiscoverableEnabler getDiscoverableEnabler() {
        return mDiscoverableEnabler;
    }
}
