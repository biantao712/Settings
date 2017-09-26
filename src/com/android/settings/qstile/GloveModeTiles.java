/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.android.settings.qstile;


import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;


public class GloveModeTiles {
    /**
     * Tile to control the "GloveMode"
     */
    public static class GloveMode extends TileService {
        private final String TAG = "GloveModeTiles";
        private Handler mHandler = new Handler();
        private GloveModeTileObserver mGloveModeTileObserver;

        @Override
        public void onStartListening() {
            super.onStartListening();
            refresh();
            Log.d(TAG, "onStartListening");
            if (mGloveModeTileObserver == null) {
                mGloveModeTileObserver = new GloveModeTileObserver(mHandler);
                getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(Settings.System.GLOVE_MODE), true,
                        mGloveModeTileObserver, UserHandle.USER_ALL);
                Log.i(TAG, "register glove tile observer");
            }
        }
        @Override
        public void onTileRemoved() {
            Log.d(TAG, "onTileRemoved");
            if (mGloveModeTileObserver != null) {
                getContentResolver().unregisterContentObserver(mGloveModeTileObserver);
                Log.i(TAG, "unregister glove tile observer");
            }
        }
        @Override
        public void onClick() {
            Settings.System.putInt(getContentResolver(),
                     Settings.System.GLOVE_MODE, (getQsTile().getState() == Tile.STATE_INACTIVE) ? 1 : 0);
        }

        private void refresh() {
            boolean enable = Settings.System.getInt(getContentResolver(), Settings.System.GLOVE_MODE, 0) == 1;
            getQsTile().setState(enable ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            getQsTile().updateTile();
            Log.i(TAG,"glove tile enable :"+ enable);
        }
        @Override
        public void onDestroy() {
            Log.i(TAG, "onDestroy");
            if (mGloveModeTileObserver != null) {
                getContentResolver().unregisterContentObserver(mGloveModeTileObserver);
                Log.i(TAG, "unregister glove tile observer");
            }
            super.onDestroy();
        }
        private class GloveModeTileObserver extends ContentObserver {
            private final String TAG = "GloveModeTileObserver";
            public GloveModeTileObserver(Handler h) {
                super(h);
            }

            @Override
            public void onChange(boolean selfChange) {
                Log.i(TAG, "onChange");
                refresh();
            }
        }
    }
}
