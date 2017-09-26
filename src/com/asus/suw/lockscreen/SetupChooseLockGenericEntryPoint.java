package com.asus.suw.lockscreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.settings.AsusSetupChooseLockGeneric;
import com.android.settings.R;
import com.android.settings.SetupChooseLockGeneric;
import com.android.settings.SetupWizardUtils;

/**
 * Created by yueting-wong on 2016/9/8.
 */
public class SetupChooseLockGenericEntryPoint extends Activity {

    protected static final int CHOOSE_LOCK_GENERIC_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);

        Intent intent = new Intent();
        AsusSuwUtilisClient client = new AsusSuwUtilisClient(this);
        if(client.getSetupWizardLayout_Short() != null){
            String clazz = AsusSetupChooseLockGeneric.class.getName();
            intent.setClassName("com.android.settings", clazz);
        }else{
            String clazz = SetupChooseLockGeneric.class.getName();
            intent.setClassName("com.android.settings", clazz);
        }

        Intent fromIntent = getIntent();
        if(fromIntent != null){
            SetupWizardUtils.copySetupExtras(fromIntent, intent);
        }
        startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST) {
            setResult(resultCode, data);
            finish();
        }
    }
}
