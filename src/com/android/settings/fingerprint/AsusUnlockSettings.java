package com.android.settings.fingerprint;

import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.CheckBoxPreference;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
/**
 * Created by yueting-wong on 2016/8/17.
 */
public class AsusUnlockSettings extends SettingsPreferenceFragment implements SwitchBar.OnSwitchChangeListener {

    private static final String TAG = "AsusUnlockSettings";
    public static final String UNLOCK_DEVICE_ENABLED = "unlock_device_with_fingerprint";
    private static final String WAKEUP_DEVICE_ENABLED = "persist.asus.fp.wakeup";

    private static final String KEY_FINGERPRINT_WAKEUP_SWITCH = "wakeup_with_fingerprint";

    private boolean WAKEUP_ENABLE_DEFAULT = AsusFindFingerprintSensorView.SENSOR_BACK
            .equals(SystemProperties.get("ro.hardware.fp_position")) ? true : false;

    private SwitchBar mSwitchBar;
    private Switch mSwitch;

    private CheckBoxPreference mWakeupDevice;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.asus_fingerprint_unlock_settings);

        mWakeupDevice = (CheckBoxPreference) findPreference(
                KEY_FINGERPRINT_WAKEUP_SWITCH);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        final String key = pref.getKey();
        if (KEY_FINGERPRINT_WAKEUP_SWITCH.equals(key)){
            if(mWakeupDevice.isEnabled()){
                SystemProperties.set(WAKEUP_DEVICE_ENABLED, String.valueOf(mWakeupDevice.isChecked()));
                //writeDB(WAKEUP_DEVICE_ENABLED, mWakeupDevice.isChecked());
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        handleMainSwitchStateChanged();
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    private void handleMainSwitchStateChanged() {
        boolean isChecke = Settings.Secure.getInt(
                getContentResolver(), UNLOCK_DEVICE_ENABLED, 1) != 0;
        setMainSwitchChecked(isChecke);
        updateSubPreferenceState(isChecke);
    }

    private void setMainSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mSwitch.setChecked(checked);
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT;
    }


    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        writeDB(UNLOCK_DEVICE_ENABLED, isChecked);
        updateSubPreferenceState(isChecked);
    }

    private void writeDB(String key, boolean isChecked){
        Settings.Secure.putInt(getContentResolver(), key, isChecked? 1: 0);
    }

    private void updateSubPreferenceState(boolean isEnable){
        if(mWakeupDevice != null){
            boolean isChecked = SystemProperties.getBoolean(WAKEUP_DEVICE_ENABLED, WAKEUP_ENABLE_DEFAULT);
            mWakeupDevice.setEnabled(isEnable);
            mWakeupDevice.setChecked(isChecked);
        }
    }
}
