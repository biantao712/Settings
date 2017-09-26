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


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceGroup;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.ims.ImsManager;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.ethernet.EthernetUtils;
import com.android.settings.nfc.NfcEnabler;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.Utils;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import android.os.ServiceManager;
import android.os.RemoteException;
import android.net.IConnectivityManager;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WirelessSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = "WirelessSettings";

    private static final String KEY_WIRELESS_CATEGORY = "wireless_category";
    private static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private static final String KEY_ETHERNET_SETTINGS = "ethernet_settings";
    private static final String KEY_WIMAX_SETTINGS = "wimax_settings";
    private static final String KEY_ANDROID_BEAM_SETTINGS = "android_beam_settings";
    private static final String KEY_VPN_SETTINGS = "vpn_settings";
    private static final String KEY_TETHER_SETTINGS = "tether_settings";
    private static final String KEY_PROXY_SETTINGS = "proxy_settings";
    private static final String KEY_MOBILE_NETWORK_SETTINGS = "mobile_network_settings";
    private static final String KEY_MANAGE_MOBILE_PLAN = "manage_mobile_plan";
    // +++ AMAX @ 20170119 7.1.1 Porting
    private static final String KEY_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    // --- AMAX @ 20170119 7.1.1 Porting
    private static final String KEY_WFC_SETTINGS = "wifi_calling_settings";
    private static final String KEY_NETWORK_RESET = "network_reset";
    // Wesley_Lee@asus.com
    private static final String KEY_DMS_SETTINGS = "dms_settings";    
    // Add PlayTo in AsusSettings
    private static final String KEY_PLAY_TO_SETTINGS = "playto_settings";

    public static final String EXIT_ECM_RESULT = "exit_ecm_result";
    public static final int REQUEST_CODE_EXIT_ECM = 1;

    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;
    private NfcEnabler mNfcEnabler;
    private NfcAdapter mNfcAdapter;
    // +++ ckenken (ChiaHsiang_Kuo) @ 20170123 Verizon requirement: change Cellular network title to Mobile Network
    private RestrictedPreference mMobileNetworkSettingsPreference;
    // --- ckenken (ChiaHsiang_Kuo) @ 20170123 Verizon requirement: change Cellular network title to Mobile Network
    private Preference mEthernetPreference;
    private ConnectivityManager mCm;
    private TelephonyManager mTm;
    private PackageManager mPm;
    private UserManager mUm;
    private EthernetManager mEm;

    private static final int MANAGE_MOBILE_PLAN_DIALOG_ID = 1;
    private static final String SAVED_MANAGE_MOBILE_PLAN_MSG = "mManageMobilePlanMessage";

    private PreferenceScreen mButtonWfc;

    // Add PlayTo in AsusSettings ++
    private static final int REMOTE_TARGET_STATE_NOT_CONNECTED = 0;
    private static final int REMOTE_TARGET_STATE_CONNECTING = 1;
    private static final int REMOTE_TARGET_STATE_CONNECTED = 2;
    private static final String ACTION_START_PLAYTO_SETTINGS = "com.asus.playto.action.PLAYTO_SETTINGS";
    private static final String ACTION_PLAYTO_STATUS_CHANGED = "com.asus.playto.action.PLAYTO_STATUS_CHANGED";
    private static final String KEY_ACTIVE_TARGET_STATUS = "active_target_status";
    private static final String KEY_ACTIVE_TARGET_FRIENDLY_NAME = "active_target_friendly_name";
    private static final String RES_PLAY_TO_APP_NAME = "app_name";
    private static final String RES_PLAY_TO_SUMMARY = "play_to_summary";
    private static final String RES_PLAY_TO_NOTIFICATION_CONNECTING_MESSAGE = "play_to_notification_connecting_message";
    private static final String RES_PLAY_TO_NOTIFICATION_CONNECTED_MESSAGE = "play_to_notification_connected_message";
    // Add PlayTo in AsusSettings --

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceFragment's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        log("onPreferenceTreeClick: preference=" + preference);
        if (preference == mAirplaneModePreference && Boolean.parseBoolean(
                SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode launch ECM app dialog
            startActivityForResult(
                new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                REQUEST_CODE_EXIT_ECM);
            return true;
        } else if (preference == findPreference(KEY_MANAGE_MOBILE_PLAN)) {
            onManageMobilePlanClick();
        } else if (preference == findPreference(KEY_PLAY_TO_SETTINGS)) {
            // Add PlayTo in AsusSettings
            Intent intent = new Intent(ACTION_START_PLAYTO_SETTINGS);
            intent.setPackage(DisplayManager.PLAYTO_PACKAGE_NAME);
            getActivity().startServiceAsUser(intent, UserHandle.CURRENT);
        // +++ Gary_Hsu@asus.com: VZ_REQ_UI_41001
        } else if (preference == findPreference(KEY_CELL_BROADCAST_SETTINGS)) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            String pkgName = AsusTelephonyUtils.isVerizon() ? "com.asus.cellbroadcastreceiver"
                    : "com.android.cellbroadcastreceiver";
            String className = AsusTelephonyUtils.isVerizon()
                    ? "com.asus.cellbroadcastreceiver.CellBroadcastListActivity"
                    : "com.android.cellbroadcastreceiver.CellBroadcastListActivity";
            ComponentName componentName = new ComponentName(pkgName, className);
            intent.setComponent(componentName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ignored) {
                Log.e(TAG, "start CellBroadcast Setting fail = " + ignored);
            }
        }
        // --- Gary_Hsu@asus.com: VZ_REQ_UI_41001

        // Let the intents be launched by the Preference manager
        return super.onPreferenceTreeClick(preference);
    }

    private String mManageMobilePlanMessage;
    public void onManageMobilePlanClick() {
        log("onManageMobilePlanClick:");
        mManageMobilePlanMessage = null;
        Resources resources = getActivity().getResources();

        NetworkInfo ni = mCm.getActiveNetworkInfo();
        if (mTm.hasIccCard() && (ni != null)) {
            // Check for carrier apps that can handle provisioning first
            Intent provisioningIntent = new Intent(TelephonyIntents.ACTION_CARRIER_SETUP);
            List<String> carrierPackages =
                    mTm.getCarrierPackageNamesForIntent(provisioningIntent);
            if (carrierPackages != null && !carrierPackages.isEmpty()) {
                if (carrierPackages.size() != 1) {
                    Log.w(TAG, "Multiple matching carrier apps found, launching the first.");
                }
                provisioningIntent.setPackage(carrierPackages.get(0));
                startActivity(provisioningIntent);
                return;
            }

            // Get provisioning URL
            String url = mCm.getMobileProvisioningUrl();
            if (!TextUtils.isEmpty(url)) {
                Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
                        Intent.CATEGORY_APP_BROWSER);
                intent.setData(Uri.parse(url));
                intent.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.w(TAG, "onManageMobilePlanClick: startActivity failed" + e);
                }
            } else {
                // No provisioning URL
                String operatorName = mTm.getSimOperatorName();
                if (TextUtils.isEmpty(operatorName)) {
                    // Use NetworkOperatorName as second choice in case there is no
                    // SPN (Service Provider Name on the SIM). Such as with T-mobile.
                    operatorName = mTm.getNetworkOperatorName();
                    if (TextUtils.isEmpty(operatorName)) {
                        mManageMobilePlanMessage = resources.getString(
                                R.string.mobile_unknown_sim_operator);
                    } else {
                        mManageMobilePlanMessage = resources.getString(
                                R.string.mobile_no_provisioning_url, operatorName);
                    }
                } else {
                    mManageMobilePlanMessage = resources.getString(
                            R.string.mobile_no_provisioning_url, operatorName);
                }
            }
        } else if (mTm.hasIccCard() == false) {
            // No sim card
            mManageMobilePlanMessage = resources.getString(R.string.mobile_insert_sim_card);
        } else {
            // NetworkInfo is null, there is no connection
            mManageMobilePlanMessage = resources.getString(R.string.mobile_connect_to_internet);
        }
        if (!TextUtils.isEmpty(mManageMobilePlanMessage)) {
            log("onManageMobilePlanClick: message=" + mManageMobilePlanMessage);
            showDialog(MANAGE_MOBILE_PLAN_DIALOG_ID);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        log("onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case MANAGE_MOBILE_PLAN_DIALOG_ID:
                return new AlertDialog.Builder(getActivity())
                            .setMessage(mManageMobilePlanMessage)
                            .setCancelable(false)
                            .setPositiveButton(com.android.internal.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    log("MANAGE_MOBILE_PLAN_DIALOG.onClickListener id=" + id);
                                    mManageMobilePlanMessage = null;
                                }
                            })
                            .create();
        }
        return super.onCreateDialog(dialogId);
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WIRELESS;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mManageMobilePlanMessage = savedInstanceState.getString(SAVED_MANAGE_MOBILE_PLAN_MSG);
        }
        log("onCreate: mManageMobilePlanMessage=" + mManageMobilePlanMessage);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mTm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mPm = getPackageManager();
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);

        addPreferencesFromResource(R.xml.wireless_settings);

        final boolean isAdmin = mUm.isAdminUser();

        final Activity activity = getActivity();
        
        PreferenceGroup wirelessCategory = (PreferenceGroup) findPreference(KEY_WIRELESS_CATEGORY);
        mAirplaneModePreference = (SwitchPreference) findPreference(KEY_TOGGLE_AIRPLANE);
        SwitchPreference nfc = (SwitchPreference) findPreference(KEY_TOGGLE_NFC);
        RestrictedPreference androidBeam = (RestrictedPreference) findPreference(
                KEY_ANDROID_BEAM_SETTINGS);

        mAirplaneModeEnabler = new AirplaneModeEnabler(activity, mAirplaneModePreference);
        mNfcEnabler = new NfcEnabler(activity, nfc, androidBeam);

        /* remove wifi_calling_settings
         * tim++
        mButtonWfc = (PreferenceScreen) findPreference(KEY_WFC_SETTINGS);
        */

        String toggleable = Settings.Global.getString(activity.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS);

        /* remove wimax_settings
         * tim++
        //enable/disable wimax depending on the value in config.xml
        final boolean isWimaxEnabled = isAdmin && this.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (!isWimaxEnabled || RestrictedLockUtils.hasBaseUserRestriction(activity,
                UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, UserHandle.myUserId())) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(KEY_WIMAX_SETTINGS);
            if (ps != null) root.removePreference(ps);
        } else {
            if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIMAX )
                    && isWimaxEnabled) {
                Preference ps = findPreference(KEY_WIMAX_SETTINGS);
                ps.setDependency(KEY_TOGGLE_AIRPLANE);
            }
        }*/

        /* remove ethernet_settings
         * tim++
        mEthernetPreference = findPreference(KEY_ETHERNET_SETTINGS);
        if (mEthernetPreference != null) {
            if (mCm.isNetworkSupported(ConnectivityManager.TYPE_ETHERNET)) {
                mEm = (EthernetManager) getActivity().getSystemService(Context.ETHERNET_SERVICE);
                mEthernetPreference.setEnabled((mEm != null) && mEm.isAvailable());
            } else {
                getPreferenceScreen().removePreference(mEthernetPreference);
            }
        }*/

        // Manually set dependencies for Wifi when not toggleable.
        Preference vpnPreference= findPreference(KEY_VPN_SETTINGS);
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_WIFI)) {
        	vpnPreference.setDependency(KEY_TOGGLE_AIRPLANE);
        }
        // Disable VPN.
        // TODO: http://b/23693383
        if (!isAdmin || RestrictedLockUtils.hasBaseUserRestriction(activity,
                UserManager.DISALLOW_CONFIG_VPN, UserHandle.myUserId())) {
        	wirelessCategory.removePreference(vpnPreference);
        } else
        	vpnPreference.setSummary(isVpnConnected() 
            		? activity.getString(R.string.vpn_connected) : activity.getString(R.string.vpn_not_connected));

        // Manually set dependencies for Bluetooth when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_BLUETOOTH)) {
            // No bluetooth-dependent items in the list. Code kept in case one is added later.
        }

        // Manually set dependencies for NFC when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.Global.RADIO_NFC)) {
            findPreference(KEY_TOGGLE_NFC).setDependency(KEY_TOGGLE_AIRPLANE);
            findPreference(KEY_ANDROID_BEAM_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }

        // Remove NFC if not available
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (mNfcAdapter == null) {
        	wirelessCategory.removePreference(nfc);  //getPreferenceScreen().removePreference(nfc);
        	wirelessCategory.removePreference(androidBeam);  //getPreferenceScreen().removePreference(androidBeam);
            mNfcEnabler = null;
        }

        /* remove mobile_network_settings
         * remove manage_mobile_plan
         * remove view_verizon_account
         * tim++
        // Remove Mobile Network Settings and Manage Mobile Plan for secondary users,
        // if it's a wifi-only device.
        if (!isAdmin || Utils.isWifiOnly(getActivity()) ||
                RestrictedLockUtils.hasBaseUserRestriction(activity,
                        UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, UserHandle.myUserId())) {
            removePreference(KEY_MOBILE_NETWORK_SETTINGS);
            removePreference(KEY_MANAGE_MOBILE_PLAN);
        }

        // Asus Jenpang begin: remove Manage Mobile Plan if mobile provisioning url is empty
        if (mCm!= null && TextUtils.isEmpty(mCm.getMobileProvisioningUrl())) {
            removePreference(KEY_MANAGE_MOBILE_PLAN);
        }
        // Asus Jenpang end: remove Manage Mobile Plan if mobile provisioning url is empty
        // +++ rock_huang@20151117: Add View Verizon Account entry
        // VRZ VZ_REQ_ACTIVATIONUI_38766
        // Only Tablet or Prepaid Smartphone has view_verizon_account
        boolean bIsPostpaidSmartphone = true;
        if (SystemProperties.getInt("ro.asus.is_verizon_device", 0) != 1 ||
            (Utils.isVoiceCapable(getActivity()) && bIsPostpaidSmartphone)) {
            try {
                removePreference("view_verizon_account");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // --- rock_huang@20151117: Add View Verizon Account entry

        // Remove Mobile Network Settings and Manage Mobile Plan
        // if config_show_mobile_plan sets false.
        final boolean isMobilePlanEnabled = this.getResources().getBoolean(
                R.bool.config_show_mobile_plan);
        if (!isMobilePlanEnabled) {
            Preference pref = findPreference(KEY_MANAGE_MOBILE_PLAN);
            if (pref != null) {
                removePreference(KEY_MANAGE_MOBILE_PLAN);
            }
        }
        */

        // Remove Airplane Mode settings if it's a stationary device such as a TV.
        if (mPm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
        	wirelessCategory.removePreference(mAirplaneModePreference);
        }

        /* remove proxy_settings
         * tim++
        // Enable Proxy selector settings if allowed.
        Preference mGlobalProxy = findPreference(KEY_PROXY_SETTINGS);
        final DevicePolicyManager mDPM = (DevicePolicyManager)
                activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // proxy UI disabled until we have better app support
        getPreferenceScreen().removePreference(mGlobalProxy);
        mGlobalProxy.setEnabled(mDPM.getGlobalProxyAdmin() == null);
        */

        /* remove tether_settings
         * tim++
        // Disable Tethering if it's not allowed or if it's a wifi-only device
        final ConnectivityManager cm =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        final boolean adminDisallowedTetherConfig = RestrictedLockUtils.checkIfRestrictionEnforced(
                activity, UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.myUserId()) != null;
        if ((!cm.isTetheringSupported() && !adminDisallowedTetherConfig) ||
                RestrictedLockUtils.hasBaseUserRestriction(activity,
                        UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.myUserId())) {
            getPreferenceScreen().removePreference(findPreference(KEY_TETHER_SETTINGS));
        } else if (!adminDisallowedTetherConfig) {
            Preference p = findPreference(KEY_TETHER_SETTINGS);
            p.setTitle(com.android.settingslib.Utils.getTetheringLabel(cm));

            // Grey out if provisioning is not available.
            p.setEnabled(!TetherSettings
                    .isProvisioningNeededButUnavailable(getActivity()));
        }
        */
        
        /* remove dms_settings
         * tim++
        // Remove DMSSettingsPreference if DLNAService package doesn't exist.
        // Get resource from DLNAService to set preference title and summary
        Preference dmsPref = findPreference(KEY_DMS_SETTINGS);
        if (dmsPref != null) {
            if (DMSSettings.isSupportDLNA(activity)) {
                dmsPref.setTitle(getString(activity, DMSSettings.DLNA_PACKAGE_NAME, "dms"));
                dmsPref.setSummary(getString(activity, DMSSettings.DLNA_PACKAGE_NAME, "desc_dms"));
            } else {
                getPreferenceScreen().removePreference(dmsPref);
            }
        }
        */

        // Add PlayTo in AsusSettings
        // Update PlayTo preference: remove it if need
        Preference playToPref = (Preference) findPreference(KEY_PLAY_TO_SETTINGS);
        if (playToPref != null) {
            if (DisplayManager.isPlayToExist(activity)) {
                if (Utils.isVerizon()) {
                    playToPref.setTitle(activity.getString(R.string.vzw_playto_title));
                    playToPref.setSummary(activity.getString(R.string.vzw_playto_summary));
                } else {
//                    playToPref.setTitle(getString(activity, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_APP_NAME));
                    playToPref.setSummary(getString(activity, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_SUMMARY));
                }

            }
            else {
            	wirelessCategory.removePreference(playToPref);  //getPreferenceScreen().removePreference(playToPref);
            }
        }
        // +++ Gary_Hsu@asus.com: VZ_REQ_UI_41001
        loadCellBroadcastPrefs();
        // --- Gary_Hsu@asus.com: VZ_REQ_UI_41001
        
        //++ tim
        //remove others
        removePreference(KEY_ETHERNET_SETTINGS);
        removePreference(KEY_WIMAX_SETTINGS);
        removePreference(KEY_TETHER_SETTINGS);
        removePreference(KEY_PROXY_SETTINGS);
        removePreference(KEY_MOBILE_NETWORK_SETTINGS);
        removePreference(KEY_MANAGE_MOBILE_PLAN);
        removePreference(KEY_WFC_SETTINGS);
        removePreference(KEY_DMS_SETTINGS);
        removePreference("view_verizon_account");

        removePreference(KEY_NETWORK_RESET);
        removePreference("cell_broadcast_settings");
 
        //-- tim
    }

    @Override
    public void onStart() {
        super.onStart();
        // Add PlayTo in AsusSettings
        // Register PlayTo receiver
        getActivity().registerReceiver(mPlayToStatusReceiver, new IntentFilter(ACTION_PLAYTO_STATUS_CHANGED));

        // +++ ckenken (ChiaHsiang_Kuo) @ 20170123 Verizon requirement: change Cellular network title to Mobile Network
        mMobileNetworkSettingsPreference = (RestrictedPreference) findPreference(KEY_MOBILE_NETWORK_SETTINGS);
        if (AsusTelephonyUtils.isVerizon()) {
            mMobileNetworkSettingsPreference.setTitle(R.string.vzw_network_settings_title);
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20170123 Verizon requirement: change Cellular network title to Mobile Network

//        if (mEm != null) {
//            mEm.addListener(mEthernetListener);
//        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mAirplaneModeEnabler.resume();
        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
        /* remove ethernet_settings
         * 
        if (mEthernetPreference != null) {
            mEthernetPreference.setEnabled((mEm != null) && mEm.isAvailable());
        }*/
        
        /* remove wifi_calling_settings
         * 
        // update WFC setting
        final Context context = getActivity();
        // +++ AMAX @ 20170119 7.1.1 Porting
        if (ImsManager.isWfcEnabledByPlatform(context) &&
                ImsManager.isWfcProvisionedOnDevice(context) && !AsusTelephonyUtils.isVerizon()) {
        // --- AMAX @ 20170119 7.1.1 Porting
            getPreferenceScreen().addPreference(mButtonWfc);

            mButtonWfc.setSummary(WifiCallingSettings.getWfcModeSummary(
                    context, ImsManager.getWfcMode(context, mTm.isNetworkRoaming())));
        } else {
            removePreference(KEY_WFC_SETTINGS);
        }*/
        
        final Activity activity = getActivity();
        Preference vpnPreference = findPreference(KEY_VPN_SETTINGS);
        if (vpnPreference != null)
        	vpnPreference.setSummary(isVpnConnected() 
        		? activity.getString(R.string.vpn_connected) : activity.getString(R.string.vpn_not_connected));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (!TextUtils.isEmpty(mManageMobilePlanMessage)) {
            outState.putString(SAVED_MANAGE_MOBILE_PLAN_MSG, mManageMobilePlanMessage);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mAirplaneModeEnabler.pause();
        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
    }

    // Add PlayTo in AsusSettings
    @Override
    public void onStop() {
        super.onStop();

        // Unregister PlayTo receiver
        getActivity().unregisterReceiver(mPlayToStatusReceiver);
//        if (mEm != null) {
//            mEm.removeListener(mEthernetListener);
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            Boolean isChoiceYes = data.getBooleanExtra(EXIT_ECM_RESULT, false);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_more_networks;
    }
    
    //tim ++
    private boolean isVpnConnected(){
    	boolean isVpnConnected = false;
    	
    	final IConnectivityManager connectivityService = IConnectivityManager.Stub
                .asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));

    	try {
	    	LegacyVpnInfo connectedLegacyVpn = connectivityService.getLegacyVpnInfo(UserHandle.myUserId());
	        if (connectedLegacyVpn != null && connectedLegacyVpn.state == LegacyVpnInfo.STATE_CONNECTED) {
	        	return true;
	        }
	        
	        VpnConfig config = connectivityService.getVpnConfig(UserHandle.myUserId());
	        if (config != null && !config.legacy) {
	        	return true;
	        }
    	} catch (RemoteException e) {
        }
        return isVpnConnected;
    }
    //tim --

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                // Remove wireless settings from search in demo mode
                if (UserManager.isDeviceInDemoMode(context)) {
                    return Collections.emptyList();
                }
                SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.wireless_settings;
                return Arrays.asList(sir);
            }
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.radio_controls_title);
                data.screenTitle = res.getString(R.string.radio_controls_title);
                result.add(data);
                return result;
            }
            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final ArrayList<String> result = new ArrayList<String>();

                final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
                final boolean isSecondaryUser = !um.isAdminUser();
                final boolean isWimaxEnabled = !isSecondaryUser
                        && context.getResources().getBoolean(
                        com.android.internal.R.bool.config_wimaxEnabled);
/*                if (!isWimaxEnabled) {
                    result.add(KEY_WIMAX_SETTINGS);
                }*/

                if (isSecondaryUser) { // Disable VPN
                    result.add(KEY_VPN_SETTINGS);
                }

                // Remove NFC if not available
                final NfcManager manager = (NfcManager)
                        context.getSystemService(Context.NFC_SERVICE);
                if (manager != null) {
                    NfcAdapter adapter = manager.getDefaultAdapter();
                    if (adapter == null) {
                        result.add(KEY_TOGGLE_NFC);
                        result.add(KEY_ANDROID_BEAM_SETTINGS);
                    }
                }

                // Remove Mobile Network Settings and Manage Mobile Plan if it's a wifi-only device.
/*                if (isSecondaryUser || Utils.isWifiOnly(context)) {
                    result.add(KEY_MOBILE_NETWORK_SETTINGS);
                    result.add(KEY_MANAGE_MOBILE_PLAN);
                }

                // Remove Mobile Network Settings and Manage Mobile Plan
                // if config_show_mobile_plan sets false.
                final boolean isMobilePlanEnabled = context.getResources().getBoolean(
                        R.bool.config_show_mobile_plan);
                if (!isMobilePlanEnabled) {
                    result.add(KEY_MANAGE_MOBILE_PLAN);
                }*/

                final PackageManager pm = context.getPackageManager();

                // Remove Airplane Mode settings if it's a stationary device such as a TV.
                if (pm.hasSystemFeature(PackageManager.FEATURE_TELEVISION)) {
                    result.add(KEY_TOGGLE_AIRPLANE);
                }

                // proxy UI disabled until we have better app support
                result.add(KEY_PROXY_SETTINGS);

                // Disable Tethering if it's not allowed or if it's a wifi-only device
/*                ConnectivityManager cm = (ConnectivityManager)
                        context.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (isSecondaryUser || !cm.isTetheringSupported()) {
                    result.add(KEY_TETHER_SETTINGS);
                }

                // Disable Ethernet if it's not supported
                if (!cm.isNetworkSupported(ConnectivityManager.TYPE_ETHERNET)) {
                    result.add(KEY_ETHERNET_SETTINGS);
                }

                // +++ AMAX @ 20170119 7.1.1 Porting

                if (!ImsManager.isWfcEnabledByPlatform(context) ||
                        !ImsManager.isWfcProvisionedOnDevice(context) && AsusTelephonyUtils.isVerizon()) {
                // --- AMAX @ 20170119 7.1.1 Porting
                    result.add(KEY_WFC_SETTINGS);
                }

                if (RestrictedLockUtils.hasBaseUserRestriction(context,
                        UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId())) {
                    result.add(KEY_NETWORK_RESET);
                }
                if (!DMSSettings.isSupportDLNA(context)) {
                    result.add(KEY_DMS_SETTINGS);
                }*/

                if (!DisplayManager.isPlayToExist(context)) {
                    result.add(KEY_PLAY_TO_SETTINGS);
                }

                    result.add(KEY_NETWORK_RESET);

                result.add(KEY_MANAGE_MOBILE_PLAN);
                result.add(KEY_ETHERNET_SETTINGS);
                result.add(KEY_WIMAX_SETTINGS);
                result.add(KEY_TETHER_SETTINGS);
                result.add(KEY_MOBILE_NETWORK_SETTINGS);
                result.add(KEY_WFC_SETTINGS);
                result.add(KEY_DMS_SETTINGS);
                result.add("view_verizon_account");
                return result;
            }
        };

    // Add PlayTo in AsusSettings ++
    // PlayTo broadcast receiver
    private final BroadcastReceiver mPlayToStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Preference playToPref = (Preference) findPreference(KEY_PLAY_TO_SETTINGS);
            if (playToPref == null || !DisplayManager.isPlayToExist(context)) {
                return;
            }

            int playToState = intent.getExtras().getInt(KEY_ACTIVE_TARGET_STATUS);
            String deviceName = intent.getExtras().getString(KEY_ACTIVE_TARGET_FRIENDLY_NAME);
            switch (playToState) {
                case REMOTE_TARGET_STATE_NOT_CONNECTED:
                    if (Utils.isVerizon()) {
                        playToPref.setSummary(context.getString(R.string.vzw_playto_summary));
                    } else {
                        playToPref.setSummary(getString(context, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_SUMMARY));
                    }
                    break;
                case REMOTE_TARGET_STATE_CONNECTING:
                    playToPref.setSummary(String.format(getString(context, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_NOTIFICATION_CONNECTING_MESSAGE), deviceName));
                    break;
                case REMOTE_TARGET_STATE_CONNECTED:
                    playToPref.setSummary(String.format(getString(context, DisplayManager.PLAYTO_PACKAGE_NAME, RES_PLAY_TO_NOTIFICATION_CONNECTED_MESSAGE), deviceName));
                    break;
            }
        }
    };

    public static String getString(Context context, String packageName, String resId) {
        Resources res = null;
        try {
            res = context.getPackageManager().getResourcesForApplication(packageName);
            int resourceId = res.getIdentifier(String.format( "%s:string/%s", packageName, resId), null, null);
            if (resourceId != 0) {
                return "" + context.getPackageManager().getText(packageName, resourceId, null);
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            Log.w(TAG, "NameNotFoundException: " + packageName);
        }
        return resId;
    }
    // Add PlayTo in AsusSettings --

// +++ AMAX @ 20170119 7.1.1 Porting
    // +++ Gary_Hsu@asus.com: VZ_REQ_UI_41001
    private void loadCellBroadcastPrefs() {
        final int myUserId = UserHandle.myUserId();
        final boolean isSecondaryUser = myUserId != UserHandle.USER_OWNER;
        final boolean isUserRestriction = mUm
                .hasUserRestriction(UserManager.DISALLOW_CONFIG_CELL_BROADCASTS);
        final boolean isWifiOnly = Utils.isWifiOnly(getActivity());
        // Enable link to CMAS app settings depending on the value in
        // config.xml.
        boolean isCellBroadcastAppLinkEnabled = getResources()
                .getBoolean(com.android.internal.R.bool.config_cellBroadcastAppLinks);
        try {
            if (isCellBroadcastAppLinkEnabled) {
                String applicationEnabledSetting = AsusTelephonyUtils.isVerizon()
                        ? "com.asus.cellbroadcastreceiver" : "com.android.cellbroadcastreceiver";
                if (mPm.getApplicationEnabledSetting(
                        applicationEnabledSetting) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    isCellBroadcastAppLinkEnabled = false; // CMAS app disabled
                }
            }
        } catch (IllegalArgumentException ignored) {
            isCellBroadcastAppLinkEnabled = false; // CMAS app not installed
        }
        Log.d(TAG, "loadCellBroadcastPrefs, isSecondaryUser = " + isSecondaryUser
                + " , isUserRestriction = " + isUserRestriction + " , isWifiOnly = " + isWifiOnly
                + " ,isCellBroadcastAppLinkEnabled = " + isCellBroadcastAppLinkEnabled);
        if (isSecondaryUser || isUserRestriction || !isCellBroadcastAppLinkEnabled || isWifiOnly) {
            PreferenceScreen prefCBScreen = (PreferenceScreen)findPreference(
                    KEY_CELL_BROADCAST_SETTINGS);
            getPreferenceScreen().removePreference(prefCBScreen);
        }
    }
    // --- Gary_Hsu@asus.com: VZ_REQ_UI_41001
// --- AMAX @ 20170119 7.1.1 Porting


    /* remove ethernet_settings
     * tim ++
    private EthernetManager.Listener mEthernetListener = new EthernetManager.Listener() {
        @Override
        public void onAvailabilityChanged(boolean isAvailable) {
            Log.d(TAG, "EthernetManager.Listener.onAvailabilityChanged");
                mEthernetPreference.setEnabled((mEm != null) && mEm.isAvailable());
        }
    };*/

}
