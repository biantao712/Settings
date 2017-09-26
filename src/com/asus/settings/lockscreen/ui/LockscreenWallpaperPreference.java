package com.asus.settings.lockscreen.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.asus.settings.lockscreen.AsusLSUtils;

public class LockscreenWallpaperPreference extends Fragment {

    private static final String TAG = "LSWallpaper";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        try {
            //Log.d(TAG,"startActivity(AsusLSUtils.getSettingLSWallpaperIntent());");
            startActivity(AsusLSUtils.getSettingLSWallpaperIntent());
        } catch (Exception e) {
            Log.w(TAG, "startActivity E: " + e);
            // For VZW, AsusLauncher uses another activty for setting wallpaper,
            // and they are going to change to use new activty in the future.
            // Therefore, we use try/catch to retry new activity.
            startLSWallpaperSetting2();
            //Log.d(TAG,"startLSWallpaperSetting2();");
        }
        //Log.d(TAG,"getActivity finish");
        getActivity().finish();
    }

    private void startLSWallpaperSetting2() {
        //Log.d(TAG,"startLSWallpaperSetting2");
        try {
            startActivity(AsusLSUtils.getSettingLSWallpaperIntent2(false));
        } catch (Exception e) {
            Log.w(TAG, "startActivity2 E: " + e);
            startLSWallpaperSetting3();
        }
    }

    private void startLSWallpaperSetting3() {
        //Log.d(TAG,"startLSWallpaperSetting3");
        try {
            startActivity(AsusLSUtils.getSettingLSWallpaperIntent2(true));
        } catch (Exception e) {
            Log.w(TAG, "startActivity3 E: " + e);
        }
    }
}
