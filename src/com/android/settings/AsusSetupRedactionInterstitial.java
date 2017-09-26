/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.settings.notification.RedactionInterstitial;
import com.asus.suw.lockscreen.AsusSuwUtilisClient;

/**
 * Setup Wizard's version of RedactionInterstitial screen. It inherits the logic and basic structure
 * from RedactionInterstitial class, and should remain similar to that behaviorally. This class
 * should only overload base methods for minor theme and behavior differences specific to Setup
 * Wizard. Other changes should be done to RedactionInterstitial class instead and let this class
 * inherit those changes.
 */
public class AsusSetupRedactionInterstitial extends RedactionInterstitial {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT,
                AsusSetupRedactionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AsusSetupRedactionInterstitialFragment.class.getName().equals(fragmentName);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getAsusTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    public static class AsusSetupRedactionInterstitialFragment extends RedactionInterstitialFragment
            implements View.OnClickListener{

        private AsusSuwUtilisClient mSuwClient;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

            mSuwClient = new AsusSuwUtilisClient(getActivity());
            ViewGroup layout = mSuwClient.getSetupWizardLayout_Short();

            mSuwClient.setSubContentView(R.layout.asus_setup_redaction_interstitial);
            mSuwClient.setHeaderText(R.string.lock_screen_notifications_interstitial_title);
            return layout;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            SetupWizardUtils.setImmersiveMode(getActivity());
            mSuwClient.initActivity(getActivity());

            Button nextNvaButton = getNavigationBarNextButton();
            if (nextNvaButton != null) {
                nextNvaButton.setOnClickListener(this);
            }

            Button backNvaButton = getNavigationBarBackButton();
            if (backNvaButton != null) {
                backNvaButton.setOnClickListener(this);
                backNvaButton.setVisibility(View.GONE);
            }
        }

        public void onNavigateBack() {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
            final AsusSetupRedactionInterstitial activity = (AsusSetupRedactionInterstitial) getActivity();
            if (activity != null) {
                activity.setResult(RESULT_OK, activity.getResultIntentData());
                finish();
            }
        }

        protected Button getNavigationBarBackButton(){
            return mSuwClient.getBackButton();
        }

        protected Button getNavigationBarNextButton(){
            return mSuwClient.getNextButton();
        }

        @Override
        public void onClick(View v) {
            if(v == getNavigationBarBackButton()) {
                onNavigateBack();
            }else if(v == getNavigationBarNextButton()) {
                onNavigateNext();
            }
        }
    }
}
