package com.android.settings.switchenabler;

import java.util.concurrent.atomic.AtomicBoolean;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.WirelessSettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import java.util.concurrent.atomic.AtomicReference;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;

import android.app.AlertDialog;
import com.android.settingslib.WirelessUtils;
import com.android.settings.Utils;

public class WifiSwitchEnabler extends AbstractEnabler implements CompoundButton.OnCheckedChangeListener {
    private static final String LOG_TAG = "WifiSwitchEnabler";
    private Context mContext;
    private Switch mSwitch;

    private AtomicBoolean mConnected = new AtomicBoolean(false);
    private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<BluetoothPan>();
    private boolean mStateMachineEvent;
    private final WifiManager mWifiManager;
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
            }
        }
    };

    public WifiSwitchEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;

        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mIntentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // The order matters! We really should not depend on this. :(
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(mContext, mProfileServiceListener,
                    BluetoothProfile.PAN);
        }

    }

    private void handleWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                mSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                setSwitchChecked(true);
                mSwitch.setEnabled(true);
                //updateSearchIndex(true);
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                mSwitch.setEnabled(false);
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                setSwitchChecked(false);
                mSwitch.setEnabled(true);
                //updateSearchIndex(false);
                break;
            default:
                setSwitchChecked(false);
                mSwitch.setEnabled(true);
                //updateSearchIndex(false);
        }
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }

        if (Utils.isVerizonSKU() && Utils.isCustomizedDeviceAdminActive(mContext)) {
            final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
            if (um != null && um.hasUserRestriction(UserManager.DISALLOW_CONFIG_WIFI)) {
                setSwitchChecked(false);
                return;
            }
        }

        // Show toast message if Wi-Fi is not allowed in airplane mode
        if (isChecked && !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_WIFI)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off. No infinite check/listenenr loop.
            mSwitch.setChecked(false);
            return;
        }

        // Disable tethering if enabling Wifi
        int wifiApState = mWifiManager.getWifiApState();
        if (Utils.isVerizon()) {
            final SharedPreferences settings = mContext.getSharedPreferences("WifiEnablerPreference", 0);
            if (isChecked && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                    (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED) || isBluetoothTethering() || isUsbTethering())) {
                if (!settings.getBoolean("skipMessage", false)) {
                    setSwitchChecked(false);
                    LayoutInflater inflater = LayoutInflater.from(mContext);
                    View checkboxLayout = inflater.inflate(R.layout.checkbox, null);
                    final CheckBox doNotshowAgain = (CheckBox) checkboxLayout.findViewById(R.id.skip);
                    new AlertDialog.Builder(mContext)
                        .setTitle(R.string.notice)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setMessage(R.string.wifi_on_msg_when_tethering_on)
                        .setView(checkboxLayout)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mWifiManager.setWifiApEnabled(null, false);
                                setSwitchChecked(true);
                                if(isUsbTethering())
                                    setUsbTethering(false);
                                if(isBluetoothTethering())
                                    stopBluetoothTethering();
                                if (!mWifiManager.setWifiEnabled(true)) {
                                    // Error
                                    mSwitch.setEnabled(true);
                                    Toast.makeText(mContext, R.string.wifi_error, Toast.LENGTH_SHORT).show();
                                }
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putBoolean("skipMessage", doNotshowAgain.isChecked());
                                editor.commit();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                } else {
                    mWifiManager.setWifiApEnabled(null, false);
                    if(isUsbTethering())
                        setUsbTethering(false);
                    if(isBluetoothTethering())
                        stopBluetoothTethering();
                    if (!mWifiManager.setWifiEnabled(true)) {
                        // Error
                        mSwitch.setEnabled(true);
                        Toast.makeText(mContext, R.string.wifi_error, Toast.LENGTH_SHORT).show();
                    }
                }
            } else if (!mWifiManager.setWifiEnabled(isChecked)) {
                // Error
                mSwitch.setEnabled(true);
                Toast.makeText(mContext, R.string.wifi_error, Toast.LENGTH_SHORT).show();
            }
        } else {
            if (isChecked && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                mWifiManager.setWifiApEnabled(null, false);
            }
            if (!mWifiManager.setWifiEnabled(isChecked)) {
                // Error
                mSwitch.setEnabled(true);
                Toast.makeText(mContext, R.string.wifi_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void setSwitch(Switch switch_) {
        // TODO Auto-generated method stub
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;

        final int wifiState = mWifiManager.getWifiState();
        boolean setSwitchChecked = ((wifiState == WifiManager.WIFI_STATE_ENABLED) || (wifiState == WifiManager.WIFI_STATE_ENABLING));
        boolean setSwitchEnabled = ((wifiState == WifiManager.WIFI_STATE_ENABLED) || (wifiState == WifiManager.WIFI_STATE_DISABLED));
        mSwitch.setChecked(setSwitchChecked);
        mSwitch.setEnabled(setSwitchEnabled);

        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub
        mContext.unregisterReceiver(mReceiver);
        mSwitch.setOnCheckedChangeListener(null);
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
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

    private boolean isUsbTethering() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        String[] ifaces = cm.getTetheredIfaces();
        for(int i = 0 ; i < ifaces.length ; i++) {
            //Log.d(TAG, "ifaces[" + i + "] = " + ifaces[i]);
            if (ifaces[i].equals("rndis0")) {
                return true;
            }
        }
        return false;
    }

    private void setUsbTethering(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.setUsbTethering(enabled);
    }

    private boolean isBluetoothTethering() {
        BluetoothPan pan = mBluetoothPan.get();
        if (pan != null && pan.isTetheringOn()) {
            Log.d(LOG_TAG,"isBluetoothTethering true");
            return true;
        } else {
            Log.d(LOG_TAG,"isBluetoothTethering false");
            return false;
        }
    }

    private BluetoothProfile.ServiceListener mProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothPan.set((BluetoothPan) proxy);
        }
        public void onServiceDisconnected(int profile) {
            mBluetoothPan.set(null);
        }
    };

    private void stopBluetoothTethering() {
        ConnectivityManager cm = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.stopTethering(ConnectivityManager.TETHERING_BLUETOOTH);
    }
}
