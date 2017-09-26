package com.android.settings.zenmotion2;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.os.SystemProperties;

import android.provider.Settings;
import android.provider.SearchIndexableResource;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.os.UserHandle;
import android.telephony.TelephonyManager;

import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.internal.logging.MetricsProto.MetricsEvent;

public class AsusZenMotion2Settings extends SettingsPreferenceFragment implements
        OnPreferenceClickListener, OnSharedPreferenceChangeListener{
    private static final String TAG = "AsusZenMotion2Settings";
    private static final boolean DEBUG = false;
    private static final boolean DEBUG_OPEN_ALL_FEATURE = false;
	private Context mContext;

    //private static final String KEY_MOTION_GESTURE = "asus_motion_gesture";
    private static final String KEY_TOUCH_GESTURE = "asus_touch_gesture";
    private static final String KEY_ONEHAND_MODE = "asus_onehand_mode";
    //private static ZenMotionGestureSwitchPreference sMotionPreference = null;
    //private static ZenTouchGestureSwitchPreference sTouchPreference = null;
    //private static Preference mOneHandMode = null;
    
    private static final String APP_ASUS_BOOSTER = "taskmanager";
    private static final String APP_FRONT_CAMERA = "frontCamera";
    private static final String APP_CAMERA_PACKAGE = "com.asus.camera";
    private static final String SYMBOL = "/";
    private static final String WAKE_UP_SCREEN = "wakeUpScreen";

    static final String KEY_W_LAUNCH = "w_launch";
    static final String KEY_S_LAUNCH = "s_launch";
    static final String KEY_E_LAUNCH = "e_launch";
    static final String KEY_C_LAUNCH = "c_launch";
    static final String KEY_Z_LAUNCH = "z_launch";
    static final String KEY_V_LAUNCH = "v_launch";
    //Preference key
    static final String KEY_CURRENT_GESTURE_LAUNCH = "current_gesture_launch";
    static final String KEY_GESTURE_TUTORIAL = "gesture_tutorial";
    public static final String KEY_ASUS_GESTURE_QUICK_START_APP = "asus_gesture_quick_start_app";
    public static final String KEY_ASUS_TOUCH_GESTURE_SLEEP_CATEGORY = "asus_touch_gesture_sleep_category";
    private static final String KEY_ASUS_TOUCH_GESTURE_CATEGORY = "asus_touch_gesture_category";//for three touch gestures 
    public static final String KEY_ASUS_GESTURE_APP = "asus_gesture_app";
	//ShardedPreferences key
	private SharedPreferences mZenMotion2SharesPreferences;
	SwitchPreference mSwitchDoubleTapOff=null;
	SwitchPreference mSwitchDoubleTapOn=null;
	SwitchPreference mSwitchSwipeUp=null;
	Preference mQuickStartApp=null;
	private static final String ZEN_MOTION2_DATA = "zen_motion2_data";	
	private static final String QUICK_START_ENABLE = "quick_start_enable";
	private static final String KEY_DOUBLE_TP_OFF = "double_tap_off";
	private static final String KEY_DOUBLE_TP_ON = "double_tap_on";
	private static final String KEY_SWIPE_UP = "swipe_up_to_wake_up";
	private static final String KEY_QUICK_START_APP="asus_gesture_quick_start_app";
    // BEGIN One-hand control
    private static final String KEY_ONEHAND_CTRL_CATEGORY = "onehand_ctrl_category";
    private static final String KEY_ONEHAND_CTRL_QUICK_TRIGGER = "onehand_ctrl_quick_trigger";
    private SwitchPreference mOneHandCtrlQuickTrigger;
    private int mOneHandCtrlQuickTriggerByDefault = 0;
    private boolean mOneHandCtrlFeatureEnabled = false;
    // END 
    //Begin motion gesture    
    private SwitchPreference mHandUpSwitchPreference = null;
	private SwitchPreference mFlipSwitchPreference = null;
	private static final String KEY_MOTION_GESTURE = "motion_gesture";
    private static final String KEY_FLIP = "motion_flip";
    private static final String KEY_HAND_UP = "motion_hand_up";
    //end
	
	//system proterty
	//double tap on
    private static final String PERSIST_ASUS_DLICK = "persist.asus.dclick";
    private static final int DISABLE_DOUBLE_TAP_MODE = 0;
    private static final int ENABLE_DOUBLE_TAP_MODE = 1;
	//Swipe-up to wake up
    private static final String PERSIST_ASUS_SWIPE_UP = "persist.asus.swipeup";
    private static final int DISABLE_SWIPE_UP_MODE = 0;
    private static final int ENABLE_SWIPE_UP_MODE = 1;

	//private member
	static boolean mGestureQuickStartOn=false;
	static String currentGesture;
    
    

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        //setDefaultGestureAppData(getContext());
        //mGestureAppPreference = getActivity().getSharedPreferences(KEY_ASUS_TOUCH_GESTURE_SLEEP_CATEGORY,Context.MODE_PRIVATE);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
	super.onCreate(savedInstanceState);
	mContext=getActivity();
	addPreferencesFromResource(R.xml.zenmotion2_gesturemotion);
    //check system hw feature    
    //Double tap on
		final boolean isSupportDoubleTap = getPackageManager().hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_DOUBLE_TAP);		
	//Swipe up
		final boolean isSupportSwipeUp = getPackageManager().hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_SWIPE_UP);
	//Gesture character
		final boolean isSupportGestureLunchApp = getPackageManager().hasSystemFeature(PackageManager.FEATURE_ASUS_TOUCHGESTURE_LAUNCH_APP);
		
	//end
		//touch gesture
		final PreferenceCategory touchCategory = (PreferenceCategory) findPreference(KEY_ASUS_TOUCH_GESTURE_CATEGORY);
		mSwitchDoubleTapOff = (SwitchPreference) findPreference(KEY_DOUBLE_TP_OFF);
    
		if (!isSupportSwipeUp |(Utils.isVerizonSKU())| (!isPhone())) {
			touchCategory.removePreference(mSwitchSwipeUp);
		}
		else{
			mSwitchSwipeUp = (SwitchPreference) findPreference(KEY_SWIPE_UP);
		}
		if (!isSupportDoubleTap) {
			touchCategory.removePreference(mSwitchDoubleTapOn);
		}
		else{
			mSwitchDoubleTapOn = (SwitchPreference) findPreference(KEY_DOUBLE_TP_ON);
		}
		if (!isSupportGestureLunchApp) {
			touchCategory.removePreference(mQuickStartApp);
		}
		else{
			mQuickStartApp = findPreference(KEY_QUICK_START_APP);
			AppList appListActivity=new AppList();
			appListActivity.setPackageNames(getContext());
		}
		//end
		//motion gesture
		if(isPhone()){
			// Flip
			final PreferenceCategory motionCategory = (PreferenceCategory) findPreference(KEY_MOTION_GESTURE);
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
		}
		else{
			removePreference(KEY_MOTION_GESTURE);
		}
		//end
		
		//create onehand mode 
		createOneHandCtrlSettings();
		//end
		
		mZenMotion2SharesPreferences = getActivity().getSharedPreferences(ZEN_MOTION2_DATA,Context.MODE_PRIVATE);	
        //setHasOptionsMenu(true);
    }
   
    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO Auto-generated method stub
        return false;
    }
    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsEvent.MAIN_SETTINGS;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference){
	    boolean switchstate=false;
        if((preference instanceof SwitchPreference)){
            if(preference.getKey().equals("double_tap_on")){
                //Log.i(TAG,"double_tap_on");
				SwitchPreference Switch=(SwitchPreference)preference;
				switchstate=Switch.isChecked();
				if(switchstate)
					writeDoubleTapOn(ENABLE_DOUBLE_TAP_MODE);
				else
					writeDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
            }
            if(preference.getKey().equals("double_tap_off")){
                //Log.i(TAG,"double_tap_off");
				SwitchPreference Switch=(SwitchPreference)preference;
				switchstate=Switch.isChecked();
				if(switchstate)
					writeDoubleTapOff(ENABLE_DOUBLE_TAP_MODE);
				else
					writeDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);

            }
            if(preference.getKey().equals("swipe_up_to_wake_up")){
                //Log.i(TAG,"swipe_up_to_wake_up");
				SwitchPreference Switch=(SwitchPreference)preference;
				switchstate=Switch.isChecked();
				if(switchstate)
					writeSwipeUpWakeUp(ENABLE_SWIPE_UP_MODE);
				else
					writeSwipeUpWakeUp(DISABLE_SWIPE_UP_MODE);

            }
            if(preference.getKey().equals("asus_gesture_quick_start_app")){
                Log.i(TAG,"asus_gesture_quick_start_app");

            }
			
			//motion gesture
            if(preference.getKey().equals(KEY_HAND_UP)){
                Log.i(TAG,KEY_HAND_UP);
				writeHandUpDB(((SwitchPreference) preference).isChecked()? 1: 0);
            }
            if(preference.getKey().equals(KEY_FLIP)){
                Log.i(TAG,KEY_FLIP);
				writeFlipDB(((SwitchPreference) preference).isChecked()? 1: 0);
            }
			//end
			
			//onehand mode
            if(preference.getKey().equals(KEY_ONEHAND_CTRL_QUICK_TRIGGER)){
                //Log.i(TAG,KEY_ONEHAND_CTRL_QUICK_TRIGGER);
				onOneHandCtrlQuickTriggerClick();
            }
			//end

        }
        return super.onPreferenceTreeClick(preference);
    }
    @Override
    public void onStart(){
    	//Log.i(TAG,"onStart+++");
        super.onStart();
        //the preference:"asus_gesture_quick_start_app" summary need to be sync with the SwitchPreference:"asus_touch_gesture_sleep_category" status
        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //mGestureQuickStartOn=sharedPreferences.getBoolean(KEY_ASUS_TOUCH_GESTURE_SLEEP_CATEGORY,false);
        mGestureQuickStartOn=mZenMotion2SharesPreferences.getBoolean(QUICK_START_ENABLE,false);
        Preference preference = findPreference(KEY_ASUS_GESTURE_QUICK_START_APP);
        if(mGestureQuickStartOn){
			if(preference!=null)
				//Log.i(TAG,"Check "+QUICK_START_ENABLE+"="+String.valueOf(mGestureQuickStartOn));
	            preference.setSummary(R.string.Gestures_Quick_Start_Applications_summary_on);
        	}
        else{
            	if(preference!=null)
					//Log.i(TAG,"Check "+QUICK_START_ENABLE+"="+String.valueOf(mGestureQuickStartOn));
					preference.setSummary(R.string.Gestures_Quick_Start_Applications_summary_off);
        }
    	//Log.i(TAG,"onStart---");
    }
	@Override
	public void onResume(){
		boolean switchstate;
		int value;
		super.onResume();
		//Log.i(TAG,"onResume");
		//touch gesture
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		if(mSwitchDoubleTapOff!=null)
		{
			switchstate = mZenMotion2SharesPreferences.getBoolean(KEY_DOUBLE_TP_OFF,false);
			value =  readDoubleTapOff();
			//Log.i(TAG,"Check "+KEY_DOUBLE_TP_OFF+"="+String.valueOf(switchstate)+", systemproperty="+String.valueOf(value));
			mSwitchDoubleTapOff.setChecked(value==ENABLE_DOUBLE_TAP_MODE);
		}
		if(mSwitchDoubleTapOn!=null)
		{
			switchstate = mZenMotion2SharesPreferences.getBoolean(KEY_DOUBLE_TP_ON,false);
			value =  readDoubleTapOn();
			//Log.i(TAG,"Check "+KEY_DOUBLE_TP_ON+"="+String.valueOf(switchstate)+", systemproperty="+String.valueOf(value));
			mSwitchDoubleTapOn.setChecked(switchstate);
		}
		if(mSwitchSwipeUp!=null)
		{
			switchstate = mZenMotion2SharesPreferences.getBoolean(KEY_SWIPE_UP,false);
			value =  readSwipeUpWakeUp();
			//Log.i(TAG,"Check "+KEY_SWIPE_UP+"="+String.valueOf(switchstate)+", systemproperty="+String.valueOf(value));
			mSwitchSwipeUp.setChecked(switchstate);
		}
		//update summary status
        mGestureQuickStartOn=mZenMotion2SharesPreferences.getBoolean(QUICK_START_ENABLE,false);
        Preference preference = findPreference(KEY_ASUS_GESTURE_QUICK_START_APP);
        if(mGestureQuickStartOn){
			if(preference!=null)
				//Log.i(TAG,"Check "+QUICK_START_ENABLE+"="+String.valueOf(mGestureQuickStartOn));
	            preference.setSummary(R.string.Gestures_Quick_Start_Applications_summary_on);
        	}
        else{
            	if(preference!=null)
					//Log.i(TAG,"Check "+QUICK_START_ENABLE+"="+String.valueOf(mGestureQuickStartOn));
					preference.setSummary(R.string.Gestures_Quick_Start_Applications_summary_off);
        }
		//end
		
		//motion gestrure
        if (null != mFlipSwitchPreference) {
            handleFlipStateChanged();
        }

        if (null != mHandUpSwitchPreference) {
            handleHandUpStateChanged();
        }
		//end
		
		//onehand
		updateOneHandCtrlSettingsState();
		//end
	}
	@Override
	public void onPause(){
		boolean switchstate;
		//Log.i(TAG,"onPause+++");
		super.onPause();
		SharedPreferences.Editor editor = mZenMotion2SharesPreferences.edit();
		if(mSwitchDoubleTapOff!=null)
		{
			switchstate = mSwitchDoubleTapOff.isChecked();
			//Log.i(TAG,"Check "+KEY_DOUBLE_TP_OFF+"="+String.valueOf(switchstate));
			editor.putBoolean(KEY_DOUBLE_TP_OFF,switchstate);
			editor.commit();
			if(switchstate)
				writeDoubleTapOff(ENABLE_DOUBLE_TAP_MODE);
			else
				writeDoubleTapOff(DISABLE_DOUBLE_TAP_MODE);
		}
		if(mSwitchDoubleTapOn!=null)
		{
			switchstate = mSwitchDoubleTapOn.isChecked();
			//Log.i(TAG,"Check "+KEY_DOUBLE_TP_ON+"="+String.valueOf(switchstate));
			editor.putBoolean(KEY_DOUBLE_TP_ON,switchstate);
			editor.commit();
			if(switchstate)
				writeDoubleTapOn(ENABLE_DOUBLE_TAP_MODE);
			else
				writeDoubleTapOn(DISABLE_DOUBLE_TAP_MODE);
		}
		if(mSwitchSwipeUp!=null)
		{
			switchstate = mSwitchSwipeUp.isChecked();
			//Log.i(TAG,"Check "+KEY_SWIPE_UP+"="+String.valueOf(switchstate));
			editor.putBoolean(KEY_SWIPE_UP,switchstate);
			editor.commit();
			if(switchstate)
				writeSwipeUpWakeUp(ENABLE_SWIPE_UP_MODE);
			else
				writeSwipeUpWakeUp(DISABLE_SWIPE_UP_MODE);
		}
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		//Log.i(TAG,"onPause---");
	}
	@Override
		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		}
	
    private int readDoubleTapOn() {
        return SystemProperties.getInt(PERSIST_ASUS_DLICK, DISABLE_DOUBLE_TAP_MODE);
    }
	
    private int readDoubleTapOff() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_DOUBLE_TAP, DISABLE_DOUBLE_TAP_MODE);
    }

    private int readSwipeUpWakeUp() {
        return SystemProperties.getInt(PERSIST_ASUS_SWIPE_UP, DISABLE_SWIPE_UP_MODE);
    }
    private void writeDoubleTapOn(int value) {
        try {
            //Log.i(TAG,"Write double tap on value--" + value);
            SystemProperties.set(PERSIST_ASUS_DLICK, Integer.toString(value));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }
	
    private void writeDoubleTapOff(int value) {
        //Log.i(TAG,"Write double tap off value--" + Settings.System.ASUS_DOUBLE_TAP + ": " + value);
        Settings.System.putInt(getContentResolver(), Settings.System.ASUS_DOUBLE_TAP, value);
    }

    private void writeSwipeUpWakeUp(int value) {
        try {
            //Log.i(TAG,"Write Swipe up to wake up value--" + value);
            SystemProperties.set(PERSIST_ASUS_SWIPE_UP, Integer.toString(value));
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e.toString());
        }
    }
	//begin motion gesture
	
    private void writeFlipDB(int value) {
        //Log.d(TAG,"Write motion flip value--"+value);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ASUS_MOTION_FLIP, value);
    }
    private void writeHandUpDB(int value) {
        //Log.d(TAG,"Write motion hand up value--"+value);
        Settings.System.putInt(getContentResolver(),
                Settings.System.ASUS_MOTION_HAND_UP, value);
    }
    private void handleFlipStateChanged() {

        mFlipSwitchPreference.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_MOTION_FLIP, 0) == 1);
    }

    private void handleHandUpStateChanged() {
        mHandUpSwitchPreference.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_MOTION_HAND_UP, 0) == 1);
    }
	//end
	//begin onehand mode
	
	private void createOneHandCtrlSettings() {
		mOneHandCtrlFeatureEnabled = getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_ASUS_WHOLE_SYSTEM_ONEHAND);
		if (!mOneHandCtrlFeatureEnabled) {
			//Log.i(TAG,"No support onehand mode");
			PreferenceCategory oneHandCtrlCategory = (PreferenceCategory) findPreference(
					KEY_ONEHAND_CTRL_CATEGORY);
			getPreferenceScreen().removePreference(oneHandCtrlCategory);
		} else {
			mOneHandCtrlQuickTrigger = (SwitchPreference) findPreference(
					KEY_ONEHAND_CTRL_QUICK_TRIGGER);
		}
	}
	private void updateOneHandCtrlSettingsState() {
        if (mOneHandCtrlFeatureEnabled && mOneHandCtrlQuickTrigger != null) {
            final int enabled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ONEHAND_CTRL_QUICK_TRIGGER_ENABLED,
                    mOneHandCtrlQuickTriggerByDefault, UserHandle.USER_CURRENT);
            mOneHandCtrlQuickTrigger.setChecked(enabled > 0 ? true : false);
        }
    }
	private void onOneHandCtrlQuickTriggerClick() {
		if(mOneHandCtrlQuickTrigger==null){
			//Log.i(TAG,"mOneHandCtrlQuickTrigger=null");
			return;
		}
        Settings.Secure.putIntForUser(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ONEHAND_CTRL_QUICK_TRIGGER_ENABLED,
                mOneHandCtrlQuickTrigger.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
    }
	//end onehand mode
	private boolean isPhone(){
		TelephonyManager telephony = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);	
		if(telephony != null && telephony.isVoiceCapable()){
			return true;
		}
		else
			return false;
	}
	private boolean isTabletDevice(){
		String device=android.os.SystemProperties.get("ro.build.characteristics","phone");
		return "tablet".contentEquals(device);
	}
    /**
     * For Search.
     */
   	/*
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
            @TargetApi(11)
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
    */
}
