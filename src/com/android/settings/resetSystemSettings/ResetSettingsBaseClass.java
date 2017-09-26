package com.android.settings.resetSystemSettings;

import android.annotation.FractionRes;
import android.annotation.StringRes;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;

/**
 * Created by JimCC
 */
public class ResetSettingsBaseClass {

    protected Context mContext;
    protected ContentResolver mResolver;
    private Resources mResources;

    public ResetSettingsBaseClass(Context context) {
        mContext = context;
        mResolver = mContext.getContentResolver();
        mResources = mContext.getResources();
    }

    protected final int getInteger(@StringRes int resId) {
        return mResources.getInteger(resId);
    }

    protected final boolean getBoolean(@StringRes int resId) {
        return mResources.getBoolean(resId);
    }

    protected final float getFraction(@FractionRes int id, int base, int pbase) {
        return mResources.getFraction(id, base, pbase);
    }
}
