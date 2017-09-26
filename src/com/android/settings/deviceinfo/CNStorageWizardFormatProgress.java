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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;

import java.util.Objects;

import static com.android.settings.deviceinfo.StorageSettings.TAG;

public class CNStorageWizardFormatProgress extends CNStorageWizardBase {
    private static final String TAG_SLOW_WARNING = "slow_warning";
    private static final boolean DEBUG=false;

    private boolean mFormatPrivate;
    private boolean mMigrateData;

    private static final String STATE_UNSUPPORT_SD = "unsupportedSD";
    private boolean mForUnsupportSD;

    private PartitionTask mTask;

    //clean valid scard list
    private static final String SP_VALID_CARD = "valid_cards";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }
        setContentView(R.layout.cn_storage_wizard_format_progress);
//        setKeepScreenOn(true);

        mFormatPrivate = getIntent().getBooleanExtra(
                StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);

        mMigrateData = getIntent().getBooleanExtra(
                StorageWizardFormatConfirm.EXTRA_DATA_MIGRATION, false);


        if(DEBUG) Log.d(TAG, "Format Info:\n"+mDisk+"\n\n"+mVolume);
        if (savedInstanceState != null) {
            mForUnsupportSD = savedInstanceState.getBoolean(STATE_UNSUPPORT_SD);
        } else if (mVolume == null) {
            VolumeInfo volInfo = null;
            for (VolumeInfo info : mStorage.getVolumes()) {
                if (Objects.equals(info.getDiskId(), mDisk.getId())) {
                    volInfo = info;
                    break;
                }
            }
            mForUnsupportSD = (volInfo == null);
        }

        mTask = (PartitionTask) getLastNonConfigurationInstance();
        if (mTask == null) {
            mTask = new PartitionTask();
            mTask.setActivity(this);
            mTask.execute();
        } else {
            mTask.setActivity(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // save unsupportSD state when rotate
        savedInstanceState.putBoolean(STATE_UNSUPPORT_SD, mForUnsupportSD);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mTask;
    }

    public static class PartitionTask extends AsyncTask<Void, Integer, Exception> {
        public CNStorageWizardFormatProgress mActivity;

        private volatile int mProgress = 20;

        //private volatile long mInternalBench;
        //private volatile long mPrivateBench;

        @Override
        protected Exception doInBackground(Void... params) {
            final CNStorageWizardFormatProgress activity = mActivity;
            final StorageManager storage = mActivity.mStorage;
            try {
                if (activity.mFormatPrivate) {
                    storage.partitionPrivate(activity.mDisk.getId());
                    //publishProgress(40);

                    //mInternalBench = storage.benchmark(null);
                    publishProgress(60);

                    //add retry
                    VolumeInfo privateVol = null;
                    for (int i = 0; i<3; i++) {
                        privateVol = activity.findFirstVolume(VolumeInfo.TYPE_PRIVATE);
                        if (privateVol != null) break;
                    }
                    //mPrivateBench = storage.benchmark(privateVol.getId());

                    // If we just adopted the device that had been providing
                    // physical storage, then automatically move storage to the
                    // new emulated volume.
                    if (activity.mDisk.isDefaultPrimary()
                            && Objects.equals(storage.getPrimaryStorageUuid(),
                                    StorageManager.UUID_PRIMARY_PHYSICAL)) {
                        Log.d(TAG, "Just formatted primary physical; silently moving "
                                + "storage to new emulated volume");
                        storage.setPrimaryStorageUuid(privateVol.getFsUuid(), new SilentObserver());
                    }

                } else {
                    storage.partitionPublic(activity.mDisk.getId());
                }
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgress = progress[0];
            mActivity.setCurrentProgress(mProgress);
        }

        public void setActivity(CNStorageWizardFormatProgress activity) {
            mActivity = activity;
            mActivity.setCurrentProgress(mProgress);
        }

        @Override
        protected void onPostExecute(Exception e) {
            final CNStorageWizardFormatProgress activity = mActivity;
            if (activity.isDestroyed()) {
                return;
            }

            if (e != null) {
                Log.e(TAG, "Failed to partition", e);
                Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                activity.finishAffinity();
                return;
            }

            if (activity.mFormatPrivate) {
                //final float pct = (float) mInternalBench / (float) mPrivateBench;
                //Log.d(TAG, "New volume is " + pct + "x the speed of internal");

                // To help set user expectations around device performance, we
                // warn if the adopted media is 0.25x the speed of internal
                // storage or slower.
                /*
                //AOSP check
                if (Float.isNaN(pct) || pct < 0.25) {
                    final SlowWarningFragment dialog = new SlowWarningFragment();
                    dialog.showAllowingStateLoss(activity.getFragmentManager(), TAG_SLOW_WARNING);
                } else {
                    activity.onFormatFinished();
                } */
                //clean valid card list
                //sharedpreference to record valid sd cards.
                SharedPreferences validSd = activity.getSharedPreferences(SP_VALID_CARD, 0);
                validSd.edit().clear().commit();
                activity.onFormatFinished();
            } else {
                activity.onFormatFinished();
            }
        }
    }

    public static class SlowWarningFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);

            final CNStorageWizardFormatProgress target =
                    (CNStorageWizardFormatProgress) getActivity();
            final String descrip = target.getDiskDescription();
            final String genericDescip = target.getGenericDiskDescription();
            builder.setMessage(TextUtils.expandTemplate(getText(R.string.storage_wizard_slow_body),
                    descrip, genericDescip));

            builder.setCancelable(false);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final CNStorageWizardFormatProgress target =
                            (CNStorageWizardFormatProgress) getActivity();
                    target.onFormatFinished();
                }
            });

            return builder.create();
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

    private void onFormatFinished() {
        final String forgetUuid = getIntent().getStringExtra(
                StorageWizardFormatConfirm.EXTRA_FORGET_UUID);
        if (!TextUtils.isEmpty(forgetUuid)) {
            mStorage.forgetVolume(forgetUuid);
        }

        final boolean offerMigrate;
        if (mFormatPrivate) {
            // Offer to migrate only if storage is currently internal
            final VolumeInfo privateVol = getPackageManager()
                    .getPrimaryStorageCurrentVolume();
            offerMigrate = (privateVol != null
                    && VolumeInfo.ID_PRIVATE_INTERNAL.equals(privateVol.getId()));
        } else {
            offerMigrate = false;
        }

        if (mForUnsupportSD) {
            //goto storagewizardinit
            final Intent intent = new Intent(this, StorageWizardInit.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.id);
            startActivity(intent);
        } else if (offerMigrate && mMigrateData) {
            //final Intent intent = new Intent(this, StorageWizardMigrate.class);
            //intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            //startActivity(intent);

            // When called with just disk, find the first private volume
            while (mVolume == null || mVolume.getState() != VolumeInfo.STATE_MOUNTED) {
                //may need to wait for mounting progress done
                if(DEBUG) Log.d(TAG, "onFormatFinished, mVolume:"+mVolume);
                mVolume = this.findFirstVolume(VolumeInfo.TYPE_PRIVATE);
            }
            final int moveId = getPackageManager().movePrimaryStorage(mVolume);

            final Intent intent = new Intent(this, StorageWizardMigrateProgress.class);
            intent.putExtra(VolumeInfo.EXTRA_VOLUME_ID, mVolume.getId());
            intent.putExtra(PackageManager.EXTRA_MOVE_ID, moveId);
            startActivity(intent);
        } else {
            final Intent intent = new Intent(this, CNStorageWizardReady.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            startActivity(intent);
        }
        finishAffinity();
    }

    private static class SilentObserver extends IPackageMoveObserver.Stub {
        @Override
        public void onCreated(int moveId, Bundle extras) {
            // Ignored
        }

        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            // Ignored
        }
    }
}
