/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AutomaticZenRule;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
//import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;
//import android.widget.TimePicker;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

import com.asus.commonui.datetimepicker.time.RadialPickerLayout;
import com.asus.commonui.datetimepicker.time.TimePickerDialog;

public class ZenModeScheduleRuleSettings extends ZenModeRuleSettingsBase
        implements OnPreferenceChangeListener {
    private static final String KEY_DAYS = "days";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_EXIT_AT_ALARM = "exit_at_alarm";

    public static final String ACTION = Settings.ACTION_ZEN_MODE_SCHEDULE_RULE_SETTINGS;

    // time picker dialog tag
    private static final String TAG_START_TIME_PICKER = "ZenModeScheduleRuleSettings_StartTimePicker";
    private static final String TAG_END_TIME_PICKER = "ZenModeScheduleRuleSettings_EndTimePicker";

    // per-instance to ensure we're always using the current locale
    private final SimpleDateFormat mDayFormat = new SimpleDateFormat("EEE");

    private Preference mDays;
    private Preference mStart;
    private Preference mEnd;
    private SwitchPreference mExitAtAlarm;
    private ScheduleInfo mSchedule;

    @Override
    protected boolean setRule(AutomaticZenRule rule) {
        mSchedule = rule != null ? ZenModeConfig.tryParseScheduleConditionId(rule.getConditionId())
                : null;
        return mSchedule != null;
    }

    @Override
    protected String getZenModeDependency() {
        return mDays.getKey();
    }

    @Override
    protected int getEnabledToastText() {
        return R.string.zen_schedule_rule_enabled_toast;
    }

    @Override
    protected void onCreateInternal() {
        addPreferencesFromResource(R.xml.zen_mode_schedule_rule_settings);
        final PreferenceScreen root = getPreferenceScreen();
        mDays = root.findPreference(KEY_DAYS);
        mStart = root.findPreference(KEY_START_TIME);
        mEnd = root.findPreference(KEY_END_TIME);
        mExitAtAlarm = (SwitchPreference) root.findPreference(KEY_EXIT_AT_ALARM);
        mExitAtAlarm.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        TimePickerDialog tstp = (TimePickerDialog) getFragmentManager().findFragmentByTag(TAG_START_TIME_PICKER);
        if (tstp != null) {
            // The dialog is already open so we need to set the listener again.
            tstp.setOnTimeSetListener(new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(RadialPickerLayout view, int hour, int minute) {
                    if ((mDisableListeners)
                            || (!ZenModeConfig.isValidHour(hour))
                            || (!ZenModeConfig.isValidMinute(minute))
                            || (hour == mSchedule.startHour && minute == mSchedule.startMinute)) {
                        return;
                    }

                    mSchedule.startHour = hour;
                    mSchedule.startMinute = minute;
                    final Activity activity = getActivity();
                    if (activity != null) {
                        updateStartTimeSummary(mSchedule);
                        updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                    }
                    if (DEBUG) Log.d(TAG, "StartTimePickerDialog onPrefChange start h=" + hour + " m=" + minute);
                }
            });
        }

        TimePickerDialog tetp = (TimePickerDialog) getFragmentManager().findFragmentByTag(TAG_END_TIME_PICKER);
        if (tetp != null) {
            // The dialog is already open so we need to set the listener again.
            tetp.setOnTimeSetListener(new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(RadialPickerLayout view, int hour, int minute) {
                    if ((mDisableListeners)
                            || (!ZenModeConfig.isValidHour(hour))
                            || (!ZenModeConfig.isValidMinute(minute))
                            || (hour == mSchedule.endHour && minute == mSchedule.endMinute)) {
                        return;
                    }

                    mSchedule.endHour = hour;
                    mSchedule.endMinute = minute;
                    final Activity activity = getActivity();
                    if (activity != null) {
                        updateEndTimeSummary(mSchedule);
                        updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                    }
                    if (DEBUG) Log.d(TAG, "EndTimePickerDialog onPrefChange start h=" + hour + " m=" + minute);
                }
            });
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mDays) {
            showDaysDialog();
        } else if (preference == mStart) {
            showStartTimePicker();
        } else if (preference == mEnd) {
            showEndTimePicker();
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals(KEY_EXIT_AT_ALARM)) {
            mSchedule.exitAtAlarm = (Boolean) newValue;
            updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
        }
        return true;
    }

    private void updateDays() {
        // Compute an ordered, delimited list of day names based on the persisted user config.
        final int[] days = mSchedule.days;
        if (days != null && days.length > 0) {
            final StringBuilder sb = new StringBuilder();
            final Calendar c = Calendar.getInstance();
            int[] daysOfWeek = ZenModeScheduleDaysSelection.getDaysOfWeekForLocale(c);
            for (int i = 0; i < daysOfWeek.length; i++) {
                final int day = daysOfWeek[i];
                for (int j = 0; j < days.length; j++) {
                    if (day == days[j]) {
                        c.set(Calendar.DAY_OF_WEEK, day);
                        if (sb.length() > 0) {
                            sb.append(mContext.getString(R.string.summary_divider_text));
                        }
                        sb.append(mDayFormat.format(c.getTime()));
                        break;
                    }
                }
            }
            if (sb.length() > 0) {
                mDays.setSummary(sb);
                mDays.notifyDependencyChange(false);
                return;
            }
        }
        mDays.setSummary(R.string.zen_mode_schedule_rule_days_none);
        mDays.notifyDependencyChange(true);
    }

    private void updateStartTimeSummary(ScheduleInfo schedule) {
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, schedule.startHour);
        c.set(Calendar.MINUTE, schedule.startMinute);
        String time = DateFormat.getTimeFormat(mContext).format(c.getTime());
        mStart.setSummary(time);
    }

    private void updateEndTimeSummary(ScheduleInfo schedule) {
        final int startMin = 60 * schedule.startHour + schedule.startMinute;
        final int endMin = 60 * schedule.endHour + schedule.endMinute;
        final boolean nextDay = startMin >= endMin;
        final int summaryFormat = nextDay ? R.string.zen_mode_end_time_next_day_summary_format : 0;
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, schedule.endHour);
        c.set(Calendar.MINUTE, schedule.endMinute);
        String time = DateFormat.getTimeFormat(mContext).format(c.getTime());
        if (summaryFormat != 0) {
            time = mContext.getResources().getString(summaryFormat, time);
        }
        mEnd.setSummary(time);
    }

    private void updateAlarmSummary(boolean checked) {
        mExitAtAlarm.setChecked(checked);
    }

    @Override
    protected void updateControlsInternal() {
        updateDays();
        updateStartTimeSummary(mSchedule);
        updateEndTimeSummary(mSchedule);
        updateAlarmSummary(mSchedule.exitAtAlarm);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE_SCHEDULE_RULE;
    }

    private void showDaysDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(R.string.zen_mode_schedule_rule_days)
                .setView(new ZenModeScheduleDaysSelection(mContext, mSchedule.days) {
                      @Override
                      protected void onChanged(final int[] days) {
                          if (mDisableListeners) return;
                          if (Arrays.equals(days, mSchedule.days)) return;
                          if (DEBUG) Log.d(TAG, "days.onChanged days=" + Arrays.asList(days));
                          mSchedule.days = days;
                          updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                      }
                })
                .setOnDismissListener(new OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        updateDays();
                    }
                })
                .setPositiveButton(R.string.done_button, null)
                .show();
    }

    private void showStartTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        TimePickerDialog sTimePickerDialog = TimePickerDialog.newInstance(
        new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialPickerLayout view,
                                  int hour, int minute) {
                if ((mDisableListeners)
                        || (!ZenModeConfig.isValidHour(hour))
                        || (!ZenModeConfig.isValidMinute(minute))
                        || (hour == mSchedule.startHour && minute == mSchedule.startMinute)) {
                    return;
                }

                mSchedule.startHour = hour;
                mSchedule.startMinute = minute;
                final Activity activity = getActivity();
                if (activity != null) {
                    updateStartTimeSummary(mSchedule);
                    updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                }
                if (DEBUG) Log.d(TAG, "StartTimePickerDialog onPrefChange start h=" + hour + " m=" + minute);
            }
        }, calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        DateFormat.is24HourFormat(getActivity()));
        sTimePickerDialog.setStartTime(mSchedule.startHour, mSchedule.startMinute);
        sTimePickerDialog.show(getFragmentManager(), TAG_START_TIME_PICKER);
    }

    private void showEndTimePicker() {
        final Calendar calendar = Calendar.getInstance();
        TimePickerDialog eTimePickerDialog = TimePickerDialog.newInstance(
        new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(RadialPickerLayout view,
                                  int hour, int minute) {
                if ((mDisableListeners)
                        || (!ZenModeConfig.isValidHour(hour))
                        || (!ZenModeConfig.isValidMinute(minute))
                        || (hour == mSchedule.endHour && minute == mSchedule.endMinute)) {
                    return;
                }

                mSchedule.endHour = hour;
                mSchedule.endMinute = minute;
                final Activity activity = getActivity();
                if (activity != null) {
                    updateEndTimeSummary(mSchedule);
                    updateRule(ZenModeConfig.toScheduleConditionId(mSchedule));
                }
                if (DEBUG) Log.d(TAG, "EndTimePickerDialog onPrefChange start h=" + hour + " m=" + minute);
            }
        }, calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        DateFormat.is24HourFormat(getActivity()));
        eTimePickerDialog.setStartTime(mSchedule.endHour, mSchedule.endMinute);
        eTimePickerDialog.show(getFragmentManager(), TAG_END_TIME_PICKER);
    }
}
