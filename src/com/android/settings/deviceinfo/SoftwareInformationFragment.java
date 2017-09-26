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

package com.android.settings.deviceinfo;
import android.content.ComponentName;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.internal.logging.MetricsProto;
import com.android.settings.R;
import com.android.settings.DevelopmentSettings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.settings.util.ResCustomizeConfig;
import com.android.internal.logging.MetricsLogger;
import com.android.settingslib.RestrictedLockUtils;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import com.android.settings.AsusTelephonyUtils;

public class SoftwareInformationFragment extends SettingsPreferenceFragment implements Indexable,OnPreferenceClickListener {

    private static final String LOG_TAG = "SoftwareInformationFragment";
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";
    private static final String IS_ROOTED = "TRUE";

    protected static final String KEY_KERNEL_VERSION = "kernel_version";
    protected static final String KEY_BUILD_NUMBER = "build_number";
    protected static final String KEY_SELINUX_STATUS = "selinux_status";
    protected static final String KEY_BASEBAND_VERSION = "baseband_version";
    protected static final String KEY_SOFTWARE_STATUS = "software_status";

    protected static final String KEY_APP_VERSION = "ro.build.app.version";
    protected static final String PROPERTY_SELINUX_STATUS = "ro.build.selinux";
    private static final String VERSION_USERDEBUG = "UD";
    protected static final String PROPERTY_ROOT_STATUS = "content://com.verizon.security/ROOT_STATUS";
//    private static final int MSG_PFS_FW_STATE = 0;
    private static final String BUILD_COUNTRY_CODE = Build.COUNTRYCODE.toLowerCase();
    private static final String BUILD_SKU = Build.ASUSSKU.toLowerCase();
    private static final String WW_SKU = "ww";
    private static final String INDONESIA_COUNTRY_CODE = "id";
    private static final String INDONESIA_TIMEZONE = "WIB";
    private static final String PC_NAME = "Tsm-0@tsm0-Z9PE-D8";

    static final int TAPS_TO_BE_A_DEVELOPER = 7;

    int mDevHitCountdown;
    Toast mDevHitToast;

    private EnforcedAdmin mDebuggingFeaturesDisallowedAdmin;
    private boolean mDebuggingFeaturesDisallowedBySystem;

    private int mRetryCount = 0;
    // ---

    // +++ Mark_Huang@20151230: Verizon hidden test menu
    private int mHiddenTestMenuHitCountdown;
    private Toast mHiddenTestMenuHitToast;
    private static final int TAPS_TO_ENABLE_HIDDEN_TEST_MENU = 20;
    private static final String PREF_HIDDEN_TEST_MENU_SHOW = "hidden_test_menu_show";
    // --- Mark_Huang@20151230: Verizon hidden test menu

    //private int mPFStationState = DockManager.EXTRA_PADDOCK_STATE_HDMI_REMOVE;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.device_info_software);

        PackageManager pm = getPackageManager();

        setValueSummary(KEY_BASEBAND_VERSION, "gsm.version.baseband");
        String DeviceInfoDefault = getResources().getString(R.string.device_info_default);
        //for CTA: Kernel version: Append UD when userdebug
        //         Build number: BSP customized
        if (isCTA() && (Utils.isCNSKU() && !Utils.isCTCC())) {
            setStringSummary(KEY_KERNEL_VERSION,
                    getFormattedKernelVersion().concat(
                            Build.TYPE.equals("userdebug") ? " " + VERSION_USERDEBUG : ""));
            setStringSummary(KEY_BUILD_NUMBER,
                    ResCustomizeConfig.getProperty(KEY_BUILD_NUMBER, DeviceInfoDefault));
        } else if(Utils.isCTCC()){
            setStringSummary(KEY_KERNEL_VERSION, getFormattedKernelVersion());
            setStringSummary(KEY_BUILD_NUMBER, generateCTCCFormatBuildNumber());
        }else{
            setStringSummary(KEY_KERNEL_VERSION, getFormattedKernelVersion());
            setStringSummary(KEY_BUILD_NUMBER, generateFormatBuildNumber());
        }

        findPreference(KEY_BUILD_NUMBER).setEnabled(true);

        // +++ Mark_Huang@20151230: Verizon hidden test menu
        if (AsusTelephonyUtils.isVerizon()) {
            findPreference(KEY_BASEBAND_VERSION).setEnabled(true);
        }
        // --- Mark_Huang@20151230: Verizon hidden test menu

        if (!SELinux.isSELinuxEnabled()) {
            String status = getResources().getString(R.string.selinux_status_disabled);
            setStringSummary(KEY_SELINUX_STATUS, status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status = getResources().getString(R.string.selinux_status_permissive);
            setStringSummary(KEY_SELINUX_STATUS, status);
        }

        // Remove selinux information if property is not present
        setPropertySummary(getPreferenceScreen(), KEY_SELINUX_STATUS,
                PROPERTY_SELINUX_STATUS);

        // Remove Baseband version if wifi-only device
        if (Utils.isWifiOnly(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(KEY_BASEBAND_VERSION));
        }

        // Remove Software status if non-Verizon device
        if (!Utils.isVerizon()) {
            getPreferenceScreen().removePreference(findPreference(KEY_SOFTWARE_STATUS));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDevHitCountdown = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                        android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER;
        mDevHitToast = null;
        mDebuggingFeaturesDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());
        mDebuggingFeaturesDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(
                getActivity(), UserManager.DISALLOW_DEBUGGING_FEATURES, UserHandle.myUserId());


        // +++ Mark_Huang@20151230: Verizon hidden test menu
        if (AsusTelephonyUtils.isVerizon()) {
            mHiddenTestMenuHitCountdown = -1;
            mHiddenTestMenuHitCountdown = (android.provider.Settings.Global.getInt(getActivity().getContentResolver(),
                    PREF_HIDDEN_TEST_MENU_SHOW, 0) == 1)? -1 : TAPS_TO_ENABLE_HIDDEN_TEST_MENU;
            mHiddenTestMenuHitToast = null;
        }
        // --- Mark_Huang@20151230: Verizon hidden test menu

        //Show device is rooted or not for Verizon
        Cursor c = getContentResolver().query(Uri.parse(PROPERTY_ROOT_STATUS), null, null, null, null);
        if(c != null && c.moveToLast()) {
            String rootStatus = c.getString(c.getColumnIndex("root_status"));
            if(rootStatus.equalsIgnoreCase(IS_ROOTED)){
                setStringSummary(KEY_SOFTWARE_STATUS,getResources().getString(R.string.rooted_devices));
            }else{
                setStringSummary(KEY_SOFTWARE_STATUS,getResources().getString(R.string.unrooted_devices));
            }
            c.close();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        // Only display EC version on device which has keyboard feature

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals(KEY_BUILD_NUMBER)) {
            // Don't enable developer options for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) return true;

            final UserManager um = UserManager.get(getActivity());
            if (um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) {
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
            return true;
        }

        // +++ Mark_Huang@20151230: Verizon hidden test menu
        if(AsusTelephonyUtils.isVerizon() && preference.getKey().equals(KEY_BASEBAND_VERSION)) {
            Log.d(LOG_TAG, "onPreferenceTreeClick(): Click base band item");
            if (mHiddenTestMenuHitCountdown > 0) {
                mHiddenTestMenuHitCountdown--;
                if (mHiddenTestMenuHitCountdown == 0) {
                    Log.d(LOG_TAG, "onPreferenceTreeClick(): Hidden test menu enabled!");
                    if (mHiddenTestMenuHitToast != null) {
                        mHiddenTestMenuHitToast.cancel();
                    }
                    mHiddenTestMenuHitToast = Toast.makeText(getActivity(), R.string.vzw_show_hidden_test_menu,
                            Toast.LENGTH_LONG);
                    mHiddenTestMenuHitToast.show();
                    android.provider.Settings.Global.putInt(getActivity().getContentResolver(), PREF_HIDDEN_TEST_MENU_SHOW, 1);
                }
            } else {
                Log.d(LOG_TAG, "onPreferenceTreeClick(): Hidden test menu has already shown");
            }
        }
        // --- Mark_Huang@20151230: Verizon hidden test menu
        return super.onPreferenceTreeClick(preference);
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

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }
    private void setStringSummary(String preference, String value) {
        try {
            if (findPreference(preference) != null)
                findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        if(findPreference(preference) == null) return ;
        try {
                findPreference(preference).setSummary(
                        SystemProperties.get(property,
                                getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));

        } catch (IOException e) {
            Log.e(LOG_TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "(#\\d+) " +              /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }

        //hard code change PC name & Timezone for indonesia in WW-sku
        if (BUILD_SKU.equals(WW_SKU) && BUILD_COUNTRY_CODE.equals(INDONESIA_COUNTRY_CODE)){
            //replace timezone to WIB
            return generateIndonesiaKernelVersion(m);
        } else {
            return m.group(1) + "\n" +                     // 3.0.31-g6fb96c9
                    m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
                    m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
        }
    }


    static private String generateIndonesiaKernelVersion(Matcher kernelVersion){
        String[] splitGroupFour =  kernelVersion.group(4).split(" ");
        splitGroupFour[4] = INDONESIA_TIMEZONE;
        StringBuilder kernelInfoGroupFour = new StringBuilder();
        for (String s : splitGroupFour){
            kernelInfoGroupFour.append(s + " ");
        }
        return kernelVersion.group(1) + "\n" +                  // 3.0.31-g6fb96c9
                PC_NAME + " " + kernelVersion.group(3) + "\n" + // Tsm-0@tsm0-Z9PE-D8 #1
                kernelInfoGroupFour.toString();                 // Thu Jun 28 11:02:39 WIB 2012
    }

    
    private boolean isCTA(){
        return ResCustomizeConfig.getBooleanConfig("isCTA", getResources()
                .getBoolean(R.bool.def_isCTA));
    }


    private String generateFormatBuildNumber(){
        String amaxVersion = SystemProperties.get(KEY_APP_VERSION);
        String productname = SystemProperties.get("ro.product.name", "");
        return amaxVersion.isEmpty() || productname.toLowerCase().startsWith("att") ? Build.DISPLAY
                : new StringBuilder(Build.DISPLAY).append("\n")
                        .append(amaxVersion).append("\n")
                        .append(Utils.getSKUEncode())
                        .append(Utils.getCountryEncode(getActivity()))
                        .append(Utils.getCIDEncode(getActivity())).toString();
    }

    private String generateCTCCFormatBuildNumber(){
        String cscVersion = SystemProperties.get("ro.build.csc.version");
        String[] cscVersionTokens =cscVersion.split("_|-");
        StringBuilder cscOutput =new StringBuilder();
        for(int i=0; i < cscVersionTokens.length; i++){
            if (i == 1) {
                continue;
            } else if (i != cscVersionTokens.length-1){
                cscOutput.append(cscVersionTokens[i]).append("_");
            } else {
                cscOutput.append(cscVersionTokens[i]);
            }
        }
        return cscVersion.isEmpty() ? cscVersion
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
                // Remove Baseband version if wifi-only device
                if (Utils.isWifiOnly(context)) {
                    keys.add((KEY_BASEBAND_VERSION));
                }
                return keys;
            }

            private boolean isPropertyMissing(String property) {
                return SystemProperties.get(property).equals("");
            }
        };

    @Override
    protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEVICEINFO;
    }
}

