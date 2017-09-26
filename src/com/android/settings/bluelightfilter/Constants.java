package com.android.settings.bluelightfilter;

import java.util.List;

import com.asus.splendidcommandagent.ISplendidCommandAgentService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.Build;

public class Constants {

    public static final int SCREEN_MODE_OPTION_BALANCE = 0;
    public static final int SCREEN_MODE_OPTION_READING = 1;
    public static final String BALACE_MODE_OPTION = "DEF";

    public static final int BLUELIGHT_FILTER_MODE_OFF = -1;
    public static final int BLUELIGHT_FILTER_LEVEL_RDWEAK = 0;   //RdWeak(default)
    public static final int BLUELIGHT_FILTER_LEVEL_RD01 = 1;     //Rd01
    public static final int BLUELIGHT_FILTER_LEVEL_RD02 = 2;     //Rd02
    public static final int BLUELIGHT_FILTER_LEVEL_RD03 = 3;     //Rd03
    public static final int BLUELIGHT_FILTER_LEVEL_RDSTRONG = 4; //RdStrong
    public static final int BLUELIGHT_FILTER_MODE_SEEKBAR_MAX = 4;
    public static final String BLUELIGHT_FILTER_MODE_OPTION = "asus_bluelight_filter_mode_option";
    public static final String BLUELIGHT_FILTER_LEVEL = "asus_bluelight_filter_mode_level"; //0-4

    public static final String ASUS_SPLENDID_SCREEN_MODE_OPTION = "asus_splendid_screen_mode_option";
    public static final String ASUS_SPLENDID_READING_MODE_MAIN_SWITCH    = "asus_splendid_reading_mode_main_switch";
    public static final String EXTRA_QUICKSETTING_READER_MODE_ON_OFF = "asus.splendid.quicksetting.intent.extra.READER_MODE";
    public static final String EXTRA_MODESETTING_CHANGE_MODE             = "asus.splendid.modesetting.intent.extra.CHANGE_MODE"; //int value
    public static final String EXTRA_QUICKSETTING_CHANGE_SCREEN_MODE     = "asus.splendid.quicksetting.intent.extra.SCREEN_MODE"; //int value
    public static final String EXTRA_DO_COMMAND = "asus.do.command";
    public static final String EXTRA_SEND_NOTIFICATION = "asus.splendid.alarmmanager.intent.extra.SEND_NOTIFICATION";
    /**
     * Reading mode Seekbar
     * Seekbar:       0  -  1   -  2   -  3   -  4
     * LUT file:  RdWeak - Rd01 - Rd02 - Rd03 - RdStrong
     */
    public static final int READING_MODE_SEEKBAR_PROGRESS2OPTION[] = {
        BLUELIGHT_FILTER_LEVEL_RDWEAK, BLUELIGHT_FILTER_LEVEL_RD01,
        BLUELIGHT_FILTER_LEVEL_RD02, BLUELIGHT_FILTER_LEVEL_RD03,
        BLUELIGHT_FILTER_LEVEL_RDSTRONG
    };
    public static final String READING_MODE_OPTION2LUT[] = {"RdWeak", "Rd01", "Rd02","Rd03", "RdStrong"};

    //SharedPreference
    public static final String SP_NAME     = "asus.preference.splendid";
    public static final String SP_KEY_BOOT_COMPLETE_COMMAND              = "asus.splendid.boot.complete.command";
    public static final String SP_KEY_NEED_TO_SHOW_NOTIFICATION          = "asus.splendid.need.to.show.notification";

    // PRIVATE_DEBUG is set by developer for locally debugging
    public static final boolean PRIVATE_DEBUG = true;//false;
    public static final boolean DEBUG = Build.TYPE.equals("userdebug");
    public static boolean isPhoneMode(Context context) {
        Configuration rConfig = context.getResources().getConfiguration();
        return (rConfig.smallestScreenWidthDp < 600) ? true : false;
    }

    public static boolean isAndroidL(Context context){
        return (Build.VERSION.SDK_INT >= 21) ? true : false;
    }
    public static boolean isAndroidM(Context context){
        return (Build.VERSION.SDK_INT >= 23) ? true : false;
    }

    public static int getDefaultScreenMode(Context context) {
        return SCREEN_MODE_OPTION_BALANCE;
    }

    public static Intent getExplicitIntent(Context context, String serviceName) {
        PackageManager pm = context.getPackageManager();
        Intent implicitIntent = new Intent(ISplendidCommandAgentService.class.getName());
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(implicitIntent, 0);

        // Is somebody else trying to intercept our call?
        if (resolveInfos == null || resolveInfos.size() != 1) {
            return null;
        }

        ResolveInfo serviceInfo = resolveInfos.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        Intent explicitIntent = new Intent();
        explicitIntent.setComponent(component);
        return explicitIntent;
    }
}
