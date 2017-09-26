/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.telephony.CarrierConfigManager;
import android.text.TextUtils;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.deviceinfo.StorageVolumePreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;
import com.android.settings.util.ResCustomizeConfig;
import com.android.settings.widget.FloatingActionButton;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.RestrictedLockUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import static java.security.AccessController.getContext;

import com.android.settings.util.RemoveSpecialCharFilter;
import com.android.settings.util.Utf8ByteLengthFilter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import com.android.internal.app.procstats.ProcessStats;

public class DeviceInfoSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String LOG_TAG = "DeviceInfoSettings";

    private static final String KEY_MANUAL = "manual";
    private static final String KEY_REGULATORY_INFO = "regulatory_info";
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";
    private static final String PROPERTY_SELINUX_STATUS = "ro.build.selinux";
    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String KEY_BUILD_NUMBER = "build_number";
    private static final String KEY_DEVICE_MODEL = "device_model";
    private static final String KEY_SELINUX_STATUS = "selinux_status";
    private static final String KEY_FIRMWARE_VERSION = "firmware_version";
    private static final String KEY_SECURITY_PATCH = "security_patch";
    private static final String KEY_UPDATE_SETTING = "additional_system_update_settings";
    private static final String KEY_EQUIPMENT_ID = "fcc_equipment_id";
    private static final String PROPERTY_EQUIPMENT_ID = "ro.ril.fccid";
    private static final String KEY_DEVICE_FEEDBACK = "device_feedback";
    private static final String KEY_SAFETY_LEGAL = "safetylegal";

    private static final String KEY_SYSTEM_CATEGORY = "system_category";
    //For Verizon
    private static final String KEY_CONFIGURATION_VERSION = "configuration_version";
    private static final String PROPERTY_CONFIGURATION_VERSION = "persist.dmc.confversion";

    protected static final String KEY_SOFTWARE_INFO = "software_info";
    protected static final String KEY_CSC_VERSION = "ro.build.csc.version";
    protected static final String KEY_ATT_SOFTWARE_UPDATE = "att_software_update";

    protected static final String KEY_CPU_FREQENCY = "cpu_frequency";
    protected static final String KEY_MEMORY_SIZE = "memory_size";
    protected static final String KEY_EMMC_SIZE = "emmc_size";

    private static final String PROPERTY_CPU_FREQUENCY = "ro.cpufreq";
    private static final String PROPERTY_MEMORY_SIZE = "ro.memsize";
    private static final String PROPERTY_EMMC_SIZE = "ro.emmc_size";

    static final int TAPS_TO_BE_A_DEVELOPER = 7;

    // Kevin_Chiou Verizon VZ_REQ_UI_15743
    private static final String FACTORY_PARTITION_ROOT_PATH = "/factory";
    private static final String FACTORY_RESET_TIME_FILE_NAME = "factory_reset";
    private static String mLastFactoryResetTime = "DEVICE~WTF";

    long[] mHits = new long[3];
    int mDevHitCountdown;
    Toast mDevHitToast;

    private UserManager mUm;

    private EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;
    private EnforcedAdmin mDebuggingFeaturesDisallowedAdmin;
    private boolean mDebuggingFeaturesDisallowedBySystem;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUm = UserManager.get(getActivity());

        addPreferencesFromResource(R.xml.device_info_settings);

        Preference software = (Preference) findPreference(KEY_SOFTWARE_INFO);
        PreferenceCategory systemCagegory = (PreferenceCategory)findPreference(KEY_SYSTEM_CATEGORY);

        setStringSummary(KEY_FIRMWARE_VERSION, Build.VERSION.RELEASE);
        findPreference(KEY_FIRMWARE_VERSION).setEnabled(true);

        final String patch = DeviceInfoUtils.getSecurityPatch();
        if (!TextUtils.isEmpty(patch)) {
            setStringSummary(KEY_SECURITY_PATCH, patch);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_SECURITY_PATCH));
        }

        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL + DeviceInfoUtils.getMsvSuffix());
        setValueSummary(KEY_EQUIPMENT_ID, PROPERTY_EQUIPMENT_ID);
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL);

        //For Verizon
        setValueSummary(KEY_CONFIGURATION_VERSION, PROPERTY_CONFIGURATION_VERSION);
/*has no device name preference
        mDeviceName = (Preference) findPreference(KEY_DEVICE_NAME);


        if(Utils.isVerizon()) {
            deviceName = Settings.System.getString(getContentResolver(), Settings.System.CONNECTIVITY_DEVICE_NAME);
            String id = Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
            if (deviceName == null) {
                deviceName = android.os.Build.PRODUCT;
                Settings.System.putString(getContentResolver(), Settings.System.CONNECTIVITY_DEVICE_NAME, deviceName);
                SystemProperties.set(PROPERTY_DEVICE_NAME, deviceName + "_" + id.substring(0,4));
                LocalBluetoothManager localManager = com.android.settings.bluetooth.Utils.getLocalBtManager(getActivity());
                localManager.getBluetoothAdapter().setName(deviceName);
            }
            if (deviceName.equals(android.os.Build.PRODUCT))
                SystemProperties.set(PROPERTY_DEVICE_NAME, deviceName + "_" + id.substring(0,4));
            else
                SystemProperties.set(PROPERTY_DEVICE_NAME, deviceName);

            mDeviceName.setTitle(deviceName);
            mDeviceName.setSummary(deviceName);

        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_NAME));
        }

        systemCagegory.removePreference(mDeviceName);
*/
        /*
        setStringSummary(KEY_BUILD_NUMBER, Build.DISPLAY);
        findPreference(KEY_BUILD_NUMBER).setEnabled(true);
        findPreference(KEY_KERNEL_VERSION).setSummary(DeviceInfoUtils.getFormattedKernelVersion());

        if (!SELinux.isSELinuxEnabled()) {
            String status = getResources().getString(R.string.selinux_status_disabled);
            setStringSummary(KEY_SELINUX_STATUS, status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status = getResources().getString(R.string.selinux_status_permissive);
            setStringSummary(KEY_SELINUX_STATUS, status);
        }

        // Remove selinux information if property is not present
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SELINUX_STATUS,
                PROPERTY_SELINUX_STATUS);

        // Remove Safety information preference if PROPERTY_URL_SAFETYLEGAL is not set
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SAFETY_LEGAL,
                PROPERTY_URL_SAFETYLEGAL);
        */

        // Remove Equipment id preference if FCC ID is not set by RIL
//        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_EQUIPMENT_ID,
//                PROPERTY_EQUIPMENT_ID);
        systemCagegory.removePreference(findPreference(KEY_SELINUX_STATUS));
        systemCagegory.removePreference(findPreference(KEY_EQUIPMENT_ID));

        // Dont show feedback option if there is no reporter.
//        if (TextUtils.isEmpty(DeviceInfoUtils.getFeedbackReporterPackage(getActivity()))) {
//            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_FEEDBACK));
//        }
        systemCagegory.removePreference(findPreference(KEY_DEVICE_FEEDBACK));

        /*
         * Settings is a generic app and should not contain any device-specific
         * info.
         */
        final Activity act = getActivity();

        // These are contained by the root preference screen
        PreferenceGroup parentPreference = getPreferenceScreen();

//jack_qi, remove item for CNSettings.
//        if (mUm.isAdminUser()) {
//            Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference,
//                    KEY_SYSTEM_UPDATE_SETTINGS,
//                    Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
//        } else {
//            // Remove for secondary users
//            removePreference(KEY_SYSTEM_UPDATE_SETTINGS);
//        }


        systemCagegory.removePreference(findPreference(KEY_SYSTEM_UPDATE_SETTINGS));

        // Read platform settings for additional system update setting
//        removePreferenceIfBoolFalse(KEY_UPDATE_SETTING,
//                R.bool.config_additional_system_update_setting_enable);
        systemCagegory.removePreference(findPreference(KEY_UPDATE_SETTING));

        // Remove manual entry if none present.
//        removePreferenceIfBoolFalse(KEY_MANUAL, R.bool.config_show_manual);
        systemCagegory.removePreference(findPreference(KEY_MANUAL));
//jack_qi
        // Remove regulatory information if none present.
//        if(!ResCustomizeConfig.isShowRegulatory())
//        systemCagegory.removePreference(findPreference(KEY_REGULATORY_INFO));

        //Add BuildInfo SKU_Version_Date
        software.setSummary(generateSoftwareInfo());

        // Remove configuration if none present
//        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_CONFIGURATION_VERSION,
//                PROPERTY_CONFIGURATION_VERSION);
        systemCagegory.removePreference(findPreference(KEY_CONFIGURATION_VERSION));

        //jack_qi
        findPreference(KEY_CPU_FREQENCY).setSummary(getCpuInfo());
        //findPreference(KEY_MEMORY_SIZE).setSummary(getTotalMemorySie());
        setPropertySummary(getPreferenceScreen(), KEY_MEMORY_SIZE,
                PROPERTY_MEMORY_SIZE);
        findPreference(KEY_EMMC_SIZE).setSummary(geteMMCSize());
        //jack_qi

        if(Utils.isLastFactoryFileExist()){
            Utils.parseLastFactoryTime(act);
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mDevHitCountdown = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                        android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER;
        mDevHitToast = null;
        mFunDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_FUN, UserHandle.myUserId());
        mFunDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(
                getActivity(), UserManager.DISALLOW_FUN, UserHandle.myUserId());
        mDebuggingFeaturesDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());
        mDebuggingFeaturesDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(
                getActivity(), UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(KEY_FIRMWARE_VERSION)) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
            mHits[mHits.length-1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis()-500)) {
                if (mUm.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                    if (mFunDisallowedAdmin != null && !mFunDisallowedBySystem) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                                mFunDisallowedAdmin);
                    }
                    Log.d(LOG_TAG, "Sorry, no fun for you!");
                    return false;
                }

                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("android",
                        com.android.internal.app.PlatLogoActivity.class.getName());
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Unable to start activity " + intent.toString());
                }
            }
        } else if (preference.getKey().equals(KEY_BUILD_NUMBER)) {
            // Don't enable developer options for secondary users.
            if (!mUm.isAdminUser()) return true;

            // Don't enable developer options until device has been provisioned
            if (!Utils.isDeviceProvisioned(getActivity())) {
                return true;
            }

            if (mUm.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) {
                if (mDebuggingFeaturesDisallowedAdmin != null &&
                        !mDebuggingFeaturesDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                            mDebuggingFeaturesDisallowedAdmin);
                }
                return true;
            }

            if (mDevHitCountdown > 0) {
                mDevHitCountdown--;
                if (mDevHitCountdown == 0) {
                    getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).edit().putBoolean(
                                    DevelopmentSettings.PREF_SHOW, true).apply();
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_on,
                            Toast.LENGTH_LONG);
                    mDevHitToast.show();
                    // This is good time to index the Developer Options
                    Index.getInstance(
                            getActivity().getApplicationContext()).updateFromClassNameResource(
                                    DevelopmentSettings.class.getName(), true, true);

                } else if (mDevHitCountdown > 0
                        && mDevHitCountdown < (TAPS_TO_BE_A_DEVELOPER-2)) {
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), getResources().getQuantityString(
                            R.plurals.show_dev_countdown, mDevHitCountdown, mDevHitCountdown),
                            Toast.LENGTH_SHORT);
                    mDevHitToast.show();
                }
            } else if (mDevHitCountdown < 0) {
                if (mDevHitToast != null) {
                    mDevHitToast.cancel();
                }
                mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_already,
                        Toast.LENGTH_LONG);
                mDevHitToast.show();
            }
        } else if (preference.getKey().equals(KEY_SECURITY_PATCH)) {
            if (getPackageManager().queryIntentActivities(preference.getIntent(), 0).isEmpty()) {
                // Don't send out the intent to stop crash
                Log.w(LOG_TAG, "Stop click action on " + KEY_SECURITY_PATCH + ": "
                        + "queryIntentActivities() returns empty" );
                return true;
            }
        } else if (preference.getKey().equals(KEY_DEVICE_FEEDBACK)) {
            sendFeedback();
        } else if(preference.getKey().equals(KEY_SYSTEM_UPDATE_SETTINGS)) {
            CarrierConfigManager configManager =
                    (CarrierConfigManager) getSystemService(Context.CARRIER_CONFIG_SERVICE);
            PersistableBundle b = configManager.getConfig();
            if (b != null && b.getBoolean(CarrierConfigManager.KEY_CI_ACTION_ON_SYS_UPDATE_BOOL)) {
                ciActionOnSysUpdate(b);
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    /**
     * Trigger client initiated action (send intent) on system update
     */
    private void ciActionOnSysUpdate(PersistableBundle b) {
        String intentStr = b.getString(CarrierConfigManager.
                KEY_CI_ACTION_ON_SYS_UPDATE_INTENT_STRING);
        if (!TextUtils.isEmpty(intentStr)) {
            String extra = b.getString(CarrierConfigManager.
                    KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_STRING);
            String extraVal = b.getString(CarrierConfigManager.
                    KEY_CI_ACTION_ON_SYS_UPDATE_EXTRA_VAL_STRING);

            Intent intent = new Intent(intentStr);
            if (!TextUtils.isEmpty(extra)) {
                intent.putExtra(extra, extraVal);
            }
            Log.d(LOG_TAG, "ciActionOnSysUpdate: broadcasting intent " + intentStr +
                    " with extra " + extra + ", " + extraVal);
            getActivity().getApplicationContext().sendBroadcast(intent);
        }
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
            String preference, String property ) {
        if (SystemProperties.get(property).equals("")) {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        }
    }

    private void removePreferenceIfActivityMissing(String preferenceKey, String action) {
        final Intent intent = new Intent(action);
        if (getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
            Preference pref = findPreference(preferenceKey);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    private void removePreferenceIfBoolFalse(String preference, int resId) {
        if (!getResources().getBoolean(resId)) {
            Preference pref = findPreference(preference);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private void sendFeedback() {
        String reporterPackage = DeviceInfoUtils.getFeedbackReporterPackage(getActivity());
        if (TextUtils.isEmpty(reporterPackage)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
        intent.setPackage(reporterPackage);
        startActivityForResult(intent, 0);
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
//                mSummaryLoader.setSummary(this, mContext.getString(R.string.about_summary,
//                        Build.VERSION.RELEASE));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    //Add BuildInfo SKU_Version_Date
    private String generateSoftwareInfo(){
        String software_summery = this.getActivity().getResources().getString(R.string.software_info_summary);
        String cscVersion = SystemProperties.get(KEY_CSC_VERSION);
        String[] cscVersionTokens =cscVersion.split("_|-");
        StringBuilder cscOutput =new StringBuilder(software_summery).append("\n");
        for(int i=0; i < cscVersionTokens.length; i++){
            if (i == 1) {
                continue;
            } else if (i != cscVersionTokens.length-1){
                cscOutput.append(cscVersionTokens[i]).append("_");
            } else {
                cscOutput.append(cscVersionTokens[i]);
            }
        }
        return cscVersion.isEmpty() ? software_summery
                : cscOutput.toString();
    }


    private String generateCTCCSoftwareInfo(){
        String software_summery = this.getActivity().getResources().getString(R.string.software_info_summary);
        String cscVersion = Build.DISPLAY;
        StringBuilder cscOutput =new StringBuilder(software_summery).append("\n");
        cscOutput.append(cscVersion);
        return cscVersion.equals("") ? software_summery
                : cscOutput.toString();
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.device_info_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                if (isPropertyMissing(PROPERTY_SELINUX_STATUS)) {
                    keys.add(KEY_SELINUX_STATUS);
                }
                if (isPropertyMissing(PROPERTY_URL_SAFETYLEGAL)) {
                    keys.add(KEY_SAFETY_LEGAL);
                }
                if (isPropertyMissing(PROPERTY_EQUIPMENT_ID)) {
                    keys.add(KEY_EQUIPMENT_ID);
                }
                // Dont show feedback option if there is no reporter.
                /*if (TextUtils.isEmpty(DeviceInfoUtils.getFeedbackReporterPackage(context))) {
                    keys.add(KEY_DEVICE_FEEDBACK);
                }
                final UserManager um = UserManager.get(context);
                // TODO: system update needs to be fixed for non-owner user b/22760654
                if (!um.isAdminUser()) {
                    keys.add(KEY_SYSTEM_UPDATE_SETTINGS);
                }
                if (!context.getResources().getBoolean(
                        R.bool.config_additional_system_update_setting_enable)) {
                    keys.add(KEY_UPDATE_SETTING);
                }*/
                //++Sunny_Yuan
                if(!Utils.isATT()){
                    keys.add(KEY_ATT_SOFTWARE_UPDATE);
                }
                keys.add(KEY_DEVICE_FEEDBACK);
                keys.add(KEY_CONFIGURATION_VERSION);
                keys.add(KEY_SYSTEM_UPDATE_SETTINGS);
                keys.add(KEY_UPDATE_SETTING);
                //keys.add(KEY_DEVICE_NAME);
                keys.add(KEY_REGULATORY_INFO);
                keys.add(KEY_MANUAL);

                return keys;
            }

            private boolean isPropertyMissing(String property) {
                return SystemProperties.get(property).equals("");
            }
        };

    //Jack Qi
    private String getMaxCpuFreq() {
        String result = "";
        long maxCpuFreq;
        ProcessBuilder cmd;
        try {
            String[] args = {"/system/bin/cat",
                    "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"};
            cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            InputStream in = process.getInputStream();
            byte[] re = new byte[24];
            while (in.read(re) != -1) {
                result = result + new String(re);
            }
            in.close();
        } catch (IOException ex) {
            ex.printStackTrace();
            result = "N/A";
        }
        //return result.trim();
        maxCpuFreq = Long.valueOf(result.trim());
        result = String.format("%.2fGHz",(double)maxCpuFreq / 1000000 );
        return result;
    }

    private String getCpuInfo(){
        String cpuInfo = "";
        String cpuCoreNum = getActivity().getResources().getString(R.string.about_cpu_core_num);
        String[] cpuCoreNums = cpuCoreNum.split(",");
        int cpuNum = Runtime.getRuntime().availableProcessors();

        String cpuFreq =  SystemProperties.get(PROPERTY_CPU_FREQUENCY);

        if(cpuNum <= cpuCoreNums.length){
            if(cpuFreq.equals("")){
                cpuInfo = cpuCoreNums[cpuNum - 1].trim() + getMaxCpuFreq();
            }else{
                cpuInfo = cpuCoreNums[cpuNum - 1].trim() + cpuFreq;
            }

        }else{
            Log.e(LOG_TAG, "CPU core number exceeded.");
        }


        return cpuInfo;
    }

    private long getTotalMemorySie(){
        long totalSize = 0L;
        try {
            String[] cmd = new String[]{"/system/bin/sh", "-c", "cat /data/data/emmc_total_size"};
            BufferedReader br = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(cmd).getInputStream()));
            String line = br.readLine();

            if(line != null) {
                totalSize = Long.parseLong(line) * 1024 * 1024 * 1024;
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return totalSize;
    }

    private String geteMMCSize(){
        StorageManager storageManager = (StorageManager)getSystemService(Context.STORAGE_SERVICE);
        final List<VolumeInfo> volumes = storageManager.getVolumes();
        long privateUsedBytes = 0;
        long privateTotalBytes = 0;
        //+++ Asus total emmc size
        VolumeInfo intVol = null;
        //---
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                final long volumeTotalBytes = getTotalSize(vol);
                Log.d("blenda", "get volume, type private");

                //---
                if (vol.isMountedReadable()) {
                    final File path = vol.getPath();

                    if (path.getName().equals("data")){
                        intVol = vol;
                    }

                    privateUsedBytes += (volumeTotalBytes - path.getFreeSpace());
                    privateTotalBytes += volumeTotalBytes;
                    //---
                }
            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                Log.d("blenda", "get volume, type public");
            }
        }       //+++ Asus total emmc size
        if (getTotalMemorySie() > 0) {
            Log.d("blenda", "get system volume, mEmmcTotalSize: " + getTotalMemorySie());
            long reservedSize = getTotalMemorySie() - intVol.getPath().getTotalSpace();
            privateUsedBytes += reservedSize;
            privateTotalBytes += reservedSize;
        }
        //---
//        String totalEMMCSize = SystemProperties.get(PROPERTY_EMMC_SIZE);
//        if(totalEMMCSize.equals("")){
        String totalEMMCSize = Formatter.formatFileSize(getContext(), privateTotalBytes);
//        }
        return getString(R.string.emmc_storage_summary, Formatter.formatFileSize(getContext(), privateTotalBytes - privateUsedBytes),
                totalEMMCSize);
    }

    private  long getTotalSize(VolumeInfo info) {
        // Device could have more than one primary storage, which could be located in the
        // internal flash (UUID_PRIVATE_INTERNAL) or in an external disk.
        // If it's internal, try to get its total size from StorageManager first
        // (sTotalInternalStorage), since that size is more precise because it accounts for
        // the system partition.
        //to
/*        if (info.getType() == VolumeInfo.TYPE_PRIVATE
                && Objects.equals(info.getFsUuid(), StorageManager.UUID_PRIVATE_INTERNAL)
                && sTotalInternalStorage > 0) {
            return sTotalInternalStorage;
        } else {*/
        final File path = info.getPath();
        if (path == null) {
            // Should not happen, caller should have checked.
            Log.e(LOG_TAG, "info's path is null on getTotalSize(): " + info);
            return 0;
        }
        return path.getTotalSpace();
    }

    private void setPropertySummary(PreferenceGroup preferenceGroup,
                                    String preference, String property) {
        if (findPreference(preference) == null) return;
        String property_content = SystemProperties.get(property);
        if (TextUtils.isEmpty(property_content)) {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        } else {
            findPreference(preference).setSummary(property_content);
        }
    }
    // jack_qi

}
