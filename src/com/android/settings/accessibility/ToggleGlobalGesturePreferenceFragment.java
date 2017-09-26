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

package com.android.settings.accessibility;

import android.os.Bundle;
import android.provider.Settings;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;

public class ToggleGlobalGesturePreferenceFragment
        extends ToggleFeaturePreferenceFragment implements OnPreferenceChangeListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSummaryPreference.setOrder(1);
        mSwitchPreference.setTitle(getPrefContext().getResources().getString(R.string.accessibility_global_gesture_preference_title));
        mSwitchPreference.setOnPreferenceChangeListener(this);
        mSwitchPreference.setOrder(0);
        boolean isChecked = getArguments().getBoolean(AccessibilitySettings.EXTRA_CHECKED, false);
        mSwitchPreference.setChecked(isChecked);
        mSwitchPreference.setLayoutResource(R.layout.asusres_preference_material_nodivider);
    }
    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        Settings.Global.putInt(getContentResolver(),
                Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, enabled ? 1 : 0);
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener(new OnBeforeCheckedChangeListener() {
                @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                mSwitchBar.setCheckedInternal(checked);
                getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, checked);
                onPreferenceToggled(mPreferenceKey, checked);
                return false;
            }
        });
    }
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mSwitchPreference == preference) {
            boolean checked = mSwitchPreference.isChecked();
            getArguments().putBoolean(AccessibilitySettings.EXTRA_CHECKED, checked);
            onPreferenceToggled(mPreferenceKey, checked);
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        return true;
    }
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ACCESSIBILITY_TOGGLE_GLOBAL_GESTURE;
    }
}
