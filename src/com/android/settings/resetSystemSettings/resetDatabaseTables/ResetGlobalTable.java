package com.android.settings.resetSystemSettings.resetDatabaseTables;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.android.ims.ImsConfig;
import com.android.internal.content.PackageHelper;
import com.android.internal.telephony.RILConstants;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;
import com.android.settings.R;
import com.android.settings.resetSystemSettings.ResetSettingsBaseClass;

/**
 * Created by JimCC
 */
public class ResetGlobalTable extends ResetSettingsBaseClass {
    private static final String TAG = "ResetGlobalTable";

    public ResetGlobalTable(Context context) {
        super(context);
    }

    public void run() {
        Log.i(TAG, "run()");
        loadGlobalSettings();
    }

    /**
     * Reference
     * {@link com.android.providers.settings.DatabaseHelper#loadGlobalSettings(
     * SQLiteDatabase db)}
     */
    private void loadGlobalSettings() {
        loadDefaultAnimationSettings();

        Settings.Global.putInt(mResolver, Settings.Global.AIRPLANE_MODE_ON,
                getBoolean(R.bool.def_airplane_mode_on) ? 1 : 0);
        Settings.Global.putInt(mResolver, Settings.Global.THEATER_MODE_ON,
                getBoolean(R.bool.def_theater_mode_on) ? 1 : 0);
        Settings.Global.putString(mResolver, Settings.Global.AIRPLANE_MODE_RADIOS,
                mContext.getString(R.string.def_airplane_mode_radios));
        Settings.Global.putString(mResolver, Settings.Global.AIRPLANE_MODE_TOGGLEABLE_RADIOS,
                mContext.getString(R.string.airplane_mode_toggleable_radios));
        Settings.Global.putInt(mResolver, Settings.Global.ASSISTED_GPS_ENABLED,
                getBoolean(R.bool.assisted_gps_enabled) ? 1 : 0);
        Settings.Global.putInt(mResolver, Settings.Global.AUTO_TIME,
                getBoolean(R.bool.def_auto_time) ? 1 : 0);
        Settings.Global.putInt(mResolver, Settings.Global.AUTO_TIME_ZONE,
                getBoolean(R.bool.def_auto_time_zone) ? 1 : 0);
        Settings.Global.putInt(mResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                ("1".equals(SystemProperties.get("ro.kernel.qemu")) ||
                        getBoolean(R.bool.def_stay_on_while_plugged_in))
                        ? 1 : 0);
        Settings.Global.putInt(mResolver, Settings.Global.WIFI_SLEEP_POLICY,
                getInteger(R.integer.def_wifi_sleep_policy));
        Settings.Global.putInt(mResolver, Settings.Global.MOBILE_SLEEP_POLICY,
                getInteger(R.integer.def_mobile_sleep_policy));

        Settings.Global.putInt(mResolver, Settings.Global.MODE_RINGER,
                AudioManager.RINGER_MODE_NORMAL);
        AudioManager audioManager = mContext.getSystemService(AudioManager.class);
        if (audioManager != null) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            audioManager.reloadAudioSettings();
        }

        Settings.Global.putInt(mResolver,
                Settings.Global.POWER_SAVING_WARNING_ACTIONBAR_ENABLED, 1);
        // {@href="https://android.googlesource.com/platform/frameworks/base/+/656fa7f%5E!/"}
        Settings.Global.putInt(mResolver, Settings.Global.PACKAGE_VERIFIER_ENABLE,
                getBoolean(R.bool.def_package_verifier_enable) ? 1 : 0);
        WifiManager wifiManager = mContext.getSystemService(WifiManager.class);
        if (wifiManager != null) {
            wifiManager.factoryReset();
        }
        Settings.Global.putInt(mResolver, Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                getBoolean(R.bool.def_networks_available_notification_on) ? 1 : 0);
        Settings.Global.putInt(mResolver, Settings.Global.BLUETOOTH_ON,
                getBoolean(R.bool.def_bluetooth_on) ? 1 : 0);
        BluetoothManager btManager = mContext.getSystemService(BluetoothManager.class);
        if (btManager != null) {
            btManager.getAdapter().factoryReset();
        }

        Settings.Global.putInt(mResolver, Settings.Global.CDMA_CELL_BROADCAST_SMS,
                RILConstants.CDMA_CELL_BROADCAST_SMS_DISABLED);
        Settings.Global.putInt(mResolver, Settings.Global.DATA_ROAMING,
                Boolean.parseBoolean(SystemProperties.get("ro.com.android.dataroaming")) ? 1 : 0);
        Settings.Global.putString(mResolver, Settings.Global.DOWNLOAD_MAX_BYTES_OVER_MOBILE,
                Integer.toString(getInteger(
                        R.integer.def_download_manager_max_bytes_over_mobile)));
        Settings.Global.putString(mResolver,
                Settings.Global.DOWNLOAD_RECOMMENDED_MAX_BYTES_OVER_MOBILE,
                Integer.toString(getInteger(
                                R.integer.def_download_manager_recommended_max_bytes_over_mobile)
                ));
        Settings.Global.putInt(mResolver, Settings.Global.MOBILE_DATA, Boolean.parseBoolean(
                SystemProperties.get("ro.com.android.mobiledata",Boolean.TRUE.toString()))
                ? 1 : 0);

        Settings.Global.putInt(mResolver, Settings.Global.NETSTATS_ENABLED,
                getBoolean(R.bool.def_netstats_enabled) ? 1 : 0);

        Settings.Global.putInt(mResolver, Settings.Global.USB_MASS_STORAGE_ENABLED,
                getBoolean(R.bool.def_usb_mass_storage_enabled) ? 1 : 0);

        Settings.Global.putInt(mResolver, Settings.Global.WIFI_MAX_DHCP_RETRY_COUNT,
                getInteger(R.integer.def_max_dhcp_retries));

        Settings.Global.putInt(mResolver, Settings.Global.WIFI_DISPLAY_ON,
                getBoolean(R.bool.def_wifi_display_on) ? 1 : 0);

        Settings.Global.putString(mResolver, Settings.Global.LOCK_SOUND,
                mContext.getString(R.string.def_lock_sound));
        Settings.Global.putString(mResolver, Settings.Global.UNLOCK_SOUND,
                mContext.getString(R.string.def_unlock_sound));

        Settings.Global.putString(mResolver, Settings.Global.TRUSTED_SOUND,
                mContext.getString(R.string.def_trusted_sound));

        Settings.Global.putInt(mResolver, Settings.Global.POWER_SOUNDS_ENABLED,
                getInteger(R.integer.def_power_sounds_enabled));

        Settings.Global.putInt(mResolver, Settings.Global.LOW_BATTERY_SOUND_TIMEOUT,
                getInteger(R.integer.def_low_battery_sound_timeout));
        Settings.Global.putString(mResolver, Settings.Global.LOW_BATTERY_SOUND,
                mContext.getString(R.string.def_low_battery_sound));

        Settings.Global.putInt(mResolver, Settings.Global.DOCK_SOUNDS_ENABLED,
                getInteger(R.integer.def_dock_sounds_enabled));

        Settings.Global.putString(mResolver, Settings.Global.DESK_DOCK_SOUND,
                mContext.getString(R.string.def_desk_dock_sound));
        Settings.Global.putString(mResolver, Settings.Global.DESK_UNDOCK_SOUND,
                mContext.getString(R.string.def_desk_undock_sound));

        Settings.Global.putString(mResolver, Settings.Global.CAR_DOCK_SOUND,
                mContext.getString(R.string.def_car_dock_sound));
        Settings.Global.putString(mResolver, Settings.Global.CAR_UNDOCK_SOUND,
                mContext.getString(R.string.def_car_undock_sound));

        Settings.Global.putString(mResolver, Settings.Global.WIRELESS_CHARGING_STARTED_SOUND,
                mContext.getString(R.string.def_wireless_charging_started_sound));
        Settings.Global.putInt(mResolver, Settings.Global.DOCK_AUDIO_MEDIA_ENABLED,
                getInteger(R.integer.def_dock_audio_media_enabled));
        Settings.Global.putInt(mResolver, Settings.Global.SET_INSTALL_LOCATION, 0);
        Settings.Global.putInt(mResolver, Settings.Global.DEFAULT_INSTALL_LOCATION,
                PackageHelper.APP_INSTALL_AUTO);

        Settings.Global.putInt(mResolver, Settings.Global.EMERGENCY_TONE, 0);
        Settings.Global.putInt(mResolver, Settings.Global.CALL_AUTO_RETRY, 0);

        Settings.Global.putInt(mResolver, Settings.Global.PREFERRED_NETWORK_MODE,
                RILConstants.PREFERRED_NETWORK_MODE);
        Settings.Global.putInt(mResolver, Settings.Global.CDMA_SUBSCRIPTION_MODE,
                SystemProperties.getInt("ro.telephony.default_cdma_sub",
                        CdmaSubscriptionSourceManager.PREFERRED_CDMA_SUBSCRIPTION));

        Settings.Global.putInt(mResolver, Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE,
                getInteger(R.integer.def_wifi_scan_always_available));
        Settings.Global.putInt(mResolver, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED,
                getInteger(R.integer.def_heads_up_enabled));
        Settings.Global.putString(mResolver, Settings.Global.DEVICE_NAME,
                mContext.getString(R.string.def_device_name_simple, Build.MODEL));
        Settings.Global.putInt(mResolver, Settings.Global.ENHANCED_4G_MODE_ENABLED,
                ImsConfig.FeatureValueConstants.ON);
        Settings.Global.putInt(mResolver, Settings.Global.DOCK_POWER_SAVING,
                getInteger(R.integer.def_dock_battery_saving));
        Settings.Global.putInt(mResolver, Settings.Global.POWER_SAVER_ENABLED, 1);
    }

    private void loadDefaultAnimationSettings() {
        Settings.Global.putFloat(mResolver, Settings.Global.WINDOW_ANIMATION_SCALE,
                getFraction(R.fraction.def_window_animation_scale, 1, 1));
        Settings.Global.putFloat(mResolver, Settings.Global.TRANSITION_ANIMATION_SCALE,
                getFraction(R.fraction.def_window_transition_scale, 1, 1));
    }
}
