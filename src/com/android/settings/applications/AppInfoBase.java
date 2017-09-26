/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Activity;
import android.app.ActivityThread;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.twinApps.TwinAppsUtil;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public abstract class AppInfoBase extends SettingsPreferenceFragment
        implements ApplicationsState.Callbacks {

    public static final String ARG_PACKAGE_NAME = "package";
    public static final String ARG_PACKAGE_UID = "uid";

    protected static final String TAG = AppInfoBase.class.getSimpleName();
    protected static final boolean localLOGV = false;

    protected EnforcedAdmin mAppsControlDisallowedAdmin;
    protected boolean mAppsControlDisallowedBySystem;

    protected ApplicationsState mState;
    protected ApplicationsState.Session mSession;
    protected ApplicationsState.AppEntry mAppEntry;
    protected PackageInfo mPackageInfo;
    protected int mUserId;
    protected String mPackageName;

    protected IUsbManager mUsbManager;
    protected DevicePolicyManager mDpm;
    protected UserManager mUserManager;
    protected PackageManager mPm;

    // Dialog identifiers used in showDialog
    protected static final int DLG_BASE = 0;

    protected boolean mFinishing;
    protected boolean mListeningToPackageRemove;

    //[TwinApps] {
    protected boolean mEnableTwinApps = false;
    protected int mTwinAppsId = -1;
    //[TwinApps] }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFinishing = false;

        mState = ApplicationsState.getInstance(getActivity().getApplication());
        mSession = mState.newSession(this);
        Context context = getActivity();
        mDpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPm = context.getPackageManager();
        IBinder b = ServiceManager.getService(Context.USB_SERVICE);
        mUsbManager = IUsbManager.Stub.asInterface(b);

        //[TwinApps] {
        mEnableTwinApps = TwinAppsUtil.isTwinAppsSupport(context);
        if (mEnableTwinApps)
            mTwinAppsId = mUserManager.getTwinAppsId();
        //[TwinApps] }

        LOG_D("call function retriveAppEntry in AppInfoBase onCreate");
        retrieveAppEntry();
        startListeningToPackageRemove();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.resume();
        mAppsControlDisallowedAdmin = RestrictedLockUtils.checkIfRestrictionEnforced(getActivity(),
                UserManager.DISALLOW_APPS_CONTROL, mUserId);
        mAppsControlDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_APPS_CONTROL, mUserId);

        LOG_D("call function refreshUi in AppInfoBase onResume");
        if (!refreshUi()) {
            LOG_D("refreshUi return false");
            setIntentAndFinish(true, true);
        }
    }

    @Override
    public void onPause() {
        mSession.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        stopListeningToPackageRemove();
        mSession.release();
        super.onDestroy();
    }

    protected String retrieveAppEntry() {
        LOG_D("in function: retrieveAppEntry");
        final Bundle args = getArguments();
        mPackageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        if (mPackageName == null) {
            Intent intent = (args == null) ?
                    getActivity().getIntent() : (Intent) args.getParcelable("intent");
            if (intent != null) {
                mPackageName = intent.getData().getSchemeSpecificPart();
            }
        }
        mUserId = UserHandle.myUserId();
        //[TwinApps] {
        if (mEnableTwinApps && mTwinAppsId != -1) {
            //Check if uid is the same as iTwinAppsId.
            //If true, set mUserId to be iTwinAppsId.
            int iEntryUid = (args != null) ? args.getInt(ARG_PACKAGE_UID) : -1;
            if(UserHandle.getUserId(iEntryUid) == mTwinAppsId || iEntryUid == mTwinAppsId) {
                mUserId = mTwinAppsId;
            }
        }
        //[TwinApps] }
        mAppEntry = mState.getEntry(mPackageName, mUserId);
        if (mAppEntry != null) {
            //[TwinApps] {
            if (mEnableTwinApps) {
                try {
                    final IPackageManager ipm = ActivityThread.getPackageManager();
                    mPackageInfo = ipm.getPackageInfo(mAppEntry.info.packageName,
                        PackageManager.GET_DISABLED_COMPONENTS |
                        PackageManager.GET_UNINSTALLED_PACKAGES |
                        PackageManager.GET_SIGNATURES |
                        PackageManager.GET_PERMISSIONS, UserHandle.getUserId(mAppEntry.info.uid));
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed to load package " + mAppEntry.info.packageName, e);
                }
            } else {
            //[TwinApps] }
                // Get application info again to refresh changed properties of application
                try {
                    mPackageInfo = mPm.getPackageInfo(mAppEntry.info.packageName,
                        PackageManager.GET_DISABLED_COMPONENTS |
                        PackageManager.GET_UNINSTALLED_PACKAGES |
                        PackageManager.GET_SIGNATURES |
                        PackageManager.GET_PERMISSIONS);
                } catch (NameNotFoundException e) {
                    Log.e(TAG, "Exception when retrieving package:" + mAppEntry.info.packageName, e);
                }
            //[TwinApps] {
            }
            //[TwinApps] }
        } else {
            Log.w(TAG, "Missing AppEntry; maybe reinstalling?");
            mPackageInfo = null;
            LOG_D("in function: retrieveAppEntry, mAppEntry is null, set mPackageInfo to null");
        }

        return mPackageName;
    }

    protected void setIntentAndFinish(boolean finish, boolean appChanged) {
        if (localLOGV) Log.i(TAG, "appChanged="+appChanged);
        Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        SettingsActivity sa = (SettingsActivity)getActivity();
        sa.finishPreferencePanel(this, Activity.RESULT_OK, intent);
        mFinishing = true;
        LOG_D("in setIntentAndFinish, set mFinishing true");
    }

    protected void showDialogInner(int id, int moveErrorCode) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id, moveErrorCode);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    protected abstract boolean refreshUi();
    protected abstract AlertDialog createDialog(int id, int errorCode);

    @Override
    public void onRunningStateChanged(boolean running) {
        // No op.
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        // No op.
    }

    @Override
    public void onPackageIconChanged() {
        // No op.
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        // No op.
    }

    @Override
    public void onAllSizesComputed() {
        // No op.
    }

    @Override
    public void onLauncherInfoChanged() {
        // No op.
    }

    @Override
    public void onLoadEntriesCompleted() {
        // No op.
    }

    @Override
    public void onPackageListChanged() {
        refreshUi();
    }
    //this change is for notification
    public static void startAppInfoFragmentT(Class<?> fragment, String label,
            String pkg, int uid, Fragment source, int request) {
        startAppInfoFragmentT(fragment, label, pkg, uid, source.getActivity(), request);
    }

    public static void startAppInfoFragmentT(Class<?> fragment, String label,
            String pkg, int uid, Activity source, int request) {
        Bundle args = new Bundle();
        args.putString(AppInfoBase.ARG_PACKAGE_NAME, pkg);
        args.putInt(AppInfoBase.ARG_PACKAGE_UID, uid);

        Intent intent = Utils.onBuildStartFragmentIntentT(source, fragment.getName(),
                args, null, label, false);
        source.startActivityForResultAsUser(intent, request,
                new UserHandle(UserHandle.getUserId(uid)));
    }

    public static void startAppInfoFragment(Class<?> fragment, int titleRes,
            String pkg, int uid, Fragment source, int request) {
        startAppInfoFragment(fragment, titleRes, pkg, uid, source.getActivity(), request);
    }

    public static void startAppInfoFragment(Class<?> fragment, int titleRes,
            String pkg, int uid, Activity source, int request) {
        Bundle args = new Bundle();
        args.putString(AppInfoBase.ARG_PACKAGE_NAME, pkg);
        args.putInt(AppInfoBase.ARG_PACKAGE_UID, uid);

        Intent intent = Utils.onBuildStartFragmentIntent(source, fragment.getName(),
                args, null, titleRes, null, false);
        source.startActivityForResultAsUser(intent, request,
                new UserHandle(UserHandle.getUserId(uid)));
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            int errorCode = getArguments().getInt("moveError");
            Dialog dialog = ((AppInfoBase) getTargetFragment()).createDialog(id, errorCode);
            if (dialog == null) {
                throw new IllegalArgumentException("unknown id " + id);
            }
            return dialog;
        }

        public static MyAlertDialogFragment newInstance(int id, int errorCode) {
            MyAlertDialogFragment dialogFragment = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putInt("moveError", errorCode);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }

    protected void startListeningToPackageRemove() {
        if (mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = true;
        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        getContext().registerReceiver(mPackageRemovedReceiver, filter);
    }

    protected void stopListeningToPackageRemove() {
        if (!mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = false;
        getContext().unregisterReceiver(mPackageRemovedReceiver);
    }

    protected void onPackageRemoved() {
        getActivity().finishAndRemoveTask();
    }

    protected final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (!mFinishing) {
                if (mAppEntry != null && mAppEntry.info.packageName.equals(packageName))
                    onPackageRemoved();
                else if (mAppEntry == null){
                    LOG_D("##### mAppEntry == null IN mPackageRemovedReceiver #####");
                    onPackageRemoved();
                }
            }
        }
    };

    public void LOG_D(String msg){
        Log.d("ForUninstall", msg);
    }
}
