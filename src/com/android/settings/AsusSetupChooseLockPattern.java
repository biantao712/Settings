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
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.asus.suw.lockscreen.AsusSuwUtilisClient;

/**
 * Setup Wizard's version of ChooseLockPattern screen. It inherits the logic and basic structure
 * from ChooseLockPattern class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockPattern class instead and let this class inherit
 * those changes.
 */
public class AsusSetupChooseLockPattern extends ChooseLockPattern {

    public static Intent createIntent(Context context, boolean requirePassword,
            boolean confirmCredentials) {
        Intent intent = ChooseLockPattern.createIntent(context, requirePassword,
                confirmCredentials, UserHandle.myUserId());
        intent.setClass(context, AsusSetupChooseLockPattern.class);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, String pattern) {
        Intent intent = ChooseLockPattern.createIntent(
                context, requirePassword, pattern, UserHandle.myUserId());
        intent.setClass(context, AsusSetupChooseLockPattern.class);
        return intent;
    }

    public static Intent createIntent(Context context, boolean requirePassword, long challenge) {
        Intent intent = ChooseLockPattern.createIntent(
                context, requirePassword, challenge, UserHandle.myUserId());
        intent.setClass(context, AsusSetupChooseLockPattern.class);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AsusSetupChooseLockPatternFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends Fragment> getFragmentClass() {
        return AsusSetupChooseLockPatternFragment.class;
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

    public static class AsusSetupChooseLockPatternFragment extends ChooseLockPatternFragment
            implements View.OnClickListener {

        private AsusSuwUtilisClient mSuwClient;
        private Button mRetryButton;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {;
            mSuwClient = new AsusSuwUtilisClient(getActivity());
            ViewGroup layout = mSuwClient.getSetupWizardLayout_Short();

            mSuwClient.setSubContentView(R.layout.asus_setup_choose_lock_pattern);
            mSuwClient.setHeaderText(getActivity().getTitle().toString());
            return layout;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            mRetryButton = (Button) view.findViewById(R.id.retryButton);
            mRetryButton.setOnClickListener(this);
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
            }
        }

        @Override
        protected Intent getRedactionInterstitialIntent(Context context) {
            return null;
        }

        @Override
        public void onClick(View v) {
            if (v == mRetryButton) {
                handleLeftButton();
            }else if(v == getNavigationBarBackButton()) {
                onNavigateBack();
            }else if(v == getNavigationBarNextButton()) {
                onNavigateNext();
            }else {
                super.onClick(v);
            }
        }

        @Override
        protected void setRightButtonEnabled(boolean enabled) {
            getNavigationBarNextButton().setEnabled(enabled);
        }

        @Override
        protected void setRightButtonText(int text) {
            getNavigationBarNextButton().setText(getText(text));
        }

        @Override
        protected void updateStage(Stage stage) {
            super.updateStage(stage);
            // Only enable the button for retry
            mRetryButton.setEnabled(stage == Stage.FirstChoiceValid);

            switch (stage) {
                case Introduction:
                case HelpScreen:
                case ChoiceTooShort:
                case FirstChoiceValid:
                    mRetryButton.setVisibility(View.VISIBLE);
                    break;
                case NeedToConfirm:
                case ConfirmWrong:
                case ChoiceConfirmed:
                    mRetryButton.setVisibility(View.INVISIBLE);
                    break;
            }
        }


        public void onNavigateBack() {
            final Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
            handleRightButton();
        }

        protected Button getNavigationBarBackButton(){
            return mSuwClient.getBackButton();
        }

        protected Button getNavigationBarNextButton(){
            return mSuwClient.getNextButton();
        }

    }
}
