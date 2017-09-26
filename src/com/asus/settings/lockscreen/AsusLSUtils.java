package com.asus.settings.lockscreen;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

public class AsusLSUtils {

    public static final boolean DEBUG_FLAG = false;
    private static final String TAG = "AsusLSUtils";

    public static final boolean mIsCNLockScreen = isCNLockScreen();

    public static Intent getSettingLSWallpaperIntent() {
        Intent intent = new Intent();
        intent.setComponent( new ComponentName(
                "com.asus.launcher",
                "com.asus.themeapp.ThemeAppActivity"));
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("tabPosition", 1);
        return intent;
    }

    // AsusLauncher - New Activity
    public static Intent getSettingLSWallpaperIntent2(boolean useDefault) {
        Intent intent = new Intent();
        String action = getIntentField(
                "ACTION_SET_WALLPAPER_LOCKSCREEN",
                "android.intent.action.SET_WALLPAPER_LOCKSCREEN");
        intent.setAction(action);
        if (!useDefault) {
            intent.setComponent(new ComponentName(
                    "com.asus.launcher", "com.android.launcher3.WallpaperPickerActivity"));
        }
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public static String getIntentField(String fieldName, String defaultValue) {
        String rtn = defaultValue;
        try {
            Class<?> c = Class.forName("android.content.Intent");
            Object ob = c.newInstance();
            Field f = c.getField(fieldName);
            rtn = (String)f.get(ob);
        } catch (Exception e) {
            Log.w(TAG, "getIntentField E=" + e);
        }
        return rtn;
    }

    public static Intent getSettingLSThemeIntent() {
        Intent intent = new Intent();
        intent.setComponent( new ComponentName(
                "com.asus.themeapp",
                "com.asus.themeapp.ThemeAppActivity"));
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("from", "com.android.systemui.lockscreen");
        return intent;
    }

    private static final String THEME_APP_PACKAGE_NAME = "com.asus.themeapp";
    public static boolean isThemeAppEnabled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(THEME_APP_PACKAGE_NAME, 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "isThemeAppEnabled E=" + e);
            return false;
        }
    }

    private static final String LOCKSCREEN2_APP_PACKAGE_NAME = "com.asus.lockscreen2";
    public static boolean isLockscreen2AppEnabled(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(LOCKSCREEN2_APP_PACKAGE_NAME, 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "isLockscreen2AppEnabled E=" + e);
            return false;
        }
    }

    public static Intent getSettingShortcutIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                    "com.asus.lockscreen2",
                    "com.asus.lockscreen2.shortcut.AsusQuickAccessSettings"));
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    // ===== ++ Asus: Alvis_Liu: send intent to inform APPs(Clock AP) ++ =====
    private static final String SECURE_START_UP = "secure_start_up";
    private static final String ACTION_SECURE_START_UP_STATE_CHANGED = "secure_start_up_state_changed";
    public static void sendIntentForSecureStartUpStateChanged (Context context, boolean credentialRequired) {
        Intent intent = new Intent(ACTION_SECURE_START_UP_STATE_CHANGED);
        intent.putExtra(SECURE_START_UP, credentialRequired);
        context.sendBroadcast(intent);
    }
    // ===== -- Asus: Alvis_Liu: send intent to inform APPs(Clock AP) -- =====

    private static boolean isCNLockScreen() {
        boolean isCNLS = SystemProperties.getBoolean("ro.asus.cnlockscreen", false);
        Log.d(TAG, "isCNLockScreen=" + isCNLS);
        return isCNLS;
    }

    private static final String DEFAULT_SLIDESHOW_PATH =
            "/system/etc/AsusSystemUIRes/default_slideshow_wallpaper01.png";
    private static final String DEFAULT_SLIDESHOW_PATH2 =
            "/system/etc/AsusSystemUIRes/default_slideshow_wallpaper01.jpg";
    public static boolean isDefaultSlideShowExist(Context context) {
        File imgFile = new File(DEFAULT_SLIDESHOW_PATH);
        if (imgFile != null && imgFile.exists()) {
            return true;
        }
        File imgFile2 = new File(DEFAULT_SLIDESHOW_PATH2);
        if (imgFile2 != null && imgFile2.exists()) {
            return true;
        }
        Log.w(TAG, "checkDefaultSlideshowExist: No Default Wallpaper");
        return false;
    }

    private static final String ACTION_SLIDESHOW_WALLPAPER_SETTINGS = "com.asus.lockscreen.slideshow.settings";
    public static Intent getSettingLSSlideShowWallpaperIntent() {
        Intent intent = new Intent();
        intent.setAction(ACTION_SLIDESHOW_WALLPAPER_SETTINGS);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public static boolean isSlideShowSettingsExist(Context context) {
        if (context == null) {
            return false;
        }
        Intent intent = new Intent();
        intent.setAction(ACTION_SLIDESHOW_WALLPAPER_SETTINGS);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent, 0);
        if (list == null || list.size() <= 0) {
            return false;
        }
        return true;
    }
}
