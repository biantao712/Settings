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
import android.content.res.Resources;
import android.os.UserHandle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;

public class AsusSetupFingerprintEnrollFindSensor extends AsusFingerprintEnrollFindSensor {

    @Override
    protected Intent getEnrollingIntent() {
        Intent intent = new Intent(this, AsusSetupFingerprintEnrollEnrolling.class);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        SetupWizardUtils.copySetupExtras(getIntent(), intent);
        return intent;
    }

    @Override
    protected void initViews() {
        mSuwClient.initActivity(this);

        final View nextButton = findViewById(R.id.next_button);
        if (nextButton != null) {
            nextButton.setVisibility(View.GONE);
        }
        getBackButton().setVisibility(View.GONE);
    }

    @Override
    protected void hideSuwNavigation(){

    }

    @Override
    protected Button getNextButton() {
        return mSuwClient.getNextButton();
    }

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
        return MetricsEvent.FINGERPRINT_FIND_SENSOR_SETUP;
    }
}
