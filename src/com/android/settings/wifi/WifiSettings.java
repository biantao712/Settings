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

import android.app.Activity;
import android.app.Fragment;
import android.app.Dialog;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.DialogInterface;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Process;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import android.widget.ImageView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.LinkifyUtils;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.location.ScanningSettings;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.util.VerizonHelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settingslib.wifi.AccessPoint.AccessPointListener;
import com.android.settingslib.wifi.AccessPointPreference;
import com.android.settingslib.wifi.WifiStatusTracker;
import com.android.settingslib.wifi.WifiTracker;
import com.android.settings.Utils;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.Display;
import android.view.Window;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.lang.reflect.Method;
import java.lang.Exception;

import static android.os.UserManager.DISALLOW_CONFIG_WIFI;

import com.google.zxing.client.result.WifiParsedResult;
//import com.asustek.DUTUtil.DUT_PKT_GET_INFO;
//import static com.asustek.DUTUtil.DUTUtil.*;

import com.asus.cncommonres.AsusButtonBar;

/**
 * Two types of UI are provided here.
 *
 * The first is for "usual Settings", appearing as any other Setup fragment.
 *
 * The second is for Setup Wizard, with a simplified interface that hides the action bar
 * and menus.
 */
public class WifiSettings extends RestrictedSettingsFragment
        implements Indexable, WifiTracker.WifiListener, AccessPointListener,
        WifiDialog.WifiDialogListener {

    private static final String TAG = "WifiSettings";

    /* package */ static final int MENU_ID_WPS_PBC = Menu.FIRST;
    private static final int MENU_ID_WPS_PIN = Menu.FIRST + 1;
    private static final int MENU_ID_ADVANCED = Menu.FIRST + 4;
    private static final int MENU_ID_SCAN = Menu.FIRST + 5;
    private static final int MENU_ID_CONNECT = Menu.FIRST + 6;
    private static final int MENU_ID_FORGET = Menu.FIRST + 7;
    private static final int MENU_ID_MODIFY = Menu.FIRST + 8;
    private static final int MENU_ID_WRITE_NFC = Menu.FIRST + 9;
    private static final int MENU_ID_CONFIGURE = Menu.FIRST + 10;
    private static final int MENU_ID_WIFI_SCAN_QRCODE = Menu.FIRST + 11;
    private static final int MENU_ID_WIFI_GEN_QRCODE = Menu.FIRST + 12;
    private static final int MENU_ID_VERIZON_HELP = Menu.FIRST + 13;
    
    private static final int MENU_ID_WIFI_DISCONNECT = Menu.FIRST + 14;
    private static final int MENU_ID_WIFI_IGNORE = Menu.FIRST + 15;

    public static final int WIFI_DIALOG_ID = 1;
    /* package */ static final int WPS_PBC_DIALOG_ID = 2;
    private static final int WPS_PIN_DIALOG_ID = 3;
    private static final int WRITE_NFC_DIALOG_ID = 6;
    private static final int WIFI_DIALOG_GENQRCODE_ID = 7;
    private static final int WIFI_DIALOG_MENU_ID = 8;

    // Instance state keys
    private static final String SAVE_DIALOG_MODE = "dialog_mode";
    private static final String SAVE_DIALOG_ACCESS_POINT_STATE = "wifi_ap_state";
    private static final String SAVED_WIFI_NFC_DIALOG_STATE = "wifi_nfc_dlg_state";
    private static final String PREFERENCES_NAME = "router_preferences";
    private static final String PREF_KEY_EMPTY_WIFI_LIST = "wifi_empty_list";
    private static final String ASUS_ROUTER_PACKAGE_NAME = "com.asus.aihome";
    private static final String ASUS_ROUTER_DIALOG_ONESHOT = "router_dialog_one_shot";
    
    private static final String KEY_CONNECTED_NETWORK = "wifi_connected_network";
    private static final String KEY_WIFI_LIST_CATEGORY = "wifi_list_category";

    protected WifiManager mWifiManager;
    private WifiManager.ActionListener mConnectListener;
    private WifiManager.ActionListener mSaveListener;
    private WifiManager.ActionListener mForgetListener;

    private WifiEnabler mWifiEnabler;
    // An access point being editted is stored here.
    private AccessPoint mSelectedAccessPoint;

    private WifiDialog mDialog;
    private WriteWifiConfigToNfcDialog mWifiToNfcDialog;

    private ProgressBar mProgressHeader;

    // this boolean extra specifies whether to disable the Next button when not connected. Used by
    // account creation outside of setup wizard.
    private static final String EXTRA_ENABLE_NEXT_ON_CONNECT = "wifi_enable_next_on_connect";
    // This string extra specifies a network to open the connect dialog on, so the user can enter
    // network credentials.  This is used by quick settings for secured networks.
    private static final String EXTRA_START_CONNECT_SSID = "wifi_start_connect_ssid";

    private final String BARCODE_WIFI_CONFIG  = "barcode_wifi_config";
    private final int BARCODE_WIFI_REQUEST_CODE = 2;
    
    private final int REFRESH_BUTTON_ID = 1;
    private final int ADD_BUTTON_ID = 2;
    private final int QRCODE_BUTTON_ID = 3;
    private final int MENU_BUTTON_ID = 4;

    // should Next button only be enabled when we have a connection?
    private boolean mEnableNextOnConnection;

    // Save the dialog details
    private int mDialogMode;
    private AccessPoint mDlgAccessPoint;
    private Bundle mAccessPointSavedState;
    private Bundle mWifiNfcDialogSavedState;

    private WifiTracker mWifiTracker;
    private String mOpenSsid;

    private HandlerThread mBgThread;

    private AccessPointPreference.UserBadgeCache mUserBadgeCache;
    private boolean isRegistered = false;
    private SharedPreferences mPreferneces;
    private AlertDialog mAlertDialog = null;
//    private Preference mAddPreference;
    private WifiInfo mLastInfo;

    private MenuItem mScanMenuItem;

    private CheckBoxPreference mPopupCheck;
    private PreferenceGroup mWifiListPreGroup;
    private PreferenceGroup mAccessPointContainer;
    
    private SwitchPreference mWifiSwitchPreference;
    private LongPressAccessPointPreference mConnectedNetworkPreference;
    
    private AsusButtonBar mButtonBar;
    public static final String APP_CAMERA = "com.asus.camera";
    private long mAnimationStartTime = 0;
    private boolean mUnavailable;

    /* End of "used in Wifi Setup context" */

    public WifiSettings() {
        super(DISALLOW_CONFIG_WIFI);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Activity activity = getActivity();
        if (activity != null) {
//            mProgressHeader = (ProgressBar) setPinnedHeaderView(R.layout.wifi_progress_header);
        }
        
        if(Utils.isVerizon()) {
            mPopupCheck = (CheckBoxPreference) getPreferenceScreen().findPreference("wifi_popup");
            mWifiListPreGroup = (PreferenceGroup) getPreferenceScreen().findPreference("wifi_list");
            mAccessPointContainer = mWifiListPreGroup;
        } else {
            removePreference("wifi_popup");
            removePreference("wifi_list");
        }
        
        mWifiSwitchPreference = (SwitchPreference) getPreferenceScreen().findPreference("wifi_switch");  
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.wifi_settings);
//        mAddPreference = new Preference(getContext());
//        mAddPreference.setIcon(R.drawable.ic_menu_add_inset);
//        mAddPreference.setTitle(R.string.wifi_add_network);

        mUserBadgeCache = new AccessPointPreference.UserBadgeCache(getPackageManager());

        mBgThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mBgThread.start();
        mPreferneces = getContext().getSharedPreferences(PREFERENCES_NAME, 0);
			         if(!mPreferneces.contains(ASUS_ROUTER_DIALOG_ONESHOT)) {
			            SharedPreferences.Editor editor = mPreferneces.edit();
			             editor.putBoolean(ASUS_ROUTER_DIALOG_ONESHOT, false);
			             editor.commit();
			        }
			        if(!mPreferneces.getBoolean(ASUS_ROUTER_DIALOG_ONESHOT, false) && !isRegistered) {
			            getContext().registerReceiver(mAsusRouterReceiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
         isRegistered = true;
			        }
    }

    @Override
    public void onDestroy() {
        mBgThread.quit();
        super.onDestroy();
        if(isRegistered) {
			            try {
         getContext().unregisterReceiver(mAsusRouterReceiver);
         } catch (IllegalArgumentException e) {
		 }
         }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWifiTracker =
                new WifiTracker(getActivity(), this, mBgThread.getLooper(), true, true, false);
        mWifiManager = mWifiTracker.getManager();

        mUnavailable = isUiRestricted();


        mConnectListener = new WifiManager.ActionListener() {
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

        mSaveListener = new WifiManager.ActionListener() {
                                @Override
                                public void onSuccess() {
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
                            };

        mForgetListener = new WifiManager.ActionListener() {
                                   @Override
                                   public void onSuccess() {
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

        if (savedInstanceState != null) {
            mDialogMode = savedInstanceState.getInt(SAVE_DIALOG_MODE);
            if (savedInstanceState.containsKey(SAVE_DIALOG_ACCESS_POINT_STATE)) {
                mAccessPointSavedState =
                    savedInstanceState.getBundle(SAVE_DIALOG_ACCESS_POINT_STATE);
            }

            if (savedInstanceState.containsKey(SAVED_WIFI_NFC_DIALOG_STATE)) {
                mWifiNfcDialogSavedState =
                    savedInstanceState.getBundle(SAVED_WIFI_NFC_DIALOG_STATE);
            }
        }

        // if we're supposed to enable/disable the Next button based on our current connection
        // state, start it off in the right state
        Intent intent = getActivity().getIntent();
        mEnableNextOnConnection = intent.getBooleanExtra(EXTRA_ENABLE_NEXT_ON_CONNECT, false);

        if (mEnableNextOnConnection) {
            if (hasNextButton()) {
                final ConnectivityManager connectivity = (ConnectivityManager)
                        getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
                if (connectivity != null) {
                    NetworkInfo info = connectivity.getNetworkInfo(
                            ConnectivityManager.TYPE_WIFI);
                    changeNextButtonState(info.isConnected());
                }
            }
        }

        if (intent.hasExtra(BARCODE_WIFI_CONFIG)) {
            connectToNetwork(intent);
        }

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);

        if (intent.hasExtra(EXTRA_START_CONNECT_SSID)) {
            mOpenSsid = intent.getStringExtra(EXTRA_START_CONNECT_SSID);
            onAccessPointsChanged();
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        WifiQRCodeUtils.DeviceH = metrics.heightPixels;
        WifiQRCodeUtils.DeviceW = metrics.widthPixels;
        
        initButtonBar();
    }
    
    private void initButtonBar(){
    	final Fragment fragment = this;
		final SettingsActivity activity = (SettingsActivity) getActivity();
		mButtonBar = activity.getButtonBar();
		if(mButtonBar != null) {
			mButtonBar.setVisibility(View.VISIBLE);

			mButtonBar.addButton(REFRESH_BUTTON_ID, R.drawable.asus_btn_refresh, getString(R.string.menu_stats_refresh));
			mButtonBar.getButton(REFRESH_BUTTON_ID).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                	case MENU_ID_SCAN:
                    Animation animation = AnimationUtils.loadAnimation(activity, R.anim.refresh_button_rotate);
                    animation.setRepeatCount(0);
                    View icon = v.findViewById(com.asus.cncommonres.R.id.icon);
                    if(icon != null){
                        icon.startAnimation(animation);
                        mAnimationStartTime = System.currentTimeMillis();
                    }

                	MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_FORCE_SCAN);
                    mWifiTracker.forceScan();
                }
             });

		    mButtonBar.addButton(ADD_BUTTON_ID, R.drawable.asus_btn_add, getString(R.string.wifi_add_network));
			mButtonBar.getButton(ADD_BUTTON_ID).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                case MENU_ID_WIFI_SCAN_QRCODE:
                	onAddNetworkPressed();
                }
            });
            
			mButtonBar.addButton(QRCODE_BUTTON_ID, R.drawable.asus_btn_scanning, getString(R.string.wifi_menu_qrcode));
			mButtonBar.getButton(QRCODE_BUTTON_ID).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                case MENU_ID_WIFI_SCAN_QRCODE:
                    openAsusCamera();
                }
            });
            
			mButtonBar.addButton(MENU_BUTTON_ID, R.drawable.asusres_btn_menu, getString(R.string.wifi_menu_options));
			mButtonBar.getButton(MENU_BUTTON_ID).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                case MENU_ID_CONFIGURE:
                	showDialog(WIFI_DIALOG_MENU_ID);
                	
                    /*if (getActivity() instanceof SettingsActivity) {
                        ((SettingsActivity) getActivity()).startPreferencePanel(
                                ConfigureWifiSettings.class.getCanonicalName(), null,
                                R.string.wifi_configure_titlebar, null, fragment, 0);
                    } else {
                        startFragment(fragment, ConfigureWifiSettings.class.getCanonicalName(),
                                R.string.wifi_configure_titlebar, -1  Do not request a results ,
                                null);
                    }*/
                }
            });
        }
	}

    private void openAsusCamera()
    {
        if(isAppDisabled(getActivity(), APP_CAMERA))
            showCollageWarningDialog(getActivity());
        else {
            Intent intent = getPackageManager().getLaunchIntentForPackage(APP_CAMERA);
            if (intent != null) {
                intent.putExtra(BARCODE_WIFI_CONFIG, BARCODE_WIFI_CONFIG);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivityForResult(intent, BARCODE_WIFI_REQUEST_CODE);
            }
        }
    }

    private void showCollageWarningDialog(final Context context) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
        TextView mTitle = (TextView) view.getRootView().findViewById(R.id.alertdialog_title);
        TextView mMessage = (TextView) view.getRootView().findViewById(R.id.alertdialog_message);
        String title = null, message = null, positive = null, negative = getString(android.R.string.cancel);

        title = getString(R.string.dialog_enable_app_title, getString(R.string.folder_camera));
        message = getString(R.string.dialog_enable_app_message, getString(R.string.folder_camera));
        positive = getString(R.string.dialog_enable_app_yes);

        mTitle.setText(title);
        mMessage.setText(message);

        AlertDialog mAdbDialog = new AlertDialog.Builder(context)
                .setPositiveButton(positive, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            openAppInfo(APP_CAMERA, context);
                        } catch (Exception e) {

                        }
                    }
                })
                .setNegativeButton(negative, null)
                .setView(view)
                .show();
        //mAdbDialog.setOnDismissListener(this);

        Window window = mAdbDialog.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        window.setAttributes(lp);
//
//        String title = null, message = null, positive = null, negative = getString(android.R.string.cancel);
//
//        title = getString(R.string.dialog_enable_app_title, getString(R.string.folder_camera));
//        message = getString(R.string.dialog_enable_app_message, getString(R.string.folder_camera));
//        positive = getString(R.string.dialog_enable_app_yes);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(context)
//                .setTitle(title)
//                .setMessage(message)
//                .setNegativeButton(negative, null)
//                .setPositiveButton(positive, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        try {
//                            openAppInfo(APP_CAMERA, context);
//                        } catch (Exception e) {
//
//                        }
//                    }
//                });
//        builder.show();
    }

    private void openAppInfo(String packageName, Context context) {
        Uri packageURI = Uri.parse("package:" + packageName);
        Intent intent = new Intent("android.settings.APPLICATION_DETAILS_SETTINGS", packageURI);
        context.startActivity(intent);
    }

    public static boolean isAppDisabled(Context context, String packageName){
        boolean result;
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName,0);
            result = ai.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Dave", "NameNotFoundException: " + packageName);
            result = false;
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        Log.d("Dave", "is app disabled: " + !result);

        //return true if app is disabled
        return !result;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mWifiEnabler != null) {
            mWifiEnabler.teardownSwitchBar();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // On/off switch is hidden for Setup Wizard (returns null)
        mWifiEnabler = createWifiEnabler();
    }

    /**
     * @return new WifiEnabler or null (as overridden by WifiSettingsForSetupWizard)
     */
    /* package */ WifiEnabler createWifiEnabler() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        activity.getSwitchBar().hide();  //tim++
        return new WifiEnabler(activity, mWifiSwitchPreference);
    }

    @Override
    public void onResume() {
        final Activity activity = getActivity();
        super.onResume();

        //+++ disable wifi for CTSV
        setIfOnlyAvailableForAdmins(true);
        if (mUnavailable) {
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
            if(mButtonBar != null)
                mButtonBar.setVisibility(View.GONE);
            return;
        }
        //---

        removePreference("dummy");
        if (mWifiEnabler != null) {
            mWifiEnabler.resume(activity);
            // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10682 Wi-Fi tutorial
            if (mDialog == null || !mDialog.isShowing()) {
		        if(VerizonHelpUtils.isVerizonMachine())
            		mWifiEnabler.setupTutorial(activity);
            }
            // ---
        }

        mWifiTracker.startTracking();
        activity.invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mUnavailable) return;

        if (mWifiEnabler != null) {
            mWifiEnabler.pause();
        }

        mWifiTracker.stopTracking();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // If the user is not allowed to configure wifi, do not show the menu.
        if (isUiRestricted()) return;

//        addOptionsMenuItems(menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    /**
     * @param menu
     */
    void addOptionsMenuItems(Menu menu) {
    	//tim++
        final boolean wifiIsEnabled = mWifiTracker.isWifiEnabled();
        mScanMenuItem = menu.add(Menu.NONE, MENU_ID_SCAN, 0, R.string.menu_stats_refresh);
        mScanMenuItem.setEnabled(wifiIsEnabled)
               .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_ADVANCED, 0, R.string.wifi_menu_advanced)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(Menu.NONE, MENU_ID_CONFIGURE, 0, R.string.wifi_menu_configure)
                .setIcon(R.drawable.asus_settings_ic_gear)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10667
	    if(VerizonHelpUtils.isVerizonMachine()){
	        menu.add(Menu.NONE, MENU_ID_VERIZON_HELP, 0, R.string.verizon_help)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	    }
        // ---
        menu.add(Menu.NONE, MENU_ID_WIFI_SCAN_QRCODE, 0, R.string.wifi_qrcode)
                .setIcon(R.drawable.asus_settings_ic_qr_code)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WIFI;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // If the dialog is showing, save its state.
        if (mDialog != null && mDialog.isShowing()) {
            outState.putInt(SAVE_DIALOG_MODE, mDialogMode);
            if (mDlgAccessPoint != null) {
                mAccessPointSavedState = new Bundle();
                mDlgAccessPoint.saveWifiState(mAccessPointSavedState);
                outState.putBundle(SAVE_DIALOG_ACCESS_POINT_STATE, mAccessPointSavedState);
            }
        }

        if (mWifiToNfcDialog != null && mWifiToNfcDialog.isShowing()) {
            Bundle savedState = new Bundle();
            mWifiToNfcDialog.saveState(savedState);
            outState.putBundle(SAVED_WIFI_NFC_DIALOG_STATE, savedState);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // If the user is not allowed to configure wifi, do not handle menu selections.
        if (isUiRestricted()) return false;

        switch (item.getItemId()) {
            case MENU_ID_WPS_PBC:
                showDialog(WPS_PBC_DIALOG_ID);
                return true;
                /*
            case MENU_ID_P2P:
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            WifiP2pSettings.class.getCanonicalName(),
                            null,
                            R.string.wifi_p2p_settings_title, null,
                            this, 0);
                } else {
                    startFragment(this, WifiP2pSettings.class.getCanonicalName(),
                            R.string.wifi_p2p_settings_title, -1, null);
                }
                return true;
                */
            case MENU_ID_WPS_PIN:
                showDialog(WPS_PIN_DIALOG_ID);
                return true;
            case MENU_ID_SCAN:
            	//tim++
                MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_FORCE_SCAN);
                mWifiTracker.forceScan();
                return true;
            case MENU_ID_ADVANCED:
            	//tim++
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            AdvancedWifiSettings.class.getCanonicalName(), null,
                            R.string.wifi_advanced_titlebar, null, this, 0);
                } else {
                    startFragment(this, AdvancedWifiSettings.class.getCanonicalName(),
                            R.string.wifi_advanced_titlebar, -1 /* Do not request a results */,
                            null);
                }
                return true;
            case MENU_ID_CONFIGURE:
            	//tim++
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            ConfigureWifiSettings.class.getCanonicalName(), null,
                            R.string.wifi_configure_titlebar, null, this, 0);
                } else {
                    startFragment(this, ConfigureWifiSettings.class.getCanonicalName(),
                            R.string.wifi_configure_titlebar, -1 /* Do not request a results */,
                            null);
                }
                return true;
            // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10667
            case MENU_ID_VERIZON_HELP:
            	VerizonHelpUtils.launchVzWHelp(getActivity(), VerizonHelpUtils.SCREEN_WIFI);
                return true;
            // ---
            case MENU_ID_WIFI_SCAN_QRCODE:
            	//tim++
                openAsusCamera();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == BARCODE_WIFI_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                connectToNetwork(intent);
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void connectToNetwork(Intent intent)
    {
        String contents = intent.getStringExtra(BARCODE_WIFI_CONFIG);
//        Log.d(TAG,"qrcode string = " + contents);
        WifiParsedResult wifiResult = WifiQRCodeUtils.getResult(contents);
        if(wifiResult == null) {
            Toast.makeText(getActivity(), "Wrong string format",
                        Toast.LENGTH_SHORT).show();
            return ;
        }
        AccessPoint ap = isSavedNetwork(wifiResult.getSsid());
        if (ap != null) {
            final WifiConfiguration config = ap.getConfig();
            WifiConfigManager.updateNetWorkConfig(mWifiManager, config, wifiResult);
            //Dave: the network id is changed when updating, so fetch it again.
            final WifiConfiguration final_config = findNetworkInExistingConfig(mWifiManager, config.SSID );
            if(final_config != null)
                connect(final_config);
        } else {
            new WifiConfigManager(mWifiManager).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, wifiResult);
        }

    }

    private static WifiConfiguration findNetworkInExistingConfig(WifiManager wifiManager, String ssid) {
        Iterable<WifiConfiguration> existingConfigs = wifiManager.getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                String existingSSID = existingConfig.SSID;
                if (existingSSID != null && existingSSID.equals(ssid)) {
                    Log.d("Dave","wifisetting. config found");
                    return existingConfig;
                }
            }
        }
        return null;
    }

    private AccessPoint isSavedNetwork(String ssid)
    {
        List<AccessPoint> savedap = mWifiTracker.getCurrentAccessPoints(getActivity(), true, false, false);
        for(AccessPoint ap : savedap) {
            if(ssid.equals(ap.getSsidStr()))
                return ap;
        }
        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {
            Preference preference = (Preference) view.getTag();
            
            menu.close();
            ArrayList<String> itemArray = new ArrayList<String>();

            if (preference instanceof LongPressAccessPointPreference) {
                mSelectedAccessPoint =
                        ((LongPressAccessPointPreference) preference).getAccessPoint();
//                menu.setHeaderTitle(mSelectedAccessPoint.getSsid());
                if (mSelectedAccessPoint.isConnectable()) {
//                    menu.add(Menu.NONE, MENU_ID_CONNECT, 0, R.string.wifi_menu_connect);
                	itemArray.add(getString(R.string.wifi_menu_connect));
                }

                WifiConfiguration config = mSelectedAccessPoint.getConfig();
                // Some configs are ineditable
                if (isEditabilityLockedDown(getActivity(), config)) {
                    return;
                }

                if (mSelectedAccessPoint.isSaved()) {
//                    menu.add(Menu.NONE, MENU_ID_MODIFY, 1, R.string.wifi_menu_modify);
                	itemArray.add(getString(R.string.wifi_menu_modify));
                    //tim++  disable write to nfc
//                    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
//                    if (nfcAdapter != null && nfcAdapter.isEnabled() &&
//                            mSelectedAccessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
//                        // Only allow writing of NFC tags for password-protected networks.
//                        menu.add(Menu.NONE, MENU_ID_WRITE_NFC, 0, R.string.wifi_menu_write_to_nfc);
//                    }
                    //tim--
                }
                
                if (mSelectedAccessPoint.isSaved() || mSelectedAccessPoint.isEphemeral()) {
                    // Allow forgetting a network if either the network is saved or ephemerally
                    // connected. (In the latter case, "forget" blacklists the network so it won't
                    // be used again, ephemerally).
//                    menu.add(Menu.NONE, MENU_ID_FORGET, 2, R.string.wifi_menu_forget);
                    itemArray.add(getString(R.string.wifi_menu_forget));
                }
                
//                if( mSelectedAccessPoint.isSaved() && !WifiQRCodeUtils.getApQRCodeString(mSelectedAccessPoint,mWifiManager).equals("")) {
//                    menu.add(Menu.NONE, MENU_ID_WIFI_GEN_QRCODE, 3, R.string.wifi_qrcode_share_network);
//                	itemArray.add(getString(R.string.wifi_qrcode_share_network));
//                }
//                if(mSelectedAccessPoint.isActive())
//                	itemArray.add(getString(R.string.wifi_disconnect));
//                	menu.add(Menu.NONE, MENU_ID_WIFI_DISCONNECT, 4, R.string.wifi_disconnect);
//                menu.add(Menu.NONE, MENU_ID_WIFI_IGNORE, 5, R.string.wifi_ignore);
//                itemArray.add(getString(R.string.wifi_ignore));
                
                if(itemArray.size() > 0)
                	showContextMenuDialog(itemArray.toArray(new String[itemArray.size()]));
            }
    }
    
    private void showContextMenuDialog(final String[] items){
    	Dialog menuDialog = new AlertDialog.Builder(getActivity())
    	    	.setTitle(mSelectedAccessPoint.getSsid())
                .setNegativeButton(android.R.string.cancel, null)
    	        .setItems(items, new DialogInterface.OnClickListener(){

    				@Override
    				public void onClick(DialogInterface dlg, int which) {
    					// TODO Auto-generated method stub
    					String item = items[which];
    					if(item.equals(getString(R.string.wifi_menu_connect))){
    						if (mSelectedAccessPoint.isSaved()) {
    		                    connect(mSelectedAccessPoint.getConfig());
    		                } else if (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
    		                    /** Bypass dialog for unsecured networks */
    		                    mSelectedAccessPoint.generateOpenNetworkConfig();
    		                    connect(mSelectedAccessPoint.getConfig());
    		                } else {
    		                    showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_CONNECT);
    		                }
    					} else if(item.equals(getString(R.string.wifi_menu_modify))){
    						showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_MODIFY);
                        } else if(item.equals(getString(R.string.wifi_menu_forget))){
                            if(mWifiManager != null && mSelectedAccessPoint != null && mSelectedAccessPoint.isActive()){
                                mWifiManager.disconnect();
                            }
                            forget();
    					} else if(item.equals(getString(R.string.wifi_disconnect))){
    						if(mWifiManager != null){
    		            		mWifiManager.disconnect();
    		            	}
    		            	removeConnectedNetworkPreference();
    					} else if(item.equals(getString(R.string.wifi_ignore))){
    						if(mWifiManager != null && mSelectedAccessPoint != null && mSelectedAccessPoint.isActive()){
    		            		mWifiManager.disconnect();
    		            	}
    		            	forget();
    					}
    					
    				}
    	        	
    	        }).create();
    	    	
    	WindowManager.LayoutParams lp = menuDialog.getWindow().getAttributes();
        lp.gravity = Gravity.BOTTOM;
        menuDialog.show();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (mSelectedAccessPoint == null) {
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case MENU_ID_CONNECT: {
                if (mSelectedAccessPoint.isSaved()) {
                    connect(mSelectedAccessPoint.getConfig());
                } else if (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE) {
                    /** Bypass dialog for unsecured networks */
                    mSelectedAccessPoint.generateOpenNetworkConfig();
                    connect(mSelectedAccessPoint.getConfig());
                } else {
                    showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_CONNECT);
                }
                return true;
            }
            case MENU_ID_FORGET: {
                forget();
                return true;
            }
            case MENU_ID_MODIFY: {
                showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_MODIFY);
                return true;
            }
            case MENU_ID_WRITE_NFC:
                showDialog(WRITE_NFC_DIALOG_ID);
                return true;
            case MENU_ID_WIFI_GEN_QRCODE: {
                mDlgAccessPoint = mSelectedAccessPoint;
//                showDialog(WIFI_DIALOG_GENQRCODE_ID);
                createQRDialog().show();
                return true;
            }
            case MENU_ID_WIFI_DISCONNECT:
            	if(mWifiManager != null){
            		mWifiManager.disconnect();
            	}
            	removeConnectedNetworkPreference();
            	return true;
            case MENU_ID_WIFI_IGNORE:
            	if(mWifiManager != null && mSelectedAccessPoint != null && mSelectedAccessPoint.isActive()){
            		mWifiManager.disconnect();
            	}
            	forget();
            	return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
    	Log.d("timhu", "onPreferenceTreeClick preference = " + preference);
        if (preference instanceof LongPressAccessPointPreference) {
            mSelectedAccessPoint = ((LongPressAccessPointPreference) preference).getAccessPoint();
            if (mSelectedAccessPoint == null) {
                return false;
            }

            /** Bypass dialog for unsecured, unsaved, and inactive networks */
            if (mSelectedAccessPoint.getSecurity() == AccessPoint.SECURITY_NONE &&
                    !mSelectedAccessPoint.isSaved() && !mSelectedAccessPoint.isActive()) {
                //mSelectedAccessPoint.generateOpenNetworkConfig();
                //connect(mSelectedAccessPoint.getConfig());
                showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_CONNECT);
            } else if (mSelectedAccessPoint.isSaved()) {
            	if(mSelectedAccessPoint.isActive()) {
            		showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_VIEW);
            	} else{
                    connect(mSelectedAccessPoint.getConfig());
//            		mWifiManager.connect(mSelectedAccessPoint.getConfig(), mConnectListener);
            	}
            } else {
                showDialog(mSelectedAccessPoint, WifiConfigUiBase.MODE_CONNECT);
            }
//        } else if (preference == mAddPreference) {
//            onAddNetworkPressed();
        } else {
            return super.onPreferenceTreeClick(preference);
        }
        // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10682 Wi-Fi tutorial
	    if(VerizonHelpUtils.isVerizonMachine())
        	VerizonHelpUtils.DisplayTutorial(getActivity(), VerizonHelpUtils.TUTORIAL_WIFI, VerizonHelpUtils.STEP_3, null);
        // ---
        return true;
    }

    private void showDialog(AccessPoint accessPoint, int dialogMode) {
        if (accessPoint != null) {
            WifiConfiguration config = accessPoint.getConfig();
            if (isEditabilityLockedDown(getActivity(), config) && accessPoint.isActive()) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(),
                        RestrictedLockUtils.getDeviceOwner(getActivity()));
                return;
            }
        }

        if (mDialog != null) {
            removeDialog(WIFI_DIALOG_ID);
            mDialog = null;
        }

        // Save the access point and edit mode
//        mDlgAccessPoint = accessPoint;
//        mDialogMode = dialogMode;

//        showDialog(WIFI_DIALOG_ID);
        
        if(dialogMode == WifiConfigUiBase.MODE_VIEW){
        	WifiViewSettings.setSelectedAccessPoint(accessPoint);
            showSettingsFragment(WifiViewSettings.class.getCanonicalName(), 
    				R.string.wifi_add_network);
        }else{
	        WifiConnectSettings.setSelectedAccessPoint(accessPoint, dialogMode);
	        showSettingsFragment(WifiConnectSettings.class.getCanonicalName(), 
					R.string.wifi_add_network);
        }
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case WIFI_DIALOG_ID:
                AccessPoint ap = mDlgAccessPoint; // For manual launch
                if (ap == null) { // For re-launch from saved state
                    if (mAccessPointSavedState != null) {
                        ap = new AccessPoint(getActivity(), mAccessPointSavedState);
                        // For repeated orientation changes
                        mDlgAccessPoint = ap;
                        // Reset the saved access point data
                        mAccessPointSavedState = null;
                    }
                }
                // If it's null, fine, it's for Add Network
                mSelectedAccessPoint = ap;
                mDialog = new WifiDialog(getActivity(), this, ap, mDialogMode,
                        /* no hide submit/connect */ false, WifiQRCodeUtils.getApQRCodeString(mSelectedAccessPoint,mWifiManager));
                return mDialog;
            case WPS_PBC_DIALOG_ID:
                return new WpsDialog(getActivity(), WpsInfo.PBC);
            case WPS_PIN_DIALOG_ID:
                return new WpsDialog(getActivity(), WpsInfo.DISPLAY);
            case WRITE_NFC_DIALOG_ID:
                if (mSelectedAccessPoint != null) {
                    mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(
                            getActivity(), mSelectedAccessPoint.getConfig().networkId,
                            mSelectedAccessPoint.getSecurity(),
                            mWifiManager);
                } else if (mWifiNfcDialogSavedState != null) {
                    mWifiToNfcDialog = new WriteWifiConfigToNfcDialog(
                            getActivity(), mWifiNfcDialogSavedState, mWifiManager);
                }

                return mWifiToNfcDialog;
            case WIFI_DIALOG_GENQRCODE_ID:
                return createQRDialog();
            case WIFI_DIALOG_MENU_ID:
            	String[] items = new String[2];
            	items[0] = getString(R.string.advanced_settings);
            	items[1] = getString(R.string.wifi_saved_access_points_titlebar);

            	Dialog menuDialog = new AlertDialog.Builder(getActivity())
            	.setTitle(getString(R.string.wifi_menu_options))
            	.setNegativeButton(android.R.string.cancel, null)
                .setItems(items, new DialogInterface.OnClickListener(){

					@Override
					public void onClick(DialogInterface dlg, int which) {
						// TODO Auto-generated method stub
						if(which == 0){
							showSettingsFragment(ConfigureWifiSettings.class.getCanonicalName(), 
									R.string.advanced_settings);
						}else if(which == 1){
							showSettingsFragment(SavedAccessPointsWifiSettings.class.getCanonicalName(), 
									R.string.wifi_saved_access_points_titlebar);
						}
					}
                	
                }).create();
            	
            	WindowManager.LayoutParams lp = menuDialog.getWindow().getAttributes();
                lp.gravity = Gravity.BOTTOM;
                
                return menuDialog;
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
        
        String qrCodeString = WifiQRCodeUtils.getApQRCodeString(mSelectedAccessPoint, mWifiManager);
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
    
    private void showSettingsFragment(String fragmentClass, int titleRes){
    	if (getActivity() instanceof SettingsActivity) {
            ((SettingsActivity) getActivity()).startPreferencePanel(
            		fragmentClass, null,
            		titleRes, null, this, 0);
        } else {
            startFragment(this, fragmentClass,
            		titleRes, -1 /* Do not request a results */,
                    null);
        }
    }

    private synchronized LongPressAccessPointPreference addConnectedNetworkPreference(final AccessPoint accessPoint){
    	removeConnectedNetworkPreference();
    	
    	LongPressAccessPointPreference preference = new LongPressAccessPointPreference(accessPoint,
                getPrefContext(), mUserBadgeCache, false, R.drawable.ic_wifi_signal_0, this, true);
        preference.setKey(KEY_CONNECTED_NETWORK);
        preference.setOrder(1);
        
        if(accessPoint.isSaved() && !WifiQRCodeUtils.getApQRCodeString(accessPoint, mWifiManager).equals("")) {
	        preference.setLayoutResource(R.layout.asusres_preference_wifi_item_nodivider, true, new View.OnClickListener(){
	        	@Override
				public void onClick(View v) {
					//show QRcode dialog
	        		mSelectedAccessPoint = accessPoint;
	        		mDlgAccessPoint = mSelectedAccessPoint;
	        		createQRDialog().show();
				}
	        });
        }else
        	preference.setLayoutResource(R.layout.asusres_preference_wifi_item_nodivider, false, null);
        
        preference.refresh();
        mConnectedNetworkPreference = preference;
		getPreferenceScreen().addPreference(mConnectedNetworkPreference);
		
		if(mWifiSwitchPreference != null)
    		mWifiSwitchPreference.setLayoutResource(R.layout.asusres_preference_material);
    	return preference;
    }
    
    private synchronized void removeConnectedNetworkPreference(){
    	if(mConnectedNetworkPreference != null){
        	getPreferenceScreen().removePreference(mConnectedNetworkPreference);
        	mConnectedNetworkPreference = null;
    	}
    	
    	if(mWifiSwitchPreference != null)
    		mWifiSwitchPreference.setLayoutResource(R.layout.asusres_preference_material_nodivider);
    }

    private synchronized void updateConnectedNetworkPreference(AccessPoint accessPoint){
        if(mConnectedNetworkPreference == null || !accessPoint.equals(mConnectedNetworkPreference.getAccessPoint())){
            addConnectedNetworkPreference(accessPoint);
            accessPoint.setListener(this);
        }
    }

    private PreferenceGroup addAccessPointContainer(){
    	removeAccessPointContainer();
    	
    	PreferenceGroup preGroup = new PreferenceCategory(getPrefContext());
    	preGroup.setKey(KEY_WIFI_LIST_CATEGORY);
    	preGroup.setTitle(R.string.wifi_select_network);
    	preGroup.setOrder(2);
    	
    	mAccessPointContainer = preGroup;
    	getPreferenceScreen().addPreference(mAccessPointContainer);

    	return preGroup;
    }
    
    private void removeAccessPointContainer(){
        if(mAccessPointContainer != null){
        	mAccessPointContainer.removeAll();
        	getPreferenceScreen().removePreference(mAccessPointContainer);
        	mAccessPointContainer = null;
        }
    }
    
    boolean isWifiConnected(){
	    final ConnectivityManager connectivity = (ConnectivityManager)
	            getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
	    if (connectivity != null) {
	        NetworkInfo info = connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	        return info.isConnected();
	    }
	    return false;
    }
    /**
     * filter the active network
     */
    private boolean filterAccessPoints(Collection<AccessPoint> accessPoints){
        for (AccessPoint accessPoint : accessPoints) {
            if(accessPoint.getDetailedState() == DetailedState.CONNECTED){
                updateConnectedNetworkPreference(accessPoint);

                accessPoints.remove(accessPoint);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Shows the latest access points available with supplemental information like
     * the strength of network and the security for it.
     */
    @Override
    public void onAccessPointsChanged() {
        // Safeguard from some delayed event handling
        if (getActivity() == null) return;
        if (mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                addMessagePreference(R.string.wifi_empty_list_user_restricted);
            }
//            if(Utils.isVerizon()) {
//                mAccessPointContainer.removeAll();
//            }
//            else {
//              getPreferenceScreen().removeAll();
//              removeConnectedNetworkPreference();
//              removeAccessPointContainer();
//            }
            return;
        }

        //1500ms to make sure the animation rotate a round
        if(mAnimationStartTime != 0 && (System.currentTimeMillis() - mAnimationStartTime > 1000)){
        if(mButtonBar != null && mButtonBar.getButton(REFRESH_BUTTON_ID) != null){
            View icon = mButtonBar.getButton(REFRESH_BUTTON_ID).findViewById(com.asus.cncommonres.R.id.icon);
            if(icon != null)
                icon.clearAnimation();
            mAnimationStartTime = 0;
        }
        }

        final int wifiState = mWifiManager.getWifiState();

        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                // AccessPoints are automatically sorted with TreeSet.
                Collection<AccessPoint> accessPoints =
                        mWifiTracker.getAccessPoints();  //++ tim

                if(!filterAccessPoints(accessPoints))
                    removeConnectedNetworkPreference();

                removeAccessPointContainer();
                if(mAccessPointContainer == null){
                	addAccessPointContainer();
                }

                if(Utils.isVerizon()) {
                    mAccessPointContainer.removeAll();
                }
                else {
                	mAccessPointContainer.removeAll();
                }

                boolean hasAvailableAccessPoints = false;
                int index = 0;
                if(Utils.isVerizon()) {
                    cacheRemoveAllPrefs(mAccessPointContainer);
                }
                else {
                    cacheRemoveAllPrefs(mAccessPointContainer);
                }
                for (AccessPoint accessPoint : accessPoints) {
                    // Ignore access points that are out of range.
                    if (accessPoint.getLevel() != -1) {
                        String key = accessPoint.getBssid();
                        if (TextUtils.isEmpty(key)) {
                            key = accessPoint.getSsidStr();
                        }
                        hasAvailableAccessPoints = true;
                        LongPressAccessPointPreference pref = (LongPressAccessPointPreference)
                                getCachedPreference(key);
                        if (pref != null) {
                            pref.setOrder(index++);
                            continue;
                        }
                        
                        LongPressAccessPointPreference
                                preference = new LongPressAccessPointPreference(accessPoint,
                                getPrefContext(), mUserBadgeCache, false,
                                R.drawable.ic_wifi_signal_0, this, true);
                        preference.setKey(key);
                        preference.setOrder(index++);
                        
                        preference.setLayoutResource(R.layout.asusres_preference_wifi_item, false, null);

                        if (mOpenSsid != null && mOpenSsid.equals(accessPoint.getSsidStr())
                                && !accessPoint.isSaved()
                                && accessPoint.getSecurity() != AccessPoint.SECURITY_NONE) {
                        	Log.d("timhu", "onPreferenceTreeClick mOpenSsid = " + mOpenSsid);
                            onPreferenceTreeClick(preference);
                            mOpenSsid = null;
                        }
                        if(Utils.isVerizon()) {
                            mAccessPointContainer.addPreference(preference);
                        }
                        else {
                        	mAccessPointContainer.addPreference(preference); //tim++
                        }
                        accessPoint.setListener(this);
                        preference.refresh();
                    }
                }
                removeCachedPrefs(mAccessPointContainer);
                
                setPreferenceLayoutResource(mAccessPointContainer);
                if (!hasAvailableAccessPoints) {
                    setProgressBarVisible(true);
                    Preference pref = new Preference(getContext()) {
                        @Override
                        public void onBindViewHolder(PreferenceViewHolder holder) {
                            super.onBindViewHolder(holder);
                            // Show a line on each side of add network.
                            holder.setDividerAllowedBelow(true);
                        }
                    };
                    pref.setSelectable(false);
                    pref.setSummary(R.string.wifi_empty_list_wifi_on);
                    pref.setOrder(0);
                    pref.setKey(PREF_KEY_EMPTY_WIFI_LIST);
                    pref.setLayoutResource(R.layout.asusres_preference_material_nodivider);
                    if(Utils.isVerizon()) {
                        mAccessPointContainer.addPreference(pref);
//                        mAddPreference.setOrder(1);
//                        mAccessPointContainer.addPreference(mAddPreference);
                    }
                    else {
                    	mAccessPointContainer.addPreference(pref);
//                        mAddPreference.setOrder(1);
//                        mAccessPointContainer.addPreference(mAddPreference);
                    }
                } else {
//                    mAddPreference.setOrder(index++);
//                    if(Utils.isVerizon()) {
//                        mAccessPointContainer.addPreference(mAddPreference);
//                    }
//                    else {
//                    	mAccessPointContainer.addPreference(mAddPreference);
//                    }
                    setProgressBarVisible(false);
                }
                if (mScanMenuItem != null) {
                    mScanMenuItem.setEnabled(true);
                }
                break;

            case WifiManager.WIFI_STATE_ENABLING:
                if(Utils.isVerizon()) {
                    mAccessPointContainer.removeAll();
                }
                else {
                	mAccessPointContainer.removeAll();
                }
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLING:
                addMessagePreference(R.string.wifi_stopping);
                setProgressBarVisible(true);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                setProgressBarVisible(false);
                if (mScanMenuItem != null) {
                    mScanMenuItem.setEnabled(false);
                }
                break;
        }
    }

    private void setOffMessage() {
        if (isUiRestricted()) {
            if (!isUiRestrictedByOnlyAdmin()) {
                addMessagePreference(R.string.wifi_empty_list_user_restricted);
            }
            if(Utils.isVerizon()) {
                mAccessPointContainer.removeAll();
            }
            else {
            	removeConnectedNetworkPreference();
            	removeAccessPointContainer();
            }
            return;
        }

        TextView emptyTextView = getEmptyTextView();
        if (emptyTextView == null) {
            return;
        }

        final CharSequence briefText = getText(R.string.wifi_empty_list_wifi_off);

        // Don't use WifiManager.isScanAlwaysAvailable() to check the Wi-Fi scanning mode. Instead,
        // read the system settings directly. Because when the device is in Airplane mode, even if
        // Wi-Fi scanning mode is on, WifiManager.isScanAlwaysAvailable() still returns "off".
        final ContentResolver resolver = getActivity().getContentResolver();
        final boolean wifiScanningMode = Settings.Global.getInt(
                resolver, Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, 0) == 1;

        if (!wifiScanningMode) {
            // Show only the brief text if the user is not allowed to configure scanning settings,
            // or the scanning mode has been turned off.
            emptyTextView.setText(briefText, BufferType.SPANNABLE);
        } else {
            // Append the description of scanning settings with link.
            final StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append(briefText);
            contentBuilder.append("\n\n");
            contentBuilder.append(getText(R.string.wifi_scan_notify_text));
            LinkifyUtils.linkify(emptyTextView, contentBuilder, new LinkifyUtils.OnClickListener() {
                @Override
                public void onClick() {
                    final SettingsActivity activity =
                            (SettingsActivity) WifiSettings.this.getActivity();
                    activity.startPreferencePanel(ScanningSettings.class.getName(), null,
                            R.string.location_scanning_screen_title, null, null, 0);
                }
            });
        }
        // Embolden and enlarge the brief description anyway.
        Spannable boldSpan = (Spannable) emptyTextView.getText();
        boldSpan.setSpan(
                new TextAppearanceSpan(getActivity(), android.R.style.TextAppearance_Medium), 0,
                briefText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if(Utils.isVerizon()) {
            mAccessPointContainer.removeAll();
        }
        else {
        	removeConnectedNetworkPreference();
        	removeAccessPointContainer();
        }
    }

    private void addMessagePreference(int messageId) {
        TextView emptyTextView = getEmptyTextView();
        if (emptyTextView != null) emptyTextView.setText(messageId);
        if(Utils.isVerizon()) {
            mAccessPointContainer.removeAll();
        }
        else {
        	removeConnectedNetworkPreference();
        	removeAccessPointContainer();
        }
    }

    protected void setProgressBarVisible(boolean visible) {
        if (mProgressHeader != null) {
            mProgressHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onWifiStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_STATE_ENABLING:
                addMessagePreference(R.string.wifi_starting);
                setProgressBarVisible(true);
//                changeAsusButtonBarButtonState(true);
                break;
                
            case WifiManager.WIFI_STATE_ENABLED:
                changeAsusButtonBarButtonState(true);
                break;

            case WifiManager.WIFI_STATE_DISABLED:
                setOffMessage();
                setProgressBarVisible(false);
                changeAsusButtonBarButtonState(false);
                break;
        }
    }
    
    private void changeAsusButtonBarButtonState(boolean enabled) {
    	if(mButtonBar != null){
	    	mButtonBar.getButton(REFRESH_BUTTON_ID).setEnabled(enabled);
	    	mButtonBar.getButton(ADD_BUTTON_ID).setEnabled(enabled);
	    	mButtonBar.getButton(QRCODE_BUTTON_ID).setEnabled(enabled);
            mButtonBar.getButton(MENU_BUTTON_ID).setEnabled(enabled);
    	}
    }
    
    private void setPreferenceLayoutResource(PreferenceGroup group){
    	if(group == null) return;
    	
		int count = group.getPreferenceCount();
        if(count > 0){
        	for(int i = 0; i < count-1; i++){
        		Preference p = group.getPreference(i);
        		if(p instanceof LongPressAccessPointPreference)
        			p.setLayoutResource(R.layout.asusres_preference_wifi_item);
        	}
        	
        	Preference p = group.getPreference(count-1);
    		if(p instanceof LongPressAccessPointPreference)
    			p.setLayoutResource(R.layout.asusres_preference_wifi_item_nodivider);
        }
	}

    @Override
    public void onConnectedChanged() {
        changeNextButtonState(mWifiTracker.isConnected());
    }

    /**
     * Renames/replaces "Next" button when appropriate. "Next" button usually exists in
     * Wifi setup screens, not in usual wifi settings screen.
     *
     * @param enabled true when the device is connected to a wifi network.
     */
    private void changeNextButtonState(boolean enabled) {
        if (mEnableNextOnConnection && hasNextButton()) {
            getNextButton().setEnabled(enabled);
        }
    }

    @Override
    public void onForget(WifiDialog dialog) {
        forget();
    }

    @Override
    public void onSubmit(WifiDialog dialog) {
        if (mDialog != null) {
            submit(mDialog.getController());
        }
    }

    /* package */ void submit(WifiConfigController configController) {

        final WifiConfiguration config = configController.getConfig();

        if (config == null) {
            if (mSelectedAccessPoint != null
                    && mSelectedAccessPoint.isSaved()) {
                connect(mSelectedAccessPoint.getConfig());
            }
        } else if (configController.getMode() == WifiConfigUiBase.MODE_MODIFY) {
            mWifiManager.save(config, mSaveListener);
        } else {
            mWifiManager.save(config, mSaveListener);
            if (mSelectedAccessPoint != null) { // Not an "Add network"
                connect(config);
            }
        }

        mWifiTracker.resumeScanning();
    }

    /* package */ void forget() {
        MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_FORGET);
        
        //tim++
        if(mConnectedNetworkPreference != null && mSelectedAccessPoint.equals(mConnectedNetworkPreference.getAccessPoint())){
        	removeConnectedNetworkPreference();
        }
        if (!mSelectedAccessPoint.isSaved()) {
            if (mSelectedAccessPoint.getNetworkInfo() != null &&
                    mSelectedAccessPoint.getNetworkInfo().getState() != State.DISCONNECTED) {
                // Network is active but has no network ID - must be ephemeral.
                mWifiManager.disableEphemeralNetwork(
                        AccessPoint.convertToQuotedString(mSelectedAccessPoint.getSsidStr()));
            } else {
                // Should not happen, but a monkey seems to trigger it
                Log.e(TAG, "Failed to forget invalid network " + mSelectedAccessPoint.getConfig());
                return;
            }
        } else {
            mWifiManager.forget(mSelectedAccessPoint.getConfig().networkId, mForgetListener);
        }

        mWifiTracker.resumeScanning();

        // We need to rename/replace "Next" button in wifi setup context.
        changeNextButtonState(false);
    }

    protected void connect(final WifiConfiguration config) {
        MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_CONNECT);
        //WifiConfigManager.updateNetwork(mWifiManager, config );
        mWifiManager.connect(config, mConnectListener);
    }

    protected void connect(final int networkId) {
        MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_CONNECT);
        mWifiManager.connect(networkId, mConnectListener);
    }

    /**
     * Called when "add network" button is pressed.
     */
    /* package */ void onAddNetworkPressed() {
        MetricsLogger.action(getActivity(), MetricsEvent.ACTION_WIFI_ADD_NETWORK);
        // No exact access point is selected.
        mSelectedAccessPoint = null;
        showDialog(null, WifiConfigUiBase.MODE_CONNECT);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_wifi;
    }

    @Override
    public void onAccessPointChanged(final AccessPoint accessPoint) {
        View view = getView();
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    Object tag = accessPoint.getTag();
                    if (tag != null) {
                        LongPressAccessPointPreference tagPreference = (LongPressAccessPointPreference) tag;
                        //tagPreference.setAccessPoint(accessPoint);
                        tagPreference.refresh();

                        if (accessPoint.getDetailedState() == DetailedState.CONNECTED) {
                            updateConnectedNetworkPreference(accessPoint);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onLevelChanged(final AccessPoint accessPoint) {
        View view = getView();
        if (view != null) {
            view.post(new Runnable() {
                @Override
                public void run() {
                    Object tag = accessPoint.getTag();
                    if (tag != null) {
                        ((LongPressAccessPointPreference) tag).onLevelChanged();
                    }
                }
            });
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<>();
                final Resources res = context.getResources();

                // Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.wifi_settings);
                data.screenTitle = res.getString(R.string.wifi_settings);
                data.keywords = res.getString(R.string.keywords_wifi);
                result.add(data);

                // Add saved Wi-Fi access points
                final Collection<AccessPoint> accessPoints =
                        WifiTracker.getCurrentAccessPoints(context, true, false, false);
                for (AccessPoint accessPoint : accessPoints) {
                    data = new SearchIndexableRaw(context);
                    data.title = accessPoint.getSsidStr();
                    data.screenTitle = res.getString(R.string.wifi_settings);
                    data.enabled = enabled;
                    result.add(data);
                }

                return result;
            }
        };

    /**
     * Returns true if the config is not editable through Settings.
     * @param context Context of caller
     * @param config The WiFi config.
     * @return true if the config is not editable through Settings.
     */
    static boolean isEditabilityLockedDown(Context context, WifiConfiguration config) {
        return !canModifyNetwork(context, config);
    }

    /**
     * This method is a stripped version of WifiConfigStore.canModifyNetwork.
     * TODO: refactor to have only one method.
     * @param context Context of caller
     * @param config The WiFi config.
     * @return true if Settings can modify the config.
     */
    static boolean canModifyNetwork(Context context, WifiConfiguration config) {
        if (config == null) {
            return true;
        }

        final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);

        // Check if device has DPM capability. If it has and dpm is still null, then we
        // treat this case with suspicion and bail out.
        final PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN) && dpm == null) {
            return false;
        }

        boolean isConfigEligibleForLockdown = false;
        if (dpm != null) {
            final ComponentName deviceOwner = dpm.getDeviceOwnerComponentOnAnyUser();
            if (deviceOwner != null) {
                final int deviceOwnerUserId = dpm.getDeviceOwnerUserId();
                try {
                    final int deviceOwnerUid = pm.getPackageUidAsUser(deviceOwner.getPackageName(),
                            deviceOwnerUserId);
                    isConfigEligibleForLockdown = deviceOwnerUid == config.creatorUid;
                } catch (NameNotFoundException e) {
                    // don't care
                }
            }
        }
        if (!isConfigEligibleForLockdown) {
            return true;
        }

        final ContentResolver resolver = context.getContentResolver();
        final boolean isLockdownFeatureEnabled = Settings.Global.getInt(resolver,
                Settings.Global.WIFI_DEVICE_OWNER_CONFIGS_LOCKDOWN, 0) != 0;
        return !isLockdownFeatureEnabled;
    }
        private BroadcastReceiver mAsusRouterReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(final Context context, Intent intent) {
			NetworkInfo network = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(network != null && network.isConnected() &&
                !mPreferneces.getBoolean(ASUS_ROUTER_DIALOG_ONESHOT, false)) {
				Log.d(TAG, "onReceive(): " + intent.getAction());
//			    new DeviceDiscoverTask(context).execute(1000);
             }
         }
     };
//
//	class DeviceDiscoverTask extends AsyncTask<Integer, String, Integer> {
//	    private Context mContext;
//	    ArrayList<DUT_PKT_GET_INFO> infos = new ArrayList<DUT_PKT_GET_INFO>();
//		public DeviceDiscoverTask(Context pContext) {
//		    mContext = pContext;
//		}
//
//		@Override
//		protected Integer doInBackground(Integer... params) {
//			Log.d(TAG, "doInBackground()");
//			return deviceDiscover(params[0], infos);
//		}
//
//		@Override
//		protected void onPostExecute(Integer result) {
//		    if(result.intValue() == DUT_RESULT_SUCCESS && !infos.isEmpty()) {
//				Log.d(TAG, "This is Asus Router!!!!!");
//				Log.d(TAG, "infos.isEmpty() : " + infos.isEmpty());
//				if(infos == null) Log.d(TAG, "infos == null");
//				else Log.d(TAG, "infos != null");
//				for(DUT_PKT_GET_INFO info : infos) {
//					Log.d(TAG, "This is Asus Router, SSID: " + info.SSID + ", ProductID: " + info.ProductID);
//				}
//				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//				builder.setTitle(R.string.asus_router_title)
//					.setMessage(R.string.asus_router_Message)
//					.setPositiveButton(R.string.asus_router_Message_Download, new DialogInterface.OnClickListener() {
//						@Override
//						public void onClick(DialogInterface pDialog, int which) {
//							try {
//								startActivity(new Intent(
//									Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ASUS_ROUTER_PACKAGE_NAME)));
//							} catch (android.content.ActivityNotFoundException e) {
//								startActivity(new Intent(
//									Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + ASUS_ROUTER_PACKAGE_NAME)));
//							}
//						}
//					})
//					.setNegativeButton(R.string.asus_router_Message_NO, null)
//					.setCancelable(false);
//				mAlertDialog = builder.create();
//				mAlertDialog.show();
//				SharedPreferences.Editor editor = mPreferneces.edit();
//				editor.putBoolean(ASUS_ROUTER_DIALOG_ONESHOT, true);
//				editor.commit();
//			} else {
//				Log.d(TAG, "This is not Asus Router...");
//			}
//		}
//	}

    //fix SettingsLib update
    public void onStartScan() {}
    public void onStopScan() {}

    private static class SummaryProvider extends BroadcastReceiver
            implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final WifiManager mWifiManager;
        private final WifiStatusTracker mWifiTracker;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mWifiManager = context.getSystemService(WifiManager.class);
            mWifiTracker = new WifiStatusTracker(mWifiManager);
        }

        private CharSequence getSummary() {
            if (!mWifiTracker.enabled) {
                return mContext.getString(R.string.wifi_disabled_generic);
            }
            if (!mWifiTracker.connected) {
                return mContext.getString(R.string.disconnected);
            }
            return mWifiTracker.ssid;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
                filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
                mSummaryLoader.registerReceiver(this, filter);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mWifiTracker.handleBroadcast(intent);
            mSummaryLoader.setSummary(this, getSummary());
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
}
