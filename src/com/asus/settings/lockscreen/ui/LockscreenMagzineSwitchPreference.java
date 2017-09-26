package com.asus.settings.lockscreen.ui;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.util.Log;
import android.os.UserHandle;
import android.provider.Settings;

import com.android.settings.R;

public class LockscreenMagzineSwitchPreference extends Preference {
    public String TAG = "LockscreenMagzineSwitchPreference";
    private Switch mSwitch = null;
    private CompoundButton.OnCheckedChangeListener mListener;
    private boolean mIsChecked = false;
    private int mStatus = 0;
    private Context mContext = null;
    public static String SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE = "SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE";



    public LockscreenMagzineSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG,"LockscreenMagzineSwitchPreference");
        mContext = context;
        Log.d(TAG,"setWidgetLayoutResource(R.layout.preference_lockscreen_magzine_switch);");
        setWidgetLayoutResource(R.layout.preference_lockscreen_magzine_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        Log.d(TAG,"onBindViewHolder");
        super.onBindViewHolder(view);
        mSwitch = (Switch) view.findViewById(R.id.switchmagzine);
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
            Log.d(TAG,"setSwitchChecked " + checked);
            mIsChecked = checked;
            if (mSwitch != null) {
                mSwitch.setChecked(checked);
                int value = (checked ? 1 : 0);
                Settings.System.putInt(mContext.getContentResolver(),
                        SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE, value);
                Log.d(TAG, "set lockscreen magazine enable to SETTINGS_SYSTEM_ASUS_LOCKSCREEN_MAGAZINE_ENABLE " + value);
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
