package com.asus.settings.lockscreen.ui;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.util.Log;
import android.provider.Settings;
import android.app.Activity;

import com.android.settings.R;

public class LockscreenWeatherColorSwitchPreference extends Preference {
    public String TAG = "LockscreenWeatherColorSwitchPreference";
    private Context mContext = null;
    private Switch mSwitch = null;
    private CompoundButton.OnCheckedChangeListener mListener;
    private boolean mIsChecked = false;
    private int mStatus = 0;
    public static String SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE = "SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE";



    public LockscreenWeatherColorSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        Log.d(TAG,"LockscreenWeatherColorSwitchPreference");

        Log.d(TAG,"setWidgetLayoutResource(R.layout.preference_lockscreen_weather_color_switch);");
        setWidgetLayoutResource(R.layout.preference_lockscreen_weather_color_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        Log.d(TAG,"onBindViewHolder");
        super.onBindViewHolder(view);
        mSwitch = (Switch) view.findViewById(R.id.switchweathercolor);
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
        try {
            Log.d(TAG, "setSwitchChecked " + checked);
            mIsChecked = checked;
            if (mSwitch != null) {
                mSwitch.setChecked(checked);
                int value = (checked ? 1 : 0);

                Settings.System.putInt(mContext.getContentResolver(),
                        SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE, value);
                Log.d(TAG, "set weather smart color to SETTINGS_SYSTEM_ASUS_LOCKSCREEN_WEATHER_SMART_COLOR_ENABLE " + value);
            } else {
                Log.d(TAG, "mSwitch is null");
            }
        }
        catch (Exception ee)
        {
            Log.d(TAG,"Exception in setSwitchChecked "+ ee);
        }
    }

    public boolean isChecked()
    {
        Log.d(TAG,"returen status : " + mIsChecked);
        return mIsChecked;
    }
}
