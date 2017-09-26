/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import com.android.internal.logging.MetricsProto;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;

import android.app.ActivityManagerNative;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentQueryMap;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

public class AsusEasyLauncherSettings extends SettingsPreferenceFragment implements SwitchBar.OnSwitchChangeListener, Indexable  {
    static final String TAG = "EasyModeSettings";
    static final String KEY_SHARED_PREFERENCES = "easymode_prefs";
    static final String SP_DEFAULT_HOME_PACKAGE = "default_home_package";
    static final String SP_DEFAULT_HOME_CLASS = "default_home_class";
    private static final ComponentName EASY_HOME =
            new ComponentName("com.asus.easylauncher","com.asus.easylauncher.AsusEasyLauncherActivity");

    private Observer mSettingsObserver;
    private ContentQueryMap mContentQueryMap;
    private SwitchBar mSwitchBar;
    private View mView;
    private CharSequence mOldActivityTitle;
    PackageManager mPm;
    ComponentName[] mHomeComponentSet;

    String mDefaultHomePackage = "com.asus.launcher";
    String mDefaultHomeClass ="com.android.launcher3.Launcher";
    final IntentFilter mHomeFilter;

    public AsusEasyLauncherSettings() {
        mHomeFilter = new IntentFilter(Intent.ACTION_MAIN);
        mHomeFilter.addCategory(Intent.CATEGORY_HOME);
        mHomeFilter.addCategory(Intent.CATEGORY_DEFAULT);
    }

    void makeCurrentHome(ComponentName newHome) {
        if(newHome.equals(EASY_HOME)) {
            //enable easy launcher
            mPm.setComponentEnabledSetting(EASY_HOME, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        } else {
            //set default(disable) laucnher
            mPm.setComponentEnabledSetting(EASY_HOME, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
        }
        buildHomeActivitiesList();
        mPm.replacePreferredActivity(mHomeFilter, IntentFilter.MATCH_CATEGORY_EMPTY,
                mHomeComponentSet, newHome);
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        i.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED|Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(i);
    }

   public void makeCurrentHomeFromEnable(ComponentName newHome, PackageManager mPm,Fragment f) {
        if(newHome.equals(EASY_HOME)) {
            //enable easy launcher
            mPm.setComponentEnabledSetting(EASY_HOME, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        } else {
            //set default(disable) laucnher
            mPm.setComponentEnabledSetting(EASY_HOME, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
        }
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        mPm.getHomeActivities(homeActivities);
        mHomeComponentSet = new ComponentName[homeActivities.size()];

        for (int i = 0; i < homeActivities.size(); i++) {
            final ResolveInfo candidate = homeActivities.get(i);
            final ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            mHomeComponentSet[i] = activityName;
        }
        mPm.replacePreferredActivity(mHomeFilter, IntentFilter.MATCH_CATEGORY_EMPTY,
                mHomeComponentSet, newHome);
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        i.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED|Intent.FLAG_ACTIVITY_NEW_TASK);
        f.startActivity(i);
    }

    void buildHomeActivitiesList() {
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        mPm.getHomeActivities(homeActivities);
        mHomeComponentSet = new ComponentName[homeActivities.size()];

        for (int i = 0; i < homeActivities.size(); i++) {
            final ResolveInfo candidate = homeActivities.get(i);
            final ActivityInfo info = candidate.activityInfo;
            ComponentName activityName = new ComponentName(info.packageName, info.name);
            mHomeComponentSet[i] = activityName;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.asus_easymode_tutorial, container, false);
        mPm = this.getActivity().getPackageManager();

        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);

        boolean state = Settings.System.getInt(this.getActivity().getContentResolver(),
                Settings.System.ASUS_EASY_LAUNCHER, 0) == 0 ? false : true;
        mSwitchBar.setChecked(state);
        mSwitchBar.show();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get Current Launcher info
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();
        ComponentName currentDefaultHome = mPm.getHomeActivities(homeActivities);
        SharedPreferences sp = this.getActivity().getSharedPreferences(KEY_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        int state = Settings.System.getInt(this.getActivity().getContentResolver(),
                Settings.System.ASUS_EASY_LAUNCHER, 0);

        if (state == Settings.System.ASUS_EASY_LAUNCHER_DISABLED) {
            //save the launcher should be restored from easymode
            SharedPreferences.Editor editor = sp.edit();
            if (currentDefaultHome != null) {
                editor.putString(SP_DEFAULT_HOME_PACKAGE, currentDefaultHome.getPackageName());
                editor.putString(SP_DEFAULT_HOME_CLASS, currentDefaultHome.getClassName());
                editor.commit();
            } else {
                editor.remove(SP_DEFAULT_HOME_PACKAGE);
                editor.remove(SP_DEFAULT_HOME_CLASS);
                editor.commit();
            }
        }

        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                public void update(Observable o, Object arg) {
                    handleStateChanged();
                }
            };
        }
        mContentQueryMap.addObserver(mSettingsObserver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean desiredState) {
        Settings.System.putInt(this.getActivity().getContentResolver(), Settings.System.ASUS_EASY_LAUNCHER, desiredState? 1:0);
        mSwitchBar.setChecked(desiredState);
        if (desiredState) {
            if(isDisable()){
                AsusEasyLauncherDialog dialog = AsusEasyLauncherDialog.newInstance();
                dialog.show(getFragmentManager(), "AsusEasyLauncherDialog");
            }
            else{
                makeCurrentHome(EASY_HOME);
            }
        } else {
            SharedPreferences sp = this.getActivity().getSharedPreferences(KEY_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            ComponentName normalHome = new ComponentName(sp.getString(SP_DEFAULT_HOME_PACKAGE, mDefaultHomePackage),
                sp.getString(SP_DEFAULT_HOME_CLASS, mDefaultHomeClass));
            makeCurrentHome(normalHome);
        }
        mSwitchBar.setEnabled(true);
    }

    protected void handleStateChanged() {
        int state = Settings.System.getInt(this.getActivity().getContentResolver(), Settings.System.ASUS_EASY_LAUNCHER, 0);
        if (Settings.System.ASUS_EASY_LAUNCHER_ENABLED != state) {
            SharedPreferences sp = this.getActivity().getSharedPreferences(KEY_SHARED_PREFERENCES, Context.MODE_PRIVATE);
            ComponentName normalHome = new ComponentName(sp.getString(SP_DEFAULT_HOME_PACKAGE, mDefaultHomePackage),
                sp.getString(SP_DEFAULT_HOME_CLASS, mDefaultHomeClass));
            makeCurrentHome(normalHome);
        } else {
            makeCurrentHome(EASY_HOME);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // listen for Location Manager settings changes
        Cursor settingsCursor = this.getActivity().getContentResolver().query(android.provider.Settings.System.CONTENT_URI, null,
                "(" + android.provider.Settings.System.NAME + "=?)",
                new String[]{android.provider.Settings.System.ASUS_EASY_LAUNCHER},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, android.provider.Settings.System.NAME, true, null);

    }

    private boolean isDisable(){
        boolean enabled = false;
        try{
            final PackageManager pm = getActivity().getPackageManager();
            enabled = pm.getApplicationInfo("com.asus.easylauncher",0).enabled;
        }catch(NameNotFoundException e){
            Log.w(TAG,"EzLauncher is not installed!!");
        }finally{
            return !enabled;
        }
    }

    static boolean hasEasyMode(Context context) {
        try {
            context.getPackageManager().getPackageInfo(EASY_HOME.getPackageName(), PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
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
                    if(hasEasyMode(context)) {
                        final Resources res = context.getResources();

                        //Add fragment title
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = res.getString(R.string.asus_easy_launcher_settings);
                        data.screenTitle = res.getString(R.string.asus_easy_launcher_settings);
                        data.keywords = res.getString(R.string.keywords_easy_mode);
                        result.add(data);
                    }

                    return result;
                }
            };
}
