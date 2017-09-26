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
 * limitations under the License.
 */

package com.android.settings.fingerprint;

import android.annotation.Nullable;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.preference.PreferenceManager;
import android.support.v14.preference.SwitchPreference;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import android.provider.SearchIndexableResource;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.ChooseLockGeneric;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.SettingsActivity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.HelpUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settingslib.RestrictedLockUtils;
import com.asus.suw.lockscreen.AsusFingerprintEnrollFindSensor;
import com.asus.suw.lockscreen.AsusFingerprintEnrollIntroduction;
import com.asus.suw.lockscreen.AsusSuwUtilisClient;

import java.util.ArrayList;
import java.util.List;
import android.text.InputFilter;
import com.android.settings.util.RemoveSpecialCharFilter;
import com.android.settings.util.Utf8ByteLengthFilter;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * Settings screen for fingerprints
 */
public class FingerprintSettings extends SubSettings implements Indexable {

    private static final String TAG = "FingerprintSettings";

    /**
     * Used by the choose fingerprint wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    protected static final int RESULT_FINISHED = RESULT_FIRST_USER;

    /**
     * Used by the enrolling screen during setup wizard to skip over setting up fingerprint, which
     * will be useful if the user accidentally entered this flow.
     */
    protected static final int RESULT_SKIP = RESULT_FIRST_USER + 1;

    /**
     * Like {@link #RESULT_FINISHED} except this one indicates enrollment failed because the
     * device was left idle. This is used to clear the credential token to require the user to
     * re-enter their pin/pattern/password before continuing.
     */
    protected static final int RESULT_TIMEOUT = RESULT_FIRST_USER + 2;
    protected static final int RESULT_STOP = RESULT_FIRST_USER + 3;

    private static final long LOCKOUT_DURATION = 30000; // time we have to wait for fp to reset, ms

    public static final String KEY_FINGERPRINT_SETTINGS = "fingerprint_settings";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintSettingsFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.security_settings_fingerprint_preference_title);
        setTitle(msg);
    }

    public static class FingerprintSettingsFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {
        private static final int MAX_RETRY_ATTEMPTS = 20;
        private static final int RESET_HIGHLIGHT_DELAY_MS = 500;

        private static final String TAG = "FingerprintSettings";
        private static final String KEY_FINGERPRINT_ITEM_PREFIX = "key_fingerprint_item";
        private static final String KEY_FINGERPRINT_ADD = "key_fingerprint_add";
        private static final String KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE =
                "fingerprint_enable_keyguard_toggle";
        private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";

        // +++ AMAX @ 20170119 7.1.1 Porting
        // Asus fingerprint key for Phone
        private static final String KEY_SECURE_WITH_FINGERPRINT_CATEGORY = "secure_with_fingerprint";
        private SwitchPreference mAnswerCall = null;
        private SwitchPreference mSnapCall = null;

        private static final String KEY_FINGERPRINT_ANSWER_CALL_SWITCH = "answer_call_with_fingerprint";
        private static final String KEY_FINGERPRINT_SNAP_CALL_SWITCH = "snap_call_with_fingerprint";
        private static final String ASUS_FINGERPRINT_ANSWER_CALL = "asus_fingerprint_id_answer_call";
        private static final String ASUS_FINGERPRINT_SNAP_CALL = "asus_fingerprint_id_snap_call";
        private static final String INTENT_SNAP_CALL_EXIST = "com.asus.contacts.SNAP_CALL_EXIST";
        // --- AMAX @ 20170119 7.1.1 Porting

        //++ ASUS fingerprint key
        private AsusUnlockSwitchPreference mUnlockDevice;
        private boolean mLaunchedIntroduction;
        private static final String KEY_FINGERPRINT_MANAGEMENT_CATEGORY = "fingerprint_management";
        private static final String KEY_FINGERPRINT_UNLOCK_SWITCH = "unlock_device_with_fingerprint";
        private static final String KEY_LAUNCHED_INTRODUCTION = "launched_introduction";
        private static final boolean sSUPPORT_WAKEUP_LUNLOCK = SystemProperties.getBoolean("persist.asus.fp.wakeup_support", false);
        //-- ASUS fingerprint key
        private static final String FINGERPRINT_POSITION = SystemProperties.get("ro.hardware.fp_position");

        private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
        private static final int MSG_FINGER_AUTH_SUCCESS = 1001;
        private static final int MSG_FINGER_AUTH_FAIL = 1002;
        private static final int MSG_FINGER_AUTH_ERROR = 1003;
        private static final int MSG_FINGER_AUTH_HELP = 1004;

        private static final int CONFIRM_REQUEST = 101;
        private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;
        private static final int FINGERPRINT_INTRODUTION_REQUEST = 103;

        private static final int ADD_FINGERPRINT_REQUEST = 10;

        // +++ AMAX @ 20170119 7.1.1 Porting
        private static final String KEY_SKIP_SHOW_CALL_LOCK_DETAIL = "skip_show_call_lock_detail";
        private static final String KEY_SKIP_SHOW_LONG_TAP_WARNING = "skip_show_long_tap_warning";
        // --- AMAX @ 20170119 7.1.1 Porting

        protected static final boolean DEBUG = true;

        private FingerprintManager mFingerprintManager;
        private CancellationSignal mFingerprintCancel;
        private boolean mInFingerprintLockout;
        private byte[] mToken;
        private boolean mLaunchedConfirm;
        private Drawable mHighlightDrawable;
        private int mUserId;

        //++ For asus setup wizard layout
        private boolean mAsusSuwReady;
        //-- For asus setup wizard layout
        private boolean mSensorFront = false;

        private boolean mNeedReunlock = false;
        private AuthenticationCallback mAuthCallback = new AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(AuthenticationResult result) {
                int fingerId = result.getFingerprint().getFingerId();
                mHandler.obtainMessage(MSG_FINGER_AUTH_SUCCESS, fingerId, 0).sendToTarget();
            }

            @Override
            public void onAuthenticationFailed() {
                mHandler.obtainMessage(MSG_FINGER_AUTH_FAIL).sendToTarget();
            };

            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_ERROR, errMsgId, 0, errString)
                        .sendToTarget();
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_HELP, helpMsgId, 0, helpString)
                        .sendToTarget();
            }
        };
        private RemovalCallback mRemoveCallback = new RemovalCallback() {

            @Override
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                mHandler.obtainMessage(MSG_REFRESH_FINGERPRINT_TEMPLATES,
                        fingerprint.getFingerId(), 0).sendToTarget();
            }

            @Override
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT);
                }
            }
        };
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH_FINGERPRINT_TEMPLATES:
                        removeFingerprintPreference(msg.arg1);
                        updateAddPreference();
                        updateFingerprintUseFor();  //update asus feature toggle.
                        retryFingerprint();
                    break;
                    case MSG_FINGER_AUTH_SUCCESS:
                        mFingerprintCancel = null;
                        highlightFingerprintItem(msg.arg1);
                        retryFingerprint();
                    break;
                    case MSG_FINGER_AUTH_FAIL:
                        // No action required... fingerprint will allow up to 5 of these
                    break;
                    case MSG_FINGER_AUTH_ERROR:
                        handleError(msg.arg1 /* errMsgId */, (CharSequence) msg.obj /* errStr */ );
                    break;
                    case MSG_FINGER_AUTH_HELP: {
                        // Not used
                    }
                    break;
                }
            };
        };

        private void stopFingerprint() {
            if (mFingerprintCancel != null && !mFingerprintCancel.isCanceled()) {
                mFingerprintCancel.cancel();
            }
            mFingerprintCancel = null;
        }

        /**
         * @param errMsgId
         */
        protected void handleError(int errMsgId, CharSequence msg) {
            mFingerprintCancel = null;
            switch (errMsgId) {
                case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                    return; // Only happens if we get preempted by another activity. Ignored.
                case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                    mInFingerprintLockout = true;
                    // We've been locked out.  Reset after 30s.
                    if (!mHandler.hasCallbacks(mFingerprintLockoutReset)) {
                        mHandler.postDelayed(mFingerprintLockoutReset,
                                LOCKOUT_DURATION);
                    }
                    // Fall through to show message
                default:
                    // Activity can be null on a screen rotation.
                    final Activity activity = getActivity();
                    if (activity != null) {
                        Toast.makeText(activity, msg , Toast.LENGTH_SHORT);
                    }
                break;
            }
            retryFingerprint(); // start again
        }

        private void retryFingerprint() {
            if (!mInFingerprintLockout && !mSensorFront) {
                if(mFingerprintManager.hasEnrolledFingerprints(mUserId)) {
                    mFingerprintCancel = new CancellationSignal();
                    mFingerprintManager.authenticate(null, mFingerprintCancel, 0 /* flags */,
                            mAuthCallback, null, mUserId);
                }
            }
        }

        @Override
        protected int getMetricsCategory() {
            return MetricsEvent.FINGERPRINT;
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View root = super.onCreateView(inflater, container, savedInstanceState);
            root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
            return root;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                mToken = savedInstanceState.getByteArray(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                mLaunchedConfirm = savedInstanceState.getBoolean(
                        KEY_LAUNCHED_CONFIRM, false);
                mLaunchedIntroduction = savedInstanceState.getBoolean(
                        KEY_LAUNCHED_INTRODUCTION, false);
            }
            mUserId = getActivity().getIntent().getIntExtra(
                    Intent.EXTRA_USER_ID, UserHandle.myUserId());

            Activity activity = getActivity();
            mFingerprintManager = (FingerprintManager) activity.getSystemService(
                    Context.FINGERPRINT_SERVICE);

//            AsusSuwUtilisClient suwClient = new AsusSuwUtilisClient(getContext());
//            mAsusSuwReady = suwClient.getSetupWizardLayout() != null;

            //++ Asus flow
//            boolean launcedIntroduction = savedInstanceState == null? maybeLauncheIntroduction() : false;
            //+++ tim_hu@asus.com remove listener for front sensor
            boolean launcedIntroduction = false;
            if(CNAsusFindFingerprintSensorView.SENSOR_FRONT.equals(FINGERPRINT_POSITION)
                    || CNAsusFindFingerprintSensorView.SENSOR_FRONT2.equals(FINGERPRINT_POSITION))
                mSensorFront = true;
            //--- tim_hu@asus.com
            //-- Asus flow

            // Need to authenticate a session token if none
            if (mToken == null && mLaunchedConfirm == false && !launcedIntroduction) {
                mLaunchedConfirm = true;
                launchChooseOrConfirmLock();
            }
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            /*
            TextView v = (TextView) LayoutInflater.from(view.getContext()).inflate(
                    R.layout.fingerprint_settings_footer, null);
            EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                    getActivity(), DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, mUserId);
            v.setText(LearnMoreSpan.linkify(getText(admin != null
                            ? R.string.security_settings_fingerprint_enroll_disclaimer_lockscreen_disabled
                            : R.string.security_settings_fingerprint_enroll_disclaimer),
                    getString(getHelpResource()), admin));
            v.setMovementMethod(new LinkMovementMethod());
            setFooterView(v);
            */
        }

        private boolean isFingerprintDisabled() {
            final DevicePolicyManager dpm =
                    (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            return dpm != null && (dpm.getKeyguardDisabledFeatures(null)
                    & DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT) != 0;
        }

        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = findPreference(name);
            if (prefToRemove != null) {
                PreferenceGroup group = (PreferenceGroup)getPreferenceScreen().findPreference(
                        KEY_FINGERPRINT_MANAGEMENT_CATEGORY);
                if (!group.removePreference(prefToRemove)) {
                    Log.w(TAG, "Failed to remove preference with key " + name);
                }
            } else {
                Log.w(TAG, "Can't find preference to remove: " + name);
            }
        }

        /**
         * Important!
         *
         * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
         * logic or adding/removing preferences here.
         */
        private PreferenceScreen createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            addPreferencesFromResource(R.xml.security_settings_fingerprint);
            root = getPreferenceScreen();
            // +++ AMAX @ 20170119 7.1.1 Porting
            // AOSP
            // addFingerprintItemPreferences(root);
            // setPreferenceScreen(root);

            // Asus fingerprint key for Phone
            PreferenceGroup switch_items = (PreferenceGroup)root.findPreference(KEY_SECURE_WITH_FINGERPRINT_CATEGORY);
            // +++ ian_tsai@20160901 check switch_items is not null
            if(switch_items != null) {
                mAnswerCall = (SwitchPreference) root.findPreference(KEY_FINGERPRINT_ANSWER_CALL_SWITCH);
                mSnapCall = (SwitchPreference) root.findPreference(KEY_FINGERPRINT_SNAP_CALL_SWITCH);

                List<ResolveInfo> appsInfo = getPackageManager().queryBroadcastReceivers(
                        new Intent(INTENT_SNAP_CALL_EXIST), 0);
                if (appsInfo.isEmpty() && null != mSnapCall) { // receiver doesn't exist
                    switch_items.removePreference(mSnapCall);
                }
                // +++ ian_tsai@20160627 remove preference if voice is not capable
                if (!Utils.isVoiceCapable(getActivity()) && null != mAnswerCall) {
                    switch_items.removePreference(mAnswerCall);
                }
                // --- ian_tsai@20160627 remove preference if voice is not capable
                
                //+++ tim
                int count = switch_items.getPreferenceCount();
                if(count > 0)
                	switch_items.getPreference(count-1).setLayoutResource(R.layout.asusres_preference_material_nodivider);
                //---
            }
            // --- ian_tsai@20160901 check switch_items is not null
            // --- AMAX @ 20170119 7.1.1 Porting

            PreferenceGroup items = (PreferenceGroup)root.findPreference(
                    KEY_FINGERPRINT_MANAGEMENT_CATEGORY);
            addFingerprintItemPreferences(items);

            //++ Asus fingerprint unlock item
            mUnlockDevice = (AsusUnlockSwitchPreference) root.findPreference(
                    KEY_FINGERPRINT_UNLOCK_SWITCH);
            //-- Asus fingerprint unlock item

            return root;
        }

        private void addFingerprintItemPreferences(PreferenceGroup root) {
            root.removeAll();
            final List<Fingerprint> items = mFingerprintManager.getEnrolledFingerprints(mUserId);
            final int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                final Fingerprint item = items.get(i);
                FingerprintPreference pref = new FingerprintPreference(root.getContext());
                pref.setKey(genKey(item.getFingerId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                pref.setIcon(R.drawable.ic_fingerprint_24dp);
                root.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }
            //tim++
//            Preference addPreference = new Preference(root.getContext());
//            addPreference.setKey(KEY_FINGERPRINT_ADD);
//            addPreference.setTitle(R.string.fingerprint_add_title);
//            addPreference.setIcon(R.drawable.ic_add_24dp);
//            root.addPreference(addPreference);
//            addPreference.setOnPreferenceChangeListener(this);
            //tim--
            updateAddPreference();
        }

        private void updateAddPreference() {
            /* Disable preference if too many fingerprints added */
            final int max = getContext().getResources().getInteger(
                    com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
            boolean tooMany = mFingerprintManager.getEnrolledFingerprints(mUserId).size() >= max;
            CharSequence maxSummary = tooMany ?
                    getContext().getString(R.string.fingerprint_add_max, max) : "";
            Preference addPreference = findPreference(KEY_FINGERPRINT_ADD);
            addPreference.setSummary(maxSummary);
            addPreference.setEnabled(!tooMany);
        }

        private static String genKey(int id) {
            return KEY_FINGERPRINT_ITEM_PREFIX + "_" + id;
        }

        @Override
        public void onResume() {
            super.onResume();
            // Make sure we reload the preference hierarchy since fingerprints may be added,
            // deleted or renamed.
            updatePreferences();
            if (mUnlockDevice != null) {
                mUnlockDevice.resume();
            }
        }

        private void updatePreferences() {
            createPreferenceHierarchy();
            retryFingerprint();
            updateFingerprintUseFor();
        }

        private void updateFingerprintUseFor(){
        	//disable switchs if no fingerprint or no password
            boolean isEnable = (mFingerprintManager.getEnrolledFingerprints(mUserId).size() > 0); //&& (mToken != null);
            if (mUnlockDevice != null) {
                if(isFingerprintDisabled()){
                    mUnlockDevice.setEnabled(false);
                    mUnlockDevice.setSummary(R.string.asus_security_settings_fingerprint_disabled_on_lockscreen);
                }else{
                    mUnlockDevice.handleStateChanged();
                    mUnlockDevice.setEnabled(isEnable);
                    
                    //tim++ 
                    //disable switch view if no password
                    mUnlockDevice.setSwitchEnable(isEnable && (mToken != null));
                }
            }
            // +++ AMAX @ 20170125 Android 7.1.1 porting
            // Asus fingerprint key for Phone
            if (mAnswerCall != null) {
                // +++ ian_tsai@20161212 disable mAnswerCall if there is no fingerprint enrolled.
                if(!isEnable){
                    Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, 0);
                }
                // --- ian_tsai@20161212 disable mAnswerCall if there is no fingerprint enrolled.
                mAnswerCall.setChecked(Settings.Global.getInt(
                    getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, 0) != 0);

                mAnswerCall.setEnabled(isEnable);
                mAnswerCall.setOnPreferenceChangeListener(getCallPreferenceChangeListener());
            }

            if (mSnapCall != null) {
                mSnapCall.setChecked(Settings.Global.getInt(
                    getContentResolver(), ASUS_FINGERPRINT_SNAP_CALL, 0) != 0);

                mSnapCall.setEnabled(isEnable);
                mSnapCall.setOnPreferenceChangeListener(getCallPreferenceChangeListener());
            }
            // --- AMAX @ 20170125 Android 7.1.1 porting
        }
        
        //tim++
        //disable switch preference if no password
        private Preference.OnPreferenceChangeListener getCallPreferenceChangeListener(){
        	return new Preference.OnPreferenceChangeListener(){
    			@Override
    			public boolean onPreferenceChange(Preference arg0, Object arg1) {
    				// TODO Auto-generated method stub
    				if(mToken == null)
    					return false;
    				return true;
    			}
        	};
        }
        //tim--

        @Override
        public void onPause() {
            super.onPause();
            stopFingerprint();
            if (mUnlockDevice != null) {
                mUnlockDevice.pause();
            }
        }

        @Override
        public void onSaveInstanceState(final Bundle outState) {
            outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                    mToken);
            outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
            outState.putBoolean(KEY_LAUNCHED_INTRODUCTION, mLaunchedIntroduction);

        }

        @Override
        public boolean onPreferenceTreeClick(Preference pref) {
            final String key = pref.getKey();
            
            //tim++
        	if(mToken == null && (KEY_FINGERPRINT_UNLOCK_SWITCH.equals(key) 
        			|| KEY_FINGERPRINT_ANSWER_CALL_SWITCH.equals(key)
        			|| KEY_FINGERPRINT_SNAP_CALL_SWITCH.equals(key))){
        		showLockWarningDialog();
        		return false;
        	}
        	
            if (KEY_FINGERPRINT_ADD.equals(key)) {
            	//tim++
            	if(mToken == null || mFingerprintManager.getEnrolledFingerprints(mUserId).size() == 0){
            		launcheIntroduction();
            		return super.onPreferenceTreeClick(pref);
            	}
            	//tim--
                Intent intent = new Intent();
                //intent.setClassName("com.android.settings",
                //        FingerprintEnrollEnrolling.class.getName());
                //Asus fingerprint flow alawys show find sensor view
/*                if(mAsusSuwReady){
                    intent.setClassName("com.android.settings",
                            AsusFingerprintEnrollFindSensor.class.getName());
                }else {
                    intent.setClassName("com.android.settings",
                            CNFingerprintEnrollFindSensor.class.getName());
                }*/
                intent.setClassName("com.android.settings",
                        CNFingerprintEnrollFindSensor.class.getName());
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startActivityForResult(intent, ADD_FINGERPRINT_REQUEST);
            } else if (pref instanceof FingerprintPreference) {
                FingerprintPreference fpref = (FingerprintPreference) pref;
                final Fingerprint fp =fpref.getFingerprint();
                showRenameDeleteDialog(fp);
                return super.onPreferenceTreeClick(pref);
            // +++ Asus fingerprint key for Phone
            } else if (KEY_FINGERPRINT_ANSWER_CALL_SWITCH.equals(key)){
                if (mAnswerCall.isEnabled()) {
                    // +++ ian_tsai@20160608 show dialog when user turn on incoming call lock
                    if (isToggled(pref)) {
                        ShowCallLockDetailDialog(pref);
                    } else {
                        Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                    }
                    // --- ian_tsai@20160608 show dialog when user turn on incoming call lock
                }
            } else if (KEY_FINGERPRINT_SNAP_CALL_SWITCH.equals(key)) {
                if(mSnapCall.isEnabled()) {
                    Settings.Global.putInt(getContentResolver(),ASUS_FINGERPRINT_SNAP_CALL, isToggled(pref)? 1: 0);
                }
            // --- Asus fingerprint key for Phone
            }
            //++ Asus features add here
            else if (KEY_FINGERPRINT_UNLOCK_SWITCH.equals(key)){
//                if(sSUPPORT_WAKEUP_LUNLOCK) {
//                    startFragment(this, AsusUnlockSettings.class.getCanonicalName(),
//                            R.string.secure_unlock_with_fingerprint_item, 0, null);
//                }else{
                    Settings.Secure.putInt(getContentResolver(),
                            AsusUnlockSettings.UNLOCK_DEVICE_ENABLED, !mUnlockDevice.isChecked()? 1: 0);
//                }
                mUnlockDevice.handleStateChanged();
            }
            //-- Asus features add here

            return true;
        }
        // +++ AMAX @ 20170119 7.1.1 Porting
        private boolean isToggled(Preference pref) {
            if(pref instanceof SwitchPreference)
                return ((SwitchPreference) pref).isChecked();
            return false;
        }
        // --- AMAX @ 20170119 7.1.1 Porting

        private void showRenameDeleteDialog(final Fingerprint fp) {
            RenameDeleteDialog renameDeleteDialog = new RenameDeleteDialog();
            Bundle args = new Bundle();
            args.putParcelable("fingerprint", fp);
            renameDeleteDialog.setArguments(args);
            renameDeleteDialog.setTargetFragment(this, 0);
            renameDeleteDialog.show(getFragmentManager(), RenameDeleteDialog.class.getName());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean result = true;
            final String key = preference.getKey();
            if (KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE.equals(key)) {
                // TODO
            } else {
                Log.v(TAG, "Unknown key:" + key);
            }
            return result;
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_fingerprint;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            mNeedReunlock = false;
            if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
                    || requestCode == CONFIRM_REQUEST
                    || requestCode == FINGERPRINT_INTRODUTION_REQUEST) {
                if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                    // The lock pin/pattern/password was set. Start enrolling!
                    if (data != null) {
                        mToken = data.getByteArrayExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                    }
                }
            } else if (requestCode == ADD_FINGERPRINT_REQUEST) {
                if (resultCode == RESULT_TIMEOUT) {
                    Activity activity = getActivity();
                    activity.setResult(RESULT_TIMEOUT);
                    activity.finish();
                }
            }

            //tim++
            if (mToken == null && requestCode != FINGERPRINT_INTRODUTION_REQUEST) {
                // Didn't get an authentication, finishing
                getActivity().finish();
            }
        }

        private boolean maybeLauncheIntroduction(){
            FingerprintManager fpm = (FingerprintManager) getActivity().getSystemService(
                    Context.FINGERPRINT_SERVICE);
            final List<Fingerprint> items = fpm.getEnrolledFingerprints(mUserId);
            final int fingerprintCount = items != null ? items.size() : 0;
            if (fingerprintCount == 0){
                Intent intent = new Intent();
                if(mAsusSuwReady) {
                    intent.setClassName("com.android.settings", AsusFingerprintEnrollIntroduction.class.getName());
                }else{
                    intent.setClassName("com.android.settings", FingerprintEnrollIntroduction.class.getName());
                }
                //fix double confirm password and token are inconsistent
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                startActivityForResult(intent, FINGERPRINT_INTRODUTION_REQUEST);
                mLaunchedIntroduction = true;
                return true;
            }
            return false;
        }
        
        //tim++
        private boolean launcheIntroduction(){
            Intent intent = new Intent();
            /*if(mAsusSuwReady) {
                intent.setClassName("com.android.settings", AsusFingerprintEnrollIntroduction.class.getName());
            }else{
                intent.setClassName("com.android.settings", CNFingerprintEnrollIntroduction.class.getName());
            }*/
            intent.setClassName("com.android.settings", CNFingerprintEnrollIntroduction.class.getName());
            //fix double confirm password and token are inconsistent
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            startActivityForResult(intent, FINGERPRINT_INTRODUTION_REQUEST);
            return true;
        }
        
        // tim++
        // warning need password change unlock switch
        private void showLockWarningDialog(){
        	View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
            TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
            title.setText(getActivity().getResources().getString(R.string.fingerprint_lock_warning_dialog_title));
            
            TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
            message.setText(getActivity().getResources().getString(R.string.fingerprint_lock_warning_dialog_msg));
            
        	final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setView(view1)
//        			.setTitle(R.string.fingerprint_lock_warning_dialog_title)
//                    .setMessage(R.string.fingerprint_lock_warning_dialog_msg)
                    .setPositiveButton(R.string.fingerprint_lock_warning_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                    .show();
        	
        	Window dialogWindow = alertDialog.getWindow();
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.BOTTOM;
            dialogWindow.setAttributes(lp);
        }
        
        //tim--
        @Override
        public void onStart(){
            super.onStart();
            if (mNeedReunlock && updatePasswordQuality()){
                mNeedReunlock = false;
                getActivity().finish();
            }
        }
        @Override
        public void onStop(){
            super.onStop();
            mNeedReunlock = true;
        }
        private boolean updatePasswordQuality() {
            final int passwordQuality = new ChooseLockSettingsHelper(getActivity()).utils()
                    .getActivePasswordQuality(UserManager.get(getActivity()).getCredentialOwnerProfile(mUserId));
            boolean hasPassword = passwordQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
            return hasPassword;

        }
        @Override
        public void onDestroy() {
            super.onDestroy();
            if (getActivity().isFinishing()) {
                int result = mFingerprintManager.postEnroll();
                if (result < 0) {
                    Log.w(TAG, "postEnroll failed: result = " + result);
                }
            }
        }

        private Drawable getHighlightDrawable() {
            if (mHighlightDrawable == null) {
                final Activity activity = getActivity();
                if (activity != null) {
                    mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
                }
            }
            return mHighlightDrawable;
        }

        private void highlightFingerprintItem(int fpId) {
            String prefName = genKey(fpId);
            FingerprintPreference fpref = (FingerprintPreference) findPreference(prefName);
            final Drawable highlight = getHighlightDrawable();
            if(fpref == null){
                Log.d(TAG, "highlightFingerprintItem fingerprint id " + fpId + " is not found");
                return;
            }
            if (highlight != null) {
                final View view = fpref.getView();
                if(view != null) {
                    final int centerX = view.getWidth() / 2;
                    final int centerY = view.getHeight() / 2;
                    highlight.setHotspot(centerX, centerY);
                    view.setBackground(highlight);
                    view.setPressed(true);
                    view.setPressed(false);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            view.setBackground(null);
                        }
                    }, RESET_HIGHLIGHT_DELAY_MS);
                }
            }
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            long challenge = mFingerprintManager.preEnroll();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_fingerprint_preference_title),
                    null, null, challenge, mUserId)) {
            	/*tim++
            	 * do not ChooseLock if no password
                intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                        true);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);*/
            }
        }

        private void deleteFingerPrint(Fingerprint fingerPrint) {
            mFingerprintManager.remove(fingerPrint, mUserId, mRemoveCallback);
        }

        private void renameFingerPrint(int fingerId, String newName) {
            mFingerprintManager.rename(fingerId, mUserId, newName);
            updatePreferences();
        }

        private final Runnable mFingerprintLockoutReset = new Runnable() {
            @Override
            public void run() {
                mInFingerprintLockout = false;
                retryFingerprint();
            }
        };

        public static class RenameDeleteDialog extends DialogFragment {

            private Fingerprint mFp;
            private EditText mDialogTextField;
            private String mFingerName;
            private Boolean mTextHadFocus;
            private int mTextSelectionStart;
            private int mTextSelectionEnd;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                if (savedInstanceState != null) {
                    mFingerName = savedInstanceState.getString("fingerName");
                    mTextHadFocus = savedInstanceState.getBoolean("textHadFocus");
                    mTextSelectionStart = savedInstanceState.getInt("startSelection");
                    mTextSelectionEnd = savedInstanceState.getInt("endSelection");
                }

                View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_textfield_with_title, null);
                TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
                title.setText(R.string.security_settings_fingerprint_rename);
                mDialogTextField = (EditText) view1.getRootView().findViewById(R.id.alertdialog_textfield);
                mDialogTextField.setPadding(0, 0, 0, 0);
                mDialogTextField.setFilters(new InputFilter[] {new Utf8ByteLengthFilter(32), new RemoveSpecialCharFilter(true)});

                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setView(view1)
                        .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String newName =
                                                mDialogTextField.getText().toString();
                                        final CharSequence name = mFp.getName();
                                        if (!newName.equals(name)) {
                                            if (DEBUG) {
                                                Log.v(TAG, "rename " + name + " to " + newName);
                                            }
                                            MetricsLogger.action(getContext(),
                                                    MetricsEvent.ACTION_FINGERPRINT_RENAME,
                                                    mFp.getFingerId());
                                            FingerprintSettingsFragment parent
                                                    = (FingerprintSettingsFragment)
                                                    getTargetFragment();
                                            parent.renameFingerPrint(mFp.getFingerId(),
                                                    newName);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.security_settings_fingerprint_enroll_dialog_delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onDeleteClick(dialog);
                                    }
                                })
                        .create();

                Window dialogWindow = alertDialog.getWindow();
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.gravity = Gravity.BOTTOM;
                dialogWindow.setAttributes(lp);

                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        CharSequence name = mFingerName == null ? mFp.getName() : mFingerName;
                        mDialogTextField.setText(name);
                        if (mTextHadFocus == null) {
                            mDialogTextField.selectAll();
                        } else {
                            mDialogTextField.setSelection(mTextSelectionStart, mTextSelectionEnd);
                        }
                    }
                });
                if (mTextHadFocus == null || mTextHadFocus) {
                    // Request the IME
                    alertDialog.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                return alertDialog;
            }

            private void onDeleteClick(DialogInterface dialog) {
                if (DEBUG) Log.v(TAG, "Removing fpId=" + mFp.getFingerId());
                MetricsLogger.action(getContext(), MetricsEvent.ACTION_FINGERPRINT_DELETE,
                        mFp.getFingerId());
                FingerprintSettingsFragment parent
                        = (FingerprintSettingsFragment) getTargetFragment();
                final boolean isProfileChallengeUser =
                        Utils.isManagedProfile(UserManager.get(getContext()), parent.mUserId);
                if (parent.mFingerprintManager.getEnrolledFingerprints(parent.mUserId).size() > 1) {
                    parent.deleteFingerPrint(mFp);
                } else {
                    ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                    Bundle args = new Bundle();
                    args.putParcelable("fingerprint", mFp);
                    args.putBoolean("isProfileChallengeUser", isProfileChallengeUser);
                    lastDeleteDialog.setArguments(args);
                    lastDeleteDialog.setTargetFragment(getTargetFragment(), 0);
                    lastDeleteDialog.show(getFragmentManager(),
                            ConfirmLastDeleteDialog.class.getName());
                }
                dialog.dismiss();
            }

            @Override
            public void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
                if (mDialogTextField != null) {
                    outState.putString("fingerName", mDialogTextField.getText().toString());
                    outState.putBoolean("textHadFocus", mDialogTextField.hasFocus());
                    outState.putInt("startSelection", mDialogTextField.getSelectionStart());
                    outState.putInt("endSelection", mDialogTextField.getSelectionEnd());
                }
            }
        }

        public static class ConfirmLastDeleteDialog extends DialogFragment {

            private Fingerprint mFp;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                final boolean isProfileChallengeUser =
                        getArguments().getBoolean("isProfileChallengeUser");
                
                View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
                TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
                title.setText(getActivity().getResources().getString(R.string.fingerprint_last_delete_title));
                
                TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
                String text = isProfileChallengeUser 
                		? getActivity().getResources().getString(R.string.fingerprint_last_delete_message_profile_challenge)
                				: getActivity().getResources().getString(R.string.fingerprint_last_delete_message);
                message.setText(text);
                
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                		.setView(view1)
//                        .setTitle(R.string.fingerprint_last_delete_title)
//                        .setMessage((isProfileChallengeUser)
//                                ? R.string.fingerprint_last_delete_message_profile_challenge
//                                : R.string.fingerprint_last_delete_message)
                        .setPositiveButton(R.string.fingerprint_last_delete_confirm,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FingerprintSettingsFragment parent
                                                = (FingerprintSettingsFragment) getTargetFragment();
                                        parent.deleteFingerPrint(mFp);
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
//                        .create();
                
                AlertDialog alertDialog = builder.show();
                Window dialogWindow = alertDialog.getWindow();
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.width = WindowManager.LayoutParams.MATCH_PARENT;
                lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
                lp.gravity = Gravity.BOTTOM;
                dialogWindow.setAttributes(lp);
                
                return alertDialog;
            }
        }

        // +++ ian_tsai@20160608 show dialog when user turn on incoming call lock
        public void ShowCallLockDetailDialog(final Preference pref) {
            boolean isDialogSkip = (Settings.Global.getInt(getContentResolver(), KEY_SKIP_SHOW_CALL_LOCK_DETAIL, 0) == 1 );
            Log.d(FingerprintSettingsFragment.TAG, "ShowCallLockDetailDialog isDialogSkip: " + String.valueOf(isDialogSkip));

            if (isDialogSkip) {
                Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                ShowLongTapWarningDialog();
            } else {
                final View checkboxLayout = getActivity().getLayoutInflater().inflate(R.layout.app_checkbox_never_show_again, null);
                TextView contentTitle = (TextView) checkboxLayout.findViewById(R.id.alertdialog_title);
                contentTitle.setText(R.string.hint);

                TextView messageTextView = (TextView) checkboxLayout.findViewById(R.id.alertdialog_message);
                messageTextView.setText(R.string.asus_call_lock_detail);
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setView(checkboxLayout)
                        .setPositiveButton(R.string.okay,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        CheckBox dontShowAgain = (CheckBox) checkboxLayout.findViewById(R.id.skip);
                                        if (dontShowAgain.isChecked()) {
                                            Settings.Global.putInt(getContentResolver(), KEY_SKIP_SHOW_CALL_LOCK_DETAIL, 1);
                                        }

                                        ShowLongTapWarningDialog();
                                        Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        CheckBox dontShowAgain = (CheckBox) checkboxLayout.findViewById(R.id.skip);
                                        if (dontShowAgain.isChecked()) {
                                            Settings.Global.putInt(getContentResolver(), KEY_SKIP_SHOW_CALL_LOCK_DETAIL, 1);
                                        }
                                        if (pref instanceof SwitchPreference) {
                                            ((SwitchPreference) pref).setChecked(false);
                                            Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        // +++ kin_lo@20160706: keep value when Cancel
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                if (pref instanceof SwitchPreference) {
                                    ((SwitchPreference) pref).setChecked(false);
                                    Settings.Global.putInt(getContentResolver(), ASUS_FINGERPRINT_ANSWER_CALL, isToggled(pref) ? 1 : 0);
                                }
                            }
                        })
                        // --- kin_lo@20160706: keep value when Cancel
                        .show();

                Window dialogWindow = alertDialog.getWindow();
                WindowManager.LayoutParams lp = dialogWindow.getAttributes();
                lp.gravity = Gravity.BOTTOM;
                dialogWindow.setAttributes(lp);
            }
        }

        public void ShowLongTapWarningDialog() {
           // boolean isLongTapMode = ((Settings.Global.getInt(getContentResolver(),
           //         Settings.Global.ASUS_FINGERPRINT_LONG_PRESS, 0)) != 0);
            boolean isLongTapMode = ((Settings.Global.getInt(getContentResolver(), "asus_fingerprint_long_press", 0)) != 0);
            boolean isDialogSkip = (Settings.Global.getInt(getContentResolver(), KEY_SKIP_SHOW_LONG_TAP_WARNING, 0) == 1 );
            Log.d(FingerprintSettingsFragment.TAG, "ShowLongTapWarningDialog isLongTapMode: " + String.valueOf(isLongTapMode) + " isDialogSkip: " + String.valueOf(isDialogSkip));

            if (!isLongTapMode || isDialogSkip) {
                return;
            }
            final View checkboxLayout = getActivity().getLayoutInflater().inflate(R.layout.app_checkbox_never_show_again, null);

            final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setView(checkboxLayout)
                    .setMessage(R.string.asus_long_tap_warning)
                    .setPositiveButton(R.string.okay,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    CheckBox dontShowAgain = (CheckBox) checkboxLayout.findViewById(R.id.skip);
                                    if (dontShowAgain.isChecked()) {
                                        Settings.Global.putInt(getContentResolver(), KEY_SKIP_SHOW_LONG_TAP_WARNING, 1);
                                    }
                                    dialog.dismiss();
                                }
                            })
                    .show();

            Window dialogWindow = alertDialog.getWindow();
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.gravity = Gravity.BOTTOM;
            dialogWindow.setAttributes(lp);
        }
        // --- ian_tsai@20160608 show dialog when user turn on incoming call lock
    }

    public static class FingerprintPreference extends Preference {
        private Fingerprint mFingerprint;
        private View mView;

        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr,
                int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }
        public FingerprintPreference(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        public FingerprintPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public FingerprintPreference(Context context) {
            super(context);
        }

        public View getView() { return mView; }

        public void setFingerprint(Fingerprint item) {
            mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            mView = view.itemView;
        }
    };

    private static class LearnMoreSpan extends URLSpan {

        private static final Typeface TYPEFACE_MEDIUM =
                Typeface.create("sans-serif-medium", Typeface.NORMAL);

        private static final String ANNOTATION_URL = "url";
        private static final String ANNOTATION_ADMIN_DETAILS = "admin_details";

        private EnforcedAdmin mEnforcedAdmin = null;

        private LearnMoreSpan(String url) {
            super(url);
        }

        private LearnMoreSpan(EnforcedAdmin admin) {
            super((String) null);
            mEnforcedAdmin = admin;
        }

        @Override
        public void onClick(View widget) {
            Context ctx = widget.getContext();
            if (mEnforcedAdmin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(ctx, mEnforcedAdmin);
            } else {
                Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
                try {
                    widget.startActivityForResult(intent, 0);
                } catch (ActivityNotFoundException e) {
                    Log.w(FingerprintSettingsFragment.TAG,
                            "Actvity was not found for intent, " + intent.toString());
                }
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri, EnforcedAdmin admin) {
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
            SpannableStringBuilder builder = new SpannableStringBuilder(msg);
            for (Annotation annotation : spans) {
                final String key = annotation.getValue();
                int start = msg.getSpanStart(annotation);
                int end = msg.getSpanEnd(annotation);
                LearnMoreSpan link = null;
                if (ANNOTATION_URL.equals(key)) {
                    link = new LearnMoreSpan(uri);
                } else if (ANNOTATION_ADMIN_DETAILS.equals(key)) {
                    link = new LearnMoreSpan(admin);
                }
                if (link != null) {
                    builder.setSpan(link, start, end, msg.getSpanFlags(link));
                }
            }
            return builder;
        }
    }

    public static Preference getFingerprintPreferenceForUser(Context context, final int userId) {
        FingerprintManager fpm = (FingerprintManager) context.getSystemService(
                Context.FINGERPRINT_SERVICE);
        if (fpm == null || !fpm.isHardwareDetected()) {
            Log.v(TAG, "No fingerprint hardware detected!!");
            return null;
        }
        Preference fingerprintPreference = new Preference(context);
        fingerprintPreference.setKey(KEY_FINGERPRINT_SETTINGS);
        fingerprintPreference.setTitle(R.string.security_settings_fingerprint_preference_title);
        final List<Fingerprint> items = fpm.getEnrolledFingerprints(userId);
        final int fingerprintCount = items != null ? items.size() : 0;
        final String clazz;
        // ++ Asus always entry fingerprint enrollment form fingerprint settings
        if (fingerprintCount > 0) {
            fingerprintPreference.setSummary(context.getResources().getQuantityString(
                    R.plurals.security_settings_fingerprint_preference_summary,
                    fingerprintCount, fingerprintCount));
            clazz = FingerprintSettings.class.getName();
        } else {
            fingerprintPreference.setSummary(
                    R.string.security_settings_fingerprint_preference_summary_none);
            clazz = FingerprintSettings.class.getName(); // FingerprintEnrollIntroduction.class.getName();
        }
        // -- Asus always entry fingerprint enrollment form fingerprint settings
        fingerprintPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Context context = preference.getContext();
                final UserManager userManager = UserManager.get(context);
                if (Utils.startQuietModeDialogIfNecessary(context, userManager,
                        userId)) {
                    return false;
                }
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", clazz);
                intent.putExtra(Intent.EXTRA_USER_ID, userId);
                context.startActivity(intent);
                return true;
            }
        });
        return fingerprintPreference;
    }

    //for search asus flip cover on settings.
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                    final Resources res = context.getResources();
                    String coverAction = null;
                    String coverPkg = null;
                    String targetClass = null;
                    String title = res.getString(R.string.cn_fingerprint);
                    String screentitle = res.getString(R.string.cn_fingerprint);


                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = title;
                    data.screenTitle = screentitle;
                    data.intentAction = "android.intent.action.MAIN";
                    data.intentTargetPackage = "com.android.settings";
                    data.intentTargetClass = "com.android.settings.fingerprint.FingerprintSettings";
                    data.iconResId = R.drawable.ic_settings_security;
                    result.add(data);

                    return result;
                }
            };

}
