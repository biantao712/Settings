/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification;

import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.DialogInterface;
import android.os.Bundle;

import android.provider.Settings.Global;//chunghung_lin@asus.com
import android.service.notification.ZenModeConfig;//chunghung_lin@asus.com
//import android.service.notification.ZenModeConfig.ZenRule;//chunghung_lin@asus.com
import android.support.v14.preference.SwitchPreference;//chunghung_lin@asus.com
import android.support.v7.preference.ListPreference;//chunghung_lin@asus.com
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;//chunghung_lin@asus.com
import android.support.v7.preference.Preference.OnPreferenceChangeListener;//chunghung_lin@asus.com
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import com.android.internal.logging.MetricsLogger;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import android.content.SharedPreferences;
import android.content.Context;
//added by leaon_wang
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Build;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.IntentFilter;

import android.content.ComponentName;
import android.service.notification.ConditionProviderService;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.net.Uri;
import android.view.LayoutInflater;
import android.app.AlertDialog;
import android.widget.TextView;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Button;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.view.WindowManager;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.widget.CompoundButton;
import android.widget.CheckBox;
import java.util.HashSet;
import com.android.settings.notification.view.WhiteListPreference;
import java.util.Calendar;
import android.util.SparseBooleanArray;
import java.util.Date;
import com.asus.cncommonres.AsusTimePicker;
import android.text.format.DateFormat;
import android.view.Window;
import android.text.TextUtils;

public class ZenModeSettings extends ZenModeSettingsBase implements CompoundButton.OnCheckedChangeListener {
    private static final String KEY_PRIORITY_SETTINGS = "priority_settings";
    private static final String KEY_VISUAL_SETTINGS = "visual_interruptions_settings";
    private static final String KEY_AUTOMATION_SETTINGS = "automation_settings";//chunghung_lin@asus.com

    private static final String KEY_OTHERS = "others";
    private static final String KEY_SEND_NOTIFICATIONS = "send_notifications";
    
    private static final String WHITE_LIST_PREFERENCE_NAME = "white_list_preference";

    private Preference mPrioritySettings;
    private Preference mVisualSettings;
    private Policy mPolicy;

    //BEGIN: Steve_Ke@asus.com
    private static final String KEY_ZEN_MODE2 = "cn_zen_mode_rule";
    private ListPreference mZenMode2;
    //END: Steve_Ke@asus.com
    //BEGIN: shane_huang@asus.com
    private Preference mAutomationSettings;
    //END: shane_huang@asus.com

    private PreferenceCategory mOthers;
    private SwitchPreference mSendNotifications;



    //BEGIN: Ashen_Gu@asus.com, for CN Zen Mode 
    private static final String KEY_CN_ZEN_MODE = "cn_switch_zen_mode";
    private static final String KEY_CN_AUTO_MODE = "cn_switch_auto_mode";
    private static final String KEY_CN_AUTO_SCHEDULE_SETTING = "cn_auto_schedule_settings";
    private static final String KEY_CN_REPEACT_CALLER = "cn_repeat_callers";
    private static final String KEY_CN_ZEN_WHITE_LIST_PHONE = "cn_zen_white_list_phone";
    private static final String KEY_CN_ZEN_WHITE_LIST_MESSAGE = "cn_zen_white_list_message";
    //BEGIN: leaon
    private static final String KEY_CN_AUTO_SCHEDULE_START_TIME = "cn_auto_schedule_start_time";
    private static final String KEY_CN_AUTO_SCHEDULE_END_TIME = "cn_auto_schedule_end_time";
    private static final String KEY_CN_AUTO_SCHEDULE_REPEAT = "cn_auto_schedule_repeat";
    private static final String KEY_CN_AUTO_MODE_CONTAINER = "cn_auto_mode_container";
    private static final String KEY_CN_NOTIFICATION = "cn_zen_mode_notification";
    //END: leaon
    private SwitchPreference mEnterZenMode = null;
    private SwitchPreference mAutoZenMode = null;
    private SwitchPreference mRepeatCaller = null;
    private PreferenceScreen mAutoSchedule = null;
    private WhiteListPreference mWhiteListPhonePreference = null;
    private WhiteListPreference mWhiteListMessagePreference = null;
    
    //++++ leaon ++++
    private PreferenceScreen mStartTimePreference = null;
    private PreferenceScreen mEndTimePreference = null;
    private PreferenceScreen mRepeatDaysPreference = null;
    private PreferenceCategory mAutoModeContainer = null;
    //++++ leaon ++++
    private int mWhiteListCheckedItem;
    private static final int WHITE_LIST_NONE = -1;
    private static final int WHITE_LIST_TOP_CONTACT = 2;
    private static final int WHITE_LIST_CONTACT = 1;
    private static final int WHITE_LIST_ANYONE = 0;
    private static final int WHITE_LIST_DEFAULT = WHITE_LIST_NONE;
    private int mWhiteListCall;
    private int mWhiteListMessage;

    private boolean bNewSchedule = true;
    private String mCnAudoScheduleRuleName = "cn_auto_schedule_rule";
    private AutomaticZenRule mCnZenRule = null;
    protected String mId;

    private int mToDifferDisturb = 0;
    //END: Ashen_Gu@asus.com

    //BEGIN: Leaon
    SharedPreferences mSharedPreferences;
    public static final String PREFERENCE_NAME = "cn_zen_mode_auto_schedule";
    private CheckBox mMondayCB;
    private CheckBox mTuesdayCB;
    private CheckBox mWednesdayCB;
    private CheckBox mThursdayCB;
    private CheckBox mFridayCB;
    private CheckBox mSaturdayCB;
    private CheckBox mSundayCB;
    private Button mOkBtn;
    private Button mCancelBtn;
    private final HashSet<String> WEEKDAYS = new HashSet<String>(){{add("1");add("2");add("3");add("4");add("5");}};
    private final HashSet<String> WEEKENDS = new HashSet<String>(){{add("6");add("7");}};
    private final HashSet<String> EVERYDAY = new HashSet<String>(){{add("1");add("2");add("3");add("4");add("5");add("6");add("7");}};
    private HashSet<String> mRepeatDays;
    private static final String DEFAULT_START_TIME = "下午 10:00";
    private static final String DEFAULT_END_TIME = "上午 07:00";
    private static final String SEPARATE_TIME_TAG = ":";
    private static final String KEY_START_TIME = "start_time";	
    private static final String KEY_END_TIME = "end_time";
	
    private Date mStartDate;
	private Date mEndDate;

    private ScheduleInfo mSchedule;
    private final SparseBooleanArray mDays = new SparseBooleanArray();
	
	private int mZenRuleType;
	private static final int DEFAULT_ZEN_RULE_TYPE = Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS;
	private static final String KEY_ZEN_RULE_TYPE = "zen_rule_type";
    //END: Leaon
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.cn_zen_mode_settings);
        final PreferenceScreen root = getPreferenceScreen();
		mSharedPreferences = getActivity().getSharedPreferences(PREFERENCE_NAME,getActivity().MODE_PRIVATE);

        mPolicy = NotificationManager.from(mContext).getNotificationPolicy();
        //BEGIN: Ashen_Gu@asus.com, for CN Zen Mode Enter Switch
		
		//+++Leaon_Wang 去掉通知
		Global.putInt(mContext.getContentResolver(), "ZENMODE_SEND_NOTIFICATION",0);

        mEnterZenMode = (SwitchPreference) root.findPreference(KEY_CN_ZEN_MODE);
        //updateZenMode();
        mEnterZenMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                if(val){
					showDescriptionDialog(KEY_CN_ZEN_MODE);
                    //setZenMode(Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, null);
/*
                    Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);

                    final ZenModeConditionSelection zenModeConditionSelection =
                    new ZenModeConditionSelection(mContext, Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS);
                    zenModeConditionSelection.confirmCondition();
*/
                }
                else{
                   // Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
                    setZenMode(Global.ZEN_MODE_OFF, null);
                    mAutoZenMode.setEnabled(true);
                }
                return true;
            }
        });
	mWhiteListPhonePreference = (WhiteListPreference)root.findPreference(KEY_CN_ZEN_WHITE_LIST_PHONE);
	mWhiteListPhonePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showWhiteListDialog(KEY_CN_ZEN_WHITE_LIST_PHONE);
                return true;
            }
        });
	mWhiteListMessagePreference = (WhiteListPreference)root.findPreference(KEY_CN_ZEN_WHITE_LIST_MESSAGE);
	mWhiteListMessagePreference.setAllowBelowDivider(false);
        mWhiteListMessagePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showWhiteListDialog(KEY_CN_ZEN_WHITE_LIST_MESSAGE);
                return true;
            }
        });

        //END: Ashen_Gu@asus.com

        //BEGIN: Ashen_Gu@asus.com, for CN Auto Schedule Switch

        mId = Global.getString(mContext.getContentResolver(), mCnAudoScheduleRuleName);
        if(mId != null){
            for (Map.Entry<String,AutomaticZenRule> ruleEntry : mRules) {
                final AutomaticZenRule rule = ruleEntry.getValue();
                if(rule.getName().equals(mCnAudoScheduleRuleName)){
                    bNewSchedule = false;
                    mCnZenRule = rule;
                    break;
                }
            }
        }
        mAutoZenMode = (SwitchPreference) root.findPreference(KEY_CN_AUTO_MODE);
	//BEGIN: leaon
	mAutoModeContainer = (PreferenceCategory)root.findPreference(KEY_CN_AUTO_MODE_CONTAINER);
	mStartTimePreference = (PreferenceScreen) root.findPreference(KEY_CN_AUTO_SCHEDULE_START_TIME);
	mEndTimePreference = (PreferenceScreen) root.findPreference(KEY_CN_AUTO_SCHEDULE_END_TIME);
	mRepeatDaysPreference = (PreferenceScreen) root.findPreference(KEY_CN_AUTO_SCHEDULE_REPEAT);
	//END: leaon
        updateAutoZenMode();
        mAutoZenMode.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                Global.putInt(mContext.getContentResolver(), "CN_AUTO_ZEN_MODE", val ? 1 : 0);
                if(val){
					showDescriptionDialog(KEY_CN_AUTO_MODE);
                 }
                 else{
					//BEGIN: Leaon
					mAutoModeContainer.removePreference(mStartTimePreference);
					mAutoModeContainer.removePreference(mEndTimePreference);
					mAutoModeContainer.removePreference(mRepeatDaysPreference);
					mAutoZenMode.setLayoutResource(R.layout.asusres_preference_material_nodivider);
					//END: Leaon
                     //mAutoSchedule.setEnabled(false);
					Log.i(TAG,"ZenModeSettings.bNewSchedule = "+bNewSchedule);
                    if(bNewSchedule == false){ 
                        for (Map.Entry<String,AutomaticZenRule> ruleEntry : mRules) {
                            final AutomaticZenRule rule = ruleEntry.getValue();
                            if(rule.getName().equals(mCnAudoScheduleRuleName)){
                                 bNewSchedule = false;
                                 mCnZenRule = rule;
                                 break;
                             }
                         }
                         mCnZenRule.setEnabled(false);
						 setZenRule(mId,mCnZenRule);
                         Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
                         setZenMode(Global.ZEN_MODE_OFF, null);
                     }
                }

                 return true;
            }
        });
        //END: Ashen_Gu@asus.com
	//BEGIN: Leaon
	if(!mAutoZenMode.isChecked()){
	    mAutoModeContainer.removePreference(mStartTimePreference);
	    mAutoModeContainer.removePreference(mEndTimePreference);
	    mAutoModeContainer.removePreference(mRepeatDaysPreference);
	}else{
		mAutoZenMode.setLayoutResource(R.layout.cnasusres_preference_parent_noarrow);
	}
	
	// ++++ CTS ++++
	mZenMode2 = (ListPreference) root.findPreference(KEY_ZEN_MODE2);
	mZenRuleType = getZenRuleType();
	int val = Global.getInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
    if (val != Global.ZEN_MODE_OFF && mZenRuleType != val) {
        mZenRuleType = val;
        saveZenRuleType(mZenRuleType);
    } else if (val == Global.ZEN_MODE_OFF && mAutoZenMode.isChecked()) {
        mZenRuleType = mCnZenRule.getInterruptionFilter() - 1;
        saveZenRuleType(mZenRuleType);
    }
	if (!Utils.isVerizonSKU()) {
        mZenMode2.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final int value = Integer.valueOf((String)newValue);
                final int oldValue = Global.getInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
                 // update to Settings.Global
                updateZenModeSummary(value);
                mZenRuleType = value;
                enableZenModeRulePref(mZenRuleType);
                saveZenRuleType(mZenRuleType);
                if(mAutoZenMode.isChecked() && mCnZenRule != null){
                    int interruptionFilter = mZenRuleType + 1;
                    mCnZenRule.setInterruptionFilter(interruptionFilter);
                    setZenRule(mId,mCnZenRule);
                }
                if(oldValue != Global.ZEN_MODE_OFF){
                    Global.putInt(mContext.getContentResolver(), Global.ZEN_MODE, value);
                    setZenMode(value, null);
                    disableAutoSwitch();
                }
                return true;
            }
            });
        } else {
            root.removePreference(mZenMode2);
            mZenMode2 = null;
        }
	// ---- CTS ----
	mStartTimePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
        @Override
        public boolean onPreferenceClick(Preference preference) {
            showTimeDialog(KEY_START_TIME,mStartDate);
            return true;
        }
    });

	mEndTimePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
        @Override
        public boolean onPreferenceClick(Preference preference) {
            showTimeDialog(KEY_END_TIME,mEndDate);
            return true;
        }
    });

	mRepeatDaysPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
        @Override
        public boolean onPreferenceClick(Preference preference) {
			showRepeatDaysPopupWindow();
            return true;
        }
    });
	//END: Leaon
        //BEGIN: Ashen_Gu@asus.com, for CN Repeat Caller Switch

        mRepeatCaller = (SwitchPreference) root.findPreference(KEY_CN_REPEACT_CALLER);
        updateRepeatCaller();
        
        if (Utils.isVoiceCapable(mContext)) {
            mRepeatCaller.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    
                    final boolean val = (Boolean) newValue;
                    MetricsLogger.action(mContext, MetricsEvent.ACTION_ZEN_ALLOW_REPEAT_CALLS, val);
                    if (DEBUG) Log.d(TAG, "onPrefChange allowRepeatCallers=" + val);
                    int priorityCategories = getNewPriorityCategories(val,
                            NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS);
                    savePolicy(priorityCategories, mPolicy.priorityCallSenders,
                            mPolicy.priorityMessageSenders, mPolicy.suppressedVisualEffects);
                    return true;
                }
            });
        } else {
            root.removePreference(mRepeatCaller);
            mRepeatCaller = null;
        }
        //END: Ashen_Gu@asus.com 
	//BEGIN: Leaon
	initSummaryData();
	//END: Leaon
    }

    private void disableAutoSwitch() {
        if (mAutoZenMode.isChecked()) {
            mAutoZenMode.setChecked(false);
            Global.putInt(mContext.getContentResolver(), "CN_AUTO_ZEN_MODE", 0);
            mAutoModeContainer.removePreference(mStartTimePreference);
            mAutoModeContainer.removePreference(mEndTimePreference);
            mAutoModeContainer.removePreference(mRepeatDaysPreference);
            mAutoZenMode.setLayoutResource(R.layout.asusres_preference_material_nodivider);
            if(bNewSchedule == false){
                for (Map.Entry<String,AutomaticZenRule> ruleEntry : mRules) {
                    final AutomaticZenRule rule = ruleEntry.getValue();
                    if(rule.getName().equals(mCnAudoScheduleRuleName)){
                        bNewSchedule = false;
                        mCnZenRule = rule;
                        break;
                    }
                }
                mCnZenRule.setEnabled(false);
                setZenRule(mId, mCnZenRule);
            }
        }
        mAutoZenMode.setEnabled(false);
    }

	
	/**BEIGN:Leaon_Wang
	*	set background color
	*/
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }
	//END:Leaon_Wang
	
	private void saveZenRuleType(int val){
		if(mSharedPreferences != null){
			SharedPreferences.Editor editor = mSharedPreferences.edit();
			editor.putInt(KEY_ZEN_RULE_TYPE,val);
			editor.commit();
		}
    }
	
	private int getZenRuleType(){
		if(mSharedPreferences == null) return DEFAULT_ZEN_RULE_TYPE;
		int val = mSharedPreferences.getInt(KEY_ZEN_RULE_TYPE,DEFAULT_ZEN_RULE_TYPE);
		return val;	
	}
	
	/**
	 * Leaon_Wang
	 * 如果选择了完全阻止，则将允许来电等条件disable；反之，则enable
	**/
	private void enableZenModeRulePref(int enable){
		if(enable == Global.ZEN_MODE_OFF) return;
		if(enable == Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS){
			mWhiteListPhonePreference.setEnabled(true);
			mWhiteListPhonePreference.setShouldDisableView(false);
			mRepeatCaller.setEnabled(true);
			mRepeatCaller.setShouldDisableView(false);
			mWhiteListMessagePreference.setEnabled(true);
			mWhiteListMessagePreference.setShouldDisableView(false);
		}else{
			mWhiteListPhonePreference.setEnabled(false);
			mWhiteListPhonePreference.setShouldDisableView(true);
			mRepeatCaller.setEnabled(false);
			mRepeatCaller.setShouldDisableView(true);
			mWhiteListMessagePreference.setEnabled(false);
			mWhiteListMessagePreference.setShouldDisableView(true);
		}
	}
	
	//+++Leaon_Wang
	private void enableAutoZenMode(){
		ZenRuleInfo ri;
		//mAutoSchedule.setEnabled(true);
        //if no schedule then new a default one as cn version's final only.
		//BEGIN: Leaon
		mAutoModeContainer.addPreference(mStartTimePreference);
		mAutoModeContainer.addPreference(mEndTimePreference);
		mAutoModeContainer.addPreference(mRepeatDaysPreference);
		mAutoZenMode.setLayoutResource(R.layout.cnasusres_preference_parent_noarrow);
		//END: Leaon
		Log.i(TAG,"mZenRuleType = " + mZenRuleType);
        int interruptionFilter = mZenRuleType + 1;
		Log.i(TAG,"interruptionFilter = " + interruptionFilter);
        if(bNewSchedule){
			Log.i(TAG,"bNewSchedule = " + bNewSchedule);
            ri = defaultNewCNSchedule();
            mCnZenRule = new AutomaticZenRule(mCnAudoScheduleRuleName,ri.serviceComponent,ri.defaultConditionId,interruptionFilter,true);
            mId = addZenRule(mCnZenRule);
			Log.i(TAG,"mId = " + mId);
            Global.putString(mContext.getContentResolver(), mCnAudoScheduleRuleName, mId);
            mCnZenRule.setEnabled(true);
            bNewSchedule = false;
        }else{     
			Log.i(TAG,"bNewSchedule = " + bNewSchedule);
			mCnZenRule.setInterruptionFilter(interruptionFilter);
            mCnZenRule.setEnabled(true);
            mToDifferDisturb = Global.getInt(mContext.getContentResolver(), "CN_AUTO_ZEN_MODE_DIFF", 1);
            ScheduleInfo schedule;
            schedule = ZenModeConfig.tryParseScheduleConditionId(mCnZenRule.getConditionId());
            if(schedule != null){
                schedule.endMinute = schedule.endMinute % 60 + mToDifferDisturb;
                mCnZenRule.setConditionId(ZenModeConfig.toScheduleConditionId(schedule));
            }else{
                Log.e("ZenModeSetting","ZenModeSetting schedule == null");
            }

            if(mToDifferDisturb == 1){
                mToDifferDisturb = -1;
            }else{
                mToDifferDisturb = 1;
            }
            Global.putInt(mContext.getContentResolver(), "CN_AUTO_ZEN_MODE_DIFF", mToDifferDisturb);
        }
        setZenRule(mId,mCnZenRule);   

        //no null mCnZenRule or ScheduleInfo allowed!
        if(mCnZenRule == null){
			Log.i(TAG,"mCnZenRule is null ");
            ri = defaultNewCNSchedule();
            mCnZenRule = new AutomaticZenRule(mCnAudoScheduleRuleName,ri.serviceComponent,ri.defaultConditionId,interruptionFilter,true);
            mId = addZenRule(mCnZenRule);
            Global.putString(mContext.getContentResolver(), mCnAudoScheduleRuleName, mId);
            mCnZenRule.setEnabled(true);
            bNewSchedule = false;
        }else{
			Log.i(TAG,"mCnZenRule is not null ");
            ScheduleInfo schedule2;
            schedule2 = ZenModeConfig.tryParseScheduleConditionId(mCnZenRule.getConditionId());
            if(schedule2 == null){
				Log.i(TAG,"schedule2 is null ");
                ri = defaultNewCNSchedule();
                mCnZenRule = new AutomaticZenRule(mCnAudoScheduleRuleName,ri.serviceComponent,ri.defaultConditionId,interruptionFilter,true);
                mId = addZenRule(mCnZenRule);
                Global.putString(mContext.getContentResolver(), mCnAudoScheduleRuleName, mId);
                mCnZenRule.setEnabled(true);
                bNewSchedule = false;
            }  
        }
	}
	
	private void showDescriptionDialog(final String key){
		LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.cnasusres_alertdialog_content_with_title,null);		
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
							.setView(rootView)
							.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									if(key.equals(KEY_CN_ZEN_MODE)){
										mEnterZenMode.setChecked(false);
									}else if(key.equals(KEY_CN_AUTO_MODE)){
                                        Global.putInt(mContext.getContentResolver(), "CN_AUTO_ZEN_MODE", 0);
										mAutoZenMode.setChecked(false);
									}
								}
							})
							.setPositiveButton(R.string.cn_zen_mode_open_button,new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									if(key.equals(KEY_CN_ZEN_MODE)){
										if(mZenMode2 != null){
											setZenMode(getZenRuleType(), null);
										}else{
											setZenMode(Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS, null);
										}
                                        disableAutoSwitch();
									}else if(key.equals(KEY_CN_AUTO_MODE)){
                                        Global.putInt(mContext.getContentResolver(), "CN_AUTO_ZEN_MODE", 1);
										enableAutoZenMode();
									}
								}
							}).create();
		TextView titleTV = (TextView)rootView.findViewById(R.id.alertdialog_title);
		titleTV.setText(R.string.cn_zen_mode_settings_title2);
		TextView messageTV = (TextView)rootView.findViewById(R.id.alertdialog_message);
		messageTV.setText(R.string.cn_zen_mode_desc);
		dialog.setCancelable(false);
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
		dialog.show();
	}
	//---Leaon_Wang
    
    //BEGIN: Leaon
    private void initSummaryData(){
		if(mSharedPreferences != null){
			mRepeatDays = new HashSet<String>(mSharedPreferences.getStringSet("days",new HashSet<String>()));
			initRepeatDaysSummary();
			initTimeSummary();
			initWhiteListSummary();
		}
    }

    private void saveTimeInPreference(String key,Date date){
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putLong(key,date.getTime());        	
		editor.commit();
    }
	
	private void updateTimePreference(String key,Date date){
		if(key.equals(KEY_START_TIME)){
			mStartTimePreference.setSummary(DateFormat.getTimeFormat(getActivity()).format(date));
		}else if(key.equals(KEY_END_TIME)){
			mEndTimePreference.setSummary(DateFormat.getTimeFormat(getActivity()).format(date));
		}
	}

    private void saveTimeInSchedule(String key,Date date){
		if(mCnZenRule == null){
           	Log.e("ZenModeAutoScheduleSetting","mCnZenRule null error.");
           	return ;
       	}

       	mSchedule = ZenModeConfig.tryParseScheduleConditionId(mCnZenRule.getConditionId());

       	if(mSchedule == null){
           	Log.e("ZenModeAutoScheduleSetting","mSchedule null error.");
           	return ;
       	}
		if(key.equals(KEY_START_TIME)){
			if (!ZenModeConfig.isValidHour(date.getHours())) return ;
            if (!ZenModeConfig.isValidMinute(date.getMinutes())) return ;
            if (date.getHours() == mSchedule.startHour && date.getMinutes() == mSchedule.startMinute) {
           		return ;
            }
            mSchedule.startHour = date.getHours();
            mSchedule.startMinute = date.getMinutes();
		}else if(key.equals(KEY_END_TIME)){
			if (!ZenModeConfig.isValidHour(date.getHours())) return ;
            if (!ZenModeConfig.isValidMinute(date.getMinutes())) return ;
             if (date.getHours() == mSchedule.endHour && date.getMinutes() == mSchedule.endMinute) {
                return ;
            }
            mSchedule.endHour = date.getHours();
            mSchedule.endMinute = date.getMinutes();
		}
		updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
    }

    private void saveDayInSchedule(){
		for(int i=1;i<=7;i++){
            if(mRepeatDays.contains(""+i)){
				updateDayRule(i, true);
			}else{
				updateDayRule(i, false);
	       }
        }
    }

    private void updateDayRule(int day,boolean isChecked){
       	if(mCnZenRule == null){
           		Log.e("ZenModeAutoScheduleSetting","mCnZenRule null error.");
           		return ;
      	 }

       	mSchedule = ZenModeConfig.tryParseScheduleConditionId(mCnZenRule.getConditionId());

       	if(mSchedule == null){
           		Log.e("ZenModeAutoScheduleSetting","mSchedule null error.");
           		return ;
       	}
	switch(day){
	        case 1:
		mDays.put(Calendar.MONDAY, isChecked);
	        break;
	        case 2:
		mDays.put(Calendar.TUESDAY, isChecked);
	        break;
	        case 3:
		mDays.put(Calendar.WEDNESDAY, isChecked);
	        break;
	        case 4:
		mDays.put(Calendar.THURSDAY, isChecked);
	        break;
	        case 5:
		mDays.put(Calendar.FRIDAY, isChecked);
	        break;
	        case 6:
		mDays.put(Calendar.SATURDAY, isChecked);
	        break;
	        case 7:
		mDays.put(Calendar.SUNDAY, isChecked);
	        break;
	}
        	mSchedule.days = getDays();
        	updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
       }

    private int[] getDays() {
        final SparseBooleanArray rt = new SparseBooleanArray(mDays.size());
        for (int i = 0; i < mDays.size(); i++) {
            final int day = mDays.keyAt(i);
            if (!mDays.valueAt(i)) continue;
            rt.put(day, true);
        }
        final int[] rta = new int[rt.size()];
        for (int i = 0; i < rta.length; i++) {
            rta[i] = rt.keyAt(i);
        }
        Arrays.sort(rta);
        return rta;
    }

    private void initWhiteListSummary(){
	mWhiteListCall = mSharedPreferences.getInt(KEY_CN_ZEN_WHITE_LIST_PHONE,R.id.radio_none);
	mWhiteListMessage =  mSharedPreferences.getInt(KEY_CN_ZEN_WHITE_LIST_MESSAGE,R.id.radio_none);
	Log.i(TAG,"ZenModeSettings.mWhiteListCall = "+mWhiteListCall);
	switch(mWhiteListCall){
	    case R.id.radio_none:
		mWhiteListPhonePreference.setDataText(getActivity().getString(R.string.cn_zen_white_list_dialog_radio_none));
	    break;
	    case R.id.radio_topcontact:
		mWhiteListPhonePreference.setDataText(getActivity().getString(R.string.cn_zen_white_list_dialog_radio_top_contact));
	    break;
	    case R.id.radio_contact:
		mWhiteListPhonePreference.setDataText(getActivity().getString(R.string.cn_zen_white_list_dialog_radio_contact));
	    break;
	    case R.id.radio_anyone:
		mWhiteListPhonePreference.setDataText(getActivity().getString(R.string.cn_zen_white_list_dialog_radio_anyone));
	    break;
	}
	switch(mWhiteListMessage){
	    case R.id.radio_none:
		mWhiteListMessagePreference.setDataText(getActivity().getString(R.string.cn_zen_white_list_dialog_radio_none));
	    break;
	    case R.id.radio_topcontact:
		mWhiteListMessagePreference.setDataText(getActivity().getString(R.string.cn_zen_white_list_dialog_radio_top_contact));
	    break;
	    case R.id.radio_contact:
		mWhiteListMessagePreference.setDataText(getActivity().getString(R.string.cn_zen_white_list_dialog_radio_contact));
	    break;
	    case R.id.radio_anyone:
		mWhiteListMessagePreference.setDataText(getActivity().getString(R.string.cn_zen_white_list_dialog_radio_anyone));
	    break;
	}
    }        

       //
    private void initTimeSummary(){
		if(!mSharedPreferences.contains(KEY_START_TIME)){
			Log.i(TAG,"new StartDate");
			mStartDate = new Date(System.currentTimeMillis());
			mStartDate.setHours(22);
			mStartDate.setMinutes(0);
		}else{
			mStartDate = new Date(mSharedPreferences.getLong(KEY_START_TIME,System.currentTimeMillis()));
		}
		Log.i(TAG,"startDate = "+mStartDate.toLocaleString());
		if(!mSharedPreferences.contains(KEY_END_TIME)){
			Log.i(TAG,"new EndDate");
			mEndDate = new Date(System.currentTimeMillis());
			mEndDate.setHours(7);
			mEndDate.setMinutes(0);
		}else{
			mEndDate = new Date(mSharedPreferences.getLong(KEY_END_TIME,System.currentTimeMillis()));
		}
		Log.i(TAG,"EndDate = "+mEndDate.toLocaleString());
		mStartTimePreference.setSummary(DateFormat.getTimeFormat(getActivity()).format(mStartDate));
		mEndTimePreference.setSummary(DateFormat.getTimeFormat(getActivity()).format(mEndDate));
    }
  
    private void initRepeatDaysSummary(){
	if(mRepeatDays.isEmpty()){
	    mRepeatDaysPreference.setSummary(getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_no_repeat));
	}else{
	    if(mRepeatDays.equals(WEEKDAYS)){
		mRepeatDaysPreference.setSummary(getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_weekdays));
	    }else if(mRepeatDays.equals(WEEKENDS)){
	    	mRepeatDaysPreference.setSummary(getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_weekends));
	    }else if(mRepeatDays.equals(EVERYDAY)){
	    	mRepeatDaysPreference.setSummary(getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_everyday));
	    }else{
	  	String summary = "";
		if(mRepeatDays.contains("1")){
		    summary += getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_monday)+" ";
		}
		if(mRepeatDays.contains("2")){
		    summary += getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_tuesday)+" ";
		}
		if(mRepeatDays.contains("3")){
		    summary += getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_wednesday)+" ";
		}
		if(mRepeatDays.contains("4")){
		    summary += getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_thursday)+" ";
		}
		if(mRepeatDays.contains("5")){
		    summary += getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_friday)+" ";
		}
		if(mRepeatDays.contains("6")){
		    summary += getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_saturday)+" ";
		}
		if(mRepeatDays.contains("7")){
		    summary += getActivity().getString(R.string.cn_zen_mode_auto_schedule_day_sunday)+" ";
		}
		mRepeatDaysPreference.setSummary(summary);
	    }
	}
    }
    private void showTimeDialog(final String key , Date valDate){
		AsusTimePicker asusTimePicker = new AsusTimePicker(getContext(), new AsusTimePicker.ResultHandler() {
            @Override
            public void handle(Date date) {
				Log.i(TAG,""+date.getHours()+":"+date.getMinutes());
				if(key.equals(KEY_START_TIME)){
					mStartDate = date;
				}else if(key.equals(KEY_END_TIME)){
					mEndDate = date;
				}
				saveTimeInPreference(key,date);
				updateTimePreference(key,date);
				saveTimeInSchedule(key,date);
            }
        },valDate);
        asusTimePicker.show();
    }

    private void showRepeatDaysPopupWindow() {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.cn_zen_mode_repeat_days_layout,null);
		AlertDialog dialog = new AlertDialog.Builder(getActivity()).setView(rootView)
							.setNegativeButton(android.R.string.cancel, null)
							.setPositiveButton(android.R.string.ok,new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialogInterface, int i) {
									saveDaysInPreference();
									initRepeatDaysSummary();
									saveDayInSchedule();
								}
							}).create();
		dialog.setCancelable(false);
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
		initRepeatDayView(rootView);
		dialog.show();
    }

    private void initRepeatDayView(View rootView){
        mMondayCB = (CheckBox) rootView.findViewById(R.id.monday);
        mTuesdayCB = (CheckBox) rootView.findViewById(R.id.tuesday);
        mWednesdayCB = (CheckBox) rootView.findViewById(R.id.wednesday);
        mThursdayCB = (CheckBox) rootView.findViewById(R.id.thursday);
        mFridayCB = (CheckBox) rootView.findViewById(R.id.friday);
        mSaturdayCB = (CheckBox) rootView.findViewById(R.id.saturday);
        mSundayCB = (CheckBox) rootView.findViewById(R.id.sunday);
        mMondayCB.setOnCheckedChangeListener(this);
        mTuesdayCB.setOnCheckedChangeListener(this);
        mWednesdayCB.setOnCheckedChangeListener(this);
        mThursdayCB.setOnCheckedChangeListener(this);
        mFridayCB.setOnCheckedChangeListener(this);
        mSaturdayCB.setOnCheckedChangeListener(this);
        mSundayCB.setOnCheckedChangeListener(this);
		
        mRepeatDays = new HashSet<String>(mSharedPreferences.getStringSet("days",new HashSet<String>()));
		Log.i(TAG,"repeatdays = " + mRepeatDays.toString());
		if(mRepeatDays.contains("1")){
			mMondayCB.setChecked(true);
		}
		if(mRepeatDays.contains("2")){
			mTuesdayCB.setChecked(true);
		}
		if(mRepeatDays.contains("3")){
			mWednesdayCB.setChecked(true);
		}
		if(mRepeatDays.contains("4")){
			mThursdayCB.setChecked(true);    
		}
		if(mRepeatDays.contains("5")){
			mFridayCB.setChecked(true);   
		}
		if(mRepeatDays.contains("6")){
			mSaturdayCB.setChecked(true);
		}
		if(mRepeatDays.contains("7")){
			mSundayCB.setChecked(true);
		}
	}

    private void saveDaysInPreference(){
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove("days");
        editor.commit();
        editor.putStringSet("days",mRepeatDays);
        editor.commit();
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        switch (compoundButton.getId()){
            case R.id.monday:
                if(b){
                    mRepeatDays.add("1");
                }else{
                    mRepeatDays.remove("1");
                }
                break;
            case R.id.tuesday:
                if(b){
                    mRepeatDays.add("2");
                }else{
                    mRepeatDays.remove("2");
                }
                break;
            case R.id.wednesday:
                if(b){
                    mRepeatDays.add("3");
                }else{
                    mRepeatDays.remove("3");
                }
                break;
            case R.id.thursday:
                if(b){
                    mRepeatDays.add("4");
                }else{
                    mRepeatDays.remove("4");
                }
                break;
            case R.id.friday:
                if(b){
                    mRepeatDays.add("5");
                }else{
                    mRepeatDays.remove("5");
                }
                break;
            case R.id.saturday:
                if(b){
                    mRepeatDays.add("6");
                }else{
                    mRepeatDays.remove("6");
                }
                break;
            case R.id.sunday:
                if(b){
                    mRepeatDays.add("7");
                }else{
                    mRepeatDays.remove("7");
                }
                break;
        }
    }
    //END: Leaon

    /** ++++Leaon +++++  */
    private void showWhiteListDialog(String key){
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View rootView = inflater.inflate(R.layout.zen_mode_white_list_dialog_layout,null);
		TextView titleTV = (TextView)rootView.findViewById(R.id.title);
		String titleStr =""; 
		if(key.equals(KEY_CN_ZEN_WHITE_LIST_PHONE)){
			titleStr = getResources().getString(R.string.cn_zen_white_list_phone_title);
		}else if(key.equals(KEY_CN_ZEN_WHITE_LIST_MESSAGE)){
			titleStr = getResources().getString(R.string.cn_zen_white_list_message_title);
		}
		titleTV.setText(titleStr);
		builder.setView(rootView).setNegativeButton(android.R.string.cancel, null);
        AlertDialog dialog = builder.show();
		dialog.setCancelable(false);
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);

		RadioGroup radioGroup = (RadioGroup)rootView.findViewById(R.id.radiogroup);
		mWhiteListCheckedItem = getWhiteListCheckedData(key);
		radioGroup.check(mWhiteListCheckedItem);
		radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int item) {
				Log.i("ZenModeSettings","WhiteListDialog.item = "+item);
				if(mWhiteListCheckedItem != item){
					mWhiteListCheckedItem = item;
					setWhiteListCheckedData(key,mWhiteListCheckedItem);
					int allowFrom = mPolicy.priorityCallSenders;
					boolean flag = true;
					switch(item){
					case R.id.radio_none:
						allowFrom = mPolicy.priorityCallSenders;
						flag = false;
						break;
					case R.id.radio_topcontact:
						allowFrom = WHITE_LIST_TOP_CONTACT;
						break;
					case R.id.radio_contact:
						allowFrom = WHITE_LIST_CONTACT;
						break;
					case R.id.radio_anyone:
						allowFrom = WHITE_LIST_ANYONE;
						break;
					}
				if(key == KEY_CN_ZEN_WHITE_LIST_PHONE){
					savePolicy(getNewPriorityCategories(flag,Policy.PRIORITY_CATEGORY_CALLS), allowFrom,
                            mPolicy.priorityMessageSenders, mPolicy.suppressedVisualEffects);
				} else if(key == KEY_CN_ZEN_WHITE_LIST_MESSAGE){
					savePolicy(getNewPriorityCategories(flag,Policy.PRIORITY_CATEGORY_MESSAGES),mPolicy.priorityCallSenders,
						allowFrom,mPolicy.suppressedVisualEffects);
				}
				dialog.dismiss();
				initWhiteListSummary();
			}
        }
        });
    }

    private int  getWhiteListCheckedData(String key){
		int checkedItem = mSharedPreferences.getInt(key,R.id.radio_none);
		return checkedItem;	
    }

    private void setWhiteListCheckedData(String key,int data){
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putInt(key,data);
		editor.commit();
    }
    
    protected void updateRule(Uri newConditionId) {
        mCnZenRule.setConditionId(newConditionId);
        setZenRule(mId, mCnZenRule);
    }

    private void updateZenMode() {
        if (mEnterZenMode == null) return;
        final int oldValue = Global.getInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
        mEnterZenMode.setChecked(oldValue != Global.ZEN_MODE_OFF);
    }

    private void updateAutoZenMode() {
        if (mAutoZenMode == null) return;
        final int autoZenMode = Global.getInt(mContext.getContentResolver(), "CN_AUTO_ZEN_MODE", 0);
        mAutoZenMode.setChecked(autoZenMode == 1);
        if (mEnterZenMode.isChecked() && !mAutoZenMode.isChecked()) {
            mAutoZenMode.setEnabled(false);
        }
//        mAutoSchedule.setEnabled(autoZenMode == 1);
    }

    private void updateRepeatCaller() {
        
        if (mRepeatCaller != null) {
            mRepeatCaller.setChecked(isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_REPEAT_CALLERS));
            mRepeatCaller.setVisible(!isPriorityCategoryEnabled(Policy.PRIORITY_CATEGORY_CALLS)
                    || mPolicy.priorityCallSenders != Policy.PRIORITY_SENDERS_ANY);
        }
     
    }

    private boolean isPriorityCategoryEnabled(int categoryType) {
        return (mPolicy.priorityCategories & categoryType) != 0;
    }

    private int getNewPriorityCategories(boolean allow, int categoryType) {
        int priorityCategories = mPolicy.priorityCategories;
        if (allow) {
            priorityCategories |= categoryType;
        } else {
            priorityCategories &= ~categoryType;
        }
        return priorityCategories;
    }

    private void savePolicy(int priorityCategories, int priorityCallSenders,
            int priorityMessageSenders, int suppressedVisualEffects) {
        mPolicy = new Policy(priorityCategories, priorityCallSenders, priorityMessageSenders,
                suppressedVisualEffects);
        NotificationManager.from(mContext).setNotificationPolicy(mPolicy);
    }

    @Override
    public void onResume() {
        super.onResume();
		//+++
		int val = Global.getInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
		Log.i(TAG,"zen_mode value = " + val);
		if(mZenMode2 != null){
			updateZenModeSummary(mZenRuleType);
		}
		enableZenModeRulePref(mZenRuleType);
		//+++
		updateZenMode();
		updateAutoZenMode();

    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE;
    }

    @Override
    protected void onZenModeChanged() {
		Log.i(TAG,"onZenModeChanged");
		int val = Global.getInt(mContext.getContentResolver(), Global.ZEN_MODE, Global.ZEN_MODE_OFF);
		Log.i(TAG,"zen_mode value = " + val);
        if (val != Global.ZEN_MODE_OFF) {
            if ( mZenRuleType != val) {
                mZenRuleType = val;
                saveZenRuleType(mZenRuleType);
            }
            if (!mAutoZenMode.isChecked()) {
                mAutoZenMode.setEnabled(false);
            }
        } else {
            if (mAutoZenMode.isChecked()) {
                mZenRuleType = mCnZenRule.getInterruptionFilter() - 1;
                saveZenRuleType(mZenRuleType);
            } else {
                mAutoZenMode.setEnabled(true);
            }
        }
		if(mZenMode2 != null){
			updateZenModeSummary(getZenRuleType());
		}
		enableZenModeRulePref(mZenRuleType);
        updateZenMode();
    }

    @Override
    protected void onZenModeConfigChanged() {
    }

    private void updateControls() {
        updatePrioritySettingsSummary();
        updateVisualSettingsSummary();
        // BEGIN: shane_huang@asus.com, for Zen Mode Notification Switch
        final int sendNotification = Global.getInt(mContext.getContentResolver(), "ZENMODE_SEND_NOTIFICATION", 1);
        mSendNotifications.setChecked(sendNotification == 1);
        // End: shane_huang@asus.com
    }

    private void updatePrioritySettingsSummary() {
        String s = getResources().getString(R.string.zen_mode_alarms);
        s = appendLowercase(s, isCategoryEnabled(mPolicy, Policy.PRIORITY_CATEGORY_REMINDERS),
                R.string.zen_mode_reminders);
        s = appendLowercase(s, isCategoryEnabled(mPolicy, Policy.PRIORITY_CATEGORY_EVENTS),
                R.string.zen_mode_events);
        if (isCategoryEnabled(mPolicy, Policy.PRIORITY_CATEGORY_MESSAGES)) {
            if (mPolicy.priorityMessageSenders == Policy.PRIORITY_SENDERS_ANY) {
                s = appendLowercase(s, true, R.string.zen_mode_all_messages);
            } else {
                s = appendLowercase(s, true, R.string.zen_mode_selected_messages);
            }
        }

        if (Utils.isVoiceCapable(mContext)) {
            if (isCategoryEnabled(mPolicy, Policy.PRIORITY_CATEGORY_CALLS)) {
                if (mPolicy.priorityCallSenders == Policy.PRIORITY_SENDERS_ANY) {
                    s = appendLowercase(s, true, R.string.zen_mode_all_callers);
                } else {
                    s = appendLowercase(s, true, R.string.zen_mode_selected_callers);
                }
            } else if (isCategoryEnabled(mPolicy, Policy.PRIORITY_CATEGORY_REPEAT_CALLERS)) {
                s = appendLowercase(s, true, R.string.zen_mode_repeat_callers);
            }
        }
        mPrioritySettings.setSummary(s);
    }

    private void updateVisualSettingsSummary() {
        String s = getString(R.string.zen_mode_all_visual_interruptions);
        if (isEffectSuppressed(Policy.SUPPRESSED_EFFECT_SCREEN_ON)
                && isEffectSuppressed(Policy.SUPPRESSED_EFFECT_SCREEN_OFF)) {
            s = getString(R.string.zen_mode_no_visual_interruptions);
        } else if (isEffectSuppressed(Policy.SUPPRESSED_EFFECT_SCREEN_ON)) {
            s = getString(R.string.zen_mode_screen_on_visual_interruptions);
        } else if (isEffectSuppressed(Policy.SUPPRESSED_EFFECT_SCREEN_OFF)) {
            s = getString(R.string.zen_mode_screen_off_visual_interruptions);
        }
        mVisualSettings.setSummary(s);
    }

    private boolean isEffectSuppressed(int effect) {
        return (mPolicy.suppressedVisualEffects & effect) != 0;
    }

    private boolean isCategoryEnabled(Policy policy, int categoryType) {
        return (policy.priorityCategories & categoryType) != 0;
    }

    private String appendLowercase(String s, boolean condition, int resId) {
        if (condition) {
            return getResources().getString(R.string.join_many_items_middle, s,
                    getResources().getString(resId).toLowerCase());
        }
        return s;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_interruptions;
    }

    //BEGIN: Steve_Ke@asus.com
    private void updateZenModeSummary(int value) {
        if (mZenMode2 != null) {
            mZenMode2.setValue(String.valueOf(value));//chunghung_lin@asus.com
            String summary = "";
            switch (value) {
                case Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                    summary = getResources().getString(R.string.zen_mode_option_important_interruptions2);
                    break;
                case Global.ZEN_MODE_NO_INTERRUPTIONS:
                    summary = getResources().getString(R.string.zen_mode_option_no_interruptions);
                    break;
				case Global.ZEN_MODE_ALARMS:
					summary = getResources().getString(R.string.zen_mode_option_alarms);
					break;
            }
            mZenMode2.setSummary(summary);
        }
    }
    //END: Steve_Ke@asus.com

    //BEGIN: shane_huang@asus.com
    private static class ZenRuleInfo2 {
        String id;
        AutomaticZenRule rule;//test
    }

    private static final Comparator<ZenRuleInfo2> RULE_COMPARATOR = new Comparator<ZenRuleInfo2>() {
        @Override
        public int compare(ZenRuleInfo2 lhs, ZenRuleInfo2 rhs) {
            return key(lhs).compareTo(key(rhs));
        }
        private String key(ZenRuleInfo2 zri) {
            final AutomaticZenRule rule = zri.rule;//test
            final int type = ZenModeConfig.isValidScheduleConditionId(rule.getConditionId()) ? 1
                    : ZenModeConfig.isValidEventConditionId(rule.getConditionId()) ? 2
                    : 3;
            return type + rule.getName();
        }
    };

    private ZenRuleInfo2[] sortedRules() {
        final ArrayList<ZenRuleInfo2> rte = new ArrayList<>();
        for (Map.Entry<String, AutomaticZenRule> mRule : mRules) {
            final ZenRuleInfo2 zri = new ZenRuleInfo2();
            zri.id = mRule.getKey();
            zri.rule = mRule.getValue();
            if(zri.rule.isEnabled()) rte.add(zri);
        }
        final ZenRuleInfo2[] rt = rte.toArray(new ZenRuleInfo2[0]);
        Arrays.sort(rt, RULE_COMPARATOR);
        return rt;
    }

    private String append(String s, boolean condition, String string) {
        if (condition) {
            return getResources().getString(R.string.join_many_items_middle, s, string);
        }
        return s;
    }

    private void updateAutomationRuleSummary() {
        final ZenRuleInfo2[] sortedRules = sortedRules();
        String summary = getResources().getString(R.string.zen_mode_in_activity)
                + (sortedRules.length!=0 ? sortedRules[0].rule.getName() : getResources().getString(R.string.zen_mode_from_none));
        for (int i = 1; i < sortedRules.length; i++) {
            summary = append(summary, true, sortedRules[i].rule.getName());
        }
        mAutomationSettings.setSummary(summary);
    }
//END: shane_huang@asus.com


    private ZenRuleInfo defaultNewCNSchedule() {
        final ZenModeConfig.ScheduleInfo schedule = new ZenModeConfig.ScheduleInfo();
        schedule.days = ZenModeConfig.ALL_DAYS;
        schedule.startHour = 22;
        schedule.endHour = 7;

        final ZenRuleInfo rt = new ZenRuleInfo();

        rt.settingsAction = ZenModeScheduleRuleSettings.ACTION;
        rt.title = mContext.getString(R.string.zen_schedule_rule_type_name);
        rt.packageName = ZenModeConfig.getEventConditionProvider().getPackageName();
        rt.defaultConditionId = ZenModeConfig.toScheduleConditionId(schedule);
        rt.serviceComponent = ZenModeConfig.getScheduleConditionProvider();
        rt.isSystem = true;

        return rt;

    }

    private Intent getRuleIntent(String settingsAction, ComponentName configurationActivity,
            String ruleId) {
        Intent intent = new Intent()
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(ConditionProviderService.EXTRA_RULE_ID, ruleId);
        if (configurationActivity != null) {
            intent.setComponent(configurationActivity);
        } else {
            intent.setAction(settingsAction);
        }
        return intent;
    }
}
