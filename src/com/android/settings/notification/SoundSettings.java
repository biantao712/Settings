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

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.preference.SeekBarVolumizer;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.AsusTelephonyUtils;
import com.android.settings.R;
import com.android.settings.RingtonePreference;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;
import android.widget.RadioGroup;
import android.widget.Button;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
//Sharon+++
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.content.Context;
//Sharon---
import android.os.SystemProperties; //Sharon+++show vibrate on touch UI or not
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import com.android.settings.DefaultRingtonePreference; //MikeHsu@20121011 add for multiple notification effects
import android.content.pm.ApplicationInfo; //MikeHsu@20121011 add for multiple notification effects
import android.support.v7.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.Gravity;
import android.app.AlertDialog;
import android.media.AudioAttributes;

public class SoundSettings extends SettingsPreferenceFragment implements Indexable {
    private static final String TAG = "SoundSettings";

    private static final String KEY_VIBRATOR = "vibration"; //Sharon+++add vibration category
    private static final String KEY_SYNC_RING_VOLUME_WITH_NOTIFICATION_VOLUME = "sync_ring_volume_with_notification_volume";
    private static final String KEY_MEDIA_VOLUME = "media_volume";
    private static final String KEY_ALARM_VOLUME = "alarm_volume";
    private static final String KEY_RING_VOLUME = "ring_volume";
    private static final String KEY_NOTIFICATION_VOLUME = "notification_volume";
    private static final String KEY_SOUND_VOLUME = "sounds_volume";
    private static final String KEY_VIBRATE_INTENSITY = "vibration_intensity";

    private static final String KEY_PHONE_RINGTONE = "ringtone";
    private static final String KEY_DUAL_SIM_PHONE_RINGTONE = "dual_sim_ringtone";
    private static final String KEY_NOTIFICATION_RINGTONE = "notification_ringtone";
    private static final String KEY_DUAL_SIM_NOTIFICATION_RINGTONE = "dual_sim_notification_ringtone"; // +++ andrew_tu@20150327
    private static final String KEY_ALARM_RINGTONE = "alarm_ringtone";
    private static final String KEY_VIBRATE_WHEN_RINGING = "vibrate_when_ringing";
    private static final String KEY_WIFI_DISPLAY = "wifi_display";
    private static final String KEY_ZEN_MODE = "zen_mode";
    private static final String KEY_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";
    private static final String KEY_VIBRATE_ON_TOUCH = "vibrate_on_touch"; //Sharon+++new feature about vibration intensity
    private static final String SELECTED_PREFERENCE_KEY = "selected_preference";
    private static final int REQUEST_CODE = 200;

    private static final String KEY_SYSTEM = "system"; 
    private static final String KEY_SILENT_SETTING = "silent_mode";
    private static final String KEY_VIBRATE_WHEN_SILENT = "vibrate_on_silent";
    private static final String KEY_PAD_TOUCH_TONES = "dial_pad_touch_tones";
    private static final String KEY_TOUCH_SOUNDS = "touch_sounds";
    private static final String KEY_SCREEN_LOCKING_SOUNDS = "screen_locking_sounds";
    private static final String KEY_SCREENSHOT_SOUND = "screenshot_sound";
    private static final String KEY_AUDIO_WIZARD1 = "audio_wizard";

    private static final String KEY_NOTIFICATION_USE_RING_VOLUME = "asus_notification_use_ring_volume";

    private static final String[] RESTRICTED_KEYS = {
        KEY_MEDIA_VOLUME,
        KEY_ALARM_VOLUME,
        KEY_RING_VOLUME,
        KEY_NOTIFICATION_VOLUME,
        KEY_ZEN_MODE,
    };

    //+++ MikeHsu@20121011 add for multiple notification effects
    private static final String KEY_NEWMAIL_RINGTONE = "newmail_ringtone";
    private static final String KEY_SENTMAILIN_RINGTONE = "sentmail_ringtone";
    private static final String KEY_CALENDARALERT_RINGTONE = "calendaralert_ringtone";
    //--- MikeHsu

    //Verizon Customize Entry for AudioWizard
    private static final String KEY_AUDIO_WIZARD = "audiowizard_entry";

    private static final int SAMPLE_CUTOFF = 2000;  // manually cap sample playback at 2 seconds

    private final VolumePreferenceCallback mVolumeCallback = new VolumePreferenceCallback();
    private final H mHandler = new H();
    private final SettingsObserver mSettingsObserver = new SettingsObserver();
    private final Receiver mReceiver = new Receiver();
    private final ArrayList<VolumeSeekBarPreference> mVolumePrefs = new ArrayList<>();

    private Context mContext;
    private boolean mVoiceCapable;
    private boolean mIsMultiSimEnabled;
    private Vibrator mVibrator;
    private AudioManager mAudioManager;
    private VolumeSeekBarPreference mRingOrNotificationPreference;
    private VolumeSeekBarPreference mNotificationPreference;
	private VolumeSeekBarPreference mAlarmVolumeSeekBarPreference;
    private int mNotificationUseRingVolume;

    private Preference mPhoneRingtonePreference;
    private Preference mDualSimPhoneRingtonePreference;
    private Preference mNotificationRingtonePreference;
    private Preference mDualSimNotificationRingtonePreference; // +++ andrew_tu@20150327
    private Preference mAlarmRingtonePreference;
    private TwoStatePreference mNotificationUseRingVolumePreference;
    private TwoStatePreference mVibrateWhenRinging;
    private TwoStatePreference mVibrateOnTouch; //Sharon+++new feature about vibration intensity
    private ComponentName mSuppressor;
    private int mRingerMode = -1;

    private PackageManager mPm;
    private UserManager mUserManager;
    private RingtonePreference mRequestPreference;
    private boolean mVibrationIntensityCapable; //Sharon+++show vibrate on touch UI or not

    //+++ MikeHsu@20121011 add for multiple notification effects
    private Preference mSentMailPreference;
    private Preference mCalendarAlertPreference;
    private Preference mAlarmPreference;
    //--- MikeHsu
    private TwoStatePreference mSilentSetting;
    private TwoStatePreference mVibrateWhenSilent;
    private TwoStatePreference mDialPadTone;
    private TwoStatePreference mTouchSound;
    private TwoStatePreference mScreenSound;
    private TwoStatePreference mScreenshotSound;
    private Preference mAudioWizard;

    //+++ Leaon_Wang
    private PreferenceScreen mVibrateIntensityPreference;
    private static final int VIBRATE_INTENSITY_CLOSE = 0;
    private static final int VIBRATE_INTENSITY_WEEK = 1;
    private static final int VIBRATE_INTENSITY_NORMAL = 3;
    private static final int VIBRATE_INTENSITY_STRONG = 5;
    //--- Leaon_Wang
	private static final int TYPE_OTHER = RingtoneManager.TYPE_NEWMAIL | RingtoneManager.TYPE_SENTMAIL;
	
	private static final String PREFERENCE_NAME = "soundsettings";

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.SOUND;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mPm = getPackageManager();
        mUserManager = UserManager.get(getContext());
        mVoiceCapable = Utils.isVoiceCapable(mContext);
        mIsMultiSimEnabled = Utils.isMultiSimEnabled(mContext);

        Settings.System.putInt(mContext.getContentResolver(),KEY_NOTIFICATION_USE_RING_VOLUME,0);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null && !mVibrator.hasVibrator()) {
            mVibrator = null;
        }

        
        addPreferencesFromResource(R.xml.sound_settings);

        initSilentMode();

        initVolumePreference(KEY_MEDIA_VOLUME, AudioManager.STREAM_MUSIC,
                R.drawable.ic_audio_vol_mute_zenui);
        mAlarmVolumeSeekBarPreference = initVolumePreference(KEY_ALARM_VOLUME, AudioManager.STREAM_ALARM,
                R.drawable.ic_audio_alarm_mute_zenui);
		if(mAlarmVolumeSeekBarPreference != null)
			mAlarmVolumeSeekBarPreference.setDivider(false);
        if (mVoiceCapable) {
            mRingOrNotificationPreference =
                    initVolumePreference(KEY_RING_VOLUME, AudioManager.STREAM_RING,
                            R.drawable.ic_audio_ring_mute_zenui);
/*
            // +++ ASUS Feature +++
            // Mofidy by Morphy_Huang@2016/10/26
            // New feature:Separate ringer and notification volume for a voice capable device
            if (mNotificationUseRingVolume == -1) {
                removePreference(KEY_NOTIFICATION_VOLUME);
            } else {
                mNotificationPreference =
                initVolumePreference(KEY_NOTIFICATION_VOLUME, AudioManager.STREAM_NOTIFICATION,
                   R.drawable.ic_audio_notification_mute_zenui);
                if (mNotificationUseRingVolume != 0) {
                    updateNotificationPreference();
                }
            }
            // --- ASUS Feature ---
*/
                mNotificationPreference =
                initVolumePreference(KEY_NOTIFICATION_VOLUME, AudioManager.STREAM_NOTIFICATION,
                   R.drawable.ic_audio_notification_mute_zenui);
                updateNotificationPreference();
        } else {
            mRingOrNotificationPreference =
                    initVolumePreference(KEY_NOTIFICATION_VOLUME, AudioManager.STREAM_NOTIFICATION,
                            R.drawable.ic_audio_notification_mute_zenui);
            removePreference(KEY_RING_VOLUME);
        }

        initRingtones();
        //initVibrateWhenRinging();
        updateRingerMode();
        updateEffectsSuppressor();

        //Sharon+++show vibrate on touch UI or not
        final PreferenceCategory vibrator = (PreferenceCategory) findPreference(KEY_VIBRATOR);
        //mVibrationIntensityCapable = Boolean.valueOf(SystemProperties.get("ro.config.zf3"));
        mVibrationIntensityCapable = mPm.hasSystemFeature(PackageManager.FEATURE_ASUS_VIBRATION_INTENSITY);
        Log.d(TAG,"test+mVibrationIntensityCapable="+mVibrationIntensityCapable);
        if(mVibrator != null){
            initVibrateWhenSilent(vibrator);
            if (!mVibrationIntensityCapable) {
                initVibrateWhenRinging();
                initVibrateOnTouch(vibrator);
                vibrator.removePreference(findPreference(KEY_VIBRATE_INTENSITY));
                Log.d(TAG,"not zf3");
            }
            else {//zf3
                mVibrateWhenRinging = (TwoStatePreference) getPreferenceScreen().findPreference(KEY_VIBRATE_ON_TOUCH);
                vibrator.removePreference(mVibrateWhenRinging);
                initVibrateOnTouch(vibrator);
                initVibrateWhenRinging_new(vibrator);
				initVibrateIntensity(vibrator);
                Log.d(TAG,"zf3");
            }
        }
        else{
            getPreferenceScreen().removePreference(findPreference(KEY_VIBRATOR));
        }

        final PreferenceCategory system = (PreferenceCategory) findPreference(KEY_SYSTEM);
        initDialPadTone(system);
        initTouchSound(system);
        initScreenSound(system);
	//+++Leaon
	initScreenshotSound(system);
        initAudioWizard(system);
	//---Leaon

        //Sharon---show vibrate on touch UI or not

        // Enable link to CMAS app settings depending on the value in config.xml.
        boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        try {
            if (isCellBroadcastAppLinkEnabled) {
                String applicationEnabledSetting = AsusTelephonyUtils.isVerizon() ? "com.asus.cellbroadcastreceiver":"com.android.cellbroadcastreceiver";
                if (mPm.getApplicationEnabledSetting(applicationEnabledSetting)
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                }
            }
        } catch (IllegalArgumentException ignored) {
            isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
        }
        if (!mUserManager.isAdminUser() || !isCellBroadcastAppLinkEnabled ||
                RestrictedLockUtils.hasBaseUserRestriction(mContext,
                        UserManager.DISALLOW_CONFIG_CELL_BROADCASTS, UserHandle.myUserId()) || Utils.isWifiOnly(mContext)) {
            removePreference(KEY_CELL_BROADCAST_SETTINGS);
        }else{
            if(AsusTelephonyUtils.isVerizon()){
                RestrictedPreference broadcastSettingsPref = (RestrictedPreference) findPreference(
                        KEY_CELL_BROADCAST_SETTINGS);
                Intent intent = new Intent();
                intent.setAction("android.intent.action.MAIN");
                intent.setComponent(new ComponentName("com.asus.cellbroadcastreceiver","com.asus.cellbroadcastreceiver.CellBroadcastSettings"));
                broadcastSettingsPref.setIntent(intent);
            }
        }

        initNotificationUseRingVolumePreference();

        if(!isAppEnabled(mContext,"com.asus.calendar")){
            Log.d(TAG,"onCreate+mCalendarAlertPreference="+mCalendarAlertPreference);
            if(mCalendarAlertPreference!=null){
                getPreferenceScreen().removePreference(mCalendarAlertPreference);
            }
        }
        //--- nancy_jiang

        if (savedInstanceState != null) {
            String selectedPreference = savedInstanceState.getString(SELECTED_PREFERENCE_KEY, null);
            if (!TextUtils.isEmpty(selectedPreference)) {
                mRequestPreference = (RingtonePreference) findPreference(selectedPreference);
            }
        }
		
    }

    @Override
    public void onResume() {
        super.onResume();
        lookupRingtoneNames();
        mSettingsObserver.register(true);
        mReceiver.register(true);
        updateRingOrNotificationPreference();
        updateEffectsSuppressor();
        updateNotificationSettings();
        for (VolumeSeekBarPreference volumePref : mVolumePrefs) {
            volumePref.onActivityResume();
        }

        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(mContext,
                UserManager.DISALLOW_ADJUST_VOLUME, UserHandle.myUserId());
        final boolean hasBaseRestriction = RestrictedLockUtils.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_ADJUST_VOLUME, UserHandle.myUserId());
        for (String key : RESTRICTED_KEYS) {
            Preference pref = findPreference(key);
            if (pref != null) {
                pref.setEnabled(!hasBaseRestriction);
            }
            if (pref instanceof RestrictedPreference && !hasBaseRestriction) {
                ((RestrictedPreference) pref).setDisabledByAdmin(admin);
            }
        }
        RestrictedPreference broadcastSettingsPref = (RestrictedPreference) findPreference(
                KEY_CELL_BROADCAST_SETTINGS);
        if (broadcastSettingsPref != null) {
            broadcastSettingsPref.checkRestrictionAndSetDisabled(
                    UserManager.DISALLOW_CONFIG_CELL_BROADCASTS);
        }
		//++++ Leaon_Wang+++++
		if(mVibrateIntensityPreference != null){
			int value = Settings.System.getInt(getContext().getContentResolver(), Settings.System.VIBRATION_LEVEL_TOUCH, VIBRATE_INTENSITY_CLOSE);
			Log.i(TAG,"VibrateIntensity value = " + value);
			value = getDefaultVibrateIntensity(value);
			Log.i(TAG,"getDefaultVibrateIntensity value = " + value);
			updateVibrateIntensityPreference(value);
		}
    }

    @Override
    public void onPause() {
        super.onPause();
        for (VolumeSeekBarPreference volumePref : mVolumePrefs) {
            volumePref.onActivityPause();
        }
        mVolumeCallback.stopSample();
        mSettingsObserver.register(false);
        mReceiver.register(false);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
		if (preference instanceof RingtonePreference) {
            mRequestPreference = (RingtonePreference) preference;
            mRequestPreference.onPrepareRingtonePickerIntent(mRequestPreference.getIntent());
            startActivityForResult(preference.getIntent(), REQUEST_CODE);
            return true;
        } else if (preference.getKey().equals(KEY_AUDIO_WIZARD)) {
            Intent intent = new Intent();
            intent.setClassName("com.asus.maxxaudio.audiowizard",
                    "com.asus.maxxaudio.audiowizard.MainActivity");
            startActivity(intent);
        } 
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRequestPreference != null) {
			Log.i(TAG,"onActivityResult");
            mRequestPreference.onActivityResult(requestCode, resultCode, data);
            mRequestPreference = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRequestPreference != null) {
            outState.putString(SELECTED_PREFERENCE_KEY, mRequestPreference.getKey());
        }
    }

    // === Volumes ===

    private VolumeSeekBarPreference initVolumePreference(String key, int stream, int muteIcon) {
        final VolumeSeekBarPreference volumePref = (VolumeSeekBarPreference) findPreference(key);
        volumePref.setCallback(mVolumeCallback);
        volumePref.setStream(stream);
        mVolumePrefs.add(volumePref);
        volumePref.setMuteIcon(muteIcon);
        if (stream == AudioManager.STREAM_RING) {
           volumePref.setTitleText(mNotificationUseRingVolume == 0 ? mContext.getString(R.string.cn_sound_setting_phone_volume) :
                  mContext.getString(R.string.ring_notification_volume_option_title));
        }
        return volumePref;
    }

    private void updateRingOrNotificationPreference() {
     /*
        mRingOrNotificationPreference.showIcon(mSuppressor != null
                ? com.android.internal.R.drawable.ic_audio_ring_notif_mute
                : mRingerMode == AudioManager.RINGER_MODE_VIBRATE || wasRingerModeVibrate()
                ? com.android.internal.R.drawable.ic_audio_ring_notif_vibrate
                : com.android.internal.R.drawable.ic_audio_ring_notif);
       */


	//AMAX +++ Morphy_Huang@2016/07/14 N-Porting
        if (mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_NORMAL) {
            mRingOrNotificationPreference.showIcon(R.drawable.ic_audio_ring_zenui);
        }
        else {
               if(mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_VIBRATE && mVibrator != null) {
                  mRingOrNotificationPreference.showIcon(R.drawable.ic_audio_ring_vibrate_zenui);
               } else {
                  mRingOrNotificationPreference.showIcon(R.drawable.ic_audio_ring_mute_zenui);
               }
        }
	//AMAX +++
    }

    // +++ Add by Morphy_Huang@2016/11/17 +++
    // NotificationPreference will be removed, when user turn on the sync_volume.
    private void updateNotificationPreference() {
        return ;
/*
        if (mNotificationPreference == null) return;
        if (mNotificationUseRingVolume != 0) {
            mNotificationPreference.onActivityPause();
            mVolumePrefs.remove(mNotificationPreference);
            getPreferenceScreen().removePreference(mNotificationPreference);
            mRingOrNotificationPreference.setTitleText(getResources().getText(R.string.ring_notification_volume_option_title));
        } else {
            mVolumePrefs.add(mNotificationPreference);
            getPreferenceScreen().addPreference(mNotificationPreference);
            mRingOrNotificationPreference.setTitleText(getResources().getText(R.string.ring_volume_option_title));
        }
*/
    }

    //Update volume sliders and Sync_Volume value, when the activity resumes.
    private void updateNotificationSettings() {
        if (mNotificationUseRingVolume == -1) return;
        int notificationUseRingVolume = Settings.System.getInt(getContentResolver(),"asus_notification_use_ring_volume", 0);
        if (mNotificationUseRingVolume != notificationUseRingVolume) {
            mNotificationUseRingVolume = notificationUseRingVolume;
            updateNotificationPreference();
            updateNotificationUseRingVolume();
        }
    }
    // --- Add by Morphy_huang@2016/11/17

    private boolean wasRingerModeVibrate() {
        return mVibrator != null && mRingerMode == AudioManager.RINGER_MODE_SILENT
                && mAudioManager.getLastAudibleStreamVolume(AudioManager.STREAM_RING) == 0;
    }

    private void updateRingerMode() {
        final int ringerMode = mAudioManager.getRingerModeInternal();
		Log.i(TAG,"updateRingerMode.ringerMode = "+ringerMode);
        if (mRingerMode == ringerMode) return;
        mRingerMode = ringerMode;
        updateRingOrNotificationPreference();
    }

    private void updateEffectsSuppressor() {
        final ComponentName suppressor = NotificationManager.from(mContext).getEffectsSuppressor();
        if (Objects.equals(suppressor, mSuppressor)) return;
        mSuppressor = suppressor;
        if (mRingOrNotificationPreference != null) {
            final String text = suppressor != null ?
                    mContext.getString(com.android.internal.R.string.muted_by,
                            getSuppressorCaption(suppressor)) : null;
            mRingOrNotificationPreference.setSuppressionText(text);
        }
        updateRingOrNotificationPreference();
    }

    private String getSuppressorCaption(ComponentName suppressor) {
        final PackageManager pm = mContext.getPackageManager();
        try {
            final ServiceInfo info = pm.getServiceInfo(suppressor, 0);
            if (info != null) {
                final CharSequence seq = info.loadLabel(pm);
                if (seq != null) {
                    final String str = seq.toString().trim();
                    if (str.length() > 0) {
                        return str;
                    }
                }
            }
        } catch (Throwable e) {
            Log.w(TAG, "Error loading suppressor caption", e);
        }
        return suppressor.getPackageName();
    }

    private final class VolumePreferenceCallback implements VolumeSeekBarPreference.Callback {
        private SeekBarVolumizer mCurrent;

        @Override
        public void onSampleStarting(SeekBarVolumizer sbv) {
            if (mCurrent != null && mCurrent != sbv) {
                mCurrent.stopSample();
            }
            mCurrent = sbv;
            if (mCurrent != null) {
                mHandler.removeMessages(H.STOP_SAMPLE);
                mHandler.sendEmptyMessageDelayed(H.STOP_SAMPLE, SAMPLE_CUTOFF);
            }
        }

        @Override
        public void onStreamValueChanged(int stream, int progress) {
            // noop
        }

        public void stopSample() {
            if (mCurrent != null) {
                mCurrent.stopSample();
            }
        }
    };


    // === Phone & notification ringtone ===

    private void initRingtones() {
        final PreferenceCategory ringtoneCategory = (PreferenceCategory) findPreference("ringtone_category");

        mPhoneRingtonePreference = ringtoneCategory.findPreference(KEY_PHONE_RINGTONE);
        // +++ ckenken (ChiaHsiang_Kuo) @ 20160705 N-Porting
        // +++ AMAX
//        if (mPhoneRingtonePreference != null && !mVoiceCapable) {
//            getPreferenceScreen().removePreference(mPhoneRingtonePreference);
//            mPhoneRingtonePreference = null;
//        }
        if (mPhoneRingtonePreference != null) {
            if (!mVoiceCapable ||mIsMultiSimEnabled|| (UserHandle.MU_ENABLED && UserHandle.myUserId() != 0)) {
                ringtoneCategory.removePreference(mPhoneRingtonePreference);
                mPhoneRingtonePreference = null;
            }
        }
        mDualSimPhoneRingtonePreference = ringtoneCategory
                .findPreference(KEY_DUAL_SIM_PHONE_RINGTONE);
        if (mDualSimPhoneRingtonePreference != null) {
            if (!mVoiceCapable ||!mIsMultiSimEnabled|| (UserHandle.MU_ENABLED && UserHandle.myUserId() != 0)) {
                ringtoneCategory.removePreference(mDualSimPhoneRingtonePreference);
                mDualSimPhoneRingtonePreference = null;
            }
        }
        // --- AMAX
        // --- ckenken (ChiaHsiang_Kuo) @ 20160705 N-Porting
        mNotificationRingtonePreference =
                getPreferenceScreen().findPreference(KEY_NOTIFICATION_RINGTONE);
		((DefaultRingtonePreference) mNotificationRingtonePreference).setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
		
        mAlarmRingtonePreference = getPreferenceScreen().findPreference(KEY_ALARM_RINGTONE);

        mCalendarAlertPreference = getPreferenceScreen().findPreference(KEY_CALENDARALERT_RINGTONE);
        ((DefaultRingtonePreference) mCalendarAlertPreference).setRingtoneType(RingtoneManager.TYPE_CALENDARALERT);

        mAlarmPreference = findPreference(KEY_ALARM_RINGTONE);
		if(mCalendarAlertPreference!=null){
			ringtoneCategory.removePreference(mCalendarAlertPreference);
		}
		if(mAlarmPreference != null){
			ringtoneCategory.removePreference(mAlarmPreference);
		}
    }

    // === Phone & notification ringtone ===

    private void lookupRingtoneNames() {
        AsyncTask.execute(mLookupRingtoneNames);
    }

    private final Runnable mLookupRingtoneNames = new Runnable() {
        @Override
        public void run() {
            if (mPhoneRingtonePreference != null) {
                final CharSequence summary = updateRingtoneName(
                        mContext, RingtoneManager.TYPE_RINGTONE);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_PHONE_RINGTONE, summary).sendToTarget();
                }
            }
            if (mNotificationRingtonePreference != null) {
                final CharSequence summary = updateRingtoneName(
                        mContext, RingtoneManager.TYPE_NOTIFICATION);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_NOTIFICATION_RINGTONE, summary).sendToTarget();
                }
            }
            if (mAlarmRingtonePreference != null) {
                final CharSequence summary =
                        updateRingtoneName(mContext, RingtoneManager.TYPE_ALARM);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_ALARM_RINGTONE, summary).sendToTarget();
                }
            }

            //+++ Ashen_gu@20140930 add for multiple notification effects +++
            if (mSentMailPreference != null) {
                final CharSequence summary = updateRingtoneName(
                        mContext, RingtoneManager.TYPE_SENTMAIL);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_SENTMAIL_SUMMARY, summary).sendToTarget();
                }
            }
            if (mCalendarAlertPreference != null) {
                final CharSequence summary = updateRingtoneName(
                        mContext, RingtoneManager.TYPE_CALENDARALERT);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_CALENDARALERT_SUMMARY, summary).sendToTarget();
                }
            }
            if (mAlarmPreference != null) {
                final CharSequence summary = updateRingtoneName(
                        mContext, RingtoneManager.TYPE_ALARM);
                if (summary != null) {
                    mHandler.obtainMessage(H.UPDATE_ALARM_SUMMARY, summary).sendToTarget();
                }
            }
            //--- Ashen_gu ---
        }
    };

    private static CharSequence updateRingtoneName(Context context, int type) {
        if (context == null) {
            Log.e(TAG, "Unable to update ringtone name, no context provided");
            return null;
        }
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(context, type);
		Log.i(TAG,"updateRingtoneName.uri = "+ ringtoneUri);
        CharSequence summary = context.getString(com.android.internal.R.string.ringtone_unknown);
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = context.getString(com.android.internal.R.string.ringtone_silent);
        } else {
            Cursor cursor = null;
            try {
                if (MediaStore.AUTHORITY.equals(ringtoneUri.getAuthority())) {
                    // Fetch the ringtone title from the media provider
                    cursor = context.getContentResolver().query(ringtoneUri,
                            new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
                } else if (ContentResolver.SCHEME_CONTENT.equals(ringtoneUri.getScheme())) {
                    cursor = context.getContentResolver().query(ringtoneUri,
                            new String[] { OpenableColumns.DISPLAY_NAME }, null, null, null);
                }
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        summary = cursor.getString(0);
                    }
                }
            } catch (SQLiteException sqle) {
                // Unknown title for the ringtone
            } catch (IllegalArgumentException iae) {
                // Some other error retrieving the column from the provider
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return summary;
    }

    // +++ Add by Morphy_Huang@2016/11/17 +++
    // Init switchpreference for sync ringtone volume with notification volume
    private void initNotificationUseRingVolumePreference() {
        mNotificationUseRingVolumePreference =
                (TwoStatePreference) getPreferenceScreen().findPreference(KEY_SYNC_RING_VOLUME_WITH_NOTIFICATION_VOLUME);
        if (mNotificationUseRingVolumePreference == null) {
            Log.i(TAG, "Preference not found: " + KEY_SYNC_RING_VOLUME_WITH_NOTIFICATION_VOLUME);
            return;
        }
        if (!mVoiceCapable || mNotificationUseRingVolume == -1) {
            getPreferenceScreen().removePreference(mNotificationUseRingVolumePreference);
            mNotificationUseRingVolumePreference = null;
            return;
        }
        mNotificationUseRingVolumePreference.setPersistent(false);
        updateNotificationUseRingVolume();
        mNotificationUseRingVolumePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                boolean changed = Settings.System.putInt(getContentResolver(),
                                    "asus_notification_use_ring_volume",val ? 1 : 0);
                if (changed) {
                    mNotificationUseRingVolume = val ? 1 : 0;
                    updateNotificationUseRingVolume();
                    updateNotificationPreference();
                }
                return changed;
            }
        });
    }

    private void updateNotificationUseRingVolume() {
        if (mNotificationUseRingVolumePreference == null) return;
        mNotificationUseRingVolumePreference.setChecked(mNotificationUseRingVolume == 1);
    }
    // --- Add by Morphy_huang@2016/11/17 ---

    // === silent mode setting ===

    private void initSilentMode() {
        mSilentSetting = (TwoStatePreference) getPreferenceScreen().findPreference(KEY_SILENT_SETTING);
        if (mSilentSetting == null) {
            Log.i(TAG, "Preference not found: " + KEY_SILENT_SETTING);
            return;
        }

        mSilentSetting.setPersistent(false);
		Log.i(TAG,"initSilentMode.getRingerMode"+mAudioManager.getRingerMode());
		Log.i(TAG,"initSilentMode.getRingerModeInternal"+mAudioManager.getRingerModeInternal());

        if (mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_NORMAL) {
            mSilentSetting.setChecked(false);
        }else{
            mSilentSetting.setChecked(true);
        }

        mSilentSetting.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
				Log.i(TAG,"mSilentSetting.isChecked = "+val);
                if(val){
					if(mVibrateWhenSilent!= null && mVibrateWhenSilent.isChecked()){
						mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);	
					}else{	
						mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
					}
                }else{
					mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL);
                }
                return true;
            }
        });
    }

	private void updateSilentSwitch(){
        Log.i(TAG,"updateSilentSwitch");
        int ringMode = mAudioManager.getRingerModeInternal();
        if(mSilentSetting != null){
            mSilentSetting.setChecked(ringMode != AudioManager.RINGER_MODE_NORMAL);
        }

        if(mVibrateWhenSilent != null){
            boolean oldV = isVibrateWhenSilent();
            boolean newV = oldV;
            if (ringMode == AudioManager.RINGER_MODE_VIBRATE) {
                newV = true;
            } else if (ringMode == AudioManager.RINGER_MODE_SILENT) {
                newV = false;
            }
            if (oldV != newV) {
                mVibrateWhenSilent.setChecked(newV);
                storeVibrateWhenSilent(newV);
            }
        }
    }

    // === Vibrate when ringing ===

    private void initVibrateWhenRinging() {
        mVibrateWhenRinging =
                (TwoStatePreference) getPreferenceScreen().findPreference(KEY_VIBRATE_WHEN_RINGING);
        if (mVibrateWhenRinging == null) {
            Log.i(TAG, "Preference not found: " + KEY_VIBRATE_WHEN_RINGING);
            return;
        }
        if (!mVoiceCapable) {
            getPreferenceScreen().removePreference(mVibrateWhenRinging);
            mVibrateWhenRinging = null;
            return;
        }
        mVibrateWhenRinging.setPersistent(false);
        updateVibrateWhenRinging();
        mVibrateWhenRinging.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.VIBRATE_WHEN_RINGING,
                        val ? 1 : 0);
            }
        });
    }

//Sharon+++
    private void initVibrateWhenRinging_new(PreferenceCategory root) {
        mVibrateWhenRinging = (TwoStatePreference) root.findPreference(KEY_VIBRATE_WHEN_RINGING);
        if (mVibrateWhenRinging == null) {
            Log.i(TAG, "Preference not found: " + KEY_VIBRATE_WHEN_RINGING);
            return;
        }
        if (!mVoiceCapable) {
            root.removePreference(mVibrateWhenRinging);
            mVibrateWhenRinging = null;
            return;
        }
        mVibrateWhenRinging.setPersistent(false);
        updateVibrateWhenRinging();
        mVibrateWhenRinging.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                Log.d(TAG, "onPreferenceChange+initVibrateWhenRinging_new+val=" + val);
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.VIBRATE_WHEN_RINGING,
                        val ? 1 : 0);
            }
        });
    }
//Sharon---
    private void updateVibrateWhenRinging() {
        if (mVibrateWhenRinging == null) return;
        mVibrateWhenRinging.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) != 0);
    }

//Ashen+++ === Vibrate when silent ===
    private void initVibrateWhenSilent(PreferenceCategory root) {
        mVibrateWhenSilent = (TwoStatePreference) root.findPreference(KEY_VIBRATE_WHEN_SILENT);
        if (mVibrateWhenSilent == null) {
            Log.i(TAG, "Preference not found: " + KEY_VIBRATE_WHEN_SILENT);
            return;
        }
        if (!mVoiceCapable) {
            root.removePreference(mVibrateWhenSilent);
            mVibrateWhenRinging = null;
            return;
        }
        mVibrateWhenSilent.setPersistent(false);
		
		//SharedPreferences prefs =PreferenceManager.getDefaultSharedPreferences(getActivity()) ;
		mVibrateWhenSilent.setChecked(isVibrateWhenSilent());

        mVibrateWhenSilent.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                if(mSilentSetting!=null && mSilentSetting.isChecked()){
					if(val){
						mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_VIBRATE);
					}else{
						mAudioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
					}
                }
				storeVibrateWhenSilent(val);
				updateRingOrNotificationPreference();
                return true;
            }
        });
    }
	
	private boolean isVibrateWhenSilent(){
		SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(PREFERENCE_NAME,getActivity().MODE_PRIVATE);
		boolean result = mSharedPreferences.getBoolean(KEY_VIBRATE_WHEN_SILENT,false);
		//boolean def = mAudioManager.getRingerModeInternal() == AudioManager.RINGER_MODE_VIBRATE ? true : false;
		//result = def ? true : result;
		return result;
	}
	
	private void storeVibrateWhenSilent(boolean val){
		SharedPreferences mSharedPreferences = getActivity().getSharedPreferences(PREFERENCE_NAME,getActivity().MODE_PRIVATE);
		SharedPreferences.Editor editor = mSharedPreferences.edit();
		editor.putBoolean(KEY_VIBRATE_WHEN_SILENT,val);        	
		editor.commit();	
	}

//Ashen---

//Ashen+++
   // === dial pad tone ===
    private void initDialPadTone(PreferenceCategory root) {
        mDialPadTone = (TwoStatePreference) root.findPreference(KEY_PAD_TOUCH_TONES);
        if (mDialPadTone == null) {
            Log.i(TAG, "Preference not found: " + KEY_PAD_TOUCH_TONES);
            return;
        }
        if (!mVoiceCapable) {
            root.removePreference(mDialPadTone);
            mDialPadTone = null;
            return;
        }

        mDialPadTone.setPersistent(false);
        updateDialPadTone();
        mDialPadTone.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                Log.d(TAG,"onPreferenceChange+HAPTIC_FEEDBACK_ENABLED+val="+val);
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.DTMF_TONE_WHEN_DIALING,
                        val ? 1 : 0);
            }
        });
    }

    private void updateDialPadTone() {
        if (mDialPadTone == null) return;
        Log.d(TAG,"updateDialPadTone");
        mDialPadTone.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.DTMF_TONE_WHEN_DIALING, 0) != 0);
    }

   // === touch sound ===
    private void initTouchSound(PreferenceCategory root) {
        mTouchSound = (TwoStatePreference) root.findPreference(KEY_TOUCH_SOUNDS);
        if (mTouchSound == null) {
            Log.i(TAG, "Preference not found: " + KEY_TOUCH_SOUNDS);
            return;
        }

        mTouchSound.setPersistent(false);
        updateTouchSound();
        mTouchSound.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                Log.d(TAG,"onPreferenceChange+KEY_TOUCH_SOUNDS+val="+val);
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.SOUND_EFFECTS_ENABLED,
                        val ? 1 : 0);
            }
        });
    }

    private void updateTouchSound() {
        if (mTouchSound == null) return;
        Log.d(TAG,"updateDialPadTone");
        mTouchSound.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.SOUND_EFFECTS_ENABLED, 0) != 0);
    }

   // === screen sound ===
    private void initScreenSound(PreferenceCategory root) {
        mScreenSound = (TwoStatePreference) root.findPreference(KEY_SCREEN_LOCKING_SOUNDS);

        mScreenSound.setPersistent(false);
        updateScreenSound();
        mScreenSound.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                Log.d(TAG,"onPreferenceChange+KEY_SCREEN_LOCKING_SOUNDS+val="+val);
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.LOCKSCREEN_SOUNDS_ENABLED,
                        val ? 1 : 0);
            }
        });
    }

    private void updateScreenSound() {
        if (mScreenSound == null) return;
        Log.d(TAG,"updateScreenSound");
        mScreenSound.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_SOUNDS_ENABLED, 0) != 0);
    }
//Ashen---

//+++Leaon
    private void initScreenshotSound(PreferenceCategory root){
	mScreenshotSound = (TwoStatePreference) root.findPreference(KEY_SCREENSHOT_SOUND);
	updateScreenshotSound();
	mScreenshotSound.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                Log.d(TAG,"onPreferenceChange+KEY_SCREENSHOT_SOUND+val="+val);
                return Settings.System.putInt(getContentResolver(), KEY_SCREENSHOT_SOUND, val ? 1 : 0);
            }
        });

    }

    private void updateScreenshotSound() {
        if (mScreenshotSound == null) return;
        Log.d(TAG,"updateScreenshotSound");
        mScreenshotSound.setChecked(Settings.System.getInt(getContentResolver(), KEY_SCREENSHOT_SOUND, 1) == 1);
    }

    private void initAudioWizard(PreferenceCategory root){
        mAudioWizard = (Preference) root.findPreference(KEY_AUDIO_WIZARD1);
        if (mAudioWizard != null) {
            if (!audioWizardEnable()) {
                root.removePreference(mAudioWizard);
            } else {
                mAudioWizard.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Log.i(TAG, "open audio wizard");
                        Intent intent = new Intent();
                        intent.setClassName("com.asus.maxxaudio.audiowizard",
                                "com.asus.maxxaudio.audiowizard.MainActivity");
                        startActivity(intent);

                        return true;
                    }
                });
            }
        }
    }

    private boolean audioWizardEnable(){
        try {
            ApplicationInfo service = getPackageManager().getApplicationInfo("com.asus.maxxaudio", 0);
            ApplicationInfo ui = getPackageManager().getApplicationInfo("com.asus.maxxaudio.audiowizard", 0);
            boolean serviceEnable = service.enabled;
            boolean UIenable = ui.enabled;
            return serviceEnable && UIenable;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

//---Leaon

//+++Leaon_Wang
    private void initVibrateIntensity(PreferenceCategory root){
		mVibrateIntensityPreference = (PreferenceScreen) root.findPreference(KEY_VIBRATE_INTENSITY);
		if (mVibrateIntensityPreference == null) {
            Log.i(TAG, "Preference not found: " + KEY_VIBRATE_INTENSITY);
            return;
        }
        mVibrateIntensityPreference.setPersistent(false);
		Log.i(TAG,"initVibrateIntensity()");
		mVibrateIntensityPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
				Log.i(TAG,"initVibrateIntensity.click");
				initVibrateIntensityDialog();
                return true;
            }
        });
		//initVibrateIntensitySummary();
    }

    private void initVibrateIntensitySummary(){
		int value = Settings.System.getInt(getContext().getContentResolver(), Settings.System.VIBRATION_LEVEL_TOUCH, 0);
		Log.i(TAG,"VibrateIntensity value = " + value);
		if (mVibrateIntensityPreference == null) {
            Log.i(TAG, "Preference not found: " + KEY_VIBRATE_INTENSITY);
            return;
        }
		updateVibrateIntensityPreference(value);
    }
	
    private void initVibrateIntensityDialog(){
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.cn_sound_settings_touch_vibrate_dialog_layout,null);
        AlertDialog dialog = new AlertDialog.Builder(getActivity()).setView(view)
							.setNegativeButton(android.R.string.ok,null).create();
        Window window = dialog.getWindow();
        window.setGravity(Gravity.BOTTOM);
        window.setWindowAnimations(R.style.CNSoundSettingsDialogStyle);
        dialog.show();
	
		RadioGroup radiogroup = (RadioGroup)view.findViewById(R.id.vibrate_radiogroup);
		int checkItem = Settings.System.getInt(getContext().getContentResolver(), Settings.System.VIBRATION_LEVEL_TOUCH, 0);;
		checkItem = getDefaultVibrateIntensity(checkItem);
		switch(checkItem){
			case VIBRATE_INTENSITY_CLOSE:
				radiogroup.check(R.id.close_radiobtn);
			break;
			case VIBRATE_INTENSITY_WEEK:
				radiogroup.check(R.id.week_radiobtn);
			break;
			case VIBRATE_INTENSITY_NORMAL:
				radiogroup.check(R.id.normal_radiobtn);
			break;
			case VIBRATE_INTENSITY_STRONG:
				radiogroup.check(R.id.strong_radiobtn);
			break;
	}
	radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int item) {
			switch(item){
				case R.id.close_radiobtn:
					setTouchVibrate(VIBRATE_INTENSITY_CLOSE);
				break;
				case R.id.week_radiobtn:
					setTouchVibrate(VIBRATE_INTENSITY_WEEK);
				break;
				case R.id.normal_radiobtn:
					setTouchVibrate(VIBRATE_INTENSITY_NORMAL);
				break;
				case R.id.strong_radiobtn:
					setTouchVibrate(VIBRATE_INTENSITY_STRONG);
				break;
			}
	    }
	});
}

    private void setTouchVibrate(int value){
		Log.i(TAG,"setTouchVibrate value="+value);
		Settings.System.putInt(getContentResolver(),
                        Settings.System.HAPTIC_FEEDBACK_ENABLED,value==VIBRATE_INTENSITY_CLOSE? 0 : 1);
		AudioAttributes mRingtoneAttr = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_UNKNOWN)
                        .build();
		if(mVibrator != null && value!=VIBRATE_INTENSITY_CLOSE){
			mVibrator.vibrate(100, mRingtoneAttr, (Integer) value, AudioAttributes.CATEGORY_TOUCH);
		}
		new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... values) {
                Settings.System.putInt(getContext().getContentResolver(), Settings.System.VIBRATION_LEVEL_TOUCH, value);
                return null;
            }
        }.execute();
		updateVibrateIntensityPreference(value);
    }
	
	private int getDefaultVibrateIntensity(int value){
		if(Settings.System.getInt(getContentResolver(),Settings.System.HAPTIC_FEEDBACK_ENABLED,0) == 0)
			return VIBRATE_INTENSITY_CLOSE;
		if(value == VIBRATE_INTENSITY_CLOSE || value == VIBRATE_INTENSITY_WEEK
				|| value == VIBRATE_INTENSITY_NORMAL || value == VIBRATE_INTENSITY_STRONG){
			return value;
		}
		int temp = VIBRATE_INTENSITY_CLOSE;
		if(value < VIBRATE_INTENSITY_CLOSE){
			temp = VIBRATE_INTENSITY_CLOSE;
		}
		if(value > VIBRATE_INTENSITY_CLOSE){
			temp = VIBRATE_INTENSITY_WEEK;
		}
		if(value > VIBRATE_INTENSITY_WEEK){
			temp = VIBRATE_INTENSITY_NORMAL;
		}
		if(value > VIBRATE_INTENSITY_NORMAL){
			temp = VIBRATE_INTENSITY_STRONG;
		}
		return temp;
	}

    private void updateVibrateIntensityPreference(int value){
	if (mVibrateIntensityPreference == null) {
            Log.i(TAG, "Preference not found: " + KEY_VIBRATE_INTENSITY);
            return;
        }
		String summary = "";
		switch(value){
			case VIBRATE_INTENSITY_CLOSE:
			summary = getString(R.string.cn_sound_settings_vibrate_intensity_close_radiobtn);
			break;
			case VIBRATE_INTENSITY_WEEK:
			summary = getString(R.string.cn_sound_settings_vibrate_intensity_week_radiobtn);
			break;
			case VIBRATE_INTENSITY_NORMAL:
			summary = getString(R.string.cn_sound_settings_vibrate_intensity_normal_radiobtn);
			break;
			case VIBRATE_INTENSITY_STRONG:
			summary = getString(R.string.cn_sound_settings_vibrate_intensity_strong_radiobtn);
			break;
		}
		mVibrateIntensityPreference.setSummary(summary);
    }
//---Leaon_Wang

//Sharon+++
   // === Vibrate on touch ===
    private void initVibrateOnTouch(PreferenceCategory root) {
        mVibrateOnTouch = (TwoStatePreference) root.findPreference(KEY_VIBRATE_ON_TOUCH);
        if (mVibrateOnTouch == null) {
            Log.i(TAG, "Preference not found: " + KEY_VIBRATE_ON_TOUCH);
            return;
        }
        mVibrateOnTouch.setPersistent(false);
        updateVibrateOnTouch();
        mVibrateOnTouch.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                Log.d(TAG,"onPreferenceChange+HAPTIC_FEEDBACK_ENABLED+val="+val);
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.HAPTIC_FEEDBACK_ENABLED,
                        val ? 1 : 0);
            }
        });
    }

    private void updateVibrateOnTouch() {
        if (mVibrateOnTouch == null) return;
        Log.d(TAG,"updateVibrateWhenRinging");
        mVibrateOnTouch.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) != 0);
    }
//Sharon---
   //+++ nancy_jiang check app is enabled or disabled
    private boolean isAppEnabled(Context mContext,String packageName){
        try {
            ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo(packageName, 0);
            if (ai != null) {
                return ai.enabled;
            }
            return false;
        } catch (PackageManager.NameNotFoundException exp) {
            return false;
        }
    }
    //--- nancy_jiang
    // === Callbacks ===

    private final class SettingsObserver extends ContentObserver {
        private final Uri VIBRATE_WHEN_RINGING_URI =
                Settings.System.getUriFor(Settings.System.VIBRATE_WHEN_RINGING);

        public SettingsObserver() {
            super(mHandler);
        }

        public void register(boolean register) {
            final ContentResolver cr = getContentResolver();
            if (register) {
                cr.registerContentObserver(VIBRATE_WHEN_RINGING_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (VIBRATE_WHEN_RINGING_URI.equals(uri)) {
                updateVibrateWhenRinging();
            }
        }
    }

    private final class H extends Handler {
        private static final int UPDATE_PHONE_RINGTONE = 1;
        private static final int UPDATE_NOTIFICATION_RINGTONE = 2;
        private static final int STOP_SAMPLE = 3;
        private static final int UPDATE_EFFECTS_SUPPRESSOR = 4;
        private static final int UPDATE_RINGER_MODE = 5;
        private static final int UPDATE_ALARM_RINGTONE = 6;
        //+++ Ashen_gu@20140930 add for multiple notification effects +++
        private static final int UPDATE_NEWMAIL_SUMMARY = 7;
        private static final int UPDATE_SENTMAIL_SUMMARY = 8;
        private static final int UPDATE_CALENDARALERT_SUMMARY = 9;
        private static final int UPDATE_ALARM_SUMMARY = 10;
        //--- Ashen_gu ---
		private static final int UPDATE_OTHER_SUMMARY = 11;

        private H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_PHONE_RINGTONE:
                    mPhoneRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
                case UPDATE_NOTIFICATION_RINGTONE:
                    mNotificationRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
                //+++ Ashen_gu@20140930 add for multiple notification effects +++
                case UPDATE_SENTMAIL_SUMMARY:
                    mSentMailPreference.setSummary((CharSequence) msg.obj);
                    break;
                case UPDATE_CALENDARALERT_SUMMARY:
					if(mCalendarAlertPreference != null)
                    mCalendarAlertPreference.setSummary((CharSequence) msg.obj);
                    break;
                case UPDATE_ALARM_SUMMARY:
					if(mAlarmPreference!=null)
                    mAlarmPreference.setSummary((CharSequence) msg.obj);
                    break;
                //--- Ashen_gu ---
                case STOP_SAMPLE:
                    mVolumeCallback.stopSample();
                    break;
                case UPDATE_EFFECTS_SUPPRESSOR:
                    updateEffectsSuppressor();
                    break;
                case UPDATE_RINGER_MODE:
                    updateRingerMode();
					updateSilentSwitch();
                    break;
                case UPDATE_ALARM_RINGTONE:
                    mAlarmRingtonePreference.setSummary((CharSequence) msg.obj);
                    break;
            }
        }
    }

    private class Receiver extends BroadcastReceiver {
        private boolean mRegistered;

        public void register(boolean register) {
            if (mRegistered == register) return;
            if (register) {
                final IntentFilter filter = new IntentFilter();
                filter.addAction(NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                mContext.registerReceiver(this, filter);
            } else {
                mContext.unregisterReceiver(this);
            }
            mRegistered = register;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
			Log.i(TAG,"onReceive.action="+action);
            if (NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED.equals(action)) {
                mHandler.sendEmptyMessage(H.UPDATE_EFFECTS_SUPPRESSOR);
            } else if (AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION.equals(action)) {
                mHandler.sendEmptyMessage(H.UPDATE_RINGER_MODE);
            }
        }
    }

    // === Summary ===

    private static class SummaryProvider extends BroadcastReceiver
            implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final AudioManager mAudioManager;
        private final SummaryLoader mSummaryLoader;
        private final int maxVolume;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
            maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
                filter.addAction(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
                filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
                filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                filter.addAction(AudioManager.STREAM_MUTE_CHANGED_ACTION);
                filter.addAction(NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED);
                mContext.registerReceiver(this, filter);
            } else {
                mContext.unregisterReceiver(this);
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String percent =  NumberFormat.getPercentInstance().format(
                    (double) mAudioManager.getStreamVolume(AudioManager.STREAM_RING) / maxVolume);
//            mSummaryLoader.setSummary(this,
//                    mContext.getString(R.string.sound_settings_summary, percent));
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    // === Indexing ===

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            final SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.sound_settings;
            return Arrays.asList(sir);
        }

        public List<String> getNonIndexableKeys(Context context) {
            final ArrayList<String> rt = new ArrayList<String>();
            if (Utils.isVoiceCapable(context)) {
                rt.add(KEY_NOTIFICATION_VOLUME);
                if (Utils.isMultiSimEnabled(context)) {
                    rt.add(KEY_PHONE_RINGTONE);
                } else {
                    rt.add(KEY_DUAL_SIM_PHONE_RINGTONE);
                }
            } else {
                rt.add(KEY_RING_VOLUME);
                rt.add(KEY_PHONE_RINGTONE);
                rt.add(KEY_DUAL_SIM_PHONE_RINGTONE);
                rt.add(KEY_WIFI_DISPLAY);
                rt.add(KEY_VIBRATE_WHEN_RINGING);
            }

            final PackageManager pm = context.getPackageManager();
            final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);

            // Enable link to CMAS app settings depending on the value in config.xml.
            boolean isCellBroadcastAppLinkEnabled = context.getResources().getBoolean(
                    com.android.internal.R.bool.config_cellBroadcastAppLinks);
            try {
                if (isCellBroadcastAppLinkEnabled) {
                    if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                            == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                        isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                    }
                }
            } catch (IllegalArgumentException ignored) {
                isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
            }
            if (!um.isAdminUser() || !isCellBroadcastAppLinkEnabled) {
                rt.add(KEY_CELL_BROADCAST_SETTINGS);
            }

            return rt;
        }
    };
}
