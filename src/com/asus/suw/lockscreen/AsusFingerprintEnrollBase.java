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

import android.annotation.Nullable;
import android.content.res.Resources;
import android.content.pm.ActivityInfo;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.InstrumentedActivity;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.SetupWizardUtils;
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.fingerprint.FingerprintEnrollBase;

/**
 * Base activity for all fingerprint enrollment steps.
 */
public abstract class AsusFingerprintEnrollBase extends InstrumentedActivity
        implements View.OnClickListener {

    static final int RESULT_FINISHED = FingerprintEnrollBase.RESULT_FINISHED;
    static final int RESULT_SKIP = FingerprintEnrollBase.RESULT_SKIP;
    static final int RESULT_TIMEOUT = FingerprintEnrollBase.RESULT_TIMEOUT;

    protected byte[] mToken;
    protected int mUserId;

    protected AsusSuwUtilisClient mSuwClient;
    private ViewGroup mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSuwClient = new AsusSuwUtilisClient(this);
        mLayout = mSuwClient.getSetupWizardLayout_Short();

        setTheme(R.style.Theme_FingerprintEnroll);
        mToken = getIntent().getByteArrayExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        if (savedInstanceState != null && mToken == null) {
            mToken = savedInstanceState.getByteArray(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        }
        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());

        setContentView(mLayout);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if(mLayout == null){
            finish();
        }

        initViews();
        initButtonListener();
        hideSuwNavigation();
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        resid = SetupWizardUtils.getAsusTheme(getIntent());
        super.onApplyThemeResource(theme, resid, first);
    }

    protected void hideSuwNavigation(){
        mSuwClient.getNavigationBar().setVisibility(View.GONE);
    }

    protected void initViews() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        getWindow().setStatusBarColor(getColor(R.color.asus_suw_status_bar));
    }

    protected void initButtonListener(){
        Button nextButton = getNextButton();
        if (nextButton != null) {
            nextButton.setOnClickListener(this);
        }
        Button backButton = getBackButton();
        if (backButton != null) {
            backButton.setOnClickListener(this);
        }
    }

    protected void setSubContentView(int resId){
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View myView = layoutInflater.inflate(resId, null);
        mSuwClient.setSubContentView(myView, false /* No need content top padding*/);
    }



    protected void setHeaderText(int resId) {
        mSuwClient.setHeaderText(resId);
    }

    protected Button getNextButton() {
        return (Button) findViewById(R.id.next_button);
    }

    protected Button getBackButton(){
        return null;
    }


    @Override
    public void onClick(View v) {
        if(v == getBackButton()) {
            onBackButtonClick();
        }else if (v == getNextButton()) {
            onNextButtonClick();
        }
    }

    protected void onNextButtonClick() {
    }

    public void onBackButtonClick() {
    }

    protected Intent getEnrollingIntent() {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", AsusFingerprintEnrollEnrolling.class.getName());
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        return intent;
    }
}
