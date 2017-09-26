package com.asus.settings.lockscreen.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.asus.settings.lockscreen.AsusLSUtils;

public class LockscreenSlideShowWallpaperPreference extends Fragment {

    private static final String TAG = "LSSlideShowWp";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            startActivity(AsusLSUtils.getSettingLSSlideShowWallpaperIntent());
        } catch (Exception e) {
            Log.w(TAG, "startActivity E: " + e);
        }
        getActivity().finish();
    }

}
