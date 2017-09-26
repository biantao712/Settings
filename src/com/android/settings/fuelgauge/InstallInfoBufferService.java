package com.android.settings.fuelgauge;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by hungchein on 1/4/16.
 */
public class InstallInfoBufferService extends Service {

    public static final boolean DEBUG = true;
    public static final String TAG = "InstallInfoBufferService";

    public static final int MSG_PACKAGE_ADD = 1;
    public Context mContext = null;
    public InstallMsgHandler mInstallMsgHandler;
    private HandlerThread mInstallMsgThread;
    private static final long INSTALL_INFO_DELAY = 5*60*1000;//5min

    public InstallInfoBufferService() {
        if(DEBUG) {
            Log.d(TAG, "InstallInfoBufferService constructor init");
        }
        mInstallMsgThread = new HandlerThread("InstallMsgHandler");
        mInstallMsgThread.start();
        mInstallMsgHandler = new InstallMsgHandler(mInstallMsgThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(DEBUG) {
            Log.d(TAG, "onStartCommand");
        }
        if(intent != null) {
            int what = intent.getIntExtra("ACTION", 0);
            if (what > 0) {
                sendMsgByIntent(what, startId);
            }
        }
        return Service.START_REDELIVER_INTENT;
    }

    private void sendMsgByIntent(int what, int startId) {
        if (mInstallMsgHandler != null) {
            if (DEBUG) {
                Log.d(TAG, "onStartCommand msg delay buffer");
            }
            mInstallMsgHandler.removeMessages(what);
            Message msg = mInstallMsgHandler.obtainMessage();
            msg.what = what;
            msg.arg1 = startId;
            mInstallMsgHandler.sendMessageDelayed(msg, INSTALL_INFO_DELAY);
        } else {
            if (DEBUG) {
                Log.d(TAG, "onStartCommand mInstallMsgHandler==null");
            }
        }
    }

    @Override
    public void onCreate() {
        if(DEBUG) {
            Log.d(TAG, "InstallInfoBufferService onCreate()");
        }
        mContext = getApplicationContext();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if(DEBUG) {
            Log.d(TAG, "InstallInfoBufferService onDestroy()");
        }
        mInstallMsgThread.quit();
        mInstallMsgThread.interrupt();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class InstallMsgHandler extends Handler {

        public InstallMsgHandler(Looper l) {
            super(l);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PACKAGE_ADD: {
                    if(DEBUG) {
                        Log.d(TAG, "handleMessage MSG_PACKAGE_ADD");
                    }
                    Intent intent = new Intent("com.android.settings.CHECK_ASUS_WHITELIST_INSTALL");
                    intent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                    sendBroadcast(intent);
                    stopSelf(msg.arg1);
                }
                break;
            }
        }
    }
}
