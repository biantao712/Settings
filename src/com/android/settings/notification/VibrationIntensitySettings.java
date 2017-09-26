package com.android.settings.notification;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.provider.Telephony;


import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.util.Log;

import android.media.AudioAttributes;
import android.os.Vibrator;

import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.view.View;
import android.view.ViewGroup;
import android.app.Activity;
import android.os.Bundle;

import android.content.ContentResolver;
import android.provider.Settings;

import android.os.AsyncTask;


import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.SeekBarPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.notification.VibrationSeekBarPreference;

public class VibrationIntensitySettings extends SettingsPreferenceFragment {
    private static final String TAG = "VibrationIntensitySettings";
    private static final int SEEK_BAR_RANGE = 5;
    private Vibrator mVibrator;
    private VibrationSeekBarPreference Vibration_Incoming_call;
    private VibrationSeekBarPreference Vibration_Notifications;
    private VibrationSeekBarPreference Vibration_Touch_feedback;
    static int Vibration_Incoming_call_newValue;
    static int Vibration_Notifications_newValue;
    static int Vibration_Touch_feedback_newValue;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.SOUND;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.vibration_intensity_settings);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        Vibration_Incoming_call_newValue = Settings.System.getInt(getContentResolver(), Settings.System.VIBRATION_LEVEL_RINGTONE, 0);
        Vibration_Notifications_newValue = Settings.System.getInt(getContentResolver(), Settings.System.VIBRATION_LEVEL_NOTIFICATION, 0);
        Vibration_Touch_feedback_newValue = Settings.System.getInt(getContentResolver(), Settings.System.VIBRATION_LEVEL_TOUCH, 0);
        Log.d(TAG, "onCreate Vibration_Incoming_call_newValue  = " + Vibration_Incoming_call_newValue + ", Vibration_Notifications_newValue  = " + Vibration_Notifications_newValue + ", Vibration_Touch_feedback_newValue  = " + Vibration_Touch_feedback_newValue);

        //Incoming_call
        Vibration_Incoming_call = (VibrationSeekBarPreference) findPreference("Incoming_call");
        Vibration_Incoming_call.setMax(SEEK_BAR_RANGE);
        Vibration_Incoming_call.setProgress(Vibration_Incoming_call_newValue);

        //Notifications
        Vibration_Notifications = (VibrationSeekBarPreference) findPreference("Notifications");
        Vibration_Notifications.setMax(SEEK_BAR_RANGE);
        Vibration_Notifications.setProgress(Vibration_Notifications_newValue);


        //Touch feedback
        Vibration_Touch_feedback = (VibrationSeekBarPreference) findPreference("Touch_feedback");
        Vibration_Touch_feedback.setMax(SEEK_BAR_RANGE);
        Vibration_Touch_feedback.setProgress(Vibration_Touch_feedback_newValue);


        //Incoming_call
        Vibration_Incoming_call.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AudioAttributes mRingtoneAttr = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build();
                mVibrator.vibrate(100, mRingtoneAttr, (Integer) newValue, AudioAttributes.CATEGORY_RINGTONE);
                Vibration_Incoming_call_newValue = (Integer) newValue;
                Log.d(TAG, "Vibration_Incoming_call_newValue = " + Vibration_Incoming_call_newValue);
                Log.d(TAG, "onPreferenceChange+1=" + Long.toString(Thread.currentThread().getId()));

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... values) {
            Settings.System.putInt(getContext().getContentResolver(), Settings.System.VIBRATION_LEVEL_RINGTONE, Vibration_Incoming_call_newValue);
                Log.d(TAG, "Vibration_Incoming_call_newValue end = " + Vibration_Incoming_call_newValue);
                Log.d(TAG, "doInBackground+1=" + Long.toString(Thread.currentThread().getId()));
                return null;
            }
        }.execute();
            return true;
            }
        });

        //Notifications:USAGE_NOTIFICATION
        Vibration_Notifications.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AudioAttributes mRingtoneAttr = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                mVibrator.vibrate(100, mRingtoneAttr, (Integer) newValue, AudioAttributes.CATEGORY_NOTIFICATION);
                Vibration_Notifications_newValue = (Integer) newValue;
                Log.d(TAG, "Vibration_Notifications_newValue = " + Vibration_Notifications_newValue);
                Log.d(TAG, "onPreferenceChange+2=" + Long.toString(Thread.currentThread().getId()));

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... values) {
            Settings.System.putInt(getContext().getContentResolver(), Settings.System.VIBRATION_LEVEL_NOTIFICATION, Vibration_Notifications_newValue);
                Log.d(TAG, "Vibration_Notifications_newValue end = " + Vibration_Notifications_newValue);
                Log.d(TAG, "doInBackground+2=" + Long.toString(Thread.currentThread().getId()));
                return null;
            }
        }.execute();
                return true;
            }
        });

        //Touch feedback
        Vibration_Touch_feedback.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                AudioAttributes mRingtoneAttr = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build();
                mVibrator.vibrate(50, mRingtoneAttr, (Integer) newValue, AudioAttributes.CATEGORY_TOUCH);
                Vibration_Touch_feedback_newValue = (Integer) newValue;
                Log.d(TAG, "Vibration_Touch_feedback_newValue = " + Vibration_Touch_feedback_newValue);
                Log.d(TAG, "onPreferenceChange+3=" + Long.toString(Thread.currentThread().getId()));

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... values) {
            Settings.System.putInt(getContext().getContentResolver(), Settings.System.VIBRATION_LEVEL_TOUCH, Vibration_Touch_feedback_newValue);
                Log.d(TAG, "Vibration_Touch_feedback_newValue end = " + Vibration_Touch_feedback_newValue);
                Log.d(TAG, "doInBackground+3=" + Long.toString(Thread.currentThread().getId()));
                return null;
            }
        }.execute();
                return true;
            }
        });
        Log.d(TAG,"onCreate");

    }

}
