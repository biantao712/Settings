package com.asus.settings.lockscreen.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.SearchIndexableResource;
import com.android.settings.R;
import android.widget.Toast;
import com.android.settings.SettingsPreferenceFragment;
import com.asus.settings.lockscreen.AsusLSUtils;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.provider.SearchIndexableResource;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import com.android.settings.AsusSecuritySettings;
import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import android.widget.Switch;

import com.asus.settings.lockscreen.ui.LockscreenWeatherColorSwitchPreference;
import com.asus.settings.lockscreen.ui.LockscreenHideMessageSwitchPreference;
import com.asus.settings.lockscreen.ui.LockscreenMagzineSwitchPreference;
import com.asus.settings.lockscreen.ui.StyleSetting;
import com.asus.settings.lockscreen.ui.TypeItem;


//import com.asus.screenlock.setting.MainSetting;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class LockscreenSettingsDateTimePreference extends AsusSecuritySettings {
    private static final String TAG = "LSSettingsDateTimeNF";
    private static final String KEY_DATE_TIME_FORMAT = "date_time_format";
    private static final String KEY_WEATHER_COLOR = "weather_color";
    private static final String KEY_HIDE_MSEEAGE = "hide_message";
    private static final String KEY_MAGZINE = "magzine";
    private static final String KEY_MAGZINE_SETTINGS = "magzine_settings";
    private static final String ACTION_WEATHER_STYLE_CHANGE = "action.com.vlife.magazine.WEATHER_STYLE_CHANGE";
    private Preference mDateTimeFormatPref = null;
    private LockscreenWeatherColorSwitchPreference mWeatherColorSwitchPref = null;
    private LockscreenHideMessageSwitchPreference mHideMessageSwitchPref = null;
    private LockscreenMagzineSwitchPreference mMagzineSwitchPref = null;
    private Preference mMagzineSettingsPref = null;

    private boolean mIsWeatherColor;
    private boolean mIsHideMessage;
    private boolean mIsMagzine;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"oncreate");
        super.onCreate(savedInstanceState);
        Log.d(TAG,"After super oncreate");
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        try {
            Log.d(TAG, "enter onCreateView");
            View mView = inflater.inflate(R.layout.view_screenlock_setting, container, false);
            return mView;
        }
        catch (Exception ee)
        {
            Log.d(TAG,"Error : " + ee);
            return null;
        }
    }


    @Override
    public void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();

        if(mDateTimeFormatPref != null)
        {
            Log.d(TAG,"set mDateTimeFormatPref changeListener");
//                mDateTimeFormatPref.setSwitchChecked(mIsWeatherColor);
//                mDateTimeFormatPref.setOnSwitchCheckedChangeListener(this);
        }

        if(mWeatherColorSwitchPref != null)
        {
            Log.d(TAG,"set mWeatherColorSwitchPref changeListener");
//            mWeatherColorSwitchPref.setSwitchChecked(mIsWeatherColor);
//            mWeatherColorSwitchPref.setOnSwitchCheckedChangeListener(this);
        }

        if(mHideMessageSwitchPref != null)
        {
            Log.d(TAG,"set mWeatherColorSwitchPref changeListener");
//            mHideMessageSwitchPref.setSwitchChecked(mIsHideMessage);
//            mHideMessageSwitchPref.setOnSwitchCheckedChangeListener(this);
        }

        if(mMagzineSwitchPref != null)
        {
            Log.d(TAG,"set mWeatherColorSwitchPref changeListener");
//            mMagzineSwitchPref.setSwitchChecked(mIsMagzine);
//            mMagzineSwitchPref.setOnSwitchCheckedChangeListener(this);
//            mMagzineSettingsPref.setEnabled(mIsMagzine);
        }

        if(mMagzineSettingsPref != null)
        {
            Log.d(TAG,"set mMagzineSettingsPref changeListener");
//                mDateTimeFormatPref.setSwitchChecked(mIsWeatherColor);
//                mDateTimeFormatPref.setOnSwitchCheckedChangeListener(this);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG,"onpause");
        super.onPause();
        Log.d(TAG,"After super.onPause");
    }

    @Override
    protected int getMetricsCategory() {
        Log.d(TAG,"getMetricsCategory");
        // TODO Auto-generated method stub
        return MetricsEvent.MAIN_SETTINGS;
    }




}
