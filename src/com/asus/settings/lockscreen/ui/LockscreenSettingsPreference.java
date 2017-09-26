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
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
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
import android.os.UserHandle;

import com.asus.settings.lockscreen.ui.LockscreenWeatherColorSwitchPreference;
import com.asus.settings.lockscreen.ui.LockscreenMagzineSwitchPreference;
import com.asus.settings.lockscreen.ui.DateTimeWeatherFormatActivity;

import android.support.v14.preference.SwitchPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.res.Resources;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import android.content.BroadcastReceiver;


public class LockscreenSettingsPreference extends AsusSecuritySettings
        implements CompoundButton.OnCheckedChangeListener, Indexable {
    private static final String TAG = "LSSettings_NF";
    private static final String KEY_LOCKSCREEN_INFO = "lockscreen_info";
    private static final String KEY_DEVICE_INFO = "device_owner_info";
    private static final String KEY_DATE_TIME_FORMAT = "date_time_format";
    private static final String KEY_WEATHER_COLOR = "weather_color";
    private static final String KEY_LOCKSCREEN_SHOW_NOTIFICATIONS = "lockscreen_show_notifications";        // this switch is copied from notification and status bar.
    private static final String KEY_MAGZINE_INFO = "magazine_info";
    private static final String KEY_MAGZINE = "magzine";
    private static final String KEY_MAGZINE_SETTINGS = "magzine_settings";
    private static final String ACTION_UPDATE_DEVICE_OWNER_INFO = "ACTION_UPDATE_DEVICE_OWNER_INFO";

    // +++++++++these string is used for communicate with System UI and LockScreen.
    public static final String SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE = "SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE";
    public static final String SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE = "SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE";
    private static final String SETTINGS_PULSE = "cnsettings_notify_pulse";
    // ---------these string is used for communicate with System UI and LockScreen.

    private PreferenceCategory mLockScreenInfoCategory;
    private Preference mDeviceOwnerInfoPref = null;
    private SwitchPreference mLockscreenShowNotificationsPref = null;
    private LockscreenWeatherColorSwitchPreference mWeatherColorSwitchPref = null;
    private PreferenceCategory mMagazineInfoCategory;
    private LockscreenMagzineSwitchPreference mMagzineSwitchPref = null;
    private Preference mMagzineSettingsPref = null;

    private boolean mIsWeatherColor = false;
    private boolean mIsLockscreenShowNotifications = false;
    private boolean mIsMagzine = false;

    private IntentFilter mIntentFilter;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Enter onReceiver");
            final String action = intent.getAction();

            if (action.equals(ACTION_UPDATE_DEVICE_OWNER_INFO)) {
                Log.d(TAG,"action update owner info");
                updateDeviceOwnerInfo();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
        } catch (Exception ee) {
            Log.d(TAG, "Exception in get settings value");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    protected PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = super.createPreferenceHierarchy();
        addPreferencesFromResource(R.xml.asus_lock_screen_settings_nf);
        mLockScreenInfoCategory = (PreferenceCategory)root.findPreference(KEY_LOCKSCREEN_INFO);

        mDeviceOwnerInfoPref = (Preference)mLockScreenInfoCategory.findPreference(KEY_DEVICE_INFO);
        mWeatherColorSwitchPref = (LockscreenWeatherColorSwitchPreference) mLockScreenInfoCategory.findPreference(KEY_WEATHER_COLOR);
        mLockscreenShowNotificationsPref = (SwitchPreference) mLockScreenInfoCategory.findPreference(KEY_LOCKSCREEN_SHOW_NOTIFICATIONS);

        mMagazineInfoCategory = (PreferenceCategory)root.findPreference(KEY_MAGZINE_INFO);
        mMagzineSwitchPref = (LockscreenMagzineSwitchPreference) mMagazineInfoCategory.findPreference(KEY_MAGZINE);
        mMagzineSettingsPref = (Preference)mMagazineInfoCategory.findPreference(KEY_MAGZINE_SETTINGS);
        initSwitchValues();
        initViews();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"enter onresume");
        try {
            if (mWeatherColorSwitchPref != null) {
                mWeatherColorSwitchPref.setSwitchChecked(mIsWeatherColor);
                mWeatherColorSwitchPref.setOnSwitchCheckedChangeListener(this);
            }

            if (mMagzineSwitchPref != null) {
                mMagzineSwitchPref.setSwitchChecked(mIsMagzine);
                mMagzineSwitchPref.setOnSwitchCheckedChangeListener(this);
                mMagzineSettingsPref.setEnabled(mIsMagzine);
            }
            Log.d(TAG,"register receiver of device owner info.");
            mIntentFilter = new IntentFilter(ACTION_UPDATE_DEVICE_OWNER_INFO);
            getActivity().registerReceiver(mReceiver, mIntentFilter);
        } catch (Exception ee) {
            Log.d(TAG, "Error : onResume " + ee);
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"enter onPause");
        try{
            Log.d(TAG,"unregister receiver of device owner info.");
            getActivity().unregisterReceiver(mReceiver);
        }
        catch(Exception ee)
        {
            Log.d(TAG,"Error onPause : " + ee);
        }

    }

    @Override
    public void onStart(){
        super.onStart();
        Log.d(TAG,"enter onstart");
        try{
            updateDeviceOwnerInfo();
        }
        catch(Exception ee)
        {
            Log.d(TAG,"Error in onStart : " + ee);
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.d(TAG,"enter on stop");
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();

        if (preference == mLockscreenShowNotificationsPref) {
            boolean value = mLockscreenShowNotificationsPref.isChecked();
            Log.d(TAG, "is lockscreen show notification clicked");
            Settings.System.putInt(getContentResolver(), SETTINGS_PULSE, (value ? 1 : 0));
        } else if (preference == mDeviceOwnerInfoPref) {
            new AsusOwnerInfoDialogFragment().show(getFragmentManager(), "update device owner info");
            Log.d(TAG, "New Preference to update deivce owner info.");
        } else if (KEY_WEATHER_COLOR.equals(key)) {
            Log.d(TAG, "KEY_WEATHER_COLOR reverse checked");
            mWeatherColorSwitchPref.setSwitchChecked(!mWeatherColorSwitchPref.isChecked());
        } else if (KEY_MAGZINE.equals(key)) {
            Log.d(TAG, "KEY_MAGZINE reverse checked");
            mMagzineSwitchPref.setSwitchChecked(!mMagzineSwitchPref.isChecked());
        } else {
            Log.d(TAG, "not handle this , transfer to super");
            return super.onPreferenceTreeClick(preference);
        }

        return true;
    }

    @Override
    protected int getMetricsCategory() {
        Log.d(TAG, "getMetricsCategory");
        // TODO Auto-generated method stub
        return MetricsEvent.MAIN_SETTINGS;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Enter fucntion on checked changed status is: " + isChecked);
        int id = buttonView.getId();
        if (id == R.id.switchweathercolor) {
            Log.d(TAG, "button switchweathercolor");
            if (buttonView instanceof Switch) {
                if (mIsWeatherColor != isChecked) {
                    Log.d(TAG, "Update status to " + isChecked);
                    mIsWeatherColor = isChecked;
                    int value = (mIsWeatherColor ? 1 : 0);
                    Settings.System.putInt(getActivity().getContentResolver(),
                            SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE, value);
                    Log.d(TAG, "set weather smart color to SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE " + value);
                    mWeatherColorSwitchPref.setSwitchChecked(mIsWeatherColor);
                }
            }
        } else if (id == R.id.switchmagzine) {
            Log.d(TAG, "button switchmagzine");
            if (buttonView instanceof Switch) {
                if (mIsMagzine != isChecked) {
                    Log.d(TAG, "Update status to " + isChecked);
                    mIsMagzine = isChecked;

                    int value = (mIsMagzine ? 1 : 0);
                    Settings.System.putInt(getActivity().getContentResolver(),
                            SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE, value);
                    Log.d(TAG, "set lockscreen magazine enable to SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE " + value);

                    mMagzineSwitchPref.setSwitchChecked(mIsMagzine);
                    mMagzineSettingsPref.setEnabled(mIsMagzine);
                }
            }
        }
    }

    // add to search
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            final Resources res = context.getResources();

            // Add fragment title
            SearchIndexableRaw data = new SearchIndexableRaw(context);
            data.title = res.getString(R.string.asus_lockscreen_settings_title_nf);
            data.screenTitle = res.getString(R.string.asus_lockscreen_settings_title_nf);
            result.add(data);
            return result;
        }
    };

    public void initSwitchValues() {
        try {
            int value = Settings.System.getInt(getContentResolver(), SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE, 0);
            Log.d(TAG, "get weather smart color value : " + value);
            mIsWeatherColor = (value == 0 ? false : true);

            value = Settings.System.getInt(getContentResolver(), SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE, 0);
            Log.d(TAG, "get lockscreen magazine enable value : " + value);
            mIsMagzine = (value == 0 ? false : true);

            value = Settings.System.getInt(getContentResolver(), SETTINGS_PULSE, 1);
            mIsLockscreenShowNotifications = (value == 0 ? false : true);
            Log.d(TAG, "is lockscreen show notifications is :" + mIsLockscreenShowNotifications);
        } catch (Exception ee) {
            Log.d(TAG, "Error in init switchValues :" + ee);
        }
    }

    private void initViews() {
        try {
            mLockscreenShowNotificationsPref.setChecked(mIsLockscreenShowNotifications);

            if (mDeviceOwnerInfoPref != null) {
                updateDeviceOwnerInfo();
            }

            if (mMagzineSettingsPref != null) {
                mMagzineSettingsPref.setOnPreferenceClickListener(
                        new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                Log.d(TAG, "onPreferenceClick: Magzine settings preference is clicked");
                                try {
                                    Intent intent = new Intent();
                                    intent.setClassName("com.bizhiquan.lockscreen.asus",
                                            "com.asus.lockscreen.sdk.activity.BZQSettingActivity");
                                    getContext().startActivity(intent);
                                } catch (Exception ex) {
                                    Log.d(TAG, "Error of magazine setting clicked : " + ex);
                                }
                                return true;
                            }
                        });
            } else {
                Log.d(TAG, "mMagzineSettingsPref is null");
            }
        } catch (Exception ee) {
            Log.d(TAG, "init views error :" + ee);
        }
    }

    public void updateDeviceOwnerInfo()
    {
        try{
            String DeviceOwnerInfoStr = "";
            if (mLockPatternUtils.isDeviceOwnerInfoEnabled())
            {
                DeviceOwnerInfoStr = mLockPatternUtils.getDeviceOwnerInfo();
                Log.d(TAG, "device owner info is enabled ,and update it to :" + DeviceOwnerInfoStr);
            }
            else
            {
                int MY_USER_ID = UserHandle.myUserId();
                if(mLockPatternUtils.isOwnerInfoEnabled(MY_USER_ID))
                {
                    DeviceOwnerInfoStr = mLockPatternUtils.getOwnerInfo(MY_USER_ID);
                    Log.d(TAG,"Owner info is enabled,update to :" + DeviceOwnerInfoStr);
                    if(DeviceOwnerInfoStr == null || DeviceOwnerInfoStr.isEmpty() || DeviceOwnerInfoStr.length() == 0)
                    {
                        Log.d(TAG,"Device owner info is invalid set to default summary");
                        DeviceOwnerInfoStr = getString(R.string.device_owner_info_summary);
                    }
                }
                else
                {
                    DeviceOwnerInfoStr = getString(R.string.device_owner_info_summary);
                }

                Log.d(TAG, "device Owner Info is disabled set to :" + DeviceOwnerInfoStr);
            }
            // if owner info is too long ,set summary to partial of it.
            if(DeviceOwnerInfoStr.length() > 25)
            {
                //
                String tempStr = DeviceOwnerInfoStr.substring(0,25) + "...";
                DeviceOwnerInfoStr = tempStr;
            }
            mDeviceOwnerInfoPref.setSummary(DeviceOwnerInfoStr);
        }
        catch(Exception ee)
        {
            Log.d(TAG,"update device owner info fail: " + ee);
        }
    }
}
