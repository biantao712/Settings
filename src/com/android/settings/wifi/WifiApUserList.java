package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ListView;

import com.android.settings.R;

import java.util.ArrayList;

public class WifiApUserList extends AlertDialog {
    private static final String TAG = "WifiApUserList";

    private Context mContext;

    private static final int EVENT_REFRESH_USER_LIST = 1;

    private WifiApUserAdapter mAdapter = null;
    private ArrayList<String> mUserList = new ArrayList<String>();

    public WifiApUserList(Context context, ArrayList<String> userList) {
        super(context);
        mContext = context;
        mUserList = userList;
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case EVENT_REFRESH_USER_LIST:
                    mContext.sendBroadcast(new Intent(WifiManager.WIFI_AP_UPDATE_REQUEST_ACTION));
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        View view = getLayoutInflater().inflate(R.layout.wifi_ap_user_dialog, null);
        ListView listView = (ListView) view.findViewById(android.R.id.list);
        listView.setEmptyView(view.findViewById(android.R.id.empty));
        setTitle(R.string.wifi_tether_users);
        setView(view);
        setInverseBackgroundForced(true);
        mAdapter = new WifiApUserAdapter(mContext, mUserList);
        listView.setAdapter(mAdapter);
        super.onCreate(bundle);
        mHandler.removeMessages(EVENT_REFRESH_USER_LIST);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_REFRESH_USER_LIST), 100);
    }

    public void notifyUserListChanged(ArrayList<String> userList) {
        mUserList = userList;
        if (mAdapter == null) {
            mAdapter = new WifiApUserAdapter(mContext, mUserList);
        }
        mAdapter.updateUserList(mUserList);
        mAdapter.notifyDataSetChanged();
        mHandler.removeMessages(EVENT_REFRESH_USER_LIST);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(EVENT_REFRESH_USER_LIST), 60*1000);
    }

}
