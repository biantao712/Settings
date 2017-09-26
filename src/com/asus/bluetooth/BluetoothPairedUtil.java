package com.asus.bluetooth;

import android.os.Build;

public class BluetoothPairedUtil {

    private static final String TAG = "BluetoothPairedUtil";

    public static final String DEVICE_NAME_DA01_SPK = "DA01-SPK";
    public static final String DEVICE_NAME_DA01_KB = "DA01-KB";
    public static final String DEVICE_NAME_DK01_KB = "DK01";

    private static final String[] BT_DOCK_SUPPORT_DEVICE_NAMES = new String[] {
            "P01T", // Z300CL, Z300CNL
            "P021", // Z300CG, Z300CNG
            "P023", // Z300C
            "P00C", // Z300M
            "P027", // Z500M
            "ASUS_P00L_1", // Z301ML
            "ASUS_P00L_2", // Z301ML
    };

    public static boolean supportToPairWithBtDock () {
        //note: Build.DEVICE correspond to ro.product.device, not ro.product.model
        String deviceName = Build.DEVICE;
        for (String supportDeviceName : BT_DOCK_SUPPORT_DEVICE_NAMES) {
            if (deviceName.startsWith(supportDeviceName)) {
                return true;
            }
        }
        return false;
    }
}
