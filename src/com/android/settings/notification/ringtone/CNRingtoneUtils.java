package com.android.settings.notification.ringtone;

import android.util.Log;
import android.os.SystemProperties;
/**
 * Created by leaon_wang on 2017/5/19.
 */

public class CNRingtoneUtils {
    private static final String TAG = "CNRingtoneUtils" ;

    public static boolean isSpecificSKU(String sku) {
        String systemSku = SystemProperties.get("ro.build.asus.sku", "");
        Log.d(TAG, "SKU is " + systemSku);
        return systemSku.toLowerCase().startsWith(sku.toLowerCase());
    }
}
