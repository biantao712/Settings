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
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.icu.text.AlphabeticIndex;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.AppHeader;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
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
import com.android.settings.twinApps.TwinAppsUtil;
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

import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_ALL;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_INSTALLED;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class CNManageApplications extends InstrumentedFragment
        implements OnItemClickListener{

    static final String TAG = "CNManageApplications";
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
    public static final int FILTER_APPS_INSTALLED = 16;

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
            R.string.filter_all_apps,      // All apps
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
            ApplicationsState.FILTER_THIRD_PARTY,  // All apps
    };

    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;

    public final static int SORT_ORDER_ALPHA = 0;
    public final static int SORT_ORDER_SIZE = 1;
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
    public static final int LIST_TYPE_INSTALLED = 8;

    private View mRootView;

    private View mSpinnerHeader;
    private Spinner mFilterSpinner;
    private FilterSpinnerAdapter mFilterAdapter;
    private NotificationBackend mNotifBackend;
    private String mVolumeUuid;
    private String mVolumeName;
    private int type;
    private CNManageAppsSync mCNManageAppsSync;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mApplicationsState = ApplicationsState.getInstance(getActivity().getApplication());

        Bundle args = getArguments();
        type = args.getInt(EXTRA_APPLICATIONS_TYPE);
        if (type == EXTRA_APPLICATIONS_TYPE_INSTALLED){
            mListType = LIST_TYPE_INSTALLED;
        }else if (type == EXTRA_APPLICATIONS_TYPE_ALL){
            mListType = LIST_TYPE_MAIN;
            mShowSystem = true;
        } else {
            mListType = LIST_TYPE_MAIN;
            mShowSystem = true;
        }
//        mSortOrder = SORT_ORDER_SIZE;

        mFilter = getDefaultFilter();

        if (savedInstanceState != null) {

            Log.d("blenda", "onCreate, savedInstanceState, mListType: "+mListType);
            mSortOrder = savedInstanceState.getInt(EXTRA_SORT_ORDER, mSortOrder);
            mShowSystem = savedInstanceState.getBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
        }

        mInvalidSizeStr = getActivity().getText(R.string.invalid_size_value);
        Log.d("blenda", "onCreate, mListType: "+mListType);
        mCNManageAppsSync = CNManageAppsSync.getInstance();
        setInterface();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
//        if (mListener_AppPagesChange != null){
//            Log.d("blenda", "mListener_AppPagesChange!= null");
//            mListener_AppPagesChange.onPageCreate(type);
//        }
        mCNManageAppsSync.onPageCreate(type);
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
            }
            mListView.setAdapter(mApplications);
            mListView.setRecyclerListener(mApplications);
            mListView.setFastScrollEnabled(false);//(isFastScrollEnabled());
            mListView.setDivider(null);
            mListView.setScrollIndicators(0);

            Utils.prepareCustomPreferencesList(container, mRootView, mListView, false);
        }

        // We have to do this now because PreferenceFrameLayout looks at it
        // only when the view is added.
        if (container instanceof PreferenceFrameLayout) {
            ((PreferenceFrameLayout.LayoutParams) mRootView.getLayoutParams()).removeBorders = true;
        }

//        createHeader();


        Log.d("blenda", "onCreateView, mListType: "+mListType);
        return mRootView;
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //BEGIN:Steve_Ke@asus.com
        PowerWhitelistBackend backend = PowerWhitelistBackend.getInstance();
        backend.createDozeModeBackend(getContext());
        backend.setManageApplication(this, DOZE_WHITE_LIST);
        Log.d("blenda", "onViewCreated, mListType: "+mListType);
        //END:Steve_Ke@asus.com
    }

    private int getDefaultFilter() {
        switch (mListType) {
            case LIST_TYPE_DOMAINS_URLS:
                return FILTER_APPS_WITH_DOMAIN_URLS;
            case LIST_TYPE_USAGE_ACCESS:
                return FILTER_APPS_USAGE_ACCESS;
            case LIST_TYPE_HIGH_POWER:
                return FILTER_APPS_POWER_WHITELIST;
            case LIST_TYPE_OVERLAY:
                return FILTER_APPS_WITH_OVERLAY;
            case LIST_TYPE_WRITE_SETTINGS:
                return FILTER_APPS_WRITE_SETTINGS;
            case LIST_TYPE_INSTALLED:
                return FILTER_APPS_INSTALLED;
            default:
                return FILTER_APPS_ALL;
        }
    }

    private boolean isFastScrollEnabled() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
            case LIST_TYPE_NOTIFICATION:
            case LIST_TYPE_STORAGE:
                return mSortOrder == SORT_ORDER_ALPHA;
            default:
                return false;
        }
    }

    @Override
    protected int getMetricsCategory() {
        switch (mListType) {
            case LIST_TYPE_MAIN:
            case LIST_TYPE_INSTALLED:
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
        Log.d("blenda", "cn manager applications onResume, mListType: "+mListType);
        updateView();
//        updateOptionsMenu();
        if (mApplications != null) {
            mApplications.resume(mSortOrder);
            mApplications.updateLoading();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("blenda", "onSaveInstanceState, mListType: "+mListType);
        outState.putInt(EXTRA_SORT_ORDER, mSortOrder);
        outState.putBoolean(EXTRA_SHOW_SYSTEM, mShowSystem);
        if (mApplications != null) {
            outState.putBoolean(EXTRA_HAS_ENTRIES, mApplications.mHasReceivedLoadEntries);
            outState.putBoolean(EXTRA_HAS_BRIDGE, mApplications.mHasReceivedBridgeCallback);
        }
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
    }

    @Override
    public void onDestroyView() {
        Log.d("blenda", "onDestoryView, mListType: "+mListType);
//        if (mListener_AppPagesChange != null){
//            mListener_AppPagesChange.onPageDestroy(type);
//        }
        mCNManageAppsSync.onPageDestroy(type);
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
//            FragmentTransaction ft = getFragmentManager().beginTransaction();
//            ft.detach(this).attach(this).commitAllowingStateLoss();
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


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mApplications != null && mApplications.getCount() > position) {
            ApplicationsState.AppEntry entry = mApplications.getAppEntry(position);
            mCurrentPkgName = entry.info.packageName;
            mCurrentUid = entry.info.uid;
            startApplicationDetailsActivity();
        }
    }


    public void updateView() {
        final Activity host = getActivity();
        if (host != null) {
            host.invalidateOptionsMenu();
        }
    }

    static class FilterSpinnerAdapter extends ArrayAdapter<CharSequence> {

        private final CNManageApplications mManageApplications;

        // Use ArrayAdapter for view logic, but have our own list for managing
        // the options available.
        private final ArrayList<Integer> mFilterOptions = new ArrayList<>();

        public FilterSpinnerAdapter(CNManageApplications manageApplications) {
            super(manageApplications.getActivity(), R.layout.filter_spinner_item);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mManageApplications = manageApplications;
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
            ApplicationsState.Callbacks, AppStateBaseBridge.Callback,
            AbsListView.RecyclerListener, SectionIndexer {
        private static final SectionInfo[] EMPTY_SECTIONS = new SectionInfo[0];

        private final ApplicationsState mState;
        private final ApplicationsState.Session mSession;
        private final CNManageApplications mManageApplications;
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
                //[TwinApps] {
                removeTwinApps();
                //[TwinApps] }
                rebuildSections();
                notifyDataSetChanged();
            }
        };

        public ApplicationsAdapter(ApplicationsState state, CNManageApplications manageApplications,
                                   int filterMode) {
            mState = state;
            mFgHandler = new Handler();
            mBgHandler = new Handler(mState.getBackgroundLooper());
            mSession = state.newSession(this);
            mManageApplications = manageApplications;
            mContext = manageApplications.getActivity();
            mPm = mContext.getPackageManager();
            mFilterMode = filterMode;
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
                case SORT_ORDER_SIZE:
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
            }
            mBaseEntries = entries;
            if (mBaseEntries != null) {
                mEntries = applyPrefixFilter(mCurFilterPrefix, mBaseEntries);
                // +[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
                removeLiveDemo();
                // -[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
                //[TwinApps] {
                removeTwinApps();
                //[TwinApps] }
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
                        mManageApplications.mListContainer, true, false);
            }
            if (mManageApplications.mListType == LIST_TYPE_USAGE_ACCESS) {
                // No enabled or disabled filters for usage access.
                return;
            }

//            mManageApplications.setHasDisabled(mState.haveDisabledApps());
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
                CNAppViewHolderTwoLine holder = (CNAppViewHolderTwoLine) mActive.get(i).getTag();
                if (holder.entry.info.packageName.equals(packageName)) {
                    synchronized (holder.entry) {
                        updateSummary(holder);
                    }
                    if (holder.entry.info.packageName.equals(mManageApplications.mCurrentPkgName)
                            && mLastSortMode == SORT_ORDER_SIZE) {
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
            if (mLastSortMode == SORT_ORDER_SIZE) {
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

            return !PowerWhitelistBackend.getInstance().isSysWhitelisted(entry.info.packageName)
                    //Begin: hungjie_tseng@asus.com
                    && !backend.isAsusWhitelisted(entry.info.packageName, mContext.getApplicationContext());
                    //End:hungjie_tseng@asus.com
            //END: Steve_Ke@asus.com
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            // A ViewHolder keeps references to children views to avoid unnecessary calls
            // to findViewById() on each row.
            CNAppViewHolderTwoLine holder = CNAppViewHolderTwoLine.createOrRecycle(mManageApplications.mInflater,
                    convertView);
            convertView = holder.rootView;

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
                /*if(((entry.info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0) &&
                        mPm.isInApp2sdBlacklist(entry.info.packageName)) {
                    holder.badge.setVisibility(View.VISIBLE);
                    holder.badge.setImageResource(android.R.drawable.stat_sys_warning);
                    holder.badge.getDrawable().setTint(Color.RED);
                } else {
                    holder.badge.setVisibility(View.INVISIBLE);
                }*/
                //---
            }
            mActive.remove(convertView);
            mActive.add(convertView);
            convertView.setEnabled(isEnabled(position));
            return convertView;
        }

        private void updateSummary(CNAppViewHolderTwoLine holder) {
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

        // [TwinApps] {
        private void removeTwinApps(){
            if (null != mEntries && !mEntries.isEmpty()){
                boolean bTwinAppsEnable = TwinAppsUtil.isTwinAppsSupport(mContext);
                int twinAppsId = -1;
                if (bTwinAppsEnable) {
                    final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
                    twinAppsId = um.getTwinAppsId();
                }
                 for (int i=0; i< mEntries.size(); i++){
                    ApplicationsState.AppEntry entry = mEntries.get(i);
                    if (null != entry){
                        if (("com.asus.twinapps").equals(entry.info.packageName)) {
                            //Hide twinapps app even if FEATURE disabled.
                            mEntries.remove(i);
                            i--;
                        } else if (bTwinAppsEnable && twinAppsId != -1
                                && UserHandle.getUserId(entry.info.uid) == twinAppsId) {
                            //Hide non-launcher apps of TwinApps
                            if ((entry.info.isSystemApp() || entry.info.isUpdatedSystemApp())
                                    && !entry.hasLauncherEntry) {
                                mEntries.remove(i);
                                i--;
                            } else if (("com.android.settings").equals(entry.info.packageName)) {
                                mEntries.remove(i);
                                i--;
                            }
                        }
                    }
                }
            }
        }
        //[TwinApps] }
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
                        mLoader.setSummary(CNManageApplications.SummaryProvider.this,
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

    public void setSortOrder(int order, boolean rebuild){

        mSortOrder = order;
        if (rebuild) {
            if (mListView != null)
            mListView.setFastScrollEnabled(false);//(isFastScrollEnabled());
            if (mApplications != null) {
                mApplications.rebuild(mSortOrder);
            }
        }
    }

    private void setInterface(){

        OnCallManageApps listener_callManageApps = new OnCallManageApps() {
            @Override
            public void destroyView() {
                onDestroyView();
            }

            @Override
            public void setSort(int type, boolean rebuild) {
                setSortOrder(type, rebuild);
            }

        };
        mCNManageAppsSync.setListener(listener_callManageApps, type);
    }
}
