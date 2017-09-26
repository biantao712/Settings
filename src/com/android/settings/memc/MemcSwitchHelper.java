package com.android.settings.memc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.support.v7.preference.Preference;
import com.android.settings.R;


/**
 * Created by smilefish on 2016/10/25.
 */

public class MemcSwitchHelper {
    public static final String MEMC_FEATURE = "asus.hardware.display.pq_chip.memc";
    public static final String VERIZON_FEATURE = "ro.asus.is_verizon_device";
    public static final String ABSENT_DEVICE_FEATURE = "persist.sys.display.pq.absent";
    public static final String ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL = "asus_visualmaster_pq_chip_memc_level";
    public static final String ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL_TEMP = "asus_visualmaster_pq_chip_memc_level_temp";

    public static final int PICTURE_QUALITY_MEMC_DISABLE = 0;
    public static final int PICTURE_QUALITY_MEMC_LOW = 1;
    public static final int PICTURE_QUALITY_MEMC_MIDDLE = 2;
    public static final int PICTURE_QUALITY_MEMC_HIGH = 3;
    public static final int PICTURE_QUALITY_MEMC_DEFAULT = PICTURE_QUALITY_MEMC_MIDDLE;
    public static final int PICTURE_QUALITY_CINEMA_VALUE = 100;

    private Preference mMemcPreference;
    private Activity mContext;
    private MemcLevelObserver mMemcLevelObserver;

    public MemcSwitchHelper(Preference memcPreference, Activity context){
        mMemcPreference = memcPreference;
        mContext = context;
        mMemcLevelObserver = new MemcLevelObserver(new Handler());
    }

    public void registerObserver(){
            if(mMemcPreference != null && mMemcLevelObserver != null){
                mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL), false,
                        mMemcLevelObserver);
            }
    }

    public void onResume(){
        updateMemcPreference();
        registerObserver();
    }

    public void onPause(){
        unRegisterObserver();
    }

    private void updateMemcPreference() {
        if (mMemcPreference != null) {
            final Resources res = mContext.getResources();
            String[] memc_desc_entries = res.getStringArray(R.array.pq_chip_memc_desc_entries);
            String memc_description = res.getString(R.string.picture_quality_memc_description);
            String[] values = res.getStringArray(R.array.pq_chip_memc_values);
            int memcLevel = Settings.System.getInt(mContext.getContentResolver(),
                    ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL, PICTURE_QUALITY_MEMC_DEFAULT);
            int realMemcLevel = memcLevel % PICTURE_QUALITY_CINEMA_VALUE;
            //Log.d(TAG, "mMemcLevel:" +mMemcLevel);
            for (int i=0; i< values.length; i++) {
                if(realMemcLevel==Integer.parseInt(values[i])){
                    mMemcPreference.setSummary(memc_description + "\n" + memc_desc_entries[i]);
                    break;
                }
            }
        }
    }

    private void unRegisterObserver(){
            if (mMemcPreference != null && mMemcLevelObserver != null) {
                mContext.getContentResolver().unregisterContentObserver(mMemcLevelObserver);
            }
    }

    private class MemcLevelObserver extends ContentObserver{
        public MemcLevelObserver(Handler handler) {
                super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateMemcPreference();
        }
    }

    public static boolean hasMemcFeature(Context context){
        return context.getPackageManager().hasSystemFeature(MEMC_FEATURE);
    }

    public static boolean isVerizonDevice(){
        if(SystemProperties.get(VERIZON_FEATURE).equals("1")){
            return true;
        }
        return false;
    }

    public static boolean isAbsentDevice(){ //Z580C and Z580CA share branch, but Z580C is absent for visual master
        if(SystemProperties.get(ABSENT_DEVICE_FEATURE).equals("1")){
            return true;
        }

        return false;
    }

}
