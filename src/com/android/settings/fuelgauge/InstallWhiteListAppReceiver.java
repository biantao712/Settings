package com.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.android.settings.fuelgauge.InstallInfoBufferService;
/**
 * Created by hungchein on 1/4/16.
 */
public class InstallWhiteListAppReceiver extends BroadcastReceiver{
    private static final String TAG = "InstallWhiteListAppReceiver";
    private static final boolean DEBUG = true;
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            int uid = intent.getIntExtra("EXTRA_UID", -1);
            String pkg = getPackageName(intent);
            boolean replacing = intent.getBooleanExtra("EXTRA_REPLACING", false);
            if(DEBUG) {
                Log.d(TAG,"onReceive "+action+" uid: "+uid+" replacing: "+replacing+" pkg: "+pkg);
            }
            Intent intentService = new Intent(context, InstallInfoBufferService.class);
            intentService.putExtra("ACTION", InstallInfoBufferService.MSG_PACKAGE_ADD);
            context.startService(intentService);
        }
    }

    String getPackageName(Intent intent) {
        Uri uri = intent.getData();
        String pkg = uri != null ? uri.getSchemeSpecificPart() : null;
        return pkg;
    }
}
