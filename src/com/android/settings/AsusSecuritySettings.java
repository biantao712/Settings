package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.RestrictedSwitchPreference;

import com.asus.settings.lockscreen.AsusLSUtils;

import java.util.ArrayList;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class AsusSecuritySettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, GearPreference.OnGearClickListener {

    private static final String TAG = "AsusSecuritySettings";
    // Lock Settings
    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_UNLOCK_SET_OR_CHANGE_PROFILE = "unlock_set_or_change_profile";
    private static final String KEY_VISIBLE_PATTERN_PROFILE = "visiblepattern_profile";
    private static final String KEY_UNIFICATION = "unification";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE = 127;
    private static final int UNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 128;
    private static final int UNIFY_LOCK_CONFIRM_PROFILE_REQUEST = 129;
    private static final int UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST = 130;
    private static final String TAG_UNIFICATION_DIALOG = "unification_dialog";

    private static final String KEY_TRUST_AGENT = "trust_agent";
    private static final int MY_USER_ID = UserHandle.myUserId();

    protected DevicePolicyManager mDPM;
    protected UserManager mUm;

    protected ChooseLockSettingsHelper mChooseLockSettingsHelper;
    protected LockPatternUtils mLockPatternUtils;
    private ManagedLockPasswordProvider mManagedPasswordProvider;

    private SwitchPreference mVisiblePatternProfile;
    private SwitchPreference mUnifyProfile;

    private int mProfileChallengeUserId;

    private String mCurrentDevicePassword;
    private String mCurrentProfilePassword;

    protected static boolean mIsCNLockScreen = false;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.LOCKSCREEN;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		
        mLockPatternUtils = new LockPatternUtils(getActivity());
        mManagedPasswordProvider = ManagedLockPasswordProvider.get(getActivity(), MY_USER_ID);
        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUm = UserManager.get(getActivity());
        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());

        mIsCNLockScreen = AsusLSUtils.mIsCNLockScreen;
    }

    private static int getResIdForLockUnlockScreen(Context context,
            LockPatternUtils lockPatternUtils, ManagedLockPasswordProvider managedPasswordProvider,
            int userId) {
        final boolean isMyUser = userId == MY_USER_ID;
        int resid = 0;
        if (!lockPatternUtils.isSecure(userId)) {
            if (!isMyUser) {
                resid = R.xml.security_settings_lockscreen_profile;
            } else if (lockPatternUtils.isLockScreenDisabled(userId)) {
                resid = R.xml.security_settings_lockscreen;
            } else {
                if (mIsCNLockScreen) {
                    resid = R.xml.security_settings_chooser_cnsku;
                } else {
                    resid = R.xml.security_settings_chooser;
                }
            }
        } else {
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality(userId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = isMyUser ? R.xml.security_settings_pattern
                            : R.xml.security_settings_pattern_profile;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    resid = isMyUser ? R.xml.security_settings_pin
                            : R.xml.security_settings_pin_profile;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = isMyUser ? R.xml.security_settings_password
                            : R.xml.security_settings_password_profile;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    resid = managedPasswordProvider.getResIdForLockUnlockScreen(!isMyUser);
                    break;
            }
        }
        return resid;
    }

    /**
     * Important!
     *
     * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
     * logic or adding/removing preferences here.
     */
    protected PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        else {
            Log.d(TAG,"root is  null");
        }
        addPreferencesFromResource(R.xml.security_settings);
        root = getPreferenceScreen();

        // Add options for lock/unlock screen
        final int resid = getResIdForLockUnlockScreen(getActivity(), mLockPatternUtils,
                mManagedPasswordProvider, MY_USER_ID);
        //addPreferencesFromResource(resid);
		
        // DO or PO installed in the user may disallow to change password.
        disableIfPasswordQualityManaged(KEY_UNLOCK_SET_OR_CHANGE, MY_USER_ID);
		
        //++ Asus: Android for work items should not be shown in LockScreen Settings.
        /*
        mProfileChallengeUserId = Utils.getManagedProfileId(mUm, MY_USER_ID);
        if (mProfileChallengeUserId != UserHandle.USER_NULL
                && mLockPatternUtils.isSeparateProfileChallengeAllowed(mProfileChallengeUserId)) {
            addPreferencesFromResource(R.xml.security_settings_profile);
            addPreferencesFromResource(R.xml.security_settings_unification);
            final int profileResid = getResIdForLockUnlockScreen(
                    getActivity(), mLockPatternUtils, mManagedPasswordProvider,
                    mProfileChallengeUserId);
            addPreferencesFromResource(profileResid);
            maybeAddFingerprintPreference(root, mProfileChallengeUserId);
            if (!mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileChallengeUserId)) {
                final Preference lockPreference =
                        root.findPreference(KEY_UNLOCK_SET_OR_CHANGE_PROFILE);
                final String summary = getContext().getString(
                        R.string.lock_settings_profile_unified_summary);
                lockPreference.setSummary(summary);
                lockPreference.setEnabled(false);
                // PO may disallow to change password for the profile, but screen lock and managed
                // profile's lock is the same. Disable main "Screen lock" menu.
                disableIfPasswordQualityManaged(KEY_UNLOCK_SET_OR_CHANGE, mProfileChallengeUserId);
            } else {
                // PO may disallow to change profile password, and the profile's password is
                // separated from screen lock password. Disable profile specific "Screen lock" menu.
                disableIfPasswordQualityManaged(KEY_UNLOCK_SET_OR_CHANGE_PROFILE,
                        mProfileChallengeUserId);
            }
        }
        */
        //-- Asus:  Android for work items should not be shown in LockScreen Settings.

        Preference unlockSetOrChange = findPreference(KEY_UNLOCK_SET_OR_CHANGE);
        if (unlockSetOrChange instanceof GearPreference) {
            ((GearPreference) unlockSetOrChange).setOnGearClickListener(this);
        }
        else
        {
            Log.d(TAG,"Not instanceof Gear preference");
        }

        mVisiblePatternProfile =
                (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN_PROFILE);
        mUnifyProfile = (SwitchPreference) root.findPreference(KEY_UNIFICATION);

        return root;
    }

    /*
     * Sets the preference as disabled by admin if PASSWORD_QUALITY_MANAGED is set.
     * The preference must be a RestrictedPreference.
     */
    private void disableIfPasswordQualityManaged(String preferenceKey, int userId) {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfPasswordQualityIsSet(
                getActivity(), userId);
        if (admin != null && mDPM.getPasswordQuality(admin.component, userId) ==
                DevicePolicyManager.PASSWORD_QUALITY_MANAGED) {
            final RestrictedPreference pref =
                    (RestrictedPreference) getPreferenceScreen().findPreference(preferenceKey);
            pref.setDisabledByAdmin(admin);
        }
    }

    private void maybeAddFingerprintPreference(PreferenceGroup securityCategory, int userId) {
        Preference fingerprintPreference =
                FingerprintSettings.getFingerprintPreferenceForUser(
                        securityCategory.getContext(), userId);
        if (fingerprintPreference != null) {
            securityCategory.addPreference(fingerprintPreference);
        }
    }


    @Override
    public void onGearClick(GearPreference p) {
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(p.getKey())) {
            startFragment(this, SecuritySettings.SecuritySubSettings.class.getName(), 0, 0, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        if (mVisiblePatternProfile != null) {
            mVisiblePatternProfile.setChecked(mLockPatternUtils.isVisiblePatternEnabled(
                    mProfileChallengeUserId));
        }

    }

    private void updateUnificationPreference() {
        if (mUnifyProfile != null) {
            mUnifyProfile.setChecked(!mLockPatternUtils.isSeparateProfileChallengeEnabled(
                    mProfileChallengeUserId));
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
//        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
//            Log.d(TAG,"key is KEY_UNLOCK_SET_OR_CHANGE");
//            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
//                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
//        } else
        if (KEY_UNLOCK_SET_OR_CHANGE_PROFILE.equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                    mProfileChallengeUserId)) {
                return false;
            }
            Bundle extras = new Bundle();
            extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title_profile,
                    SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE, extras);
        } else {
            Log.d(TAG,"not handle this call super.onPreferenceTreeClick");
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preference);
        }
        return true;
    }

    /**
     * see confirmPatternThenDisableAndClear
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UNIFY_LOCK_CONFIRM_DEVICE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentDevicePassword =
                    data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            launchConfirmProfileLockForUnification();
            return;
        } else if (requestCode == UNIFY_LOCK_CONFIRM_PROFILE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            mCurrentProfilePassword =
                    data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            unifyLocks();
            return;
        } else if (requestCode == UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST
                && resultCode == Activity.RESULT_OK) {
            ununifyLocks();
            return;
        }
        Log.d(TAG,"call create preference hierarchy");
        createPreferenceHierarchy();
    }

    private void launchConfirmDeviceLockForUnification() {
        final String title = getActivity().getString(
                R.string.unlock_set_unlock_launch_picker_title);
        final ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(getActivity(), this);
        if (!helper.launchConfirmationActivity(
                UNIFY_LOCK_CONFIRM_DEVICE_REQUEST, title, true, MY_USER_ID)) {
            launchConfirmProfileLockForUnification();
        }
    }

    private void launchConfirmProfileLockForUnification() {
        final String title = getActivity().getString(
                R.string.unlock_set_unlock_launch_picker_title_profile);
        final ChooseLockSettingsHelper helper =
                new ChooseLockSettingsHelper(getActivity(), this);
        if (!helper.launchConfirmationActivity(
                UNIFY_LOCK_CONFIRM_PROFILE_REQUEST, title, true, mProfileChallengeUserId)) {
            unifyLocks();
            createPreferenceHierarchy();
        }
    }

    private void unifyLocks() {
        int profileQuality =
                mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileChallengeUserId);
        if (profileQuality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            mLockPatternUtils.saveLockPattern(
                    LockPatternUtils.stringToPattern(mCurrentProfilePassword),
                    mCurrentDevicePassword, MY_USER_ID);
        } else {
            mLockPatternUtils.saveLockPassword(
                    mCurrentProfilePassword, mCurrentDevicePassword,
                    profileQuality, MY_USER_ID);
        }
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileChallengeUserId, false,
                mCurrentProfilePassword);
        final boolean profilePatternVisibility =
                mLockPatternUtils.isVisiblePatternEnabled(mProfileChallengeUserId);
        mLockPatternUtils.setVisiblePatternEnabled(profilePatternVisibility, MY_USER_ID);
        mCurrentDevicePassword = null;
        mCurrentProfilePassword = null;
    }

    private void unifyUncompliantLocks() {
        mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileChallengeUserId, false,
                mCurrentProfilePassword);
        startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, null);
    }

    private void ununifyLocks() {
        Bundle extras = new Bundle();
        extras.putInt(Intent.EXTRA_USER_ID, mProfileChallengeUserId);
        startFragment(this,
                "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                R.string.lock_settings_picker_title_profile,
                SET_OR_CHANGE_LOCK_METHOD_REQUEST_PROFILE, extras);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean result = true;
        final String key = preference.getKey();
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        if (KEY_VISIBLE_PATTERN_PROFILE.equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                    mProfileChallengeUserId)) {
                return false;
            }
            lockPatternUtils.setVisiblePatternEnabled((Boolean) value, mProfileChallengeUserId);
        } else if (KEY_UNIFICATION.equals(key)) {
            if (Utils.startQuietModeDialogIfNecessary(this.getActivity(), mUm,
                    mProfileChallengeUserId)) {
                return false;
            }
            if ((Boolean) value) {
                final boolean compliantForDevice =
                        (mLockPatternUtils.getKeyguardStoredPasswordQuality(mProfileChallengeUserId)
                                >= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING
                        && mLockPatternUtils.isSeparateProfileChallengeAllowedToUnify(
                                mProfileChallengeUserId));
                UnificationConfirmationDialog dialog =
                        UnificationConfirmationDialog.newIntance(compliantForDevice);
                dialog.show(getChildFragmentManager(), TAG_UNIFICATION_DIALOG);
            } else {
                final String title = getActivity().getString(
                        R.string.unlock_set_unlock_launch_picker_title);
                final ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(getActivity(), this);
                if(!helper.launchConfirmationActivity(
                        UNUNIFY_LOCK_CONFIRM_DEVICE_REQUEST, title, true, MY_USER_ID)) {
                    ununifyLocks();
                }
            }
        }
        return result;
    }

    public static class UnificationConfirmationDialog extends DialogFragment {
        private static final String EXTRA_COMPLIANT = "compliant";

        public static UnificationConfirmationDialog newIntance(boolean compliant) {
            UnificationConfirmationDialog dialog = new UnificationConfirmationDialog();
            Bundle args = new Bundle();
            args.putBoolean(EXTRA_COMPLIANT, compliant);
            dialog.setArguments(args);
            return dialog;
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
            final AsusSecuritySettings parentFragment = ((AsusSecuritySettings) getParentFragment());
            final boolean compliant = getArguments().getBoolean(EXTRA_COMPLIANT);
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lock_settings_profile_unification_dialog_title)
                    .setMessage(compliant ? R.string.lock_settings_profile_unification_dialog_body
                            : R.string.lock_settings_profile_unification_dialog_uncompliant_body)
                    .setPositiveButton(
                            compliant ? R.string.lock_settings_profile_unification_dialog_confirm
                            : R.string.lock_settings_profile_unification_dialog_uncompliant_confirm,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (compliant) {
                                        parentFragment.launchConfirmDeviceLockForUnification();
                                    }    else {
                                        parentFragment.unifyUncompliantLocks();
                                    }
                                }
                            }
                    )
                    .setNegativeButton(R.string.cancel, null)
                    .create();
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            ((AsusSecuritySettings) getParentFragment()).updateUnificationPreference();
        }
    }

}
