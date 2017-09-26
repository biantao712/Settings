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

package com.android.settings;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.*;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.MetricsProto;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;

import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.app.ActivityManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

public class MoreSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "MoreSettings";
    private SwitchPreference mNotificationPulse;
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_THREE="key_category_for_floatingdock";
    private static final String KEY_DEVELOP_SETTING = "develop_setting";
    private static final String KEY_EASY_MODE = "easy_launcher_setting";

    private PreferenceScreen mDevelopPreferenceScreen;
    private SharedPreferences mDevelopmentPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WIRELESS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        addPreferencesFromResource(R.xml.more_settings);

        mDevelopPreferenceScreen = (PreferenceScreen)findPreference(KEY_DEVELOP_SETTING);
        final ContentResolver resolver = getActivity().getContentResolver();
        mNotificationPulse = (SwitchPreference) findPreference(KEY_NOTIFICATION_PULSE);
        try {
            int i = android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE);
            Log.d("blenda", "get NOTIFICATION_LIGHT_PULSE: "+i);
            mNotificationPulse.setChecked(android.provider.Settings.System.getInt(resolver,
                    android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
            mNotificationPulse.setOnPreferenceChangeListener(this);

            File file = new File("/sys/class/leds/red/brightness");
            if(!file.exists()) {
                removePreference(KEY_NOTIFICATION_PULSE);
            }
        } catch (android.provider.Settings.SettingNotFoundException snfe) {
            Log.e(TAG, android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            removePreference(KEY_NOTIFICATION_PULSE);
        }
        
        if(!showFloatingDockOption(getActivity())){
            removePreference("key_floatingdock");
            android.support.v7.preference.PreferenceCategory mCategoryThree= (android.support.v7.preference.PreferenceCategory) findPreference(KEY_THREE);
            if(mCategoryThree!=null){
                android.support.v7.preference.Preference mPreferenceFloatingDock= mCategoryThree.findPreference("key_floatingdock");
                if(mPreferenceFloatingDock!=null){
                    Log.i(TAG, "remove floating dock option");
                    mCategoryThree.removePreference(mPreferenceFloatingDock);
                }else{
                    Log.i(TAG, "mPreferenceFloatingDock==null");
                }
            }else{
                Log.i(TAG, "mCategoryThree==null");
            }
        }

        removePreferenceForCnRequest();

        mDevelopmentPreferences = ((SettingsActivity)getActivity()).getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);
        updateDevPreference();
        removeEasyMode();
    }

    @Override
    public void onStart() {
        super.onStart();
        mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateDevPreference();
            }
        };
        mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);
        mDevelopmentPreferencesListener = null;
    }
    private void updateDevPreference(){
        if(getActivity() == null){
            return;
        }
        final UserManager um = UserManager.get(getActivity());
        final boolean showDev = mDevelopmentPreferences.getBoolean(
                DevelopmentSettings.PREF_SHOW, android.os.Build.TYPE.equals("eng"))
                && !um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);

        if (!showDev){
            PreferenceScreen p= (PreferenceScreen)findPreference(KEY_DEVELOP_SETTING);
            PreferenceCategory group = (PreferenceCategory)findPreference("last_category");
            if (p != null)
                group.removePreference(p);
        } else{
            PreferenceScreen p= (PreferenceScreen)findPreference(KEY_DEVELOP_SETTING);
            PreferenceCategory group = (PreferenceCategory)findPreference("last_category");
            if (p == null)
                group.addPreference(mDevelopPreferenceScreen);
        }
    }
    private void removePreferenceForCnRequest(){
       PreferenceCategory group = (PreferenceCategory)getPreferenceScreen().findPreference("more_settings_security_group_key");
       if(group != null){
           Preference pref = group.findPreference("call_setting");
           if(pref != null){
                Log.i(TAG, "removePreference " + pref.getKey());
		        group.removePreference(pref);
           }
       }
	}

    private void removeEasyMode(){
        PreferenceCategory group = (PreferenceCategory)getPreferenceScreen().findPreference("more_settings_security_group_key");
        if(group != null){
            Preference pref = group.findPreference(KEY_EASY_MODE);
            if(pref != null){
                Log.i(TAG, "removePreference " + pref.getKey());
                group.removePreference(pref);
            }
        }
    }
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mNotificationPulse) {
            boolean value = mNotificationPulse.isChecked();
            android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE,
                    ((Boolean)objValue) ? 1 : 0);
            try {
                int i = android.provider.Settings.System.getInt(getContentResolver(),
                        android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE);
                Log.d("blenda", "get NOTIFICATION_LIGHT_PULSE: "+i);
            } catch (android.provider.Settings.SettingNotFoundException snfe) {
                Log.e(TAG, android.provider.Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
        }

        return true;
    } 
  
    private static boolean showFloatingDockOption(Context context){
        Intent intent = new Intent("asus.action.start.cnfloatingdock");
        intent.setClassName("com.asus.cnfloatingdock",
                "com.asus.cnfloatingdock.UI.MainActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        boolean isAvailable = (list != null && list.size() > 0);
        if(isAvailable){
            Log.i(TAG, "show floating dock option");
            return true;
        }else{
            Log.i(TAG, "hide floating dock option");
            return false;
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.more_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();

                    UserManager um = UserManager.get(context);

                    SharedPreferences mDevelopmentPreferences = context.getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE);
                    final boolean showDev = mDevelopmentPreferences.getBoolean(
                            DevelopmentSettings.PREF_SHOW, android.os.Build.TYPE.equals("eng"))
                            && !um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);

                    if (!showDev) {
                        result.add(KEY_DEVELOP_SETTING);

                    }
                    File file = new File("/sys/class/leds/red/brightness");
                    if(!file.exists()) {
                        result.add(KEY_NOTIFICATION_PULSE);
                    }
                    result.add("call_setting");
                    if(!showFloatingDockOption(context)){
                        result.add("key_floatingdock");
                    }
                    result.add(KEY_EASY_MODE);
                    return result;
                }
            };
}
