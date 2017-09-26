package com.asus.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothPairedReceiver extends BroadcastReceiver {

    private static final String TAG = "BluetoothPairedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED) && BluetoothPairedUtil.supportToPairWithBtDock()) {
            if (BluetoothAdapter.STATE_ON == intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                Log.v(TAG, "Received BLUETOOTH_STATE_CHANGED_ACTION, BLUETOOTH_STATE_ON");
                Intent in = new Intent(context, BluetoothPairedService.class);
                context.startService(in);
            }
        }
    }
}