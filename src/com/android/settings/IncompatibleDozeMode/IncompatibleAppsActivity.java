package com.android.settings.IncompatibleDozeMode;

import android.annotation.Nullable;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.ActionBar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import src.com.android.settings.IncompatibleDozeMode.IncompatibleAppsController;

/**
 * Created by steve on 2016/3/8.
 */
public class IncompatibleAppsActivity extends Activity {

    private static final String TAG = "IncompatibleAppsActivity";
    private ListView mListView;
    private PackageManager mPackageManager;
    private IncompatibleAppsController mIncompatibleAppsController;
    private ArrayList<String> mBlackList = new ArrayList<String>();
    private static final int MENU_DONT_SHOW_AGAIN = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incompatibleapps);
        mIncompatibleAppsController = new IncompatibleAppsController();
        mPackageManager = getPackageManager();

        mListView = (ListView) findViewById(R.id.app_listview);

        getActionBar().setTitle(Html.fromHtml("<font color='#FFFFFF'>"
                + getResources().getString(R.string.incompatible_apps) + " </font>"));

        mIncompatibleAppsController.setListView(mListView);
        mIncompatibleAppsController.setAdapter(this);
        mIncompatibleAppsController.getBlackList(this, mBlackList);
        mIncompatibleAppsController.loadAppsInfo(mBlackList, mPackageManager);
        mIncompatibleAppsController.changeAdapterData();

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position,
                                    long id) {
                mIncompatibleAppsController.startPlayStore(position, getApplicationContext());
            }
        });

        mListView.setRecyclerListener(new AbsListView.RecyclerListener() {
            @Override
            public void onMovedToScrapHeap(View view) {
                final ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
                imageView.setImageBitmap(null);
            }
        });

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuItem notifyOff = menu.add(0, MENU_DONT_SHOW_AGAIN, 0, R.string.notify_off)
                .setCheckable(true).setChecked(mIncompatibleAppsController.getNotifyState(this));
        notifyOff.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case MENU_DONT_SHOW_AGAIN:
                item.setChecked(!item.isChecked());
                mIncompatibleAppsController.setNotifyOff(this,item.isChecked());
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    public static class AppListAdapter extends BaseAdapter {

        private final LayoutInflater mInflater;
        private List<PackageInfo> mList;
        private PackageManager mPackageManager;

        public AppListAdapter(Activity activity) {
            mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mPackageManager = activity.getPackageManager();
        }

        public void setData(List<PackageInfo> packageInfos) {
            mList = packageInfos;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList != null ? mList.size() : 0;
        }

        @Override
        public PackageInfo getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            convertView = mInflater.inflate(R.layout.inconpatible_item, parent, false);
            final PackageInfo appData = (PackageInfo) getItem(position);

            ((ImageView) convertView.findViewById(android.R.id.icon))
                    .setImageDrawable(appData.applicationInfo.loadIcon(mPackageManager));
            ((TextView) convertView.findViewById(android.R.id.title))
                    .setText(appData.applicationInfo.loadLabel(mPackageManager));

            return convertView;
        }
    }
}
