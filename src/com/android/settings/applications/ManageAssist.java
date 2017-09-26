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
import android.content.ComponentName;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import com.android.internal.app.AssistUtils;
import android.support.v7.preference.PreferenceScreen;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.voice.CNVoiceInputPrefHelper;
import com.android.settings.voice.VoiceInputHelper;
import com.android.settings.voice.VoiceInputListPreference;
import com.android.internal.app.AssistUtils;

/**
 * Settings screen to manage everything about assist.
 */
public class ManageAssist extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_DEFAULT_ASSIST = "default_assist";
    private static final String KEY_CONTEXT = "context";
    private static final String KEY_SCREENSHOT = "screenshot";
    private static final String KEY_VOICE_INPUT = "voice_input_settings";
    private static final String KEY_FLASH = "flash";

    private PreferenceScreen mDefaultAssitPref;
    private SwitchPreference mContextPref;
    private SwitchPreference mScreenshotPref;
    private SwitchPreference mFlashPref;
    private PreferenceScreen mVoiceInputPref;
    private Handler mHandler = new Handler();

    private CNDefaultAssistHelper mAssistHelper;
    private CNVoiceInputPrefHelper mVoiceHelper;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.manage_assist);

        mDefaultAssitPref = (PreferenceScreen) findPreference(KEY_DEFAULT_ASSIST);
//        mDefaultAssitPref.setOnPreferenceChangeListener(this);

        mContextPref = (SwitchPreference) findPreference(KEY_CONTEXT);
        mContextPref.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ASSIST_STRUCTURE_ENABLED, 1) != 0);
        mContextPref.setOnPreferenceChangeListener(this);

        mScreenshotPref = (SwitchPreference) findPreference(KEY_SCREENSHOT);
        mScreenshotPref.setOnPreferenceChangeListener(this);

        mFlashPref = (SwitchPreference) findPreference(KEY_FLASH);
        mFlashPref.setOnPreferenceChangeListener(this);

        mVoiceInputPref = (PreferenceScreen) findPreference(KEY_VOICE_INPUT);
//        updateUi();
    }
    @Override
    public void onResume(){
        super.onResume();
        updateUi();
    }
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_MANAGE_ASSIST;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mContextPref) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSIST_STRUCTURE_ENABLED,
                    (boolean) newValue ? 1 : 0);
            mHandler.post(() -> {
                guardScreenshotPref();
                guardFlashPref();
            });
            return true;
        }
        if (preference == mScreenshotPref) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSIST_SCREENSHOT_ENABLED,
                    (boolean) newValue ? 1 : 0);
            return true;
        }
        if (preference == mFlashPref) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ASSIST_DISCLOSURE_ENABLED,
                    (boolean) newValue ? 1 : 0);
            return true;
        }
/*
        if (preference == mDefaultAssitPref) {
            String newAssitPackage = (String)newValue;
            if (newAssitPackage == null ||
                    newAssitPackage.contentEquals(DefaultAssistPreference.ITEM_NONE_VALUE)) {
                setDefaultAssist(DefaultAssistPreference.ITEM_NONE_VALUE);
                return false;
            }
	}
*/
        return false;
    }

    private void guardScreenshotPref() {
        boolean isChecked = mContextPref.isChecked();
        boolean screenshotPrefWasSet = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.ASSIST_SCREENSHOT_ENABLED, 1) != 0;
        mScreenshotPref.setEnabled(isChecked);
        mScreenshotPref.setChecked(isChecked && screenshotPrefWasSet);
    }

    private void guardFlashPref() {
        ComponentName assistant = mAssistHelper.getCurrentAssist();

        boolean isContextChecked = mContextPref.isChecked();
        boolean willShowFlash = AssistUtils.shouldDisclose(getContext(), assistant);
        boolean isSystemAssistant = AssistUtils.isPreinstalledAssistant(getContext(), assistant);

        mFlashPref.setEnabled(isContextChecked && isSystemAssistant);
        mFlashPref.setChecked(willShowFlash);
    }

    private void updateUi() {
        mAssistHelper = CNDefaultAssistHelper.getInstance(getActivity());
        mAssistHelper.refreshAssistApps();
        mVoiceHelper = CNVoiceInputPrefHelper.getInstance(getActivity());
        mVoiceHelper.refreshVoiceInputs();
        final ComponentName currentAssist = mAssistHelper.getCurrentAssist();
        final boolean hasAssistant = currentAssist != null;
        mDefaultAssitPref.setSummary(mAssistHelper.getSummary());
        mVoiceInputPref.setSummary(mVoiceHelper.getSummary());
        if (hasAssistant) {
            getPreferenceScreen().addPreference(mContextPref);
            getPreferenceScreen().addPreference(mScreenshotPref);
        } else {
            getPreferenceScreen().removePreference(mContextPref);
            getPreferenceScreen().removePreference(mScreenshotPref);
            getPreferenceScreen().removePreference(mFlashPref);
        }

        if (hasAssistant && AssistUtils.allowDisablingAssistDisclosure(getContext())) {
            getPreferenceScreen().addPreference(mFlashPref);
        } else {
            getPreferenceScreen().removePreference(mFlashPref);
        }

        if (isCurrentAssistVoiceService()) {
            getPreferenceScreen().removePreference(mVoiceInputPref);
        } else {
            getPreferenceScreen().addPreference(mVoiceInputPref);
        }

        guardScreenshotPref();
        guardFlashPref();
    }


    private boolean isCurrentAssistVoiceService() {
        ComponentName currentAssist = mAssistHelper.getCurrentAssist();
        ComponentName activeService = mVoiceHelper.getCurrentService();
        return currentAssist == null && activeService == null ||
                currentAssist != null && currentAssist.equals(activeService);
    }


}
