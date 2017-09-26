
package com.android.settings.zenmotion;

import com.android.settings.R;
import com.android.settings.switchenabler.TouchGestureSwitchEnabler;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

public class ZenTouchGestureSwitchPreference extends Preference {
    private final Context mContext;
    private Switch mSwitch;
    private TouchGestureSwitchEnabler mEnabler;

    public ZenTouchGestureSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        setWidgetLayoutResource(R.layout.preference_touch_gesture_switch);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mEnabler = new TouchGestureSwitchEnabler(mContext, new Switch(mContext));
        mSwitch = (Switch) view.findViewById(R.id.switch_touch);
        mEnabler.setSwitch(mSwitch);
    }

    public void resume() {
        if (null != mEnabler) mEnabler.resume();
    }

    public void pause() {
        if (null != mEnabler) mEnabler.pause();
    }

    public void handleStateChanged() {
        mEnabler.handleMainSwitchChanged();
    }
}
