package com.android.settings.bluelightfilter;

import com.android.settings.bluelightfilter.Constants;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

public class BluelightFilterSwitchEnabler extends AbstractEnabler implements CompoundButton.OnCheckedChangeListener{
    private final Context mContext;
    private Switch mSwitch;
    private boolean mStateMachineEvent;

    public BluelightFilterSwitchEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
    }

    private ContentObserver mBluelight_Switch_Observer = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onSwitchChanged();
        }
    };

    @Override
    public void setSwitch(Switch switch_) {
        // TODO Auto-generated method stub
        if (mSwitch == switch_)
            return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        int option = Settings.System.getInt(mContext.getContentResolver(),
                Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
        setSwitchChecked(option == 1);
        mSwitch.setOnCheckedChangeListener(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH), true,
                mBluelight_Switch_Observer);
    }

    public void handleStateChanged() {
        int option = Settings.System.getInt(mContext.getContentResolver(),
                Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
        setSwitchChecked(option == 1);
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            //mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            //mStateMachineEvent = false;
        }
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH), true,
                mBluelight_Switch_Observer);
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub
        mSwitch.setOnCheckedChangeListener(null);
        mContext.getContentResolver().unregisterContentObserver(mBluelight_Switch_Observer);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub
        if (mStateMachineEvent){
            return;
        }
        Settings.System.putInt(mContext.getContentResolver(),
                Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, (isChecked)?1:0);
        Intent service_intent = new Intent(mContext, TaskWatcherService5Level.class);
        service_intent.putExtra(Constants.EXTRA_QUICKSETTING_READER_MODE_ON_OFF, (isChecked)?1:0);
        mContext.startService(service_intent);
    }

    private void onSwitchChanged() {
        int option = Settings.System.getInt(mContext.getContentResolver(),
                Constants.ASUS_SPLENDID_READING_MODE_MAIN_SWITCH, 0);
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch.setChecked(option==1);
        mSwitch.setOnCheckedChangeListener(this);
    }
}
