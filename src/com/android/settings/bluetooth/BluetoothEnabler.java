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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.search.Index;
import com.android.settings.util.VerizonHelpUtils;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import android.support.v7.preference.Preference;
import android.support.v14.preference.SwitchPreference;

/**
 * BluetoothEnabler is a helper to manage the Bluetooth on/off checkbox
 * preference. It turns on/off Bluetooth and ensures the summary of the
 * preference reflects the current state.
 */
public final class BluetoothEnabler implements SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceChangeListener {
    private Context mContext;
    private Switch mSwitch;
    private SwitchBar mSwitchBar;
    private boolean mValidListener;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final IntentFilter mIntentFilter;
    private SwitchPreference mSwitchPreference;

    private static final String EVENT_DATA_IS_BT_ON = "is_bluetooth_on";
    private static final int EVENT_UPDATE_INDEX = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_INDEX:
                    final boolean isBluetoothOn = msg.getData().getBoolean(EVENT_DATA_IS_BT_ON);
                    Index.getInstance(mContext).updateFromClassNameResource(
                            BluetoothSettings.class.getName(), true, isBluetoothOn);
                    break;
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Broadcast receiver is always running on the UI thread here,
            // so we don't need consider thread synchronization.
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            handleStateChanged(state);
        }
    };

    public BluetoothEnabler(Context context, SwitchPreference switchPreference) {
        mContext = context;
        //mSwitchBar = switchBar;
        mSwitchPreference = switchPreference;
        //mSwitch = switchBar.getSwitch();
        mValidListener = false;

        LocalBluetoothManager manager = Utils.getLocalBtManager(context);
        if (manager == null) {
            // Bluetooth is not supported
            mLocalAdapter = null;
            mSwitchPreference.setEnabled(false);
        } else {
            mLocalAdapter = manager.getBluetoothAdapter();
        }
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    public void setupSwitchBar() {
    //    mSwitchBar.show();
        if (null != mSwitchPreference)
            mSwitchPreference.setOnPreferenceChangeListener(this);
    }

    public void teardownSwitchBar() {
      //  mSwitchBar.hide();
      mSwitchPreference.setOnPreferenceChangeListener(null);
    }

    public void resume(Context context) {
        if (mLocalAdapter == null) {
            mSwitchPreference.setEnabled(false);
            return;
        }

        if (mContext != context) {
            mContext = context;
        }

        // Bluetooth state is not sticky, so set it manually
        handleStateChanged(mLocalAdapter.getBluetoothState());

       // mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchPreference.setOnPreferenceChangeListener(this);
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mValidListener = true;
    }

    public void pause() {
        if (mLocalAdapter == null) {
            return;
        }

        //mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchPreference.setOnPreferenceChangeListener(null);
        mContext.unregisterReceiver(mReceiver);
        mValidListener = false;
    }

    void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mSwitchPreference.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                setChecked(true);
                mSwitchPreference.setEnabled(true);
                updateSearchIndex(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitchPreference.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
                setChecked(false);
                mSwitchPreference.setEnabled(true);
                updateSearchIndex(false);
                break;
            default:
                setChecked(false);
                mSwitchPreference.setEnabled(true);
                updateSearchIndex(false);
        }
    }

    private void setChecked(boolean isChecked) {
        if (isChecked != mSwitchPreference.isChecked()) {
            // set listener to null, so onCheckedChanged won't be called
            // if the checked status on Switch isn't changed by user click
            if (mValidListener) {
                mSwitchPreference.setOnPreferenceChangeListener(null);
            }
            mSwitchPreference.setChecked(isChecked);
            if (mValidListener) {
                mSwitchPreference.setOnPreferenceChangeListener(this);
            }
        }
    }

    private void updateSearchIndex(boolean isBluetoothOn) {
        mHandler.removeMessages(EVENT_UPDATE_INDEX);

        Message msg = new Message();
        msg.what = EVENT_UPDATE_INDEX;
        msg.getData().putBoolean(EVENT_DATA_IS_BT_ON, isBluetoothOn);
        mHandler.sendMessage(msg);
    }

    //Suleman
     @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        boolean status = (Boolean) objValue;
        if (preference == mSwitchPreference) {
            if (mLocalAdapter != null) {
                boolean mTrue = mLocalAdapter.setBluetoothEnabled(status);
                if (status && !mTrue) {
                    mSwitchPreference.setChecked(false);
                    mSwitchPreference.setEnabled(true);
                    return false;
                }
            }
        }

        // +++ ShawnMC_Liu@2016/11/03: Verizon VZ_REQ_DEVHELP_10682 Bluetooth tutorial
        if (status) {
            if(VerizonHelpUtils.isVerizonMachine())
                VerizonHelpUtils.DisplayTutorial(mContext, VerizonHelpUtils.TUTORIAL_BLUETOOTH, VerizonHelpUtils.STEP_2, null);
        }
        // ---


        return true;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
      /*  if (com.android.settings.Utils.isVerizonSKU()) {
            final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            if(um != null && um.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH)) {
                mSwitchBar.setChecked(false);
                return;
            }
        }

        // Show toast message if Bluetooth is not allowed in airplane mode
        if (isChecked &&
                !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_BLUETOOTH)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off
            switchView.setChecked(false);
        }

        MetricsLogger.action(mContext, MetricsEvent.ACTION_BLUETOOTH_TOGGLE, isChecked);

        if (mLocalAdapter != null) {
            boolean status = mLocalAdapter.setBluetoothEnabled(isChecked);
            // If we cannot toggle it ON then reset the UI assets:
            // a) The switch should be OFF but it should still be togglable (enabled = True)
            // b) The switch bar should have OFF text.
            if (isChecked && !status) {
                switchView.setChecked(false);
                mSwitch.setEnabled(true);
                mSwitchBar.setTextViewLabel(false);
                return;
            }
        }
        mSwitch.setEnabled(false);
        // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10682 Bluetooth tutorial
        if (isChecked) {
	        if(VerizonHelpUtils.isVerizonMachine())
            	VerizonHelpUtils.DisplayTutorial(mContext, VerizonHelpUtils.TUTORIAL_BLUETOOTH, VerizonHelpUtils.STEP_2, null);
        }
        // ---*/
    }
	
    // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10682 Bluetooth tutorial
    public void setupTutorial(Context context) {
        final Context appContext = context;
        VerizonHelpUtils.resetTutorialStep(context, VerizonHelpUtils.TUTORIAL_BLUETOOTH);
        if (VerizonHelpUtils.isTutorialEnable(appContext, VerizonHelpUtils.TUTORIAL_BLUETOOTH) == VerizonHelpUtils.TUTORIAL_ENABLE &&
            VerizonHelpUtils.getTutorialStep(appContext, VerizonHelpUtils.TUTORIAL_BLUETOOTH) < VerizonHelpUtils.STEP_0) {
            DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        if (mLocalAdapter.getBluetoothState() == BluetoothAdapter.STATE_ON) {
                            VerizonHelpUtils.DisplayTutorial(appContext, VerizonHelpUtils.TUTORIAL_BLUETOOTH, VerizonHelpUtils.STEP_2, null);
                        } else {
                            VerizonHelpUtils.DisplayTutorial(appContext, VerizonHelpUtils.TUTORIAL_BLUETOOTH, VerizonHelpUtils.STEP_1, null);
                        }
                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        VerizonHelpUtils.setTutorialStep(appContext, VerizonHelpUtils.TUTORIAL_BLUETOOTH, VerizonHelpUtils.STEP_RESET);
                    }
                }
            };
            String bluetooth = appContext.getString(R.string.bluetooth_settings);
            VerizonHelpUtils.showTutorialDialog(appContext, VerizonHelpUtils.INTRODUCTION_DIALOG, clickListener,
            		VerizonHelpUtils.TUTORIAL_BLUETOOTH, VerizonHelpUtils.STEP_0,
                    appContext.getString(R.string.tutorial_introduction_title, bluetooth),
                    appContext.getString(R.string.tutorial_bluetooth_step0_content, bluetooth));
        }
    }
    // ---
}
