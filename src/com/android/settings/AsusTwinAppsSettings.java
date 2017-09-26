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

public class AsusTwinAppsSettings extends SettingsPreferenceFragment implements Indexable {

    protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MAIN_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.asus.twinapps", "com.asus.twinapps.TwinAppsActivity"));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
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
                    data.title = res.getString(R.string.twinapps_title);
                    data.screenTitle = res.getString(R.string.twinapps_title);
                    result.add(data);
                    return result;
                }

            };
}
