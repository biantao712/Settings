package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

public class AsusEasyLauncherDialog extends DialogFragment{
    public static final String START_FROM_EZMODE = "START_FROM_EZMODE";
    public static AsusEasyLauncherDialog newInstance() {
        return new AsusEasyLauncherDialog();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.ezmode_enable_title)
            .setMessage(R.string.ezmode_enable_description)
            .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String packageName = "com.asus.easylauncher";
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.fromParts("package", packageName, null));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    intent.putExtra(START_FROM_EZMODE, true);
                    startActivity(intent);
            }})
            .setNegativeButton(R.string.lockpattern_tutorial_cancel_label, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dismiss();
            }})
            .create();
            }
    }