package com.android.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.logging.MetricsProto;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

public class ManagePermissionSettings extends SettingsPreferenceFragment implements Indexable {
    @Override protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.MAIN_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.setClassName("com.asus.cn.permissionmanager", "com.asus.cn.permissionmanager.MainActivity");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }

//        Intent intent = new Intent();
//        intent.setComponent(new ComponentName("com.asus.themeapp", "com.asus.themeapp.ThemeAppActivity"));
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//        intent.putExtra("switchToMyTheme", true);
//        intent.putExtra("from", "AsusSettings");
//        try {
//            startActivity(intent);
//        } catch (ActivityNotFoundException e) {
//            e.printStackTrace();
//        }
        Log.d("Jack","Manage account test.");

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
                    data.title = res.getString(R.string.permissions_manager_label);
                    data.screenTitle = res.getString(R.string.permissions_manager_label);
                    result.add(data);
                    return result;
                }

            };

    public static boolean hasPermissionManager(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo("com.asus.cn.permissionmanager", PackageManager.GET_META_DATA);
            if (!info.applicationInfo.enabled) {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }
}
