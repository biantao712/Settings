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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.settings.R;

public class CNStorageWizardFormatConfirm extends CNStorageWizardBase {
    public static final String EXTRA_FORMAT_PRIVATE = "format_private";
    public static final String EXTRA_DATA_MIGRATION = "data_migration";
    public static final String EXTRA_FORGET_UUID = "forget_uuid";
    public static final String EXTRA_UNSUPPORTED_CARD = "unsupported_card";

    private boolean mFormatPrivate;
    private boolean mMigrateData;
    private boolean mUnsupportedCard;

    private Button mConfirmButton;

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

        setContentView(R.layout.cn_storage_wizard_format_confirm);

        mConfirmButton = (Button)findViewById(R.id.confirm_button);
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initDialog();
            }
        });

    }

    private void  initDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setPositiveButton(R.string.lockpattern_confirm_button_text,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        formatDisk();
                    }
                });
        builder.setNegativeButton(R.string.cancel, null);

        View view1 = LayoutInflater.from(builder.getContext()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
        TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
        title.setText(getResources().getString(R.string.format_title));
        TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
        String text = getResources().getString(R.string.format_dialog_content);
        message.setText(text);

        builder.setView(view1);
        AlertDialog alertDialog1 = builder.show();
//            alertDialog1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        Window dialogWindow = alertDialog1.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        dialogWindow.setAttributes(lp);
    }

    private void formatDisk(){
        final Intent intent = new Intent(this, CNStorageWizardFormatProgress.class);
        intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
        intent.putExtra(EXTRA_FORMAT_PRIVATE, mFormatPrivate);
        intent.putExtra(EXTRA_FORGET_UUID, getIntent().getStringExtra(EXTRA_FORGET_UUID));
        intent.putExtra(EXTRA_DATA_MIGRATION, mMigrateData);
        startActivity(intent);
        finishAffinity();
    }

}
