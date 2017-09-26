package com.android.settings.accessibility;

import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.widget.RadioButton;

import com.android.settings.SettingsPreferenceFragment;

import com.android.settings.R;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.location.RadioButtonPreference;

/**
 * Settings fragment containing virtual key.
 */

public class VirtualKeyFragment extends SettingsPreferenceFragment implements AccessibilityRadioButtonPreference.OnClickListener {
    static final String TAG = "VirtualKeyFragment";
    static final String VIRTUAL_KEY_KEY = "navigation_bar_style";
    AccessibilityRadioButtonPreference virtualKeyGroup1;
    AccessibilityRadioButtonPreference virtualKeyGroup2;
    AccessibilityRadioButtonPreference virtualKeyGroup3;
    AccessibilityRadioButtonPreference virtualKeyGroup4;

    @Override
    protected int getMetricsCategory(){
        return MetricsEvent.SUW_ACCESSIBILITY;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_virtualkey);
        virtualKeyGroup1 = (AccessibilityRadioButtonPreference) findPreference("virtualkey_group1");
        virtualKeyGroup1.setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        virtualKeyGroup1.setOnClickListener(this);
        virtualKeyGroup2 = (AccessibilityRadioButtonPreference) findPreference("virtualkey_group2");
        virtualKeyGroup2.setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        virtualKeyGroup2.setOnClickListener(this);
        virtualKeyGroup3 = (AccessibilityRadioButtonPreference) findPreference("virtualkey_group3");
        virtualKeyGroup3.setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        virtualKeyGroup3.setOnClickListener(this);
        virtualKeyGroup4 = (AccessibilityRadioButtonPreference) findPreference("virtualkey_group4");
        virtualKeyGroup4.setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
        virtualKeyGroup4.setOnClickListener(this);
        switch (Settings.System.getInt(getContentResolver(),VIRTUAL_KEY_KEY, 0)){
            case 0:
            case 1:
                virtualKeyGroup1.setChecked(true);
                virtualKeyGroup2.setChecked(false);
                virtualKeyGroup3.setChecked(false);
                virtualKeyGroup4.setChecked(false);
                break;
            case 2:
                virtualKeyGroup1.setChecked(false);
                virtualKeyGroup2.setChecked(true);
                virtualKeyGroup3.setChecked(false);
                virtualKeyGroup4.setChecked(false);
                break;
            case 3:
                virtualKeyGroup1.setChecked(false);
                virtualKeyGroup2.setChecked(false);
                virtualKeyGroup3.setChecked(true);
                virtualKeyGroup4.setChecked(false);
                break;
            case 4:
                virtualKeyGroup1.setChecked(false);
                virtualKeyGroup2.setChecked(false);
                virtualKeyGroup3.setChecked(false);
                virtualKeyGroup4.setChecked(true);
                break;
        }

    }

//    @Override
//    public boolean onPreferenceClick(Preference preference) {
//        // TODO Auto-generated method stub
//        Log.i(TAG, "onPreferenceClick----->"+String.valueOf(preference.getKey()));
//        // 对控件进行操作
////        operatePreference(preference);
//        return false;
//    }

    @Override
    public void onRadioButtonClicked(AccessibilityRadioButtonPreference emiter) {
        int value = 0;
        if (emiter == virtualKeyGroup1) {
            virtualKeyGroup1.setChecked(true);
            virtualKeyGroup2.setChecked(false);
            virtualKeyGroup3.setChecked(false);
            virtualKeyGroup4.setChecked(false);
            value = 1;
        }else if(emiter == virtualKeyGroup2){
            virtualKeyGroup2.setChecked(true);
            virtualKeyGroup1.setChecked(false);
            virtualKeyGroup3.setChecked(false);
            virtualKeyGroup4.setChecked(false);
            value = 2;
        }else if(emiter == virtualKeyGroup3){
            virtualKeyGroup1.setChecked(false);
            virtualKeyGroup2.setChecked(false);
            virtualKeyGroup3.setChecked(true);
            virtualKeyGroup4.setChecked(false);
            value = 3;
        }else if(emiter == virtualKeyGroup4){
            virtualKeyGroup1.setChecked(false);
            virtualKeyGroup2.setChecked(false);
            virtualKeyGroup3.setChecked(false);
            virtualKeyGroup4.setChecked(true);
            value = 4;
        }
        Settings.System.putInt(getContentResolver(),VIRTUAL_KEY_KEY, value);
        Log.i(TAG, "onPreferenceClick--value-->"+ Settings.System.getInt(getContentResolver(),VIRTUAL_KEY_KEY, 0));
    }


}
