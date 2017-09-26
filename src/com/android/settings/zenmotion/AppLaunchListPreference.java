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

package com.android.settings.zenmotion;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.content.ComponentName;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.twinApps.TwinAppsUtil;

import libcore.util.Objects;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Extends ListPreference to allow us to show the icons for a given list of launch activities. We do
 * this because the names of applications are very similar and the user may not be able to determine
 * what app they are selecting without an icon.
 */
public class AppLaunchListPreference extends ListPreference implements
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "AppLaunchListPreference";
    private static final String APP_ASUS_BOOSTER = "taskmanager";
    private static final String APP_FRONT_CAMERA = "frontCamera";
    private static final String APP_CAMERA_PACKAGE = "com.asus.camera";
    private static final String SYMBOL = "/";
    private static final String WAKE_UP_SCREEN = "wakeUpScreen";

    static final String KEY_W_LAUNCH = "w_launch";
    static final String KEY_S_LAUNCH = "s_launch";
    static final String KEY_E_LAUNCH = "e_launch";
    static final String KEY_C_LAUNCH = "c_launch";
    static final String KEY_Z_LAUNCH = "z_launch";
    static final String KEY_V_LAUNCH = "v_launch";

    private final Object mEntryLock = new Object();
    private final List<CharSequence> mEntry = new ArrayList<>();       // APP name
    private final List<CharSequence> mEntryValues = new ArrayList<>(); // package/activity
    private final List<Drawable> mEntryDrawables = new ArrayList<>();

    private Switch mSwitch;
    private boolean mIsChecked = false;
    private AppLaunchListSwitchChangedListener mAppLaunchListSwitchChangedListener;

    // For switch of app launch
    public interface AppLaunchListSwitchChangedListener {
        public void onAppLaunchListSwitchChanged(String key, boolean isChecked);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mIsChecked == isChecked) return;
        mIsChecked = isChecked;
        mAppLaunchListSwitchChangedListener.onAppLaunchListSwitchChanged(getKey(), isChecked);
    }

    public void setAppLaunchListSwitchChangedListener(
            AppLaunchListSwitchChangedListener onAppLaunchListSwitchChangedListener) {
        mAppLaunchListSwitchChangedListener = onAppLaunchListSwitchChangedListener;
    }

    public AppLaunchListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.asus_preference_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSwitch = (Switch) view.findViewById(R.id.preference_switch);
        mSwitch.setOnCheckedChangeListener(this);
        setSwitchChecked(mIsChecked);
        setOnPreferenceClickListener(getOnPreferenceClickListener());
    }

    public class AppArrayAdapter extends ArrayAdapter<CharSequence> {
        public AppArrayAdapter(Context context, CharSequence[] objects) {
            super(context, R.layout.zenmotion_app_preference_item, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.zenmotion_app_preference_item, parent, false);
            CheckedTextView checkedTextView = (CheckedTextView) view.findViewById(R.id.app_label);
            checkedTextView.setText(getItem(position));
            if (position == findIndexOfValue(getValue())) {
                checkedTextView.setChecked(true);
            }
            ImageView imageView = (ImageView) view.findViewById(R.id.app_image);
            imageView.setImageDrawable(mEntryDrawables.get(position));
            return view;
        }
    }

    public void setSwitchChecked(boolean checked) {
        mIsChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public void setPackageNames() {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return setupEntries(sortAppInfos(getAppInfos()));
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != Integer.MIN_VALUE) {
                    updateEntry(result);
                }
            }
        }.execute();
    }

    private Integer setupEntries(List<AppInfo> appInfos) {
        // Show the label and icon for each application package.(packageName/activityName)
        synchronized (mEntryLock) {
            Pair<String, String> pair = parsePackageAndActivity(getSettingsSystemKeyValue());
            String packageName = pair.first;
            String activityName = pair.second;
            if (!mEntry.isEmpty()) {
                // Keep the current one (first match) if it exists
                for (AppInfo app : appInfos) {
                    if (Objects.equal(packageName, app.packageName) &&
                            Objects.equal(activityName, app.activityName))
                        return appInfos.indexOf(app);
                }
                // Do nothing
                return Integer.MIN_VALUE;
            }
            Integer index = 0;
            mEntry.clear();
            mEntryValues.clear();
            mEntryDrawables.clear();

            //Add wake up screen entry in first place
            appInfos.add(0,AppInfo.getWakeUpScreen(getContext()));


            for (AppInfo app : appInfos) {
                mEntry.add(app.label);
                mEntryDrawables.add(app.icon);
                mEntryValues.add(app.activityName == null
                        ? app.packageName
                        : app.packageName + SYMBOL + app.activityName);
                if (Objects.equal(packageName, app.packageName) &&
                    Objects.equal(activityName, app.activityName)) {
                    index = appInfos.indexOf(app);
                }
            }
            setEntries(mEntry.toArray(new CharSequence[mEntry.size()]));
            setEntryValues(mEntryValues.toArray(new CharSequence[mEntryValues.size()]));
            return index;
        }
    }

    private void updateEntry(Integer index) {
        setValueIndex(index);
        setIcon(mEntryDrawables.get(index));
        setSummary(getEntry());
    }

    public void updateEntry(String defaultValue) {
        if (null == defaultValue) return;
        CharSequence[] entryValues = getEntryValues();
	if (null == entryValues) return;
        for (int i = 0; i < entryValues.length; i++) {
            if (entryValues[i] != null && defaultValue.contentEquals(entryValues[i])) {
                updateEntry(i);
                return;
            }
        }
    }

//    @Override
//    protected void onPrepareDialogBuilder(final Builder builder) {
//        if (getEntries() == null) {
//            setupEntries(sortAppInfos(getAppInfos()));
//        }
//        builder.setAdapter(new AppArrayAdapter(getContext(), getEntries()), this);
//        super.onPrepareDialogBuilder(builder);
//    }

    private Pair<String, String> parsePackageAndActivity(String savedValue) {
        if (APP_ASUS_BOOSTER.equals(savedValue)) return Pair.create(APP_ASUS_BOOSTER, null);
	if (WAKE_UP_SCREEN.equals(savedValue)) return Pair.create(WAKE_UP_SCREEN, null);
        if (APP_FRONT_CAMERA.equals(savedValue)) return Pair.create(APP_FRONT_CAMERA, null);
        if (null != savedValue && savedValue.contains(SYMBOL)) {
            String[] splitedValue = savedValue.split(SYMBOL);
            return Pair.create(splitedValue[0], splitedValue[1]);
        } else {
            // Use asus booster as default value
            return Pair.create(APP_ASUS_BOOSTER, null);
        }
    }

    private static class AppInfo {
        public final String packageName;
        public final String activityName;
        public final CharSequence label;
        public final Drawable icon;

        AppInfo(String packageName, String activityName, CharSequence label, Drawable icon) {
            this.packageName = packageName;
            this.activityName = activityName;
            this.label = label;
            this.icon = icon;
        }

        static AppInfo convertFromResolveInfo(ResolveInfo resolveInfo, PackageManager pm) {
            return new AppInfo(resolveInfo.activityInfo.packageName,
                    resolveInfo.activityInfo.name, resolveInfo.activityInfo.loadLabel(pm),
                    resolveInfo.activityInfo.loadIcon(pm));
        }

        static AppInfo getAsusBooster(Context context) {
            return new AppInfo(APP_ASUS_BOOSTER, null,
                    context.getString(R.string.app_asus_boost_name),
                    context.getDrawable(R.drawable.asus_icon_boost));
        }

        static AppInfo getFrontCamera(Context context) {
            Drawable icon = null;
            try {
                icon = context.getPackageManager().getApplicationIcon(APP_CAMERA_PACKAGE);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            return new AppInfo(APP_FRONT_CAMERA, null,
                    context.getString(R.string.app_asus_front_camera_name), icon);
        }

        //Get App Info for SmartLauncher Weather
        static AppInfo getSmartLauncherWeather(Context context) {
            Drawable icon = null;
            try {
                icon = context.getPackageManager().getActivityIcon(new ComponentName("com.asus.launcher3","com.asus.zenlife.zlweather.ZLWeatherActivity"));
            } catch (NameNotFoundException e) {
                Log.w(TAG, "Failed getting icon for smartlauncher.");
                e.printStackTrace();
            }
            return new AppInfo("com.asus.launcher3", "com.asus.zenlife.zlweather.ZLWeatherActivity",
                    context.getString(R.string.app_asus_smartlauncher_weather_name), icon);
        }

        static AppInfo getWakeUpScreen(Context context) {
            return new AppInfo(WAKE_UP_SCREEN, null,
                    context.getString(R.string.gesture_wake_up_screen),
                    context.getDrawable(R.drawable.asus_ic_wake_up_screen));
        }
    }

    private List<AppInfo> sortAppInfos(List<AppInfo> appInfos) {
        Collections.sort(appInfos, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return Collator.getInstance().compare(lhs.label.toString(), rhs.label.toString());
            }
        });
        return appInfos;
    }

    private List<AppInfo> getAppInfos() {
        Context context = getContext();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos;
        //[TwinApps] {
        if (TwinAppsUtil.isTwinAppsSupport(context)) {
            resolveInfos = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        , PackageManager.MATCH_USER);
        } else {
        //[TwinApps] }
            resolveInfos = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        //[TwinApps] {
        }
        //[TwinApps] }
        List<AppInfo> appInfos = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            appInfos.add(AppInfo.convertFromResolveInfo(resolveInfo, pm));
        }
        if (supportFrontCamera(pm)) {
            appInfos.add(AppInfo.getFrontCamera(context));
        }
        //check if AsusSmartLauncher is exists
        boolean isSmartLauncherInstalled = true;
        boolean isSmartLauncherEnabled = true;
        try {
            PackageInfo SmartLauncherPackageInfo = context.getPackageManager().getPackageInfo("com.asus.launcher3",PackageManager.GET_META_DATA);
            ApplicationInfo SmartLauncherApplicationInfo = SmartLauncherPackageInfo.applicationInfo;
            isSmartLauncherEnabled = SmartLauncherApplicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed querying backup app, Assuming it is not installed.");
            isSmartLauncherInstalled = false;
        }
        if (isSmartLauncherInstalled && isSmartLauncherEnabled) {
            appInfos.add(AppInfo.getSmartLauncherWeather(context));
        }
        appInfos.add(AppInfo.getAsusBooster(context));
        return appInfos;
    }

    private boolean supportFrontCamera(PackageManager pm) {
        Intent intent = new Intent("com.asus.camera.action.STILL_IMAGE_FRONT_CAMERA");
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    String getSettingsSystemKey() {
        String key = getKey();
        return KEY_W_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE1_APP :
               KEY_S_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE2_APP :
               KEY_E_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE3_APP :
               KEY_C_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE4_APP :
               KEY_Z_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE5_APP :
               KEY_V_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE6_APP :
               null;
    }

    String getSettingsSystemKeyValue() {
        return Settings.System.getString(getContext().getContentResolver(), getSettingsSystemKey());
    }
}
