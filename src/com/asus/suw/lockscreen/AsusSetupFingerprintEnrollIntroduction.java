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

package com.asus.suw.lockscreen;

import android.content.Intent;
import android.os.UserHandle;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.AsusSetupChooseLockGeneric;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;

import com.android.settings.fingerprint.SetupSkipDialog;

public class AsusSetupFingerprintEnrollIntroduction extends AsusFingerprintEnrollIntroduction {

    @Override
    protected Intent getChooseLockIntent() {
        final Intent intent = new Intent(this, AsusSetupChooseLockGeneric.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected Intent getFindSensorIntent() {
        final Intent intent = new Intent(this, AsusSetupFingerprintEnrollFindSensor.class);
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }


    @Override
    protected void initViews() {
        mSuwClient.initActivity(this);

        final View buttonBar = findViewById(R.id.button_bar);
        if (buttonBar != null) {
            buttonBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FINGERPRINT_FIND_SENSOR_REQUEST) {
            if (data == null) {
                data = new Intent();
            }
            LockPatternUtils lockPatternUtils = new LockPatternUtils(this);
            data.putExtra(AsusSetupChooseLockGeneric.
                    AsusSetupChooseLockGenericFragment.EXTRA_PASSWORD_QUALITY,
                    lockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()));
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onCancelButtonClick() {
        SetupSkipDialog dialog = SetupSkipDialog.newInstance(
                getIntent().getBooleanExtra(SetupSkipDialog.EXTRA_FRP_SUPPORTED, false));
        dialog.show(getFragmentManager());
    }

    @Override
    protected void hideSuwNavigation(){

    }

    @Override
    protected Button getNextButton() {
        return mSuwClient.getNextButton();
    }

    @Override
    protected Button getBackButton(){
        return mSuwClient.getBackButton();
    }

    @Override
    public void onBackButtonClick() {
        onBackPressed();
    }

    @Override
    public void onNextButtonClick() {
        super.onNextButtonClick();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_INTRO_SETUP;
    }
}
