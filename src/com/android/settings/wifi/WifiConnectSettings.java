package com.android.settings.wifi;

import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.app.Activity;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import android.support.v7.preference.Preference;
import com.android.settings.wifi.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceGroup;
import android.text.InputType;
import android.text.InputFilter;
import com.android.settings.util.RemoveSpecialCharFilter;
import com.android.settings.util.Utf8ByteLengthFilter;
import android.text.TextUtils;
import android.support.v14.preference.SwitchPreference;

import android.security.Credentials;
import android.security.KeyStore;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.ProxySelector;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.wifi.AccessPoint;

/**
 * should <b>setSelectedAccessPoint</b> before create
 * @see #setSelectedAccessPoint(AccessPoint accessPoint, int dialogMode)
 */
public class WifiConnectSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
	
	private static final String TAG = "WifiConnectSettings";

	private static final String KEY_SSID_CATEGORY = "ssid_category";
	private static final String KEY_SSID = "ssid";
	
	private static final String KEY_SECURITY_CATEGORY = "security_category";
	private static final String KEY_SECURITY = "security";
	private static final String KEY_SECURITY_EAP_METHOD = "security_eap_method";
	private static final String KEY_SECURITY_EAP_PHASE2 = "security_eap_phase2";
	private static final String KEY_SECURITY_EAP_CA_CERT = "security_eap_ca_cert";
	private static final String KEY_SECURITY_EAP_DOMAIN = "security_eap_domain";
	private static final String KEY_SECURITY_EAP_USER_CERT = "security_eap_user_cert";
	private static final String KEY_SECURITY_EAP_IDENTITY = "security_eap_identity";
	private static final String KEY_SECURITY_EAP_ANONYMOUS = "security_eap_anonymous";
	private static final String KEY_PASSWORD = "password";
	
	private static final String KEY_ADVANCED_CATEGORY = "wifi_advanced_category";
	private static final String KEY_ENABLE_ADVANCED = "enable_advanced";
	private static final String KEY_PROXY_SETTINGS = "proxy_settings";
	private static final String KEY_PROXY_PAC = "proxy_pac";
	private static final String KEY_PROXY_HOSTNAME = "proxy_hostname";
	private static final String KEY_PROXY_PORT = "proxy_port";
	private static final String KEY_PROXY_EXCLUSIONLIST = "proxy_exclusionlist";
	
	private static final String KEY_IP_SETTINGS = "ip_settings";
	private static final String KEY_IP_ADDRESS = "ipaddress";
	private static final String KEY_GATEWAY = "gateway";
	private static final String KEY_NETWORK_PREFIX = "network_prefix_length";
	private static final String KEY_DNS1 = "dns1";
	private static final String KEY_DNS2 = "dns2";
	
	/* This value comes from "wifi_ip_settings" resource array */
    private static final int DHCP = 0;
    private static final int STATIC_IP = 1;

    /* These values come from "wifi_proxy_settings" resource array */
    public static final int PROXY_NONE = 0;
    public static final int PROXY_STATIC = 1;
    public static final int PROXY_PAC = 2;

    /* These values come from "wifi_eap_method" resource array */
    public static final int WIFI_EAP_METHOD_PEAP = 0;
    public static final int WIFI_EAP_METHOD_TLS  = 1;
    public static final int WIFI_EAP_METHOD_TTLS = 2;
    public static final int WIFI_EAP_METHOD_PWD  = 3;
    public static final int WIFI_EAP_METHOD_SIM  = 4;
    public static final int WIFI_EAP_METHOD_AKA  = 5;
    public static final int WIFI_EAP_METHOD_AKA_PRIME  = 6;

    /* These values come from "wifi_peap_phase2_entries" resource array */
    public static final int WIFI_PEAP_PHASE2_NONE       = 0;
    public static final int WIFI_PEAP_PHASE2_MSCHAPV2   = 1;
    public static final int WIFI_PEAP_PHASE2_GTC        = 2;
    
    private static final String SYSTEM_CA_STORE_PATH = "/system/etc/security/cacerts";
    
    private static AccessPoint mAccessPoint = null;
    private static int mMode = WifiConfigUiBase.MODE_CONNECT;
    
	private WifiConfiguration mWifiConfig;
	private WifiManager mWifiManager;
	
	private EditTextPreference mSsid;
	
	private PreferenceGroup mSecurityCategory;
	private ListPreference mSecurity;
	private ListPreference mEapMethodList;
	private ListPreference mEapPhase2List;
	private ListPreference mEapCACertList;
	private EditTextPreference mEapDomainEdit;
	private ListPreference mEapUserCertList;
	private EditTextPreference mEapIdentityEdit;
	private EditTextPreference mEapAnonymousEdit;
	private EditTextPreference mPassword;

    private SwitchPreference mEnableAdvanced;
	private ListPreference mProxyList;
	private EditTextPreference mProxyPacEdit;
	private EditTextPreference mProxyHostnameEdit;
	private EditTextPreference mProxyPortEdit;
	private EditTextPreference mProxyExcluEdit;
	
	private ListPreference mIpList;
	private EditTextPreference mIpAddressEdit;
	private EditTextPreference mIpGatewayEdit;
	private EditTextPreference mIpPrefixEdit;
	private EditTextPreference mIpDNS1Edit;
	private EditTextPreference mIpDNS2Edit;
	
	private int mSecurityIndex = 0;
	private int mEapMethodListIndex = Eap.SIM;
	private int mEapPhase2ListIndex = 0;
	private int mEapCACertListIndex = 0;
	private int mEapUserCertListIndex = 0;
	private int mProxyListIndex = 0;
	private int mIpListIndex = 0;
	
	private IpAssignment mIpAssignment = IpAssignment.UNASSIGNED;
    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;
    
    private Button mFooterRightButton;

    private String mUnspecifiedCertString;
    private String mMultipleCertSetString;
    private String mUseSystemCertsString;
    private String mDoNotProvideEapUserCertString;
    private String mDoNotValidateEapServerString;

    /**
     * <b>static</b> method<p>
     * init data before create
     */
    public static void setSelectedAccessPoint(AccessPoint accessPoint, int dialogMode){
		mAccessPoint = accessPoint;
		mMode = dialogMode;
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
        addPreferencesFromResource(R.xml.wifi_connect_settings);
        setRetainInstance(true);
        
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        initPreferences();
        
        if (mAccessPoint == null) { // add network
        	setDefaultPreferences(true);
        } else{
        	getActivity().setTitle(mAccessPoint.getSsidStr());
        	setDefaultPreferences(false);
        	setPreferencesValues();
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
		//category ssid
        PreferenceGroup ssidCategory = (PreferenceGroup) findPreference(KEY_SSID_CATEGORY);
        addPreferenceChangeListener(ssidCategory);
        
        mSsid = (EditTextPreference) findPreference(KEY_SSID);
        mSsid.init(InputType.TYPE_CLASS_TEXT, R.string.wifi_ssid_hint, true,
                new InputFilter[] {new Utf8ByteLengthFilter(32), new RemoveSpecialCharFilter(true)});
        
      //category security
        mSecurityCategory = (PreferenceGroup) findPreference(KEY_SECURITY_CATEGORY);
        addPreferenceChangeListener(mSecurityCategory);
        
        mSecurity = (ListPreference) findPreference(KEY_SECURITY);
        mEapMethodList = (ListPreference) findPreference(KEY_SECURITY_EAP_METHOD);
        mEapPhase2List = (ListPreference) findPreference(KEY_SECURITY_EAP_PHASE2);
        mEapCACertList = (ListPreference) findPreference(KEY_SECURITY_EAP_CA_CERT);
        
        mEapDomainEdit = (EditTextPreference) findPreference(KEY_SECURITY_EAP_DOMAIN);
        mEapDomainEdit.init(R.string.wifi_eap_domain_hint, false);
        
        mEapUserCertList = (ListPreference) findPreference(KEY_SECURITY_EAP_USER_CERT);
        
        mEapIdentityEdit = (EditTextPreference) findPreference(KEY_SECURITY_EAP_IDENTITY);
        mEapIdentityEdit.init(R.string.wifi_eap_identity_hint, false);
        
        mEapAnonymousEdit = (EditTextPreference) findPreference(KEY_SECURITY_EAP_ANONYMOUS);
        mEapAnonymousEdit.init(R.string.wifi_eap_anonymous_hint, false);
        
        mPassword = (EditTextPreference) findPreference(KEY_PASSWORD);
        mPassword.init((InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
        		,R.string.wifi_password_hint, true);

        mEnableAdvanced = (SwitchPreference) findPreference(KEY_ENABLE_ADVANCED);
      //category ip settings
        PreferenceGroup advancedCategory = (PreferenceGroup) findPreference(KEY_ADVANCED_CATEGORY);
        addPreferenceChangeListener(advancedCategory);
        
        mProxyList = (ListPreference) findPreference(KEY_PROXY_SETTINGS);
        mProxyPacEdit = (EditTextPreference) findPreference(KEY_PROXY_PAC);
        mProxyPacEdit.init(InputType.TYPE_TEXT_VARIATION_URI, R.string.proxy_pac_hint);//
        
        mProxyHostnameEdit = (EditTextPreference) findPreference(KEY_PROXY_HOSTNAME);
        mProxyHostnameEdit.init(InputType.TYPE_CLASS_TEXT, R.string.proxy_hostname_hint);
        
        mProxyPortEdit = (EditTextPreference) findPreference(KEY_PROXY_PORT);
        mProxyPortEdit.init(InputType.TYPE_CLASS_NUMBER, R.string.proxy_port_hint);
        
        mProxyExcluEdit = (EditTextPreference) findPreference(KEY_PROXY_EXCLUSIONLIST);
        mProxyExcluEdit.init(InputType.TYPE_CLASS_TEXT, R.string.proxy_exclusionlist_hint);
        
        mIpList = (ListPreference) findPreference(KEY_IP_SETTINGS);
        mIpAddressEdit = (EditTextPreference) findPreference(KEY_IP_ADDRESS);
        mIpAddressEdit.init(InputType.TYPE_CLASS_TEXT, R.string.wifi_ip_address_hint);
        
        mIpGatewayEdit = (EditTextPreference) findPreference(KEY_GATEWAY);
        mIpGatewayEdit.init(InputType.TYPE_CLASS_TEXT, R.string.wifi_gateway_hint);
        
        mIpPrefixEdit = (EditTextPreference) findPreference(KEY_NETWORK_PREFIX);
        mIpPrefixEdit.init(InputType.TYPE_CLASS_NUMBER, R.string.wifi_network_prefix_length_hint);
        
        mIpDNS1Edit = (EditTextPreference) findPreference(KEY_DNS1);
        mIpDNS1Edit.init(InputType.TYPE_CLASS_TEXT, R.string.wifi_dns1_hint);
        
        mIpDNS2Edit = (EditTextPreference) findPreference(KEY_DNS2);
        mIpDNS2Edit.init(InputType.TYPE_CLASS_TEXT, R.string.wifi_dns2_hint, true);
	}
	
	//add network
	private void setDefaultPreferences(boolean validate){
        Context context = getActivity();
        mUnspecifiedCertString = context.getString(R.string.wifi_unspecified);
        mMultipleCertSetString = context.getString(R.string.wifi_multiple_cert_added);
        mUseSystemCertsString = context.getString(R.string.wifi_use_system_certs);
        mDoNotProvideEapUserCertString = context.getString(R.string.wifi_do_not_provide_eap_user_cert);
        mDoNotValidateEapServerString = context.getString(R.string.wifi_do_not_validate_eap_server);

        loadCertificates(
                mEapCACertList,
                Credentials.CA_CERTIFICATE,
                mDoNotValidateEapServerString,
                false,
                true);
        loadCertificates(
                mEapUserCertList,
                Credentials.USER_PRIVATE_KEY,
                mDoNotProvideEapUserCertString,
                false,
                false);

		if(validate){
			validateSecurityFields();
			validateProxyFields();
			validateIpFields();
		}
		
		updateListPreferencesValues();
		
//		mSsid.setSummary(checkNull(null));
//		mEapDomainEdit.setSummary(checkNull(null));
//		mEapIdentityEdit.setSummary(checkNull(null));
//		mEapAnonymousEdit.setSummary(checkNull(null));
//		mPassword.setSummary(checkNull(null));
//		mProxyPacEdit.setSummary(checkNull(null));
//		mProxyHostnameEdit.setSummary(checkNull(null));
//		mProxyPortEdit.setSummary(checkNull(null));
//		mProxyExcluEdit.setSummary(checkNull(null));
//		mIpAddressEdit.setSummary(checkNull(null));
//		mIpGatewayEdit.setSummary(checkNull(null));
//		mIpPrefixEdit.setSummary(checkNull(null));
//		mIpDNS1Edit.setSummary(checkNull(null));
//		mIpDNS2Edit.setSummary(checkNull(null));
	}

    private void loadCertificates(
            ListPreference listPreference,
            String prefix,
            String noCertificateString,
            boolean showMultipleCerts,
            boolean showUsePreinstalledCertOption) {
        ArrayList<String> certs = new ArrayList<String>();
        certs.add(mUnspecifiedCertString);
        certs.add(noCertificateString);
        if (showMultipleCerts) {
            certs.add(mMultipleCertSetString);
        }
        if (showUsePreinstalledCertOption) {
            certs.add(mUseSystemCertsString);
        }
        certs.addAll(
                Arrays.asList(KeyStore.getInstance().list(prefix, android.os.Process.WIFI_UID)));
//        certs.add(noCertificateString);

        String[] entryValues = new String[certs.size()];
        for(int i = 0; i < certs.size(); i++ ){
            entryValues[i] = String.valueOf(i);
        }
        listPreference.setEntries(certs.toArray(new String[certs.size()]));
        listPreference.setEntryValues(entryValues);
    }

    private String findValueofIndex(int index, ListPreference listPreference){
        String value = new String();
        String[] values = (String[]) listPreference.getEntries();
        if(index < values.length)
            value = values[index];
        return value;
    }

    private int findIndexByValue(ListPreference listPreference, String value){
        String[] values = (String[]) listPreference.getEntries();
        for(int i = 0; i < values.length; i++ ){
            if (value.equals(values[i])) {
                return i;
            }
        }
        return 0;
	}

	private void updateListPreferencesValues(){
		mSecurity.setValueIndex(mSecurityIndex);
		mSecurity.setSummary(findValueofIndex(mSecurityIndex, R.array.wifi_security));
		
		mEapMethodList.setValueIndex(mEapMethodListIndex);
		mEapMethodList.setSummary(findValueofIndex(mEapMethodListIndex, R.array.wifi_eap_method));
		
		mEapPhase2List.setValueIndex(mEapPhase2ListIndex);
		mEapPhase2List.setSummary(findValueofIndex(mEapPhase2ListIndex, R.array.wifi_phase2_entries));
		
		mEapCACertList.setValueIndex(mEapCACertListIndex);
        mEapCACertList.setSummary(findValueofIndex(mEapCACertListIndex, mEapCACertList));
		
		mEapUserCertList.setValueIndex(mEapUserCertListIndex);
        mEapUserCertList.setSummary(findValueofIndex(mEapUserCertListIndex, mEapUserCertList));
		
		mProxyList.setValueIndex(mProxyListIndex);
		mProxyList.setSummary(findValueofIndex(mProxyListIndex, R.array.wifi_proxy_settings));
		
		mIpList.setValueIndex(mIpListIndex);
		mIpList.setSummary(findValueofIndex(mIpListIndex, R.array.wifi_ip_settings));
	}
	
	private void setPreferencesValues(){
		mSsid.setText(mAccessPoint.getSsidStr());
//		mSsid.setSummary(checkNull(mAccessPoint.getSsidStr()));
		
		int security = mAccessPoint.getSecurity();
		mSecurityIndex = checkSecurityIndex(security);
		mSecurity.setEnabled(false);
		if (!mAccessPoint.isSaved()) {
			//connect network
			findPreference(KEY_SSID_CATEGORY).setVisible(false);
			mSsid.setVisible(false);
			
//			mSecurity.setEnabled(false);  //cannot change security
		} else{
			//modify network
			findPreference(KEY_SSID_CATEGORY).setVisible(false);
			mSsid.setVisible(false);
			
			WifiConfiguration config = mAccessPoint.getConfig();
			if(config == null)  return;
				
			switch(security) {
	            case AccessPoint.SECURITY_WEP:
//	            	mPassword.setText(config.wepKeys[0]);
//	            	mPassword.setSummary(checkNull(config.wepKeys[0]));
//	            	mPassword.setSummary(R.string.wifi_unchanged);
	            	break;
	            case AccessPoint.SECURITY_PSK:
//	            	mPassword.setText(config.preSharedKey);
//	            	mPassword.setSummary(checkNull(config.preSharedKey));
//	            	mPassword.setSummary(R.string.wifi_unchanged);
	            	break;
	            case AccessPoint.SECURITY_EAP:
//	            	mPassword.setText(config.enterpriseConfig.getPassword());
//	            	mPassword.setSummary(checkNull(config.enterpriseConfig.getPassword()));
//	            	mPassword.setSummary(R.string.wifi_unchanged);

	            	mEapMethodListIndex = checkEapMethodIndex(config.enterpriseConfig.getEapMethod());
                    mEapPhase2ListIndex = config.enterpriseConfig.getPhase2Method();

                    String eapCACertString;
                    if (!TextUtils.isEmpty(config.enterpriseConfig.getCaPath())) {
                        eapCACertString = mUseSystemCertsString;
                    } else {
                        String[] caCerts = config.enterpriseConfig.getCaCertificateAliases();
                        if (caCerts == null) {
                            eapCACertString = mDoNotValidateEapServerString;
                        } else if (caCerts.length == 1) {
                            eapCACertString = caCerts[0];
                        } else {
                            // Reload the cert spinner with an extra "multiple certificates added" item.
                        loadCertificates(
                                mEapCACertList,
                                Credentials.CA_CERTIFICATE,
                                mDoNotValidateEapServerString,
                                true,
                                true);
                        eapCACertString = mMultipleCertSetString;
                        }
                    }
                    mEapCACertListIndex = findIndexByValue(mEapCACertList, eapCACertString);

                    String userCert = config.enterpriseConfig.getClientCertificateAlias();
                    if (TextUtils.isEmpty(userCert))
                        userCert = mDoNotProvideEapUserCertString;
                    mEapUserCertListIndex = findIndexByValue(mEapUserCertList, userCert);

	        		mEapDomainEdit.setText(config.enterpriseConfig.getDomainSuffixMatch());
//	        		mEapDomainEdit.setSummary(checkNull(config.enterpriseConfig.getDomainSuffixMatch()));
	        		
	        		mEapIdentityEdit.setText(config.enterpriseConfig.getIdentity());
//	        		mEapIdentityEdit.setSummary(checkNull(config.enterpriseConfig.getIdentity()));
	        		
	        		mEapAnonymousEdit.setText(config.enterpriseConfig.getAnonymousIdentity());
//	        		mEapAnonymousEdit.setSummary(checkNull(config.enterpriseConfig.getAnonymousIdentity()));
	            	break;
	            default:
	            	break;
			}
			
			mIpAssignment = config.getIpAssignment();
			if (mIpAssignment == IpAssignment.STATIC) {
				mIpListIndex = STATIC_IP;
				
				mStaticIpConfiguration = config.getStaticIpConfiguration();
				if (mStaticIpConfiguration != null) {
                	
                    if (mStaticIpConfiguration.ipAddress != null) {
                        mIpAddressEdit.setText(mStaticIpConfiguration.ipAddress
                        		.getAddress().getHostAddress());
//                        mIpAddressEdit.setSummary(checkNull(mStaticIpConfiguration.ipAddress
//                        		.getAddress().getHostAddress()));
    	            	
                        mIpPrefixEdit.setText(Integer.toString(mStaticIpConfiguration.ipAddress
                                .getNetworkPrefixLength()));
//                        mIpPrefixEdit.setSummary(checkNull(Integer.toString(mStaticIpConfiguration.ipAddress
//                                .getNetworkPrefixLength())));
                    }

                    if (mStaticIpConfiguration.gateway != null) {
                        mIpGatewayEdit.setText(mStaticIpConfiguration.gateway.getHostAddress());
//                        mIpGatewayEdit.setSummary(checkNull(mStaticIpConfiguration.gateway.getHostAddress()));
                    }

                    if (mStaticIpConfiguration.dnsServers != null) {
	                    Iterator<InetAddress> dnsIterator = mStaticIpConfiguration.dnsServers.iterator();
	                    if (dnsIterator.hasNext()) {
	                    	InetAddress address = dnsIterator.next();
	                    	if(address != null){
	                        mIpDNS1Edit.setText(address.getHostAddress());
//	                        mIpDNS1Edit.setSummary(checkNull(address.getHostAddress()));
	                    	}
	                    }
	                    if (dnsIterator.hasNext()) {
	                    	InetAddress address = dnsIterator.next();
	                    	if(address != null){
	                        mIpDNS2Edit.setText(address.getHostAddress());
//	                        mIpDNS2Edit.setSummary(checkNull(address.getHostAddress()));
	                    	}
	                    }
                    }
                }
			}
			
			mProxySettings = config.getProxySettings();
			ProxyInfo proxyInfo = config.getHttpProxy();
			
			if (mProxySettings == ProxySettings.STATIC) {
				mProxyListIndex = PROXY_STATIC;
				
				if (proxyInfo != null) {
                    mProxyHostnameEdit.setText(proxyInfo.getHost());
//                    mProxyHostnameEdit.setSummary(checkNull(proxyInfo.getHost()));
	            	
                    mProxyPortEdit.setText(Integer.toString(proxyInfo.getPort()));
//                    mProxyPortEdit.setSummary(checkNull(Integer.toString(proxyInfo.getPort())));
	            	
                    mProxyExcluEdit.setText(proxyInfo.getExclusionListAsString());
//                    mProxyExcluEdit.setSummary(checkNull(proxyInfo.getExclusionListAsString()));
                }
			} else if (mProxySettings == ProxySettings.PAC) {
				mProxyListIndex = PROXY_PAC;
				
				if (proxyInfo != null) {
                    mProxyPacEdit.setText(proxyInfo.getPacFileUrl().toString());
//                    mProxyPacEdit.setSummary(checkNull(proxyInfo.getPacFileUrl().toString()));
                }
			}
		}

        mEnableAdvanced.setChecked(mProxyListIndex != PROXY_NONE || mIpListIndex != DHCP);
		updateListPreferencesValues();
		
		validateSecurityFields();
		validateProxyFields();
		validateIpFields();
	}
	
	private int checkSecurityIndex(int index){
		String[] values = getResources().getStringArray(R.array.wifi_security);
		if(index < 0){
			return AccessPoint.SECURITY_NONE;
		} else if(index >= values.length){
			return AccessPoint.SECURITY_PSK;
		}else
			return index;
	}
	
	private int checkEapMethodIndex(int index){
		String[] values = getResources().getStringArray(R.array.wifi_eap_method);
		if(index < 0 || index >= values.length){
			return Eap.SIM;
		}else
			return index;
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (mAccessPoint != null) {
        	getActivity().setTitle(mAccessPoint.getSsidStr());
        }
        initFooterButtonBar();
        enableSubmitIfAppropriate();
	}
	
	private void initFooterButtonBar(){
		final Activity activity = getActivity();
		View footerButtonBar = activity.findViewById(R.id.footer_button_bar);
		if(footerButtonBar != null) {
			footerButtonBar.setVisibility(View.VISIBLE);
			
			View footerLeftButton = activity.findViewById(R.id.footerLeftButton);
			footerLeftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	finish();
                }
             });
			mFooterRightButton = (Button) activity.findViewById(R.id.footerRightButton);
			mFooterRightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	onSave();
                	finish();
                }
             });
			
			if((mMode == WifiConfigUiBase.MODE_CONNECT) && (mAccessPoint != null))
				mFooterRightButton.setText(R.string.wifi_connect);
		}
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
	
	@Override
    public void onResume() {
        super.onResume();
        
        if(mPassword != null && mPassword.isVisible())
        	mPassword.setFocus(true);
	}
	
	@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }
	
	@Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();

    }
	
	@Override
    public boolean onPreferenceTreeClick(Preference preference) {
    	
    	return super.onPreferenceTreeClick(preference);
	}
	
	public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        if (KEY_SECURITY.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
            mSecurity.setSummary(findValueofIndex(index, R.array.wifi_security));
            mSecurity.setValueIndex(index);
            
            mSecurityIndex = index;
            validateSecurityFields();
        } else if (KEY_SECURITY_EAP_METHOD.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mEapMethodList.setSummary(findValueofIndex(index, R.array.wifi_eap_method));
        	mEapMethodList.setValueIndex(index);
        	
            mEapMethodListIndex = index;
            //need validate
            validateSecurityFields();
        } else if (KEY_SECURITY_EAP_PHASE2.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mEapPhase2List.setSummary(findValueofIndex(index, R.array.wifi_phase2_entries));
        	mEapPhase2List.setValueIndex(index);
        	
        	mEapPhase2ListIndex = index;
        	//no need validate
        	mEapPhase2List.setValueIndex(index);
        } else if (KEY_SECURITY_EAP_CA_CERT.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
            mEapCACertList.setSummary(findValueofIndex(index, mEapCACertList));
        	mEapCACertList.setValueIndex(index);
        	
        	mEapCACertListIndex = index;
            validateSecurityFields();
        } else if (KEY_SECURITY_EAP_USER_CERT.equals(key)) {
            int index = Integer.parseInt((String) newValue);
            mEapUserCertList.setSummary(findValueofIndex(index, mEapUserCertList));
            mEapUserCertList.setValueIndex(index);

            mEapUserCertListIndex = index;
            //no need validate
        } else if (KEY_PROXY_SETTINGS.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mProxyList.setSummary(findValueofIndex(index, R.array.wifi_proxy_settings));
        	mProxyList.setValueIndex(index);
        	
        	mProxyListIndex = index;
        	validateProxyFields();
        } else if (KEY_IP_SETTINGS.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mIpList.setSummary(findValueofIndex(index, R.array.wifi_ip_settings));
        	mIpList.setValueIndex(index);
        	
        	mIpListIndex = index;
        	validateIpFields();
        } else if (preference instanceof EditTextPreference) {
//        	((EditTextPreference) preference).setText(String.valueOf(newValue));
//        	
//        	if(KEY_PASSWORD.equals(key)){
//        		preference.setSummary("*");
//        	}else {
//        		preference.setSummary(checkNull(String.valueOf(newValue)));
//        	}
        	
        	if (KEY_NETWORK_PREFIX.equals(key)) {
                try {
                	int networkPrefixLength = Integer.parseInt((String) newValue);
                    if (networkPrefixLength > -1 && networkPrefixLength < 33) {
                    	mIpGatewayEdit.setText("");
                    }
                } catch (NumberFormatException e) {
                }
        	}
        }
        
        enableSubmitIfAppropriate();

        return true;
    }
	
	private String findValueofIndex(int index, int arrayId){
		String value = new String();
		String[] values = getResources().getStringArray(arrayId);
		if(index < values.length)
			value = values[index];
    	return value;
	}
	
	private void validateSecurityFields() {
		mEapMethodList.setVisible(false);
		mEapPhase2List.setVisible(false);
    	mEapCACertList.setVisible(false);
    	mEapDomainEdit.setVisible(false);
    	mEapUserCertList.setVisible(false);
    	mEapIdentityEdit.setVisible(false);
    	mEapAnonymousEdit.setVisible(false);
    	mPassword.setVisible(false);
    	
		switch (mSecurityIndex){
	        case AccessPoint.SECURITY_NONE:
	        	break;
	        case AccessPoint.SECURITY_WEP:
	        case AccessPoint.SECURITY_PSK:
	        	mPassword.setVisible(true);
	        	break;
	        default:
	        	validateEapMethodFields();
	        	break;
		}

        setPreferenceGroupChildrenLayout(mSecurityCategory);
	}
	
	private void validateEapMethodFields() {
		mEapMethodList.setVisible(true);
		mEapMethodList.setValueIndex(mEapMethodListIndex);

		switch (mEapMethodListIndex){
		    case WIFI_EAP_METHOD_PEAP:
		    	mEapPhase2List.setVisible(true);
		    	mEapPhase2List.setValueIndex(mEapPhase2ListIndex);
		    	
            	mEapCACertList.setVisible(true);
            	mEapCACertList.setValueIndex(mEapCACertListIndex);
                if(mEapCACertListIndex > 1){
            		mEapDomainEdit.setVisible(true);
            	}
            	mEapIdentityEdit.setVisible(true);
            	mEapAnonymousEdit.setVisible(true);
            	mPassword.setVisible(true);
		    	break;
		    case WIFI_EAP_METHOD_TLS:
		    	mEapCACertList.setVisible(true);
            	mEapCACertList.setValueIndex(mEapCACertListIndex);
                if(mEapCACertListIndex > 1){
            		mEapDomainEdit.setVisible(true);
            	}
            	mEapUserCertList.setVisible(true);
            	mEapUserCertList.setValueIndex(mEapUserCertListIndex);
            	mEapIdentityEdit.setVisible(true);
		    	break;
		    case WIFI_EAP_METHOD_TTLS:
		    	mEapPhase2List.setVisible(true);
		    	mEapPhase2List.setValueIndex(mEapPhase2ListIndex);
		    	
            	mEapCACertList.setVisible(true);
            	mEapCACertList.setValueIndex(mEapCACertListIndex);
                if(mEapCACertListIndex > 1){
            		mEapDomainEdit.setVisible(true);
            	}
//            	mEapUserCertList.setVisible(false);
            	mEapIdentityEdit.setVisible(true);
            	mEapAnonymousEdit.setVisible(true);
            	mPassword.setVisible(true);
		    	break;
		    case WIFI_EAP_METHOD_PWD:
            	mEapIdentityEdit.setVisible(true);
            	mPassword.setVisible(true);
		    	break;
		    case WIFI_EAP_METHOD_SIM:
            case WIFI_EAP_METHOD_AKA:
            case WIFI_EAP_METHOD_AKA_PRIME:
		    	break;
		}
	}
	
	private void validateProxyFields() {
		mProxyPacEdit.setVisible(false);
		mProxyHostnameEdit.setVisible(false);
		mProxyPortEdit.setVisible(false);
		mProxyExcluEdit.setVisible(false);
    	
		switch (mProxyListIndex){
	        case PROXY_STATIC:
	        	mProxyHostnameEdit.setVisible(true);
	    		mProxyPortEdit.setVisible(true);
	    		mProxyExcluEdit.setVisible(true);
	        	break;
	        case PROXY_PAC:
	        	mProxyPacEdit.setVisible(true);
	        	break;
	        default:
	        	break;
		}
	}
	
	private void validateIpFields() {
		boolean visible = (mIpListIndex == STATIC_IP);
		mIpAddressEdit.setVisible(visible);
		mIpGatewayEdit.setVisible(visible);
		mIpPrefixEdit.setVisible(visible);
		mIpDNS1Edit.setVisible(visible);
		mIpDNS2Edit.setVisible(visible);
		
		mIpList.setLayoutResource(visible ? R.layout.cnasusres_preference_parent : R.layout.cnasusres_preference_parent_nodivider);
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
	
	private WifiConfiguration getConfig() {
        if (mMode == WifiConfigUiBase.MODE_VIEW) {
            return null;
        }

        WifiConfiguration config = new WifiConfiguration();

        if (mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(getText(mSsid).trim());
            // If the user adds a network manually, assume that it is hidden.
			Log.d("Dave", "getConfig, maccess point is null");
            config.hiddenSSID = true;
        } else if (!mAccessPoint.isSaved()) {
            config.SSID = AccessPoint.convertToQuotedString(mAccessPoint.getSsidStr());
			if(mAccessPoint.getConfig() == null)
				config.hiddenSSID = true;
			else
				config.hiddenSSID = mAccessPoint.getConfig().hiddenSSID;
        } else {
            config.networkId = mAccessPoint.getConfig().networkId;
			if(mAccessPoint.getConfig() == null)
				config.hiddenSSID = true;
			else
				config.hiddenSSID = mAccessPoint.getConfig().hiddenSSID;
        }

        config.shared = true;

        switch (mSecurityIndex) {
            case AccessPoint.SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;

            case AccessPoint.SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                
                String password = getText(mPassword);
                int length = password.length();
                if (length != 0) {
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58)
                            && password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                
                String passwordPSK = getText(mPassword);
                if (passwordPSK.length() != 0) {
                    if (passwordPSK.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = passwordPSK;
                    } else {
                        config.preSharedKey = '"' + passwordPSK + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_EAP:
                config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                
                int eapMethod = mEapMethodListIndex;
                int phase2Method = mEapPhase2ListIndex;
                config.enterpriseConfig.setEapMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.PEAP:
                        // PEAP supports limited phase2 values
                        // Map the index from the mPhase2PeapAdapter to the one used
                        // by the API which has the full list of PEAP methods.
                        switch(phase2Method) {
                            case WIFI_PEAP_PHASE2_NONE:
                                config.enterpriseConfig.setPhase2Method(Phase2.NONE);
                                break;
                            case WIFI_PEAP_PHASE2_MSCHAPV2:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAPV2);
                                break;
                            case WIFI_PEAP_PHASE2_GTC:
                                config.enterpriseConfig.setPhase2Method(Phase2.GTC);
                                break;
                            default:
                                Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    default:
                        // The default index from mPhase2FullAdapter maps to the API
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }

                config.enterpriseConfig.setCaCertificateAliases(null);
                config.enterpriseConfig.setCaPath(null);
                config.enterpriseConfig.setDomainSuffixMatch(getText(mEapDomainEdit));

                String caCert = findValueofIndex(mEapCACertListIndex, mEapCACertList);
                if (caCert.equals(mUnspecifiedCertString)
                        || caCert.equals(mDoNotValidateEapServerString)) {
                    // ca_cert already set to null, so do nothing.
                } else if (caCert.equals(mUseSystemCertsString)) {
                    config.enterpriseConfig.setCaPath(SYSTEM_CA_STORE_PATH);
                } else if (caCert.equals(mMultipleCertSetString)) {
                    if (mAccessPoint != null) {
                        if (!mAccessPoint.isSaved()) {
                            Log.e(TAG, "Multiple certs can only be set when editing saved network");
                        }else{
                            config.enterpriseConfig.setCaCertificateAliases(
                                    mAccessPoint.getConfig().enterpriseConfig.getCaCertificateAliases());
                        }
                    }
                } else {
                    config.enterpriseConfig.setCaCertificateAliases(new String[] {caCert});
                }

                // ca_cert or ca_path should not both be non-null, since we only intend to let
                // the use either their own certificate, or the system certificates, not both.
                // The variable that is not used must explicitly be set to null, so that a
                // previously-set value on a saved configuration will be erased on an update.
                if (config.enterpriseConfig.getCaCertificateAliases() != null
                        && config.enterpriseConfig.getCaPath() != null) {
                    Log.d(TAG, "ca_cert ("
                            + config.enterpriseConfig.getCaCertificateAliases()
                            + ") and ca_path ("
                            + config.enterpriseConfig.getCaPath()
                            + ") should not both be non-null");
                }


                String clientCert = findValueofIndex(mEapUserCertListIndex, mEapUserCertList);
                if (clientCert.equals(mUnspecifiedCertString)
                        || clientCert.equals(mDoNotProvideEapUserCertString)) {
                    // Note: |clientCert| should not be able to take the value |unspecifiedCert|,
                    // since we prevent such configurations from being saved.
                    clientCert = "";
                }
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                if (eapMethod == Eap.SIM || eapMethod == Eap.AKA || eapMethod == Eap.AKA_PRIME) {
                    config.enterpriseConfig.setIdentity("");
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else if (eapMethod == Eap.PWD) {
                    config.enterpriseConfig.setIdentity(getText(mEapIdentityEdit));
                    config.enterpriseConfig.setAnonymousIdentity("");
                } else {
                    config.enterpriseConfig.setIdentity(getText(mEapIdentityEdit));
                    config.enterpriseConfig.setAnonymousIdentity(getText(mEapAnonymousEdit));
                }

                if (mPassword.isVisible()) {
                    // For security reasons, a previous password is not displayed to user.
                    // Update only if it has been changed.
                    if (!TextUtils.isEmpty(getText(mPassword))) {
                        config.enterpriseConfig.setPassword(getText(mPassword));
                    }
                } else {
                    // clear password
                    config.enterpriseConfig.setPassword(getText(mPassword));
                }
                break;
            default:
                return null;
        }

        config.setIpConfiguration(new IpConfiguration(mIpAssignment, mProxySettings,
                mStaticIpConfiguration, mHttpProxy));

        return config;
    }
	
	void enableSubmitIfAppropriate() {
        if (mFooterRightButton == null) return;

        mFooterRightButton.setEnabled(isSubmittable());
    }
	
	private boolean isSubmittable() {
        boolean enabled = false;
        boolean passwordInvalid = false;
        if (mPassword != null
                && ((mSecurityIndex == AccessPoint.SECURITY_WEP && getText(mPassword).length() == 0)
                    || (mSecurityIndex == AccessPoint.SECURITY_PSK
                           && getText(mPassword).length() < 8))) {
            passwordInvalid = true;
        }
        
        if(getText(mSsid).length() == 0 || ((mAccessPoint == null || !mAccessPoint.isSaved()) && passwordInvalid)) {
            enabled = false;
            
//            if(mSecurityIndex == AccessPoint.SECURITY_PSK && getText(mPassword).length() < 8)
//            	mPassword.setSummary(R.string.wifi_ip_settings_invalid_password_8);//
        } else {
        	enabled = ipAndProxyFieldsAreValid();
        }

        if(mEapCACertList.isVisible() && (mEapCACertListIndex == 0))
            enabled = false;
        if(mEapUserCertList.isVisible() && (mEapUserCertListIndex == 0))
            enabled = false;
        if(mEapDomainEdit.isVisible()&& (mEapCACertListIndex == 2) && TextUtils.isEmpty(getText(mEapDomainEdit)))
            enabled = false;

        return enabled;
	}
	
	private boolean ipAndProxyFieldsAreValid() {
		mIpAssignment = IpAssignment.DHCP;
		mStaticIpConfiguration = null;
		mProxySettings = ProxySettings.NONE;
        mHttpProxy = null;
        
		if (mIpListIndex == STATIC_IP) {
			mIpAssignment = IpAssignment.STATIC;
            mStaticIpConfiguration = new StaticIpConfiguration();
            int result = validateIpConfigFields(mStaticIpConfiguration);
            if (result != 0) {
                return false;
            }
        }
        
        if (mProxyListIndex == PROXY_STATIC) {
            mProxySettings = ProxySettings.STATIC;
            String host = getText(mProxyHostnameEdit);
            String portStr = getText(mProxyPortEdit);
            String exclusionList = getText(mProxyExcluEdit);
            int port = 0;
            int result = 0;
            try {
                port = Integer.parseInt(portStr);
                result = ProxySelector.validate(host, portStr, exclusionList);
//                setProxyWarning(result);//
            } catch (NumberFormatException e) {
                result = R.string.proxy_error_invalid_port;
//                mProxyPortEdit.setSummary(result);//
            }
            if (result == 0) {
                mHttpProxy = new ProxyInfo(host, port, exclusionList);
            } else {
                return false;
            }
        } else if (mProxyListIndex == PROXY_PAC) {
            mProxySettings = ProxySettings.PAC;
            String uriSequence = getText(mProxyPacEdit);
            if (TextUtils.isEmpty(uriSequence)) {
                return false;
            }
            Uri uri = Uri.parse(uriSequence);
            if (uri == null) {
//            	mProxyPacEdit.setSummary(R.string.proxy_error_invalid_url);//
                return false;
            }
            mHttpProxy = new ProxyInfo(uri);
        }
        return true;
	}
	
//	private void setProxyWarning(int result) { //
//		switch (result){
//		    case R.string.proxy_error_invalid_host:
//		    	mProxyHostnameEdit.setSummary(result);
//		    	break;
//		    case R.string.proxy_error_invalid_port:
//		    	mProxyPortEdit.setSummary(result);
//		    	break;
//		    case R.string.proxy_error_invalid_exclusion_list:
//		    	mProxyExcluEdit.setSummary(result);
//		    	break;
//		    default:
//                break;
//		}
//    }
	
	private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException | ClassCastException e) {
            return null;
        }
    }
	
	private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        String ipAddr = getText(mIpAddressEdit);
        if (TextUtils.isEmpty(ipAddr))
        	return R.string.wifi_ip_settings_invalid_ip_address;

        Inet4Address inetAddr = getIPv4Address(ipAddr);
        if (inetAddr == null || inetAddr.equals(Inet4Address.ANY)) {
//        	mIpAddressEdit.setSummary(R.string.wifi_ip_settings_invalid_ip_address);//
            return R.string.wifi_ip_settings_invalid_ip_address;
        }

        int networkPrefixLength = -1;
        try {
            networkPrefixLength = Integer.parseInt(getText(mIpPrefixEdit));
            if (networkPrefixLength < 0 || networkPrefixLength > 32) {
//            	mIpPrefixEdit.setSummary(R.string.wifi_ip_settings_invalid_network_prefix_length);//
                return R.string.wifi_ip_settings_invalid_network_prefix_length;
            }
            
            staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
        } catch (NumberFormatException e) {
            // Set the hint as default after user types in ip address
//        	mIpPrefixEdit.setSummary(R.string.wifi_ip_settings_invalid_network_prefix_length);//
        } catch (IllegalArgumentException e) {
//        	mIpAddressEdit.setSummary(R.string.wifi_ip_settings_invalid_ip_address);//
            return R.string.wifi_ip_settings_invalid_ip_address;
        }

        String gateway = getText(mIpGatewayEdit);
        if (TextUtils.isEmpty(gateway)) {
            try {
                //Extract a default gateway from IP address
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length - 1] = 1;
                mIpGatewayEdit.setText(InetAddress.getByAddress(addr).getHostAddress());
//                mIpGatewayEdit.setSummary(checkNull(InetAddress.getByAddress(addr).getHostAddress()));
            } catch (RuntimeException ee) {
            } catch (java.net.UnknownHostException u) {
            }
        } else {
            InetAddress gatewayAddr = getIPv4Address(gateway);
            if (gatewayAddr == null) {
//            	mIpGatewayEdit.setSummary(R.string.wifi_ip_settings_invalid_gateway);//
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            if (gatewayAddr.isMulticastAddress()) {
//            	mIpGatewayEdit.setSummary(R.string.wifi_ip_settings_invalid_gateway);//
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            staticIpConfiguration.gateway = gatewayAddr;
        }

        String dns = getText(mIpDNS1Edit);
        InetAddress dnsAddr = null;

        if (TextUtils.isEmpty(dns)) {
            //If everything else is valid, provide hint as a default option
            //mDns1View.setText(mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
            return R.string.wifi_ip_settings_invalid_dns;
        } else {
            dnsAddr = getIPv4Address(dns);
            if (dnsAddr == null) {
//            	mIpDNS1Edit.setSummary(R.string.wifi_ip_settings_invalid_dns);//
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(dnsAddr);
        }

        dns = getText(mIpDNS2Edit);
        if (dns.length() > 0) {
            dnsAddr = getIPv4Address(dns);
            if (dnsAddr == null) {
//            	mIpDNS2Edit.setSummary(R.string.wifi_ip_settings_invalid_dns);//
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(dnsAddr);
        }
        return 0;
    }

	private void onSave(){
		Log.d("timhu", "onSave");
		final WifiConfiguration config = getConfig();
		if (config == null) {
			if (mAccessPoint != null && mAccessPoint.isSaved()) {
                connect(mAccessPoint.getConfig());
            }
		} else{
			if (mWifiManager != null) {
				mWifiManager.save(config, new WifiManager.ActionListener() {
                    @Override
                    public void onSuccess() {
						Log.d("Dave", "onSave Success. config is hidden = " + config.hiddenSSID);
						//WifiConfigManager.updateNetwork(mWifiManager, config);
                    }
                    @Override
                    public void onFailure(int reason) {
                        Activity activity = getActivity();
                        if (activity != null) {
                            Toast.makeText(activity,
                                R.string.wifi_failed_save_message,
                                Toast.LENGTH_SHORT).show();
                        }
                    }
                });
	        }
			
			if(mMode == WifiConfigUiBase.MODE_CONNECT){
				Log.d("timhu", "onConnect");
				if (mAccessPoint != null) { // Not "Add network"
                    connect(config);
                }
			}
		}
	}
	
	protected void connect(final WifiConfiguration config) {
		MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_CONNECT);
        mWifiManager.connect(config, new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
            }
            @Override
            public void onFailure(int reason) {
                Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity,
                         R.string.wifi_failed_connect_message,
                         Toast.LENGTH_SHORT).show();
                }
            }
        });
	}
	
    private void setPreferenceGroupChildrenLayout(PreferenceGroup group){
        if(group == null) return;

        ArrayList<Preference> visiblePreferences = new ArrayList<Preference>();
        int countAll = group.getPreferenceCount();
        for(int i = 0; i < countAll; i++){
            Preference p = group.getPreference(i);
            if(p.isVisible())
                visiblePreferences.add(p);
        }

        int countVisible = visiblePreferences.size();
        for(int i = 0; i < countVisible; i++){
            Preference p = visiblePreferences.get(i);
            boolean hasNext = (i != (countVisible-1));

            if(p instanceof ListPreference){
                p.setLayoutResource(hasNext ?
                        R.layout.cnasusres_preference_parent : R.layout.cnasusres_preference_parent_nodivider);
            } else if(p instanceof EditTextPreference){
                p.setLayoutResource(hasNext ?
                        R.layout.asusres_preference_wifi_edit_text : R.layout.asusres_preference_wifi_edit_text_nodivider);
            } else{
                p.setLayoutResource(hasNext ?
                        R.layout.asusres_preference_material : R.layout.asusres_preference_material_nodivider);
            }
        }
    }
}
