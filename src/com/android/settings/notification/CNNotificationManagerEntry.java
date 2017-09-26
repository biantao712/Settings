/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

import android.support.design.widget.TabLayout;
import android.app.Fragment;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.app.FragmentManager;

import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Toast;

import com.asus.cncommonres.AsusButtonBar;

import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_ALL;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_BACKGROUND;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_INSTALLED;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_RUNNING_SERVICE;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_NOTIFY;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_NOTIFY_IMPORTANCE;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_NOTIFY_HEADSUP;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_NOTIFY_ALLOWED;

import android.content.res.Resources;

import com.android.settings.search.Indexable;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;

/*
 *from package application
 */
import com.android.settings.applications.*;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class CNNotificationManagerEntry extends InstrumentedFragment  implements Indexable {

    static final String TAG = "CNManageAppsEntry_NF";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private View mRootView;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private FragmentPagerAdapter mPagerAdapter;
    private List<Fragment> mFragmentList;
    private List<String> mTabTitleList;
    /*Notification Fragment*/
    private Fragment mFragment_all;
    private Fragment mFragment_important;
    private Fragment mFragment_headsUp;
    private Fragment mFragment_disable;


    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;

    public final static int SORT_ORDER_ALPHA = 0;
    public final static int SORT_ORDER_SIZE = 1;
    private static final String EXTRA_SORT_ORDER = "sortOrder";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt(EXTRA_SORT_ORDER, mSortOrder);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.CNManageAppTheme);
        // clone the inflater using the ContextThemeWrapper
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        mRootView = localInflater.inflate(R.layout.cn_manage_notifications, null);
        initTabs(mRootView);

        return mRootView;
    }

    private boolean f_installed_in = false;
    private boolean f_all_in = false;
    private void initTabs(View view) {
        mTabLayout = (TabLayout)view.findViewById(R.id.tabs);
        mViewPager = (ViewPager)view.findViewById(R.id.vp_view);
 
        Bundle bundle_important = new Bundle();
        bundle_important.putInt(EXTRA_APPLICATIONS_TYPE,EXTRA_APPLICATIONS_TYPE_NOTIFY_IMPORTANCE);
        mFragment_important = Fragment.instantiate(getActivity(), "com.android.settings.applications.CNNotifyApplications", bundle_important);
        Bundle bundle_headsup = new Bundle();
        bundle_headsup.putInt(EXTRA_APPLICATIONS_TYPE,EXTRA_APPLICATIONS_TYPE_NOTIFY_HEADSUP);
        mFragment_headsUp = Fragment.instantiate(getActivity(), "com.android.settings.applications.CNNotifyApplications", bundle_headsup);
        Bundle bundle_disable = new Bundle();
        bundle_disable.putInt(EXTRA_APPLICATIONS_TYPE,EXTRA_APPLICATIONS_TYPE_NOTIFY_ALLOWED);
        mFragment_disable = Fragment.instantiate(getActivity(), "com.android.settings.applications.CNNotifyApplications", bundle_disable);
        Bundle bundle_all = new Bundle();
        bundle_all.putInt(EXTRA_APPLICATIONS_TYPE,EXTRA_APPLICATIONS_TYPE_NOTIFY);
        mFragment_all = Fragment.instantiate(getActivity(), "com.android.settings.applications.CNNotifyApplications", bundle_all);

        mFragmentList = new ArrayList<Fragment>();
        mFragmentList.add(mFragment_all);
        mFragmentList.add(mFragment_important);
        mFragmentList.add(mFragment_headsUp);
        mFragmentList.add(mFragment_disable);

        String[] stringArray = getActivity().getResources().getStringArray(R.array.cn_manage_notification_values);
        mTabTitleList = new ArrayList<String>();
        for (String str : stringArray) {
            mTabTitleList.add(str);
        }

        mTabLayout.setTabMode(TabLayout.MODE_FIXED);

        mTabLayout.addTab(mTabLayout.newTab().setText(mTabTitleList.get(0)));
        mTabLayout.addTab(mTabLayout.newTab().setText(mTabTitleList.get(1)));
        mTabLayout.addTab(mTabLayout.newTab().setText(mTabTitleList.get(2)));
        mTabLayout.addTab(mTabLayout.newTab().setText(mTabTitleList.get(3)));

        mPagerAdapter = new MyPagerAdapter(getActivity().getFragmentManager(),mFragmentList,mTabTitleList);
        mViewPager.setAdapter(mPagerAdapter);
//        mViewPager.setOffscreenPageLimit(3);
        mTabLayout.setupWithViewPager(mViewPager);
        for (int i = 0; i < mTabLayout.getTabCount(); i++) {
            TabLayout.Tab tab = mTabLayout.getTabAt(i);
            if (tab != null) {
                tab.setTag(i);
            }
        }
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0)
                {
                    //mFragment_all.show();
                    //mFragment_important.hide();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                Log.d("blenda", "onTabUnselected, tag: "+tab.getTag());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    static class MyPagerAdapter extends FragmentPagerAdapter {

        private List<Fragment> list_fragment;
        private List<String> list_Title;

        public MyPagerAdapter(FragmentManager fm,List<Fragment> list_fragment,List<String> list_Title) {
            super(fm);
            this.list_fragment = list_fragment;
            this.list_Title = list_Title;
        }

        @Override
        public Fragment getItem(int position) {
            return list_fragment.get(position);
        }

        @Override
        public int getCount() {
            return list_Title.size();
        }

        //此方法用来显示tab上的名字
        @Override
        public CharSequence getPageTitle(int position) {
            return list_Title.get(position % list_Title.size());
        }
    }
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.MANAGE_APPLICATIONS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mRootView = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SORT_ORDER, mSortOrder);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    // add to search
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                    final Resources res = context.getResources();

                    // Add fragment title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.cn_notification_manager_title);
                    data.screenTitle = res.getString(R.string.cn_notification_manager_title);
                    result.add(data);
                    return result;
                }
            };

}
