
package com.android.settings.zenmotion;

import com.android.settings.switchenabler.MotionGestureSwitchEnabler;
import com.android.settings.R;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

public class ZenMotionGestureSwitchPreference extends Preference {

    private static final String TAG = "ZenMotionGestureSwitchPreference";
    private final Context mContext;
    private MotionGestureSwitchEnabler mEnabler;
    private Switch mSwitch;

    public ZenMotionGestureSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setWidgetLayoutResource(R.layout.preference_motion_gesture_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mEnabler = new MotionGestureSwitchEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) view.findViewById(R.id.switch_motion);
        mEnabler.setSwitch(mSwitch);
    }
			
    public void handleStateChanged() {
        mEnabler.handleMainSwitchChanged();
    }

    public void resume() {
        if (null != mEnabler) mEnabler.resume();
    }

    public void pause() {
        if (null != mEnabler) mEnabler.pause();
    }

}
