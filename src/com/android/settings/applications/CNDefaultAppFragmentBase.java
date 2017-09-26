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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.List;


public class CNDefaultAppFragmentBase extends SettingsPreferenceFragment
        implements CNAppListPreference.OnClickListener {

    private static final String TAG = CNDefaultAppFragmentBase.class.getSimpleName();

    protected CNDefaultAppHelperBase mHelper;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.empty_fragment);
    }

    @Override
    public void onResume(){
        super.onResume();
        updateUI();
    }

    private void updateUI(){
        getPreferenceScreen().removeAll();
        List<CharSequence> applicationNames = mHelper.getApplicationNames();
        List<CharSequence> validatedPackageNames = mHelper.getValidatedPackageNames();
        List<Drawable> entryDrawables = mHelper.getEntryDrawables();
        int selectedIndex = mHelper.getSelectedIndex();
        if ( applicationNames != null) {
            for (int i = 0; i < applicationNames.size(); i++) {
                CNAppListPreference preference = new CNAppListPreference(getActivity());
                preference.setTitle(applicationNames.get(i));
                preference.setValue((String) validatedPackageNames.get(i));
                preference.setIcon(entryDrawables.get(i));
                preference.setOnClickListener(this);
                if (selectedIndex == i) {
                    preference.setChecked(true);
                } else {
                    preference.setChecked(false);
                }
                getPreferenceScreen().addPreference(preference);
            }
        }
    }
    @Override
    public void onRadioButtonClicked(CNAppListPreference emiter) {

    }

    protected void updateRidioButtonState(String packageName){
        for (int i = 0, count = getPreferenceScreen().getPreferenceCount(); i < count; i++) {
            CNAppListPreference pref = (CNAppListPreference) getPreferenceScreen().getPreference(i);
            String value = pref.getValue();
            if (value.equals(packageName)){
                pref.setChecked(true);
                mHelper.setSummary((String)pref.getTitle());
                mHelper.setSelectedIndex(i);
            }else{
                pref.setChecked(false);
            }
        }

    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_MANAGE_ASSIST;
    }

}
