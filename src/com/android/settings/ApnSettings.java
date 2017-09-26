/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.asus.cncommonres.AsusButtonBar;
import java.util.ArrayList;
import java.util.List;

public class ApnSettings extends RestrictedSettingsFragment implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";

    public static final String APN_ID = "apn_id";
    public static final String SUB_ID = "sub_id";
    public static final String MVNO_TYPE = "mvno_type";
    public static final String MVNO_MATCH_DATA = "mvno_match_data";
    // +++ AMAX @ 20170119 7.1.1 Porting
    private static final String APN_NAME_DM = "CMCC DM";
    // --- AMAX @ 20170119 7.1.1 Porting

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int MVNO_TYPE_INDEX = 4;
    private static final int MVNO_MATCH_DATA_INDEX = 5;
    // +++ AMAX @ 20170119 7.1.1 Porting
    private static final int RO_INDEX = 6;
    // --- AMAX @ 20170119 7.1.1 Porting

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static boolean mRestoreDefaultApnMode;

    private UserManager mUserManager;
    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private SubscriptionInfo mSubscriptionInfo;
    private UiccController mUiccController;
    private String mMvnoType;
    private String mMvnoMatchData;

    private String mSelectedKey;

    private IntentFilter mMobileStateFilter;

    private boolean mUnavailable;

    private boolean mHideImsApn;
    private boolean mAllowAddingApns;
    // +++ AMAX @ 20170119 7.1.1 Porting
    private boolean mApnSettingsHidden;
    // --- AMAX @ 20170119 7.1.1 Porting

    public ApnSettings() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }
// +++ AMAX
    private boolean mUseNvOperatorForEhrpd = SystemProperties.getBoolean(
            "persist.radio.use_nv_for_ehrpd", false);
    // +++ allen_chu@20140429: <CDR-CDS-500> Create a New Custom APN
    private static List<String> mApnName = new ArrayList<String>();
    private static List<String> mPreApnName = new ArrayList<String>();
    private String mNewApnKey;
    private boolean mRestoreToDefault = false;
    // --- allen_chu@20140429
    private static String CTCC_3GPP_NUMERIC = "46011";
    private static String CTCC_3GPP2_NUMERIC = "46003";
// --- AMAX

    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        Log.d(TAG, "mMobileStateReceiver");  // +++ AMAX @ 20170119 7.1.1 Porting
                        fillList();
                    } else {
                        showDialog(DIALOG_RESTORE_DEFAULTAPN);
                    }
                    break;
                }
            }
        }
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        final int subId = activity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);

        setIfOnlyAvailableForAdmins(true);

        mSubscriptionInfo = SubscriptionManager.from(activity).getActiveSubscriptionInfo(subId);
        mUiccController = UiccController.getInstance();

        CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfig();
        // +++ rock_huang@20160328: TT-768385 Use subId to get ConfigManager
        try {
            PersistableBundle tempPB = configManager.getConfigForSubId(mSubscriptionInfo.getSubscriptionId());
            Log.d(TAG, "onCreate tempPB = " + tempPB);
            if (tempPB != null) {
                b = tempPB;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // --- rock_huang@20160328: TT-768385 Use subId to get ConfigManager
        mHideImsApn = b.getBoolean(CarrierConfigManager.KEY_HIDE_IMS_APN_BOOL);
        mAllowAddingApns = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL);
        mUserManager = UserManager.get(activity);
        // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
        mAllowAddingApns = (!AsusTelephonyUtils.isVerizon() && mAllowAddingApns) ||
                (AsusTelephonyUtils.isVerizon() && !AsusTelephonyUtils.isVerizonSim() && mAllowAddingApns) ||
                (SystemProperties.getInt("persist.radio.for_verizon_test", 0) == 1);
        // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getActivity().setTheme(R.style.Theme_CNAsusRes);
        super.onActivityCreated(savedInstanceState);

        getEmptyTextView().setText(R.string.apn_settings_not_available);
        mUnavailable = isUiRestricted();
        setHasOptionsMenu(!mUnavailable);
        if (mUnavailable) {
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
            getPreferenceScreen().removeAll();
            return;
        }

        addPreferencesFromResource(R.xml.apn_settings);

        com.asus.cncommonres.AsusButtonBar buttonBar = ((SettingsActivity)getActivity()).getButtonBar();
        if(buttonBar != null) {
            buttonBar.setVisibility(View.VISIBLE);
            buttonBar.addButton(1, R.drawable.cn_add_account_bt, getResources().getString(R.string.menu_new));
            buttonBar.getButton(1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addNewApn();;
                }
            });
            buttonBar.addButton(2, R.drawable.asusres_btn_restore_to_default, getResources().getString(R.string.menu_restore));
            buttonBar.getButton(2).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    restoreDefaultApn();
                }
            });
        }
    }

    // +++ AMAX @ 20170119 7.1.1 Porting
    @Override
    public void onStop() {
        super.onStop();
        mApnSettingsHidden = true;
    }
    // --- AMAX @ 20170119 7.1.1 Porting

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            return;
        }

        getActivity().registerReceiver(mMobileStateReceiver, mMobileStateFilter);

        if (!mRestoreDefaultApnMode) {
            fillList();
        }
        // +++ AMAX @ 20170119 7.1.1 Porting
        mApnSettingsHidden = false;
        // --- AMAX @ 20170119 7.1.1 Porting
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mUnavailable) {
            return;
        }

        getActivity().unregisterReceiver(mMobileStateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }

        // +++ allen_chu@20141202: clear data
        if (isATT()) {
            if(mApnName != null) {
                mApnName.clear();
            }
            if(mPreApnName != null) {
                mPreApnName.clear();
            }
        }
        // --- allen_chu@20141202
    }

    @Override
    public EnforcedAdmin getRestrictionEnforcedAdmin() {
        final UserHandle user = UserHandle.of(mUserManager.getUserHandle());
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, user)
                && !mUserManager.hasBaseUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                        user)) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        return null;
    }

    private void fillList() {
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // +++ AMAX @ 20170119 7.1.1 Porting
        // +++ rock_huang@20160518: For ril request
        String mccmnc = mSubscriptionInfo == null ? ""
            : tm.getSimOperator(mSubscriptionInfo.getSubscriptionId());
        Log.d(TAG, "mccmnc = " + mccmnc);
        if (SystemProperties.getInt("ro.asus.is_verizon_device", 0) == 1 && SystemProperties.getInt("ril.is_verizon_sim", 0) == 1) {
            mccmnc = "311480";
        }
        String sRilMccmnc = SystemProperties.get("gsm.radio.asus.ril.mccmnc", "");
        if (sRilMccmnc != null && sRilMccmnc.length() > 0) {
            mccmnc = sRilMccmnc;
        }
        Log.d(TAG, "final mccmnc = " + mccmnc);
        // --- rock_huang@20160518: For ril request
        // --- AMAX @ 20170119 7.1.1 Porting
        StringBuilder where = new StringBuilder("numeric=\"" + mccmnc +
                "\" AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL)) AND user_visible!=0");
        // +++ AMAX @ 20170119 7.1.1 Porting
        if (SystemProperties.getBoolean("persist.sys.hideapn", true)) {
            Log.d(TAG, "hiden apn feature enable.");
            // remove the filtered items, no need to show in UI

            if(false/*getResources().getBoolean(R.bool.config_hide_ims_apns)*/){
                mHideImsApn = true;
            }

            // Filer fota and dm for specail carrier
            if (true/*getResources().getBoolean(R.bool.config_hide_dm_enabled)*/) {
                String[] sHidedmPlmnList = new String[] {"46000", "46002", "46003", "46007"};
                for (String plmn : sHidedmPlmnList/*getResources().getStringArray(R.array.hidedm_plmn_list)*/) {
                    if (plmn.equals(mccmnc)) {
                        where.append(" and name <>\"" + APN_NAME_DM + "\"");
                        break;
                    }
                }
            }

            if (false/*getResources().getBoolean(R.bool.config_hidesupl_enable)*/) {
                boolean needHideSupl = false;
                String[] sHidesuplPlmnList = new String[] {"46001", "46003", "46011", "46012", "45502", "45507"};
                for (String plmn : sHidesuplPlmnList/*getResources().getStringArray(R.array.hidesupl_plmn_list)*/) {
                    if (plmn.equals(mccmnc)) {
                        needHideSupl = true;
                        break;
                    }
                }

                if (needHideSupl) {
                    where.append(" and type <>\"" + PhoneConstants.APN_TYPE_SUPL + "\"");
                }
            }

            // Hide mms if config is true
            if (false/*getResources().getBoolean(R.bool.config_hide_mms_enable)*/) {
                  where.append( " and type <>\"" + PhoneConstants.APN_TYPE_MMS + "\"");
            }
        }

        if(false/*getResources().getBoolean(R.bool.config_regional_hide_ims_and_dun_apns)*/){
            where.append(" AND type <>\"" + PhoneConstants.APN_TYPE_DUN + "\"");
            where.append(" AND type <>\"" + PhoneConstants.APN_TYPE_IMS + "\"");
        }
        // --- AMAX @ 20170119 7.1.1 Porting
        if (mHideImsApn) {
            where.append(" AND NOT (type='ims')");
        }

        // +++ AMAX @ 20170119 7.1.1 Porting
        Log.d(TAG, "where---" + where);

        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[] {
                "_id", "name", "apn", "type", "mvno_type", "mvno_match_data", "read_only"},
                where.toString(), null, Telephony.Carriers.DEFAULT_SORT_ORDER);

        Log.d(TAG, "cursor1 = " + cursor);
        if (cursor == null) {
            cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[] {
                    "_id", "name", "apn", "type", "mvno_type", "mvno_match_data"}, where.toString(),
                    null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        }
        Log.d(TAG, "cursor2 = " + cursor);
        // --- AMAX @ 20170119 7.1.1 Porting
        if (cursor != null) {
            IccRecords r = null;
            if (mUiccController != null && mSubscriptionInfo != null) {
                r = mUiccController.getIccRecords(SubscriptionManager.getPhoneId(
                        mSubscriptionInfo.getSubscriptionId()), UiccController.APP_FAM_3GPP);
            }
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();

            ArrayList<ApnPreference> mnoApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mvnoApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mnoMmsApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mvnoMmsApnList = new ArrayList<ApnPreference>();

            mSelectedKey = getSelectedApnKey();
            // +++ Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
            if (mSelectedKey == null && !mRestoreDefaultApnMode){
                resetSelectedApnKey();
            }
            // --- Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                String apn = cursor.getString(APN_INDEX);
                String key = cursor.getString(ID_INDEX);
                String type = cursor.getString(TYPES_INDEX);
                String mvnoType = cursor.getString(MVNO_TYPE_INDEX);
                String mvnoMatchData = cursor.getString(MVNO_MATCH_DATA_INDEX);
                // +++ AMAX @ 20170119 7.1.1 Porting
                boolean readOnly = false;
                try {
                    readOnly = (cursor.getInt(RO_INDEX) == 1);
                } catch (Exception e) {
                    Log.d(TAG, "Cannot get RO_INDEX");
                }
                String localizedName = getLocalizedName(getActivity(), cursor, NAME_INDEX);
                if (!TextUtils.isEmpty(localizedName)) {
                    name = localizedName;
                }
                Log.d(TAG, "fillList(): mvnoType = " + mvnoType);
                Log.d(TAG, "fillList(): mvnoMatchData = " + mvnoMatchData);
                // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
                if (!AsusTelephonyUtils.shouldDisplayAPN(name) && SystemProperties.getInt("persist.radio.for_verizon_test", 0) != 1) {
                    Log.d(TAG, "fillList(): APN " + name + " should not be displayed");
                    cursor.moveToNext();
                    continue;
                }
                // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
                // --- AMAX @ 20170119 7.1.1 Porting

                ApnPreference pref = new ApnPreference(getPrefContext());
                // +++ AMAX @ 20170119 7.1.1 Porting
                pref.setApnReadOnly(readOnly);
                // --- AMAX @ 20170119 7.1.1 Porting
                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);

                // +++ AMAX
                boolean selectable = false;//((type == null) || !type.equals("mms"));
                Log.d(TAG, "fillList(): selectable 1 = " + selectable + ", type = " + type);
                if (!selectable && type != null) {
                    selectable = type.indexOf("default") >= 0 || type.equals("*") || type.equals("");
                }
                Log.d(TAG, "fillList(): selectable 2 = " + selectable);
                int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                        : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                if (subId >= 0) {
                    int iSlotId = mSubscriptionInfo.getSimSlotIndex();
                    Log.d(TAG, "fillList(): iSlotId = " + iSlotId + ", mobile_slot = " + SystemProperties.getInt("persist.asus.mobile_slot", 0));
                    if (SystemProperties.getInt("persist.asus.mobile_slot", 0) != iSlotId) {// Only Default Data SIM can select APN
                        pref.setSelectable(false);
                    } else {
                        pref.setSelectable(selectable);
                    }
                }
                Log.d(TAG, "fillList(): mSelectedKey = " + mSelectedKey);
                Log.d(TAG, "fillList(): key = " + key);
                Log.d(TAG, "fillList(): pref = " + pref);
                // --- AMAX
                if (selectable) {
                    if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                    }
                    // +++ rock_huang@20160824: For CTCC SPEC testing
                    if (mSelectedKey != null && (CTCC_3GPP2_NUMERIC.equals(mccmnc) || CTCC_3GPP_NUMERIC.equals(mccmnc))) {
                        int iPos = -1;
                        Cursor c = null;
                        Cursor c2 = null;
                        try {
                            iPos = Integer.parseInt(mSelectedKey);
                            Log.d(TAG, "fillList(): iPos++ = " + iPos);
                            if (iPos >= 0) {
                                Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, iPos);
                                c = getContentResolver().query(url, new String[] {Telephony.Carriers.NAME, Telephony.Carriers.APN, Telephony.Carriers.USER, Telephony.Carriers.PASSWORD, Telephony.Carriers.MCC, Telephony.Carriers.MNC, Telephony.Carriers.NUMERIC}, null, null, null);
                                if (c != null) {
                                    c.moveToFirst();
                                }
                                String sNumeric = c.getString(c.getColumnIndex(Telephony.Carriers.NUMERIC));
                                Log.d(TAG, "fillList(): sNumeric = " + sNumeric);
                                if ((CTCC_3GPP2_NUMERIC.equals(mccmnc) && CTCC_3GPP_NUMERIC.equals(sNumeric)) ||
                                        (CTCC_3GPP_NUMERIC.equals(mccmnc) && CTCC_3GPP2_NUMERIC.equals(sNumeric))) {
                                    url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, Integer.parseInt(pref.getKey()));
                                    c2 = getContentResolver().query(url, new String[] {Telephony.Carriers.NAME, Telephony.Carriers.APN, Telephony.Carriers.USER, Telephony.Carriers.PASSWORD, Telephony.Carriers.MCC, Telephony.Carriers.MNC, Telephony.Carriers.NUMERIC}, null, null, null);
                                    if (c2 != null) {
                                        c2.moveToFirst();
                                    }
                                    if (isSimilarApn(c, c2)) {
                                        Log.d(TAG, "fillList(): Need make fake Checked at " + mccmnc + " UI list with " + sNumeric);
                                        pref.setChecked();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            if (c != null) {
                                c.close();
                                c = null;
                            }
                            if (c2 != null) {
                                c2.close();
                                c2 = null;
                            }
                        }
                        Log.d(TAG, "fillList(): iPos-- = " + iPos);
                    }
                    // --- rock_huang@20160824: For CTCC SPEC testing
                    addApnToList(pref, mnoApnList, mvnoApnList, r, mvnoType, mvnoMatchData);
                } else {
                    addApnToList(pref, mnoMmsApnList, mvnoMmsApnList, r, mvnoType, mvnoMatchData);
                }
                cursor.moveToNext();
            }
            cursor.close();

            if (!mvnoApnList.isEmpty()) {
                mnoApnList = mvnoApnList;
                mnoMmsApnList = mvnoMmsApnList;

                // Also save the mvno info
            }

            for (Preference preference : mnoApnList) {
                apnList.addPreference(preference);
            }
            for (Preference preference : mnoMmsApnList) {
                apnList.addPreference(preference);
            }
            // +++ AMAX @ 20170119 7.1.1 Porting
            // +++ allen_chu@20140430: special case: restore to default
            if (isATT()) {
                if (mRestoreDefaultApnMode) {
                    copyData(mApnName, mPreApnName);
                    mRestoreToDefault = false;
                }
            }
            // --- allen_chu@20140430
            // --- AMAX @ 20170119 7.1.1 Porting
        }
    }
    // +++ AMAX @ 20170119 7.1.1 Porting
    public static String getLocalizedName(Context context, Cursor cursor, int index) {
        // If can find a localized name, replace the APN name with it
        String resName = cursor.getString(index);
        String localizedName = null;
        if (resName != null && !resName.isEmpty()) {
            int resId = context.getResources().getIdentifier(resName, "string",
                    context.getPackageName());
            try {
                localizedName = context.getResources().getString(resId);
                Log.d(TAG, "Replaced apn name with localized name");
            } catch (NotFoundException e) {
                Log.e(TAG, "Got execption while getting the localized apn name.", e);
            }
        }
        return localizedName;
    }
    // --- AMAX @ 20170119 7.1.1 Porting

    private void addApnToList(ApnPreference pref, ArrayList<ApnPreference> mnoList,
                              ArrayList<ApnPreference> mvnoList, IccRecords r, String mvnoType,
                              String mvnoMatchData) {
        if (r != null && !TextUtils.isEmpty(mvnoType) && !TextUtils.isEmpty(mvnoMatchData)) {
            if (ApnSetting.mvnoMatches(r, mvnoType, mvnoMatchData)) {
                mvnoList.add(pref);
                // Since adding to mvno list, save mvno info
                mMvnoType = mvnoType;
                mMvnoMatchData = mvnoMatchData;
            }
        } else {
            mnoList.add(pref);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        if (!mUnavailable) {
//            // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
//            boolean canAddApn = (mAllowAddingApns && !AsusTelephonyUtils.isVerizon()) ||
//                    (AsusTelephonyUtils.isVerizon() && !AsusTelephonyUtils.isVerizonSim()) ||
//                    (SystemProperties.getInt("persist.radio.for_verizon_test", 0) == 1);
//            if (canAddApn) {
//                menu.add(0, MENU_NEW, 0,
//                        getResources().getString(R.string.menu_new))
//                        .setIcon(android.R.drawable.ic_menu_add)
//                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
//            }
//            // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
//            menu.add(0, MENU_RESTORE, 0,
//                    getResources().getString(R.string.menu_restore))
//                    .setIcon(android.R.drawable.ic_menu_upload);
//        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            addNewApn();
            return true;

        case MENU_RESTORE:
            restoreDefaultApn();
            return true;
        // +++ AMAX
        case android.R.id.home:
            finish();
            return true;
        // --- AMAX
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        intent.putExtra(SUB_ID, subId);
        if (!TextUtils.isEmpty(mMvnoType) && !TextUtils.isEmpty(mMvnoMatchData)) {
            intent.putExtra(MVNO_TYPE, mMvnoType);
            intent.putExtra(MVNO_MATCH_DATA, mMvnoMatchData);
        }
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        startActivity(new Intent(Intent.ACTION_EDIT, url));
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    private void setSelectedApnKey(String key) {
        Log.d(TAG, "setSelectedApnKey mSelectedKey = " + mSelectedKey +", key = " + key);  // +++ AMAX @ 20170119 7.1.1 Porting
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(PREFERAPN_URI, values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;

        Cursor cursor = getContentResolver().query(PREFERAPN_URI, new String[] {"_id"},
                null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        Log.d(TAG, "getSelectedApnKey mSelectedKey = " + mSelectedKey +", key = " + key);  // +++ AMAX @ 20170119 7.1.1 Porting
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        // +++ allen_chu@20140429: clear data
        if (isATT()) {
            if(mApnName != null) {
                mApnName.clear();
            }
            if(mPreApnName != null) {
                mPreApnName.clear();
            }
        }
        // --- allen_chu@20140429
        return true;
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    Log.d(TAG, "EVENT_RESTORE_DEFAULTAPN_COMPLETE ");  // +++ AMAX @ 20170119 7.1.1 Porting
                    Activity activity = getActivity();
                    if (activity == null) {
                        mRestoreDefaultApnMode = false;
                        return;
                    }
                    // +++ AMAX @ 20170119 7.1.1 Porting
                    if (isATT()) { // +++ allen_chu@20140430: set restoreToDefault flag
                        mRestoreToDefault = true;
                    }
                    // --- AMAX @ 20170119 7.1.1 Porting
                    fillList();
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    removeDialogAllowStateLoss(DIALOG_RESTORE_DEFAULTAPN); // +++ Millie_Chang@20160504: TT-791395
                    Toast.makeText(
                        activity,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(DEFAULTAPN_URI, null, null);
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.cnasusres_alertdialog_progress_circle, null);
            TextView title = (TextView) view.getRootView().findViewById(R.id.alertdialog_title);
            title.setText(getResources().getString(R.string.menu_restore));
            TextView message = (TextView) view.getRootView().findViewById(R.id.alertdialog_message);
            message.setText(getResources().getString(R.string.restore_default_apn));
            builder.setView(view);
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

// +++ AMAX
    // +++ allen_chu@20131129
    public boolean isATT() {
        boolean result = false;
        String SKU = SystemProperties.get("ro.product.name", "");
        Log.d(TAG, "isATT(): SKU=" + SKU);
        if (SKU != null) {
            result = SKU.toLowerCase().startsWith("att_");
        }

        return result;
    }
    // --- allen_chu@20131129

    private void copyData(List<String> from, List<String> dest) {
        for (int i=0; i<from.size(); i++) {
            dest.add(from.get(i));
        }
    }

    // +++ Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
    private void resetSelectedApnKey(){
        ApnPreference pref = new ApnPreference(getActivity());
        pref.resetSelectKey();
    }
    // --- Millie_Chang@201160817 : reset default-related variable if default apn is deleted

    // +++ rock_huang@20160824: For CTCC SPEC testing
    private boolean isSimilarApn(Cursor c, Cursor c2) {
        boolean ret = false;
        String sName = c2.getString(c.getColumnIndex(Telephony.Carriers.NAME));
        String sApn = c2.getString(c.getColumnIndex(Telephony.Carriers.APN));
        String sUser = c2.getString(c.getColumnIndex(Telephony.Carriers.USER));
        String sPassword = c2.getString(c.getColumnIndex(Telephony.Carriers.PASSWORD));
        String sMcc = c2.getString(c.getColumnIndex(Telephony.Carriers.MCC));
        String sMnc = c2.getString(c.getColumnIndex(Telephony.Carriers.MNC));
        Log.d(TAG, "isSimilarApn(): sName = " + sName +
                ", sApn = " + sApn +
                ", sUser = " + sUser +
                ", sPassword = " + sPassword +
                ", sMcc = " + sMcc +
                ", sMnc = " + sMnc);
        if (sName.equals(c.getString(c.getColumnIndex(Telephony.Carriers.NAME))) &&
                sApn.equals(c.getString(c.getColumnIndex(Telephony.Carriers.APN))) &&
                sUser.equals(c.getString(c.getColumnIndex(Telephony.Carriers.USER))) &&
                sPassword.equals(c.getString(c.getColumnIndex(Telephony.Carriers.PASSWORD))) &&
                sMcc.equals(c.getString(c.getColumnIndex(Telephony.Carriers.MCC)))) {
            ret = true;
        }
        Log.d(TAG, "isSimilarApn(): ret = " + ret);
        return ret;
    }
    // --- rock_huang@20160824: For CTCC SPEC testing
// --- AMAX
}
