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

import android.content.Intent;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.VolumeInfo;
import android.preference.PreferenceManager;
import android.text.format.Formatter;

import com.android.settings.R;

public class StorageWizardFormatConfirm extends StorageWizardBase {
    public static final String EXTRA_FORMAT_PRIVATE = "format_private";
    public static final String EXTRA_DATA_MIGRATION = "data_migration";
    public static final String EXTRA_FORGET_UUID = "forget_uuid";
    public static final String EXTRA_UNSUPPORTED_CARD = "unsupported_card";

    private boolean mFormatPrivate;
    private boolean mMigrateData;
    private boolean mUnsupportedCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mDisk == null) {
            finish();
            return;
        }

        mFormatPrivate = getIntent().getBooleanExtra(EXTRA_FORMAT_PRIVATE, false);

        mMigrateData = getIntent().getBooleanExtra(EXTRA_DATA_MIGRATION, false);
        mUnsupportedCard = getIntent().getBooleanExtra(EXTRA_UNSUPPORTED_CARD, false);

        setContentView(R.layout.storage_wizard_generic);

        setIllustrationType(
                mFormatPrivate ? ILLUSTRATION_INTERNAL : ILLUSTRATION_PORTABLE);

        if (mFormatPrivate) {
            setHeaderText(R.string.storage_wizard_format_confirm_title);
            setBodyTextForCDATA(R.string.storage_wizard_format_confirm_body,
                    mDisk.getDescription());
        } else {
            setHeaderText(R.string.storage_wizard_format_confirm_public_title);
            setBodyTextForCDATA(R.string.storage_wizard_format_confirm_public_body,
                    mDisk.getDescription());
        }

        getNextButton().setText(R.string.storage_wizard_format_confirm_next);
        getNextButton().setBackgroundTintList(getColorStateList(R.color.storage_wizard_button_red));
    }

    @Override
    public void onNavigateNext() {
        final Intent intent = new Intent(this, StorageWizardFormatProgress.class);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
        intent.putExtra(EXTRA_FORMAT_PRIVATE, mFormatPrivate);
        intent.putExtra(EXTRA_FORGET_UUID, getIntent().getStringExtra(EXTRA_FORGET_UUID));
        intent.putExtra(EXTRA_DATA_MIGRATION, mMigrateData);
        startActivity(intent);
        finishAffinity();
    }
}
