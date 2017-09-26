package com.android.settings.fingerprint;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.settings.R;

/**
 * Created by yueting-wong on 2016/8/17.
 */
public class AsusUnlockSwitchPreference extends Preference {

    private Context mContext;
    private Switch mSwitch;
    private boolean mChecked = false;
    private AsusUnlockEnabler mEnabler;
    
    private boolean mSwitchEnable = true;

    public AsusUnlockSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public AsusUnlockSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public AsusUnlockSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AsusUnlockSwitchPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context){
        mContext = context;
        setWidgetLayoutResource(R.layout.preference_fingerprint_unlock_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder parent) {
        super.onBindViewHolder(parent);
        mEnabler = new AsusUnlockEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) parent.findViewById(R.id.switch_fp_unlock);
        mEnabler.setSwitch(mSwitch);
        
        mSwitch.setEnabled(mSwitchEnable);
    }

    public void handleStateChanged() {
        if (null != mEnabler) mEnabler.handleMainSwitchChanged();
    }

    public void resume() {
        if (null != mEnabler) mEnabler.resume();
    }

    public void pause() {
        if (null != mEnabler) mEnabler.pause();
    }

    public void setSwitchEnable(boolean enabled) {
        mSwitchEnable = enabled;
    }

    public boolean isChecked(){
        boolean isChecked = false;
        if(mSwitch != null) {
            isChecked = mSwitch.isChecked();
        }
        return isChecked;
    }
}
