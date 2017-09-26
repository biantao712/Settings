/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.preference.CheckBoxPreference;
//import android.preference.Preference;
//import android.preference.SwitchPreference;
import android.util.Log;
import android.view.ViewConfiguration;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneStateIntentReceiver;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settingslib.WirelessUtils;

import android.telephony.ServiceState;
import android.telephony.TelephonyManager; // ASUS_BSP+ "Set airplane mode unclickable when it's processing"
import android.os.CountDownTimer;

public class AirplaneModeEnabler implements Preference.OnPreferenceChangeListener {

    private final Context mContext;

    private PhoneStateIntentReceiver mPhoneStateReceiver;
    
    private final SwitchPreference mSwitchPref;

    private static final int EVENT_SERVICE_STATE_CHANGED = 3;

// +++ AMAX
    private static final int EVENT_ENABLE_SWITCH_PREF = 100;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private final IntentFilter mIntentFilter;

    private final String TAG = "AirplaneModeEnabler";


// ASUS_BSP+++ "Set airplane mode unclickable when it's processing"
    private boolean mHasPhone;
    private boolean mPhoneProcess;
    private boolean mBtProcess;
    private int mServiceState;
// ASUS_BSP--- "Set airplane mode unclickable when it's processing"
// --- AMAX

    private static final long EVENT_ENABLE_SWITCH_PREF_DELAY = ViewConfiguration.getDoubleTapTimeout(); //2000; //ms

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_SERVICE_STATE_CHANGED:
                    onAirplaneModeChanged();
// +++ AMAX
// ASUS_BSP+++ "Set airplane mode unclickable when it's processing"
                    Log.d(TAG, "EVENT_SERVICE_STATE_CHANGED mHasPhone = " + mHasPhone);
                    if (mHasPhone) {
                        mPhoneProcess = false;
                    }
                    Log.d(TAG, "EVENT_SERVICE_STATE_CHANGED mBtProcess = " + mBtProcess);
                    Log.d(TAG, "EVENT_SERVICE_STATE_CHANGED mServiceState = " + mServiceState);
                    Log.d(TAG, "EVENT_SERVICE_STATE_CHANGED mPhoneStateReceiver.getServiceState().getState() = " + mPhoneStateReceiver.getServiceState().getState());
                    if (!mBtProcess) {
                        if(mServiceState == ServiceState.STATE_POWER_OFF || mPhoneStateReceiver.getServiceState().getState() == ServiceState.STATE_POWER_OFF){
                            if(mServiceState != mPhoneStateReceiver.getServiceState().getState()){
                                mServiceState = mPhoneStateReceiver.getServiceState().getState();
                                Log.d(TAG, "EVENT_SERVICE_STATE_CHANGED Ready to setEnabled true");
                                mHandler.removeMessages(EVENT_ENABLE_SWITCH_PREF);
                                mHandler.sendEmptyMessageDelayed(EVENT_ENABLE_SWITCH_PREF, EVENT_ENABLE_SWITCH_PREF_DELAY);
                            }
                        }
                    }
// ASUS_BSP--- "Set airplane mode unclickable when it's processing"
                    break;
                case EVENT_ENABLE_SWITCH_PREF:
                    if (mSwitchPref != null) {
                        Log.d(TAG, "EVENT_ENABLE_SWITCH_PREF setEnabled NOW");
                        mSwitchPref.setEnabled(true);
                    }
// --- AMAX
                    break;
            }
        }
    };

    private ContentObserver mAirplaneModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onAirplaneModeChanged();
        }
    };

    public AirplaneModeEnabler(Context context, SwitchPreference airplaneModeSwitchPreference) {
        
        mContext = context;
        mSwitchPref = airplaneModeSwitchPreference;

        airplaneModeSwitchPreference.setPersistent(false);
    
        mPhoneStateReceiver = new PhoneStateIntentReceiver(mContext, mHandler);
        mPhoneStateReceiver.notifyServiceState(EVENT_SERVICE_STATE_CHANGED);

        // +++ AMAX
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        // --- AMAX
    }

    public void resume() {
        
        mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(mContext));

        mPhoneStateReceiver.registerIntent();
        mSwitchPref.setOnPreferenceChangeListener(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true,
                mAirplaneModeObserver);
        // +++ AMAX
        mContext.registerReceiver(mReceiver, mIntentFilter);
        // ASUS_BSP+++ "Set airplane mode unclickable when it's processing"
        mHasPhone = (TelephonyManager.getDefault().getDeviceId() != null);
        mPhoneProcess = false;
        mBtProcess = false;
        mSwitchPref.setEnabled(true);
        // ASUS_BSP--- "Set airplane mode unclickable when it's processing"
        // --- AMAX
    }
    
    public void pause() {
        mPhoneStateReceiver.unregisterIntent();
        mSwitchPref.setOnPreferenceChangeListener(null);
        mContext.getContentResolver().unregisterContentObserver(mAirplaneModeObserver);
        // +++ AMAX
        mContext.unregisterReceiver(mReceiver);
        // --- AMAX
    }

    private void setAirplaneModeOn(boolean enabling) {
        // Change the system setting
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 
                                enabling ? 1 : 0);
        // Update the UI to reflect system setting
        mSwitchPref.setChecked(enabling);
        // +++ AMAX
        // ASUS_BSP+++ "Set airplane mode unclickable when it's processing"
        mHandler.removeMessages(EVENT_ENABLE_SWITCH_PREF);
        mSwitchPref.setEnabled(false);
        if (mHasPhone) {
            mPhoneProcess = true;
        } else {
            mHandler.removeMessages(EVENT_ENABLE_SWITCH_PREF);
            mHandler.sendEmptyMessageDelayed(EVENT_ENABLE_SWITCH_PREF, EVENT_ENABLE_SWITCH_PREF_DELAY);
        }
        // ASUS_BSP--- "Set airplane mode unclickable when it's processing"
        // --- AMAX
        // Post the intent
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enabling);
        mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    /**
     * Called when we've received confirmation that the airplane mode was set.
     * TODO: We update the checkbox summary when we get notified
     * that mobile radio is powered up/down. We should not have dependency
     * on one radio alone. We need to do the following:
     * - handle the case of wifi/bluetooth failures
     * - mobile does not send failure notification, fail on timeout.
     */
    private void onAirplaneModeChanged() {
        // +++ AMAX
        if (mBluetoothAdapter != null) {
            if ((mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_ON) ||
                    (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_TURNING_OFF)) {
                mHandler.removeMessages(EVENT_ENABLE_SWITCH_PREF);
                mSwitchPref.setEnabled(false);
                mBtProcess = true; // ASUS_BSP+ "Set airplane mode unclickable when it's processing"
            }
        }
        // --- AMAX
        mSwitchPref.setChecked(WirelessUtils.isAirplaneModeOn(mContext));
    }
    
    /**
     * Called when someone clicks on the checkbox preference.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (Boolean.parseBoolean(
                    SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode, do not update database at this point
        } else {
            Boolean value = (Boolean) newValue;
            MetricsLogger.action(mContext, MetricsEvent.ACTION_AIRPLANE_TOGGLE, value);
            setAirplaneModeOn(value);
        }
        return true;
    }

    public void setAirplaneModeInECM(boolean isECMExit, boolean isAirplaneModeOn) {
        if (isECMExit) {
            // update database based on the current checkbox state
            setAirplaneModeOn(isAirplaneModeOn);
        } else {
            // update summary
            onAirplaneModeChanged();
        }
    }

// +++ AMAX
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            Log.d(TAG, "BT mReceiver state = " + state);
            // ASUS_BSP+++ "Set airplane mode unclickable when it's processing"
            if ((state == BluetoothAdapter.STATE_ON) || (state == BluetoothAdapter.STATE_OFF)) {
                // STATE_ON = 12 or STATE_OFF = 10
                mBtProcess = false;
            }
            Log.d(TAG, "BT mReceiver unclickable mHasPhone = " + mHasPhone);
            Log.d(TAG, "BT mReceiver unclickable mPhoneProcess = " + mPhoneProcess);

            if (mHasPhone) {
                if (!mPhoneProcess) {
                    Log.d(TAG, "BT mReceiver A Ready to setEnabled true");
                    mHandler.removeMessages(EVENT_ENABLE_SWITCH_PREF);
                    mHandler.sendEmptyMessageDelayed(EVENT_ENABLE_SWITCH_PREF, EVENT_ENABLE_SWITCH_PREF_DELAY);
                    mServiceState = mPhoneStateReceiver.getServiceState().getState();
                }
            } else {
                Log.d(TAG, "BT mReceiver B Ready to setEnabled true");
                mHandler.removeMessages(EVENT_ENABLE_SWITCH_PREF);
                mHandler.sendEmptyMessageDelayed(EVENT_ENABLE_SWITCH_PREF, EVENT_ENABLE_SWITCH_PREF_DELAY);
            }
            // ASUS_BSP--- "Set airplane mode unclickable when it's processing"
        }
    };
// --- AMAX
}
