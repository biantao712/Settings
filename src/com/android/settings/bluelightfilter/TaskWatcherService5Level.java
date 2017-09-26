package com.android.settings.bluelightfilter;

import com.asus.splendidcommandagent.ISplendidCommandAgentService;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.android.settings.R;
/**
 * For AMAX 1.5
 * with 5 level reading-mode
 * with 4 level screen-mode
 * @author ChungYi_Wu
 *
 */
public class TaskWatcherService5Level extends Service {
    private static final String TAG = "TaskWatcherService5LForSetting";
    private ISplendidCommandAgentService mService;
    private boolean mIsBootComplete = false;
    private int mQuickSettingOnOff = -1;
    private int mModeSettingChangeMode = -1;
    private int mUserSwitched = -1;
    private int mChangeScreenMode = -1;
    private boolean mDoCommand = false;
    private boolean mSendNotification = false;
    private int mDefaultScreenMode;

    //Create a Service connection
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Log.d(TAG, "onServiceConnected");
            mService = ISplendidCommandAgentService.Stub.asInterface(service);
            doTask();
            if (Constants.PRIVATE_DEBUG) Log.d(TAG, "mService:"+mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            Log.d(TAG, "onServiceDisconnected");
            mService = null;
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!BlueLightFilterHelper.hasSplendidFeature(this)){
            return START_NOT_STICKY;
        }
        if (Constants.PRIVATE_DEBUG) Log.d(TAG, "onStartCommand");
        mDefaultScreenMode = Constants.getDefaultScreenMode(this);

        boolean success = false;
        Intent explicitIntent = Constants.getExplicitIntent(this, ISplendidCommandAgentService.class.getName());
        if (explicitIntent != null) {
            success = bindServiceAsUser(explicitIntent, mConnection, Context.BIND_AUTO_CREATE, UserHandle.CURRENT);
        }
        if (Constants.PRIVATE_DEBUG)  Log.d(TAG, "bind success:"+success);

        if (intent != null) {
            mIsBootComplete = intent.getBooleanExtra("boot_complete", false);
            mQuickSettingOnOff = intent.getIntExtra(Constants.EXTRA_QUICKSETTING_READER_MODE_ON_OFF, -1);
            mModeSettingChangeMode = intent.getIntExtra(Constants.EXTRA_MODESETTING_CHANGE_MODE, -1);
            mChangeScreenMode = intent.getIntExtra(Constants.EXTRA_QUICKSETTING_CHANGE_SCREEN_MODE, -1);
            mDoCommand = intent.getBooleanExtra(Constants.EXTRA_DO_COMMAND,false);
            mSendNotification = intent.getBooleanExtra(Constants.EXTRA_SEND_NOTIFICATION,false);
        }
        if (mService != null) {
            doTask();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (Constants.PRIVATE_DEBUG) Log.d(TAG, "onDestroy");
        if (Constants.PRIVATE_DEBUG) Log.d(TAG,"unbind service");
        unbindService(mConnection);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void doTask() {
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (mIsBootComplete || mUserSwitched != -1){
            if(mIsBootComplete && !pm.isScreenOn()){
                SharedPreferences pref = getSharedPreferences(Constants.SP_NAME, Context.MODE_PRIVATE);
                pref.edit().putBoolean(Constants.SP_KEY_BOOT_COMPLETE_COMMAND, true).commit();
            }else{
                int screen_mode = Settings.System.getInt(getContentResolver(),
                        Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
                doChangeScreenMode(screen_mode, mIsBootComplete);
                sendScreenModeNotify(screen_mode);
            }
        } else if (mQuickSettingOnOff != -1) {
            if (Constants.DEBUG) Log.d(TAG, "quick_setting_on_off:" + mQuickSettingOnOff);
        Settings.System.putInt(this.getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, mQuickSettingOnOff);
            doQuickSettingOnOff(mQuickSettingOnOff);
        } else if (mModeSettingChangeMode != -1) {
            if (Constants.DEBUG) Log.d(TAG, "mode_setting_change_mode:" + mModeSettingChangeMode);
            doReadingMode(this, false);
        } else if (mChangeScreenMode != -1) {
            doChangeScreenMode(mChangeScreenMode, false);
            sendScreenModeNotify(mChangeScreenMode);
            Settings.System.putInt(getContentResolver(),
                    Constants.ASUS_SPLENDID_SCREEN_MODE_OPTION, mChangeScreenMode);
        }else if(mDoCommand && pm.isScreenOn()){
            int screen_mode = Settings.System.getInt(getContentResolver(),
                    Constants.ASUS_SPLENDID_SCREEN_MODE_OPTION, -1);
            if (screen_mode == -1)
                screen_mode = getLastScreenMode();
            Settings.System.putInt(getContentResolver(),
                    Constants.ASUS_SPLENDID_SCREEN_MODE_OPTION, screen_mode);
            doChangeScreenMode(screen_mode, mDoCommand);
            sendScreenModeNotify(screen_mode);
            SharedPreferences pref = getSharedPreferences(Constants.SP_NAME, Context.MODE_PRIVATE);
            pref.edit().putBoolean(Constants.SP_KEY_BOOT_COMPLETE_COMMAND, false).commit();
        }else if(mSendNotification) {
            int screen_mode = Settings.System.getInt(getContentResolver(),
                    Constants.ASUS_SPLENDID_SCREEN_MODE_OPTION, Constants.SCREEN_MODE_OPTION_BALANCE);
            sendScreenModeNotify(screen_mode);
        }
        stopSelf();
    }

    private int getLastScreenMode(){
        int screen_mode = Constants.SCREEN_MODE_OPTION_BALANCE;
        int reading_mode = Settings.System.getInt(getContentResolver(),Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
        //SharedPreferences sharedPreferences = getSharedPreferences(Constants.SP_NAME, Context.MODE_PRIVATE);

        if(reading_mode == 1)
            screen_mode = Constants.SCREEN_MODE_OPTION_READING;
        else
            screen_mode = Constants.SCREEN_MODE_OPTION_BALANCE;
        return screen_mode;
    }

    private void sendScreenModeNotify(int screen_mode)
    {
        String key = Constants.SP_KEY_NEED_TO_SHOW_NOTIFICATION;
        SharedPreferences pref = getSharedPreferences(Constants.SP_NAME, Context.MODE_PRIVATE);
        boolean showNotification = pref.getBoolean(key, true);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if(!showNotification) {
            notificationManager.cancel(1);
            return;
        }

        Intent notificationIntent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        int notifiColor = R.color.notification_background;
        String title = getString(R.string.notification_bluelight_filter_title);
        int iconId = R.drawable.asus_notification_ic_screenmode_blance;
        int summaryId = R.string.notification_bluelight_filter_title;
        switch (screen_mode) {
        case Constants.SCREEN_MODE_OPTION_BALANCE:
            iconId = R.drawable.asus_notification_ic_screenmode_blance;
            showNotification = false;
            break;
        case Constants.SCREEN_MODE_OPTION_READING:
            summaryId = R.string.notification_bluelight_filter_summary;
            iconId = R.drawable.asus_notification_ic_screenmode_reading;
            break;
        }
        if(Constants.isAndroidL(this)) notifiColor = R.color.notification_background_L;
        if(showNotification) {
            Notification notification = new NotificationCompat.Builder(this)
            .setSmallIcon(iconId)
            .setColor(getResources().getColor(notifiColor))
            .setWhen(System.currentTimeMillis())
            .setContentTitle(title)
            .setContentText(getString(summaryId))
            .setContentIntent(pendingIntent)
            .build();
            notificationManager.notify(1, notification);
        } else {
            notificationManager.cancel(1);
        }
    }

    private void doChangeScreenMode(int screen_mode_option, boolean isBoot) {
        switch (screen_mode_option) {
        case Constants.SCREEN_MODE_OPTION_READING:
            doReadingMode(this, isBoot);
            break;
        case Constants.SCREEN_MODE_OPTION_BALANCE:
            doBalanceMode(this);
            break;
        }
    }

    /**
     * Only for QuickSettings with 2 level reading-mode
     * @param isOn
     */
    private void doQuickSettingOnOff(int isOn) {
        int target_screent_mode;
        if (isOn == 1) {
            target_screent_mode = Constants.SCREEN_MODE_OPTION_READING;
            doReadingMode(this, false);
        } else {
            target_screent_mode = Constants.SCREEN_MODE_OPTION_BALANCE;
            doBalanceMode(this);
        }

        Settings.System.putInt(getContentResolver(),
                Constants.ASUS_SPLENDID_SCREEN_MODE_OPTION, target_screent_mode);
        sendScreenModeNotify(target_screent_mode);
    }

    private void doReadingMode(Context context, boolean isBoot) {
        if (Constants.PRIVATE_DEBUG) Log.d(TAG, "doReadingMode");
        int option = Settings.System.getInt(this.getContentResolver(),
                Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_LEVEL_RDWEAK);
        if (option < 0 || option > 4) option = 0;
        String reading_mode;
        reading_mode = Constants.READING_MODE_OPTION2LUT[option];
        Log.d(TAG, "Reading-mode option: "+reading_mode);

        String bootCmd ="";
        //odm don't need -b parameter
        //if (isBoot) bootCmd = "-b true";
        int cmdCTMode = -1;
        int cmdHSVMode = -1;
        cmdCTMode = SplendidCommand.MODE_CT;
        cmdHSVMode = SplendidCommand.MODE_HSV;

        if(SplendidCommand.isCommandExists(cmdCTMode)){
            try {
                SplendidCommand.run(mService, cmdCTMode, bootCmd + " -d " + reading_mode);
            } catch (Exception e) {
                Log.w(TAG, "run command error!" + e);
            }
        } else Log.d(TAG, SplendidCommand.COMMAND_NAME_LIST[cmdCTMode] + " not exist!");

        if(SplendidCommand.isCommandExists(cmdHSVMode)){
            try {
                SplendidCommand.run(mService, cmdHSVMode, bootCmd + " -r " + reading_mode);
            } catch (Exception e) {
                Log.w(TAG, "run command error!" + e);
            }
        } else Log.d(TAG, SplendidCommand.COMMAND_NAME_LIST[cmdHSVMode] + " not exist!");
        Settings.System.putInt(this.getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 1);
    }

    public void doBalanceMode(Context context){
        String balance_mode = Constants.BALACE_MODE_OPTION;
        int cmdCTMode = SplendidCommand.MODE_CT;
        int cmdHSVMode = SplendidCommand.MODE_HSV;

        if(SplendidCommand.isCommandExists(cmdHSVMode)){
            try {
                SplendidCommand.run(mService, cmdHSVMode, " -h 0.0 -s 1.0 -i 1.0");
            } catch (Exception e) {
                Log.w(TAG, "run command error!" + e);
            }
        } else Log.d(TAG, SplendidCommand.COMMAND_NAME_LIST[cmdHSVMode] + " not exist!");

        if(SplendidCommand.isCommandExists(cmdCTMode)){
            try {
                SplendidCommand.run(mService, cmdCTMode, " -d " + balance_mode);
            } catch (Exception e) {
                Log.w(TAG, "run command error!" + e);
            }
        } else Log.d(TAG, SplendidCommand.COMMAND_NAME_LIST[cmdCTMode] + " not exist!");

        Settings.System.putInt(this.getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
    }
}
