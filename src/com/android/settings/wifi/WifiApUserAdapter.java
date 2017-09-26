package com.android.settings.wifi;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.settings.R;

import java.util.ArrayList;

public class WifiApUserAdapter extends BaseAdapter implements ListAdapter {

    private LayoutInflater mInflater = null;
    private ArrayList<String> mUserList = null;

    public WifiApUserAdapter(Context context, ArrayList<String> userList) {
        mInflater = LayoutInflater.from(context);
        mUserList = userList;
    }

    @Override
    public int getCount() {
        if (mUserList != null) {
            return mUserList.size();
        }
        return 0;
    }

    @Override
    public String getItem(int position) {
        if (mUserList != null) {
            return mUserList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (mUserList != null) {
            return position;
        }
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView != null) {
            view = convertView;
        } else {
            view = mInflater.inflate(android.R.layout.simple_list_item_2, parent, false);
        }
        String[] userdata = mUserList.get(position).split("\n");
        if (userdata.length < 3) {
            ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.wifi_tether_no_name);
            ((TextView) view.findViewById(android.R.id.text2)).setText(userdata[0]);
        } else {
            ((TextView) view.findViewById(android.R.id.text1)).setText(userdata[2]);
            ((TextView) view.findViewById(android.R.id.text2)).setText(userdata[1]);
        }
        return view;
    }

    public void updateUserList(ArrayList<String> userList) {
        mUserList = userList;
    }

}
