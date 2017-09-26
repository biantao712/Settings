
package com.android.settings.switchenabler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

public class MotionGestureSwitchEnabler extends AbstractEnabler implements
        CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "MotionGestureSwitchEnabler";
    private final Context mContext;

    private static final int DISABLE_MODE = 0;
    private static final int ENABLE_MODE = 1;
    private static final String ZEN_MOTION_DATA = "zen_motion_data";
    private static final String SHAKE_SHAKE_DATA = "shake_shake";
    private static final String MOTION_GESTURE_BROADCAST_MESSAGE = "com.android.settings.MOTION_GESTURE";

    private final SharedPreferences mZenMotionSharesPreferences;
    private Switch mSwitch;
    private boolean mStateMachineEvent;
    private final Intent mMotionGestureIntent = new Intent(MOTION_GESTURE_BROADCAST_MESSAGE);

    public MotionGestureSwitchEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mZenMotionSharesPreferences = mContext.getSharedPreferences(ZEN_MOTION_DATA,
                Context.MODE_PRIVATE);
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

    @Override
    public void resume() {

        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void pause() {

        mSwitch.setOnCheckedChangeListener(null);
    }

    public void handleMainSwitchChanged() {
        handleStateChanged();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent)
            return;
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.ASUS_MOTION_GESTURE_SETTINGS, isChecked ? 1 : 0);
        Log.d(TAG, "MotionGestureSwitchEnabler-- write ASUS_MOTION_GESTURE_SETTINGS value:"
                + isChecked);
        mContext.sendBroadcast(mMotionGestureIntent);
//        writeShakeShakeDB(isChecked ? 1 : 0);
    }

    private void handleStateChanged() {
        boolean isChecked = false;
        try {
            isChecked = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.ASUS_MOTION_GESTURE_SETTINGS) == ENABLE_MODE;
        } catch (SettingNotFoundException snfe) {
            isChecked = false;
        }

        setSwitchChecked(isChecked);
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }

    private void writeShakeShakeDB(int Value) {
        int savedDBValue = Value;

        int defShakeDBValue = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.ASUS_MOTION_SHAKE, DISABLE_MODE);
        // ON
        if (ENABLE_MODE == Value) {
            savedDBValue = mZenMotionSharesPreferences.getInt(SHAKE_SHAKE_DATA, defShakeDBValue);
        } else { // OFF
            mZenMotionSharesPreferences.edit().putInt(SHAKE_SHAKE_DATA, defShakeDBValue)
                    .apply();
        }
        Log.d(TAG, "MotionGestureSwitchEnabler-- write ASUS_MOTION_SHAKE value:" + savedDBValue);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.ASUS_MOTION_SHAKE, savedDBValue);

    }
}
