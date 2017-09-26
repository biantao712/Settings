package com.android.settings.zenmotion;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.widget.SwitchBar;
import com.android.settings.zenmotion.AppLaunchListPreference.AppLaunchListSwitchChangedListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.android.settings.widget.SwitchBarWithFixedTitle; //for verizon
import com.android.settings.Utils;//for verizon 


public class AsusTouchSettings extends SettingsPreferenceFragment implements
        OnSharedPreferenceChangeListener, OnPreferenceChangeListener,
        SwitchBar.OnSwitchChangeListener, AppLaunchListSwitchChangedListener, Indexable {

	private SwitchBarWithFixedTitle mVZWSwitchBar;// for verizon
    private static final String TAG = "AsusTouchSettings";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_OPEN_ALL_FEATURE = false;
    private static final String KEY_WAKE_UP_CATEGORY = "asus_touch_gesture_wake_up_category";
    private static final String KEY_SLEEP_CATEGORY = "asus_touch_gesture_sleep_category";
    private static final String KEY_DOUBLE_TAP_ON = "double_tap_on";
    private static final String KEY_DOUBLE_TAP_OFF = "double_tap_off";
    private static final String KEY_SWIPE_UP_WAKE_UP = "swipe_up_to_wake_up";

    private static final String[] ALL_KEY_LAUNCH = {
            AppLaunchListPreference.KEY_W_LAUNCH,
            AppLaunchListPreference.KEY_S_LAUNCH,
            AppLaunchListPreference.KEY_E_LAUNCH,
            AppLaunchListPreference.KEY_C_LAUNCH,
            AppLaunchListPreference.KEY_Z_LAUNCH,
            AppLaunchListPreference.KEY_V_LAUNCH
    };

    private static final String PERSIST_ASUS_DLICK = "persist.asus.dclick";
    private static final String PERSIST_ASUS_GESTURE_TYPE = "persist.asus.gesture.type";
    private static final String DEFAULT_ASUS_GESTURE_TYPE = "1111111";
    private static final String DISABLE_ASUS_GESTURE_TYPE = "0000000";

    private static final int OP_ALL = 1 << 6;
    private static final int OP_W = 1 << 5;
    private static final int OP_S = 1 << 4;
    private static final int OP_E = 1 << 3;
    private static final int OP_C = 1 << 2;
    private static final int OP_Z = 1 << 1;
    private static final int OP_V = 1 << 0;


    // +++ Double tap mode settings
    private static final int DISABLE_DOUBLE_TAP_MODE = 0;
    private static final int ENABLE_DOUBLE_TAP_MODE = 1;
    private static final String ZEN_MOTION_DATA = "zen_motion_data";
    private static final String DOUBLE_TAP_ON_DATA = "double_tap_on";
    private static final String DOUBLE_TAP_OFF_DATA = "double_tap_off";

    // Swipe-up to wake up
    private static final String PERSIST_ASUS_SWIPE_UP = "persist.asus.swipeup";
    private static final int DISABLE_SWIPE_UP_MODE = 0;
    private static final int ENABLE_SWIPE_UP_MODE = 1;
    private static final String SWIPE_UP_DATA = "swipe_up_wake_up";

    // JimCC: Padding for separating the title and switch
    private static final int TEXT_VIEW_PADDING_END = 50;

    private OnSwitchChangedListener mCallBack;
    private SwitchPreference mDoubleTapOn = null;
    private SwitchPreference mDoubleTapOff = null;
    private SwitchPreference mSwipeUpWakeUp = null;
    private final List<AppLaunchListPreference> mListPreferences = new ArrayList<>(6);

    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private boolean mStateMachineEvent;
    private static int first_set =1 ;

    private SharedPreferences mZenMotionSharesPreferences;

    public interface OnSwitchChangedListener {
        public void onTouchSwitchChanged(boolean isChecked);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
		if(Utils.isVerizonSKU()){ //for verizon
		//if(!Utils.isVerizonSKU()){ //for verizon test
        	mCallBack = null;
			mVZWSwitchBar = ((SettingsActivity) getActivity()).getSwitchBarWithFixedTitle();
			mVZWSwitchBar.setTitle(getResources().getString(R.string.vzw_touch_gesture_summary));
			mVZWSwitchBar.setTitleTypeface(Typeface.NORMAL);
			mVZWSwitchBar.setTextViewPaddingRelative(
					mVZWSwitchBar.getTextViewPaddingStart(),
					mVZWSwitchBar.getTextViewPaddingTop(),
					TEXT_VIEW_PADDING_END,
					mVZWSwitchBar.getTextViewPaddingBottom());
		}
		else{
			mCallBack = new AsusZenMotionSettings();
			mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
		}
		if(Utils.isVerizonSKU()){ //for verizon
		//if(!Utils.isVerizonSKU()){ //for verizon test
mSwitch = mVZWSwitchBar.getSwitch();
			mVZWSwitchBar.show();
        	mSwitch = mVZWSwitchBar.getSwitch();
		}
		else{
	        mSwitch = mSwitchBar.getSwitch();
	        mSwitchBar.show();
		}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
		if(Utils.isVerizonSKU()) // for verizon
		//if(!Utils.isVerizonSKU()) // for verizon test 
	        mVZWSwitchBar.hide();
		else
	        mSwitchBar.hide();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		if(Utils.isVerizonSKU()) //for verizon
		//if(!Utils.isVerizonSKU()) //for verizon test
			addPreferencesFromResource(R.xml.vzw_touch_operation_settings);
		else
			addPreferencesFromResource(R.xml.asus_touch_operation_settings);

        final boolean isSupportDoubleTap = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP);

        final boolean isSupportGestureLunchApp = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP);

        final PreferenceCategory sleepCategory = (PreferenceCategory) findPreference(
                KEY_SLEEP_CATEGORY);
        mDoubleTapOn = (SwitchPreference) findPreference(KEY_DOUBLE_TAP_ON);
        mDoubleTapOff = (SwitchPreference) findPreference(KEY_DOUBLE_TAP_OFF);

        // Swipe up
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_SWIPE_UP)
                || DEBUG_OPEN_ALL_FEATURE) {
            if(Utils.isVerizonSKU()){ //for veizon
                removePreference(KEY_SWIPE_UP_WAKE_UP);
            }else
                mSwipeUpWakeUp = (SwitchPreference) findPreference(KEY_SWIPE_UP_WAKE_UP);
        } else {
            removePreference(KEY_SWIPE_UP_WAKE_UP);
        }


        if (!isSupportDoubleTap && !DEBUG_OPEN_ALL_FEATURE) {
            removePreference(KEY_WAKE_UP_CATEGORY);
            sleepCategory.removePreference(mDoubleTapOn);
        }

        if (isSupportGestureLunchApp || DEBUG_OPEN_ALL_FEATURE) {
            initAllListPreference();
        } else {
            if (isSupportDoubleTap) {
                // remove all the touch gesture
                sleepCategory.removeAll();
            } else {
                removePreference(KEY_SLEEP_CATEGORY);
            }
        }

        mZenMotionSharesPreferences = getActivity().getSharedPreferences(ZEN_MOTION_DATA,
                Context.MODE_PRIVATE);
    }

    private void handleMainSwitchStateChanged() {
            boolean enable = getSystemPropGestureIsChecked()
                    || readDoubleTapOn() == ENABLE_DOUBLE_TAP_MODE
                    || readDoubleTapOff() == ENABLE_DOUBLE_TAP_MODE
                    || readSwipeUpWakeUp() == ENABLE_SWIPE_UP_MODE;
            setMainSwitchChecked(enable);
            setIsEnableView(enable);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        handleMainSwitchStateChanged();
        if (null != mDoubleTapOn) {
            int value = mSwitch.isChecked() ? readDoubleTapOn() : loadLastDoubleTapOn();
            mDoubleTapOn.setChecked(value == ENABLE_DOUBLE_TAP_MODE);
        }
        if (null != mDoubleTapOff) {
            int value = mSwitch.isChecked() ? readDoubleTapOff() : loadLastDoubleTapOff();
            mDoubleTapOff.setChecked(value == ENABLE_DOUBLE_TAP_MODE);
        }
        if (null != mSwipeUpWakeUp) {
            int value = mSwitch.isChecked() ? readSwipeUpWakeUp() : loadLastSwipeUpWakeUp();
            mSwipeUpWakeUp.setChecked(value == ENABLE_SWIPE_UP_MODE);
        }
		if(Utils.isVerizonSKU()){ //for veizon
		//if(!Utils.isVerizonSKU()){ //for veizon test
			mVZWSwitchBar.addOnSwitchChangeListener(this);
			}
		else
			mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
		if(Utils.isVerizonSKU()) //for verizon
		//if(!Utils.isVerizonSKU()) //for verizon test
			mVZWSwitchBar.removeOnSwitchChangeListener(this);
		else
			mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mDoubleTapOn) {
            if (mDoubleTapOn.isChecked()) {
                mDoubleTapOn.setChecked(false);
                createDoubleTapOnModeConfirmDialog().show();
            } else {
                writeDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
                saveLastDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
            }
        } else if (preference == mDoubleTapOff) {
            if (mDoubleTapOff.isChecked()) {
                mDoubleTapOff.setChecked(false);
                createDoubleTapOffModeConfirmDialog().show();
            } else {
                writeDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
                saveLastDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
            }
        } else if (preference == mSwipeUpWakeUp) {
            if (mSwipeUpWakeUp.isChecked()) {
                writeSwipeUpWakeUp(ENABLE_SWIPE_UP_MODE);
                saveLastSwipeUpWakeUp(ENABLE_SWIPE_UP_MODE);
            } else {
                writeSwipeUpWakeUp(DISABLE_SWIPE_UP_MODE);
                saveLastSwipeUpWakeUp(DISABLE_SWIPE_UP_MODE);
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
    }

    private void initAllListPreference() {
        // Get persist.asus.gesture.type and ensure it is not null and its length == 7
        String type = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE);
        if (type.length() != 7) {
            type = DEFAULT_ASUS_GESTURE_TYPE;
            writeSystemPropGesture(type);

        }
        mListPreferences.clear();
        for (String key : ALL_KEY_LAUNCH) {
            AppLaunchListPreference listPreference = (AppLaunchListPreference) findPreference(key);
            listPreference.setOnPreferenceChangeListener(this);
            listPreference.setAppLaunchListSwitchChangedListener(this);
            listPreference.setSummary(R.string.loading_notification_apps);
            listPreference.setEnabled(false);
            switch (listPreference.getKey()) {
                case AppLaunchListPreference.KEY_W_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(1)));
                    break;
                case AppLaunchListPreference.KEY_S_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(2)));
                    break;
                case AppLaunchListPreference.KEY_E_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(3)));
                    break;
                case AppLaunchListPreference.KEY_C_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(4)));
                    break;
                case AppLaunchListPreference.KEY_Z_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(5)));
                    break;
                case AppLaunchListPreference.KEY_V_LAUNCH:
                    listPreference.setSwitchChecked('1' == (type.charAt(6)));
                    break;
            }
            listPreference.setPackageNames();
            mListPreferences.add(listPreference);
        }
    }

    private int readDoubleTapOn() {
        return SystemProperties.getInt(PERSIST_ASUS_DLICK, DISABLE_DOUBLE_TAP_MODE);
    }

    private int readDoubleTapOff() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_DOUBLE_TAP, DISABLE_DOUBLE_TAP_MODE);
    }
    private int readFirststepup() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_FIRST_SET, DISABLE_DOUBLE_TAP_MODE);
    }

    private int readSwipeUpWakeUp() {
        return SystemProperties.getInt(PERSIST_ASUS_SWIPE_UP, DISABLE_SWIPE_UP_MODE);
    }

    private void writeDoubleTapOn(int value) {
        try {
            log("Write double tap on value--" + value);
            SystemProperties.set(PERSIST_ASUS_DLICK, Integer.toString(value));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    private void writeDoubleTapOff(int value) {
        log("Write double tap off value--" + Settings.System.ASUS_DOUBLE_TAP + ": " + value);
        Settings.System.putInt(getContentResolver(), Settings.System.ASUS_DOUBLE_TAP, value);
    }
    private void writeFirstSet(int value) {
        log("Write first setup value--" + Settings.System.ASUS_FIRST_SET + ": " + value);
        Settings.System.putInt(getContentResolver(), Settings.System.ASUS_FIRST_SET, value);
    }

    private void writeSwipeUpWakeUp(int value) {
        try {
            log("Write Swipe up to wake up value--" + value);
            SystemProperties.set(PERSIST_ASUS_SWIPE_UP, Integer.toString(value));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    private int loadLastDoubleTapOn() {
        return mZenMotionSharesPreferences.getInt(DOUBLE_TAP_ON_DATA, DISABLE_DOUBLE_TAP_MODE);
    }

    private int loadLastDoubleTapOff() {
        return mZenMotionSharesPreferences.getInt(DOUBLE_TAP_OFF_DATA, DISABLE_DOUBLE_TAP_MODE);
    }

    private int loadLastSwipeUpWakeUp() {
        return mZenMotionSharesPreferences.getInt(SWIPE_UP_DATA, DISABLE_SWIPE_UP_MODE);
    }

    private void saveLastDoubleTapOn(int value) {
        mZenMotionSharesPreferences.edit().putInt(DOUBLE_TAP_ON_DATA, value).apply();
    }

    private void saveLastDoubleTapOff(int value) {
        mZenMotionSharesPreferences.edit().putInt(DOUBLE_TAP_OFF_DATA, value).apply();
    }

    private void saveLastSwipeUpWakeUp(int value) {
        mZenMotionSharesPreferences.edit().putInt(SWIPE_UP_DATA, value).apply();
    }

    private String convertToFormatString(int value) {
        return String.format("%7s", Integer.toBinaryString(value)).replace(' ', '0');
    }

    private void writeAllDB(boolean checked) {
        int decimalValue = Integer.parseInt(SystemProperties.get(
                PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE), 2);

        if (checked) {
            decimalValue = decimalValue | OP_ALL;
            writeDoubleTapOn(loadLastDoubleTapOn());
            writeDoubleTapOff(loadLastDoubleTapOff());
            writeSwipeUpWakeUp(loadLastSwipeUpWakeUp());
        } else {
            decimalValue = decimalValue & ~OP_ALL;
            saveLastDoubleTapOn(readDoubleTapOn());
            saveLastDoubleTapOff(readDoubleTapOff());
            saveLastSwipeUpWakeUp(readSwipeUpWakeUp());
            writeDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
            writeDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
            writeSwipeUpWakeUp(DISABLE_SWIPE_UP_MODE);
        }
        writeSystemPropGesture(convertToFormatString(decimalValue));
    }

    private void writeAppGestureDB(Preference preference, Object newValue) {
        int operation = 0;
        for (AppLaunchListPreference listPreference : mListPreferences) {
            if (listPreference.isChecked()) {
                String key = listPreference.getKey();
                if (AppLaunchListPreference.KEY_W_LAUNCH.equals(key)) operation |= OP_W;
                else if (AppLaunchListPreference.KEY_S_LAUNCH.equals(key)) operation |= OP_S;
                else if (AppLaunchListPreference.KEY_E_LAUNCH.equals(key)) operation |= OP_E;
                else if (AppLaunchListPreference.KEY_C_LAUNCH.equals(key)) operation |= OP_C;
                else if (AppLaunchListPreference.KEY_Z_LAUNCH.equals(key)) operation |= OP_Z;
                else if (AppLaunchListPreference.KEY_V_LAUNCH.equals(key)) operation |= OP_V;
            }
        }
        writeSystemPropGesture((0 == operation)
                ? DISABLE_ASUS_GESTURE_TYPE
                : Integer.toBinaryString(OP_ALL | operation));
    }

    private void writeSystemPropGesture(String opString) {
        log("Write system property--" + PERSIST_ASUS_GESTURE_TYPE + " : " + opString);
        try {
            SystemProperties.set(PERSIST_ASUS_GESTURE_TYPE, opString);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof AppLaunchListPreference && newValue != null) {
            AppLaunchListPreference listPreference = (AppLaunchListPreference) preference;
            String appSettingKey = listPreference.getSettingsSystemKey();
            String appSettingValue = newValue.toString();
            log("onPreferenceChange AppLaunchListPreference newValue: Change AppLaunch--"
                    + appSettingKey + ": " + appSettingValue);
            Settings.System.putString(getContentResolver(), appSettingKey, appSettingValue);
            listPreference.updateEntry(newValue.toString());
            if (!listPreference.isChecked()) {
                listPreference.setSwitchChecked(true);
            }
            writeAppGestureDB(preference, newValue);
        }
        return false;
    }

    private AlertDialog createDoubleTapOnModeConfirmDialog() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        log("DoubleTapOnModeConfirmDialog PositiveButton onclick");
                        writeDoubleTapOn(ENABLE_DOUBLE_TAP_MODE);
                        saveLastDoubleTapOn(ENABLE_DOUBLE_TAP_MODE);
                        mDoubleTapOn.setChecked(true);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        log("DoubleTapOnModeConfirmDialog NegativeButton onclick");
                        writeDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
                        saveLastDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
                        mDoubleTapOn.setChecked(false);
                        break;
                }
            }
        };

        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.double_tap_on_confirm_dialog_message)
                .setTitle(R.string.double_tap_on_confirm_dialog_title)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, listener)
                .create();
    }

    private AlertDialog createDoubleTapOffModeConfirmDialog() {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        log("DoubleTapOffModeConfirmDialog PositiveButton onclick");
                        writeDoubleTapOff(ENABLE_DOUBLE_TAP_MODE);
                        saveLastDoubleTapOff(ENABLE_DOUBLE_TAP_MODE);
                        mDoubleTapOff.setChecked(true);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        log("DoubleTapOffModeConfirmDialog NegativeButton onclick");
                        writeDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
                        saveLastDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
                        mDoubleTapOff.setChecked(false);
                        break;
                }
            }
        };

        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.double_tap_off_confirm_dialog_message)
                .setTitle(R.string.double_tap_off_confirm_dialog_title)
                .setPositiveButton(android.R.string.yes, listener)
                .setNegativeButton(android.R.string.no, listener)
                .create();
    }

    private void setIsEnableView(boolean isEnable) {
        if (null != mDoubleTapOn) {
            mDoubleTapOn.setEnabled(isEnable);
        }
        if (null != mDoubleTapOff) {
            mDoubleTapOff.setEnabled(isEnable);
        }
        if (null != mSwipeUpWakeUp) {
            mSwipeUpWakeUp.setEnabled(isEnable);
        }
        if (null != mListPreferences) {
            for (AppLaunchListPreference listPreference : mListPreferences) {
                listPreference.setEnabled(isEnable);
            }
        }
    }

    private void setMainSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }

    // for Main Switch
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        if (mStateMachineEvent) return;
        if (readFirststepup()==1){
            if(isChecked){
                String gesture="1111111";
                writeDoubleTapOff(1);
                writeSystemPropGesture(gesture);
                boolean enable = getSystemPropGestureIsChecked()
                    || readDoubleTapOn() == ENABLE_DOUBLE_TAP_MODE
                    || readDoubleTapOff() == ENABLE_DOUBLE_TAP_MODE
                    || readSwipeUpWakeUp() == ENABLE_SWIPE_UP_MODE;
                setMainSwitchChecked(enable);
               /* saveLastSwipeUpWakeUp(ENABLE_DOUBLE_TAP_MODE);
                mSwipeUpWakeUp.setChecked(true);
                mSwipeUpWakeUp.setEnabled(true);*/
                saveLastDoubleTapOff(ENABLE_DOUBLE_TAP_MODE);
                mDoubleTapOff.setChecked(true);
                mDoubleTapOff.setEnabled(true);
                if(Utils.isVerizonSKU()) { //for veizon
                    writeDoubleTapOn(1);
                    saveLastDoubleTapOn(ENABLE_DOUBLE_TAP_MODE);
                    mDoubleTapOn.setChecked(true);
                }else {
                    writeSwipeUpWakeUp(1);
                    saveLastSwipeUpWakeUp(ENABLE_DOUBLE_TAP_MODE);
                    mSwipeUpWakeUp.setChecked(true);
                    mSwipeUpWakeUp.setEnabled(true);
                }
                mDoubleTapOn.setEnabled(true);

                // mListPreferences.clear();
                for (String key : ALL_KEY_LAUNCH) {
                    AppLaunchListPreference listPreference = (AppLaunchListPreference) findPreference(key);
                    // listPreference.setOnPreferenceChangeListener(this);
                    //listPreference.setAppLaunchListSwitchChangedListener(this);
                    // listPreference.setSummary(R.string.loading_notification_apps);
                    listPreference.setEnabled(true);
                    switch (listPreference.getKey()) {
                        case AppLaunchListPreference.KEY_W_LAUNCH:
                            listPreference.setSwitchChecked('1' == (gesture.charAt(1)));
                            break;
                        case AppLaunchListPreference.KEY_S_LAUNCH:
                            listPreference.setSwitchChecked('1' == (gesture.charAt(2)));
                            break;
                        case AppLaunchListPreference.KEY_E_LAUNCH:
                            listPreference.setSwitchChecked('1' == (gesture.charAt(3)));
                            break;
                        case AppLaunchListPreference.KEY_C_LAUNCH:
                            listPreference.setSwitchChecked('1' == (gesture.charAt(4)));
                            break;
                        case AppLaunchListPreference.KEY_Z_LAUNCH:
                            listPreference.setSwitchChecked('1' == (gesture.charAt(5)));
                            break;
                        case AppLaunchListPreference.KEY_V_LAUNCH:
                            listPreference.setSwitchChecked('1' == (gesture.charAt(6)));
                            break;
                    }
                    //  listPreference.setPackageNames();
                    //  mListPreferences.add(listPreference);
                }
                for (AppLaunchListPreference listPreference : mListPreferences) {
                    listPreference.setEnabled(isChecked);
                }
                if (!Utils.isVerizonSKU()) {
                    mCallBack.onTouchSwitchChanged(isChecked);
                }
            }else{
                writeAllDB(isChecked);
                setIsEnableView(isChecked);
                if (!Utils.isVerizonSKU()) {
                    mCallBack.onTouchSwitchChanged(isChecked);
                }
            }
            writeFirstSet(0);
        }else {
            writeAllDB(isChecked);
            setIsEnableView(isChecked);
            if (!Utils.isVerizonSKU()) {
                mCallBack.onTouchSwitchChanged(isChecked);
            }
        }
    }

    @Override
    protected int getMetricsCategory() {
        return com.android.internal.logging.MetricsProto.MetricsEvent.MAIN_SETTINGS;
    }

    @Override
    public void onAppLaunchListSwitchChanged(String key, boolean isChecked) {
        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE,
                DEFAULT_ASUS_GESTURE_TYPE);
        StringBuilder opStringBuilder = new StringBuilder(opString);
        switch (key) {
            case AppLaunchListPreference.KEY_W_LAUNCH:
                opStringBuilder.setCharAt(1, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_S_LAUNCH:
                opStringBuilder.setCharAt(2, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_E_LAUNCH:
                opStringBuilder.setCharAt(3, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_C_LAUNCH:
                opStringBuilder.setCharAt(4, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_Z_LAUNCH:
                opStringBuilder.setCharAt(5, isChecked ? '1' : '0');
                break;
            case AppLaunchListPreference.KEY_V_LAUNCH:
                opStringBuilder.setCharAt(6, isChecked ? '1' : '0');
                break;
            default:
                break;
        }

        int decimalValue = Integer.parseInt(opStringBuilder.toString(), 2);
        if (OP_ALL == decimalValue) { // 1000000
            opString = DISABLE_ASUS_GESTURE_TYPE;
        } else {
            if (decimalValue < OP_ALL && decimalValue > 0) { // 0xxxxxx -> 1xxxxxx
                opStringBuilder.setCharAt(0, '1');
            }
            opString = opStringBuilder.toString();
        }
        writeSystemPropGesture(String.format("%7s", opString.replace(' ', '0')));
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
                sir.xmlResId = R.xml.asus_touch_operation_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                PackageManager pm = context.getPackageManager();
                if (!pm.hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP)) {
                    keys.add(KEY_DOUBLE_TAP_ON);
                    keys.add(KEY_DOUBLE_TAP_OFF);
                }
                if (!pm.hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP)) {
                    for (String key : ALL_KEY_LAUNCH) {
                        keys.add(key);
                    }
                }
                return keys;
            }
        };
    }

    protected static boolean getSystemPropGestureIsChecked() {
        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE,DEFAULT_ASUS_GESTURE_TYPE);
        return (Integer.parseInt(opString, 2) & OP_ALL) != 0;
    }

    private static void log(String msg) {
        if (DEBUG) {
            Log.d(TAG, msg);
        }
    }
}
