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

package com.android.settings.fuelgauge;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v14.preference.SwitchPreference;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.PowerProfile;
import com.android.settings.R;
import com.android.settings.Settings.HighPowerApplicationsActivity;
import com.android.settings.SettingsActivity;
import com.android.settings.analytic.AnalyticUtils;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.applications.ManageApplications;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.BatteryInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager;
import android.content.ContentResolver;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import com.android.settings.Utils;

/**
 * Displays a list of apps and subsystems that consume power, ordered by how much power was
 * consumed since the last time it was unplugged.
 */
public class PowerUsageSummary extends PowerUsageBase implements
       Preference.OnPreferenceChangeListener {

    private static final boolean DEBUG = false;

    private static final boolean USE_FAKE_DATA = false;

    static final String TAG = "PowerUsageSummary";

    private static final String KEY_APP_LIST = "app_list";
    private static final String KEY_BATTERY_HISTORY = "battery_history";
    private static final String KEY_DETECT_DRAIN_APPS = "detect_drain_apps"; // harrison, battery draining apps
    private static final int MENU_STATS_TYPE = Menu.FIRST;
	private static final int MENU_BATTERY_SAVER = Menu.FIRST + 2;
    private static final int MENU_HIGH_POWER_APPS = Menu.FIRST + 3;
    private static final int MENU_HELP = Menu.FIRST + 4;
    private static final int MENU_SHOW_BATTERY_LEVEL = Menu.FIRST + 5; //wilson_chen@asus.com
    private static final int MENU_DETECT_DRAIN_APPS = Menu.FIRST + 6; //wilson_chen@asus.com

    private BatteryHistoryPreference mHistPref;
    private PreferenceGroup mAppListGroup;
    private CheckBoxPreference mDetectDrainApps; // harrison, battery draining apps
    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;

    private static final int MIN_POWER_THRESHOLD_MILLI_AMP = 5;
    private static final int MAX_ITEMS_TO_LIST = USE_FAKE_DATA ? 30 : 10;
    private static final int MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP = 10;
    private static final int SECONDS_IN_HOUR = 60 * 60;
    
    private static final String POWER_SAVER_PACKAGE = "com.asus.cnmobilemanager";
    private static final String POWER_SAVER_CLASS = "com.asus.cnmobilemanager.powersaver.PowerSaverSettings";
    private static final String AUTO_START_CLASS = "com.asus.cnmobilemanager.MainActivity";

    //+++ blenda
    private static final String KEY_ON_AUTOSTART = "on_auto_start";
    private static final String KEY_ON_POWERSAVER = "on_power_saver";
    private static final String KEY_ON_POWERSTATISTICS = "on_power_statistics";
    //tim++
    private static final String KEY_BATTERY_DETAILS = "battery_details";
    private static final String KEY_HIGH_POWER_APPS = "high_power_apps";
    private static final String KEY_SHOW_BATTERY_ENABLE = "show_battery_enable";
    //tim--

    private Context mContext;
    private Preference mPowerSaverPref;
    private Preference mPowerStatisticsPref;
    private Preference mAutoStartPref;

    private Intent mAutoStartIntent;
    //--- blenda
    
    private SwitchPreference showBatteryEnable;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setAnimationAllowed(true);

        addPreferencesFromResource(R.xml.power_usage_summary);
        mHistPref = (BatteryHistoryPreference) findPreference(KEY_BATTERY_HISTORY);
        removePreference(KEY_BATTERY_HISTORY);
//        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        removePreference(KEY_APP_LIST);

        boolean isCnMobileManagerExist = isCnMobileManagerExist();
        //+++ blenda
        mContext = getActivity();
        mAutoStartIntent = new Intent();
        String pkg = isCnMobileManagerExist? POWER_SAVER_PACKAGE : "com.asus.mobilemanager";
        String clz = isCnMobileManagerExist? AUTO_START_CLASS : "com.asus.mobilemanager.MainActivity";
        mAutoStartIntent.setComponent(new ComponentName(pkg, clz));
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

        TrackerManager.sendEvents(getActivity(), TrackerManager.TrackerName.TRACKER_MAIN_ENTRIES, AnalyticUtils.Category.POWER_SETTINGS_ENTRY,
                AnalyticUtils.Action.ENTER_SETTINGS, TrackerManager.DEFAULT_LABEL, TrackerManager.DEFAULT_VALUE);
        //--- blenda
        
        //tim++
        showBatteryEnable = (SwitchPreference) getPreferenceScreen().findPreference(KEY_SHOW_BATTERY_ENABLE);
        showBatteryEnable.setChecked(Settings.System.getInt(getActivity().getContentResolver(), "show_battery", 1) != 0);
        showBatteryEnable.setOnPreferenceChangeListener(this);
        //tim--
    }
    
    private boolean isCnMobileManagerExist(){
    	Intent intent = new Intent();
        intent.setComponent(new ComponentName(POWER_SAVER_PACKAGE, POWER_SAVER_CLASS));
        PackageManager pm = getActivity().getPackageManager();
        boolean appExist = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null;
        return appExist;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FUELGAUGE_POWER_USAGE_SUMMARY;
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }

    @Override
    public void onPause() {
        BatteryEntry.stopRequestQueue();
        mHandler.removeMessages(BatteryEntry.MSG_UPDATE_NAME_ICON);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            BatteryEntry.clearUidCache();
        }
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
    	Log.d("timhu", "onPreferenceChange preference = " + preference);
        String key = preference.getKey();
        if(KEY_SHOW_BATTERY_ENABLE.equals(key)){
        	boolean enable = (boolean) newValue;
            Settings.System.putInt(getActivity().getContentResolver(), "show_battery",
            		enable ? 1 : 0);
        }
        
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
//    	String key = preference.getKey();
//    	if(KEY_HIGH_POWER_APPS.equals(key)){
//    		final SettingsActivity sa = (SettingsActivity) getActivity();
//    		Bundle args = new Bundle();
//            args.putString(ManageApplications.EXTRA_CLASSNAME,
//                    HighPowerApplicationsActivity.class.getName());
//            sa.startPreferencePanel(ManageApplications.class.getName(), args,
//                    R.string.high_power_apps, null, null, 0);
//            return true;
//    	}
    	
        //+++ blenda
        if (preference == mPowerSaverPref || preference == mAutoStartPref) {
            if (Utils.isMonkeyRunning()) {
                return false;
            }
            
            if (preference == mPowerSaverPref) {
            	boolean isCnMobileManagerExist = isCnMobileManagerExist();
            	
                Intent res = new Intent();
                String mPackage = isCnMobileManagerExist? POWER_SAVER_PACKAGE : "com.asus.powersaver";
                String mClass = isCnMobileManagerExist? POWER_SAVER_CLASS : "com.asus.powersaver.PowerSaverSettings";
                res.setComponent(new ComponentName(mPackage, mClass));
                
                try {
	                startActivity(res);
                } catch (ActivityNotFoundException e) {
    				Log.e(TAG,"powersaver activity not found!");
    			}
            } else if (preference == mAutoStartPref) {
                startActivity(mAutoStartIntent);
            }
            return true;
        }
        //--- blenda

        if (!(preference instanceof PowerGaugePreference)) {
            return super.onPreferenceTreeClick(preference);
        }
        PowerGaugePreference pgp = (PowerGaugePreference) preference;
        BatteryEntry entry = pgp.getInfo();
        PowerUsageDetail.startBatteryDetailPage((SettingsActivity) getActivity(), mStatsHelper,
                mStatsType, entry, true, true);
        return super.onPreferenceTreeClick(preference);
    }

   private Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

//    @Override
//    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        if (DEBUG) {
//            menu.add(0, MENU_STATS_TYPE, 0, R.string.menu_stats_total)
//                    .setIcon(com.android.internal.R.drawable.ic_menu_info_details)
//                    .setAlphabeticShortcut('t');
//        }
//
//        // +++ battery_level & drain_apps wilson_chen@asus.com
//        ContentResolver resolver = getActivity().getContentResolver();
//
//        MenuItem showBatteryLevel = menu.add(0, MENU_SHOW_BATTERY_LEVEL, 0, R.string.show_battery)
//                .setCheckable(true);
//        showBatteryLevel.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//        showBatteryLevel.setChecked(Settings.System.getInt(resolver, "show_battery", 0) != 0);
//
//        String sku = SystemProperties.get("ro.product.name", "");
//        MenuItem detectDrainApps = menu.add(0, MENU_DETECT_DRAIN_APPS, 0,
//                R.string.detect_drain_apps).setCheckable(true);
//        detectDrainApps.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//        if (!sku.toLowerCase().startsWith("vzw_") && !Utils.isATT() && UserHandle.USER_OWNER == ActivityManager.getCurrentUser()) {
//            detectDrainApps.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//            detectDrainApps.setChecked(Settings.System.getInt(resolver,
//                    Settings.System.DETECT_DRAIN_APPS, 1) != 0);
//        }
//        else {
//            detectDrainApps.setVisible(false);
//        }
//        // ---
//
//        MenuItem batterySaver = menu.add(0, MENU_BATTERY_SAVER, 0, R.string.powersaver_app_name);
//        batterySaver.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//        //BEGIN : roy_huang@asus.com
//        if (UserHandle.USER_OWNER == ActivityManager.getCurrentUser()) {
//            batterySaver.setVisible(true);
//        } else {
//            batterySaver.setVisible(false);
//        }
//        //END : roy_huang@asus.com
//
//
//        menu.add(0, MENU_HIGH_POWER_APPS, 0, R.string.high_power_apps);
//        super.onCreateOptionsMenu(menu, inflater);
//    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_battery;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//		final SettingsActivity sa = (SettingsActivity) getActivity();
//        switch (item.getItemId()) {
//			 // +++ wilson_chen@asus.com
//            case MENU_SHOW_BATTERY_LEVEL:
//                item.setChecked(!item.isChecked());
//                Settings.System.putInt(getActivity().getContentResolver(), "show_battery",
//                        item.isChecked() ? 1 : 0);
//                return true;
//            case MENU_DETECT_DRAIN_APPS:
//                item.setChecked(!item.isChecked());
//                Settings.System.putInt(getActivity().getContentResolver(),
//                        Settings.System.DETECT_DRAIN_APPS, item.isChecked() ? 1 : 0);
//                Intent intent = new Intent("com.asus.powersaver.detectdrainapps");
//                Intent explicit = createExplicitFromImplicitIntent(getActivity(), intent);
//                if (item.isChecked()) {
//                    getActivity().startService(explicit);
//                }
//                else {
//                    getActivity().stopService(explicit);
//                }
//                return true;
//                // ---
//            case MENU_STATS_TYPE:
//                if (mStatsType == BatteryStats.STATS_SINCE_CHARGED) {
//                    mStatsType = BatteryStats.STATS_SINCE_UNPLUGGED;
//                } else {
//                    mStatsType = BatteryStats.STATS_SINCE_CHARGED;
//                }
//                refreshStats();
//                return true;
//            case MENU_BATTERY_SAVER:
//			    startActivity(new Intent().setClassName("com.asus.powersaver", "com.asus.powersaver.PowerSaverSettings"));
//                return true;
//            case MENU_HIGH_POWER_APPS:
//                Bundle args = new Bundle();
//                args.putString(ManageApplications.EXTRA_CLASSNAME,
//                        HighPowerApplicationsActivity.class.getName());
//                sa.startPreferencePanel(ManageApplications.class.getName(), args,
//                        R.string.high_power_apps, null, null, 0);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    private void addNotAvailableMessage() {
        final String NOT_AVAILABLE = "not_available";
        Preference notAvailable = getCachedPreference(NOT_AVAILABLE);
        if (notAvailable == null) {
            notAvailable = new Preference(getPrefContext());
            notAvailable.setKey(NOT_AVAILABLE);
            notAvailable.setTitle(R.string.power_usage_not_available);
            mAppListGroup.addPreference(notAvailable);
        }
    }

    private static boolean isSharedGid(int uid) {
        return UserHandle.getAppIdFromSharedAppGid(uid) > 0;
    }

    private static boolean isSystemUid(int uid) {
        return uid >= Process.SYSTEM_UID && uid < Process.FIRST_APPLICATION_UID;
    }

    /**
     * We want to coalesce some UIDs. For example, dex2oat runs under a shared gid that
     * exists for all users of the same app. We detect this case and merge the power use
     * for dex2oat to the device OWNER's use of the app.
     * @return A sorted list of apps using power.
     */
    private static List<BatterySipper> getCoalescedUsageList(final List<BatterySipper> sippers) {
        final SparseArray<BatterySipper> uidList = new SparseArray<>();

        final ArrayList<BatterySipper> results = new ArrayList<>();
        final int numSippers = sippers.size();
        for (int i = 0; i < numSippers; i++) {
            BatterySipper sipper = sippers.get(i);
            if (sipper.getUid() > 0) {
                int realUid = sipper.getUid();

                // Check if this UID is a shared GID. If so, we combine it with the OWNER's
                // actual app UID.
                if (isSharedGid(sipper.getUid())) {
                    realUid = UserHandle.getUid(UserHandle.USER_SYSTEM,
                            UserHandle.getAppIdFromSharedAppGid(sipper.getUid()));
                }

                // Check if this UID is a system UID (mediaserver, logd, nfc, drm, etc).
                if (isSystemUid(realUid)
                        && !"mediaserver".equals(sipper.packageWithHighestDrain)) {
                    // Use the system UID for all UIDs running in their own sandbox that
                    // are not apps. We exclude mediaserver because we already are expected to
                    // report that as a separate item.
                    realUid = Process.SYSTEM_UID;
                }

                if (realUid != sipper.getUid()) {
                    // Replace the BatterySipper with a new one with the real UID set.
                    BatterySipper newSipper = new BatterySipper(sipper.drainType,
                            new FakeUid(realUid), 0.0);
                    newSipper.add(sipper);
                    newSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    newSipper.mPackages = sipper.mPackages;
                    sipper = newSipper;
                }

                int index = uidList.indexOfKey(realUid);
                if (index < 0) {
                    // New entry.
                    uidList.put(realUid, sipper);
                } else {
                    // Combine BatterySippers if we already have one with this UID.
                    final BatterySipper existingSipper = uidList.valueAt(index);
                    existingSipper.add(sipper);
                    if (existingSipper.packageWithHighestDrain == null
                            && sipper.packageWithHighestDrain != null) {
                        existingSipper.packageWithHighestDrain = sipper.packageWithHighestDrain;
                    }

                    final int existingPackageLen = existingSipper.mPackages != null ?
                            existingSipper.mPackages.length : 0;
                    final int newPackageLen = sipper.mPackages != null ?
                            sipper.mPackages.length : 0;
                    if (newPackageLen > 0) {
                        String[] newPackages = new String[existingPackageLen + newPackageLen];
                        if (existingPackageLen > 0) {
                            System.arraycopy(existingSipper.mPackages, 0, newPackages, 0,
                                    existingPackageLen);
                        }
                        System.arraycopy(sipper.mPackages, 0, newPackages, existingPackageLen,
                                newPackageLen);
                        existingSipper.mPackages = newPackages;
                    }
                }
            } else {
                results.add(sipper);
            }
        }

        final int numUidSippers = uidList.size();
        for (int i = 0; i < numUidSippers; i++) {
            results.add(uidList.valueAt(i));
        }

        // The sort order must have changed, so re-sort based on total power use.
        Collections.sort(results, new Comparator<BatterySipper>() {
            @Override
            public int compare(BatterySipper a, BatterySipper b) {
                return Double.compare(b.totalPowerMah, a.totalPowerMah);
            }
        });
        return results;
    }

    protected void refreshStats() {
        super.refreshStats();
        updatePreference(mHistPref);
        
//        cacheRemoveAllPrefs(mAppListGroup);
//        mAppListGroup.setOrderingAsAdded(false);
//        boolean addedSome = false;
//
//        final PowerProfile powerProfile = mStatsHelper.getPowerProfile();
//        final BatteryStats stats = mStatsHelper.getStats();
//        final double averagePower = powerProfile.getAveragePower(PowerProfile.POWER_SCREEN_FULL);
//
//        /* ignore original theme and color style of aosp. wilson_chen@asus.com
//        TypedValue value = new TypedValue();
//        getContext().getTheme().resolveAttribute(android.R.attr.colorControlNormal, value, true);
//        int colorControl = getContext().getColor(value.resourceId);
//        */
//
//        if (averagePower >= MIN_AVERAGE_POWER_THRESHOLD_MILLI_AMP || USE_FAKE_DATA) {
//            final List<BatterySipper> usageList = getCoalescedUsageList(
//                    USE_FAKE_DATA ? getFakeStats() : mStatsHelper.getUsageList());
//
//            final int dischargeAmount = USE_FAKE_DATA ? 5000
//                    : stats != null ? stats.getDischargeAmount(mStatsType) : 0;
//            final int numSippers = usageList.size();
//            for (int i = 0; i < numSippers; i++) {
//                final BatterySipper sipper = usageList.get(i);
//                if ((sipper.totalPowerMah * SECONDS_IN_HOUR) < MIN_POWER_THRESHOLD_MILLI_AMP) {
//                    continue;
//                }
//                double totalPower = USE_FAKE_DATA ? 4000 : mStatsHelper.getTotalPower();
//                final double percentOfTotal =
//                        ((sipper.totalPowerMah / totalPower) * dischargeAmount);
//                if (((int) (percentOfTotal + .5)) < 1) {
//                    continue;
//                }
//                if (sipper.drainType == BatterySipper.DrainType.OVERCOUNTED) {
//                    // Don't show over-counted unless it is at least 2/3 the size of
//                    // the largest real entry, and its percent of total is more significant
//                    if (sipper.totalPowerMah < ((mStatsHelper.getMaxRealPower()*2)/3)) {
//                        continue;
//                    }
//                    if (percentOfTotal < 10) {
//                        continue;
//                    }
//                    if ("user".equals(Build.TYPE)) {
//                        continue;
//                    }
//                }
//                if (sipper.drainType == BatterySipper.DrainType.UNACCOUNTED) {
//                    // Don't show over-counted unless it is at least 1/2 the size of
//                    // the largest real entry, and its percent of total is more significant
//                    if (sipper.totalPowerMah < (mStatsHelper.getMaxRealPower()/2)) {
//                        continue;
//                    }
//                    if (percentOfTotal < 5) {
//                        continue;
//                    }
//                    if ("user".equals(Build.TYPE)) {
//                        continue;
//                    }
//                }
//                final UserHandle userHandle = new UserHandle(UserHandle.getUserId(sipper.getUid()));
//                final BatteryEntry entry = new BatteryEntry(getActivity(), mHandler, mUm, sipper);
//                final Drawable badgedIcon = mUm.getBadgedIconForUser(entry.getIcon(),
//                        userHandle);
//                final CharSequence contentDescription = mUm.getBadgedLabelForUser(entry.getLabel(),
//                        userHandle);
//                final String key = sipper.drainType == DrainType.APP ? sipper.getPackages() != null
//                        ? TextUtils.concat(sipper.getPackages()).toString()
//                        : String.valueOf(sipper.getUid())
//                        : sipper.drainType.toString();
//                PowerGaugePreference pref = (PowerGaugePreference) getCachedPreference(key);
//                if (pref == null) {
//                    pref = new PowerGaugePreference(getPrefContext(), badgedIcon,
//                            contentDescription, entry);
//                    pref.setKey(key);
//                }
//
//                final double percentOfMax = (sipper.totalPowerMah * 100)
//                        / mStatsHelper.getMaxPower();
//                sipper.percent = percentOfTotal;
//                pref.setTitle(entry.getLabel());
//                pref.setOrder(i + 1);
//                pref.setPercent(percentOfMax, percentOfTotal);
//                if (sipper.uidObj != null) {
//                    pref.setKey(Integer.toString(sipper.uidObj.getUid()));
//                }
//                if ((sipper.drainType != DrainType.APP || sipper.uidObj.getUid() == 0)
//                         && sipper.drainType != DrainType.USER) {
//                    pref.setTint(getContext().getColor(android.R.color.black));
//                }
//                addedSome = true;
//                mAppListGroup.addPreference(pref);
//                if (mAppListGroup.getPreferenceCount() - getCachedCount()
//                        > (MAX_ITEMS_TO_LIST + 1)) {
//                    break;
//                }
//            }
//        }
//        if (!addedSome) {
//            addNotAvailableMessage();
//        }
//        removeCachedPrefs(mAppListGroup);

        BatteryEntry.startRequestQueue();
    }

    private static List<BatterySipper> getFakeStats() {
        ArrayList<BatterySipper> stats = new ArrayList<>();
        float use = 5;
        for (DrainType type : DrainType.values()) {
            if (type == DrainType.APP) {
                continue;
            }
            stats.add(new BatterySipper(type, null, use));
            use += 5;
        }
        for (int i = 0; i < 100; i++) {
            stats.add(new BatterySipper(DrainType.APP,
                    new FakeUid(Process.FIRST_APPLICATION_UID + i), use));
        }
        stats.add(new BatterySipper(DrainType.APP,
                new FakeUid(0), use));

        // Simulate dex2oat process.
        BatterySipper sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.FIRST_APPLICATION_UID)), 10.0f);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);

        sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.FIRST_APPLICATION_UID + 1)), 10.0f);
        sipper.packageWithHighestDrain = "dex2oat";
        stats.add(sipper);

        sipper = new BatterySipper(DrainType.APP,
                new FakeUid(UserHandle.getSharedAppGid(Process.LOG_UID)), 9.0f);
        stats.add(sipper);

        return stats;
    }

    Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BatteryEntry.MSG_UPDATE_NAME_ICON:
                    BatteryEntry entry = (BatteryEntry) msg.obj;
                    PowerGaugePreference pgp =
                            (PowerGaugePreference) findPreference(
                                    Integer.toString(entry.sipper.uidObj.getUid()));
                    if (pgp != null) {
                        final int userId = UserHandle.getUserId(entry.sipper.getUid());
                        final UserHandle userHandle = new UserHandle(userId);
                        pgp.setIcon(mUm.getBadgedIconForUser(entry.getIcon(), userHandle));
                        pgp.setTitle(entry.name);
                        if (entry.sipper.drainType == DrainType.APP) {
                            pgp.setContentDescription(entry.name);
                        }
                    }
                    break;
                case BatteryEntry.MSG_REPORT_FULLY_DRAWN:
                    Activity activity = getActivity();
                    if (activity != null) {
                        activity.reportFullyDrawn();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                // TODO: Listen.
                BatteryInfo.getBatteryInfo(mContext, new BatteryInfo.Callback() {
                    @Override
                    public void onBatteryInfoLoaded(BatteryInfo info) {
                        mLoader.setSummary(SummaryProvider.this, info.mChargeLabelString);
                    }
                });
            }
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
}
