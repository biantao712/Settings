package com.asus.settings.lockscreen.ui;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.android.settings.R;

public class LockscreenClockWidgetSwitchPreference extends Preference {

    private Switch mSwitch;
    private CompoundButton.OnCheckedChangeListener mListener;
    private boolean mIsChecked = false;

    public LockscreenClockWidgetSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_lockscreen_clock_widget_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mSwitch = (Switch) view.findViewById(R.id.switch_asus_lockscreen_clock_widget);
        mSwitch.setOnCheckedChangeListener(mListener);
        setSwitchChecked(mIsChecked);
    }

    public void setOnSwitchCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        mListener = listener;
    }

    public void setSwitchChecked(boolean checked) {
        mIsChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
    }

    public boolean isChecked() {
        return mIsChecked;
    }
}
