package com.android.settings.firmware;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.Intent;

import android.util.Log;

public class FirmwareManager {
    public static final String TAG = "FirmwareManager";

    // Action code for service flow
    public static final int ACTION_CONFIRM = 2;
    public static final int ACTION_CHECK  = 1;
    public static final int ACTION_CANCEL = 0;

    // Supported firmware types
    public static final int TYPE_PAD           = 0;
    public static final int TYPE_DOCK          = 1;
    public static final int TYPE_CAMERA        = 2;
    public static final int TYPE_TOUCH         = 3;
    public static final int TYPE_SCALAR        = 4;
    public static final int TYPE_PFS_TOUCH     = 5;
    public static final int TYPE_PFS_CAMERA    = 6;
    public static final int TYPE_PFS_PAD_EC    = 7;
    public static final int TYPE_PAD_GIC       = 8;
    public static final int TYPE_DOCK_GIC      = 9;
    public static final int TYPE_STYLUS        = 10;
    public static final int TYPE_BATTERY       = 11;
    public static final int TYPE_IAFW          = 12;
    public static final int TYPE_KBCFW          = 13;

    public static final int FIRMWARE_STATUS_FAIL        = 0;
    public static final int FIRMWARE_STATUS_READY       = 1;
    public static final int FIRMWARE_STATUS_UPDATING    = 2;

    // Intent actions for firmware managing actions
    public static final String ACTION_VERSION_CHANGE = "com.asus.services.firmware.action.VERSION_CHANGE";
    public static final String ACTION_FIRMWARE_UPDATING = "com.asus.services.firmware.action.FIRMWARE_UPDATING";

    // Extra name of ACTION_UPDATE_FIRMWARE and ACTION_FIRMWARE_UPDATING intent
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_VERSION = "version";
    public static final String EXTRA_EXECUTABLE = "executable";
    public static final String EXTRA_ROM = "rom";

    // Firmware version paths
    private static final String [] VERSION_PATHS = {
                                     "/sys/class/switch/pad/name",
                                     "/sys/class/switch/dock/name",
                                     "/sys/class/switch/camera/name",
                                     "/sys/class/switch/touch/name",
                                     "/sys/class/switch/scalar/name",
                                     "/sys/class/switch/pfs_touch/name",
                                     "/sys/class/switch/pfs_camera/name",
                                     "/sys/class/switch/pfs_pad_ec/name",
                                     "/sys/class/switch/pad_gic/name",
                                     "/sys/class/switch/dock_gic/name",
                                     "/sys/class/switch/digitizer/name",
                                     "/sys/class/switch/battery/name",
                                     "/sys/class/switch/ia_fw/name",
                                     "/sys/class/switch/keyboard/name"};

    public static boolean canShowFirmwareVersion(final int type) {
        File deviceVersionFile = new File(VERSION_PATHS[type]);
        boolean canShow = deviceVersionFile.isFile() && deviceVersionFile.exists();
        Log.v(TAG, "Can show version for firmware," + type + " ? " + canShow);
        return canShow;
    }

    public static String getFirmwareVersion(final int type) {
        if (!canShowFirmwareVersion(type)) return null;
        FileInputStream input = null;
        try {
            File deviceVersionFile = new File(VERSION_PATHS[type]);
            input = new FileInputStream(deviceVersionFile);
            byte[] buffer = new byte[(int) deviceVersionFile.length()];
            input.read(buffer);
            String str = new String(buffer);
            return str.trim();
        } catch (IOException e) {
            Log.e(TAG, "IO Exception when getting version for F/W type:" + type);
            return null;
        } finally {
            if (input != null) try { input.close(); } catch (IOException e) {}
        }
    }

    public static Intent obtainUtilityIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.asus.services", "com.asus.services.firmware.FirmwareUpdateActivity");
        return intent;
    }
}
