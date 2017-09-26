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

import android.app.AlertDialog;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

import android.content.pm.IntentFilterVerificationInfo;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import java.util.List;


public class CNSupportLinkList extends AppInfoBase {
    private static final String TAG = "CNSupportLinkList";

    private static final String KEY_SUPPORT_LINK_LIST = "support_link_list";
    private PreferenceCategory mSupportLinkCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.cn_support_link_list);
        mSupportLinkCategory = (PreferenceCategory) findPreference(KEY_SUPPORT_LINK_LIST);

    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }


    private void updateUi() {

        PackageManager mPm = getActivity().getPackageManager();
        List<IntentFilterVerificationInfo> iviList = mPm.getIntentFilterVerifications(mPackageName);
        List<IntentFilter> filters = mPm.getAllIntentFilters(mPackageName);
        CharSequence[] entries = getEntries(mPackageName, iviList, filters);
        for (int i = 0; i<entries.length; i++) {
            mSupportLinkCategory.addPreference(getPreference(entries[i]));
        }
    }


    private Preference getPreference(CharSequence text) {
        Preference pref = new Preference(getContext());
        pref.setTitle(text);
        return pref;
    }
    @Override
    protected boolean refreshUi() {
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        return null;
    }

    private CharSequence[] getEntries(String packageName, List<IntentFilterVerificationInfo> iviList,
                                      List<IntentFilter> filters) {
        ArraySet<String> result = Utils.getHandledDomains(mPm, packageName);
        return result.toArray(new CharSequence[result.size()]);
    }
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_APP_LAUNCH;
    }
}
