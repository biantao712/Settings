package com.asus.settings.lockscreen;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;

/**
 * Created by scottsu on 2016/5/25.
 */
public class AsusLSShortcutHelp {

    private HashMap<ComponentName, ComponentName> mReplaceIconMap = new HashMap<ComponentName, ComponentName>();
    private static String[] REPLACE_APP_PACKAGE = new String[]{
            AsusShortcutConsts.LAUNCHER_ASUS_BROWSER_PACKAGE_NAME,
            AsusShortcutConsts.LAUNCHER_ASUS_MESSAGE_PACKAGE_NAME,
            AsusShortcutConsts.LAUNCHER_ASUS_CALENDAR_PACKAGE_NAME,
            AsusShortcutConsts.LAUNCHER_ASUS_EMAIL_PACKAGE_NAME
    };

    private static String[] REPLACE_APP_CLASS = new String[]{
            AsusShortcutConsts.LAUNCHER_ASUS_BROWSER_CLASS_NAME,
            AsusShortcutConsts.LAUNCHER_ASUS_MESSAGE_CLASS_NAME,
            AsusShortcutConsts.LAUNCHER_ASUS_CALENDAR_CLASS_NAME,
            AsusShortcutConsts.LAUNCHER_ASUS_EMAIL_CLASS_NAME
    };

    private static ComponentName[] REPLACE_TO_APP = new ComponentName[]{
            new ComponentName(AsusShortcutConsts.LAUNCHER_CHROME_BROWSER_PACKAGE_NAME,
                    AsusShortcutConsts.LAUNCHER_CHROME_BROWSER_CLASS_NAME),
            new ComponentName(AsusShortcutConsts.LAUNCHER_GOOGLE_MESSAGE_PACKAGE_NAME,
                    AsusShortcutConsts.LAUNCHER_GOOGLE_MESSAGE_CLASS_NAME),
            new ComponentName(AsusShortcutConsts.LAUNCHER_GOOGLE_CALENDAR_PACKAGE_NAME,
                    AsusShortcutConsts.LAUNCHER_GOOGLE_CALENDAR_CLASS_NAME),
            new ComponentName(AsusShortcutConsts.LAUNCHER_GOOGLE_EMAIL_PACKAGE_NAME,
                    AsusShortcutConsts.LAUNCHER_GOOGLE_EMAIL_CLASS_NAME)
    };

    private static AsusLSShortcutHelp sInstance = null;
    public static AsusLSShortcutHelp getInstance() {
        if (sInstance == null) {
            synchronized (AsusLSShortcutHelp.class) {
                if (sInstance == null) {
                    sInstance = new AsusLSShortcutHelp();
                }
            }
        }
        return sInstance;
    }

    private AsusLSShortcutHelp() {
        for (int i = 0; i < REPLACE_TO_APP.length; ++i) {
            mReplaceIconMap.put(REPLACE_TO_APP[i], new ComponentName(REPLACE_APP_PACKAGE[i],
                    REPLACE_APP_CLASS[i]));
        }
    }

    public ComponentName getReplaceIconAppName(Context context, String packageName, String className) {
        ComponentName comp = new ComponentName(packageName, className);
        ComponentName iconComp = mReplaceIconMap.get(comp);
        if (iconComp != null) {
            String iconPackage = iconComp.getPackageName();
            if (!isAppInstalled(context, iconPackage)) {
                return iconComp;
            }
        }
        return null;
    }

    public boolean isAppInstalled(Context context, String packageName) {
        boolean result = true;
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName,
                    PackageManager.GET_ACTIVITIES);
            if (info == null) {
                result = false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            result = false;
        }
        return result;
    }
}
