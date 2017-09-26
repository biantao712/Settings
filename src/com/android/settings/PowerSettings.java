package com.android.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsProto;
import com.android.settings.R;

import android.app.ActivityManager;
import android.os.UserHandle;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;

public class PowerSettings extends SettingsPreferenceFragment
{
    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static final String TAG = "PowerSettings";

    private static final String KEY_ON_AUTOSTART = "on_auto_start";
    private static final String KEY_ON_POWERSAVER = "on_power_saver";
    private static final String KEY_ON_POWERSTATISTICS = "on_power_statistics";


    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------
    private Context mContext;

    private Preference mAutoStartPref;
    private Preference mPowerSaverPref;
    private Preference mPowerStatisticsPref;

    private Intent mAutoStartIntent;

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Logging.logd(TAG, "PowerSavingSettings onCreate");
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        if(Utils.isVerizonSKU() && Utils.isPackageEnabled(mContext,"com.asus.powersaver")){
            Intent intents = new Intent();
            ComponentName comp = new ComponentName("com.asus.powersaver", "com.asus.powersaver.PowerSaverSettings");
            intents.setComponent(comp);
            intents.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intents);
            finish();
        }

        addPreferencesFromResource(com.android.settings.R.xml.power_settings);

        mAutoStartIntent = new Intent();
        String pkg = "com.asus.mobilemanager";
        String clz = ".MainActivity";
        mAutoStartIntent.setComponent(new ComponentName(pkg, pkg + clz));
        mAutoStartIntent.putExtra("showNotice", true);
        PackageManager pm = mContext.getPackageManager();
        boolean appExist = pm.resolveActivity(mAutoStartIntent, PackageManager.MATCH_DEFAULT_ONLY) != null;
        if (appExist) {
            mAutoStartPref = findPreference(KEY_ON_AUTOSTART);
        } else {
            removePreference(KEY_ON_AUTOSTART);
        }

        //Check whether user is Owner or not to decide the existence of PowerSaver preference.
        if (UserHandle.USER_OWNER != ActivityManager.getCurrentUser()) {
            removePreference(KEY_ON_POWERSAVER);
        } else {
            mPowerSaverPref = findPreference(KEY_ON_POWERSAVER);
        }

        TrackerManager.sendEvents(getActivity(), TrackerName.TRACKER_MAIN_ENTRIES, Category.POWER_SETTINGS_ENTRY,
                Action.ENTER_SETTINGS, TrackerManager.DEFAULT_LABEL, TrackerManager.DEFAULT_VALUE);

        //mPowerSaverPref = findPreference(KEY_ON_POWERSAVER);
        //mPowerStatisticsPref = findPreference(KEY_ON_POWERSTATISTICS);

    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        //Logging.logd(TAG, "PowerSavingSettings onPreferenceTreeClick");
        if (Utils.isMonkeyRunning()) {
            return false;
        }

        if (preference == mPowerSaverPref) {
            Intent res = new Intent();
            String mPackage = "com.asus.powersaver";
            String mClass = ".PowerSaverSettings";
            res.setComponent(new ComponentName(mPackage, mPackage + mClass));
            startActivity(res);
        }/*else if (preference == mPowerStatisticsPref) {
            //ashan_yang@asus.com TT300027
            Intent res = new Intent();
            String mPackage = "com.asus.powerstatistics";
            String mClass = ".PowerHistory";
            res.setComponent(new ComponentName(mPackage, mPackage + mClass));
            // JimCC: for N porting, disable it to prevent crash
            //startActivity(res);
            //---
        }*/
        else if (preference == mAutoStartPref) {
            startActivity(mAutoStartIntent);
        }

        return true;
    }

    /**
     * Return value can not be 0 (MetricsEvent.VIEW_UNKNOWN)
     * TODO: add new entry in frameworks
     */
    @Override
    protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MAIN_SETTINGS;
    }
}
