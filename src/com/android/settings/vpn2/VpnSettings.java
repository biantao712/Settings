/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.annotation.UiThread;
import android.annotation.WorkerThread;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.ConnectivityManager;
import android.net.IConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.security.Credentials;
import android.security.KeyStore;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;

import android.app.Activity;
import android.content.DialogInterface;
import android.app.Dialog;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.view.Window;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.util.ArrayUtils;
import com.android.settings.GearPreference;
import com.android.settings.GearPreference.OnGearClickListener;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.asus.cncommonres.AsusButtonBar;
import com.android.settingslib.RestrictedLockUtils;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static android.app.AppOpsManager.OP_ACTIVATE_VPN;

/**
 * Settings screen listing VPNs. Configured VPNs and networks managed by apps
 * are shown in the same list.
 */
public class VpnSettings extends RestrictedSettingsFragment implements
        Handler.Callback, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private static final String LOG_TAG = "VpnSettings";
    
    private static final String KEY_VPN_ENABLE = "vpn_enable";
    private static final String KEY_VPN_LIST = "vpn_list";
    private static final String KEY_CONNECTED_PREF = "vpn_connected_pref";
    
    private static final String KEY_SHARED_PREFERENCE_VPN_ENABLE = "shared_preferences_vpn_enable";
    private static final String KEY_SHARED_PREFERENCE_CONNECTED_VPN = "shared_preferences_connected_vpn";

    private static final int MENU_ID_MODIFY = Menu.FIRST;
    private static final int MENU_ID_DELETE = Menu.FIRST + 1;
    private static final int MENU_ID_DISCONNECT = Menu.FIRST + 2;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 3;
    
    private final int ADD_BUTTON_ID = 1;

    private static final int RESCAN_MESSAGE = 0;
    private static final int RESCAN_INTERVAL_MS = 1000 * 3;

    private static final NetworkRequest VPN_REQUEST = new NetworkRequest.Builder()
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
            .build();

    private final IConnectivityManager mConnectivityService = IConnectivityManager.Stub
            .asInterface(ServiceManager.getService(Context.CONNECTIVITY_SERVICE));
    private ConnectivityManager mConnectivityManager;
    private UserManager mUserManager;

    private final KeyStore mKeyStore = KeyStore.getInstance();

    private Map<String, LegacyVpnPreference> mLegacyVpnPreferences = new ArrayMap<>();
    private Map<AppVpnInfo, AppPreference> mAppPreferences = new ArrayMap<>();

    private Handler mUpdater;
    private LegacyVpnInfo mConnectedLegacyVpn;

    private boolean mUnavailable;
    
    private SwitchPreference mVpnEnable;
    private ManageablePreference mConnectedPreference;
    private VpnProfile mSelectedProfile;
    private String[] mVpnTypesLong;
    
    private AsusButtonBar mButtonBar;
    private View.OnClickListener mArrowListener;

    public VpnSettings() {
        super(UserManager.DISALLOW_CONFIG_VPN);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.VPN;
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mUserManager = (UserManager) getSystemService(Context.USER_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        mUnavailable = isUiRestricted();

        addPreferencesFromResource(R.xml.vpn_settings2);
        
        registerForContextMenu(getListView());
        initButtonBar();
        mVpnTypesLong = getResources().getStringArray(R.array.vpn_types_long);
        
        mVpnEnable = (SwitchPreference) getPreferenceScreen().findPreference(KEY_VPN_ENABLE);
        SharedPreferences sharedPreferences = getSharedPreferences();
    	if(sharedPreferences != null){
    		boolean vpnEnable = sharedPreferences.getBoolean(KEY_SHARED_PREFERENCE_VPN_ENABLE, false);
    		mVpnEnable.setChecked(vpnEnable);
    		changeAsusButtonBarButtonState(vpnEnable);
    	}
        mVpnEnable.setOnPreferenceChangeListener(this);
        
        PreferenceGroup vpnGroup = (PreferenceGroup) getPreferenceScreen().findPreference(KEY_VPN_LIST);
        vpnGroup.setVisible((vpnGroup.getPreferenceCount() > 0) ? true : false);
        
        mArrowListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	Object preference = v.getTag();
            	if(preference != null){
	                if (preference instanceof LegacyVpnPreference) {
	                	LegacyVpnPreference pref = (LegacyVpnPreference) preference;
	                	VpnViewSettings.show(VpnSettings.this, pref.getProfile(), pref.getState());
	                }
            	}
            }
        };
        
    }
    
    private void initButtonBar(){
		final SettingsActivity activity = (SettingsActivity) getActivity();
		mButtonBar = activity.getButtonBar();
		
		if(mButtonBar != null) {
			mButtonBar.setVisibility(View.VISIBLE);
			
			mButtonBar.addButton(ADD_BUTTON_ID, R.drawable.asusres_icon_add, getString(R.string.vpn_button_add));
			mButtonBar.getButton(ADD_BUTTON_ID).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	// Generate a new key. Here we just use the current time.
                    long millis = System.currentTimeMillis();
                    while (mLegacyVpnPreferences.containsKey(Long.toHexString(millis))) {
                        ++millis;
                    }
                    VpnProfile profile = new VpnProfile(Long.toHexString(millis));
                    VpnConnectSettings.show(VpnSettings.this, profile, true /* editing */, false /* exists */);
                }
            });
        }
	}
    
    private void changeAsusButtonBarButtonState(boolean enabled) {
    	if(mButtonBar != null){
	    	mButtonBar.getButton(ADD_BUTTON_ID).setEnabled(enabled);
    	}
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
    	Object viewTag = view.getTag();
    	if(viewTag == null) return;
    	
    	menu.close();
        ArrayList<String> itemArray = new ArrayList<String>();
        
            Preference preference = (Preference) viewTag;

            if (preference instanceof LegacyVpnPreference) {
            	LegacyVpnPreference pref = (LegacyVpnPreference) preference;
            	mSelectedProfile = pref.getProfile();
            	if(mSelectedProfile == null)
            		return;
            	
//                menu.setHeaderTitle(mSelectedProfile.name);
            	itemArray.add(getString(R.string.vpn_button_modify));
            	itemArray.add(getString(R.string.vpn_button_delete));
//                menu.add(Menu.NONE, MENU_ID_MODIFY, 0, R.string.vpn_button_modify);
//                menu.add(Menu.NONE, MENU_ID_DELETE, 1, R.string.vpn_button_delete);
                
                if(pref.getState() == LegacyVpnInfo.STATE_CONNECTED)
                	itemArray.add(getString(R.string.vpn_button_disconnect));
//                	menu.add(Menu.NONE, MENU_ID_DISCONNECT, 2, R.string.vpn_button_disconnect);
                else
                	itemArray.add(getString(R.string.vpn_button_connect));
//                	menu.add(Menu.NONE, MENU_ID_CONNECT, 3, R.string.vpn_button_connect);
                
                if(itemArray.size() > 0)
                	showContextMenuDialog(itemArray.toArray(new String[itemArray.size()]));
            }
    }
    
    private void showContextMenuDialog(final String[] items){
    	Dialog menuDialog = new AlertDialog.Builder(getActivity())
    	    	.setTitle(mSelectedProfile.name)
    	        .setItems(items, new DialogInterface.OnClickListener(){

    				@Override
    				public void onClick(DialogInterface dlg, int which) {
    					// TODO Auto-generated method stub
    					String item = items[which];
    					if(item.equals(getString(R.string.vpn_button_modify))){
    						VpnConnectSettings.show(VpnSettings.this, mSelectedProfile, true /* editing */, true /* exists */);
    					} else if(item.equals(getString(R.string.vpn_button_delete))){
    						showForgetConfirmDialog();
    					} else if(item.equals(getString(R.string.vpn_button_disconnect))){
    						disconnect(mSelectedProfile);
    					} else if(item.equals(getString(R.string.vpn_button_connect))){
    						try {
    		                    connect(mSelectedProfile);
    		                } catch (RemoteException e) {
    		                    Log.e(LOG_TAG, "Failed to connect", e);
    		                }
    					}
    					
    				}
    	        	
    	        }).create();
    	    	
    	WindowManager.LayoutParams lp = menuDialog.getWindow().getAttributes();
        lp.gravity = Gravity.BOTTOM;
        menuDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mSelectedProfile == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case MENU_ID_CONNECT: {
            	try {
                    connect(mSelectedProfile);
                } catch (RemoteException e) {
                    Log.e(LOG_TAG, "Failed to connect", e);
                }
                return true;
            }
            case MENU_ID_DISCONNECT: {
            	// Disable profile if connected
                disconnect(mSelectedProfile);
                return true;
            }
            case MENU_ID_MODIFY: {
            	VpnConnectSettings.show(this, mSelectedProfile, true /* editing */, true /* exists */);
                return true;
            }
            case MENU_ID_DELETE:{
            	showForgetConfirmDialog();
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }
    
    private void connect(VpnProfile profile) throws RemoteException {
        try {
        	mConnectivityService.startLegacyVpn(profile);
        	
        	refresh();
        } catch (IllegalStateException e) {
            Toast.makeText(getActivity(), R.string.vpn_no_network, Toast.LENGTH_LONG).show();
        }
    }

    private void disconnect(VpnProfile profile) {
        try {
            LegacyVpnInfo connected = mConnectivityService.getLegacyVpnInfo(UserHandle.myUserId());
            if (connected != null && profile.key.equals(connected.key)) {
                VpnUtils.clearLockdownVpn(getContext());
                mConnectivityService.prepareVpn(VpnConfig.LEGACY_VPN, VpnConfig.LEGACY_VPN,
                        UserHandle.myUserId());
                
                refresh();
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failed to disconnect", e);
        }
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
        
        refresh();
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
						forget(mSelectedProfile);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.vpn, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Disable all actions if VPN configuration has been disallowed
        for (int i = 0; i < menu.size(); i++) {
            if (isUiRestrictedByOnlyAdmin()) {
                RestrictedLockUtils.setMenuItemAsDisabledByAdmin(getPrefContext(),
                        menu.getItem(i), getRestrictionEnforcedAdmin());
            } else {
                menu.getItem(i).setEnabled(!mUnavailable);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.vpn_create: {
                // Generate a new key. Here we just use the current time.
                long millis = System.currentTimeMillis();
                while (mLegacyVpnPreferences.containsKey(Long.toHexString(millis))) {
                    ++millis;
                }
                VpnProfile profile = new VpnProfile(Long.toHexString(millis));
                VpnConnectSettings.show(this, profile, true /* editing */, false /* exists */);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            // Show a message to explain that VPN settings have been disabled
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.vpn_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            if(mButtonBar != null)
                mButtonBar.setVisibility(View.GONE);
            return;
        } else {
            getEmptyTextView().setText(R.string.vpn_no_vpns_added);
        }

        // Start monitoring
        mConnectivityManager.registerNetworkCallback(VPN_REQUEST, mNetworkCallback);

        // Trigger a refresh
        if (mUpdater == null) {
            mUpdater = new Handler(this);
        }
        mUpdater.sendEmptyMessage(RESCAN_MESSAGE);
    }

    @Override
    public void onPause() {
        if (mUnavailable) {
            super.onPause();
            return;
        }

        // Stop monitoring
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);

        if (mUpdater != null) {
            mUpdater.removeCallbacksAndMessages(null);
        }

        super.onPause();
    }
    
    private SharedPreferences getSharedPreferences(){
    	return getActivity().getSharedPreferences("VpnSettings", 0);
    }
    
    private void addConnectedPreference(ManageablePreference preference){
    	removeConnectedPreference();
    	
    	preference.setKey(KEY_CONNECTED_PREF);
        preference.setOrder(1);
        if(preference instanceof LegacyVpnPreference)
        	((LegacyVpnPreference) preference).setLayoutResource(R.layout.asusres_preference_vpn_item_nodivider, mArrowListener);
        
        mConnectedPreference = preference;
		getPreferenceScreen().addPreference(mConnectedPreference);
		
		if(mVpnEnable != null)
			mVpnEnable.setLayoutResource(R.layout.asusres_preference_material);
		
		if(mConnectedLegacyVpn != null){
			//save last connected vpn
			SharedPreferences sharedPreferences = getSharedPreferences();
	    	if(sharedPreferences != null){
	    		if(!sharedPreferences.getString(KEY_SHARED_PREFERENCE_CONNECTED_VPN, "").equals(mConnectedLegacyVpn.key))
	    			sharedPreferences.edit().putString(KEY_SHARED_PREFERENCE_CONNECTED_VPN, mConnectedLegacyVpn.key).commit();
	    	}
		}
    }
    
    private void removeConnectedPreference(){
    	if(mConnectedPreference != null){
        	getPreferenceScreen().removePreference(mConnectedPreference);
        	mConnectedPreference = null;
    	}
    	
    	if(mVpnEnable != null)
    		mVpnEnable.setLayoutResource(R.layout.asusres_preference_material_nodivider);
    }

    @Override
    public boolean handleMessage(Message message) {
        mUpdater.removeMessages(RESCAN_MESSAGE);

        // Run heavy RPCs before switching to UI thread
        final List<VpnProfile> vpnProfiles = loadVpnProfiles(mKeyStore);
        final List<AppVpnInfo> vpnApps = Lists.newArrayList();
//        final List<AppVpnInfo> vpnApps = getVpnApps(getActivity(), /* includeProfiles */ true);

        final Map<String, LegacyVpnInfo> connectedLegacyVpns = getConnectedLegacyVpns();
        final Set<AppVpnInfo> connectedAppVpns = getConnectedAppVpns();

        final Set<AppVpnInfo> alwaysOnAppVpnInfos = getAlwaysOnAppVpnInfos();
        final String lockdownVpnKey = VpnUtils.getLockdownVpn();

        // Refresh list of VPNs
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Can't do anything useful if the context has gone away
                if (!isAdded()) {
                    return;
                }

                removeConnectedPreference();

                // Find new VPNs by subtracting existing ones from the full set
                final Set<Preference> updates = new ArraySet<>();

                for (VpnProfile profile : vpnProfiles) {
                    LegacyVpnPreference p = findOrCreatePreference(profile);
                    if (connectedLegacyVpns.containsKey(profile.key)) {
                        p.setState(connectedLegacyVpns.get(profile.key).state);
                    } else {
                        p.setState(LegacyVpnPreference.STATE_NONE);
                        
                        if(mVpnTypesLong != null && profile.type < mVpnTypesLong.length){
                        	p.setSummary(mVpnTypesLong[profile.type]);
                        }
                    }
                    p.setAlwaysOn(lockdownVpnKey != null && lockdownVpnKey.equals(profile.key));
                    
                    if(p.getState() == LegacyVpnInfo.STATE_CONNECTED)
                    	addConnectedPreference(p);
                    
                    updates.add(p);
                }
                for (AppVpnInfo app : vpnApps) {
                    AppPreference p = findOrCreatePreference(app);
                    if (connectedAppVpns.contains(app)) {
                        p.setState(AppPreference.STATE_CONNECTED);
                    } else {
                        p.setState(AppPreference.STATE_DISCONNECTED);
                    }
                    p.setAlwaysOn(alwaysOnAppVpnInfos.contains(app));
                    updates.add(p);
                }

                // Trim out deleted VPN preferences
                mLegacyVpnPreferences.values().retainAll(updates);
                mAppPreferences.values().retainAll(updates);
                
                if(mConnectedPreference != null)
                	updates.remove(mConnectedPreference);

                final PreferenceGroup vpnGroup = (PreferenceGroup) getPreferenceScreen().findPreference(KEY_VPN_LIST);
                for (int i = vpnGroup.getPreferenceCount() - 1; i >= 0; i--) {
                    Preference p = vpnGroup.getPreference(i);
                    if (updates.contains(p)) {
                        updates.remove(p);
                    } else {
                        vpnGroup.removePreference(p);
                    }
                }

                // Show any new preferences on the screen
                for (Preference pref : updates) {
                	ManageablePreference p = (ManageablePreference) pref;
            		if(p.getState() == LegacyVpnInfo.STATE_CONNECTED)
            			continue;
            		
            		if(pref instanceof LegacyVpnPreference)
            			((LegacyVpnPreference) pref).setLayoutResource(R.layout.asusres_preference_vpn_item, mArrowListener);
            			
                    vpnGroup.addPreference(pref);
                    
                  //setDependency must after addPreference
                    pref.setDependency(KEY_VPN_ENABLE);
                }
                
                vpnGroup.setVisible((vpnGroup.getPreferenceCount() > 0) ? true : false);
                setPreferenceLayoutResource(vpnGroup);
            }
        });

        mUpdater.sendEmptyMessageDelayed(RESCAN_MESSAGE, RESCAN_INTERVAL_MS);
        return true;
    }
    
    private void setPreferenceLayoutResource(PreferenceGroup group){
		int count = group.getPreferenceCount();
        if(count > 0){
        	for(int i = 0; i < count-1; i++){
        		Preference p = group.getPreference(i);
        		if(p instanceof LegacyVpnPreference)
        			((LegacyVpnPreference) p).setLayoutResource(R.layout.asusres_preference_vpn_item, mArrowListener);
        	}
        	
        	Preference p = group.getPreference(count-1);
    		if(p instanceof LegacyVpnPreference)
    			((LegacyVpnPreference) p).setLayoutResource(R.layout.asusres_preference_vpn_item_nodivider, mArrowListener);
        }
	}

    @Override
    public boolean onPreferenceClick(Preference preference) {
    	Log.d("timhu", "onPreferenceClick preference = " + preference);
        if (preference instanceof LegacyVpnPreference) {
            LegacyVpnPreference pref = (LegacyVpnPreference) preference;
            VpnProfile profile = pref.getProfile();
            if (mConnectedLegacyVpn != null && profile.key.equals(mConnectedLegacyVpn.key) &&
                    mConnectedLegacyVpn.state == LegacyVpnInfo.STATE_CONNECTED) {
//                try {
//                    mConnectedLegacyVpn.intent.send();
//                    return true;
//                } catch (Exception e) {
//                    Log.w(LOG_TAG, "Starting config intent failed", e);
//                }
            	VpnViewSettings.show(this, profile, pref.getState());
            	return true;
            }
            VpnConnectSettings.show(this, profile, false /* editing */, true /* exists */);
            return true;
        } else if (preference instanceof AppPreference) {
            AppPreference pref = (AppPreference) preference;
            boolean connected = (pref.getState() == AppPreference.STATE_CONNECTED);

            if (!connected) {
                try {
                    UserHandle user = UserHandle.of(pref.getUserId());
                    Context userContext = getActivity().createPackageContextAsUser(
                            getActivity().getPackageName(), 0 /* flags */, user);
                    PackageManager pm = userContext.getPackageManager();
                    Intent appIntent = pm.getLaunchIntentForPackage(pref.getPackageName());
                    if (appIntent != null) {
                        userContext.startActivityAsUser(appIntent, user);
                        return true;
                    }
                } catch (PackageManager.NameNotFoundException nnfe) {
                    Log.w(LOG_TAG, "VPN provider does not exist: " + pref.getPackageName(), nnfe);
                }
            }

            // Already connected or no launch intent available - show an info dialog
            PackageInfo pkgInfo = pref.getPackageInfo();
            AppDialogFragment.show(this, pkgInfo, pref.getLabel(), false /* editing */, connected);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if(KEY_VPN_ENABLE.equals(key)){
        	boolean vpnEnable = (boolean) newValue;
        	changeAsusButtonBarButtonState(vpnEnable);
        	
        	if(vpnEnable){
        		//auto connect
        		String connectedVpnKey = null;
        		SharedPreferences sharedPreferences = getSharedPreferences();
    	    	if(sharedPreferences != null){
    	    		connectedVpnKey = sharedPreferences.getString(KEY_SHARED_PREFERENCE_CONNECTED_VPN, "");
    	    		Log.d("timhu", "connectedVpnKey = " + connectedVpnKey);
    	    	}
    	    	
                if ((mConnectedPreference == null) && (connectedVpnKey != null)) {
                	LegacyVpnPreference pref = mLegacyVpnPreferences.get(connectedVpnKey);
                	if (pref != null) {
                		try {
                            connect(pref.getProfile());
                        } catch (RemoteException e) {
                            Log.e(LOG_TAG, "Failed to connect", e);
                        }
	                }
                }
        	} else{
        		//disconnect
        		if(mConnectedPreference != null){
        			if (mConnectedPreference instanceof LegacyVpnPreference) {
        	            LegacyVpnPreference pref = (LegacyVpnPreference) mConnectedPreference;
        	            disconnect(pref.getProfile());
        			}
        		}
        		removeConnectedPreference();
        	}
        	
        	SharedPreferences sharedPreferences = getSharedPreferences();
        	if(sharedPreferences != null)
        		sharedPreferences.edit().putBoolean(KEY_SHARED_PREFERENCE_VPN_ENABLE, vpnEnable).commit();

        }
        return true;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_vpn;
    }

    private OnGearClickListener mGearListener = new OnGearClickListener() {
        @Override
        public void onGearClick(GearPreference p) {
            if (p instanceof LegacyVpnPreference) {
                LegacyVpnPreference pref = (LegacyVpnPreference) p;
                VpnConnectSettings.show(VpnSettings.this, pref.getProfile(), true /* editing */,
                        true /* exists */);
            } else if (p instanceof AppPreference) {
                AppPreference pref = (AppPreference) p;;
                AppManagementFragment.show(getPrefContext(), pref);
            }
        }
    };

    private NetworkCallback mNetworkCallback = new NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            if (mUpdater != null) {
                mUpdater.sendEmptyMessage(RESCAN_MESSAGE);
            }
        }

        @Override
        public void onLost(Network network) {
            if (mUpdater != null) {
                mUpdater.sendEmptyMessage(RESCAN_MESSAGE);
            }
        }
    };

    @UiThread
    private LegacyVpnPreference findOrCreatePreference(VpnProfile profile) {
        LegacyVpnPreference pref = mLegacyVpnPreferences.get(profile.key);
        if (pref == null) {
            pref = new LegacyVpnPreference(getPrefContext(), this);
//            pref.setDependency(KEY_VPN_ENABLE);
//            pref.setOnGearClickListener(mGearListener);
            pref.setOnPreferenceClickListener(this);
            mLegacyVpnPreferences.put(profile.key, pref);
        }
        // This may change as the profile can update and keep the same key.
        pref.setProfile(profile);
        return pref;
    }

    @UiThread
    private AppPreference findOrCreatePreference(AppVpnInfo app) {
        AppPreference pref = mAppPreferences.get(app);
        if (pref == null) {
            pref = new AppPreference(getPrefContext(), app.userId, app.packageName);
            pref.setOnGearClickListener(mGearListener);
            pref.setOnPreferenceClickListener(this);
            mAppPreferences.put(app, pref);
        }
        return pref;
    }

    @WorkerThread
    private Map<String, LegacyVpnInfo> getConnectedLegacyVpns() {
        try {
            mConnectedLegacyVpn = mConnectivityService.getLegacyVpnInfo(UserHandle.myUserId());
            if (mConnectedLegacyVpn != null) {
                return Collections.singletonMap(mConnectedLegacyVpn.key, mConnectedLegacyVpn);
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failure updating VPN list with connected legacy VPNs", e);
        }
        return Collections.emptyMap();
    }

    @WorkerThread
    private Set<AppVpnInfo> getConnectedAppVpns() {
        // Mark connected third-party services
        Set<AppVpnInfo> connections = new ArraySet<>();
        try {
            for (UserHandle profile : mUserManager.getUserProfiles()) {
                VpnConfig config = mConnectivityService.getVpnConfig(profile.getIdentifier());
                if (config != null && !config.legacy) {
                    connections.add(new AppVpnInfo(profile.getIdentifier(), config.user));
                }
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Failure updating VPN list with connected app VPNs", e);
        }
        return connections;
    }

    @WorkerThread
    private Set<AppVpnInfo> getAlwaysOnAppVpnInfos() {
        Set<AppVpnInfo> result = new ArraySet<>();
        for (UserHandle profile : mUserManager.getUserProfiles()) {
            final int profileId = profile.getIdentifier();
            final String packageName = mConnectivityManager.getAlwaysOnVpnPackageForUser(profileId);
            if (packageName != null) {
                result.add(new AppVpnInfo(profileId, packageName));
            }
        }
        return result;
    }

    static List<AppVpnInfo> getVpnApps(Context context, boolean includeProfiles) {
        List<AppVpnInfo> result = Lists.newArrayList();

        final Set<Integer> profileIds;
        if (includeProfiles) {
            profileIds = new ArraySet<>();
            for (UserHandle profile : UserManager.get(context).getUserProfiles()) {
                profileIds.add(profile.getIdentifier());
            }
        } else {
            profileIds = Collections.singleton(UserHandle.myUserId());
        }

        // Fetch VPN-enabled apps from AppOps.
        AppOpsManager aom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        List<AppOpsManager.PackageOps> apps = aom.getPackagesForOps(new int[] {OP_ACTIVATE_VPN});
        if (apps != null) {
            for (AppOpsManager.PackageOps pkg : apps) {
                int userId = UserHandle.getUserId(pkg.getUid());
                if (!profileIds.contains(userId)) {
                    // Skip packages for users outside of our profile group.
                    continue;
                }
                // Look for a MODE_ALLOWED permission to activate VPN.
                boolean allowed = false;
                for (AppOpsManager.OpEntry op : pkg.getOps()) {
                    if (op.getOp() == OP_ACTIVATE_VPN &&
                            op.getMode() == AppOpsManager.MODE_ALLOWED) {
                        allowed = true;
                    }
                }
                if (allowed) {
                    result.add(new AppVpnInfo(userId, pkg.getPackageName()));
                }
            }
        }

        Collections.sort(result);
        return result;
    }

    static List<VpnProfile> loadVpnProfiles(KeyStore keyStore, int... excludeTypes) {
        final ArrayList<VpnProfile> result = Lists.newArrayList();

        for (String key : keyStore.list(Credentials.VPN)) {
            final VpnProfile profile = VpnProfile.decode(key, keyStore.get(Credentials.VPN + key));
            if (profile != null && !ArrayUtils.contains(excludeTypes, profile.type)) {
                result.add(profile);
            }
        }
        return result;
    }
    
    private void refresh(){
    	if (mUpdater != null) {
            mUpdater.sendEmptyMessage(RESCAN_MESSAGE);
        }
    }
    
    public void showSettingsFragment(String fragmentClass, int titleRes, Bundle args){
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
}
