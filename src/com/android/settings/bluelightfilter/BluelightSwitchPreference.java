
package com.android.settings.bluelightfilter;

import com.android.settings.R;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

public class BluelightSwitchPreference extends Preference {
    private final Context mContext;
    private Switch mSwitch;
    //private MemcSwitchEnabler mEnabler;
    private BluelightFilterSwitchEnabler mEnabler;

    public BluelightSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setWidgetLayoutResource(R.layout.preference_bluelight_filter_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view){
        super.onBindViewHolder(view);
        mEnabler = new BluelightFilterSwitchEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) view.findViewById(R.id.switch_bluelight_filter);
        mEnabler.setSwitch(mSwitch);
    }
/*
    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        mEnabler = new MemcSwitchEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) view.findViewById(R.id.switch_memc);
        mEnabler.setSwitch(mSwitch);
        return view;
    }

    public void resume() {
        if (null != mEnabler){
            mEnabler.resume();
        }
    }

    public void pause() {
        if (null != mEnabler){
            mEnabler.pause();
        }
    }

    public Switch getSwitch(){
        return mSwitch;
    }
    */
}
