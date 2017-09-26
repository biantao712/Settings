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

package com.android.settings.notification;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.*;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.AppInfoBase;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.UserManager;
import com.android.settingslib.Utils;
import android.os.UserHandle;
import android.app.ActivityThread;
import com.android.settings.twinApps.TwinAppsUtil;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.RemoteException;
import java.util.List;

public class CNAppNotifySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "CNAppNotifySettings";
    private PreferenceCategory mNotifyAllowedCategory = null;
    private PreferenceCategory mNotifyStyleCategory = null;
    private PreferenceCategory mNotifyPromptCategory = null;

    private SwitchPreference mNotifyAllowed;
    private SwitchPreference mNotifyStyleImportance;
    private SwitchPreference mNotifyStyleStatusBar;
    private SwitchPreference mNotifyStyleIconSum;
    private SwitchPreference mNotifyStyleHeadsUp;
    private SwitchPreference mNotifyStyleLockScreen;
    private SwitchPreference mNotifyStyleLockScreenHideNotification;
    private SwitchPreference mNotifyAllowDisturb;

    private SwitchPreference mNotifyRing;
    private SwitchPreference mNotifyShock;

    private final NotificationBackend mBackend = new NotificationBackend();

    private static final String KYE_NITIFY_ALLOWED_CATEGORY = "cn_notify_allow_category";
    private static final String KYE_NITIFY_STYLE_CATEGORY = "cn_notify_style_category";
    private static final String KYE_NITIFY_PROMPT_CATEGORY = "cn_notify_prompt_category";

    private static final String KEY_NOTIFY_ALLOWED = "notify_allow";
    private static final String KEY_NOTIFY_STYLE_IMPORTANCE="notify_importance";
    private static final String KEY_NOTIFY_STYLE_STAUSBAR="notify_status_bar";
    private static final String KEY_NOTIFY_STYLE_ICONSUM="notify_iconsum";
    private static final String KEY_NOTIFY_STYLE_HEADSUP="notify_headsup";
    private static final String KEY_NOTIFY_STYLE_LOCKSCREEN="notify_lockscreen";
    private static final String KEY_NOTIFY_STYLE_LOCKSCREENHIDENOTIFICATION="notify_lockscreen_hide_notification";

    private static final String KEY_NOTIFY_RING = "notify_ring";
    private static final String KEY_NOTIFY_SHOCK = "notify_shock";
    private static final String KEY_NOTIFY_ALLOWED_DISTURB = "notify_allow_disturb";
    private String mPkg;
    private int mUid;

    private static final int NOTIFY_TYPE_STATUS_BAR = 0;
    private static final int NOTIFY_TYPE_SHOCK = 1;
    private static final int NOTIFY_TYPE_RING = 2;
    String[] mAppWhiteListArray1;
    String[] mAppWhiteListArray2;

    private PackageManager mPm;
    private PackageInfo mPkgInfo;
    private boolean mEnableTwinApps;
    private UserManager mUm;
    private Context mContext;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.WIRELESS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*init env*/
        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        if (intent == null && args == null) {
            Log.w(TAG, "No intent");
            return;
        }

        mAppWhiteListArray1 = getResources().getStringArray(R.array.cn_notification_management_hide_allowed_switch_white_list);
        mAppWhiteListArray2 = getResources().getStringArray(R.array.cn_notification_management_hide_three_switch_white_list);

        mPkg = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_NAME)? args.getString(AppInfoBase.ARG_PACKAGE_NAME): intent.getStringExtra(Settings.EXTRA_APP_PACKAGE);
        mUid = args != null && args.containsKey(AppInfoBase.ARG_PACKAGE_UID)? args.getInt(AppInfoBase.ARG_PACKAGE_UID): intent.getIntExtra(Settings.EXTRA_APP_UID, -1);

        addPreferencesFromResource(R.xml.cn_app_notification_settings);
        // get all categories first.
        mNotifyAllowedCategory = (PreferenceCategory)findPreference(KYE_NITIFY_ALLOWED_CATEGORY);
        mNotifyStyleCategory = (PreferenceCategory)findPreference(KYE_NITIFY_STYLE_CATEGORY);
        mNotifyPromptCategory = (PreferenceCategory)findPreference(KYE_NITIFY_PROMPT_CATEGORY);

        // get mNotifyAllowedCategory switch.
        mNotifyAllowed = (SwitchPreference)findPreference(KEY_NOTIFY_ALLOWED);

        // get mNotifyStyleCategory switches.
        mNotifyStyleImportance=(SwitchPreference)findPreference(KEY_NOTIFY_STYLE_IMPORTANCE);
        mNotifyStyleStatusBar = (SwitchPreference)findPreference(KEY_NOTIFY_STYLE_STAUSBAR);
        mNotifyStyleIconSum=(SwitchPreference)findPreference(KEY_NOTIFY_STYLE_ICONSUM);
        mNotifyStyleHeadsUp=(SwitchPreference)findPreference(KEY_NOTIFY_STYLE_HEADSUP);
        mNotifyStyleLockScreen=(SwitchPreference)findPreference(KEY_NOTIFY_STYLE_LOCKSCREEN);
        mNotifyStyleLockScreenHideNotification = (SwitchPreference)findPreference(KEY_NOTIFY_STYLE_LOCKSCREENHIDENOTIFICATION);
        mNotifyAllowDisturb = (SwitchPreference)findPreference(KEY_NOTIFY_ALLOWED_DISTURB);
        // force remove this preference for new feature.
        mNotifyStyleCategory.removePreference(mNotifyStyleLockScreenHideNotification);
        mNotifyStyleCategory.removePreference(mNotifyStyleIconSum);

        // get mNitofyPromptCategory
        mNotifyRing = (SwitchPreference)findPreference(KEY_NOTIFY_RING);
        mNotifyShock = (SwitchPreference)findPreference(KEY_NOTIFY_SHOCK);

        // whether allow switch is on.
        if(mBackend.getAllowed(mPkg,mUid)!= 1)
        {
            setStylePromptEnable(false);
        }
        //set SwitchPreference listener
        mNotifyAllowed.setChecked(mBackend.getAllowed(mPkg,mUid)==1?true:false);
        mNotifyAllowed.setOnPreferenceChangeListener(this);

        mNotifyStyleImportance.setChecked(mBackend.getStyleImportance(mPkg,mUid)==1?true:false);
        mNotifyStyleImportance.setOnPreferenceChangeListener(this);
        mNotifyStyleStatusBar.setChecked(mBackend.getNotifySettings(mPkg,mUid,NOTIFY_TYPE_STATUS_BAR)==1?true:false);
        mNotifyStyleStatusBar.setOnPreferenceChangeListener(this);
        mNotifyStyleIconSum.setChecked(mBackend.getStyleIcon(mPkg,mUid)==1?true:false);
        mNotifyStyleIconSum.setOnPreferenceChangeListener(this);
        mNotifyStyleHeadsUp.setChecked(mBackend.getStyleHeadsUp(mPkg,mUid)==1?true:false);
        mNotifyStyleHeadsUp.setOnPreferenceChangeListener(this);
        mNotifyStyleLockScreen.setChecked(mBackend.getStyleLockScreen(mPkg,mUid)==1?true:false);
        mNotifyStyleLockScreen.setOnPreferenceChangeListener(this);

        mNotifyRing.setChecked(mBackend.getNotifySettings(mPkg,mUid,NOTIFY_TYPE_RING)==1?true:false);
        mNotifyRing.setOnPreferenceChangeListener(this);
        mNotifyShock.setChecked(mBackend.getNotifySettings(mPkg,mUid,NOTIFY_TYPE_SHOCK)==1?true:false);
        mNotifyShock.setOnPreferenceChangeListener(this);

        mContext = getActivity();
        mPm = getPackageManager();
        mUm = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        mEnableTwinApps = TwinAppsUtil.isTwinAppsSupport(mContext) && mUm.getTwinAppsId() != -1;
        mPkgInfo = findPackageInfo(mPkg, mUid);
        setupAllowedPref(mBackend.getBypassZenMode(mPkg, mUid));
        handleWhiteListAppSwitches(mPkg);
    }

    private void setupAllowedPref(boolean value) {
        mNotifyAllowDisturb.setChecked(value);
        mNotifyAllowDisturb.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean notifyallowed = (Boolean) newValue;
                return mBackend.setBypassZenMode(mPkgInfo.packageName, mUid, notifyallowed);
            }
        });
    }

    private boolean isSystemPkg() {
        boolean res = false;
        if (mPkgInfo != null) {
            res = Utils.isSystemPackage(mContext.getResources(), mPm, mPkgInfo);
        }

        return res;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    private void setStylePromptEnable(boolean state) {
        if (state == false) {
            Log.d(TAG, "hide categories");
            getPreferenceScreen().removePreference(mNotifyStyleCategory);
            getPreferenceScreen().removePreference(mNotifyPromptCategory);
        } else {
            Log.d(TAG, "Show cagegories");
            getPreferenceScreen().addPreference(mNotifyStyleCategory);
            getPreferenceScreen().addPreference(mNotifyPromptCategory);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if(preference == mNotifyAllowed)
        {
            boolean value = mNotifyAllowed.isChecked();
            mBackend.setAllowed(mPkg,mUid,value?0:1);
            //mBackend.setImportance(mPkg,mUid,value?Ranking.IMPORTANCE_NONE : Ranking.IMPORTANCE_UNSPECIFIED);
            setStylePromptEnable(!value);
        }else if(preference == mNotifyStyleImportance) {
            boolean value = mNotifyStyleImportance.isChecked();
            mBackend.setStyleImportance(mPkg, mUid, value ? 0 : 1);
        }
        else if(preference == mNotifyStyleStatusBar)
        {
            boolean value = mNotifyStyleStatusBar.isChecked();
            mBackend.setNotifySettings(mPkg,mUid,NOTIFY_TYPE_STATUS_BAR,value?0:1);
        }else if(preference == mNotifyStyleIconSum){
            boolean value = mNotifyStyleIconSum.isChecked();
            mBackend.setStyleIcon(mPkg,mUid,value?0:1);
        }else if(preference == mNotifyStyleHeadsUp){
            boolean value = mNotifyStyleHeadsUp.isChecked();
            mBackend.setStyleHeadsUp(mPkg,mUid,value?0:1);
        }else if(preference == mNotifyStyleLockScreen){
            boolean value = mNotifyStyleLockScreen.isChecked();
            mBackend.setStyleLockScreen(mPkg,mUid,value?0:1);
        }
        else if(preference == mNotifyRing)
        {
            boolean value = mNotifyRing.isChecked();
            mBackend.setNotifySettings(mPkg,mUid,NOTIFY_TYPE_RING,value?0:1);
        }
        else if(preference == mNotifyShock)
        {
            boolean value = mNotifyShock.isChecked();
            mBackend.setNotifySettings(mPkg,mUid,NOTIFY_TYPE_SHOCK,value?0:1);
        }
        return true;
    }

    private void handleWhiteListAppSwitches(String packageName) {
        try {
            Log.d(TAG, "enter function is Special apps, app name is :" + packageName);
            if (isInWhiteList1(packageName)) {
                // hide allowed switch
                Log.d(TAG, "remove allowed");
                getPreferenceScreen().removePreference(mNotifyAllowedCategory);
            } else if (isInWhiteList2(packageName)) {
                // hide allowed switch
                Log.d(TAG, "remove allowed");
                getPreferenceScreen().removePreference(mNotifyAllowedCategory);

                // hide status bar switch
                Log.d(TAG, "remove status bard switch");
                mNotifyStyleCategory.removePreference(mNotifyStyleStatusBar);

                // hide ring heads-up
                Log.d(TAG, "remove heads-up switch");
                mNotifyStyleCategory.removePreference(mNotifyStyleHeadsUp);
            } else if (isSystemPkg()) {
                getPreferenceScreen().removePreference(mNotifyAllowedCategory);
            }
        } catch (Exception ee) {
            Log.e(TAG, "error occured in isSpecial apps: " + ee);
        }
    }

    private boolean isInWhiteList1(String packageName)
    {
        try{
            Log.d(TAG,"enter function is In White List apps, app name is :" + packageName);
            for (int mm = 0;mm < mAppWhiteListArray1.length;mm++)
            {
                if(packageName.equals(mAppWhiteListArray1[mm]))
                {
                    Log.d(TAG,"package " + packageName + " is in white list");
                    return true;
                }
            }
            Log.d(TAG,"package " + packageName + " is not in white list 1");
            return false;
        }
        catch(Exception ee)
        {
            Log.e(TAG,"error occured in is in white list 1 apps: " +ee);
            return false;
        }
    }

    private boolean isInWhiteList2(String packageName)
    {
        try{
            Log.d(TAG,"enter function is In White List apps, app name is :" + packageName);
            for (int mm = 0;mm < mAppWhiteListArray2.length;mm++)
            {
                if(packageName.equals(mAppWhiteListArray2[mm]))
                {
                    Log.d(TAG,"package " + packageName + " is in white list 2 ");
                    return true;
                }
            }
            Log.d(TAG,"package " + packageName + " is not in white list");
            return false;
        }
        catch(Exception ee)
        {
            Log.e(TAG,"error occured in is in white list 2 apps: " +ee);
            return false;
        }
    }

    private PackageInfo findPackageInfo(String pkg, int uid) {
        final String[] packages = mPm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            final int N = packages.length;
            for (int i = 0; i < N; i++) {
                final String p = packages[i];
                if (pkg.equals(p)) {
                    //[TwinApps] {
                    if (mEnableTwinApps) {
                        try {
                            final IPackageManager iPm = ActivityThread.getPackageManager();
                            return iPm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES, UserHandle.getUserId(uid));
                        } catch (RemoteException e) {
                            Log.w(TAG, "Failed to load package " + pkg, e);
                        }
                    } else {
                    //[TwinApps] {
                        try {
                            return mPm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                        } catch (NameNotFoundException e) {
                            Log.w(TAG, "Failed to load package " + pkg, e);
                        }
                    //[TwinApps] }
                    }
                    //[TwinApps] }
                }
            }
        }
        return null;
    }
}
