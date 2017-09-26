package com.android.settings.bluelightfilter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.location.SettingInjectorService;
import android.os.UserHandle;

public class BluelightFilterReceiver extends BroadcastReceiver{
    private static final String TAG = "BluelightFilterReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        if(!BlueLightFilterHelper.isODMDevice(context)){
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Log.d(TAG, action);
            Intent service_intent = new Intent(context, TaskWatcherService5Level.class);
            service_intent.putExtra("boot_complete", true);
            //context.startService(service_intent);
            context.startServiceAsUser(service_intent, UserHandle.CURRENT);
        }else if (Intent.ACTION_USER_PRESENT.equals(action)) {
            SharedPreferences pref = context.getSharedPreferences(Constants.SP_NAME, Context.MODE_PRIVATE);
            if (pref.getBoolean(Constants.SP_KEY_BOOT_COMPLETE_COMMAND, false)){
                Log.d(TAG, action);
                Intent service_intent = new Intent(context, TaskWatcherService5Level.class);
                service_intent.putExtra(Constants.EXTRA_DO_COMMAND, true);
                context.startService(service_intent);
                context.startServiceAsUser(service_intent, UserHandle.CURRENT);
            }
        }
    }

}
