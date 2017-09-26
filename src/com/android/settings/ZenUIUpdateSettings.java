package com.android.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.android.internal.logging.MetricsProto;

public class ZenUIUpdateSettings extends SettingsPreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        String url = "market://search?q=pub:\"ZenUI,+ASUS+Computer+Inc.\"";
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }

    /**
     * Return value can not be 0 (MetricsEvent.VIEW_UNKNOWN)
     * TODO: add new entry in frameworks
     */
    @Override
    protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MAIN_SETTINGS;
    }
}
