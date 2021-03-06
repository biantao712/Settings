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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.MoveCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.storage.DiskInfo;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.settings.R;

import static android.content.pm.PackageManager.EXTRA_MOVE_ID;
import static com.android.settings.deviceinfo.StorageSettings.TAG;

public class StorageWizardMigrateProgress extends StorageWizardBase {
    private static final String ACTION_FINISH_WIZARD = "com.android.systemui.action.FINISH_WIZARD";

    private int mMoveId;

    private SharedPreferences mValidSd;
    private static final String SP_VALID_CARD = "valid_cards";
    private final String KEY_MOVE_ID ="key_mode_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mVolume == null) {
            finish();
            return;
        }

        //sharedpreference to record info about valid sd cards.
        mValidSd = getSharedPreferences(SP_VALID_CARD, 0);

        setContentView(R.layout.storage_wizard_progress);

        mMoveId = getIntent().getIntExtra(EXTRA_MOVE_ID, -1);

        final String descrip = mStorage.getBestVolumeDescription(mVolume);
        setIllustrationType(ILLUSTRATION_INTERNAL);
        setHeaderText(R.string.storage_wizard_migrate_progress_title, descrip);
        setBodyText(R.string.storage_wizard_migrate_details, descrip);

        getNextButton().setVisibility(View.GONE);

        if(mMoveId == Integer.MIN_VALUE) {
            //no enought storage, cancel the move progress
            mCallback.onStatusChanged(mMoveId, PackageManager.MOVE_FAILED_INSUFFICIENT_STORAGE, -1);
        } else {
            // Register for updates and push through current status
            getPackageManager().registerMoveCallback(mCallback, new Handler());
            mCallback.onStatusChanged(mMoveId, getPackageManager().getMoveStatus(mMoveId), -1);
        }
    }

    private final MoveCallback mCallback = new MoveCallback() {
        @Override
        public void onStatusChanged(int moveId, int status, long estMillis) {
            if (mMoveId != moveId) return;

            final Context context = StorageWizardMigrateProgress.this;
            if (PackageManager.isMoveStatusFinished(status)) {
                Log.d(TAG, "Finished with status " + status);
                mValidSd.edit().remove(KEY_MOVE_ID).commit();
                if (status == PackageManager.MOVE_SUCCEEDED) {
                    if (mDisk != null) {
                        // Kinda lame, but tear down that shiny finished
                        // notification, since user is still in wizard flow
                        final Intent finishIntent = new Intent(ACTION_FINISH_WIZARD);
                        finishIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                        sendBroadcast(finishIntent);

                        if (!StorageWizardMigrateProgress.this.isFinishing()) {
                            final Intent intent = new Intent(context, StorageWizardReady.class);
                            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
                            startActivity(intent);
                        }
                    }
                } else {
                    Toast.makeText(context, getString(R.string.insufficient_storage),
                            Toast.LENGTH_LONG).show();
                }
                finishAffinity();

            } else {
                setCurrentProgress(status);
            }
        }
    };
}
