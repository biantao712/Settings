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

package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.*;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.android.settings.R;

import java.util.Objects;

public class StorageWizardInit extends StorageWizardBase {
    private RadioButton mRadioExternal;
    private RadioButton mRadioInternal;

    private boolean mIsPermittedToAdopt;
    //SP for recording the sd warning dialog do not show again config.
    static final private String SP_KEY_SDWARN_DONOTAGAIN = "sd_warning_dont_again";
    boolean isShowSDWarnDialog = true;

    //dialog for show the notice about SD card for user
    SdWarningFragment mDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }

        //unsupport sd card(may be the adoptable storage for other device)
        //ask user to format as portable first
        if(mVolume == null) {
            VolumeInfo volInfo = null;
            for (VolumeInfo info : mStorage.getVolumes()) {
                if (Objects.equals(info.getDiskId(), mDisk.getId())) {
                    volInfo = info;
                    break;
                }
            }
            if(volInfo == null) {
                final Intent intent = new Intent(this, StorageWizardFormatConfirm.class);
                intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
                intent.putExtra(StorageWizardFormatConfirm.EXTRA_UNSUPPORTED_CARD, true);
                startActivity(intent);
                finishAffinity();
            }
        }

        setContentView(R.layout.storage_wizard_init);

        mIsPermittedToAdopt = UserManager.get(this).isAdminUser()
                && !ActivityManager.isUserAMonkey();

        setIllustrationType(ILLUSTRATION_SETUP);
        setHeaderText(R.string.storage_wizard_init_title, mDisk.getDescription());

        mRadioExternal = (RadioButton) findViewById(R.id.storage_wizard_init_external_title);
        mRadioInternal = (RadioButton) findViewById(R.id.storage_wizard_init_internal_title);

        mRadioExternal.setOnCheckedChangeListener(mRadioListener);
        mRadioInternal.setOnCheckedChangeListener(mRadioListener);

        findViewById(R.id.storage_wizard_init_external_summary).setPadding(
                mRadioExternal.getCompoundPaddingLeft(), 0,
                mRadioExternal.getCompoundPaddingRight(), 0);
        findViewById(R.id.storage_wizard_init_internal_summary).setPadding(
                mRadioExternal.getCompoundPaddingLeft(), 0,
                mRadioExternal.getCompoundPaddingRight(), 0);

        getNextButton().setEnabled(false);

        if (!mDisk.isAdoptable()) {
            // If not adoptable, we only have one choice
            mRadioExternal.setChecked(true);
            onNavigateNext();
            finish();
        }

        // TODO: Show a message about why this is disabled for guest and that only an admin user
        // can adopt an sd card.
        if (!mIsPermittedToAdopt) {
            mRadioInternal.setEnabled(false);
        }

        //SD warning dialog shared preference
        isShowSDWarnDialog = !(PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getBoolean(SP_KEY_SDWARN_DONOTAGAIN, false));
        if(isShowSDWarnDialog && mDialog==null) {
            mDialog = new SdWarningFragment();
            mDialog.setRetainInstance(true);
            mDialog.show(getFragmentManager(), "SD_WARNING");
        }
    }

    private final OnCheckedChangeListener mRadioListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (buttonView == mRadioExternal) {
                    mRadioInternal.setChecked(false);
                    setIllustrationType(ILLUSTRATION_PORTABLE);
                } else if (buttonView == mRadioInternal) {
                    mRadioExternal.setChecked(false);
                    setIllustrationType(ILLUSTRATION_INTERNAL);
                }
                getNextButton().setEnabled(true);
            }
        }
    };

    @Override
    public void onNavigateNext() {
        if (mRadioExternal.isChecked()) {
            if (mVolume != null && mVolume.getType() == VolumeInfo.TYPE_PUBLIC
                    && mVolume.getState() != VolumeInfo.STATE_UNMOUNTABLE) {
                // Remember that user made decision
                mStorage.setVolumeInited(mVolume.getFsUuid(), true);

                final Intent intent = new Intent(this, StorageWizardReady.class);
                intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                startActivity(intent);

            } else {
                // Gotta format to get there
                final Intent intent = new Intent(this, StorageWizardFormatConfirm.class);
                intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
                startActivity(intent);
            }

        } else if (mRadioInternal.isChecked()) {
            //final Intent intent = new Intent(this, StorageWizardFormatConfirm.class);
            //intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            //intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, true);
            final Intent intent = new Intent(this, StorageWizardIntInit.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            startActivity(intent);
        }
    }

    /**
     * Asus show alert dialog for high light the sdcard quality do matter.
     */
    public static class SdWarningFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            int targetThemeRes = android.R.style.Theme_DeviceDefault_Light;
            Context mThemeContext = new ContextThemeWrapper(context, targetThemeRes);

            LinearLayout view = new LinearLayout(mThemeContext);
            view.setOrientation(LinearLayout.VERTICAL);
            int padding = 30;
            int padding_in_dp_LR = 13;
            int padding_in_dp_UD = 15;
            final float scale = context.getResources().getDisplayMetrics().density;
            int padding_in_px_LR = (int) (padding_in_dp_LR * scale + 0.5f);
            int padding_in_px_UD = (int) (padding_in_dp_UD * scale + 0.5f);
            view.setPadding(padding_in_px_LR, padding_in_px_UD, padding_in_px_LR, padding_in_px_UD);

            TextView message = new TextView(mThemeContext);
            message.setText(R.string.asus_storage_sd_warning_content);
            message.setTextSize(18);
            message.setTextColor(android.graphics.Color.BLACK);
            view.addView(message);

            final CheckBox checkBox = new CheckBox(mThemeContext);
            checkBox.setText(R.string.wifi_scan_notify_remember_choice);
            checkBox.setTextSize(15);
            checkBox.setTextColor(android.graphics.Color.BLACK);
            view.addView(checkBox);

            ScrollView scrollView = new ScrollView(mThemeContext);
            scrollView.addView(view);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setTitle(R.string.notice);
            builder.setView(scrollView);

            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(checkBox.isChecked()) {//do not show again
                        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().
                                putBoolean(SP_KEY_SDWARN_DONOTAGAIN, true).commit();
                    }
                }
            });

            return builder.create();
        }
    }
}
