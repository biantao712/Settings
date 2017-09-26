package com.asus.settings.lockscreen.ui;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.util.Log;


import com.android.settings.R;

public class LockscreenShowMagzineSwitchPreference extends Preference {
    public String TAG = "LockscreenShowMagzineSwitchPreference";
    private Switch mSwitch = null;
    private CompoundButton.OnCheckedChangeListener mListener;
    private boolean mIsChecked = false;
    public LockscreenShowMagzineSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG,"LockscreenShowMagzineSwitchPreference");

        Log.d(TAG,"setWidgetLayoutResource(R.layout.preference_lockscreen_skip_slide_switch);");
        setWidgetLayoutResource(R.layout.preference_lockscreen_show_magzine_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        Log.d(TAG,"onBindViewHolder");
        super.onBindViewHolder(view);
        mSwitch = (Switch) view.findViewById(R.id.switchshowpattern);
        if(mSwitch != null)
        {
            mSwitch.setOnCheckedChangeListener(mListener);
        }
        else
        {
            Log.d(TAG,"mSwitch is null");
        }

        Log.d(TAG,"Set on Checked listener");
        setSwitchChecked(mIsChecked);
    }

    public void setOnSwitchCheckedChangeListener(CompoundButton.OnCheckedChangeListener listener) {
        Log.d(TAG,"setOnSwitchCheckedChangeListener");
        mListener = listener;
    }

    public void setSwitchChecked(boolean checked) {
        Log.d(TAG,"setSwitchChecked " + checked);
        mIsChecked = checked;
        if (mSwitch != null) {
            mSwitch.setChecked(checked);
        }
        else
        {
            Log.d(TAG,"mSwitch is null");
        }
    }

    public boolean isChecked()
    {
        Log.d(TAG,"returen status : " + mIsChecked);
        return mIsChecked;
    }
}
