package com.android.settings.wifi;

import android.net.ConnectivityManager;
import android.net.IpConfiguration;
import android.net.LinkProperties;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.NetworkInfo.DetailedState;
import android.net.ProxyInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import android.app.Dialog;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.Display;
import android.widget.TextView;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.text.TextUtils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Iterator;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settingslib.wifi.AccessPoint;
import com.asus.cncommonres.AsusButtonBar;

/**
 * should <b>setSelectedAccessPoint</b> before create
 * @see #setSelectedAccessPoint(AccessPoint accessPoint)
 */
public class WifiViewSettings extends SettingsPreferenceFragment {
	
	private static final String TAG = "WifiViewSettings";

	private static final String KEY_SHARE_NETWORK = "share_network";
    
    private static AccessPoint mAccessPoint = null;
    
	private WifiConfiguration mWifiConfig;
	private WifiManager mWifiManager;
	
    
    private String[] mLevels;

    //mMode == WifiConfigUiBase.MODE_VIEW
    public static void setSelectedAccessPoint(AccessPoint accessPoint){
		mAccessPoint = accessPoint;
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
        addPreferencesFromResource(R.xml.wifi_view_settings);
        
        mLevels = getResources().getStringArray(R.array.wifi_signal);
        mWifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
        initPreferences();
        
        if (mAccessPoint == null) {
        	finish();
        } else{
        	getActivity().setTitle(mAccessPoint.getSsidStr());

        	setPreferencesValues();
        }
	}
	
	private void initPreferences(){
//        PreferenceGroup ssidCategory = (PreferenceGroup) findPreference(KEY_SSID_CATEGORY);
	}
	
	private Preference addPreference(PreferenceGroup group, int titleRes, String summary) {
		Preference preference = new Preference(getPrefContext());
		preference.setTitle(titleRes);
		if(summary == null){
			preference.setKey(KEY_SHARE_NETWORK);
//			preference.setIcon(R.drawable.asus_settings_ic_qr_code);
			preference.setLayoutResource(R.layout.asusres_preference_wifi_view_item_qrcode);
		} else{
			preference.setSummary(summary);
			preference.setLayoutResource(R.layout.asusres_preference_wifi_view_item);
		}
        
        group.addPreference(preference);
        return preference;
    }

    private String getSignalString() {
        int level = mAccessPoint.getLevel();
        if (level > 0) { level -= 1; }
        return (level > -1 && level < mLevels.length) ? mLevels[level] : null;
    }
    
	private void setPreferencesValues(){
		final Activity activity = getActivity();
		final Resources res = getResources();
		
		PreferenceGroup group = getPreferenceScreen();
		Preference ipPreference = null;
		Preference qrPreference = null;
		
		final DetailedState state = mAccessPoint.getDetailedState();
        final String signalLevel = getSignalString();

        if ((state == null || state == DetailedState.DISCONNECTED) && signalLevel != null) {
        	Log.d("timhu", "DISCONNECTED state = " + state);
        	if (state == DetailedState.DISCONNECTED) {
        		String summary = AccessPoint.getSummary(activity, state, false);
        		addPreference(group, R.string.wifi_status, summary);
        	} else{
        		addPreference(group, R.string.wifi_status, res.getString(R.string.wifi_disconnect));
        	}
        	
        	addPreference(group, R.string.wifi_security, mAccessPoint.getSecurityString(false));
        	addPreference(group, R.string.wifi_signal, signalLevel);
        } else {
        	Log.d("timhu", "state = " + state);
            if (state != null) {
                boolean isEphemeral = mAccessPoint.isEphemeral();
                WifiConfiguration config = mAccessPoint.getConfig();
                String providerFriendlyName = null;
                if (config != null && config.isPasspoint()) {
                    providerFriendlyName = config.providerFriendlyName;
                }
                String summary = AccessPoint.getSummary(activity, state, isEphemeral, providerFriendlyName);
                addPreference(group, R.string.wifi_status, summary);
            }
            
            addPreference(group, R.string.wifi_security, mAccessPoint.getSecurityString(false));
            if (signalLevel != null) {
            	addPreference(group, R.string.wifi_signal, signalLevel);
            }

            WifiInfo info = mAccessPoint.getInfo();
            if (info != null && info.getLinkSpeed() != -1) {
            	addPreference(group, R.string.wifi_speed, String.format(
            			res.getString(R.string.link_speed), info.getLinkSpeed()));
            }

            if (info != null && info.getFrequency() != -1) {
                final int frequency = info.getFrequency();
                String band = null;

                if (frequency >= AccessPoint.LOWER_FREQ_24GHZ
                        && frequency < AccessPoint.HIGHER_FREQ_24GHZ) {
                    band = res.getString(R.string.wifi_band_24ghz);
                } else if (frequency >= AccessPoint.LOWER_FREQ_5GHZ
                        && frequency < AccessPoint.HIGHER_FREQ_5GHZ) {
                    band = res.getString(R.string.wifi_band_5ghz);
                } else {
                    Log.e(TAG, "Unexpected frequency " + frequency);
                }
                if (band != null) {
                	addPreference(group, R.string.wifi_frequency, band);
                }
            }

            if (mAccessPoint.isActive()) {
            	ipPreference = addPreference(group, R.string.wifi_ip_address, res.getString(R.string.status_unavailable));
            }
            
            if(mAccessPoint.isSaved() && !WifiQRCodeUtils.getApQRCodeString(mAccessPoint, mWifiManager).equals(""))
            	qrPreference = addPreference(group, R.string.wifi_qrcode_share_network, null);
        }
        
        if(qrPreference == null)
        	setLastPreferenceNoDivider(group);
        
        WifiConfiguration config = mAccessPoint.getConfig();
        if (config != null) {
        	//add ip category
        	ProxySettings proxySettings = config.getProxySettings();
    		ProxyInfo proxyInfo = config.getHttpProxy();
    		if((proxySettings == ProxySettings.STATIC) || (proxySettings == ProxySettings.PAC)){
    			PreferenceGroup proxyGroup = new PreferenceCategory(getPrefContext());
    			proxyGroup.setTitle(R.string.proxy_settings_title);
    			getPreferenceScreen().addPreference(proxyGroup);
    			
	            if (proxySettings == ProxySettings.STATIC) {
	            	if(!TextUtils.isEmpty(proxyInfo.getHost()))
	            		addPreference(proxyGroup, R.string.proxy_hostname_label, proxyInfo.getHost());
	            	if(proxyInfo.getPort() != 0)
	            		addPreference(proxyGroup, R.string.proxy_port_label, Integer.toString(proxyInfo.getPort()));
	            } else if (proxySettings == ProxySettings.PAC) {
	            	if(!TextUtils.isEmpty(proxyInfo.getPacFileUrl().toString()))
	            		addPreference(proxyGroup, R.string.proxy_url_title, proxyInfo.getPacFileUrl().toString());
	            }
	            
	            setLastPreferenceNoDivider(proxyGroup);
    		}
            
            //add proxy category
    		if(mAccessPoint.isActive() || (config.getIpAssignment() == IpAssignment.STATIC)){
    			PreferenceGroup ipv4Group = new PreferenceCategory(getPrefContext());
    			ipv4Group.setTitle(R.string.wifi_ip_settings);
    			getPreferenceScreen().addPreference(ipv4Group);
    			
	            if(mAccessPoint.isActive()){
	            	addActiveIpv4CateGory(activity, ipv4Group, ipPreference);
	            }else {
	            	//+++ static ip
	            	StaticIpConfiguration mStaticIpConfiguration = config.getStaticIpConfiguration();
	            	if (mStaticIpConfiguration != null) {
	                    if (mStaticIpConfiguration.ipAddress != null) {
	                    	String ipAddr = mStaticIpConfiguration.ipAddress.getAddress().getHostAddress();
	                    	if(!TextUtils.isEmpty(ipAddr))
	    	            		addPreference(ipv4Group, R.string.wifi_ip_address, ipAddr);
	                    }
	                    if (mStaticIpConfiguration.gateway != null) {
	                    	String gateway = mStaticIpConfiguration.gateway.getHostAddress();
	                    	if(!TextUtils.isEmpty(gateway))
	    	            		addPreference(ipv4Group, R.string.wifi_gateway, gateway);
		            	}
	                    if (mStaticIpConfiguration.dnsServers != null) {
		                    Iterator<InetAddress> dnsIterator = mStaticIpConfiguration.dnsServers.iterator();
		                    if (dnsIterator.hasNext()) {
		                    	String dns = dnsIterator.next().getHostAddress();
		                    	if(!TextUtils.isEmpty(dns))
		    	            		addPreference(ipv4Group, R.string.wifi_dns1, dns);
		                    }
		                    if (dnsIterator.hasNext()) {
		                    	String dns = dnsIterator.next().getHostAddress();
		                    	if(!TextUtils.isEmpty(dns))
		    	            		addPreference(ipv4Group, R.string.wifi_dns2, dns);
		                    }
	                    }
	            	}
	            	//---
	            }
	            setLastPreferenceNoDivider(ipv4Group);
                ipv4Group.setVisible(ipv4Group.getPreferenceCount() > 0);
    		}
        }
        
	}
	
	private void addActiveIpv4CateGory(Context context, PreferenceGroup ipv4Group, Preference ipPreference){
    	String ipv4Addr = null;
    	String ipGateway = null;
    	String ipDns1 = null;
    	String ipDns2 = null;
		
    	ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        LinkProperties prop = cm.getLinkProperties(ConnectivityManager.TYPE_WIFI);
        if (prop == null) return ;
        
        Iterator<InetAddress> iter = prop.getAllAddresses().iterator();
        while (iter.hasNext()) {
        	InetAddress address = iter.next();
        	if(address instanceof Inet4Address){
        		ipv4Addr = address.getHostAddress();
        		
        		byte[] addr = address.getAddress();
                addr[addr.length - 1] = 1;
                try {
                	ipGateway = InetAddress.getByAddress(addr).getHostAddress();
                } catch (java.net.UnknownHostException u) {
                }

        	}
        }
        
        Iterator<InetAddress> dnsIter = prop.getDnsServers().iterator();
        if(dnsIter.hasNext()){
        	ipDns1 = dnsIter.next().getHostAddress();
        }
        if(dnsIter.hasNext()){
        	ipDns2 = dnsIter.next().getHostAddress();
        }
        
        if(!TextUtils.isEmpty(ipv4Addr))
    		addPreference(ipv4Group, R.string.wifi_ip_address, ipv4Addr);
        if(!TextUtils.isEmpty(ipGateway))
    		addPreference(ipv4Group, R.string.wifi_gateway, ipGateway);
        if(!TextUtils.isEmpty(ipDns1))
    		addPreference(ipv4Group, R.string.wifi_dns1, ipDns1);
        if(!TextUtils.isEmpty(ipDns2))
    		addPreference(ipv4Group, R.string.wifi_dns2, ipDns2);
        
        String ipAddr = (ipv4Addr != null) ? ipv4Addr : Utils.getWifiIpAddresses(context);
        if(!TextUtils.isEmpty(ipAddr) && (ipPreference != null))
        	ipPreference.setSummary(ipAddr);
	}
	
	private void setLastPreferenceNoDivider(PreferenceGroup group){
		int count = group.getPreferenceCount();
        if(count > 0)
        	group.getPreference(count-1).setLayoutResource(R.layout.asusres_preference_wifi_view_item_nodivider);
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        if (mAccessPoint != null) {
        	getActivity().setTitle(mAccessPoint.getSsidStr());
            initFooterButtonBar();
        }
	}
	
	private void initFooterButtonBar(){
		final SettingsActivity activity = (SettingsActivity) getActivity();
		AsusButtonBar mButtonBar = activity.getButtonBar();
		
		if(mButtonBar != null) {
			mButtonBar.setVisibility(View.VISIBLE);
			
			if(mAccessPoint.isActive()){
				//modify delete disconnect ignore
				mButtonBar.addButton(1, R.drawable.asusres_icon_modify, getString(R.string.wifi_menu_modify));
				mButtonBar.getButton(1).setOnClickListener(new View.OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                	onModify();
	                }
	             });
				
				mButtonBar.addButton(2, R.drawable.asusres_icon_delete, getString(R.string.wifi_menu_forget));
				mButtonBar.getButton(2).setOnClickListener(new View.OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                    onIgnore();
	                }
	            });
	            
//				mButtonBar.addButton(3, R.drawable.asusres_icon_disconnect, getString(R.string.wifi_disconnect));
//				mButtonBar.getButton(3).setOnClickListener(new View.OnClickListener() {
//	                @Override
//	                public void onClick(View v) {
//	                	onDisconnect(true);
//	                }
//	            });
				
			} else{
				//connect ignore
				mButtonBar.addButton(1, R.drawable.asusres_icon_connect, getString(R.string.wifi_menu_connect));
				mButtonBar.getButton(1).setOnClickListener(new View.OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                	onConnect();
	                }
	             });
				
			}
			
//			mButtonBar.addButton(4, R.drawable.asus_btn_ignore, getString(R.string.wifi_ignore));
//			mButtonBar.getButton(4).setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                	onIgnore();
//                }
//            });
        }
	}
	
	private void onModify(){
		WifiConnectSettings.setSelectedAccessPoint(mAccessPoint, WifiConfigUiBase.MODE_MODIFY);
		
        if (getActivity() instanceof SettingsActivity) {
            ((SettingsActivity) getActivity()).startPreferencePanel(
            		WifiConnectSettings.class.getCanonicalName(), null,
            		R.string.wifi_add_network, null, this, 0);
        } else {
            startFragment(this, WifiConnectSettings.class.getCanonicalName(),
            		R.string.wifi_add_network, -1 /* Do not request a results */, null);
        }
        
        finish();
	}
	
	private void onConnect(){
		WifiManager.ActionListener mConnectListener = new WifiManager.ActionListener() {
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
        };
        
		if(mWifiManager != null && (mAccessPoint.getConfig() != null)){
			mWifiManager.connect(mAccessPoint.getConfig(), mConnectListener);
			finish();
		}
	}
	private void onForget(){
		WifiManager.ActionListener mForgetListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
            	finish();
            }
            @Override
            public void onFailure(int reason) {
                Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity,
                        R.string.wifi_failed_forget_message,
                        Toast.LENGTH_SHORT).show();
                }
            }
        };
        
		if(mWifiManager != null && (mAccessPoint.getConfig() != null)){
			mWifiManager.forget(mAccessPoint.getConfig().networkId, mForgetListener);
		}
	}
	private void onDisconnect(boolean needFinish){
		if(mWifiManager != null && mAccessPoint.isActive()){
    		boolean success = mWifiManager.disconnect();
    		if(success && needFinish)
    			finish();
    	}
	}
	private void onIgnore(){
		onDisconnect(false);
		onForget();
	}

	private static final int WIFI_DIALOG_GENQRCODE_ID = 7;
	private void showQrcodeDialog(){
		if(mWifiManager == null || mAccessPoint == null)
			return ;

		showDialog(WIFI_DIALOG_GENQRCODE_ID);
		//createQRDialog().show();
	}

	@Override
	public Dialog onCreateDialog(int dialogId) {
		switch (dialogId) {
			case WIFI_DIALOG_GENQRCODE_ID:
				return createQRDialog();
		}
		return super.onCreateDialog(dialogId);
	}

	private Dialog createQRDialog()
	{
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.wifi_qrcode_dialog, null);
		dialogBuilder.setView(dialogView);

		ImageView imageView = (ImageView) dialogView.findViewById(R.id.qrcode);
		WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		int smallerDimension = width < height ? width : height;
		smallerDimension = smallerDimension * 164 / 360;

        String qrCodeString = WifiQRCodeUtils.getApQRCodeString(mAccessPoint, mWifiManager);
        if(!"".equals(qrCodeString))
            imageView.setImageBitmap(WifiQRCodeUtils.getQRCode(qrCodeString, smallerDimension, smallerDimension));

		AlertDialog alertDialog = dialogBuilder.create();
		WindowManager.LayoutParams wmlp = alertDialog.getWindow().getAttributes();
		wmlp.gravity = Gravity.BOTTOM;

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
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
		String key = preference.getKey();
		
		if(KEY_SHARE_NETWORK.equals(key)){
			showQrcodeDialog();
		}
    	return super.onPreferenceTreeClick(preference);
	}

}
