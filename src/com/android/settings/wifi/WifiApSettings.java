package com.android.settings.wifi;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import android.provider.Settings;
import android.util.Log;
import android.text.InputType;
import android.text.InputFilter;
import com.android.settings.util.RemoveSpecialCharFilter;
import com.android.settings.util.Utf8ByteLengthFilter;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration.KeyMgmt;

import android.support.v7.preference.Preference;
import com.android.settings.wifi.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class WifiApSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
	
	private static final String TAG = "WifiApSettings";

	private static final String KEY_SSID = "ssid";
	private static final String KEY_SECURITY = "security";
	private static final String KEY_PASSWORD = "password";
	private static final String KEY_ENABLE_ADVANCED = "enable_advanced";
	private static final String KEY_CHOOSE_CHANNEL = "choose_channel";
	private static final String KEY_DISABLE_HOTSPOT_POLICY = "disable_hotspot_policy";
	
	public static final int OPEN_INDEX = 0;
    public static final int WPA2_INDEX = 1;
    
	private WifiConfiguration mWifiConfig;
	private WifiManager mWifiManager;
	
	private int mSecurityTypeIndex = OPEN_INDEX;
	private int mHotspotDisablePolicyIndex;
	private int mBandIndex = OPEN_INDEX;

	private EditTextPreference mSsid;
	private ListPreference mSecurity;
	private EditTextPreference mPassword;
	private SwitchPreference mEnableAdvanced;
	private ListPreference mChannel;
	private ListPreference mHotspotDisablePolicy;
	
	private Button mFooterRightButton;

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
        addPreferencesFromResource(R.xml.wifi_ap_settings);
        setRetainInstance(true);

        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        mWifiConfig = mWifiManager.getWifiApConfiguration();

        mSsid = (EditTextPreference) findPreference(KEY_SSID);
        mSsid.init(InputType.TYPE_CLASS_TEXT, R.string.wifi_ap_ssid_hint, true,
                new InputFilter[] {new Utf8ByteLengthFilter(32), new RemoveSpecialCharFilter(true)});
        mSsid.setOnPreferenceChangeListener(this);
        
        mSecurity = (ListPreference) findPreference(KEY_SECURITY);
        mSecurity.setOnPreferenceChangeListener(this);
        
        mPassword = (EditTextPreference) findPreference(KEY_PASSWORD);
        mPassword.init(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
        		, R.string.wifi_ip_settings_invalid_password_8, true);
        mPassword.setOnPreferenceChangeListener(this);
        
        mEnableAdvanced = (SwitchPreference) findPreference(KEY_ENABLE_ADVANCED);
//        mEnableAdvanced.setOnPreferenceChangeListener(this);
        
        mChannel = (ListPreference) findPreference(KEY_CHOOSE_CHANNEL);
        String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.isDualBandSupported() || countryCode == null) {
            //If no country code, 5GHz AP is forbidden
            Log.i(TAG,(!mWifiManager.isDualBandSupported() ? "Device do not support 5GHz " :"")
                    + (countryCode == null ? " NO country code" :"") +  " forbid 5GHz");
            mChannel.setEntries(R.array.wifi_ap_band_config_2G_only);
            mWifiConfig.apBand = 0;
        } else {
            mChannel.setEntries(R.array.wifi_ap_band_config_full);
        }
        mChannel.setOnPreferenceChangeListener(this);
        
        mHotspotDisablePolicy = (ListPreference) findPreference(KEY_DISABLE_HOTSPOT_POLICY);
        mHotspotDisablePolicy.setOnPreferenceChangeListener(this);
        
//        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
//            getPreferenceScreen().getPreference(i).setOnPreferenceChangeListener(this);
//        }

        mHotspotDisablePolicyIndex = getHdpIndex();
      
        if (mWifiConfig != null) {
            mSsid.setText(mWifiConfig.SSID);
//            mSsid.setSummary(checkNull(mWifiConfig.SSID));
            
            mSecurityTypeIndex = getSecurityTypeIndex(mWifiConfig);
            mSecurity.setValueIndex(mSecurityTypeIndex);
            mSecurity.setSummary(findValueofIndex(mSecurityTypeIndex, R.array.wifi_ap_security));
            
            if (mSecurityTypeIndex == WPA2_INDEX) {
                mPassword.setText(mWifiConfig.preSharedKey);
//                mPassword.setSummary(checkNull(mWifiConfig.preSharedKey));
            }
            
            if (mWifiConfig.apBand == 0) {
               mBandIndex = 0;
            } else {
               mBandIndex = 1;
            }
            mChannel.setValueIndex(mBandIndex);
            mChannel.setSummary(findValueofIndex(mBandIndex, R.array.wifi_ap_band_config_full));
            
            mHotspotDisablePolicy.setValueIndex(mHotspotDisablePolicyIndex);
            mHotspotDisablePolicy.setSummary(findValueofIndex(mHotspotDisablePolicyIndex, R.array.wifi_disable_hotspot_entries));
        }
        
        if(mSecurityTypeIndex == OPEN_INDEX){
        	mPassword.setVisible(false);
            mSecurity.setLayoutResource(R.layout.asusres_preference_material_parent_nodivider);
        }
	}
	
	public static int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
            return WPA2_INDEX;
        }
        return OPEN_INDEX;
    }
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
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
                    setResult(Activity.RESULT_OK);
                	finish();
                }
             });
		}
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
	
	@Override
    public void onResume() {
        super.onResume();

        if(mPassword != null && mPassword.isVisible()){
        	mPassword.setFocus(true);
        } else if(mSsid != null && mSsid.isVisible()){
        	mSsid.setFocus(true);
        }
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
            mSecurityTypeIndex = index;
            mSecurity.setValueIndex(index);

//            String[] values = getResources().getStringArray(R.array.wifi_ap_security);
            mSecurity.setSummary(findValueofIndex(index, R.array.wifi_ap_security));
            
            boolean passwordVisible = (mSecurityTypeIndex != OPEN_INDEX);
            mPassword.setVisible(passwordVisible);
            mSecurity.setLayoutResource(passwordVisible ? R.layout.asusres_preference_material_parent
                    : R.layout.asusres_preference_material_parent_nodivider);
        } else if (KEY_CHOOSE_CHANNEL.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mBandIndex = index;
        	
        	mChannel.setValueIndex(index);

//            String[] values = getResources().getStringArray(R.array.wifi_ap_band_config_full);
            mChannel.setSummary(findValueofIndex(index, R.array.wifi_ap_band_config_full));
        } else if (KEY_DISABLE_HOTSPOT_POLICY.equals(key)) {
        	int index = Integer.parseInt((String) newValue);
        	mHotspotDisablePolicyIndex = index;
        	
        	mHotspotDisablePolicy.setValueIndex(index);
//        	String[] values = getResources().getStringArray(R.array.wifi_disable_hotspot_entries);
        	mHotspotDisablePolicy.setSummary(findValueofIndex(index, R.array.wifi_disable_hotspot_entries));
        } else if (KEY_SSID.equals(key)) {
//        	((EditTextPreference) preference).setText(String.valueOf(newValue));
//            preference.setSummary(checkNull(String.valueOf(newValue)));
        } else if (KEY_PASSWORD.equals(key)) {
//        	((EditTextPreference) preference).setText(String.valueOf(newValue));
//        	preference.setSummary(checkNull(String.valueOf(newValue)));
//        	preference.setSummary("*");
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

	private int getHdpIndex() {
		int hotspotRemainTime = Settings.System.getInt(getContentResolver(),
                Settings.System.HOTSPOT_DISABLE_POLICY,
                Settings.System.HOTSPOT_DISABLE_POLICY_DEFAULT);
		
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
        boolean enabled = false;
        boolean passwordInvalid = false;
        
        if (mPassword != null && mSecurityTypeIndex == WPA2_INDEX && getText(mPassword).length() < 8) {
        	passwordInvalid = true;
//        	mPassword.setSummary(R.string.wifi_ip_settings_invalid_password_8);//
        }
        if(getText(mSsid).trim().length() == 0 || passwordInvalid) {
            enabled = false;
        } else {
        	enabled = true;
        }

        return enabled;
	}
	
	public WifiConfiguration getConfig() {

        WifiConfiguration config = new WifiConfiguration();

        /**
         * TODO: SSID in WifiConfiguration for soft ap
         * is being stored as a raw string without quotes.
         * This is not the case on the client side. We need to
         * make things consistent and clean it up
         */
        config.SSID = getText(mSsid).trim();

        config.apBand = mBandIndex;

        switch (mSecurityTypeIndex) {
            case OPEN_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                return config;

            case WPA2_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.preSharedKey = getText(mPassword);
                return config;
        }
        return null;
    }
	
	private void onSave(){
		Log.d("timhu", "onSave");
		if (mWifiManager != null) {
            mWifiManager.setWifiApConfiguration(getConfig());
        }
		int[] hotspotDisablePolicyValues = getResources().getIntArray(R.array.wifi_disable_hotspot_values);
        Settings.System.putInt(getContentResolver(),
                Settings.System.HOTSPOT_DISABLE_POLICY,
                hotspotDisablePolicyValues[mHotspotDisablePolicyIndex]);
	}
}
