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

package com.android.settings;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.TELEPHONY_SERVICE;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.preference.PreferenceActivity;
import android.provider.Telephony;
import android.support.v14.preference.MultiSelectListPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.util.ArrayUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ApnEditor extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener, OnKeyListener {

    private final static String TAG = ApnEditor.class.getSimpleName();

    private final static String SAVED_POS = "pos";
    private final static String KEY_AUTH_TYPE = "auth_type";
    private final static String KEY_PROTOCOL = "apn_protocol";
    private final static String KEY_ROAMING_PROTOCOL = "apn_roaming_protocol";
    private final static String KEY_CARRIER_ENABLED = "carrier_enabled";
    private final static String KEY_BEARER_MULTI = "bearer_multi";
    private final static String KEY_MVNO_TYPE = "mvno_type";

    private static final int MENU_DELETE = Menu.FIRST;
    private static final int MENU_SAVE = Menu.FIRST + 1;
    private static final int MENU_CANCEL = Menu.FIRST + 2;

    private static String sNotSet;
    private EditTextPreference mName;
    private EditTextPreference mApn;
    private EditTextPreference mProxy;
    private EditTextPreference mPort;
    private EditTextPreference mUser;
    private EditTextPreference mServer;
    private EditTextPreference mPassword;
    private EditTextPreference mMmsc;
    private EditTextPreference mMcc;
    private EditTextPreference mMnc;
    private EditTextPreference mMmsProxy;
    private EditTextPreference mMmsPort;
    private ListPreference mAuthType;
    private EditTextPreference mApnType;
    private ListPreference mProtocol;
    private ListPreference mRoamingProtocol;
    private SwitchPreference mCarrierEnabled;
    private MultiSelectListPreference mBearerMulti;
    private ListPreference mMvnoType;
    private EditTextPreference mMvnoMatchData;

    private String mCurMnc;
    private String mCurMcc;
    private boolean mDisableEditor = false;  // +++ AMAX @ 20170119 7.1.1 Porting

    private Uri mUri;
    private Cursor mCursor;
    private boolean mNewApn;
    private boolean mFirstTime;
    private int mSubId;
    private Resources mRes;
    private TelephonyManager mTelephonyManager;
    private int mBearerInitialVal = 0;
    private String mMvnoTypeStr;
    private String mMvnoMatchDataStr;
    private String[] mReadOnlyApnTypes;
    private String[] mReadOnlyApnFields;
    private boolean mReadOnlyApn;

// +++ AMAX
    private final static String[] ATT_FORBIDDEN_APNS = {"phone", "wap.cingular"};
    private final static String[] ATT_NUMERIC = {"310070", "310090", "310150", "310170", "310380", "310410", "310560", "310680", "311180"};
    // +++ TerryYC@20141117 : <CDR-CDS-048> Custom APN for Data-Centric Devices
    private final static String[] ATT_DATA_CENTRIC_FORBIDDEN_APNS = {"phone", "nxtgenphone", "pta", "wap.cingular"};
    private static boolean sVoiceCapable = true;
    // --- TerryYC@20141117 : <CDR-CDS-048> Custom APN for Data-Centric Devices
    private boolean mIsDefaultApn; // +++ allen_chu@20131210: [at&t] <CDR-CDS-152> PDN/PDP Type-Engineering/Test Mode
    private final static String IS_DEFAULT = "is_default"; // +++ allen_chu@20131210: [at&t] <CDR-CDS-152> PDN/PDP Type-Engineering/Test Mode
    private boolean mIsInsert = false; // +++ allen_chu@20141020: distinguish INSERT or EDIT apn
    public static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);
    private static String CTCC_3GPP_NUMERIC = "46011";
    private static String CTCC_3GPP2_NUMERIC = "46003";
// --- AMAX

    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] sProjection = new String[] {
            Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.PROXY,   // 3
            Telephony.Carriers.PORT,    // 4
            Telephony.Carriers.USER,    // 5
            Telephony.Carriers.SERVER,  // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY,// 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.PROTOCOL, // 16
            Telephony.Carriers.CARRIER_ENABLED, // 17
            Telephony.Carriers.BEARER, // 18
            Telephony.Carriers.BEARER_BITMASK, // 19
            Telephony.Carriers.ROAMING_PROTOCOL, // 20
            Telephony.Carriers.MVNO_TYPE,   // 21
            Telephony.Carriers.MVNO_MATCH_DATA,  // 22
            Telephony.Carriers.EDITED,   // 23
            IS_DEFAULT // +++ allen_chu@20131210: 24
    };

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int PROXY_INDEX = 3;
    private static final int PORT_INDEX = 4;
    private static final int USER_INDEX = 5;
    private static final int SERVER_INDEX = 6;
    private static final int PASSWORD_INDEX = 7;
    private static final int MMSC_INDEX = 8;
    private static final int MCC_INDEX = 9;
    private static final int MNC_INDEX = 10;
    private static final int MMSPROXY_INDEX = 12;
    private static final int MMSPORT_INDEX = 13;
    private static final int AUTH_TYPE_INDEX = 14;
    private static final int TYPE_INDEX = 15;
    private static final int PROTOCOL_INDEX = 16;
    private static final int CARRIER_ENABLED_INDEX = 17;
    private static final int BEARER_INDEX = 18;
    private static final int BEARER_BITMASK_INDEX = 19;
    private static final int ROAMING_PROTOCOL_INDEX = 20;
    private static final int MVNO_TYPE_INDEX = 21;
    private static final int MVNO_MATCH_DATA_INDEX = 22;
    private static final int EDITED_INDEX = 23;
    private static final int IS_DEFAULT_INDEX = 24; // +++ allen_chu@20131210: [at&t] <CDR-CDS-152> PDN/PDP Type-Engineering/Test Mode

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.apn_editor);

        sNotSet = getResources().getString(R.string.apn_not_set);
        mName = (EditTextPreference) findPreference("apn_name");
        mApn = (EditTextPreference) findPreference("apn_apn");
        mProxy = (EditTextPreference) findPreference("apn_http_proxy");
        mPort = (EditTextPreference) findPreference("apn_http_port");
        mUser = (EditTextPreference) findPreference("apn_user");
        mServer = (EditTextPreference) findPreference("apn_server");
        mPassword = (EditTextPreference) findPreference("apn_password");
        mMmsProxy = (EditTextPreference) findPreference("apn_mms_proxy");
        mMmsPort = (EditTextPreference) findPreference("apn_mms_port");
        mMmsc = (EditTextPreference) findPreference("apn_mmsc");
        mMcc = (EditTextPreference) findPreference("apn_mcc");
        mMnc = (EditTextPreference) findPreference("apn_mnc");
        mApnType = (EditTextPreference) findPreference("apn_type");

        mAuthType = (ListPreference) findPreference(KEY_AUTH_TYPE);
        mAuthType.setOnPreferenceChangeListener(this);

        mProtocol = (ListPreference) findPreference(KEY_PROTOCOL);
        mProtocol.setOnPreferenceChangeListener(this);

        mRoamingProtocol = (ListPreference) findPreference(KEY_ROAMING_PROTOCOL);
        mRoamingProtocol.setOnPreferenceChangeListener(this);

        mCarrierEnabled = (SwitchPreference) findPreference(KEY_CARRIER_ENABLED);

        mBearerMulti = (MultiSelectListPreference) findPreference(KEY_BEARER_MULTI);
        mBearerMulti.setOnPreferenceChangeListener(this);

        mMvnoType = (ListPreference) findPreference(KEY_MVNO_TYPE);
        mMvnoType.setOnPreferenceChangeListener(this);
        mMvnoMatchData = (EditTextPreference) findPreference("mvno_match_data");

        // +++ TerryYC@20141117 : <CDR-CDS-048> Custom APN for Data-Centric Devices
        sVoiceCapable = getResources().getBoolean(getResources().getIdentifier("config_voice_capable", "bool", "android"));
        // --- TerryYC@20141117 : <CDR-CDS-048> Custom APN for Data-Centric Devices

        mRes = getResources();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        mSubId = intent.getIntExtra(ApnSettings.SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        // +++ AMAX @ 20170119 7.1.1 Porting
        mDisableEditor = intent.getBooleanExtra("DISABLE_EDITOR", false);
        if (mDisableEditor) {
            getPreferenceScreen().setEnabled(false);
            Log.d(TAG, "ApnEditor form is disabled.");
        }
        Log.d(TAG,"ApnEditor onCreate received mSubId = " + mSubId);
        // --- AMAX @ 20170119 7.1.1 Porting
        mFirstTime = icicle == null;
        mReadOnlyApn = false;
        mReadOnlyApnTypes = null;
        mReadOnlyApnFields = null;

        if (action.equals(Intent.ACTION_EDIT)) {
            Uri uri = intent.getData();
            if (!uri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                Log.e(TAG, "Edit request not for carrier table. Uri: " + uri);
                finish();
                return;
            }
            CarrierConfigManager configManager = (CarrierConfigManager)
                    getSystemService(Context.CARRIER_CONFIG_SERVICE);
            if (configManager != null) {
                PersistableBundle b = configManager.getConfig();
                if (b != null) {
                    mReadOnlyApnTypes = b.getStringArray(
                            CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY);
                    mReadOnlyApnFields = b.getStringArray(
                            CarrierConfigManager.KEY_READ_ONLY_APN_FIELDS_STRING_ARRAY);
                }
            }
            mUri = uri;
            mIsInsert = false; // +++ allen_chu@20141020: indicate EDIT
        } else if (action.equals(Intent.ACTION_INSERT)) {
            mIsInsert = true; // +++ allen_chu@20141020: indicate INSERT
            if (mFirstTime || icicle.getInt(SAVED_POS) == 0) {
                Uri uri = intent.getData();
                if (!uri.isPathPrefixMatch(Telephony.Carriers.CONTENT_URI)) {
                    Log.e(TAG, "Insert request not for carrier table. Uri: " + uri);
                    finish();
                    return;
                }
                mUri = getContentResolver().insert(uri, new ContentValues());
            } else {
                mUri = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI,
                        icicle.getInt(SAVED_POS));
            }
            mNewApn = true;
            mMvnoTypeStr = intent.getStringExtra(ApnSettings.MVNO_TYPE);
            mMvnoMatchDataStr = intent.getStringExtra(ApnSettings.MVNO_MATCH_DATA);
            // If we were unable to create a new note, then just finish
            // this activity.  A RESULT_CANCELED will be sent back to the
            // original activity if they requested a result.
            if (mUri == null) {
                Log.w(TAG, "Failed to insert new telephony provider into "
                        + getIntent().getData());
                finish();
                return;
            }

            // The new entry was created, so assume all will end well and
            // set the result to be returned.
            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        } else {
            finish();
            return;
        }

        // +++ AMAX
        // +++ allen_chu@20131023: fix lost mCursor when coming from pause state
        //mCursor = managedQuery(mUri, sProjection, null, null);
        // +++ ckenken (ChiaHsiang_Kuo) @ 20160704 N-Porting , change to fragment
        //mCursor = getContentResolver().query(mUri, sProjection, null, null, null);

        mCursor = getActivity().getContentResolver().query(mUri, sProjection, null, null, null);

        // --- ckenken (ChiaHsiang_Kuo) @ 20160704 N-Porting , change to fragment
        // --- allen_chu@20131023
        if(mCursor != null) { // +++ allen_chu@20140107: fix NPE
            mCursor.moveToFirst();
        }
        // --- AMAX

        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);

        Log.d(TAG, "onCreate: EDITED " + mCursor.getInt(EDITED_INDEX));
        // if it's not a USER_EDITED apn, check if it's read-only
        if (mCursor.getInt(EDITED_INDEX) != Telephony.Carriers.USER_EDITED &&
                apnTypesMatch(mReadOnlyApnTypes, mCursor.getString(TYPE_INDEX))) {
            Log.d(TAG, "onCreate: apnTypesMatch; read-only APN");
            mReadOnlyApn = true;
            disableAllFields();
        } else if (!ArrayUtils.isEmpty(mReadOnlyApnFields)) {
            disableFields(mReadOnlyApnFields);
        }

        for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); i++) {
            getPreferenceScreen().getPreference(i).setOnPreferenceChangeListener(this);
        }

        fillUi();
        // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
        boolean canOnlyEditNI = (AsusTelephonyUtils.isVerizon() && AsusTelephonyUtils.isVerizonSim()) &&
                !(SystemProperties.getInt("persist.radio.for_verizon_test", 0) == 1);
        Log.d(TAG, "onCreate(): canOnlyEditNI = " + canOnlyEditNI);
        if (canOnlyEditNI) {
            removePreferenceForVerizon();
        }
        // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
    }

    /**
     * Check if passed in array of APN types indicates all APN types
     * @param apnTypes array of APN types. "*" indicates all types.
     * @return true if all apn types are included in the array, false otherwise
     */
    private boolean hasAllApns(String[] apnTypes) {
        if (ArrayUtils.isEmpty(apnTypes)) {
            return false;
        }

        List apnList = Arrays.asList(apnTypes);
        if (apnList.contains(PhoneConstants.APN_TYPE_ALL)) {
            Log.d(TAG, "hasAllApns: true because apnList.contains(PhoneConstants.APN_TYPE_ALL)");
            return true;
        }
        for (String apn : PhoneConstants.APN_TYPES) {
            if (!apnList.contains(apn)) {
                return false;
            }
        }

        Log.d(TAG, "hasAllApns: true");
        return true;
    }

    /**
     * Check if APN types overlap.
     * @param apnTypesArray1 array of APNs. Empty array indicates no APN type; "*" indicates all
     *                       types
     * @param apnTypes2 comma separated string of APN types. Empty string represents all types.
     * @return if any apn type matches return true, otherwise return false
     */
    private boolean apnTypesMatch(String[] apnTypesArray1, String apnTypes2) {
        if (ArrayUtils.isEmpty(apnTypesArray1)) {
            return false;
        }

        if (hasAllApns(apnTypesArray1) || TextUtils.isEmpty(apnTypes2)) {
            return true;
        }

        List apnTypesList1 = Arrays.asList(apnTypesArray1);
        String[] apnTypesArray2 = apnTypes2.split(",");

        for (String apn : apnTypesArray2) {
            if (apnTypesList1.contains(apn.trim())) {
                Log.d(TAG, "apnTypesMatch: true because match found for " + apn.trim());
                return true;
            }
        }

        Log.d(TAG, "apnTypesMatch: false");
        return false;
    }

    /**
     * Function to get Preference obj corresponding to an apnField
     * @param apnField apn field name for which pref is needed
     * @return Preference obj corresponding to passed in apnField
     */
    private Preference getPreferenceFromFieldName(String apnField) {
        switch (apnField) {
            case Telephony.Carriers.NAME:
                return mName;
            case Telephony.Carriers.APN:
                return mApn;
            case Telephony.Carriers.PROXY:
                return mProxy;
            case Telephony.Carriers.PORT:
                return mPort;
            case Telephony.Carriers.USER:
                return mUser;
            case Telephony.Carriers.SERVER:
                return mServer;
            case Telephony.Carriers.PASSWORD:
                return mPassword;
            case Telephony.Carriers.MMSPROXY:
                return mMmsProxy;
            case Telephony.Carriers.MMSPORT:
                return mMmsPort;
            case Telephony.Carriers.MMSC:
                return mMmsc;
            case Telephony.Carriers.MCC:
                return mMcc;
            case Telephony.Carriers.MNC:
                return mMnc;
            case Telephony.Carriers.TYPE:
                return mApnType;
            case Telephony.Carriers.AUTH_TYPE:
                return mAuthType;
            case Telephony.Carriers.PROTOCOL:
                return mProtocol;
            case Telephony.Carriers.ROAMING_PROTOCOL:
                return mRoamingProtocol;
            case Telephony.Carriers.CARRIER_ENABLED:
                return mCarrierEnabled;
            case Telephony.Carriers.BEARER:
            case Telephony.Carriers.BEARER_BITMASK:
                return mBearerMulti;
            case Telephony.Carriers.MVNO_TYPE:
                return mMvnoType;
            case Telephony.Carriers.MVNO_MATCH_DATA:
                return mMvnoMatchData;
        }
        return null;
    }

    /**
     * Disables given fields so that user cannot modify them
     *
     * @param apnFields fields to be disabled
     */
    private void disableFields(String[] apnFields) {
        for (String apnField : apnFields) {
            Preference preference = getPreferenceFromFieldName(apnField);
            if (preference != null) {
                preference.setEnabled(false);
            }
        }
    }

    /**
     * Disables all fields so that user cannot modify the APN
     */
    private void disableAllFields() {
        mName.setEnabled(false);
        mApn.setEnabled(false);
        mProxy.setEnabled(false);
        mPort.setEnabled(false);
        mUser.setEnabled(false);
        mServer.setEnabled(false);
        mPassword.setEnabled(false);
        mMmsProxy.setEnabled(false);
        mMmsPort.setEnabled(false);
        mMmsc.setEnabled(false);
        mMcc.setEnabled(false);
        mMnc.setEnabled(false);
        mApnType.setEnabled(false);
        mAuthType.setEnabled(false);
        mProtocol.setEnabled(false);
        mRoamingProtocol.setEnabled(false);
        mCarrierEnabled.setEnabled(false);
        mBearerMulti.setEnabled(false);
        mMvnoType.setEnabled(false);
        mMvnoMatchData.setEnabled(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        com.asus.cncommonres.AsusButtonBar buttonBar = ((SettingsActivity)getActivity()).getButtonBar();
        if(buttonBar != null) {
            buttonBar.setVisibility(View.GONE);
        }
        View footerButtonBar = getActivity().findViewById(R.id.footer_button_bar);
        Log.d(TAG, "onActivityCreated(): buttonBar=" + footerButtonBar);
        if(footerButtonBar != null) {
            Log.d(TAG, "onActivityCreated(): create footerButtonBar");
            footerButtonBar.setVisibility(View.VISIBLE);
            Button footerLeftButton = (Button) getActivity().findViewById(R.id.footerLeftButton);
            if (mNewApn) {
                footerLeftButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
            } else {
                footerLeftButton.setText(getResources().getString(R.string.menu_delete));
                footerLeftButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        deleteApn();
                    }
                });
            }
            Button footerRightButton = (Button) getActivity().findViewById(R.id.footerRightButton);
            footerRightButton.setText(getResources().getString(R.string.menu_save));
            footerRightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (validateAndSave(false)) {
                        finish();
                    }
                }
            });
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APN_EDITOR;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void fillUi() {
        if (mFirstTime) {
            mFirstTime = false;
            // Fill in all the values from the db in both text editor and summary
            mName.setText(mCursor.getString(NAME_INDEX));
            mApn.setText(mCursor.getString(APN_INDEX));
            mProxy.setText(mCursor.getString(PROXY_INDEX));
            mPort.setText(mCursor.getString(PORT_INDEX));
            mUser.setText(mCursor.getString(USER_INDEX));
            mServer.setText(mCursor.getString(SERVER_INDEX));
            mPassword.setText(mCursor.getString(PASSWORD_INDEX));
            mMmsProxy.setText(mCursor.getString(MMSPROXY_INDEX));
            mMmsPort.setText(mCursor.getString(MMSPORT_INDEX));
            mMmsc.setText(mCursor.getString(MMSC_INDEX));
            mMcc.setText(mCursor.getString(MCC_INDEX));
            mMnc.setText(mCursor.getString(MNC_INDEX));
            mApnType.setText(mCursor.getString(TYPE_INDEX));
            if (mNewApn) {
                String numeric = mTelephonyManager.getSimOperator(mSubId);
                // MCC is first 3 chars and then in 2 - 3 chars of MNC
                if (numeric != null && numeric.length() > 4) {
                    // Country code
                    String mcc = numeric.substring(0, 3);
                    // Network code
                    String mnc = numeric.substring(3);
                    // Auto populate MNC and MCC for new entries, based on what SIM reports
                    mMcc.setText(mcc);
                    mMnc.setText(mnc);
                    mCurMnc = mnc;
                    mCurMcc = mcc;
                }
            }
            int authVal = mCursor.getInt(AUTH_TYPE_INDEX);
            if (authVal != -1) {
                mAuthType.setValueIndex(authVal);
            } else {
                mAuthType.setValue(null);
            }

            mProtocol.setValue(mCursor.getString(PROTOCOL_INDEX));
            mRoamingProtocol.setValue(mCursor.getString(ROAMING_PROTOCOL_INDEX));
            mCarrierEnabled.setChecked(mCursor.getInt(CARRIER_ENABLED_INDEX)==1);
            mBearerInitialVal = mCursor.getInt(BEARER_INDEX);

            HashSet<String> bearers = new HashSet<String>();
            int bearerBitmask = mCursor.getInt(BEARER_BITMASK_INDEX);
            if (bearerBitmask == 0) {
                if (mBearerInitialVal == 0) {
                    bearers.add("" + 0);
                }
            } else {
                int i = 1;
                while (bearerBitmask != 0) {
                    if ((bearerBitmask & 1) == 1) {
                        bearers.add("" + i);
                    }
                    bearerBitmask >>= 1;
                    i++;
                }
            }

            if (mBearerInitialVal != 0 && bearers.contains("" + mBearerInitialVal) == false) {
                // add mBearerInitialVal to bearers
                bearers.add("" + mBearerInitialVal);
            }
            mBearerMulti.setValues(bearers);

            mMvnoType.setValue(mCursor.getString(MVNO_TYPE_INDEX));
            mMvnoMatchData.setEnabled(false);
            mMvnoMatchData.setText(mCursor.getString(MVNO_MATCH_DATA_INDEX));
            if (mNewApn && mMvnoTypeStr != null && mMvnoMatchDataStr != null) {
                mMvnoType.setValue(mMvnoTypeStr);
                mMvnoMatchData.setText(mMvnoMatchDataStr);
            }
            // +++ AMAX @ 20170119 7.1.1 Porting
            String localizedName = ApnSettings.getLocalizedName(getActivity(), mCursor,NAME_INDEX);
            if (!TextUtils.isEmpty(localizedName)) {
                mName.setText(localizedName);
            }
            // --- AMAX @ 20170119 7.1.1 Porting
        }

        mName.setSummary(checkNull(mName.getText()));
        mApn.setSummary(checkNull(mApn.getText()));
        mProxy.setSummary(checkNull(mProxy.getText()));
        mPort.setSummary(checkNull(mPort.getText()));
        mUser.setSummary(checkNull(mUser.getText()));
        mServer.setSummary(checkNull(mServer.getText()));
        mPassword.setSummary(starify(mPassword.getText()));
        mMmsProxy.setSummary(checkNull(mMmsProxy.getText()));
        mMmsPort.setSummary(checkNull(mMmsPort.getText()));
        mMmsc.setSummary(checkNull(mMmsc.getText()));
        mMcc.setSummary(checkNull(mMcc.getText()));
        mMnc.setSummary(checkNull(mMnc.getText()));
        mApnType.setSummary(checkNull(mApnType.getText()));

        // +++ allen_chu@20140327: at&t<CDR-CDS-522>: using "default" type when apn type is empty
        if (isATT() &&
            ((mCursor.getString(TYPE_INDEX) == null) || (mCursor.getString(TYPE_INDEX) != null && mCursor.getString(TYPE_INDEX).isEmpty()))) {
            Log.d(TAG, "fillUi(): apn type is null => set default");
            mApnType.setSummary("default,hipri");
            mApnType.setText("default,hipri");
        }
        // --- allen_chu@20140327

        String authVal = mAuthType.getValue();
        if (authVal != null) {
            int authValIndex = Integer.parseInt(authVal);
            mAuthType.setValueIndex(authValIndex);

            String []values = mRes.getStringArray(R.array.apn_auth_entries);
            mAuthType.setSummary(values[authValIndex]);
        } else {
            mAuthType.setSummary(sNotSet);
        }

        // +++ allen_chu@20141215: AT&T: LTE-BTR-1-5931: Protocol/Roaming Protocol type should be IPV4IPV6
        // 0: IPV4; 1: IPV6; 2: IPV4IPV6
        if (isATT() && mNewApn) {
            if(mProtocol != null) {
                mProtocol.setValueIndex(2);
            }
            if(mRoamingProtocol != null) {
                mRoamingProtocol.setValueIndex(2);
            }
        }
        // --- allen_chu@20141215

        mProtocol.setSummary(
                checkNull(protocolDescription(mProtocol.getValue(), mProtocol)));
        mRoamingProtocol.setSummary(
                checkNull(protocolDescription(mRoamingProtocol.getValue(), mRoamingProtocol)));
        mBearerMulti.setSummary(
                checkNull(bearerMultiDescription(mBearerMulti.getValues())));
        mMvnoType.setSummary(
                checkNull(mvnoDescription(mMvnoType.getValue())));
        mMvnoMatchData.setSummary(checkNull(mMvnoMatchData.getText()));
        // allow user to edit carrier_enabled for some APN
        boolean ceEditable = getResources().getBoolean(R.bool.config_allow_edit_carrier_enabled);
        if (ceEditable) {
            mCarrierEnabled.setEnabled(true);
        } else {
            mCarrierEnabled.setEnabled(false);
        }

        // +++ AMAX
        // +++ allen_chu@20140619: if there exists mvno type in sim card, fill these two field: mvno_type, mvno_match_data
        if(hasMVNOType()) {
            // fill mvno type
            mMvnoType.setValue(getMVNOType());
            mMvnoType.setSummary(mvnoDescription(getMVNOType()));
            Log.d(TAG, "fillUi(): fill mvno type = " + mMvnoType.getSummary());

            // fill mvno match data
            mMvnoMatchData.setText(getMVNOMatchData());
            mMvnoMatchData.setSummary(getMVNOMatchData());
            Log.d(TAG, "fillUi(): fill mvno match data = " + mMvnoMatchData.getSummary());
        }
        // --- allen_chu@20140619

        if ((isATT() && isAttNumeric(getOperatorNumeric(mSubId)))
                || android.os.SystemProperties.get("ro.build.asus.sku", "").toLowerCase().endsWith("cucc")
                || android.os.SystemProperties.get("ro.product.name", "").toLowerCase().startsWith("cucc")) { // +++ allen_chu@20131210: [at&t] <CDR-CDS-152> PDN/PDP Type-Engineering/Test Mode
            mIsDefaultApn = mCursor.getInt(IS_DEFAULT_INDEX) == 1? true: false;
            Log.d(TAG, "fillUi(): mIsDefaultApn = " + mIsDefaultApn);
            disableApnEditor();

            // +++ allen_chu@20140121: remove protocol and roaming protocol field
            //getPreferenceScreen().removePreference((ListPreference) mProtocol);
            //getPreferenceScreen().removePreference((ListPreference) mRoamingProtocol);
            // --- allen_chu@20140121
        }
        // --- AMAX
    }

    /**
     * Returns the UI choice (e.g., "IPv4/IPv6") corresponding to the given
     * raw value of the protocol preference (e.g., "IPV4V6"). If unknown,
     * return null.
     */
    private String protocolDescription(String raw, ListPreference protocol) {
        int protocolIndex = protocol.findIndexOfValue(raw);
        if (protocolIndex == -1) {
            return null;
        } else {
            String[] values = mRes.getStringArray(R.array.apn_protocol_entries);
            try {
                return values[protocolIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    private String bearerDescription(String raw) {
        int mBearerIndex = mBearerMulti.findIndexOfValue(raw);
        if (mBearerIndex == -1) {
            return null;
        } else {
            String[] values = mRes.getStringArray(R.array.bearer_entries);
            try {
                return values[mBearerIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    private String bearerMultiDescription(Set<String> raw) {
        String[] values = mRes.getStringArray(R.array.bearer_entries);
        StringBuilder retVal = new StringBuilder();
        boolean first = true;
        for (String bearer : raw) {
            int bearerIndex = mBearerMulti.findIndexOfValue(bearer);
            try {
                if (first) {
                    retVal.append(values[bearerIndex]);
                    first = false;
                } else {
                    retVal.append(", " + values[bearerIndex]);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                // ignore
            }
        }
        String val = retVal.toString();
        if (!TextUtils.isEmpty(val)) {
            return val;
        }
        return null;
    }

    private String mvnoDescription(String newValue) {
        int mvnoIndex = mMvnoType.findIndexOfValue(newValue);
        String oldValue = mMvnoType.getValue();

        if (mvnoIndex == -1) {
            return null;
        } else {
            String[] values = mRes.getStringArray(R.array.mvno_type_entries);
            // +++ ckenken (ChiaHsiang_Kuo) @ 20160704 N-Porting
            //mMvnoMatchData.setEnabled(mvnoIndex != 0);
            // --- ckenken (ChiaHsiang_Kuo) @ 20160704 N-Porting
            // +++ AMAX @ 20170119 7.1.1 Porting
            if (values[mvnoIndex].equals("None")) {
                mMvnoMatchData.setEnabled(false);
            } else {
                // +++ allen_chu@20131129: [at&t] <CDR-CDS-152> PDN/PDP Type-Engineering/Test Mode
                if (isATT()) {
                    if(!mIsDefaultApn) {
                        mMvnoMatchData.setEnabled(true);
                    }
                } else {
                    mMvnoMatchData.setEnabled(true);
                }
                // --- allen_chu@20131129
            }
            // --- AMAX @ 20170119 7.1.1 Porting
            if (newValue != null && newValue.equals(oldValue) == false) {
                if (values[mvnoIndex].equals("SPN")) {
                    mMvnoMatchData.setText(mTelephonyManager.getSimOperatorName());
                } else if (values[mvnoIndex].equals("IMSI")) {
                    String numeric = mTelephonyManager.getSimOperator(mSubId);
                    mMvnoMatchData.setText(numeric + "x");
                } else if (values[mvnoIndex].equals("GID")) {
                    mMvnoMatchData.setText(mTelephonyManager.getGroupIdLevel1());
                // +++ AMAX @ 20170119 7.1.1 Porting
                } else if (values[mvnoIndex].equals("ICCID")) {
                    if (mMvnoMatchDataStr != null) {
                        Log.d(TAG, "mMvnoMatchDataStr: " + mMvnoMatchDataStr);
                        mMvnoMatchData.setText(mMvnoMatchDataStr);
                    }
                }
                // --- AMAX @ 20170119 7.1.1 Porting
            }

            try {
                return values[mvnoIndex];
            } catch (ArrayIndexOutOfBoundsException e) {
                return null;
            }
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        Log.d(TAG, "onPreferenceChange(): key = " + key + ",newValue=" + newValue);
        if (KEY_AUTH_TYPE.equals(key)) {
            try {
                int index = Integer.parseInt((String) newValue);
                mAuthType.setValueIndex(index);

                String[] values = mRes.getStringArray(R.array.apn_auth_entries);
                mAuthType.setSummary(values[index]);
            } catch (NumberFormatException e) {
                return false;
            }
        } else if (KEY_PROTOCOL.equals(key)) {
            String protocol = protocolDescription((String) newValue, mProtocol);
            if (protocol == null) {
                return false;
            }
            mProtocol.setValue((String) newValue);
            mProtocol.setSummary(protocol);  // +++ AMAX: setSummary need after setValue for UI display
        } else if (KEY_ROAMING_PROTOCOL.equals(key)) {
            String protocol = protocolDescription((String) newValue, mRoamingProtocol);
            if (protocol == null) {
                return false;
            }
            mRoamingProtocol.setValue((String) newValue);
            mRoamingProtocol.setSummary(protocol);  // +++ AMAX: setSummary need after setValue for UI display
        } else if (KEY_BEARER_MULTI.equals(key)) {
            String bearer = bearerMultiDescription((Set<String>) newValue);
            if (bearer == null) {
                return false;
            }
            mBearerMulti.setValues((Set<String>) newValue);
            mBearerMulti.setSummary(bearer);
        } else if (KEY_MVNO_TYPE.equals(key)) {
            String mvno = mvnoDescription((String) newValue);
            if (mvno == null) {
                return false;
            }
            mMvnoType.setValue((String) newValue);
            mMvnoType.setSummary(mvno);
        } else if (preference.equals(mPassword)) {  // +++ AMAX @ 20170119 7.1.1 Porting
            preference.setSummary(starify(newValue != null ? String.valueOf(newValue) : ""));
        } else if (preference.equals(mCarrierEnabled) || preference.equals(mBearerMulti)) {
            // do nothing
        } else {
            preference.setSummary(checkNull(newValue != null ? String.valueOf(newValue) : null));
        }

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        if (mDisableEditor) {
//            Log.d(TAG, "Form is disabled. Do not create the options menu.");
//            return;
//        }
//        // If it's a new APN, then cancel will delete the new entry in onPause
//        if (!mNewApn) {
//            menu.add(0, MENU_DELETE, 0, R.string.menu_delete)
//                .setIcon(R.drawable.ic_menu_delete);
//        }
//        menu.add(0, MENU_SAVE, 0, R.string.menu_save)
//            .setIcon(android.R.drawable.ic_menu_save);
//        menu.add(0, MENU_CANCEL, 0, R.string.menu_cancel)
//            .setIcon(android.R.drawable.ic_menu_close_clear_cancel);
//
//        // +++ allen_chu@20131129: [at&t] <CDR-CDS-152> PDN/PDP Type-Engineering/Test Mode
//        if (mIsDefaultApn) {
//            menu.getItem(0).setEnabled(false);
//            menu.getItem(1).setEnabled(false);
//            if (!mNewApn) {
//                menu.getItem(2).setEnabled(false);
//            }
//        }
//        // --- allen_chu@20131129: [at&t] <CDR-CDS-152> PDN/PDP Type-Engineering/Test Mode
//        //return true; // become void after Android N
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DELETE:
            deleteApn();
            return true;
        case MENU_SAVE:
            if (validateAndSave(false)) {
                finish();
            }
            return true;
        case MENU_CANCEL:
            if (mNewApn) {
                getContentResolver().delete(mUri, null, null);
            }
            finish();
            return true;
        // +++ AMAX
        case android.R.id.home:
            if (validateAndSave(false)) {
                finish();
            }
            return true;
        // --- AMAX
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnKeyListener(this);
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK: {
                if (validateAndSave(false)) {
                    finish();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        if (validateAndSave(true)) {
            icicle.putInt(SAVED_POS, mCursor.getInt(ID_INDEX));
        }
    }

    /**
     * Check the key fields' validity and save if valid.
     * @param force save even if the fields are not valid, if the app is
     *        being suspended
     * @return true if the data was saved
     */
    private boolean validateAndSave(boolean force) {
        // +++ AMAX @ 20170119 7.1.1 Porting
        Log.d(TAG, "validateAndSave()++ force = " + force);
        // If the form is not editable, do nothing and return.
        if (mDisableEditor){
            Log.d(TAG, "Form is disabled. Nothing to save.");
            return true;
        }
        // --- AMAX @ 20170119 7.1.1 Porting

        String name = checkNotSet(mName.getText());
        String apn = checkNotSet(mApn.getText());
        String mcc = checkNotSet(mMcc.getText());
        String mnc = checkNotSet(mMnc.getText());

        if (getErrorMsg() != null && !force) {
            ErrorDialog.showError(this);
            return false;
        }

        // +++ AMAX
        //Forbidden apns should never be saved in any cases
        if (mRes.getString(R.string.error_apn_not_allowed).equals(getErrorMsg())) {
            return false;
        }
        // --- AMAX

        if (!mCursor.moveToFirst()) {
            Log.w(TAG,
                    "Could not go to the first row in the Cursor when saving data.");
            return false;
        }

        // If it's a new APN and a name or apn haven't been entered, then erase the entry
        if (force && mNewApn && name.length() < 1 && apn.length() < 1) {
            getContentResolver().delete(mUri, null, null);
            return false;
        }

        ContentValues values = new ContentValues();

        // Add a dummy name "Untitled", if the user exits the screen without adding a name but
        // entered other information worth keeping.
        values.put(Telephony.Carriers.NAME,
                name.length() < 1 ? getResources().getString(R.string.untitled_apn) : name);
        values.put(Telephony.Carriers.APN, apn);
        values.put(Telephony.Carriers.PROXY, checkNotSet(mProxy.getText()));
        values.put(Telephony.Carriers.PORT, checkNotSet(mPort.getText()));
        values.put(Telephony.Carriers.MMSPROXY, checkNotSet(mMmsProxy.getText()));
        values.put(Telephony.Carriers.MMSPORT, checkNotSet(mMmsPort.getText()));
        values.put(Telephony.Carriers.USER, checkNotSet(mUser.getText()));
        values.put(Telephony.Carriers.SERVER, checkNotSet(mServer.getText()));
        values.put(Telephony.Carriers.PASSWORD, checkNotSet(mPassword.getText()));
        values.put(Telephony.Carriers.MMSC, checkNotSet(mMmsc.getText()));

        String authVal = mAuthType.getValue();
        if (authVal != null) {
            values.put(Telephony.Carriers.AUTH_TYPE, Integer.parseInt(authVal));
        }

        values.put(Telephony.Carriers.PROTOCOL, checkNotSet(mProtocol.getValue()));
        values.put(Telephony.Carriers.ROAMING_PROTOCOL, checkNotSet(mRoamingProtocol.getValue()));

        values.put(Telephony.Carriers.TYPE, checkNotSet(mApnType.getText()));

        // +++ allen_chu@20140327: at&t<CDR-CDS-522>: using "default" type when apn type is empty
        if (isATT() &&
            ((mApnType.getText() == null) || ((mApnType.getText() != null) && mApnType.getText().isEmpty()))) {
            Log.d(TAG, "validateAndSave(): apn type is null => set default");
            values.put(Telephony.Carriers.TYPE, "default,hipri");
        }
        // --- allen_chu@20140327

        values.put(Telephony.Carriers.MCC, mcc);
        values.put(Telephony.Carriers.MNC, mnc);

        values.put(Telephony.Carriers.NUMERIC, mcc + mnc);

        if (mCurMnc != null && mCurMcc != null) {
            if (mCurMnc.equals(mnc) && mCurMcc.equals(mcc)) {
                values.put(Telephony.Carriers.CURRENT, 1);
            }
        }

        Set<String> bearerSet = mBearerMulti.getValues();
        int bearerBitmask = 0;
        for (String bearer : bearerSet) {
            if (Integer.parseInt(bearer) == 0) {
                bearerBitmask = 0;
                break;
            } else {
                bearerBitmask |= ServiceState.getBitmaskForTech(Integer.parseInt(bearer));
            }
        }
        values.put(Telephony.Carriers.BEARER_BITMASK, bearerBitmask);

        int bearerVal;
        if (bearerBitmask == 0 || mBearerInitialVal == 0) {
            bearerVal = 0;
        } else if (ServiceState.bitmaskHasTech(bearerBitmask, mBearerInitialVal)) {
            bearerVal = mBearerInitialVal;
        } else {
            // bearer field was being used but bitmask has changed now and does not include the
            // initial bearer value -- setting bearer to 0 but maybe better behavior is to choose a
            // random tech from the new bitmask??
            bearerVal = 0;
        }
        values.put(Telephony.Carriers.BEARER, bearerVal);

        values.put(Telephony.Carriers.MVNO_TYPE, checkNotSet(mMvnoType.getValue()));
        values.put(Telephony.Carriers.MVNO_MATCH_DATA, checkNotSet(mMvnoMatchData.getText()));

        values.put(Telephony.Carriers.CARRIER_ENABLED, mCarrierEnabled.isChecked() ? 1 : 0);
        // +++ AMAX @ 20170119 7.1.1 Porting
        // +++ allen_chu@20141020 set is_default value to 0 if user's apn
        if (mIsInsert) {
            values.put(IS_DEFAULT, "0");
        }
        // --- allen_chu@20141020

        // +++ allen_chu@20131023: If row deleted in pause-state, insert it
        int updateCount = getContentResolver().update(mUri, values, null, null);
        if (updateCount == 0) {
            Uri uri = getContentResolver().insert(Telephony.Carriers.CONTENT_URI, values);
            Log.d(TAG, "validateAndSave(): uri: " + uri);
        }
        // --- allen_chu@20131023
        // +++ rock_huang@20160824: For CTCC SPEC testing
        String sNumeric = mcc + mnc;
        Log.d(TAG, "validateAndSave() sNumeric = " + sNumeric);
        if (CTCC_3GPP2_NUMERIC.equals(sNumeric)) {
            values.put(Telephony.Carriers.MNC, "11");
            values.put(Telephony.Carriers.NUMERIC, CTCC_3GPP_NUMERIC);
            Uri uri = getContentResolver().insert(Telephony.Carriers.CONTENT_URI, values);
            Log.d(TAG, "validateAndSave(): uri: " + uri);
        } else if (CTCC_3GPP_NUMERIC.equals(sNumeric)) {
            values.put(Telephony.Carriers.MNC, "03");
            values.put(Telephony.Carriers.NUMERIC, CTCC_3GPP2_NUMERIC);
            Uri uri = getContentResolver().insert(Telephony.Carriers.CONTENT_URI, values);
            Log.d(TAG, "validateAndSave(): uri: " + uri);
        }
        // --- rock_huang@20160824: For CTCC SPEC testing
        Log.d(TAG, "validateAndSave()--");
        // --- AMAX @ 20170119 7.1.1 Porting
        return true;
    }

    private String getErrorMsg() {
        String errorMsg = null;

        String name = checkNotSet(mName.getText());
        String apn = checkNotSet(mApn.getText());
        String mcc = checkNotSet(mMcc.getText());
        String mnc = checkNotSet(mMnc.getText());

        if (name.length() < 1) {
            errorMsg = mRes.getString(R.string.error_name_empty);
        } else if (apn.length() < 1) {
            errorMsg = mRes.getString(R.string.error_apn_empty);
        } else if (mcc.length() != 3) {
            errorMsg = mRes.getString(R.string.error_mcc_not3);
        } else if ((mnc.length() & 0xFFFE) != 2) {
            errorMsg = mRes.getString(R.string.error_mnc_not23);
        // +++ TerryYC@20141117 : <CDR-CDS-048> Custom APN for Data-Centric Devices
        } else if(isATT() && !sVoiceCapable) {
            for (String forbiddenApn: ATT_DATA_CENTRIC_FORBIDDEN_APNS) {
                if (forbiddenApn.equalsIgnoreCase(apn)) {
                    errorMsg = mRes.getString(R.string.error_apn_not_allowed);
                    break;
                }
            }
        // --- TerryYC@20141117 : <CDR-CDS-048> Custom APN for Data-Centric Devices
        }

        return errorMsg;
    }

    private void deleteApn() {
        // +++ Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
        if (isPreferApn()) {
            resetPreferApn();
        }
        // --- Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
        getContentResolver().delete(mUri, null, null);
        // +++ rock_huang@20160824: For CTCC SPEC testing
        String sNumeric = mCursor.getString(MCC_INDEX) + mCursor.getString(MNC_INDEX);
        if (CTCC_3GPP2_NUMERIC.equals(sNumeric)) {
            String sWhere = mCursor.getColumnName(NAME_INDEX) + "=? AND " +
                    mCursor.getColumnName(APN_INDEX) + "=? AND " +
                    mCursor.getColumnName(USER_INDEX) + "=? AND " +
                    mCursor.getColumnName(PASSWORD_INDEX) + "=? AND " +
                    mCursor.getColumnName(MCC_INDEX) + "=? AND " +
                    mCursor.getColumnName(MNC_INDEX) + "=?";
            String[] sSelectionArgs = new String[] { mCursor.getString(NAME_INDEX),
                    mCursor.getString(APN_INDEX),
                    mCursor.getString(USER_INDEX),
                    mCursor.getString(PASSWORD_INDEX),
                    mCursor.getString(MCC_INDEX),
                    "11"};
            getContentResolver().delete(Telephony.Carriers.CONTENT_URI, sWhere, sSelectionArgs);
        } else if (CTCC_3GPP_NUMERIC.equals(sNumeric)) {
            String sWhere = mCursor.getColumnName(NAME_INDEX) + "=? AND " +
                    mCursor.getColumnName(APN_INDEX) + "=? AND " +
                    mCursor.getColumnName(USER_INDEX) + "=? AND " +
                    mCursor.getColumnName(PASSWORD_INDEX) + "=? AND " +
                    mCursor.getColumnName(MCC_INDEX) + "=? AND " +
                    mCursor.getColumnName(MNC_INDEX) + "=?";
            String[] sSelectionArgs = new String[] { mCursor.getString(NAME_INDEX),
                    mCursor.getString(APN_INDEX),
                    mCursor.getString(USER_INDEX),
                    mCursor.getString(PASSWORD_INDEX),
                    mCursor.getString(MCC_INDEX),
                    "03"};
            getContentResolver().delete(Telephony.Carriers.CONTENT_URI, sWhere, sSelectionArgs);
        }
        // --- rock_huang@20160824: For CTCC SPEC testing
        finish();
    }

    public static String starify(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        } else {
            char[] password = new char[value.length()];
            for (int i = 0; i < password.length; i++) {
                password[i] = '*';
            }
            return new String(password);
        }
    }

    public static String checkNull(String value) {
        if (value == null || value.length() == 0) {
            return sNotSet;
        } else {
            return value;
        }
    }

    private String checkNotSet(String value) {
        if (value == null || value.equals(sNotSet)) {
            return "";
        } else {
            return value;
        }
    }

    public static class ErrorDialog extends DialogFragment {

        public static void showError(ApnEditor editor) {
            ErrorDialog dialog = new ErrorDialog();
            dialog.setTargetFragment(editor, 0);
            dialog.show(editor.getFragmentManager(), "error");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String msg = ((ApnEditor) getTargetFragment()).getErrorMsg();

            return new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setPositiveButton(android.R.string.ok, null)
                    .setMessage(msg)
                    .create();
        }
    }

// +++ AMAX
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCursor != null) { // +++ allen_chu@20140107: fix NPE
            mCursor.close(); // +++ allen_chu@20131023: Avoid cursor leak
        }
    }

    private boolean isAttNumeric(String numeric) {
        for (String att_numeric : ATT_NUMERIC) {
            if (att_numeric.equals(numeric)) return true;
        }
        return false;
    }

    // +++ allen_chu@20131129: <CDR-CDS-152> PDN/PDP Type-Engineering/Test Mode
    public void disableApnEditor() {

        String tmp_name = checkNotSet(mName.getText());
        String tmp_mcc = checkNotSet(mMcc.getText());
        String tmp_mnc = checkNotSet(mMnc.getText());

        String CRAPN[][] = {
                {
                        "310070", "ATT Nextgenphone"
                }, {
                        "310090", "ATT Nextgenphone"
                }, {
                        "310150", "ATT Nextgenphone"
                }, {
                        "310170", "ATT Nextgenphone"
                }, {
                        "310380", "ATT Nextgenphone"
                }, {
                        "310410", "ATT Nextgenphone"
                }, {
                        "310560", "ATT Nextgenphone"
                }, {
                        "310680", "ATT Nextgenphone"
                }, {
                        "310070", "ATT Broadband"
                }, {
                        "310070", "AT&T activation"
                }, {
                        "310070", "ATT activation"
                }, {
                        "310090", "ATT Broadband"
                }, {
                        "310090", "AT&T activation"
                }, {
                        "310090", "ATT activation"
                }, {
                        "310150", "ATT Broadband"
                }, {
                        "310150", "AT&T activation"
                }, {
                        "310150", "ATT activation"
                }, {
                        "310170", "ATT Broadband"
                }, {
                        "310170", "AT&T activation"
                }, {
                        "310170", "ATT activation"
                }, {
                        "310380", "ATT Broadband"
                }, {
                        "310380", "AT&T activation"
                }, {
                        "310380", "ATT activation"
                }, {
                        "310410", "ATT Broadband"
                }, {
                        "310410", "AT&T activation"
                }, {
                        "310410", "ATT activation"
                }, {
                        "310560", "ATT Broadband"
                }, {
                        "310560", "AT&T activation"
                }, {
                        "310560", "ATT activation"
                }, {
                        "310680", "ATT Broadband"
                }, {
                        "310680", "AT&T activation"
                }, {
                        "310680", "ATT activation"
                }
        };

        String tmp_numeric;
        if ((tmp_mcc != null) && (tmp_mnc != null)) {
            tmp_numeric = tmp_mcc + tmp_mnc;
        } else {
            tmp_numeric = "00000";
        }

        if ((tmp_name != null) && (tmp_numeric != null)) {
            for (int i = 0; i < CRAPN.length; i++) {
                if ((tmp_numeric.equals(CRAPN[i][0])) && (tmp_name.equals(CRAPN[i][1]))) {
                    // Log.d(TAG, "RemoveCustomAPN name = " +CRAPN[i][0] + "numeric = " + CRAPN[i][1]);
                }
            }
        }

        if (mIsDefaultApn) {
            Log.d(TAG, "AT&T SKU => default apn page, disable every item");
            getPreferenceScreen().setEnabled(false);
        } else {
            Log.d(TAG, "AT&T SKU => disable protocol and roaming protocol item");
            mProtocol.setEnabled(false);
            mRoamingProtocol.setEnabled(false);
        }
    }

    // --- allen_chu@20131129

    // +++ allen_chu@20140619
    // check mvno type
    private boolean hasMVNOType() {
        String mvnoType = getMVNOType();
        if (mvnoType == null || mvnoType.isEmpty()) {
            Log.d(TAG, "hasMVNOType: false");
            return false;
        }
        Log.d(TAG, "hasMVNOType: true");
        return true;
    }

    // get mvno type
    private String getMVNOType() {
        return android.provider.Settings.Global.getString(getContentResolver(),
                ApnSettings.MVNO_TYPE);
    }

    // get mvno match data
    private String getMVNOMatchData() {
        return android.provider.Settings.Global.getString(getContentResolver(),
                ApnSettings.MVNO_MATCH_DATA);
    }

    // --- allen_chu@20140619

    private String getOperatorNumeric(int sub) {
        String mccMncFromSim = mTelephonyManager.getSimOperator(sub);
        return mccMncFromSim;
    }

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

    // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
    private void removePreferenceForVerizon() {
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs == null) {
            Log.d(TAG, "removePreferenceForVerizon(): Failed to remove preference since prefs is null");
            return;
        }
        if (mName != null) {
            prefs.removePreference((EditTextPreference) mName);
        }
        if (mProxy != null) {
            prefs.removePreference((EditTextPreference) mProxy);
        }
        if (mPort != null) {
            prefs.removePreference((EditTextPreference) mPort);
        }
        if (mUser != null) {
            prefs.removePreference((EditTextPreference) mUser);
        }
        if (mServer != null) {
            prefs.removePreference((EditTextPreference) mServer);
        }
        if (mPassword != null) {
            prefs.removePreference((EditTextPreference) mPassword);
        }
        if (mMmsc != null) {
            prefs.removePreference((EditTextPreference) mMmsc);
        }
        if (mMcc != null) {
            prefs.removePreference((EditTextPreference) mMcc);
        }
        if (mMnc != null) {
            prefs.removePreference((EditTextPreference) mMnc);
        }
        if (mMmsProxy != null) {
            prefs.removePreference((EditTextPreference) mMmsProxy);
        }
        if (mMmsPort != null) {
            prefs.removePreference((EditTextPreference) mMmsPort);
        }
        if (mAuthType != null) {
            prefs.removePreference((ListPreference) mAuthType);
        }
        if (mApnType != null) {
            prefs.removePreference((EditTextPreference) mApnType);
        }
        if (mProtocol != null) {
            prefs.removePreference((ListPreference) mProtocol);
        }
        if (mRoamingProtocol != null) {
            prefs.removePreference((ListPreference) mRoamingProtocol);
        }
        if (mCarrierEnabled != null) {
            prefs.removePreference((SwitchPreference) mCarrierEnabled);
        }
        if (mBearerMulti != null) {
            prefs.removePreference((MultiSelectListPreference) mBearerMulti);
        }
        if (mMvnoType != null) {
            prefs.removePreference((ListPreference) mMvnoType);
        }
        if (mMvnoMatchData != null) {
            prefs.removePreference((EditTextPreference) mMvnoMatchData);
        }
    }
    // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742

    // +++ Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
    private void resetPreferApn() {
        Log.d(TAG,"resetPreferApn()");
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(ApnSettings.APN_ID, "");
        try {
            resolver.update(PREFERAPN_URI, values, null, null);
        } catch (Exception e) {
            Log.d(TAG,"resetPreferApn() : update PREFERAPN_URI fail");
        }
    }

    private boolean isPreferApn() {
        String key = null;
        String id = String.valueOf(ContentUris.parseId(mUri));

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(PREFERAPN_URI, new String[] {"_id"},
                    null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
            if (cursor == null) {
                return false;
            }

            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                key = cursor.getString(ID_INDEX);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        Log.d(TAG, "isPreferApn() key = " + key + ", id = " + id);
        if ((key != null) && key.equals(id)) {
            return true;
        }
        return false;
    }
    // --- Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
// --- AMAX
}
