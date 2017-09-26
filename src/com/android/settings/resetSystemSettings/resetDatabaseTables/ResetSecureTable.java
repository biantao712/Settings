package com.android.settings.resetSystemSettings.resetDatabaseTables;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.resetSystemSettings.ResetSettingsBaseClass;

/**
 * Created by JimCC
 */
public class ResetSecureTable extends ResetSettingsBaseClass {
    private static final String TAG = "ResetSecureTable";

    public ResetSecureTable(Context context) {
        super(context);
    }

    public void run() {
        Log.i(TAG, "run()");
        loadSecureSettings();
    }

    /**
     * Reference
     * {@link com.android.providers.settings.DatabaseHelper#loadSecureSettings(
     * SQLiteDatabase db)}
     */
    private void loadSecureSettings() {
        // ?? empty string in db-backup, skip now ?
        // <!-- Comma-separated list of location providers.
        //      Network location is off by default because it requires
        //      user opt-in via Setup Wizard or Settings.
        // -->
        // <string name="def_location_providers_allowed" translatable="false">gps</string>
        /*
        Settings.Secure.putString(mResolver, Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                res.getString(R.string.def_location_providers_allowed));
        */
        // This constant was deprecated in API level 19.
        // use LOCATION_MODE and MODE_CHANGED_ACTION (or PROVIDERS_CHANGED_ACTION)
        // can not find the option / effect

        // no such field in db-backup now
        String wifiWatchList = SystemProperties.get("ro.com.android.wifi-watchlist");
        if (!TextUtils.isEmpty(wifiWatchList)) {
            Settings.Secure.putString(mResolver, Settings.Secure.WIFI_WATCHDOG_WATCH_LIST,
                    wifiWatchList);
        }

        // Skip Settings.Secure.ADB_ENABLED because SystemServer will initialize ADB_ENABLED from
        // a persistent system property instead.
        /*
        Settings.Secure.putInt(mResolver, Settings.Secure.ADB_ENABLED, 0);
        */
        Settings.Secure.putInt(mResolver, Settings.Secure.ALLOW_MOCK_LOCATION,
                "1".equals(SystemProperties.get("ro.allow.mock.location")) ? 1 : 0);
        /**
         * Do NOT reset BACKUP_ENABLED & BACKUP_TRANSPORT in "Backup & Reset",
         * because of VZ_REQ_SOFTRESETMODE_32364
         * {@link com.android.providers.settings.DatabaseHelper#loadSecure35Settings(
         * SQLiteStatement stmt)}
         */
        Settings.Secure.putInt(mResolver, Settings.Secure.MOUNT_PLAY_NOTIFICATION_SND,
                getInteger(R.bool.def_mount_play_notification_snd));
        Settings.Secure.putInt(mResolver, Settings.Secure.MOUNT_UMS_AUTOSTART,
                getInteger(R.bool.def_mount_ums_autostart));
        Settings.Secure.putInt(mResolver, Settings.Secure.MOUNT_UMS_PROMPT,
                getInteger(R.bool.def_mount_ums_prompt));
        Settings.Secure.putInt(mResolver, Settings.Secure.MOUNT_UMS_NOTIFY_ENABLED,
                getInteger(R.bool.def_mount_ums_notify_enabled));
        Settings.Secure.putInt(mResolver, Settings.Secure.ACCESSIBILITY_SCRIPT_INJECTION,
                getInteger(R.bool.def_accessibility_script_injection));
        Settings.Secure.putString(mResolver,
                Settings.Secure.ACCESSIBILITY_WEB_CONTENT_KEY_BINDINGS,
                mContext.getString(R.string.def_accessibility_web_content_key_bindings));
        Settings.Secure.putInt(mResolver, Settings.Secure.LONG_PRESS_TIMEOUT,
                getInteger(R.integer.def_long_press_timeout_millis));
        Settings.Secure.putInt(mResolver, Settings.Secure.TOUCH_EXPLORATION_ENABLED,
                getInteger(R.bool.def_touch_exploration_enabled));
        Settings.Secure.putInt(mResolver, Settings.Secure.ACCESSIBILITY_SPEAK_PASSWORD,
                getInteger(R.bool.def_accessibility_speak_password));
        Settings.Secure.putString(mResolver, Settings.Secure.ACCESSIBILITY_SCREEN_READER_URL,
                mContext.getString(R.string.def_accessibility_screen_reader_url));
        final int lockscreenDisabled = getBoolean(R.bool.def_lockscreen_disabled) ||
                SystemProperties.getBoolean("ro.lockscreen.disable.default", false) ? 1 : 0;
        Settings.System.putInt(mResolver, Settings.System.LOCKSCREEN_DISABLED, lockscreenDisabled);
        Settings.Secure.putInt(mResolver, Settings.Secure.SCREENSAVER_ENABLED,
                getBoolean(com.android.internal.R.bool.config_dreamsEnabledByDefault) ? 1 : 0);
        Settings.Secure.putInt(mResolver, Settings.Secure.SCREENSAVER_ACTIVATE_ON_DOCK,
                getBoolean(com.android.internal.R.bool.config_dreamsActivatedOnDockByDefault)
                        ? 1 : 0);
        Settings.Secure.putInt(mResolver, Settings.Secure.SCREENSAVER_ACTIVATE_ON_SLEEP,
                getBoolean(com.android.internal.R.bool.config_dreamsActivatedOnSleepByDefault)
                        ? 1 : 0);
        Settings.Secure.putString(mResolver, Settings.Secure.SCREENSAVER_COMPONENTS,
                mContext.getString(com.android.internal.R.string.config_dreamsDefaultComponent));
        Settings.Secure.putString(mResolver, Settings.Secure.SCREENSAVER_DEFAULT_COMPONENT,
                mContext.getString(com.android.internal.R.string.config_dreamsDefaultComponent));
        Settings.Secure.putInt(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                getBoolean(R.bool.def_accessibility_display_magnification_enabled) ? 1 : 0);
        Settings.Secure.putFloat(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_SCALE,
                getFraction(R.fraction.def_accessibility_display_magnification_scale, 1, 1));
        Settings.Secure.putInt(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_AUTO_UPDATE,
                getBoolean(R.bool.def_accessibility_display_magnification_auto_update) ? 1 : 0);

        // Do NOT reset whether the current user has been set up via setup wizard
        /*
        Settings.Secure.putInt(mResolver, Settings.Secure.USER_SETUP_COMPLETE,
                getBoolean(R.bool.def_user_setup_complete) ? 1 : 0);
        */
        Settings.Secure.putString(mResolver, Settings.Secure.IMMERSIVE_MODE_CONFIRMATIONS,
                mContext.getString(R.string.def_immersive_mode_confirmations));

        // Do NOT reset Unknown sources because of VZ_REQ_SOFTRESETMODE_32364/32374
        /*
        Settings.Secure.putInt(mResolver, Settings.Secure.INSTALL_NON_MARKET_APPS,
                getBoolean(R.bool.def_install_non_market_apps) ? 1 : 0);
        */
        Settings.Secure.putInt(mResolver, Settings.Secure.WAKE_GESTURE_ENABLED,
                getBoolean(R.bool.def_wake_gesture_enabled) ? 1 : 0);

        // Sound & notification -> Notification -> When device is locked
        // 0,  true: Don't show notifications at all
        // 0, false: Don't show notifications at all
        // 1,  true: Show all notification content
        // 1, false: Hide sensitive notification content
        Settings.Secure.putInt(mResolver, Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                getInteger(R.integer.def_lock_screen_show_notifications));
        Settings.Secure.putInt(mResolver, Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                getBoolean(R.bool.def_lock_screen_allow_private_notifications) ? 1 : 0);

        Settings.Secure.putInt(mResolver, Settings.Secure.SLEEP_TIMEOUT,
                getInteger(R.integer.def_sleep_timeout));

        Settings.Secure.putInt(mResolver, Settings.Secure.ASUS_LOCKSCREEN_DISPLAY_APP, 1);
    }
}
