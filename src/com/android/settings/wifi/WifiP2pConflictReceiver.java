package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.WindowManager;

import com.android.settings.R;

public class WifiP2pConflictReceiver extends BroadcastReceiver {
    private static final String TAG = "WifiP2pConflictReceiver";
    // Notify user P2P/WFD will disconnect due to SCC (freq conflict)
    private static final String ACTION_WIFI_P2P_FREQ_CONFLICT = "com.asus.wifi.p2p.FREQ_CONFLICT";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(ACTION_WIFI_P2P_FREQ_CONFLICT)) {
            showAlertDialog(context, R.string.proxy_error, R.string.miracast2_reestablish_connection_msg);
        }
    }

    private void showAlertDialog(Context context, int resTitle, int resMsg) {
        AlertDialog alertDialog = new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
            .setIconAttribute(android.R.attr.alertDialogIcon)
            .setTitle(resTitle)
            .setMessage(resMsg)
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
            .create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.show();
    }

}
