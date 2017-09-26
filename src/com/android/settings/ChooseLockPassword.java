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

import android.app.Activity;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.*;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.RequestThrottledException;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.notification.RedactionInterstitial;
import com.android.settings.password.PasswordRequirementAdapter;
import com.android.setupwizardlib.GlifLayout;
import com.android.setupwizardlib.SetupWizardLayout;

import java.util.ArrayList;
import java.util.List;

import static android.app.admin.DevicePolicyManager.*;

public class ChooseLockPassword extends SettingsActivity {
    public static final String PASSWORD_MIN_KEY = "lockscreen.password_min";
    public static final String PASSWORD_MAX_KEY = "lockscreen.password_max";
    public static final String PASSWORD_MIN_LETTERS_KEY = "lockscreen.password_min_letters";
    public static final String PASSWORD_MIN_LOWERCASE_KEY = "lockscreen.password_min_lowercase";
    public static final String PASSWORD_MIN_UPPERCASE_KEY = "lockscreen.password_min_uppercase";
    public static final String PASSWORD_MIN_NUMERIC_KEY = "lockscreen.password_min_numeric";
    public static final String PASSWORD_MIN_SYMBOLS_KEY = "lockscreen.password_min_symbols";
    public static final String PASSWORD_MIN_NONLETTER_KEY = "lockscreen.password_min_nonletter";

    private static final String TAG = "ChooseLockPassword_NF";
    private static boolean mIsOnPauseCalled = false;

    @Override
    public Intent getIntent() {
        Log.d(TAG,"enter getIntent and return fragment in EXTRA_SHOW_FRAGMENT");
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    public static Intent createIntent(Context context, int quality,
            int minLength, final int maxLength, boolean requirePasswordToDecrypt,
            boolean confirmCredentials) {
        Log.d(TAG,"createIntent 1");
        Intent intent = new Intent().setClass(context, ChooseLockPassword.class);
        intent.putExtra(LockPatternUtils.PASSWORD_TYPE_KEY, quality);
        intent.putExtra(PASSWORD_MIN_KEY, minLength);
        intent.putExtra(PASSWORD_MAX_KEY, maxLength);
        intent.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, confirmCredentials);
        intent.putExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, requirePasswordToDecrypt);
        return intent;
    }

    public static Intent createIntent(Context context, int quality,
            int minLength, final int maxLength, boolean requirePasswordToDecrypt,
            boolean confirmCredentials, int userId) {
        Log.d(TAG,"createIntent 2");
        Intent intent = createIntent(context, quality, minLength, maxLength,
                requirePasswordToDecrypt, confirmCredentials);
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        return intent;
    }

    public static Intent createIntent(Context context, int quality,
            int minLength, final int maxLength, boolean requirePasswordToDecrypt, String password) {
        Log.d(TAG,"createIntent 3");
        Intent intent = createIntent(context, quality, minLength, maxLength,
                requirePasswordToDecrypt, false);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, password);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength,
            int maxLength, boolean requirePasswordToDecrypt, String password, int userId) {
        Log.d(TAG,"createIntent 4");
        Intent intent = createIntent(context, quality, minLength, maxLength,
                requirePasswordToDecrypt, password);
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        return intent;
    }

    public static Intent createIntent(Context context, int quality,
            int minLength, final int maxLength, boolean requirePasswordToDecrypt, long challenge) {
        Log.d(TAG,"createIntent 5");
        Intent intent = createIntent(context, quality, minLength, maxLength,
                requirePasswordToDecrypt, false);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        return intent;
    }

    public static Intent createIntent(Context context, int quality, int minLength,
            int maxLength, boolean requirePasswordToDecrypt, long challenge, int userId) {
        Log.d(TAG,"createIntent 6");
        Intent intent = createIntent(context, quality, minLength, maxLength,
                requirePasswordToDecrypt, challenge);
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        Log.d(TAG,"enter isValidFramgent");
        if (ChooseLockPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        Log.d(TAG,"return fragment name");
        return ChooseLockPasswordFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"Enter activity onCreate");
        setTheme(R.style.Theme_PasswordPattern);
        super.onCreate(savedInstanceState);
        if(mIsOnPauseCalled == true)
        {
            Log.d(TAG,"on pause is called and finish on create.");
            mIsOnPauseCalled = false;
            finish();
        }

        CharSequence msg = getText(R.string.lockpassword_choose_your_password_header_nf);
        setTitle(msg);
        LinearLayout layout = (LinearLayout) findViewById(R.id.content_parent);
        layout.setFitsSystemWindows(false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(getColor(R.color.action_bar_background));
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        mIsOnPauseCalled = true;
    }

    @Override
    public void onDestroy()
    {
        mIsOnPauseCalled = false;
        super.onDestroy();
    }

    public static class ChooseLockPasswordFragment extends InstrumentedFragment
            implements OnClickListener, OnEditorActionListener, TextWatcher,
            SaveAndFinishWorker.Listener {
        private static final String TAG = "ChooseLockPasswordFragment_NF";
        private static final String KEY_FIRST_PIN = "first_pin";
        private static final String KEY_UI_STAGE = "ui_stage";
        private static final String KEY_CURRENT_PASSWORD = "current_password";
        private static final String FRAGMENT_TAG_SAVE_AND_FINISH = "save_and_finish_worker";

        private String mCurrentPassword;
        private String mChosenPassword;
        private boolean mHasChallenge;
        private long mChallenge;
        private EditText mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private int mPasswordMinLength = LockPatternUtils.MIN_LOCK_PASSWORD_SIZE;
        private int mPasswordMaxLength = 17;
        private int mPasswordMinLetters = 0;
        private int mPasswordMinUpperCase = 0;
        private int mPasswordMinLowerCase = 0;
        private int mPasswordMinSymbols = 0;
        private int mPasswordMinNumeric = 0;
        private int mPasswordMinNonLetter = 0;
        private int mPasswordMinLengthToFulfillAllPolicies = 0;
        private int mUserId;
        private boolean mHideDrawer = false;
        /**
         * Password requirements that we need to verify.
         */
        private int[] mPasswordRequirements;

        private LockPatternUtils mLockPatternUtils;
        private SaveAndFinishWorker mSaveAndFinishWorker;
        private int mRequestedQuality = DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private Stage mUiStage = Stage.Introduction;
        private PasswordRequirementAdapter mPasswordRequirementAdapter;

        private TextView mHeaderText;
        private TextView mPasswordHintText;
//        private TextView mPinHintText;
        private int      mPinLength = 4;
        private String mFirstPin;
//        private RecyclerView mPasswordRestrictionView;
        private KeyboardView mKeyboardView;
        private PasswordEntryKeyboardHelper mKeyboardHelper;
        private boolean mIsAlphaMode;
        private Button mCancelButton;
        private Button mNextButton;

        private TextChangedHandler mTextChangedHandler;

        private static final int CONFIRM_EXISTING_REQUEST = 58;
        static final int RESULT_FINISHED = RESULT_FIRST_USER;

        private static final int MIN_LETTER_IN_PASSWORD = 0;
        private static final int MIN_UPPER_LETTERS_IN_PASSWORD = 1;
        private static final int MIN_LOWER_LETTERS_IN_PASSWORD = 2;
        private static final int MIN_SYMBOLS_IN_PASSWORD = 3;
        private static final int MIN_NUMBER_IN_PASSWORD = 4;
        private static final int MIN_NON_LETTER_IN_PASSWORD = 5;

        // Error code returned from {@link #validatePassword(String)}.
        private static final int NO_ERROR = 0;
        private static final int CONTAIN_INVALID_CHARACTERS = 1 << 0;
        private static final int TOO_SHORT = 1 << 1;
        private static final int TOO_LONG = 1 << 2;
        private static final int CONTAIN_NON_DIGITS = 1 << 3;
        private static final int CONTAIN_SEQUENTIAL_DIGITS = 1 << 4;
        private static final int RECENTLY_USED = 1 << 5;
        private static final int NOT_ENOUGH_LETTER = 1 << 6;
        private static final int NOT_ENOUGH_UPPER_CASE = 1 << 7;
        private static final int NOT_ENOUGH_LOWER_CASE = 1 << 8;
        private static final int NOT_ENOUGH_DIGITS = 1 << 9;
        private static final int NOT_ENOUGH_SYMBOLS = 1 << 10;
        private static final int NOT_ENOUGH_NON_LETTER = 1 << 11;
        private static final long ERROR_MESSAGE_TIMEOUT = 3000;
        private static final int MSG_SHOW_ERROR = 1;

        private static boolean mIsOnPauseCalled = false;
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_SHOW_ERROR) {
                    updateStage((Stage) msg.obj);
                }
            }
        };
        /**
         * Keep track internally of where the user is in choosing a pattern.
         */
        protected enum Stage {

            Introduction(R.string.lockpassword_choose_your_password_header_nf,
                    R.string.lockpassword_choose_your_pin_header_nf,
                    R.string.lockpassword_continue_label),

            NeedToConfirm(R.string.lockpassword_confirm_your_password_header_nf,
                    R.string.lockpassword_confirm_your_pin_header_nf,
                    R.string.lockpassword_ok_label),

            ConfirmWrong(R.string.lockpassword_confirm_passwords_dont_match,
                    R.string.lockpassword_confirm_pins_dont_match_nf,
                    R.string.lockpassword_continue_label);

            Stage(int hintInAlpha, int hintInNumeric, int nextButtonText) {
                this.alphaHint = hintInAlpha;
                this.numericHint = hintInNumeric;
                this.buttonText = nextButtonText;
            }

            public final int alphaHint;
            public final int numericHint;
            public final int buttonText;
        }

        // required constructor for fragments
        public ChooseLockPasswordFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            if(mIsOnPauseCalled == true)
            {
                Log.d(TAG,"on pause is called and finish on create.");
                mIsOnPauseCalled = false;
                getActivity().finish();
            }
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
            Intent intent = getActivity().getIntent();
            if (!(getActivity() instanceof ChooseLockPassword)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }

            // Only take this argument into account if it belongs to the current profile.
            mUserId = Utils.getUserIdFromBundle(getActivity(), intent.getExtras());
            processPasswordRequirements(intent);
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            mHideDrawer = getActivity().getIntent().getBooleanExtra(EXTRA_HIDE_DRAWER, false);

            if (intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_FOR_CHANGE_CRED_REQUIRED_FOR_BOOT, false)) {
                SaveAndFinishWorker w = new SaveAndFinishWorker();
                final boolean required = getActivity().getIntent().getBooleanExtra(
                        EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, EncryptionInterstitial.DEFAULT_REQUIRE_PASSWORD);
                String current = intent.getStringExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                w.setBlocking(true);
                w.setListener(this);
                w.start(mChooseLockSettingsHelper.utils(), required,
                        false, 0, current, current, mRequestedQuality, mUserId);
            }
            mTextChangedHandler = new TextChangedHandler();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.choose_lock_password_nf, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mCancelButton = (Button) view.findViewById(R.id.cancel_button);
            mNextButton = (Button) view.findViewById(R.id.next_button);
            if(mCancelButton == null || mNextButton == null)
            {
                Log.d(TAG,"Buttons is null");
            }
            else
            {
                mCancelButton.setOnClickListener(this);
                mNextButton.setOnClickListener(this);
            }

            mIsAlphaMode = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == mRequestedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == mRequestedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == mRequestedQuality;
            mKeyboardView = (PasswordEntryKeyboardView) view.findViewById(R.id.keyboard);
            setupPasswordRequirementsView(view);

//            mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mPasswordEntry = (EditText) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntry.addTextChangedListener(this);
            mPasswordEntry.requestFocus();
            mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

            final Activity activity = getActivity();
            mKeyboardHelper = new PasswordEntryKeyboardHelper(activity,
                    mKeyboardView, mPasswordEntry);
            mKeyboardHelper.setKeyboardMode(mIsAlphaMode ?
                    PasswordEntryKeyboardHelper.KEYBOARD_MODE_ALPHA
                    : PasswordEntryKeyboardHelper.KEYBOARD_MODE_NUMERIC);
            mHeaderText = (TextView) view.findViewById(R.id.headerText);
            mPasswordHintText = (TextView) view.findViewById(R.id.passwordHint);
//            mPinHintText.setOnClickListener(this);

            mKeyboardView.requestFocus();

            int currentType = mPasswordEntry.getInputType();
            mPasswordEntry.setInputType(mIsAlphaMode ? currentType
                    : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));

            Intent intent = getActivity().getIntent();
            final boolean confirmCredentials = intent.getBooleanExtra(
                    ChooseLockGeneric.CONFIRM_CREDENTIALS, true);
            mCurrentPassword = intent.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            mHasChallenge = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
            mChallenge = intent.getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            if (savedInstanceState == null) {
                updateStage(Stage.Introduction);
                if (confirmCredentials) {
                    mChooseLockSettingsHelper.launchConfirmationActivity(CONFIRM_EXISTING_REQUEST,
                            getString(R.string.unlock_set_unlock_launch_picker_title), true,
                            mUserId);
                }
            } else {
                // restore from previous state
                mFirstPin = savedInstanceState.getString(KEY_FIRST_PIN);
                final String state = savedInstanceState.getString(KEY_UI_STAGE);
                if (state != null) {
                    mUiStage = Stage.valueOf(state);
                    updateStage(mUiStage);
                }

                if (mCurrentPassword == null) {
                    mCurrentPassword = savedInstanceState.getString(KEY_CURRENT_PASSWORD);
                }

                // Re-attach to the exiting worker if there is one.
                mSaveAndFinishWorker = (SaveAndFinishWorker) getFragmentManager().findFragmentByTag(
                        FRAGMENT_TAG_SAVE_AND_FINISH);
            }

            // Workaround to show one password requirement below EditText when IME is shown.
            // By adding an inset to the edit text background, we make the EditText occupy more
            // vertical space, and the keyboard will then avoid hiding it. We have also set
            // negative margin in the layout below in order to have them show in the correct
            // position.
//            final int visibleVerticalSpaceBelowPassword =
//                    getResources().getDimensionPixelOffset(
//                        R.dimen.visible_vertical_space_below_password);
//            InsetDrawable drawable =
//                    new InsetDrawable(
//                    mPasswordEntry.getBackground(), 0, 0, 0, visibleVerticalSpaceBelowPassword);
//            mPasswordEntry.setBackgroundDrawable(drawable);
//            LinearLayout bottomContainer = (LinearLayout) view.findViewById(R.id.bottom_container);
//            LinearLayout.LayoutParams bottomContainerLp =
//                    (LinearLayout.LayoutParams) bottomContainer.getLayoutParams();
//            bottomContainerLp.setMargins(0, -visibleVerticalSpaceBelowPassword, 0, 0);

            if (activity instanceof SettingsActivity) {
                final SettingsActivity sa = (SettingsActivity) activity;
                int id = mIsAlphaMode ? R.string.lockpassword_choose_your_password_header_nf
                        : R.string.lockpassword_choose_your_pin_header_nf;
                CharSequence title = getText(id);
                sa.setTitle(title);
                if(view instanceof GlifLayout) {
                    ((GlifLayout) view).setHeaderText(title);
                }else if(view instanceof SetupWizardLayout ){
                    //++ Keep M7.0.0 design
                    ((SetupWizardLayout) view).setHeaderText(title);
                    //-- Keep M7.0.0 design
                }
            }
        }

        private void setupPasswordRequirementsView(View view) {
            // Construct passwordRequirements and requirementDescriptions.
            List<Integer> passwordRequirements = new ArrayList<>();
            List<String> requirementDescriptions = new ArrayList<>();
            if (mPasswordMinUpperCase > 0) {
                passwordRequirements.add(MIN_UPPER_LETTERS_IN_PASSWORD);
                requirementDescriptions.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_uppercase, mPasswordMinUpperCase,
                        mPasswordMinUpperCase));
            }
            if (mPasswordMinLowerCase > 0) {
                passwordRequirements.add(MIN_LOWER_LETTERS_IN_PASSWORD);
                requirementDescriptions.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_lowercase, mPasswordMinLowerCase,
                        mPasswordMinLowerCase));
            }
            if (mPasswordMinLetters > 0) {
                //Log.d(TAG,"mPasswordMinLetters > 0 it is :" + mPasswordMinLetters);
                if (mPasswordMinLetters > mPasswordMinUpperCase + mPasswordMinLowerCase) {
                    //Log.d(TAG,"password Min Letters > " + (mPasswordMinUpperCase + mPasswordMinLowerCase));
                    passwordRequirements.add(MIN_LETTER_IN_PASSWORD);
                    requirementDescriptions.add(getResources().getQuantityString(
                            R.plurals.lockpassword_password_requires_letters, mPasswordMinLetters,
                            mPasswordMinLetters));
//                    Log.d(TAG,"requirementDescriptions add :" + getResources().getQuantityString(
//                            R.plurals.lockpassword_password_requires_letters, mPasswordMinLetters,
//                            mPasswordMinLetters));
                }
            }
            if (mPasswordMinNumeric > 0) {
                passwordRequirements.add(MIN_NUMBER_IN_PASSWORD);
                requirementDescriptions.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_numeric, mPasswordMinNumeric,
                        mPasswordMinNumeric));
            }
            if (mPasswordMinSymbols > 0) {
                passwordRequirements.add(MIN_SYMBOLS_IN_PASSWORD);
                requirementDescriptions.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_symbols, mPasswordMinSymbols,
                        mPasswordMinSymbols));
            }
            if (mPasswordMinNonLetter > 0) {
                if (mPasswordMinNonLetter > mPasswordMinNumeric + mPasswordMinSymbols) {
                    passwordRequirements.add(MIN_NON_LETTER_IN_PASSWORD);
                    requirementDescriptions.add(getResources().getQuantityString(
                            R.plurals.lockpassword_password_requires_nonletter, mPasswordMinNonLetter,

                            mPasswordMinNonLetter));
                }
            }
            // Convert list to array.
            mPasswordRequirements = passwordRequirements.stream().mapToInt(i -> i).toArray();
//            mPasswordRestrictionView =
//                    (RecyclerView) view.findViewById(R.id.password_requirements_view);
//            mPasswordRestrictionView.setLayoutManager(new LinearLayoutManager(getActivity()));
            mPasswordRequirementAdapter = new PasswordRequirementAdapter();
//            mPasswordRestrictionView.setAdapter(mPasswordRequirementAdapter);
        }

        @Override
        protected int getMetricsCategory() {
            return MetricsEvent.CHOOSE_LOCK_PASSWORD;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(mUiStage);
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(this);
            } else {
                mPasswordEntry.requestFocus();
                mKeyboardView.requestFocus();
            }
        }

        @Override
        public void onPause() {
            mHandler.removeMessages(MSG_SHOW_ERROR);
            if (mSaveAndFinishWorker != null) {
                mSaveAndFinishWorker.setListener(null);
            }
            super.onPause();
            mIsOnPauseCalled = true;
        }

        @Override
        public void onDestroy()
        {
            mIsOnPauseCalled = false;
            super.onDestroy();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(KEY_UI_STAGE, mUiStage.name());
            outState.putString(KEY_FIRST_PIN, mFirstPin);
            outState.putString(KEY_CURRENT_PASSWORD, mCurrentPassword);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case CONFIRM_EXISTING_REQUEST:
                    if (resultCode != Activity.RESULT_OK) {
                        getActivity().setResult(RESULT_FINISHED);
                        getActivity().finish();
                    } else {
                        mCurrentPassword = data.getStringExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                    }
                    break;
            }
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context, mUserId);
        }

        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;
            mUiStage = stage;
            updateUi();

            // If the stage changed, announce the header for accessibility. This
            // is a no-op when accessibility is disabled.
            if (previousStage != stage) {
                mHeaderText.announceForAccessibility(mHeaderText.getText());
            }
        }

        /**
         * Read the requirements from {@link DevicePolicyManager} and intent and aggregate them.
         *
         * @param intent the incoming intent
         */
        private void processPasswordRequirements(Intent intent) {
            final int dpmPasswordQuality = mLockPatternUtils.getRequestedPasswordQuality(mUserId);
            mRequestedQuality = Math.max(intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY,
                    mRequestedQuality), dpmPasswordQuality);
            mPasswordMinLength = Math.max(Math.max(
                    LockPatternUtils.MIN_LOCK_PASSWORD_SIZE,
                    intent.getIntExtra(PASSWORD_MIN_KEY, mPasswordMinLength)),
                    mLockPatternUtils.getRequestedMinimumPasswordLength(mUserId));
            mPasswordMaxLength = intent.getIntExtra(PASSWORD_MAX_KEY, mPasswordMaxLength);
            mPasswordMinLetters = Math.max(intent.getIntExtra(PASSWORD_MIN_LETTERS_KEY,
                    mPasswordMinLetters), mLockPatternUtils.getRequestedPasswordMinimumLetters(
                    mUserId));
            mPasswordMinUpperCase = Math.max(intent.getIntExtra(PASSWORD_MIN_UPPERCASE_KEY,
                    mPasswordMinUpperCase), mLockPatternUtils.getRequestedPasswordMinimumUpperCase(
                    mUserId));
            mPasswordMinLowerCase = Math.max(intent.getIntExtra(PASSWORD_MIN_LOWERCASE_KEY,
                    mPasswordMinLowerCase), mLockPatternUtils.getRequestedPasswordMinimumLowerCase(
                    mUserId));
            mPasswordMinNumeric = Math.max(intent.getIntExtra(PASSWORD_MIN_NUMERIC_KEY,
                    mPasswordMinNumeric), mLockPatternUtils.getRequestedPasswordMinimumNumeric(
                    mUserId));
            mPasswordMinSymbols = Math.max(intent.getIntExtra(PASSWORD_MIN_SYMBOLS_KEY,
                    mPasswordMinSymbols), mLockPatternUtils.getRequestedPasswordMinimumSymbols(
                    mUserId));
            mPasswordMinNonLetter = Math.max(intent.getIntExtra(PASSWORD_MIN_NONLETTER_KEY,
                    mPasswordMinNonLetter), mLockPatternUtils.getRequestedPasswordMinimumNonLetter(
                    mUserId));

            // Modify the value based on dpm policy.
            switch (dpmPasswordQuality) {
                case PASSWORD_QUALITY_ALPHABETIC:
                    if (mPasswordMinLetters == 0) {
                        mPasswordMinLetters = 1;
                    }
                    break;
                case PASSWORD_QUALITY_ALPHANUMERIC:
                    if (mPasswordMinLetters == 0) {
                        mPasswordMinLetters = 1;
                    }
                    if (mPasswordMinNumeric == 0) {
                        mPasswordMinNumeric = 1;
                    }
                    break;
                case PASSWORD_QUALITY_COMPLEX:
                    // Reserve all the requirements.
                    break;
                default:
                    mPasswordMinNumeric = 0;
                    mPasswordMinLetters = 0;
                    mPasswordMinUpperCase = 0;
                    mPasswordMinLowerCase = 0;
                    mPasswordMinSymbols = 0;
                    mPasswordMinNonLetter = 0;
            }

            switch (mRequestedQuality) {
                case PASSWORD_QUALITY_ALPHABETIC:
                break;
                case PASSWORD_QUALITY_ALPHANUMERIC:
                break;
                case PASSWORD_QUALITY_COMPLEX:
                break;
                case PASSWORD_QUALITY_NUMERIC:
                break;
                case PASSWORD_QUALITY_NUMERIC_COMPLEX:
                break;
                default:
            }
            mPasswordMinLengthToFulfillAllPolicies = getMinLengthToFulfillAllPolicies();
        }

        /**
         * Validates PIN and returns the validation result.
         *
         * @param password the raw password the user typed in
         * @return the validation result.
         */
        private int validatePassword(String password) {
            int errorCode = NO_ERROR;

            if (password.length() < mPasswordMinLength) {
                Log.d(TAG,"length smaller than min length " + password.length() + " " + mPasswordMinLength);
                if (mPasswordMinLength > mPasswordMinLengthToFulfillAllPolicies) {
                    errorCode |= TOO_SHORT;
                    Log.d(TAG,"ERROR: length too short");
                }
            }
            else if (password.length() > mPasswordMaxLength) {
                errorCode |= TOO_LONG;
                Log.d(TAG,"ERROR: length too long");
            }
            else
            {
                // The length requirements are fulfilled.
                if (mRequestedQuality == PASSWORD_QUALITY_NUMERIC_COMPLEX) {
                    // Check for repeated characters or sequences (e.g. '1234', '0000', '2468')
                    final int sequence = LockPatternUtils.maxLengthSequence(password);
                    if (sequence > LockPatternUtils.MAX_ALLOWED_SEQUENCE) {
                        Log.d(TAG,"ERROR: password repeat too much.");
                        errorCode |= CONTAIN_SEQUENTIAL_DIGITS;
                    }
                }
                // Is the password recently used?
                if (mLockPatternUtils.checkPasswordHistory(password, mUserId)) {
                    Log.d(TAG,"ERROR: Password is used before");
                    errorCode |= RECENTLY_USED;
                }
            }

            // Count different types of character.
            int letters = 0;
            int numbers = 0;
            int lowercase = 0;
            int symbols = 0;
            int uppercase = 0;
            int nonletter = 0;
            for (int i = 0; i < password.length(); i++) {
                char c = password.charAt(i);
                // allow non control Latin-1 characters only
                if (c < 32 || c > 127) {
                    errorCode |= CONTAIN_INVALID_CHARACTERS;
                    Log.d(TAG,"ERROR: password contain invalid characters");
                    continue;
                }
                if (c >= '0' && c <= '9') {
                    numbers++;
                    nonletter++;
                } else if (c >= 'A' && c <= 'Z') {
                    letters++;
                    uppercase++;
                } else if (c >= 'a' && c <= 'z') {
                    letters++;
                    lowercase++;
                } else {
                    symbols++;
                    nonletter++;
                }
            }

            // Ensure no non-digits if we are requesting numbers. This shouldn't be possible unless
            // user finds some way to bring up soft keyboard.
            if (mRequestedQuality == PASSWORD_QUALITY_NUMERIC || mRequestedQuality == PASSWORD_QUALITY_NUMERIC_COMPLEX) {
                if (letters > 0 || symbols > 0) {
                    Log.d(TAG,"ERROR: password contain none digits");
                    errorCode |= CONTAIN_NON_DIGITS;
                }
            }
            else
            {
                final boolean alphabetic = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
                        == mRequestedQuality;
                final boolean alphanumeric = DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC
                        == mRequestedQuality;
                if ((alphabetic || alphanumeric) && letters == 0) {
                    //return getString(R.string.lockpassword_password_requires_alpha);
                    Log.d(TAG,"ERROR: Not Enough letters");
                    errorCode |= NOT_ENOUGH_LETTER;
                }
                if (alphanumeric && numbers == 0) {
                    errorCode |= NOT_ENOUGH_DIGITS;
                    Log.d(TAG,"ERROR: password not enough ditit");
                    //return getString(R.string.lockpassword_password_requires_digit);
                }
            }

            // Check the requirements one by one.
            for (int i = 0; i < mPasswordRequirements.length; i++) {
                int passwordRestriction = mPasswordRequirements[i];
                switch (passwordRestriction) {
                    case MIN_LETTER_IN_PASSWORD:
                        if (letters < mPasswordMinLetters) {
                            Log.d(TAG,"ERROR: NOT_ENOUGH_LETTER");
                            errorCode |= NOT_ENOUGH_LETTER;
                        }
                        break;
                    case MIN_UPPER_LETTERS_IN_PASSWORD:
                        if (uppercase < mPasswordMinUpperCase) {
                            Log.d(TAG,"ERROR: NOT_ENOUGH_UPPER_CASE");
                            errorCode |= NOT_ENOUGH_UPPER_CASE;
                        }
                        break;
                    case MIN_LOWER_LETTERS_IN_PASSWORD:
                        if (lowercase < mPasswordMinLowerCase) {
                            Log.d(TAG,"ERROR: NOT_ENOUGH_LOWER_CASE");
                            errorCode |= NOT_ENOUGH_LOWER_CASE;
                        }
                        break;
                    case MIN_SYMBOLS_IN_PASSWORD:
                        if (symbols < mPasswordMinSymbols) {
                            Log.d(TAG,"ERROR: NOT_ENOUGH_SYMBOLS");
                            errorCode |= NOT_ENOUGH_SYMBOLS;
                        }
                        break;
                    case MIN_NUMBER_IN_PASSWORD:
                        if (numbers < mPasswordMinNumeric) {
                            Log.d(TAG,"ERROR: NOT_ENOUGH_DIGITS");
                            errorCode |= NOT_ENOUGH_DIGITS;
                        }
                        break;
                    case MIN_NON_LETTER_IN_PASSWORD:
                        if (nonletter < mPasswordMinNonLetter) {
                            Log.d(TAG,"ERROR: NOT_ENOUGH_NON_LETTER");
                            errorCode |= NOT_ENOUGH_NON_LETTER;
                        }
                        break;
                }
            }
            return errorCode;
        }

        public void handleNext() {
            Log.d(TAG,"handle next button");
            if (mSaveAndFinishWorker != null)
            {
                Log.d(TAG,"m Save And Finish Worker not null, return");
                return;
            }
            mChosenPassword = mPasswordEntry.getText().toString();
            if (TextUtils.isEmpty(mChosenPassword)) {
                return;
            }
            if (mUiStage == Stage.Introduction) {
                Log.d(TAG,"stage introduction");
                if (validatePassword(mChosenPassword) == NO_ERROR) {
					Log.d(TAG,"NO ERROR");
                    mFirstPin = mChosenPassword;
                    mPasswordEntry.setText("");
                    updateStage(Stage.NeedToConfirm);
                }
                else
                {
                    Log.d(TAG,"Error of validate password and do nothing.");
                }
            } else if (mUiStage == Stage.NeedToConfirm) {
				Log.d(TAG,"stage NeedToConfirm");
                if (mFirstPin.equals(mChosenPassword)) {
					Log.d(TAG,"mFirstPin equal password");
                    startSaveAndFinish();
                } else {
					Log.d(TAG,"first pin not equal password");
                    CharSequence tmp = mPasswordEntry.getText();
                    if (tmp != null) {
                        Selection.setSelection((Spannable) tmp, 0, tmp.length());
                    }
                    updateStage(Stage.ConfirmWrong);
                }
            }
        }

        protected void setNextEnabled(boolean enabled) {
            if(mNextButton != null)
            {
                mNextButton.setEnabled(enabled);
                ColorStateList csList =  getResources().getColorStateList(enabled ?
                        R.color.security_button_enable_color:R.color.security_button_disable_color);
                mNextButton.setTextColor(csList);
            }
        }

        protected void setNextText(int text) {
            if(mNextButton != null)
            {
                mNextButton.setText(text);
            }
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.next_button:
                    handleNext();
                    break;

                case R.id.cancel_button:
                    getActivity().finish();
                    break;
            }
        }

        private void showError(String msg, final Stage next) {
            mHeaderText.setText(msg);
            mHeaderText.announceForAccessibility(mHeaderText.getText());
            Message mesg = mHandler.obtainMessage(MSG_SHOW_ERROR, next);
            mHandler.removeMessages(MSG_SHOW_ERROR);
            mHandler.sendMessageDelayed(mesg, ERROR_MESSAGE_TIMEOUT);
        }
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

        /**
         * @param errorCode error code returned from {@link #validatePassword(String)}.
         * @return an array of messages describing the error, important messages come first.
         */
        private String[] convertErrorCodeToMessages(int errorCode) {
            Log.d(TAG,"Convert error code to message,error code:" + errorCode);
            List<String> messages = new ArrayList<>();
            if ((errorCode & CONTAIN_INVALID_CHARACTERS) > 0) {
                messages.add(getString(R.string.lockpassword_illegal_character));
            }
            if ((errorCode & CONTAIN_NON_DIGITS) > 0) {
                messages.add(getString(R.string.lockpassword_pin_contains_non_digits));
            }
            if ((errorCode & NOT_ENOUGH_UPPER_CASE) > 0) {
                messages.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_uppercase, mPasswordMinUpperCase,
                        mPasswordMinUpperCase));
            }
            if ((errorCode & NOT_ENOUGH_LOWER_CASE) > 0) {
                messages.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_lowercase, mPasswordMinLowerCase,
                        mPasswordMinLowerCase));
            }
            if ((errorCode & NOT_ENOUGH_LETTER) > 0) {
                messages.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_letters, mPasswordMinLetters,
                        mPasswordMinLetters));
            }
            if ((errorCode & NOT_ENOUGH_DIGITS) > 0) {
                messages.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_numeric, mPasswordMinNumeric,
                        mPasswordMinNumeric));
            }
            if ((errorCode & NOT_ENOUGH_SYMBOLS) > 0) {
                messages.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_symbols, mPasswordMinSymbols,
                        mPasswordMinSymbols));
            }
            if ((errorCode & NOT_ENOUGH_NON_LETTER) > 0) {
                messages.add(getResources().getQuantityString(
                        R.plurals.lockpassword_password_requires_nonletter, mPasswordMinNonLetter,
                        mPasswordMinNonLetter));
            }
            if ((errorCode & TOO_SHORT) > 0) {
                messages.add(getString(mIsAlphaMode ?
                        R.string.lockpassword_password_too_short
                        : R.string.lockpassword_pin_too_short, mPasswordMinLength));
            }
            if ((errorCode & TOO_LONG) > 0) {
                messages.add(getString(mIsAlphaMode ?
                        R.string.lockpassword_password_too_long
                        : R.string.lockpassword_pin_too_long, mPasswordMaxLength + 1));
            }
            if ((errorCode & CONTAIN_SEQUENTIAL_DIGITS) > 0) {
                messages.add(getString(R.string.lockpassword_pin_no_sequential_digits));
            }
            if ((errorCode & RECENTLY_USED) > 0) {
                messages.add(getString((mIsAlphaMode) ? R.string.lockpassword_password_recently_used
                        : R.string.lockpassword_pin_recently_used));
            }
            return messages.toArray(new String[0]);
        }

        private int getMinLengthToFulfillAllPolicies() {
            final int minLengthForLetters = Math.max(mPasswordMinLetters,
                    mPasswordMinUpperCase + mPasswordMinLowerCase);

            final int minLengthForNonLetters = Math.max(mPasswordMinNonLetter,
                    mPasswordMinSymbols + mPasswordMinNumeric);
            return minLengthForLetters + minLengthForNonLetters;
        }

        /**
         * Update the hint based on current Stage and length of password entry
         */
        private void updateUi() {
            Log.d(TAG,"upsate UI to ::" + mUiStage);
            final boolean canInput = mSaveAndFinishWorker == null;
            String password = mPasswordEntry.getText().toString();
            final int length = password.length();
            if (mUiStage == Stage.Introduction) {
                if(length <= mPasswordMaxLength)
                {
                    Log.d(TAG,"Password length is valid show normal title.");
                    mHeaderText.setText(getString(R.string.lockpassword_input_your_first_password_header_nf));
                }
                if (mIsAlphaMode) {
                    mPasswordHintText.setText(getString(R.string.lockpassword_choose_your_password_hint_nf));
                } else {
                    mPasswordHintText.setText(getString(R.string.lockpassword_choose_your_pin_hint_nf));
                }
//                mPasswordRestrictionView.setVisibility(View.VISIBLE);
                final int errorCode = validatePassword(password);
                String[] messages = convertErrorCodeToMessages(errorCode);
                // Update the fulfillment of requirements.
                mPasswordRequirementAdapter.setRequirements(messages);
                // Enable/Disable the next button accordingly.
                setNextEnabled(errorCode == NO_ERROR);
            } else {
                // Hide password requirement view when we are just asking user to confirm the pw.
//                mPasswordRestrictionView.setVisibility(View.GONE);
                setHeaderText(getString(
                        mIsAlphaMode ? mUiStage.alphaHint : mUiStage.numericHint));
                setNextEnabled(canInput && length > 0);
            }
            setNextText(mUiStage.buttonText);
            mPasswordEntryInputDisabler.setInputEnabled(canInput);
        }

        private void setHeaderText(String text) {
            // Only set the text if it is different than the existing one to avoid announcing again.
            if (!TextUtils.isEmpty(mHeaderText.getText())
                    && mHeaderText.getText().toString().equals(text)) {
                return;
            }
            mHeaderText.setText(text);
        }

        public void afterTextChanged(Editable s) {
            // Changing the text while error displayed resets to NeedToConfirm state
            int length = mPasswordEntry.getText().toString().length();
            if (mUiStage == Stage.ConfirmWrong) {
                mUiStage = Stage.NeedToConfirm;
            }
            else if (mUiStage == Stage.Introduction)
            {
                if(length > mPasswordMaxLength)
                {
                    String ErrorStr = getString(mIsAlphaMode ?
                            R.string.lockpassword_password_too_long : R.string.lockpassword_pin_too_long, mPasswordMaxLength + 1);

                    mHeaderText.setText(ErrorStr);
                }
                else
                {
                    mHeaderText.setText(getString(R.string.lockpassword_input_your_first_password_header_nf));
                }
            }
            // Schedule the UI update.
            mTextChangedHandler.notifyAfterTextChanged();
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        private void startSaveAndFinish() {
            if (mSaveAndFinishWorker != null) {
                Log.w(TAG, "startSaveAndFinish with an existing SaveAndFinishWorker.");
                return;
            }

            mPasswordEntryInputDisabler.setInputEnabled(false);
            setNextEnabled(false);

            mSaveAndFinishWorker = new SaveAndFinishWorker();
            mSaveAndFinishWorker.setListener(this);

            getFragmentManager().beginTransaction().add(mSaveAndFinishWorker,
                    FRAGMENT_TAG_SAVE_AND_FINISH).commit();
            getFragmentManager().executePendingTransactions();

            final boolean required = getActivity().getIntent().getBooleanExtra(
                    EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, EncryptionInterstitial.DEFAULT_REQUIRE_PASSWORD);
            mSaveAndFinishWorker.start(mLockPatternUtils, required, mHasChallenge, mChallenge,
                    mChosenPassword, mCurrentPassword, mRequestedQuality, mUserId);
        }

        @Override
        public void onChosenLockSaveFinished(boolean wasSecureBefore, Intent resultData) {
            getActivity().setResult(RESULT_FINISHED, resultData);

            if (!wasSecureBefore) {
                Intent intent = getRedactionInterstitialIntent(getActivity());
                if (intent != null) {
                    intent.putExtra(EXTRA_HIDE_DRAWER, mHideDrawer);
                    startActivity(intent);
                }
            }
            getActivity().finish();
        }

        class TextChangedHandler extends Handler {
            private static final int ON_TEXT_CHANGED = 1;
            private static final int DELAY_IN_MILLISECOND = 100;

            /**
             * With the introduction of delay, we batch processing the text changed event to reduce
             * unnecessary UI updates.
             */
            private void notifyAfterTextChanged() {
                removeMessages(ON_TEXT_CHANGED);
                sendEmptyMessageDelayed(ON_TEXT_CHANGED, DELAY_IN_MILLISECOND);
            }

            @Override
            public void handleMessage(Message msg) {
                if (getActivity() == null) {
                    return;
                }
                if (msg.what == ON_TEXT_CHANGED) {
                    updateUi();
                }
            }
        }
    }

    private static class SaveAndFinishWorker extends SaveChosenLockWorkerBase {

        private String mChosenPassword;
        private String mCurrentPassword;
        private int mRequestedQuality;

        public void start(LockPatternUtils utils, boolean required,
                boolean hasChallenge, long challenge,
                String chosenPassword, String currentPassword, int requestedQuality, int userId) {
            prepare(utils, required, hasChallenge, challenge, userId);

            mChosenPassword = chosenPassword;
            mCurrentPassword = currentPassword;
            mRequestedQuality = requestedQuality;
            mUserId = userId;

            start();
        }

        @Override
        protected Intent saveAndVerifyInBackground() {
            Intent result = null;
            mUtils.saveLockPassword(mChosenPassword, mCurrentPassword, mRequestedQuality,
                    mUserId);

            if (mHasChallenge) {
                byte[] token;
                try {
                    token = mUtils.verifyPassword(mChosenPassword, mChallenge, mUserId);
                } catch (RequestThrottledException e) {
                    token = null;
                }

                if (token == null) {
                    Log.e(TAG, "critical: no token returned for known good password.");
                }

                result = new Intent();
                result.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
            }

            return result;
        }
    }
}
