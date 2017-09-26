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

package com.android.settings.search;

import android.provider.SearchIndexableResource;

import com.android.settings.AsusCNAccountSettings;
import com.android.settings.AsusEasyLauncherSettings;
import com.android.settings.AsusKidsLauncherSettings;
import com.android.settings.AsusLockScreenSettings;
import com.android.settings.AsusThemeAppSettings;
import com.android.settings.AsusTwinAppsSettings;
import com.android.settings.DateTimeSettings;
import com.android.settings.DevelopmentSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.LegalSettings;
import com.android.settings.ManagePermissionSettings;
import com.android.settings.MoreSettings;
import com.android.settings.PrivacySettings;
import com.android.settings.R;
import com.android.settings.ScreenPinningSettings;
import com.android.settings.SecuritySettings;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.SystemUpdateSettings;
import com.android.settings.TetherSettings;
import com.android.settings.WallpaperTypeSettings;
import com.android.settings.WirelessSettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accounts.AccountSettings;
import com.android.settings.applications.AdvancedAppSettings;
import com.android.settings.applications.CNManageAppsEntry;
import com.android.settings.applications.SpecialAccessSettings;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.datausage.DataUsageMeteredSettings;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deviceinfo.PrivateVolumeSettings;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.ScreenZoomSettings;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.ScanningSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.OtherSoundSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.notification.CNStatusBarNotificationEntry;
import com.android.settings.notification.CNNotificationManagerEntry;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.sim.SimSettings;
import com.android.settings.users.UserSettings;
import com.android.settings.wifi.AdvancedWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;
import com.android.settings.AsusCoverSettings;
import com.android.settings.zenmotion.AsusMotionSettings;
import com.android.settings.zenmotion.AsusTouchSettings;
import com.android.settings.zenmotion.AsusZenMotionSettings;

import java.util.Collection;
import java.util.HashMap;

public final class SearchIndexableResources {

    public static int NO_DATA_RES_ID = 0;

    private static HashMap<String, SearchIndexableResource> sResMap =
            new HashMap<String, SearchIndexableResource>();

    static {
        sResMap.put(WifiSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(WifiSettings.class.getName()),
                        NO_DATA_RES_ID,
                        WifiSettings.class.getName(),
                        R.drawable.ic_settings_wireless));

/*        sResMap.put(AdvancedWifiSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AdvancedWifiSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AdvancedWifiSettings.class.getName(),
                        R.drawable.ic_settings_wireless));*/

        sResMap.put(SavedAccessPointsWifiSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SavedAccessPointsWifiSettings.class.getName()),
                        R.xml.wifi_display_saved_access_points,
                        SavedAccessPointsWifiSettings.class.getName(),
                        R.drawable.ic_settings_wireless));

        sResMap.put(TetherSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(TetherSettings.class.getName()),
                        NO_DATA_RES_ID,
                        TetherSettings.class.getName(),
                        R.drawable.ic_settings_wireless));

        sResMap.put(BluetoothSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(BluetoothSettings.class.getName()),
                        NO_DATA_RES_ID,
                        BluetoothSettings.class.getName(),
                        R.drawable.ic_settings_bluetooth));

        sResMap.put(SimSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SimSettings.class.getName()),
                        NO_DATA_RES_ID,
                        SimSettings.class.getName(),
                        R.drawable.ic_sim_sd));

        sResMap.put(DataUsageSummary.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DataUsageSummary.class.getName()),
                        NO_DATA_RES_ID,
                        DataUsageSummary.class.getName(),
                        R.drawable.ic_settings_data_usage));

        sResMap.put(DataUsageMeteredSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DataUsageMeteredSettings.class.getName()),
                        NO_DATA_RES_ID,
                        DataUsageMeteredSettings.class.getName(),
                        R.drawable.ic_settings_data_usage));

        sResMap.put(WirelessSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(WirelessSettings.class.getName()),
                        NO_DATA_RES_ID,
                        WirelessSettings.class.getName(),
                        R.drawable.ic_settings_more));

        sResMap.put(MoreSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(MoreSettings.class.getName()),
                        NO_DATA_RES_ID,
                        MoreSettings.class.getName(),
                        R.drawable.ic_settings_more));

        sResMap.put(ScreenZoomSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ScreenZoomSettings.class.getName()),
                        NO_DATA_RES_ID,
                        ScreenZoomSettings.class.getName(),
                        R.drawable.ic_settings_display));

        sResMap.put(DisplaySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DisplaySettings.class.getName()),
                        NO_DATA_RES_ID,
                        DisplaySettings.class.getName(),
                        R.drawable.ic_settings_display));

/*        sResMap.put(WallpaperTypeSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(WallpaperTypeSettings.class.getName()),
                        NO_DATA_RES_ID,
                        WallpaperTypeSettings.class.getName(),
                        R.drawable.ic_settings_display));*/
        sResMap.put(AsusThemeAppSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusThemeAppSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusThemeAppSettings.class.getName(),
                        R.drawable.ic_settings_display));

        sResMap.put(ConfigureNotificationSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ConfigureNotificationSettings.class.getName()),
                        R.xml.configure_notification_settings,
                        ConfigureNotificationSettings.class.getName(),
                        R.drawable.ic_settings_notifications));

        sResMap.put(CNStatusBarNotificationEntry.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(CNStatusBarNotificationEntry.class.getName()),
                        R.xml.cn_statusbar_notification_entry,
                        CNStatusBarNotificationEntry.class.getName(),
                        R.drawable.ic_settings_notifications));

        sResMap.put(CNNotificationManagerEntry.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(CNNotificationManagerEntry.class.getName()),
                        R.layout.cn_manage_notifications,
                        CNNotificationManagerEntry.class.getName(),
                        R.drawable.ic_settings_notifications));

        sResMap.put(SoundSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SoundSettings.class.getName()),
                        NO_DATA_RES_ID,
                        SoundSettings.class.getName(),
                        R.drawable.ic_settings_sound));

/*        sResMap.put(OtherSoundSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(OtherSoundSettings.class.getName()),
                        NO_DATA_RES_ID,
                        OtherSoundSettings.class.getName(),
                        R.drawable.ic_settings_sound));*/

        sResMap.put(ZenModeSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ZenModeSettings.class.getName()),
                        R.xml.zen_mode_settings,
                        ZenModeSettings.class.getName(),
                        R.drawable.ic_settings_notifications));

/*        sResMap.put(ZenModePrioritySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ZenModePrioritySettings.class.getName()),
                        R.xml.zen_mode_priority_settings,
                        ZenModePrioritySettings.class.getName(),
                        R.drawable.ic_settings_notifications));*/

        sResMap.put(StorageSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(StorageSettings.class.getName()),
                        NO_DATA_RES_ID,
                        StorageSettings.class.getName(),
                        R.drawable.ic_settings_storage));

        sResMap.put(PowerUsageSummary.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(PowerUsageSummary.class.getName()),
                        R.xml.power_usage_summary,
                        PowerUsageSummary.class.getName(),
                        R.drawable.ic_settings_battery));

        /*sResMap.put(BatterySaverSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(BatterySaverSettings.class.getName()),
                        R.xml.battery_saver_settings,
                        BatterySaverSettings.class.getName(),
                        R.drawable.ic_settings_battery)); */

        sResMap.put(CNManageAppsEntry.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(CNManageAppsEntry.class.getName()),
                        NO_DATA_RES_ID,
                        CNManageAppsEntry.class.getName(),
                        R.drawable.ic_settings_applications));
        
        sResMap.put(AdvancedAppSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AdvancedAppSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AdvancedAppSettings.class.getName(),
                        R.drawable.ic_settings_applications));

        sResMap.put(SpecialAccessSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SpecialAccessSettings.class.getName()),
                        R.xml.special_access,
                        SpecialAccessSettings.class.getName(),
                        R.drawable.ic_settings_applications));
        sResMap.put(AsusTwinAppsSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusTwinAppsSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusTwinAppsSettings.class.getName(),
                        R.drawable.ic_settings_applications));
        sResMap.put(ManagePermissionSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ManagePermissionSettings.class.getName()),
                        NO_DATA_RES_ID,
                        ManagePermissionSettings.class.getName(),
                        R.drawable.ic_settings_applications));

        sResMap.put(UserSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(UserSettings.class.getName()),
                        NO_DATA_RES_ID,
                        UserSettings.class.getName(),
                        R.drawable.ic_settings_multiuser));

        sResMap.put(GestureSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(GestureSettings.class.getName()),
                        NO_DATA_RES_ID,
                        GestureSettings.class.getName(),
                        R.drawable.ic_settings_gestures));

        sResMap.put(LocationSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(LocationSettings.class.getName()),
                        R.xml.location_settings,
                        LocationSettings.class.getName(),
                        R.drawable.ic_settings_location));

/*        sResMap.put(ScanningSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ScanningSettings.class.getName()),
                        R.xml.location_scanning,
                        ScanningSettings.class.getName(),
                        R.drawable.ic_settings_location));*/

        sResMap.put(SecuritySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SecuritySettings.class.getName()),
                        NO_DATA_RES_ID,
                        SecuritySettings.class.getName(),
                        R.drawable.ic_settings_security));

        sResMap.put(AsusLockScreenSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusLockScreenSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusLockScreenSettings.class.getName(),
                        R.drawable.ic_settings_security));

        sResMap.put(ChooseLockGeneric.ChooseLockGenericFragment.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ChooseLockGeneric.ChooseLockGenericFragment.class.getName()),
                        NO_DATA_RES_ID,
                        ChooseLockGeneric.ChooseLockGenericFragment.class.getName(),
                        R.drawable.ic_settings_security));

        sResMap.put(ScreenPinningSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(ScreenPinningSettings.class.getName()),
                        NO_DATA_RES_ID,
                        ScreenPinningSettings.class.getName(),
                        R.drawable.ic_settings_security));

        sResMap.put(FingerprintSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(FingerprintSettings.class.getName()),
                        NO_DATA_RES_ID,
                        FingerprintSettings.class.getName(),
                        R.drawable.ic_settings_security));

        sResMap.put(AccountSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AccountSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AccountSettings.class.getName(),
                        R.drawable.ic_settings_accounts));

        sResMap.put(AsusCNAccountSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusCNAccountSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusCNAccountSettings.class.getName(),
                        R.drawable.ic_settings_accounts));

        sResMap.put(InputMethodAndLanguageSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(InputMethodAndLanguageSettings.class.getName()),
                        NO_DATA_RES_ID,
                        InputMethodAndLanguageSettings.class.getName(),
                        R.drawable.ic_settings_language));

        sResMap.put(PrivacySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(PrivacySettings.class.getName()),
                        NO_DATA_RES_ID,
                        PrivacySettings.class.getName(),
                        R.drawable.ic_settings_backup));

        sResMap.put(DateTimeSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DateTimeSettings.class.getName()),
                        NO_DATA_RES_ID,
                        DateTimeSettings.class.getName(),
                        R.drawable.ic_settings_date_time));

        sResMap.put(AccessibilitySettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AccessibilitySettings.class.getName()),
                        NO_DATA_RES_ID,
                        AccessibilitySettings.class.getName(),
                        R.drawable.ic_settings_accessibility));

/*        sResMap.put(PrintSettingsFragment.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(PrintSettingsFragment.class.getName()),
                        NO_DATA_RES_ID,
                        PrintSettingsFragment.class.getName(),
                        R.drawable.ic_settings_print));*/

        sResMap.put(DevelopmentSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DevelopmentSettings.class.getName()),
                        NO_DATA_RES_ID,
                        DevelopmentSettings.class.getName(),
                        R.drawable.ic_settings_development));

        sResMap.put(DeviceInfoSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(DeviceInfoSettings.class.getName()),
                        NO_DATA_RES_ID,
                        DeviceInfoSettings.class.getName(),
                        R.drawable.ic_settings_about));

        sResMap.put(LegalSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(LegalSettings.class.getName()),
                        NO_DATA_RES_ID,
                        LegalSettings.class.getName(),
                        R.drawable.ic_settings_about));
        sResMap.put(SystemUpdateSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(SystemUpdateSettings.class.getName()),
                        NO_DATA_RES_ID,
                        SystemUpdateSettings.class.getName(),
                        R.drawable.ic_settings_about));

/*        sResMap.put(ZenModeVisualInterruptionSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(
                                ZenModeVisualInterruptionSettings.class.getName()),
                        R.xml.zen_mode_visual_interruptions_settings,
                        ZenModeVisualInterruptionSettings.class.getName(),
                        R.drawable.ic_settings_notifications));*/

        //for search asus flip cover on settings ++
        sResMap.put(AsusCoverSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusCoverSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusCoverSettings.class.getName(),
                        R.drawable.ic_settings_cover));
        //--

        //Easy Mode
       /* sResMap.put(AsusEasyLauncherSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusEasyLauncherSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusEasyLauncherSettings.class.getName(),
                        R.drawable.asus_settings_ic_easy_mode));
        */
        //ZenMotion
        /*
        sResMap.put(AsusZenMotionSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusZenMotionSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusZenMotionSettings.class.getName(),
                        R.drawable.asus_settings_ic_zenmotion));
        sResMap.put(AsusTouchSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusTouchSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusTouchSettings.class.getName(),
                        R.drawable.asus_settings_ic_zenmotion));
        sResMap.put(AsusMotionSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusMotionSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusMotionSettings.class.getName(),
                        R.drawable.asus_settings_ic_zenmotion));
        */
        //Kids Mode
        sResMap.put(AsusKidsLauncherSettings.class.getName(),
                new SearchIndexableResource(
                        Ranking.getRankForClassName(AsusKidsLauncherSettings.class.getName()),
                        NO_DATA_RES_ID,
                        AsusKidsLauncherSettings.class.getName(),
                        R.drawable.asus_settings_ic_kids_mode));
    }

    private SearchIndexableResources() {
    }

    public static int size() {
        return sResMap.size();
    }

    public static SearchIndexableResource getResourceByName(String className) {
        return sResMap.get(className);
    }

    public static Collection<SearchIndexableResource> values() {
        return sResMap.values();
    }
}
