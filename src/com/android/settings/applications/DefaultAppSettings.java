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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.AsusTelephonyUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.applications.PermissionsSummaryHelper.PermissionsResultCallback;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.Session;

import com.asus.cncommonres.AsusButtonBar;
import com.asus.cncommonres.AsusButtonBarButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.os.UserManager;
import android.os.UserHandle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class DefaultAppSettings extends SettingsPreferenceFragment {


    private static final String KEY_DEFAULT_BROWSER = "default_browser";
    private static final String KEY_DEFAULT_PHONE = "default_phone_app";
    private static final String KEY_DEFAULT_SMS = "default_sms_app";
    private static final String KEY_DEFAULT_HOME = "default_home";

    private PreferenceScreen mDefaultHomePreference;
    private PreferenceScreen mDefaultBrowserPreference;
    private PreferenceScreen mDefaultPhonePreference;
    private PreferenceScreen mDefaultSmsPreference;

    private CNDefaultBrowserHelper mBrowserHelper;
    private CNDefaultPhoneHelper mPhoneHelper;
    private CNDefaultSmsHelper mSmsHelper;
    private CNDefaultHomeHelper mHomeHelper;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.default_app_setting);
        mDefaultHomePreference = (PreferenceScreen)findPreference(KEY_DEFAULT_HOME);
        mDefaultBrowserPreference = (PreferenceScreen)findPreference(KEY_DEFAULT_BROWSER);
        mDefaultPhonePreference = (PreferenceScreen)findPreference(KEY_DEFAULT_PHONE);
        mDefaultSmsPreference = (PreferenceScreen)findPreference(KEY_DEFAULT_SMS);

        // +++ Mark_Huang@20160105: Verizon VZ_REQ_UI_37449
        //  ckenken (ChiaHsiang_Kuo) @ 20161115 TT-907047 check if the device doesn't have sms feature
        if (null != mDefaultSmsPreference) {
            if (!AsusTelephonyUtils.isVerizon()) {
                mDefaultSmsPreference.setTitle(R.string.sms_application_title);
            } else {
                mDefaultSmsPreference.setTitle(R.string.vzw_sms_application_title);
            }
        }
        // --- Mark_Huang@20160105: Verizon VZ_REQ_UI_37449
        mBrowserHelper = CNDefaultBrowserHelper.getInstance(getActivity());
        mHomeHelper = CNDefaultHomeHelper.getInstance(getActivity());
        mSmsHelper = CNDefaultSmsHelper.getInstance(getActivity());
        mPhoneHelper = CNDefaultPhoneHelper.getInstance(getActivity());
    }
    @Override
    public void onResume(){
        super.onResume();
        initButtonBar();
        updateUi();
    }

/*    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mBrowserHelper = CNDefaultBrowserHelper.getInstance(context);
        mBrowserHelper.registerMonitor();
    }

    @Override
    public void onDetach() {
        mBrowserHelper.unregisterMonitor();
        super.onDetach();
    }*/


    public boolean isPhoneAvailable(Context context) {
        final TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (!tm.isVoiceCapable()) {
            return false;
        }

        final UserManager um =
                (UserManager) context.getSystemService(Context.USER_SERVICE);
        return !um.hasUserRestriction(UserManager.DISALLOW_OUTGOING_CALLS);
    }
    public boolean isSmsAvailable(Context context) {
        boolean isRestrictedUser =
                UserManager.get(context)
                        .getUserInfo(UserHandle.myUserId()).isRestricted();
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return !isRestrictedUser && tm.isSmsCapable();
    }
    private void updateUi() {

        mHomeHelper.refreshHomeOptions();
        mBrowserHelper.refreshBrowserApps();
        mDefaultHomePreference.setSummary(mHomeHelper.getSummary());
        mDefaultBrowserPreference.setSummary(mBrowserHelper.getSummary());
        if (!isPhoneAvailable(getActivity())){
            getPreferenceScreen().removePreference(mDefaultPhonePreference);
        }else {
            mPhoneHelper.loadDialerApps();
            mDefaultPhonePreference.setSummary(mPhoneHelper.getSummary());
        }
        if (!isSmsAvailable(getActivity())){
            getPreferenceScreen().removePreference(mDefaultSmsPreference);
        }else {
            mSmsHelper.loadSmsApps();
            mDefaultSmsPreference.setSummary(mSmsHelper.getSummary());
        }

    }
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_ADVANCED;
    }

    private void initButtonBar(){

        AsusButtonBar buttonBar = ((SettingsActivity)getActivity()).getButtonBar();
        buttonBar.setVisibility(View.VISIBLE);
        buttonBar.addButton(1, R.drawable.cn_application_reset_button,
                getActivity().getResources().getString(R.string.reset_app_default));
        buttonBar.getButton(1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createResetDialog();
            }
        });
    }

    private AlertDialog createResetDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setPositiveButton(R.string.reset_app_default,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            resetAppToDefault();
                        }
                    });

        builder.setNegativeButton(R.string.cancel, null);

        View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
        TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
        title.setText(getActivity().getResources().getString(R.string.reset_app_default));
        TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
        String text = getActivity().getResources().getString(R.string.reset_default_dialog_content);
        message.setText(text);

        builder.setView(view1);
        AlertDialog dialog = builder.show();

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        dialogWindow.setAttributes(lp);
        return dialog;
    }
    private void resetAppToDefault(){
        mHomeHelper.setToDefault();
        mBrowserHelper.setToDefault();
        mSmsHelper.setToDefault();
        mPhoneHelper.setToDefault();
        CNDefaultAssistHelper mAssistHelper = CNDefaultAssistHelper.getInstance(getActivity());
        mAssistHelper.setAssistNone();
        updateUi();
    }
}
