package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;

import com.android.internal.logging.MetricsProto;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

// This is the version for creating a new file
public class SystemUpdateSettings extends SettingsPreferenceFragment implements Indexable {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // startActivity(new Intent("android.settings.SYSTEM_UPDATE_SETTINGS"));
        startActivity(new Intent("android.settings.ASUS_SYSTEM_UPDATE_SETTINGS"));
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

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                    final Resources res = context.getResources();

                    // Add fragment title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.system_update_settings_list_item_title);
                    data.screenTitle = res.getString(R.string.system_update_settings_list_item_title);
                    result.add(data);
                    return result;
                }

            };
}
