/*
 * Copyright 2012, ASUSTeK Computer Inc. All rights reserved.
 *
 * mikesc_huang@20121009
 */

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.provider.Settings;
import android.content.Context;

/**
 * Pop up an alert dialog to inform user of that there is
 * no network connection
 */
public class AsusNoNetworkDialog extends Activity {
    private AlertDialog mDialog;

    // Positive button
    private OnClickListener settingsButton = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivity(intent); // Go to Settings
            AsusNoNetworkDialog.this.finish();
        }
    };

    // Negative button
    private OnClickListener cancelButton = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            AsusNoNetworkDialog.this.finish();
        }
    };

    // BACK key pressed listerner
    private OnCancelListener canceled = new OnCancelListener() {
        public void onCancel(DialogInterface dialog) {
            AsusNoNetworkDialog.this.finish();
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if(mDialog == null) {
            showDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDialog.dismiss();
        mDialog = null;
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void showDialog() {
        mDialog = new AlertDialog.Builder(this).create();
        mDialog.setTitle(getString(R.string.load_suspended_title));
        mDialog.setMessage(getString(R.string.load_suspended));
        mDialog.setButton(DialogInterface.BUTTON_POSITIVE,
                getString(R.string.button_label_settings), settingsButton);
        mDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                getString(R.string.button_label_cancel), cancelButton);
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.setOnCancelListener(canceled); // Register OnCancelListener to handle BACK key pressed
        mDialog.show();
    }
}
