package com.android.settings.ethernet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ContentResolver;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import com.android.settings.CustomEditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.EditText;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;

import java.io.File;
import java.net.Inet4Address;

public class EthernetSettings extends SettingsPreferenceFragment implements
    Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener, TextWatcher, Indexable {
    private static final String TAG = "EthernetSettings";

    private static final int CONNECTION_TYPE_DHCP           = 0;
    private static final int CONNECTION_TYPE_STATIC         = 1;

    private static final String KEY_ETHERNET_POLICY         = "eth_policy";
    private static final String KEY_CONNECTION_TYPE         = "eth_conn_type";
    private static final String KEY_DHCP_IP_ADDRESS         = "eth_dhcp_ip_address";
    private static final String KEY_STATIC_IP_ADDRESS       = "eth_static_ip_address";
    private static final String KEY_NETWORK_PREFIX_LENGTH   = "eth_network_prefix_length";
    private static final String KEY_GATEWAY                 = "eth_gateway";
    private static final String KEY_DNS1                    = "eth_dns1";
    private static final String KEY_DNS2                    = "eth_dns2";

    private Context mContext;

    private SwitchBar mEthernetSwitch;
    private CheckBoxPreference mEthernetPolicy;

    private ListPreference mConnectionType;
    private Preference mDhcpIpAddress;
    private CustomEditTextPreference mStaticIpAddress;
    private CustomEditTextPreference mNetworkPrefixLength;
    private CustomEditTextPreference mGateway;
    private CustomEditTextPreference mDns1;
    private CustomEditTextPreference mDns2;

    private CustomEditTextPreference mForegroundEditTextPreference;

    private ConnectivityManager mCm;
    private EthernetManager mEm;
    private ContentResolver mCr;
    private IpConfiguration mIpConfig;
    private StaticIpConfiguration mStaticIpConfig;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        addPreferencesFromResource(R.xml.ethernet_settings);
        super.onActivityCreated(savedInstanceState);
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mEm = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
        mCr = mContext.getContentResolver();

        SettingsActivity activity = (SettingsActivity) getActivity();
        mEthernetSwitch = activity.getSwitchBar();
        mEthernetSwitch.show();
        if (mEm != null) {
            updateEthernetSwitch(mEm.getEthernetState());
        }
        mEthernetSwitch.addOnSwitchChangeListener(this);

        mEthernetPolicy = (CheckBoxPreference) findPreference(KEY_ETHERNET_POLICY);
        mEthernetPolicy.setOnPreferenceChangeListener(this);

        mConnectionType = (ListPreference) findPreference(KEY_CONNECTION_TYPE);
        if (mConnectionType != null) {
            mConnectionType.setOnPreferenceChangeListener(this);
        }

        mDhcpIpAddress = (Preference) findPreference(KEY_DHCP_IP_ADDRESS);
        mStaticIpAddress = (CustomEditTextPreference) findPreference(KEY_STATIC_IP_ADDRESS);
        mStaticIpAddress.setOnPreferenceChangeListener(this);
        //mStaticIpAddress.getEditText().addTextChangedListener(this);
        mNetworkPrefixLength = (CustomEditTextPreference) findPreference(KEY_NETWORK_PREFIX_LENGTH);
        mNetworkPrefixLength.setOnPreferenceChangeListener(this);
        //mNetworkPrefixLength.getEditText().addTextChangedListener(this);
        mGateway = (CustomEditTextPreference) findPreference(KEY_GATEWAY);
        mGateway.setOnPreferenceChangeListener(this);
        //mGateway.getEditText().addTextChangedListener(this);
        mDns1 = (CustomEditTextPreference) findPreference(KEY_DNS1);
        mDns1.setOnPreferenceChangeListener(this);
        //mDns1.getEditText().addTextChangedListener(this);
        mDns2 =(CustomEditTextPreference) findPreference(KEY_DNS2);
        mDns2.setOnPreferenceChangeListener(this);
        //mDns2.getEditText().addTextChangedListener(this);

        updateEthernetFields();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mEm != null) {
            mEm.addListener(mEthernetListener);
        }
        if (mCm != null) {
            NetworkRequest request = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build();
            mCm.registerNetworkCallback(request, mNetworkCallback);
        }

        if (mEm != null) {
            updateEthernetSwitch(mEm.getEthernetState());
            updateEthernetFields();
        }
        if ((mEm != null) && (mEm.getEthernetSleepPolicy() == EthernetManager.ETHERNET_POLICY_DISCONNECT_WHEN_SUSPEND)) {
            mEthernetPolicy.setChecked(false);
        } else {
            mEthernetPolicy.setChecked(true);
        }
        readIpConfiguration();
        getActivity().registerReceiver(mEthernetStateReceiver, new IntentFilter(EthernetManager.ETHERNET_STATE_CHANGED_ACTION));
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEm != null) {
            mEm.removeListener(mEthernetListener);
        }
        if (mCm != null) {
            mCm.unregisterNetworkCallback(mNetworkCallback);
        }
        getActivity().unregisterReceiver(mEthernetStateReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mEthernetSwitch.removeOnSwitchChangeListener(this);
        mEthernetSwitch.hide();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ETHERNET;
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (isChecked) {
            writeIpConfiguration();
            readIpConfiguration();
        }

        synchronized (mEthernetSwitchByCode) {
            if ((mEm != null) && (mEm.isAvailable()) && !mEthernetSwitchByCode) {
                mEm.setEthernetEnabled(isChecked);
            }
        }
        readIpConfiguration();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        if (preference.getKey().equals(KEY_CONNECTION_TYPE)) {
            return true;
        } else if (preference.getKey().equals(KEY_ETHERNET_POLICY)) {
            if (mEm != null) {
                if (mEthernetPolicy.isChecked()) {
                    mEm.setEthernetSleepPolicy(EthernetManager.ETHERNET_POLICY_KEEP_ON_WHEN_SUSPEND);
                } else {
                    mEm.setEthernetSleepPolicy(EthernetManager.ETHERNET_POLICY_DISCONNECT_WHEN_SUSPEND);
                }
            }
            return true;
        } else if (preference instanceof CustomEditTextPreference) {
            mForegroundEditTextPreference = (CustomEditTextPreference)preference;
            setDialogPositiveEnabled(mForegroundEditTextPreference, false);
            /*mForegroundEditTextPreference.getEditText().addTextChangedListener(this);
            if (mForegroundEditTextPreference.getEditText().getText().toString() != null) {
                mForegroundEditTextPreference.getEditText().setSelection(mForegroundEditTextPreference.getEditText().getText().toString().length());
            }
            if (mForegroundEditTextPreference.getKey().equals(KEY_NETWORK_PREFIX_LENGTH)) {
                if (EthernetUtils.validateNetPrefixConfigFields(mForegroundEditTextPreference.getEditText().getText().toString())) {
                    setDialogPositiveEnabled(mForegroundEditTextPreference, true);
                }
            } else {
                if (EthernetUtils.validateIpConfigFields(mForegroundEditTextPreference.getEditText().getText().toString())) {
                    setDialogPositiveEnabled(mForegroundEditTextPreference, true);
                }
            }*/
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key == null) return true;
        if (key.equals(KEY_CONNECTION_TYPE)) {
            boolean useStaticIp = false;
            try {
                useStaticIp = (Integer.parseInt((String)newValue) == CONNECTION_TYPE_STATIC);
            } catch (NumberFormatException e) {}
            showStaticIpSettings(useStaticIp);
            writeIpConfiguration();
            // readIpConfiguration();
        } else if (preference instanceof CustomEditTextPreference) {
            ((CustomEditTextPreference)preference).setText((String)newValue);
            writeIpConfiguration();
            handleEmptyField((CustomEditTextPreference)preference);
        }
        return true;
    }

    private void showStaticIpSettings(boolean show) {
        mConnectionType.setValue(show ? String.valueOf(CONNECTION_TYPE_STATIC) : String.valueOf(CONNECTION_TYPE_DHCP));
        mConnectionType.setSummary(show ? mConnectionType.getEntries()[CONNECTION_TYPE_STATIC] : mConnectionType.getEntries()[CONNECTION_TYPE_DHCP]);
        if (show) {
            getPreferenceScreen().removePreference(mDhcpIpAddress);
            getPreferenceScreen().addPreference(mStaticIpAddress);
            getPreferenceScreen().addPreference(mNetworkPrefixLength);
            getPreferenceScreen().addPreference(mGateway);
            getPreferenceScreen().addPreference(mDns1);
            getPreferenceScreen().addPreference(mDns2);
        } else {
            getPreferenceScreen().addPreference(mDhcpIpAddress);
            getPreferenceScreen().removePreference(mStaticIpAddress);
            getPreferenceScreen().removePreference(mNetworkPrefixLength);
            getPreferenceScreen().removePreference(mGateway);
            getPreferenceScreen().removePreference(mDns1);
            getPreferenceScreen().removePreference(mDns2);
        }
    }

    private void readIpConfiguration() {
        if ((mConnectionType == null) || (mDhcpIpAddress == null) ||  (mStaticIpAddress == null) ||
                (mNetworkPrefixLength == null) || (mGateway == null) || (mDns1 == null) || (mDns2 == null)) {
            return;
        }
        mIpConfig = mEm.getConfiguration();
        if (mIpConfig.getIpAssignment() == IpConfiguration.IpAssignment.DHCP) {
            mConnectionType.setValue(String.valueOf(CONNECTION_TYPE_DHCP));
            mConnectionType.setSummary(mConnectionType.getEntries()[CONNECTION_TYPE_DHCP]);
            showStaticIpSettings(false);
        } else {
            mConnectionType.setValue(String.valueOf(CONNECTION_TYPE_STATIC));
            mConnectionType.setSummary(mConnectionType.getEntries()[CONNECTION_TYPE_STATIC]);
            showStaticIpSettings(true);
        }
        mStaticIpConfig = mIpConfig.getStaticIpConfiguration();
        if (mStaticIpConfig != null) {
            if ((mStaticIpConfig.ipAddress != null) && (mStaticIpConfig.ipAddress.getAddress() instanceof Inet4Address)) {
                mStaticIpAddress.setText(EthernetUtils.inet4AddressToString((Inet4Address)mStaticIpConfig.ipAddress.getAddress()));
                mNetworkPrefixLength.setText(Integer.valueOf(mStaticIpConfig.ipAddress.getNetworkPrefixLength()).toString());
            }
            if ((mStaticIpConfig.gateway != null) && (mStaticIpConfig.gateway instanceof Inet4Address)) {
                mGateway.setText(EthernetUtils.inet4AddressToString((Inet4Address)mStaticIpConfig.gateway));
            }
            if (mStaticIpConfig.dnsServers != null) {
                if ((mStaticIpConfig.dnsServers.size() > 0)  && (mStaticIpConfig.dnsServers.get(0) instanceof Inet4Address)) {
                    mDns1.setText(EthernetUtils.inet4AddressToString((Inet4Address)mStaticIpConfig.dnsServers.get(0)));
                }
                if ((mStaticIpConfig.dnsServers.size() > 1)  && (mStaticIpConfig.dnsServers.get(1) instanceof Inet4Address)) {
                    mDns2.setText(EthernetUtils.inet4AddressToString((Inet4Address)mStaticIpConfig.dnsServers.get(1)));
                }
            }
        }

        handleEmptyField(mStaticIpAddress);
        handleEmptyField(mNetworkPrefixLength);
        handleEmptyField(mGateway);
        handleEmptyField(mDns1);
        handleEmptyField(mDns2);
        updateDhcpIpAddress();
    }

    private void writeIpConfiguration() {
        if ((mEm == null) || (mConnectionType == null) || (mStaticIpAddress == null) ||
                (mNetworkPrefixLength == null) || (mGateway == null) || (mDns1 == null) || (mDns2 == null)) {
            return;
        }
        try {
            boolean useDhcp = (Integer.parseInt(mConnectionType.getValue()) == CONNECTION_TYPE_DHCP);
            if (mStaticIpConfig == null) {
                mStaticIpConfig = new StaticIpConfiguration();
            }
            // Static IP and network mask
            Inet4Address staticIpAddress = null;
            int networkPrefixLength = -1;
            if ((mStaticIpAddress.getText() != null) && (mStaticIpAddress.getText().length() > 0)) {
                staticIpAddress = EthernetUtils.strToInet4Address(mStaticIpAddress.getText());
            }
            if ((mNetworkPrefixLength.getText() != null) && (mNetworkPrefixLength.getText().length() > 0)) {
                networkPrefixLength = Integer.parseInt(mNetworkPrefixLength.getText());
            }
            if ((staticIpAddress != null) && (networkPrefixLength > 0)) {
                mStaticIpConfig.ipAddress = new LinkAddress(staticIpAddress, networkPrefixLength);
            }
            // Gateway
            Inet4Address gateway = null;
            if ((mGateway.getText() != null) && (mGateway.getText().length() > 0)) {
                gateway = EthernetUtils.strToInet4Address(mGateway.getText());
            }
            if (gateway != null) {
                mStaticIpConfig.gateway = gateway;
            }
            mStaticIpConfig.dnsServers.clear();
            // DNS 1
            Inet4Address dns1 = null;
            if ((mDns1.getText() != null) && (mDns1.getText().length() > 0)) {
                dns1 = EthernetUtils.strToInet4Address(mDns1.getText());
            }
            if (dns1 != null) {
                mStaticIpConfig.dnsServers.add(dns1);
            }
            // DNS 2
            Inet4Address dns2 = null;
            if ((mDns2.getText() != null) && (mDns2.getText().length() > 0)) {
                dns2 = EthernetUtils.strToInet4Address(mDns2.getText());
            }
            if ((dns1 != null) && (dns2 != null)) {
                mStaticIpConfig.dnsServers.add(dns2);
            }

            if (mIpConfig == null) {
                mIpConfig = new IpConfiguration();
            }
            mIpConfig.setIpAssignment(useDhcp ? IpConfiguration.IpAssignment.DHCP : IpConfiguration.IpAssignment.STATIC);
            mIpConfig.setProxySettings(IpConfiguration.ProxySettings.NONE);
            mIpConfig.setStaticIpConfiguration(useDhcp ? null : mStaticIpConfig);
            mEm.setConfiguration(mIpConfig);
        } catch (NumberFormatException e) {
        }

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (mForegroundEditTextPreference == null) {
            return;
        }
        if (mForegroundEditTextPreference.getKey().equals(KEY_NETWORK_PREFIX_LENGTH)) {
            if (EthernetUtils.validateNetPrefixConfigFields(mForegroundEditTextPreference.getEditText().getText().toString())) {
                setDialogPositiveEnabled(mForegroundEditTextPreference, true);
            } else {
                setDialogPositiveEnabled(mForegroundEditTextPreference, false);
            }
        } else {
            if (EthernetUtils.validateIpConfigFields(mForegroundEditTextPreference.getEditText().getText().toString())) {
                setDialogPositiveEnabled(mForegroundEditTextPreference, true);
            } else {
                setDialogPositiveEnabled(mForegroundEditTextPreference, false);
            }
        }
    }

    private void setDialogPositiveEnabled(CustomEditTextPreference editPref, boolean enable) {
        if (editPref == null) {
            return;
        }
        AlertDialog alertDialog = (AlertDialog)editPref.getDialog();
        if (alertDialog == null) {
            return;
        }
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enable);
    }

    private void handleEmptyField(CustomEditTextPreference editPref) {
        if (editPref == null) {
            return;
        }
        if ((editPref.getText() != null) && (editPref.getText().length() > 0)) {
            editPref.setSummary(editPref.getText());
            return;
        }
        if (editPref.getKey().equals(KEY_STATIC_IP_ADDRESS)) {
            editPref.setSummary(R.string.ethernet_ip_address_not_set);
        } else if (editPref.getKey().equals(KEY_NETWORK_PREFIX_LENGTH)) {
            editPref.setSummary(R.string.ethernet_network_prefix_length_not_set);
        } else if (editPref.getKey().equals(KEY_GATEWAY)) {
            editPref.setSummary(R.string.ethernet_gateway_not_set);
        } else if (editPref.getKey().equals(KEY_DNS1)) {
            editPref.setSummary(R.string.ethernet_dns1_not_set);
        } else if (editPref.getKey().equals(KEY_DNS2)) {
            editPref.setSummary(R.string.ethernet_dns2_not_set);
        }
    }

    private final BroadcastReceiver mEthernetStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int ethernetState = intent.getExtras().getInt(EthernetManager.EXTRA_ETHERNET_STATE);
            updateEthernetSwitch(ethernetState);
            updateEthernetFields();
        }
    };

    private EthernetManager.Listener mEthernetListener = new EthernetManager.Listener() {
        @Override
        public void onAvailabilityChanged(boolean isAvailable) {
            Log.d(TAG, "EthernetManager.Listener.onAvailabilityChanged");
            if (mEm != null) {
                updateEthernetSwitch(mEm.getEthernetState());
                updateEthernetFields();
            }
        }
    };

    private ConnectivityManager.NetworkCallback mNetworkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            updateDhcpIpAddress();
        }
        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            updateDhcpIpAddress();
        }
        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            updateDhcpIpAddress();
        }
        @Override
        public void onLosing(Network network, int maxMsToLive) {
            updateDhcpIpAddress();
        }
        @Override
        public void onLost(Network network) {
            updateDhcpIpAddress();
        }
    };

    private Boolean mEthernetSwitchByCode = false;
    private void updateEthernetSwitch(final int ethernetState) {
        if (mEthernetSwitch == null || mEm == null) {
            return;
        }
        final String ipAddress = EthernetUtils.getEthernetIpAddresses(mCm);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                synchronized (mEthernetSwitchByCode) {
                    mEthernetSwitchByCode = true;
                    boolean ethernetAvailable = (mEm != null) && mEm.isAvailable();
                    boolean ethernetEnabled = (ethernetState == EthernetManager.ETHERNET_STATE_ENABLING) ||  (ethernetState == EthernetManager.ETHERNET_STATE_ENABLED);
                    mEthernetSwitch.setEnabled(ethernetAvailable && ((ethernetState == EthernetManager.ETHERNET_STATE_ENABLED) || (ethernetState == EthernetManager.ETHERNET_STATE_DISABLED)));
                    mEthernetSwitch.setChecked(ethernetAvailable && ethernetEnabled);
                    mEthernetSwitchByCode = false;
                }
            }
        });
    }

    private void updateEthernetFields() {
        int ethernetState = (mEm != null) ? mEm.getEthernetState() : EthernetManager.ETHERNET_STATE_UNKNOWN;
        final boolean enable = (mEm != null) && mEm.isAvailable() && (ethernetState == EthernetManager.ETHERNET_STATE_ENABLED);
        if ((mEthernetPolicy == null) || (mConnectionType == null) || (mDhcpIpAddress == null) ||  (mStaticIpAddress == null) ||
                (mNetworkPrefixLength == null) || (mGateway == null) || (mDns1 == null) || (mDns2 == null)) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mEthernetPolicy.setEnabled(enable);
                mConnectionType.setEnabled(enable);
                updateDhcpIpAddress();
                mStaticIpAddress.setEnabled(enable);
                mNetworkPrefixLength.setEnabled(enable);
                mGateway.setEnabled(enable);
                mDns1.setEnabled(enable);
                mDns2.setEnabled(enable);
            }
        });
    }

    private void updateDhcpIpAddress() {
        if (mDhcpIpAddress == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                int ethernetState = (mEm != null) ? mEm.getEthernetState() : EthernetManager.ETHERNET_STATE_UNKNOWN;
                String ipAddress = EthernetUtils.getEthernetIpAddresses(mCm);
                if ((mEm != null) && mEm.isAvailable() && (ethernetState == EthernetManager.ETHERNET_STATE_ENABLED) && !TextUtils.isEmpty(ipAddress)) {
                    mDhcpIpAddress.setSummary(ipAddress);
                } else {
                    mDhcpIpAddress.setSummary(mContext.getString(R.string.status_unavailable));
                }
            }
        });
    }

}

