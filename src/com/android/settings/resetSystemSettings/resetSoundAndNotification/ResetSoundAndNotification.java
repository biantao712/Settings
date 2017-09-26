package com.android.settings.resetSystemSettings.resetSoundAndNotification;

import android.content.Context;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.util.Log;

import com.android.settings.resetSystemSettings.ResetSettingsBaseClass;

/**
 * Created by JimCC
 * Reference {@link com.android.settings.notification.NotificationSettings}
 * Reference {@link
 * com.android.settings.resetSystemSettings.resetDatabaseTables.ResetSystemTable#loadVolumeLevels()
 * }
 *
 * Since we do not use: mVoiceCapable = Utils.isVoiceCapable(mContext);
 * {@link #resetStreamVolume()} may be needed for {@link AudioManager.STREAM_RING}
 */
public class ResetSoundAndNotification extends ResetSettingsBaseClass {
    private static final String TAG = "ResetSoundAndNotification";
    final int[] defaults = AudioSystem.DEFAULT_STREAM_VOLUME;
    private AudioManager mAudioManager;

    public ResetSoundAndNotification(Context context) {
        super(context);
    }

    public void run() {
        Log.i(TAG, "run()");
        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        resetSound();
        resetStreamVolume();
    }

    private void resetSound() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                defaults[AudioSystem.STREAM_MUSIC], 0);
        // If muted, the UI (system UI) may not show the updated results
        // This may request system UI team to handle it
        mAudioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                defaults[AudioSystem.STREAM_ALARM], 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION,
                defaults[AudioSystem.STREAM_NOTIFICATION], 0);
    }

    private void resetStreamVolume() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING,
                defaults[AudioSystem.STREAM_RING], 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_SYSTEM,
                defaults[AudioSystem.STREAM_SYSTEM], 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                defaults[AudioSystem.STREAM_VOICE_CALL], 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_BLUETOOTH_SCO,
                defaults[AudioSystem.STREAM_BLUETOOTH_SCO], 0);
    }
}
