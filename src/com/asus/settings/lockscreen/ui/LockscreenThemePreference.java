package com.asus.settings.lockscreen.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.asus.settings.lockscreen.AsusLSUtils;

public class LockscreenThemePreference extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            startActivity(AsusLSUtils.getSettingLSThemeIntent());
        } catch (Exception e) {
            Log.w("LSTheme", "startActivity E: " + e);
        }
        getActivity().finish();
    }
}
