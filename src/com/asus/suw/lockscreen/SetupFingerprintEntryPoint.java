package com.asus.suw.lockscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.android.settings.R;
import com.android.settings.fingerprint.SetupFingerprintEnrollIntroduction;
import com.android.settings.SetupWizardUtils;

/**
 * Created by yueting-wong on 2016/9/8.
 */
public class SetupFingerprintEntryPoint extends FingerprintEntryPoint {

    protected static final int FINGERPRINT_INTRODUCTION_REQUEST = 1;

    @Override
    protected void start(){
        Intent intent = new Intent();
        AsusSuwUtilisClient client = new AsusSuwUtilisClient(this);
        if(client.getSetupWizardLayout_Short() != null){
            String clazz = AsusSetupFingerprintEnrollIntroduction.class.getName();
            intent.setClassName("com.android.settings", clazz);
        }else{
            String clazz = SetupFingerprintEnrollIntroduction.class.getName();
            intent.setClassName("com.android.settings", clazz);
        }

        Intent fromIntent = getIntent();
        if(fromIntent != null){
            SetupWizardUtils.copySetupExtras(fromIntent, intent);
        }
        startActivityForResult(intent, FINGERPRINT_INTRODUCTION_REQUEST);
    }
}
