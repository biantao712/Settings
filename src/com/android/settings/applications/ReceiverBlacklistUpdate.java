package com.android.settings.applications;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v4.app.NotificationCompat;
import android.util.Slog;

import com.android.settings.R;
import com.android.settings.Settings.StorageUseActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by RobertJW_Chen on 2015/12/28.
 */
public class ReceiverBlacklistUpdate  extends BroadcastReceiver {

    private static final String TAG = "AsusSettings.appReceiver";
    List<String> mPackageList = new ArrayList<String>();

    @Override
    public void onReceive(Context context, Intent intent) {
        //update the list
        if (intent.getAction().equals("asus.intent.action.BLACKLIST_UPDATE")){
            context.getPackageManager().refreshApp2sdBlacklist();
            return;
        }
        mPackageList = warningApps(context);
        if(!mPackageList.isEmpty()) {
            Intent listIntent = new Intent();
            listIntent.setClass(context, StorageUseActivity.class);
            //TODO: need to build up new filter for this blacklist case in ManageApplications
            //listIntent.putExtra(ManageApplications.EXTRA_BLKLIST_VIOLATION, true);
            listIntent.setFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            VolumeInfo vol = getPrivateVolume(context);
            String volName = vol.getDescription();
            listIntent.putExtra(ManageApplications.EXTRA_VOLUME_UUID, vol.getFsUuid());
            listIntent.putExtra(ManageApplications.EXTRA_VOLUME_NAME, volName);
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            PendingIntent pi = PendingIntent.getActivity(context, 0, listIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification  notification = new NotificationCompat.Builder(context)
                            .setContentTitle(context.getString(R.string.asus_app2sd_notif_title))
                            .setStyle(new NotificationCompat.BigTextStyle().
                                    bigText(context.getResources().getString(R.string.asus_app2sd_blacklist_warning)))
                            .setSmallIcon(android.R.drawable.stat_sys_warning)
                            .setContentIntent(pi)
                            .setAutoCancel(true)
                            .build();
            notificationManager.notify(0, notification);
        } else {
            Slog.i(TAG, "Apps in SD card are valid");
        }
    }

    private List<String> warningApps(Context context){
        List<String> list = new ArrayList<String>();
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> pkgs = pm.getInstalledApplications(0);

        for(ApplicationInfo info:pkgs) {
            if(((info.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0)
                    && pm.isInApp2sdBlacklist(info.packageName)) {
                String name = info.packageName;
                Slog.i(TAG, name+" is in backlist and installed in External Storage");
                list.add(name);
            }
        }

        return list;
    }

    private VolumeInfo getPrivateVolume(Context context) {
        StorageManager sm = context.getSystemService(StorageManager.class);
        for (VolumeInfo info : sm.getVolumes()) {
            if(info.getType() == VolumeInfo.TYPE_PRIVATE) {
                return info;
            }
        }
        return null;
    }
}
