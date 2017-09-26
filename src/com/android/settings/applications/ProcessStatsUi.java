/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.applications;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.TimeUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.app.procstats.ProcessStats;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.applications.ProcStatsData.MemInfo;
import com.android.settings.twinApps.TwinAppsUtil;

import com.asus.cncommonres.AsusButtonBar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessStatsUi extends ProcessStatsBase {
    static final String TAG = "ProcessStatsUi";
    static final boolean DEBUG = false;

    private static final String KEY_APP_LIST = "app_list";

    private static final int MENU_SHOW_AVG = Menu.FIRST;
    private static final int MENU_SHOW_MAX = Menu.FIRST + 1;

//    private PreferenceGroup mAppListGroup;
    private PackageManager mPm;

    private boolean mShowMax;
    private MenuItem mMenuAvg;
    private MenuItem mMenuMax;
    //[TwinApps] {
    private boolean mEnableTwinApps = false;
    private int mTwinAppsId = -1;
    //[TwinApps] }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPm = getActivity().getPackageManager();

        addPreferencesFromResource(R.xml.process_stats_ui);
        //[TwinApps] {
        mEnableTwinApps = TwinAppsUtil.isTwinAppsSupport(getActivity());
        if(mEnableTwinApps) {
            UserManager userManager = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
            mTwinAppsId = userManager.getTwinAppsId();
        }
        //[TwinApps] }
//        mAppListGroup = (PreferenceGroup) findPreference(KEY_APP_LIST);
        setHasOptionsMenu(false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initButtonBar();
    }
/*    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenuAvg = menu.add(0, MENU_SHOW_AVG, 0, R.string.sort_avg_use);
        mMenuMax = menu.add(0, MENU_SHOW_MAX, 0, R.string.sort_max_use);
//        updateMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SHOW_AVG:
            case MENU_SHOW_MAX:
                mShowMax = !mShowMax;
                refreshUi();
                updateMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMenu() {
        mMenuMax.setVisible(!mShowMax);
        mMenuAvg.setVisible(mShowMax);
    }*/

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_PROCESS_STATS_UI;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (!(preference instanceof ProcessStatsPreference)) {
            return false;
        }
        ProcessStatsPreference pgp = (ProcessStatsPreference) preference;
        MemInfo memInfo = mStatsManager.getMemInfo();
        launchMemoryDetail((SettingsActivity) getActivity(), memInfo, pgp.getEntry(), true);

        return super.onPreferenceTreeClick(preference);
    }

    /**
     * All states in which we consider a process to be actively running (rather than
     * something that can be freely killed to reclaim RAM).  Note this also includes
     * the HOME state, because we prioritize home over all cached processes even when
     * it is in the background, so it is effectively always running from the perspective
     * of the information we want to show the user here.
     */
    public static final int[] BACKGROUND_AND_SYSTEM_PROC_STATES = new int[] {
            ProcessStats.STATE_PERSISTENT, ProcessStats.STATE_IMPORTANT_FOREGROUND,
            ProcessStats.STATE_IMPORTANT_BACKGROUND, ProcessStats.STATE_BACKUP,
            ProcessStats.STATE_HEAVY_WEIGHT, ProcessStats.STATE_SERVICE,
            ProcessStats.STATE_SERVICE_RESTARTING, ProcessStats.STATE_RECEIVER,
            ProcessStats.STATE_HOME
    };

    public static final int[] FOREGROUND_PROC_STATES = new int[] {
            ProcessStats.STATE_TOP
    };

    public static final int[] CACHED_PROC_STATES = new int[] {
            ProcessStats.STATE_CACHED_ACTIVITY, ProcessStats.STATE_CACHED_ACTIVITY_CLIENT,
            ProcessStats.STATE_CACHED_EMPTY
    };

    public static String makeDuration(long time) {
        StringBuilder sb = new StringBuilder(32);
        TimeUtils.formatDuration(time, sb);
        return sb.toString();
    }

    @Override
    public void refreshUi() {

        getPreferenceScreen().removeAll();
        getPreferenceScreen().setOrderingAsAdded(false);
/*        mAppListGroup.removeAll();
        mAppListGroup.setOrderingAsAdded(false);
        mAppListGroup.setTitle(mShowMax ? R.string.maximum_memory_use
                : R.string.average_memory_use);*/

        final Context context = getActivity();
        MemInfo memInfo = mStatsManager.getMemInfo();

        List<ProcStatsPackageEntry> pkgEntries = mStatsManager.getEntries();

        // Update everything and get the absolute maximum of memory usage for scaling.
        for (int i=0, N=pkgEntries.size(); i<N; i++) {
            ProcStatsPackageEntry pkg = pkgEntries.get(i);
            pkg.updateMetrics();
        }

        Collections.sort(pkgEntries, mShowMax ? sMaxPackageEntryCompare : sPackageEntryCompare);

        // Now collect the per-process information into applications, so that applications
        // running as multiple processes will have only one entry representing all of them.

        if (DEBUG) Log.d(TAG, "-------------------- BUILDING UI");

        double maxMemory = mShowMax ? memInfo.realTotalRam
                : memInfo.usedWeight * memInfo.weightToRam;
        for (int i = 0; i < pkgEntries.size(); i++) {
            ProcStatsPackageEntry pkg = pkgEntries.get(i);
            ProcessStatsPreference pref = new ProcessStatsPreference(getPrefContext());
            //[TwinApps] {
            if(mEnableTwinApps){
                pkg.retrieveUiDataAsUser(context, mPm);
                if (pkg.mUiTargetApp == null) continue;
                if (pkg.mUiTargetApp.packageName.equals("com.asus.twinapps")) {
                    pkgEntries.remove(i);
                    i--;
                    continue;
                }
                if (mTwinAppsId != -1) {
                    if (UserHandle.getUserId(pkg.mUiTargetApp.uid) == mTwinAppsId) {
                        if ((pkg.mUiTargetApp.isSystemApp() || pkg.mUiTargetApp.isUpdatedSystemApp())
                            /*&& !entry.hasLauncherEntry*/) {
                            pkgEntries.remove(i);
                            i--;
                            continue;
                        } else if (("com.android.settings").equals(pkg.mUiTargetApp.packageName)) {
                            pkgEntries.remove(i);
                            i--;
                            continue;
                        }
                    }
                }
            } else {
            //[TwinApps] }
                pkg.retrieveUiData(context, mPm);
            //[TwinApps] {
            }
            //[TwinApps] }
            pref.init(pkg, mPm, maxMemory, memInfo.weightToRam,
                    memInfo.totalScale, !mShowMax);
            pref.setOrder(i);
//            mAppListGroup.addPreference(pref);
            getPreferenceScreen().addPreference(pref);
        }
    }

    final static Comparator<ProcStatsPackageEntry> sPackageEntryCompare
            = new Comparator<ProcStatsPackageEntry>() {
        @Override
        public int compare(ProcStatsPackageEntry lhs, ProcStatsPackageEntry rhs) {
            double rhsWeight = Math.max(rhs.mRunWeight, rhs.mBgWeight);
            double lhsWeight = Math.max(lhs.mRunWeight, lhs.mBgWeight);
            if (lhsWeight == rhsWeight) {
                return 0;
            }
            return lhsWeight < rhsWeight ? 1 : -1;
        }
    };

    final static Comparator<ProcStatsPackageEntry> sMaxPackageEntryCompare
            = new Comparator<ProcStatsPackageEntry>() {
        @Override
        public int compare(ProcStatsPackageEntry lhs, ProcStatsPackageEntry rhs) {
            double rhsMax = Math.max(rhs.mMaxBgMem, rhs.mMaxRunMem);
            double lhsMax = Math.max(lhs.mMaxBgMem, lhs.mMaxRunMem);
            if (lhsMax == rhsMax) {
                return 0;
            }
            return lhsMax < rhsMax ? 1 : -1;
        }
    };

    private void initButtonBar(){

        AsusButtonBar buttonBar = ((SettingsActivity)getActivity()).getButtonBar();
        if(buttonBar != null) {
            buttonBar.setVisibility(View.VISIBLE);
            buttonBar.addButton(1, R.drawable.cn_application_sort_button,
                    getActivity().getResources().getString(R.string.order_text));
            buttonBar.getButton(1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setDialog();
                }
            });

        }else{
	    Log.d("blenda","asusButtonBar is null");
	}
    }

    private void setDialog(){
        AlertDialog.Builder alertDialog_single_list_item = new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(new String[] { getActivity().getResources().getString(R.string.sort_avg_use),
                                getActivity().getResources().getString(R.string.sort_max_use)},
                        mShowMax?1:0, buttonOnClick)
                .setTitle(R.string.order_text)
                .setNegativeButton(R.string.cancel, null);
        AlertDialog orderDialog = alertDialog_single_list_item.show();

        Window dialogWindow = orderDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        dialogWindow.setAttributes(lp);
    }

    private ButtonOnClick buttonOnClick = new ButtonOnClick(1);
    private class ButtonOnClick implements DialogInterface.OnClickListener {
        private int index;

        public ButtonOnClick(int index) {
            this.index = index;
        }

        @Override
        public void onClick(DialogInterface dialog,int which) {
            if (which >= 0) {
                index = which;
                if (which == 0){
                    mShowMax = false;
                }else{
                    mShowMax = true;
                }
                refreshUi();
                dialog.dismiss();
            }
        }
    }
}
