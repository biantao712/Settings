package com.android.settings;

import com.android.internal.logging.MetricsProto;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class AsusThemeAppSettings  extends SettingsPreferenceFragment implements Indexable {

    @Override protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MAIN_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
//        Intent intent = new Intent();
//        intent.setComponent(new ComponentName("com.asus.themeapp", "com.asus.themeapp.ThemeAppActivity"));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.putExtra("switchToMyTheme", true);
//        intent.putExtra("from", "AsusSettings");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.asus.launcher3", "com.asus.zenlife.activity.table.StartActivity"));
        intent.putExtra("type", 0);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        finish();
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                    final Resources res = context.getResources();

                    // Add fragment title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.asus_theme_app_title);
                    data.screenTitle = res.getString(R.string.asus_theme_app_title);
                    result.add(data);
                    return result;
                }

            };
}
