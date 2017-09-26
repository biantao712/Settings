package com.android.settings;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.android.settings.search.Index;
import com.android.settings.search.IndexDatabaseHelper;

/**
 * Created by Blenda_Fu on 2017/4/12.
 */

public class SearchBlankActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        String action = intent.getStringExtra(IndexDatabaseHelper.IndexColumns.INTENT_ACTION);
        String key = intent.getStringExtra(IndexDatabaseHelper.IndexColumns.DATA_KEY_REF);
        String className = intent.getStringExtra(IndexDatabaseHelper.IndexColumns.CLASS_NAME);
        String screenTitle = intent.getStringExtra(IndexDatabaseHelper.IndexColumns.SCREEN_TITLE);



        if (TextUtils.isEmpty(action)) {
            Bundle args = new Bundle();
            args.putString(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);

            Intent newintent = Utils.onBuildStartFragmentIntent(this, className, args, null,
                    -1, screenTitle, false);
            startActivityForResult(newintent, 1);
//            Utils.startWithFragment(this, className, args, null, 0, -1, screenTitle);
        } else {
            final Intent newIntent = new Intent(action);

            String targetPackage = intent.getStringExtra(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_PACKAGE);
            String targetClass = intent.getStringExtra(IndexDatabaseHelper.IndexColumns.INTENT_TARGET_CLASS);
            if (!TextUtils.isEmpty(targetPackage) && !TextUtils.isEmpty(targetClass)) {
                final ComponentName component =
                        new ComponentName(targetPackage, targetClass);
                intent.setComponent(component);
            }
            newIntent.putExtra(SettingsActivity.EXTRA_FRAGMENT_ARG_KEY, key);

            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            this.finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
