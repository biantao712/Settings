package com.android.settings.deviceinfo;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Created by robert on 2016/3/2.
 */
public class AsusStorageUtils {

    private static final String TAG = "AsusStorageUtils";

    private static final long KB_IN_BYTES = 1024;
    private static final long MB_IN_BYTES = KB_IN_BYTES * 1024;
    private static final long MIGRATE_THRESHOLD_IN_BYTES = 300*MB_IN_BYTES;

    public static VolumeInfo getEmmcStorage(StorageManager storageManager){
        for(VolumeInfo vol:storageManager.getVolumes()){
            String id = vol.getId();
            if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(id) || VolumeInfo.ID_EMULATED_INTERNAL.equals(id)) {
                return vol;
            }
        }
        Log.e(TAG, "No Private_Internal, nor EMULATED_INTERNAL");
        return null;
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public static boolean isMigrateTargetAvailable(StorageManager sm, VolumeInfo target){
        File curPrimary = sm.getPrimaryVolume().getPathFile();
        File targetFile = target.getPath();
        long targetFreeBytes = targetFile.getFreeSpace();
        long primaryUsedBytes = curPrimary.getTotalSpace() - curPrimary.getFreeSpace();
        Log.i(TAG, curPrimary.getPath()+" ("+primaryUsedBytes+"MB Used) -> "+targetFile.getPath()+" ("+targetFreeBytes+"MB Free)");
        if((targetFreeBytes - primaryUsedBytes) < MIGRATE_THRESHOLD_IN_BYTES) return false;
        return true;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static VolumeInfo findInternalVolumeNotPrimary(List<VolumeInfo> vols, String excludeFsUuid) {
        VolumeInfo ret = null;
        for (VolumeInfo vol : vols) {
            if ((vol.getType() == VolumeInfo.TYPE_PRIVATE) &&
                    !Objects.equals(vol.getFsUuid(), excludeFsUuid)) {
                ret = vol;
                break;
            }
        }
        return ret;
    }
}
