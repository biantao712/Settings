/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.notification;


import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.*;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;



public class CNStatusBarNotificationEntry extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener , Indexable {
    private static final String TAG = "StatusBarNotify_NF";
    private PreferenceCategory mLockScreenCategory;
    private SwitchPreference mNotificationPulse;

    private static final String KEY_LOCKSCREEN = "cn_notification_lockscreen_category";
    private static final String KEY_NOTIFICATION_PULSE = "cn_notification_lockscreen_show";
    private static final String KEY_NOTIFICATION_PUBLIC="cn_notification_lockscreen_private";
    private static final String SETTINGS_PULSE="cnsettings_notify_pulse";
    private static final String SETTINGS_LSSHOW="cnsettings_notify_lsshow";
    private static final String SETTINGS_PRIVATE="cnsettings_notify_private";

    //status bar settings +++++++++
    private PreferenceCategory mStatusBarCategory;
    private SwitchPreference mStatusBarNetspeedPreference;
    private SwitchPreference mStatusBarBatteryRatePreference;
    private ListPreference mSelectStatusBarNotificationShowPreference;
    private static final String KEY_STATUS_BAR_CATEGORY = "cn_statusbar_category";
    private static final String KEY_SELECT_STATUS_BAR_NOTIFICATION_SHOW = "cn_select_status_bar_notification_show_preference";
    private static final String KEY_STATUS_BAR_NETSPEED_SHOW = "cn_statusbar_netspeed_show";
    private static final String KEY_STATUS_BAR_BATTERY_RATE = "cn_statusbar_battery_rate_show";
    private static final String SELECT_NOTIFICATION_SHOW_STYLE = "notification_show_style_select";
    private static final String SHOW_BATTERY = "show_battery";
    private static final String SHOW_NETSPEED = "show_network_speed";
    //status bar settings ---------
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WIRELESS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.cn_statusbar_notification_entry);
        //status bar settings +++++++++
        initStatusBar();
        //status bar settings ---------
        initLockScreenStatus();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        //status bar settings +++++++++
        if(preference == mSelectStatusBarNotificationShowPreference) {
            handleSelectStatusBarNotificationShowPreferenceChanged(Integer.parseInt((String) objValue));
            return true;
        }
        //status bar settings ---------
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mStatusBarBatteryRatePreference) {
            putStatusBarBatteryRateShow(mStatusBarBatteryRatePreference.isChecked());
        } else if (preference == mStatusBarNetspeedPreference) {
            putStatusBarNetspeedShow(mStatusBarNetspeedPreference.isChecked());
        } else if (preference == mNotificationPulse) {
            setPulseStae(mNotificationPulse.isChecked());
        }
        return super.onPreferenceTreeClick(preference);
    }

    //status bar settings +++++++++
    private void initStatusBar() {
        mStatusBarCategory = (PreferenceCategory) findPreference(KEY_STATUS_BAR_CATEGORY);

        mSelectStatusBarNotificationShowPreference = (ListPreference) mStatusBarCategory.findPreference(KEY_SELECT_STATUS_BAR_NOTIFICATION_SHOW);
        int style = getSelectStatusBarNotificationShowStyle();
        if(mSelectStatusBarNotificationShowPreference != null) {
            mSelectStatusBarNotificationShowPreference.setValue(String.valueOf(style));
            mSelectStatusBarNotificationShowPreference.setSummary(mSelectStatusBarNotificationShowPreference.getEntry());
            mSelectStatusBarNotificationShowPreference.setOnPreferenceChangeListener(this);
        }

        mStatusBarNetspeedPreference = (SwitchPreference) mStatusBarCategory.findPreference(KEY_STATUS_BAR_NETSPEED_SHOW);
        if(mStatusBarNetspeedPreference != null) {
            mStatusBarNetspeedPreference.setChecked(getStatusBarNetspeedShow());
        }

        mStatusBarBatteryRatePreference = (SwitchPreference) mStatusBarCategory.findPreference(KEY_STATUS_BAR_BATTERY_RATE);
        if(mStatusBarBatteryRatePreference != null) {
            mStatusBarBatteryRatePreference.setChecked(getStatusBarBatteryRateShow());
        }
    }

    private void handleSelectStatusBarNotificationShowPreferenceChanged(int value) {
        setSelectStatusBarNotificationShowStyle(value);
        mSelectStatusBarNotificationShowPreference.setValue(String.valueOf(value));
        mSelectStatusBarNotificationShowPreference.setSummary(mSelectStatusBarNotificationShowPreference.getEntry());
    }

    private int getSelectStatusBarNotificationShowStyle() {
        return Settings.System.getInt(getContentResolver(), SELECT_NOTIFICATION_SHOW_STYLE, 1);
    }

    private void setSelectStatusBarNotificationShowStyle(int value) {
        Settings.System.putInt(getContentResolver(), SELECT_NOTIFICATION_SHOW_STYLE, value);
    }

    private boolean getStatusBarNetspeedShow() {
        return Settings.System.getInt(getContentResolver(),SHOW_NETSPEED, 0) != 0;
    }

    private void putStatusBarNetspeedShow(boolean enable) {
        Settings.System.putInt(getContentResolver(), SHOW_NETSPEED, (enable ? 1 : 0));
    }

    private boolean getStatusBarBatteryRateShow() {
        return Settings.System.getInt(getContentResolver(), SHOW_BATTERY, 1) != 0;
    }

    private void putStatusBarBatteryRateShow(boolean enable) {
        Settings.System.putInt(getContentResolver(), SHOW_BATTERY, (enable ? 1 : 0));
    }
    //status bar settings ---------
    private void initLockScreenStatus()
    {
        mLockScreenCategory = (PreferenceCategory) findPreference(KEY_LOCKSCREEN);
        mNotificationPulse = (SwitchPreference)mLockScreenCategory.findPreference(KEY_NOTIFICATION_PULSE);
        mNotificationPulse.setChecked(getPulseState());
    }
    private boolean  getPulseState()
    {
        return Settings.System.getInt(getContentResolver(),SETTINGS_PULSE,1) ==1;
    }

    private boolean getLockShowState()
    {
        return Settings.System.getInt(getContentResolver(),SETTINGS_LSSHOW,1) ==1;
    }

    private boolean getPrivateState()
    {
        return Settings.System.getInt(getContentResolver(),SETTINGS_PRIVATE,1)==1;
    }

    private void setPulseStae(boolean value)
    {
        Settings.System.putInt(getContentResolver(),SETTINGS_PULSE,(value?1:0));
    }

    private void setLockShowState(boolean value)
    {
        Settings.System.putInt(getContentResolver(),SETTINGS_LSSHOW,(value?1:0));
    }

    private void setPrivateState(boolean value)
    {
        Settings.System.putInt(getContentResolver(),SETTINGS_PRIVATE,(value?1:0));
    }

    // add to search
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

            final Resources res = context.getResources();

            // Add fragment title
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.cn_statusbar_notification_title);
            data.screenTitle = res.getString(R.string.cn_statusbar_notification_title);
            result.add(data);
            return result;
        }
    };
}
