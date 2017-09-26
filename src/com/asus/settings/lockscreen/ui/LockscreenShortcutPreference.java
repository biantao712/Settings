package com.asus.settings.lockscreen.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;

import com.asus.settings.lockscreen.AsusLSUtils;

public class LockscreenShortcutPreference extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            startActivity(AsusLSUtils.getSettingShortcutIntent());
        } catch (Exception e) {
            Log.w("LSShortcut", "startActivity E: " + e);
        }
        getActivity().finish();
    }
}
