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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageParser;
import android.graphics.drawable.ColorDrawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.android.settings.AppProgressPreference;
import com.android.settings.twinApps.TwinAppsUtil;

public class ProcessStatsPreference extends AppProgressPreference {

    private ProcStatsPackageEntry mEntry;
    //[TwinApps] {
    private boolean mEnableTwinApps = false;
    private int mTwinAppsId = -1;
    //[TwinApps] }

    public ProcessStatsPreference(Context context) {
        super(context, null);
        //[TwinApps] {
        mEnableTwinApps = TwinAppsUtil.isTwinAppsSupport(context);
        if(mEnableTwinApps) {
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            mTwinAppsId = userManager.getTwinAppsId();
        }
        //[TwinApps] }
    }

    public void init(ProcStatsPackageEntry entry, PackageManager pm, double maxMemory,
            double weightToRam, double totalScale, boolean avg) {
        mEntry = entry;
        setTitle(TextUtils.isEmpty(entry.mUiLabel) ? entry.mPackage : entry.mUiLabel);
        //[TwinApps] {
        if(mEnableTwinApps && mTwinAppsId != -1){
            if (entry.mUiTargetApp != null) {
                if(UserHandle.getUserId(entry.mUiTargetApp.uid) == mTwinAppsId){
                    setIcon(pm.getUserBadgedIcon(entry.mUiTargetApp.loadIcon(pm), new UserHandle(mTwinAppsId)));
                } else {
                    setIcon(entry.mUiTargetApp.loadIcon(pm));
                }
            } else {
                setIcon(new ColorDrawable(0));
            }
        } else {
        //[TwinApps] }
            if (entry.mUiTargetApp != null) {
                setIcon(entry.mUiTargetApp.loadIcon(pm));
            } else {
                setIcon(new ColorDrawable(0));
            }
        //[TwinApps] {
        }
        //[TwinApps] }
        boolean statsForeground = entry.mRunWeight > entry.mBgWeight;
        double amount = avg ? (statsForeground ? entry.mRunWeight : entry.mBgWeight) * weightToRam
                : (statsForeground ? entry.mMaxRunMem : entry.mMaxBgMem) * totalScale * 1024;
        setSummary(Formatter.formatShortFileSize(getContext(), (long) amount));
        setProgress((int) (100 * amount / maxMemory));
    }

    public ProcStatsPackageEntry getEntry() {
        return mEntry;
    }
}
