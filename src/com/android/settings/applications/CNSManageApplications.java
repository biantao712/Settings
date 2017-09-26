/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.icu.text.AlphabeticIndex;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.LocaleList;
import android.os.UserHandle;
import android.os.UserManager;
import android.preference.PreferenceFrameLayout;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.AppHeader;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Settings.AllApplicationsActivity;
import com.android.settings.Settings.DomainsURLsAppListActivity;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.Settings.NotificationAppListActivity;
import com.android.settings.Settings.OverlaySettingsActivity;
import com.android.settings.Settings.StorageUseActivity;
import com.android.settings.Settings.UsageAccessSettingsActivity;
import com.android.settings.Settings.WriteSettingsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.AppStateAppOpsBridge.PermissionState;
import com.android.settings.applications.AppStateUsageBridge.UsageState;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.fuelgauge.HighPowerDetail;
import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.notification.NotificationBackend.AppRow;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.applications.ApplicationsState.CompoundFilter;
import com.android.settingslib.applications.ApplicationsState.VolumeFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import android.widget.Switch;
import android.app.admin.DevicePolicyManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.widget.CompoundButton;
import android.widget.RelativeLayout; 
import com.asus.cncommonres.AsusButtonBar;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class CNSManageApplications extends InstrumentedFragment
        implements OnItemClickListener, OnItemSelectedListener{

    static final String TAG = "CNSManageApplications";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    // Intent extras.
    public static final String EXTRA_CLASSNAME = "classname";
    // Used for storage only.
    public static final String EXTRA_VOLUME_UUID = "volumeUuid";
    public static final String EXTRA_VOLUME_NAME = "volumeName";

    private static final String EXTRA_SORT_ORDER = "sortOrder";
    private static final String EXTRA_SHOW_SYSTEM = "showSystem";
    private static final String EXTRA_HAS_ENTRIES = "hasEntries";
    private static final String EXTRA_HAS_BRIDGE = "hasBridge";

    //TODO: need to implement new list type and filter for this blacklist violation case
    //public static final String EXTRA_BLKLIST_VIOLATION = "blackListViolation"

    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String APP_CHG = "chg";

    // constant value that can be used to check return code from sub activity.
    private static final int INSTALLED_APP_DETAILS = 1;
    private static final int ADVANCED_SETTINGS = 2;

    //BEGIN:Steve_Ke@asus.com
    private static final int DOZE_WHITE_LIST = 3;
    //END:Steve_Ke@asus.com

    public static final int SIZE_TOTAL = 0;
    public static final int SIZE_INTERNAL = 1;
    public static final int SIZE_EXTERNAL = 2;

    // Filter options used for displayed list of applications
    // The order which they appear is the order they will show when spinner is present.
    public static final int FILTER_APPS_POWER_WHITELIST = 0;
    public static final int FILTER_APPS_POWER_WHITELIST_ALL = 1;
    public static final int FILTER_APPS_ALL = 2;
    public static final int FILTER_APPS_ENABLED = 3;
    public static final int FILTER_APPS_DISABLED = 4;
    public static final int FILTER_APPS_BLOCKED = 5;
    public static final int FILTER_APPS_SILENT = 6;
    public static final int FILTER_APPS_SENSITIVE = 7;
    public static final int FILTER_APPS_HIDE_NOTIFICATIONS = 8;
    public static final int FILTER_APPS_PRIORITY = 9;
    public static final int FILTER_APPS_PERSONAL = 10;
    public static final int FILTER_APPS_WORK = 11;
    public static final int FILTER_APPS_WITH_DOMAIN_URLS = 12;
    public static final int FILTER_APPS_USAGE_ACCESS = 13;
    public static final int FILTER_APPS_WITH_OVERLAY = 14;
    public static final int FILTER_APPS_WRITE_SETTINGS = 15;

    // This is the string labels for the filter modes above, the order must be kept in sync.
    public static final int[] FILTER_LABELS = new int[]{
            R.string.high_power_filter_on, // High power whitelist, on
            R.string.filter_all_apps,      // Without disabled until used
            R.string.filter_all_apps,      // All apps
            R.string.filter_enabled_apps,  // Enabled
            R.string.filter_apps_disabled, // Disabled
            R.string.filter_notif_blocked_apps,   // Blocked Notifications
            R.string.filter_notif_silent,    // Silenced Notifications
            R.string.filter_notif_sensitive_apps, // Sensitive Notifications
            R.string.filter_notif_hide_notifications_apps, // Sensitive Notifications
            R.string.filter_notif_priority_apps,  // Priority Notifications
            R.string.filter_personal_apps, // Personal
            R.string.filter_work_apps,     // Work
            R.string.filter_with_domain_urls_apps,     // Domain URLs
            R.string.filter_all_apps,      // Usage access screen, never displayed
            R.string.filter_overlay_apps,   // Apps with overlay permission
            R.string.filter_write_settings_apps,   // Apps that can write system settings
    };
    // This is the actual mapping to filters from FILTER_ constants above, the order must
    // be kept in sync.
    public static final AppFilter[] FILTERS = new AppFilter[]{
            new CompoundFilter(AppStatePowerBridge.FILTER_POWER_WHITELISTED,
                    ApplicationsState.FILTER_ALL_ENABLED),     // High power whitelist, on
            new CompoundFilter(ApplicationsState.FILTER_WITHOUT_DISABLED_UNTIL_USED,
                    ApplicationsState.FILTER_ALL_ENABLED),     // Without disabled until used
            ApplicationsState.FILTER_EVERYTHING,  // All apps
            ApplicationsState.FILTER_ALL_ENABLED, // Enabled
            ApplicationsState.FILTER_DISABLED,    // Disabled
            AppStateNotificationBridge.FILTER_APP_NOTIFICATION_BLOCKED,   // Blocked Notifications
            AppStateNotificationBridge.FILTER_APP_NOTIFICATION_SILENCED,   // Silenced Notifications
            AppStateNotificationBridge.FILTER_APP_NOTIFICATION_HIDE_SENSITIVE, // Sensitive Notifications
            AppStateNotificationBridge.FILTER_APP_NOTIFICATION_HIDE_ALL, // Hide all Notifications
            AppStateNotificationBridge.FILTER_APP_NOTIFICATION_PRIORITY,  // Priority Notifications
            ApplicationsState.FILTER_PERSONAL,    // Personal
            ApplicationsState.FILTER_WORK,        // Work
            ApplicationsState.FILTER_WITH_DOMAIN_URLS,   // Apps with Domain URLs
            AppStateUsageBridge.FILTER_APP_USAGE, // Apps with Domain URLs
            AppStateOverlayBridge.FILTER_SYSTEM_ALERT_WINDOW,   // Apps that can draw overlays
            AppStateWriteSettingsBridge.FILTER_WRITE_SETTINGS,  // Apps that can write system settings
    };

    // sort order
    private int mSortOrder = R.id.sort_order_alpha;

    // whether showing system apps.
    private boolean mShowSystem;

    private ApplicationsState mApplicationsState;

    public int mListType;
    public int mFilter;

    public ApplicationsAdapter mApplications;

    private View mLoadingContainer;

    private View mListContainer;

    // ListView used to display list
    private ListView mListView;

    // Size resource used for packages whose size computation failed for some reason
    CharSequence mInvalidSizeStr;

    // layout inflater object used to inflate views
    private LayoutInflater mInflater;

    private String mCurrentPkgName;
    private int mCurrentUid;
    private boolean mFinishAfterDialog;

    private Menu mOptionsMenu;

    public static final int LIST_TYPE_MAIN = 0;
    public static final int LIST_TYPE_NOTIFICATION = 1;
    public static final int LIST_TYPE_DOMAINS_URLS = 2;
    public static final int LIST_TYPE_STORAGE = 3;
    public static final int LIST_TYPE_USAGE_ACCESS = 4;
    public static final int LIST_TYPE_HIGH_POWER = 5;
    public static final int LIST_TYPE_OVERLAY = 6;
    public static final int LIST_TYPE_WRITE_SETTINGS = 7;

    private View mRootView;

    private View mSpinnerHeader;
    private Spinner mFilterSpinner;
    private FilterSpinnerAdapter mFilterAdapter;
    private NotificationBackend mNotifBackend;
    private ResetAppsHelper mResetAppsHelper;
    private String mVolumeUuid;
    private String mVolumeName;

    //+++ suleman
    private Switch mSwitch;
    //--- suleman
    
    //+++ tim
    private int mSingleChoiceItems = 0;
    private TextView mHeaderTitle;
    //---

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());

        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        String className = args != null ? args.getString(EXTRA_CLASSNAME) : null;
        if (className == null) {
            className = intent.getComponent().getClassName();
        }
        if (className.equals(AllApplicationsActivity.class.getName())) {
            mShowSystem = true;
        } else if (className.equals(NotificationAppListActivity.class.getName())) {
            mListType = LIST_TYPE_NOTIFICATION;
            mNotifBackend = new NotificationBackend();
        } else if (className.equals(DomainsURLsAppListActivity.class.getName())) {
            mListType = LIST_TYPE_DOMAINS_URLS;
        } else if (className.equals(StorageUseActivity.class.getName())) {
            if (args != null && args.containsKey(EXTRA_VOLUME_UUID)) {
                mVolumeUuid = args.getString(EXTRA_VOLUME_UUID);
                mVolumeName = args.getString(EXTRA_VOLUME_NAME);
                mListType = LIST_TYPE_STORAGE;
            } else {
                // No volume selected, display a normal list, sorted by size.
                mListType = LIST_TYPE_MAIN;
            }
            mSortOrder = R.id.sort_order_size;
        } else if (className.equals(UsageAccessSettingsActivity.class.getName())) {
            mListType = LIST_TYPE_USAGE_ACCESS;
        } else if (className.equals(HighPowerApplicationsActivity.class.getName())) {
            mListType = LIST_TYPE_HIGH_POWER;
            // Default to showing system.
            mShowSystem = true;
        } else if (className.equals(OverlaySettingsActivity.class.getName())) {
            mListType = LIST_TYPE_OVERLAY;
        } else if (className.equals(WriteSettingsActivity.class.getName())) {
            mListType = LIST_TYPE_WRITE_SETTINGS;
        } else {
            mListType = LIST_TYPE_MAIN;
            mSortOrder = R.id.sort_order_size;
        }
        mFilter = getDefaultFilter();

        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt(EXTRA_SORT_ORDER, mSortOrder);
            mShowSystem = savedInstanceState.getBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
        }

        mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);

        mResetAppsHelper = new ResetAppsHelper(getActivity());
    }
    
    //+++ tim
    private void initButtonBar(){
		final SettingsActivity activity = (SettingsActivity) getActivity();
		AsusButtonBar mButtonBar = activity.getButtonBar();
		if(mButtonBar != null) {
			mButtonBar.setVisibility(View.VISIBLE);

			mButtonBar.addButton(1, R.drawable.asus_icon_batch_setting, getString(R.string.high_power_apps_optimize));
			mButtonBar.getButton(1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	String[] items = new String[] {
                			getString(R.string.high_power_apps_optimize_all),
                			getString(R.string.high_power_apps_optimize_none)
                			};
                	
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getContext())
                            .setSingleChoiceItems(items, mSingleChoiceItems, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO Auto-generated method stub

                                    switch (which){
                                        case 0:
                                        	if(mApplications != null)
                                        		mApplications.setAll(false);
                                            break;
                                        case 1:
                                        	if(mApplications != null)
                                        		mApplications.setAll(true);
                                            break;
                                    }
                                    mSingleChoiceItems = which;
                                    dialog.dismiss();
                                }
                            })
                            .setTitle(getString(R.string.high_power_apps_optimize))
                            .setNegativeButton(getString(android.R.string.cancel), null);
                    
                    AlertDialog dialog = alertDialog.create();
                    Window window = dialog.getWindow();
                    window.setGravity(Gravity.BOTTOM);
                    dialog.show();
                }
             });
        }
	}
    //---

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // initialize the inflater
        mInflater = inflater;

        mRootView = inflater.inflate(R.layout.manage_applications_apps, null);
        mLoadingContainer = mRootView.findViewById(R.id.loading_container);
        mLoadingContainer.setVisibility(View.VISIBLE);
        mListContainer = mRootView.findViewById(R.id.list_container);
        if (mListContainer != null) {
            // Create adapter and list view here
            View emptyView = mListContainer.findViewById(com.android.internal.R.id.empty);
            ListView lv = (ListView) mListContainer.findViewById(android.R.id.list);
            if (emptyView != null) {
                lv.setEmptyView(emptyView);
            }
            lv.setOnItemClickListener(this);
            lv.setSaveEnabled(true);
            lv.setItemsCanFocus(true);
            lv.setTextFilterEnabled(true);
            mListView = lv;
            mApplications = new ApplicationsAdapter(mApplicationsState, this, mFilter);
            if (savedInstanceState != null) {
                mApplications.mHasReceivedLoadEntries =
                        savedInstanceState.getBoolean(EXTRA_HAS_ENTRIES, false);
                mApplications.mHasReceivedBridgeCallback =
                        savedInstanceState.getBoolean(EXTRA_HAS_BRIDGE, false);
		mApplications.resume(mSortOrder);
            }
            mListView.setAdapter(mApplications);
            mListView.setRecyclerListener(mApplications);
            mListView.setFastScrollEnabled(isFastScrollEnabled());
            mListView.setFooterDividersEnabled(false);

            Utils.prepareCustomPreferencesList(container, mRootView, mListView, false);
        }

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) mRootView.getLayoutParams()).removeBorders = true;
        }

        createHeader();

        mResetAppsHelper.onRestoreInstanceState(savedInstanceState);
        
        //+++ tim
        if(mListType == LIST_TYPE_HIGH_POWER)
        	initButtonBar();
        //---

        return mRootView;
    }

    private void createHeader() {
        Activity activity = getActivity();
        FrameLayout pinnedHeader = (FrameLayout) mRootView.findViewById(R.id.pinned_header);
        mSpinnerHeader = (ViewGroup) activity.getLayoutInflater()
                .inflate(R.layout.apps_filter_spinner, pinnedHeader, false);
        mFilterSpinner = (Spinner) mSpinnerHeader.findViewById(R.id.filter_spinner);
        mFilterAdapter = new FilterSpinnerAdapter(this);
        mFilterSpinner.setAdapter(mFilterAdapter);
        mFilterSpinner.setOnItemSelectedListener(this);
        pinnedHeader.addView(mSpinnerHeader, 0);

        mFilterAdapter.enableFilter(getDefaultFilter());
        if (mListType == LIST_TYPE_MAIN) {
            if (UserManager.get(getActivity()).getUserProfiles().size() > 1) {
                mFilterAdapter.enableFilter(FILTER_APPS_PERSONAL);
                mFilterAdapter.enableFilter(FILTER_APPS_WORK);
            }
        }
        if (mListType == LIST_TYPE_NOTIFICATION) {
            mFilterAdapter.enableFilter(FILTER_APPS_BLOCKED);
            mFilterAdapter.enableFilter(FILTER_APPS_SILENT);
            mFilterAdapter.enableFilter(FILTER_APPS_SENSITIVE);
            mFilterAdapter.enableFilter(FILTER_APPS_HIDE_NOTIFICATIONS);
            mFilterAdapter.enableFilter(FILTER_APPS_PRIORITY);
        }
        if (mListType == LIST_TYPE_HIGH_POWER) {
//            mFilterAdapter.enableFilter(FILTER_APPS_POWER_WHITELIST_ALL);
        	
        	//+++ tim
        	View headerTitleView = (ViewGroup) activity.getLayoutInflater()
                    .inflate(R.layout.apps_filter_title, pinnedHeader, false);
        	mHeaderTitle = (TextView) headerTitleView.findViewById(android.R.id.title);
        	pinnedHeader.addView(headerTitleView, 1);
        	//---
        }
        if (mListType == LIST_TYPE_STORAGE) {
            mApplications.setOverrideFilter(new VolumeFilter(mVolumeUuid));
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mListType == LIST_TYPE_STORAGE) {
            FrameLayout pinnedHeader = (FrameLayout) mRootView.findViewById(R.id.pinned_header);
            AppHeader.createAppHeader(getActivity(), null, mVolumeName, null, -1, pinnedHeader);
        }

        //BEGIN:Steve_Ke@asus.com
        PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
        backend.createDozeModeBackend(getContext());
        backend.setManageApplication(this, DOZE_WHITE_LIST);
        //END:Steve_Ke@asus.com
    }

    private int getDefaultFilter() {
        switch (mListType) {
            case LIST_TYPE_DOMAINS_URLS:
                return FILTER_APPS_WITH_DOMAIN_URLS;
            case LIST_TYPE_USAGE_ACCESS:
                return FILTER_APPS_USAGE_ACCESS;
            case LIST_TYPE_HIGH_POWER:
                return FILTER_APPS_POWER_WHITELIST_ALL;  //FILTER_APPS_POWER_WHITELIST;
            case LIST_TYPE_OVERLAY:
                return FILTER_APPS_WITH_OVERLAY;
            case LIST_TYPE_WRITE_SETTINGS:
                return FILTER_APPS_WRITE_SETTINGS;
            default:
                return FILTER_APPS_ALL;
        }
    }

    private boolean isFastScrollEnabled() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
            case LIST_TYPE_NOTIFICATION:
            case LIST_TYPE_STORAGE:
                return mSortOrder == R.id.sort_order_alpha;
            default:
                return false;
        }
    }

    @Override
    protected int getMetricsCategory() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
                return MetricsEvent.MANAGE_APPLICATIONS;
            case LIST_TYPE_NOTIFICATION:
                return MetricsEvent.MANAGE_APPLICATIONS_NOTIFICATIONS;
            case LIST_TYPE_DOMAINS_URLS:
                return MetricsEvent.MANAGE_DOMAIN_URLS;
            case LIST_TYPE_STORAGE:
                return MetricsEvent.APPLICATIONS_STORAGE_APPS;
            case LIST_TYPE_USAGE_ACCESS:
                return MetricsEvent.USAGE_ACCESS;
            case LIST_TYPE_HIGH_POWER:
                return MetricsEvent.APPLICATIONS_HIGH_POWER_APPS;
            case LIST_TYPE_OVERLAY:
                return MetricsEvent.SYSTEM_ALERT_WINDOW_APPS;
            case LIST_TYPE_WRITE_SETTINGS:
                return MetricsEvent.SYSTEM_ALERT_WINDOW_APPS;
            default:
                return MetricsEvent.VIEW_UNKNOWN;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
        updateOptionsMenu();
        if (mApplications != null) {
            mApplications.resume(mSortOrder);
            mApplications.updateLoading();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mResetAppsHelper.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SORT_ORDER, mSortOrder);
        outState.putBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
        outState.putBoolean(EXTRA_HAS_ENTRIES, mApplications.mHasReceivedLoadEntries);
        outState.putBoolean(EXTRA_HAS_BRIDGE, mApplications.mHasReceivedBridgeCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mApplications != null) {
            mApplications.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mResetAppsHelper.stop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mApplications != null) {
            mApplications.release();
        }
        mRootView = null;

        //BEGIN:Steve_Ke@asus.com
        PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
        backend.createDozeModeBackend(getContext());
        backend.releaseManageApplication();
        //END:Steve_Ke@asus.com

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == INSTALLED_APP_DETAILS && mCurrentPkgName != null) {
            if (mListType == LIST_TYPE_NOTIFICATION) {
                mApplications.mExtraInfoBridge.forceUpdate(mCurrentPkgName, mCurrentUid);
            } else if (mListType == LIST_TYPE_HIGH_POWER || mListType == LIST_TYPE_OVERLAY
                    || mListType == LIST_TYPE_WRITE_SETTINGS) {
                if (mFinishAfterDialog) {
                    getActivity().onBackPressed();
                } else {
                    mApplications.mExtraInfoBridge.forceUpdate(mCurrentPkgName, mCurrentUid);
                }
            } else {
                mApplicationsState.requestSize(mCurrentPkgName, UserHandle.getUserId(mCurrentUid));
            }
        }

        //BEGIN:Steve_Ke@asus.com
        if (requestCode == DOZE_WHITE_LIST) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            ft.detach(this).attach(this).commitAllowingStateLoss();
        }
        //END:Steve_Ke@asus.com
    }

    // utility method used to start sub activity
    private void startApplicationDetailsActivity() {
        switch (mListType) {
            case LIST_TYPE_NOTIFICATION:
                startAppInfoFragment(AppNotificationSettings.class,
                        R.string.app_notifications_title);
                break;
            case LIST_TYPE_DOMAINS_URLS:
                startAppInfoFragment(AppLaunchSettings.class, R.string.auto_launch_label);
                break;
            case LIST_TYPE_USAGE_ACCESS:
                startAppInfoFragment(UsageAccessDetails.class, R.string.usage_access);
                break;
            case LIST_TYPE_STORAGE:
                startAppInfoFragment(AppStorageSettings.class, R.string.storage_settings);
                break;
            case LIST_TYPE_HIGH_POWER:
                HighPowerDetail.show(this, mCurrentPkgName, INSTALLED_APP_DETAILS,
                        mFinishAfterDialog);
                break;
            case LIST_TYPE_OVERLAY:
                startAppInfoFragment(DrawOverlayDetails.class, R.string.overlay_settings);
                break;
            case LIST_TYPE_WRITE_SETTINGS:
                startAppInfoFragment(WriteSettingsDetails.class, R.string.write_system_settings);
                break;
            // TODO: Figure out if there is a way where we can spin up the profile's settings
            // process ahead of time, to avoid a long load of data when user clicks on a managed app.
            // Maybe when they load the list of apps that contains managed profile apps.
            default:
                startAppInfoFragment(InstalledAppDetails.class, R.string.application_info_label);
                break;
        }
    }

    private void startAppInfoFragment(Class<?> fragment, int titleRes) {
        AppInfoBase.startAppInfoFragment(fragment, titleRes, mCurrentPkgName, mCurrentUid, this,
                INSTALLED_APP_DETAILS);
    }
/*
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mListType == LIST_TYPE_DOMAINS_URLS) {
            return;
        }
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, mListType == LIST_TYPE_MAIN
                ? R.string.help_uri_apps : R.string.help_uri_notifications, getClass().getName());
        mOptionsMenu = menu;
        inflater.inflate(R.menu.manage_apps, menu);
        updateOptionsMenu();
    }
*/
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        updateOptionsMenu();
    }

    @Override
    public void onDestroyOptionsMenu() {
        mOptionsMenu = null;
    }

    void updateOptionsMenu() {
        if (mOptionsMenu == null) {
            return;
        }
        mOptionsMenu.findItem(R.id.advanced).setVisible(
                mListType == LIST_TYPE_MAIN || mListType == LIST_TYPE_NOTIFICATION);

        mOptionsMenu.findItem(R.id.sort_order_alpha).setVisible(mListType == LIST_TYPE_STORAGE
                && mSortOrder != R.id.sort_order_alpha);
        mOptionsMenu.findItem(R.id.sort_order_size).setVisible(mListType == LIST_TYPE_STORAGE
                && mSortOrder != R.id.sort_order_size);

        mOptionsMenu.findItem(R.id.show_system).setVisible(!mShowSystem
                && mListType != LIST_TYPE_HIGH_POWER);
        mOptionsMenu.findItem(R.id.hide_system).setVisible(mShowSystem
                && mListType != LIST_TYPE_HIGH_POWER);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        switch (item.getItemId()) {
            case R.id.sort_order_alpha:
            case R.id.sort_order_size:
                mSortOrder = menuId;
                mListView.setFastScrollEnabled(isFastScrollEnabled());
                if (mApplications != null) {
                    mApplications.rebuild(mSortOrder);
                }
                break;
            case R.id.show_system:
            case R.id.hide_system:
                mShowSystem = !mShowSystem;
                mApplications.rebuild(false);
                break;
            case R.id.reset_app_preferences:
                mResetAppsHelper.buildResetDialog();
                return true;
            case R.id.advanced:
                if (mListType == LIST_TYPE_NOTIFICATION) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            ConfigureNotificationSettings.class.getName(), null,
                            R.string.configure_notification_settings, null, this, ADVANCED_SETTINGS);
                } else {
                    ((SettingsActivity) getActivity()).startPreferencePanel(
                            AdvancedAppSettings.class.getName(), null, R.string.configure_apps,
                            null, this, ADVANCED_SETTINGS);
                }
                return true;
            default:
                // Handle the home button
                return false;
        }
        updateOptionsMenu();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mApplications != null && mApplications.getCount() > position) {
            ApplicationsState.AppEntry entry = mApplications.getAppEntry(position);
            mCurrentPkgName = entry.info.packageName;
            mCurrentUid = entry.info.uid;
            //startApplicationDetailsActivity();

            mSwitch = (Switch) view.findViewById(R.id.usage_check_switch);

            final boolean isChecked = !mSwitch.isChecked();
            mSwitch.setChecked(isChecked);
 
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mFilter = mFilterAdapter.getFilter(position);
        mApplications.setFilter(mFilter);
        if (DEBUG) Log.d(TAG, "Selecting filter " + mFilter);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    public void updateView() {
        updateOptionsMenu();
        final Activity host = getActivity();
        if (host != null) {
            host.invalidateOptionsMenu();
        }
    }

    public void setHasDisabled(boolean hasDisabledApps) {
        if (mListType != LIST_TYPE_MAIN) {
            return;
        }
        mFilterAdapter.setFilterEnabled(FILTER_APPS_ENABLED, hasDisabledApps);
        mFilterAdapter.setFilterEnabled(FILTER_APPS_DISABLED, hasDisabledApps);
    }

    static class FilterSpinnerAdapter extends ArrayAdapter<CharSequence> {

        private final CNSManageApplications mManageApplications;

        // Use ArrayAdapter for view logic, but have our own list for managing
        // the options available.
        private final ArrayList<Integer> mFilterOptions = new ArrayList<>();

        public FilterSpinnerAdapter(CNSManageApplications manageApplications) {
            super(manageApplications.getActivity(), R.layout.filter_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mManageApplications = manageApplications;
        }

        public int getFilter(int position) {
            return mFilterOptions.get(position);
        }

        public void setFilterEnabled(int filter, boolean enabled) {
            if (enabled) {
                enableFilter(filter);
            } else {
                disableFilter(filter);
            }
        }

        public void enableFilter(int filter) {
            if (mFilterOptions.contains(filter)) return;
            if (DEBUG) Log.d(TAG, "Enabling filter " + filter);
            mFilterOptions.add(filter);
            Collections.sort(mFilterOptions);
            mManageApplications.mSpinnerHeader.setVisibility(
                    mFilterOptions.size() > 1 ? View.VISIBLE : View.GONE);
            notifyDataSetChanged();
            if (mFilterOptions.size() == 1) {
                if (DEBUG) Log.d(TAG, "Auto selecting filter " + filter);
                mManageApplications.mFilterSpinner.setSelection(0);
                mManageApplications.onItemSelected(null, null, 0, 0);
            }
        }

        public void disableFilter(int filter) {
            if (!mFilterOptions.remove((Integer) filter)) {
                return;
            }
            if (DEBUG) Log.d(TAG, "Disabling filter " + filter);
            Collections.sort(mFilterOptions);
            mManageApplications.mSpinnerHeader.setVisibility(
                    mFilterOptions.size() > 1 ? View.VISIBLE : View.GONE);
            notifyDataSetChanged();
            if (mManageApplications.mFilter == filter) {
                if (mFilterOptions.size() > 0) {
                    if (DEBUG) Log.d(TAG, "Auto selecting filter " + mFilterOptions.get(0));
                    mManageApplications.mFilterSpinner.setSelection(0);
                    mManageApplications.onItemSelected(null, null, 0, 0);
                }
            }
        }

        @Override
        public int getCount() {
            return mFilterOptions.size();
        }

        @Override
        public CharSequence getItem(int position) {
            return getFilterString(mFilterOptions.get(position));
        }

        private CharSequence getFilterString(int filter) {
            return mManageApplications.getString(FILTER_LABELS[filter]);
        }

    }

    /*
     * Custom adapter implementation for the ListView
     * This adapter maintains a map for each displayed application and its properties
     * An index value on each AppInfo object indicates the correct position or index
     * in the list. If the list gets updated dynamically when the user is viewing the list of
     * applications, we need to return the correct index of position. This is done by mapping
     * the getId methods via the package name into the internal maps and indices.
     * The order of applications in the list is mirrored in mAppLocalList
     */
    static class ApplicationsAdapter extends BaseAdapter implements Filterable,
            ApplicationsState.Callbacks, AppStateBaseBridge.Callback, CompoundButton.OnCheckedChangeListener,
            AbsListView.RecyclerListener, SectionIndexer {
        private static final SectionInfo[] EMPTY_SECTIONS = new SectionInfo[0];

        private final ApplicationsState mState;
        private final ApplicationsState.Session mSession;
        private final CNSManageApplications mManageApplications;
        private final Context mContext;
        private final ArrayList<View> mActive = new ArrayList<View>();
        private final AppStateBaseBridge mExtraInfoBridge;
        private final Handler mBgHandler;
        private final Handler mFgHandler;
        private int mFilterMode;
        private ArrayList<ApplicationsState.AppEntry> mBaseEntries;
        private ArrayList<ApplicationsState.AppEntry> mEntries;
        private boolean mResumed;
        private int mLastSortMode = -1;
        private int mWhichSize = SIZE_TOTAL;
        CharSequence mCurFilterPrefix;
        private PackageManager mPm;
        private AppFilter mOverrideFilter;
        private boolean mHasReceivedLoadEntries;
        private boolean mHasReceivedBridgeCallback;

        private AlphabeticIndex.ImmutableIndex mIndex;
        private SectionInfo[] mSections = EMPTY_SECTIONS;
        private int[] mPositionToSectionIndex;
        //+++ suleman
        private UsageState mUsageState;
        private AppStateUsageBridge mUsageBridge;
        private DevicePolicyManager mDpm;
        private AppOpsManager mAppOpsManager;
        private String mPkgName;
        private int mPkgUid;
        //--- suleman

        private Filter mFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<ApplicationsState.AppEntry> entries
                        = applyPrefixFilter(constraint, mBaseEntries);
                FilterResults fr = new FilterResults();
                fr.values = entries;
                fr.count = entries.size();
                return fr;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mCurFilterPrefix = constraint;
                mEntries = (ArrayList<ApplicationsState.AppEntry>) results.values;
                // +[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
                removeLiveDemo();
                // -[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
                rebuildSections();
                notifyDataSetChanged();
            }
        };

        public ApplicationsAdapter(ApplicationsState state, CNSManageApplications manageApplications,
                                   int filterMode) {
            mState = state;
            mFgHandler = new Handler();
            mBgHandler = new Handler(mState.getBackgroundLooper());
            mSession = state.newSession(this);
            mManageApplications = manageApplications;
            mContext = manageApplications.getActivity();
            mPm = mContext.getPackageManager();
            mFilterMode = filterMode;
            //+++ suleman
            mUsageBridge = new AppStateUsageBridge(mContext, mState, null);
            mAppOpsManager = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
            mDpm = mContext.getSystemService(DevicePolicyManager.class);
            //--- suleman
            if (mManageApplications.mListType == LIST_TYPE_NOTIFICATION) {
                mExtraInfoBridge = new AppStateNotificationBridge(mContext, mState, this,
                        manageApplications.mNotifBackend);
            } else if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                mExtraInfoBridge = new AppStateUsageBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_HIGH_POWER) {
                mExtraInfoBridge = new AppStatePowerBridge(mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_OVERLAY) {
                mExtraInfoBridge = new AppStateOverlayBridge(mContext, mState, this);
            } else if (mManageApplications.mListType == LIST_TYPE_WRITE_SETTINGS) {
                mExtraInfoBridge = new AppStateWriteSettingsBridge(mContext, mState, this);
            } else {
                mExtraInfoBridge = null;
            }
        }

        public void setOverrideFilter(AppFilter overrideFilter) {
            mOverrideFilter = overrideFilter;
            rebuild(true);
        }

        public void setFilter(int filter) {
            mFilterMode = filter;
            rebuild(true);
        }

        public void resume(int sort) {
            if (DEBUG) Log.i(TAG, "Resume!  mResumed=" + mResumed);
            if (!mResumed) {
                mResumed = true;
                mSession.resume();
                mLastSortMode = sort;
                if (mExtraInfoBridge != null) {
                    mExtraInfoBridge.resume();
                }
                rebuild(false);
            } else {
                rebuild(sort);
            }
        }

        public void pause() {
            if (mResumed) {
                mResumed = false;
                mSession.pause();
                if (mExtraInfoBridge != null) {
                    mExtraInfoBridge.pause();
                }
            }
        }

        public void release() {
            mSession.release();
            if (mExtraInfoBridge != null) {
                mExtraInfoBridge.release();
            }
        }

        public void rebuild(int sort) {
            if (sort == mLastSortMode) {
                return;
            }
            mLastSortMode = sort;
            rebuild(true);
        }

        public void rebuild(boolean eraseold) {
            if (!mHasReceivedLoadEntries
                    || (mExtraInfoBridge != null && !mHasReceivedBridgeCallback)) {
                // Don't rebuild the list until all the app entries are loaded.
                return;
            }
            if (DEBUG) Log.i(TAG, "Rebuilding app list...");
            ApplicationsState.AppFilter filterObj;
            Comparator<AppEntry> comparatorObj;
            boolean emulated = Environment.isExternalStorageEmulated();
            if (emulated) {
                mWhichSize = SIZE_TOTAL;
            } else {
                mWhichSize = SIZE_INTERNAL;
            }
            filterObj = FILTERS[mFilterMode];
            if (mOverrideFilter != null) {
                filterObj = mOverrideFilter;
            }
            if (!mManageApplications.mShowSystem) {
                filterObj = new CompoundFilter(filterObj,
                        ApplicationsState.FILTER_DOWNLOADED_AND_LAUNCHER);
            }
            switch (mLastSortMode) {
                case R.id.sort_order_size:
                    switch (mWhichSize) {
                        case SIZE_INTERNAL:
                            comparatorObj = ApplicationsState.INTERNAL_SIZE_COMPARATOR;
                            break;
                        case SIZE_EXTERNAL:
                            comparatorObj = ApplicationsState.EXTERNAL_SIZE_COMPARATOR;
                            break;
                        default:
                            comparatorObj = ApplicationsState.SIZE_COMPARATOR;
                            break;
                    }
                    break;
                default:
                    comparatorObj = ApplicationsState.ALPHA_COMPARATOR;
                    break;
            }
            AppFilter finalFilterObj = filterObj;
            mBgHandler.post(() -> {
                final ArrayList<AppEntry> entries = mSession.rebuild(finalFilterObj,
                        comparatorObj, false);
                
                if (entries != null) {
                    mFgHandler.post(() -> onRebuildComplete(entries));
                }
            });
        }

        //+++ tim
        private void sortEntries(ArrayList<AppEntry> entries){
        	if(entries == null || entries.size() < 2) return;
        	
        	Comparator<AppEntry> comparator = new Comparator<AppEntry>() {
    		    @Override
    		    public int compare(AppEntry object1, AppEntry object2) {
    		    	if (object1.info != null && object2.info != null) {
    	        		final PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
    	        		
    	        		boolean isWhitelisted1 = backend.isSysWhitelisted(object1.info.packageName) ||
    	        				backend.isWhitelisted(object1.info.packageName);
    	        		
    	        		boolean isWhitelisted2 = backend.isSysWhitelisted(object2.info.packageName) ||
    	        				backend.isWhitelisted(object2.info.packageName);
    	        		
    		        	int x1 = isWhitelisted1 ? 1 : 0;
    		        	int x2 = isWhitelisted2 ? 1 : 0;
    		        	
    		        	return x1 - x2;
    		    	}
    		    	
    		    	return 0;
    		    }
    		};
    		
    		Collections.sort(entries, comparator);
        }

        /**
         * @param newValue if true set all apps hign power
         */
        public void setAll(boolean newValue){
        	boolean changed = false;
        	
    		if(mEntries != null){
    			PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
    			backend.createDozeModeBackend(mContext.getApplicationContext());
    			
    			for(AppEntry entry : mEntries){
    				String pkg = entry.info.packageName;
                    boolean changeable = !backend.isSysWhitelisted(pkg);
//                          && !backend.isAsusWhitelisted(pkg, mContext.getApplicationContext());
    				if(changeable){
    					boolean oldValue = backend.isWhitelisted(pkg);
    					
    					if (newValue != oldValue) {
    	                    if (newValue) {
    	                    	backend.addApp(pkg);
    	                    } else {
    	                    	backend.removeApp(pkg);
    	                    }
    	                    
    	                    if(!changed)
    	                    	changed = true;
    	                }
    				}
    			}
    		}
    		
    		if(changed){
    			updateHeaderTitle();
    			notifyDataSetChanged();
    		}
        }
        
        private void updateHeaderTitle(){
        	updateHeaderTitle(true, true);
        }
        
        private synchronized void updateHeaderTitle(boolean calcAll, boolean checked){
        	if(mManageApplications.mHeaderTitle != null){
        		int count = 0;
        		
        		if(calcAll){
	        		if(mEntries != null){
	        			PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
	        			backend.createDozeModeBackend(mContext.getApplicationContext());
	        			
	        			for(AppEntry entry : mEntries){
	        				String pkg = entry.info.packageName;
	        				boolean isWhitelisted = backend.isSysWhitelisted(pkg) ||
//                                  backend.isAsusWhitelisted(pkg, mContext.getApplicationContext()) ||
	        						backend.isWhitelisted(pkg);
	        				
	        				if(!isWhitelisted)
	        					count++;
	        			}
	        		}
        		} else{
        			if(mManageApplications.mHeaderTitle.getTag() != null){
        				count = (int) mManageApplications.mHeaderTitle.getTag();
        				if(checked)
        					count++;
        				else
        					count--;
        			}
        		}
        		
        		mManageApplications.mHeaderTitle.setTag(count);
        		mManageApplications.mHeaderTitle.setText(
        				mContext.getString(R.string.high_power_apps_optimize_header_title, count));
        	}
        }
        //---

        static private boolean packageNameEquals(PackageItemInfo info1, PackageItemInfo info2) {
            if (info1 == null || info2 == null) {
                return false;
            }
            if (info1.packageName == null || info2.packageName == null) {
                return false;
            }
            return info1.packageName.equals(info2.packageName);
        }

        private ArrayList<ApplicationsState.AppEntry> removeDuplicateIgnoringUser(
                ArrayList<ApplicationsState.AppEntry> entries)
        {
            int size = entries.size();
            // returnList will not have more entries than entries
            ArrayList<ApplicationsState.AppEntry> returnEntries = new
                    ArrayList<ApplicationsState.AppEntry>(size);

            // assume appinfo of same package but different users are grouped together
            PackageItemInfo lastInfo = null;
            for (int i = 0; i < size; i++) {
                AppEntry appEntry = entries.get(i);
                PackageItemInfo info = appEntry.info;
                if (!packageNameEquals(lastInfo, appEntry.info)) {
                    returnEntries.add(appEntry);
                }
                lastInfo = info;
            }
            returnEntries.trimToSize();
            return returnEntries;
        }

        @Override
        public void onRebuildComplete(ArrayList<AppEntry> entries) {
            if (mFilterMode == FILTER_APPS_POWER_WHITELIST ||
                    mFilterMode == FILTER_APPS_POWER_WHITELIST_ALL) {
                entries = removeDuplicateIgnoringUser(entries);
                
                //+++ tim
                sortEntries(entries);
                updateHeaderTitle();
                //---
            }
            mBaseEntries = entries;
            if (mBaseEntries != null) {
                mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
                // +[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
                removeLiveDemo();
                // -[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
                rebuildSections();
            } else {
                mEntries = null;
                mSections = EMPTY_SECTIONS;
                mPositionToSectionIndex = null;
            }

            notifyDataSetChanged();

            if (mSession.getAllApps().size() != 0
                    && mManageApplications.mListContainer.getVisibility() != View.VISIBLE) {
                Utils.handleLoadingContainer(mManageApplications.mLoadingContainer,
                        mManageApplications.mListContainer, true, true);
            }
            if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                // No enabled or disabled filters for usage access.
                return;
            }

            mManageApplications.setHasDisabled(mState.haveDisabledApps());
        }

        private void rebuildSections() {
            if (mEntries!= null && mManageApplications.mListView.isFastScrollEnabled()) {
                // Rebuild sections
                if (mIndex == null) {
                    LocaleList locales = mContext.getResources().getConfiguration().getLocales();
                    if (locales.size() == 0) {
                        locales = new LocaleList(Locale.ENGLISH);
                    }
                    AlphabeticIndex index = new AlphabeticIndex<>(locales.get(0));
                    int localeCount = locales.size();
                    for (int i = 1; i < localeCount; i++) {
                        index.addLabels(locales.get(i));
                    }
                    // Ensure we always have some base English locale buckets
                    index.addLabels(Locale.ENGLISH);
                    mIndex = index.buildImmutableIndex();
                }

                ArrayList<SectionInfo> sections = new ArrayList<>();
                int lastSecId = -1;
                int totalEntries = mEntries.size();
                mPositionToSectionIndex = new int[totalEntries];

                for (int pos = 0; pos < totalEntries; pos++) {
                    String label = mEntries.get(pos).label;
                    int secId = mIndex.getBucketIndex(TextUtils.isEmpty(label) ? "" : label);
                    if (secId != lastSecId) {
                        lastSecId = secId;
                        sections.add(new SectionInfo(mIndex.getBucket(secId).getLabel(), pos));
                    }
                    mPositionToSectionIndex[pos] = sections.size() - 1;
                }
                mSections = sections.toArray(EMPTY_SECTIONS);
            } else {
                mSections = EMPTY_SECTIONS;
                mPositionToSectionIndex = null;
            }
        }

        private void updateLoading() {
            Utils.handleLoadingContainer(mManageApplications.mLoadingContainer,
                    mManageApplications.mListContainer,
                    mHasReceivedLoadEntries && mSession.getAllApps().size() != 0, false);
        }

        ArrayList<ApplicationsState.AppEntry> applyPrefixFilter(CharSequence prefix,
                                                                ArrayList<ApplicationsState.AppEntry> origEntries) {
            if (prefix == null || prefix.length() == 0) {
                return origEntries;
            } else {
                String prefixStr = ApplicationsState.normalize(prefix.toString());
                final String spacePrefixStr = " " + prefixStr;
                ArrayList<ApplicationsState.AppEntry> newEntries
                        = new ArrayList<ApplicationsState.AppEntry>();
                for (int i = 0; i < origEntries.size(); i++) {
                    ApplicationsState.AppEntry entry = origEntries.get(i);
                    String nlabel = entry.getNormalizedLabel();
                    if (nlabel.startsWith(prefixStr) || nlabel.indexOf(spacePrefixStr) != -1) {
                        newEntries.add(entry);
                    }
                }
                return newEntries;
            }
        }

        @Override
        public void onExtraInfoUpdated() {
            mHasReceivedBridgeCallback = true;
            rebuild(false);
        }

        @Override
        public void onRunningStateChanged(boolean running) {
            mManageApplications.getActivity().setProgressBarIndeterminateVisibility(running);
        }

        @Override
        public void onPackageListChanged() {
            rebuild(false);
        }

        @Override
        public void onPackageIconChanged() {
            // We ensure icons are loaded when their item is displayed, so
            // don't care about icons loaded in the background.
        }

        @Override
        public void onLoadEntriesCompleted() {
            mHasReceivedLoadEntries = true;
            // We may have been skipping rebuilds until this came in, trigger one now.
            rebuild(false);
        }

        @Override
        public void onPackageSizeChanged(String packageName) {
            for (int i = 0; i < mActive.size(); i++) {
                CNSAppViewHolder holder = (CNSAppViewHolder) mActive.get(i).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        updateSummary(holder);
                    }
                    if (holder.entry.info.packageName.equals(mManageApplications.mCurrentPkgName)
                            && mLastSortMode == R.id.sort_order_size) {
                        // We got the size information for the last app the
                        // user viewed, and are sorting by size...  they may
                        // have cleared data, so we immediately want to resort
                        // the list with the new size to reflect it to the user.
                        rebuild(false);
                    }
                    return;
                }
            }
        }

        @Override
        public void onLauncherInfoChanged() {
            if (!mManageApplications.mShowSystem) {
                rebuild(false);
            }
        }

        @Override
        public void onAllSizesComputed() {
            if (mLastSortMode == R.id.sort_order_size) {
                rebuild(false);
            }
        }

        public int getCount() {
            return mEntries != null ? mEntries.size() : 0;
        }

        public Object getItem(int position) {
            return mEntries.get(position);
        }

        public ApplicationsState.AppEntry getAppEntry(int position) {
            return mEntries.get(position);
        }

        public long getItemId(int position) {
            return mEntries.get(position).id;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            if (mManageApplications.mListType != LIST_TYPE_HIGH_POWER) {
                return true;
            }
            ApplicationsState.AppEntry entry = mEntries.get(position);

            //BEGIN: Steve_Ke@asus.com
            PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
            backend.createDozeModeBackend(mContext.getApplicationContext());

            return !PowerWhitelistBackend.getInstance().isSysWhitelisted(entry.info.packageName);
                    //Begin: hungjie_tseng@asus.com
//                    && !backend.isAsusWhitelisted(entry.info.packageName, mContext.getApplicationContext());
                    //End:hungjie_tseng@asus.com
            //END: Steve_Ke@asus.com
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            CNSAppViewHolder holder = CNSAppViewHolder.createOrRecycle(mManageApplications.mInflater,
                    convertView, position);
            convertView = holder.rootView;

            //+++ tim
            boolean enable = isEnabled(position);
            //---
            //+++ suleman
            holder.mSwitch.setOnCheckedChangeListener(null);
            //--- suleman
            // Bind the data efficiently with the holder
            ApplicationsState.AppEntry entry = mEntries.get(position);
            synchronized (entry) {
                holder.entry = entry;
                if (entry.label != null) {
                    holder.appName.setText(entry.label);
                }
                mState.ensureIcon(entry);
                if (entry.icon != null) {
                    holder.appIcon.setImageDrawable(entry.icon);
                }
                updateSummary(holder);
                if ((entry.info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
                    holder.disabled.setVisibility(View.VISIBLE);
                    holder.disabled.setText(R.string.not_installed);
                } else if (!entry.info.enabled) {
                    holder.disabled.setVisibility(View.VISIBLE);
                    holder.disabled.setText(R.string.disabled);
                } else {
                    holder.disabled.setVisibility(View.GONE);
                }
                //+++Asus APP2SD blacklist
                if(((entry.info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) &&
                        mPm.isInApp2sdBlacklist(entry.info.packageName)) {
                    holder.badge.setVisibility(View.VISIBLE);
                    holder.badge.setImageResource(android.R.drawable.stat_sys_warning);
                    holder.badge.getDrawable().setTint(Color.RED);
                } else {
                    holder.badge.setVisibility(View.INVISIBLE);
                }
                //---

                //+++ tim
                
                if(mManageApplications.mListType == LIST_TYPE_HIGH_POWER){
                	boolean checked = enable && !PowerWhitelistBackend.getInstance().isWhitelisted(entry.info.packageName);
                	holder.mSwitch.setChecked(checked);
                } else {
                //---
                	
                //+++ suleman
                mPkgName = entry.info.packageName;
                mPkgUid = entry.info.uid;

                mUsageState = mUsageBridge.getUsageInfo(mPkgName,mPkgUid);
                boolean hasAccess = mUsageState.isPermissible();
                holder.mSwitch.setChecked(hasAccess);
                //---
                }
            }
            mActive.remove(convertView);
            mActive.add(convertView);
            
            if(enable){
	            //+++ suleman
	            holder.mSwitch.setOnCheckedChangeListener(this);
	            //--- suleman
            }
            holder.mSwitch.setEnabled(enable);
            convertView.setEnabled(enable);
            return convertView;
        }

        @Override  
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {  
            switch (buttonView.getId()){  
                case R.id.usage_check_switch:
                    setSwitch((Switch)buttonView, isChecked);


                break;  
                default:  
                break;  
            }  
        }

        public void setSwitch(Switch mSwitch, boolean isChecked) {
        	ApplicationsState.AppEntry entry = mEntries.get((int)mSwitch.getTag());
        	mPkgName = entry.info.packageName;
            mPkgUid = entry.info.uid;
            
            Log.d("timhu", "mListType = " + mManageApplications.mListType);
        	//+++ tim
            if(mManageApplications.mListType == LIST_TYPE_HIGH_POWER){
            	PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
                backend.createDozeModeBackend(mContext.getApplicationContext());
                
            	boolean newValue = !isChecked;  //checked  turn off high power
                boolean oldValue = backend.isWhitelisted(mPkgName);
                if (newValue != oldValue) {
                    if (newValue) {
                    	backend.addApp(mPkgName);
                    } else {
                    	backend.removeApp(mPkgName);
                    }
                    //updateSummary
                    onPackageSizeChanged(mPkgName);
                    updateHeaderTitle(false, isChecked);
                }
            } else {
            //---

            mUsageState = mUsageBridge.getUsageInfo(mPkgName,mPkgUid);
            if (mUsageState != null && (Boolean) isChecked != mUsageState.isPermissible()) {
                if (mUsageState.isPermissible() && mDpm.isProfileOwnerApp(mPkgName)) {
                    new AlertDialog.Builder(mContext)
                            .setIcon(com.android.internal.R.drawable.ic_dialog_alert_material)
                            .setTitle(android.R.string.dialog_alert_title)
                            .setMessage(R.string.work_profile_usage_access_warning)
                            .setPositiveButton(R.string.okay, null)
                            .show();
                }

                mAppOpsManager.setMode(AppOpsManager.OP_GET_USAGE_STATS, mPkgUid,  mPkgName, !mUsageState.isPermissible() ? AppOpsManager.MODE_ALLOWED : AppOpsManager.MODE_IGNORED);
                //need set again *mUsageState*//suleman
                mUsageState = mUsageBridge.getUsageInfo(mPkgName,mPkgUid);
                boolean hasAccess = mUsageState.isPermissible();
                mSwitch.setChecked(hasAccess);
                mSwitch.setEnabled(mUsageState.permissionDeclared);
           }
            }
        }

        private void updateSummary(CNSAppViewHolder holder) {
            switch (mManageApplications.mListType) {
                case LIST_TYPE_NOTIFICATION:
                    if (holder.entry.extraInfo != null) {
                        holder.summary.setText(InstalledAppDetails.getNotificationSummary(
                                (AppRow) holder.entry.extraInfo, mContext));
                    } else {
                        holder.summary.setText(null);
                    }
                    break;

                case LIST_TYPE_DOMAINS_URLS:
                    holder.summary.setText(getDomainsSummary(holder.entry.info.packageName));
                    break;

                case LIST_TYPE_USAGE_ACCESS:
                    //+++ suleman
                    holder.summary.setVisibility(View.GONE);
                    //--- suleman
                    if (holder.entry.extraInfo != null) {
                        holder.summary.setText((new UsageState((PermissionState) holder.entry
                                .extraInfo)).isPermissible() ? R.string.switch_on_text :
                                R.string.switch_off_text);
                    } else {
                        holder.summary.setText(null);
                    }
                    break;

                case LIST_TYPE_HIGH_POWER:
                    holder.summary.setText(HighPowerDetail.getSummary(mContext, holder.entry));
                    break;

                case LIST_TYPE_OVERLAY:
                    holder.summary.setText(DrawOverlayDetails.getSummary(mContext, holder.entry));
                    break;

                case LIST_TYPE_WRITE_SETTINGS:
                    holder.summary.setText(WriteSettingsDetails.getSummary(mContext,
                            holder.entry));
                    break;

                default:
                    holder.updateSizeText(mManageApplications.mInvalidSizeStr, mWhichSize);
                    break;
            }
        }

        @Override
        public Filter getFilter() {
            return mFilter;
        }

        @Override
        public void onMovedToScrapHeap(View view) {
            mActive.remove(view);
        }

        private CharSequence getDomainsSummary(String packageName) {
            // If the user has explicitly said "no" for this package, that's the
            // string we should show.
            int domainStatus = mPm.getIntentVerificationStatusAsUser(packageName, UserHandle.myUserId());
            if (domainStatus == PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER) {
                return mContext.getString(R.string.domain_urls_summary_none);
            }
            // Otherwise, ask package manager for the domains for this package,
            // and show the first one (or none if there aren't any).
            ArraySet<String> result = Utils.getHandledDomains(mPm, packageName);
            if (result.size() == 0) {
                return mContext.getString(R.string.domain_urls_summary_none);
            } else if (result.size() == 1) {
                return mContext.getString(R.string.domain_urls_summary_one, result.valueAt(0));
            } else {
                return mContext.getString(R.string.domain_urls_summary_some, result.valueAt(0));
            }
        }

        @Override
        public Object[] getSections() {
            return mSections;
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return mSections[sectionIndex].position;
        }

        @Override
        public int getSectionForPosition(int position) {
            return mPositionToSectionIndex[position];
        }

        // +[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
        private void removeLiveDemo(){
            if (mEntries != null && !mEntries.isEmpty()) {
                for (int i = 0; i < mEntries.size(); i++) {
                    ApplicationsState.AppEntry entry = mEntries.get(i);
                    if (entry != null) {
                        String pkg = entry.info.packageName;
                        boolean bLiveDemoHide = ("com.asus.livedemo").equals(pkg)
                                 || ("com.asus.livedemoservice").equals(pkg);
                        if (bLiveDemoHide) {
                            mEntries.remove(i);
                            i--;
                        }
                    }
                }
            }
        }
        // -[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mLoader;
        private ApplicationsState.Session mSession;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                new AppCounter(mContext) {
                    @Override
                    protected void onCountComplete(int num) {
                        mLoader.setSummary(SummaryProvider.this,
                                mContext.getString(R.string.apps_summary, num));
                    }

                    @Override
                    protected boolean includeInCount(ApplicationInfo info) {
                        if ((info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                            return true;
                        } else if ((info.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                            return true;
                        }
                        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null)
                                .addCategory(Intent.CATEGORY_LAUNCHER)
                                .setPackage(info.packageName);
                        int userId = UserHandle.getUserId(info.uid);
                        List<ResolveInfo> intents = mPm.queryIntentActivitiesAsUser(
                                launchIntent,
                                PackageManager.GET_DISABLED_COMPONENTS
                                        | PackageManager.MATCH_DIRECT_BOOT_AWARE
                                        | PackageManager.MATCH_DIRECT_BOOT_UNAWARE,
                                userId);
                        return intents != null && intents.size() != 0;
                    }
                }.execute();
            }
        }
    }

    private static class SectionInfo {
        final String label;
        final int position;

        public SectionInfo(String label, int position) {
            this.label = label;
            this.position = position;
        }

        @Override
        public String toString() {
            return label;
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
