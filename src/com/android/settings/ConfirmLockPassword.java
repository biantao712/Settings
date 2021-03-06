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

import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.os.*;
import android.os.storage.StorageManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import java.util.ArrayList;
public class ConfirmLockPassword extends ConfirmDeviceCredentialBaseActivity {
    public static final String TAG = "ConfirmLockPassword_NF";
    // The index of the array is isStrongAuth << 2 + isProfile << 1 + isAlpha.
    private static final int[] DETAIL_TEXTS = new int[] {
        R.string.lockpassword_confirm_your_pin_generic_nf,
        R.string.lockpassword_confirm_your_password_generic_nf,
        R.string.lockpassword_confirm_your_pin_generic_profile,
        R.string.lockpassword_confirm_your_password_generic_profile_nf,
        R.string.lockpassword_strong_auth_required_reason_restart_device_pin,
        R.string.lockpassword_strong_auth_required_reason_restart_device_password,
        R.string.lockpassword_strong_auth_required_reason_restart_work_pin,
        R.string.lockpassword_strong_auth_required_reason_restart_work_password,
    };

    public static class InternalActivity extends ConfirmLockPassword {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Fix on phones
        // Disable IME on our window since we provide our own keyboard
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
        //WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.lockpassword_confirm_your_password_header_nf);
        setTitle(msg);
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmLockPasswordFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmLockPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragment != null && fragment instanceof ConfirmLockPasswordFragment) {
            ((ConfirmLockPasswordFragment)fragment).onWindowFocusChanged(hasFocus);
        }
    }

    public static class ConfirmLockPasswordFragment extends ConfirmDeviceCredentialBaseFragment
            implements OnClickListener, OnEditorActionListener,TextWatcher,
            CredentialCheckResultTracker.Listener {
        public static final String TAG = "ConfirmLockPasswordFragment_NF";
        private static final long ERROR_MESSAGE_TIMEOUT = 3000;
        private static final String FRAGMENT_TAG_CHECK_LOCK_RESULT = "check_lock_result";
        private TextView mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private CredentialCheckResultTracker mCredentialCheckResultTracker;
        private boolean mDisappearing = false;
        private TextView mHeaderTextView;
        private TextView mDetailsTextView;
        private CountDownTimer mCountdownTimer;
        private boolean mIsAlpha;
        private InputMethodManager mImm;
        private boolean mUsingFingerprint = false;
        private AppearAnimationUtils mAppearAnimationUtils;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private boolean mBlockImm;
        private Button mCancelButton;
        private Button mNextButton;


        //++ Asus: for verizon "secure set-up feature" feature
        private boolean mFeedbackPwd = false;
        //-- Asus: for verizon "secure set-up feature" feature

        // required constructor for fragments
        public ConfirmLockPasswordFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            Log.d(TAG,"Enter oncreate");
            super.onCreate(savedInstanceState);
            Log.d(TAG,"Force set to portrait");
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.d(TAG,"on create veiw Force set to portrait");
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            final int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality(
                    mEffectiveUserId);
            View view = inflater.inflate(R.layout.confirm_lock_password_nf, null);

            mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            if (mPasswordEntry == null)
            {
                Log.d(TAG,"mPasswordEntery is null");
            }
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);
            mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

            mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            if(mHeaderTextView == null)
            {
                Log.d(TAG,"mHeaderTextView is null");
            }

            mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
            if(mDetailsTextView == null)
            {
                Log.d(TAG,"mDetails TextView is null");
            }
            mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            if(mErrorTextView == null)
            {
                Log.d(TAG,"mError TextView is null");
            }
            //mErrorTextView.setVisibility(View.INVISIBLE);

            mCancelButton = (Button) view.findViewById(R.id.cancel_button);
            mNextButton = (Button) view.findViewById(R.id.next_button);

            if(mCancelButton == null || mNextButton == null)
            {
                Log.d(TAG,"Buttons are null");
            }
            else
            {
                mCancelButton.setOnClickListener(this);
                mNextButton.setOnClickListener(this);
            }

            mIsAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_MANAGED == storedQuality;

            mImm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                CharSequence headerMessage = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.HEADER_TEXT);
                CharSequence detailsMessage = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT);
                if (TextUtils.isEmpty(headerMessage)) {
                    headerMessage = getString(getDefaultHeader());
                }
                if (TextUtils.isEmpty(detailsMessage)) {
                    detailsMessage = getString(getDefaultDetails());
                }
                headerMessage = getString(getDefaultHeader());
                if(mHeaderTextView != null)
                {
                    mHeaderTextView.setText(headerMessage);
                }
                if(mDetailsTextView != null)
                {
                    mDetailsTextView.setText(detailsMessage);
                }
                mFeedbackPwd = intent.getBooleanExtra("needpwd",false);
            }
            int currentType = mPasswordEntry.getInputType();
            mPasswordEntry.setInputType(mIsAlpha ? currentType
                    : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));
            mAppearAnimationUtils = new AppearAnimationUtils(getContext(),
                    220, 2f /* translationScale */, 1f /* delayScale*/,
                    AnimationUtils.loadInterpolator(getContext(),
                            android.R.interpolator.linear_out_slow_in));
            mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                    110, 1f /* translationScale */,
                    0.5f /* delayScale */, AnimationUtils.loadInterpolator(
                            getContext(), android.R.interpolator.fast_out_linear_in));
            if(mHeaderTextView != null)
            {
                setAccessibilityTitle(mHeaderTextView.getText());
            }

            mCredentialCheckResultTracker = (CredentialCheckResultTracker) getFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAG_CHECK_LOCK_RESULT);
            if (mCredentialCheckResultTracker == null) {
                mCredentialCheckResultTracker = new CredentialCheckResultTracker();
                getFragmentManager().beginTransaction().add(mCredentialCheckResultTracker,
                        FRAGMENT_TAG_CHECK_LOCK_RESULT).commit();
            }

            return view;
        }

        private int getDefaultHeader() {
            return mIsAlpha ? R.string.lockpassword_confirm_your_password_header
                    : R.string.lockpassword_confirm_your_pin_header;
        }

        private int getDefaultDetails() {
            boolean isProfile = Utils.isManagedProfile(
                    UserManager.get(getActivity()), mEffectiveUserId);
            // Map boolean flags to an index by isStrongAuth << 2 + isProfile << 1 + isAlpha.
            // wesley remove strong auth required for display set to large UI is now friendly.
            int index = /*((mIsStrongAuthRequired ? 1 : 0) << 2) + */((isProfile ? 1 : 0) << 1) + (mIsAlpha ? 1 : 0);
            return DETAIL_TEXTS[index];
        }

        private int getErrorMessage() {
            return mIsAlpha ? R.string.lockpassword_invalid_password
                    : R.string.lockpassword_invalid_pin_nf;
        }

        @Override
        protected int getLastTryErrorMessage() {
            return mIsAlpha ? R.string.lock_profile_wipe_warning_content_password
                    : R.string.lock_profile_wipe_warning_content_pin;
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            if(mHeaderTextView != null)
            {
                mHeaderTextView.setAlpha(0f);
            }
            if(mDetailsTextView != null)
            {
                mDetailsTextView.setAlpha(0f);
            }
            mCancelButton.setAlpha(0f);
            mPasswordEntry.setAlpha(0f);
            mFingerprintIcon.setAlpha(0f);
            mBlockImm = true;
        }

        private View[] getActiveViews() {
            ArrayList<View> result = new ArrayList<>();
            if(mHeaderTextView != null)
            {
                result.add(mHeaderTextView);
            }

            if(mDetailsTextView != null)
            {
                result.add(mDetailsTextView);
            }
            if (mCancelButton.getVisibility() == View.VISIBLE) {
                result.add(mCancelButton);
            }
            result.add(mPasswordEntry);
            if (mFingerprintIcon.getVisibility() == View.VISIBLE) {
                result.add(mFingerprintIcon);
            }
            return result.toArray(new View[] {});
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            mAppearAnimationUtils.startAnimation(getActiveViews(), new Runnable() {
                @Override
                public void run() {
                    mBlockImm = false;
                    resetState();
                }
            });
        }

        @Override
        public void onPause() {
            Log.d(TAG,"enter on pause, set to full sensor");
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            super.onPause();
            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
                mCountdownTimer = null;
            }
            mCredentialCheckResultTracker.setListener(null);
        }

        @Override
        protected int getMetricsCategory() {
            return MetricsEvent.CONFIRM_LOCK_PASSWORD;
        }

        @Override
        public void onResume() {
            Log.d(TAG,"enter on resume, set to portrait");
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            super.onResume();
            long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
            if (deadline != 0) {
                mCredentialCheckResultTracker.clearResult();
                handleAttemptLockout(deadline);
            } else {
                resetState();
                if(mErrorTextView != null) {
                    mErrorTextView.setText("");
                }
                if (isProfileChallenge()) {
                    updateErrorMessage(mLockPatternUtils.getCurrentFailedPasswordAttempts(
                            mEffectiveUserId));
                }
            }
            mCredentialCheckResultTracker.setListener(this);
        }

        @Override
        protected void authenticationSucceeded() {
            mCredentialCheckResultTracker.setResult(true, new Intent(), 0, mEffectiveUserId);
        }

        @Override
        public void onFingerprintIconVisibilityChanged(boolean visible) {
            mUsingFingerprint = visible;
        }

        private void resetState() {
            if (mBlockImm) return;
            mPasswordEntry.setEnabled(true);
            mPasswordEntryInputDisabler.setInputEnabled(true);
            if (shouldAutoShowSoftKeyboard()) {
                mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
            }
        }

        private boolean shouldAutoShowSoftKeyboard() {
            return mPasswordEntry.isEnabled() && !mUsingFingerprint;
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            if (!hasFocus || mBlockImm) {
                return;
            }
            // Post to let window focus logic to finish to allow soft input show/hide properly.
            mPasswordEntry.post(new Runnable() {
                @Override
                public void run() {
                    if (shouldAutoShowSoftKeyboard()) {
                        resetState();
                        return;
                    }

                    mImm.hideSoftInputFromWindow(mPasswordEntry.getWindowToken(),
                            InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            });
        }
		
        protected void setNextEnabled(boolean enabled) {
            if(mNextButton != null)
            {
                mNextButton.setEnabled(enabled);
                ColorStateList csList =  getResources().getColorStateList(enabled ?
                        R.color.security_button_enable_color:R.color.security_button_disable_color);
                mNextButton.setTextColor(csList);
            }
            else
            {
                Log.d(TAG,"Next Button is null in setNextEnabled");
            }
        }
		
        public void afterTextChanged(Editable s) {
            String textStr = mPasswordEntry.getText().toString();
            if(textStr.length() > 0) {
                setNextEnabled(true);
            }
            else
            {
                setNextEnabled(false);
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }
        private void handleNext() {
            if (mPendingLockCheck != null || mDisappearing) {
                Log.d(TAG,"return directly 1");
                return;
            }

            final String pin = mPasswordEntry.getText().toString();
            if (TextUtils.isEmpty(pin)) {
                Log.d(TAG,"return directly");
                return;
            }

            mPasswordEntryInputDisabler.setInputEnabled(false);
            final boolean verifyChallenge = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);

            Intent intent = new Intent();
            if (verifyChallenge)  {
                if (isInternalActivity()) {
                    startVerifyPassword(pin, intent);
                    Log.d(TAG,"return start veifity.");
                    return;
                }
            } else {
                startCheckPassword(pin, intent);
                Log.d(TAG,"returen start check");
                return;
            }

            mCredentialCheckResultTracker.setResult(false, intent, 0, mEffectiveUserId);
            Log.d(TAG,"return end");
        }

        private boolean isInternalActivity() {
            return getActivity() instanceof ConfirmLockPassword.InternalActivity;
        }

        private void startVerifyPassword(final String pin, final Intent intent) {
            long challenge = getActivity().getIntent().getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            final int localEffectiveUserId = mEffectiveUserId;
            final int localUserId = mUserId;
            final LockPatternChecker.OnVerifyCallback onVerifyCallback =
                    new LockPatternChecker.OnVerifyCallback() {
                        @Override
                        public void onVerified(byte[] token, int timeoutMs) {
                            mPendingLockCheck = null;
                            boolean matched = false;
                            if (token != null) {
                                matched = true;
                                if (mReturnCredentials) {
                                    intent.putExtra(
                                            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                                            token);
                                }
                            }
					        if(!matched)
                            {
                                Log.d(TAG,"not match,show error in header.");
                                CharSequence headerMessage = getString(getErrorMessage());
                                if(mHeaderTextView != null)
                                {
                                    mHeaderTextView.setText(headerMessage);
                                }
                            }
                            mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs,
                                    localUserId);
                        }
            };
            mPendingLockCheck = (localEffectiveUserId == localUserId)
                    ? LockPatternChecker.verifyPassword(
                            mLockPatternUtils, pin, challenge, localUserId, onVerifyCallback)
                    : LockPatternChecker.verifyTiedProfileChallenge(
                            mLockPatternUtils, pin, false, challenge, localUserId,
                            onVerifyCallback);
        }

        private void startCheckPassword(final String pin, final Intent intent) {
            final int localEffectiveUserId = mEffectiveUserId;
            mPendingLockCheck = LockPatternChecker.checkPassword(
                    mLockPatternUtils,
                    pin,
                    localEffectiveUserId,
                    new LockPatternChecker.OnCheckCallback() {
                        @Override
                        public void onChecked(boolean matched, int timeoutMs) {
                            mPendingLockCheck = null;
                            if (matched && isInternalActivity() && mReturnCredentials) {
                                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_TYPE,
                                                mIsAlpha ? StorageManager.CRYPT_TYPE_PASSWORD
                                                         : StorageManager.CRYPT_TYPE_PIN);
                                intent.putExtra(
                                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, pin);
                            }
                            mCredentialCheckResultTracker.setResult(matched, intent, timeoutMs,
                                    localEffectiveUserId);
                        }
                    });
        }

        private void startDisappearAnimation(final Intent intent) {
            if (mDisappearing) {
                return;
            }
            mDisappearing = true;

            if (getActivity().getThemeResId() == R.style.Theme_ConfirmDeviceCredentialsDark) {
                mDisappearAnimationUtils.startAnimation(getActiveViews(), new Runnable() {
                    @Override
                    public void run() {
                        // Bail if there is no active activity.
                        if (getActivity() == null || getActivity().isFinishing()) {
                            return;
                        }

                        getActivity().setResult(RESULT_OK, intent);
                        getActivity().finish();
                        getActivity().overridePendingTransition(
                                R.anim.confirm_credential_close_enter,
                                R.anim.confirm_credential_close_exit);
                    }
                });
            } else {
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            }
        }

        private void onPasswordChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId, boolean newResult) {
            mPasswordEntryInputDisabler.setInputEnabled(true);
            if (matched) {
                if (newResult) {
                    reportSuccessfullAttempt();
                }
                startDisappearAnimation(intent);
                checkForPendingIntent();
            } else {
                if (timeoutMs > 0) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                            effectiveUserId, timeoutMs);
                    handleAttemptLockout(deadline);
                } else {
                    showError(getErrorMessage(), ERROR_MESSAGE_TIMEOUT);
                }
                if (newResult) {
                    reportFailedAttempt();
                }
            }
        }

        @Override
        public void onCredentialChecked(boolean matched, Intent intent, int timeoutMs,
                int effectiveUserId, boolean newResult) {
            onPasswordChecked(matched, intent, timeoutMs, effectiveUserId, newResult);
        }

        @Override
        protected void onShowError() {
            mPasswordEntry.setText(null);
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            mPasswordEntry.setEnabled(false);
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    showError(getString(
                            R.string.lockpattern_too_many_failed_confirmation_attempts,
                            secondsCountdown), 0);
                }

                @Override
                public void onFinish() {
                    resetState();
                    if(mErrorTextView != null) {
                        mErrorTextView.setText("");
                    }
                    if (isProfileChallenge()) {
                        updateErrorMessage(mLockPatternUtils.getCurrentFailedPasswordAttempts(
                                mEffectiveUserId));
                    }
                }
            }.start();
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.next_button:
                    handleNext();
                    break;

                case R.id.cancel_button:
                    getActivity().setResult(RESULT_CANCELED);
                    getActivity().finish();
                    break;
            }
        }

        // {@link OnEditorActionListener} methods.
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handleNext();
                return true;
            }
            return false;
        }
    }
}
