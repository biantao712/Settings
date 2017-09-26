
package com.android.settings.zenmotion;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.internal.logging.MetricsProto.MetricsEvent;

public class AsusMotionSettings extends SettingsPreferenceFragment implements OnSharedPreferenceChangeListener
,OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener, Indexable {

    private static final String TAG = "AsusMotionSettings";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_OPEN_ALL_FEATURE = false;
    private static final String KEY_SHAKE_LAUNCH = "shake_launch";
    private static final String KEY_SHAKE_SENSITIVITY = "shake_sensitivity";
    private static final String KEY_FLIP = "motion_flip";
    private static final String KEY_HAND_UP = "motion_hand_up";
    private static final String KEY_DOUBLE_CLICK = "motion_double_click";
    private static final String KEY_WALKING = "motion_walking";
    private static final String KEY_TUTORIAL = "asus_motion_tutorial";
    private static final String ZEN_MOTION_DATA = "zen_motion_data";
    private static final String SHAKE_SHAKE_DATA = "shake_shake";
    private static final String MOTION_GESTURE_BROADCAST_MESSAGE = "com.android.settings.MOTION_GESTURE";
    private static final String DO_IT_LATER="com.asus.task";
    private static final int SWITCH_OFF = 0;
    private static final int SWITCH_ON = 1;

    private OnSwitchChangedListener mCallBack;

    private SharedPreferences mZenMotionSharesPreferences;

    private SwitchPreference mShakeSwitch = null;
    private ListPreference mShakeSensitivityListPreference = null;
    private SwitchPreference mFlipSwitchPreference = null;
    private SwitchPreference mHandUpSwitchPreference = null;
    private SwitchPreference mDoubleClickSwitchPreference = null;
    private SwitchPreference mWalkingSwitchPreference = null;
    private Intent mMotionGestureIntent = new Intent(MOTION_GESTURE_BROADCAST_MESSAGE);

    private SwitchBar mSwitchBar;
    private Switch mSwitch;

    public interface OnSwitchChangedListener {

        public void onMotionSwitchChanged(boolean isChecked);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final AsusZenMotionSettings fragment = new AsusZenMotionSettings();
        mCallBack = (OnSwitchChangedListener) fragment;

        mSwitchBar = activity.getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        // TODO Auto-generated method stub
        super.onDestroyView();
        mSwitchBar.hide();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.asus_motion_operation_settings);

        // Shake Shake
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE_FLICK)
                || DEBUG_OPEN_ALL_FEATURE) {
            mShakeSwitch = (SwitchPreference) findPreference(KEY_SHAKE_LAUNCH);

            // mShakeSensitivityListPreference = (ListPreference)
            // findPreference(KEY_SHAKE_SENSITIVITY);
            removePreference(KEY_SHAKE_SENSITIVITY);

            // mShakeSensitivityListPreference.setOnPreferenceChangeListener(this);
            // updateShakeSensitivity();
        } else {

            // remove mShakeSwitch and mShakeSensitivityListPreference
            removePreference(KEY_SHAKE_LAUNCH);
            removePreference(KEY_SHAKE_SENSITIVITY);
        }

        //if DO_IT_LATER doesn't install, remove shake shake item
        if (null == getPackageManager().getLaunchIntentForPackage(DO_IT_LATER)) {
            removePreference(KEY_SHAKE_LAUNCH);
            removePreference(KEY_SHAKE_SENSITIVITY);
        }

        // Flip
        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_SENSOR_SERVICE_TERMINAL)
                || DEBUG_OPEN_ALL_FEATURE) {
            mFlipSwitchPreference = (SwitchPreference) findPreference(KEY_FLIP);
        } else {
            removePreference(KEY_FLIP);
        }

        // Hand up
        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_SENSOR_SERVICE_EARTOUCH)
                || DEBUG_OPEN_ALL_FEATURE) {
            mHandUpSwitchPreference = (SwitchPreference) findPreference(KEY_HAND_UP);
        } else {
            removePreference(KEY_HAND_UP);
        }

        // Double click
        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_SENSOR_SERVICE_TAPPING)
                || DEBUG_OPEN_ALL_FEATURE) {
            mDoubleClickSwitchPreference = (SwitchPreference) findPreference(KEY_DOUBLE_CLICK);
        } else {
            removePreference(KEY_DOUBLE_CLICK);
        }

        // Walking
        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_SENSOR_SERVICE_INSTANTACTIVITY)
                || DEBUG_OPEN_ALL_FEATURE) {
            mWalkingSwitchPreference = (SwitchPreference) findPreference(KEY_WALKING);
        } else {
            removePreference(KEY_WALKING);
        }

        mZenMotionSharesPreferences = getActivity().getSharedPreferences(ZEN_MOTION_DATA,
                Context.MODE_PRIVATE);

        // JimCC: hide tutorial if all features are disabled during developing
        // Hide it will prevent crash if no feature is available when user clicks the tutorial
        // TODO: Always check the "if" statement => should match the cases
        if (null != mShakeSwitch || null != mShakeSensitivityListPreference
                || null != mFlipSwitchPreference || null != mHandUpSwitchPreference
                || null != mDoubleClickSwitchPreference || null != mWalkingSwitchPreference) {
        } else {
            removePreference(KEY_TUTORIAL);
        }

    }

    @Override
    public void onStart() {
        super.onStart();

    }

    private void handleShakeShakeStateChanged() {
        //read settings provider : read SharesPreference
//        boolean shakeEnabled = mSwitch.isChecked() ? getShakeShakeState(Settings.System.ASUS_MOTION_SHAKE)
//                : SWITCH_ON == mZenMotionSharesPreferences.getInt(SHAKE_SHAKE_DATA, SWITCH_OFF);

        mShakeSwitch.setChecked(getShakeShakeState(Settings.System.ASUS_MOTION_SHAKE));
    }

    private void handleFlipStateChanged() {

        mFlipSwitchPreference.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_MOTION_FLIP, 0) == 1);
    }

    private void handleHandUpStateChanged() {
        mHandUpSwitchPreference.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_MOTION_HAND_UP, 0) == 1);
    }

    private void handleDoubleClickStateChanged() {
        mDoubleClickSwitchPreference.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_MOTION_DOUBLE_CLICK, 0) == 1);
    }

    private void handleWalkingStateChanged() {
        mWalkingSwitchPreference.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_MOTION_WALKING, 0) == 1);
    }

    private void handleMainSwitchStateChanged() {
        boolean isChecked = Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_MOTION_GESTURE_SETTINGS, 0) == 1;

        mSwitch.setChecked(isChecked);

        setIsEnableView(isChecked);
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        handleMainSwitchStateChanged();

        if(null != mShakeSwitch){
            handleShakeShakeStateChanged();
        }

        if (null != mFlipSwitchPreference) {
            handleFlipStateChanged();
        }

        if (null != mHandUpSwitchPreference) {
            handleHandUpStateChanged();
        }

        if (null != mDoubleClickSwitchPreference) {
            handleDoubleClickStateChanged();
        }

        if (null != mWalkingSwitchPreference) {
            handleWalkingStateChanged();
        }

        mSwitchBar.addOnSwitchChangeListener(this);
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwitchBar.removeOnSwitchChangeListener(this);
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        if (preference == mShakeSwitch) {
            int shakeCheck = Settings.System.getInt(getContentResolver(),
                    Settings.System.ASUS_MOTION_SHAKE, 0);
            int newShakeCheck = mShakeSwitch.isChecked() ? 1 : 0;
            if (newShakeCheck != shakeCheck) {

                Settings.System.putInt(getContentResolver(),
                        Settings.System.ASUS_MOTION_SHAKE, newShakeCheck);
                getActivity().sendBroadcast(mMotionGestureIntent);

                Log.d(TAG, "AsusMotionSettings--write ASUS_MOTION_SHAKE value: " + newShakeCheck);

            }
        }

        if (preference == mFlipSwitchPreference) {
            writeFlipDB(((SwitchPreference) preference).isChecked()? 1: 0);
        }

        if (preference == mHandUpSwitchPreference) {
            writeHandUpDB(((SwitchPreference) preference).isChecked()? 1: 0);
        }

        if (preference == mDoubleClickSwitchPreference) {
            writeDoubleClickDB(((SwitchPreference) preference).isChecked()? 1: 0);
        }

        if (preference == mWalkingSwitchPreference) {
            writeWalkingDB(((SwitchPreference) preference).isChecked()? 1: 0);
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {

    }

    private boolean getShakeShakeState(String name) {
        try {
            return Settings.System.getInt(getContentResolver(), name) == 1;
        } catch (SettingNotFoundException snfe) {
            return false;
        }
    }

    private void updateShakeSensitivityView() {
        if (null != mShakeSensitivityListPreference) {
            ListPreference preference = mShakeSensitivityListPreference;
            int value = Settings.System.getInt(getContentResolver(),
                        Settings.System.ASUS_SHAKE_SENSITIVITY, 2);
            int index = value != 0 ? value - 1 : 0;
            if (DEBUG) Log.i(TAG, "updateShakeSensitivity()...value: " + value);

            preference.setValueIndex(index);
            preference.setSummary(preference.getEntry());
        }

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // TODO Auto-generated method stub
        final String key = preference.getKey();

        if (KEY_SHAKE_SENSITIVITY.equals(key)) {
            try {
                int value = Integer.parseInt((String) newValue);
                if (DEBUG) Log.i(TAG, "ASUS_SHAKE_SENSITIVITY...newValue: " + value);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ASUS_SHAKE_SENSITIVITY, value);
                updateShakeSensitivityView();
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }

        return false;
    }

    @Override
    public void onSwitchChanged(Switch switchView, final boolean isChecked) {
        // TODO Auto-generated method stub

        setSwitchChange(isChecked ? 1 : 0);
        setIsEnableView(isChecked);

        mCallBack.onMotionSwitchChanged(isChecked);
    }

    private void setSwitchChange(int value) {
        writeMotionGestureSettingsDB(value);
        getActivity().sendBroadcast(mMotionGestureIntent);
//        writeShakeShakeDB(value);
    }

    private void writeShakeShakeDB(int value) {
        int savedDBValue = value;

        int defShakeDBValue = Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_MOTION_SHAKE, SWITCH_OFF);
        // ON
        if (SWITCH_ON == value) {
            savedDBValue = mZenMotionSharesPreferences.getInt(SHAKE_SHAKE_DATA, defShakeDBValue);
        } else { // OFF
            mZenMotionSharesPreferences.edit().putInt(SHAKE_SHAKE_DATA, defShakeDBValue)
                    .commit();
        }
        Log.d(TAG, "AsusMotionSettings SwitchChange--write ASUS_MOTION_SHAKE value: "
                + savedDBValue);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ASUS_MOTION_SHAKE, savedDBValue);

        handleShakeShakeStateChanged();
    }

    private void writeFlipDB(int value) {
        Log.d(TAG,"Write motion flip value--"+value);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ASUS_MOTION_FLIP, value);
    }

    private void writeHandUpDB(int value) {
        Log.d(TAG,"Write motion hand up value--"+value);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ASUS_MOTION_HAND_UP, value);
    }

    private void writeDoubleClickDB(int value) {
        Log.d(TAG,"Write motion double click value--"+value);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ASUS_MOTION_DOUBLE_CLICK, value);
    }

    private void writeWalkingDB(int value) {
        Log.d(TAG,"Write motion walking value--"+value);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ASUS_MOTION_WALKING, value);
    }

    // For main switch
    private void writeMotionGestureSettingsDB(int value) {
        try {
            Log.d(TAG, "AsusMotionSettings-- write ASUS_MOTION_GESTURE_SETTINGS value: " + value);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ASUS_MOTION_GESTURE_SETTINGS, value);
        } catch (NumberFormatException e) {
            Log.e(TAG, "could not persist screen timeout setting", e);
        }
    }

    private void setIsEnableView(boolean isEnable){

        if (null != mShakeSwitch) mShakeSwitch.setEnabled(isEnable);
        if (null != mFlipSwitchPreference) mFlipSwitchPreference.setEnabled(isEnable);
        if (null != mHandUpSwitchPreference) mHandUpSwitchPreference.setEnabled(isEnable);
        if (null != mDoubleClickSwitchPreference) mDoubleClickSwitchPreference.setEnabled(isEnable);
        if (null != mWalkingSwitchPreference) mWalkingSwitchPreference.setEnabled(isEnable);

    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER;
    static {
        SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.asus_motion_operation_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                PackageManager pm = context.getPackageManager();
                keys.add(KEY_SHAKE_SENSITIVITY);
                if (!pm.hasSystemFeature(PackageManager.FEATURE_ASUS_SENSOR_SERVICE)) {
                    keys.add(KEY_SHAKE_LAUNCH);

                }

                return keys;
            }

        };
    }

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsEvent.MAIN_SETTINGS;
    }
}
