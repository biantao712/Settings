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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.widget.RecyclerView;
import android.support.v14.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;

import com.asus.suw.lockscreen.AsusSuwUtilisClient;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.fingerprint.SetupSkipDialog;


/**
 * Setup Wizard's version of ChooseLockGeneric screen. It inherits the logic and basic structure
 * from ChooseLockGeneric class, and should remain similar to that behaviorally. This class should
 * only overload base methods for minor theme and behavior differences specific to Setup Wizard.
 * Other changes should be done to ChooseLockGeneric class instead and let this class inherit
 * those changes.
 */
public class AsusSetupChooseLockGeneric extends ChooseLockGeneric {

    private static final String TAG ="SetupChooseLockGeneric";
    private static final String KEY_UNLOCK_SET_DO_LATER = "unlock_set_do_later";

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return AsusSetupChooseLockGenericFragment.class.getName().equals(fragmentName);
    }

    @Override
    /* package */ Class<? extends PreferenceFragment> getFragmentClass() {
        return AsusSetupChooseLockGenericFragment.class;
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

    public static class AsusSetupChooseLockGenericFragment extends ChooseLockGenericFragment
            implements View.OnClickListener {

        public static final String EXTRA_PASSWORD_QUALITY = ":settings:password_quality";
        private AsusSuwUtilisClient mSuwClient;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);

            RecyclerView listView = getListView();
            ((ViewGroup)listView.getParent()).removeView(listView);

            LinearLayout lLayour = new LinearLayout(getContext());
            lLayour.addView(listView);
            listView.setFocusable(false);

            mSuwClient = new AsusSuwUtilisClient(getActivity());
            ViewGroup layout = mSuwClient.getSetupWizardLayout();

            mSuwClient.setSubContentView(lLayour);

            if (!mForFingerprint) {
                mSuwClient.setHeaderText(R.string.setup_lock_settings_picker_title);
            } else {
                mSuwClient.setHeaderText(R.string.lock_settings_picker_title);
            }
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
                nextNvaButton.setEnabled(false);
            }
            Button backNvaButton = getNavigationBarBackButton();
            if (backNvaButton != null) {
                backNvaButton.setOnClickListener(this);
            }
        }

        protected TextView createHeaderView(int layout_id) {
            TextView message = (TextView) LayoutInflater.from(getActivity()).inflate(
                    layout_id, null, false);
            return message;
        }


        @Override
        protected void addHeaderView() {
            if (mForFingerprint) {
                setHeaderView(createHeaderView(R.layout.setup_choose_lock_generic_fingerprint_header));
            } else {
                setHeaderView(createHeaderView(R.layout.setup_choose_lock_generic_header));
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode != RESULT_CANCELED) {
                if (data == null) {
                    data = new Intent();
                }
                // Add the password quality extra to the intent data that will be sent back for
                // Setup Wizard.
                LockPatternUtils lockPatternUtils = new LockPatternUtils(getActivity());
                data.putExtra(EXTRA_PASSWORD_QUALITY,
                        lockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()));

                PackageManager packageManager = getPackageManager();
                ComponentName componentName = new ComponentName("com.android.settings",
                        "com.android.settings.SetupRedactionInterstitial");
                packageManager.setComponentEnabledSetting(componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                super.onActivityResult(requestCode, resultCode, data);
            }
            // If the started activity was cancelled (e.g. the user presses back), then this
            // activity will be resumed to foreground.
        }

        /***
         * Disables preferences that are less secure than required quality and shows only secure
         * screen lock options here.
         *
         * @param quality the requested quality.
         */
        @Override
        protected void disableUnusablePreferences(final int quality, boolean hideDisabled) {
            // At this part of the flow, the user has already indicated they want to add a pin,
            // pattern or password, so don't show "None" or "Slide". We disable them here and set
            // the HIDE_DISABLED flag to true to hide them. This only happens for setup wizard.
            // We do the following max check here since the device may already have a Device Admin
            // installed with a policy we need to honor.
            final int newQuality = Math.max(quality,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
            super.disableUnusablePreferencesImpl(newQuality, true /* hideDisabled */);
        }

        @Override
        protected void addPreferences() {
            if (mForFingerprint) {
                super.addPreferences();
            } else {
                addPreferencesFromResource(R.xml.setup_security_settings_picker);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();
            if (KEY_UNLOCK_SET_DO_LATER.equals(key)) {
                // show warning.
                SetupSkipDialog dialog = SetupSkipDialog.newInstance(getActivity().getIntent()
                        .getBooleanExtra(SetupSkipDialog.EXTRA_FRP_SUPPORTED, false));
                dialog.show(getFragmentManager());
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, boolean confirmCredentials, int userId) {
            final Intent intent = AsusSetupChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, confirmCredentials);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, long challenge, int userId) {
            final Intent intent = AsusSetupChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, challenge);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPasswordIntent(Context context, int quality, int minLength,
                final int maxLength, boolean requirePasswordToDecrypt, String password, int userId) {
            final Intent intent = AsusSetupChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, password);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final boolean confirmCredentials, int userId) {
            final Intent intent = AsusSetupChooseLockPattern.createIntent(context, requirePassword,
                    confirmCredentials);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                long challenge, int userId) {
            final Intent intent = AsusSetupChooseLockPattern.createIntent(context, requirePassword,
                    challenge);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final String pattern, int userId) {
            final Intent intent = AsusSetupChooseLockPattern.createIntent(context, requirePassword,
                    pattern);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        @Override
        protected Intent getEncryptionInterstitialIntent(Context context, int quality,
                boolean required, Intent unlockMethodIntent) {
            Intent intent = AsusSetupEncryptionInterstitial.createStartIntent(context, quality,
                    required, unlockMethodIntent);
            SetupWizardUtils.copySetupExtras(getActivity().getIntent(), intent);
            return intent;
        }

        public void onNavigateBack() {
            Activity activity = getActivity();
            if (activity != null) {
                activity.onBackPressed();
            }
        }

        public void onNavigateNext() {
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
