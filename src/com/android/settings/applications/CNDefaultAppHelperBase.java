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
 * limitations under the License
 */

package com.android.settings.applications;

import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionServiceInfo;
import android.speech.RecognitionService;
import android.util.Log;

import com.android.internal.app.AssistUtils;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;


public class CNDefaultAppHelperBase {

    public static final String ITEM_NONE_VALUE = "";

    protected boolean mForWork;
    protected int mUserId;

    private boolean mShowItemNone = false;
    private int mSystemAppIndex = -1;
    protected Context mContext;
    private List<CharSequence> applicationNames;
    private List<CharSequence> validatedPackageNames;
    private List<Drawable> entryDrawables;
    private int selectedIndex;
    private String mSummary;
    private CharSequence[] mSummaries;

    public List<CharSequence> getApplicationNames(){
        return applicationNames;
    }

    public List<CharSequence> getValidatedPackageNames(){
        return validatedPackageNames;
    }

    public List<Drawable> getEntryDrawables(){
        return entryDrawables;
    }

    public int getSelectedIndex(){
        return selectedIndex;
    }

    public void setSelectedIndex(int index){
        selectedIndex = index;
    }
    public String getSummary(){
        return mSummary;
    }

    public void setSummary(String summary){
        mSummary = summary;
    }

    public CNDefaultAppHelperBase(Context context){
        mContext = context;
    }


    protected void setShowItemNone(boolean showItemNone) {
        mShowItemNone = showItemNone;
    }

    protected void setForWork(boolean forWork) {
        mForWork = forWork;
        final UserHandle managedProfile = com.android.settings.Utils.getManagedProfile(UserManager.get(mContext));
        mUserId = mForWork && managedProfile != null ? managedProfile.getIdentifier()
                : UserHandle.myUserId();
    }
    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName) {
        setPackageNames(packageNames, defaultPackageName, null);
    }

    public void setPackageNames(CharSequence[] packageNames, CharSequence defaultPackageName,
                                CharSequence systemPackageName) {
        // Look up all package names in PackageManager. Skip ones we can't find.
        PackageManager pm = mContext.getPackageManager();
        final int entryCount = packageNames.length + (mShowItemNone ? 1 : 0);
        applicationNames = new ArrayList<>(entryCount);
        validatedPackageNames = new ArrayList<>(entryCount);
        entryDrawables = new ArrayList<>(entryCount);
        selectedIndex = -1;
        mSystemAppIndex = -1;

        if (mShowItemNone) {
            applicationNames.add(
                    mContext.getResources().getText(R.string.app_list_preference_none));
            validatedPackageNames.add(ITEM_NONE_VALUE);
            entryDrawables.add(mContext.getDrawable(R.drawable.asusres_app_none));
            if (defaultPackageName == null || defaultPackageName.equals(ITEM_NONE_VALUE)) {
                selectedIndex = 0;
                mSummary = (String) mContext.getResources().getText(R.string.app_list_preference_none);
            }
        }

        for (int i = 0; i < packageNames.length; i++) {
            try {
                ApplicationInfo appInfo = pm.getApplicationInfoAsUser(packageNames[i].toString(), 0,
                        mUserId);
                applicationNames.add(appInfo.loadLabel(pm));
                validatedPackageNames.add(appInfo.packageName);
                entryDrawables.add(appInfo.loadIcon(pm));
                if (defaultPackageName != null &&
                        appInfo.packageName.contentEquals(defaultPackageName)) {
                    selectedIndex = mShowItemNone?i+1:i;
                    mSummary = (String)appInfo.loadLabel(pm);
                }
                if (appInfo.packageName != null && systemPackageName != null &&
                        appInfo.packageName.contentEquals(systemPackageName)) {
                    mSystemAppIndex = mShowItemNone?i+1:i;
                }
            } catch (PackageManager.NameNotFoundException e) {
                // Skip unknown packages.
            }
        }
        if (selectedIndex == -1 && mShowItemNone){
            selectedIndex = 0;
            mSummary = (String)mContext.getResources().getText(R.string.app_list_preference_none);
        }
        if (selectedIndex == -1){
            mSummary = (String)mContext.getResources().getText(R.string.app_list_preference_none);
        }

    }

    public void setComponentNames(ComponentName[] componentNames, ComponentName defaultCN) {
        setComponentNames(componentNames, defaultCN, null);
    }

    public void setComponentNames(ComponentName[] componentNames, ComponentName defaultCN,
                                  CharSequence[] summaries) {
        mSummaries = summaries;
        // Look up all package names in PackageManager. Skip ones we can't find.
        PackageManager pm = mContext.getPackageManager();
        final int entryCount = componentNames.length + (mShowItemNone ? 1 : 0);
        applicationNames = new ArrayList<>(entryCount);
        validatedPackageNames = new ArrayList<>(entryCount);
        entryDrawables = new ArrayList<>(entryCount);
        selectedIndex = -1;

        if (mShowItemNone) {
            applicationNames.add(
                    mContext.getResources().getText(R.string.app_list_preference_none));
            validatedPackageNames.add(ITEM_NONE_VALUE);
            entryDrawables.add(mContext.getDrawable(R.drawable.ic_remove_circle));
            if (defaultCN == null || defaultCN.equals(ITEM_NONE_VALUE))
                selectedIndex = 0;
        }
        if (defaultCN == null || defaultCN.equals(ITEM_NONE_VALUE)){
            mSummary = (String)mContext.getResources().getText(R.string.app_list_preference_none);
        }
        for (int i = 0; i < componentNames.length; i++) {
            try {
                ActivityInfo activityInfo = AppGlobals.getPackageManager().getActivityInfo(
                        componentNames[i], 0, mUserId);
                if (activityInfo == null) continue;
                applicationNames.add(activityInfo.loadLabel(pm));
                validatedPackageNames.add(componentNames[i].flattenToString());
                entryDrawables.add(activityInfo.loadIcon(pm));
                if (defaultCN != null && componentNames[i].equals(defaultCN)) {
                    selectedIndex = mShowItemNone?i+1:i;
                    mSummary = (String)activityInfo.loadLabel(pm);
                }
            } catch (RemoteException e) {
                // Skip unknown packages.
            }
        }

    }
}
