package com.android.settings.resetSystemSettings.resetDatabaseTables;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.resetSystemSettings.ResetSettingsBaseClass;

/**
 * Created by JimCC
 */
public class ResetSystemTable extends ResetSettingsBaseClass {
    private static final String TAG = "ResetSystemTable";

    public ResetSystemTable(Context context) {
        super(context);
    }

    public void run() {
        Log.i(TAG, "run()");
        loadSystemSettings();
    }

    /**
     * Reference
     * {@link com.android.providers.settings.DatabaseHelper#loadSystemSettings(
     * SQLiteDatabase db)}
     */
    private void loadSystemSettings() {
        loadVolumeLevels();

        Settings.System.putInt(mResolver, Settings.System.SCREEN_OFF_TIMEOUT,
                Build.TYPE.equals("eng") ? 0 : getInteger(R.integer.def_screen_off_timeout));
        Settings.System.putInt(mResolver, Settings.System.DTMF_TONE_TYPE_WHEN_DIALING, 0);
        Settings.System.putInt(mResolver,Settings.System.HEARING_AID, 0);
        Settings.System.putInt(mResolver,Settings.System.TTY_MODE, 0);

        /*
         * Correspond to "Brightness level -> Automatic brightness"
         * If automatic brightness is not set, SCREEN_BRIGHTNESS will be set first and then
         * System.SCREEN_BRIGHTNESS_MODE on / off
         * if sensor!=null -> on -> set previous value of Automatic mode
         * this may change the brightness TWO times, change the code later (?)
         * ex: check current settings to decide the reset steps
         */
        Settings.System.putInt(mResolver, Settings.System.SCREEN_BRIGHTNESS,
                getInteger(R.integer.def_screen_brightness));
        SensorManager sensorManager = mContext.getSystemService(SensorManager.class);
        Settings.System.putInt(mResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                (sensorManager != null &&
                        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null)
                        || getBoolean(R.bool.def_screen_brightness_automatic_mode) ? 1 : 0);

        // Move to Global
        //loadDefaultAnimationSettings();

        Settings.System.putInt(mResolver, Settings.System.ACCELEROMETER_ROTATION,
                getBoolean(R.bool.def_accelerometer_rotation) ? 1 : 0);

        Settings.System.putInt(mResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED,
                getBoolean(R.bool.def_haptic_feedback) ? 1 : 0);
        Settings.System.putInt(mResolver, Settings.System.NOTIFICATION_LIGHT_PULSE,
                getBoolean(R.bool.def_notification_pulse) ? 1 : 0);

        loadUISoundEffectsSettings();

        // Do NOT reset Language & Input (VZ_REQ_SOFTRESETMODE_32375)
        /*
        Settings.System.putInt(mResolver, Settings.System.POINTER_SPEED,
                getInteger(R.integer.def_pointer_speed));
        */

        Settings.System.putInt(mResolver, Settings.System.ASUS_LOCKSCREEN_SKIP_SLID_DISABLED,
                getInteger(R.integer.def_skip_slide_disabled));
        Settings.System.putInt(mResolver, Settings.System.ASUS_TRANSCOVER,
                Settings.System.ASUS_TRANSCOVER_DEFAULT_MODE);
        Settings.System.putInt(mResolver, Settings.System.ASUS_TRANSCOVER_AUTOMATIC_UNLOCK, 1);

        // Move to Secure
        //Settings.Secure.putInt(mResolver, Settings.Secure.ASUS_LOCKSCREEN_DISPLAY_APP, 1);

        Settings.System.putInt(mResolver, Settings.System.ASUS_LOCKSCREEN_WHATSNEXT, 1);
        Settings.System.putInt(mResolver, Settings.System.ASUS_ANALYTICS, 0);

        if (Build.TYPE.equals("eng")) {
            Settings.System.putInt(mResolver, Settings.System.RIGHT_BUTTON_MAPPING,
                    android.view.MotionEvent.BUTTON_SECONDARY);
            Settings.System.putInt(mResolver, Settings.System.MIDDLE_BUTTON_MAPPING,
                    android.view.MotionEvent.BUTTON_TERTIARY);
        } else {
            /*
            Settings.System.putInt(mResolver, Settings.System.RIGHT_BUTTON_MAPPING,
                    getInteger(R.integer.def_right_button_mapping));
            Settings.System.putString(mResolver, Settings.System.MIDDLE_BUTTON_MAPPING,
                    getInteger(R.integer.def_middle_button_mapping));
            */
        }
        loadZenMotionSettings();

        Settings.System.putInt(mResolver, Settings.System.DETECT_DRAIN_APPS, 1);
    }

    /**
     * Reference
     * {@link com.android.providers.settings.DatabaseHelper#loadVolumeLevels(
     * SQLiteDatabase db)}
     * {@link com.android.providers.settings.DatabaseHelper#loadVibrateWhenRingingSetting(
     * SQLiteDatabase db)}
     */
    private void loadVolumeLevels() {
        final int[] defaults = AudioSystem.DEFAULT_STREAM_VOLUME;
        Settings.System.putInt(mResolver, Settings.System.VOLUME_MUSIC,
                defaults[AudioSystem.STREAM_MUSIC]);
        Settings.System.putInt(mResolver, Settings.System.VOLUME_RING,
                defaults[AudioSystem.STREAM_RING]);
        Settings.System.putInt(mResolver, Settings.System.VOLUME_SYSTEM,
                defaults[AudioSystem.STREAM_SYSTEM]);
        Settings.System.putInt(mResolver, Settings.System.VOLUME_VOICE,
                defaults[AudioSystem.STREAM_VOICE_CALL]);
        Settings.System.putInt(mResolver, Settings.System.VOLUME_ALARM,
                defaults[AudioSystem.STREAM_ALARM]);
        Settings.System.putInt(mResolver, Settings.System.VOLUME_NOTIFICATION,
                defaults[AudioSystem.STREAM_NOTIFICATION]);
        Settings.System.putInt(mResolver, Settings.System.VOLUME_BLUETOOTH_SCO,
                defaults[AudioSystem.STREAM_BLUETOOTH_SCO]);
        Settings.System.putInt(mResolver, Settings.System.MUTE_STREAMS_AFFECTED,
                AudioSystem.DEFAULT_MUTE_STREAMS_AFFECTED);
        int ringerModeAffectedStreams = (1 << AudioSystem.STREAM_RING) |
                (1 << AudioSystem.STREAM_NOTIFICATION) |
                (1 << AudioSystem.STREAM_SYSTEM) |
                (1 << AudioSystem.STREAM_SYSTEM_ENFORCED);
        if (getBoolean(com.android.internal.R.bool.config_voice_capable)) {
            ringerModeAffectedStreams |= (1 << AudioSystem.STREAM_MUSIC);
        }
        Settings.System.putInt(mResolver, Settings.System.MODE_RINGER_STREAMS_AFFECTED,
                ringerModeAffectedStreams);

        // loadVibrateWhenRingingSetting
        // The default should be off. VIBRATE_SETTING_ONLY_SILENT should also be ignored here.
        // Phone app should separately check whether AudioManager#getRingerMode() returns
        // RINGER_MODE_VIBRATE, with which the device should vibrate anyway.
        final boolean vibrateWhenRinging = (AudioManager.VIBRATE_SETTING_ON == Settings.System
                .getInt(mResolver, Settings.System.VIBRATE_ON, AudioManager.VIBRATE_SETTING_OFF));
        Settings.System.putInt(mResolver, Settings.System.VIBRATE_WHEN_RINGING,
                vibrateWhenRinging ? 1 : 0);
    }

    private void loadZenMotionSettings() {
        Settings.System.putInt(mResolver, Settings.System.ASUS_ONE_HAND_OPERATION, 0);
        Settings.System.putInt(mResolver, Settings.System.ASUS_DOUBLE_TAP, 0);
        /*
        TelephonyManager telephony = mContext.getSystemService(TelephonyManager.class);
        if (telephony != null && telephony.isVoiceCapable()) {
            // Phone
            Settings.System.putString(mResolver,
                    Settings.System.GESTURE_TYPE2_APP,
                    res.getString(R.string.def_gesture_type2_app));
            Settings.System.putString(
                    mResolver,
                    Settings.System.GESTURE_TYPE6_APP,
                    res.getString(R.string.def_gesture_type6_app));
        } else {
            // Pad
            Settings.System.putString(mResolver, Settings.System.GESTURE_TYPE2_APP,
                    res.getString(R.string.def_gesture_type2_app_pad));
            Settings.System.putString(mResolver, Settings.System.GESTURE_TYPE6_APP,
                    res.getString(R.string.def_gesture_type6_app_pad));
        }
        */
        // vendor/amax/overlays/common/frameworks/base/packages/SettingsProvider/res/values/
        // - defaults.xml
        // - add_resource.xml
        Settings.System.putInt(mResolver, Settings.System.ASUS_MOTION_SHAKE, 0);
        Settings.System.putInt(mResolver, Settings.System.ASUS_SHAKE_SENSITIVITY, 1);
        /*
        Settings.System.putString(mResolver, Settings.System.GESTURE_TYPE1_APP,
                getString(R.string.def_gesture_type1_app));
        Settings.System.putString(mResolver, Settings.System.GESTURE_TYPE2_APP,
                getString(R.string.def_gesture_type2_app));
        Settings.System.putString(mResolver, Settings.System.GESTURE_TYPE3_APP,
                getString(R.string.def_gesture_type3_app));
        Settings.System.putString(mResolver, Settings.System.GESTURE_TYPE4_APP,
                getString(R.string.def_gesture_type4_app));
        Settings.System.putString(mResolver, Settings.System.GESTURE_TYPE5_APP,
                getString(R.string.def_gesture_type5_app));
        Settings.System.putString(mResolver, Settings.System.GESTURE_TYPE6_APP,
                getString(R.string.def_gesture_type6_app));
        */
    }

    // Move to Global
    /*
    private void loadDefaultAnimationSettings() {
        Settings.Global.putFloat(mResolver, Settings.Global.WINDOW_ANIMATION_SCALE,
                getFraction(R.fraction.def_window_animation_scale, 1, 1));
        Settings.Global.putFloat(mResolver, Settings.Global.TRANSITION_ANIMATION_SCALE,
                getFraction(R.fraction.def_window_transition_scale, 1, 1));
    }
    */

    private void loadUISoundEffectsSettings() {
        Settings.System.putInt(mResolver, Settings.System.DTMF_TONE_WHEN_DIALING,
                getBoolean(R.bool.def_dtmf_tones_enabled) ? 1 : 0);
        Settings.System.putInt(mResolver, Settings.System.SOUND_EFFECTS_ENABLED,
                getBoolean(R.bool.def_sound_effects_enabled) ? 1 : 0);
        Settings.System.putInt(mResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED,
                getBoolean(R.bool.def_haptic_feedback) ? 1 : 0);
        Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_SOUNDS_ENABLED,
                getInteger(R.integer.def_lockscreen_sounds_enabled));
    }
}
