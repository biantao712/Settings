package com.android.settings.vpn2;

import android.os.Bundle;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.security.Credentials;
import android.security.KeyStore;

import android.view.View;
import android.view.Window;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.Log;
import android.widget.Toast;
import android.app.Dialog;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;
import android.view.ViewGroup;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;

import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SettingsActivity;
import com.asus.cncommonres.AsusButtonBar;

/**
 * @author Modified by Tim_Hu
 */

public class VpnViewSettings extends SettingsPreferenceFragment {
	
	private static final String TAG = "VpnViewSettings";

	private static final String ARG_PROFILE = "profile";
	private static final String ARG_EDITING = "editing";
    private static final String ARG_EXISTS = "exists";
	private static final String ARG_STATE = "state";
    
	private static final int BUTTON_ID_MODIFY = 1;
	private static final int BUTTON_ID_DELETE = 2;
	private static final int BUTTON_ID_DISCONNECT = 3;
	private static final int BUTTON_ID_CONNECT = 4;

	private static final int STATE_NONE = ManageablePreference.STATE_NONE;
	
    private VpnProfile mProfile;
    private int mState = STATE_NONE;
    
    private final IConnectivityManager mConnectivityService = IConnectivityManager.Stub.asInterface(
            ServiceManager.getService(Context.CONNECTIVITY_SERVICE));

    public static void show(VpnSettings parent, VpnProfile profile, int state) {
        if (!parent.isAdded()) return;

        Bundle args = new Bundle();
        args.putParcelable(ARG_PROFILE, profile);
        args.putInt(ARG_STATE, state);
        
        parent.showSettingsFragment(VpnViewSettings.class.getCanonicalName(), R.string.vpn_add_title, args);
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
        addPreferencesFromResource(R.xml.vpn_view_settings);
        
        final Bundle arguments = getArguments();
        if(arguments != null){
        	if (arguments.containsKey(ARG_PROFILE))
        		mProfile = arguments.getParcelable(ARG_PROFILE);
        	if (arguments.containsKey(ARG_STATE))
        		mState = arguments.getInt(ARG_STATE, STATE_NONE);
        }
        if(mProfile == null){
    		finish();
    		return;
    	}
        
        getActivity().setTitle(mProfile.name);
        
        initPreferences();
    	setPreferencesValues();
	}
	
	private void initPreferences(){
//        PreferenceGroup ssidCategory = (PreferenceGroup) findPreference(KEY_SSID_CATEGORY);
	}
    
	private void setPreferencesValues(){
		final Activity activity = getActivity();
		final Resources res = getResources();
		
		//info category
		PreferenceGroup infoGroup = new PreferenceCategory(getPrefContext());
		infoGroup.setTitle(R.string.vpn_view_category_info);
		infoGroup.setOrder(0);
		getPreferenceScreen().addPreference(infoGroup);
		
		addPreference(infoGroup, R.string.wifi_status, getStateString(mState));
		addPreference(infoGroup, R.string.vpn_name, checkNull(mProfile.name));
		addPreference(infoGroup, R.string.vpn_server, checkNull(mProfile.server));
		addPreference(infoGroup, R.string.vpn_username, checkNull(mProfile.username));
		addPreference(infoGroup, R.string.vpn_password, getPasswordString(mProfile.password));
		
		setLastPreferenceNoDivider(infoGroup);
		
		//type
		PreferenceGroup typeGroup = new PreferenceCategory(getPrefContext());
		typeGroup.setTitle(R.string.vpn_category_type);
		typeGroup.setOrder(1);
		getPreferenceScreen().addPreference(typeGroup);
		
		addPreference(typeGroup, R.string.vpn_type, findValueofIndex(mProfile.type, R.array.vpn_types));
		switch (mProfile.type){
	        case VpnProfile.TYPE_PPTP:
	        	addPreference(typeGroup, R.string.vpn_mppe, getMppeString(mProfile.mppe));
	            break;
	
	        case VpnProfile.TYPE_L2TP_IPSEC_PSK:
	        	addPreference(typeGroup, R.string.vpn_l2tp_secret, checkNull(mProfile.l2tpSecret));
	            // fall through
	        case VpnProfile.TYPE_IPSEC_XAUTH_PSK:
	        	addPreference(typeGroup, R.string.vpn_ipsec_identifier, checkNull(mProfile.ipsecIdentifier));
	    		addPreference(typeGroup, R.string.vpn_ipsec_secret, checkNull(mProfile.ipsecSecret));
	            break;
	
	        case VpnProfile.TYPE_L2TP_IPSEC_RSA:
	        	addPreference(typeGroup, R.string.vpn_l2tp_secret, checkNull(mProfile.l2tpSecret));
	            // fall through
	        case VpnProfile.TYPE_IPSEC_XAUTH_RSA:
	        	addPreference(typeGroup, R.string.vpn_ipsec_user_cert,
	    				checkNull(mProfile.ipsecUserCert, R.string.vpn_no_user_cert));
	            // fall through
	        case VpnProfile.TYPE_IPSEC_HYBRID_RSA:
	        	addPreference(typeGroup, R.string.vpn_ipsec_ca_cert,
	    				checkNull(mProfile.ipsecCaCert, R.string.vpn_no_ca_cert));
	    		addPreference(typeGroup, R.string.vpn_ipsec_server_cert,
	    				checkNull(mProfile.ipsecServerCert, R.string.vpn_no_server_cert));
	            break;
	   }
		
//		addPreference(typeGroup, R.string.vpn_mppe, getMppeString(mProfile.mppe));
//		
//		addPreference(typeGroup, R.string.vpn_l2tp_secret, checkNull(mProfile.l2tpSecret));
//		addPreference(typeGroup, R.string.vpn_ipsec_identifier, checkNull(mProfile.ipsecIdentifier));
//		addPreference(typeGroup, R.string.vpn_ipsec_secret, checkNull(mProfile.ipsecSecret));
//		
//		addPreference(typeGroup, R.string.vpn_ipsec_user_cert,
//				checkNull(mProfile.ipsecUserCert, R.string.vpn_no_user_cert));
//		addPreference(typeGroup, R.string.vpn_ipsec_ca_cert,
//				checkNull(mProfile.ipsecCaCert, R.string.vpn_no_ca_cert));
//		addPreference(typeGroup, R.string.vpn_ipsec_server_cert,
//				checkNull(mProfile.ipsecServerCert, R.string.vpn_no_server_cert));
		
		setLastPreferenceNoDivider(typeGroup);
		
		//advanced
		PreferenceGroup advancedGroup = new PreferenceCategory(getPrefContext());
		advancedGroup.setTitle(R.string.vpn_category_advanced);
		advancedGroup.setOrder(2);
		getPreferenceScreen().addPreference(advancedGroup);
		
		addPreference(advancedGroup, R.string.vpn_search_domains, checkNull(mProfile.searchDomains));
		addPreference(advancedGroup, R.string.vpn_dns_servers, checkNull(mProfile.dnsServers));
		addPreference(advancedGroup, R.string.vpn_routes, checkNull(mProfile.routes));
		
		setLastPreferenceNoDivider(advancedGroup);
		
	}
	
	private Preference addPreference(PreferenceGroup group, int titleRes, String summary) {
		Preference preference = new Preference(getPrefContext());
		preference.setTitle(titleRes);
		preference.setSummary(summary);
		preference.setLayoutResource(R.layout.asusres_preference_wifi_view_item);
        
        group.addPreference(preference);
        return preference;
    }

    private String getStateString(int state) {
        final Resources res = getContext().getResources();
        final String[] states = res.getStringArray(R.array.vpn_states);
        String summary = (state == STATE_NONE ? states[0] : states[state]);
        return summary;
    }
    
    private String getPasswordString(String value) {
    	if (value == null || value.length() == 0) {
    		return checkNull(value);
    	}
    	
    	int length = value.length();
    	char[] data = new char[length];
    	for(int i = 0; i < length; i++){
    		data[i] = '*';
    	}
    	return String.valueOf(data);
    }
    
    private String getMppeString(boolean enable) {
        return enable ? getResources().getString(R.string.vpn_view_mppe_enable)
        		: getResources().getString(R.string.vpn_view_mppe_disable);
    }
    
    private String findValueofIndex(int index, int arrayId){
		String value = new String();
		String[] values = getResources().getStringArray(arrayId);
		if(index > -1 && index < values.length)
			value = values[index];
    	return value;
	}
	
	private String checkNull(String value) {
        return checkNull(value, R.string.wifi_ap_not_set);
    }
	
	private String checkNull(String value, int defValueRes) {
        if (value == null || value.length() == 0) {
            return getResources().getString(defValueRes);
        } else {
            return value;
        }
    }
	
	private void setLastPreferenceNoDivider(PreferenceGroup group){
		int count = group.getPreferenceCount();
        if(count > 0)
        	group.getPreference(count-1).setLayoutResource(R.layout.asusres_preference_wifi_view_item_nodivider);
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        getActivity().setTitle(mProfile.name);
        initFooterButtonBar();
	}
	
	private void initFooterButtonBar(){
		final SettingsActivity activity = (SettingsActivity) getActivity();
		AsusButtonBar mButtonBar = activity.getButtonBar();
		
		if(mButtonBar != null) {
			mButtonBar.setVisibility(View.VISIBLE);
			
			mButtonBar.addButton(BUTTON_ID_MODIFY, R.drawable.asusres_icon_modify, getString(R.string.wifi_menu_modify));
			mButtonBar.getButton(BUTTON_ID_MODIFY).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	modify();
                }
             });
			
			mButtonBar.addButton(BUTTON_ID_DELETE, R.drawable.asusres_icon_delete, getString(R.string.wifi_menu_forget));
			mButtonBar.getButton(BUTTON_ID_DELETE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	showForgetConfirmDialog();
                }
             });
			
			if(mState == LegacyVpnInfo.STATE_CONNECTED){
				mButtonBar.addButton(BUTTON_ID_DISCONNECT, R.drawable.asusres_icon_break_off, getString(R.string.vpn_button_disconnect));
				mButtonBar.getButton(BUTTON_ID_DISCONNECT).setOnClickListener(new View.OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                	disconnect(mProfile);
	                }
	             });
			} else{
				mButtonBar.addButton(BUTTON_ID_CONNECT, R.drawable.asusres_icon_connect, getString(R.string.vpn_button_connect));
				mButtonBar.getButton(BUTTON_ID_CONNECT).setOnClickListener(new View.OnClickListener() {
	                @Override
	                public void onClick(View v) {
	                	try {
	                        connect(mProfile);
	                    } catch (RemoteException e) {
	                        Log.e(TAG, "Failed to connect", e);
	                    }
	                }
	             });
				
			}
        }
	}
	
	private void modify(){
		Bundle args = new Bundle();
        args.putParcelable(ARG_PROFILE, mProfile);
        args.putBoolean(ARG_EDITING, true);
        args.putBoolean(ARG_EXISTS, true);
        
        showSettingsFragment(VpnConnectSettings.class.getCanonicalName(), R.string.vpn_add_title, args);
        finish();
	}
	
	private void showSettingsFragment(String fragmentClass, int titleRes, Bundle args){
    	if (getActivity() instanceof SettingsActivity) {
            ((SettingsActivity) getActivity()).startPreferencePanel(
            		fragmentClass, args,
            		titleRes, null, this, 0);
        } else {
            startFragment(this, fragmentClass,
            		titleRes, -1 /* Do not request a results */,
            		args);
        }
    }
	
	private void connect(VpnProfile profile) throws RemoteException {
        try {
        	mConnectivityService.startLegacyVpn(profile);
        } catch (IllegalStateException e) {
            Toast.makeText(getActivity(), R.string.vpn_no_network, Toast.LENGTH_LONG).show();
        }
        
        finish();
    }

    private void disconnect(VpnProfile profile) {
        try {
            LegacyVpnInfo connected = mConnectivityService.getLegacyVpnInfo(UserHandle.myUserId());
            if (connected != null && profile.key.equals(connected.key)) {
                VpnUtils.clearLockdownVpn(getContext());
                mConnectivityService.prepareVpn(VpnConfig.LEGACY_VPN, VpnConfig.LEGACY_VPN,
                        UserHandle.myUserId());
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to disconnect", e);
        }
        
        finish();
    }
    
    private void forget(VpnProfile profile){
    	// Disable profile if connected
        disconnect(profile);

        // Delete from KeyStore
        KeyStore keyStore = KeyStore.getInstance();
        keyStore.delete(Credentials.VPN + profile.key, KeyStore.UID_SELF);
        
        // update only if lockdown vpn has been changed
        if (VpnUtils.isVpnLockdown(profile.key)) {
            VpnUtils.clearLockdownVpn(getContext());
        }
        
        finish();
    }

	private void showForgetConfirmDialog(){
		Activity activity = getActivity();
		View view1 = LayoutInflater.from(activity).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
        TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
        title.setText(activity.getResources().getString(android.R.string.dialog_alert_title));
        
        TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
        message.setText(activity.getResources().getString(R.string.vpn_delete_confirm_message));
    	
        AlertDialog dialog = new AlertDialog.Builder(activity)
        		.setView(view1)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						forget(mProfile);
						dialog.dismiss();
					}
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
                })
                .create();
        dialog.show();
        
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        dialogWindow.setAttributes(lp);
	}
	
	@Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        
        outState.putInt(ARG_STATE, mState);
        if (mProfile != null)
        	outState.putParcelable(ARG_PROFILE, mProfile);
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

}
