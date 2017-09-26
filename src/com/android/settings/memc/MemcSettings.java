/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.memc;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.widget.SwitchBar;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;

public class MemcSettings extends SettingsPreferenceFragment implements
        SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceClickListener {
    private static final String TAG = MemcSettings.class.getSimpleName();
    private final String ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL_TEMP = "asus_visualmaster_pq_chip_memc_level_temp";
    private final String KEY_MEMC_HIGH = "radio_button_memc_high";
    private final String KEY_MEMC_MIDDLE = "radio_button_memc_middle";
    private final String KEY_MEMC_LOW = "radio_button_memc_low";
    private final String KEY_CINEMA_MODE = "cinema_mode";
    static final boolean DEBUG = false;

    private Context mContext;
    private SwitchBar mSwitchBar;
    private MemcRadioButtonPreference mMemcHigh;
    private MemcRadioButtonPreference mMemcMiddle;
    private MemcRadioButtonPreference mMemcLow;
    private CheckBoxPreference mCinemaMode;
    
    private int mMemcLevel;
    private int mMemcLevelTemp;
    private boolean mOnResuming;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.memc_settings);
        mMemcHigh = MemcRadioButtonPreference.class.cast(findPreference(KEY_MEMC_HIGH));
        mMemcHigh.setOnPreferenceClickListener(this);

        mMemcMiddle = MemcRadioButtonPreference.class.cast(findPreference(KEY_MEMC_MIDDLE));
        mMemcMiddle.setOnPreferenceClickListener(this);

        mMemcLow = MemcRadioButtonPreference.class.cast(findPreference(KEY_MEMC_LOW));
        mMemcLow.setOnPreferenceClickListener(this);

        mCinemaMode = CheckBoxPreference.class.cast(findPreference(KEY_CINEMA_MODE));
        mCinemaMode.setOnPreferenceClickListener(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity sa = (SettingsActivity) getActivity();
        mSwitchBar = sa.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mOnResuming = true;
        updateState();
        mSwitchBar.show(); //[TT-735805]Carol
        mOnResuming = false;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setPreferenceEnabled(isChecked);
        if (!mOnResuming) {
            if (isChecked) {
                mMemcLevel = mMemcLevelTemp;
            } else {
                mMemcLevelTemp = mMemcLevel;
                mMemcLevel = 0;
                Settings.System.putInt(getContentResolver(),
                        ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL_TEMP,mMemcLevelTemp);
            }
            Settings.System.putInt(getContentResolver(),
                    MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL,mMemcLevel);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        //mars_li
        if (preference instanceof MemcRadioButtonPreference) {
            int cinemaValue = mCinemaMode.isChecked() ? MemcSwitchHelper.PICTURE_QUALITY_CINEMA_VALUE : 0;
            mMemcHigh.setChecked(false);
            mMemcMiddle.setChecked(false);
            mMemcLow.setChecked(false);
            switch (preference.getKey()) {
                case KEY_MEMC_HIGH:
                    mMemcHigh.setChecked(true);
                    mMemcLevel = cinemaValue+MemcSwitchHelper.PICTURE_QUALITY_MEMC_HIGH;
                    break;
                case KEY_MEMC_MIDDLE:
                    mMemcMiddle.setChecked(true);
                    mMemcLevel = cinemaValue+MemcSwitchHelper.PICTURE_QUALITY_MEMC_MIDDLE;
                    break;
                case KEY_MEMC_LOW:
                    mMemcLow.setChecked(true);
                    mMemcLevel = cinemaValue+MemcSwitchHelper.PICTURE_QUALITY_MEMC_LOW;
                    break;
            }
            Settings.System.putInt(getContentResolver(),
                    MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL,mMemcLevel);
        } else if (preference instanceof CheckBoxPreference) {
            if (preference.getKey().equals(KEY_CINEMA_MODE)) {
                if(mCinemaMode.isChecked() && mMemcLevel / MemcSwitchHelper.PICTURE_QUALITY_CINEMA_VALUE == 0) {
                    mMemcLevel += MemcSwitchHelper.PICTURE_QUALITY_CINEMA_VALUE;
                } else if (!mCinemaMode.isChecked() && mMemcLevel / MemcSwitchHelper.PICTURE_QUALITY_CINEMA_VALUE == 1) {
                    mMemcLevel -= MemcSwitchHelper.PICTURE_QUALITY_CINEMA_VALUE;
                }
                Settings.System.putInt(getContentResolver(),
                        MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL,mMemcLevel);
            }
        }

        return true;
        
   }


    private void updateState() {
        mMemcLevel = Settings.System.getInt(getContentResolver(),
                MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL, MemcSwitchHelper.PICTURE_QUALITY_MEMC_DEFAULT); //default middle
        mMemcLevelTemp = Settings.System.getInt(getContentResolver(),
                ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL_TEMP, MemcSwitchHelper.PICTURE_QUALITY_MEMC_DEFAULT);
        mSwitchBar.setChecked(mMemcLevel != 0);
        mCinemaMode.setChecked(needToCheckCinemaMode(mMemcLevel == 0 ? mMemcLevelTemp : mMemcLevel));
        setRadioButtonChecked(mMemcLevel == 0 ? mMemcLevelTemp : mMemcLevel);
        setPreferenceEnabled(mMemcLevel != 0);
    }

    private boolean needToCheckCinemaMode (int memcLevel) {
        return memcLevel / MemcSwitchHelper.PICTURE_QUALITY_CINEMA_VALUE == 1;
    }

    private void setPreferenceEnabled (boolean enable) {
        mMemcHigh.setEnabled(enable);
        mMemcMiddle.setEnabled(enable);
        mMemcLow.setEnabled(enable);
        mCinemaMode.setEnabled(enable);
    }

    private void setRadioButtonChecked (int memcLevel) {
        mMemcHigh.setChecked(false);
        mMemcMiddle.setChecked(false);
        mMemcLow.setChecked(false);
        memcLevel %= MemcSwitchHelper.PICTURE_QUALITY_CINEMA_VALUE;
        switch (memcLevel) {
        case MemcSwitchHelper.PICTURE_QUALITY_MEMC_HIGH:
            mMemcHigh.setChecked(true);
            break;
        case MemcSwitchHelper.PICTURE_QUALITY_MEMC_MIDDLE:
            mMemcMiddle.setChecked(true);
            break;
        case MemcSwitchHelper.PICTURE_QUALITY_MEMC_LOW:
            mMemcLow.setChecked(true);
            break;
        }
    }
    
    @Override
    protected int getMetricsCategory() {
        return 1;
    }
}

