
package com.android.settings.memc;

import com.android.settings.bluelightfilter.AbstractEnabler;

import android.content.Context;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

public class MemcSwitchEnabler extends AbstractEnabler implements CompoundButton.OnCheckedChangeListener{
    private final Context mContext;
    private Switch mSwitch;
    private boolean mStateMachineEvent;

    public MemcSwitchEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
    }

    @Override
    public void setSwitch(Switch switch_) {
        // TODO Auto-generated method stub
        if (mSwitch == switch_)
            return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        int memcLevel = Settings.System.getInt(mContext.getContentResolver(),
                MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL, MemcSwitchHelper.PICTURE_QUALITY_MEMC_DEFAULT);
        setSwitchChecked(memcLevel != 0);
        mSwitch.setOnCheckedChangeListener(this);
    }

    public void handleStateChanged() {
        int memcLevel = Settings.System.getInt(mContext.getContentResolver(),
                MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL, MemcSwitchHelper.PICTURE_QUALITY_MEMC_DEFAULT);
        setSwitchChecked(memcLevel != 0);
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // TODO Auto-generated method stub
        int memcLevel = Settings.System.getInt(mContext.getContentResolver(),
                MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL, MemcSwitchHelper.PICTURE_QUALITY_MEMC_DEFAULT);
        int memcLevelTemp = Settings.System.getInt(mContext.getContentResolver(), MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL_TEMP, MemcSwitchHelper.PICTURE_QUALITY_MEMC_DEFAULT);
        if (mStateMachineEvent){
            return;
        }
        if (isChecked) {
            memcLevel = memcLevelTemp;
        } else {
            memcLevelTemp = memcLevel;
            memcLevel = 0;
            Settings.System.putInt(mContext.getContentResolver(), MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL_TEMP, memcLevelTemp);
        }
        Settings.System.putInt(mContext.getContentResolver(),
                MemcSwitchHelper.ASUS_VISUALMASTER_PQ_CHIP_MEMC_LEVEL,memcLevel);
    }
}
