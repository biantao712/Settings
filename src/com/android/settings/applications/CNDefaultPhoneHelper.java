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
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telecom.DefaultDialerManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.settings.AppListPreference;
import com.android.settings.SelfAvailablePreference;

import java.util.List;
import java.util.Objects;

public class CNDefaultPhoneHelper extends CNDefaultAppHelperBase {

    private static CNDefaultPhoneHelper mInstance;
    private CNDefaultPhoneHelper(Context context){
        super(context);
        setShowItemNone(false);
        setForWork(true);
    }

    public static CNDefaultPhoneHelper getInstance(Context context){
        if (mInstance == null){
            mInstance = new CNDefaultPhoneHelper(context);
        }
        return mInstance;
    }


    public boolean setPhoneValue(String value) {
        if (!TextUtils.isEmpty(value) && !Objects.equals(value, getDefaultPackage())) {
            DefaultDialerManager.setDefaultDialerApplication(mContext, value, mUserId);
        }
        return true;
    }

    public void setToDefault(){
        mContext.getPackageManager().clearPackagePreferredActivities(getDefaultPackage());
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.DIALER_DEFAULT_APPLICATION, ITEM_NONE_VALUE);
    }
    public void loadDialerApps() {
        List<String> dialerPackages =
                DefaultDialerManager.getInstalledDialerApplications(mContext, mUserId);

        final String[] dialers = new String[dialerPackages.size()];
        for (int i = 0; i < dialerPackages.size(); i++) {
            dialers[i] = dialerPackages.get(i);
        }
        setPackageNames(dialers, getDefaultPackage(), getSystemPackage());
    }

    private String getDefaultPackage() {
        return DefaultDialerManager.getDefaultDialerApplication(mContext, mUserId);
    }

    private String getSystemPackage() {
        TelecomManager tm = TelecomManager.from(mContext);
        return tm.getSystemDialerPackage();
    }


    public static boolean hasPhonePreference(String pkg, Context context) {
        List<String> dialerPackages =
                DefaultDialerManager.getInstalledDialerApplications(context, UserHandle.myUserId());
        return dialerPackages.contains(pkg);
    }

    public static boolean isPhoneDefault(String pkg, Context context) {
        String def = DefaultDialerManager.getDefaultDialerApplication(context,
                UserHandle.myUserId());
        return def != null && def.equals(pkg);
    }
}
