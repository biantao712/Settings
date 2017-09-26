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

import android.annotation.Nullable;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.InstrumentedActivity;
import com.android.settings.R;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.NavigationBar;
import com.android.internal.logging.MetricsLogger;
import com.asus.cncommonres.AsusActivity;

/**
 * Base activity for all fingerprint enrollment steps.
 */
public abstract class CNFingerprintEnrollBase extends AsusActivity{
    public static final int RESULT_FINISHED = FingerprintSettings.RESULT_FINISHED;
    public static final int RESULT_SKIP = FingerprintSettings.RESULT_SKIP;
    public static final int RESULT_TIMEOUT = FingerprintSettings.RESULT_TIMEOUT;
    public static final int RESULT_STOP = FingerprintSettings.RESULT_STOP;

    protected byte[] mToken;
    protected int mUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToken = getIntent().getByteArrayExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        if (savedInstanceState != null && mToken == null) {
            mToken = savedInstanceState.getByteArray(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        }
        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        setActionBar();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(getColor(R.color.action_bar_background));

    }

    private void setActionBar(){
        setAsusActionBarType(AsusActivity.ASUS_ACTIONBAR_SECOND_LEVEL);
        setTitle(R.string.cn_fingerprint);

    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
    }

    protected Intent getEnrollingIntent() {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", CNFingerprintEnrollEnrolling.class.getName());
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        return intent;
    }
    protected abstract int getMetricsCategory();

    @Override
    public void onResume() {
        super.onResume();
        MetricsLogger.visible(this, getMetricsCategory());
    }

    @Override
    public void onPause() {
        super.onPause();
        MetricsLogger.hidden(this, getMetricsCategory());
    }
}
