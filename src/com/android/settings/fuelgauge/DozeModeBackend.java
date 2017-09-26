package com.android.settings.fuelgauge;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IDeviceIdleController;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.util.ArraySet;
import android.util.Log;
import android.util.Xml;

import com.android.internal.os.AtomicFile;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.settings.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.android.settings.IncompatibleDozeMode.AppDataSource;

/**
 * Created by steve on 2016/3/30.
 */
public class DozeModeBackend {
    private static final String TAG = "DozeModeBackend";

    //Begin:hungjie_tseng@asus.com
    ArrayList<String> mLastLiveUpdateWhitelist = new ArrayList<>();
    ArrayList<String> mUserAddWhitelist = new ArrayList<>();
    public static final boolean DEBUG = true;
    public UpdateDozeHandler mUpdateDozeHandler;
    private static final int MSG_WRITE_USER_WHITELIST = 0;
    private static final long WRITE_USER_CONFIG_DELAY = 1000;
    //End:hungjie_tseng@asus.com

    //Begin:Steve_Ke@asus.com
    boolean mNotifyEnable = true;
    boolean mQaEnable = true;
    ArrayList<String> mLastLiveUpdateBlacklist = new ArrayList<>();
    private static final String DOZEMODELIST = "DozeModeList";
    private Fragment mManageApplication;
    private int mFragmentCode;
    private Context mContext;
    //End:Steve_Ke@asus.com

    public DozeModeBackend(Context context) {
        //Begin:hungjie_tseng@asus.com
        mContext = context;
        File localSaveDir = new File(context.getFilesDir(), "Update");
        AtomicFile lastUpdateConfigFile = new AtomicFile(new File(localSaveDir, "last_update_asus_doze_list.xml"));
        readConfigFileLocked(lastUpdateConfigFile, mLastLiveUpdateWhitelist, mLastLiveUpdateBlacklist);

        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST,
                context.MODE_PRIVATE);
        Set<String> userAdd = list.getStringSet("user_add_doze_list", new HashSet<String>());
        mUserAddWhitelist.addAll(userAdd);

        mUpdateDozeHandler = new UpdateDozeHandler();
        Log.d(TAG,"mDozeHThread be created");
        //End:hungjie_tseng@asus.com
    }

    public void setManageApplication(Fragment target, int fragmentCode) {
        mManageApplication = target;
        mFragmentCode = fragmentCode;
    }

    public void releaseManageApplication() {
        mManageApplication = null;
    }

    public void addApp(String pkg) {
        //Begin:hungjie_tseng@asus.com
        if(!mUserAddWhitelist.contains(pkg)) {
            mUserAddWhitelist.add(pkg);
        }
        mUpdateDozeHandler.removeMessages(MSG_WRITE_USER_WHITELIST);
        mUpdateDozeHandler.sendEmptyMessageDelayed(MSG_WRITE_USER_WHITELIST, WRITE_USER_CONFIG_DELAY);
        //End:hungjie_tseng@asus.com

        //Begin: Steve_Ke@asus.com
        SharedPreferences list = mContext.getSharedPreferences(DOZEMODELIST,
                mContext.MODE_PRIVATE);
        Set<String> set = new HashSet<String>();
        set.addAll(mUserAddWhitelist);

        list.edit().putStringSet("user_add_doze_list", set).commit();
        //End: Steve_Ke@asus.com

    }

    public void removeApp(String pkg) {
        //Begin:hungjie_tseng@asus.com
        mUserAddWhitelist.remove(pkg);
        mUpdateDozeHandler.removeMessages(MSG_WRITE_USER_WHITELIST);
        mUpdateDozeHandler.sendEmptyMessageDelayed(MSG_WRITE_USER_WHITELIST, WRITE_USER_CONFIG_DELAY);
        //End:hungjie_tseng@asus.com
    }

    //Begin:hungjie_tseng@asus.com
    private void readConfigFileLocked(AtomicFile configFile, ArrayList<String> mainTargetList,
                                      ArrayList<String> secondeTargetList) {
        if(configFile == null) {
            if(DEBUG) {
                Log.d(TAG,"AtomicFile is null, return");
            }
            return;
        }
        mainTargetList.clear();
        if (DEBUG) Log.d(TAG, "Reading config from " + configFile.getBaseFile());
        FileInputStream stream;
        try { stream = configFile.openRead(); } catch (FileNotFoundException e) { return; }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            readConfigFileLocked(parser, mainTargetList, secondeTargetList);
        } catch (XmlPullParserException e) {
        } finally {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }
    }

    private void readConfigFileLocked(XmlPullParser parser, ArrayList<String> mainTargetList,
                                      ArrayList<String> secondeTargetList) {
        try {
            int type = parser.next();
            while (type != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                type = parser.next();
            }

            if (type != XmlPullParser.START_TAG) {
                throw new IllegalStateException("no start tag found");
            }

            int outerDepth = parser.getDepth();
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                    continue;
                }

                String tagName = parser.getName();
                if (tagName.equals("whitelist")) {
                    String name = parser.getAttributeValue(null, "packagename");
                    mainTargetList.add(name);
                    if (DEBUG) {
                        Log.d(TAG, "name: " + name + " mainTargetList: " + mainTargetList);
                    }
                } else if (tagName.equals("blacklist")) {
                    String name = parser.getAttributeValue(null, "packagename");
                    secondeTargetList.add(name);
                    if (DEBUG) {
                        Log.d(TAG, "name: " + name + " secondeTargetList: " + secondeTargetList);
                    }
                } else if (tagName.equals("notify")) {
                    String name = parser.getAttributeValue(null, "enable");
                    mNotifyEnable = ("1".equals(name))? true : false;
                    if (DEBUG) {
                        Log.d(TAG, "Enable: " + name + " NotifyEnable: " + mNotifyEnable);
                    }
                } else if (tagName.equals("qa")){
                    String name = parser.getAttributeValue(null, "enable");
                    mQaEnable = ("1".equals(name))? true : false;
                    if (DEBUG) {
                        Log.d(TAG, "Enable: " + name + " QaEnable: " + mQaEnable);
                    }
                } else {
                    Log.w(TAG, "Unknown element under <config>: "
                            + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }

        } catch (IllegalStateException e) {
            Log.w(TAG, "Failed parsing config " + e);
        } catch (NullPointerException e) {
            Log.w(TAG, "Failed parsing config " + e);
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed parsing config " + e);
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Failed parsing config " + e);
        } catch (IOException e) {
            Log.w(TAG, "Failed parsing config " + e);
        } catch (IndexOutOfBoundsException e) {
            Log.w(TAG, "Failed parsing config " + e);
        }
    }

    private File getSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    public boolean isAsusWhitelisted(String pkg, Context context) {
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST,
                context.MODE_PRIVATE);
        Set<String> white = list.getStringSet("userWhiteList", new HashSet<String>());
        return white.contains(pkg);
    }

    private class UpdateDozeHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_WRITE_USER_WHITELIST: {
                    handleWriteConfigFile();
                }
                break;
            }
        }
    }

    private void handleWriteConfigFile() {
        SharedPreferences list = mContext.getSharedPreferences(DOZEMODELIST,
                mContext.MODE_PRIVATE);
        Set<String> userAdd = new HashSet<String>();
        userAdd.addAll(mUserAddWhitelist);
        list.edit().putStringSet("user_add_doze_list", userAdd).commit();
    }

    public void performLiveUpdate(Context context, ArraySet<String> whitelistedApps, IDeviceIdleController deviceIdleService) {
        updateNewWhiteList(context);
        clearLastLiveUpdate(context, deviceIdleService, whitelistedApps);
        addThisUpdateWhiteList(mLastLiveUpdateWhitelist, context, mLastLiveUpdateBlacklist, whitelistedApps);
    }

    private void clearLastLiveUpdate(Context context, IDeviceIdleController deviceIdleService,
                                     ArraySet<String> whitelistedApps) {
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST,
                context.MODE_PRIVATE);
        Set<String> white = list.getStringSet("userWhiteList", new HashSet<String>());
        ArrayList<String> removelist = new ArrayList<String>();
        for(String name : white) {
            if(!mUserAddWhitelist.contains(name)) {
                if(!mLastLiveUpdateWhitelist.contains(name)) {
                    if(DEBUG) {
                        Log.d(TAG,"removeAppByLiveUpdate name: "+name);
                    }
                    removeAppByLiveUpdate(name, deviceIdleService, whitelistedApps);
                    removelist.add(name);
                }
            } else {
                if(!mLastLiveUpdateWhitelist.contains(name)) {
                    removelist.add(name);
                }
            }
        }
        for (String remove : removelist) {
            white.remove(remove);
        }
        list.edit().putStringSet("userWhiteList", white).commit();

        if(mManageApplication != null && removelist.size() != 0) {
            mManageApplication.onActivityResult(mFragmentCode, 0, null);
        }
    }

    private void removeAppByLiveUpdate(String pkg, IDeviceIdleController deviceIdleService,
                                       ArraySet<String> whitelistedApps) {
        try {
            deviceIdleService.removePowerSaveWhitelistApp(pkg);
            whitelistedApps.remove(pkg);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach IDeviceIdleController", e);
        }
    }

    private void updateNewWhiteList(Context context) {
        File localSaveDir = new File(context.getFilesDir(), "Update");
        AtomicFile newUpdateConfigFile = new AtomicFile(new File(localSaveDir, "last_update_asus_doze_list.xml"));
        readConfigFileLocked(newUpdateConfigFile, mLastLiveUpdateWhitelist, mLastLiveUpdateBlacklist);
    }

    //Begin:Steve_Ke@asus.com
    private void addThisUpdateWhiteList(ArrayList<String> liveUpdateWhitelist, Context context,
                                        ArrayList<String> lastLiveUpdateBlacklist, ArraySet<String> whitelistedApps) {
        PackageManager pm = context.getPackageManager();
        ArrayList<String> whitelist = new ArrayList<String>();
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST,
                context.MODE_PRIVATE);
        Set<String> userBlackList = list.getStringSet("userBlackList", new HashSet<String>());
        Set<String> userWhiteList = list.getStringSet("userWhiteList", new HashSet<String>());
        for(int i=0; i<liveUpdateWhitelist.size(); i++) {
            String name = liveUpdateWhitelist.get(i);
            if(DEBUG) {
                Log.d(TAG, "addThisUpdateWhiteList name: " + name);
            }
            if(isPkgInstalled(pm, name) && !userBlackList.contains(name) && !userWhiteList.contains(name)) {
                if(!whitelistedApps.contains(name)) {
                    whitelist.add(name);
                } else {
                    userWhiteList.add(name);
                }
            }
        }

        list.edit().putStringSet("userWhiteList", userWhiteList).commit();

        ArrayList<String> blacklist = new ArrayList<String>();
        for(int i=0; i<lastLiveUpdateBlacklist.size(); i++) {
            String name = lastLiveUpdateBlacklist.get(i);
            if(DEBUG) {
                Log.d(TAG, "lastLiveUpdateBlacklist name: " + name);
            }
            if(isPkgInstalled(pm, name)) {
                blacklist.add(name);
            }
        }
        updateSharePreference(context, whitelist, blacklist);
    }
    //End:Steve_Ke@asus.com

    private void addAppByLiveUpdate(String pkg, IDeviceIdleController deviceIdleService,
                                    ArraySet<String> whitelistedApps) {
        try {
            deviceIdleService.addPowerSaveWhitelistApp(pkg);
            whitelistedApps.add(pkg);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to reach IDeviceIdleController", e);
        }
    }

    public int getNoOptimizedNum(Context context, ArraySet<String> whitelistedApps,
                                 ArraySet<String> sysWhitelistedApps) {
        PackageManager pm = context.getPackageManager();
        int liveUpdateInstallNum = 0;
        for(String app : mLastLiveUpdateWhitelist) {
            Log.d(TAG,"getNoOptimizedNum app: "+app);
            if(isPkgInstalled(pm, app)) {
                liveUpdateInstallNum ++;
            }
        }
        int whitelistedAppsNum = 0;
        for(String app : whitelistedApps) {
            if(isPkgInstalled(pm, app)){
                whitelistedAppsNum++;
            }
        }

        int sysWhiteListNum = 0;
        for(String app : sysWhitelistedApps) {
            if(isPkgInstalled(pm, app)) {
                sysWhiteListNum++;
            }
        }

        int userAddWhiteListNum = whitelistedAppsNum - sysWhiteListNum
                - liveUpdateInstallNum;
        if(DEBUG) {
            Log.d(TAG,"getNoOptimizedNum app: "+userAddWhiteListNum);
        }
        return userAddWhiteListNum;
    }

    public void optimiseAllUserWhiteList(ArraySet<String> whitelistedApps, ArraySet<String> sysWhitelistedApps,
                                         IDeviceIdleController deviceIdleService) {
        clearUserWhitelist(whitelistedApps, sysWhitelistedApps, deviceIdleService, null);
    }

    private boolean isPkgInstalled(PackageManager pm, String pkgName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(pkgName, 0);
            Log.d(TAG,"install app: "+pkgName);
            return true;
        }catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG,"Not install app: "+pkgName);
            return false;
        }
    }

    private void clearUserWhitelist(ArraySet<String> whitelistedApps, ArraySet<String> sysWhitelistedApps,
                                    IDeviceIdleController deviceIdleService, ArraySet<String> userSelected) {
        ArraySet<String> whitelist = new ArraySet<>();
        if (userSelected != null) {
            whitelist.addAll(userSelected);
        } else {
            whitelist.addAll(whitelistedApps);
        }
        if(DEBUG) {
            Log.d(TAG, "optimiseAllUserWhiteList whitelist: " + whitelist);
        }
        for(String app : whitelist) {
            if(sysWhitelistedApps.contains(app)){
                continue;
            }
//            if(mLastLiveUpdateWhitelist.contains(app)){
//                Log.d(TAG,"optimiseAllUserWhiteList isAsusWhiteList removeApp: "+app);
//                mUserAddWhitelist.remove(app);
//                continue;
//            }
            if(DEBUG) {
                Log.d(TAG,"optimiseAllUserWhiteList removeApp: "+app);
            }
            try {
                deviceIdleService.removePowerSaveWhitelistApp(app);
                whitelistedApps.remove(app);
                mUserAddWhitelist.remove(app);
            }catch (RemoteException e) {
                Log.w(TAG, "Unable to reach IDeviceIdleController", e);
            }
        }
        mUpdateDozeHandler.removeMessages(MSG_WRITE_USER_WHITELIST);
        mUpdateDozeHandler.sendEmptyMessageDelayed(MSG_WRITE_USER_WHITELIST, WRITE_USER_CONFIG_DELAY);
    }

    public void checkAsusWhiteListAdd(Context context, ArraySet<String> whitelistedApps) {
        PackageManager pm = context.getPackageManager();
        ArrayList<String> whitelist = new ArrayList<String>();
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST,
                context.MODE_PRIVATE);
        Set<String> userBlackList = list.getStringSet("userBlackList", new HashSet<String>());
        Set<String> userWhiteList = list.getStringSet("userWhiteList", new HashSet<String>());
        for(String app : mLastLiveUpdateWhitelist) {
            if(DEBUG) {
                Log.d(TAG,"handlerAsusWhiteListAdd app: "+app);
            }
            if(isPkgInstalled(pm, app) && !userBlackList.contains(app) && !userWhiteList.contains(app)) {
                if(!whitelistedApps.contains(app)) {
                    whitelist.add(app);
                    Log.d("steve", app);
                } else {
                    userWhiteList.add(app);
                }
            }
        }
        list.edit().putStringSet("userWhiteList", userWhiteList).commit();

        ArrayList<String> blacklist = new ArrayList<String>();
        for(int i=0; i<mLastLiveUpdateBlacklist.size(); i++) {
            String name = mLastLiveUpdateBlacklist.get(i);
            if(DEBUG) {
                Log.d(TAG, "handlerAsusBlackListAdd name: " + name);
            }
            if(isPkgInstalled(pm, name)) {
                blacklist.add(name);
            }
        }
        updateSharePreference(context, whitelist, blacklist);
    }
    //End:hungjie_tseng@asus.com

    //Begin: Steve_Ke@asus.com
    public void optimizeAddUserWhiteList(ArraySet<String> whitelistedApps, ArraySet<String> sysWhitelistedApps,
                                          IDeviceIdleController deviceIdleService) {
        ArraySet<String> listApp = new ArraySet<String>();
        SharedPreferences list = mContext.getSharedPreferences(DOZEMODELIST,
                mContext.MODE_PRIVATE);
        Set<String> select = list.getStringSet("userSelectOptimizeList", new HashSet<String>());
        list.edit().remove("userSelectOptimizeList").commit();
        listApp.addAll(select);
        clearUserWhitelist(whitelistedApps, sysWhitelistedApps, deviceIdleService, listApp);
    }

    public void addWiteList(Context context, IDeviceIdleController deviceIdleService,
                            ArraySet<String> whitelistedApps) {
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST,
                context.MODE_PRIVATE);
        Set<String> white = list.getStringSet("userWhiteList", new HashSet<String>());
        Set<String> uid = new  HashSet<String>();
        for (String name : white) {
            if(!whitelistedApps.contains(name)) {
                addAppByLiveUpdate(name, deviceIdleService, whitelistedApps);
            }
        }
        list.edit().remove("whiteList").commit();

        if(mManageApplication != null) {
            mManageApplication.onActivityResult(mFragmentCode, 0, null);
        }
    }

    private void updateSharePreference(Context context, ArrayList<String> whiteList,
                                       ArrayList<String> blackList) {
        ContentResolver cr = context.getContentResolver();
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST,
                context.MODE_PRIVATE);
        if(whiteList.size() > 0) {
            Log.d(TAG, "SharePreferences update new whitelist");
//            list.edit().remove("whiteList").commit();
//            Set<String> set = new HashSet<String>();
//            set.addAll(whiteList);
//            list.edit().putStringSet("whiteList", set).commit();
//            notifyWhiteListToUser(context, set);

            //+++ tim
            if(cr != null){
                String[] selectionArgs = whiteList.toArray(new String[whiteList.size()]);
                cr.update(DozeModeListProvider.CONTENT_URI, null, "userWhiteList", selectionArgs);
            }
            //---
        } else {
            list.edit().remove("whiteList").commit();
        }

        Boolean notify = list.getBoolean("notify", mNotifyEnable);

        if (mNotifyEnable == false) {
            notify = false;
        }

        if(blackList.size() > 0) {
            Log.d(TAG, "SharePreferences update new blacklist");
//            list.edit().remove("blackList").commit();
//            Set<String> set = new HashSet<String>();
//            set.addAll(blackList);
//            list.edit().putStringSet("blackList", set).commit();
//            if (notify == true) {
//                notifyBlackListToUser(context, set);
//            }

            //+++ tim
            if(cr != null){
                String[] selectionArgs = blackList.toArray(new String[blackList.size()]);
                cr.update(DozeModeListProvider.CONTENT_URI, null, "userBlackList", selectionArgs);
            }
            //---
        } else {
            list.edit().remove("blackList").commit();
        }

        list.edit().putBoolean("qa", mQaEnable).commit();
    }

    private void notifyWhiteListToUser(Context context, Set<String> whitelist) {
        List<PackageInfo> apps = new ArrayList<PackageInfo>();
        ArrayList<String> white = new ArrayList<String>();
        white.addAll(whitelist);
        PackageManager pm = context.getPackageManager();
        AppDataSource aps = new AppDataSource(pm);
        aps.queryAppInfo(apps, white);
        aps.destroy();
        String text = "";
        for(PackageInfo app: apps) {
            text = text + app.applicationInfo.loadLabel(pm) + ",";
        }
        if(text.length()>0) {
            text = text.substring(0, text.length() - 1);
        }
        //show notification:
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.asus_ep_notify_power_saver)
                .setContentTitle(context.getResources().getString(R.string.get_real_time_notification))
                .setContentText(text);
        Intent i = new Intent();
        i.setAction("com.asus.mobilemanager.DozeMode.StartCheckDozeMode");
        //add for doze mode cn white list start
        //use cnmobilemanager
        if(context.getPackageManager().resolveActivity(i, 0) == null) {
            i.setAction("com.asus.cnmobilemanager.DozeMode.StartCheckDozeMode");
        }
        //add for doze mode cn white list end
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                context,
                0,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(resultPendingIntent);
        notificationManager.notify(1, builder.build());
    }

    private void notifyBlackListToUser(Context context, Set<String> blacklist) {
        List<PackageInfo> apps = new ArrayList<PackageInfo>();
        ArrayList<String> black = new ArrayList<String>();
        black.addAll(blacklist);
        PackageManager pm = context.getPackageManager();
        AppDataSource aps = new AppDataSource(pm);
        aps.queryAppInfo(apps, black);
        aps.destroy();
        String text = "";
        for(PackageInfo app: apps) {
            text = text + app.applicationInfo.loadLabel(pm) + ",";
        }
        if(text.length()>0) {
            text = text.substring(0, text.length() - 1);
        }
        //show notification:
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.asus_autostart_notification)
                .setContentTitle(context.getResources().getString(R.string.incompatible_notify_title))
                .setContentText(text);
        Intent i = new Intent();
        i.setAction("com.android.settings.IncompatibleDozeMode.IncompatilbeAppsActivity");
        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                context,
                0,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        builder.setContentIntent(resultPendingIntent);
        notificationManager.notify(2, builder.build());
    }
    //End: Steve_Ke@asus.com
}
