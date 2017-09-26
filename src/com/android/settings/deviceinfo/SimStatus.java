/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.telephony.CarrierConfigManager;
import android.os.SystemProperties;
import android.telephony.CellBroadcastMessage;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.AsusTelephonyUtils;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TabWidget;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.DefaultPhoneNotifier;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.DeviceInfoUtils;

import java.util.List;

import static android.content.Context.CARRIER_CONFIG_SERVICE;
import static android.content.Context.TELEPHONY_SERVICE;


/**
 * Display the following information
 * # Phone Number
 * # Network
 * # Roaming
 * # Device Id (IMEI in GSM and MEID in CDMA)
 * # Network type
 * # Operator info (area info cell broadcast for Brazil)
 * # Signal Strength
 *
 */
public class SimStatus extends SettingsPreferenceFragment {
    private static final String TAG = "SimStatus";

    private static final String KEY_DATA_STATE = "data_state";
    private static final String KEY_SERVICE_STATE = "service_state";
    private static final String KEY_OPERATOR_NAME = "operator_name";
    private static final String KEY_ROAMING_STATE = "roaming_state";
    private static final String KEY_NETWORK_TYPE = "network_type";
    private static final String KEY_LATEST_AREA_INFO = "latest_area_info";
    private static final String KEY_PHONE_NUMBER = "number";
    private static final String KEY_SIGNAL_STRENGTH = "signal_strength";
    private static final String KEY_IMEI = "imei";
    private static final String KEY_IMEI_SV = "imei_sv";
    private static final String KEY_ICCID = "iccid";
    private static final String COUNTRY_ABBREVIATION_BRAZIL = "br";
    // Millie_Chang, VZ_REQ_IMS_22873 IMS Registration Status
    private static final String KEY_IMS_STATUS = "ims_registration_status";
    // Millie_Chang VZ_REQ_UI_39934
    private static final String KEY_VERIZON_SIGNAL_STRENGTH = "verizon_signal_strength";

    static final String CB_AREA_INFO_RECEIVED_ACTION =
            "android.cellbroadcastreceiver.CB_AREA_INFO_RECEIVED";

    static final String GET_LATEST_CB_AREA_INFO_ACTION =
            "android.cellbroadcastreceiver.GET_LATEST_CB_AREA_INFO";

    // Require the sender to have this permission to prevent third-party spoofing.
    static final String CB_AREA_INFO_SENDER_PERMISSION =
            "android.permission.RECEIVE_EMERGENCY_BROADCAST";


    private TelephonyManager mTelephonyManager;
    private CarrierConfigManager mCarrierConfigManager;
    private Phone mPhone = null;
    private Resources mRes;
    // +++ AMAX @ 20170119 7.1.1 Porting
    private ImagePreference mSignalStrength; // VZ_REQ_UI_39934
    private Preference mRoamingState; //Millie_Chang@20151216: VZ_REQ_UI_15685 Roaming Settings
    private int miSlotId = 0;
    // --- AMAX @ 20170119 7.1.1 Porting
    private SubscriptionInfo mSir;
    private boolean mShowLatestAreaInfo;
    private boolean mShowICCID;

    // +++ Millie_Chang VZ_REQ_UI_39934
    private static final int NO_SERVICE = 6;
    public static final String ACTION_FEMTOCELL_STATE_CHANGED = "android.intent.action.FEMTOCELL_STATE_CHANGED";
    public static final String FEMTOCELL_STATE = "femtocellState";
    // --- Millie_Chang VZ_REQ_UI_39934

    // Default summary for items
    private String mDefaultText;

    private TabHost mTabHost;
    private TabWidget mTabWidget;
    private ListView mListView;
    private List<SubscriptionInfo> mSelectableSubInfos;
    // +++ rock_huang@20161024
    private boolean mbWifiOnly = false;
    private boolean mbVoiceCapable = false;
    // --- rock_huang@20161024

    private PhoneStateListener mPhoneStateListener;
    private BroadcastReceiver mAreaInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CB_AREA_INFO_RECEIVED_ACTION.equals(action)) {
                Bundle extras = intent.getExtras();
                if (extras == null) {
                    return;
                }
                CellBroadcastMessage cbMessage = (CellBroadcastMessage) extras.get("message");
                if (cbMessage != null && cbMessage.getServiceCategory() == 50
                        && mSir.getSubscriptionId() == cbMessage.getSubId()) {
                    String latestAreaInfo = cbMessage.getMessageBody();
                    updateAreaInfo(latestAreaInfo);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(TAG, "onCreate()");  // +++ AMAX @ 20170119 7.1.1 Porting
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mCarrierConfigManager = (CarrierConfigManager) getSystemService(CARRIER_CONFIG_SERVICE);

        mSelectableSubInfos = SubscriptionManager.from(getContext())
                .getActiveSubscriptionInfoList();

        addPreferencesFromResource(R.xml.device_info_sim_status);

        mRes = getResources();
        mDefaultText = mRes.getString(R.string.device_info_default);
        // Note - missing in zaku build, be careful later...
        // +++ AMAX @ 20170119 7.1.1 Porting
        mSignalStrength = (ImagePreference) findPreference(KEY_SIGNAL_STRENGTH); // VZ_REQ_UI_39934
        mRoamingState = findPreference(KEY_ROAMING_STATE); //Millie_Chang@20151216: VZ_REQ_UI_15685 Roaming Settings
        // +++ AMAX
        mbWifiOnly = Utils.isWifiOnly(getContext());
        mbVoiceCapable = Utils.isVoiceCapable(getContext());
        // --- AMAX
        // --- AMAX @ 20170119 7.1.1 Porting
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (mSelectableSubInfos == null) {
            mSir = null;
        } else {
            mSir = mSelectableSubInfos.size() > 0 ? mSelectableSubInfos.get(0) : null;

            if (mSelectableSubInfos.size() > 1) {
                View view = inflater.inflate(R.layout.icc_lock_tabs, container, false);
                final ViewGroup prefs_container = (ViewGroup) view.findViewById(
                        R.id.prefs_container);
                Utils.prepareCustomPreferencesList(container, view, prefs_container, false);
                View prefs = super.onCreateView(inflater, prefs_container, savedInstanceState);
                prefs_container.addView(prefs);

                mTabHost = (TabHost) view.findViewById(android.R.id.tabhost);
                mTabWidget = (TabWidget) view.findViewById(android.R.id.tabs);
                mListView = (ListView) view.findViewById(android.R.id.list);

                mTabHost.setup();
                mTabHost.setOnTabChangedListener(mTabListener);
                mTabHost.clearAllTabs();

                for (int i = 0; i < mSelectableSubInfos.size(); i++) {
                    // +++ rock_huang@20151130: Format the Tab lab name
                    String tabName = String.valueOf(mSelectableSubInfos.get(i).getDisplayName());
                    if (mSelectableSubInfos.size() < 2) {// single SIM

                    } else {
                        tabName = "SIM " + (i + 1);
                    }
                    mTabHost.addTab(buildTabSpec(String.valueOf(i), tabName));
                    // --- rock_huang@20151130: Format the Tab lab name
                }
                return view;
            }
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updatePhoneInfos();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_SIM_STATUS;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");   // +++ AMAX @ 20170119 7.1.1 Porting
        if (mPhone != null) {
            updatePreference();
            // +++ AMAX @ 20170119 7.1.1 Porting
            Log.d(TAG, "onResume updateSignalStrength mPhone.getPhoneId() = " + mPhone.getPhoneId() + ", miSlotId = " + miSlotId);
            miSlotId = mPhone.getPhoneId();
            Log.d(TAG, "onResume updateSignalStrength mPhone.getPhoneId() = " + mPhone.getPhoneId() + ", NEW miSlotId = " + miSlotId);
            // --- AMAX @ 20170119 7.1.1 Porting
            updateSignalStrength(mPhone.getSignalStrength());
            updateServiceState(mPhone.getServiceState());
            updateDataState();
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                    | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                    | PhoneStateListener.LISTEN_SERVICE_STATE);
            if (mShowLatestAreaInfo) {
                getContext().registerReceiver(mAreaInfoReceiver,
                        new IntentFilter(CB_AREA_INFO_RECEIVED_ACTION),
                        CB_AREA_INFO_SENDER_PERMISSION, null);
                // Ask CellBroadcastReceiver to broadcast the latest area info received
                Intent getLatestIntent = new Intent(GET_LATEST_CB_AREA_INFO_ACTION);
                getContext().sendBroadcastAsUser(getLatestIntent, UserHandle.ALL,
                        CB_AREA_INFO_SENDER_PERMISSION);
            }
        }
        // +++ rock_huang@20151130: Format layout
        try {
            ListView tabLV = null;
            int resId = getResources().getIdentifier("list", "id", "android");
            tabLV = (ListView) getActivity().findViewById(resId);
            int padding_in_dp_LR = 13;
            final float scale = getResources().getDisplayMetrics().density;
            int padding_in_px_LR = (int) (padding_in_dp_LR * scale + 0.5f);
            if (tabLV != null) {
                tabLV.setPadding(padding_in_px_LR, 0, padding_in_px_LR, 0);//Common UI
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // +++ rock_huang@20151130: Format layout
        // +++ Millie_Chang@20151216: VZ_REQ_UI_15685 Roaming Settings
        if (AsusTelephonyUtils.isVerizon()) {
            mRoamingState.setTitle(R.string.vzw_status_roaming);
        }
        // --- Millie_Chang@20151216: VZ_REQ_UI_15685
        // +++ Millie_Chang@20151216: Remove information in SimStatus
        removePreferenceForAsusRequest();
        // --- Millie_Chang@20151216

        // +++ Millie_Chang VZ_REQ_UI_39934
        final IntentFilter femtocellStateFilter = new IntentFilter(ACTION_FEMTOCELL_STATE_CHANGED);
        getContext().registerReceiver(mFemtocellStateReceiver, femtocellStateFilter);
        // --- Millie_Chang VZ_REQ_UI_39934
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");  // +++ AMAX @ 20170119 7.1.1 Porting
        if (mPhone != null) {
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
        if (mShowLatestAreaInfo) {
            getContext().unregisterReceiver(mAreaInfoReceiver);
        }
        getContext().unregisterReceiver(mFemtocellStateReceiver); // Millie_Chang VZ_REQ_UI_39934
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void setSummaryText(String key, String text) {
        Log.d(TAG, "setSummary(): key = " + key + ", " + "text = " + text);  // +++ AMAX @ 20170119 7.1.1 Porting
        if (TextUtils.isEmpty(text)) {
            text = mDefaultText;
        }
        // some preferences may be missing
        final Preference preference = findPreference(key);
        if (preference != null) {
            preference.setSummary(text);
        }
    }

    // +++ AMAX @ 20170119 7.1.1 Porting
    private String getServiceStateString(int state) {
        Log.v(TAG, "getServiceStateString()");

        switch (state) {
            case ServiceState.STATE_IN_SERVICE:
                return mRes.getString(R.string.radioInfo_service_in);
            case ServiceState.STATE_OUT_OF_SERVICE:
            case ServiceState.STATE_EMERGENCY_ONLY:
                return mRes.getString(R.string.radioInfo_service_out);
            case ServiceState.STATE_POWER_OFF:
                return mRes.getString(R.string.radioInfo_service_off);
            default:
                return mRes.getString(R.string.radioInfo_unknown);
        }
    }
    // --- AMAX @ 20170119 7.1.1 Porting

    private void updateNetworkType() {
        // +++ ckenken (ChiaHsiang_Kuo) @ 20161116 TT-907322 If Activity is null, no need to update UI
        if (getActivity() == null) {
            return;
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20161116 TT-907322 If Activity is null, no need to update UI
        // Whether EDGE, UMTS, etc...
        String networktype = null;
        final int subId = mSir.getSubscriptionId();
        final int actualDataNetworkType = mTelephonyManager.getDataNetworkType(
                mSir.getSubscriptionId());
        final int actualVoiceNetworkType = mTelephonyManager.getVoiceNetworkType(
                mSir.getSubscriptionId());
        if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualDataNetworkType) {
            networktype = mTelephonyManager.getNetworkTypeName(actualDataNetworkType);
        } else if (TelephonyManager.NETWORK_TYPE_UNKNOWN != actualVoiceNetworkType) {
            networktype = mTelephonyManager.getNetworkTypeName(actualVoiceNetworkType);
        }

        boolean show4GForLTE = false;
        try {
            Context con = getActivity().createPackageContext("com.android.systemui", 0);
            int id = con.getResources().getIdentifier("config_show4GForLTE",
                    "bool", "com.android.systemui");
            show4GForLTE = con.getResources().getBoolean(id);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "NameNotFoundException for show4GFotLTE");
        }

        if (networktype != null && networktype.equals("LTE") && show4GForLTE) {
            networktype = "4G";
            // +++ Millie_Chang@20160506 : Support display 4G+
            if(SystemProperties.getInt("ril.lteca.mode", 0) != 0) {
                networktype = "4G+";
            }
            // --- Millie_Chang@20160506 : Support display 4G+
        }
        setSummaryText(KEY_NETWORK_TYPE, networktype);
    }

    private void updateDataState() {
        final int state =
                DefaultPhoneNotifier.convertDataState(mPhone.getDataConnectionState());

        String display = mRes.getString(R.string.radioInfo_unknown);

        switch (state) {
            case TelephonyManager.DATA_CONNECTED:
                display = mRes.getString(R.string.radioInfo_data_connected);
                break;
            case TelephonyManager.DATA_SUSPENDED:
                display = mRes.getString(R.string.radioInfo_data_suspended);
                break;
            case TelephonyManager.DATA_CONNECTING:
                display = mRes.getString(R.string.radioInfo_data_connecting);
                break;
            case TelephonyManager.DATA_DISCONNECTED:
                display = mRes.getString(R.string.radioInfo_data_disconnected);
                break;
        }

        setSummaryText(KEY_DATA_STATE, display);
    }

    private void updateServiceState(ServiceState serviceState) {
        // +++ AMAX @ 20170119 7.1.1 Porting
        int voiceState = serviceState.getState();
        String voiceDisplay = getServiceStateString(voiceState);
        String msg = null; // +++ TerryYC@20140610

        int dataState = serviceState.getDataRegState();
        String dataDisplay = getServiceStateString(dataState);
        
        if (ServiceState.STATE_IN_SERVICE != voiceState && ServiceState.STATE_IN_SERVICE != dataState) {
            mSignalStrength.setSummary("0");
            mSignalStrength.setSignalImage(NO_SERVICE); // ++ Millie_Chang VZ_REQ_UI_39934
        }

        // +++ allen_chu@20140418: do not show voice information in data only device
        ///boolean voiceCapable =
                //getResources().getBoolean(com.android.internal.R.bool.config_voice_capable);
        if (mbVoiceCapable) {
            // +++ TerryYC@20140610
            msg = mRes.getString(R.string.status_voice) + ": " + voiceDisplay + " / " + mRes.getString(R.string.status_data) + ": " + dataDisplay;
            // --- TerryYC@20140610
            setSummaryText(KEY_SERVICE_STATE, msg);
        } else {
            // +++ TerryYC@20140610
            // +++ ckenken @ 20160920 Remove "data: "
            msg = dataDisplay;
            // --- ckenken @ 20160920 Remove "data: "
            // --- TerryYC@20140610
            setSummaryText(KEY_SERVICE_STATE, msg);
        }
        // --- allen_chu@20140418: do not show voice information in data only device
        if (serviceState.getRoaming()) {
        // +++ ckenken (ChiaHsiang_Kuo) @ 20160919 N-Porting
            boolean isVoiceRoaming = serviceState.getVoiceRoaming();
            boolean isDataRoaming = serviceState.getDataRoaming();
            Log.d(TAG, "isVoiceRoaming = " + isVoiceRoaming + ", isDataRoaming = " + isDataRoaming);

            String roamingInString = mRes.getString(R.string.radioInfo_roaming_in);
            String roamingNotString = mRes.getString(R.string.radioInfo_roaming_not);
            if (mbVoiceCapable) {
                msg = mRes.getString(R.string.status_voice) + ": " + (isVoiceRoaming ? roamingInString : roamingNotString) +
                     " / " + mRes.getString(R.string.status_data) + ": " + (isDataRoaming ? roamingInString : roamingNotString);
            } else {
                msg = (isDataRoaming ? roamingInString : roamingNotString);
            }
            setSummaryText(KEY_ROAMING_STATE, msg);
        //    setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_in));
        // --- ckenken (ChiaHsiang_Kuo) @ 20160919 N-Porting
        // --- AMAX @ 20170119 7.1.1 Porting
        } else {
            setSummaryText(KEY_ROAMING_STATE, mRes.getString(R.string.radioInfo_roaming_not));
        }
        setSummaryText(KEY_OPERATOR_NAME, serviceState.getOperatorAlphaLong());
    }

    private void updateAreaInfo(String areaInfo) {
        if (areaInfo != null) {
            setSummaryText(KEY_LATEST_AREA_INFO, areaInfo);
        }
    }

    void updateSignalStrength(SignalStrength signalStrength) {
        // +++ ckenken (ChiaHsiang_Kuo) @ 20161011 TT-885981 Check if attached to activity
        if (null == getActivity()) {
            return;
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20161011 TT-885981 Check if attached to activity

        // +++ AMAX
        Log.d(TAG, "updateSignalStrength signalStrength = " + signalStrength);
        if (mPhone != null) {
            Log.d(TAG, "updateSignalStrength mPhone = " + mPhone.getPhoneId() + ", miSlotId = " + miSlotId);
            if (mPhone.getPhoneId() != miSlotId) {
                mSignalStrength.setSummary(getResources().getString(R.string.master_clear_progress_text));
                mSignalStrength.setSignalImage(NO_SERVICE);  // Millie_Chang VZ_REQ_UI_39934
                return;
            }
        }
        // --- AMAX
        if (mSignalStrength != null) {
            final int state = mPhone.getServiceState().getState();
            // +++ AMAX @ 20170119 7.1.1 Porting
            int dataState = mPhone.getServiceState().getDataRegState();
            // --- AMAX @ 20170119 7.1.1 Porting
            Resources r = getResources();

            // +++ AMAX @ 20170119 7.1.1 Porting
            if  (((ServiceState.STATE_OUT_OF_SERVICE == state) ||
                    (ServiceState.STATE_POWER_OFF == state)) &&
                ((ServiceState.STATE_OUT_OF_SERVICE == dataState) ||
                    (ServiceState.STATE_POWER_OFF == dataState))) {
                mSignalStrength.setSummary("0");
                mSignalStrength.setSignalImage(NO_SERVICE);  // Millie_Chang VZ_REQ_UI_39934
                return;
            }
            // --- AMAX @ 20170119 7.1.1 Porting

            int signalDbm = signalStrength.getDbm();
            int signalAsu = signalStrength.getAsuLevel();

            if (-1 == signalDbm) {
                signalDbm = 0;
            }

            if (-1 == signalAsu) {
                signalAsu = 0;
            }

            Log.d(TAG, "updateSignalStrength signalDbm = " + signalDbm + ", signalAsu = " + signalAsu);  // +++ AMAX @ 20170119 7.1.1 Porting
            mSignalStrength.setSummary(r.getString(R.string.sim_signal_strength,
                        signalDbm, signalAsu));
            // +++ Millie_Chang VZ_REQ_UI_39934
            int level = signalStrength.getLevel();
            Log.d(TAG,"signalStrength.getLevel() = " + signalStrength.getLevel());
            mSignalStrength.setSignalImage(level);
            // --- Millie_Chang VZ_REQ_UI_39934
        }
    }

    private void updatePreference() {
        if (mPhone.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
            // only show area info when SIM country is Brazil
            if (COUNTRY_ABBREVIATION_BRAZIL.equals(mTelephonyManager.getSimCountryIso(
                            mSir.getSubscriptionId()))) {
                mShowLatestAreaInfo = true;
            }
        }
        PersistableBundle carrierConfig = mCarrierConfigManager.getConfigForSubId(
                mSir.getSubscriptionId());
        mShowICCID = carrierConfig.getBoolean(
                CarrierConfigManager.KEY_SHOW_ICCID_IN_SIM_STATUS_BOOL);


        // If formattedNumber is null or empty, it'll display as "Unknown".
        setSummaryText(KEY_PHONE_NUMBER,
                DeviceInfoUtils.getFormattedPhoneNumber(getContext(), mSir));
        // +++ Millie_Chang@20151211 : VZ_REQ_UI_15765 - IMEI or ICCID shall be displayed in 3 or 4 digit chunks
        setSummaryText(KEY_IMEI, AsusTelephonyUtils.formatIdentityDisplay(mPhone.getImei()));
        // --- Millie_Chang@20151211 : VZ_REQ_UI_15765
        setSummaryText(KEY_IMEI_SV, mPhone.getDeviceSvn());

        if (!mShowICCID) {
            removePreferenceFromScreen(KEY_ICCID);
        } else {
            // Get ICCID, which is SIM serial number
            String iccid = mTelephonyManager.getSimSerialNumber(mSir.getSubscriptionId());
            setSummaryText(KEY_ICCID, iccid);
        }

        if (!mShowLatestAreaInfo) {
            removePreferenceFromScreen(KEY_LATEST_AREA_INFO);
        }

        // +++ Mark_Huang@20151118: Verizon VZ_REQ_GLOBAL_11797
        if ((mTelephonyManager.getSimState() == TelephonyManager.SIM_STATE_ABSENT) ||
                mbWifiOnly) {
            Log.d(TAG, "updatePreference(): Remove ICCID preference since SIM state = " +
                    mTelephonyManager.getSimState() + ", mbWifiOnly = " + mbWifiOnly);
            removePreferenceFromScreen(KEY_ICCID);
        } else {
            String simSerialNumber = mTelephonyManager.getSimSerialNumber(mSir.getSubscriptionId());
            if (!TextUtils.isEmpty(simSerialNumber)) {
                // +++ Millie_Chang@20151211 : VZ_REQ_UI_15765 - IMEI or ICCID shall be displayed in 3 or 4 digit chunks
                setSummaryText(KEY_ICCID, AsusTelephonyUtils.formatIdentityDisplay(simSerialNumber));
                // --- Millie_Chang@20151211 : VZ_REQ_UI_15765
            } else {
                Log.d(TAG, "updatePreference(): Failed to set ICCID since getSimSerialNumber returns null");
            }
        }
        // --- Mark_Huang@20151118: Verizon VZ_REQ_GLOBAL_11797
        //  +++ Millie_Chang, VZ_REQ_IMS_22873 IMS Registration Status
        if (AsusTelephonyUtils.isVerizon()) {
            boolean isImsRegistered = false;
            if (mPhone != null) {
                isImsRegistered = mPhone.isImsRegistered();
            }
            setSummaryText(KEY_IMS_STATUS, getResources().getString(isImsRegistered
                            ? R.string.ims_registration_status_registered
                            : R.string.ims_registration_status_not_registered));
        } else {
            removePreferenceFromScreen(KEY_IMS_STATUS);
        }
        //  ---  Millie_Chang, VZ_REQ_IMS_22873 IMS Registration Status
    }

    private void updatePhoneInfos() {
        Log.d(TAG, "updatePhoneInfos miSlotId = " + miSlotId);   // +++ AMAX @ 20170119 7.1.1 Porting
        if (mSir != null) {
            // TODO: http://b/23763013
            Log.d(TAG, "updatePhoneInfos mSir.getSubscriptionId() = " + mSir.getSubscriptionId());  // +++ AMAX @ 20170119 7.1.1 Porting
            final Phone phone = PhoneFactory.getPhone(SubscriptionManager.getPhoneId(
                        mSir.getSubscriptionId()));
            if (UserManager.get(getContext()).isAdminUser()
                    && SubscriptionManager.isValidSubscriptionId(mSir.getSubscriptionId())) {
                if (phone == null) {
                    Log.e(TAG, "Unable to locate a phone object for the given Subscription ID.");
                    return;
                }

                mPhone = phone;
                Log.d(TAG, "updatePhoneInfos mPhone.getPhoneId() = " + mPhone.getPhoneId());  // +++ AMAX @ 20170119 7.1.1 Porting
                mPhoneStateListener = new PhoneStateListener(mSir.getSubscriptionId()) {
                    @Override
                    public void onDataConnectionStateChanged(int state) {
                        Log.d(TAG, "onDataConnectionStateChanged()");  // +++ AMAX @ 20170119 7.1.1 Porting
                        updateDataState();
                        updateNetworkType();
                    }

                    @Override
                    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                        Log.d(TAG, "PhoneStateListener onSignalStrengthsChanged updateSignalStrength mPhone.getPhoneId() = " + mPhone.getPhoneId() + ", miSlotId = " + miSlotId);  // +++ AMAX @ 20170119 7.1.1 Porting
                        updateSignalStrength(signalStrength);
                    }

                    @Override
                    public void onServiceStateChanged(ServiceState serviceState) {
                        updateServiceState(serviceState);
                        // +++ AMAX @ 20170119 7.1.1 Porting
                        Log.d(TAG, "PhoneStateListener onServiceStateChanged updateSignalStrength mPhone.getPhoneId() = " + mPhone.getPhoneId() + ", miSlotId = " + miSlotId);
                        updateSignalStrength(mPhone.getSignalStrength()); //+++ Millie_Chang@20160128 : TT-735691
                        // --- AMAX @ 20170119 7.1.1 Porting
                    }
                };
            }
        }
    }
    private OnTabChangeListener mTabListener = new OnTabChangeListener() {
        @Override
        public void onTabChanged(String tabId) {
            final int slotId = Integer.parseInt(tabId);
            miSlotId = slotId;   // +++ AMAX @ 20170119 7.1.1 Porting
            mSir = mSelectableSubInfos.get(slotId);
            // +++ QC
            if (mPhoneStateListener != null) {
                mTelephonyManager.listen(mPhoneStateListener,
                        PhoneStateListener.LISTEN_NONE);
            }
            // --- QC
            // The User has changed tab; update the SIM information.
            updatePhoneInfos();
            mTelephonyManager.listen(mPhoneStateListener,
                    PhoneStateListener.LISTEN_DATA_CONNECTION_STATE
                    | PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
                    | PhoneStateListener.LISTEN_SERVICE_STATE);
            // +++ rock_huang@20160516: Fix AOSP summary delay
            if (mPhone != null) {
                Log.d(TAG, "onTabChanged updateSignalStrength mPhone.getPhoneId() = " + mPhone.getPhoneId() + ", miSlotId = " + miSlotId);
                updateSignalStrength(mPhone.getSignalStrength());
                updateServiceState(mPhone.getServiceState());
            }
            // --- rock_huang@20160516: Fix AOSP summary delay
            updateDataState();
            updateNetworkType();
            updatePreference();
        }
    };

    private TabContentFactory mEmptyTabContent = new TabContentFactory() {
        @Override
        public View createTabContent(String tag) {
            return new View(mTabHost.getContext());
        }
    };

    private TabSpec buildTabSpec(String tag, String title) {
        return mTabHost.newTabSpec(tag).setIndicator(title).setContent(
                mEmptyTabContent);
    }
// +++ AMAX @ 20170119 7.1.1 Porting
    private void removePreferenceForAsusRequest() {
        // +++ Millie_Chang@20151216 : Remove IMEI/ IMEI SV information in SimStatus
        Log.d(TAG,"removePreferenceForAsusRequest()");
        removePreferenceFromScreen(KEY_IMEI);
        removePreferenceFromScreen(KEY_IMEI_SV);
        if (AsusTelephonyUtils.isVerizon()) {
            removePreferenceFromScreen(KEY_PHONE_NUMBER);
        } else {
            // +++ tony_shih@20170414 fix TT-985152
            removePreferenceFromScreen(KEY_IMS_STATUS);
            // --- tony_shih@20170414 fix TT-985152
        }
        // --- Millie_Chang@20151216 : Remove IMEI/ IMEI SV information in SimStatus
     }

    // +++ Millie_Chang VZ_REQ_UI_39934
    private final BroadcastReceiver mFemtocellStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isFemtocellState = intent.getExtras().getBoolean(FEMTOCELL_STATE, false);
            if (mSignalStrength != null) {
                if(isFemtocellState) {
                    mSignalStrength.showFemtocell(true);
                } else {
                    mSignalStrength.showFemtocell(false);
                }
            }
        }
    };
    // --- Millie_Chang VZ_REQ_UI_39934
// --- AMAX @ 20170119 7.1.1 Porting
}
