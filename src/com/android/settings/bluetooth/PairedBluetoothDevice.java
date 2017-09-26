/*
 Copyright (C) 2015 The Android Open Source Project
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

import android.provider.Settings.Global;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceCategory;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.MapProfile;
import com.android.settingslib.bluetooth.PanProfile;
import com.android.settingslib.bluetooth.PbapServerProfile;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.SystemProperties;
import android.os.ParcelUuid;
import android.bluetooth.BluetoothUuid;
import android.text.TextUtils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.View;
import android.util.Log;

import android.widget.TextView;
import android.widget.EditText;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.text.InputFilter;
import android.text.TextUtils;
import com.android.settings.util.RemoveSpecialCharFilter;
import com.android.settings.util.Utf8ByteLengthFilter;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.ViewGroup;


/**
 * A page that configures paired Bluetooth Devices.
 */
public class PairedBluetoothDevice  extends SettingsPreferenceFragment implements 
        CachedBluetoothDevice.Callback,Preference.OnPreferenceChangeListener {
    private static final String KEY_RENAME_PREF = "rename_pref";
    private static final String KEY_CANCEL_CONFIG_PREF = "cancel_config_pref";
    private static final String KEY_CONFIG_CATEGORY = "devices_configure_category";
    //Suleman
    public static final String ARG_DEVICE_ADDRESS = "device_address";
    private CachedBluetoothDevice mCachedDevice;
    private LocalBluetoothManager mManager;
    private String address;
    private PreferenceCategory mConfigPreferenceCategory;
    private LocalBluetoothProfileManager mProfileManager;   
    private View mRootView;

    private static final String KEY_PBAP_SERVER = "PBAP Server";
    private static final int OK_BUTTON = -1;
    private AlertDialog mDisconnectDialog;
    private TextView mProfileLabel;


    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.LOCATION_SCANNING;
    }

    @Override
    public void onResume() {
        super.onResume();
        createPreferenceHierarchy();

        mManager = Utils.getLocalBtManager(getActivity());
        CachedBluetoothDeviceManager deviceManager = mManager.getCachedDeviceManager();

        address = getArguments().getString(ARG_DEVICE_ADDRESS);
        BluetoothDevice remoteDevice = mManager.getBluetoothAdapter().getRemoteDevice(address);

        mCachedDevice = deviceManager.findDevice(remoteDevice);
        if (mCachedDevice == null) {
            mCachedDevice = deviceManager.addDevice(mManager.getBluetoothAdapter(),
                    mManager.getProfileManager(), remoteDevice);
        }

        mProfileManager = mManager.getProfileManager(); 
       
        mManager.setForegroundActivity(getActivity());

        addPreferencesForProfiles();
        refreshProfiles();
        if (mCachedDevice != null) {
            mCachedDevice.registerCallback(this);
            if (mCachedDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                getActivity().onBackPressed();
                return ;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mCachedDevice != null) {
            mCachedDevice.unregisterCallback(this);
        }
        mManager.setForegroundActivity(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDisconnectDialog != null) {
            mDisconnectDialog.dismiss();
            mDisconnectDialog = null;
        }
        if (mCachedDevice != null) {
            mCachedDevice.unregisterCallback(this);
        }
    }

    //+++ suleman
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }
    //---

    @Override
    public void onDeviceAttributesChanged() {
        refreshProfiles();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.paired_bluetooth_devices);
        root = getPreferenceScreen();
       // initPreferences();
        mConfigPreferenceCategory = (PreferenceCategory)findPreference(KEY_CONFIG_CATEGORY);
        return root;
    }

    private void initPreferences() {
        final Preference rename =
            (Preference) findPreference(KEY_RENAME_PREF);
        final Preference bleScanAlwaysAvailable =
            (Preference) findPreference(KEY_CANCEL_CONFIG_PREF);
        final SwitchPreference  mConfigSwitchPref =
            (SwitchPreference) findPreference(KEY_CANCEL_CONFIG_PREF);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof SwitchPreference) {
            LocalBluetoothProfile prof = getProfileOf(preference);
            onProfileClicked(prof, (SwitchPreference) preference);
        }

        String key = preference.getKey();
        if (KEY_RENAME_PREF.equals(key)) {
        /*    Bundle args = new Bundle();
            args.putString(DeviceProfilesSettings.ARG_DEVICE_ADDRESS,
                    address);
            DeviceProfilesSettings profileSettings = new DeviceProfilesSettings();
            profileSettings.setArguments(args);
            profileSettings.show(getFragmentManager(),
                    DeviceProfilesSettings.class.getSimpleName());
        */
        //mRootView = LayoutInflater.from(getContext()).inflate(R.layout.device_profiles_settings,null);
        mRootView = LayoutInflater.from(getContext()).inflate(R.layout.cnasusres_alertdialog_textfield_with_title,null);
        TextView title = (TextView) mRootView.getRootView().findViewById(R.id.alertdialog_title);
        title.setText(getActivity().getResources().getString(R.string.bluetooth_preference_paired_devices));
        //mProfileLabel = (TextView) mRootView.findViewById(R.id.profiles_label);
        //mProfileLabel.setVisibility(View.GONE);

        final EditText deviceName = (EditText) mRootView.getRootView().findViewById(R.id.alertdialog_textfield);
        //deviceName.setFilters(new InputFilter[] {new Utf8ByteLengthFilter(248), new RemoveSpecialCharFilter(false)});
        deviceName.setText(mCachedDevice.getName(), TextView.BufferType.EDITABLE);
        deviceName.setSelection(mCachedDevice.getName().length());
        deviceName.setPadding(0,0,0,0);

        Builder pairedDialog = new AlertDialog.Builder(getActivity())
            .setView(mRootView)
            .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener(){
             public void onClick(DialogInterface dialog, int which) {
                 // TODO Auto-generated method stub
                 mCachedDevice.setName(deviceName.getText().toString().trim());
                 }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
             public void onClick(DialogInterface dialog, int which) {
                 // TODO Auto-generated method stub
                 }
            });
         pairedDialog.show();
        } else if (KEY_CANCEL_CONFIG_PREF.equals(key)) {
                mCachedDevice.unpair();
                com.android.settings.bluetooth.Utils.updateSearchIndex(getActivity(),
                        BluetoothSettings.class.getName(), mCachedDevice.getName(),
                        getString(R.string.bluetooth_settings),
                        R.drawable.ic_settings_bluetooth, false);
                getActivity().onBackPressed();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    private void addPreferencesForProfiles() {
        mConfigPreferenceCategory.removeAll();
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {

            // MAP and PBAP profiles would be added based on permission access
            if (SystemProperties.get("ro.board.platform").contains("msm")) { // in QCOM platform , not add PBAP or MAP preference
                if (!((profile instanceof PbapServerProfile) || (profile instanceof MapProfile))) {
                    SwitchPreference pref = createProfilePreference(profile);
                    mConfigPreferenceCategory.addPreference(pref);
                }
            } else {
                    SwitchPreference pref = createProfilePreference(profile);
                    mConfigPreferenceCategory.addPreference(pref);
            }
        }

        ParcelUuid[] uuids = mCachedDevice.getDevice().getUuids();
        final PbapServerProfile psp = mManager.getProfileManager().getPbapProfile();

        if (BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.PBAP_PCE) ||
                (psp.getConnectionStatus(mCachedDevice.getDevice()) == BluetoothProfile.STATE_CONNECTING) ||
                (psp.getConnectionStatus(mCachedDevice.getDevice()) == BluetoothProfile.STATE_CONNECTED)) {
            final int pbapPermission = mCachedDevice.getPhonebookPermissionChoice();
            // Only provide PBAP cabability if the client device has requested PBAP.
            if (pbapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN) {
                SwitchPreference pref = createProfilePreference(psp);
                mConfigPreferenceCategory.addPreference(pref);
            }
        }

        final MapProfile mapProfile = mManager.getProfileManager().getMapProfile();
        final int mapPermission = mCachedDevice.getMessagePermissionChoice();
        if (mapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN) {
            SwitchPreference pref = createProfilePreference(mapProfile);
            mConfigPreferenceCategory.addPreference(pref);
        }

        showOrHideProfileGroup();
    }

    private void showOrHideProfileGroup() {
        int numProfiles =  mConfigPreferenceCategory.getPreferenceCount();
        if (numProfiles == 0)
            getPreferenceScreen().removePreference(mConfigPreferenceCategory);
    }

    private SwitchPreference createProfilePreference(LocalBluetoothProfile profile) {
            SwitchPreference pref = new SwitchPreference(getActivity());
            pref.setKey(profile.toString());
            pref.setTitle(profile.getNameResource(mCachedDevice.getDevice()));
            pref.setSummary(R.string.config_switch_pref_summary);
            pref.setOnPreferenceChangeListener(this);

            refreshProfilePreference(pref, profile);

            return pref;
    }

     @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
         boolean isChecked = (Boolean)objValue;
        return true;
    }

    private void onProfileClicked(LocalBluetoothProfile profile, SwitchPreference profilePref) {
        BluetoothDevice device = mCachedDevice.getDevice();
        if (!SystemProperties.get("ro.board.platform").contains("msm")) {
            if (KEY_PBAP_SERVER.equals(profilePref.getKey())) {
                final int newPermission = mCachedDevice.getPhonebookPermissionChoice()
                    == CachedBluetoothDevice.ACCESS_ALLOWED ? CachedBluetoothDevice.ACCESS_REJECTED
                    : CachedBluetoothDevice.ACCESS_ALLOWED;
                mCachedDevice.setPhonebookPermissionChoice(newPermission);
                profilePref.setChecked(newPermission == CachedBluetoothDevice.ACCESS_ALLOWED);
                return;
            }
        }

        if (!profilePref.isChecked()) {
            // Recheck it, until the dialog is done.
            profilePref.setChecked(true);
            askDisconnect(mManager.getForegroundActivity(), profile);
        } else {
            if (profile instanceof MapProfile) {
                mCachedDevice.setMessagePermissionChoice(BluetoothDevice.ACCESS_ALLOWED);
            }

            if (SystemProperties.get("ro.board.platform").contains("msm")) {
                if (profile instanceof PbapServerProfile) {
                    mCachedDevice.setPhonebookPermissionChoice(BluetoothDevice.ACCESS_ALLOWED);
                    refreshProfilePreference(profilePref, profile);
                    return;
                }
            }

            if (profile.isPreferred(device)) {
                // profile is preferred but not connected: disable auto-connect
                if (profile instanceof PanProfile) {
                    mCachedDevice.connectProfile(profile);
                } else {
                    profile.setPreferred(device, false);
                }
            } else {
                profile.setPreferred(device, true);
                mCachedDevice.connectProfile(profile);
            }
            refreshProfilePreference(profilePref, profile);
        }
    }

    private void askDisconnect(Context context,
            final LocalBluetoothProfile profile) {
        // local reference for callback
        final CachedBluetoothDevice device = mCachedDevice;
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
            name = context.getString(R.string.bluetooth_device);
        }
        String profileName = context.getString(profile.getNameResource(device.getDevice()));
        String title = context.getString(R.string.bluetooth_disable_profile_title);
        String message = context.getString(R.string.bluetooth_disable_profile_message,
                profileName, name);

        DialogInterface.OnClickListener disconnectListener =
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (SystemProperties.get("ro.board.platform").contains("msm")) {
                    // Disconnect only when user has selected OK
                    if (which == OK_BUTTON) {
                        device.disconnect(profile);
                        profile.setPreferred(device.getDevice(), false);
                        if (profile instanceof MapProfile) {
                            device.setMessagePermissionChoice(BluetoothDevice.ACCESS_REJECTED);
                        }
                        if (profile instanceof PbapServerProfile) {
                            device.setPhonebookPermissionChoice(BluetoothDevice.ACCESS_REJECTED);
                        }
                     }
                } else {
                    device.disconnect(profile);
                    profile.setPreferred(device.getDevice(), false);
                    if (profile instanceof MapProfile) {
                        device.setMessagePermissionChoice(BluetoothDevice.ACCESS_REJECTED);
                    }
                }
                refreshProfilePreference(findProfile(profile.toString()), profile);
            }
        };

            mDisconnectDialog = Utils.showDisconnectDialog(context,
                    mDisconnectDialog, disconnectListener, title, Html.fromHtml(message));
    }

    private void refreshProfiles() {
        boolean allChecked = true;
        for (LocalBluetoothProfile profile : mCachedDevice.getConnectableProfiles()) {
            SwitchPreference profilePref = findProfile(profile.toString());
            if (profilePref == null) {
                profilePref = createProfilePreference(profile);
                mConfigPreferenceCategory.addPreference(profilePref);
            } else {
                refreshProfilePreference(profilePref, profile);
            }
        }

        for (LocalBluetoothProfile profile : mCachedDevice.getRemovedProfiles()) {
            SwitchPreference profilePref = findProfile(profile.toString());
            if (profilePref != null) {
                if (SystemProperties.get("ro.board.platform").contains("msm")) {
                    if (profile instanceof PbapServerProfile) {
                        final int pbapPermission = mCachedDevice.getPhonebookPermissionChoice();
                        if (pbapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN)
                            continue;
                    }
                    if (profile instanceof MapProfile) {
                        final int mapPermission = mCachedDevice.getMessagePermissionChoice();
                        if (mapPermission != CachedBluetoothDevice.ACCESS_UNKNOWN)
                            continue;
                    }
                }
                mConfigPreferenceCategory.removePreference(profilePref);
            }
        }

        showOrHideProfileGroup();
    }

    private void refreshProfilePreference(SwitchPreference profilePref,
            LocalBluetoothProfile profile) {
        BluetoothDevice device = mCachedDevice.getDevice();

        // Gray out checkbox while connecting and disconnecting.
        profilePref.setEnabled(!mCachedDevice.isBusy());

        if (profile instanceof MapProfile) {
            profilePref.setChecked(mCachedDevice.getMessagePermissionChoice()
                    == CachedBluetoothDevice.ACCESS_ALLOWED);

        } else if (profile instanceof PbapServerProfile) {
            profilePref.setChecked(mCachedDevice.getPhonebookPermissionChoice()
                    == CachedBluetoothDevice.ACCESS_ALLOWED);

        } else if (profile instanceof PanProfile) {
            profilePref.setChecked(profile.getConnectionStatus(device) ==
                    BluetoothProfile.STATE_CONNECTED);

        } else {
                profilePref.setChecked(profile.isPreferred(device));
        }
    }
   
    private SwitchPreference findProfile(String profile) {
        return (SwitchPreference) mConfigPreferenceCategory.findPreference(profile);
    }

    private LocalBluetoothProfile getProfileOf(Preference v) {
         if (!(v instanceof SwitchPreference)) {
            return null;
        }    

        String key = (String) v.getKey();
        if (TextUtils.isEmpty(key)) return null;

        try {
            return mProfileManager.getProfileByName(key);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
