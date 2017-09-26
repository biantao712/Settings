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

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.PreferenceGroup;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LegalSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String KEY_TERMS = "terms";
    private static final String KEY_LICENSE = "license";
    private static final String KEY_COPYRIGHT = "copyright";
    private static final String KEY_WEBVIEW_LICENSE = "webview_license";
    private static final String KEY_GENERAL_SOURCECODE = "general_sourcecode_offer";
    private static final String KEY_GEONAMES = "geonames_cc_by";


    // Added by Mingszu Liang, 2017.01.13. - BEGIN.
    //
    //   - this preference is
    //     for AsusAnalytics (com.asus.as) and DataSDK (com.asus.abcdatasdk).
    //
    //   <PreferenceScreen android:key="inspire_asus" />
    private static final String KEY_INSPIRE_ASUS = "inspire_asus";
    //
    // Added by Mingszu Liang, 2017.01.13. - END.


    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.about_legal);

        final Activity act = getActivity();
        // These are contained in the "container" preference group
        PreferenceGroup parentPreference = getPreferenceScreen();
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_TERMS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_COPYRIGHT,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_WEBVIEW_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);

        // Added by Mingszu Liang, 2017.01.13. - BEGIN.
        //
        //   - this preference is
        //     for AsusAnalytics (com.asus.as) and DataSDK (com.asus.abcdatasdk).
        //   - We must remove/hide the "Diagnostic Data" preference for some cases.
        //
        updateDiagnosticDataPreferenceOrRemove();
        //
        // Added by Mingszu Liang, 2017.01.13. - END.
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ABOUT_LEGAL_SETTINGS;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.about_legal;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                if (!checkIntentAction(context, "android.settings.TERMS")) {
                    keys.add(KEY_TERMS);
                }
                if (!checkIntentAction(context, "android.settings.LICENSE")) {
                    keys.add(KEY_LICENSE);
                }
                if (!checkIntentAction(context, "android.settings.COPYRIGHT")) {
                    keys.add(KEY_COPYRIGHT);
                }
                if (!checkIntentAction(context, "android.settings.WEBVIEW_LICENSE")) {
                    keys.add(KEY_WEBVIEW_LICENSE);
                }
                return keys;
            }

            private boolean checkIntentAction(Context context, String action) {
                final Intent intent = new Intent(action);

                // Find the activity that is in the system image
                final PackageManager pm = context.getPackageManager();
                final List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
                final int listSize = list.size();

                for (int i = 0; i < listSize; i++) {
                    ResolveInfo resolveInfo = list.get(i);
                    if ((resolveInfo.activityInfo.applicationInfo.flags &
                            ApplicationInfo.FLAG_SYSTEM) != 0) {
                        return true;
                    }
                }

                return false;
            }
    };

    // Added by Mingszu Liang, 2017.01.13. - BEGIN.
    //
    //   - this preference is
    //     for AsusAnalytics (com.asus.as) and DataSDK (com.asus.abcdatasdk).
    //
    private void updateDiagnosticDataPreferenceOrRemove() {

        //==========================================================================================
        //
        //  According to Verizon's request, we must remove the "Diagnostic Data" preference.
        //
        //==========================================================================================
        //  [AMAX_M][AsusSettings][Verizon]Remove Inspire ASUS and ZenUIUpdate in VerizonSKU.
        //      Code Review: http://amax01:8080/263396
        //      SHA1       : fadcfc1827c327b6dce738dd0cb2ecbce90a760f
        //      Owner      : Kevin Chiou <Kevin_Chiou@asus.com>
        //      Updated    : Thu Dec 3 15:05:23 2015 +0800
        //------------------------------------------------------------------------------------------
        //  [UI - View Hierarchy]
        //      <PreferenceScreen
        //          android:key="inspire_asus"
        //          android:title="@string/inspire_asus_title"
        //          android:fragment="com.android.settings.analytic.AsusDiagnosticDataFragment" />
        //==========================================================================================

        if (Utils.isVerizonSKU()) {
            removePreference(KEY_INSPIRE_ASUS);
        } //END OF if (Utils.isVerizonSKU())
    } //END OF updateDiagnosticDataPreferenceOrRemove()
    //
    // Added by Mingszu Liang, 2017.01.13. - END.
}
