package com.android.settings.fingerprint;

import android.content.Context;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.switchenabler.AbstractEnabler;

/**
 * Created by yueting-wong on 2016/8/17.
 */
public class AsusUnlockEnabler extends AbstractEnabler implements
        CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "AsusUnlockEnabler";
    private Context mContext;
    private Switch mSwitch;

    private static final int DISABLE_MODE = 0;
    private static final int ENABLE_MODE = 1;
    private static final String UNLOCK_DEVICE_ENABLED = "unlock_device_with_fingerprint";

    private boolean mStateMachineEvent;

    public AsusUnlockEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
    }

    @Override
    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_)
            return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    private void handleStateChanged() {
        boolean isChecked = Settings.Secure.getInt(mContext.getContentResolver(),
                UNLOCK_DEVICE_ENABLED, ENABLE_MODE) != DISABLE_MODE;
        setSwitchChecked(isChecked);
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mSwitch.setChecked(checked);
        }
    }

    public void handleMainSwitchChanged() {
        handleStateChanged();
    }

    @Override
    public void resume() {
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                UNLOCK_DEVICE_ENABLED, isChecked ? ENABLE_MODE : DISABLE_MODE);
    }
}
