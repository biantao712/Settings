/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Switch;
import android.widget.Toast;

import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.search.Index;
import com.android.settings.util.VerizonHelpUtils;
//import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.WirelessUtils;

import java.util.concurrent.atomic.AtomicBoolean;

public class WifiEnabler implements Preference.OnPreferenceChangeListener  {
    private Context mContext;
    //timhu  use SwitchPreference instead SwitchBar
    private SwitchPreference mSwitchBar;
    private boolean mListeningToOnSwitchChange = false;
    private AtomicBoolean mConnected = new AtomicBoolean(false);

    private final WifiManager mWifiManager;
    private boolean mStateMachineEvent;
    private final IntentFilter mIntentFilter;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
                handleWifiStateChanged(intent.getIntExtra(
                        WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN));
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
                if (!mConnected.get()) {
                    handleStateChanged(WifiInfo.getDetailedStateOf((SupplicantState)
                            intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE)));
                }
            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                        WifiManager.EXTRA_NETWORK_INFO);
                mConnected.set(info.isConnected());
                handleStateChanged(info.getDetailedState());
                // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10682 Wi-Fi tutorial
		        if(VerizonHelpUtils.isVerizonMachine()){
			        NetworkInfo.DetailedState state = info.getDetailedState();
                	if (state == NetworkInfo.DetailedState.CONNECTED &&
                    	  VerizonHelpUtils.isTutorialEnable(mContext, VerizonHelpUtils.TUTORIAL_WIFI) == VerizonHelpUtils.TUTORIAL_ENABLE) {
                    	  VerizonHelpUtils.DisplayTutorial(mContext, VerizonHelpUtils.TUTORIAL_WIFI, VerizonHelpUtils.STEP_4, null);
                 	}                
		        } 
                // ---
            }
        }
    };

    private static final String EVENT_DATA_IS_WIFI_ON = "is_wifi_on";
    private static final int EVENT_UPDATE_INDEX = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_INDEX:
                    final boolean isWiFiOn = msg.getData().getBoolean(EVENT_DATA_IS_WIFI_ON);
                    Index.getInstance(mContext).updateFromClassNameResource(
                            WifiSettings.class.getName(), true, isWiFiOn);
                    break;
            }
        }
    };

    public WifiEnabler(Context context, SwitchPreference switchBar) {
        mContext = context;
        mSwitchBar = switchBar;

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // The order matters! We really should not depend on this. :(
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        setupSwitchBar();
    }

    public void setupSwitchBar() {
        final int state = mWifiManager.getWifiState();
        handleWifiStateChanged(state);
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.setOnPreferenceChangeListener(this);
            mListeningToOnSwitchChange = true;
        }
//        mSwitchBar.show();
    }

    public void teardownSwitchBar() {
        if (mListeningToOnSwitchChange) {
            mSwitchBar.setOnPreferenceChangeListener(null);
            mListeningToOnSwitchChange = false;
        }
//        mSwitchBar.hide();
    }

    public void resume(Context context) {
        mContext = context;
        // Wi-Fi state is sticky, so just let the receiver update UI
        mContext.registerReceiver(mReceiver, mIntentFilter);
        if (!mListeningToOnSwitchChange) {
            mSwitchBar.setOnPreferenceChangeListener(this);
            mListeningToOnSwitchChange = true;
        }
    }

    public void pause() {
        mContext.unregisterReceiver(mReceiver);
        if (mListeningToOnSwitchChange) {
            mSwitchBar.setOnPreferenceChangeListener(null);
            mListeningToOnSwitchChange = false;
        }
    }

    private void handleWifiStateChanged(int state) {
        // Clear any previous state
//        mSwitchBar.setDisabledByAdmin(null);    //tim++   use RestrictedSwitchPreference instead of SwitchPreference

        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                mSwitchBar.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                setSwitchBarChecked(true);
                mSwitchBar.setEnabled(true);
                updateSearchIndex(true);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                mSwitchBar.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                setSwitchBarChecked(false);
                mSwitchBar.setEnabled(true);
                updateSearchIndex(false);
                break;
            default:
                setSwitchBarChecked(false);
                mSwitchBar.setEnabled(true);
                updateSearchIndex(false);
        }
        if (mayDisableTethering(!mSwitchBar.isChecked())) {
            if (RestrictedLockUtils.hasBaseUserRestriction(mContext,
                    UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.myUserId())) {
                mSwitchBar.setEnabled(false);
            } else {
                final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(mContext,
                    UserManager.DISALLOW_CONFIG_TETHERING, UserHandle.myUserId());
//                mSwitchBar.setDisabledByAdmin(admin);
            }
        }
    }

    private void updateSearchIndex(boolean isWiFiOn) {
        mHandler.removeMessages(EVENT_UPDATE_INDEX);

        Message msg = new Message();
        msg.what = EVENT_UPDATE_INDEX;
        msg.getData().putBoolean(EVENT_DATA_IS_WIFI_ON, isWiFiOn);
        mHandler.sendMessage(msg);
    }

    private void setSwitchBarChecked(boolean checked) {
        mStateMachineEvent = true;
        mSwitchBar.setChecked(checked);
        mStateMachineEvent = false;
    }

    private void handleStateChanged(@SuppressWarnings("unused") NetworkInfo.DetailedState state) {
        // After the refactoring from a CheckBoxPreference to a Switch, this method is useless since
        // there is nowhere to display a summary.
        // This code is kept in case a future change re-introduces an associated text.
        /*
        // WifiInfo is valid if and only if Wi-Fi is enabled.
        // Here we use the state of the switch as an optimization.
        if (state != null && mSwitch.isChecked()) {
            WifiInfo info = mWifiManager.getConnectionInfo();
            if (info != null) {
                //setSummary(Summary.get(mContext, info.getSSID(), state));
            }
        }
        */
    }
    
    /*@Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        
        return true;
    }*/

    /*@Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {*/
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
        	return true;
        }
        
        boolean isChecked = (Boolean) objValue;

        if (Utils.isVerizonSKU() && Utils.isCustomizedDeviceAdminActive(mContext)) {
            UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            if (um != null && um.hasUserRestriction(UserManager.DISALLOW_CONFIG_WIFI)) {
                mSwitchBar.setChecked(false);
                return true;
            }
        }
        // Show toast message if Wi-Fi is not allowed in airplane mode
        if (isChecked && !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_WIFI)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off. No infinite check/listenenr loop.
            mSwitchBar.setChecked(false);
            return true;
        }

        // Disable tethering if enabling Wifi
        if (mayDisableTethering(isChecked)) {
            mWifiManager.setWifiApEnabled(null, false);
        }
        MetricsLogger.action(mContext,
                isChecked ? MetricsEvent.ACTION_WIFI_ON : MetricsEvent.ACTION_WIFI_OFF);
        if (!mWifiManager.setWifiEnabled(isChecked)) {
            // Error
            mSwitchBar.setEnabled(true);
            Toast.makeText(mContext, R.string.wifi_error, Toast.LENGTH_SHORT).show();
        }
        if (isChecked) {
            // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10682 Wi-Fi tutorial
            if(VerizonHelpUtils.isVerizonMachine())
            	VerizonHelpUtils.DisplayTutorial(mContext, VerizonHelpUtils.TUTORIAL_WIFI, VerizonHelpUtils.STEP_2, null);
            // ---
        }
	
	return true;
    }

    private boolean mayDisableTethering(boolean isChecked) {
        final int wifiApState = mWifiManager.getWifiApState();
        return isChecked && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
            (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED));
    }
	
	
    // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10682 Wi-Fi tutorial
    public void setupTutorial(Context context) {    	
        final Context appContext = context;
        VerizonHelpUtils.resetTutorialStep(context, VerizonHelpUtils.TUTORIAL_WIFI);
        if (VerizonHelpUtils.isTutorialEnable(appContext, VerizonHelpUtils.TUTORIAL_WIFI) == VerizonHelpUtils.TUTORIAL_ENABLE &&
            VerizonHelpUtils.getTutorialStep(appContext, VerizonHelpUtils.TUTORIAL_WIFI) < VerizonHelpUtils.STEP_0) {
            DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        if (mSwitchBar.isChecked()) {
                            VerizonHelpUtils.DisplayTutorial(appContext, VerizonHelpUtils.TUTORIAL_WIFI, VerizonHelpUtils.STEP_2, null);
                        } else {
                            VerizonHelpUtils.DisplayTutorial(appContext, VerizonHelpUtils.TUTORIAL_WIFI, VerizonHelpUtils.STEP_1, null);
                        }
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        VerizonHelpUtils.setTutorialStep(appContext, VerizonHelpUtils.TUTORIAL_WIFI, VerizonHelpUtils.STEP_RESET);
                    }
                }
            };
            String wifi = appContext.getString(R.string.wifi_settings);
            VerizonHelpUtils.showTutorialDialog(appContext, VerizonHelpUtils.INTRODUCTION_DIALOG, clickListener,
            		VerizonHelpUtils.TUTORIAL_WIFI, VerizonHelpUtils.STEP_0, appContext.getString(R.string.tutorial_introduction_title, wifi),
                    appContext.getString(R.string.tutorial_wifi_step0_content, wifi));
        }        
    }
    // ---
}
