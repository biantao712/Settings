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

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;

/**
 * Activity which concludes fingerprint enrollment.
 */
public class CNFingerprintEnrollFinish extends CNFingerprintEnrollBase
                implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cn_fingerprint_enroll_finish);
        Button addButton = (Button) findViewById(R.id.add_another_button);

        Button doneButton = (Button) findViewById(R.id.next_button);
        Button doneButton2 = (Button) findViewById(R.id.next_button2);
        FingerprintManager fpm = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        int enrolled = fpm.getEnrolledFingerprints(mUserId).size();
        int max = getResources().getInteger(
                com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);

        if (enrolled >= max) {
            /* Don't show "Add" button if too many fingerprints already added */
            addButton.setVisibility(View.GONE);
            doneButton.setVisibility(View.GONE);
            doneButton2.setVisibility(View.VISIBLE);
            doneButton2.setOnClickListener(this);
        } else {
            addButton.setVisibility(View.VISIBLE);
            doneButton.setVisibility(View.VISIBLE);
            doneButton2.setVisibility(View.GONE);
            addButton.setOnClickListener(this);
            doneButton.setOnClickListener(this);
        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_another_button) {
            final Intent intent = getEnrollingIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(intent);
            finish();
        } else if (v.getId() == R.id.next_button || v.getId() == R.id.next_button2){
            setResult(RESULT_FINISHED);
            finish();
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_FINISH;
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
    @Override
    public void onStop(){
        super.onStop();
        mNeedReunlock = true;
    }

}
