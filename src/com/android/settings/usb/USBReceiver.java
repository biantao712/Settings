package com.android.settings.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.SystemProperties;
import android.provider.Settings.Secure;

import com.android.settings.Utils;

/**
 * Care the issue of plugging DAC
 * AOA (Android Open Accessory)
 */
public class USBReceiver extends BroadcastReceiver {
    private static final String SHARED_PREFERENCE_NAME = "stateUSBReceiver";
    private static final String PREFERENCE_LAST_CONNECTED_STATE = "connected";
    private static final String PREFERENCE_HAS_SHOWN = "hasShown";
    private static final String SYSTEM_PROPERTY_FACTORY_ADB_ON = "factory.adbon";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!validation(context, intent)) return;

        boolean connected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
        SharedPreferences stateSharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCE_NAME, 0);
        boolean lastConnectedState = stateSharedPreferences.getBoolean(
                PREFERENCE_LAST_CONNECTED_STATE, false);
        SharedPreferences.Editor editor = stateSharedPreferences.edit();
        if (connected && !lastConnectedState) {
            // Uncomment it for debugging (thus the dialog can be shown)
            //editor.putBoolean(PREFERENCE_HAS_SHOWN, false).commit();
            // Replace this line for debugging using non-Verizon devices
            //if (!Utils.isVerizonSKU()) {
            if (Utils.isVerizonSKU()) {
                if (stateSharedPreferences.getBoolean(PREFERENCE_HAS_SHOWN, false)) return;
                context.startActivity(new Intent(context, HintUSBOptions.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                context.startActivity(new Intent(context, USBChangeConfigDialog.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            editor.putBoolean(PREFERENCE_HAS_SHOWN, true).commit();
            // Uncomment it for debugging (thus the dialog can be shown)
            //editor.putBoolean(PREFERENCE_HAS_SHOWN, false).commit();
        }
        editor.putBoolean(PREFERENCE_LAST_CONNECTED_STATE, connected).commit();
    }

    /**
     * Return false: do not show the dialog (bad inputs or bypass rules)
     * Return true: show the dialog if needed and change the connection state
     */
    private boolean validation(Context context, Intent intent) {
        if (null == context || null == intent) return false;
        if (!UsbManager.ACTION_USB_STATE.equals(intent.getAction())) return false;

        // Factory auto-test will execute "fastboot oem adb_enable 1" to turn it on.
        // We bypass this situation to make testing process smoothly.
        if (SystemProperties.getInt(SYSTEM_PROPERTY_FACTORY_ADB_ON, 0) == 1) return false;
        // Bypass when Setup Wizard is still running.
        if (Secure.getInt(context.getContentResolver(), Secure.USER_SETUP_COMPLETE, 0) == 0)
            return false;
        // Bypass when plugging DAC (AOA)
        if (intent.getBooleanExtra(UsbManager.USB_FUNCTION_ACCESSORY, false) ||
                intent.getBooleanExtra(UsbManager.USB_FUNCTION_AUDIO_SOURCE, false)) return false;

        // Add new rules if needed
        return true;
    }
}
