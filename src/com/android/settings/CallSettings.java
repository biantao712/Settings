package com.android.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.List;
import com.android.internal.logging.MetricsProto;

public class CallSettings extends SettingsPreferenceFragment {

    private static final String TAG = "CallSettings";
    public static final String CALL_SETTINGS_PACKAGE = "com.android.phone";
    public static final String CALL_SETTINGS_CLASS = "com.android.phone.CallFeaturesSetting";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setClassName(CALL_SETTINGS_PACKAGE, CALL_SETTINGS_CLASS);
        Log.d(TAG, "onCreate(): startActivity on Phone for intent = " + intent + " when CallFeaturesSetting is not on top");
        startActivity(intent);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        finish();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsProto.MetricsEvent.MAIN_SETTINGS;
    }
}
