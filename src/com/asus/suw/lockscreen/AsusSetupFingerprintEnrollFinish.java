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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.UserHandle;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SetupWizardUtils;

public class AsusSetupFingerprintEnrollFinish extends AsusFingerprintEnrollFinish{

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
        //super.initViews();
        mSuwClient.initActivity(this);

        final View nextButton = findViewById(R.id.next_button);
        if (nextButton != null) {
            nextButton.setVisibility(View.GONE);
        }

        getBackButton().setVisibility(View.GONE);

        final TextView message = (TextView) findViewById(R.id.message);
        message.setText(R.string.asus_security_settings_fingerprint_enroll_finish_message);
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
        //++ Asus setup wizard style
        overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);
        //-- Asus setup wizard style

    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_FINISH_SETUP;
    }
}
