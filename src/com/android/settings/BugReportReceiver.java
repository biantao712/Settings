package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

public class BugReportReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "BugReportReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_TAG, "Received : com.asus.userfeedback.settings.BUG_REPORT");
        Settings.System.putInt(context.getContentResolver(), "bugreport_from_ASUSHelp", 1);
        SystemProperties.set("ctl.start", "bugreport");
    }

}
