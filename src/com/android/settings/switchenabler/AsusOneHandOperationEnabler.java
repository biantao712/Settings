package com.android.settings.switchenabler;

import java.util.Observable;
import java.util.Observer;

import android.content.ContentQueryMap;
import android.content.Context;
import android.database.Cursor;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

public class AsusOneHandOperationEnabler  extends AbstractEnabler implements CompoundButton.OnCheckedChangeListener {

    private Context mContext;
    private Switch mSwitch;
    private ContentQueryMap mContentQueryMap;
    private Observer mSettingsObserver;
    private boolean mStateMachineEvent;

    public AsusOneHandOperationEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
    }

    @Override
    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void start() {
        // listen for Location Manager settings changes
        Cursor settingsCursor = mContext.getContentResolver().query(Settings.System.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.System.ASUS_ONE_HAND_OPERATION},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
        if (settingsCursor != null) {
            settingsCursor.close();
        }
    }

    @Override
    public void resume() {
        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                public void update(Observable o, Object arg) {
                    handleStateChanged();
                }
            };
        }
        mContentQueryMap.addObserver(mSettingsObserver);

        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }


    @Override
    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    @Override
    public void stop() {
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent) return;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.ASUS_ONE_HAND_OPERATION, (isChecked? 1 : 0));
        handleStateChanged();
    }

    protected void handleStateChanged() {
        int state = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ASUS_ONE_HAND_OPERATION, 0);
        setSwitchChecked(state==0? false : true);
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }
}
