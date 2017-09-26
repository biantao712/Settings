/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.vpn2;

import java.net.InetAddress;
import java.util.Arrays;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.app.Activity;

import android.support.v7.preference.Preference;
import com.android.settings.wifi.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceGroup;
import android.text.InputType;
import android.text.TextUtils;
import android.support.v14.preference.SwitchPreference;

import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

/**
 * @author Modified by Tim_Hu
 */

public class VpnConnectSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, View.OnClickListener {
	
    private static final String TAG = "VpnConnectSettings";
    
    private static final String ARG_PROFILE = "profile";
    private static final String ARG_EDITING = "editing";
    private static final String ARG_EXISTS = "exists";
    
    private static final String KEY_CATEGORY_INFO = "vpn_category_info";
    private static final String KEY_NAME = "vpn_name";
    private static final String KEY_SERVER = "vpn_server";
    
    private static final String KEY_CATEGORY_USER = "vpn_category_user";
    private static final String KEY_USERNAME = "vpn_username";
    private static final String KEY_PASSWORD = "vpn_password";
    
    private static final String KEY_CATEGORY_TYPE = "vpn_category_type";
    private static final String KEY_TYPE = "vpn_type";
    private static final String KEY_MPPE = "vpn_mppe";
    private static final String KEY_L2TP_SECRET = "vpn_l2tp_secret";
    private static final String KEY_IPSEC_IDENTIFIER = "vpn_ipsec_identifier";
    private static final String KEY_IPSEC_SECRET = "vpn_ipsec_secret";    
    private static final String KEY_IPSEC_USER_CERT = "vpn_ipsec_user_cert";
    private static final String KEY_IPSEC_CA_CERT = "vpn_ipsec_ca_cert";
    private static final String KEY_IPSEC_SERVER_CERT = "vpn_ipsec_server_cert";
    
    private static final String KEY_CATEGORY_ADVANCED = "vpn_category_advanced";
    private static final String KEY_SEARCH_DOMAINS = "vpn_search_domains";
    private static final String KEY_DNS_SERVERS = "vpn_dns_servers";
    private static final String KEY_ROUTES = "vpn_routes";
	
	private EditTextPreference mName;
	private EditTextPreference mServer;
	private EditTextPreference mUsername;
	private EditTextPreference mPassword;
	
	private ListPreference mType;
	private SwitchPreference mMppe;
	
	private EditTextPreference mL2tpSecret;
	private EditTextPreference mIpsecIdentifier;
	private EditTextPreference mIpsecSecret;
	
	private ListPreference mIpsecUserCert;
	private ListPreference mIpsecCaCert;
	private ListPreference mIpsecServerCert;
	
	private EditTextPreference mSearchDomains;
	private EditTextPreference mDnsServers;
	private EditTextPreference mRoutes;

	private int mTypeIndex = 0;
	private int mIpsecUserCertIndex = 0;  // = -1;  //default not selected
	private int mIpsecCaCertIndex = 0;
	private int mIpsecServerCertIndex = 0;
	
	private Button mFooterRightButton;

	private VpnProfile mProfile;
	private boolean mEdit = false;
	private boolean mExists = false;

    private boolean mUnlocking = false;
    
    private final IConnectivityManager mService = IConnectivityManager.Stub.asInterface(
            ServiceManager.getService(Context.CONNECTIVITY_SERVICE));

    public static void show(VpnSettings parent, VpnProfile profile, boolean edit, boolean exists) {
        if (!parent.isAdded()) return;

        Bundle args = new Bundle();
        args.putParcelable(ARG_PROFILE, profile);
        args.putBoolean(ARG_EDITING, edit);
        args.putBoolean(ARG_EXISTS, exists);
        
        parent.showSettingsFragment(VpnConnectSettings.class.getCanonicalName(), R.string.vpn_add_title, args);
    }
    
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.TETHER;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.vpn_connect_settings);
        setRetainInstance(true);
        
        final Bundle arguments = getArguments();
        if(arguments != null){
        	if (arguments.containsKey(ARG_PROFILE))
        		mProfile = arguments.getParcelable(ARG_PROFILE);
        	if (arguments.containsKey(ARG_EDITING))
        		mEdit = arguments.getBoolean(ARG_EDITING);
        	if (arguments.containsKey(ARG_EXISTS))
        		mExists = arguments.getBoolean(ARG_EXISTS);
        }
        if(mProfile == null){
    		finish();
    		return;
    	}
        
        initPreferences();
        
        if (!mExists) { // add network
        	setDefaultPreferences(true);
        } else{
        	setDefaultPreferences(false);
        	setPreferencesValues();
        	
        	if(mProfile.name != null){
        		getActivity().setTitle(mEdit ? mProfile.name :
        			getString(R.string.vpn_connect_to, mProfile.name));
        	}
        }
        
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if(mExists && mProfile.name != null){
    		getActivity().setTitle(mEdit ? mProfile.name :
    			getString(R.string.vpn_connect_to, mProfile.name));
    	}
        initFooterButtonBar();
        enableSubmitIfAppropriate();
	}

    @Override
    public void onResume() {
        super.onResume();

        // Check KeyStore here, so others do not need to deal with it.
        if (!KeyStore.getInstance().isUnlocked()) {
            if (!mUnlocking) {
                // Let us unlock KeyStore. See you later!
                Credentials.getInstance().unlock(getActivity());
            } else {
                // We already tried, but it is still not working!
            	finish();
            }
            mUnlocking = !mUnlocking;
            return;
        }

        // Now KeyStore is always unlocked. Reset the flag.
        mUnlocking = false;
        
        if(mPassword != null && mPassword.isVisible())
        	mPassword.setFocus(true);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putBoolean(ARG_EDITING, mEdit);
        outState.putBoolean(ARG_EXISTS, mExists);
        if (mProfile != null)
        	outState.putParcelable(ARG_PROFILE, mProfile);
    }

    @Override
    public void onClick(View v) {
    	int buttonId = v.getId();
    	switch (buttonId){
	    	case R.id.footerLeftButton:
	    		finish();
	    		break;
	    	case R.id.footerRightButton:
	    		onSave();
	        	finish();
	    		break;
    	}
    }
    
    private void onSave(){
		Log.d("timhu", "onSave");
		VpnProfile profile = getProfile();
		
		if (profile != null) {
			// Update KeyStore entry
            KeyStore.getInstance().put(Credentials.VPN + profile.key, profile.encode(),
                    KeyStore.UID_SELF, /* flags */ 0);

            // Flush out old version of profile
            disconnect(profile);

//            updateLockdownVpn(dialog.isVpnAlwaysOn(), profile);
            
            // If we are not editing, connect!
            if (!mEdit && !VpnUtils.isVpnLockdown(profile.key)) {
                try {
                    connect(profile);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to connect", e);
                }
            }
        }
    }

    private void updateLockdownVpn(boolean isVpnAlwaysOn, VpnProfile profile) {
        // Save lockdown vpn
        if (isVpnAlwaysOn) {
            // Show toast if vpn profile is not valid
            if (!profile.isValidLockdownProfile()) {
                Toast.makeText(getContext(), R.string.vpn_lockdown_config_error,
                        Toast.LENGTH_LONG).show();
                return;
            }

            final ConnectivityManager conn = ConnectivityManager.from(getActivity());
            conn.setAlwaysOnVpnPackageForUser(UserHandle.myUserId(), null,
                    /* lockdownEnabled */ false);
            VpnUtils.setLockdownVpn(getContext(), profile.key);
        } else {
            // update only if lockdown vpn has been changed
            if (VpnUtils.isVpnLockdown(profile.key)) {
                VpnUtils.clearLockdownVpn(getContext());
            }
        }
    }

    private void connect(VpnProfile profile) throws RemoteException {
        try {
            mService.startLegacyVpn(profile);
        } catch (IllegalStateException e) {
            Toast.makeText(getActivity(), R.string.vpn_no_network, Toast.LENGTH_LONG).show();
        }
    }

    private void disconnect(VpnProfile profile) {
        try {
            LegacyVpnInfo connected = mService.getLegacyVpnInfo(UserHandle.myUserId());
            if (connected != null && profile.key.equals(connected.key)) {
                VpnUtils.clearLockdownVpn(getContext());
                mService.prepareVpn(VpnConfig.LEGACY_VPN, VpnConfig.LEGACY_VPN,
                        UserHandle.myUserId());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to disconnect", e);
        }
    }
    
    private void addPreferenceChangeListener(PreferenceGroup preferenceGroup){
		if(preferenceGroup != null){
			for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
				preferenceGroup.getPreference(i).setOnPreferenceChangeListener(this);
		      }
		}
	}
	
	private void initPreferences(){
        //category info
        mName = (EditTextPreference) findPreference(KEY_NAME);
        mName.init(false);
        mServer = (EditTextPreference) findPreference(KEY_SERVER);
        mServer.init(true);
        
        PreferenceGroup categoryInfo = (PreferenceGroup) findPreference(KEY_CATEGORY_INFO);
        addPreferenceChangeListener(categoryInfo);
        
        //category user
        mUsername = (EditTextPreference) findPreference(KEY_USERNAME);
        mUsername.init(R.string.vpn_username_hint, false);
        mPassword = (EditTextPreference) findPreference(KEY_PASSWORD);
        int hintResid = mEdit ? R.string.vpn_password_hint : R.string.vpn_password_connect_hint;
        mPassword.init((InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
        		,hintResid, true);
        
        PreferenceGroup categoryUser = (PreferenceGroup) findPreference(KEY_CATEGORY_USER);
        addPreferenceChangeListener(categoryUser);
        
      //category type
        mType = (ListPreference) findPreference(KEY_TYPE);
        mMppe = (SwitchPreference) findPreference(KEY_MPPE);
        mIpsecUserCert = (ListPreference) findPreference(KEY_IPSEC_USER_CERT);
        mIpsecCaCert = (ListPreference) findPreference(KEY_IPSEC_CA_CERT);
        mIpsecServerCert = (ListPreference) findPreference(KEY_IPSEC_SERVER_CERT);
        
        mL2tpSecret = (EditTextPreference) findPreference(KEY_L2TP_SECRET);
        mL2tpSecret.init(false);
        mIpsecIdentifier = (EditTextPreference) findPreference(KEY_IPSEC_IDENTIFIER);
        mIpsecIdentifier.init(false);
        mIpsecSecret = (EditTextPreference) findPreference(KEY_IPSEC_SECRET);
        mIpsecSecret.init(true);
        
        PreferenceGroup categoryType = (PreferenceGroup) findPreference(KEY_CATEGORY_TYPE);
        addPreferenceChangeListener(categoryType);
        
      //category advanced
        mSearchDomains = (EditTextPreference) findPreference(KEY_SEARCH_DOMAINS);
        mSearchDomains.init(false);
        mDnsServers = (EditTextPreference) findPreference(KEY_DNS_SERVERS);
        mDnsServers.init(R.string.vpn_dns_servers_hint, false);
        mRoutes = (EditTextPreference) findPreference(KEY_ROUTES);
        mRoutes.init(R.string.vpn_routes_hint, true);
        
        PreferenceGroup categoryAdvanced = (PreferenceGroup) findPreference(KEY_CATEGORY_ADVANCED);
        addPreferenceChangeListener(categoryAdvanced);
        
	}
	
	//add network
	private void setDefaultPreferences(boolean validate){
		if(validate){
			validateTypeFields();
		}
		
		updateListPreferencesValues();
		mMppe.setChecked(true);
	}

	private void setPreferencesValues(){
    	mName.setText(mProfile.name);
    	mServer.setText(mProfile.server);
    	if (mProfile.saveLogin) {
            mUsername.setText(mProfile.username);
            mPassword.setText(mProfile.password);
        }
    	
    	mMppe.setChecked(mProfile.mppe);
    	mL2tpSecret.setText(mProfile.l2tpSecret);
        mIpsecIdentifier.setText(mProfile.ipsecIdentifier);
        mIpsecSecret.setText(mProfile.ipsecSecret);
        
    	mSearchDomains.setText(mProfile.searchDomains);
        mDnsServers.setText(mProfile.dnsServers);
        mRoutes.setText(mProfile.routes);
    	
    	
    	mTypeIndex = checkArrayIndex(mProfile.type, R.array.vpn_types);
    	mIpsecUserCertIndex = checkArrayIndex(mProfile.ipsecUserCert, R.array.vpn_user_cert_entries);
    	mIpsecCaCertIndex = checkArrayIndex(mProfile.ipsecCaCert, R.array.vpn_ca_cert_entries);
    	mIpsecServerCertIndex = checkArrayIndex(mProfile.ipsecServerCert, R.array.vpn_server_cert_entries);
//    	mIpsecUserCertIndex = mProfile.ipsecUserCert;
//    	mIpsecCaCertIndex = mProfile.ipsecCaCert;
//    	mIpsecServerCertIndex = mProfile.ipsecServerCert;
				
		validateTypeFields();
		updateListPreferencesValues();
		
		validateEditFields();
	}
	
	private void updateListPreferencesValues(){
		mType.setValueIndex(mTypeIndex);
		mType.setSummary(findValueofIndex(mTypeIndex, R.array.vpn_types));
		
		if(mIpsecUserCertIndex > -1){
		mIpsecUserCert.setValueIndex(mIpsecUserCertIndex);
		mIpsecUserCert.setSummary(findValueofIndex(mIpsecUserCertIndex, R.array.vpn_user_cert_entries));
		}
		
		mIpsecCaCert.setValueIndex(mIpsecCaCertIndex);
		mIpsecCaCert.setSummary(findValueofIndex(mIpsecCaCertIndex, R.array.vpn_ca_cert_entries));
		
		mIpsecServerCert.setValueIndex(mIpsecServerCertIndex);
		mIpsecServerCert.setSummary(findValueofIndex(mIpsecServerCertIndex, R.array.vpn_server_cert_entries));
	}
	
	private void validateEditFields() {
		if(!mEdit){
			//connect , only username and password visible
			mName.setVisible(false);
			mServer.setVisible(false);
			
			mType.setVisible(false);
			mMppe.setVisible(false);
			
			mL2tpSecret.setVisible(false);
			mIpsecIdentifier.setVisible(false);
			mIpsecSecret.setVisible(false);
			
			mIpsecUserCert.setVisible(false);
			mIpsecCaCert.setVisible(false);
			mIpsecServerCert.setVisible(false);
			
			mSearchDomains.setVisible(false);
			mDnsServers.setVisible(false);
			mRoutes.setVisible(false);
			
			//hide category
			PreferenceGroup categoryInfo = (PreferenceGroup) findPreference(KEY_CATEGORY_INFO);
			categoryInfo.setVisible(false);
			PreferenceGroup categoryUser = (PreferenceGroup) findPreference(KEY_CATEGORY_USER);
			categoryUser.setVisible(false);
			PreferenceGroup categoryType = (PreferenceGroup) findPreference(KEY_CATEGORY_TYPE);
			categoryType.setVisible(false);
			PreferenceGroup categoryAdvanced = (PreferenceGroup) findPreference(KEY_CATEGORY_ADVANCED);
			categoryAdvanced.setVisible(false);
		}
	}
	
	private void validateTypeFields() {
		mMppe.setVisible(false);
		
		mL2tpSecret.setVisible(false);
		mIpsecIdentifier.setVisible(false);
		mIpsecSecret.setVisible(false);
		
		mIpsecUserCert.setVisible(false);
		mIpsecCaCert.setVisible(false);
		mIpsecServerCert.setVisible(false);
    	
		switch (mTypeIndex){
	        case VpnProfile.TYPE_PPTP:
	        	mMppe.setVisible(true);
	            break;
	
	        case VpnProfile.TYPE_L2TP_IPSEC_PSK:
	        	mL2tpSecret.setVisible(true);
	            // fall through
	        case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
	        	mIpsecIdentifier.setVisible(true);
	    		mIpsecSecret.setVisible(true);
	            break;
	
	        case VpnProfile.TYPE_L2TP_IPSEC_RSA:
	        	mL2tpSecret.setVisible(true);
	            // fall through
	        case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
	        	mIpsecUserCert.setVisible(true);
	            // fall through
	        case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
	        	mIpsecCaCert.setVisible(true);
	    		mIpsecServerCert.setVisible(true);
	            break;
       }
	}
	
	private int checkArrayIndex(int index, int arrayId){
		String[] values = getResources().getStringArray(arrayId);
		if(index > -1 && index < values.length){
			return index;
		}
		return 0;
	}
	
	private int checkArrayIndex(String entry, int arrayId){
		String[] entrys = getResources().getStringArray(arrayId);
		for(int i = 0; i< entrys.length; i++){
			if(entrys[i].equals(entry)){
				return i;
			}
		}

		return 0;
	}
	
	private void initFooterButtonBar(){
		final Activity activity = getActivity();
		View footerButtonBar = activity.findViewById(R.id.footer_button_bar);
		if(footerButtonBar != null) {
			footerButtonBar.setVisibility(View.VISIBLE);
			
			View footerLeftButton = activity.findViewById(R.id.footerLeftButton);
			footerLeftButton.setOnClickListener(this);
			mFooterRightButton = (Button) activity.findViewById(R.id.footerRightButton);
			mFooterRightButton.setOnClickListener(this);
			
			if(!mEdit)
				mFooterRightButton.setText(R.string.wifi_connect);
		}
	}
    
	@Override
    public boolean onPreferenceTreeClick(Preference preference) {
    	
    	return super.onPreferenceTreeClick(preference);
	}
	
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (KEY_TYPE.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mTypeIndex = index;
        	
        	mType.setValueIndex(mTypeIndex);
    		mType.setSummary(findValueofIndex(mTypeIndex, R.array.vpn_types));
        	
        	//need validate
            validateTypeFields();
        } else if (KEY_IPSEC_USER_CERT.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mIpsecUserCertIndex = index;
        	
        	mIpsecUserCert.setValueIndex(mIpsecUserCertIndex);
    		mIpsecUserCert.setSummary(findValueofIndex(mIpsecUserCertIndex, R.array.vpn_user_cert_entries));
        } else if (KEY_IPSEC_CA_CERT.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mIpsecCaCertIndex = index;
        	
        	mIpsecCaCert.setValueIndex(mIpsecCaCertIndex);
    		mIpsecCaCert.setSummary(findValueofIndex(mIpsecCaCertIndex, R.array.vpn_ca_cert_entries));
        } else if (KEY_IPSEC_SERVER_CERT.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mIpsecServerCertIndex = index;
        	
        	mIpsecServerCert.setValueIndex(mIpsecServerCertIndex);
    		mIpsecServerCert.setSummary(findValueofIndex(mIpsecServerCertIndex, R.array.vpn_server_cert_entries));
        } else if (KEY_MPPE.equals(key)) {
        	
        } else if (preference instanceof EditTextPreference) {
        	
        }
        
        enableSubmitIfAppropriate();

        return true;
    }
	
	private String findValueofIndex(int index, int arrayId){
		String value = new String();
		String[] values = getResources().getStringArray(arrayId);
		if(index > -1 && index < values.length)
			value = values[index];
    	return value;
	}
	
	private String checkNull(String value) {
        if (value == null || value.length() == 0) {
            return getResources().getString(R.string.wifi_ap_not_set);
        } else {
            return value;
        }
    }
	
	private String getText(EditTextPreference editText) {
		String value = editText.getText();
        if (value == null || value.length() == 0) {
            return "";
        } else {
            return value;
        }
    }
	
	void enableSubmitIfAppropriate() {
        if (mFooterRightButton == null) return;

        mFooterRightButton.setEnabled(isSubmittable());
    }
	
	private boolean isSubmittable() {
        if (!mEdit) {
            return !TextUtils.isEmpty(getText(mUsername)) && !TextUtils.isEmpty(getText(mPassword));
        }
//        if (mAlwaysOnVpn.isChecked() && !getProfile().isValidLockdownProfile()) {
//            return false;
//        }
        if (TextUtils.isEmpty(getText(mName)) ||
        		TextUtils.isEmpty(getText(mServer)) ||
        		TextUtils.isEmpty(getText(mUsername)) ||
                !validateAddresses(getText(mDnsServers), false) ||
                !validateAddresses(getText(mRoutes), true)) {
            return false;
        }
        switch (mTypeIndex) {
            case VpnProfile.TYPE_PPTP:
            case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
                return true;

            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                return !TextUtils.isEmpty(getText(mIpsecSecret));

            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                return mIpsecUserCertIndex > -1;
        }
        return false;
    }

    private boolean validateAddresses(String addresses, boolean cidr) {
        try {
            for (String address : addresses.split(" ")) {
                if (address.isEmpty()) {
                    continue;
                }
                // Legacy VPN currently only supports IPv4.
                int prefixLength = 32;
                if (cidr) {
                    String[] parts = address.split("/", 2);
                    address = parts[0];
                    prefixLength = Integer.parseInt(parts[1]);
                }
                byte[] bytes = InetAddress.parseNumericAddress(address).getAddress();
                int integer = (bytes[3] & 0xFF) | (bytes[2] & 0xFF) << 8 |
                        (bytes[1] & 0xFF) << 16 | (bytes[0] & 0xFF) << 24;
                if (bytes.length != 4 || prefixLength < 0 || prefixLength > 32 ||
                        (prefixLength < 32 && (integer << prefixLength) != 0)) {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
	
	private VpnProfile getProfile(){
        // First, save common fields.
        VpnProfile profile = new VpnProfile(mProfile.key);
        profile.name = getText(mName).trim();
        profile.type = mTypeIndex;
        profile.server = getText(mServer).trim();
        profile.username = getText(mUsername);
        profile.password = getText(mPassword);
        profile.searchDomains = getText(mSearchDomains).trim();
        profile.dnsServers = getText(mDnsServers).trim();
        profile.routes = getText(mRoutes).trim();

        // Then, save type-specific fields.
        switch (profile.type) {
            case VpnProfile.TYPE_PPTP:
                profile.mppe = mMppe.isChecked();
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_PSK:
                profile.l2tpSecret = getText(mL2tpSecret);
                // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
                profile.ipsecIdentifier = getText(mIpsecIdentifier);
                profile.ipsecSecret = getText(mIpsecSecret);
                break;

            case VpnProfile.TYPE_L2TP_IPSEC_RSA:
                profile.l2tpSecret = getText(mL2tpSecret);
                // fall through
            case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
                if (mIpsecUserCertIndex > 0) {
                    profile.ipsecUserCert = findValueofIndex(mIpsecUserCertIndex, R.array.vpn_user_cert_entries);
                }
                // fall through
            case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
                if (mIpsecCaCertIndex > 0) {
                    profile.ipsecCaCert = findValueofIndex(mIpsecCaCertIndex, R.array.vpn_ca_cert_entries);
                }
                if (mIpsecServerCertIndex > 0) {
                    profile.ipsecServerCert = findValueofIndex(mIpsecServerCertIndex, R.array.vpn_server_cert_entries);
                }
                break;
        }

        boolean hasLogin = !profile.username.isEmpty() || !profile.password.isEmpty();
        profile.saveLogin = hasLogin;
        
        return profile;
	}
}
