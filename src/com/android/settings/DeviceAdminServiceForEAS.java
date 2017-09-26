/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Service;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vincent
 * @version v1.0
 * This service help EAS to add device adminstration in system
 */
public class DeviceAdminServiceForEAS extends Service {
    public static final String ACTION_EMAIL_ADD_DEVICE_ADMIN = "android.app.action.EMAIL_ADD_DEVICE_ADMIN";
    static final String TAG = "DeviceAdminServiceForEAS";

    List<ComponentName> mList;
    DevicePolicyManager mDPM;
    DeviceAdminInfo mDeviceAdmin;

    boolean mAdding;
    boolean mRefreshing;

    private final IDeviceAdmin.Stub mBinder = new IDeviceAdmin.Stub() {
        @Override
        public boolean setAdmin(ComponentName component) throws RemoteException {
            return setComponentAdmin(component);
        }
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        mList = new ArrayList<ComponentName>();
        mList.add(new ComponentName("com.asus.email",
                "com.android.email.SecurityPolicy$PolicyAdmin"));
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        mList.clear();
    }

    public boolean setComponentAdmin(ComponentName component) {
        Log.d(TAG, "setComponentAdmin");

        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (component == null || !mList.contains(component)) {
            Log.w(TAG, "No component specified or component not in white list ");
            return false;
        }

        ComponentName cn = component;
        ActivityInfo ai = null;
        try {
            ai = getPackageManager().getReceiverInfo(cn,
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            return false;
        }

        ResolveInfo ri = new ResolveInfo();
        ri.activityInfo = ai;
        try {
            mDeviceAdmin = new DeviceAdminInfo(this, ri);
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            return false;
        } catch (IOException e) {
            Log.w(TAG, "Unable to retrieve device policy " + cn, e);
            return false;
        }

        mRefreshing = false;
        if (mDPM.isAdminActive(cn)) {
            ArrayList<DeviceAdminInfo.PolicyInfo> newPolicies = mDeviceAdmin
                    .getUsedPolicies();
            for (int i = 0; i < newPolicies.size(); i++) {
                DeviceAdminInfo.PolicyInfo pi = newPolicies.get(i);
                if (!mDPM.hasGrantedPolicy(cn, pi.ident)) {
                    mRefreshing = true;
                    break;
                }
            }
            if (!mRefreshing) {
                // Nothing changed (or policies were removed) - return
                // immediately
                return true;
            }
        }

        try {
            mDPM.setActiveAdmin(mDeviceAdmin.getComponent(), mRefreshing);
            return true;
        } catch (RuntimeException e) {
            // Something bad happened... could be that it was
            // already set, though.
            Log.w(TAG,
                    "Exception trying to activate admin "
                            + mDeviceAdmin.getComponent(), e);
            if (mDPM.isAdminActive(mDeviceAdmin.getComponent())) {
                return true;
            }
        }

        return false;
    }
}
