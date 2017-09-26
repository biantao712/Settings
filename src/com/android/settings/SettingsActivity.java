/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ActionBar;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SearchView;

import com.android.internal.util.ArrayUtils;
import com.android.settings.Settings.WifiSettingsActivity;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilitySettingsForSetupWizard;
import com.android.settings.accessibility.CaptionPropertiesFragment;
import com.android.settings.accessibility.ServicesFragment;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.accounts.AccountSyncSettings;
import com.android.settings.accounts.ChooseAccountActivity;
import com.android.settings.accounts.ManagedProfileSettings;
import com.android.settings.applications.AdvancedAppSettings;
import com.android.settings.applications.DrawOverlayDetails;
import com.android.settings.applications.InstalledAppDetails;
import com.android.settings.applications.ManageApplications;
import com.android.settings.applications.CNManageAppsEntry;
import com.android.settings.applications.ManageAssist;
import com.android.settings.applications.ManageDomainUrls;
import com.android.settings.applications.NotificationApps;
import com.android.settings.applications.ProcessStatsSummary;
import com.android.settings.applications.ProcessStatsUi;
import com.android.settings.applications.UsageAccessDetails;
import com.android.settings.applications.VrListenerSettings;
import com.android.settings.applications.WriteSettingsDetails;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.dashboard.DashboardContainerFragment;
import com.android.settings.dashboard.SearchResultsSummary;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deletionhelper.AutomaticStorageManagerSettings;
import com.android.settings.deviceinfo.ImeiInformation;
import com.android.settings.deviceinfo.PrivateVolumeForget;
import com.android.settings.deviceinfo.PrivateVolumeSettings;
import com.android.settings.deviceinfo.PublicVolumeSettings;
import com.android.settings.deviceinfo.SimStatus;
import com.android.settings.deviceinfo.Status;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.NightDisplaySettings;
import com.android.settings.fingerprint.AsusFingerprintSettings;
import com.android.settings.ethernet.EthernetSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageDetail;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment;
import com.android.settings.inputmethod.KeyboardLayoutPickerFragment2;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.SpellCheckersSettings;
import com.android.settings.inputmethod.UserDictionaryList;
import com.android.settings.localepicker.LocaleListEditor;
import com.android.settings.location.LocationSettings;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.nfc.PaymentDefaultDialog;
import com.android.settings.notification.AppNotificationSettings;
import com.android.settings.notification.CNAppNotifySettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.NotificationAccessSettings;
import com.android.settings.notification.NotificationStation;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.VibrationIntensitySettings; //Sharon+++new feature about vibration intensity
import com.android.settings.notification.CNStatusBarNotificationEntry;
import com.android.settings.notification.CNNotificationManagerEntry;
import com.android.settings.notification.ZenAccessSettings;
import com.android.settings.notification.ZenModeAutomationSettings;
import com.android.settings.notification.ZenModeEventRuleSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeScheduleRuleSettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintJobSettingsFragment;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.qstile.DevelopmentTiles;
import com.android.settings.search.DynamicIndexableContentMonitor;
import com.android.settings.search.Index;
import com.android.settings.sim.SimSettings;
import com.android.settings.tts.TextToSpeechSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.util.AlwaysOnUtils;
import com.android.settings.util.ResCustomizeConfig;
import com.android.settings.util.VerizonHelpUtils;
import com.android.settings.vpn2.VpnSettings;
import com.android.settings.wfd.WifiDisplaySettings;
import com.android.settings.widget.SwitchBar;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiAPITest;
import com.android.settings.wifi.WifiInfo;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.wifi.p2p.WifiP2pSettings;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.Tile;
import com.android.settings.zenmotion.AsusMotionSettings;
import com.android.settings.zenmotion.AsusTouchSettings;
import com.android.settings.zenmotion.AsusZenMotionSettings;
import com.android.settings.zenmotion.OneHandModeSettings;
//zenmotion2+++
import com.android.settings.zenmotion2.AsusZenMotion2Settings;
import com.android.settings.zenmotion2.AsusGestureQuickStart;
import com.android.settings.zenmotion2.AppList;
import com.android.settings.zenmotion2.GestureToturial;
//zenmotion2---
import com.android.settings.memc.MemcSettingsVerizon;
import com.android.settings.memc.MemcSettings;
import com.android.settings.widget.SwitchBarWithFixedTitle; //for verizon
//[TwinApps] {
import com.android.settings.AsusTwinAppsSettings;
import android.content.pm.UserInfo;
import com.android.settings.twinApps.TwinAppsUtil;
//[TwinApps] }
import com.android.settings.drawer.SettingsDrawerActivity;

import java.net.URISyntaxException;

//+++ suleman
import com.asus.cncommonres.AsusButtonBar;
//--- suleman


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends SettingsDrawerActivity
        implements PreferenceManager.OnPreferenceTreeClickListener,
        PreferenceFragment.OnPreferenceStartFragmentCallback,
        ButtonBarHandler, FragmentManager.OnBackStackChangedListener,
        AsusSearchView.OnQueryTextListener, AsusSearchView.OnCloseListener,
        MenuItem.OnActionExpandListener {

    private SwitchBarWithFixedTitle mSwitchBarFixedTitle;// for verizon
    private static final String LOG_TAG = "SettingsActivity";

    private static final int LOADER_ID_INDEXABLE_CONTENT_MONITOR = 1;

    //jeson_li, constants for asus cover
    protected static final String FEATURE_ASUS_TRANSCOVER = "asus.hardware.transcover";
    protected static final String FEATURE_ASUS_TRANSCOVER_INFO = "asus.hardware.transcover_info";
    protected static final String FEATURE_ASUS_TRANSCOVER_SETTING_COVER1 = "asus.hardware.transcover_version1";
    protected static final String FEATURE_ASUS_TRANSCOVER_SETTING_COVER2 = "asus.hardware.transcover_version2";
    protected static final String FEATURE_ASUS_TRANSCOVER_SETTING_COVER3 = "asus.hardware.transcover_version3";
    public static final String FLIPCOVER3_ACTION="com.asus.flipcover3.action.COVER_SETTING";
    protected static final String FLIPCOVER3_PKG="com.asus.flipcover3";
    public static final String FLIPCOVER2_ACTION="com.asus.flipcover2.action.COVER_SETTING";
    protected static final String FLIPCOVER2_PKG="com.asus.flipcover2";
    public static final String FLIPCOVER_ACTION="com.asus.flipcover.action.COVER_SETTING";
    protected static final String FLIPCOVER_PKG="com.asus.flipcover";

    // Constants for state save/restore
    private static final String SAVE_KEY_CATEGORIES = ":settings:categories";
    private static final String SAVE_KEY_SEARCH_MENU_EXPANDED = ":settings:search_menu_expanded";
    private static final String SAVE_KEY_SEARCH_QUERY = ":settings:search_query";
    private static final String SAVE_KEY_SHOW_HOME_AS_UP = ":settings:show_home_as_up";
    private static final String SAVE_KEY_SHOW_SEARCH = ":settings:show_search";
    private static final String SAVE_KEY_HOME_ACTIVITIES_COUNT = ":settings:home_activities_count";

    /**
     * When starting this activity, the invoking Intent can contain this extra
     * string to specify which fragment should be initially displayed.
     * <p/>Starting from Key Lime Pie, when this argument is passed in, the activity
     * will call isValidFragment() to confirm that the fragment class name is valid for this
     * activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT = ":settings:show_fragment";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * this extra can also be specified to supply a Bundle of arguments to pass
     * to that fragment when it is instantiated during the initial creation
     * of the activity.
     */
    public static final String EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args";

    /**
     * Fragment "key" argument passed thru {@link #EXTRA_SHOW_FRAGMENT_ARGUMENTS}
     */
    public static final String EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key";

    public static final String BACK_STACK_PREFS = ":settings:prefs";

    // extras that allow any preference activity to be launched as part of a wizard

    // show Back and Next buttons? takes boolean parameter
    // Back will then return RESULT_CANCELED and Next RESULT_OK
    protected static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";

    // add a Skip button?
    private static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";

    // specify custom text for the Back or Next buttons, or cause a button to not appear
    // at all by setting it to null
    protected static final String EXTRA_PREFS_SET_NEXT_TEXT = "extra_prefs_set_next_text";
    protected static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";

    /**
     * When starting this activity and using {@link #EXTRA_SHOW_FRAGMENT},
     * those extra can also be specify to supply the title or title res id to be shown for
     * that fragment.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE = ":settings:show_fragment_title";
    /**
     * The package name used to resolve the title resource id.
     */
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME =
            ":settings:show_fragment_title_res_package_name";
    public static final String EXTRA_SHOW_FRAGMENT_TITLE_RESID =
            ":settings:show_fragment_title_resid";
    public static final String EXTRA_SHOW_FRAGMENT_AS_SHORTCUT =
            ":settings:show_fragment_as_shortcut";

    public static final String EXTRA_SHOW_FRAGMENT_AS_SUBSETTING =
            ":settings:show_fragment_as_subsetting";

    public static final String EXTRA_HIDE_DRAWER = ":settings:hide_drawer";

    public static final String META_DATA_KEY_FRAGMENT_CLASS =
        "com.android.settings.FRAGMENT_CLASS";

    private static final String EXTRA_UI_OPTIONS = "settings:ui_options";

    private static final String EMPTY_QUERY = "";

    private static final int REQUEST_SUGGESTION = 42;

    public static final String EXTRA_APPLICATIONS_TYPE = "applications_type";
    public static final int EXTRA_APPLICATIONS_TYPE_INSTALLED = 0;
    public static final int EXTRA_APPLICATIONS_TYPE_ALL = 1;
    public static final int EXTRA_APPLICATIONS_TYPE_RUNNING_SERVICE = 2;
    public static final int EXTRA_APPLICATIONS_TYPE_BACKGROUND = 3;
    public static final int EXTRA_APPLICATIONS_TYPE_NOTIFY =4;
    public static final int EXTRA_APPLICATIONS_TYPE_NOTIFY_IMPORTANCE=5;
    public static final int EXTRA_APPLICATIONS_TYPE_NOTIFY_HEADSUP=6;
    public static final int EXTRA_APPLICATIONS_TYPE_NOTIFY_ALLOWED=7;
    private String mFragmentClass;

    private CharSequence mInitialTitle;
    private int mInitialTitleResId;

    // Show only these settings for restricted users
    private String[] SETTINGS_FOR_RESTRICTED = {
            //wireless_section
            WifiSettingsActivity.class.getName(),
            Settings.BluetoothSettingsActivity.class.getName(),
            // +++ ckenken (ChiaHsiang_Kuo) @ 20160908 N-Porting: Add CallSettings and MultiSimSettings
            Settings.CallSettingsActivity.class.getName(),
            Settings.MultiSimSettingsActivity.class.getName(),
            // +++ ckenken (ChiaHsiang_Kuo) @ 20160908 N-Porting: Add CallSettings and MultiSimSettings
            // +++ Millie_Chang, Verizon spec : Verizon spec : Advanced Calling
            Settings.AdvancedCallingActivity.class.getName(),
            // --- Millie_Chang, Verizon spec : Verizon spec : Advanced Calling
            Settings.DataUsageSummaryActivity.class.getName(),
            Settings.SimSettingsActivity.class.getName(),
            Settings.WirelessSettingsActivity.class.getName(),
            //device_section
            Settings.HomeSettingsActivity.class.getName(),
            Settings.SoundSettingsActivity.class.getName(),
            Settings.DisplaySettingsActivity.class.getName(),
            Settings.StorageSettingsActivity.class.getName(),
            Settings.ManageApplicationsActivity.class.getName(),
            Settings.ManageApplicationsCtsActivity.class.getName(),
            Settings.PowerUsageSummaryActivity.class.getName(),
            Settings.GestureSettingsActivity.class.getName(),
            //Flip Cover+++
            Settings.AsusCoverSettingsActivity.class.getName(),
            Settings.AsusCover3SettingsActivity.class.getName(),
            //Flip Cover---
            AlwaysOnUtils.getSettingsClassName(),  // [Always-on Panel][AlwaysOn], added by Mingszu Liang, 2017.01.12.
            //personal_section
            Settings.LocationSettingsActivity.class.getName(),
            Settings.SecuritySettingsActivity.class.getName(),
            Settings.InputMethodAndLanguageSettingsActivity.class.getName(),
            Settings.UserSettingsActivity.class.getName(),
            Settings.AccountSettingsActivity.class.getName(),
            Settings.AsusCNAccountSettingsActivity.class.getName(),
            Settings.AsusLockScreenSettingsActivity.class.getName(),
            Settings.AsusEasyLauncherMoreSettingsActivity.class.getName(),
            //system_section
            Settings.DateTimeSettingsActivity.class.getName(),
            Settings.DeviceInfoSettingsActivity.class.getName(),
            Settings.AccessibilitySettingsActivity.class.getName(),
            //jeson_li: add for floatingdock
            Settings.AccessibilityServicesListActivity.class.getName(),
            Settings.PrintSettingsActivity.class.getName(),
            Settings.PaymentSettingsActivity.class.getName(),
    };

    private static final String[] SETTINGS_FOR_EASYMODE = {
            WifiSettingsActivity.class.getName(),
            Settings.BluetoothSettingsActivity.class.getName(),
            Settings.DisplaySettingsActivity.class.getName(),
            Settings.SoundSettingsActivity.class.getName(),
            Settings.StorageSettingsActivity.class.getName(),
            Settings.AsusEasyLauncherSettingsActivity.class.getName(),
            Settings.AsusEasyLauncherMoreSettingsActivity.class.getName(),
    };

    private static final String[] ENTRY_FRAGMENTS = {
            WirelessSettings.class.getName(),
            WifiSettings.class.getName(),
            AdvancedWifiSettings.class.getName(),
            SavedAccessPointsWifiSettings.class.getName(),
            BluetoothSettings.class.getName(),
            SimSettings.class.getName(),
            // +++ ckenken (ChiaHsiang_Kuo) @ 20160908 N-Porting: Add CallSettings and MultiSimSettings
            CallSettings.class.getName(),
            MultiSimSettings.class.getName(),
            // +++ ckenken (ChiaHsiang_Kuo) @ 20160908 N-Porting: Add CallSettings and MultiSimSettings
            // +++ Millie_Chang, Verizon spec : Verizon spec : Advanced Calling
            AdvancedCalling.class.getName(),
            // --- Millie_Chang, Verizon spec : Verizon spec : Advanced Calling
            EthernetSettings.class.getName(),
            TetherSettings.class.getName(),
            WifiP2pSettings.class.getName(),
            VpnSettings.class.getName(),
            DateTimeSettings.class.getName(),
            LocaleListEditor.class.getName(),
            InputMethodAndLanguageSettings.class.getName(),
            AvailableVirtualKeyboardFragment.class.getName(),
            SpellCheckersSettings.class.getName(),
            UserDictionaryList.class.getName(),
            UserDictionarySettings.class.getName(),
            HomeSettings.class.getName(),
            DisplaySettings.class.getName(),
            DeviceInfoSettings.class.getName(),
            ManageApplications.class.getName(),
            CNManageAppsEntry.class.getName(),
            NotificationApps.class.getName(),
            ManageAssist.class.getName(),
            ProcessStatsUi.class.getName(),
            NotificationStation.class.getName(),
            LocationSettings.class.getName(),
            SecuritySettings.class.getName(),
            UsageAccessDetails.class.getName(),
            PrivacySettings.class.getName(),
            DeviceAdminSettings.class.getName(),
            AccessibilitySettings.class.getName(),
            //jeson_li: add for floatingdock
            ServicesFragment.class.getName(),
            AccessibilitySettingsForSetupWizard.class.getName(),
            CaptionPropertiesFragment.class.getName(),
            com.android.settings.accessibility.ToggleDaltonizerPreferenceFragment.class.getName(),
            TextToSpeechSettings.class.getName(),
            StorageSettings.class.getName(),
            PrivateVolumeForget.class.getName(),
            PrivateVolumeSettings.class.getName(),
            PublicVolumeSettings.class.getName(),
            DevelopmentSettings.class.getName(),
            AndroidBeam.class.getName(),
            WifiDisplaySettings.class.getName(),
            PowerUsageSummary.class.getName(),
            AccountSyncSettings.class.getName(),
            AccountSettings.class.getName(),
            GestureSettings.class.getName(),
            AsusCNAccountSettings.class.getName(),
            DataUsageSummary.class.getName(),
            DreamSettings.class.getName(),
            UserSettings.class.getName(),
            ScreenPinningSettings.class.getName(),
            NotificationAccessSettings.class.getName(),
            ZenAccessSettings.class.getName(),
            PrintSettingsFragment.class.getName(),
            PrintJobSettingsFragment.class.getName(),
            TrustedCredentialsSettings.class.getName(),
            PaymentSettings.class.getName(),
            KeyboardLayoutPickerFragment.class.getName(),
            KeyboardLayoutPickerFragment2.class.getName(),
            PhysicalKeyboardFragment.class.getName(),
            ZenModeSettings.class.getName(),
            SoundSettings.class.getName(),
            ConfigureNotificationSettings.class.getName(),
            ChooseLockPassword.ChooseLockPasswordFragment.class.getName(),
            ChooseLockPattern.ChooseLockPatternFragment.class.getName(),
            InstalledAppDetails.class.getName(),
            //BatterySaverSettings.class.getName(),
            AppNotificationSettings.class.getName(),
            OtherSoundSettings.class.getName(),
            ApnSettings.class.getName(),
            ApnEditor.class.getName(),
            WifiCallingSettings.class.getName(),
            ZenModePrioritySettings.class.getName(),
            ZenModeAutomationSettings.class.getName(),
            ZenModeScheduleRuleSettings.class.getName(),
            ZenModeEventRuleSettings.class.getName(),
            ZenModeVisualInterruptionSettings.class.getName(),
            ProcessStatsUi.class.getName(),
            PowerUsageDetail.class.getName(),
            ProcessStatsSummary.class.getName(),
            DrawOverlayDetails.class.getName(),
            WriteSettingsDetails.class.getName(),
            AdvancedAppSettings.class.getName(),
            WallpaperTypeSettings.class.getName(),
            VrListenerSettings.class.getName(),
            ManagedProfileSettings.class.getName(),
            ChooseAccountActivity.class.getName(),
            IccLockSettings.class.getName(),
            ImeiInformation.class.getName(),
            SimStatus.class.getName(),
            Status.class.getName(),
            TestingSettings.class.getName(),
            WifiAPITest.class.getName(),
            WifiInfo.class.getName(),
            MasterClear.class.getName(),
            NightDisplaySettings.class.getName(),
            ManageDomainUrls.class.getName(),
            AutomaticStorageManagerSettings.class.getName(),
            CustomizeSettings.class.getName(),
            PowerSettings.class.getName(),
            ZenUIUpdateSettings.class.getName(),

            SystemUpdateSettings.class.getName(),
            AsusLockScreenSettings.class.getName(),
            //FlipCover+++
            AsusCoverSettings.class.getName(),
            //FlipCover---
            AsusThemeAppSettings.class.getName(),
            AsusFingerprintSettings.class.getName(),
            AsusKidsLauncherSettings.class.getName(),
            AsusEasyLauncherSettings.class.getName(),
            //ZenMotion
            AsusZenMotionSettings.class.getName(),
            AsusMotionSettings.class.getName(),
            AsusTouchSettings.class.getName(),
            //ZenMotion2+++
            AsusZenMotion2Settings.class.getName(),
            AsusGestureQuickStart.class.getName(),
            AppList.class.getName(),
            GestureToturial.class.getName(),
            //ZenMotion2---
            OneHandModeSettings.class.getName(),
            MemcSettings.class.getName(),
            MemcSettingsVerizon.class.getName(), //smilefish for visualmaster
            DownloadsSettings.class.getName(),
            ScreenshotSettings.class.getName(),
	    //blenda, add MoreSettings class
            MoreSettings.class.getName(),
            AsusTwinAppsSettings.class.getName(), //[TwinApps]
            //--- Ashen_Gu@2016/10/11 add for CN version ---
            //ZenModeWhiteListSettings.class.getName(),
            //--- Ashen_Gu@2016/10/11 ---
            //jack_qi, add manage permission
            ManagePermissionSettings.class.getName(),
            CNStatusBarNotificationEntry.class.getName(),
            CNNotificationManagerEntry.class.getName(),
            CNAppNotifySettings.class.getName(),
    };

    private static final String[] LIKE_SHORTCUT_INTENT_ACTION_ARRAY = {
            "android.settings.APPLICATION_DETAILS_SETTINGS"
    };

    private static final String FONT_SIZE_DATA = "font_size_data";
    private static final String EASY_MODE_FONT_SIZE = "easy_mode_font_size";
    private static final String NORMAL_MODE_FONT_SIZE = "normal_mode_font_size";
    private static final int LARGE_FONT_SIZE_INDEX = 2;
    private static final int NORMAL_FONT_SIZE_INDEX = 1;

    private static boolean sIsEasyLauncherMoreAttached;
    private SharedPreferences mDevelopmentPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener mDevelopmentPreferencesListener;
    private SharedPreferences mDisplayPreferences;

    private boolean mBatteryPresent = true;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean batteryPresent = Utils.isBatteryPresent(intent);

                if (mBatteryPresent != batteryPresent) {
                    mBatteryPresent = batteryPresent;
                    updateTilesList();
                }
            }
        }
    };

    private final BroadcastReceiver mUserAddRemoveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_USER_ADDED)
                    || action.equals(Intent.ACTION_USER_REMOVED)) {
                Index.getInstance(getApplicationContext()).update();
            }
        }
    };

    private final ContentObserver mEasyLauncherObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange ( boolean selfChange) {
            updateEasyLauncherChanged();
        }
    };

    private final DynamicIndexableContentMonitor mDynamicIndexableContentMonitor =
            new DynamicIndexableContentMonitor();

    private ActionBar mActionBar;
    private SwitchBar mSwitchBar;
    //+++ suleman
    private AsusButtonBar buttonBar;
    //--- suleamn

    private Button mNextButton;

    private boolean mDisplayHomeAsUpEnabled;
    private boolean mDisplaySearch;

    private boolean mIsShowingDashboard;
    private boolean mIsShortcut;

    private ViewGroup mContent;

    private AsusSearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private boolean mSearchMenuItemExpanded = false;
    private SearchResultsSummary mSearchResultsFragment;
    private String mSearchQuery;

    // Categories
    private ArrayList<DashboardCategory> mCategories = new ArrayList<DashboardCategory>();

    private static final String MSG_DATA_FORCE_REFRESH = "msg_data_force_refresh";

    private boolean mNeedToRevertToInitialFragment = false;

    private Intent mResultIntentData;
    private ComponentName mCurrentSuggestion;

    public SwitchBar getSwitchBar() {
        return mSwitchBar;
    }

    //+++ suleman
    public AsusButtonBar getButtonBar() {
        return buttonBar;
    }
    //--- suleamn
    private boolean mEnableTwinApps = false; //[TwinApps]

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        startPreferencePanel(pref.getFragment(), pref.getExtras(), -1, pref.getTitle(),
                null, 0);
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // +++ rock_huang@20151207: Avoid Invalid int cause search.Index.addIndexablesForRawDataUri crash
        try {
            Index.getInstance(this).update();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // --- rock_huang@20151207: Avoid Invalid int cause search.Index.addIndexablesForRawDataUri crash
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mDisplaySearch) {
            return false;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        // Cache the search query (can be overriden by the OnQueryTextListener)
        final String query = mSearchQuery;

        mSearchMenuItem = menu.findItem(R.id.search);

        if (mSearchMenuItem == null || mSearchView == null) {
            return false;
        }
/*
        mSearchMenuItem.setOnActionExpandListener(this);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);*/

        if (mSearchMenuItemExpanded) {
            expandActionBar();
            switchToSearchResultsFragmentIfNeeded();
//            mSearchMenuItem.expandActionView();
        }else{
            collapseActionBar();
        }
//        mSearchView.setQuery(query, true /* submit */);

        return true;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (name.equals(getPackageName() + "_preferences")) {
            return new SharedPreferencesLogger(this, getMetricsTag());
        }
        return super.getSharedPreferences(name, mode);
    }

    private String getMetricsTag() {
        String tag = getClass().getName();
        if (getIntent() != null && getIntent().hasExtra(EXTRA_SHOW_FRAGMENT)) {
            tag = getIntent().getStringExtra(EXTRA_SHOW_FRAGMENT);
        }
        if (tag.startsWith("com.android.settings.")) {
            tag = tag.replace("com.android.settings.", "");
        }
        return tag;
    }

    private static boolean isShortCutIntent(final Intent intent) {
        Set<String> categories = intent.getCategories();
        return (categories != null) && categories.contains("com.android.settings.SHORTCUT");
    }

    private static boolean isLikeShortCutIntent(final Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return false;
        }
        for (int i = 0; i < LIKE_SHORTCUT_INTENT_ACTION_ARRAY.length; i++) {
            if (LIKE_SHORTCUT_INTENT_ACTION_ARRAY[i].equals(action)) return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        long startTime = System.currentTimeMillis();

        // Should happen before any call to getIntent()
        getMetaData();

        final Intent intent = getIntent();

        //+++ tim_hu@asus.com
        doUpdateNFCTilesList();
        //---

        //++chrisit_chang App link
        //catch the first launch action of AppLink function
        activateAppLink(intent);

        if (intent.hasExtra(EXTRA_UI_OPTIONS)) {
            getWindow().setUiOptions(intent.getIntExtra(EXTRA_UI_OPTIONS, 0));
        }
        if (intent.getBooleanExtra(EXTRA_HIDE_DRAWER, false)) {
            setIsDrawerPresent(false);
        }

        mDevelopmentPreferences = getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE);
        mDisplayPreferences = getSharedPreferences(FONT_SIZE_DATA, Context.MODE_PRIVATE);

        // Getting Intent properties can only be done after the super.onCreate(...)
        final String initialFragmentName = intent.getStringExtra(EXTRA_SHOW_FRAGMENT);

        mIsShortcut = isShortCutIntent(intent) || isLikeShortCutIntent(intent) ||
                intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SHORTCUT, false);

        final ComponentName cn = intent.getComponent();
        final String className = cn.getClassName();

        mIsShowingDashboard = className.equals(Settings.class.getName())
                || className.equals(Settings.WirelessSettings.class.getName())
                || className.equals(Settings.DeviceSystemSettings.class.getName())
                || className.equals(Settings.PersonalSettings.class.getName())
                || className.equals(Settings.WirelessSettings.class.getName())
                || className.equals(Settings.AsusEasyLauncherMoreSettingsActivity.class.getName())
                || className.equals(Settings.AccountSettings.class.getName())
                || className.equals(Settings.PhoneSettings.class.getName());

        if(isEasyLauncherMore()){
            sIsEasyLauncherMoreAttached = true;
        }else if(className.equals(Settings.class.getName())){
            sIsEasyLauncherMoreAttached = false;
        }

        // This is a "Sub Settings" when:
        // - this is a real SubSettings
        // - or :settings:show_fragment_as_subsetting is passed to the Intent
        final boolean isSubSettings = this instanceof SubSettings ||
                intent.getBooleanExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, false);
        // If this is a sub settings, then apply the SubSettings Theme for the ActionBar content insets
        if (isSubSettings) {
            // Check also that we are not a Theme Dialog as we don't want to override them
            final int themeResId = getThemeResId();
            if (themeResId != R.style.Theme_DialogWhenLarge &&
                    themeResId != R.style.Theme_SubSettingsDialogWhenLarge) {
                setTheme(R.style.Theme_SubSettings);
            }
        }

        setContentView(mIsShowingDashboard ?
                R.layout.settings_main_dashboard : R.layout.settings_main_prefs);

        mContent = (ViewGroup) findViewById(R.id.main_content);

        getFragmentManager().addOnBackStackChangedListener(this);

        if (mIsShowingDashboard) {
            // Run the Index update only if we have some space
            if (!Utils.isLowStorage(this)) {
                long indexStartTime = System.currentTimeMillis();
                Index.getInstance(getApplicationContext()).update();
                if (DEBUG_TIMING) Log.d(LOG_TAG, "Index.update() took "
                        + (System.currentTimeMillis() - indexStartTime) + " ms");
            } else {
                Log.w(LOG_TAG, "Cannot update the Indexer as we are running low on storage space!");
            }
        }

        if (savedState != null) {
            // We are restarting from a previous saved state; used that to initialize, instead
            // of starting fresh.
            mSearchMenuItemExpanded = savedState.getBoolean(SAVE_KEY_SEARCH_MENU_EXPANDED);
            mSearchQuery = savedState.getString(SAVE_KEY_SEARCH_QUERY);
            setTitleFromIntent(intent);

            ArrayList<DashboardCategory> categories =
                    savedState.getParcelableArrayList(SAVE_KEY_CATEGORIES);
            if (categories != null) {
                mCategories.clear();
                mCategories.addAll(categories);
                setTitleFromBackStack();
            }

            mDisplayHomeAsUpEnabled = savedState.getBoolean(SAVE_KEY_SHOW_HOME_AS_UP);
            mDisplaySearch = savedState.getBoolean(SAVE_KEY_SHOW_SEARCH);

        } else {
            if (!mIsShowingDashboard) {
                mDisplaySearch = false;
                // UP will be shown only if it is a sub settings
                if (mIsShortcut) {
                    mDisplayHomeAsUpEnabled = isSubSettings;
                } else if (isSubSettings) {
                    mDisplayHomeAsUpEnabled = true;
                } else {
                    mDisplayHomeAsUpEnabled = false;
                }

                setTitleFromIntent(intent);

                Bundle initialArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS);
                switchToFragment(initialFragmentName, initialArguments, true, false,
                        mInitialTitleResId, mInitialTitle, false);
            } else {
                // No UP affordance if we are displaying the main Dashboard
                mDisplayHomeAsUpEnabled = false;
                // Show Search affordance
                mDisplaySearch = true;
                mInitialTitleResId = R.string.dashboard_title;
                if(isEasyLauncherMore()){
                    setTitle(mInitialTitleResId);
                }

                // add argument to indicate which settings tab should be initially selected
                final Bundle args = new Bundle();
                final String extraName = DashboardContainerFragment.EXTRA_SELECT_SETTINGS_TAB;
                args.putString(extraName, intent.getStringExtra(extraName));

                switchToFragment(DashboardContainerFragment.class.getName(), args, false, false,
                        mInitialTitleResId, mInitialTitle, false);
            }
        }

        mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(false);
            mActionBar.setHomeButtonEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(false);
        }
        setActionBarBackButton(mIsShowingDashboard ? false : true);
        mSwitchBar = (SwitchBar) findViewById(R.id.switch_bar);
        if (mSwitchBar != null) {
            mSwitchBar.setMetricsTag(getMetricsTag());
        }
        if (Utils.isVerizonSKU()) //for verizon
            //if(!Utils.isVerizonSKU()) //for verizon
            mSwitchBarFixedTitle = (SwitchBarWithFixedTitle) findViewById(R.id.switch_bar_fixed_title);

        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        window.setStatusBarColor(Color.TRANSPARENT);
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//        window.setStatusBarColor(getColor(R.color.action_bar_background));

        if (mIsShowingDashboard){
            setToolBarTitleTextGrave(Gravity.CENTER);
        }

        //+++ suleman
        buttonBar = (AsusButtonBar) findViewById(R.id.bottom_button_bar);
        //--- suleamn

        // see if we should show Back/Next buttons
        if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)) {

            View buttonBar = findViewById(R.id.button_bar);
            if (buttonBar != null) {
                buttonBar.setVisibility(View.VISIBLE);

                Button backButton = (Button)findViewById(R.id.back_button);
                backButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_CANCELED, getResultIntentData());
                        finish();
                    }
                });
                Button skipButton = (Button)findViewById(R.id.skip_button);
                skipButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK, getResultIntentData());
                        finish();
                    }
                });
                mNextButton = (Button)findViewById(R.id.next_button);
                mNextButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        setResult(RESULT_OK, getResultIntentData());
                        finish();
                    }
                });

                // set our various button parameters
                if (intent.hasExtra(EXTRA_PREFS_SET_NEXT_TEXT)) {
                    String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_NEXT_TEXT);
                    if (TextUtils.isEmpty(buttonText)) {
                        mNextButton.setVisibility(View.GONE);
                    }
                    else {
                        mNextButton.setText(buttonText);
                    }
                }
                if (intent.hasExtra(EXTRA_PREFS_SET_BACK_TEXT)) {
                    String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_BACK_TEXT);
                    if (TextUtils.isEmpty(buttonText)) {
                        backButton.setVisibility(View.GONE);
                    }
                    else {
                        backButton.setText(buttonText);
                    }
                }
                if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_SKIP, false)) {
                    skipButton.setVisibility(View.VISIBLE);
                }
            }
        }

        if(mIsShowingDashboard && ResCustomizeConfig.hasConfigFile())
            ResCustomizeConfig.parsingConfig();

        //[TwinApps] {
        mEnableTwinApps = TwinAppsUtil.isTwinAppsSupport(getApplicationContext());
        //[TwinApps] }

        if (DEBUG_TIMING) Log.d(LOG_TAG, "onCreate took " + (System.currentTimeMillis() - startTime)
                + " ms");

        if (mSearchView == null){
            mSearchView = new AsusSearchView(this);
        }

        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.setQuery(mSearchQuery, false /* submit */);

    }

    public void setDisplaySearchMenu(boolean displaySearch) {
        if (displaySearch != mDisplaySearch) {
            mDisplaySearch = displaySearch;
            invalidateOptionsMenu();
        }
    }

    private void setTitleFromIntent(Intent intent) {
        final int initialTitleResId = intent.getIntExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
        if (initialTitleResId > 0) {
            mInitialTitle = null;
            mInitialTitleResId = initialTitleResId;

            final String initialTitleResPackageName = intent.getStringExtra(
                    EXTRA_SHOW_FRAGMENT_TITLE_RES_PACKAGE_NAME);
            if (initialTitleResPackageName != null) {
                try {
                    Context authContext = createPackageContextAsUser(initialTitleResPackageName,
                            0 /* flags */, new UserHandle(UserHandle.myUserId()));
                    mInitialTitle = authContext.getResources().getText(mInitialTitleResId);
                    setTitle(mInitialTitle);
                    mInitialTitleResId = -1;
                    return;
                } catch (NameNotFoundException e) {
                    Log.w(LOG_TAG, "Could not find package" + initialTitleResPackageName);
                }
            } else {
                setTitle(mInitialTitleResId);
            }
        } else {
            mInitialTitleResId = -1;
            final String initialTitle = intent.getStringExtra(EXTRA_SHOW_FRAGMENT_TITLE);
            mInitialTitle = (initialTitle != null) ? initialTitle : getTitle();
            setTitle(mInitialTitle);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(getActionBar() != null){
        	getActionBar().setHomeAsUpIndicator(R.drawable.asusres_ic_ab_back_holo_light);
        	getActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.search){
            expandActionBar();
            switchToSearchResultsFragmentIfNeeded();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackStackChanged() {
        setTitleFromBackStack();
    }

    private void setTitleFromBackStack() {
        final int count = getFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            if (mInitialTitleResId > 0) {
                setTitle(mInitialTitleResId);
            } else {
                setTitle(mInitialTitle);
            }
            return;
        }

        FragmentManager.BackStackEntry bse = getFragmentManager().getBackStackEntryAt(count - 1);
        setTitleFromBackStackEntry(bse);
    }

    private void setTitleFromBackStackEntry(FragmentManager.BackStackEntry bse) {
        final CharSequence title;
        final int titleRes = bse.getBreadCrumbTitleRes();
        if (titleRes > 0) {
            title = getText(titleRes);
        } else {
            title = bse.getBreadCrumbTitle();
        }
        if (title != null) {
            setTitle(title);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mCategories.size() > 0) {
            outState.putParcelableArrayList(SAVE_KEY_CATEGORIES, mCategories);
        }

        outState.putBoolean(SAVE_KEY_SHOW_HOME_AS_UP, mDisplayHomeAsUpEnabled);
        outState.putBoolean(SAVE_KEY_SHOW_SEARCH, mDisplaySearch);

        if (mDisplaySearch) {
            // The option menus are created if the ActionBar is visible and they are also created
            // asynchronously. If you launch Settings with an Intent action like
            // android.intent.action.POWER_USAGE_SUMMARY and at the same time your device is locked
            // thru a LockScreen, onCreateOptionsMenu() is not yet called and references to the search
            // menu item and search view are null.
            boolean isExpanded = (mSearchMenuItem != null) && mSearchMenuItemExpanded;
            outState.putBoolean(SAVE_KEY_SEARCH_MENU_EXPANDED, isExpanded);

            String query = (mSearchView != null) ? mSearchView.getQuery().toString() : EMPTY_QUERY;
            outState.putString(SAVE_KEY_SEARCH_QUERY, query);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mNeedToRevertToInitialFragment) {
            revertToInitialFragment();
        }

        mDevelopmentPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                updateTilesList();
            }
        };
        mDevelopmentPreferences.registerOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        registerReceiver(mUserAddRemoveReceiver, new IntentFilter(Intent.ACTION_USER_ADDED));
        registerReceiver(mUserAddRemoveReceiver, new IntentFilter(Intent.ACTION_USER_REMOVED));

        mDynamicIndexableContentMonitor.register(this, LOADER_ID_INDEXABLE_CONTENT_MONITOR);

        getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor(
                android.provider.Settings.System.ASUS_EASY_LAUNCHER), true, mEasyLauncherObserver);

        if(mDisplaySearch && !TextUtils.isEmpty(mSearchQuery) && mSearchMenuItemExpanded) {
            switchToSearchResultsFragmentIfNeeded(false);
            mSearchResultsFragment.onQueryTextSubmit(mSearchQuery);
//            onQueryTextSubmit(mSearchQuery);
        }

        updateTilesList();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mBatteryInfoReceiver);
        unregisterReceiver(mUserAddRemoveReceiver);
        mDynamicIndexableContentMonitor.unregister();
        getContentResolver().unregisterContentObserver(mEasyLauncherObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10682 Step-by-step tutorials
        if(VerizonHelpUtils.isVerizonMachine()){
            VerizonHelpUtils.closeTutorial(this);
        }
        // ---
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mDevelopmentPreferences.unregisterOnSharedPreferenceChangeListener(
                mDevelopmentPreferencesListener);
        mDevelopmentPreferencesListener = null;
    }

    protected boolean isValidFragment(String fragmentName) {
        // Almost all fragments are wrapped in this,
        // except for a few that have their own activities.
        for (int i = 0; i < ENTRY_FRAGMENTS.length; i++) {
            if (ENTRY_FRAGMENTS[i].equals(fragmentName)) return true;
        }
        return false;
    }

    @Override
    public Intent getIntent() {
        Intent superIntent = super.getIntent();
        String startingFragment = getStartingFragmentClass(superIntent);
        // This is called from super.onCreate, isMultiPane() is not yet reliable
        // Do not use onIsHidingHeaders either, which relies itself on this method
        if (startingFragment != null) {
            Intent modIntent = new Intent(superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT, startingFragment);
            Bundle args = superIntent.getExtras();
            if (args != null) {
                args = new Bundle(args);
            } else {
                args = new Bundle();
            }
            args.putParcelable("intent", superIntent);
            modIntent.putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, args);
            return modIntent;
        }
        else
        {
            Log.d(LOG_TAG,"StartingFragment is null");
        }
        return superIntent;
    }

    /**
     * Checks if the component name in the intent is different from the Settings class and
     * returns the class name to load as a fragment.
     */
    private String getStartingFragmentClass(Intent intent) {
        if (mFragmentClass != null) return mFragmentClass;

        String intentClass = intent.getComponent().getClassName();
        if (intentClass.equals(getClass().getName())) return null;

        if ("com.android.settings.ManageApplications".equals(intentClass)
                || "com.android.settings.RunningServices".equals(intentClass)
                || "com.android.settings.applications.StorageUse".equals(intentClass)) {
            // Old names of manage apps.
            intentClass = com.android.settings.applications.ManageApplications.class.getName();
        }

        return intentClass;
    }

    /**
     * Start a new fragment containing a preference panel.  If the preferences
     * are being displayed in multi-pane mode, the given fragment class will
     * be instantiated and placed in the appropriate pane.  If running in
     * single-pane mode, a new activity will be launched in which to show the
     * fragment.
     *
     * @param fragmentClass Full name of the class implementing the fragment.
     * @param args Any desired arguments to supply to the fragment.
     * @param titleRes Optional resource identifier of the title of this
     * fragment.
     * @param titleText Optional text of the title of this fragment.
     * @param resultTo Optional fragment that result data should be sent to.
     * If non-null, resultTo.onActivityResult() will be called when this
     * preference panel is done.  The launched panel must use
     * {@link #finishPreferencePanel(Fragment, int, Intent)} when done.
     * @param resultRequestCode If resultTo is non-null, this is the caller's
     * request code to be received with the result.
     */
    public void startPreferencePanel(String fragmentClass, Bundle args, int titleRes,
            CharSequence titleText, Fragment resultTo, int resultRequestCode) {
        String title = null;
        if (titleRes < 0) {
            if (titleText != null) {
                title = titleText.toString();
            } else {
                // There not much we can do in that case
                title = "";
            }
        }
        Utils.startWithFragment(this, fragmentClass, args, resultTo, resultRequestCode,
                titleRes, title, mIsShortcut);
    }

    /**
     * Start a new fragment in a new activity containing a preference panel for a given user. If the
     * preferences are being displayed in multi-pane mode, the given fragment class will be
     * instantiated and placed in the appropriate pane. If running in single-pane mode, a new
     * activity will be launched in which to show the fragment.
     *
     * @param fragmentClass Full name of the class implementing the fragment.
     * @param args Any desired arguments to supply to the fragment.
     * @param titleRes Optional resource identifier of the title of this fragment.
     * @param titleText Optional text of the title of this fragment.
     * @param userHandle The user for which the panel has to be started.
     */
    public void startPreferencePanelAsUser(String fragmentClass, Bundle args, int titleRes,
            CharSequence titleText, UserHandle userHandle) {
        // This is a workaround.
        //
        // Calling startWithFragmentAsUser() without specifying FLAG_ACTIVITY_NEW_TASK to the intent
        // starting the fragment could cause a native stack corruption. See b/17523189. However,
        // adding that flag and start the preference panel with the same UserHandler will make it
        // impossible to use back button to return to the previous screen. See b/20042570.
        //
        // We work around this issue by adding FLAG_ACTIVITY_NEW_TASK to the intent, while doing
        // another check here to call startPreferencePanel() instead of startWithFragmentAsUser()
        // when we're calling it as the same user.

        //[TwinApps] {
        if (mEnableTwinApps) {
            UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            final UserInfo ui = um.getUserInfo(userHandle.getIdentifier());
            if (userHandle.getIdentifier() == UserHandle.myUserId() || ui.isTwinApps()) {
                startPreferencePanel(fragmentClass, args, titleRes, titleText, null, 0);
            } else {
                String title = null;
                if (titleRes < 0) {
                    if (titleText != null) {
                        title = titleText.toString();
                    } else {
                        // There not much we can do in that case
                        title = "";
                    }
                }
                Utils.startWithFragmentAsUser(this, fragmentClass, args,
                        titleRes, title, mIsShortcut, userHandle);
            }
        } else {
        //[TwinApps] }
            if (userHandle.getIdentifier() == UserHandle.myUserId()) {
                startPreferencePanel(fragmentClass, args, titleRes, titleText, null, 0);
            } else {
                String title = null;
                if (titleRes < 0) {
                    if (titleText != null) {
                        title = titleText.toString();
                    } else {
                        // There not much we can do in that case
                        title = "";
                    }
                }
                Utils.startWithFragmentAsUser(this, fragmentClass, args,
                        titleRes, title, mIsShortcut, userHandle);
            }
        } //[TwinApps]
    }

    /**
     * Called by a preference panel fragment to finish itself.
     *
     * @param caller The fragment that is asking to be finished.
     * @param resultCode Optional result code to send back to the original
     * launching fragment.
     * @param resultData Optional result data to send back to the original
     * launching fragment.
     */
    public void finishPreferencePanel(Fragment caller, int resultCode, Intent resultData) {
        setResult(resultCode, resultData);
        finish();
    }

    /**
     * Start a new fragment.
     *
     * @param fragment The fragment to start
     * @param push If true, the current fragment will be pushed onto the back stack.  If false,
     * the current fragment will be replaced.
     */
    public void startPreferenceFragment(Fragment fragment, boolean push) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, fragment);
        if (push) {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack(BACK_STACK_PREFS);
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
        transaction.commitAllowingStateLoss();
    }

    /**
     * Switch to a specific Fragment with taking care of validation, Title and BackStack
     */
    private Fragment switchToFragment(String fragmentName, Bundle args, boolean validate,
            boolean addToBackStack, int titleResId, CharSequence title, boolean withTransition) {
        if (validate && !isValidFragment(fragmentName)) {
            throw new IllegalArgumentException("Invalid fragment for this activity: "
                    + fragmentName);
        }
        Fragment f = Fragment.instantiate(this, fragmentName, args);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, f);
        if (withTransition) {
            TransitionManager.beginDelayedTransition(mContent);
        }
        if (addToBackStack) {
            transaction.addToBackStack(SettingsActivity.BACK_STACK_PREFS);
        }
        if (titleResId > 0) {
            transaction.setBreadCrumbTitle(titleResId);
        } else if (title != null) {
            transaction.setBreadCrumbTitle(title);
        }
        transaction.commitAllowingStateLoss();
        getFragmentManager().executePendingTransactions();
        return f;
    }

    private void updateTilesList() {
        // Generally the items that are will be changing from these updates will
        // not be in the top list of tiles, so run it in the background and the
        // SettingsDrawerActivity will pick up on the updates automatically.
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                doUpdateTilesList();
            }
        });
    }

    private void doUpdateTilesList() {
        PackageManager pm = getPackageManager();
        final UserManager um = UserManager.get(this);
        final boolean isAdmin = um.isAdminUser();

        String packageName = getPackageName();

        final ComponentName cn = getIntent().getComponent();
        final String className = cn.getClassName();
        if(className.equals(Settings.class.getName())){
            sIsEasyLauncherMoreAttached = false;
        }

//        if(Utils.isEasyLauncher(this)){
//            if(isEasyLauncherMore() || sIsEasyLauncherMoreAttached){
//                clearHideList();
//            }else{
//                List<DashboardCategory> categories = getDashboardCategories();
//                for (DashboardCategory category : categories) {
//                    for (Tile tile : category.tiles) {
//                        ComponentName component = tile.intent.getComponent();
//                        if(!ArrayUtils.contains(SETTINGS_FOR_EASYMODE, component.getClassName())) {
//                            setHideTileUiOnly(component, true);
//                        }
//                    }
//                }
//            }
//        }

        setHideTileUiOnly(new ComponentName(packageName,
                Settings.AsusEasyLauncherMoreSettingsActivity.class.getName()),
                /*!Utils.isEasyLauncher(this)*/true || sIsEasyLauncherMoreAttached);

        setTileEnabled(new ComponentName(packageName, WifiSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_WIFI), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.BluetoothSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH), isAdmin, pm);

        //jack_qi, modified for CNSettings.
//        if (!Utils.isVerizon()){
//            setTileEnabled(new ComponentName(packageName,
//                Settings.TetherSettingsActivity.class.getName()),
//            false, isAdmin, pm);
//        } else {
            setTileEnabled(new ComponentName(packageName,
                Settings.TetherSettingsActivity.class.getName()),
            true, isAdmin, pm);
//        }

        // +++ ckenken (ChiaHsiang_Kuo) @ 20160908 N-Porting: Add CallSettings and MultiSimSettings
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final boolean voiceCapable = tm.isVoiceCapable();

        //jack_qi, hide CallSettings for CNSettings
        setTileEnabled(new ComponentName(packageName,
                Settings.CallSettingsActivity.class.getName()),
                voiceCapable, isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.MultiSimSettingsActivity.class.getName()),
                Utils.isMultiSimEnabled(this), isAdmin, pm);
        // --- ckenken (ChiaHsiang_Kuo) @ 20160908 N-Porting: Add CallSettings and MultiSimSettings

        //jack_qi, move DataUsageSummaryActivity from dashboard to "more"
        setTileEnabled(new ComponentName(packageName,
                Settings.DataUsageSummaryActivity.class.getName()),
                Utils.isBandwidthControlEnabled(), isAdmin, pm);

        // +++ ckenken (ChiaHsiang_Kuo) @ 20160908 N-Porting: Hide SIM Settings Activity because already had DualSIM Settings
        setTileEnabled(new ComponentName(packageName,
                Settings.SimSettingsActivity.class.getName()),
                false/*Utils.showSimCardTile(this)*/, isAdmin, pm);
        // --- ckenken (ChiaHsiang_Kuo) @ 20160908 N-Porting: Hide SIM Settings Activity because already had DualSIM Settings

        // For VZW DND chunghung_lin@asus.com
        // TT-905033, not disable component, but only ui
        setHideTileUiOnly(new ComponentName(packageName,
                Settings.ZenModeSettingsActivity.class.getName()),
                Utils.isVerizonSKU());
        // End For VZW DND chunghung_lin@asus.com

        // +++ Millie_Chang, Verizon spec : Verizon spec : Advanced Calling
        setTileEnabled(new ComponentName(packageName,
                Settings.AdvancedCallingActivity.class.getName()),
                AsusTelephonyUtils.shouldDisplayAdvanceCalling(this), isAdmin, pm);
        // --- Millie_Chang, Verizon spec : Verizon spec : Advanced Calling

        setTileEnabled(new ComponentName(packageName,
                Settings.PowerUsageSummaryActivity.class.getName()),
                mBatteryPresent, isAdmin, pm);

        //For flip cover to show Cover2 or Cover3 entry on settings+++
        int coverVersion = AsusCoverSettings.getCoverVersion(this);
        if (coverVersion == -1) {
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusCoverSettingsActivity.class.getName()),
                    false, isAdmin, pm);
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusCover3SettingsActivity.class.getName()),
                    false, isAdmin, pm);
        } else if (coverVersion == 0 || coverVersion == 1 || coverVersion == 2) {
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusCoverSettingsActivity.class.getName()),
                    true, isAdmin, pm);
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusCover3SettingsActivity.class.getName()),
                    false, isAdmin, pm);
        } else if (coverVersion == 3) {
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusCoverSettingsActivity.class.getName()),
                    false, isAdmin, pm);
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusCover3SettingsActivity.class.getName()),
                    true, isAdmin, pm);
        }
        //For flip cover to show Cover2 or Cover3 entry on settings---

        setTileEnabled(new ComponentName(packageName,
                Settings.UserSettingsActivity.class.getName()),
                UserHandle.MU_ENABLED && UserManager.supportsMultipleUsers()
                // +[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
                && LiveDemoUnit.AccFlagRead() == 0
                // -[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
                && !Utils.isMonkeyRunning(), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                        Settings.WirelessSettingsActivity.class.getName()),
                !UserManager.isDeviceInDemoMode(this), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                        Settings.DateTimeSettingsActivity.class.getName()),
                !UserManager.isDeviceInDemoMode(this), isAdmin, pm);

        updateNFCTilesList(packageName, isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                Settings.PrintSettingsActivity.class.getName()),
                pm.hasSystemFeature(PackageManager.FEATURE_PRINTING), isAdmin, pm);


        final boolean showDev = mDevelopmentPreferences.getBoolean(
                    DevelopmentSettings.PREF_SHOW, android.os.Build.TYPE.equals("eng"))
                && !um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
        setTileEnabled(new ComponentName(packageName,
                        Settings.DevelopmentSettingsActivity.class.getName()),
                showDev, isAdmin, pm);
        //jack_qi

        // Reveal development-only quick settings tiles
        DevelopmentTiles.setTilesEnabled(this, showDev);

        // N Porting: rules for top level tiles (START)
        // TODO: Please check the rules => need update with newest spec
        try {
            getPackageManager().getPackageInfo("com.asus.dm", PackageManager.GET_META_DATA);
        } catch(NameNotFoundException e) {
            // N Porting: hide it later
            /*
            setTileEnabled(new ComponentName(packageName,
                            Settings.SystemUpdateSettingsActivity.class.getName()),
                    false, isAdmin, pm);
                    */
            Log.w(LOG_TAG, "DMClient not installed, remove this header");
        }
        //jack_qi, hide for CNSettings.
        if (Utils.isVerizon() || Utils.isCNSKU() || Utils.isATT() || android.os.SystemProperties.get(
                "ro.build.asus.sku").toUpperCase().startsWith("CUCC")) {
            setTileEnabled(new ComponentName(packageName,
                    Settings.ZenUIUpdateSettingsActivity.class.getName()),
                    false, isAdmin, pm);
            Log.w(LOG_TAG, "ZenUIUpdateSettingsActivity: remove this header");
        }

        // Rules of hiding customizeSettings
        boolean removeCustomizeSettings = !Utils.isAdminUser() || (!CustomizeSettings.isGloveModeExist(this) && CustomizeSettings.isNavigationBarExist()
                && !Utils.isGameGenieExist(this) && !Utils.isVerizonSKU());
        setTileEnabled(new ComponentName(packageName,
                Settings.CustomizeSettingsActivity.class.getName()),
                !removeCustomizeSettings, isAdmin, pm);

        // Hide ScreenshotSettings in Verison sku
        setTileEnabled(new ComponentName(packageName,
                Settings.ScreenshotSettingsActivity.class.getName()),
                !Utils.isVerizonSKU(), isAdmin, pm);

        setTileEnabled(new ComponentName(packageName,
                                Settings.ManagePermissionSettingsActivity.class.getName()),
                ManagePermissionSettings.hasPermissionManager(this), isAdmin, pm);

        //jack_qi

        setTileEnabled(new ComponentName(packageName,
                Settings.AsusThemeAppSettings.class.getName()),
                /*Utils.isThemeAppEnabled(getApplicationContext())*/true, isAdmin, pm);




        // hide fingerprint tile if no HW-feature
        if (!Utils.isFingerprintExist(getApplicationContext())){
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusFingerprintSettings.class.getName()),
                    false, isAdmin, pm);
        }

        // N Porting: rules for top level tiles (END)

            // ZenMotion2+++
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusZenMotion2SettingsActivity.class.getName()),
                   Utils.isCNSKU(), isAdmin, pm);
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusZenMotionSettingsActivity.class.getName()),
                    !Utils.isCNSKU(), isAdmin, pm);
               //     true, isAdmin, pm);
            setTileEnabled(new ComponentName(packageName,
                            Settings.AsusVZWZenMotionSettingsActivity.class.getName()),
                    !Utils.isCNSKU(), isAdmin, pm);
            // ZenMotion2---
        //remove Memory for Verizon
        //jack_qi, remove for CNSettings.
        setTileEnabled(new ComponentName(packageName,
                        Settings.MemorySettingsActivity.class.getName()),
                !Utils.isVerizon(), isAdmin, pm);

        // remove Download for Verizon
        setTileEnabled(new ComponentName(packageName,
                        Settings.DownloadsSettings.class.getName()),
                !Utils.isVerizon(), isAdmin, pm);
        //jack_qi

        // +++ Easy Mode
        //jack_qi,hide for CNSettings
        setTileEnabled(new ComponentName(packageName,
                Settings.AsusEasyLauncherSettingsActivity.class.getName()),
                AsusEasyLauncherSettings.hasEasyMode(this), isAdmin, pm);
        // ---

        // Kids Mode START
        setTileEnabled(new ComponentName(packageName,
                        Settings.AsusKidsLauncherSettingsActivity.class.getName()),
                AsusKidsLauncherSettings.hasKidsMode(this), isAdmin, pm);
        // Kids Mode END
        //jack_qi

		// ZenMotion
        //jack_qi, remvoe for CNSettings
		setTileEnabled(new ComponentName(packageName,
                Settings.AsusZenMotionSettingsActivity.class.getName()),
                !Utils.isVerizonSKU(), isAdmin, pm);
        setTileEnabled(new ComponentName(packageName,
                Settings.AsusVZWZenMotionSettingsActivity.class.getName()),
                Utils.isVerizonSKU(), isAdmin, pm);
        // ZenMotion2
        setTileEnabled(new ComponentName(packageName,
                        Settings.AsusZenMotion2SettingsActivity.class.getName()),
                true, isAdmin, pm);
        //jack_qi
        //jack_qi, hide Google
        try {
            setTileEnabled(new ComponentName("com.google.android.gms",
                            "com.google.android.gms.app.settings.GoogleSettingsLink"),
                    false, isAdmin, pm);
        } catch (Exception e) {
            //ignore
        }
        // jack_qi

        //[TwinApps] {
        //TwinApps is enabled while having FEATURE and the device RAM size is equal or larger than 3G.
        boolean bTwinApps = false;
        if (mEnableTwinApps) {
            final long RAM_BASE = (1024*1024*1024);
            long ramSize = android.os.Process.getTotalMemory();
            if (ramSize >= (2.5*RAM_BASE)) {
                bTwinApps = true;
            }
        }
        setTileEnabled(new ComponentName(packageName, Settings.AsusTwinAppsSettings.class.getName()),
            bTwinApps, isAdmin, pm);
        //[TwinApps] }

        // Added by Mingszu Liang, 2017.01.12. - BEGIN
        //
        //   [Always-on Panel][AlwaysOn]
        //   - if this device desn't support AlwaysOn feature,
        //     we must hide "Always-on Panel" preference.
        //   - if AlwaysOn APK doesn't exist in the device,
        //     setTileEnabled() will throw exception.
        //   - AlwaysOn is a system and platform key app.
        //   - AlwaysOn uses EXTRA_SETTINGS_ACTION plugin mechanism.
        //
        if (AlwaysOnUtils.checkApkExist(this)) {
            final boolean enableAlwaysOnOption = AlwaysOnUtils.supportAlwaysOnFeature(this);
            final boolean hideAlwaysOnOption = !AlwaysOnUtils.showInFirstPage();
            final ComponentName componentAlwaysOn = AlwaysOnUtils.getSettingsComponent();

            try {
                if (enableAlwaysOnOption && hideAlwaysOnOption) {
                    setHideTileUiOnly(componentAlwaysOn, hideAlwaysOnOption);
                } //END OF if (enableAlwaysOnOption && hideAlwaysOnOption)
                setTileEnabled(componentAlwaysOn, enableAlwaysOnOption, isAdmin, pm);
            }
            catch (Exception e) {
                // ignore this part.
            }
        } //END OF if (AlwaysOnUtils.checkApkExist(this))
        //
        // Added by Mingszu Liang, 2017.01.12. - END

        //remove Memory for Verizon
        setTileEnabled(new ComponentName(packageName,
                        Settings.MemorySettingsActivity.class.getName()),
                !Utils.isVerizon(), isAdmin, pm);

        // remove Download for Verizon
        setTileEnabled(new ComponentName(packageName,
                        Settings.DownloadsSettings.class.getName()),
                !Utils.isVerizon(), isAdmin, pm);

        // Kids Mode START
        setTileEnabled(new ComponentName(packageName,
                        Settings.AsusKidsLauncherSettingsActivity.class.getName()),
                        AsusKidsLauncherSettings.hasKidsMode(this), isAdmin, pm);
        // Kids Mode END

        // +++ Easy Mode
        setTileEnabled(new ComponentName(packageName,
                Settings.AsusEasyLauncherSettingsActivity.class.getName()),
                AsusEasyLauncherSettings.hasEasyMode(this), isAdmin, pm);
        // ---

        List<DashboardCategory> categories = getDashboardCategories();
        if (UserHandle.MU_ENABLED && !isAdmin) {
            // When on restricted users, disable all extra categories (but only the settings ones).
            for (DashboardCategory category : categories) {
                for (Tile tile : category.tiles) {
                    ComponentName component = tile.intent.getComponent();
                    if (packageName.equals(component.getPackageName()) && !ArrayUtils.contains(
                            SETTINGS_FOR_RESTRICTED, component.getClassName())) {
                        setTileEnabled(component, false, isAdmin, pm);
                    }
                }
            }
        }

        String backupIntent = getResources().getString(R.string.config_backup_settings_intent);
        boolean useDefaultBackup = TextUtils.isEmpty(backupIntent);
        setTileEnabled(new ComponentName(packageName,
                Settings.PrivacySettingsActivity.class.getName()), useDefaultBackup, isAdmin, pm);
        boolean hasBackupActivity = false;
        if (!useDefaultBackup) {
            try {
                Intent intent = Intent.parseUri(backupIntent, 0);
                hasBackupActivity = !getPackageManager().queryIntentActivities(intent, 0).isEmpty();
            } catch (URISyntaxException e) {
                Log.e(LOG_TAG, "Invalid backup intent URI!", e);
            }
        }
        setTileEnabled(new ComponentName(packageName,
                BackupSettingsActivity.class.getName()), hasBackupActivity, isAdmin, pm);

   }

    //+++ tim_hu@asus.com disable action receiver if nfc not available
    private boolean updateNFCTilesList(String packageName, boolean isAdmin, PackageManager pm){
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        boolean nfcEnable = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
                && pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
                && adapter != null && adapter.isEnabled();

        setTileEnabled(new ComponentName(packageName,
                        PaymentDefaultDialog.class.getName()),
                nfcEnable, isAdmin, pm);
        setTileEnabled(new ComponentName(packageName,
                        Settings.AndroidBeamSettingsActivity.class.getName()),
                nfcEnable, isAdmin, pm);
        setTileEnabled(new ComponentName(packageName,
                        Settings.PaymentSettingsActivity.class.getName()),
                nfcEnable, isAdmin, pm);

        return nfcEnable;
    }

    //disable action receiver before finish
    private void doUpdateNFCTilesList(){
        PackageManager pm = getPackageManager();
        final UserManager um = UserManager.get(this);
        final boolean isAdmin = um.isAdminUser();

        String packageName = getPackageName();
        boolean nfcEnable = updateNFCTilesList(packageName, isAdmin, pm);

        if (!nfcEnable && ((this instanceof Settings.AndroidBeamSettingsActivity) ||
                (this instanceof Settings.PaymentSettingsActivity)))
            finish();
    }
    //---

    public SwitchBarWithFixedTitle getSwitchBarWithFixedTitle() {
        return mSwitchBarFixedTitle;
    }

    private void setTileEnabled(ComponentName component, boolean enabled, boolean isAdmin,
                                PackageManager pm) {
        if (UserHandle.MU_ENABLED && !isAdmin && getPackageName().equals(component.getPackageName())
                && !ArrayUtils.contains(SETTINGS_FOR_RESTRICTED, component.getClassName())) {
            enabled = false;
        }
        setTileEnabled(component, enabled);
    }

    private void getMetaData() {
        try {
            ActivityInfo ai = getPackageManager().getActivityInfo(getComponentName(),
                    PackageManager.GET_META_DATA);
            if (ai == null || ai.metaData == null) return;
            mFragmentClass = ai.metaData.getString(META_DATA_KEY_FRAGMENT_CLASS);
        } catch (NameNotFoundException nnfe) {
            // No recovery
            Log.d(LOG_TAG, "Cannot get Metadata for: " + getComponentName().toString());
        }
    }

    // give subclasses access to the Next button
    public boolean hasNextButton() {
        return mNextButton != null;
    }

    public Button getNextButton() {
        return mNextButton;
    }

    @Override
    public boolean shouldUpRecreateTask(Intent targetIntent) {
        return super.shouldUpRecreateTask(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        switchToSearchResultsFragmentIfNeeded();
        mSearchQuery = query;
        return mSearchResultsFragment.onQueryTextSubmit(query);
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchQuery = newText;
        if (mSearchResultsFragment == null) {
            return false;
        }
        return mSearchResultsFragment.onQueryTextChange(newText);
    }

    @Override
    public boolean onClose() {
        if (mSearchMenuItemExpanded) {

            mSearchMenuItemExpanded = false;
            clearSearchQuery();
            collapseActionBar();
            revertToInitialFragment();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (mSearchResultsFragment != null){
                if (mSearchView != null)
                    mSearchView.clearSearchText();
                onClose();
            } else{
                return super.onKeyDown(keyCode, event);
            }
            return false;
        }else {
            return super.onKeyDown(keyCode, event);
        }

    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mDisplaySearch) {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_SEARCH) {
                switchToSearchResultsFragmentIfNeeded();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == mSearchMenuItem.getItemId()) {
            switchToSearchResultsFragmentIfNeeded();
        }
        return true;
    }

    private void expandActionBar(){
        ActionBar actionBar = getActionBar();
        ActionBar.LayoutParams lp = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT,
                ActionBar.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        if (null != actionBar) {
            actionBar.setCustomView(mSearchView, lp);
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        Log.d(LOG_TAG,"expandActionBar remove action bar");
        findViewById(R.id.toolbar_title).setVisibility(View.GONE);
        mSearchMenuItem.setVisible(false);
    }

    private void collapseActionBar(){
        ActionBar actionBar = getActionBar();
        if (null != actionBar)
        	actionBar.setDisplayShowCustomEnabled(false);
        Log.d(LOG_TAG,"collapseActionBar show action bar");
        findViewById(R.id.toolbar_title).setVisibility(View.VISIBLE);
        mSearchMenuItem.setVisible(true);
    }
    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (item.getItemId() == mSearchMenuItem.getItemId()) {
            if (mSearchMenuItemExpanded) {
                revertToInitialFragment();
            }
        }
        return true;
    }

    @Override
    protected void onTileClicked(Tile tile) {
        if (mIsShowingDashboard && !isEasyLauncherMore()) {
            // If on dashboard, don't finish so the back comes back to here.
            openTile(tile);
        } else {
            super.onTileClicked(tile);
        }
    }

    @Override
    public void onProfileTileOpen() {
        if (!mIsShowingDashboard) {
            finish();
        }
    }
    private void switchToSearchResultsFragmentIfNeeded(boolean clear) {
        if (mSearchResultsFragment != null) {
            return;
        }
        if (mSearchView != null && clear)
            mSearchView.clearSearchText();

        Fragment current = getFragmentManager().findFragmentById(R.id.main_content);
        if (current != null && current instanceof SearchResultsSummary) {
            mSearchResultsFragment = (SearchResultsSummary) current;
        } else {
            //setContentHeaderView(null); blenda
            mSearchResultsFragment = (SearchResultsSummary) switchToFragment(
                    SearchResultsSummary.class.getName(), null, false, true,
                    R.string.search_results_title, null, true);
        }
        mSearchResultsFragment.setSearchView(mSearchView);
        mSearchMenuItemExpanded = true;
    }
    private void switchToSearchResultsFragmentIfNeeded() {
        if (mSearchResultsFragment != null) {
            return;
        }
        if (mSearchView != null)
            mSearchView.clearSearchText();

        Fragment current = getFragmentManager().findFragmentById(R.id.main_content);
        if (current != null && current instanceof SearchResultsSummary) {
            mSearchResultsFragment = (SearchResultsSummary) current;
        } else {
            //setContentHeaderView(null); blenda
            mSearchResultsFragment = (SearchResultsSummary) switchToFragment(
                    SearchResultsSummary.class.getName(), null, false, true,
                    R.string.search_results_title, null, true);
        }
        mSearchResultsFragment.setSearchView(mSearchView);
        mSearchMenuItemExpanded = true;
    }

    public void needToRevertToInitialFragment() {
        mNeedToRevertToInitialFragment = true;
    }

    public void clearSearchQuery(){
        mSearchQuery = null;

    }
    private void revertToInitialFragment() {

        mNeedToRevertToInitialFragment = false;
        mSearchResultsFragment = null;
        mSearchMenuItemExpanded = false;
        getFragmentManager().popBackStackImmediate(SettingsActivity.BACK_STACK_PREFS,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (mSearchMenuItem != null) {
//            mSearchMenuItem.collapseActionView();
        }
    }

    public Intent getResultIntentData() {
        return mResultIntentData;
    }

    public void setResultIntentData(Intent resultIntentData) {
        mResultIntentData = resultIntentData;
    }

    public void startSuggestion(Intent intent) {
        if (intent == null || ActivityManager.isUserAMonkey()) {
            return;
        }
        mCurrentSuggestion = intent.getComponent();
        try {
            startActivityForResult(intent, REQUEST_SUGGESTION);
        }catch (Exception e){
            Log.e(LOG_TAG, "startActivityForResult: " + e);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SUGGESTION && mCurrentSuggestion != null
                && resultCode != RESULT_CANCELED) {
            getPackageManager().setComponentEnabledSetting(mCurrentSuggestion,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //++chrisit_chang App link
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        activateAppLink(intent);
    }
    //--chrisit_chang App link

    //++chrisit_chang App link
    private void activateAppLink(Intent intent) {
        //get required data and action to make sure the intent is a app link intent
        String action = intent.getAction();
        Uri deepLink = intent.getData();

        //if it's a app link, get LastPathSegment to identify which activity is wanted
        if (intent.ACTION_VIEW.equals(action) && deepLink != null) {
            String lastPathSegment = deepLink.getLastPathSegment();
            if (lastPathSegment == null) return;

            //start activity from the correspond intent
            startActivity(SettingsAppLink.getIntent(lastPathSegment));
            finish();
        }
    }
    //--chrisit_chang App link

    private boolean isEasyLauncherMore(){
        return this.getClass().equals(Settings.AsusEasyLauncherMoreSettingsActivity.class);
    }

    private void updateEasyLauncherChanged(){
        final String[] strEntryValues = getResources().getStringArray(R.array.entryvalues_font_size);
        boolean state = android.provider.Settings.System.getInt(getContentResolver(),
                android.provider.Settings.System.ASUS_EASY_LAUNCHER, 0) == 0 ? false : true;
        if (state){
            saveCurrentFontScale(NORMAL_MODE_FONT_SIZE);
            setSystemFontScale(EASY_MODE_FONT_SIZE,
                    Float.parseFloat(strEntryValues[LARGE_FONT_SIZE_INDEX]));
            return;
        }
        saveCurrentFontScale(EASY_MODE_FONT_SIZE);
        setSystemFontScale(NORMAL_MODE_FONT_SIZE,
                Float.parseFloat(strEntryValues[NORMAL_FONT_SIZE_INDEX]));
        clearHideList();
    }

    private void saveCurrentFontScale(String mode) {
        final float currentScale =
                android.provider.Settings.System.getFloat(getContentResolver(),
                android.provider.Settings.System.FONT_SCALE, 1.0f);
        mDisplayPreferences.edit().putFloat(mode, currentScale).commit();
    }

    private void setSystemFontScale(String mode, float defaultValue){
        android.provider.Settings.System.putFloat(getContentResolver(),
                android.provider.Settings.System.FONT_SCALE,
                mDisplayPreferences.getFloat(mode, defaultValue));
    }
}
