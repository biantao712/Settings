/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Origin from {@link MasterClear} <p>
 * Implement soft reset using MasterClear.java as an initial trial
 * soft reset -> reset settings only<br>
 *
 *  0. Change class name to ResetSettings and TAG to "ResetSettings"<br>
 *  1. Remove other checking<br>
 *     MircoSD, external storage, ... should not be erased<br>
 *  2. Remove unused items<br>
 *
 *  Note:
 *  For M, use {@link InstrumentedFragment}
 *  Keep keyguard validation
 * @since 2015/11/04
 * @author JimCC
 */
public class ResetSettings extends InstrumentedFragment {
    private static final String TAG = "ResetSettings";
    private static final boolean DEBUG = false;

    private static final int KEYGUARD_REQUEST = 55;

    private View mContentView;

    /**
     * Origin from {@link CryptKeeperSettings#runKeyguardConfirmation} <p>
     * Keyguard validation is run using the standard {@link ConfirmLockPattern}
     * component as a subactivity
     * @param request the request code to be returned once confirmation finishes
     * @return true if confirmation launched
     */
    private boolean runKeyguardConfirmation(int request) {
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivity(
                request, getText(R.string.reset_settings_title));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (KEYGUARD_REQUEST == requestCode && Activity.RESULT_OK == resultCode) {
            // If the user entered a valid keyguard trace, present the final
            // confirmation prompt; otherwise, go back to the initial state.
            showFinalConfirmation();
        }
    }

    /**
     * JimCC
     * for reset settings, check the parameters for args
     */
    private void showFinalConfirmation() {
        // need declare it (ResetSettingsConfirm) in AndroidManifest.xml
        ((SettingsActivity) getActivity()).startPreferencePanel(
                ResetSettingsConfirm.class.getName(), new Bundle(),
                R.string.reset_settings_confirm_title, null, null, 0);
    }

    /**
     * Reference {@link MasterClear#onCreateView}
     * Only the primary user (owner) can perform the reset settings
     * TODO
     * Check DISALLOW_FACTORY_RESET or not
     * Or, should we need to add DISALLOW_RESET_SETTINGS?
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        if (!Process.myUserHandle().isOwner()) {
            return inflater.inflate(R.layout.reset_settings_disallowed_screen, null);
        }
        mContentView = inflater.inflate(R.layout.reset_settings, null);
        mContentView.findViewById(R.id.initiate_reset_settings).setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        if (!runKeyguardConfirmation(
                                KEYGUARD_REQUEST)) {
                            showFinalConfirmation();
                        }
                    }
                }
        );

        return mContentView;
    }

    /**
     * TODO
     * Add a category in {@link com.android.internal.logging.MetricsLogger}
     * instead of in {@link com.android.settings.InstrumentedFragment}.
     */
    @Override
    protected int getMetricsCategory() {
        return RESET_SETTINGS;
    }
}
