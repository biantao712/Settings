package com.android.settings.fuelgauge;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.settings.fuelgauge.PowerWhitelistBackend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;

//add for doze mode cn white list start
import android.os.Build;
//add for doze mode cn white list end

public class UpdateDozeModeWhiteListService extends Service {

    public static final boolean DEBUG = true;
    public static final String TAG = "UpdateDozeModeWhiteListService";

    public static final int MSG_LIVE_UPDATE = 1;
    public static final int MSG_GET_NO_OPTIMISED_APPS=2;
    public static final int MSG_OPTIMISED_USER_WHITELIST_APPS=3;
    public static final int MSG_CHECK_ASUS_WHITELIST_INATLL=4;
    public static final int MSG_ADD_WHITELIST = 5;
    public static final int MSG_OPTIMZED_ADD_USER_WHITELIST_APPS=6;
    public Context mContext = null;
    public UpdateDozeHandler mUpdateDozeHandler;
    private PowerWhitelistBackend mBackend;
    private static final long DELAY_TIME = 1000;

    //BEGIN:Steve_Ke@asus.com
    private final static boolean DozeModeProcessSwitch = true;
    //END:Steve_Ke@asus.com

    //add for doze mode cn white list start
    private static final boolean IS_BUILD_CNSKU = Build.ASUSSKU.equals("CN");
    //add for doze mode cn white list end

    public UpdateDozeModeWhiteListService() {
        if(DEBUG) {
            Log.d(TAG, "UpdateDozeModeWhiteListService constructor init");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(DEBUG) {
            Log.d(TAG, "UpdateDozeModeWhiteListService onStartCommand");
        }
        boolean enabled = getApplicationContext().getResources().getBoolean(
                com.android.internal.R.bool.config_enableAutoPowerModes);

        if (DozeModeProcessSwitch && enabled) {
            Log.d("steve","mBackend.getDozeHThread().getLooper()");
            if(intent != null) {
                int what = intent.getIntExtra("ACTION", 0);
                //Detect whether is live update
                if (what == 0) {
                    //add for doze mode cn white list
                    if (IS_BUILD_CNSKU) {
                        if ("asus.intent.action.DOZEMODE_CN_WHITELIST_UPDATED".equals(intent.getAction())) {
                            if (DEBUG) {
                                Log.d(TAG, "Start service for doze mode whitelist update in CN SKU ");
                            }
                            what = MSG_LIVE_UPDATE;
                        }
                    } else if ("asus.intent.action.DOZEMODE_WHITELIST_UPDATED".equals(intent.getAction())) {
                        if (DEBUG) {
                            Log.d(TAG, "Start service for doze mode whitelist update");
                            what = MSG_LIVE_UPDATE;
                        }
                    }
                }
                if (what > 0) {
                    sendMsgByIntent(what, startId, intent);
                }
            }
        } else {
            stopSelf();
        }

        return Service.START_REDELIVER_INTENT;
    }

    private void sendMsgByIntent(int what, int startId, Intent intent) {
        if (mUpdateDozeHandler != null) {
            if (DEBUG) {
                Log.d(TAG, "UpdateDozeModeWhiteListService onStartCommand by "+what);
            }
            mUpdateDozeHandler.removeMessages(what);
            Message msg = mUpdateDozeHandler.obtainMessage();
            msg.what = what;
            msg.arg1 = startId;
            //add for remote file
            msg.obj = intent.getData();
            //add for remote file
            if(msg.obj == null) {
                msg.obj = intent.getExtras();
            }
            mUpdateDozeHandler.sendMessageDelayed(msg, DELAY_TIME);
        } else {
            if (DEBUG) {
                Log.d(TAG, "UpdateDozeModeWhiteListService onStartCommand mUpdateDozeHandler==null, can't update");
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if(DEBUG) {
            Log.d(TAG, "UpdateDozeModeWhiteListService onCreate()");
        }
        mContext = getApplicationContext();
        mBackend = PowerWhitelistBackend.getInstance();
        mBackend.createDozeModeBackend(mContext);
        mUpdateDozeHandler = new UpdateDozeHandler();

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        if(DEBUG) {
            Log.d(TAG, "UpdateDozeModeWhiteListService onDestroy()");
        }
        super.onDestroy();
    }

    private class UpdateDozeHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            if(msg.what != MSG_LIVE_UPDATE){
                File localSaveDir = new File(getFilesDir(), "Update");
                File localSaveFile = new File(localSaveDir, "last_update_asus_doze_list.xml");
                if(!localSaveFile.exists()){
                    Log.d(TAG, "cannot find last_update_asus_doze_list.xml");
                    File remoteSaveFile = new File("/data/data/com.asus.configupdater/files/configs/asus_doze_cn_list.xml");
                    if(remoteSaveFile.exists()){
                        Log.d(TAG, "copy from remote");
                        updateFileCopy(Uri.fromFile(remoteSaveFile));
                        mBackend.performLiveUpdate(getApplicationContext());
                    }
                }
            }

            switch (msg.what) {
                case MSG_LIVE_UPDATE: {
                    if(DEBUG) {
                        Log.d(TAG,"handleMessage MSG_LIVE_UPDATE");
                    }
                    //begin:remoteUpdate file copy
                    updateFileCopy((Uri)msg.obj);
                    //end:remoteUpdate file copy
                    mBackend.performLiveUpdate(getApplicationContext());
                    stopSelf(msg.arg1);
                }
                    break;
                case MSG_GET_NO_OPTIMISED_APPS: {
                    if(DEBUG) {
                        Log.d(TAG,"handleMessage MSG_GET_NO_OPTIMISED_APPS");
                    }
                    int num = mBackend.getNoOptimizedNum(mContext);
                    Intent responseIntent = new Intent("com.android.settings.RESPONSE_NO_OPTIMIZED_APP_NUM");
                    responseIntent.putExtra("Num", num);
                    responseIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                    mContext.sendBroadcast(responseIntent);
                    stopSelf(msg.arg1);
                }
                    break;
                case MSG_OPTIMISED_USER_WHITELIST_APPS: {
                    if(DEBUG) {
                        Log.d(TAG,"handleMessage MSG_OPTIMISED_USER_WHITELIST_APPS");
                    }
                    mBackend.optimiseAllUserWhiteList();
                    stopSelf(msg.arg1);
                }
                    break;
                case MSG_CHECK_ASUS_WHITELIST_INATLL: {
                    if(DEBUG) {
                        Log.d(TAG,"handleMessage MSG_CHECK_ASUS_WHITELIST_INATLL");
                    }
                    mBackend.checkAsusWhiteListAdd(getApplicationContext());
                    stopSelf(msg.arg1);
                }
                    break;
                case MSG_ADD_WHITELIST: {
                    if(DEBUG) {
                        Log.d(TAG,"handleMessage MSG_ADD_WHITELIST");
                    }
                    mBackend.addWiteList(getApplicationContext());
                    stopSelf(msg.arg1);
                }
                    break;
                case MSG_OPTIMZED_ADD_USER_WHITELIST_APPS: {
                    if(DEBUG) {
                        Log.d(TAG, "handleMessage MSG_OPTIMZED_ADD_USER_WHITELIST_APPS");
                    }
                    mBackend.optimizeAddUserWhiteList();
                    stopSelf(msg.arg1);
                }
            }
        }
    }

    private void updateFileCopy(Uri remoteFileUri) {
        //get remote file
        File localSaveDir = new File(getFilesDir(), "Update");
        Log.d(TAG, "localSaveFile: "+localSaveDir.getAbsolutePath());
        if(!localSaveDir.exists()) {
            localSaveDir.mkdir();
        }
        File newFile = new File(localSaveDir, "last_update_asus_doze_list.xml");
        FileOutputStream fos = null;
        FileInputStream fis = null;

        ParcelFileDescriptor remoteUpdateFd = null;
        if(remoteFileUri != null) {
            try {
                Log.d(TAG,"remoteFileUri path: "+remoteFileUri.getPath());
                remoteUpdateFd = getContentResolver().openFileDescriptor(remoteFileUri, "r");
                fos = new FileOutputStream(newFile);
            }catch(FileNotFoundException e) {
                Log.e(TAG,"FileNotFoundException: "+e);
                return;
            }
            FileDescriptor fd = remoteUpdateFd.getFileDescriptor();
            try {
                fis = new FileInputStream(fd);
                int i = fis.read();
                while (i != -1) {
                    fos.write(i);
                    i = fis.read();
                }
                Log.d(TAG,"file copy complete");
                fis.close();
                fos.close();
            }catch (IOException e) {
                Log.d(TAG,"IOException: "+e);
                return;
            }
        }
        //get remote file
    }
}
