/*
 * Copyright (C) 2012 The Android Open Source Project
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
package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;


import com.android.settings.R;


import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Dialog to show WPS progress.
 */
public class WpsDialog extends AlertDialog {

    private final static String TAG = "WpsDialog";
    private static final String DIALOG_STATE = "android:dialogState";
    private static final String DIALOG_MSG_STRING = "android:dialogMsg";

    private View mView;
    private TextView mTextView;
    private ProgressBar mTimeoutBar;
    private ProgressBar mProgressBar;
    private ImageView mImageView;
    private TextView mTimeoutText;
    private Button mButton;
    private Timer mTimer;

    private static final int WPS_TIMEOUT_S = 120;

    private WifiManager mWifiManager;
    private static WpsListener mWpsListener;
    private int mWpsSetup;

    private final IntentFilter mFilter;
    private BroadcastReceiver mReceiver;

    private Context mContext;
    private Handler mHandler = new Handler();
    private String mMsgString = "";

    private enum DialogState {
        WPS_INIT,
        WPS_START,
        WPS_COMPLETE,
        CONNECTED, //WPS + IP config is done
        WPS_FAILED
    }
    DialogState mDialogState = DialogState.WPS_INIT;

    static class WpsListener extends WifiManager.WpsCallback {
         WeakReference<WpsDialog> mWeakWpsDialog;
         WeakReference<Context> mWeakContext;

        public WpsListener(WpsDialog wpsDialog, Context context) {
            super();
            mWeakWpsDialog = new WeakReference<WpsDialog>(wpsDialog);
            mWeakContext = new WeakReference<Context>(context);
        }

        public void bindDialog(WpsDialog wpsDialog, Context context) {
            mWeakWpsDialog = new WeakReference<WpsDialog>(wpsDialog);
            mWeakContext = new WeakReference<Context>(context);
        }

        public void onStarted(String pin) {
            if (pin != null) {
                if(mWeakWpsDialog.get() != null && mWeakContext.get() != null)
                    mWeakWpsDialog.get().updateDialog(DialogState.WPS_START, String.format(
                        mWeakContext.get().getString(R.string.wifi_wps_onstart_pin), pin));
            } else {
                if(mWeakWpsDialog.get() != null && mWeakContext.get() != null)
                    mWeakWpsDialog.get().updateDialog(DialogState.WPS_START, mWeakContext.get().getString(
                        R.string.wifi_wps_onstart_pbc));
            }
        }

        public void onSucceeded() {
            if(mWeakWpsDialog.get() != null && mWeakContext.get() != null)
                mWeakWpsDialog.get().updateDialog(DialogState.WPS_COMPLETE,
                    mWeakContext.get().getString(R.string.wifi_wps_complete));
        }

        public void onFailed(int reason) {
            String msg = null;
            if(mWeakWpsDialog.get() != null && mWeakContext.get() != null) {
                switch (reason) {
                    case WifiManager.WPS_OVERLAP_ERROR:
                        msg = mWeakContext.get().getString(R.string.wifi_wps_failed_overlap);
                        break;
                    case WifiManager.WPS_WEP_PROHIBITED:
                        msg = mWeakContext.get().getString(R.string.wifi_wps_failed_wep);
                        break;
                    case WifiManager.WPS_TKIP_ONLY_PROHIBITED:
                        msg = mWeakContext.get().getString(R.string.wifi_wps_failed_tkip);
                        break;
                    case WifiManager.IN_PROGRESS:
                        msg = mWeakContext.get().getString(R.string.wifi_wps_in_progress);
                        break;
                    default:
                        msg = mWeakContext.get().getString(R.string.wifi_wps_failed_generic);
                        break;
                }
                mWeakWpsDialog.get().updateDialog(DialogState.WPS_FAILED, msg);
            }
        }
    }

    public WpsDialog(Context context, int wpsSetup) {
        super(context);
        mContext = context;
        mWpsSetup = wpsSetup;

        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };
        setCanceledOnTouchOutside(false);
    }

    @Override
    public Bundle onSaveInstanceState () {
        Bundle bundle  = super.onSaveInstanceState();
        bundle.putString(DIALOG_STATE, mDialogState.toString());
        bundle.putString(DIALOG_MSG_STRING, mMsgString.toString());
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            super.onRestoreInstanceState(savedInstanceState);
            DialogState dialogState = mDialogState.valueOf(savedInstanceState.getString(DIALOG_STATE));
            String msg = savedInstanceState.getString(DIALOG_MSG_STRING);
            updateDialog(dialogState, msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.wifi_wps_dialog, null);

        TextView title = (TextView) mView.findViewById(R.id.alertdialog_title);
        int titleResid = (mWpsSetup == WpsInfo.DISPLAY) ? R.string.wifi_menu_wps_pin : R.string.wifi_menu_wps_pbc;
        title.setText(mContext.getString(titleResid));
        
        mTextView = (TextView) mView.findViewById(R.id.wps_dialog_txt);
        mTextView.setText(R.string.wifi_wps_setup_msg);

        mTimeoutBar = ((ProgressBar) mView.findViewById(R.id.wps_timeout_bar));
        mTimeoutBar.setMax(WPS_TIMEOUT_S);
        mTimeoutBar.setProgress(0);

        mProgressBar = ((ProgressBar) mView.findViewById(R.id.wps_progress_bar));
        mProgressBar.setVisibility(View.GONE);

        mImageView = ((ImageView) mView.findViewById(R.id.wps_dialog_icon));
        mImageView.setVisibility((mWpsSetup == WpsInfo.DISPLAY) ? View.GONE : View.VISIBLE);

        mTimeoutText = ((TextView) mView.findViewById(R.id.wps_timeout_txt));
        notifyTimeoutTextChanged(mTimeoutBar.getMax() - mTimeoutBar.getProgress());

//        mButton = ((Button) mView.findViewById(R.id.wps_dialog_btn));
//        mButton.setText(R.string.wifi_cancel);
//        mButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (mDialogState != DialogState.WPS_COMPLETE) {
//                    mWifiManager.cancelWps(null);
//                }
//                mWpsListener = null;
//                dismiss();
//            }
//        });
        
        setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.wifi_cancel), new DialogInterface.OnClickListener(){
        	@Override
            public void onClick(DialogInterface dialog, int which) {
        		if (mDialogState != DialogState.WPS_COMPLETE) {
                    mWifiManager.cancelWps(null);
                }
                mWpsListener = null;
                dismiss();
        	}
        });

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        setView(mView);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
    	mButton = getButton(DialogInterface.BUTTON_POSITIVE);
    	
        /*
         * increment timeout bar per second.
         */
        mTimer = new Timer(false);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mTimeoutBar.incrementProgressBy(1);
                        notifyTimeoutTextChanged(mTimeoutBar.getMax() - mTimeoutBar.getProgress());
                    }
                });
            }
        }, 1000, 1000);

        mContext.registerReceiver(mReceiver, mFilter);

        WpsInfo wpsConfig = new WpsInfo();
        wpsConfig.setup = mWpsSetup;
        if (mWpsListener == null) {
            mWpsListener = new WpsListener(this, mContext);
            mWifiManager.startWps(wpsConfig, mWpsListener);
        } else {
            mWpsListener.bindDialog(this, mContext);
        }
    }

    @Override
    protected void onStop() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    private void updateDialog(final DialogState state, final String msg) {
        if (mDialogState.ordinal() >= state.ordinal()) {
            //ignore.
            return;
        }
        mDialogState = state;
        mMsgString = msg;

        mHandler.post(new Runnable() {
                @Override
                public void run() {
                    switch(state) {
                        case WPS_COMPLETE:
                            mTimeoutBar.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.VISIBLE);
                            break;
                        case CONNECTED:
                        case WPS_FAILED:
                        	if(mButton != null)
                        		mButton.setText(mContext.getString(R.string.dlg_ok));
                            mTimeoutBar.setVisibility(View.GONE);
                            mProgressBar.setVisibility(View.GONE);
                            mImageView.setVisibility(View.GONE);
                            mTimeoutText.setVisibility(View.GONE);
                            if (mReceiver != null) {
                                mContext.unregisterReceiver(mReceiver);
                                mReceiver = null;
                            }
                            break;
                    }
                    mTextView.setText(msg);
                }
            });
   }

    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(
                    WifiManager.EXTRA_NETWORK_INFO);
            final NetworkInfo.DetailedState state = info.getDetailedState();
            if (state == DetailedState.CONNECTED &&
                    mDialogState == DialogState.WPS_COMPLETE) {
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String msg = String.format(mContext.getString(
                            R.string.wifi_wps_connected), removeDoubleQuotes(wifiInfo.getSSID()));
                    updateDialog(DialogState.CONNECTED, msg);
                }
            }
        }
    }

    static String removeDoubleQuotes(String string) {
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }

    private void notifyTimeoutTextChanged(int time){
        if (mTimeoutText != null && mTimeoutText.getVisibility() == View.VISIBLE) {
            mTimeoutText.setText(mContext.getString(R.string.wps_timeout_txt_wait, time));
        }
    }
}
