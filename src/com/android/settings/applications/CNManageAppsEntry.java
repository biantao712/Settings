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

package com.android.settings.applications;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
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

import com.android.settings.SettingsActivity;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.asus.cncommonres.AsusButtonBar;

import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_ALL;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_BACKGROUND;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_INSTALLED;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_RUNNING_SERVICE;

/**
 * Activity to pick an application that will be used to display installation information and
 * options to uninstall/delete user data for system applications. This activity
 * can be launched through Settings or via the ACTION_MANAGE_PACKAGE_STORAGE
 * intent.
 */
public class CNManageAppsEntry extends InstrumentedFragment implements Indexable {

    static final String TAG = "CNManageAppsEntry";
    static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private View mRootView;

    private TabLayout mTabLayout;
    private ViewPager mViewPager;
    private FragmentPagerAdapter mPagerAdapter;
    private List<Fragment> mFragmentList;
    private List<String> mTabTitleList;
    private Fragment mFragment_installed;
    private Fragment mFragment_all;
    private Fragment mFragment_running;
    private Fragment mFragment_stack;

    private AsusButtonBar buttonBar;
    private ResetAppsHelper mResetAppsHelper;

    // sort order
    private int mSortOrder = SORT_ORDER_ALPHA;

    public final static int SORT_ORDER_ALPHA = 0;
    public final static int SORT_ORDER_SIZE = 1;
    private static final String EXTRA_SORT_ORDER = "sortOrder";
    private static final String EXTRA_SELECTED_TAB = "seltecteTab";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mSortOrder = savedInstanceState.getInt(EXTRA_SORT_ORDER, mSortOrder);
            CNManageAppsSync.getInstance().setSelectedTabPos(savedInstanceState.getInt(EXTRA_SELECTED_TAB, 0));
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.CNManageAppTheme);
        // clone the inflater using the ContextThemeWrapper
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        mRootView = localInflater.inflate(R.layout.cn_manage_applications_apps, null);
        initTabs(mRootView);
        initButtonBar(mRootView);

        mResetAppsHelper = new ResetAppsHelper(getActivity());
        mResetAppsHelper.onRestoreInstanceState(savedInstanceState);
        return mRootView;
    }

    public boolean startFragment(Fragment caller, String fragmentClass, int titleRes,
                                 int requestCode, Bundle extras) {
        final Activity activity = getActivity();
        if (activity instanceof SettingsActivity) {
            SettingsActivity sa = (SettingsActivity) activity;
            sa.startPreferencePanel(fragmentClass, extras, titleRes, null, caller, requestCode);
            return true;
        } else {
            Log.w(TAG,
                    "Parent isn't SettingsActivity nor PreferenceActivity, thus there's no way to "
                            + "launch the given Fragment (name: " + fragmentClass
                            + ", requestCode: " + requestCode + ")");
            return false;
        }
    }
    private void startDefaultAppFragment(){

        startFragment(this, DefaultAppSettings.class.getCanonicalName(),
                R.string.default_apps_setting, 0, null);
    }
    private void startSpecialAccessFragment(){

        startFragment(this, SpecialAccessSettings.class.getCanonicalName(),
                R.string.special_access, 0, null);
    }
    private void initButtonBar(View view){

        buttonBar = (AsusButtonBar) view.findViewById(R.id.button_bar);
        if(buttonBar != null) {
            buttonBar.setVisibility(View.VISIBLE);
            buttonBar.addButton(1, R.drawable.cn_default_setting_button,
                    getActivity().getResources().getString(R.string.default_apps_setting));
            buttonBar.getButton(1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    buttonBar.selectButton(1);
                    startDefaultAppFragment();
                }
            });
            buttonBar.addButton(2, R.drawable.cn_application_sort_button,
                    getActivity().getResources().getString(R.string.order_text));
            buttonBar.getButton(2).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    buttonBar.selectButton(2);
                    setDialog();
                }
            });
            buttonBar.addButton(3, R.drawable.cn_application_reset_button,
                    getActivity().getResources().getString(R.string.reset_app_preferences));
            buttonBar.getButton(3).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    buttonBar.selectButton(3);
                    mResetAppsHelper.buildResetDialog();
                }
            });
            buttonBar.addButton(4, R.drawable.cn_application_more_button,
                    getActivity().getResources().getString(R.string.wifi_ap_advanced_settings));
            buttonBar.getButton(4).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    buttonBar.selectButton(3);
                    startSpecialAccessFragment();
                }
            });
        }
    }
    private boolean f_installed_in = false;
    private boolean f_all_in = false;
    private void initTabs(View view) {
        mTabLayout = (TabLayout)view.findViewById(R.id.tabs);
        mViewPager = (ViewPager)view.findViewById(R.id.vp_view);
        OnAppPagesChange listener_pageChange = new OnAppPagesChange() {
            @Override
            public void onPageCreate(int type) {
                Log.d("blenda", "onPageCreate, type: "+type+"; f_installed_in: "+f_installed_in+"; f_all_in: "+f_all_in);
                if (type == EXTRA_APPLICATIONS_TYPE_INSTALLED) {
                    f_installed_in = true;
                    if (f_all_in){
                        mFragment_all.onDestroyView();
                    }
                }
                else if (type == EXTRA_APPLICATIONS_TYPE_ALL) {
                    f_all_in = true;
                    if (f_installed_in){
                        mFragment_installed.onDestroyView();
                    }
                }
            }

            @Override
            public void onPageDestroy(int type) {
                if (type == EXTRA_APPLICATIONS_TYPE_INSTALLED)
                    f_installed_in = false;
                else if (type == EXTRA_APPLICATIONS_TYPE_ALL)
                    f_all_in = false;

                Log.d("blenda", "onPageDestroy, type: "+type+"; f_installed_in: "+f_installed_in+"; f_all_in: "+f_all_in);
            }
        };
        Bundle bundle_installed = new Bundle();
        bundle_installed.putInt(EXTRA_APPLICATIONS_TYPE,EXTRA_APPLICATIONS_TYPE_INSTALLED);
        mFragment_installed = Fragment.instantiate(getActivity(), "com.android.settings.applications.CNManageApplications", bundle_installed);
//        ((CNManageApplications)mFragment_installed).setOnAppPagesChange(listener_pageChange);

        Bundle bundle_all = new Bundle();
        bundle_all.putInt(EXTRA_APPLICATIONS_TYPE,EXTRA_APPLICATIONS_TYPE_ALL);
        mFragment_all = Fragment.instantiate(getActivity(), "com.android.settings.applications.CNManageApplications", bundle_all);
//        ((CNManageApplications)mFragment_all).setOnAppPagesChange(listener_pageChange);

        Bundle bundle_running = new Bundle();
        bundle_running.putInt(EXTRA_APPLICATIONS_TYPE,EXTRA_APPLICATIONS_TYPE_RUNNING_SERVICE);
        mFragment_running = Fragment.instantiate(getActivity(), "com.android.settings.applications.CNRunningServices", bundle_running);
        Bundle bundle_bg = new Bundle();
        bundle_bg.putInt(EXTRA_APPLICATIONS_TYPE,EXTRA_APPLICATIONS_TYPE_BACKGROUND);
        mFragment_stack = Fragment.instantiate(getActivity(), "com.android.settings.applications.CNRunningServices", bundle_bg);

        mFragmentList = new ArrayList<Fragment>();
        mFragmentList.add(mFragment_installed);
        mFragmentList.add(mFragment_running);
        mFragmentList.add(mFragment_stack);
        mFragmentList.add(mFragment_all);

        String[] stringArray = getActivity().getResources().getStringArray(R.array.applications_category);
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
                Log.d("blenda", "onPageSelected: "+position);
                if (position == 0 || position == 3){
                    buttonBar.getButton(2).setVisibility(View.VISIBLE);
                } else {
                    buttonBar.getButton(2).setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d("blenda", "onTabSelected");
                if ((int) tab.getTag() == 0 || (int) tab.getTag() == 3){
                    ((View)buttonBar.getButton(2)).setVisibility(View.VISIBLE);
                } else {
                    ((View)buttonBar.getButton(2)).setVisibility(View.GONE);
                }
                CNManageAppsSync.getInstance().onTabSelectedChange((int) tab.getTag(), true);

            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                Log.d("blenda", "onTabUnselected, tag: "+tab.getTag());
                CNManageAppsSync.getInstance().onTabSelectedChange((int) tab.getTag(), false);
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

    private ButtonOnClick buttonOnClick = new ButtonOnClick(0);
    private class ButtonOnClick implements DialogInterface.OnClickListener {
        private int index;

        public ButtonOnClick(int index) {
            this.index = index;
        }

        @Override
        public void onClick(DialogInterface dialog,int which) {
            if (which >= 0) {
                index = which;
                if (which == 0){
                    mSortOrder = SORT_ORDER_ALPHA;
                }else{
                    mSortOrder = SORT_ORDER_SIZE;
                }
                CNManageAppsSync.getInstance().setSort(mSortOrder);
/*                if (f_installed_in) {
                    ((CNManageApplications) mFragment_all).setSortOrder(mSortOrder, false);
                    ((CNManageApplications)mFragment_installed).setSortOrder(mSortOrder, true);
                }else if (f_all_in) {
                    ((CNManageApplications) mFragment_all).setSortOrder(mSortOrder, true);
                    ((CNManageApplications) mFragment_installed).setSortOrder(mSortOrder, false);
                }*/
                dialog.dismiss();
            }
        }
    }

    private void setDialog(){
        AlertDialog.Builder alertDialog_single_list_item = new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(new String[] { getActivity().getResources().getString(R.string.sort_order_alpha),
                        getActivity().getResources().getString(R.string.sort_order_size)},
                        mSortOrder == SORT_ORDER_ALPHA?0:1, buttonOnClick)
                .setTitle(R.string.order_text)
                .setNegativeButton(R.string.cancel, null);
        AlertDialog orderDialog = alertDialog_single_list_item.show();

        Window dialogWindow = orderDialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        dialogWindow.setAttributes(lp);
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
        CNManageAppsSync.getInstance().setSelectedTabPos(0);
        mRootView = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SORT_ORDER, mSortOrder);
        outState.putInt(EXTRA_SELECTED_TAB, CNManageAppsSync.getInstance().getSelectedTabPos());

        mResetAppsHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        mResetAppsHelper.stop();
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                    final Resources res = context.getResources();

                    // Add fragment title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.applications_settings);
                    data.screenTitle = res.getString(R.string.applications_settings);
                    result.add(data);
                    return result;
                }

            };
}
