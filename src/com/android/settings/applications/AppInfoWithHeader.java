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

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import com.android.settings.AppHeader;
import com.android.settings.twinApps.TwinAppsUtil;

public abstract class AppInfoWithHeader extends AppInfoBase {

    private boolean mCreated;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mCreated) {
            Log.w(TAG, "onActivityCreated: ignoring duplicate call");
            return;
        }
        mCreated = true;
        if (mPackageInfo == null) return;
        //[TwinApps] {
        Drawable icon = mPackageInfo.applicationInfo.loadIcon(mPm);
        if (TwinAppsUtil.isTwinAppsSupport(getActivity())) {
            int iTwinAppsId = mUserManager.getTwinAppsId();
            if ((iTwinAppsId != -1) && (iTwinAppsId == UserHandle.getUserId(mAppEntry.info.uid))) {
                icon = mPm.getUserBadgedIcon(mPackageInfo.applicationInfo.loadIcon(mPm), new UserHandle(iTwinAppsId));
            }
        }
        //[TwinApps] }
        //AppHeader.createAppHeader(this, mPackageInfo.applicationInfo.loadIcon(mPm),  //[TwinApps]
        AppHeader.createAppHeader(this, icon,
                mPackageInfo.applicationInfo.loadLabel(mPm), mPackageName,
                mPackageInfo.applicationInfo.uid, 0, false);
        //+++ blacklist
        if ((mPackageInfo.applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0 &&
                 mPm.isInApp2sdBlacklist(mPackageName)) {
            AppHeader.setAppInfoIconTint(this, android.graphics.Color.RED);
        } else {
            AppHeader.setAppInfoIconTint(this, android.graphics.Color.BLACK);
        }
        //---
    }
}
