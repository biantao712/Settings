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

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.android.internal.telephony.SmsApplication;
import com.android.internal.telephony.SmsApplication.SmsApplicationData;
import com.android.settings.AppListPreference;
import com.android.settings.AsusTelephonyUtils;
import com.android.settings.SelfAvailablePreference;

import java.util.Collection;
import java.util.Objects;

public class CNDefaultSmsHelper extends CNDefaultAppHelperBase {

    private static CNDefaultSmsHelper mInstance;
    private CNDefaultSmsHelper(Context context){
        super(context);
        setShowItemNone(false);
        setForWork(false);
    }

    public static CNDefaultSmsHelper getInstance(Context context){
        if (mInstance == null){
            mInstance = new CNDefaultSmsHelper(context);
        }
        return mInstance;
    }

    public void loadSmsApps() {
        Collection<SmsApplicationData> smsApplications =
                SmsApplication.getApplicationCollection(mContext);

        int count = smsApplications.size();
        String[] packageNames = new String[count];
        int i = 0;
        for (SmsApplicationData smsApplicationData : smsApplications) {
            packageNames[i++] = smsApplicationData.mPackageName;
        }
        setPackageNames(packageNames, getDefaultPackage());
    }

    private String getDefaultPackage() {
        ComponentName appName = SmsApplication.getDefaultSmsApplication(mContext, true);
        if (appName != null) {
            return appName.getPackageName();
        }
        return null;
    }

    public void setToDefault(){
        mContext.getPackageManager().clearPackagePreferredActivities(getDefaultPackage());
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.SMS_DEFAULT_APPLICATION, ITEM_NONE_VALUE);
    }

    public boolean setSmsValue(String value) {
        if (!TextUtils.isEmpty(value) && !Objects.equals(value, getDefaultPackage())) {
            SmsApplication.setDefaultApplication(value, mContext);
        }
        return true;
    }

    public static boolean hasSmsPreference(String pkg, Context context) {
        Collection<SmsApplicationData> smsApplications =
                SmsApplication.getApplicationCollection(context);
        for (SmsApplicationData data : smsApplications) {
            if (data.mPackageName.equals(pkg)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSmsDefault(String pkg, Context context) {
        ComponentName appName = SmsApplication.getDefaultSmsApplication(context, true);
        return appName != null && appName.getPackageName().equals(pkg);
    }
}
