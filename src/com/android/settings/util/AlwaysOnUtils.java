package com.android.settings.util;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.util.Log;

import java.util.List;


public class AlwaysOnUtils {
    private static final String GLOBAL_TAG = "Settings";
    private static final String TAG = GLOBAL_TAG + ":" + AlwaysOnUtils.class.getSimpleName();
    private static final Boolean DEBUG
                                = (android.os.Build.TYPE.equals("userdebug") ? true : false);


    //  database
    private static final String DB_KEY = "asus_alwayson_enable";
    private static final int DB_VALUE_UNKNOWN = -999999;
    private static final int DB_VALUE_DISALBE = 0;
    private static final int DB_VALUE_ENABLE = 1;
    //  component
    private static final String SETTINGS_ACTION = "com.asus.alwayson.action.ALWAYSON_SETTING";
    private static final String CN_SETTINGS_ACTION = "com.asus.alwayson.action.ALWAYSON_SETTING_CN";
    private static final String PACKAGE_NAME = "com.asus.alwayson";
    private static final String SETTINGS_CLASS_NAME
                                    = "com.asus.alwayson.settings.AlwaysOnSettingsActivity";
    private static final String CN_SETTINGS_CLASS_NAME
            = "com.asus.alwayson.settings.cn.AlwaysOnSettingsActivity";
    private static final ComponentName SETTINGS_COMPONENT
                                        = new ComponentName(PACKAGE_NAME, SETTINGS_CLASS_NAME);
    private static final ComponentName CN_SETTINGS_COMPONENT
            = new ComponentName(PACKAGE_NAME, CN_SETTINGS_CLASS_NAME);


    public static ComponentName getSettingsComponent() {
        return SETTINGS_COMPONENT;
    } //END OF getSettingsComponent()

    public static String getSettingsClassName() {
        return SETTINGS_COMPONENT.getClassName();
    } //END OF getSettingsClassName()

    public static String getSettingsPackageName() {
        return SETTINGS_COMPONENT.getPackageName();
    } //END OF getSettingsPackageName()

    public static boolean checkApkExist(Context context) {
        boolean result = false;

        try {
            result = (context.getPackageManager().getApplicationInfo(PACKAGE_NAME, 0) != null)
                            ? true : false;
        }
        catch (PackageManager.NameNotFoundException e1) {
            if (DEBUG) {
                Log.w(TAG, " Warning: AlwaysOn cannot be found!!! (maybe it is not installed)");
            } //END OF if (DEBUG)

            result = false;
        }
        catch (Exception e2) {
            Log.e(TAG, " ERROR: Unknown ERROR!!! (error=" + e2.getMessage() + ") ...", e2);

            result = false;
        }

        return result;
    } //END OF checkApkExist()

    public static boolean checkCNAlwaysOnApkExist(Context context){
        Intent intent = new Intent("com.asus.alwayson.action.ALWAYSON_SETTING_CN");
        intent.setClassName("com.asus.alwayson",
                "com.asus.alwayson.settings.cn.AlwaysOnSettingsActivity");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> list = context.getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        boolean isAvailable = (list != null && list.size() > 0);
        if(isAvailable){
            Log.i(TAG, "CN Always on apk exist");
            return true;
        }else{
            Log.i(TAG, "CN Always on apk dose not exist");
            return false;
        }
    }

    public static boolean supportAlwaysOnFeature(Context context) {
        return context.getPackageManager().hasSystemFeature("asus.hardware.alwayson");
    } //END OF supportAlwaysOnFeature()

    public static boolean showInFirstPage() {
        //
        //  From: Kevin Chiou(邱文顥)
        //  Sent: Thursday, May 04, 2017 5:30 PM
        //  Subject: RE: [ZenUI 4.0]Settings第一層排序調整
        //      請各位將修改上在AMAX_android-7.1.1_r1_dev
        //
        //  From: Kevin Chiou(邱文顥)
        //  Sent: Thursday, May 04, 2017 5:22 PM
        //  Subject: FW: [ZenUI 4.0]Settings第一層排序調整
        //      always on移至：Display > 排序於螢幕保護程式之前
        //
        return false;
    } //END OF showInFirstPage()

    public static boolean showPreference(Context context) {
        //
        //  From: Kevin Chiou(邱文顥)
        //  Sent: Thursday, May 04, 2017 5:22 PM
        //  Subject: FW: [ZenUI 4.0]Settings第一層排序調整
        //      always on移至：Display > 排序於螢幕保護程式之前
        //
        if (AlwaysOnUtils.checkApkExist(context)
                && AlwaysOnUtils.supportAlwaysOnFeature(context)
                && !AlwaysOnUtils.showInFirstPage()) {
            return true;
        }
        else {
            return false;
        }
    } //END OF showPreference()

    public static boolean isEnabled(Context context) {
        boolean support = AlwaysOnUtils.supportAlwaysOnFeature(context);
        int dbValue = Settings.System.getInt(context.getContentResolver(),
                DB_KEY,
                DB_VALUE_UNKNOWN);
        boolean dbEnabled = (dbValue == DB_VALUE_ENABLE);

        return (support && dbEnabled);
    } //END OF isEnabled()

    public static void showSettingsActivity(Context context) {
        Intent intent = new Intent(SETTINGS_ACTION);

        intent.setComponent(SETTINGS_COMPONENT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        }
        catch (Exception e) {
            Log.e(TAG, " ERROR: Failed to start activity!!! (error="
                    + e.getMessage() + ") ...", e);
        }
    } //END OF showSettingsActivity()

    public static void showCNSettingsActivity(Context context) {
        Intent intent = new Intent(CN_SETTINGS_ACTION);

        intent.setComponent(CN_SETTINGS_COMPONENT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        }
        catch (Exception e) {
            Log.e(TAG, " ERROR: Failed to start activity!!! (error="
                    + e.getMessage() + ") ...", e);
        }
    } //END OF showSettingsActivity()
}
