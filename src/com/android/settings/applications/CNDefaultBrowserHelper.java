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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import com.android.internal.content.PackageMonitor;
import com.android.settings.AppListPreference;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

public class CNDefaultBrowserHelper extends CNDefaultAppHelperBase {

    private static final String TAG = "DefaultBrowserPref";

    private static final long DELAY_UPDATE_BROWSER_MILLIS = 500;

    private final Handler mHandler = new Handler();

    final private PackageManager mPm;

    private static CNDefaultBrowserHelper mInstance;
    private CNDefaultBrowserHelper(Context context){
        super(context);
        setShowItemNone(false);
        setForWork(true);
        mPm = context.getPackageManager();
    }

    public static CNDefaultBrowserHelper getInstance(Context context){
        if (mInstance == null){
            mInstance = new CNDefaultBrowserHelper(context);
        }
        return mInstance;
    }

    public void registerMonitor(){
        mPackageMonitor.register(mContext, mContext.getMainLooper(), false);
    }
    public void unregisterMonitor(){
        mPackageMonitor.unregister();
    }


    public void setBrowserValue(String newValue) {

        if (newValue == null) {
            return;
        }
        final CharSequence packageName = (CharSequence) newValue;
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        boolean result = mPm.setDefaultBrowserPackageNameAsUser(
                packageName.toString(), mUserId);

    }

    public void setToDefault(){
        String packageName = mPm.getDefaultBrowserPackageNameAsUser(mUserId);
        mPm.clearPackagePreferredActivities(packageName);
        mPm.setDefaultBrowserPackageNameAsUser(null, mUserId);
    }

    public void refreshBrowserApps() {
        String packageName = mPm.getDefaultBrowserPackageNameAsUser(mUserId);
        List<String> browsers = resolveBrowserApps();
        setPackageNames(browsers.toArray(new String[browsers.size()]), packageName);
    }

    private List<String> resolveBrowserApps() {
        List<String> result = new ArrayList<>();

        // Create an Intent that will match ALL Browser Apps
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("http:"));

        // Resolve that intent and check that the handleAllWebDataURI boolean is set
        List<ResolveInfo> list = mPm.queryIntentActivitiesAsUser(intent, PackageManager.MATCH_ALL,
                mUserId);
        final int count = list.size();
        for (int i=0; i<count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo == null || result.contains(info.activityInfo.packageName)
                    || !info.handleAllWebDataURI) {
                continue;
            }

            result.add(info.activityInfo.packageName);
        }

        return result;
    }

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            refreshBrowserApps();
        }
    };

    private final PackageMonitor mPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            mHandler.postDelayed(mUpdateRunnable, DELAY_UPDATE_BROWSER_MILLIS);
        }
    };

    public static boolean hasBrowserPreference(String pkg, Context context) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(Uri.parse("http:"));
        intent.setPackage(pkg);
        final List<ResolveInfo> resolveInfos =
                context.getPackageManager().queryIntentActivities(intent, 0);
        return resolveInfos != null && resolveInfos.size() != 0;
    }

    public static boolean isBrowserDefault(String pkg, Context context) {
        String defaultPackage = context.getPackageManager()
                .getDefaultBrowserPackageNameAsUser(UserHandle.myUserId());
        return defaultPackage != null && defaultPackage.equals(pkg);
    }
}
