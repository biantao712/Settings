/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.display;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.asus.cncommonres.AsusButtonBar;
import com.android.settings.SettingsActivity;
import android.content.res.Resources;

import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceCategory;
import com.android.settings.SeekBarPreference;
import android.util.Log;
import android.provider.Settings;
import android.provider.Settings.System;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;

public class DisplayBrightnessPreference  extends SettingsPreferenceFragment implements 
                 Preference.OnPreferenceChangeListener {

    private static final String  DISPLAY_BRIGHTNESS_SWITCH_KEY = "display_brightness_switch_key";
    private static final String DISPLAY_MODITY_BRIGHTNESS_KEY = "display_modify_brightness_key";

    private SwitchPreference mBrightnessSwitchPreference;
    private BrightnessSeekBarPreference mBrightnessSeekBarPreference; 

    private ContentObserver mBrightnessModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessModeChanged();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPreferenceHierarchy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
     //   createPreferenceHierarchy();

        boolean checked =
            getBrightnessMode() == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        mBrightnessSwitchPreference.setChecked(checked);
 
        getContext().getContentResolver().registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true,
            mBrightnessModeObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContext().getContentResolver().unregisterContentObserver(mBrightnessModeObserver);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RUNNING_SERVICES;
    }

    private int getBrightnessMode() {
        return Settings.System.getInt(getContext().getContentResolver(),
                                      Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    private void onBrightnessModeChanged() {
        boolean checked =
            getBrightnessMode() == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        mBrightnessSwitchPreference.setChecked(checked);
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.display_settings_brightness_preference);
        root = getPreferenceScreen();
        mBrightnessSwitchPreference = (SwitchPreference) findPreference(DISPLAY_BRIGHTNESS_SWITCH_KEY);
        mBrightnessSwitchPreference.setOnPreferenceChangeListener(this);
        mBrightnessSeekBarPreference = (BrightnessSeekBarPreference) findPreference(DISPLAY_MODITY_BRIGHTNESS_KEY);
        mBrightnessSeekBarPreference.setOnPreferenceChangeListener(this);
       // initPreferences();
        return root;
    }

     @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final boolean isChecked = (Boolean)objValue;
        if (DISPLAY_BRIGHTNESS_SWITCH_KEY.equals(preference.getKey())) {
            mBrightnessSeekBarPreference.onSwitchChanged(isChecked);
        } else if (DISPLAY_MODITY_BRIGHTNESS_KEY.equals(preference.getKey())) {
        }

        return true;
    }
}
