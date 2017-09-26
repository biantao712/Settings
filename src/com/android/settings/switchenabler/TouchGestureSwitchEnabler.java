
package com.android.settings.switchenabler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

public class TouchGestureSwitchEnabler extends AbstractEnabler implements
        CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "TouchGestureSwitchEnabler";
    private final Context mContext;
    private Switch mSwitch;

    private static final String PERSIST_ASUS_DLICK = "persist.asus.dclick";
    private static final String PERSIST_ASUS_GESTURE_TYPE = "persist.asus.gesture.type";
    private static final String DEFAULT_ASUS_GESTURE_TYPE = "1111111";
    // +++ Double tap mode settings
    private static final int DISABLE_DOUBLE_TAP_MODE = 0;
    private static final int ENABLE_DOUBLE_TAP_MODE = 1;
    private static final int OP_ALL = 64;
    private static final String ZEN_MOTION_DATA = "zen_motion_data";
    private static final String DOUBLE_TAP_ON_DATA = "double_tap_on";
    private static final String DOUBLE_TAP_OFF_DATA = "double_tap_off";

    // Swipe-up to wake up
    private static final String PERSIST_ASUS_SWIPE_UP = "persist.asus.swipeup";
    private static final int DISABLE_SWIPE_UP_MODE = 0;
    private static final int ENABLE_SWIPE_UP_MODE = 1;
    private static final String SWIPE_UP_DATA = "swipe_up_wake_up";

    private final SharedPreferences mZenMotionSharesPreferences;

    private boolean mStateMachineEvent;

    public TouchGestureSwitchEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mZenMotionSharesPreferences = mContext.getSharedPreferences(ZEN_MOTION_DATA,
                Context.MODE_PRIVATE);

    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub
        if (mStateMachineEvent)
            return;
        writeDB(isChecked);
    }

    @Override
    public void setSwitch(Switch switch_) {
        // TODO Auto-generated method stub
        if (mSwitch == switch_)
            return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void handleMainSwitchChanged() {
        handleStateChanged();
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }

    private void writeDB(boolean checked) {

        if (checked) {
            writeDBSwitchOn();
        } else {
            writeDBSwitchOff();
        }
    }

    private void handleStateChanged() {
        Boolean enable = getSystemPropGestureIsChecked() || getDoubleTapOnIsChecked()
                || getDoubleTapOffIsChecked() || getSwipeUPIsChecked();
        setSwitchChecked(enable);
    }

    private void writeDBSwitchOn() {
        int doubleTapOnValue = mZenMotionSharesPreferences.getInt(DOUBLE_TAP_ON_DATA,
                DISABLE_DOUBLE_TAP_MODE);
        int doubleTapOffValue = mZenMotionSharesPreferences.getInt(DOUBLE_TAP_OFF_DATA,
                DISABLE_DOUBLE_TAP_MODE);
        int swipeUpOnValue = mZenMotionSharesPreferences.getInt(SWIPE_UP_DATA,
                DISABLE_SWIPE_UP_MODE);
        writeDoubleTapOnDB(doubleTapOnValue);
        writeDoubleTapOffDB(doubleTapOffValue);
        writeSwipeUpDB(swipeUpOnValue);

        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE,
                DEFAULT_ASUS_GESTURE_TYPE);
        int decimalValue = Integer.parseInt(opString, 2);
        if (decimalValue < OP_ALL)
            decimalValue += OP_ALL;
        writeAppGestureDB(Integer.toBinaryString(decimalValue));
    }

    private void writeDBSwitchOff() {
        Editor editor = mZenMotionSharesPreferences.edit();
        // save double tap ON value
        editor.putInt(DOUBLE_TAP_ON_DATA,getDoubleTapOnIsChecked()?
                ENABLE_DOUBLE_TAP_MODE : DISABLE_DOUBLE_TAP_MODE);

        // save double tap OFF value
        editor.putInt(DOUBLE_TAP_OFF_DATA,getDoubleTapOffIsChecked()?
                ENABLE_DOUBLE_TAP_MODE : DISABLE_DOUBLE_TAP_MODE);

        // save swipe up ON value
        editor.putInt(SWIPE_UP_DATA,getSwipeUPIsChecked()?
                ENABLE_SWIPE_UP_MODE : DISABLE_SWIPE_UP_MODE);

        editor.apply();

        writeDoubleTapOnDB(DISABLE_DOUBLE_TAP_MODE);
        writeDoubleTapOffDB(DISABLE_DOUBLE_TAP_MODE);
        writeSwipeUpDB(DISABLE_SWIPE_UP_MODE);

        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE,
                DEFAULT_ASUS_GESTURE_TYPE);

        int decimalValue = Integer.parseInt(opString, 2);
        if (decimalValue >= OP_ALL)
            decimalValue -= OP_ALL;

        writeAppGestureDB(String.format("%7s", Integer.toBinaryString(decimalValue)).replace(' ',
                '0'));
    }

    private void writeDoubleTapOnDB(int value) {
        try {
            Log.d(TAG, "TouchGestureSwitchEnabler--Write double tap on value: " + value);
            SystemProperties.set(PERSIST_ASUS_DLICK, Integer.toString(value));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    private void writeDoubleTapOffDB(int value) {
        Log.d(TAG, "TouchGestureSwitchEnabler--Write double tap off value("
                + Settings.System.ASUS_DOUBLE_TAP + "): " + value);
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.ASUS_DOUBLE_TAP, value);
    }

    private void writeAppGestureDB(String opString) {
        try {
            Log.d(TAG, "TouchGestureSwitchEnabler--Write system property("
                    + PERSIST_ASUS_GESTURE_TYPE + "): " + opString);
            SystemProperties.set(PERSIST_ASUS_GESTURE_TYPE, opString);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    private Boolean getSystemPropGestureIsChecked() {
        String type = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE);
        if (type.length() != 7) {
            type = DEFAULT_ASUS_GESTURE_TYPE;
            writeAppGestureDB(type);
        }
        int decimalValue = Integer.parseInt(type, 2);
        return decimalValue >= OP_ALL;
    }

    private Boolean getDoubleTapOnIsChecked() {
        String stringValue = SystemProperties.get(PERSIST_ASUS_DLICK,
                String.valueOf(DISABLE_DOUBLE_TAP_MODE));
        return stringValue.equals(String.valueOf(ENABLE_DOUBLE_TAP_MODE));
    }

    private Boolean getDoubleTapOffIsChecked() {
        try {
            return Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.ASUS_DOUBLE_TAP) == ENABLE_DOUBLE_TAP_MODE;
        } catch (SettingNotFoundException snfe) {
            return false;
        }
    }

    private void writeSwipeUpDB(int value) {
        try {
            Log.d(TAG, "TouchGestureSwitchEnabler--Write swipe up on value: " + value);
            SystemProperties.set(PERSIST_ASUS_SWIPE_UP, Integer.toString(value));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    private Boolean getSwipeUPIsChecked() {
        String stringValue = SystemProperties.get(PERSIST_ASUS_SWIPE_UP,
                String.valueOf(DISABLE_SWIPE_UP_MODE));
        return stringValue.equals(String.valueOf(ENABLE_SWIPE_UP_MODE));
    }
}
