/*
 * Copyright (C) 2010 The Android Open Source Project
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

import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD;
import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;
import static com.android.settings.ChooseLockPassword.ChooseLockPasswordFragment.RESULT_FINISHED;
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.security.KeyStore;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.EventLog;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.fingerprint.FingerprintEnrollBase;
import com.android.settings.fingerprint.FingerprintEnrollFindSensor;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.asus.settings.lockscreen.AsusLSUtils;
import com.android.settings.fingerprint.AsusUnlockSettings;

import java.util.List;
import java.util.ArrayList;
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import android.content.res.Resources;
import com.android.settings.search.Indexable;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import com.asus.cncommonres.AsusAlertDialogHelper;


public class ChooseLockGeneric extends SettingsActivity implements Indexable{
    public String TAG = "ChooseLockGeneric_NF";
    public static final String CONFIRM_CREDENTIALS = "confirm_credentials";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());

        String action = modIntent.getAction();
        if (DevicePolicyManager.ACTION_SET_NEW_PASSWORD.equals(action)
                || DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(action)) {
            modIntent.putExtra(EXTRA_HIDE_DRAWER, true);
        }
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        Log.d(TAG,"enter isValidFragment");
        if (ChooseLockGenericFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        Log.d(TAG,"Enter getFragmentClass");
        return ChooseLockGenericFragment.class;
    }

    public static class InternalActivity extends ChooseLockGeneric {
    }

    public static class ChooseLockGenericFragment extends SettingsPreferenceFragment  implements Indexable{
        private static final String TAG = "ChooseLockGenericFragment_NF";
        private static final int MIN_PASSWORD_LENGTH = 4;
        private static final String KEY_UNLOCK_SET_OFF = "unlock_set_off";
        private static final String KEY_UNLOCK_SET_NONE = "unlock_set_none";
        private static final String KEY_UNLOCK_SET_PIN = "unlock_set_pin";
        private static final String KEY_UNLOCK_SET_PASSWORD = "unlock_set_password";
        private static final String KEY_UNLOCK_SET_PATTERN = "unlock_set_pattern";
        private static final String KEY_UNLOCK_SET_MANAGED = "unlock_set_managed";
        private static final String KEY_SKIP_FINGERPRINT = "unlock_skip_fingerprint";
        private static final String PASSWORD_CONFIRMED = "password_confirmed";
        private static final String WAITING_FOR_CONFIRMATION = "waiting_for_confirmation";
        public static final String MINIMUM_QUALITY_KEY = "minimum_quality";
        public static final String HIDE_DISABLED_PREFS = "hide_disabled_prefs";
        public static final String ENCRYPT_REQUESTED_QUALITY = "encrypt_requested_quality";
        public static final String ENCRYPT_REQUESTED_DISABLED = "encrypt_requested_disabled";
        public static final String TAG_FRP_WARNING_DIALOG = "frp_warning_dialog";
        public static final String TAG_FRF_WARNING_DIALOG = "frf_warning_dialog";

        private static final int CONFIRM_EXISTING_REQUEST = 100;
        private static final int ENABLE_ENCRYPTION_REQUEST = 101;
        private static final int CHOOSE_LOCK_REQUEST = 102;
        private static final int CHOOSE_LOCK_BEFORE_FINGERPRINT_REQUEST = 103;
        private static final int SKIP_FINGERPRINT_REQUEST = 104;

        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private DevicePolicyManager mDPM;
        private KeyStore mKeyStore;
        private boolean mHasChallenge = false;
        private long mChallenge;
        private boolean mPasswordConfirmed = false;
        private boolean mWaitingForConfirmation = false;
        private int mEncryptionRequestQuality;
        private boolean mEncryptionRequestDisabled;
        private boolean mRequirePassword;
        private boolean mForChangeCredRequiredForBoot = false;
        private String mUserPassword;
        private LockPatternUtils mLockPatternUtils;
        private FingerprintManager mFingerprintManager;
        private int mUserId;
        private boolean mHideDrawer = false;
        private ManagedLockPasswordProvider mManagedPasswordProvider;
        private boolean mIsSetNewPassword = false;

        protected boolean mForFingerprint = false;
        //+ tim
        private boolean mKeepFingerprintWithoutPassword = false;
        //- tim

        @Override
        protected int getMetricsCategory() {
            return MetricsEvent.CHOOSE_LOCK_GENERIC;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            String chooseLockAction = getActivity().getIntent().getAction();
            Log.d(TAG,"Choose lock action is :" + chooseLockAction);
            mFingerprintManager =
                (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
            mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mKeyStore = KeyStore.getInstance();
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this.getActivity());
            mLockPatternUtils = new LockPatternUtils(getActivity());
            mIsSetNewPassword = ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(chooseLockAction)
                    || ACTION_SET_NEW_PASSWORD.equals(chooseLockAction);

            // Defaults to needing to confirm credentials
            final boolean confirmCredentials = getActivity().getIntent()
                .getBooleanExtra(CONFIRM_CREDENTIALS, true);
            if (getActivity() instanceof ChooseLockGeneric.InternalActivity) {
                mPasswordConfirmed = !confirmCredentials;
            }
            mHideDrawer = getActivity().getIntent().getBooleanExtra(EXTRA_HIDE_DRAWER, false);

            mHasChallenge = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
            mChallenge = getActivity().getIntent().getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            mForFingerprint = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, false);
            mForChangeCredRequiredForBoot = getArguments() != null && getArguments().getBoolean(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_CHANGE_CRED_REQUIRED_FOR_BOOT);

            if (savedInstanceState != null) {
                mPasswordConfirmed = savedInstanceState.getBoolean(PASSWORD_CONFIRMED);
                mWaitingForConfirmation = savedInstanceState.getBoolean(WAITING_FOR_CONFIRMATION);
                mEncryptionRequestQuality = savedInstanceState.getInt(ENCRYPT_REQUESTED_QUALITY);
                mEncryptionRequestDisabled = savedInstanceState.getBoolean(
                        ENCRYPT_REQUESTED_DISABLED);
            }

            int targetUser = Utils.getSecureTargetUser(
                    getActivity().getActivityToken(),
                    UserManager.get(getActivity()),
                    null,
                    getActivity().getIntent().getExtras()).getIdentifier();
//            if (ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(chooseLockAction)
//                    || !mLockPatternUtils.isSeparateProfileChallengeAllowed(targetUser)) {
            if (DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD.equals(
                    getActivity().getIntent().getAction()) ||
                    !mLockPatternUtils.isSeparateProfileChallengeAllowed(targetUser)) {
                // Always use parent if explicitely requested or if profile challenge is not
                // supported
                Bundle arguments = getArguments();
                mUserId = Utils.getUserIdFromBundle(getContext(), arguments != null ? arguments
                        : getActivity().getIntent().getExtras());
                Log.d(TAG,"user ID get from intent is " + mUserId);
            } else {
                mUserId = targetUser;
                Log.d(TAG,"Set user id from target user :" + mUserId);
            }
            if (mForFingerprint){
                getActivity().setTitle(R.string.cn_fingerprint);
            }else if (ACTION_SET_NEW_PASSWORD.equals(chooseLockAction)
            && DevicePolicyManager.ACTION_SET_NEW_PASSWORD
                    .equals(getActivity().getIntent().getAction())
                    && Utils.isManagedProfile(UserManager.get(getActivity()), mUserId)
                    && mLockPatternUtils.isSeparateProfileChallengeEnabled(mUserId)) {
                getActivity().setTitle(R.string.lock_settings_picker_title_profile);
            }

            mManagedPasswordProvider = ManagedLockPasswordProvider.get(getActivity(), mUserId);

            if (mPasswordConfirmed) {
                updatePreferencesOrFinish();
                if (mForChangeCredRequiredForBoot) {
                    maybeEnableEncryption(mLockPatternUtils.getKeyguardStoredPasswordQuality(
                            mUserId), false);
                }
            } else if (!mWaitingForConfirmation) {
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                boolean managedProfileWithUnifiedLock = Utils
                        .isManagedProfile(UserManager.get(getActivity()), mUserId)
                        && !mLockPatternUtils.isSeparateProfileChallengeEnabled(mUserId);
                if (managedProfileWithUnifiedLock
                        || !helper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST,
                        getString(R.string.unlock_set_unlock_launch_picker_title), true, mUserId)) {
                    mPasswordConfirmed = true; // no password set, so no need to confirm
                    updatePreferencesOrFinish();
                } else {
                    mWaitingForConfirmation = true;
                }
            }
            addHeaderView();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View root = super.onCreateView(inflater, container, savedInstanceState);
            root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
            return root;
        }

        protected void addHeaderView() {
            if (mForFingerprint) {
                setHeaderView(R.layout.choose_lock_generic_fingerprint_header);
                if (mIsSetNewPassword) {
                    ((TextView) getHeaderView().findViewById(R.id.fingerprint_header_description))
                            .setText(R.string.fingerprint_unlock_title);
                }

                // remove useless item of none and swipe.
                Log.d(TAG,"for finger print is true, remove off and none of password.");
                removePreference(KEY_UNLOCK_SET_OFF);
                removePreference(KEY_UNLOCK_SET_NONE);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();
            Log.d(TAG,"onPreferenceTreeClick key is " + key);
            // +[AMAX01][LiveDemo] TJ Tsai, 2016.08.22
            if (LiveDemoUnit.AccFlagRead() >= 1) {
                Log.d(TAG, "Demo app is running.");
                return true;
            }
            // -[AMAX01][LiveDemo] TJ Tsai, 2016.08.22

            //+ tim
			/*if (!isUnlockMethodSecure(key) && (mLockPatternUtils.isSecure(mUserId) || mFingerprintManager.hasEnrolledFingerprints(mUserId))) {
                // Show the disabling FRP warning only when the user is switching from a secure
                // unlock method to an insecure one
                showFactoryResetProtectionWarningDialog(key);
                return true;
            } else if (KEY_SKIP_FINGERPRINT.equals(key)) {
                Intent chooseLockGenericIntent = new Intent(getActivity(), ChooseLockGeneric.class);
                chooseLockGenericIntent.setAction(getIntent().getAction());
                chooseLockGenericIntent.putExtra(PASSWORD_CONFIRMED, mPasswordConfirmed);
                startActivityForResult(chooseLockGenericIntent, SKIP_FINGERPRINT_REQUEST);
                return true;
            } else {
                return setUnlockMethod(key);
            }*/
            boolean isHasFingerPrint = mFingerprintManager.hasEnrolledFingerprints(mUserId);
            Log.d(TAG,"is fingprint enable is :" + isHasFingerPrint + " in onPreferenceTreeClick");
            if (!isUnlockMethodSecure(key) && (mLockPatternUtils.isSecure(mUserId) || isHasFingerPrint))
            {
                // Show the disabling FRP warning only when the user is switching from a secure
                // unlock method to an insecure one
                Log.d(TAG,"Set to unsecure mode and device now is secure ,show FRP Dialog");
                showFactoryResetProtectionWarningDialog(key);
                return true;
            }
            else if (KEY_SKIP_FINGERPRINT.equals(key))
            {
                Log.d(TAG,"SKP_FINGERPRINT");
                Intent chooseLockGenericIntent = new Intent(getActivity(), ChooseLockGeneric.class);
                chooseLockGenericIntent.setAction(getIntent().getAction());
                chooseLockGenericIntent.putExtra(PASSWORD_CONFIRMED, mPasswordConfirmed);
                startActivityForResult(chooseLockGenericIntent, SKIP_FINGERPRINT_REQUEST);
                return true;
            }
            else {
                Log.d(TAG,"else set unlock method to " + key);
                return setUnlockMethod(key);
            }
        }

        /**
         * If the device has encryption already enabled, then ask the user if they
         * also want to encrypt the phone with this password.
         *
         * @param quality
         * @param disabled
         */
        // TODO: why does this take disabled, its always called with a quality higher than
        // what makes sense with disabled == true
        private void maybeEnableEncryption(int quality, boolean disabled) {
            Log.d(TAG,"Enter function maybeEnableEncryption,param :" + quality + " " + disabled);
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            if (UserManager.get(getActivity()).isAdminUser()
                    && mUserId == UserHandle.myUserId()
                    && LockPatternUtils.isDeviceEncryptionEnabled()
                    && !LockPatternUtils.isFileEncryptionEnabled()
                    && !dpm.getDoNotAskCredentialsOnBoot()
                    //++ Asus: verison request feature
                    && !Utils.isVerizonSKU()) {
                /*Log.d(TAG,"Enter if,show param:");
                Log.d(TAG,"UserManager.get(getActivity()).isAdminUser() : " + UserManager.get(getActivity()).isAdminUser());
                Log.d(TAG,"mUserId " + mUserId + " UserHandle.myUserId()" + UserHandle.myUserId() );
                Log.d(TAG,"LockPatternUtils.isDeviceEncryptionEnabled() :" + LockPatternUtils.isDeviceEncryptionEnabled());
                Log.d(TAG,"!LockPatternUtils.isFileEncryptionEnabled()" + !LockPatternUtils.isFileEncryptionEnabled());
                Log.d(TAG,"!dpm.getDoNotAskCredentialsOnBoot(): " + !dpm.getDoNotAskCredentialsOnBoot());
                Log.d(TAG,"!Utils.isVerizonSKU() :" + !Utils.isVerizonSKU());*/

                    //-- Asus: verison request feature
                mEncryptionRequestQuality = quality;
                mEncryptionRequestDisabled = disabled;
                // Get the intent that the encryption interstitial should start for creating
                // the new unlock method.
                Intent unlockMethodIntent = getIntentForUnlockMethod(quality, disabled);
                unlockMethodIntent.putExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_FOR_CHANGE_CRED_REQUIRED_FOR_BOOT,
                        mForChangeCredRequiredForBoot);
                final Context context = getActivity();
                // If accessibility is enabled and the user hasn't seen this dialog before, set the
                // default state to agree with that which is compatible with accessibility
                // (password not required).
                final boolean accEn = AccessibilityManager.getInstance(context).isEnabled();
                final boolean required = mLockPatternUtils.isCredentialRequiredToDecrypt(!accEn);
                Intent intent = getEncryptionInterstitialIntent(context, quality, required,
                        unlockMethodIntent);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT,
                        mForFingerprint);
                intent.putExtra(EXTRA_HIDE_DRAWER, mHideDrawer);
		Log.d(TAG,"startActivityForResult(intent, ENABLE_ENCRYPTION_REQUEST);");
				/*startActivityForResult(
                        intent,
                        mIsSetNewPassword && mHasChallenge
                                ? CHOOSE_LOCK_BEFORE_FINGERPRINT_REQUEST
                                : ENABLE_ENCRYPTION_REQUEST);*/
				startActivityForResult(intent, ENABLE_ENCRYPTION_REQUEST);
            } else {
                if (mForChangeCredRequiredForBoot) {
                    Log.d(TAG,"if (mForChangeCredRequiredForBoot) {");
                    // Welp, couldn't change it. Oh well.
                    finish();
                    return;
                }
                boolean isRequirePwdInSetting = mLockPatternUtils.isCredentialRequiredToDecrypt(EncryptionInterstitial.DEFAULT_REQUIRE_PASSWORD);
                boolean otherCondition = Process.myUserHandle().isOwner()
                        && LockPatternUtils.isDeviceEncryptionEnabled()
                        && !dpm.getDoNotAskCredentialsOnBoot();
                Log.d(TAG,"isRequirePwdInSetting:"+isRequirePwdInSetting+";other:"+otherCondition+";isOwn:"+Process.myUserHandle().isOwner()+";dna:"+ dpm.getDoNotAskCredentialsOnBoot());
                mRequirePassword = isRequirePwdInSetting && otherCondition; // device encryption not enabled or not device owner
                updateUnlockMethodAndFinish(quality, disabled);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            mWaitingForConfirmation = false;
            if (requestCode == CONFIRM_EXISTING_REQUEST && resultCode == Activity.RESULT_OK) {
                mPasswordConfirmed = true;
                mUserPassword = data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                updatePreferencesOrFinish();
                if (mForChangeCredRequiredForBoot) {
                    if (!TextUtils.isEmpty(mUserPassword)) {
                        maybeEnableEncryption(
                                mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId), false);
                    } else {
                        finish();
                    }
                }
            } else if (requestCode == CHOOSE_LOCK_REQUEST
                    || requestCode == ENABLE_ENCRYPTION_REQUEST) {
                if (resultCode != RESULT_CANCELED || mForChangeCredRequiredForBoot) {
                    getActivity().setResult(resultCode, data);
                    finish();
                }
            } else if (requestCode == CHOOSE_LOCK_BEFORE_FINGERPRINT_REQUEST
                    && resultCode == FingerprintEnrollBase.RESULT_FINISHED) {
                Intent intent = new Intent(getActivity(), FingerprintEnrollFindSensor.class);
                if (data != null) {
                    intent.putExtras(data.getExtras());
                }
                startActivity(intent);
                finish();
            } else if (requestCode == SKIP_FINGERPRINT_REQUEST) {
                if (resultCode != RESULT_CANCELED) {
                    getActivity().setResult(
                            resultCode == RESULT_FINISHED ? RESULT_OK : resultCode, data);
                    finish();
                }
            } else {
                getActivity().setResult(Activity.RESULT_CANCELED);
                finish();
            }
            if (requestCode == Activity.RESULT_CANCELED && mForChangeCredRequiredForBoot) {
                finish();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            // Saved so we don't force user to re-enter their password if configuration changes
            outState.putBoolean(PASSWORD_CONFIRMED, mPasswordConfirmed);
            outState.putBoolean(WAITING_FOR_CONFIRMATION, mWaitingForConfirmation);
            outState.putInt(ENCRYPT_REQUESTED_QUALITY, mEncryptionRequestQuality);
            outState.putBoolean(ENCRYPT_REQUESTED_DISABLED, mEncryptionRequestDisabled);
        }

        private void updatePreferencesOrFinish() {
            Intent intent = getActivity().getIntent();
            int quality = intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, -1);
            if (quality == -1) {
                // If caller didn't specify password quality, show UI and allow the user to choose.
                quality = intent.getIntExtra(MINIMUM_QUALITY_KEY, -1);
                quality = upgradeQuality(quality);
                final boolean hideDisabledPrefs = intent.getBooleanExtra(
                        HIDE_DISABLED_PREFS, false);
                final PreferenceScreen prefScreen = getPreferenceScreen();
                if (prefScreen != null) {
                    prefScreen.removeAll();
                }
                addPreferences();
                disableUnusablePreferences(quality, hideDisabledPrefs);
                updatePreferenceText();
                updateCurrentPreference();
                updatePreferenceSummaryIfNeeded();
            } else {
                updateUnlockMethodAndFinish(quality, false);
            }
        }

        protected void addPreferences() {
            addPreferencesFromResource(R.xml.security_settings_picker);

            // Used for testing purposes
            findPreference(KEY_UNLOCK_SET_NONE).setViewId(R.id.lock_none);
            //findPreference(KEY_SKIP_FINGERPRINT).setViewId(R.id.lock_none);
            findPreference(KEY_UNLOCK_SET_PIN).setViewId(R.id.lock_pin);
            findPreference(KEY_UNLOCK_SET_PASSWORD).setViewId(R.id.lock_password);
        }

        private void updatePreferenceText() {
            if (mForFingerprint) {
                final String key[] = { KEY_UNLOCK_SET_PATTERN,
                        KEY_UNLOCK_SET_PIN,
                        KEY_UNLOCK_SET_PASSWORD };
                final int res[] = { R.string.fingerprint_unlock_set_unlock_pattern,
                        R.string.fingerprint_unlock_set_unlock_pin,
                        R.string.fingerprint_unlock_set_unlock_password };
                for (int i = 0; i < key.length; i++) {
                    Preference pref = findPreference(key[i]);
                    if (pref != null) { // can be removed by device admin
                        pref.setTitle(res[i]);
                        pref.setLayoutResource(R.layout.cnasusres_preference_parent);
                    }
                }
            }

            if (mManagedPasswordProvider.isSettingManagedPasswordSupported()) {
                Preference managed = findPreference(KEY_UNLOCK_SET_MANAGED);
                managed.setTitle(mManagedPasswordProvider.getPickerOptionTitle(mForFingerprint));
            } else {
                removePreference(KEY_UNLOCK_SET_MANAGED);
            }

            if (!(mForFingerprint && mIsSetNewPassword)) {
                removePreference(KEY_SKIP_FINGERPRINT);
            }
        }

        private void updateCurrentPreference() {
            String currentKey = getKeyForCurrent();
            Preference preference = findPreference(currentKey);
            if (preference != null) {
                preference.setSummary(R.string.current_screen_lock);
            }
        }

        private String getKeyForCurrent() {
            final int credentialOwner = UserManager.get(getContext())
                    .getCredentialOwnerProfile(mUserId);
            if (mLockPatternUtils.isLockScreenDisabled(credentialOwner)) {
                return KEY_UNLOCK_SET_OFF;
            }
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(credentialOwner)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return KEY_UNLOCK_SET_PATTERN;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return KEY_UNLOCK_SET_PIN;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    return KEY_UNLOCK_SET_PASSWORD;
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    return KEY_UNLOCK_SET_MANAGED;
                case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                    return KEY_UNLOCK_SET_NONE;
            }
            return null;
        }

        /** increases the quality if necessary */
        private int upgradeQuality(int quality) {
            quality = upgradeQualityForDPM(quality);
            return quality;
        }

        private int upgradeQualityForDPM(int quality) {
            // Compare min allowed password quality
            int minQuality = mDPM.getPasswordQuality(null, mUserId);
            if (quality < minQuality) {
                quality = minQuality;
            }
            return quality;
        }

        /***
         * Disables preferences that are less secure than required quality. The actual
         * implementation is in disableUnusablePreferenceImpl.
         *
         * @param quality the requested quality.
         * @param hideDisabledPrefs if false preferences show why they were disabled; otherwise
         * they're not shown at all.
         */
        protected void disableUnusablePreferences(final int quality, boolean hideDisabledPrefs) {
            disableUnusablePreferencesImpl(quality, hideDisabledPrefs);
        }

        /***
         * Disables preferences that are less secure than required quality.
         *
         * @param quality the requested quality.
         * @param hideDisabled whether to hide disable screen lock options.
         */
        protected void disableUnusablePreferencesImpl(final int quality,
                boolean hideDisabled) {
            final PreferenceScreen entries = getPreferenceScreen();

            int adminEnforcedQuality = mDPM.getPasswordQuality(null, mUserId);
            EnforcedAdmin enforcedAdmin = RestrictedLockUtils.checkIfPasswordQualityIsSet(
                    getActivity(), mUserId);
            for (int i = entries.getPreferenceCount() - 1; i >= 0; --i) {
                Preference pref = entries.getPreference(i);
                if (pref instanceof RestrictedPreference) {
                    final String key = pref.getKey();
                    boolean enabled = true;
                    boolean visible = true;
                    boolean disabledByAdmin = false;
                    if (KEY_UNLOCK_SET_OFF.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                        if (getResources().getBoolean(R.bool.config_hide_none_security_option)) {
                            enabled = false;
                            visible = false;
                        }
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                    } else if (KEY_UNLOCK_SET_NONE.equals(key)) {
                        if (mUserId != UserHandle.myUserId()) {
                            // Swipe doesn't make sense for profiles.
                            visible = false;
                        }
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                    } else if (KEY_UNLOCK_SET_PATTERN.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
                    } else if (KEY_UNLOCK_SET_PIN.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
                    } else if (KEY_UNLOCK_SET_PASSWORD.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
                    } else if (KEY_UNLOCK_SET_MANAGED.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_MANAGED
                                && mManagedPasswordProvider.isManagedPasswordChoosable();
                        disabledByAdmin = adminEnforcedQuality
                                > DevicePolicyManager.PASSWORD_QUALITY_MANAGED;
                    }
                    if (hideDisabled) {
                        visible = enabled;
                    }
                    if (!visible) {
                        entries.removePreference(pref);
                    } else if (disabledByAdmin && enforcedAdmin != null) {
                        ((RestrictedPreference) pref).setDisabledByAdmin(enforcedAdmin);
                    } else if (!enabled) {
                        // we need to setDisabledByAdmin to null first to disable the padlock
                        // in case it was set earlier.
                        ((RestrictedPreference) pref).setDisabledByAdmin(null);
                        pref.setSummary(R.string.unlock_set_unlock_disabled_summary);
                        pref.setEnabled(false);
                    } else {
                        ((RestrictedPreference) pref).setDisabledByAdmin(null);
                    }
                }
            }
        }

        private void updatePreferenceSummaryIfNeeded() {
            // On a default block encrypted device with accessibility, add a warning
            // that your data is not credential encrypted
            if (!StorageManager.isBlockEncrypted()) {
                return;
            }

            if (StorageManager.isNonDefaultBlockEncrypted()) {
                return;
            }

            if (AccessibilityManager.getInstance(getActivity()).getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK).isEmpty()) {
                return;
            }

            // remove this for new feature remove boot check password.
//            CharSequence summary = getString(R.string.secure_lock_encryption_warning);
//
//            PreferenceScreen screen = getPreferenceScreen();
//            final int preferenceCount = screen.getPreferenceCount();
//            for (int i = 0; i < preferenceCount; i++) {
//                Preference preference = screen.getPreference(i);
//                switch (preference.getKey()) {
//                    case KEY_UNLOCK_SET_PATTERN:
//                    case KEY_UNLOCK_SET_PIN:
//                    case KEY_UNLOCK_SET_PASSWORD:
//                    case KEY_UNLOCK_SET_MANAGED: {
//                        preference.setSummary(summary);
//                    } break;
//                }
//            }
        }

        protected Intent getLockManagedPasswordIntent(boolean requirePassword, String password) {
            return mManagedPasswordProvider.createIntent(requirePassword, password);
        }

        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, boolean confirmCredentials, int userId) {
            Log.d(TAG,"get lock password intent is call 1");
            return ChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, confirmCredentials, userId);
        }

        protected Intent getLockPasswordIntent(Context context, int quality,
                int minLength, final int maxLength,
                boolean requirePasswordToDecrypt, long challenge, int userId) {
            Log.d(TAG,"get lock password intent is call 2");
            return ChooseLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, challenge, userId);
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength,
                int maxLength, boolean requirePasswordToDecrypt, String password, int userId) {
            Log.d(TAG,"get lock password intent is call 3");
            return ChooseLockPassword.createIntent(context, quality, minLength, maxLength,
                    requirePasswordToDecrypt, password, userId);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final boolean confirmCredentials, int userId) {
            return ChooseLockPattern.createIntent(context, requirePassword,
                    confirmCredentials, userId);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
               long challenge, int userId) {
            return ChooseLockPattern.createIntent(context, requirePassword, challenge, userId);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                final String pattern, int userId) {
            return ChooseLockPattern.createIntent(context, requirePassword, pattern, userId);
        }

        protected Intent getEncryptionInterstitialIntent(Context context, int quality,
                boolean required, Intent unlockMethodIntent) {
            return EncryptionInterstitial.createStartIntent(context, quality, required,
                    unlockMethodIntent);
        }

        /**
         * Invokes an activity to change the user's pattern, password or PIN based on given quality
         * and minimum quality specified by DevicePolicyManager. If quality is
         * {@link DevicePolicyManager#PASSWORD_QUALITY_UNSPECIFIED}, password is cleared.
         *
         * @param quality the desired quality. Ignored if DevicePolicyManager requires more security
         * @param disabled whether or not to show LockScreen at all. Only meaningful when quality is
         * {@link DevicePolicyManager#PASSWORD_QUALITY_UNSPECIFIED}
         */
        void updateUnlockMethodAndFinish(int quality, boolean disabled) {
            Log.d(TAG,"enter update UnlockMethodAndFinish ,param :" + quality + " " + disabled);
            // Sanity check. We should never get here without confirming user's existing password.
            if (!mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }

            quality = upgradeQuality(quality);
            Intent intent = getIntentForUnlockMethod(quality, disabled);
            if (intent != null) {
	    	Log.d(TAG,"intent is not null");
			/*startActivityForResult(intent,
                        mIsSetNewPassword && mHasChallenge
                                ? CHOOSE_LOCK_BEFORE_FINGERPRINT_REQUEST
                                : CHOOSE_LOCK_REQUEST);*/
			Log.d(TAG,"intent is not null");
			startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
                return;
            }
            else
            {
                Log.d(TAG,"getIntentForUnlockMethod is null");
            }

            if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                Log.d(TAG,"qulait is : DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED");
                mLockPatternUtils.setSeparateProfileChallengeEnabled(mUserId, true, mUserPassword);
                mChooseLockSettingsHelper.utils().clearLock(mUserId);
                mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled, mUserId);
                Log.d(TAG,"call remove AllFingerprintForUserAndFinish with pararm : " + mUserId);
                removeAllFingerprintForUserAndFinish(mUserId);
                getActivity().setResult(Activity.RESULT_OK);
            } else {
                Log.d(TAG,"esle call remove AllFingerprintForUserAndFinish ,with param :" + mUserId);
                removeAllFingerprintForUserAndFinish(mUserId);
            }
        }

        private Intent getIntentForUnlockMethod(int quality, boolean disabled) {
            Intent intent = null;
            final Context context = getActivity();
            if (quality >= DevicePolicyManager.PASSWORD_QUALITY_MANAGED) {
                intent = getLockManagedPasswordIntent(mRequirePassword, mUserPassword);
            } else if (quality >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
                int minLength = mDPM.getPasswordMinimumLength(null, mUserId);
                if (minLength < MIN_PASSWORD_LENGTH) {
                    minLength = MIN_PASSWORD_LENGTH;
                }
                int maxLength = mDPM.getPasswordMaximumLength(quality);
                Log.d(TAG,"max length get from dpm is :" + maxLength);
                Log.d(TAG,"Force set max length to 17");
                maxLength = 17;
//                Log.d(TAG,"set max length force to 17");
//                mDPM.setPasswordMaximumLength(quality);
                if (mHasChallenge) {
                    Log.d(TAG,"mHasChallenge is called");
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mChallenge, mUserId);
                } else {
                    Log.d(TAG,"not mHasChallenge is called");
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mUserPassword, mUserId);
                }
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                if (mHasChallenge) {
                    intent = getLockPatternIntent(context, mRequirePassword,
                            mChallenge, mUserId);
                } else {
                    intent = getLockPatternIntent(context, mRequirePassword,
                            mUserPassword, mUserId);
                }
            }
            if (intent != null) {
                intent.putExtra(EXTRA_HIDE_DRAWER, mHideDrawer);
            }
            return intent;
        }

        //+ tim
        private void removeAllFingerprintForUserAndFinish(final int userId) {
            Log.d(TAG, "remove AllFingerprintForUserAndFinish mKeep FingerprintWithoutPassword = " + mKeepFingerprintWithoutPassword);
        	//disable unlock device switch
        	Settings.Secure.putInt(getContentResolver(), AsusUnlockSettings.UNLOCK_DEVICE_ENABLED, 0);
        	//delete fingerprint if mKeepFingerprintWithoutPassword is false
            if (!mKeepFingerprintWithoutPassword && mFingerprintManager != null && mFingerprintManager.isHardwareDetected()) {
                Log.d(TAG,"keep is false and hard ware detected.");
                if (mFingerprintManager.hasEnrolledFingerprints(userId)) {
                    Log.d(TAG,"exit fingerpint");
                    mFingerprintManager.setActiveUser(userId);
                    // For the purposes of M and N, groupId is the same as userId.
                    final int groupId = userId;
                    Fingerprint finger = new Fingerprint(null, groupId, 0, 0);
                    mFingerprintManager.remove(finger, userId,
                            new RemovalCallback() {
                                @Override
                                public void onRemovalError(Fingerprint fp, int errMsgId,
                                        CharSequence errString) {
                                    Log.v(TAG, "Fingerprint onRemovalError removed: " + fp.getFingerId());
                                    if (fp.getFingerId() == 0) {
                                        Log.d(TAG,"call remove managed profilefingerprintandfinishifnecessagey");
                                        removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                                    }
                                }

                                @Override
                                public void onRemovalSucceeded(Fingerprint fingerprint) {
                                    Log.d(TAG,"onRemovalSucceeded");
                                    if (fingerprint.getFingerId() == 0) {
                                        Log.d(TAG,"call remove managedprofilefiingerprintsandfinishifnecessage in func onremovalsuccessed");
                                        removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                                    }
                                }
                            });
                } else {
                    Log.d(TAG,"Not exit fingerprint of current user,call remove ManagedProfileFingerprintsAndFinishIfNecessary");
                    // No fingerprints in this user, we may also want to delete managed profile
                    // fingerprints
                    removeManagedProfileFingerprintsAndFinishIfNecessary(userId);
                }
            } else {
                Log.d(TAG,"else not remove finger print");
                // The removal callback will call finish, once all fingerprints are removed.
                // We need to wait for that to occur, otherwise, the UI will still show that
                // fingerprints exist even though they are (about to) be removed depending on
                // the race condition.
                if (!mForFingerprint)
                    finish();
            }
        }

        private void removeManagedProfileFingerprintsAndFinishIfNecessary(final int parentUserId) {
            Log.d(TAG,"remove ManagedProfileFingerprintsAndFinishIfNecessary, param parentUserId " + parentUserId);
            //++ Asus: fix activity is null crash
            Activity activity = getActivity();
            if(activity == null){
                Log.e(TAG, "remove ManagedProfileFingerprintsAndFinishIfNecessary activity is null");
                return;
            }
            //-- Asus: fix activity is null crash
            mFingerprintManager.setActiveUser(UserHandle.myUserId());
            final UserManager um = UserManager.get(getActivity());
            boolean hasChildProfile = false;
            if (!um.getUserInfo(parentUserId).isManagedProfile()) {
                // Current user is primary profile, remove work profile fingerprints if necessary
                final List<UserInfo> profiles = um.getProfiles(parentUserId);
                final int profilesSize = profiles.size();
                for (int i = 0; i < profilesSize; i++) {
                    final UserInfo userInfo = profiles.get(i);
                    if (userInfo.isManagedProfile() && !mLockPatternUtils
                            .isSeparateProfileChallengeEnabled(userInfo.id)) {
                        Log.d(TAG,"call remove AllFingerprintForUserAndFinish with param :" + userInfo.id);
                        removeAllFingerprintForUserAndFinish(userInfo.id);
                        hasChildProfile = true;
                        break;
                    }
                }
            }
            if (!hasChildProfile) {
                if (!mForFingerprint)
                    finish();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_choose_lockscreen;
        }

        private int getResIdForFactoryResetProtectionWarningTitle() {
            boolean isProfile = Utils.isManagedProfile(UserManager.get(getActivity()), mUserId);
            return isProfile ? R.string.unlock_disable_frp_warning_title_profile
                    : R.string.unlock_disable_frp_warning_title;
        }

        private int getResIdForFactoryResetProtectionWarningMessage() {
            boolean hasFingerprints = mFingerprintManager.hasEnrolledFingerprints(mUserId);
            boolean isProfile = Utils.isManagedProfile(UserManager.get(getActivity()), mUserId);
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    if (hasFingerprints && isProfile) {
                        return R.string
                                .unlock_disable_frp_warning_content_pattern_fingerprint_profile_nf;
                    } else if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pattern_fingerprint_nf;
                    } else if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pattern_profile_nf;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_pattern_nf;
                    }
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    if (hasFingerprints && isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_fingerprint_profile_nf;
                    } else if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_fingerprint_nf;
                    } else if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_pin_profile_nf;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_pin_nf;
                    }
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    if (hasFingerprints && isProfile) {
                        return R.string
                                .unlock_disable_frp_warning_content_password_fingerprint_profile_nf;
                    } else if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_password_fingerprint_nf;
                    } else if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_password_profile_nf;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_password_nf;
                    }
                default:
                    if (hasFingerprints && isProfile) {
                        return R.string
                                .unlock_disable_frp_warning_content_unknown_fingerprint_profile_nf;
                    } else if (hasFingerprints && !isProfile) {
                        return R.string.unlock_disable_frp_warning_content_unknown_fingerprint_nf;
                    } else if (isProfile) {
                        return R.string.unlock_disable_frp_warning_content_unknown_profile_nf;
                    } else {
                        return R.string.unlock_disable_frp_warning_content_unknown_nf;
                    }
            }
        }

        private boolean isUnlockMethodSecure(String unlockMethod) {
            return !(KEY_UNLOCK_SET_OFF.equals(unlockMethod) ||
                    KEY_UNLOCK_SET_NONE.equals(unlockMethod));
        }

        private boolean setUnlockMethod(String unlockMethod) {
            Log.d(TAG,"enter function setUnlockMethod, target unlockmethod is : " + unlockMethod);
            EventLog.writeEvent(EventLogTags.LOCK_SCREEN_TYPE, unlockMethod);
            boolean isHasFingerPrint = mFingerprintManager.hasEnrolledFingerprints(mUserId);
            boolean isSecureBefore = mLockPatternUtils.isSecure(mUserId);

            Log.d(TAG,"setUnlockMethod to : " + unlockMethod + " isHasFingerPrint:" + isHasFingerPrint + " isSecureBefore:" + isSecureBefore);

            if (KEY_UNLOCK_SET_OFF.equals(unlockMethod)) {
                updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, true /* disabled */ );
            } else if (KEY_UNLOCK_SET_NONE.equals(unlockMethod)) {
                updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, false /* disabled */ );
            } else if (KEY_UNLOCK_SET_MANAGED.equals(unlockMethod)) {
                maybeEnableEncryption(DevicePolicyManager.PASSWORD_QUALITY_MANAGED, false);
            } else if (KEY_UNLOCK_SET_PATTERN.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, false);
            } else if (KEY_UNLOCK_SET_PIN.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC, false);
            } else if (KEY_UNLOCK_SET_PASSWORD.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC, false);
            } else {
                Log.e(TAG, "Encountered unknown unlock method to set: " + unlockMethod);
                return false;
            }
            return true;
        }

        private boolean setFingerPrintUnlockDeviceStatus(boolean isEnable)
        {
            try {
                int status = isEnable ? 1 : 0;
                Settings.Secure.putInt(getContentResolver(), AsusUnlockSettings.UNLOCK_DEVICE_ENABLED, status);
            }
            catch(Exception ee)
            {
                Log.d(TAG,"Exception in setFingerPrintUnlockDevcieStatus : " + isEnable);
            }
            return true;
        }
	
        private void showFactoryResetProtectionWarningDialog(String unlockMethodToSet) {
            boolean isHasFingerPrint = mFingerprintManager.hasEnrolledFingerprints(mUserId);
            Log.d(TAG, "hasEnrolledFingerprints is " + isHasFingerPrint);
//            if (isHasFingerPrint)
//            {
//                Log.d(TAG,"Show warning dialog of set an new unlock method from non secure status with finger print before");
//                FactoryResetFingerprintWarningDialog fdialog =
//                        FactoryResetFingerprintWarningDialog.newInstance(unlockMethodToSet);
//                fdialog.show(getChildFragmentManager(), TAG_FRF_WARNING_DIALOG);
//                return;
//            }
//            else
//            {
                Log.d(TAG,"Show Warning dialog of remove unlock method from secure status.");
                Log.d(TAG, "disable unlock device of fingerprint");
                //setFingerPrintUnlockDeviceStatus(false);
                int title = getResIdForFactoryResetProtectionWarningTitle();
                int message = getResIdForFactoryResetProtectionWarningMessage();
                Log.d(TAG, "remove lock method warnging title: " + title + " Message is :" + message);
                FactoryResetProtectionWarningDialog dialog =
                        FactoryResetProtectionWarningDialog.newInstance(
                                title, message, unlockMethodToSet);
                dialog.show(getChildFragmentManager(), TAG_FRP_WARNING_DIALOG);
                return ;
//            }
        }

        public static class FactoryResetProtectionWarningDialog extends DialogFragment {

            private static final String ARG_TITLE_RES = "titleRes";
            private static final String ARG_MESSAGE_RES = "messageRes";
            private static final String ARG_UNLOCK_METHOD_TO_SET = "unlockMethodToSet";

            public static FactoryResetProtectionWarningDialog newInstance(
                    int titleRes, int messageRes, String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag =
                        new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
                args.putInt(ARG_TITLE_RES, titleRes);
                args.putInt(ARG_MESSAGE_RES, messageRes);
                args.putString(ARG_UNLOCK_METHOD_TO_SET, unlockMethodToSet);
                frag.setArguments(args);
                return frag;
            }

            @Override
            public void show(FragmentManager manager, String tag) {
                if (manager.findFragmentByTag(tag) == null) {
                    // Prevent opening multiple dialogs if tapped on button quickly
                    super.show(manager, tag);
                }
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Bundle args = getArguments();
                View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
                TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
                title.setText(args.getInt(ARG_TITLE_RES));

                TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
                message.setText(args.getInt(ARG_MESSAGE_RES));
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setView(view1)
//                        .setTitle(args.getInt(ARG_TITLE_RES))
//                        .setMessage(args.getInt(ARG_MESSAGE_RES))
                        .setPositiveButton(R.string.unlock_disable_frp_warning_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.d(TAG,"positive button clicked,set keep finger print ture");
                                        ChooseLockGenericFragment parentFragment = (ChooseLockGenericFragment) getParentFragment();
//                                        parentFragment.mKeepFingerprintWithoutPassword = true;
//                                        parentFragment.setFingerPrintUnlockDeviceStatus(false);
                                        parentFragment.setUnlockMethod(args.getString(ARG_UNLOCK_METHOD_TO_SET));
                                        AsusLSUtils.sendIntentForSecureStartUpStateChanged(getContext(), false);
                                    }
                                }
                        )
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Log.d(TAG,"Negative button clicked");
                                        dismiss();
                                    }
                                }
                        );
                        //.create();

                AlertDialog dialog = builder.show();
                Window dialogWindow = dialog.getWindow();
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.BOTTOM;
                dialogWindow.setAttributes(lp);
                return dialog;
            }
        }
        
        //+ tim
        public static class FactoryResetFingerprintWarningDialog extends DialogFragment {

            private static final String ARG_UNLOCK_METHOD_TO_SET = "unlockMethodToSet";

            public static FactoryResetFingerprintWarningDialog newInstance(String unlockMethodToSet) {
            	FactoryResetFingerprintWarningDialog frag =
                        new FactoryResetFingerprintWarningDialog();
                Bundle args = new Bundle();
                args.putString(ARG_UNLOCK_METHOD_TO_SET, unlockMethodToSet);
                frag.setArguments(args);
                return frag;
            }

            @Override
            public void show(FragmentManager manager, String tag) {
                if (manager.findFragmentByTag(tag) == null) {
                    // Prevent opening multiple dialogs if tapped on button quickly
                    super.show(manager, tag);
                }
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Bundle args = getArguments();
                String unlockMethodToSet = args.getString(ARG_UNLOCK_METHOD_TO_SET);
                
                View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
                TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
                title.setText(getActivity().getResources().getString(R.string.choose_lock_fingerprint_warning_dialog_title));
                
                TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
                message.setText(getActivity().getResources().getString(R.string.choose_lock_fingerprint_warning_dialog_msg));

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                		.setView(view1)
//                        .setTitle(R.string.choose_lock_fingerprint_warning_dialog_title)
//                        .setMessage(R.string.choose_lock_fingerprint_warning_dialog_msg)
                        .setPositiveButton(R.string.choose_lock_fingerprint_warning_dialog_delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    	ChooseLockGenericFragment parentFragment = (ChooseLockGenericFragment) getParentFragment();
                                    	parentFragment.mKeepFingerprintWithoutPassword = false;
                                        parentFragment.setFingerPrintUnlockDeviceStatus(false);
                                        parentFragment.setUnlockMethod(args.getString(ARG_UNLOCK_METHOD_TO_SET));
                                        Log.d(TAG, "button delete clicked,exist finger print and remove them.");
                                        AsusLSUtils.sendIntentForSecureStartUpStateChanged(getContext(), false);
                                    }
                                }
                        )
                        .setNegativeButton(R.string.choose_lock_fingerprint_warning_dialog_keep,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    	//keep
                                        Log.d(TAG,"button keep clicked,Exit fingerprint and not remove it,and enable it to unlock device");

                                    	ChooseLockGenericFragment parentFragment = (ChooseLockGenericFragment) getParentFragment();
                                    	parentFragment.mKeepFingerprintWithoutPassword = true;
                                        //parentFragment.setFingerPrintUnlockDeviceStatus(false);
                                    	parentFragment.setUnlockMethod(args.getString(ARG_UNLOCK_METHOD_TO_SET));
                                        AsusLSUtils.sendIntentForSecureStartUpStateChanged(getContext(), false);
                                    }
                                }
                        );
//                        .create();
                
                AlertDialog dialog = builder.show();
                Window dialogWindow = dialog.getWindow();
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.BOTTOM;
                dialogWindow.setAttributes(lp);
                
                return dialog;
            }
        }
        //- tim

        // add to search
        public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
                new BaseSearchIndexProvider() {
                    @Override
                    public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                        final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                        final Resources res = context.getResources();

                        // Add fragment title
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.asus_lockscreen_unlock_password_title_nf);
                        data.screenTitle = res.getString(R.string.asus_lockscreen_unlock_password_title_nf);
                        result.add(data);
                        return result;
                    }

                };
    }
}
