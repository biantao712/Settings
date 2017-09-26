package com.android.settings.twinApps;

import java.lang.reflect.Method;

import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

public class TwinAppsUtil {
    private static final String TAG = "TwinApps";
    static final boolean DEBUG = false;  //DEBUG log on/off control for all TwinApps logs.

    public static boolean isTwinAppsSupport(Context context) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        boolean bHasSystemFeature = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_ASUS_TWINAPPS);
        boolean bIsTwinAppsSupport = isTwinAppsReflect(um);
        return (bHasSystemFeature && bIsTwinAppsSupport);
    }

    public static boolean isTwinAppsSupport(UserManager um) {
        boolean bHasSystemFeature = false;
        boolean bIsTwinAppsSupport = isTwinAppsReflect(um);
        try {
            bHasSystemFeature = AppGlobals.getPackageManager().hasSystemFeature(PackageManager.FEATURE_ASUS_TWINAPPS, 0);
        } catch (RemoteException e) {}
        return (bHasSystemFeature && bIsTwinAppsSupport);
    }

    private static boolean isTwinAppsReflect(UserManager um) {
        UserInfo userInfo = um.getUserInfo(UserHandle.USER_SYSTEM);
        try {
            Class<?> c = Class.forName("android.content.pm.UserInfo");
            Object ob = userInfo;
            Method m = c.getMethod("isTwinApps", (Class[]) null);
            m.invoke(ob, (Object[]) null);
            //if(DEBUG) Log.v(TAG, "===ReflectionMethods.TwinApps true ===============");
            return true;
        } catch (Exception e) {
            if(DEBUG) Log.v(TAG, "===ReflectionMethods.TwinApps false ===============");
            return false;
        }
    }
}
