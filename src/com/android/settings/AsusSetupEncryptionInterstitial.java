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
import android.support.v7.widget.RecyclerView;
import android.support.v7.preference.PreferenceRecyclerViewAccessibilityDelegate;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.asus.suw.lockscreen.AsusSuwUtilisClient;

import com.android.settings.utils.SettingsDividerItemDecoration;
import com.android.setupwizardlib.GlifPreferenceLayout;
import com.android.setupwizardlib.view.HeaderRecyclerView;

/**
 * Setup Wizard's version of EncryptionInterstitial screen. It inherits the logic and basic
 * structure from EncryptionInterstitial class, and should remain similar to that behaviorally. This
 * class should only overload base methods for minor theme and behavior differences specific to
 * Setup Wizard. Other changes should be done to EncryptionInterstitial class instead and let this
 * class inherit those changes.
 */
public class AsusSetupEncryptionInterstitial extends EncryptionInterstitial {

    public static Intent createStartIntent(Context ctx, int quality,
            boolean requirePasswordDefault, Intent unlockMethodIntent) {
        Intent startIntent = EncryptionInterstitial.createStartIntent(ctx, quality,
                requirePasswordDefault, unlockMethodIntent);
        startIntent.setClass(ctx, AsusSetupEncryptionInterstitial.class);
        startIntent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false)
                .putExtra(EXTRA_SHOW_FRAGMENT_TITLE_RESID, -1);
        return startIntent;
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT,
                AsusSetupEncryptionInterstitialFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AsusSetupEncryptionInterstitialFragment.class.getName().equals(fragmentName);
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

    public static class AsusSetupEncryptionInterstitialFragment extends EncryptionInterstitialFragment
            implements View.OnClickListener{

        private AsusSuwUtilisClient mSuwClient;
        private GlifPreferenceLayout mGlifLayout;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);

            if(view instanceof GlifPreferenceLayout) {
                mGlifLayout = (GlifPreferenceLayout) view;
            }

            mSuwClient = new AsusSuwUtilisClient(getActivity());
            ViewGroup layout = mSuwClient.getSetupWizardLayout_Short();

            return layout;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if(mGlifLayout != null) {
                mGlifLayout.setDividerItemDecoration(new SettingsDividerItemDecoration(getContext()));
                mGlifLayout.setDividerInset(getContext().getResources().getDimensionPixelSize(
                        R.dimen.suw_items_glif_icon_divider_inset));
                RecyclerView recyclerView = mGlifLayout.getRecyclerView();
                if (recyclerView instanceof HeaderRecyclerView) {
                    View header = ((HeaderRecyclerView) recyclerView).getHeader();
                    if (header != null) header.setVisibility(View.GONE);
                }
                // Use the dividers in SetupWizardRecyclerLayout. Suppress the dividers in
                // PreferenceFragment.
                setDivider(null);
            }

            RecyclerView list = getListView();
            ((ViewGroup)list.getParent()).removeView(list);

            mSuwClient.setSubContentView(list);
            mSuwClient.setHeaderText(R.string.encryption_interstitial_header);

            Button nextNvaButton = getNavigationBarNextButton();
            if (nextNvaButton != null) {
                nextNvaButton.setEnabled(false);
                nextNvaButton.setOnClickListener(this);
            }

            Button backNvaButton = getNavigationBarBackButton();
            if (backNvaButton != null) {
                backNvaButton.setOnClickListener(this);
            }

            Activity activity = getActivity();
            if (activity != null) {
                SetupWizardUtils.setImmersiveMode(activity);
                mSuwClient.initActivity(activity);
            }

        }

        @Override
        public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
                                                 Bundle savedInstanceState) {
            GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
            return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
        }

        public void onNavigateBack() {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
            final AsusSetupEncryptionInterstitial activity =
                    (AsusSetupEncryptionInterstitial) getActivity();
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
