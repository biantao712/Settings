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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;

import static com.android.settings.deviceinfo.StorageSettings.TAG;
import com.android.settings.R;

import java.util.Objects;

public class StorageWizardIntInit extends StorageWizardBase {
    private RadioButton mRadioInternalApps;
    private RadioButton mRadioInternalAppsData;
    private RadioButton mInitState = null;
    private boolean mIsAvailTargetStorage = true;
    private SharedPreferences mValidSd;
    private static final String SP_VALID_CARD = "valid_cards";
    static protected float mBenchMarkThreshold;
    private final String KEY_MOVE_ID ="key_mode_id";

    private BenchMarkTask mTask;
    ProgressDialogFragment mProgressDialog = null;
    SlowWarningFragment mSlowDialog = null;
    static private final int SLOW_CARD = -1;

    private static final String TAG_BENCHMARK = TAG+".benchmarking";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.storage_wizard_init_internal);

        setIllustrationType(ILLUSTRATION_INTERNAL);
        setHeaderText(R.string.storage_wizard_init_title, mDisk.getDescription());

        mRadioInternalApps = (RadioButton) findViewById(R.id.storage_wizard_init_internal_app_only);
        mRadioInternalAppsData = (RadioButton) findViewById(R.id.storage_wizard_init_internal_title);

        mRadioInternalApps.setOnCheckedChangeListener(mRadioListener);
        mRadioInternalAppsData.setOnCheckedChangeListener(mRadioListener);

        findViewById(R.id.storage_wizard_init_external_summary).setPadding(
                mRadioInternalApps.getCompoundPaddingLeft(), 0,
                mRadioInternalApps.getCompoundPaddingRight(), 0);
        findViewById(R.id.storage_wizard_init_internal_summary).setPadding(
                mRadioInternalApps.getCompoundPaddingLeft(), 0,
                mRadioInternalApps.getCompoundPaddingRight(), 0);

        //Check Storage Size
        if(AsusStorageUtils.getEmmcStorage(mStorage).getPath().getTotalSpace() > mDisk.size){
            //if sd size too small, not allow to migrate data
            Log.i(TAG, "SD card size is too small to set as primary storage. ("+mDisk.size+")");
            mRadioInternalAppsData.setEnabled(false);
        }

        //sharedpreference to record valid sd cards.
        mValidSd = getSharedPreferences(SP_VALID_CARD, 0);

        //check data migration status, if exist moving progress go to migrate progess directly
        int moveId = mValidSd.getInt(KEY_MOVE_ID, -1);
        if(moveId != -1) {
            final Intent intent = new Intent(this, StorageWizardMigrateProgress.class);
            intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
            intent.putExtra(PackageManager.EXTRA_MOVE_ID, moveId);
            startActivity(intent);
            finishAffinity();
        }

        //Both 2 choice are Adoptable Storage
        setIllustrationType(ILLUSTRATION_INTERNAL);

        getNextButton().setEnabled(false);

        //bench mark threshold, allow project adjust
        mBenchMarkThreshold = Float.valueOf(SystemProperties.get("ro.config.sd_threshold", "0.25")).floatValue();
        Log.i(TAG_BENCHMARK, "current bench mark threshold is "+mBenchMarkThreshold);
        //AsyncTask for doing Bench mark
        mTask = (BenchMarkTask) getLastNonConfigurationInstance();
        if(mTask == null){
            mTask = new BenchMarkTask();
        }
        mTask.setActivity(this);
        //To init the UI
        // 1.change mode process for adoptable SD
        // 2.BenchMark for Portable SD.
        final String primary = mStorage.getPrimaryStorageUuid();
        if (mVolume!=null && mVolume.getType() == VolumeInfo.TYPE_PRIVATE) {
            if (mVolume.getFsUuid().equals(primary)){
                mRadioInternalAppsData.setChecked(true);
                mRadioInternalApps.setChecked(false);
                mInitState = mRadioInternalAppsData;
                mIsAvailTargetStorage = AsusStorageUtils.isMigrateTargetAvailable(
                        mStorage, AsusStorageUtils.findInternalVolumeNotPrimary(mStorage.getVolumes(), mVolume.getFsUuid()));
            } else {
                mRadioInternalAppsData.setChecked(false);
                mRadioInternalApps.setChecked(true);
                mInitState = mRadioInternalApps;
                mIsAvailTargetStorage = AsusStorageUtils.isMigrateTargetAvailable(mStorage, mVolume);
            }
            getNextButton().setEnabled(false);
        } else {
            //is valid cards?
            VolumeInfo volInfo = null;
            for (VolumeInfo info : mStorage.getVolumes()) {
                if (Objects.equals(info.getDiskId(), mDisk.getId())) {
                    volInfo = info;
                    break;
                }
            }

            if((!mValidSd.getBoolean(volInfo.getFsUuid(), false) && !mTask.isRunning() && mSlowDialog==null)) {
                mTask.execute();
                showProcessingDialog();
            } else {
                if(mTask.isRunning()) {
                    Log.i(TAG_BENCHMARK, volInfo.getFsUuid() + " is doing benchmark.");
                    showProcessingDialog();
                } else
                    Log.i(TAG_BENCHMARK, volInfo.getFsUuid()+" is benched card");
            }

        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mTask;
    }

    private final OnCheckedChangeListener mRadioListener = new OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (buttonView == mRadioInternalApps) {
                    mRadioInternalAppsData.setChecked(false);
                    //setIllustrationInternal(false);
                } else if (buttonView == mRadioInternalAppsData) {
                    mRadioInternalApps.setChecked(false);
                    //setIllustrationInternal(true);
                }
                if (buttonView.equals(mInitState)) {
                    getNextButton().setEnabled(false);
                } else {
                    getNextButton().setEnabled(true);
                }
            }
        }
    };

    @Override
    public void onNavigateNext() {
        if (mInitState != null) {
            VolumeInfo target = null;
            if (mInitState == mRadioInternalApps){
            //move to SD
                target = mVolume;
            } else {
            //move to internal storage
                //target = findFirstVolume(VolumeInfo.TYPE_PRIVATE);
                /*
                for (VolumeInfo vol : mStorage.getVolumes()) {
                    if ((vol.getType() == VolumeInfo.TYPE_PRIVATE) &&
                            !Objects.equals(vol.getFsUuid(), mVolume.getFsUuid())) {
                        target = vol;
                        break;
                    }
                }
                */
                target = AsusStorageUtils.findInternalVolumeNotPrimary(mStorage.getVolumes(), mVolume.getFsUuid());
            }

            final int moveId;
            //storage size check
            //if(!AsusStorageUtils.isMigrateTargetAvailable(mStorage, target)){
            if(!mIsAvailTargetStorage){
                moveId = Integer.MIN_VALUE;
            } else {
                moveId = getPackageManager().movePrimaryStorage(target);
                mValidSd.edit().putInt(KEY_MOVE_ID, moveId).commit();
            }

            final Intent intent = new Intent(this, StorageWizardMigrateProgress.class);
            intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
            intent.putExtra(PackageManager.EXTRA_MOVE_ID, moveId);
            startActivity(intent);
            finishAffinity();
        } else {
            final Intent intent = new Intent(this, StorageWizardFormatConfirm.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            if (mRadioInternalApps.isChecked()) {
                intent.putExtra(StorageWizardFormatConfirm.EXTRA_DATA_MIGRATION, false);
            } else if (mRadioInternalAppsData.isChecked()) {
                intent.putExtra(StorageWizardFormatConfirm.EXTRA_DATA_MIGRATION, true);
            }
            intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, true);
            startActivity(intent);
        }
    }

    public static class SlowWarningFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);

            final StorageWizardIntInit target =
                    (StorageWizardIntInit) getActivity();
            final String descrip = target.getDiskDescription();
            final String genericDescip = target.getGenericDiskDescription();
            builder.setMessage(
                    TextUtils.expandTemplate(getText(R.string.storage_wizard_slow_body),
                    descrip, genericDescip).toString()
            );

            builder.setCancelable(false);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.i(TAG,"dismiss SlowWarningDialog");
                }
            });

            return builder.create();
        }
    }

    public static class ProgressDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final ProgressDialog dialog = new ProgressDialog(context);
            dialog.setTitle(R.string.storage_wizard_benchmark_tittle);
            dialog.setMessage(getString(R.string.storage_wizard_benchmark_content));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            return dialog;
        }
    }

    public static class BenchMarkTask extends AsyncTask<Void, Integer, Exception> {

        private StorageWizardIntInit mActivity;
        private volatile long mInternalBench;
        private volatile long mExternalBench;
        private boolean mRunning = false;
        private boolean mSlowCard = false;
        //private ProgressDialogFragment mDialog;

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mRunning = true;
                Log.i(TAG_BENCHMARK, "BenchMark Starting...");
                final StorageManager storage = mActivity.mStorage;
                mInternalBench = storage.benchmark(null);
                VolumeInfo volInfo = null;
                for (VolumeInfo info : storage.getVolumes()) {
                    if (Objects.equals(info.getDiskId(), mActivity.mDisk.getId())) {
                        volInfo = info;
                        break;
                    }
                }
                mExternalBench = storage.benchmark(volInfo.getId());
                final float pct = (float) mInternalBench / (float) mExternalBench;
                Log.d(TAG_BENCHMARK, mInternalBench + "/" + mExternalBench + "=" + pct);
                if (Float.isNaN(pct) || pct < mActivity.mBenchMarkThreshold) {
                    mSlowCard = true;
                } else {
                    //store the sdcard as valid card
                    mActivity.storeValidCard(volInfo.getFsUuid());
                }
                return null;
            } catch(Exception e){
                return e;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Exception e) {
            super.onPostExecute(e);
            mRunning = false;
            mActivity.onBenchMarkComplete(mSlowCard?SLOW_CARD:0);
        }

        public void setActivity(StorageWizardIntInit activity){
            mActivity = activity;
        }

        public boolean isRunning(){
            return mRunning;
        }
    }

    private void storeValidCard(String fsUuid) {
        mValidSd.edit().putBoolean(fsUuid, true).commit();
    }

    private void showProcessingDialog() {
        //progress dialog
        if(mProgressDialog == null) {
            mProgressDialog = new ProgressDialogFragment();
            mProgressDialog.setCancelable(false);
        }
        if(!mProgressDialog.isVisible()) mProgressDialog.showAllowingStateLoss(getFragmentManager(), TAG_BENCHMARK);
    }

    private void onBenchMarkComplete(int status) {
        mProgressDialog.dismissAllowingStateLoss();
        mProgressDialog = null;
        if(status == SLOW_CARD){
            //slow dialog
            if(mSlowDialog == null) {
                mSlowDialog = new SlowWarningFragment();
                mSlowDialog.setCancelable(false);
                mSlowDialog.showAllowingStateLoss(getFragmentManager(), TAG_BENCHMARK);
            }
        }
    }

    private String getDiskDescription() {
        return mDisk.getDescription();
    }

    private String getGenericDiskDescription() {
        // TODO: move this directly to DiskInfo
        if (mDisk.isSd()) {
            return getString(com.android.internal.R.string.storage_sd_card);
        } else if (mDisk.isUsb()) {
            return getString(com.android.internal.R.string.storage_usb_drive);
        } else {
            return null;
        }
    }
}
