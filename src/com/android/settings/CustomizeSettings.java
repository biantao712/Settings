package com.android.settings;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.graphics.drawable.Drawable;
import com.android.settings.R;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Handler;

public class CustomizeSettings extends SettingsPreferenceFragment
    implements OnPreferenceChangeListener
{
    private static final String TAG = "CustomizeSettings";
    private static final String KEY_RECENT_APPS_KEY_LONG_PRESS_CONTROL = "recent_apps_key_long_press_control_settings";
    private static final String KEY_RECENT_APPS_KEY_LONG_PRESS_CATEGORY = "recent_apps_key_long_press_category";
    private static final String SCREENSHOT = "screenshot";
    private static final String LONG_PRESSED_FUNC = "long_pressed_func";
    private static final int LONG_PRESSED_FUNC_SCREENSHOT = 0;
    private static final int LONG_PRESSED_FUNC_MULTIWINDOW = 1;
    private static final int LONG_PRESSED_FUNC_DEFAULT = LONG_PRESSED_FUNC_MULTIWINDOW;

    //BEGIN: Jeffrey_Chiang@asus.com Glove mode settings
    private static final String KEY_GLOVE_MODE_SETTING = "asus_glove_mode_setting";
    private static final String KEY_GLOVE_MODE_SETTING_CATEGORY = "asus_glove_mode_category";
    //END: Jeffrey_Chiang@asus.com

    // +++Arthur2_Liu: VZW
    private static final String KEY_SCREENSHOT_CATEGORY = "screenshot_category";
    private static final String KEY_SCREENSHOT_HOT_KEY = "screenshot_hot_key";
    private static final String KEY_SCREENSHOT_SOUND = "screenshot_sound";
    private static final String KEY_SCREENSHOT_FORMAT = "screenshot_format";
    private static final String SCREENSHOT_SOUND = "screenshot_sound";
    private static final String SCREENSHOT_FORMAT = "screenshot_format";
    private ListPreference mScreenshotFormat;
    private TwoStatePreference mScreenshotHotkey;
    private TwoStatePreference mScreenshotSound;
    // ---
    private ListPreference mRecentAppsKeyLongPressControlPref;

    //BEGIN: Jeffrey_Chiang@asus.com Glove mode settings
    private CheckBoxPreference mGloveModeSetting;
    private Handler mHandler = new Handler();
    private boolean mHasGloveModeHWFeature = false;
    private class GloveModeContentObserver extends ContentObserver {
        GloveModeContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            boolean value = isGloveModeEnabled();
            Log.v(TAG, "Glove mode settings onChange. Value is " + value);
            setGloveModeCheckBox(value);
        }
    }
    private ContentObserver mGloveModeObserver = null;
    //END: Jeffrey_Chiang@asus.com Glove mode settings

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.customize_settings);

        // +++Richo_Han: Code refactoring for VZW
        // +++Arthur2_Liu: VZW
        mScreenshotHotkey = (TwoStatePreference) findPreference(KEY_SCREENSHOT_HOT_KEY);
        mScreenshotHotkey.setOnPreferenceChangeListener(this);
        mScreenshotSound = (TwoStatePreference) findPreference(KEY_SCREENSHOT_SOUND);
        mScreenshotSound.setOnPreferenceChangeListener(this);
        mScreenshotFormat = (ListPreference) findPreference(KEY_SCREENSHOT_FORMAT);
        mScreenshotFormat.setOnPreferenceChangeListener(this);

        // ---
        if(Utils.isVerizonSKU()) { // +++Arthur2_Liu: VZW
            // Screenshot settings is preserved for Verizon project only.
            PreferenceCategory screenShotCategory = (PreferenceCategory) findPreference(KEY_SCREENSHOT_CATEGORY);
            screenShotCategory.removePreference(mScreenshotHotkey);
        } else {
            removePreference(KEY_SCREENSHOT_CATEGORY);
        }
        // ---Richo_Han
        mRecentAppsKeyLongPressControlPref = (ListPreference)findPreference(KEY_RECENT_APPS_KEY_LONG_PRESS_CONTROL);
        int mode = getRecentAppsKeyLongPressControlMode();
        if (mRecentAppsKeyLongPressControlPref != null) {
            mRecentAppsKeyLongPressControlPref.setValue(String.valueOf(mode));
            mRecentAppsKeyLongPressControlPref.setSummary(mRecentAppsKeyLongPressControlPref.getEntry());
            mRecentAppsKeyLongPressControlPref.setOnPreferenceChangeListener(this);
            Drawable icon = getResources().getDrawable(R.drawable.ic_sysbar_recent_default_resize_dark);
            mRecentAppsKeyLongPressControlPref.setDialogIcon(icon);
            Settings.System.putInt(getContentResolver(), SCREENSHOT,
                    (mode == LONG_PRESSED_FUNC_SCREENSHOT) ? 1 : 0);
            Settings.System.putInt(getContentResolver(), LONG_PRESSED_FUNC, mode);
        }

        // Add Game Genie Settings ++
        PreferenceCategory inAPPToolBarCategory = (PreferenceCategory) findPreference(KEY_IN_APP_TOOLBAR_CATEGORY);
        Preference gameToolBar = (Preference) findPreference(KEY_GAME_TOOLBAR_APP_SETTINGS);
        if ((inAPPToolBarCategory != null) && (gameToolBar != null)) {
            if (!isGameGenieExist(getActivity())) {
                // Game Genie APK not exists, remove Game Genie setting from "In-app Toolbar"
                inAPPToolBarCategory.removePreference(gameToolBar);
            }
        }
        if ((inAPPToolBarCategory != null) && inAPPToolBarCategory.getPreferenceCount() == 0) {
            // "In-app Toolbar" category contains nothing, remove category itself
            getPreferenceScreen().removePreference(inAPPToolBarCategory);
        }
        // Add Game Genie Settings --

        //BEGIN: Steven_Chao@asus.com Glove mode settings
        PackageManager pm = getPackageManager();
        mHasGloveModeHWFeature = pm.hasSystemFeature(PackageManager.FEATURE_ASUS_GLOVE);
        if (!mHasGloveModeHWFeature) {
            PreferenceCategory gloveModeSettingCategory = (PreferenceCategory) findPreference(KEY_GLOVE_MODE_SETTING_CATEGORY);
            getPreferenceScreen().removePreference(gloveModeSettingCategory);
        } else {
            mGloveModeSetting = (CheckBoxPreference) findPreference(KEY_GLOVE_MODE_SETTING);
            mGloveModeObserver = new GloveModeContentObserver(mHandler);
        }
        //END: Steven_Chao@asus.com Glove mode settings
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue)
    {
        final String key = preference.getKey();
        // +++Arthur2_Liu: VZW
        if (KEY_SCREENSHOT_FORMAT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(),
                        SCREENSHOT_FORMAT, value);
                updateScreenshotPreferenceDescription(getContentResolver(), value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        } else if (KEY_SCREENSHOT_HOT_KEY.equals(key)) {
            final boolean isScreenshot = (Boolean) objValue;
            if (isScreenshot) {
                Settings.System.putInt(getContentResolver(), LONG_PRESSED_FUNC,
                        LONG_PRESSED_FUNC_SCREENSHOT);
            } else {
                Settings.System.putInt(getContentResolver(), LONG_PRESSED_FUNC,
                        LONG_PRESSED_FUNC_DEFAULT);
            }
            Settings.System.putInt(getContentResolver(), SCREENSHOT, isScreenshot ? 1 : 0);
        } else if (KEY_SCREENSHOT_SOUND.equals(key)) {
            final boolean isSoundEnable = (Boolean)objValue;
            Settings.System.putInt(getContentResolver(), SCREENSHOT_SOUND, isSoundEnable ? 1 : 0);
        } else if(preference == mRecentAppsKeyLongPressControlPref){
            if (objValue instanceof String) {
                putRecentAppsKeyLongPressControlSetting(Integer.parseInt((String)objValue));
            }
        }
        // ---
        return true;
    }

     @Override
    public void onResume() {
        super.onResume();
        updateState(true);
    //BEGIN: Steven_Chao@asus.com Glove mode settings
        if (mHasGloveModeHWFeature) {
            setGloveModeCheckBox(isGloveModeEnabled());
            registerGloveModeContentObserver();
        }
    //END: Steven_Chao@asus.com Glove mode settings
    }

    @Override
    public void onStop() {
         super.onStop();
     //BEGIN: Steven_Chao@asus.com Glove mode settings
         if (mHasGloveModeHWFeature) {
             unregisterGloveModeContentObserver();
         }
     //END: Steven_Chao@asus.com Glove mode settings
    }

    private void updateState(boolean force) {
        updateRecentAppsKeyLongPressControlSetting();
        // +++Arthur2_Liu: VZW
        updateScreenshotHotkeySetting();

        mScreenshotSound.setChecked(Settings.System.getInt(getContentResolver(), SCREENSHOT_SOUND, 1) == 1);

        final int screenshotFormat = Settings.System.getInt(getContentResolver(),
                                     SCREENSHOT_FORMAT, 0);
        mScreenshotFormat.setValueIndex(screenshotFormat);
        updateScreenshotPreferenceDescription(getContentResolver(), screenshotFormat);
        // ---
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        if(mScreenshotHotkey != null) mScreenshotHotkey.setEnabled(!isInMultiWindowMode); // +++Arthur2_Liu: VZW
        if(mRecentAppsKeyLongPressControlPref != null) mRecentAppsKeyLongPressControlPref.setEnabled(!isInMultiWindowMode);
    }

    private void putRecentAppsKeyLongPressControlSetting(int mode) {
        Settings.System.putInt(getContentResolver(), SCREENSHOT,
                    (mode == LONG_PRESSED_FUNC_SCREENSHOT) ? 1 : 0);
        Settings.System.putInt(getContentResolver(), LONG_PRESSED_FUNC, mode);
        mRecentAppsKeyLongPressControlPref.setValue(String.valueOf(mode));
        mRecentAppsKeyLongPressControlPref.setSummary(mRecentAppsKeyLongPressControlPref.getEntry());
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // Add Game Genie Settings ++
        if (preference == findPreference(KEY_GAME_TOOLBAR_APP_SETTINGS)) {
            startGameGenieSettings(getActivity());
        }
        // Add Game Genie Settings --

        //BEGIN: Jeffrey_Chiang@asus.com Glove mode settings
        if (preference == mGloveModeSetting) {
            setGloveModeEnabled(mGloveModeSetting.isChecked());
        }
        //END: Jeffrey_Chiang@asus.com Glove mode settings

        return super.onPreferenceTreeClick(preference);
    }

    private int getRecentAppsKeyLongPressControlMode() {
        return Settings.System.getInt(getContentResolver(), LONG_PRESSED_FUNC,
            LONG_PRESSED_FUNC_DEFAULT);
    }

    // +++Arthur2_Liu: VZW
    private void updateScreenshotHotkeySetting() {
        if(mScreenshotHotkey == null) return;
        try {
            boolean isInMultiWindowMode = WindowManagerGlobal.getWindowManagerService().getDockedStackSide() != WindowManager.DOCKED_INVALID;
            if(!isInMultiWindowMode){
                if(!mScreenshotHotkey.isEnabled()) mScreenshotHotkey.setEnabled(true);
            mScreenshotHotkey.setChecked(Settings.System.getInt(getContentResolver(), SCREENSHOT) == 1);
            } else if(mScreenshotHotkey.isEnabled()) mScreenshotHotkey.setEnabled(false);
        } catch (Settings.SettingNotFoundException snfe) {
            Log.e(TAG, SCREENSHOT + " not found");
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get dock side: " + e);
        }
    }

    private void updateScreenshotPreferenceDescription(ContentResolver resolver, int screenshotFormat) {
        mScreenshotFormat.setSummary((screenshotFormat == 0)
                ? R.string.jpeg_tag
                        : R.string.png_tag);
    }
    // ---

    private void updateRecentAppsKeyLongPressControlSetting() {
        if(mRecentAppsKeyLongPressControlPref == null) return;
        try {
            boolean isInMultiWindowMode = WindowManagerGlobal.getWindowManagerService().getDockedStackSide() != WindowManager.DOCKED_INVALID;
            if(!isInMultiWindowMode){
                if(!mRecentAppsKeyLongPressControlPref.isEnabled()) mRecentAppsKeyLongPressControlPref.setEnabled(true);
                int mode = getRecentAppsKeyLongPressControlMode();
                mRecentAppsKeyLongPressControlPref.setValue(String.valueOf(mode));
                mRecentAppsKeyLongPressControlPref.setSummary(mRecentAppsKeyLongPressControlPref.getEntry());
            } else if(mRecentAppsKeyLongPressControlPref.isEnabled()) mRecentAppsKeyLongPressControlPref.setEnabled(false);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get dock side: " + e);
        }
    }

    /**
     * Return value can not be 0 (MetricsEvent.VIEW_UNKNOWN)
     * TODO: add new entry in frameworks
     */
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.MAIN_SETTINGS;
    }

    // Add Game Genie Settings ++
    private static final String KEY_IN_APP_TOOLBAR_CATEGORY = "in_app_toolbar_category";
    private static final String KEY_GAME_TOOLBAR_APP_SETTINGS = "game_toolbar_app_settings";
    private static final String GAMEGENIE_PACKAGE_NAME = "com.asus.gamewidget";
    private static final String ACTION_START_GAMEGENIE_SETTINGS = "com.asus.gamewidget.action.SETTINGS";
    private static final String EXTRA_KEY_GAMEGENIE_SETTINGS_CALLFROM = "callfrom";
    private static final String EXTRA_VALUE_GAMEGENIE_SETTINGS_CALLFROM_ASUSSETTINGS = "AsusSettings";

    private static boolean isGameGenieExist(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(GAMEGENIE_PACKAGE_NAME, 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {}
        return false;
    }

    private static void startGameGenieSettings(Context context) {
        try {
            context.startActivity(new Intent(ACTION_START_GAMEGENIE_SETTINGS).putExtra(EXTRA_KEY_GAMEGENIE_SETTINGS_CALLFROM, EXTRA_VALUE_GAMEGENIE_SETTINGS_CALLFROM_ASUSSETTINGS));
        } catch (ActivityNotFoundException e) {
            // ActivityNotFoundException
        }
    }
    // Add Game Genie Settings --

    public static boolean isGloveModeExist(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.GLOVE_MODE, 0) == 1;
    }

    //when NavigationBar exist, it won't show recent app key in Customize Settings
    public static boolean isNavigationBarExist() {
        boolean hasNaNavigationBar = false;
        try {
            hasNaNavigationBar = WindowManagerGlobal.getWindowManagerService().hasNavigationBar();
        } catch (Exception e) {
        }
        return hasNaNavigationBar;
    }

    //BEGIN: Jeffrey_Chiang@asus.com Glove mode settings
    private void registerGloveModeContentObserver() {
        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.GLOVE_MODE), true, mGloveModeObserver);
    }

    private void unregisterGloveModeContentObserver() {
        getContentResolver().unregisterContentObserver(mGloveModeObserver);
    }

    private void setGloveModeEnabled(boolean value) {
        Settings.System.putInt(getContentResolver(),Settings.System.GLOVE_MODE, value?1:0);
    }

    private boolean isGloveModeEnabled() {
        return Settings.System.getInt(getContentResolver(), Settings.System.GLOVE_MODE, 0) == 1;
    }

    private void setGloveModeCheckBox(boolean checked) {
        if (mGloveModeSetting != null) {
            mGloveModeSetting.setChecked(checked);
        }
    }
    //END: Jeffrey_Chiang@asus.com Glove mode settings
}
