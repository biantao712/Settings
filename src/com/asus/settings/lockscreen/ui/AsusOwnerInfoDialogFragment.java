/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.asus.settings.lockscreen.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.settings.R;

import com.android.settings.util.RemoveSpecialCharFilter;
import com.android.settings.util.Utf8ByteLengthFilter;
import android.graphics.Color;

import android.os.UserHandle;
import com.android.internal.widget.LockPatternUtils;
import android.content.BroadcastReceiver;


/**
 * Dialog fragment for edit device owner info.
 */
public final class AsusOwnerInfoDialogFragment extends DialogFragment implements TextWatcher {
    private static final int BLUETOOTH_NAME_MAX_LENGTH_BYTES = 40;

    private AlertDialog mAlertDialog;
    private Button mOkButton;

    private int mUserId;
    boolean mIsDeviceOwnerInfoEnabled = false;
    private LockPatternUtils mLockPatternUtils;

    // accessed from inner class (not private to avoid thunks)
    static final String TAG = "AsusOwnerInfo_NF";
    EditText mDeviceOwnerInfoView;

    // This flag is set when the name is updated by code, to distinguish from user changes
    private boolean mDeviceOwnerInfoUpdated = false;

    // This flag is set when the user edits the name (preserved on rotation)
    private boolean mDeviceOwnerInfoEdited = false;

    // Key to save the edited name and edit status for restoring after rotation
    private static final String KEY_DEVICE_INFO = "device_info";
    private static final String KEY_DEVICE_INFO_EDITED = "device_name_edited";
    private static final String ACTION_UPDATE_DEVICE_OWNER_INFO = "ACTION_UPDATE_DEVICE_OWNER_INFO";

    public AsusOwnerInfoDialogFragment() {
        Log.d(TAG,"Enter Constructor");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"enter oncreate ");
        Log.d(TAG,"init lock pattern utils ");

        mUserId = UserHandle.myUserId();

        mLockPatternUtils = new LockPatternUtils(getActivity());
        if(mLockPatternUtils == null)
        {
            Log.d(TAG,"ERROR:: lock pattenr utils is null");
        }
        else
        {
            boolean status = mLockPatternUtils.isOwnerInfoEnabled(mUserId);
            Log.d(TAG,"get owner info enable status default is :" +status);
            Log.d(TAG,"force set dievce info enabled true");
            status = true;
            mLockPatternUtils.setOwnerInfoEnabled(status, mUserId);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String deviceOwnerInfo = getDeviceOwnerInfo();
        Log.d(TAG,"device owner info get is :" +deviceOwnerInfo);
        if (savedInstanceState != null) {
            deviceOwnerInfo = savedInstanceState.getString(KEY_DEVICE_INFO, deviceOwnerInfo);
            mDeviceOwnerInfoEdited = savedInstanceState.getBoolean(KEY_DEVICE_INFO_EDITED, false);
            Log.d(TAG,"save instance device info is :" +deviceOwnerInfo);
            Log.d(TAG,"is device owner info edited :" + mDeviceOwnerInfoEdited);
        }

        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setView(createDialogView(deviceOwnerInfo))
                .setPositiveButton(R.string.device_owner_info_positive_button_title,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG,"positive button clicked");
                                String deviceOwnerInfo = mDeviceOwnerInfoView.getText().toString().trim();
                                setDeviceOwnerInfo(deviceOwnerInfo);
                                Intent intent = new Intent(ACTION_UPDATE_DEVICE_OWNER_INFO);
                                getActivity().sendBroadcast(intent);
                                Log.d(TAG,"Send update device owner info broadcast");
                            }
                        })
                .setNegativeButton(R.string.device_owner_info_negative_button_title, null)
                .create();

        return mAlertDialog;
    }

    private void setDeviceOwnerInfo(String deviceOwnerInfo) {
        Log.d(TAG, "Setting device Info to " + deviceOwnerInfo);
        mLockPatternUtils.setOwnerInfo(deviceOwnerInfo, mUserId);

        // get deivce owner info again to verify

        Log.d(TAG,"get owner info again :");
        mIsDeviceOwnerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(mUserId);
        String deviceOwnerInfoStr = mLockPatternUtils.getOwnerInfo(mUserId);
        Log.d(TAG,"user id :" + mUserId);
        Log.d(TAG,"is device info enabled :" + mIsDeviceOwnerInfoEnabled);
        Log.d(TAG,"devcie info string :" + deviceOwnerInfoStr);
    }

    private String getDeviceOwnerInfo()
    {
        try{
            mIsDeviceOwnerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(mUserId);
            String deviceOwnerInfoStr = mLockPatternUtils.getOwnerInfo(mUserId);
            Log.d(TAG,"user id :" + mUserId);
            Log.d(TAG,"is device info enabled :" + mIsDeviceOwnerInfoEnabled);
            Log.d(TAG,"devcie info string :" + deviceOwnerInfoStr);

            if(deviceOwnerInfoStr == null)
            {
                Log.d(TAG,"devcie owner info is null force set to empty");
                deviceOwnerInfoStr = "";
            }

            return deviceOwnerInfoStr;
        }
        catch (Exception ee)
        {
            Log.d(TAG,"Error of get Device Owner Info: " + ee);
            return "";
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_DEVICE_INFO, mDeviceOwnerInfoView.getText().toString());
        outState.putBoolean(KEY_DEVICE_INFO_EDITED, mDeviceOwnerInfoEdited);
    }

    private View createDialogView(String deviceOwnerInfo) {
        try{
            Log.d(TAG,"enter createDialogView");
            final LayoutInflater layoutInflater = (LayoutInflater)getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view =  layoutInflater.inflate(R.layout.cnasusres_alertdialog_textfield_with_title, null);
            TextView title = (TextView) view.getRootView().findViewById(R.id.alertdialog_title);
            title.setText(getActivity().getResources().getString(R.string.device_owner_info_title));

            mDeviceOwnerInfoView = (EditText) view.getRootView().findViewById(R.id.alertdialog_textfield);
            mDeviceOwnerInfoView.setFilters(new InputFilter[] {new Utf8ByteLengthFilter(248), new RemoveSpecialCharFilter(false)});
            mDeviceOwnerInfoView.setText(deviceOwnerInfo == null?"":deviceOwnerInfo, TextView.BufferType.EDITABLE);
            //mDeviceOwnerInfoView.setSelection(deviceOwnerInfo.length());
            mDeviceOwnerInfoView.setPadding(0,0,0,0);

            return view;
        }
        catch(Exception ee)
        {
            Log.d(TAG,"Error in createDialogView :" + ee);
            return null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAlertDialog = null;
        mDeviceOwnerInfoView = null;
        mOkButton = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"enter onresume");
        if (mOkButton == null) {
            boolean status = mDeviceOwnerInfoView.getText().toString().trim().length() != 0;
            Log.d(TAG,"button is not null ,set button enable to : " + status);
            mOkButton = mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            //mOkButton.setEnabled(status);    // Ok button enabled after user edits
            mOkButton.setEnabled(true);
        }

        mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.parseColor("#03bed4"));
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"Enter onPause");
    }

    void updateDeviceOwnerInfo() {
        Log.d(TAG,"enter updateDevcieOwnerInfo");
    }

    public void afterTextChanged(Editable s) {
        Log.d(TAG,"enter after text chagned.");
        if (mDeviceOwnerInfoUpdated) {
            Log.d(TAG,"device info updated");
            mDeviceOwnerInfoUpdated = false;
            //mOkButton.setEnabled(false);
            mOkButton.setEnabled(true);
        } else {
            Log.d(TAG,"device info not update.");
            mDeviceOwnerInfoEdited = true;
            if (mOkButton != null) {
                //mOkButton.setEnabled(s.toString().trim().length() != 0);
                mOkButton.setEnabled(true);
            }
        }
    }

    public void onConfigurationChanged(Configuration newConfig, CharSequence s) {
        Log.d(TAG,"onConfiguration chagned. ");
        super.onConfigurationChanged(newConfig);
        if (mOkButton != null) {
            Log.d(TAG,"button is not null");
            //mOkButton.setEnabled(s.length() != 0 && !(s.toString().trim().isEmpty()));
            mOkButton.setEnabled(true);
        }
        else
        {
            Log.d(TAG,"Button is null");
        }
    }

    /* Not used */
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    /* Not used */
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }
}
