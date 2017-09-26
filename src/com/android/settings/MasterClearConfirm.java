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

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.inputmethodservice.KeyboardView;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.service.persistentdata.PersistentDataBlockManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.PasswordEntryKeyboardHelper;
import com.android.internal.widget.PasswordEntryKeyboardView;
import com.android.settingslib.RestrictedLockUtils;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Calendar;

/**
 * Confirm and execute a reset of the device to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL ERASE EVERYTHING
 * ON THE PHONE" prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
public class MasterClearConfirm extends OptionsMenuFragment {

    private static final String TAG = "MasterClearConfirm";
    private View mContentView;
    private boolean mEraseSdCard;
    private Button mFinalButton;
    //+++MikeHsu
    private TextView mCheckEditText;
    private TextView mHintNumber;
    private String mCheckNumber;
    //---MikeHsu

    //+++MikeHsu
    private boolean DEBUG = false;
    //---MikeHsu

    // BEGIN channing_yeh@asus.com
    // For feature: Erase SD card
    private StorageVolume mMicroSD = null;
    private boolean mEraseMircoSD;
    // END channing_yeh@asus.com

    // porting keyboard from ScreenLock->PIN: ConfirmLockPassword.java
    private KeyboardView mKeyboardView;
    private PasswordEntryKeyboardHelper mKeyboardHelper;

    // BEGIN channing_yeh@asus.com, fix TT-891809
    private InputMethodManager mImm;
    // END channing_yeh@asus.com

    // BEGIN channing_yeh@asus.com
    // Factory Reset protection while in Verizon demo mode
    private static final String DEMO_MODE = "store_demo_mode";
    private static final String DEMO_PASSWORD = "#VerizonDemoUnit#";
    private TextView mHintMessage;
    // END channing_yeh@asus.com

    // Kevin_Chiou Verizon VZ_REQ_UI_15743
    private static final String FACTORY_PARTITION_ROOT_PATH = "/factory";
    private static final String FACTORY_RESET_TIME_FILE_NAME = "factory_reset";

    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and invoke the Checkin Service to reset the device to its factory-default
     * state (rebooting in the process).
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }

            final PersistentDataBlockManager pdbManager = (PersistentDataBlockManager)
                    getActivity().getSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE);
            // BEGIN channing_yeh@asus.com
            // For feature: Erase SD card
            final StorageManager storageManager = (StorageManager)
                    getActivity().getSystemService(Context.STORAGE_SERVICE);
            final VolumeInfo volumeInfo = (mMicroSD != null) ?
                    storageManager.findVolumeByUuid(mMicroSD.getUuid()) : null;
            final boolean doWipePdbManager = pdbManager != null && !pdbManager.getOemUnlockEnabled() &&
                    Utils.isDeviceProvisioned(getActivity());
            final boolean doEraseSdCard = mEraseMircoSD && storageManager != null && volumeInfo != null;
            // END channing_yeh@asus.com

            if (doWipePdbManager || doEraseSdCard) { // channing_yeh@asus.com
                // if OEM unlock is enabled, this will be wiped during FR process. If disabled, it
                // will be wiped here, unless the device is still being provisioned, in which case
                // the persistent data block will be preserved.
                new AsyncTask<Void, Void, Void>() {
                    int mOldOrientation;
                    ProgressDialog mProgressDialog;

                    @Override
                    protected Void doInBackground(Void... params) {
                        if (doWipePdbManager) { // channing_yeh@asus.com
                            pdbManager.wipe();
                        }
                        // BEGIN channing_yeh@asus.com
                        // For feature: Erase SD card
                        if (doEraseSdCard) {
                            storageManager.partitionPublic(volumeInfo.getDiskId());
                        }
                        // END channing_yeh@asus.com
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        mProgressDialog.hide();
                        if (getActivity() != null) {
                            getActivity().setRequestedOrientation(mOldOrientation);
                            doMasterClear();
                        }
                    }

                    @Override
                    protected void onPreExecute() {
                        mProgressDialog = getProgressDialog();
                        mProgressDialog.show();

                        // need to prevent orientation changes as we're about to go into
                        // a long IO request, so we won't be able to access inflate resources on flash
                        mOldOrientation = getActivity().getRequestedOrientation();
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    }
                }.execute();
            } else {
                doMasterClear();
            }
        }

        private ProgressDialog getProgressDialog() {
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setTitle(
                    getActivity().getString(R.string.master_clear_progress_title));
            progressDialog.setMessage(
                    getActivity().getString(R.string.master_clear_progress_text));
            return progressDialog;
        }
    };

    // +++ Kevin_Chiou Verizon VZ_REQ_UI_15743
    public void setLastFactoryTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        Writer writer;
        File file = new File(FACTORY_PARTITION_ROOT_PATH, FACTORY_RESET_TIME_FILE_NAME);
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(DateFormat.getDateTimeInstance().format(calendar.getTime()));
            writer.close();
        } catch (IOException e) {
            Log.d(TAG, "Unable to write to factory partition.");
        }
    }

    // --- Kevin_Chiou Verizon VZ_REQ_UI_15743


    private void doMasterClear() {
        if (DEBUG) {
            Log.d(TAG, "doMasterClear()");
        }

        //Kevin_Chiou Verizon VZ_REQ_UI_15743
        setLastFactoryTime();

        Intent intent = new Intent(Intent.ACTION_MASTER_CLEAR);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.putExtra(Intent.EXTRA_REASON, "MasterClearConfirm");
        intent.putExtra(Intent.EXTRA_WIPE_EXTERNAL_STORAGE, mEraseSdCard);
        getActivity().sendBroadcast(intent);
        // Intent handling is asynchronous -- assume it will happen soon.
    }

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState(String saveNumber) {
        mFinalButton = (Button) mContentView.findViewById(R.id.execute_master_clear);
        mFinalButton.setOnClickListener(mFinalClickListener);

        Context context = getActivity();
        // BEGIN channing_yeh@asus.com
        // Factory Reset protection while in Verizon demo mode
        if (Utils.isVerizonSKU() && Settings.Secure.getInt(context.getContentResolver(), DEMO_MODE, 0) != 0) {
            TextView masterClearConfirm = (TextView) mContentView.findViewById(R.id.master_clear_confirm);
            masterClearConfirm.setText(R.string.verizon_demo_mode_factory_reset_master_clear_final_desc);
            LinearLayout layout = (LinearLayout) mContentView.findViewById(R.id.content_layout);
            layout.setOrientation(LinearLayout.VERTICAL);
            mHintMessage = (TextView) mContentView.findViewById(R.id.hint_messeage_to_input);
            mHintMessage.setText(R.string.verizon_demo_mode_factory_reset_password_header_text);
            mHintNumber = (TextView) mContentView.findViewById(R.id.hint_number);
            mHintNumber.setVisibility(View.GONE);

            TextView tempEditText = (TextView) mContentView.findViewById(R.id.check_number);
            tempEditText.setVisibility(View.GONE);
            mCheckEditText = (TextView) mContentView.findViewById(R.id.check_password);
            mCheckEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
            mCheckEditText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(DEMO_PASSWORD.length()) });
            mCheckEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            mCheckEditText.setVisibility(View.VISIBLE);
            mCheckNumber = DEMO_PASSWORD;
        }
        // Don't show factory reset 4 digit PIN
        else if (Utils.isVerizonSKU()) {
            mContentView.findViewById(R.id.hint_messeage_to_input).setVisibility(View.GONE);
            mCheckEditText = (TextView) mContentView.findViewById(R.id.check_number);
            mCheckEditText.setVisibility(View.GONE);
            mHintNumber = (TextView) mContentView.findViewById(R.id.hint_number);
            mHintNumber.setVisibility(View.GONE);
            mCheckNumber = "";
            mFinalButton.setEnabled(true);
        }
        // END channing_yeh@asus.com
        else {
            mCheckEditText = (TextView) mContentView.findViewById(R.id.check_number);
            mCheckEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
            //+++MikeHsu
            mHintNumber = (TextView) mContentView.findViewById(R.id.hint_number);
            //+++MikeHsu@20130718 fix TT-288676
            if (TextUtils.isEmpty(saveNumber)) {
                mCheckNumber = getRandomNumber();
            } else {
                String sHintNumber = saveNumber + " : ";
                final SpannableStringBuilder sp = new SpannableStringBuilder(sHintNumber);
                sp.setSpan(new ForegroundColorSpan(0xff0c0c0c), 5, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                mHintNumber.setText(sp);
                mCheckNumber = saveNumber;
            }
            //---MikeHsu@20130718 fix TT-288676
            //---MikeHsu
        }

        // porting keyboard from ScreenLock->PIN: ConfirmLockPassword.java
        mKeyboardView = (PasswordEntryKeyboardView) mContentView.findViewById(R.id.keyboard);
        mKeyboardHelper = new PasswordEntryKeyboardHelper(context, mKeyboardView, mCheckEditText);
        // BEGIN channing_yeh@asus.com
        // Factory Reset protection while in Verizon demo mode
        if (Utils.isVerizonSKU() && Settings.Secure.getInt(context.getContentResolver(), DEMO_MODE, 0) != 0) {
            mKeyboardHelper.setKeyboardMode(PasswordEntryKeyboardHelper.KEYBOARD_MODE_ALPHA);
        } else {
            mKeyboardHelper.setKeyboardMode(PasswordEntryKeyboardHelper.KEYBOARD_MODE_NUMERIC);
        }
        // END channing_yeh@asus.com
        mKeyboardView.requestFocus();

        mCheckEditText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {}
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String inputValue = mCheckEditText.getText().toString();
                if (inputValue.equals(mCheckNumber)) {
                    mFinalButton.setEnabled(true);
                } else {
                    mFinalButton.setEnabled(false);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_FACTORY_RESET, UserHandle.myUserId());
        if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_FACTORY_RESET, UserHandle.myUserId())) {
            return inflater.inflate(R.layout.master_clear_disallowed_screen, null);
        } else if (admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(View.VISIBLE);
            return view;
        }
        mContentView = inflater.inflate(R.layout.master_clear_confirm, null);
        mContentView.setBackgroundColor(getActivity().getResources().getColor(R.color.category_divider_background));
        //+++MikeHsu@20130718 fix TT-288676
        String number = null;
        if (savedInstanceState != null && !TextUtils.isEmpty(savedInstanceState.getString("hintnumber"))) {
            number = savedInstanceState.getString("hintnumber");
        }
        establishFinalConfirmationState(number);
        //---MikeHsu@20130718 fix TT-288676
        setAccessibilityTitle();
        // BEGIN channing_yeh@asus.com, fix TT-891809
        mImm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        // END channing_yeh@asus.com
        return mContentView;
    }

    private void setAccessibilityTitle() {
        CharSequence currentTitle = getActivity().getTitle();
        TextView confirmationMessage =
                (TextView) mContentView.findViewById(R.id.master_clear_confirm);
        if (confirmationMessage != null) {
            String accessibileText = new StringBuilder(currentTitle).append(",").append(
                    confirmationMessage.getText()).toString();
            getActivity().setTitle(Utils.createAccessibleSequence(currentTitle, accessibileText));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mEraseSdCard = args != null
                && args.getBoolean(MasterClear.ERASE_EXTERNAL_EXTRA);
        String build = SystemProperties.get("ro.build.type", "unknown");
        DEBUG = (build.toLowerCase().equals("user") == true) ? false : true;
        // BEGIN channing_yeh@asus.com
        // For feature: Erase SD card
        mEraseMircoSD = args != null && args.getBoolean(MasterClear.ERASE_MICROSD_EXTRA, false);
        mMicroSD = args != null ? (StorageVolume) args.getParcelable(MasterClear.ERASE_VOLUME_EXTRA) : null;

        if (DEBUG) {
            Log.d(TAG, "mEraseSdCard: " + mEraseSdCard);
            Log.d(TAG, "mEraseMircoSD: " + mEraseMircoSD);
            if (mMicroSD != null) {
                Log.d(TAG, "mMicroSD: " + mMicroSD.getPath());
                Log.d(TAG, "mMicroSD: " + mMicroSD.getId());
            }
        }
        // END channing_yeh@asus.com
    }

    //+++MikeHsu
    public String getRandomNumber() {
        String randomNumber = "";
        for (int i = 0; i < 4; i++) {
            randomNumber += String.valueOf((int) (Math.random() * 10));
        }

        final String sHintNumber = randomNumber + " : ";
        final SpannableStringBuilder sp = new SpannableStringBuilder(sHintNumber);
        sp.setSpan(new ForegroundColorSpan(0xff0c0c0c), 5, 6, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mHintNumber.setText(sp);
        return randomNumber;
    }

    //+++MikeHsu@20130718 fix TT-288676
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("hintnumber", mCheckNumber);
    }
    //---MikeHsu@20130718 fix TT-288676

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.MASTER_CLEAR_CONFIRM;
    }

    // BEGIN channing_yeh@asus.com, fix TT-891809
    @Override
    public void onResume() {
        super.onResume();
        if (!(Utils.isVerizonSKU() && Settings.Secure.getInt(getActivity().getContentResolver(), DEMO_MODE, 0) == 0)) {
            mCheckEditText.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mImm.showSoftInput(mCheckEditText, 0);
                }
            }, 300);
        }
    }
    // END channing_yeh@asus.com
}
