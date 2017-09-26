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
 * limitations under the License.
 */

package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.asus.suw.lockscreen.AsusSuwUtilisClient;

/**
 * Setup Wizard's version of ChooseLockPassword screen. It inherits the logic and basic structure
 * from ChooseLockPassword class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPassword class instead and let this class inherit
 * those changes.
 */
public class AsusSetupChooseLockPassword extends ChooseLockPassword {

    public static Intent createIntent(Context context, int quality,
            int minLength, final int maxLength, boolean requirePasswordToDecrypt,
            boolean confirmCredentials) {
        Intent intent = ChooseLockPassword.createIntent(context, quality, minLength,
                maxLength, requirePasswordToDecrypt, confirmCredentials);
        intent.setClass(context, AsusSetupChooseLockPassword.class);
        intent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false);
        return intent;
    }

    public static Intent createIntent(Context context, int quality,
            int minLength, final int maxLength, boolean requirePasswordToDecrypt, String password) {
        Intent intent = ChooseLockPassword.createIntent(context, quality, minLength, maxLength,
                requirePasswordToDecrypt, password);
        intent.setClass(context, AsusSetupChooseLockPassword.class);
        intent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false);
        return intent;
    }

    public static Intent createIntent(Context context, int quality,
            int minLength, final int maxLength, boolean requirePasswordToDecrypt, long challenge) {
        Intent intent = ChooseLockPassword.createIntent(context, quality, minLength, maxLength,
                requirePasswordToDecrypt, challenge);
        intent.setClass(context, AsusSetupChooseLockPassword.class);
        intent.putExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AsusSetupChooseLockPasswordFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return AsusSetupChooseLockPasswordFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getAsusTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    public static class AsusSetupChooseLockPasswordFragment extends ChooseLockPasswordFragment
            implements View.OnClickListener {

        private AsusSuwUtilisClient mSuwClient;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mSuwClient = new AsusSuwUtilisClient(getActivity());
            ViewGroup layout = mSuwClient.getSetupWizardLayout_Short();
            mSuwClient.setSubContentView(R.layout.asus_setup_choose_lock_password);
            return layout;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            SetupWizardUtils.setImmersiveMode(getActivity());
            mSuwClient.initActivity(getActivity());
            mSuwClient.setHeaderText(getActivity().getTitle().toString());
            Button nextNvaButton = getNavigationBarNextButton();
            if (nextNvaButton != null) {
                nextNvaButton.setOnClickListener(this);
            }

            Button backNvaButton = getNavigationBarBackButton();
            if (backNvaButton != null) {
                backNvaButton.setOnClickListener(this);
            }
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            return null;
        }

        @Override
        protected void setNextEnabled(boolean enabled) {
            getNavigationBarNextButton().setEnabled(enabled);
        }

        @Override
        protected void setNextText(int text) {
            getNavigationBarNextButton().setText(getActivity().getText(text));
        }

        public void onNavigateBack() {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
            handleNext();
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
            }else{
                super.onClick(v);
            }
        }
    }
}
