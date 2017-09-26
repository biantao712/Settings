package com.android.settings.switchenabler;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.WirelessSettings;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.bluetooth.DockService.DockBluetoothCallback;
import com.android.settings.bluetooth.Utils;
import com.android.settingslib.bluetooth.LocalBluetoothAdapter;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager.BluetoothManagerCallback;
import com.android.settingslib.bluetooth.Utils.ErrorListener;
import com.android.settingslib.WirelessUtils;
import com.android.settings.search.Index;
import com.android.settings.widget.SwitchBar;


public class BluetoothSwitchEnabler extends AbstractEnabler implements CompoundButton.OnCheckedChangeListener{
    private Context mContext;
    private Switch mSwitch;
    private boolean mValidListener;
    private final LocalBluetoothAdapter mLocalAdapter;
    private final IntentFilter mIntentFilter;

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

    public BluetoothSwitchEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mValidListener = false;

        LocalBluetoothManager manager = Utils.getLocalBtManager(context);
        if (manager == null) {
            // Bluetooth is not supported
            mLocalAdapter = null;
            mSwitch.setEnabled(false);
        } else {
            mLocalAdapter = manager.getBluetoothAdapter();
        }
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mSwitch.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                setChecked(true);
                mSwitch.setEnabled(true);
                //updateSearchIndex(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitch.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
                setChecked(false);
                mSwitch.setEnabled(true);
                //updateSearchIndex(false);
                break;
            default:
                setChecked(false);
                mSwitch.setEnabled(true);
                //updateSearchIndex(false);
        }
    }

    private void setChecked(boolean isChecked) {
        if (isChecked != mSwitch.isChecked()) {
            // set listener to null, so onCheckedChanged won't be called
            // if the checked status on Switch isn't changed by user click
            if (mValidListener) {
                mSwitch.setOnCheckedChangeListener(null);
            }
            mSwitch.setChecked(isChecked);
            if (mValidListener) {
                mSwitch.setOnCheckedChangeListener(this);
            }
        }
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub
        // Show toast message if Bluetooth is not allowed in airplane mode
        if (com.android.settings.Utils.isVerizonSKU() && com.android.settings.Utils.isCustomizedDeviceAdminActive(mContext)) {
            final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            if(um != null && um.hasUserRestriction(UserManager.DISALLOW_CONFIG_BLUETOOTH)) {
                setChecked(false);
                return;
            }
        }

        if (isChecked &&
                !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_BLUETOOTH)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off
            buttonView.setChecked(false);
        }

        if (mLocalAdapter != null) {
            mLocalAdapter.setBluetoothEnabled(isChecked);
        }
        mSwitch.setEnabled(false);

    }

    @Override
    public void setSwitch(Switch switch_) {
        // TODO Auto-generated method stub
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        // Bluetooth state is not sticky, so set it manually
        if (mLocalAdapter != null) {
            handleStateChanged(mLocalAdapter.getBluetoothState());
        }
        mSwitch.setOnCheckedChangeListener(this);

    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
        if (mLocalAdapter == null) {
            mSwitch.setEnabled(false);
            return;
        }

        mContext.registerReceiver(mReceiver, mIntentFilter);
        mValidListener = true;
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub
        if (mLocalAdapter == null) {
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnCheckedChangeListener(null);
        mValidListener = false;
    }

}
