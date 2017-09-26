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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;

import java.nio.charset.Charset;

import com.android.settings.util.RemoveSpecialCharFilter;
import com.android.settings.util.Utf8ByteLengthFilter;

/**
 * Dialog to configure the SSID and security settings
 * for Access Point operation
 */
public class WifiApDialog extends AlertDialog implements View.OnClickListener,
        TextWatcher, AdapterView.OnItemSelectedListener {

    private static final String TAG = "WifiApDialog";
    private static final String PREF_KEY_SHOW_WIFI_AP_PASSWORD = "key_pref_show_wifi_ap_password";

    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

    private final DialogInterface.OnClickListener mListener;

    public static final int OPEN_INDEX = 0;
    public static final int WPA2_INDEX = 1;

    private View mView;
    private TextView mSsid;
    private int mSecurityTypeIndex = OPEN_INDEX;
    // MC1++: SoftAP powersaving implementation
    private int mHotspotDisablePolicyIndex;
    // MC1--: SoftAP powersaving implementation
    private EditText mPassword;
    private int mBandIndex = OPEN_INDEX;

    // MC1++: SoftAP powersaving implementation
    Spinner mSecurity;
    Spinner mHotspotDisablePolicy;
    // MC1--: SoftAP powersaving implementation
    WifiConfiguration mWifiConfig;
    WifiManager mWifiManager;
    private Context mContext;

        // MC1++: SoftAP powersaving implementation
    public WifiApDialog(Context context, DialogInterface.OnClickListener listener,
            WifiConfiguration wifiConfig, int disablePolicyIndex) {
        // MC1--: SoftAP powersaving implementation
        super(context);
        mListener = listener;
        mWifiConfig = wifiConfig;
        if (wifiConfig != null) {
            mSecurityTypeIndex = getSecurityTypeIndex(wifiConfig);
            // MC1++: SoftAP powersaving implementation
            mHotspotDisablePolicyIndex = disablePolicyIndex;
            // MC1--: SoftAP powersaving implementation
        }
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mContext =  context;
    }

    public static int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
            return WPA2_INDEX;
        }
        return OPEN_INDEX;
    }

    // MC1++: SoftAP powersaving implementation
    public int getDisableHotspotSelection() {
        return mHotspotDisablePolicyIndex;
    }
    // MC1--: SoftAP powersaving implementation

    public WifiConfiguration getConfig() {

        WifiConfiguration config = new WifiConfiguration();

        /**
         * TODO: SSID in WifiConfiguration for soft ap
         * is being stored as a raw string without quotes.
         * This is not the case on the client side. We need to
         * make things consistent and clean it up
         */
        config.SSID = mSsid.getText().toString().trim();

        config.apBand = mBandIndex;

        switch (mSecurityTypeIndex) {
            case OPEN_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                return config;

            case WPA2_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                if (mPassword.length() != 0) {
                    String password = mPassword.getText().toString();
                    config.preSharedKey = password;
                }
                return config;
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean mInit = true;
        mView = getLayoutInflater().inflate(R.layout.wifi_ap_dialog, null);
        final Spinner mChannel = (Spinner) mView.findViewById(R.id.choose_channel);
        // MC1++: SoftAP powersaving implementation
        // Spinner mSecurity = ((Spinner) mView.findViewById(R.id.security));
        mSecurity = ((Spinner) mView.findViewById(R.id.security));
        mHotspotDisablePolicy = ((Spinner) mView.findViewById(R.id.disable_hotspot_policy));
        // MC1--: SoftAP powersaving implementation
        setView(mView);
        setInverseBackgroundForced(true);

        Context context = getContext();

        setTitle(R.string.wifi_tether_configure_ap_text);
        mView.findViewById(R.id.type).setVisibility(View.VISIBLE);
        mSsid = (TextView) mView.findViewById(R.id.ssid);
        mSsid.setFilters(new InputFilter[] {new Utf8ByteLengthFilter(32), new RemoveSpecialCharFilter(true)});
        mPassword = (EditText) mView.findViewById(R.id.password);

        ArrayAdapter <CharSequence> channelAdapter,hotspotAdapter,securityAdapter;
        String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.isDualBandSupported() || countryCode == null) {
            //If no country code, 5GHz AP is forbidden
            Log.i(TAG,(!mWifiManager.isDualBandSupported() ? "Device do not support 5GHz " :"")
                    + (countryCode == null ? " NO country code" :"") +  " forbid 5GHz");
            channelAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.wifi_ap_band_config_2G_only, android.R.layout.simple_spinner_item);
            mWifiConfig.apBand = 0;
        } else {
            channelAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.wifi_ap_band_config_full, android.R.layout.simple_spinner_item);
        }
        hotspotAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.wifi_disable_hotspot_entries, android.R.layout.simple_spinner_item);
        securityAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.wifi_ap_security, android.R.layout.simple_spinner_item);
        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hotspotAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        securityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mHotspotDisablePolicy.setAdapter(hotspotAdapter);
        mSecurity.setAdapter(securityAdapter);
        setButton(BUTTON_SUBMIT, context.getString(R.string.wifi_save), mListener);
        setButton(DialogInterface.BUTTON_NEGATIVE,
        context.getString(R.string.wifi_cancel), mListener);

        if (mWifiConfig != null) {
            mSsid.setText(mWifiConfig.SSID);
            if (mWifiConfig.apBand == 0) {
               mBandIndex = 0;
            } else {
               mBandIndex = 1;
            }

            mSecurity.setSelection(mSecurityTypeIndex);
            if (mSecurityTypeIndex == WPA2_INDEX) {
                mPassword.setText(mWifiConfig.preSharedKey);
            }
            // MC1++: SoftAP powersaving implementation
            mHotspotDisablePolicy.setSelection(mHotspotDisablePolicyIndex);
            // MC1--: SoftAP powersaving implementation
        }

        mChannel.setAdapter(channelAdapter);
        mChannel.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    boolean mInit = true;
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                               long id) {
                        if (!mInit) {
                            mBandIndex = position;
                            mWifiConfig.apBand = mBandIndex;
                            Log.i(TAG, "config on channelIndex : " + mBandIndex + " Band: " +
                                    mWifiConfig.apBand);
                        } else {
                            mInit = false;
                            mChannel.setSelection(mBandIndex);
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                }
        );

        mSsid.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        final boolean showPassword = getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE).getBoolean(PREF_KEY_SHOW_WIFI_AP_PASSWORD, true);
        ((CheckBox) mView.findViewById(R.id.show_password)).setChecked(showPassword);
        ((CheckBox) mView.findViewById(R.id.show_password)).setOnClickListener(this);
        mSecurity.setOnItemSelectedListener(this);
        // MC1++: SoftAP powersaving implementation
        mHotspotDisablePolicy.setOnItemSelectedListener(this);
        // MC1--: SoftAP powersaving implementation
        super.onCreate(savedInstanceState);

        showSecurityFields();
        validate();
    }

    // [MC1][Connectivity][AsusSettings][Wi-Fi] Set Wi-Fi hotspot password default visible +++
    @Override
    protected void onStart() {
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | (((CheckBox) mView.findViewById(R.id.show_password)).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }
    // [MC1][Connectivity][AsusSettings][Wi-Fi] Set Wi-Fi hotspot password default visible ---

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                (((CheckBox) mView.findViewById(R.id.show_password)).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    private void validate() {
        String mSsidString = mSsid.getText().toString();
        if ((mSsid != null && mSsid.length() == 0)
                || ((mSecurityTypeIndex == WPA2_INDEX) && mPassword.length() < 8)
                || (mSsid != null &&
                Charset.forName("UTF-8").encode(mSsidString).limit() > 32)) {
            getButton(BUTTON_SUBMIT).setEnabled(false);
        } else {
            getButton(BUTTON_SUBMIT).setEnabled(true);
        }
    }

    public void onClick(View view) {
        getContext().getSharedPreferences(TAG, Context.MODE_PRIVATE).edit().putBoolean(PREF_KEY_SHOW_WIFI_AP_PASSWORD, ((CheckBox)view).isChecked()).commit();
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | (((CheckBox) view).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable editable) {
        validate();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mSecurity) {
            mSecurityTypeIndex = position;
            showSecurityFields();
            validate();
        } else if (parent == mHotspotDisablePolicy) {
            mHotspotDisablePolicyIndex = position;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void showSecurityFields() {
        if (mSecurityTypeIndex == OPEN_INDEX) {
            mView.findViewById(R.id.fields).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.fields).setVisibility(View.VISIBLE);
    }
}
