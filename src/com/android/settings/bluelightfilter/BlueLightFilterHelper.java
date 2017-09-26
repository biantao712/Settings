package com.android.settings.bluelightfilter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.support.v7.preference.Preference;
import com.android.settings.R;


/**
 * Created by mars on 2016/10/19.
 */

public class BlueLightFilterHelper {
    public static final String SPLENDID_FEATURE = "asus.hardware.display.splendid";
    public static final String ASUS_SPLENDID_PACKAGE_NAME = "com.asus.splendid";

    private BluelightSwitchPreference mBluelightFilterScreen;
    private Activity mContext;
    private int mBluelightFilterMode;
    private int mBluelightLevel;
    private BluelightFilterModeObserver mBluelightFilterModeObserver;
    private BluelightLevelObserver mBluelightLevelObserver;

    public BlueLightFilterHelper(BluelightSwitchPreference bluelightFilterPreference, Activity context){
        mBluelightFilterScreen = bluelightFilterPreference;
        mContext = context;
        mBluelightFilterModeObserver = new BluelightFilterModeObserver(new Handler());
        mBluelightLevelObserver = new BluelightLevelObserver(new Handler());

        if(bluelightFilterPreference != null){
            bluelightFilterPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(preference == mBluelightFilterScreen){
                        showSeekBar();
                    }
                    return false;
                }
            });
        }
    }

    public void registerObserver(){
            if (mBluelightFilterScreen != null && mBluelightFilterModeObserver != null) {
                mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH), false,
                        mBluelightFilterModeObserver);
            }
            if(mBluelightFilterScreen != null && mBluelightLevelObserver != null){
                mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(Constants.BLUELIGHT_FILTER_LEVEL), false,
                mBluelightLevelObserver);
            }
    }

    public void onPause(){
        unRegisterObserver();
        saveAndUpdate();
    }

    public void showSeekBar(){
        SeekBarDialog sbd = new SeekBarDialog(mContext.getApplicationContext(), mContext);
        sbd.show();
    }

    private void saveAndUpdate(){
            if (mBluelightFilterScreen != null) { //++Carol_Wang
                mBluelightFilterMode = Settings.System.getInt(mContext.getContentResolver(),
                    Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
                mBluelightLevel = (mBluelightFilterMode==1)?Settings.System.getInt(mContext.getContentResolver(),
                    Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_MODE_OFF):-1; //default off
                updateBluelightFilterModeTitle();
            }
    }

    private void unRegisterObserver(){
           if (mBluelightFilterScreen != null && mBluelightFilterModeObserver != null) {
               mContext.getContentResolver().unregisterContentObserver(mBluelightFilterModeObserver);
            }
            if (mBluelightFilterScreen != null && mBluelightLevelObserver != null) {
                mContext.getContentResolver().unregisterContentObserver(mBluelightLevelObserver);
            }
    }

    private class BluelightFilterModeObserver extends ContentObserver {
               public BluelightFilterModeObserver(Handler handler) {
                        super(handler);
                    }

                @Override
                public void onChange(boolean selfChange) {
                        Log.d("BluelightFilterMode", "BluelightFilterModeObserver onChange");
                        int currentScreenMode = Settings.System.getInt(mContext.getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
                        int currentBluelightLevel = Settings.System.getInt(mContext.getContentResolver(), Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_MODE_OFF);
                        if (mBluelightFilterMode != currentScreenMode ) {
                                mBluelightFilterMode = currentScreenMode;
                                mBluelightLevel = (mBluelightFilterMode==1)?currentBluelightLevel:-1; //default off
                                updateBluelightFilterModeTitle();
                        }
                }
    }

    //Carol_Wang++
    private class BluelightLevelObserver extends ContentObserver{
        public BluelightLevelObserver(Handler handler) {
                super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d("BluelightLevel", "BluelightLevel onChange");
                int currentScreenMode = Settings.System.getInt(mContext.getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
                int currentBluelightLevel = Settings.System.getInt(mContext.getContentResolver(), Constants.BLUELIGHT_FILTER_LEVEL, Constants.BLUELIGHT_FILTER_MODE_OFF);
                if (currentScreenMode == 1 && mBluelightLevel != currentBluelightLevel) {
                    mBluelightFilterMode = currentScreenMode;
                    mBluelightLevel = currentBluelightLevel;
                    updateBluelightFilterModeTitle();
                }
        }
    }

    private void updateBluelightFilterModeTitle() { //++Carol_Wang
            switch (mBluelightLevel) {
            case Constants.BLUELIGHT_FILTER_MODE_OFF:
                mBluelightFilterScreen.setSummary(mContext.getString(R.string.splendid_bluelight_filter_mode_off));
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RDWEAK:
                mBluelightFilterScreen.setSummary(mContext.getString(R.string.splendid_bluelightfilter_level_one));
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RD01:
                mBluelightFilterScreen.setSummary(mContext.getString(R.string.splendid_bluelightfilter_level_two));
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RD02:
                mBluelightFilterScreen.setSummary(mContext.getString(R.string.splendid_bluelightfilter_level_three));
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RD03:
                mBluelightFilterScreen.setSummary(mContext.getString(R.string.splendid_bluelightfilter_level_four));
                break;
            case Constants.BLUELIGHT_FILTER_LEVEL_RDSTRONG:
                mBluelightFilterScreen.setSummary(mContext.getString(R.string.splendid_bluelightfilter_level_five));
                break;
            default:
                mBluelightFilterScreen.setSummary(mContext.getString(R.string.splendid_bluelight_filter_mode_off));
                break;
            }
    }

    public static boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getApplicationInfo(packageName, 0);
            return true;
        }
        catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean hasSplendidFeature(Context context){
        return context.getPackageManager().hasSystemFeature(SPLENDID_FEATURE);
    }


    public static boolean isODMDevice(Context context){
        if(!isAppInstalled(context, ASUS_SPLENDID_PACKAGE_NAME)&&hasSplendidFeature(context)){
            return true;
        }

        return false;
    }

}
