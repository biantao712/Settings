/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.twinApps.TwinAppsUtil;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;

public class ClearDefaultsPreference extends Preference {

    protected static final String TAG = ClearDefaultsPreference.class.getSimpleName();

    private Button mActivitiesButton;
    private TextView mAutoLaunchView;

    private AppWidgetManager mAppWidgetManager;
    private IUsbManager mUsbManager;
    private PackageManager mPm;
    private String mPackageName;
    protected ApplicationsState.AppEntry mAppEntry;
    //[TwinApps] {
    private boolean mEnableTwinApps = false;
    private int mUserId = -1;
    //[TwinApps] }

    public ClearDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setLayoutResource(R.layout.app_preferred_settings);

        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mPm = context.getPackageManager();
        IBinder b = ServiceManager.getService(Context.USB_SERVICE);
        mUsbManager = IUsbManager.Stub.asInterface(b);
        //[TwinApps] {
        mEnableTwinApps = TwinAppsUtil.isTwinAppsSupport(context);
        //[TwinApps] }
    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClearDefaultsPreference(Context context) {
        this(context, null);
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public void setAppEntry(ApplicationsState.AppEntry entry) {
        mAppEntry = entry;
        //[TwinApps] {
        if(mEnableTwinApps) {
            mUserId = UserHandle.getUserId(mAppEntry.info.uid);
        } else {
            mUserId = UserHandle.myUserId();
        }
        //[TwinApps] }
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        mAutoLaunchView = (TextView) view.findViewById(R.id.auto_launch);
        mActivitiesButton = (Button) view.findViewById(R.id.clear_activities_button);
        mActivitiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                creatHintDialog();
/*                if (mUsbManager != null) {
                    //final int userId = UserHandle.myUserId(); //[TwinApps] replaced by mUserId
                    mPm.clearPackagePreferredActivities(mPackageName);
                    if (isDefaultBrowser(mPackageName)) {
                        mPm.setDefaultBrowserPackageNameAsUser(null, mUserId); //[TwinApps]
                    }
                    try {
                        mUsbManager.clearDefaults(mPackageName, mUserId); //[TwinApps]
                    } catch (RemoteException e) {
                        Log.e(TAG, "mUsbManager.clearDefaults", e);
                    }
                    mAppWidgetManager.setBindAppWidgetPermission(mPackageName, false);
                    TextView autoLaunchView = (TextView) view.findViewById(R.id.auto_launch);
                    resetLaunchDefaultsUi(autoLaunchView);
                }*/
            }
        });

        updateUI(view);
    }

    private void creatHintDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(R.string.proxy_clear_text,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (mUsbManager != null) {
                    //final int userId = UserHandle.myUserId(); //[TwinApps] replaced by mUserId
                   mPm.clearPackagePreferredActivities(mPackageName);
                    if (isDefaultBrowser(mPackageName)) {
                        mPm.setDefaultBrowserPackageNameAsUser(null, mUserId); //[TwinApps]
                    }
                    try {
                        mUsbManager.clearDefaults(mPackageName, mUserId); //[TwinApps]
                    } catch (RemoteException e) {
                        Log.e(TAG, "mUsbManager.clearDefaults", e);
                    }
                    mAppWidgetManager.setBindAppWidgetPermission(mPackageName, false);
                    resetLaunchDefaultsUi(mAutoLaunchView);

                    }
                    }
                });
        builder.setNegativeButton(R.string.cancel, null);

        View view1 = LayoutInflater.from(getContext()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
        TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
        title.setText(getContext().getResources().getString(R.string.hint));
        TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
        String text = getContext().getResources().getString(R.string.clear_defaults_hint);
        message.setText(text);

        builder.setView(view1);
        AlertDialog dialog = builder.show();

        Button btnPositive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        btnPositive.setTextColor(0xFFFD3424);
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        dialogWindow.setAttributes(lp);

    }
    public boolean updateUI(PreferenceViewHolder view) {
        boolean hasBindAppWidgetPermission =
                mAppWidgetManager.hasBindAppWidgetPermission(mAppEntry.info.packageName);

        TextView autoLaunchView = (TextView) view.findViewById(R.id.auto_launch);
        boolean autoLaunchEnabled = AppUtils.hasPreferredActivities(mPm, mPackageName)
                || isDefaultBrowser(mPackageName)
                || AppUtils.hasUsbDefaults(mUsbManager, mPackageName);
        if (!autoLaunchEnabled && !hasBindAppWidgetPermission) {
            resetLaunchDefaultsUi(autoLaunchView);
        } else {
            boolean useBullets = hasBindAppWidgetPermission && autoLaunchEnabled;

            if (hasBindAppWidgetPermission) {
                autoLaunchView.setText(R.string.auto_launch_label_generic);
            } else {
                autoLaunchView.setText(R.string.auto_launch_label);
            }

            Context context = getContext();
            CharSequence text = null;
            int bulletIndent = context.getResources().getDimensionPixelSize(
                    R.dimen.installed_app_details_bullet_offset);
            if (autoLaunchEnabled) {
                CharSequence autoLaunchEnableText = context.getText(
                        R.string.auto_launch_enable_text);
                SpannableString s = new SpannableString(autoLaunchEnableText);
                if (useBullets) {
                    s.setSpan(new BulletSpan(bulletIndent), 0, autoLaunchEnableText.length(), 0);
                }
                text = (text == null) ?
                        TextUtils.concat(s, "\n") : TextUtils.concat(text, "\n", s, "\n");
            }
            if (hasBindAppWidgetPermission) {
                CharSequence alwaysAllowBindAppWidgetsText =
                        context.getText(R.string.always_allow_bind_appwidgets_text);
                SpannableString s = new SpannableString(alwaysAllowBindAppWidgetsText);
                if (useBullets) {
                    s.setSpan(new BulletSpan(bulletIndent),
                            0, alwaysAllowBindAppWidgetsText.length(), 0);
                }
                text = (text == null) ?
                        TextUtils.concat(s, "\n") : TextUtils.concat(text, "\n", s, "\n");
            }
            autoLaunchView.setText(text);
            mActivitiesButton.setEnabled(true);
        }
        return true;
    }

    private boolean isDefaultBrowser(String packageName) {
        final String defaultBrowser = mPm.getDefaultBrowserPackageNameAsUser(UserHandle.myUserId());
        return packageName.equals(defaultBrowser);
    }

    private void resetLaunchDefaultsUi(TextView autoLaunchView) {
        autoLaunchView.setText(R.string.auto_launch_disable_text);
        // Disable clear activities button
        mActivitiesButton.setEnabled(false);
    }
}
