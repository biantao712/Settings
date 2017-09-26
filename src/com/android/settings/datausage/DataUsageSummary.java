/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.INetworkStatsSession;
import android.net.NetworkPolicy;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.AsusTelephonyUtils;
import com.android.settings.R;
import com.android.settings.SummaryPreference;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.util.VerizonHelpUtils;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;

import java.util.ArrayList;
import java.util.List;

import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_WIFI;
// +++ ckenken @ 20170111 VZ_REQ_UI_15706
import static android.net.NetworkPolicy.WARNING_DISABLED;
// --- ckenken @ 20170111 VZ_REQ_UI_15706

// +++ ckenken @ 20170120
import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.TrafficStats.GB_IN_BYTES;
import static android.net.TrafficStats.MB_IN_BYTES;
// --- ckenken @ 20170120

public class DataUsageSummary extends DataUsageBase implements Indexable, DataUsageEditController,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "DataUsageSummary";
    static final boolean LOGD = false;

    public static final boolean TEST_RADIOS = false;
    public static final String TEST_RADIOS_PROP = "test.radios";

    private static final String KEY_STATUS_HEADER = "status_header";
    private static final String KEY_LIMIT_SUMMARY = "limit_summary";
    private static final String KEY_RESTRICT_BACKGROUND = "restrict_background";
    // +++ ckenken (ChiaHsiang_Kuo) @ 20161028 TT-892474 Remove data usage enable switch which is not current data sim
    private static final String KEY_DATA_USAGE_ENABLE = "data_usage_enable";
    // --- ckenken (ChiaHsiang_Kuo) @ 20161028 TT-892474 Remove data usage enable switch which is not current data sim
    // +++ ckenken (ChiaHsiang_Kuo) @ 20161110 VZ_REQ_LTEPCO_34854 hide data saver preference if PCO == 3
    private static final String KEY_DATA_USAGE_CATEGORY = "data_usage_category";
    // --- ckenken (ChiaHsiang_Kuo) @ 20161110 VZ_REQ_LTEPCO_34854 hide data saver preference if PCO == 3
    // +++ ckenken (ChiaHsiang_Kuo) @ 20170112 Verizon Requirement
    private static final String KEY_CELLULAR_DATA_USAGE = "cellular_data_usage";
    // +++ ckenken (ChiaHsiang_Kuo) @ 20170112 Verizon Requirement

    // +++ ckenken (ChiaHsiang_Kuo) @ 20170120
    private static final String KEY_SET_DATA_LIMIT = "set_data_limit";
    private static final String KEY_SET_DATA_WARNING = "set_data_warning";
    // --- ckenken (ChiaHsiang_Kuo) @ 20170120

    private static final String MOBILEMANAGER_PACKAGE = "com.asus.cnmobilemanager";
    private static final String DATAUSAGE_CLASS = "com.asus.cnmobilemanager.net.DataUsageActivity";
    private static final String EXTERNAL_CALL = "external_call";


    private DataUsageController mDataUsageController;
    private DataUsageInfoController mDataInfoController;
    private SummaryPreference mSummaryPreference;
    private Preference mLimitPreference;
    private NetworkTemplate mDefaultTemplate;
    private int mDataUsageTemplate;
    // +++ AMAX @ 20170119 7.1.1 Porting
    private PreferenceCategory mDataUsageCategory;
    private Preference mDataSaverPreference;

    private MenuItem mMenuCellularNetworks;
    // --- AMAX @ 20170119 7.1.1 Porting

    @Override
    protected int getHelpResource() {
        return R.string.help_url_data_usage;
    }

    // +++ ckenken (ChiaHsiang_Kuo) @ 20170120
    private SwitchPreference mEnableDataLimit;
    private SwitchPreference mEnableDataWarning;
    // --- ckenken (ChiaHsiang_Kuo) @ 20170120

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        if(!hasManagedProfile() && isCnMobileManagerExist()){
            Intent intent = new Intent();
            intent.putExtra(EXTERNAL_CALL, true);
            intent.setClassName(MOBILEMANAGER_PACKAGE, DATAUSAGE_CLASS);
            startActivity(intent);
            finish();
        }

        boolean hasMobileData = hasMobileData(getContext());
        mDataUsageController = new DataUsageController(getContext());
        mDataInfoController = new DataUsageInfoController();
        addPreferencesFromResource(R.xml.data_usage);

        int defaultSubId = getDefaultSubscriptionId(getContext());
        if (defaultSubId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            hasMobileData = false;
        }
        mDefaultTemplate = getDefaultTemplate(getContext(), defaultSubId);
        mSummaryPreference = (SummaryPreference) findPreference(KEY_STATUS_HEADER);

        if (!hasMobileData || !isAdmin()) {
            removePreference(KEY_RESTRICT_BACKGROUND);
        }
        if (hasMobileData) {
            mLimitPreference = findPreference(KEY_LIMIT_SUMMARY);
            List<SubscriptionInfo> subscriptions =
                    services.mSubscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptions == null || subscriptions.size() == 0) {
                addMobileSection(defaultSubId);
            }
            for (int i = 0; subscriptions != null && i < subscriptions.size(); i++) {
                addMobileSection(subscriptions.get(i).getSubscriptionId());
            }
            mSummaryPreference.setSelectable(true);
        } else {
            removePreference(KEY_LIMIT_SUMMARY);
            mSummaryPreference.setSelectable(false);
        }
        boolean hasWifiRadio = hasWifiRadio(getContext());
        if (hasWifiRadio) {
            addWifiSection();
        }
        if (hasEthernet(getContext())) {
            addEthernetSection();
        }
        // +++ ckenken (ChiaHsiang_Kuo) @ 20170112 Verizon requirement
        mDataUsageTemplate = hasMobileData ? (AsusTelephonyUtils.isVerizon() ? R.string.vzw_cell_data_template : R.string.cell_data_template)
                : hasWifiRadio ? R.string.wifi_data_template
                : R.string.ethernet_data_template;
        // --- ckenken (ChiaHsiang_Kuo) @ 20170112 Verizon requirement

        mSummaryPreference = (SummaryPreference) findPreference(KEY_STATUS_HEADER);

        // +++ AMAX @ 20170119 7.1.1 Porting
        mDataUsageCategory = (PreferenceCategory) findPreference(KEY_DATA_USAGE_CATEGORY);
        mDataSaverPreference = findPreference(KEY_RESTRICT_BACKGROUND);
        // --- AMAX @ 20170119 7.1.1 Porting

        // +++ ckenken (ChiaHsiang_Kuo) @ 20170120
        mEnableDataLimit = (SwitchPreference) findPreference(KEY_SET_DATA_LIMIT);
        mEnableDataLimit.setOnPreferenceChangeListener(this);
        mEnableDataLimit.setTitle(R.string.vzw_set_data_limit);
        mEnableDataWarning = (SwitchPreference) findPreference(KEY_SET_DATA_WARNING);
        mEnableDataWarning.setOnPreferenceChangeListener(this);
        mEnableDataWarning.setTitle(R.string.vzw_data_usage_mobile_data_warning_2017);
        if (!AsusTelephonyUtils.isVerizon()) {
            mEnableDataWarning.setVisible(false);
            mEnableDataLimit.setVisible(false);
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20170120
    }

    private boolean isCnMobileManagerExist(){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(MOBILEMANAGER_PACKAGE, DATAUSAGE_CLASS));
        PackageManager pm = getActivity().getPackageManager();
        boolean appExist = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null;
        return appExist;
    }

    //cts verifier
    // Looking for a managed profile
    private boolean hasManagedProfile(){
        UserManager um = (UserManager) getContext().getSystemService(Context.USER_SERVICE);
        UserHandle managedProfile = Utils.getManagedProfile(um);
        if (managedProfile != null) {
            Log.d(TAG, "There is managed profile");
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    	/* remove options menu
    	 * tim++
        if (UserManager.get(getContext()).isAdminUser()) {
            inflater.inflate(R.menu.data_usage, menu);
        }
        // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10667
        MenuItem verizonHelpItem = menu.findItem(R.id.vzw_data_usage_menu_verizon_help);
        if(!VerizonHelpUtils.isVerizonMachine() && verizonHelpItem != null){
	        verizonHelpItem.setVisible(false);
        }
	    // ---
        // +++ ckenken (ChiaHsiang_Kuo) @ 20161116 TT-906661 check if is WifiOnly device, hide the cellular network menu
        final Context context = getContext();
        if (context != null) {
            final boolean isWifiOnly = isWifiOnly(context);
            Log.d(TAG, "isWifiOnly = " + isWifiOnly);
            if (menu != null) {
                mMenuCellularNetworks = menu.findItem(R.id.data_usage_menu_cellular_networks);
                mMenuCellularNetworks.setVisible(!isWifiOnly);
                if (AsusTelephonyUtils.isVerizon()) {
                    mMenuCellularNetworks.setTitle(R.string.vzw_data_usage_menu_cellular_networks);
                }
            }
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20161116 TT-906661 check if is WifiOnly device, hide the cellular network menu
        */
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.data_usage_menu_cellular_networks: {
                final Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setComponent(new ComponentName("com.android.phone",
                        "com.android.phone.MobileNetworkSettings"));
                startActivity(intent);
                return true;
            }
            // +++ ShawnMC_Liu@2017/01/12: Verizon VZ_REQ_DEVHELP_10667
            case R.id.vzw_data_usage_menu_verizon_help: {
		        VerizonHelpUtils.launchVzWHelp(getActivity(), VerizonHelpUtils.SCREEN_DATA);
                return true;
	        }
	        // ---
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == findPreference(KEY_STATUS_HEADER)) {
            BillingCycleSettings.BytesEditorFragment.show(this, false);
            return false;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void addMobileSection(int subId) {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_cellular);
        category.setTemplate(getNetworkTemplate(subId), subId, services);
        category.pushTemplates(services);

        // +++ ckenken (ChiaHsiang_Kuo) @ 20161028 TT-892474 update category title and remove data usage enable switch which is not current data sim
        category.updateTitles();

        CellDataPreference cellEnablePreference = (CellDataPreference) category.findPreference(KEY_DATA_USAGE_ENABLE + String.valueOf(subId));
        DataUsagePreference dataUsagePreference = (DataUsagePreference) category.findPreference(KEY_CELLULAR_DATA_USAGE);

        if (null != dataUsagePreference) {
            dataUsagePreference.updateTitle(true);
        }

        if (null != cellEnablePreference) {
            android.util.Log.d(TAG, "CellDataPreference key = " + cellEnablePreference.getKey());
            // +++ ckenken (ChiaHsiang_Kuo) @ 20161101 VZ_REQ_UI_15706 change preference title
            if (AsusTelephonyUtils.isVerizon()) {
                cellEnablePreference.setTitle(R.string.vzw_data_usage_enable_mobile_data);
            }
            // +++ ckenken (ChiaHsiang_Kuo) @ 20161101 VZ_REQ_UI_15706 change preference title

            int slotId = SubscriptionManager.getSlotId(subId);
            android.util.Log.d(TAG, "subId = " + subId + ", slotId = " + slotId);
            if (slotId != getMobileSlot()) {
                cellEnablePreference.setVisible(false);
            }
        } else {
            android.util.Log.d(TAG, "cellEnablePreference is null!");
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20161028 TT-892474 update category title and remove data usage enable switch which is not current data sim
    }

    private void addWifiSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_wifi);
        category.setTemplate(NetworkTemplate.buildTemplateWifiWildcard(), 0, services);
    }

    private void addEthernetSection() {
        TemplatePreferenceCategory category = (TemplatePreferenceCategory)
                inflatePreferences(R.xml.data_usage_ethernet);
        category.setTemplate(NetworkTemplate.buildTemplateEthernet(), 0, services);
    }

    private Preference inflatePreferences(int resId) {
        PreferenceScreen rootPreferences = getPreferenceManager().inflateFromResource(
                getPrefContext(), resId, null);
        Preference pref = rootPreferences.getPreference(0);
        rootPreferences.removeAll();

        PreferenceScreen screen = getPreferenceScreen();
        pref.setOrder(screen.getPreferenceCount());
        screen.addPreference(pref);

        return pref;
    }

    private NetworkTemplate getNetworkTemplate(int subscriptionId) {
        NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                services.mTelephonyManager.getSubscriberId(subscriptionId));
        return NetworkTemplate.normalize(mobileAll,
                services.mTelephonyManager.getMergedSubscriberIds());
    }

    @Override
    public void onResume() {
        super.onResume();
        // +++ ckenken (ChiaHsiang_Kuo) @ 20161110 VZ_REQ_LTEPCO_34854 hide data saver preference if PCO == 3
        if (AsusTelephonyUtils.isVerizon()) {
            int iPco = SystemProperties.getInt("persist.radio.asus.verizon_pco", -1);
            if ((iPco == 2 || iPco == 3) && null != findPreference(KEY_RESTRICT_BACKGROUND)) {
                mDataUsageCategory.removePreference(mDataSaverPreference);
            } else if (iPco != 3 && null == findPreference(KEY_RESTRICT_BACKGROUND)) {
                mDataUsageCategory.addPreference(mDataSaverPreference);
            }
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20161110 VZ_REQ_LTEPCO_34854 hide data saver preference if PCO == 3
        // +++ ckenken (ChiaHsiang_Kuo) @ 20170120
        if (AsusTelephonyUtils.isVerizon()) {
            updateStateForVerizon();
            updatePrefs();
        } else {
            updateState();
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20170120
    }

    private static void verySmallSpanExcept(SpannableString s, CharSequence exception) {
        final float SIZE = 0.8f * 0.8f;
        final int FLAGS = Spannable.SPAN_INCLUSIVE_INCLUSIVE;
        final int exceptionStart = TextUtils.indexOf(s, exception);
        if (exceptionStart == -1) {
           s.setSpan(new RelativeSizeSpan(SIZE), 0, s.length(), FLAGS);
        } else {
            if (exceptionStart > 0) {
                s.setSpan(new RelativeSizeSpan(SIZE), 0, exceptionStart, FLAGS);
            }
            final int exceptionEnd = exceptionStart + exception.length();
            if (exceptionEnd < s.length()) {
                s.setSpan(new RelativeSizeSpan(SIZE), exceptionEnd, s.length(), FLAGS);
            }
        }
    }

    private static CharSequence formatTitle(Context context, String template, long usageLevel) {
        final SpannableString amountTemplate = new SpannableString(
                context.getString(com.android.internal.R.string.fileSizeSuffix)
                .replace("%1$s", "^1").replace("%2$s", "^2"));
        verySmallSpanExcept(amountTemplate, "^1");
        final Formatter.BytesResult usedResult = Formatter.formatBytes(context.getResources(),
                usageLevel, Formatter.FLAG_SHORTER);
        final CharSequence formattedUsage = TextUtils.expandTemplate(amountTemplate,
                usedResult.value, usedResult.units);

        final SpannableString fullTemplate = new SpannableString(template.replace("%1$s", "^1"));
        verySmallSpanExcept(fullTemplate, "^1");
        return TextUtils.expandTemplate(fullTemplate,
                BidiFormatter.getInstance().unicodeWrap(formattedUsage));
    }

    private void updateState() {
        DataUsageController.DataUsageInfo info = mDataUsageController.getDataUsageInfo(
                mDefaultTemplate);

        // +++ ckenken (ChiaHsiang_Kuo) @ 20170125 Add log for warning and limit bytes from DataUsageInfo, compare to PolicyManager
        if (info == null) {
            Log.d(TAG, "updateState(): DataUsageInfo is null, return");
            return;
        }
        Log.d(TAG, "updateState(): warning = " + info.warningLevel);
        Log.d(TAG, "updateState(): limiting = " + info.limitLevel);
        // --- ckenken (ChiaHsiang_Kuo) @ 20170125 Add log for warning and limit bytes from DataUsageInfo, compare to PolicyManager

        Context context = getContext();

        mDataInfoController.updateDataLimit(info,
                services.mPolicyEditor.getPolicy(mDefaultTemplate));

        if (mSummaryPreference != null) {
            mSummaryPreference.setTitle(
                    formatTitle(context, getString(mDataUsageTemplate), info.usageLevel));
            long limit = mDataInfoController.getSummaryLimit(info);
            mSummaryPreference.setSummary(info.period);

            if (limit <= 0) {
                mSummaryPreference.setChartEnabled(false);
            } else {
                // +++ ckenken @ 20170111 VZ_REQ_UI_15706
                if (AsusTelephonyUtils.isVerizon()) {
                    if (info.warningLevel > 0) {
                        if (info.warningLevel >= limit) {
                            mSummaryPreference.setWarningLineLocationRatio(1.0);
                        } else {
                            mSummaryPreference.setWarningLineLocationRatio((double) info.warningLevel / (double) limit);
                        }
                    } else {
                        mSummaryPreference.setWarningLineLocationRatio(WARNING_DISABLED);
                    }
                }
                // --- ckenken @ 20170111 VZ_REQ_UI_15706
                mSummaryPreference.setChartEnabled(true);
                mSummaryPreference.setLabels(Formatter.formatFileSize(context, 0),
                        Formatter.formatFileSize(context, limit));
                mSummaryPreference.setRatios(info.usageLevel / (float) limit, 0,
                        (limit - info.usageLevel) / (float) limit);
            }
        }
        if (mLimitPreference != null && (info.warningLevel > 0 || info.limitLevel > 0)) {
            String warning = Formatter.formatFileSize(context, info.warningLevel);
            String limit = Formatter.formatFileSize(context, info.limitLevel);
            // +++ ckenken (ChiaHsiang_Kuo) @ 20170111 VZ_REQ_UI_15706
            if (AsusTelephonyUtils.isVerizon()) {
                mLimitPreference.setVisible(true);
                StringBuilder sb = new StringBuilder();
                boolean hasWarning = false;
                if (info.warningLevel > 0) {
                    sb.append(getString(R.string.vzw_cell_warning, warning));
                    hasWarning = true;
                }
                if (info.limitLevel > 0) {
                    if (hasWarning) {
                        sb.append("\n");
                    }
                    sb.append(getString(R.string.vzw_cell_limit, limit));
                }
                mLimitPreference.setSummary(sb.toString());
                if (sb.toString().length() == 0) {
                    mLimitPreference.setVisible(false);
                }
            } else {
                mLimitPreference.setSummary(getString(info.limitLevel <= 0 ? R.string.cell_warning_only
                        : R.string.cell_warning_and_limit, warning, limit));
            }
            // --- ckenken (ChiaHsiang_Kuo) @ 20170111 VZ_REQ_UI_15706
        } else if (mLimitPreference != null) {
            mLimitPreference.setSummary(null);
        }

        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 1; i < screen.getPreferenceCount(); i++) {
            ((TemplatePreferenceCategory) screen.getPreference(i)).pushTemplates(services);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DATA_USAGE_SUMMARY;
    }

    @Override
    public NetworkPolicyEditor getNetworkPolicyEditor() {
        return services.mPolicyEditor;
    }

    @Override
    public NetworkTemplate getNetworkTemplate() {
        return mDefaultTemplate;
    }

    @Override
    public void updateDataUsage() {
        updateState();
    }

    /**
     * Test if device has an ethernet network connection.
     */
    public boolean hasEthernet(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("ethernet");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final boolean hasEthernet = conn.isNetworkSupported(TYPE_ETHERNET);

        final long ethernetBytes;
        try {
            INetworkStatsSession statsSession = services.mStatsService.openSession();
            if (statsSession != null && NetworkTemplate.buildTemplateEthernet() != null) {
                //Ken1_yu, getSummaryForNetwork may be null
                NetworkStats network = statsSession.getSummaryForNetwork(
                        NetworkTemplate.buildTemplateEthernet(),
                        Long.MIN_VALUE, Long.MAX_VALUE);
                ethernetBytes = network != null ? network.getTotalBytes() : 0;
                TrafficStats.closeQuietly(statsSession);
            } else {
                ethernetBytes = 0;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        // only show ethernet when both hardware present and traffic has occurred
        return hasEthernet && ethernetBytes > 0;
    }

    public static boolean hasMobileData(Context context) {
        return ConnectivityManager.from(context).isNetworkSupported(
                ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Test if device has a Wi-Fi data radio.
     */
    public static boolean hasWifiRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("wifi");
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        return conn.isNetworkSupported(TYPE_WIFI);
    }

    public static int getDefaultSubscriptionId(Context context) {
        SubscriptionManager subManager = SubscriptionManager.from(context);
        if (subManager == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        SubscriptionInfo subscriptionInfo = subManager.getDefaultDataSubscriptionInfo();
        if (subscriptionInfo == null) {
            List<SubscriptionInfo> list = subManager.getAllSubscriptionInfoList();
            if (list.size() == 0) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
            subscriptionInfo = list.get(0);
        }
        return subscriptionInfo.getSubscriptionId();
    }

    public static NetworkTemplate getDefaultTemplate(Context context, int defaultSubId) {
        if (hasMobileData(context) && defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                    telephonyManager.getSubscriberId(defaultSubId));
            return NetworkTemplate.normalize(mobileAll,
                    telephonyManager.getMergedSubscriberIds());
        } else if (hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        } else {
            return NetworkTemplate.buildTemplateEthernet();
        }
    }

    private static class SummaryProvider
            implements SummaryLoader.SummaryProvider {

        private final Activity mActivity;
        private final SummaryLoader mSummaryLoader;
        private final DataUsageController mDataController;

        public SummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            mActivity = activity;
            mSummaryLoader = summaryLoader;
            mDataController = new DataUsageController(activity);
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                DataUsageController.DataUsageInfo info = mDataController.getDataUsageInfo();
                String used;
                if (info == null) {
                    used = Formatter.formatFileSize(mActivity, 0);
                } else if (info.limitLevel <= 0) {
                    used = Formatter.formatFileSize(mActivity, info.usageLevel);
                } else {
                    used = Utils.formatPercentage(info.usageLevel, info.limitLevel);
                }
                mSummaryLoader.setSummary(this,
                        mActivity.getString(R.string.data_usage_summary_format, used));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    /**
     * For search
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                    boolean enabled) {
                ArrayList<SearchIndexableResource> resources = new ArrayList<>();
                SearchIndexableResource resource = new SearchIndexableResource(context);
                resource.xmlResId = R.xml.data_usage;
                resources.add(resource);

                if (hasMobileData(context)) {
                    resource = new SearchIndexableResource(context);
                    resource.xmlResId = R.xml.data_usage_cellular;
                    resources.add(resource);
                }
                if (hasWifiRadio(context)) {
                    resource = new SearchIndexableResource(context);
                    resource.xmlResId = R.xml.data_usage_wifi;
                    resources.add(resource);
                }
                return resources;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                ArrayList<String> keys = new ArrayList<>();
                boolean hasMobileData = ConnectivityManager.from(context).isNetworkSupported(
                        ConnectivityManager.TYPE_MOBILE);

                if (hasMobileData) {
                    keys.add(KEY_RESTRICT_BACKGROUND);
                }

                return keys;
            }
        };
// +++ AMAX @ 20170119 7.1.1 Porting
    // +++ ckenken (ChiaHsiang_Kuo) @ 20170120
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mEnableDataLimit == preference) {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                showLimitDialog();
            } else {
                setPolicyLimitBytes(LIMIT_DISABLED);
            }
            // +++ ckenken (ChiaHsiang_Kuo) @ 20170120
            if (AsusTelephonyUtils.isVerizon()) {
                updateStateForVerizon();
                updatePrefs();
            } else {
                updateState();
            }
            // --- ckenken (ChiaHsiang_Kuo) @ 20170120
            return true;
        } else if (mEnableDataWarning == preference) {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                showAlertMeDialog();
            } else {
                setPolicyWarningBytes(WARNING_DISABLED);
            }
            // +++ ckenken (ChiaHsiang_Kuo) @ 20170120
            if (AsusTelephonyUtils.isVerizon()) {
                updateStateForVerizon();
                updatePrefs();
            } else {
                updateState();
            }
            // --- ckenken (ChiaHsiang_Kuo) @ 20170120
            return true;
        }
        return false;
    }

    private void updatePrefs() {
        NetworkPolicy policy = services.mPolicyEditor.getPolicy(mDefaultTemplate);

        if (policy != null) {
            Log.d(TAG, "updatePrefs(): warning = " + policy.warningBytes);
            Log.d(TAG, "updatePrefs(): limiting = " + policy.limitBytes);
        } else {
            Log.d(TAG, "updatePrefs(): policy == null");
        }

        if (null != policy && policy.warningBytes > WARNING_DISABLED) {
            mEnableDataWarning.setSummary(Formatter.formatFileSize(getContext(), policy.warningBytes));
            mEnableDataWarning.setChecked(true);
        } else {
            mEnableDataWarning.setSummary(getString(R.string.disabled));
            mEnableDataWarning.setChecked(false);
        }

        if (null != policy && policy.limitBytes > LIMIT_DISABLED) {
            mEnableDataLimit.setSummary(Formatter.formatFileSize(getContext(), policy.limitBytes));
            mEnableDataLimit.setChecked(true);
        } else {
            mEnableDataLimit.setSummary(getString(R.string.disabled));
            mEnableDataLimit.setChecked(false);
        }
    }

    private String formatText(float v) {
        v = Math.round(v * 100) / 100f;
        return String.valueOf(v);
    }

    private void showAlertMeDialog() {
        Context context = getContext();
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        final Resources res = context.getResources();
        android.telephony.TelephonyManager telephonyManager = (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        boolean isVoiceCapable = telephonyManager.isVoiceCapable();
        CharSequence message = res.getString(R.string.vzw_data_usage_warning_dialog_message_2016_Nov,
                (isVoiceCapable) ? res.getString(R.string.vzw_phone) : res.getString(R.string.vzw_tablet));
        final View confirmAlertDialogLayoutView = getActivity().getLayoutInflater().inflate(R.layout.confirm_alert_dialog_layout, null);
        final TextView textView = (TextView) confirmAlertDialogLayoutView.findViewById(R.id.confirmAlertTextView);
        final EditText bytesEditText = (EditText) confirmAlertDialogLayoutView.findViewById(R.id.alert_data_bytes);
        final Spinner confirmAlertSpinner = (Spinner) confirmAlertDialogLayoutView.findViewById(R.id.alert_data_size_spinner);
        textView.setText(message);
        setupPicker(bytesEditText, confirmAlertSpinner, false);

        builder.setView(confirmAlertDialogLayoutView);
        builder.setTitle(R.string.vzw_data_usage_warning_dialog_title);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String bytesString = bytesEditText.getText().toString();
                if (bytesString.isEmpty()) {
                    bytesString = "0";
                }
                final long bytes = (long) (Float.valueOf(bytesString) * (confirmAlertSpinner.getSelectedItemPosition() == 0 ? MB_IN_BYTES : GB_IN_BYTES));
                setPolicyWarningBytes(bytes);
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updatePrefs();
            }
        });
        builder.create().show();
    }

    private void showLimitDialog() {
        Context context = getContext();

        final View confirmLimitDialogLayoutView = getActivity().getLayoutInflater().inflate(R.layout.confirm_limit_dialog_layout, null);
        final TextView textViewTop = (TextView) confirmLimitDialogLayoutView.findViewById(R.id.confirmLimitTextView_top);
        final EditText bytesEditText = (EditText) confirmLimitDialogLayoutView.findViewById(R.id.limit_data_bytes);
        final Spinner confirmAlertSpinner = (Spinner) confirmLimitDialogLayoutView.findViewById(R.id.limit_data_size_spinner);
        final TextView textViewBottom = (TextView) confirmLimitDialogLayoutView.findViewById(R.id.confirmLimitTextView_bottom);
        textViewTop.setText(R.string.vzw_data_usage_limit_dialog_message_top_2016_Nov);
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        boolean isVoiceCapable = telephonyManager.isVoiceCapable();
        textViewBottom.setText(String.format(getString(R.string.vzw_data_usage_limit_dialog_message_bottom_2016_Nov), (isVoiceCapable) ? getString(R.string.vzw_phone) : getString(R.string.vzw_tablet)));
        setupPicker(bytesEditText, confirmAlertSpinner, true);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(confirmLimitDialogLayoutView);
        builder.setTitle(R.string.data_usage_limit_dialog_title);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String bytesString = bytesEditText.getText().toString();
                if (bytesString.isEmpty()) {
                    bytesString = "0";
                }
                final long bytes = (long) (Float.valueOf(bytesString) * (confirmAlertSpinner.getSelectedItemPosition() == 0 ? MB_IN_BYTES : GB_IN_BYTES));
                setPolicyLimitBytes(bytes);
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                updatePrefs();
            }
        });
        builder.create().show();
    }

    private void setupPicker(EditText bytesPicker, Spinner type, boolean isLimit) {
        final NetworkPolicyEditor editor = this.services.mPolicyEditor;
        if (isLimit) {
            final long bytes = editor.getPolicyLimitBytes(mDefaultTemplate);
            if (bytes <= 0) {
                bytesPicker.setText(BillingCycleSettings.DEFAULT_LIMIT_LEVEL);
                type.setSelection(1);
            } else if (bytes > 1.5f * GB_IN_BYTES) {
                bytesPicker.setText(formatText(bytes / (float) GB_IN_BYTES));
                type.setSelection(1);
            } else {
                bytesPicker.setText(formatText(bytes / (float) MB_IN_BYTES));
                type.setSelection(0);
            }
        } else {
            final long bytes = editor.getPolicyWarningBytes(mDefaultTemplate);
            if (bytes <= 0) {
                bytesPicker.setText(BillingCycleSettings.DEFAULT_WARNING_LEVEL);
                type.setSelection(1);
            } else if (bytes > 1.5f * GB_IN_BYTES) {
                bytesPicker.setText(formatText(bytes / (float) GB_IN_BYTES));
                type.setSelection(1);
            } else {
                bytesPicker.setText(formatText(bytes / (float) MB_IN_BYTES));
                type.setSelection(0);
            }
        }
    }

    private void setPolicyLimitBytes(long limitBytes) {
        if (LOGD) Log.d(TAG, "setPolicyLimitBytes()");
        services.mPolicyEditor.setPolicyLimitBytes(mDefaultTemplate, limitBytes);
        if (AsusTelephonyUtils.isVerizon()) {
            updateStateForVerizon();
            updatePrefs();
        } else {
            updateState();
        }
    }

    private void setPolicyWarningBytes(long warningBytes) {
        if (LOGD) Log.d(TAG, "setPolicyWarningBytes()");
        services.mPolicyEditor.setPolicyWarningBytes(mDefaultTemplate, warningBytes);
        if (AsusTelephonyUtils.isVerizon()) {
            updateStateForVerizon();
            updatePrefs();
        } else {
            updateState();
        }
    }

    private void updateStateForVerizon() {
        DataUsageController.DataUsageInfo info = mDataUsageController.getDataUsageInfo(
                mDefaultTemplate);

        NetworkPolicy policy = services.mPolicyEditor.getPolicy(mDefaultTemplate);

        if (policy == null) {
            Log.d(TAG, "updateStateForVerizon(): policy == null! call AOSP updateState() and return");
            updateState();
            return;
        }

        Log.d(TAG, "updateStateForVerizon(): warning = " + policy.warningBytes);
        Log.d(TAG, "updateStateForVerizon(): limiting = " + policy.limitBytes);

        Context context = getContext();
        if (mSummaryPreference != null) {
            mSummaryPreference.setTitle(
                    formatTitle(context, getString(mDataUsageTemplate), info.usageLevel));
            long limit = policy.limitBytes;
            if (limit <= 0) {
                limit = policy.warningBytes;
            }
            if (info.usageLevel > limit) {
                limit = info.usageLevel;
            }
            // +++ ckenken
            if (AsusTelephonyUtils.isVerizon()) {
                if (policy.warningBytes > 0) {
                    if (policy.warningBytes >= limit) {
                        mSummaryPreference.setWarningLineLocationRatio(1.0);
                    } else {
                        mSummaryPreference.setWarningLineLocationRatio((double) policy.warningBytes / (double) limit);
                    }
                } else {
                    mSummaryPreference.setWarningLineLocationRatio(WARNING_DISABLED);
                }
            }
            // --- ckenken

            mSummaryPreference.setSummary(info.period);
            mSummaryPreference.setLabels(Formatter.formatFileSize(context, 0),
                    Formatter.formatFileSize(context, limit));
            mSummaryPreference.setRatios(info.usageLevel / (float) limit, 0,
                    (limit - policy.warningBytes) / (float) limit);
        }
        if (mLimitPreference != null) {
            String warning = Formatter.formatFileSize(context, policy.warningBytes);
            String limit = Formatter.formatFileSize(context, policy.limitBytes);

            if (AsusTelephonyUtils.isVerizon()) {
                mLimitPreference.setVisible(true);
                StringBuilder sb = new StringBuilder();
                boolean hasWarning = false;
                if (policy.warningBytes > 0) {
                    sb.append(getString(R.string.vzw_cell_warning, warning));
                    hasWarning = true;
                }
                if (policy.limitBytes > 0) {
                    if (hasWarning) {
                        sb.append("\n");
                    }
                    sb.append(getString(R.string.vzw_cell_limit, limit));
                }
                mLimitPreference.setSummary(sb.toString());
                if (sb.toString().length() <= 0) {
                    mLimitPreference.setVisible(false);
                }
            } else {
                mLimitPreference.setSummary(getString(policy.limitBytes <= 0 ? R.string.cell_warning_only
                        : R.string.cell_warning_and_limit, warning, limit));
            }
        }

        PreferenceScreen screen = getPreferenceScreen();
        for (int i = 1; i < screen.getPreferenceCount(); i++) {
            ((TemplatePreferenceCategory) screen.getPreference(i)).pushTemplates(services);
        }
    }
    // --- ckenken (ChiaHsiang_Kuo) @ 20170120

    // +++ AMAX ckenken (ChiaHsiang_Kuo) @ 20161028 TT-892474 get current data sim slot id
    public static int getMobileSlot() {
        int ret = SystemProperties.getInt("persist.asus.mobile_slot", 0);
        android.util.Log.d(TAG, "getMobileSlot(): slotId = " + ret);
        return ret;
    }
    // --- AMAX ckenken (ChiaHsiang_Kuo) @ 20161028 TT-892474 get current data sim slot id

    // +++ ckenken (ChiaHsiang_Kuo) @ 20161116 TT-906661 check if is WifiOnly device
    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }
    // --- ckenken (ChiaHsiang_Kuo) @ 20161116 TT-906661 check if is WifiOnly device
// --- AMAX @ 20170119 7.1.1 Porting
}
