package com.android.settings.bluelightfilter;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.android.settings.bluelightfilter.Constants;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
//import android.preference.Preference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.BaseSavedState;
import android.support.v7.preference.PreferenceViewHolder;
import android.provider.Settings;
import com.android.settings.R;

public class BluelightFilterPreference extends Preference{
    private static final String TAG = "BluelightFilterPreference";
    private BluelightFilterSwitchEnabler mEnabler;
    private Switch mSwitch;
    private Context mContext;
    private boolean mRestoredOldState;

    /*private ContentObserver mBluelight_Switch_Observer = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onSwitchChanged();
        }
    };*/

    public BluelightFilterPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setWidgetLayoutResource(R.layout.preference_bluelight_filter_switch);
    }

    @Override
    public void onBindViewHolder (PreferenceViewHolder holder){
        mEnabler = new BluelightFilterSwitchEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) holder.findViewById(R.id.switch_bluelight_filter);
        mEnabler.setSwitch(mSwitch);
        mSwitch.setVisibility(View.VISIBLE);
    }

    /*
    @Override
    protected View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, parent, savedInstanceState);
        mEnabler = new BluelightFilterSwitchEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) view.findViewById(R.id.switch_bluelight_filter);
        mEnabler.setSwitch(mSwitch);
        mSwitch.setVisibility(View.VISIBLE);

        return view;
    }*/

    /*private void onSwitchChanged() {
    int switch_on = Settings.System.getInt(mContext.getContentResolver(), Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
    mSwitch.setChecked(switch_on==1);
    }*/
}
