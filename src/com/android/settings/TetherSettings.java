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

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.wifi.WifiApEnabler;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApUserList;
import com.android.settings.wifi.WifiApSettings;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Gravity;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.util.VerizonHelpUtils;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;
import com.android.settingslib.TetherUtil;
import com.android.settings.wifi.WifiQRCodeUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.CheckBox;
import android.provider.SearchIndexableResource;

import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.ConnectivityManager.TETHERING_WIFI;

import java.util.List;

/*
 * Displays preferences for Tethering.
 */
public class TetherSettings extends RestrictedSettingsFragment
        implements DialogInterface.OnClickListener, Preference.OnPreferenceChangeListener,
        DataSaverBackend.Listener, Indexable {

    private static final String USB_TETHER_SETTINGS = "usb_tether_settings";
    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";
    private static final String ENABLE_BLUETOOTH_TETHERING = "enable_bluetooth_tethering";
    private static final String TETHER_CHOICE = "TETHER_TYPE";
    private static final String DATA_SAVER_FOOTER = "disabled_on_data_saver";

    private static final int DIALOG_AP_SETTINGS = 1;
    private static final int DIALOG_AP_TIP = 2;
    private static final int DIALOG_AP_USER_LIST = 3;
    private static final int DIALOG_AP_SHARE_NETWORK = 4;

    private static final String TAG = "TetheringSettings";

    private SwitchPreference mUsbTether;

    private WifiApEnabler mWifiApEnabler;
    private SwitchPreference mEnableWifiAp;

    private SwitchPreference mBluetoothTether;

    private BroadcastReceiver mTetherChangeReceiver;

    private String[] mUsbRegexs;

    private String[] mWifiRegexs;

    private String[] mBluetoothRegexs;
    private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<BluetoothPan>();

    private ArrayList<String> mUserList = new ArrayList<String>();
    private WifiApUserList mUserListDialog;

    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;

    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
//    private static final String WIFI_AP_USER_LIST = "wifi_ap_user_list";
    private static final String WIFI_AP_SHARE_NETWORK = "wifi_ap_share_network";
    //+ tim
    private static final String USER_LIST_CATEGORY = "wifi_ap_user_list_category";
    private static final String USER_LIST_EMPTY = "wifi_ap_user_list_empty";
    //- tim
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

    private String[] mSecurityType;
    private Preference mCreateNetwork;
//    private Preference mWifiApUserList;
    private Preference mWifiApShareNetwork;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;
    private ConnectivityManager mCm;

    private boolean mRestartWifiApAfterConfigChange;

    private boolean mUsbConnected;
    private boolean mMassStorageActive;

    private boolean mBluetoothEnableForTether;

    /* Stores the package name and the class name of the provisioning app */
    private String[] mProvisionApp;
    private static final int PROVISION_REQUEST = 0;
    private final int WIFI_AP_SETTINGS_REQUEST_CODE = 1;

    private boolean mUnavailable;

    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    private Preference mDataSaverFooter;

    //+ tim
    private static final int EVENT_REFRESH_USER_LIST = 1;
    private Activity mActivity;
    private Handler userListHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_REFRESH_USER_LIST:
                	if(mActivity != null){
                		Log.d("timhu", "send WIFI_AP_UPDATE_REQUEST_ACTION ");
	                	mActivity.sendBroadcast(new Intent(WifiManager.WIFI_AP_UPDATE_REQUEST_ACTION));
                	}
                    break;
            }
        }
    };
    //- tim
    
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.TETHER;
    }

    public TetherSettings() {
        super(UserManager.DISALLOW_CONFIG_TETHERING);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.tether_prefs);

        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();
        mDataSaverFooter = findPreference(DATA_SAVER_FOOTER);

        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted()) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
            return;
        }

        final Activity activity = getActivity();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(activity.getApplicationContext(), mProfileServiceListener,
                    BluetoothProfile.PAN);
        }

        mEnableWifiAp = (SwitchPreference) findPreference(ENABLE_WIFI_AP);
        Preference wifiApSettings = findPreference(WIFI_AP_SSID_AND_SECURITY);
//        Preference wifiApUserList = findPreference(WIFI_AP_USER_LIST);
        mUsbTether = (SwitchPreference) findPreference(USB_TETHER_SETTINGS);
        mBluetoothTether = (SwitchPreference) findPreference(ENABLE_BLUETOOTH_TETHERING);

        mWifiApShareNetwork = findPreference(WIFI_AP_SHARE_NETWORK);

        mDataSaverBackend.addListener(this);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mUsbRegexs = mCm.getTetherableUsbRegexs();
        mWifiRegexs = mCm.getTetherableWifiRegexs();
        mBluetoothRegexs = mCm.getTetherableBluetoothRegexs();

        final boolean usbAvailable = mUsbRegexs.length != 0;
        final boolean wifiAvailable = mWifiRegexs.length != 0;
        final boolean bluetoothAvailable = mBluetoothRegexs.length != 0;

        if (!usbAvailable || Utils.isMonkeyRunning()) {
//            getPreferenceScreen().removePreference(mUsbTether);
        }

        if (wifiAvailable && !Utils.isMonkeyRunning()) {
            mWifiApEnabler = new WifiApEnabler(activity, mDataSaverBackend, mEnableWifiAp);
            initWifiTethering();
        } else {
        	mEnableWifiAp.setVisible(false);
        	wifiApSettings.setVisible(false);
            mWifiApShareNetwork.setVisible(false);
//            getPreferenceScreen().removePreference(wifiApUserList);
        }

        if (!bluetoothAvailable) {
//            getPreferenceScreen().removePreference(mBluetoothTether);
        } else {
            BluetoothPan pan = mBluetoothPan.get();
            if (pan != null && pan.isTetheringOn()) {
                mBluetoothTether.setChecked(true);
            } else {
                mBluetoothTether.setChecked(false);
            }
        }
        // Set initial state based on Data Saver mode.
        onDataSaverChanged(mDataSaverBackend.isDataSaverEnabled());

        if (icicle != null && icicle.containsKey(WifiManager.EXTRA_WIFI_AP_USER_LIST)) {
            mUserList = icicle.getStringArrayList(WifiManager.EXTRA_WIFI_AP_USER_LIST);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        WifiQRCodeUtils.DeviceH = metrics.heightPixels;
        WifiQRCodeUtils.DeviceW = metrics.widthPixels;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    @Override
    public void onDestroy() {
        mDataSaverBackend.remListener(this);
        super.onDestroy();
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        mEnableWifiAp.setEnabled(!mDataSaverEnabled);
        mUsbTether.setEnabled(!mDataSaverEnabled);
        mBluetoothTether.setEnabled(!mDataSaverEnabled);
        mDataSaverFooter.setVisible(mDataSaverEnabled);
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted)  {
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putStringArrayList(WifiManager.EXTRA_WIFI_AP_USER_LIST, mUserList);
        super.onSaveInstanceState(savedInstanceState);
    }
    
    @Override
    public void onResume() {
    	//+ tim
        if (!mUnavailable) {
    	mActivity = getActivity();
    	initWifiTethering();
    	
    	if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_DISABLED)
    		updateUserList(false);
    	else
    		refreshUserList();
        }
    	//- tim
    	super.onResume();
    }

    private void initWifiTethering() {
        final Activity activity = getActivity();
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);

        mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);
//        mWifiApUserList = findPreference(WIFI_AP_USER_LIST);

        if(WifiConfiguration.KeyMgmt.strings[mWifiConfig.getAuthType()].equals("NONE")){
            mWifiApShareNetwork.setVisible(false);
            mCreateNetwork.setLayoutResource(R.layout.cnasusres_preference_parent_nodivider);
        }else {
            mWifiApShareNetwork.setVisible(true);
            mCreateNetwork.setLayoutResource(R.layout.cnasusres_preference_parent);
        }
//        mRestartWifiApAfterConfigChange = false;
        //+ tim
//        if(WifiConfiguration.KeyMgmt.strings[mWifiConfig.getAuthType()].equals("NONE"))
//            mWifiApShareNetwork.setEnabled(false);
//        if (mWifiConfig == null) {
//            final String s = activity.getString(
//                    com.android.internal.R.string.wifi_tether_configure_ssid_default);
//            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
//                    s, mSecurityType[WifiApDialog.OPEN_INDEX]));
//        } else {
//            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
//            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
//                    mWifiConfig.SSID,
//                    mSecurityType[index]));
//        }
        
        if(mWifiConfig != null)
        	mCreateNetwork.setSummary(mWifiConfig.SSID);
        else
        	mCreateNetwork.setSummary(getResources().getString(R.string.wifi_ap_not_set));
      //- tim
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            // MC1++: SoftAP powersaving implementation
            // mDialog = new WifiApDialog(activity, this, mWifiConfig);
            int hotspotRemainTime = Settings.System.getInt(getContentResolver(),
                                                            Settings.System.HOTSPOT_DISABLE_POLICY,
                                                            Settings.System.HOTSPOT_DISABLE_POLICY_DEFAULT);
            mDialog = new WifiApDialog(activity, this, mWifiConfig, getHdpIndex(hotspotRemainTime));
            mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            // MC1--: SoftAP powersaving implementation
            return mDialog;
        }
        if (id == DIALOG_AP_USER_LIST) {
            final Activity activity = getActivity();
            mUserListDialog = new WifiApUserList(activity, mUserList);
            return mUserListDialog;
        }
        if (id == DIALOG_AP_SHARE_NETWORK) {
            ImageView mQrcodeView = new ImageView(getActivity());
            mQrcodeView.setImageBitmap(WifiQRCodeUtils.getQRCode(WifiQRCodeUtils.getApQRCodeString(mWifiConfig)));
            Dialog dialog = new AlertDialog.Builder(getActivity())
            .setTitle(R.string.wifi_qrcode_share_network)
            .setView(mQrcodeView)
            .setNegativeButton(R.string.wifi_qrcode_close,new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {

                }

            }).create();
            return dialog;
        }
        return null;
    }

    // MC1++: SoftAP powersaving implementation
    private int getHdpIndex(int hotspotRemainTime) {
        int hotspotDisablePolicyIndex;
        int[] hotspotDisablePolicyValues = getResources().getIntArray(R.array.wifi_disable_hotspot_values);
        for (hotspotDisablePolicyIndex = 0; hotspotDisablePolicyIndex < hotspotDisablePolicyValues.length; hotspotDisablePolicyIndex++) {
            if (hotspotRemainTime == hotspotDisablePolicyValues[hotspotDisablePolicyIndex]) {
                return hotspotDisablePolicyIndex;
            }
        }
        Settings.System.putInt(getContentResolver(),
                                Settings.System.HOTSPOT_DISABLE_POLICY,
                                Settings.System.HOTSPOT_DISABLE_POLICY_DEFAULT);
        hotspotRemainTime = Settings.System.HOTSPOT_DISABLE_POLICY_DEFAULT;
        for (hotspotDisablePolicyIndex = 0; hotspotDisablePolicyIndex < hotspotDisablePolicyValues.length; hotspotDisablePolicyIndex++) {
            if (hotspotRemainTime == hotspotDisablePolicyValues[hotspotDisablePolicyIndex]) {
                return hotspotDisablePolicyIndex;
            }
        }
        Settings.System.putInt(getContentResolver(),
                                Settings.System.HOTSPOT_DISABLE_POLICY,
                                hotspotDisablePolicyValues[0]);
                    return 0;
    }
    // MC1--: SoftAP powersaving implementation
    
    //+ tim
    private String getHdpValue(){
    	String value = new String();
    	int hotspotRemainTime = Settings.System.getInt(getContentResolver(),
                Settings.System.HOTSPOT_DISABLE_POLICY,
                Settings.System.HOTSPOT_DISABLE_POLICY_DEFAULT);
    	int index = getHdpIndex(hotspotRemainTime);
        if(index == 0)  return "";
    	String[] values = getResources().getStringArray(R.array.wifi_disable_hotspot_entries);
    	if(index < values.length)
    		value = values[index];
    	return value;
    }
    //- tim

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                // TODO - this should understand the interface types
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateState(available.toArray(new String[available.size()]),
                        active.toArray(new String[active.size()]),
                        errored.toArray(new String[errored.size()]));
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_DISABLED
                        && mRestartWifiApAfterConfigChange) {
                    mRestartWifiApAfterConfigChange = false;
                    Log.d(TAG, "Restarting WifiAp due to prior config change.");
                    startTethering(TETHERING_WIFI);
                }
            } else if (action.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, 0);
                if (state == WifiManager.WIFI_AP_STATE_DISABLED
                        && mRestartWifiApAfterConfigChange) {
                    mRestartWifiApAfterConfigChange = false;
                    Log.d(TAG, "Restarting WifiAp due to prior config change.");
                    startTethering(TETHERING_WIFI);
                }

                if (state != WifiManager.WIFI_AP_STATE_ENABLED) {
                    if (mUserListDialog != null) {
                        mUserListDialog.dismiss();
                        mUserListDialog = null;
                    }
                } else {
                    if (state != WifiManager.WIFI_AP_STATE_ENABLED) {
                        if (mUserListDialog != null) {
                            mUserListDialog.dismiss();
                            mUserListDialog = null;
                        }
                        //+ tim
                    } else
                    	refreshUserList();
                    if(state == WifiManager.WIFI_AP_STATE_DISABLED)
                    	updateUserList(false);
                    //- tim
                }
            } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                mMassStorageActive = true;
                updateState();
            } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
                mMassStorageActive = false;
                updateState();
            } else if (action.equals(UsbManager.ACTION_USB_STATE)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
                updateState();
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (mBluetoothEnableForTether) {
                    switch (intent
                            .getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            startTethering(TETHERING_BLUETOOTH);
                            mBluetoothEnableForTether = false;
                            break;

                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.ERROR:
                            mBluetoothEnableForTether = false;
                            break;

                        default:
                            // ignore transition states
                    }
                }
                updateState();
            } else if (action.equals(WifiManager.WIFI_AP_USER_UPDATE_ACTION)) {
            	Log.d(TAG, "WIFI_AP_USER_UPDATE_ACTION ");
                mUserList = intent.getStringArrayListExtra(WifiManager.EXTRA_WIFI_AP_USER_LIST);
                for (String user : mUserList) {
                    Log.i(TAG, "Get hotspot user list: " + user.split("\n")[0]);
                }
//                if (mUserListDialog != null) {
//                    mUserListDialog.notifyUserListChanged(mUserList);
//                }
                //+ tim
                updateUserList(mEnableWifiAp.isChecked());
                //- tim
            }
        }
    }
    
    //+ tim
    private void updateUserList(boolean wifiApEnable){
    	PreferenceGroup container = (PreferenceGroup) findPreference(USER_LIST_CATEGORY);
    	Preference emptyPreference = findPreference(USER_LIST_EMPTY);

        String hdpValue = getHdpValue();
        emptyPreference.setTitle("".equals(hdpValue) ?
                getActivity().getString(R.string.tether_settings_title_disable_never) :
                getActivity().getString(R.string.tether_settings_title_disable_tether_info, hdpValue));
    	
    	if(!wifiApEnable){
    		container.removeAll();
    		container.setVisible(false);
    		emptyPreference.setVisible(false);
    		return;
    	}
    		
    	container.setVisible(true);
    	container.removeAll();
    	if(mUserList == null || mUserList.size() == 0){
    		emptyPreference.setVisible(true);
    		container.setSummary(getActivity().getString(R.string.tether_settings_summary_connected, 0));
    	}else {
    		emptyPreference.setVisible(false);
    		container.setSummary(getActivity().getString(R.string.tether_settings_summary_connected, mUserList.size()));
    		
    		for (String user : mUserList) {
    			String[] datas = user.split("\n");
            	String mac = datas[0];
            	String name = null;
            	if(datas.length > 2)
            		name = datas[2];
                Preference preference = new Preference(getContext());
                preference.setTitle(name);
                preference.setSummary(mac);
                container.addPreference(preference);
            }
            setPreferenceGroupChildrenLayout(container, R.layout.asusres_preference_material,
                    R.layout.asusres_preference_material_nodivider);
    	}

    	userListHandler.removeMessages(EVENT_REFRESH_USER_LIST);
        userListHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_REFRESH_USER_LIST), 60*1000);
    }
    
    private void refreshUserList(){
    	updateUserList(true);
    	
    	userListHandler.removeMessages(EVENT_REFRESH_USER_LIST);
    	userListHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_REFRESH_USER_LIST), 100);
    }
    //- tim

    @Override
    public void onStart() {
        super.onStart();

        if (mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.tethering_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }

        final Activity activity = getActivity();

        mStartTetheringCallback = new OnStartTetheringCallback(this);

        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        Intent intent = activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.WIFI_AP_USER_UPDATE_ACTION);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        if (intent != null) mTetherChangeReceiver.onReceive(activity, intent);
        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(this);
            mWifiApEnabler.resume();
        }

        updateState();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
        mStartTetheringCallback = null;
        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(null);
            mWifiApEnabler.pause();
        }
    }

    private void updateState() {
        String[] available = mCm.getTetherableIfaces();
        String[] tethered = mCm.getTetheredIfaces();
        String[] errored = mCm.getTetheringErroredIfaces();
        updateState(available, tethered, errored);
    }

    private void updateState(String[] available, String[] tethered,
            String[] errored) {
        updateUsbState(available, tethered, errored);
        updateBluetoothState(available, tethered, errored);
    }


    private void updateUsbState(String[] available, String[] tethered,
            String[] errored) {
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = mCm.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (String s: errored) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbErrored = true;
            }
        }

        if (usbTethered) {
            mUsbTether.setSummary(R.string.usb_tethering_active_subtext);
            mUsbTether.setEnabled(!mDataSaverEnabled);
            mUsbTether.setChecked(true);
        } else if (usbAvailable) {
            if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            } else {
                mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            }
            mUsbTether.setEnabled(!mDataSaverEnabled);
            mUsbTether.setChecked(false);
        } else if (usbErrored) {
            mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        } else if (mMassStorageActive) {
            mUsbTether.setSummary(R.string.usb_tethering_storage_active_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        } else {
            mUsbTether.setSummary(R.string.usb_tethering_unavailable_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        }
    }

    private void updateBluetoothState(String[] available, String[] tethered,
            String[] errored) {
        boolean bluetoothErrored = false;
        for (String s: errored) {
            for (String regex : mBluetoothRegexs) {
                if (s.matches(regex)) bluetoothErrored = true;
            }
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        int btState = adapter.getState();
        if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
            mBluetoothTether.setEnabled(false);
            mBluetoothTether.setSummary(R.string.bluetooth_turning_off);
        } else if (btState == BluetoothAdapter.STATE_TURNING_ON) {
            mBluetoothTether.setEnabled(false);
            mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
        } else {
            BluetoothPan bluetoothPan = mBluetoothPan.get();
            if (btState == BluetoothAdapter.STATE_ON && bluetoothPan != null
                    && bluetoothPan.isTetheringOn()) {
                mBluetoothTether.setChecked(true);
                mBluetoothTether.setEnabled(!mDataSaverEnabled);
                int bluetoothTethered = bluetoothPan.getConnectedDevices().size();
                if (bluetoothTethered > 1) {
                    String summary = getString(
                            R.string.asus_bluetooth_tethering_devices_connected_subtext,
                            bluetoothTethered);
                    mBluetoothTether.setSummary(summary);
                } else if (bluetoothTethered == 1) {
                    mBluetoothTether.setSummary(
                            R.string.asus_bluetooth_tethering_device_connected_subtext);
                } else if (bluetoothErrored) {
                    mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                } else {
                    mBluetoothTether.setSummary(R.string.asus_bluetooth_tethering_available_subtext);
                }
            } else {
                mBluetoothTether.setEnabled(!mDataSaverEnabled);
                mBluetoothTether.setChecked(false);
                mBluetoothTether.setSummary(R.string.asus_bluetooth_tethering_off_subtext);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean enable = (Boolean) value;

        if (enable) {
            startTethering(TETHERING_WIFI);
        } else {
            mCm.stopTethering(TETHERING_WIFI);
        }

        mRestartWifiApAfterConfigChange = false;
        if (mEnableWifiAp != null)
            mEnableWifiAp.setEnabled(false);
        return true;
    }

    public static boolean isProvisioningNeededButUnavailable(Context context) {
        return (TetherUtil.isProvisioningNeeded(context)
                && !isIntentAvailable(context));
    }

    private static boolean isIntentAvailable(Context context) {
        String[] provisionApp = context.getResources().getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app);
        if (provisionApp.length < 2) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(provisionApp[0], provisionApp[1]);

        return (packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY).size() > 0);
    }

    private void startTethering(int choice) {
        if (choice == TETHERING_BLUETOOTH) {
            // Turn on Bluetooth first.
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
                mBluetoothEnableForTether = true;
                adapter.enable();
                mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
                mBluetoothTether.setEnabled(false);
                return;
            }
        }

        mCm.startTethering(choice, true, mStartTetheringCallback, mHandler);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mUsbTether) {
            if (mUsbTether.isChecked()) {
                startTethering(TETHERING_USB);
            } else {
                mCm.stopTethering(TETHERING_USB);
            }
        } else if (preference == mBluetoothTether) {
            if (mBluetoothTether.isChecked()) {
                startTethering(TETHERING_BLUETOOTH);
            } else {
                mCm.stopTethering(TETHERING_BLUETOOTH);
                // No ACTION_TETHER_STATE_CHANGED is fired or bluetooth unless a device is
                // connected. Need to update state manually.
                updateState();
            }
        } else if (preference == mCreateNetwork) {
            showSettingsFragment(WifiApSettings.class.getCanonicalName(),
                    R.string.wifi_tether_configure_ap_text, WIFI_AP_SETTINGS_REQUEST_CODE);
//            showDialog(DIALOG_AP_SETTINGS);
//        } else if (preference == mWifiApUserList) {
//            showDialog(DIALOG_AP_USER_LIST);
        } else if(preference == mWifiApShareNetwork){
            if(mWifiConfig != null)
                createQRDialog().show();
//            showDialog(DIALOG_AP_SHARE_NETWORK);
        }

        return super.onPreferenceTreeClick(preference);
    }

    private void showSettingsFragment(String fragmentClass, int titleRes, int requestCode){
        Utils.startWithFragment(getActivity(), fragmentClass, null,
                this, requestCode, titleRes, null);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == WIFI_AP_SETTINGS_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    Log.d(TAG, "Wifi AP config changed while enabled, stop and restart");
                    mRestartWifiApAfterConfigChange = true;
                    mCm.stopTethering(TETHERING_WIFI);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private Dialog createQRDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.wifi_qrcode_dialog, null);
        dialogBuilder.setView(dialogView);

        ImageView imageView = (ImageView) dialogView.findViewById(R.id.qrcode);
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        android.view.Display display = manager.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        int smallerDimension = width < height ? width : height;
        smallerDimension = smallerDimension * 164 / 360;
        imageView.setImageBitmap(WifiQRCodeUtils.getQRCode(
                WifiQRCodeUtils.getApQRCodeString(mWifiConfig), smallerDimension, smallerDimension));

        AlertDialog alertDialog = dialogBuilder.create();
        WindowManager.LayoutParams wmlp = alertDialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.BOTTOM;

        TextView titleText = (TextView) dialogView.findViewById(R.id.hint_title);
        titleText.setText(getText(R.string.wifi_qrcode_share_network));
        TextView hintText = (TextView) dialogView.findViewById(R.id.hint_text);
        hintText.setText(getText(R.string.wifi_ap_qrcode_description));

        TextView cancelText = (TextView) dialogView.findViewById(R.id.hint_cancel);
        cancelText.setClickable(true);
        cancelText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(alertDialog != null) {
                    alertDialog.cancel();
              }
            }
        });
        return alertDialog;
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                /**
                 * if soft AP is stopped, bring up
                 * else restart with new config
                 * TODO: update config on a running access point when framework support is added
                 */
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    Log.d("TetheringSettings",
                            "Wifi AP config changed while enabled, stop and restart");
                    mRestartWifiApAfterConfigChange = true;
                    mCm.stopTethering(TETHERING_WIFI);
                }
                mWifiManager.setWifiApConfiguration(mWifiConfig);

                /* remove wifi_ap_share_network
                 * tim++
                if(WifiConfiguration.KeyMgmt.strings[mWifiConfig.getAuthType()].equals("NONE"))
                        mWifiApShareNetwork.setEnabled(false);
                    else
                        mWifiApShareNetwork.setEnabled(true);
                */

                // AMAX++: SoftAP powersaving implementation
                int[] hotspotDisablePolicyValues = getResources().getIntArray(R.array.wifi_disable_hotspot_values);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.HOTSPOT_DISABLE_POLICY,
                        hotspotDisablePolicyValues[mDialog.getDisableHotspotSelection()]);
                // AMAX--: SoftAP powersaving implementation

                int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
                mCreateNetwork.setSummary(String.format(getActivity().getString(CONFIG_SUBTEXT),
                        mWifiConfig.SSID,
                        mSecurityType[index]));
            }
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	/* remove options menu
	 * tim++
	inflater.inflate(R.menu.wifi_ap_advanced, menu);
	// +++ ShawnMC_Liu@2016/11/01: Verizon VZ_REQ_DEVHELP_10667
	MenuItem verizonHelpItem = menu.findItem(R.id.wifi_ap_help);
	if(!VerizonHelpUtils.isVerizonMachine()){
	  verizonHelpItem.setVisible(false);
	}
	// ---
	*/
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.wifi_ap_help) {
            // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10667
            VerizonHelpUtils.launchVzWHelp(getActivity(), VerizonHelpUtils.SCREEN_TETHER);
            // ---
        }
        return super.onOptionsItemSelected(item);
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

    private static final class OnStartTetheringCallback extends
            ConnectivityManager.OnStartTetheringCallback {
        final WeakReference<TetherSettings> mTetherSettings;

        OnStartTetheringCallback(TetherSettings settings) {
            mTetherSettings = new WeakReference<TetherSettings>(settings);
        }

        @Override
        public void onTetheringStarted() {
            update();
        }

        @Override
        public void onTetheringFailed() {
            update();
        }

        private void update() {
            TetherSettings settings = mTetherSettings.get();
            if (settings != null) {
                settings.updateState();
            }
        }
    }
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                            boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.tether_prefs;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();

                    result.add(WIFI_AP_SHARE_NETWORK);
                    return result;
                }
            };

    private static class SummaryProvider extends BroadcastReceiver
            implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final WifiManager mWifiManager;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mWifiManager = context.getSystemService(WifiManager.class);
        }

        private CharSequence getSummary() {
            if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                return mContext.getString(R.string.develop_asusres_switch_open);
            }
            return mContext.getString(R.string.develop_asusres_switch_close);
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
                filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
                mSummaryLoader.registerReceiver(this, filter);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mSummaryLoader.setSummary(this, getSummary());
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
}
