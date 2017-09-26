package com.android.settings.applications;

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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceCategory;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.android.settings.R;

import static java.security.AccessController.getContext;

/**
 * Created by jack_qi on 2016/12/21.
 */

public class AllAppPermissions  extends AppInfoWithHeader implements View.OnClickListener,
        Preference.OnPreferenceChangeListener{

    private static final String LOG_TAG = "CNSettings_AllAppPermissionsFragment";

    private static final String KEY_OTHER = "settings_other_perms";

    public AllAppPermissions(){

    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_APP_LAUNCH;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    private void updateUi() {

        if (getPreferenceScreen() != null) {
            getPreferenceScreen().removeAll();
        }

        addPreferencesFromResource(R.xml.all_permissions);

        PreferenceGroup otherGroup = (PreferenceGroup) findPreference(KEY_OTHER);
        ArrayList<Preference> prefs = new ArrayList<>(); // Used for sorting.
        prefs.add(otherGroup);
        //String pkg = getArguments().getString(InstalledAppDetails.ARG_PACKAGE_NAME);
        otherGroup.removeAll();
        PackageManager pm = getContext().getPackageManager();

        try {
            PackageInfo info = pm.getPackageInfo(mPackageName, PackageManager.GET_PERMISSIONS);

            ApplicationInfo appInfo = info.applicationInfo;
            final Drawable icon = appInfo.loadIcon(pm);
            final CharSequence label = appInfo.loadLabel(pm);
//            Intent infoIntent = null;
//            if (!getActivity().getIntent().getBooleanExtra(
//                    AppPermissionsFragment.EXTRA_HIDE_INFO_BUTTON, false)) {
//                infoIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
//                        .setData(Uri.fromParts("package", pkg, null));
//            }
//            setHeader(icon, label, infoIntent);

            if (info.requestedPermissions != null) {
                for (int i = 0; i < info.requestedPermissions.length; i++) {
                    PermissionInfo perm;
                    try {
                        perm = pm.getPermissionInfo(info.requestedPermissions[i], 0);
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(LOG_TAG,
                                "Can't get permission info for " + info.requestedPermissions[i], e);
                        continue;
                    }

                    if ((perm.flags & PermissionInfo.FLAG_INSTALLED) == 0
                            || (perm.flags & PermissionInfo.FLAG_REMOVED) != 0) {
                        continue;
                    }

                    if (perm.protectionLevel == PermissionInfo.PROTECTION_DANGEROUS) {
                        PermissionGroupInfo group = getGroup(perm.group, pm);
                        PreferenceGroup pref =
                                findOrCreate(group != null ? group : perm, pm, prefs);
                        pref.addPreference(getPreference(perm, group, pm));
                    } else if (perm.protectionLevel == PermissionInfo.PROTECTION_NORMAL) {
                        PermissionGroupInfo group = getGroup(perm.group, pm);
                        otherGroup.addPreference(getPreference(perm, group, pm));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Problem getting package info for " + mPackageName, e);
        }
        // Sort an ArrayList of the groups and then set the order from the sorting.
        Collections.sort(prefs, new Comparator<Preference>() {
            @Override
            public int compare(Preference lhs, Preference rhs) {
                String lKey = lhs.getKey();
                String rKey = rhs.getKey();
                if (lKey.equals(KEY_OTHER)) {
                    return 1;
                } else if (rKey.equals(KEY_OTHER)) {
                    return -1;
                } else if (Utils.isModernPermissionGroup(lKey)
                        != Utils.isModernPermissionGroup(rKey)) {
                    return Utils.isModernPermissionGroup(lKey) ? -1 : 1;
                }
                return lhs.getTitle().toString().compareTo(rhs.getTitle().toString());
            }
        });
        for (int i = 0; i < prefs.size(); i++) {
            prefs.get(i).setOrder(i);
        }

        for (int i = 0; i < prefs.size(); i++) {
            PreferenceGroup pref = (PreferenceGroup)prefs.get(i);
            for(int j = 0; j < pref.getPreferenceCount(); j++){
                if(j == pref.getPreferenceCount() - 1){
                    pref.getPreference(j).setLayoutResource(R.layout.asusres_preference_material_nodivider);
                }
            }

        }
    }

    private PermissionGroupInfo getGroup(String group, PackageManager pm) {
        try {
            return pm.getPermissionGroupInfo(group, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private PreferenceGroup findOrCreate(PackageItemInfo group, PackageManager pm,
                                         ArrayList<Preference> prefs) {
        PreferenceGroup pref = (PreferenceGroup) findPreference(group.name);
        if (pref == null) {
            pref = new PreferenceCategory(getContext());
            pref.setKey(group.name);
            pref.setTitle(group.loadLabel(pm));
            prefs.add(pref);
            getPreferenceScreen().addPreference(pref);
        }
        return pref;
    }

    private Preference getPreference(PermissionInfo perm, PermissionGroupInfo group,
                                     PackageManager pm) {
        Preference pref = new Preference(getContext());
        Drawable icon = null;
        if (perm.icon != 0) {
            icon = perm.loadIcon(pm);
        } else if (group != null && group.icon != 0) {
            icon = group.loadIcon(pm);
        } else {
            icon = getContext().getDrawable(R.drawable.ic_perm_device_info);
        }
//        pref.setIcon(Utils.applyTint(getContext(), icon, android.R.attr.colorControlNormal));
        pref.setTitle(perm.loadLabel(pm));
        final CharSequence desc = perm.loadDescription(pm);
        final PermissionGroupInfo groupInfo =group;

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                creatHintDialog(groupInfo, desc);
                return true;
            }
        });

        return pref;
    }

    private void creatHintDialog(PermissionGroupInfo group, CharSequence desc){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(R.string.cancel,null);

        View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
        TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
        if (group != null) {
            title.setText(group.loadLabel(getContext().getPackageManager()));
        } else{
            title.setText(getActivity().getResources().getString(R.string.other_permissions));
        }
        TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
        message.setText(desc);

        builder.setView(view1);
        AlertDialog dialog = builder.show();

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        dialogWindow.setAttributes(lp);
    }
    protected boolean refreshUi() {
//        mClearDefaultsPreference.setPackageName(mPackageName);
//        mClearDefaultsPreference.setAppEntry(mAppEntry);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        // No dialogs for preferred launch settings.
        return null;
    }

    @Override
    public void onClick(View v) {
        // Nothing to do
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // actual updates are handled by the app link dropdown callback
        return true;
    }
}
