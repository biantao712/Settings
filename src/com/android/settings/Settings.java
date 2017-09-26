/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.SharedPreferences;
import android.os.Bundle;
import com.android.settings.applications.AppOpsSummary;
import com.android.settings.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.fingerprint.FingerprintSettings;

import com.asus.suw.lockscreen.FingerprintEntryPoint;

/**
 * Top-level Settings activity
 */
public class Settings extends SettingsActivity {

    /*
    * Settings subclasses for launching independently.
    */
    public static class BluetoothSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WirelessSettingsActivity extends SettingsActivity { /* empty */ }
    // +++ ckenken (ChiaHsiang_Kuo) @ 20160908 Add CallSettings and DualSIM Setting
    public static class CallSettingsActivity extends SettingsActivity { /* empty */ }
    public static class MultiSimSettingsActivity extends SettingsActivity { /* empty */ }
    // --- ckenken (ChiaHsiang_Kuo) @ 20160908 Add CallSettings and DualSIM Setting
    //  +++  MiilieChang, Verizon spec : Advanced calling
    public static class AdvancedCallingActivity extends SettingsActivity { /* empty */ }
    //  ---  MiilieChang, Verizon spec : Advanced calling
    public static class SimSettingsActivity extends SettingsActivity { /* empty */ }
    public static class EthernetSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TetherSettingsActivity extends SettingsActivity { /* empty */ }
    public static class VpnSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DateTimeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class StorageSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrivateVolumeForgetActivity extends SettingsActivity { /* empty */ }
    public static class PrivateVolumeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PublicVolumeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiP2pSettingsActivity extends SettingsActivity { /* empty */ }
    public static class InputMethodAndLanguageSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AvailableVirtualKeyboardActivity extends SettingsActivity { /* empty */ }
    public static class KeyboardLayoutPickerActivity extends SettingsActivity { /* empty */ }
    public static class PhysicalKeyboardActivity extends SettingsActivity { /* empty */ }
    public static class InputMethodAndSubtypeEnablerActivity extends SettingsActivity { /* empty */ }
    public static class SpellCheckersSettingsActivity extends SettingsActivity { /* empty */ }
    public static class LocalePickerActivity extends SettingsActivity { /* empty */ }
    public static class UserDictionarySettingsActivity extends SettingsActivity { /* empty */ }
    public static class HomeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DisplaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class NightDisplaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class DeviceInfoSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ApplicationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ManageApplicationsActivity extends SettingsActivity { /* empty */ }
    public static class ManageApplicationsCtsActivity extends SettingsActivity { /* empty */ }
    public static class ManageAssistActivity extends SettingsActivity { /* empty */ }
    public static class AllApplicationsActivity extends SettingsActivity { /* empty */ }
    public static class HighPowerApplicationsActivity extends SettingsActivity { /* empty */ }
    public static class ManagePermissionSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppOpsSummaryActivity extends SettingsActivity {
        @Override
        public boolean isValidFragment(String className) {
            if (AppOpsSummary.class.getName().equals(className)) {
                return true;
            }
            return super.isValidFragment(className);
            }
    }
    public static class BackgroundCheckSummaryActivity extends SettingsActivity { /* empty */ }
    public static class StorageUseActivity extends SettingsActivity { /* empty */ }
    public static class DevelopmentSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilitySettingsActivity extends SettingsActivity { /* empty */ }
    public static class CaptioningSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityInversionSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityContrastSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccessibilityDaltonizerSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SecuritySettingsActivity extends SettingsActivity { /* empty */ }
    public static class UsageAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class LocationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrivacySettingsActivity extends SettingsActivity { /* empty */ }
    public static class FactoryResetActivity extends SettingsActivity { /* empty */ }
    public static class RunningServicesActivity extends SettingsActivity { /* empty */ }
    public static class ManageAccountsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PowerUsageSummaryActivity extends SettingsActivity { /* empty */ }
    public static class BatterySaverSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccountSyncSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccountSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AsusCNAccountSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AccountSyncSettingsInAddAccountActivity extends SettingsActivity { /* empty */ }
    public static class GestureSettingsActivity extends SettingsActivity { /* empty */ }
    public static class CryptKeeperSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DeviceAdminSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DataUsageSummaryActivity extends SettingsActivity { /* empty */ }
    public static class AdvancedWifiSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SavedAccessPointsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TextToSpeechSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AndroidBeamSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiDisplaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class DreamSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationStationActivity extends SettingsActivity { /* empty */ }
    public static class UserSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ScreenPinningSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class VrListenersSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenAccessSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ConditionProviderSettingsActivity extends SettingsActivity { /* empty */ }
    public static class UsbSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TrustedCredentialsSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PaymentSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrintSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PrintJobSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModePrioritySettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeAutomationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeScheduleRuleSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeEventRuleSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeExternalRuleSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeVisualInterruptionSettingsActivity extends SettingsActivity { /* empty */}
    public static class SoundSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ConfigureNotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class NotificationAppListActivity extends SettingsActivity { /* empty */ }
    public static class AppNotificationSettingsActivity extends SettingsActivity { /* empty */ }
    public static class OtherSoundSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ManageDomainUrlsActivity extends SettingsActivity { /* empty */ }
    public static class AutomaticStorageManagerSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DomainsURLsAppListActivity extends SettingsActivity { /* empty */ }
    public static class CustomizeSettingsActivity extends SettingsActivity { /* empty */ }
    public static class PowerSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ZenUIUpdateSettingsActivity extends SettingsActivity { /* empty */ }
    public static class SystemUpdateSettingsActivity extends SettingsActivity { /* empty */ }
    public static class VibrationIntensitySettingsActivity extends SettingsActivity { /* empty */ } //Sharon+++new feature about vibration intensity
    public static class AsusThemeAppSettings extends SettingsActivity { /* empty */ }
    public static class AsusFingerprintSettings extends SettingsActivity { /* empty */ }
    public static class AsusEasyLauncherSettingsActivity extends SettingsActivity { /* empty */ }
    public static class TopLevelSettings extends SettingsActivity { /* empty */ }
    public static class ApnSettingsActivity extends SettingsActivity {
        @Override
        protected void onCreate(Bundle savedState) {
            setTheme(R.style.Theme_SubSettings);
            super.onCreate(savedState);
        }
    }
    public static class WifiCallingSettingsActivity extends SettingsActivity { /* empty */ }
    public static class MemorySettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppMemoryUsageActivity extends SettingsActivity { /* empty */ }
    public static class OverlaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class WriteSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppDrawOverlaySettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppWriteSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AdvancedAppsActivity extends SettingsActivity { /* empty */ }

    public static class WifiCallingSuggestionActivity extends SettingsActivity { /* empty */ }
    public static class ZenModeAutomationSuggestionActivity extends SettingsActivity { /* empty */ }
    public static class FingerprintSuggestionActivity extends FingerprintSettings { /* empty */ }
    public static class FingerprintEnrollSuggestionActivity extends FingerprintEntryPoint {
        /* empty */
    }
    public static class ScreenLockSuggestionActivity extends ChooseLockGeneric { /* empty */ }
    public static class WallpaperSettingsActivity extends SettingsActivity { /* empty */ }
    public static class ManagedProfileSettingsActivity extends SettingsActivity { /* empty */ }
    public static class DeletionHelperActivity extends SettingsActivity { /* empty */ }

    public static class ApnEditorActivity extends SettingsActivity { /* empty */ }
    public static class ChooseAccountActivity extends SettingsActivity { /* empty */ }
    public static class IccLockSettingsActivity extends SettingsActivity {
        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            return getBaseContext().getSharedPreferences(name, mode);
        }
    }
    public static class ImeiInformationActivity extends SettingsActivity { /* empty */ }
    public static class SimStatusActivity extends SettingsActivity { /* empty */ }
    public static class StatusActivity extends SettingsActivity { /* empty */ }
    public static class TestingSettingsActivity extends SettingsActivity { /* empty */ }
    public static class WifiAPITestActivity extends SettingsActivity { /* empty */ }
    public static class WifiInfoActivity extends SettingsActivity { /* empty */ }
    //ablenda, add MoreSettingsActivity
    public static class MoreSettingsActivity extends SettingsActivity { /* empty */ }

    // Categories.jack_qi modified for CNSettings
    public static class WirelessSettings extends SettingsActivity { /* empty */ }
    public static class DeviceSystemSettings extends SettingsActivity { /* empty */ }
    public static class PersonalSettings extends SettingsActivity { /* empty */ }
    public static class PhoneSettings extends SettingsActivity { /* empty */ }
    public static class AccountSettings extends SettingsActivity { /* empty */ }
    public static class HideSettings extends SettingsActivity { /* empty */ }

    public static class AsusLockScreenSettingsActivity extends SettingsActivity { /* empty */ }

    //For Flip cover entry+++
    public static class AsusCoverSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AsusCover3SettingsActivity extends SettingsActivity { /* empty */ }
    //For Flip cover entry---

    public static class AsusKidsLauncherSettingsActivity extends SettingsActivity { /* empty */ }

    // ZenMotion
    public static class AsusZenMotionSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AsusVZWZenMotionSettingsActivity extends SettingsActivity { /* empty */ }
    //zenmotion2++
    public static class AsusZenMotion2SettingsActivity extends SettingsActivity { /* empty */ }
    public static class AppList extends SettingsActivity { /* empty */ }
    public static class GestureToturial extends SettingsActivity { /* empty */ }
    //zenmotion2--
    public static class DownloadsSettings extends SettingsActivity { /* empty */ }

    public static class ScreenshotSettingsActivity extends SettingsActivity { /* empty */ }
    public static class AsusEasyLauncherMoreSettingsActivity extends SettingsActivity { /* empty */ }

    public static class AsusTwinAppsSettings extends SettingsActivity { /* empty */ } //[TwinApps]
    //Jeson_Li: add for floatingdock
    public static class AccessibilityServicesListActivity extends SettingsActivity { /* empty */ }

    // Mark_Xiao notification Manger
    public static class NotificationManagerEntry extends SettingsActivity{/*empty*/}
    public static class NotificationManagerAppEntry extends SettingsActivity{/*empty*/}
}
