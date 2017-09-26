/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.internal.content.PackageMonitor;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.view.RotationPolicy;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import com.android.settings.CustomizeSettings;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

//Display Size
import com.android.settings.display.ScreenZoomPreference;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity with the accessibility settings.
 */
public class AccessibilitySettings extends SettingsPreferenceFragment implements DialogCreatable,
        Preference.OnPreferenceChangeListener, Indexable {

    // Preference categories
    private static final String SERVICES_CATEGORY = "services_category";
    private static final String SYSTEM_CATEGORY = "system_category";
    private static final String PACKET_GLOVE_CATEGORY = "packet_glove_category";
    private static final String VIRTUAL_RECENT_KEY_CATEGORY = "virtual_recent_key_category";
    private static final String DISPLAY_CATEGORY = "display_category";

    // Preferences
    private static final String TOGGLE_HIGH_TEXT_CONTRAST_PREFERENCE =
            "toggle_high_text_contrast_preference";
    private static final String TOGGLE_INVERSION_PREFERENCE =
            "toggle_inversion_preference";
    private static final String TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE =
            "toggle_power_button_ends_call_preference";
    private static final String TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE =
            "toggle_lock_screen_rotation_preference";
    private static final String TOGGLE_SPEAK_PASSWORD_PREFERENCE =
            "toggle_speak_password_preference";
    private static final String TOGGLE_LARGE_POINTER_ICON =
            "toggle_large_pointer_icon";
    private static final String TOGGLE_MASTER_MONO =
            "toggle_master_mono";
    private static final String SELECT_LONG_PRESS_TIMEOUT_PREFERENCE =
            "select_long_press_timeout_preference";
    private static final String VIRTUAL_KEY_PREFERENCE_SCREEN =
            "virtualkey_preference_screen";//++ jack_qi
    private static final String SELECT_RECENT_KEY_FUNCTION_PREFERENCE =
            "select_recent_key_function_preference";//++ jack_qi
    private static final String ENABLE_ACCESSIBILITY_GESTURE_PREFERENCE_SCREEN =
            "enable_global_gesture_preference_screen";
    private static final String CAPTIONING_PREFERENCE_SCREEN =
            "captioning_preference_screen";
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN =
            "screen_magnification_preference_screen";
    private static final String FONT_SIZE_PREFERENCE_SCREEN =
            "font_size_preference_screen";
    private static final String AUTOCLICK_PREFERENCE_SCREEN =
            "autoclick_preference_screen";
    private static final String DISPLAY_DALTONIZER_PREFERENCE_SCREEN =
            "daltonizer_preference_screen";


    private static final String PACKET_MODE_PREFERENCE =
            "packet_mode_preference";
    private static final String GLOVE_MODE_PREFERENCE =
            "glove_mode_preference";

    private static final String KEY_INSTANT_CAMERA_SWITCH = "lockscreen_instant_camera_widget";

    // Extras passed to sub-fragments.
    static final String EXTRA_PREFERENCE_KEY = "preference_key";
    static final String EXTRA_CHECKED = "checked";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_SUMMARY = "summary";
    static final String EXTRA_SETTINGS_TITLE = "settings_title";
    static final String EXTRA_COMPONENT_NAME = "component_name";
    static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";

    // Display size
    private static final String KEY_DISPLAY_SIZE = "screen_zoom";
    private static final String KEY_SYSTEM_CATEGORY = "system_category";
    private static final String FEATURE_SINGLE_DENSITY = "asus.software.singledensity";

    // Timeout before we update the services if packages are added/removed
    // since the AccessibilityManagerService has to do that processing first
    // to generate the AccessibilityServiceInfo we need for proper
    // presentation.
    private static final long DELAY_UPDATE_SERVICES_MILLIS = 1000;

    //jack_qi
    static final String SELECT_RECENT_KEY_FUNCTION_KEY = "long_pressed_func";
    static final String PACKET_MODE_KEY = "pocket_mode";

    // Auxiliary members.
    static final Set<ComponentName> sInstalledServices = new HashSet<>();

    private final Map<String, String> mLongPressTimeoutValuetoTitleMap = new HashMap<>();

    //jack_qi
    private final Map<String, String> mRecentKeyFunctionValuetoTitleMap = new HashMap<>();

    private final Handler mHandler = new Handler();

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null) {
                updateServicesPreferences();
            }
        }
    };

    private final PackageMonitor mSettingsPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            mHandler.postDelayed(mUpdateRunnable, DELAY_UPDATE_SERVICES_MILLIS);
        }
    };

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(mHandler) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    updateServicesPreferences();
                }
            };

    private final RotationPolicyListener mRotationPolicyListener = new RotationPolicyListener() {
        @Override
        public void onChange() {
            updateLockScreenRotationCheckbox();
        }
    };

    // Preference controls.
    private PreferenceCategory mServicesCategory;
    private PreferenceCategory mSystemsCategory;
    private PreferenceCategory mPacketGloveCategory;
    private PreferenceCategory mVirtualRecentCategory;
    private PreferenceCategory mDisplayCategory;

    private SwitchPreference mToggleHighTextContrastPreference;
    private SwitchPreference mTogglePowerButtonEndsCallPreference;
    private SwitchPreference mToggleLockScreenRotationPreference;
    private SwitchPreference mToggleSpeakPasswordPreference;
    private SwitchPreference mToggleLargePointerIconPreference;
    private SwitchPreference mToggleMasterMonoPreference;
    private ListPreference mSelectLongPressTimeoutPreference;
    private Preference mNoServicesMessagePreference;
    private PreferenceScreen mCaptioningPreferenceScreen;
    private PreferenceScreen mDisplayMagnificationPreferenceScreen;
    private PreferenceScreen mFontSizePreferenceScreen;
    private PreferenceScreen mAutoclickPreferenceScreen;
    private PreferenceScreen mGlobalGesturePreferenceScreen;
    private PreferenceScreen mDisplayDaltonizerPreferenceScreen;
    private SwitchPreference mToggleInversionPreference;
    private PreferenceScreen mVirtualKeyPreference;//jack_qi
    private ListPreference mSelectRecentKeyFuntionPreference;//jack_qi
    private SwitchPreference mPacketModePreference;
    private SwitchPreference mGloveModePreference;
    private PreferenceScreen mToggleDaltonizerPreference;
    private SwitchPreference mInstantCameraSwitchPref;

    // Display size
    private ScreenZoomPreference mScreenZoomPreference;

    private int mLongPressTimeoutDefault;

    private int mRecentKeyValueDefault;

    private DevicePolicyManager mDpm;

    private class GloveModeContentObserver extends ContentObserver {
        GloveModeContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            boolean value = isGloveModeEnabled();
            Log.v(TAG, "Glove mode settings onChange. Value is " + value);
            setGloveModePreference(value);
        }
    }
    private ContentObserver mGloveModeObserver = null;
    private Handler mGloveModeHandler = new Handler();
    static final String TAG = "AccessibilitySettings";
    boolean mHasGloveModeHWFeature = false;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_accessibility;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings);
        initializeAllPreferences();
        mDpm = (DevicePolicyManager) (getActivity()
                .getSystemService(Context.DEVICE_POLICY_SERVICE));


        // Hide Display Size with Single_Density feature
        PreferenceGroup system_items = (PreferenceGroup)findPreference(KEY_SYSTEM_CATEGORY);
        mScreenZoomPreference = (ScreenZoomPreference)findPreference(KEY_DISPLAY_SIZE);
        if (isSingleDensityEnable(getActivity())){
            system_items.removePreference(mScreenZoomPreference);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllPreferences();

        if (mHasGloveModeHWFeature) {
            setGloveModePreference(isGloveModeEnabled());
            registerGloveModeContentObserver();
        }
        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        mSettingsContentObserver.register(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }

    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        mSettingsContentObserver.unregister(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }
        if (mHasGloveModeHWFeature) {
            unregisterGloveModeContentObserver();
        }
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mSelectLongPressTimeoutPreference == preference) {
            handleLongPressTimeoutPreferenceChange((String) newValue);
            return true;
        } else if (mToggleInversionPreference == preference) {
            handleToggleInversionPreferenceChange((Boolean) newValue);
            return true;
        }else if (mSelectRecentKeyFuntionPreference == preference) {
            handleSelectRecentKeyFunctionPreferenceChange((String) newValue);
            return true;
        }
        return false;
    }

    private void handleLongPressTimeoutPreferenceChange(String stringValue) {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, Integer.parseInt(stringValue));
        mSelectLongPressTimeoutPreference.setSummary(
                mLongPressTimeoutValuetoTitleMap.get(stringValue));
    }

    private void handleToggleInversionPreferenceChange(boolean checked) {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, (checked ? 1 : 0));
    }

    private void handleSelectRecentKeyFunctionPreferenceChange(String stringValue) {
        Settings.System.putInt(getContentResolver(),
                SELECT_RECENT_KEY_FUNCTION_KEY, Integer.parseInt(stringValue));
        mSelectRecentKeyFuntionPreference.setSummary(
                mRecentKeyFunctionValuetoTitleMap.get(stringValue));
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mToggleHighTextContrastPreference == preference) {
            handleToggleTextContrastPreferenceClick();
            return true;
        } else if (mTogglePowerButtonEndsCallPreference == preference) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (mToggleLockScreenRotationPreference == preference) {
            handleLockScreenRotationPreferenceClick();
            return true;
        } else if (mToggleSpeakPasswordPreference == preference) {
            handleToggleSpeakPasswordPreferenceClick();
            return true;
        } else if (mToggleLargePointerIconPreference == preference) {
            handleToggleLargePointerIconPreferenceClick();
            return true;
        } else if (mToggleMasterMonoPreference == preference) {
            handleToggleMasterMonoPreferenceClick();
            return true;
        } else if (mGlobalGesturePreferenceScreen == preference) {
            handleToggleEnableAccessibilityGesturePreferenceClick();
            return true;
        } else if (mDisplayMagnificationPreferenceScreen == preference) {
            handleDisplayMagnificationPreferenceScreenClick();
            return true;
        } else if (mDisplayDaltonizerPreferenceScreen == preference) {
            handleDisplayDaltonizerPreferenceScreenClick();
            return true;
        } else if (mPacketModePreference == preference) {
            handlePacketModePreferenceScreenClick();
            return true;
        } else if (mGloveModePreference == preference) {
            handleGloveModePreferenceScreenClick();
            return true;
        } else if (mInstantCameraSwitchPref == preference) {
            handleInstantCameraPreferenceScreenClick();
        }

            return super.onPreferenceTreeClick(preference);
    }

    private void updateInstantCameraPreference() {
        int status = 0;

        status = Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_LOCKSCREEN_INSTANT_CAMERA, 0);
        boolean issInstantCameraEnabled = (status == 0 ? false : true);
        mInstantCameraSwitchPref.setChecked(issInstantCameraEnabled);
    }


    private void handleToggleTextContrastPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED,
                (mToggleHighTextContrastPreference.isChecked() ? 1 : 0));
    }

    private void handlePacketModePreferenceScreenClick() {
        Settings.System.putInt(getContentResolver(),
                PACKET_MODE_KEY,
                (mPacketModePreference.isChecked() ? 1 : 0));
    }

    private void handleGloveModePreferenceScreenClick() {
        setGloveModeEnabled(mGloveModePreference.isChecked());
    }

    private void handleInstantCameraPreferenceScreenClick() {
        if (mInstantCameraSwitchPref != null) {
            int status = (mInstantCameraSwitchPref.isChecked() ? 1 : 0);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ASUS_LOCKSCREEN_INSTANT_CAMERA, status);
            //BEGIN: Steven_Chao@asus.com
            Log.d(TAG, "mIsInstantCameraEnabled=" + mInstantCameraSwitchPref.isChecked()  + ". Set persist.asus.instant_camera");
            SystemProperties.set("persist.asus.instant_camera", mInstantCameraSwitchPref.isChecked()  ? "1" : "0");
            //END: Steven_Chao@asus.com
        }
    }

    //jack_qi, add for GloveMode, copy from CustomizeSettings.
    private void registerGloveModeContentObserver() {
        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.GLOVE_MODE), true, mGloveModeObserver);
    }

    private void unregisterGloveModeContentObserver() {
        getContentResolver().unregisterContentObserver(mGloveModeObserver);
    }

    private void setGloveModeEnabled(boolean value) {
        Settings.System.putInt(getContentResolver(), Settings.System.GLOVE_MODE, value ? 1 : 0);
    }

    private boolean isGloveModeEnabled() {
        return Settings.System.getInt(getContentResolver(), Settings.System.GLOVE_MODE, 0) == 1;
    }

    private void setGloveModePreference(boolean checked) {
        if (mGloveModePreference != null) {
            mGloveModePreference.setChecked(checked);
        }
    }


    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                (mTogglePowerButtonEndsCallPreference.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void handleLockScreenRotationPreferenceClick() {
        RotationPolicy.setRotationLockForAccessibility(getActivity(),
                !mToggleLockScreenRotationPreference.isChecked());
    }

    private void handleToggleSpeakPasswordPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD,
                mToggleSpeakPasswordPreference.isChecked() ? 1 : 0);
    }

    private void handleToggleLargePointerIconPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON,
                mToggleLargePointerIconPreference.isChecked() ? 1 : 0);
    }

    private void handleToggleMasterMonoPreferenceClick() {
        Settings.System.putIntForUser(getContentResolver(), Settings.System.MASTER_MONO,
                mToggleMasterMonoPreference.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private void handleToggleEnableAccessibilityGesturePreferenceClick() {
        Bundle extras = mGlobalGesturePreferenceScreen.getExtras();
        extras.putString(EXTRA_TITLE, getString(
                R.string.accessibility_global_gesture_preference_title));
        extras.putString(EXTRA_SUMMARY, getString(
                R.string.accessibility_global_gesture_preference_description));
        extras.putBoolean(EXTRA_CHECKED, Settings.Global.getInt(getContentResolver(),
                Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, 0) == 1);
        super.onPreferenceTreeClick(mGlobalGesturePreferenceScreen);
    }

    private void handleDisplayMagnificationPreferenceScreenClick() {
        Bundle extras = mDisplayMagnificationPreferenceScreen.getExtras();
        extras.putString(EXTRA_TITLE, getString(
                R.string.accessibility_screen_magnification_title));
        extras.putCharSequence(EXTRA_SUMMARY, getActivity().getResources().getText(
                R.string.accessibility_screen_magnification_summary));
        extras.putBoolean(EXTRA_CHECKED, Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1);
        super.onPreferenceTreeClick(mDisplayMagnificationPreferenceScreen);
    }

    private void handleDisplayDaltonizerPreferenceScreenClick() {
        Bundle extras = mDisplayDaltonizerPreferenceScreen.getExtras();
        extras.putString(EXTRA_TITLE, getString(
                R.string.accessibility_display_inversion_preference_title));
        extras.putCharSequence(EXTRA_SUMMARY, getActivity().getResources().getText(
                R.string.accessibility_display_inversion_preference_subtitle));
        extras.putBoolean(EXTRA_CHECKED, Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0) == 1);
        super.onPreferenceTreeClick(mDisplayDaltonizerPreferenceScreen);
    }

    private void initializeAllPreferences() {
        mServicesCategory = (PreferenceCategory) findPreference(SERVICES_CATEGORY);
        mSystemsCategory = (PreferenceCategory) findPreference(SYSTEM_CATEGORY);
        mPacketGloveCategory = (PreferenceCategory) findPreference(PACKET_GLOVE_CATEGORY);
        mVirtualRecentCategory = (PreferenceCategory) findPreference(VIRTUAL_RECENT_KEY_CATEGORY);
        mDisplayCategory = (PreferenceCategory) findPreference(DISPLAY_CATEGORY);

        getPreferenceScreen().removePreference(mServicesCategory);

        // Text contrast.
        mToggleHighTextContrastPreference =
                (SwitchPreference) findPreference(TOGGLE_HIGH_TEXT_CONTRAST_PREFERENCE);
        mSystemsCategory.removePreference(mToggleHighTextContrastPreference);

        mPacketModePreference =
                (SwitchPreference) findPreference(PACKET_MODE_PREFERENCE);

        PackageManager pm = getPackageManager();
        mHasGloveModeHWFeature = pm.hasSystemFeature(PackageManager.FEATURE_ASUS_GLOVE);
        mGloveModePreference =
                (SwitchPreference) findPreference(GLOVE_MODE_PREFERENCE);
        if(!mHasGloveModeHWFeature){
            mPacketGloveCategory.removePreference(mGloveModePreference);
            mPacketModePreference.setLayoutResource(R.layout.asusres_preference_material_nodivider);
        }else {
            mGloveModeObserver = new GloveModeContentObserver(mGloveModeHandler);
        }

        // Display inversion.
        mToggleInversionPreference = (SwitchPreference) findPreference(TOGGLE_INVERSION_PREFERENCE);
        mToggleInversionPreference.setOnPreferenceChangeListener(this);
        mDisplayCategory.removePreference(mToggleInversionPreference);

        // Power button ends calls.
        mTogglePowerButtonEndsCallPreference =
                (SwitchPreference) findPreference(TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE);
        if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                || !Utils.isVoiceCapable(getActivity())) {
            mSystemsCategory.removePreference(mTogglePowerButtonEndsCallPreference);
        }

        // Lock screen rotation.
        mToggleLockScreenRotationPreference =
                (SwitchPreference) findPreference(TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE);
//        if (!RotationPolicy.isRotationSupported(getActivity())) {
            mSystemsCategory.removePreference(mToggleLockScreenRotationPreference);
//        }

        // Speak passwords.
        mToggleSpeakPasswordPreference =
                (SwitchPreference) findPreference(TOGGLE_SPEAK_PASSWORD_PREFERENCE);

        // Large pointer icon.
        mToggleLargePointerIconPreference =
                (SwitchPreference) findPreference(TOGGLE_LARGE_POINTER_ICON);
        mSystemsCategory.removePreference(mToggleLargePointerIconPreference);

        // Master Mono
        mToggleMasterMonoPreference =
                (SwitchPreference) findPreference(TOGGLE_MASTER_MONO);
        mSystemsCategory.removePreference(mToggleMasterMonoPreference);

        // Long press timeout.
        mSelectLongPressTimeoutPreference =
                (ListPreference) findPreference(SELECT_LONG_PRESS_TIMEOUT_PREFERENCE);
        mSelectLongPressTimeoutPreference.setOnPreferenceChangeListener(this);

        //jack_qi
        mSelectRecentKeyFuntionPreference =
                (ListPreference) findPreference(SELECT_RECENT_KEY_FUNCTION_PREFERENCE);
        mSelectRecentKeyFuntionPreference.setOnPreferenceChangeListener(this);

        mVirtualKeyPreference =
                (PreferenceScreen) findPreference(VIRTUAL_KEY_PREFERENCE_SCREEN);
        if(SystemProperties.get("qemu.hw.mainkeys") != null && SystemProperties.get("qemu.hw.mainkeys").equals("1")){
            mVirtualRecentCategory.removePreference(mVirtualKeyPreference);
        }

        if (mLongPressTimeoutValuetoTitleMap.size() == 0) {
            String[] timeoutValues = getResources().getStringArray(
                    R.array.long_press_timeout_selector_values);
            mLongPressTimeoutDefault = Integer.parseInt(timeoutValues[0]);
            String[] timeoutTitles = getResources().getStringArray(
                    R.array.long_press_timeout_selector_titles);
            final int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                mLongPressTimeoutValuetoTitleMap.put(timeoutValues[i], timeoutTitles[i]);
            }
        }

        //jack_qi
        if (mRecentKeyFunctionValuetoTitleMap.size() == 0) {
            String[] recentKeyValues = getResources().getStringArray(
                    R.array.recent_key_selector_values);
            mRecentKeyValueDefault = Integer.parseInt(recentKeyValues[0]);
            String[] RecentKeyTitles = getResources().getStringArray(
                    R.array.recent_key_selector_titles);
            final int RecentKeyValueCount = recentKeyValues.length;
            for (int i = 0; i < RecentKeyValueCount; i++) {
                mRecentKeyFunctionValuetoTitleMap.put(recentKeyValues[i], RecentKeyTitles[i]);
            }
        }

        // Captioning.
        mCaptioningPreferenceScreen = (PreferenceScreen) findPreference(
                CAPTIONING_PREFERENCE_SCREEN);

        // Display magnification.
        mDisplayMagnificationPreferenceScreen = (PreferenceScreen) findPreference(
                DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN);

        // Font size.
        mFontSizePreferenceScreen = (PreferenceScreen) findPreference(
                FONT_SIZE_PREFERENCE_SCREEN);
        mSystemsCategory.removePreference(mFontSizePreferenceScreen);

        // Autoclick after pointer stops.
        mAutoclickPreferenceScreen = (PreferenceScreen) findPreference(
                AUTOCLICK_PREFERENCE_SCREEN);
        mSystemsCategory.removePreference(mAutoclickPreferenceScreen);

        // Display color adjustments.
        mDisplayDaltonizerPreferenceScreen = (PreferenceScreen) findPreference(
                DISPLAY_DALTONIZER_PREFERENCE_SCREEN);

        // Global gesture.
        mGlobalGesturePreferenceScreen =
                (PreferenceScreen) findPreference(ENABLE_ACCESSIBILITY_GESTURE_PREFERENCE_SCREEN);
        final int longPressOnPowerBehavior = getActivity().getResources().getInteger(
                com.android.internal.R.integer.config_longPressOnPowerBehavior);
        final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
        if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                || longPressOnPowerBehavior != LONG_PRESS_POWER_GLOBAL_ACTIONS) {
            // Remove accessibility shortcut if power key is not present
            // nor long press power does not show global actions menu.
            mSystemsCategory.removePreference(mGlobalGesturePreferenceScreen);
        }

        mInstantCameraSwitchPref = (SwitchPreference) mSystemsCategory.findPreference(KEY_INSTANT_CAMERA_SWITCH);
//        if(!SystemProperties.get("asus.hardware.instant_camera").equals("1")){
//            mSystemsCategory.removePreference(mInstantCameraSwitchPref);
//        }

    }

    private void updateAllPreferences() {
        updateServicesPreferences();
        updateSystemPreferences();
        updateInstantCameraPreference();
    }

    private void updateServicesPreferences() {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go and then based on
        // the global accessibility state to decided whether it is enabled.

        // Generate.
        mServicesCategory.removeAll();

        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getActivity());

        List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(
                getActivity());
        List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        final boolean accessibilityEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            AccessibilityServiceInfo info = installedServices.get(i);

            RestrictedPreference preference = new RestrictedPreference(getActivity());
            String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();

            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);

            preference.setKey(componentName.flattenToString());

            preference.setTitle(title);
            final boolean serviceEnabled = accessibilityEnabled
                    && enabledServices.contains(componentName);
            String serviceEnabledString;
            if (serviceEnabled) {
                serviceEnabledString = getString(R.string.accessibility_feature_state_on);
            } else {
                serviceEnabledString = getString(R.string.accessibility_feature_state_off);
            }

            // Disable all accessibility services that are not permitted.
            String packageName = serviceInfo.packageName;
            boolean serviceAllowed =
                    permittedServices == null || permittedServices.contains(packageName);
            if (!serviceAllowed && !serviceEnabled) {
                EnforcedAdmin admin = RestrictedLockUtils.checkIfAccessibilityServiceDisallowed(
                        getActivity(), serviceInfo.packageName, UserHandle.myUserId());
                if (admin != null) {
                    preference.setDisabledByAdmin(admin);
                } else {
                    preference.setEnabled(false);
                }
            } else {
                preference.setEnabled(true);
            }

            preference.setSummary(serviceEnabledString);

            preference.setOrder(i);
            preference.setFragment(ToggleAccessibilityServicePreferenceFragment.class.getName());
            preference.setPersistent(true);

            Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, preference.getKey());
            extras.putBoolean(EXTRA_CHECKED, serviceEnabled);
            extras.putString(EXTRA_TITLE, title);

            String description = info.loadDescription(getPackageManager());
            if (TextUtils.isEmpty(description)) {
                description = getString(R.string.accessibility_service_default_description);
            }
            extras.putString(EXTRA_SUMMARY, description);

            String settingsClassName = info.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        getString(R.string.accessibility_menu_item_settings));
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(info.getResolveInfo().serviceInfo.packageName,
                                settingsClassName).flattenToString());
            }

            extras.putParcelable(EXTRA_COMPONENT_NAME, componentName);

            mServicesCategory.addPreference(preference);
        }

        if (mServicesCategory.getPreferenceCount() == 0) {
            if (mNoServicesMessagePreference == null) {
                mNoServicesMessagePreference = new Preference(getPrefContext());
                mNoServicesMessagePreference.setPersistent(false);
                mNoServicesMessagePreference.setLayoutResource(
                        R.layout.text_description_preference);
                mNoServicesMessagePreference.setSelectable(false);
                mNoServicesMessagePreference.setSummary(
                        getString(R.string.accessibility_no_services_installed));
            }
            mServicesCategory.addPreference(mNoServicesMessagePreference);
        }
    }

    private void updateSystemPreferences() {
        // Text contrast.
        mToggleHighTextContrastPreference.setChecked(
                Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, 0) == 1);

        // If the quick setting is enabled, the preference MUST be enabled.
        mToggleInversionPreference.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0) == 1);

        mPacketModePreference.setChecked(
                Settings.System.getInt(getContentResolver(),
                        PACKET_MODE_KEY, 0) == 1);

        mGloveModePreference.setChecked(
                Settings.System.getInt(getContentResolver(),
                        Settings.System.GLOVE_MODE, 0) == 1);

        // Power button ends calls.
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                && Utils.isVoiceCapable(getActivity())) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mTogglePowerButtonEndsCallPreference.setChecked(powerButtonEndsCall);
        }

        // Auto-rotate screen
        updateLockScreenRotationCheckbox();

        // Speak passwords.
        final boolean speakPasswordEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD, 0) != 0;
        mToggleSpeakPasswordPreference.setChecked(speakPasswordEnabled);

        // Large pointer icon.
        mToggleLargePointerIconPreference.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON, 0) != 0);

        // Master mono
        updateMasterMono();

        // Long press timeout.
        final int longPressTimeout = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, mLongPressTimeoutDefault);
        String value = String.valueOf(longPressTimeout);
        mSelectLongPressTimeoutPreference.setValue(value);
        mSelectLongPressTimeoutPreference.setSummary(mLongPressTimeoutValuetoTitleMap.get(value));

        //jack_qi
        final int recentKey = Settings.System.getInt(getContentResolver(),
                SELECT_RECENT_KEY_FUNCTION_KEY, mRecentKeyValueDefault);
        String recentKeyValue = String.valueOf(recentKey);
        mSelectRecentKeyFuntionPreference.setValue(recentKeyValue);
        mSelectRecentKeyFuntionPreference.setSummary(mRecentKeyFunctionValuetoTitleMap.get(recentKeyValue));

        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED,
                mCaptioningPreferenceScreen);
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                mDisplayMagnificationPreferenceScreen);
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                mDisplayDaltonizerPreferenceScreen);

        updateFontSizeSummary(mFontSizePreferenceScreen);

        updateAutoclickSummary(mAutoclickPreferenceScreen);

        // Global gesture
        updateGlobalGesture();
    }

    private void updateGlobalGesture() {
        // Remove accessibility shortcut if TalkBack is not present
        if (mServicesCategory.findPreference("com.google.android.marvin.talkback/"
                + "com.google.android.marvin.talkback.TalkBackService") == null) {
            mSystemsCategory.removePreference(mGlobalGesturePreferenceScreen);
        } else {
            final boolean globalGestureEnabled = Settings.Global.getInt(getContentResolver(),
                    Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, 0) == 1;
            mGlobalGesturePreferenceScreen.setSummary(globalGestureEnabled ?
                    R.string.accessibility_global_gesture_preference_summary_on:
                    R.string.accessibility_global_gesture_preference_summary_off);
        }
    }

    private void updateFeatureSummary(String prefKey, Preference pref) {
        final boolean enabled = Settings.Secure.getInt(getContentResolver(), prefKey, 0) == 1;
        pref.setSummary(enabled ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off);
    }

    private void updateAutoclickSummary(Preference pref) {
        final boolean enabled = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, 0) == 1;
        if (!enabled) {
            pref.setSummary(R.string.accessibility_feature_state_off);
            return;
        }
        int delay = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_DEFAULT);
        pref.setSummary(ToggleAutoclickPreferenceFragment.getAutoclickPreferenceSummary(
                getResources(), delay));
    }

    private void updateFontSizeSummary(Preference pref) {
        final float currentScale = Settings.System.getFloat(getContext().getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);
        final Resources res = getContext().getResources();
        final String[] entries = res.getStringArray(R.array.entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        final int index = ToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale,
                strEntryValues);
        pref.setSummary(entries[index]);
    }

    private void updateLockScreenRotationCheckbox() {
        Context context = getActivity();
        if (context != null) {
            mToggleLockScreenRotationPreference.setChecked(
                    !RotationPolicy.isRotationLocked(context));
        }
    }

    private void updateMasterMono() {
        final boolean masterMono = Settings.System.getIntForUser(
                getContentResolver(), Settings.System.MASTER_MONO,
                0 /* default */, UserHandle.USER_CURRENT) == 1;
        mToggleMasterMonoPreference.setChecked(masterMono);
    }

    private static boolean isSingleDensityEnable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(FEATURE_SINGLE_DENSITY);
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<SearchIndexableRaw>();

            PackageManager packageManager = context.getPackageManager();
            AccessibilityManager accessibilityManager = (AccessibilityManager)
                    context.getSystemService(Context.ACCESSIBILITY_SERVICE);

            String screenTitle = context.getResources().getString(
                    R.string.accessibility_services_title);


            // Indexing all services, regardless if enabled.
            List<AccessibilityServiceInfo> services = accessibilityManager
                    .getInstalledAccessibilityServiceList();
            final int serviceCount = services.size();
           /* for (int i = 0; i < serviceCount; i++) {
                AccessibilityServiceInfo service = services.get(i);
                if (service == null || service.getResolveInfo() == null) {
                    continue;
                }

                ServiceInfo serviceInfo = service.getResolveInfo().serviceInfo;
                ComponentName componentName = new ComponentName(serviceInfo.packageName,
                        serviceInfo.name);

                SearchIndexableRaw indexable = new SearchIndexableRaw(ToggleAccessibilityServicePreferenceFragment.getContext());
                indexable.key = componentName.flattenToString();
                indexable.title = service.getResolveInfo().loadLabel(packageManager).toString();
                indexable.summaryOn = context.getString(R.string.accessibility_feature_state_on);
                indexable.summaryOff = context.getString(R.string.accessibility_feature_state_off);
                indexable.screenTitle = screenTitle;

                indexables.add(indexable);
            }*/

            return indexables;
        }

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
               boolean enabled) {
            List<SearchIndexableResource> indexables = new ArrayList<SearchIndexableResource>();
            SearchIndexableResource indexable = new SearchIndexableResource(context);
            indexable.xmlResId = R.xml.accessibility_settings;
            indexables.add(indexable);
            return indexables;
        }
                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();

                    result.add(TOGGLE_HIGH_TEXT_CONTRAST_PREFERENCE);
                    result.add(TOGGLE_INVERSION_PREFERENCE);
                    if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                            || !Utils.isVoiceCapable(context)) {
                        result.add(TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE);
                    }
                    result.add(TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE);
                    result.add(TOGGLE_LARGE_POINTER_ICON);
                    result.add(TOGGLE_MASTER_MONO);
                    if(SystemProperties.get("qemu.hw.mainkeys") != null && SystemProperties.get("qemu.hw.mainkeys").equals("1")){
                        result.add(VIRTUAL_KEY_PREFERENCE_SCREEN);
                    }
                    result.add(FONT_SIZE_PREFERENCE_SCREEN);
                    result.add(AUTOCLICK_PREFERENCE_SCREEN);

                    final int longPressOnPowerBehavior = context.getResources().getInteger(
                            com.android.internal.R.integer.config_longPressOnPowerBehavior);
                    final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
                    if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                            || longPressOnPowerBehavior != LONG_PRESS_POWER_GLOBAL_ACTIONS) {
                        result.add(ENABLE_ACCESSIBILITY_GESTURE_PREFERENCE_SCREEN);
                    }
                    return result;
                }
    };
}
