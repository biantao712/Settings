package src.com.android.settings.IncompatibleDozeMode;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ListView;
import com.android.settings.IncompatibleDozeMode.AppDataSource;
import com.android.settings.IncompatibleDozeMode.IncompatibleAppsActivity.AppListAdapter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by steve on 2016/3/9.
 */
public class IncompatibleAppsController {

    private List<PackageInfo> mApps = new ArrayList<PackageInfo>();
    private AppListAdapter mAdapter;
    private ListView mListView;
    private static final String DOZEMODELIST = "DozeModeList";

    public void setListView(ListView lv) {
        mListView = lv;
    }

    public void setAdapter(Activity activity) {
        mAdapter = new AppListAdapter(activity);
    }

    public void changeAdapterData() {
        mAdapter.notifyDataSetChanged();
        mAdapter.setData(mApps);
        mListView.setAdapter(mAdapter);
    }

    public void loadAppsInfo(ArrayList<String> blacklist, PackageManager pm) {
        AppDataSource aps = new AppDataSource(pm);
        aps.queryAppInfo(mApps, blacklist);
        aps.destroy();
    }

    public void getBlackList(Context context, ArrayList<String> blacklist) {
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST, context.MODE_PRIVATE);
        Set<String> black = list.getStringSet("blackList", new HashSet<String>());
        blacklist.clear();
        for(String b : black) {
            blacklist.add(b);
        }
    }

    public void setNotifyOff(Context context, boolean checked) {
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST, context.MODE_PRIVATE);
        list.edit().putBoolean("notify", !checked).commit();
    }

    public boolean getNotifyState(Context context) {
        SharedPreferences list = context.getSharedPreferences(DOZEMODELIST, context.MODE_PRIVATE);
        return !list.getBoolean("notify", false);

    }

    public void startPlayStore(int position, Context context) {

        final String appPackageName = mApps.get(position).packageName;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        } catch (android.content.ActivityNotFoundException anfe) {
            Intent i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

}
