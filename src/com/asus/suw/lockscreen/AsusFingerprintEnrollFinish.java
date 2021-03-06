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

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.fingerprint.AsusFindFingerprintSensorView;
import com.android.settings.fingerprint.FingerprintEnrollBase;

/**
 * Activity which concludes fingerprint enrollment.
 */
public class AsusFingerprintEnrollFinish extends AsusFingerprintEnrollBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSubContentView(R.layout.asus_fingerprint_enroll_finish);
        setHeaderText(R.string.asus_security_settings_fingerprint_enroll_finish_title);
        Button addButton = (Button) findViewById(R.id.add_another_button);

        FingerprintManager fpm = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        int enrolled = fpm.getEnrolledFingerprints(mUserId).size();
        int max = getResources().getInteger(
                com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
        //++ Asus feature
        String position = SystemProperties.get("ro.hardware.fp_position");
        TextView hint = (TextView)findViewById(R.id.message_add_more_hint);
        if(AsusFindFingerprintSensorView.SENSOR_FRONT.equals(position)){
            hint.setText(R.string.asus_security_settings_fingerprint_enroll_finish_more_hint_front);
        }
        //-- Asus feature
        if (enrolled >= max) {
            /* Don't show "Add" button if too many fingerprints already added */
            addButton.setVisibility(View.GONE);
            hint.setVisibility(View.GONE);
        } else {
            addButton.setOnClickListener(this);
        }
    }

    @Override
    protected void onNextButtonClick() {
        setResult(RESULT_FINISHED);
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_another_button) {
            final Intent intent = getEnrollingIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);
            finish();
        }
        super.onClick(v);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_FINISH;
    }
}
