package com.asus.suw.lockscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.android.settings.R;
import com.android.settings.fingerprint.CNFingerprintEnrollIntroduction;
import com.android.settings.fingerprint.FingerprintEnrollIntroduction;

/**
 * Created by yueting-wong on 2016/9/8.
 */
public class FingerprintEntryPoint extends Activity {

    protected static final int FINGERPRINT_INTRODUCTION_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setTheme(R.style.Theme_FingerprintEnroll);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        getWindow().setStatusBarColor(getColor(R.color.asus_suw_status_bar));
        getWindow().setBackgroundDrawableResource(R.color.asus_suw_bg);
        overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);

        start();
    }

    protected void start(){
        Intent intent = new Intent();
        AsusSuwUtilisClient client = new AsusSuwUtilisClient(this);
/*        if(client.getSetupWizardLayout_Short() != null){
            String clazz = AsusFingerprintEnrollIntroduction.class.getName();
            intent.setClassName("com.android.settings", clazz);
        }else{
            String clazz = FingerprintEnrollIntroduction.class.getName();
            intent.setClassName("com.android.settings", clazz);
        }*/
        String clazz = CNFingerprintEnrollIntroduction.class.getName();
        intent.setClassName("com.android.settings", clazz);
        startActivityForResult(intent, FINGERPRINT_INTRODUCTION_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FINGERPRINT_INTRODUCTION_REQUEST) {
            setResult(resultCode, data);
            finish();
        }
    }
}
