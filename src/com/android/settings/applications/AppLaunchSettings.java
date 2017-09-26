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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IntentFilterVerificationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.util.ArraySet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;

import java.util.List;

import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;
import static android.content.pm.PackageManager.INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED;

public class AppLaunchSettings extends AppInfoWithHeader implements OnClickListener,
        OnPreferenceClickListener {
    private static final String TAG = "AppLaunchSettings";

    private static final String KEY_APP_LINK_STATE = "app_link_state";
    private static final String KEY_SUPPORTED_DOMAIN_URLS = "app_launch_supported_domain_urls";
    private static final String KEY_CLEAR_DEFAULTS = "app_launch_clear_defaults";

    private static final Intent sBrowserIntent;
    static {
        sBrowserIntent = new Intent()
                .setAction(Intent.ACTION_VIEW)
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .setData(Uri.parse("http:"));
    }

    private PackageManager mPm;

    private boolean mIsBrowser;
    private boolean mHasDomainUrls;
    private Preference mAppLinkState;
    private Preference mAppDomainUrls;
    private ClearDefaultsPreference mClearDefaultsPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.installed_app_launch_settings);
        mAppDomainUrls = (Preference) findPreference(KEY_SUPPORTED_DOMAIN_URLS);
        mClearDefaultsPreference = (ClearDefaultsPreference) findPreference(KEY_CLEAR_DEFAULTS);
        mAppLinkState = (Preference) findPreference(KEY_APP_LINK_STATE);

        mPm = getActivity().getPackageManager();

        mIsBrowser = isBrowserApp(mPackageName);
        mHasDomainUrls =
                (mAppEntry.info.privateFlags & ApplicationInfo.PRIVATE_FLAG_HAS_DOMAIN_URLS) != 0;

        /*if (!mIsBrowser) {
            List<IntentFilterVerificationInfo> iviList = mPm.getIntentFilterVerifications(mPackageName);
            List<IntentFilter> filters = mPm.getAllIntentFilters(mPackageName);
            CharSequence[] entries = getEntries(mPackageName, iviList, filters);
            mAppDomainUrls.setTitles(entries);
            mAppDomainUrls.setValues(new int[entries.length]);
        }*/
        buildStatePreference();
    }

    // An app is a "browser" if it has an activity resolution that wound up
    // marked with the 'handleAllWebDataURI' flag.
    private boolean isBrowserApp(String packageName) {
        sBrowserIntent.setPackage(packageName);
        List<ResolveInfo> list = mPm.queryIntentActivitiesAsUser(sBrowserIntent,
                PackageManager.MATCH_ALL, mUserId);
        final int count = list.size();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo != null && info.handleAllWebDataURI) {
                return true;
            }
        }
        return false;
    }

    private void buildStatePreference() {
        if (mIsBrowser) {
            // Browsers don't show the app-link prefs
//            mAppLinkState.setShouldDisableView(true);
            mAppLinkState.setEnabled(false);
//            mAppDomainUrls.setShouldDisableView(true);
            mAppDomainUrls.setEnabled(false);
        } else {
            // Designed order of states in the dropdown:
            //
            // * always
            // * ask
            // * never
/*            mAppLinkState.setEntries(new CharSequence[] {
                    getString(R.string.app_link_open_always),
                    getString(R.string.app_link_open_ask),
                    getString(R.string.app_link_open_never),
            });
            mAppLinkState.setEntryValues(new CharSequence[] {
                    Integer.toString(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS),
                    Integer.toString(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK),
                    Integer.toString(INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER),
            });*/

            mAppLinkState.setEnabled(mHasDomainUrls);
            mAppDomainUrls.setEnabled(mHasDomainUrls);
            if (mHasDomainUrls) {
                // Present 'undefined' as 'ask' because the OS treats them identically for
                // purposes of the UI (and does the right thing around pending domain
                // verifications that might arrive after the user chooses 'ask' in this UI).
                final int state = mPm.getIntentVerificationStatusAsUser(mPackageName, mUserId);
                mAppLinkState.setSummary(convertStateToString(state));
                mAppLinkState.setOnPreferenceClickListener(this);
                mAppDomainUrls.setOnPreferenceClickListener(this);
/*                mAppLinkState.setValue(
                        Integer.toString((state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED)
                                ? INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK
                                        : state));

                // Set the callback only after setting the initial selected item
                mAppLinkState.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        return updateAppLinkState(Integer.parseInt((String) newValue));
                    }
                });*/
            }
        }
    }

    private boolean updateAppLinkState(final int newState) {
        if (mIsBrowser) {
            // We shouldn't get into this state, but if we do make sure
            // not to cause any permanent mayhem.
            return false;
        }

        final int userId = mUserId;
        final int priorState = mPm.getIntentVerificationStatusAsUser(mPackageName, userId);

        if (priorState == newState) {
            return false;
        }

        boolean success = mPm.updateIntentVerificationStatusAsUser(mPackageName, newState, userId);
        if (success) {
            // Read back the state to see if the change worked
            final int updatedState = mPm.getIntentVerificationStatusAsUser(mPackageName, userId);
            success = (newState == updatedState);
        } else {
            Log.e(TAG, "Couldn't update intent verification status!");
        }
        return success;
    }

    private CharSequence[] getEntries(String packageName, List<IntentFilterVerificationInfo> iviList,
            List<IntentFilter> filters) {
        ArraySet<String> result = Utils.getHandledDomains(mPm, packageName);
        return result.toArray(new CharSequence[result.size()]);
    }

    @Override
    protected boolean refreshUi() {
        mClearDefaultsPreference.setPackageName(mPackageName);
        mClearDefaultsPreference.setAppEntry(mAppEntry);
        return true;
    }

    @Override
    protected AlertDialog createDialog(int id, int errorCode) {
        // No dialogs for preferred launch settings.
        return null;
    }

    @Override
    public void onClick(View v) {
        // Nothing to do
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mAppLinkState) {
            createChooseLinkDialog();
        } else if (preference == mAppDomainUrls) {

            InstalledAppDetails.startAppInfoFragment(CNSupportLinkList.class, mAppDomainUrls.getTitle(), this, mAppEntry);
        } else {
            return false;
        }
        return true;
    }

    private ButtonOnClick buttonOnClick = new ButtonOnClick(1);
    private class ButtonOnClick implements DialogInterface.OnClickListener {
        private int index;

        public ButtonOnClick(int index) {
            this.index = index;
        }

        @Override
        public void onClick(DialogInterface dialog,int which) {
            if (which >= 0) {
                index = which;
                updateAppLinkState(convertPositionToState(index));
                mAppLinkState.setSummary(convertStateToString(convertPositionToState(index)));
            }
            dialog.dismiss();
        }
    }

    private int convertStateToPosition(int state){
        if (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS)
            return 0;
        if (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK)
            return 1;
        if (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER)
            return 2;
        if (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED)
            return 1;
        return 1;
    }

    private String convertStateToString(int state){
        if (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS)
            return getString(R.string.app_link_open_always);
        if (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK)
            return getString(R.string.app_link_open_ask);
        if (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER)
            return getString(R.string.app_link_open_never);
        if (state == INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED)
            return getString(R.string.app_link_open_ask);
        return getString(R.string.app_link_open_ask);
    }

    private int convertPositionToState(int position){
        if (position == 0)
            return INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS;
        if (position == 1)
            return INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_ALWAYS_ASK;
        if (position == 2)
            return INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_NEVER;
        return INTENT_FILTER_DOMAIN_VERIFICATION_STATUS_UNDEFINED;
    }
    private void createChooseLinkDialog(){
         int state = mPm.getIntentVerificationStatusAsUser(mPackageName, UserHandle.myUserId());

        Log.d("blenda", "state: "+state);
        AlertDialog.Builder alertDialog_single_list_item = new AlertDialog.Builder(getActivity())
                .setSingleChoiceItems(new String[] { getString(R.string.app_link_open_always),
                        getString(R.string.app_link_open_ask), getString(R.string.app_link_open_never)},
                        convertStateToPosition(state), buttonOnClick)
                .setTitle(R.string.app_launch_open_domain_urls_title)
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
        return MetricsEvent.APPLICATIONS_APP_LAUNCH;
    }
}
