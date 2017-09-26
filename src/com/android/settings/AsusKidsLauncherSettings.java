
package com.android.settings;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class AsusKidsLauncherSettings extends SettingsPreferenceFragment implements OnPreferenceClickListener, Indexable{
    // ------------------------------------------------------------------------
    // TYPES
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC FIELDS
    // ------------------------------------------------------------------------
    private static final String KEY_KIDS_LAUNCHER_SWITCHS = "asus_kids_launcher_switch";

    // ------------------------------------------------------------------------
    // STATIC INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // STATIC METHODS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // FIELDS
    // ------------------------------------------------------------------------
    private SwitchPreference mSwitch;

    // ------------------------------------------------------------------------
    // INITIALIZERS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // CONSTRUCTORS
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // METHODS
    // ------------------------------------------------------------------------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.asus_kids_launcher_settings);
        PreferenceScreen root = getPreferenceScreen();
        mSwitch = (SwitchPreference) root.findPreference(KEY_KIDS_LAUNCHER_SWITCHS);
        mSwitch.setOnPreferenceClickListener(this);

        TrackerManager.sendEvents(getActivity(), TrackerName.TRACKER_MAIN_ENTRIES, Category.KIDS_MODE_ENTRY,
                Action.ENTER_SETTINGS, TrackerManager.DEFAULT_LABEL, TrackerManager.DEFAULT_VALUE);
    }

    public void onResume() {
        super.onResume();
        if (hasKidsMode(getActivity())) {
            mSwitch.setChecked(false);
        } else {
            finish();
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (hasKidsMode(getActivity())) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setClassName("com.asus.kidslauncher", "com.asus.kidslauncher.AsusKidsLauncherEntry");
            startActivity(intent);
            return true;
        }
        return false;
    }

    public static boolean hasKidsMode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("com.asus.kidslauncher", PackageManager.GET_META_DATA);
            if (!info.applicationInfo.enabled) {
                return false;
            }
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }

    @Override
    protected int getMetricsCategory(){
        return MetricsEvent.MAIN_SETTINGS;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
            if (hasKidsMode(context)) {
                final Resources res = context.getResources();

                //Add fragment title
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.asus_kids_launcher_settings);
                data.screenTitle = res.getString(R.string.asus_kids_launcher_settings);
                result.add(data);
            }

            return result;
        }
    };
}
