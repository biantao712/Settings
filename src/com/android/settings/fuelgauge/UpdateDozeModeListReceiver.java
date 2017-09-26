package com.android.settings.fuelgauge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.android.settings.fuelgauge.PowerWhitelistBackend;
import com.android.settings.fuelgauge.UpdateDozeModeWhiteListService;

import java.util.ArrayList;

/**
 * Created by hungchein on 11/30/15.
 */
public class UpdateDozeModeListReceiver extends BroadcastReceiver{
    public final static String TAG = "UpdateDozeModeListReceiver";
    public final static boolean DEBUG = true;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "UpdateDozeModeListReceiver onReceive, intent: " + intent);
        }
        String action = intent.getAction();
        if("com.android.settings.GET_NO_OPTIMIZED_APP_NUM".equals(action)) {
            Intent intentService = new Intent(context, UpdateDozeModeWhiteListService.class);
            intentService.putExtra("ACTION", UpdateDozeModeWhiteListService.MSG_GET_NO_OPTIMISED_APPS);
            context.startService(intentService);
        }
        if("com.android.settings.OPTIMIZED_ALL_USER_WHITELIST".equals(action)) {
            Intent intentService = new Intent(context, UpdateDozeModeWhiteListService.class);
            intentService.putExtra("ACTION", UpdateDozeModeWhiteListService.MSG_OPTIMISED_USER_WHITELIST_APPS);
            context.startService(intentService);
        }
        if("com.android.settings.OPTIMIZED_ADD_USER_WHITELIST".equals(action)) {
            Intent intentService = new Intent(context, UpdateDozeModeWhiteListService.class);
            intentService.putExtra("ACTION", UpdateDozeModeWhiteListService.MSG_OPTIMZED_ADD_USER_WHITELIST_APPS);
            context.startService(intentService);
        }
        if("com.android.settings.CHECK_ASUS_WHITELIST_INSTALL".equals(action)) {
            Intent intentService = new Intent(context, UpdateDozeModeWhiteListService.class);
            intentService.putExtra("ACTION", UpdateDozeModeWhiteListService.MSG_CHECK_ASUS_WHITELIST_INATLL);
            context.startService(intentService);
        }
        if("com.android.settings.ADD_WHITELIST".equals(action)) {
            Intent intentService = new Intent(context, UpdateDozeModeWhiteListService.class);
            intentService.putExtra("ACTION", UpdateDozeModeWhiteListService.MSG_ADD_WHITELIST);
            context.startService(intentService);
        }
    }
}
