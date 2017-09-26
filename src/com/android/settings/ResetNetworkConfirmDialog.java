package com.android.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import android.net.NetworkPolicyManager;
import com.android.ims.ImsManager;

public class ResetNetworkConfirmDialog extends DialogFragment {

    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private DialogInterface.OnClickListener mFinalClickListener;
    
    public ResetNetworkConfirmDialog() {
        super();
        initListener();
    }
    
    public ResetNetworkConfirmDialog(int subId) {
        super();
        mSubId = subId;
        initListener();
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = createDialog();
        
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.gravity = Gravity.BOTTOM;
        
        return dialog;
    }
    
    private Dialog createDialog(){
        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
        TextView contentTitle = (TextView) contentView.getRootView().findViewById(R.id.alertdialog_title);
        TextView contentMessage = (TextView) contentView.getRootView().findViewById(R.id.alertdialog_message);
        contentTitle.setText(getActivity().getString(R.string.hint));
        contentMessage.setText(getActivity().getString(R.string.reset_network_final_desc));
        
        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setView(contentView)
            .setPositiveButton(getActivity().getString(R.string.dlg_ok), mFinalClickListener)
            .setNegativeButton(getActivity().getString(R.string.dlg_cancel), null)
            .create();
        return dialog;
    }
    
    private void initListener() {
        mFinalClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    if (Utils.isMonkeyRunning()) {
                        return;
                    }
    
                    // TODO maybe show a progress dialog if this ends up taking a while
                    Context context = getActivity();
    
                    ConnectivityManager connectivityManager = (ConnectivityManager)
                            context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connectivityManager != null) {
                        connectivityManager.factoryReset();
                    }
    
                    WifiManager wifiManager = (WifiManager)
                            context.getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        wifiManager.factoryReset();
                    }

                    //+++ tim_hu reset wifi advanced settings to default
                    Settings.Global.putInt(context.getContentResolver(),
                            Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 1);
                    Settings.Global.putInt(context.getContentResolver(),
                            Settings.Global.WIFI_SLEEP_POLICY, Settings.Global.WIFI_SLEEP_POLICY_NEVER);
                    //---

                    TelephonyManager telephonyManager = (TelephonyManager)
                            context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        telephonyManager.factoryReset(mSubId);
                    }
    
                    NetworkPolicyManager policyManager = (NetworkPolicyManager)
                            context.getSystemService(Context.NETWORK_POLICY_SERVICE);
                    if (policyManager != null) {
                        String subscriberId = telephonyManager.getSubscriberId(mSubId);
                        policyManager.factoryReset(subscriberId);
                    }
    
                    BluetoothManager btManager = (BluetoothManager)
                            context.getSystemService(Context.BLUETOOTH_SERVICE);
                    if (btManager != null) {
                        BluetoothAdapter btAdapter = btManager.getAdapter();
                        if (btAdapter != null) {
                            btAdapter.factoryReset();
                        }
                    }
    
                    ImsManager.factoryReset(context);
    
                    dialog.dismiss();
                    Toast.makeText(context, R.string.reset_network_complete_toast, Toast.LENGTH_SHORT)
                            .show();
                }
            }
        };
    }
}
