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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.fingerprint.FingerprintEnrollSidecar.Listener;

/**
 * Activity explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class CNFingerprintEnrollFindSensor extends CNFingerprintEnrollBase
                implements View.OnClickListener{

    private static String TAG = "CNFingerprintEnrollFindSensor";
    private static boolean DEBUG = true;

    private static final int CONFIRM_REQUEST = 1;
    private static final int ENROLLING = 2;
    public static final String EXTRA_KEY_LAUNCHED_CONFIRM = "launched_confirm_lock";
    public static final String EXTRA_KEY_SENEOR_POSITION = "fingerprint_sensor_position";

    private FingerprintFindSensorAnimation mAnimation;
    private boolean mLaunchedConfirmLock;
    private FingerprintEnrollSidecar mSidecar;
    private boolean mNextClicked;

    private TextView mMessage;
    private CNAsusFindFingerprintSensorView mSensorPositionView;
    private boolean mPosition = true; //True == front, False == back

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        if (savedInstanceState != null) {
            mLaunchedConfirmLock = savedInstanceState.getBoolean(EXTRA_KEY_LAUNCHED_CONFIRM);
            mToken = savedInstanceState.getByteArray(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        }
        if (mToken == null && !mLaunchedConfirmLock) {
            launchConfirmLock();
        } else if (mToken != null) {
            startLookingForFingerprint(); // already confirmed, so start looking for fingerprint
        }

        //++ Asus feature
        //mAnimation = (FingerprintFindSensorAnimation) findViewById(
        //        R.id.fingerprint_sensor_location_animation);
        mSensorPositionView = (CNAsusFindFingerprintSensorView)findViewById(R.id.find_sensor_view);
        mMessage = (TextView)findViewById(R.id.find_sensor_text);
        mPosition = CNAsusFindFingerprintSensorView.SENSOR_FRONT.equals(mSensorPositionView.getPosition()) ||
                CNAsusFindFingerprintSensorView.SENSOR_FRONT2.equals(mSensorPositionView.getPosition());
        if(mPosition){
            mMessage.setText(R.string.cn_asus_security_settings_fingerprint_enroll_find_sensor_message_front);
        }else{
            mMessage.setText(R.string.cn_asus_security_settings_fingerprint_enroll_find_sensor_message_back);
        }
        Button nextBtn = (Button)findViewById(R.id.next);
        nextBtn.setOnClickListener(this);

        //-- Asus feature
    }

    protected int getContentView() {
        return R.layout.cn_fingerprint_enroll_find_sensor;
    }

    @Override
    protected void onStart() {
        super.onStart();
        //mAnimation.startAnimation();
    }
    private boolean mNeedReunlock = false;
    @Override
    public void onRestart(){
        super.onRestart();
        if (mNeedReunlock){
            mNeedReunlock = false;
            Intent intent = new Intent();
            intent.setAction("android.settings.SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
        }
    }

    private void startLookingForFingerprint() {
        mSidecar = (FingerprintEnrollSidecar) getFragmentManager().findFragmentByTag(
                CNFingerprintEnrollEnrolling.TAG_SIDECAR);
        if (mSidecar == null) {
            mSidecar = new FingerprintEnrollSidecar();
            getFragmentManager().beginTransaction()
                    .add(mSidecar, CNFingerprintEnrollEnrolling.TAG_SIDECAR).commit();
        }
        mSidecar.setListener(new Listener() {
            @Override
            public void onEnrollmentProgressChange(int steps, int remaining) {
                mNextClicked = true;
                if (!mSidecar.cancelEnrollment()) {
                    proceedToEnrolling();
                }
            }

            @Override
            public void onEnrollmentHelp(CharSequence helpString) {
            }

            @Override
            public void onEnrollmentError(int errMsgId, CharSequence errString) {
                if (mNextClicked && errMsgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                    mNextClicked = false;
                    proceedToEnrolling();
                }
            }
        });
    }

    @Override
    public void onStop(){
        super.onStop();
        mNeedReunlock = true;
        //mAnimation.stopAnimation();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //mAnimation.stopAnimation();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_KEY_LAUNCHED_CONFIRM, mLaunchedConfirmLock);
        outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
    }


    private void proceedToEnrolling() {
        Log.d(TAG, "proceedToEnrolling");
        if(mSidecar != null){
           getFragmentManager().beginTransaction().remove(mSidecar).commit();
        }else{
           Log.d(TAG, "mSidecar is null");
        }
        mSidecar = null;
        startActivityForResult(getEnrollingIntent(), ENROLLING);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mNeedReunlock = false;
        if (requestCode == CONFIRM_REQUEST) {
            if (resultCode == RESULT_OK) {
                mToken = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);
                getIntent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startLookingForFingerprint();
            } else {
                finish();
            }
        } else if (requestCode == ENROLLING) {
            //++ Asus flow: return token to FingerprintSettings
            if(data == null){
                data = new Intent();
            }
            data.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            //-- Asus flow: return token to FingerprintSettings
            if (resultCode == RESULT_FINISHED) {
                setResult(RESULT_FINISHED, data); // Asus flow: return token to FingerprintSettings
                finish();
            } else if (resultCode == RESULT_SKIP) {
                setResult(RESULT_SKIP, data);  // Asus flow: return token to FingerprintSettings
                finish();
            } else if (resultCode == RESULT_TIMEOUT) {
                setResult(RESULT_TIMEOUT);
                finish();
            } else if (resultCode == RESULT_STOP) {
                mNeedReunlock = true;
            }else {
                FingerprintManager fpm = getSystemService(FingerprintManager.class);
                int enrolled = fpm.getEnrolledFingerprints().size();
                int max = getResources().getInteger(
                        com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
                if (enrolled >= max) {
                    finish();
                } else {
                    // We came back from enrolling but it wasn't completed, start again.
                    startLookingForFingerprint();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void launchConfirmLock() {
        long challenge = getSystemService(FingerprintManager.class).preEnroll();
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
        boolean launchedConfirmationActivity = false;
        if (mUserId == UserHandle.USER_NULL) {
            launchedConfirmationActivity = helper.launchConfirmationActivity(CONFIRM_REQUEST,
                getString(R.string.security_settings_fingerprint_preference_title),
                null, null, challenge);
        } else {
            launchedConfirmationActivity = helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_fingerprint_preference_title),
                    null, null, challenge, mUserId);
        }
        if (!launchedConfirmationActivity) {
            // This shouldn't happen, as we should only end up at this step if a lock thingy is
            // already set.
            finish();
        } else {
            mLaunchedConfirmLock = true;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_FIND_SENSOR;
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onNextButtonClick");
        mNextClicked = true;
        if (mSidecar == null || (mSidecar != null && !mSidecar.cancelEnrollment())) {
            proceedToEnrolling();
        }
    }

}
