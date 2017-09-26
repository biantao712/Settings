package com.android.settings;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

public class LockscreenLongPressInstantCameraSwitchPreference extends Preference {
    static final String TAG = "LockscreenLongPressInstantCameraSwitchPreference";
    private Switch mSwitch;
    private CompoundButton.OnCheckedChangeListener mListener;
    private boolean mIsChecked = false;
    public LockscreenLongPressInstantCameraSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_lockscreen_longpress_instant_camera_widget_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSwitch = (Switch) view.findViewById(R.id.switch_longpress_instant_camera);
        mSwitch.setOnCheckedChangeListener(mListener);
        setSwitchChecked(mIsChecked);
    }

    public void setOnSwitchCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mListener = listener;
    }

    public void setSwitchChecked(boolean checked) {
        Log.d(TAG, "setSwitchChecked()");
        mIsChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
    }

    public boolean isChecked(){
        return mIsChecked;
    }

}
