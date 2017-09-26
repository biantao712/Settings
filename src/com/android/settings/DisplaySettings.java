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

package com.android.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.UiModeManager;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.text.TextUtils;
import android.util.Log;
//mars_li
//import android.preference.PreferenceScreen;
import android.os.UserHandle;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;

import com.android.settings.analytic.AnalyticUtils;
import com.android.settings.bluelightfilter.BluelightSwitchPreference;
import com.android.settings.bluelightfilter.BlueLightFilterHelper;
//end

import java.util.Arrays;


import com.android.internal.app.NightDisplayController;
//FlipFont {
import com.android.settings.bluelightfilter.BluelightFilterSwitchEnabler;
import com.android.settings.bluelightfilter.Constants;
import com.android.settings.bluelightfilter.TaskWatcherService5Level;
import com.android.settings.flipfont.FontListPreference;
//FlipFont }

//smilefish
import com.android.settings.memc.MemcSwitchPreference;
import com.android.settings.memc.MemcSwitchHelper;
//end
//FlipFont {
import com.android.settings.flipfont.FontListPreference;
//FlipFont }

//Display Size
import com.android.settings.display.ScreenZoomPreference;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.view.RotationPolicy;
import com.android.settings.accessibility.ToggleFontSizePreferenceFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.provider.Settings.SettingNotFoundException;

//+++ suleman
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.CheckedTextView;
import android.view.View;
import android.widget.TextView;
import android.view.Window;
import android.view.WindowManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.LayoutInflater;
//--- suleman

//==========lovelyfonts add ===========
import android.app.ActivityManagerNative;
import android.os.RemoteException;
import android.content.pm.PackageInfo;
import android.content.ActivityNotFoundException;
import android.content.pm.ApplicationInfo;
import android.support.v7.preference.PreferenceCategory;
//==========lovelyfonts end ===========

//  [AlwaysOn] Always-on Panel  - BEGIN 	107
import com.android.settings.util.AlwaysOnUtils;
//  [AlwaysOn] Always-on Panel  - END

import static android.provider.Settings.Global.TOUCH_KEY_LIGHT;
import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;
import static android.provider.Settings.Secure.CAMERA_GESTURE_DISABLED;
import static android.provider.Settings.Secure.DOUBLE_TAP_TO_WAKE;
import static android.provider.Settings.Secure.DOZE_ENABLED;
import static android.provider.Settings.Secure.WAKE_GESTURE_ENABLED;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "DisplaySettings";
//==========lovelyfonts add ===========
    private static final String KEY_LOVELYFONT_SETTING = "lovelyfont_setting";
    private Preference mLovelyFontSettingPreference;
    private PreferenceCategory mFontCategory;
    private static final String KEY_FONT_CATEGORY = "font_category";
    private static final boolean mIsCNSKU = Build.ASUSSKU.equals("CN");
//==========lovelyfonts end ===========

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_FONT_SIZE = "font_size";
    //+++ suleman
    //private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    //private static final String KEY_TOUCH_KEY_LIGHT = "touch_key_light";
    //private static final String KEY_SCREEN_SAVER = "screensaver";
    //private static final String KEY_LIFT_TO_WAKE = "lift_to_wake";
    //private static final String KEY_DOZE = "doze";
    //private static final String KEY_TAP_TO_WAKE = "tap_to_wake";
    //--- suleman

    private static final String KEY_AUTO_BRIGHTNESS = "auto_brightness";
    private static final String KEY_AUTO_ROTATE = "auto_rotate";
    private static final String KEY_NIGHT_DISPLAY = "night_display";
    private static final String KEY_NIGHT_MODE = "night_mode";
    //+++ suleman
    //private static final String KEY_CAMERA_GESTURE = "camera_gesture";
    //private static final String KEY_CAMERA_DOUBLE_TAP_POWER_GESTURE
    //        = "camera_double_tap_power_gesture";
    //private static final String KEY_WALLPAPER = "wallpaper";
    //--- suleman
    private static final String KEY_VR_DISPLAY_PREF = "vr_display_pref";

    // Hide Wi-Fi display with PlayTo existed
    private static final String KEY_WIFI_DISPLAY = "wifi_display";

    // FlipFont {
    private static final String KEY_FLIPFONT = "MONOTYPE";
    // FlipFont }

    // Dispaly size
    private static final String KEY_DISPLAY_SIZE = "screen_zoom";
    private static final String FEATURE_SINGLE_DENSITY = "asus.software.singledensity";


    // Eason1_Wang
    private static final String KEY_PQ_CHIP_MEMC = "pq_chip_memc";
//    private static final String KEY_PQ_CHIP_MEMC_VERIZON = "pq_chip_memc_verizon"; //add by smilefish
    public static final String ASUS_SPLENDID_SCREEN_MODE_OPTION = "asus_splendid_screen_mode_option";
    public static final String ASUS_SPLENDID_SCREEN_MODE_CURRENT_RES_ID = "asus_splendid_screen_mode_current_res_id";
    public static final String ASUS_SPLENDID_PACKAGE_NAME = "com.asus.splendid";
    public static final int SCREEN_MODE_OPTION_BALANCE = 0;
    public static final int SCREEN_MODE_OPTION_READING = 1;
    public static final int SCREEN_MODE_OPTION_VIVID = 2;
    public static final int SCREEN_MODE_OPTION_CUSTOMIZED = 3;


    public static final int TOUCH_KEY_LIGHT_OFF = 0;
    public static final int TOUCH_KEY_LIGHT_ON = 1;
    public static final int TOUCH_KEY_LIGHT_DEFAULT = TOUCH_KEY_LIGHT_OFF;

    private Preference mFontSizePref;
    private SwitchPreference mNotificationPulse;
    private SwitchPreference mTouchKeyLight;
    private SwitchPreference mAutoRotatePreference;


    // FlipFont {
    private FontListPreference mFlipfontPreference;
    // FlipFont }

   //Eason1_Wang +++
    private Preference mScreenColorModeScreen;
    private MemcSwitchHelper mMemcSwitchHelper; //smilefish
    private int mScreenColorMode;
    private ScreenModeOptionObserver mScreenModeOptionObserver;
    private Resources mSplendidRes;
    private BluelightSwitchPreference mBluelightFilterScreen;
    private SwitchPreference mBluelightFilterSwitchPreference;
    private BlueLightFilterHelper mBluelightFilterHelper;
    private PreferenceCategory mDisplayCategoryScreen;
    //Eason1_Wang ---


    // Dispaly size
    private ScreenZoomPreference mScreenZoomPreference;

    private TimeoutListPreference mScreenTimeoutPreference;
    private ListPreference mNightModePreference;
    private Preference mScreenSaverPreference;
    private SwitchPreference mLiftToWakePreference;
    private SwitchPreference mDozePreference;
    private SwitchPreference mTapToWakePreference;
    private SwitchPreference mCameraGesturePreference;

    private AlertDialog mFontDialog;
    private CheckedTextView smallFont;
    private CheckedTextView defaultFont;
    private CheckedTextView bigFont;
    private CheckedTextView largeFont;
    private Float mFontScale;

    //  [AlwaysOn] Always-on Panel  - BEGIN 	232
    private static final String KEY_ALWAYS_ON = "always_on_panel";
    private static final String KEY_SYSTEM_CATEGORY = "display_system_category";
    private boolean mAlwaysOnPreferenceIsShowed = false;
    private Preference mAlwaysOnPreference;
    private PreferenceCategory mSystemCategory;
    //  [AlwaysOn] Always-on Panel  - END

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DISPLAY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        //+++ suleman
        /*
        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }
        */
        //--- suleman
        mScreenTimeoutPreference = (TimeoutListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        disableUnusableTimeouts(mScreenTimeoutPreference);

        mFontSizePref = (Preference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (preference == mFontSizePref) {
                mFontDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.title_font_size)
                        .setNegativeButton(R.string.security_device_admin_active_cancel, new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO Auto-generated method stub
                                 mFontDialog.dismiss();

                            }

                        })
                    .setView(createView())
                    .setCancelable(true)
                    .create();
                mFontDialog.show();
                Window dialogWindow = mFontDialog.getWindow();
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.BOTTOM;
                dialogWindow.setAttributes(lp);
                }
                //end

                return false;
            }
        });

        mFontSizePref.setOnPreferenceChangeListener(this);

        
        //+++ suleman
        /*
        if (isLiftToWakeAvailable(activity)) {
            mLiftToWakePreference = (SwitchPreference) findPreference(KEY_LIFT_TO_WAKE);
            mLiftToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_LIFT_TO_WAKE);
        }
        mNotificationPulse = (SwitchPreference) findPreference(KEY_NOTIFICATION_PULSE);
        //+++ sheng-en_fann@asus.com
        //if (mNotificationPulse != null
        //        && getResources().getBoolean(
        //                com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
        //    mDisplayCategory.removePreference(mNotificationPulse);
        //} else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);

                File file = new File("/sys/class/leds/red/brightness");
                if(!file.exists()) {
                    removePreference(KEY_NOTIFICATION_PULSE);
                }
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
                removePreference(KEY_NOTIFICATION_PULSE);
            }
        //}
        if (isVirtualKeyAvailable(activity)) {
            mTouchKeyLight = (SwitchPreference) findPreference(KEY_TOUCH_KEY_LIGHT);
            mTouchKeyLight.setChecked(Settings.Global.getInt(getContentResolver(), TOUCH_KEY_LIGHT,
                    TOUCH_KEY_LIGHT_DEFAULT) == TOUCH_KEY_LIGHT_ON);
            mTouchKeyLight.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_TOUCH_KEY_LIGHT);
        }
        if (isDozeAvailable(activity)) {
            mDozePreference = (SwitchPreference) findPreference(KEY_DOZE);
            mDozePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_DOZE);
        }
        */
        //--- suleman
        // Eason1_Wang@asus.com
        // add screen color mode entry point
        mBluelightFilterScreen = (BluelightSwitchPreference)findPreference("bluelight_filter_mode");
        mDisplayCategoryScreen = (PreferenceCategory) findPreference("display_category_screen");
        mBluelightFilterSwitchPreference = (SwitchPreference) findPreference("bluelight_filter_switch");
        int option = Settings.System.getInt(getContentResolver(),
                Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
        mBluelightFilterSwitchPreference.setChecked(option == 1);
        mBluelightFilterSwitchPreference.setOnPreferenceChangeListener(this);
        mScreenColorModeScreen =  findPreference("screen_color_mode");
        //if (!getPackageManager().hasSystemFeature("asus.hardware.display.splendid")){
        if (!BlueLightFilterHelper.isAppInstalled(mScreenColorModeScreen.getContext(), ASUS_SPLENDID_PACKAGE_NAME)){
            mDisplayCategoryScreen.removePreference(mScreenColorModeScreen);
            mScreenColorModeScreen = null;
            if(!BlueLightFilterHelper.hasSplendidFeature(getActivity())){
                getPreferenceScreen().removePreference(mBluelightFilterScreen);
                mBluelightFilterScreen = null;
                mDisplayCategoryScreen.removePreference(mBluelightFilterSwitchPreference);
            }else{
                 mSplendidRes = getSplendidRes();
                 mBluelightFilterHelper = new BlueLightFilterHelper(mBluelightFilterScreen, getActivity());
            }
        } else {
            if(mBluelightFilterScreen != null){
                getPreferenceScreen().removePreference(mBluelightFilterScreen);
                mBluelightFilterScreen = null;
            }
            mDisplayCategoryScreen.removePreference(mBluelightFilterSwitchPreference);
            mSplendidRes = getSplendidRes();
            mScreenModeOptionObserver = new ScreenModeOptionObserver(new Handler());
            mScreenColorModeScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    //mars_li
                    if (preference == mScreenColorModeScreen) {
                        Intent intent = new Intent();
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setClassName(ASUS_SPLENDID_PACKAGE_NAME, "com.asus.splendid.AsusSplendidActivity");
                        startActivity(intent);
                    }
                    //end

                    return false;
                }
            });

            if(getPackageManager().getApplicationEnabledSetting(ASUS_SPLENDID_PACKAGE_NAME)==PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER){
                mScreenColorModeScreen.setEnabled(false);
            }else{
                mScreenColorModeScreen.setEnabled(true);
            }
        }

        if(mBluelightFilterScreen != null){
            getPreferenceScreen().removePreference(mBluelightFilterScreen);
        }
        //mars_li--

        //mars_li++
        // add picture quality memc level
//        MemcSwitchPreference memcPreferenceScreenVerizon;
//        Preference memcPreferenceScreen;
//        if(MemcSwitchHelper.isVerizonDevice()) {
//            memcPreferenceScreen = findPreference(KEY_PQ_CHIP_MEMC);
//            getPreferenceScreen().removePreference(memcPreferenceScreen);
//            memcPreferenceScreen = null;
//            memcPreferenceScreenVerizon = (MemcSwitchPreference) findPreference(KEY_PQ_CHIP_MEMC_VERIZON);
//            if (!MemcSwitchHelper.hasMemcFeature(getActivity()) ||
//                    ActivityManager.getCurrentUser() != UserHandle.USER_OWNER ||
//                    MemcSwitchHelper.isAbsentDevice()) {
//                getPreferenceScreen().removePreference(memcPreferenceScreenVerizon);
//                memcPreferenceScreenVerizon = null;
//            } else {
//                mMemcSwitchHelper = new MemcSwitchHelper(memcPreferenceScreenVerizon, getActivity()); //smilefish
//            }
//        }else{
//            memcPreferenceScreenVerizon = (MemcSwitchPreference) findPreference(KEY_PQ_CHIP_MEMC_VERIZON);
//            getPreferenceScreen().removePreference(memcPreferenceScreenVerizon);
//            memcPreferenceScreenVerizon = null;
//            memcPreferenceScreen = findPreference(KEY_PQ_CHIP_MEMC);
//            if (!MemcSwitchHelper.hasMemcFeature(getActivity()) ||
//                    ActivityManager.getCurrentUser() != UserHandle.USER_OWNER ||
//                    MemcSwitchHelper.isAbsentDevice()) {
//                getPreferenceScreen().removePreference(memcPreferenceScreen);
//                memcPreferenceScreen = null;
//            } else {
//                mMemcSwitchHelper = new MemcSwitchHelper(memcPreferenceScreen, getActivity()); //smilefish
//            }
//        }
        //mars_li--

        //+++ suleman
        /*
        if (isTapToWakeAvailable(getResources())) {
            mTapToWakePreference = (SwitchPreference) findPreference(KEY_TAP_TO_WAKE);
            mTapToWakePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_TAP_TO_WAKE);
        }
        if (isCameraGestureAvailable(getResources())) {
            mCameraGesturePreference = (SwitchPreference) findPreference(KEY_CAMERA_GESTURE);
            mCameraGesturePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_CAMERA_GESTURE);
        }
        if (isCameraDoubleTapPowerGestureAvailable(getResources())) {
            mCameraDoubleTapPowerGesturePreference
                    = (SwitchPreference) findPreference(KEY_CAMERA_DOUBLE_TAP_POWER_GESTURE);
            mCameraDoubleTapPowerGesturePreference.setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_CAMERA_DOUBLE_TAP_POWER_GESTURE);
        }
        */
        //--- suleman
        //++Suleman,change DropDownPreference to SwitchPreference
        if (RotationPolicy.isRotationSupported(getActivity())) {
            mAutoRotatePreference = (SwitchPreference) findPreference(KEY_AUTO_ROTATE);
            mAutoRotatePreference .setOnPreferenceChangeListener(this);
        } else {
            removePreference(KEY_AUTO_ROTATE);
        }
        //+++ suleman
        /*
        if (RotationPolicy.isRotationLockToggleVisible(activity)) {
            DropDownPreference rotatePreference =
                    (DropDownPreference) findPreference(KEY_AUTO_ROTATE);
            int rotateLockedResourceId;
            // The following block sets the string used when rotation is locked.
            // If the device locks specifically to portrait or landscape (rather than current
            // rotation), then we use a different string to include this information.
            if (allowAllRotations(activity)) {
                rotateLockedResourceId = R.string.display_auto_rotate_stay_in_current;
            } else {
                if (RotationPolicy.getRotationLockOrientation(activity)
                        == Configuration.ORIENTATION_PORTRAIT) {
                    rotateLockedResourceId =
                            R.string.display_auto_rotate_stay_in_portrait;
                } else {
                    rotateLockedResourceId =
                            R.string.display_auto_rotate_stay_in_landscape;
                }
            }
            rotatePreference.setEntries(new CharSequence[] {
                    activity.getString(R.string.display_auto_rotate_rotate),
                    activity.getString(rotateLockedResourceId),
            });
            rotatePreference.setEntryValues(new CharSequence[] { "0", "1" });
            rotatePreference.setValueIndex(RotationPolicy.isRotationLocked(activity) ?
                    1 : 0);
            rotatePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final boolean locked = Integer.parseInt((String) newValue) != 0;
                    MetricsLogger.action(getActivity(), MetricsEvent.ACTION_ROTATION_LOCK,
                            locked);
                    RotationPolicy.setRotationLock(activity, locked);
                    return true;
                }
            });
        } else {
            removePreference(KEY_AUTO_ROTATE);
        }
        */
        //--- suleman
        if (isVrDisplayModeAvailable(activity)) {
            DropDownPreference vrDisplayPref =
                    (DropDownPreference) findPreference(KEY_VR_DISPLAY_PREF);
            vrDisplayPref.setEntries(new CharSequence[] {
                    activity.getString(R.string.display_vr_pref_low_persistence),
                    activity.getString(R.string.display_vr_pref_off),
            });
            vrDisplayPref.setEntryValues(new CharSequence[] { "0", "1" });

            final Context c = activity;
            int currentUser = ActivityManager.getCurrentUser();
            int current = Settings.Secure.getIntForUser(c.getContentResolver(),
                            Settings.Secure.VR_DISPLAY_MODE,
                            /*default*/Settings.Secure.VR_DISPLAY_MODE_LOW_PERSISTENCE,
                            currentUser);
            vrDisplayPref.setValueIndex(current);
            vrDisplayPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    int i = Integer.parseInt((String) newValue);
                    int u = ActivityManager.getCurrentUser();
                    if (!Settings.Secure.putIntForUser(c.getContentResolver(),
                            Settings.Secure.VR_DISPLAY_MODE,
                            i, u)) {
                        Log.e(TAG, "Could not change setting for " +
                                Settings.Secure.VR_DISPLAY_MODE);
                    }
                    return true;
                }
            });
        } else {
            removePreference(KEY_VR_DISPLAY_PREF);
        }

        mNightModePreference = (ListPreference) findPreference(KEY_NIGHT_MODE);
        if (mNightModePreference != null) {
            final UiModeManager uiManager = (UiModeManager) getSystemService(
                    Context.UI_MODE_SERVICE);
            final int currentNightMode = uiManager.getNightMode();
            mNightModePreference.setValue(String.valueOf(currentNightMode));
            mNightModePreference.setOnPreferenceChangeListener(this);
        }

        // Hide Wi-Fi display with PlayTo existed
        // Update wifiDisplay preference: remove it if need
        if (DisplayManager.isPlayToExist(activity)) {
            removePreference(KEY_WIFI_DISPLAY);
        }

        // FlipFont {
        // Remove flipfont preference in DisplaySettings page if not OWNER user.
        mFlipfontPreference = (FontListPreference)findPreference(KEY_FLIPFONT);
        if (mFlipfontPreference != null) {
            if(!mFlipfontPreference.isFlipfontSupport()
                || (ActivityManager.getCurrentUser() != UserHandle.USER_OWNER)
                || Utils.isVerizon() || Utils.isVerizonSKU()) {
                getPreferenceScreen().removePreference(mFlipfontPreference);
            }
        }
        // FlipFont }

        //==========lovelyfonts add ===========
        mLovelyFontSettingPreference = (Preference)findPreference(KEY_LOVELYFONT_SETTING);
        mFontCategory = (PreferenceCategory)findPreference(KEY_FONT_CATEGORY);
        if (mIsCNSKU && Build.FEATURES.LOVELYFONTS_SUPPORT) {
            if (mFontCategory != null && mFlipfontPreference != null) {
                mFontCategory.removePreference(mFlipfontPreference);
            }
            if(mFontCategory != null && mLovelyFontSettingPreference != null
                && (!resoleLovelyFontsApp(mLovelyFontSettingPreference.getContext()))){
                mFontCategory.removePreference(mLovelyFontSettingPreference);
            }
        }else if(mFontCategory != null && mLovelyFontSettingPreference != null){
            mFontCategory.removePreference(mLovelyFontSettingPreference);
        }
        //==========lovelyfonts end ===========

        // Hide Display Size with Single_Density feature
        mScreenZoomPreference = (ScreenZoomPreference)findPreference(KEY_DISPLAY_SIZE);
        if (isSingleDensityEnable(activity)){
            getPreferenceScreen().removePreference(mScreenZoomPreference);
        }

        //  [AlwaysOn] Always-on Panel  - BEGIN
        mSystemCategory = (PreferenceCategory)findPreference(KEY_SYSTEM_CATEGORY);
        mAlwaysOnPreference = (Preference) findPreference(KEY_ALWAYS_ON);
        if (mAlwaysOnPreference != null) {
            if (AlwaysOnUtils.showPreference(activity)) {
                mAlwaysOnPreferenceIsShowed = true;
            }
            else {
                mAlwaysOnPreferenceIsShowed = false;
                mSystemCategory.removePreference(mAlwaysOnPreference);
                mAutoRotatePreference.setLayoutResource(R.layout.asusres_preference_material_nodivider);
            }
        } //END OF if (mAlwaysOnPreference != null) 	574
        //  [AlwaysOn] Always-on Panel  - END
    }

    //BEGIN:SamYF_Chen@asus.com
    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {

        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        CharSequence[] resultEntries, resultValues;

        String sku = SystemProperties.get("ro.product.name", "");
        long max = SystemProperties.getLong("ro.config.screen_timeout_max",0);

        if (sku.toLowerCase().startsWith("vzw_")  ||   max != 0   ) {
            final long maxTimeout =  max != 0  ?  max : 600000L ;      //Verizon remove 30mins and nerver, thus MAX is 10 mins
            ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
            ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();

            for (int i = 0; i < values.length; i++) {
                long timeout = Long.parseLong(values[i].toString());
                if(timeout <= maxTimeout && timeout != 0){
                    revisedEntries.add(entries[i]);
                    revisedValues.add(values[i]);
                }
            }
            resultEntries = revisedEntries.toArray(new CharSequence[revisedEntries.size()]);
            resultValues = revisedValues.toArray(new CharSequence[revisedValues.size()]);
        }else{
            resultEntries = Arrays.copyOf(entries, entries.length+1);
            resultEntries[entries.length] = getString(R.string.never_timeout_entry);
            resultValues = Arrays.copyOf(values, values.length+1);
            resultValues[values.length] = "0";
        }
        screenTimeoutPreference.setEntries(resultEntries);
        screenTimeoutPreference.setEntryValues(resultValues);
    }
    //END:SamYF_Chen@asus.com


    //+++ suleman
    private void handleLockScreenRotationPreferenceClick() {
        //RotationPolicy.setRotationLockForAccessibility(
        //Don't rotate screen when disable Auto-rotate screen
        RotationPolicy.setRotationLock(getActivity(),
                mAutoRotatePreference.isChecked());
    }

    private void handleBluelightFilterPreferenceClick(boolean isChecked) {
        Settings.System.putInt(getContentResolver(),
                Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, (isChecked)?1:0);
        Intent service_intent = new Intent(getActivity(), TaskWatcherService5Level.class);
        service_intent.putExtra(Constants.EXTRA_QUICKSETTING_READER_MODE_ON_OFF, (isChecked)?1:0);
        getActivity().startService(service_intent);
    }

    private void updateLockScreenRotationCheckbox() {
        Context context = getActivity();
        if (context != null) {
            mAutoRotatePreference.setChecked(
                    !RotationPolicy.isRotationLocked(context));
        }
    }

    private final RotationPolicyListener mRotationPolicyListener = new RotationPolicyListener() {
        @Override
        public void onChange() {
            updateLockScreenRotationCheckbox();
        }
    };
    //--- suleman
    private static boolean allowAllRotations(Context context) {
        return Resources.getSystem().getBoolean(
                com.android.internal.R.bool.config_allowAllRotations);
    }

    private static boolean isLiftToWakeAvailable(Context context) {
        SensorManager sensors = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        return sensors != null && sensors.getDefaultSensor(Sensor.TYPE_WAKE_GESTURE) != null;
    }

    private static boolean isDozeAvailable(Context context) {
        String name = Build.IS_DEBUGGABLE ? SystemProperties.get("debug.doze.component") : null;
        if (TextUtils.isEmpty(name)) {
            name = context.getResources().getString(
                    com.android.internal.R.string.config_dozeComponent);
        }
        return !TextUtils.isEmpty(name);
    }

    private static boolean isTapToWakeAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    private static boolean isAutomaticBrightnessAvailable(Resources res) {
        return res.getBoolean(com.android.internal.R.bool.config_automatic_brightness_available);
    }

    private static boolean isCameraGestureAvailable(Resources res) {
        boolean configSet = res.getInteger(
                com.android.internal.R.integer.config_cameraLaunchGestureSensorType) != -1;
        return configSet &&
                !SystemProperties.getBoolean("gesture.disable_camera_launch", false);
    }

    private static boolean isVrDisplayModeAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE);
    }

    private static boolean isVirtualKeyAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_ASUS_VIRTUAL_KEY);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        TimeoutListPreference preference = mScreenTimeoutPreference;
        //+++ suleman
        if (preference.isDisabledByAdmin()) {
            preference.setSummary(getString(R.string.disabled_by_policy_title));
            return;
        }
        //---
        final Resources res = getActivity().getResources();
        String summary = " ";
        final CharSequence[] entries = preference.getEntries();
        final CharSequence[] values = preference.getEntryValues();
        int best = 0;
        // Resolve screen never timeout error for other language  ~Javid 2011.03.01~
        if (currentTimeout == 0L) {
            summary = getString(R.string.never_timeout_summary);
        } else {
            for (int i = 0; i < (values.length); i++) {
                long timeout = Long.parseLong(values[i].toString());
                if (currentTimeout >= timeout && timeout != 0L) {
                    best = i;
                }
                summary = getString(R.string.screen_timeout_summary, entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    private static boolean isSingleDensityEnable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(FEATURE_SINGLE_DENSITY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState();

        final long currentTimeout = Settings.System.getLong(getActivity().getContentResolver(),
                SCREEN_OFF_TIMEOUT, FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        //+++ suleman
        final DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            final EnforcedAdmin admin = RestrictedLockUtils.checkIfMaximumTimeToLockIsSet(
                    getActivity());
            final long maxTimeout = dpm
                    .getMaximumTimeToLockForUserAndProfiles(UserHandle.myUserId());
            mScreenTimeoutPreference.removeUnusableTimeouts(maxTimeout, admin);
        }
        //---
        updateTimeoutPreferenceDescription(currentTimeout);

        //+++ suleman
        //disablePreferenceIfManaged(KEY_WALLPAPER, UserManager.DISALLOW_SET_WALLPAPER);
        //--- suleman
		//mars_li
        if(mScreenColorModeScreen != null){
            if(getPackageManager().getApplicationEnabledSetting(ASUS_SPLENDID_PACKAGE_NAME)==PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER){
                mScreenColorModeScreen.setEnabled(false);
            }else{
                mScreenColorModeScreen.setEnabled(true);
            }
        }

        if (mScreenModeOptionObserver != null) {
            getContentResolver().registerContentObserver(Settings.System.getUriFor(ASUS_SPLENDID_SCREEN_MODE_OPTION), false,
                    mScreenModeOptionObserver);
        }
        if(mBluelightFilterHelper != null){
            mBluelightFilterHelper.registerObserver();
        }

        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH), true,
                mBluelight_Switch_Observer);
        //end

        //smilefish
        if(mMemcSwitchHelper != null){
            mMemcSwitchHelper.onResume();
        }
        //end

        //+++ suleman
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScreenModeOptionObserver != null) {
            getContentResolver().unregisterContentObserver(mScreenModeOptionObserver);
        }
        if(mBluelightFilterHelper != null){
            mBluelightFilterHelper.onPause();
        }
        //smilefish
        if(mMemcSwitchHelper != null){
            mMemcSwitchHelper.onPause();
        }
        //end

       //+++ suleman
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }

        getContentResolver().unregisterContentObserver(mBluelight_Switch_Observer);

    }

    private ContentObserver mBluelight_Switch_Observer = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onSwitchChanged();
        }
    };

    private void onSwitchChanged() {
        int option = Settings.System.getInt(getContentResolver(),
                Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
        mBluelightFilterSwitchPreference.setChecked(option==1);
    }

    private void updateState() {
        updateFontSizeSummary();
        updateScreenSaverSummary();

        //+++ suleman
        /*
        // Update lift-to-wake if it is available.
        if (mLiftToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), WAKE_GESTURE_ENABLED, 0);
            mLiftToWakePreference.setChecked(value != 0);
        }
        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOZE_ENABLED, 1);
            mDozePreference.setChecked(value != 0);
        }
        // Update tap to wake if it is available.
        if (mTapToWakePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, 0);
            mTapToWakePreference.setChecked(value != 0);
        }
        */
        //--- suleman

        // Update doze if it is available.
        if (mDozePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), DOZE_ENABLED, 1);
            mDozePreference.setChecked(value != 0);
        }

        // Update camera gesture #1 if it is available.
        if (mCameraGesturePreference != null) {
            int value = Settings.Secure.getInt(getContentResolver(), CAMERA_GESTURE_DISABLED, 0);
            mCameraGesturePreference.setChecked(value == 0);
        }

        //+++ suleman
        // Update camera gesture #2 if it is available.
        //if (mCameraDoubleTapPowerGesturePreference != null) {
        //    int value = Settings.Secure.getInt(
        //            getContentResolver(), CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, 0);
        //    mCameraDoubleTapPowerGesturePreference.setChecked(value == 0);
        //}
        //--- suleman

        //Eason1_Wang +++ update splendid
        if (mScreenColorModeScreen != null) {
            mScreenColorMode = Settings.System.getInt(getContentResolver(),
                    ASUS_SPLENDID_SCREEN_MODE_OPTION, SCREEN_MODE_OPTION_BALANCE); //default balance mode
            updateScreenColorModeTitle();
        }
        //+++ suleman, update Auto Rotate if it is available.
        if(mAutoRotatePreference != null){
            updateLockScreenRotationCheckbox();
        }
        //end

        //  [AlwaysOn] Always-on Panel  - BEGIN
        updateAlwaysOnPreferenceSummary();
        //  [AlwaysOn] Always-on Panel  - END
    }

    private void updateScreenSaverSummary() {
        //+++ suleman
        /*
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
        */
        //--- suleman
    }

	private void updateScreenColorModeTitle() {
        int modeSummary =-1;
        int resIdOfSplendid = 0;
        switch (mScreenColorMode) {
        case SCREEN_MODE_OPTION_BALANCE:
            if(mSplendidRes != null)
                resIdOfSplendid = mSplendidRes.getIdentifier("balance_mode_text", "string", ASUS_SPLENDID_PACKAGE_NAME);
            modeSummary = R.string.splendid_balance_mode_text;
            break;
        case SCREEN_MODE_OPTION_READING:
            if(mSplendidRes != null)
                resIdOfSplendid = mSplendidRes.getIdentifier("reading_mode_text_L", "string", ASUS_SPLENDID_PACKAGE_NAME);
            modeSummary = R.string.splendid_reading_mode_text;
            break;
        case SCREEN_MODE_OPTION_VIVID:
            if(mSplendidRes != null)
                resIdOfSplendid = mSplendidRes.getIdentifier("vivid_mode_text", "string", ASUS_SPLENDID_PACKAGE_NAME);
            modeSummary = R.string.splendid_vivid_mode_text;
            break;
        case SCREEN_MODE_OPTION_CUSTOMIZED:
            if(mSplendidRes != null)
                resIdOfSplendid = mSplendidRes.getIdentifier("customized_mode_text", "string", ASUS_SPLENDID_PACKAGE_NAME);
            modeSummary = R.string.splendid_customized_mode_text;
            break;
        default:
            if(mSplendidRes != null) {
                try {
                    resIdOfSplendid = Settings.System.getInt(getContentResolver(),ASUS_SPLENDID_SCREEN_MODE_CURRENT_RES_ID , -1);
                    if (resIdOfSplendid > 0)
                        mSplendidRes.getString(resIdOfSplendid);
                } catch (NotFoundException e) {
                    resIdOfSplendid = -1;
                    e.printStackTrace();
                }
            }
        }
        if(mSplendidRes != null && resIdOfSplendid >0)
            mScreenColorModeScreen.setSummary(mSplendidRes.getString(resIdOfSplendid));
        else if(modeSummary == -1 || resIdOfSplendid == -1)
            mScreenColorModeScreen.setSummary(R.string.splendid_balance_mode_text);
        else
            mScreenColorModeScreen.setSummary(modeSummary);
    }

    private Resources getSplendidRes() {
        PackageManager manager = getPackageManager();
        Resources splendidResources = null;
        try {
            splendidResources = manager.getResourcesForApplication(ASUS_SPLENDID_PACKAGE_NAME);
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Unable to load splendid resource:"+e.getMessage());
        }
        return splendidResources;
    }
    //Eason1_Wang ---

	//Eason1_Wang +++
    private class ScreenModeOptionObserver extends ContentObserver {
        public ScreenModeOptionObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("ScreenModeOption", "ScreenModeOptionObserver onChange");
            int currentScreenMode = Settings.System.getInt(getContentResolver(), ASUS_SPLENDID_SCREEN_MODE_OPTION, SCREEN_MODE_OPTION_BALANCE);
            if (mScreenColorMode != currentScreenMode) {
                mScreenColorMode = currentScreenMode;
                updateScreenColorModeTitle();
            }
        }
    }

    //Eason1_Wang ---

    //+++ suleman
    /**
     *  Utility function that returns the index in a string array with which the represented value is
     *  the closest to a given float value.
     */
    public static int fontSizeValueToIndex(float val, String[] indices) {
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }
    //---

    private void updateFontSizeSummary() {
/*        final Context context = mFontSizePref.getContext();
        final Float currentScale = Settings.System.getFloat(context.getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);
        final Resources res = context.getResources();
        final String[] entries = res.getStringArray(R.array.entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        final int index = fontSizeValueToIndex(currentScale,
                strEntryValues);
        //mFontSizePref.setValue(currentScale.toString());
        mFontSizePref.setValueIndex(index);
        mFontSizePref.setSummary(entries[index]);
  */    initFontSize();
    }

    private void initFontSize() {
        final Float currentScale = Settings.System.getFloat(getActivity().getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);
        if (currentScale >= 0.83f && currentScale <= 0.86f) {
           if (null != smallFont) {
               smallFont.setChecked(true);
           }
           mFontSizePref.setSummary(R.string.display_font_size_small);
        } else if (currentScale >= 0.9f &&  currentScale <= 1.1f) {
           if (null != defaultFont) {
               defaultFont.setChecked(true);
           }
           mFontSizePref.setSummary(R.string.display_font_size_default);
        } else if (currentScale >= 1.11f && currentScale <= 1.18f) {
            if (null != bigFont) {
                bigFont.setChecked(true);
            }
            mFontSizePref.setSummary(R.string.display_font_size_big);
        } else if (currentScale >= 1.2f && currentScale <= 1.40f){
            if (null != largeFont) {
               largeFont.setChecked(true);
             }
             mFontSizePref.setSummary(R.string.display_font_size_large);
        }
    }

    private View createView() {
        View view = getActivity().getLayoutInflater().inflate(R.layout.asusres_display_font_dialog_view, null);
        //+++ suleman
        final Resources res = getActivity().getResources();
//        TextView myTitle = (TextView) view.findViewById(R.id.font_dialog_title);
//        myTitle.setText(R.string.title_font_size);
//        TextView cancelButton = (TextView) view.findViewById(R.id.font_dialog_cancel_buttton);
//        cancelButton.setOnClickListener(new View.OnClickListener()
//        {
//
//            @Override
//            public void onClick(View v) {
//            // TODO Auto-generated method stub
//            mFontDialog.dismiss();
//            }
//        });

        smallFont = (CheckedTextView) view.findViewById(R.id.checkedTextViewa);
        smallFont.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
            // TODO Auto-generated method stub
                mFontScale = 0.85f;
                if (!smallFont.isChecked()) {
                    smallFont.setChecked(true);
                }
                Settings.System.putFloat(getContentResolver(), Settings.System.FONT_SCALE, mFontScale);
                defaultFont.setChecked(false);
                bigFont.setChecked(false);
                largeFont.setChecked(false);
                mFontDialog.dismiss();
            }
        });
        defaultFont = (CheckedTextView) view.findViewById(R.id.checkedTextViewb);
        defaultFont.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
            // TODO Auto-generated method stubi
                mFontScale = 1.0f;
                if (!defaultFont.isChecked()) {
                    defaultFont.setChecked(true);
                }
                Settings.System.putFloat(getContentResolver(), Settings.System.FONT_SCALE, mFontScale);
                smallFont.setChecked(false);
                bigFont.setChecked(false);
                largeFont.setChecked(false);
                mFontDialog.dismiss();
            }
        });

        bigFont = (CheckedTextView) view.findViewById(R.id.checkedTextViewc);
        bigFont.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
            // TODO Auto-generated method stub
                mFontScale = 1.15f;
                if (!bigFont.isChecked()) {
                    bigFont.setChecked(true);
                }
                Settings.System.putFloat(getContentResolver(), Settings.System.FONT_SCALE, mFontScale);
                defaultFont.setChecked(false);
                smallFont.setChecked(false);
                largeFont.setChecked(false);
                mFontDialog.dismiss();
            }
        });

        largeFont = (CheckedTextView) view.findViewById(R.id.checkedTextViewd);
        largeFont.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v) {
            // TODO Auto-generated method stub
                mFontScale = 1.30f;
                if (!largeFont.isChecked()) {
                    largeFont.setChecked(true);
                }
                Settings.System.putFloat(getContentResolver(), Settings.System.FONT_SCALE, mFontScale);
                defaultFont.setChecked(false);
                bigFont.setChecked(false);
                smallFont.setChecked(false);
                mFontDialog.dismiss();
            }
        });

        initFontSize();
        return view;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            try {
                int value = Integer.parseInt((String) objValue);
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        //+++ suleman
        if (preference == mFontSizePref) {

/*
            Float mScale = Float.parseFloat(objValue.toString());
            //[AMAX][Font]Font Size notification dialog for extra-large font {
            if(mScale >= 1.30f) {
                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setTitle(android.R.string.dialog_alert_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.extra_large_font_size_warning)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Settings.System.putFloat(getContentResolver(), Settings.System.FONT_SCALE, mScale);
                            mFontSizePref.setValue(mScale.toString());
                            dialog.dismiss();
                        }
                    }).create();
                dialog.show();
            } else {
             //[AMAX][Font]Font Size notification dialog for extra-large font }
            Settings.System.putFloat(getContentResolver(), Settings.System.FONT_SCALE, mScale);
            mFontSizePref.setValue(mScale.toString());
            }*/
        }
        /*
        if (preference == mLiftToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), WAKE_GESTURE_ENABLED, value ? 1 : 0);
        }
        if (preference == mDozePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOZE_ENABLED, value ? 1 : 0);
        }
        if (preference == mTapToWakePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), DOUBLE_TAP_TO_WAKE, value ? 1 : 0);
        }
        */
        //--- suleman
        if (preference == mCameraGesturePreference) {
            boolean value = (Boolean) objValue;
            Settings.Secure.putInt(getContentResolver(), CAMERA_GESTURE_DISABLED,
                    value ? 0 : 1 /* Backwards because setting is for disabling */);
        }
        //+++ suleman
        //if (preference == mCameraDoubleTapPowerGesturePreference) {
        //    boolean value = (Boolean) objValue;
        //    Settings.Secure.putInt(getContentResolver(), CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
        //            value ? 0 : 1 /* Backwards because setting is for disabling */);
        //}
        //--- suleman
        
        if (preference == mNightModePreference) {
            try {
                final int value = Integer.parseInt((String) objValue);
                final UiModeManager uiManager = (UiModeManager) getSystemService(
                        Context.UI_MODE_SERVICE);
                uiManager.setNightMode(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist night mode setting", e);
            }
        }
        //+++ suleman

        /*
        if (preference == mNotificationPulse) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    value ? 1 : 0);
        }
        if (preference == mTouchKeyLight) {
            boolean value = (Boolean) objValue;
            Settings.Global.putInt(getContentResolver(), TOUCH_KEY_LIGHT,
                    value ? TOUCH_KEY_LIGHT_ON : TOUCH_KEY_LIGHT_OFF);
        }
        */
        //--- suleman
        //+++ suleman
        if (preference == mAutoRotatePreference) {
           handleLockScreenRotationPreferenceClick();
        }
        //--- suleman

        if(preference == mBluelightFilterSwitchPreference){
            boolean isChecked = (Boolean) objValue;
            handleBluelightFilterPreferenceClick(isChecked);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mDozePreference) {
            MetricsLogger.action(getActivity(), MetricsEvent.ACTION_AMBIENT_DISPLAY);
        }
        //asus flip cover---
        //  [AlwaysOn] Always-on Panel  - BEGIN
        else if (preference == mAlwaysOnPreference) {
            if(AlwaysOnUtils.checkCNAlwaysOnApkExist(getActivity())){
                AlwaysOnUtils.showCNSettingsActivity(getActivity());
            }else {
                AlwaysOnUtils.showSettingsActivity(getActivity());
            }
            return true;
        }
        //  [AlwaysOn] Always-on Panel  - END

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_display;
    }

    private void disablePreferenceIfManaged(String key, String restriction) {
        final RestrictedPreference pref = (RestrictedPreference) findPreference(key);
        if (pref != null) {
            pref.setDisabledByAdmin(null);
            if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(), restriction,
                    UserHandle.myUserId())) {
                pref.setEnabled(false);
            } else {
                pref.checkRestrictionAndSetDisabled(restriction);
            }
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;
        private BrightnessModeObserver mBrightnessModeObserver;
        private boolean mListening = false;
        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;

        }

        private class BrightnessModeObserver extends ContentObserver {
            public BrightnessModeObserver(Handler handler) {
                super(handler);
            }

            @Override
            public void onChange(boolean selfChange) {
                if(mListening == false){
                    return;
                }

                try {
                    updateSummary();
                } catch (Exception e) {

                }
            }
        }

        @Override
        public void setListening(boolean listening) {
            mListening = listening;
            if(mBrightnessModeObserver == null ){
                if(listening == true){
                    mBrightnessModeObserver = new BrightnessModeObserver(new Handler());
                    mContext.getContentResolver().registerContentObserver(
                        Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                        false, mBrightnessModeObserver , UserHandle.USER_ALL);
                }
            } else{
                if(mBrightnessModeObserver != null){
                    mContext.getContentResolver().unregisterContentObserver(mBrightnessModeObserver);
                    mBrightnessModeObserver = null;
                }
            }

            if (listening) {
                updateSummary();
            }
        }

        private void updateSummary() {
            boolean auto = Settings.System.getInt(mContext.getContentResolver(),
                    SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
                    == SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
//            mLoader.setSummary(this, mContext.getString(auto ? R.string.display_summary_on
//                    : R.string.display_summary_off));
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

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.display_settings;
                    result.add(sir);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    ArrayList<String> result = new ArrayList<String>();
                    //+++ suleman
                    /*
                    if (!context.getResources().getBoolean(
                            com.android.internal.R.bool.config_dreamsSupported)) {
                        result.add(KEY_SCREEN_SAVER);
                    }
                    if (!isLiftToWakeAvailable(context)) {
                        result.add(KEY_LIFT_TO_WAKE);
                    }
                    if (!isDozeAvailable(context)) {
                        result.add(KEY_DOZE);
                    }
                    */
                    //--- suleman
                    if (!RotationPolicy.isRotationLockToggleVisible(context)) {
                        result.add(KEY_AUTO_ROTATE);
                    }
                    //+++ suleman
                    //if (!isTapToWakeAvailable(context.getResources())) {
                    //    result.add(KEY_TAP_TO_WAKE);
                    //}
                    //if (!isCameraGestureAvailable(context.getResources())) {
                    //    result.add(KEY_CAMERA_GESTURE);
                    //}
                    //if (!isCameraDoubleTapPowerGestureAvailable(context.getResources())) {
                    //    result.add(KEY_CAMERA_DOUBLE_TAP_POWER_GESTURE);
                    //}
                    //--- suleman
                    if (!isVrDisplayModeAvailable(context)) {
                        result.add(KEY_VR_DISPLAY_PREF);
                    }
                    // Hide Wi-Fi display with PlayTo existed
                    if (DisplayManager.isPlayToExist(context)) {
                        result.add(KEY_WIFI_DISPLAY);
                    }

                    //  [AlwaysOn] Always-on Panel  - BEGIN
                    if (!AlwaysOnUtils.showPreference(context)) {
                        result.add(KEY_ALWAYS_ON);
                    } //END OF if (AlwaysOnUtils.showPreference(context))
                    //  [AlwaysOn] Always-on Panel  - END

                    //+++ suleman
                    //if (!isVirtualKeyAvailable(context)) {
                    //    result.add(KEY_TOUCH_KEY_LIGHT);
                    //}
                    //--- suleman
//                    if(MemcSwitchHelper.isVerizonDevice()) {
//                        result.add(KEY_PQ_CHIP_MEMC);
//                        if (!MemcSwitchHelper.hasMemcFeature(context) ||
//                                ActivityManager.getCurrentUser() != UserHandle.USER_OWNER ||
//                                MemcSwitchHelper.isAbsentDevice()) {
//                            result.add(KEY_PQ_CHIP_MEMC_VERIZON);
//                        }
//                    }else{
//                        result.add(KEY_PQ_CHIP_MEMC_VERIZON);
//                        if (!MemcSwitchHelper.hasMemcFeature(context) ||
//                                ActivityManager.getCurrentUser() != UserHandle.USER_OWNER ||
//                                MemcSwitchHelper.isAbsentDevice()) {
//                            result.add(KEY_PQ_CHIP_MEMC);
//                        }
//                    }

                    if (!BlueLightFilterHelper.isAppInstalled(context, ASUS_SPLENDID_PACKAGE_NAME)){
                        result.add("screen_color_mode");
                        if(!BlueLightFilterHelper.hasSplendidFeature(context)){
                            result.add("bluelight_filter_mode");
                        }
                    } else {
                        result.add("bluelight_filter_mode");
                    }
                    return result;
                }
            };

//==========lovelyfonts start ===========
    private boolean resoleLovelyFontsApp(Context context){
        final String sdkMain = "com.iekie.lovelyfonts.fonts.activity.MainActivity";
        if(resolveLovelyFontsComponent(context,"com.mephone.fonts",
                "com.mephone.fonts.activity.MainActivity")){
            return true;
        }
        if(resolveLovelyFontsComponent(context,"com.ekesoo.font",
                "com.ekesoo.font.activity.MainActivity")){
            return true;
        }
        if(resolveLovelyFontsComponent(context,"com.mephone.fonts", sdkMain)){
            return true;
        }
        if(resolveLovelyFontsComponent(context,"com.ekesoo.font", sdkMain)){
            return true;
        }
        if(resolveLovelyFontsComponent(context,"com.ekesoo.mtfont", sdkMain)){
            return true;
        }
        boolean found = false;
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> lai = pm.getInstalledApplications(0);
        for (ApplicationInfo ai : lai) {
            if (ai.packageName.startsWith("com.iekie.lovelyfonts.")) {
                if(resolveLovelyFontsComponent(context,ai.packageName, sdkMain)){
                     found = true;
                     break;
                }
            }
        }
        return found;
    }

    private boolean resolveLovelyFontsComponent(Context context,String pkgName,String comp){
        Intent intent = new Intent();
        intent.setClassName(pkgName, comp);
        if (context.getPackageManager().resolveActivity(intent, 0) != null) {
            return true;
        }
        return false;
    }
//==========lovelyfonts end ===========

    //  [AlwaysOn] Always-on Panel  - BEGIN
    private void updateAlwaysOnPreferenceSummary() {
        final Context context = this.getContext();

        if (mAlwaysOnPreferenceIsShowed && (mAlwaysOnPreference != null)) {
            if (AlwaysOnUtils.isEnabled(context)) {
                mAlwaysOnPreference.setSummary(R.string.settings_enable_alwayson_title);
            }
            else {
                mAlwaysOnPreference.setSummary(R.string.settings_enable_alwayson_title_off);
            }
        } //END OF if (mAlwaysOnPreferenceIsShowed && (mAlwaysOnPreference != null)) 	1256
    } //END OF updateAlwaysOnPreferenceSummary() 	1257
    //  [AlwaysOn] Always-on Panel  - END

}
