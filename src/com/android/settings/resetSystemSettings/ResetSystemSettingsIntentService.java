package com.android.settings.resetSystemSettings;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.resetSystemSettings.resetDatabaseTables.ResetGlobalTable;
import com.android.settings.resetSystemSettings.resetDatabaseTables.ResetSecureTable;
import com.android.settings.resetSystemSettings.resetDatabaseTables.ResetSystemTable;
import com.android.settings.resetSystemSettings.resetDeveloperOptions.ResetDeveloperOptions;
import com.android.settings.resetSystemSettings.resetSoundAndNotification.ResetSoundAndNotification;

/**
 * Created by JimCC
 * TODO
 * string translation
 */
public class ResetSystemSettingsIntentService extends IntentService {

    private static final String TAG = "ResetSystemSettingsIntentService";
    private static final int NOTIFICATION_ID_RESET_SYSTEM_SETTINGS = 9527;

    public static final String SHARED_PREFERENCE_NAME = "resetSystemSettings";
    public static final String PREFERENCE_RESET_ONGOING = "resetOngoing";

    private Handler mHandler;

    public ResetSystemSettingsIntentService() {
        super(TAG);
        setIntentRedelivery(true);
        mHandler = new Handler();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "onHandleIntent");

        // Final check for the reset process
        if (!Utils.isVerizonSKU()) return ;
        if (!android.os.Process.myUserHandle().isOwner()) return ;

        final Context context = getApplicationContext();

        // Set true (ongoing)
        SharedPreferences resetSharedPreferences =
                getApplicationContext().getSharedPreferences(SHARED_PREFERENCE_NAME, 0);
        SharedPreferences.Editor editor = resetSharedPreferences.edit();
        editor.putBoolean(PREFERENCE_RESET_ONGOING, true).commit();

        // Notification, ongoing
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setContentTitle(context.getString(
                                R.string.reset_settings_title_notification))
                        .setContentText(context.getString(
                                R.string.reset_settings_ongoing_notification))
                        .setSmallIcon(R.drawable.empty_icon)
                        .setOngoing(true)
                        .setProgress(0, 0, true);
        NotificationManagerCompat notificationCompat = NotificationManagerCompat.from(context);
        notificationCompat.notify(NOTIFICATION_ID_RESET_SYSTEM_SETTINGS, builder.build());

        // +++
        // Reset all system settings
        // Add new classes / codes to complete the work if needed
        //

        ResetSystemTable resetSystemTable =
                new ResetSystemTable(context);
        resetSystemTable.run();

        ResetSecureTable resetSecureTable =
                new ResetSecureTable(context);
        resetSecureTable.run();

        ResetGlobalTable resetGlobalTable =
                new ResetGlobalTable(context);
        resetGlobalTable.run();

        ResetDeveloperOptions resetDeveloperOptions =
                new ResetDeveloperOptions(context);
        resetDeveloperOptions.run();

        ResetSoundAndNotification resetSoundAndNotification =
                new ResetSoundAndNotification(context);
        resetSoundAndNotification.run();
        //
        // End of the reset process
        // +++

        // Set false (finished)
        editor.putBoolean(PREFERENCE_RESET_ONGOING, false).commit();

        // Notification, finished
        builder.setContentText(context.getString(
                R.string.reset_settings_complete_notification))
                .setOngoing(false)
                .setProgress(0, 0, false);
        notificationCompat.notify(NOTIFICATION_ID_RESET_SYSTEM_SETTINGS, builder.build());

        // Show a toast to notify the user
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,
                        R.string.reset_settings_complete_toast, Toast.LENGTH_LONG).show();
            }
        });
        // Note: the toast will not be shown if using a local variable
    }
}
