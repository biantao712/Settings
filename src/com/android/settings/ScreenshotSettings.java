
package com.android.settings;

import java.lang.reflect.Field;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.TwoStatePreference;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.util.Log;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.graphics.drawable.Drawable;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.util.ResCustomizeConfig;

public class ScreenshotSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "ScreenshotSettings";

    private static final String KEY_HOT_KEY = "hot_key";

    private static final String KEY_SOUND = "sound";
    private static final String KEY_NOTIFY = "notify";

    private static final String KEY_SCREENSHOT_FORMAT = "screenshot_format";

    private static final String KEY_SCREENSHOT_HOT_KEY = "screenshot_hot_key";

    private static final String KEY_SCREENSHOT_SOUND = "screenshot_sound";
    private static final String KEY_SCREENSHOT_NOTIFY = "screenshot_notify";

    private static final String SCREENSHOT = "screenshot";

    private static final String SCREENSHOT_FORMAT = "screenshot_format";

    private static final String LONG_PRESSED_FUNC = "long_pressed_func";

    private static final String SCREENSHOT_SOUND = "screenshot_sound";
    private static final String SCREENSHOT_NOTIFY = "screenshot_notify";

    private static final int LONG_PRESSED_FUNC_SCREENSHOT = 0;

    private static final int LONG_PRESSED_FUNC_MENU = 1;

    private static final int LONG_PRESSED_FUNC_DEFAULT = LONG_PRESSED_FUNC_MENU;

    private ListPreference mScreenshotFormat;

    private TwoStatePreference mScreenshotHotkey;
    private TwoStatePreference mScreenshotSound;
    private TwoStatePreference mScreenshotNotify;
    private boolean mIsInMultiWindowMode;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.screenshot_settings);

        mScreenshotFormat = (ListPreference) findPreference(KEY_SCREENSHOT_FORMAT);
        mScreenshotFormat.setOnPreferenceChangeListener(this);

        PreferenceCategory screenShotHotKey = (PreferenceCategory) findPreference(KEY_HOT_KEY);
        mScreenshotHotkey = (TwoStatePreference) screenShotHotKey.findPreference(KEY_SCREENSHOT_HOT_KEY);
        mScreenshotHotkey.setOnPreferenceChangeListener(this);

        PreferenceCategory screenshotSound = (PreferenceCategory) findPreference(KEY_SOUND);
        mScreenshotSound = (TwoStatePreference) screenshotSound.findPreference(KEY_SCREENSHOT_SOUND);
        mScreenshotSound.setOnPreferenceChangeListener(this);

        PreferenceCategory screenshotNotify = (PreferenceCategory) findPreference(KEY_NOTIFY);
        mScreenshotNotify = (TwoStatePreference) screenshotNotify.findPreference(KEY_SCREENSHOT_NOTIFY);
        mScreenshotNotify.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateState(true);
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        mScreenshotHotkey.setEnabled(!isInMultiWindowMode);
    }

    private void updateState(boolean force) {
        try {
            boolean isInMultiWindowMode = WindowManagerGlobal.getWindowManagerService().getDockedStackSide() != WindowManager.DOCKED_INVALID;
            if(!isInMultiWindowMode){
                if(!mScreenshotHotkey.isEnabled()) mScreenshotHotkey.setEnabled(true);
                mScreenshotHotkey
                        .setChecked(Settings.System.getInt(getContentResolver(), SCREENSHOT) == 1);
            } else if(mScreenshotHotkey.isEnabled()) mScreenshotHotkey.setEnabled(false);
        } catch (Settings.SettingNotFoundException snfe) {
            Log.e(TAG, SCREENSHOT + " not found");
        } catch (RemoteException e) {
            Log.w(TAG, "Failed to get dock side: " + e);
        }
        mScreenshotSound.setChecked(Settings.System.getInt(getContentResolver(), SCREENSHOT_SOUND, 1) == 1);
        mScreenshotNotify.setChecked(Settings.System.getInt(getContentResolver(), SCREENSHOT_NOTIFY, 1) == 1);
        final int screenshotFormat = Settings.System.getInt(getContentResolver(),
                SCREENSHOT_FORMAT, 0);
        mScreenshotFormat.setValueIndex(screenshotFormat);
        updateScreenshotPreferenceDescription(getContentResolver(), screenshotFormat);
    }

    private void updateScreenshotPreferenceDescription(ContentResolver resolver,
            int screenshotFormat) {
        mScreenshotFormat
                .setSummary((screenshotFormat == 0) ? R.string.jpeg_tag : R.string.png_tag);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (KEY_SCREENSHOT_FORMAT.equals(key)) {
            int value = Integer.parseInt((String) newValue);
            try {
                Settings.System.putInt(getContentResolver(),
                        SCREENSHOT_FORMAT, value);
                updateScreenshotPreferenceDescription(getContentResolver(), value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        } else if (KEY_SCREENSHOT_HOT_KEY.equals(key)) {
            final boolean isScreenshot = (Boolean)newValue;
            if (isScreenshot) {
                Settings.System.putInt(getContentResolver(), LONG_PRESSED_FUNC,
                        LONG_PRESSED_FUNC_SCREENSHOT);
            } else {
                Settings.System.putInt(getContentResolver(), LONG_PRESSED_FUNC,
                        LONG_PRESSED_FUNC_DEFAULT);
            }
            Settings.System.putInt(getContentResolver(), SCREENSHOT, isScreenshot ? 1 : 0);
        } else if (KEY_SCREENSHOT_SOUND.equals(key)) {
            final boolean isSoundEnable = (Boolean)newValue;
            Settings.System.putInt(getContentResolver(), SCREENSHOT_SOUND, isSoundEnable ? 1 : 0);
        } else if (KEY_SCREENSHOT_NOTIFY.equals(key)) {
            final boolean isNotifyEnable = (Boolean)newValue;
            Settings.System.putInt(getContentResolver(), SCREENSHOT_NOTIFY, isNotifyEnable ? 1 : 0);
        }
        return true;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.MAIN_SETTINGS;
    }
}
