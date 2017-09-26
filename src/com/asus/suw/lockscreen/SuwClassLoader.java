package com.asus.suw.lockscreen;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Constructor;

public class SuwClassLoader{
    private String TARGET_PACKAGE_NAME = AsusSuwUtilisClient.TARGET_PACKAGE_NAME;
    private String TARGET_CLASS_NAME = AsusSuwUtilisClient.TARGET_CLASS_NAME;
    private static final String TAG ="SuwClassLoader";

    private Context mContext;
    private ClassLoader mClassloader;
    private Class mTargetClass;
    private Constructor mConstructor;
    private static SuwClassLoader sSuwClassLoader;

    private SuwClassLoader(Context context){
        try {
            mContext = context.getApplicationContext().createPackageContext(TARGET_PACKAGE_NAME
                    , Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_RESTRICTED);
            mClassloader = mContext.getClassLoader();
            mTargetClass = mClassloader.loadClass(TARGET_CLASS_NAME);
            mConstructor = mTargetClass.getConstructor(new Class[] {Context.class});
        } catch(Exception e) {
            e.printStackTrace();
            Log.d(TAG, "getTargetContext fail");
        }
    }

    public static SuwClassLoader getInstance(Context context){
        if(sSuwClassLoader == null){
            synchronized (SuwClassLoader.class) {
                if (sSuwClassLoader == null) {
                    sSuwClassLoader = new SuwClassLoader(context);
                }
            }
        }
        return sSuwClassLoader;
    }

    public ClassLoader getClassLoader(){
        Log.d(TAG, "getClassLoader fail:" + mClassloader);
        return mClassloader;
    }

    public Context getTargetContext() {
        return mContext;
    }

    public Class getTargetClass() {
        return mTargetClass;
    }

    public Constructor getTargetConstructor() {
        return mConstructor;
    }
}