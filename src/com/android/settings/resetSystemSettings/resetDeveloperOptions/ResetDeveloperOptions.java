package com.android.settings.resetSystemSettings.resetDeveloperOptions;

import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.ListPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.ThreadedRenderer;
import android.view.IWindowManager;
import android.view.View;

import com.android.settings.DevelopmentSettings;
import com.android.internal.app.LocalePicker;
import com.android.settings.resetSystemSettings.ResetSettingsBaseClass;

import java.util.List;

/**
 * Created by JimCC
 *
 * TODO
 * Use ContentValues and bulkInsert()
 * Check mEnableTerminal, mEnableOemUnlock, and mEnableMultiWindow
 * Check mClearAdbKeys
 * Move default values to xml (?)
 */
public class ResetDeveloperOptions extends ResetSettingsBaseClass {
    private static final String TAG = "ResetDeveloperOptions";

    private static final int SHOW_UPDATES = 1;
    private static final int DISABLE_OVERLAYS = 2;

    private static final int FLINGER_CODE_SHOW_UPDATES = 1002;
    private static final int FLINGER_CODE_TOGGLE_USE_OF_HW_COMPOSER = 1008;
    private static final int FLINGER_CODE_INTERROGATE = 1010;

    private static final int FLINGER_REPLY_SHOW_UPDATES_ON = 1;
    private static final int FLINGER_REPLY_DISABLE_OVERLAYS_ON = 1;

    /**
     * Index for {@link #resetAnimationScaleOption()}
     */
    private static final int WINDOW_ANIMATION_SCALE = 0;
    private static final int TRANSITION_ANIMATION_SCALE = 1;
    private static final int ANIMATOR_DURATION_SCALE = 2;

    private static final String HDCP_CHECKING_PROPERTY = "persist.sys.hdcp_checking";
    private static final int[] MOCK_LOCATION_APP_OPS = new int[] {AppOpsManager.OP_MOCK_LOCATION};
    private static final String SELECT_LOGD_DEFAULT_SIZE_PROPERTY = "ro.logd.size";
    private static final String SELECT_LOGD_SIZE_PROPERTY = "persist.logd.size";
    private static final String MULTI_WINDOW_SYSTEM_PROPERTY = "persist.sys.debug.multi_window";
    private static final String HARDWARE_UI_PROPERTY = "persist.sys.ui.hw";
    private static final String MSAA_PROPERTY = "debug.egl.force_msaa";
    private static final String OPENGL_TRACES_PROPERTY = "debug.egl.trace";

    // +++
    // All default values
    //
    private static final int DEFAULT_DEVELOPMENT_SETTINGS_ENABLED = 1;

    // Others (Does not belong to any category)
    private static final String DEFAULT_LOCAL_BACKUP_PASSWORD = ""; // not not reset password
    private static final int DEFAULT_KEEP_SCREEN_ON = 0;
    private static final String DEFAULT_HDCP_CHECKING = "drm-only";
    private static final int DEFAULT_BT_HCI_SNOOP_LOG = 0;

    // Debugging
    private static final int DEFAULT_ENABLE_ADB = 0;
    private static final int DEFAULT_BUG_REPORT_IN_POWER = 0;
    private static final int DEFAULT_DEBUG_VIEW_ATTRIBUTES = 0;
    private static final int DEFAULT_VERIFY_APPS_OVER_USB = 0;
    private static final String DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES = "262144"; // 256K

    // Networking
    private static final int DEFAULT_WIFI_DISPLAY_CERTIFICATION = 0;
    private static final int DEFAULT_WIFI_VERBOSE_LOGGING = 0;
    private static final int DEFAULT_WIFI_AGGRESSIVE_HANDOVER = 0;
    private static final int DEFAULT_WIFI_ALLOW_SCAN_WITH_TRAFFIC = 0;
    private static final int DEFAULT_WIFI_LEGACY_DHCP_CLIENT = 0;
    private static final int DEFAULT_MOBILE_DATA_ALWAYS_ON = 0;
    private static final String DEFAULT_USB_CONFIGURATION_CURRENT_FUNCTION = null;
    private static final boolean DEFAULT_USB_CONFIGURATION_USB_DATA_UNLOCK = false;

    // Input
    private static final int DEFAULT_SHOW_TOUCHES = 0;
    private static final int DEFAULT_POINTER_LOCATION = 0;

    // Drawing
    private static final int DEFAULT_SHOW_SCREEN_UPDATES = 0;
    private static final String DEFAULT_DEBUG_LAYOUT = "false";
    private static final int DEFAULT_FORCE_RTL = 0;
    private static final float DEFAULT_WINDOW_ANIMATION_SCALE = 1;
    private static final float DEFAULT_TRANSITION_ANIMATION_SCALE = 1;
    private static final float DEFAULT_ANIMATOR_DURATION_SCALE = 1;
    private static final String DEFAULT_OVERLAY_DISPLAY_DEVICES = null;
    private static final String DEFAULT_ENABLE_MULTI_WINDOW = "false";

    // Hardware accelerated rendering
    private static final String DEFAULT_FORCE_HARDWARE_UI = "false";
    private static final String DEFAULT_SHOW_HW_SCREEN_UPDATES = null;
    private static final String DEFAULT_SHOW_HW_LAYERS_UPDATES = null;
    private static final String DEFAULT_DEBUG_HW_OVERDRAW = "";
    private static final String DEFAULT_SHOW_NON_RECTANGULAR_CLIP = "";
    private static final String DEFAULT_FORCE_MSAA = "false";
    private static final int DEFAULT_DISABLE_OVERLAYS = 0;
    private static final int DEFAULT_SIMULATE_COLOR_SPACE = 0;
    /**
     * Force Accessibility -> Color correction -> Correction mode
     * to display "Overridden by Simulated color space"
     * Check if 0 is the correct value
     * See {@link #resetSimulateColorSpace()} and SurfaceFlinger.cpp
     */
    private static final int SET_CORRECTION_MODE_OVERRIDDEN_BY_SIMULATE_COLOR_SPACE = 0;

    // Media
    private static final int DEFAULT_USB_AUDIO = 0;

    // Monitoring
    private static final String DEFAULT_STRICT_MODE = "";
    private static final int DEFAULT_SHOW_CPU_USAGE = 0;
    private static final String DEFAULT_TRACK_FRAME_TIME = "";
    private static final String DEFAULT_OPENGL_TRACES = "";

    // Apps
    private static final boolean DEFAULT_IMMEDIATELY_DESTROY_ACTIVITIES = false;
    private static final int DEFAULT_APP_PROCESS_LIMIT = -1; // Standard limit
    private static final int DEFAULT_SHOW_ALL_ANRS = 0;
    //
    // End of all default values
    // +++

    private WifiManager mWifiManager;
    private IWindowManager mWindowManager;

    public ResetDeveloperOptions(Context context) {
        super(context);
    }

    public void run() {
        Log.i(TAG, "run()");

        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));

        resetOtherSettings();

        // Debugging
        resetDebugging();

        // Networking
        resetNetworking();

        // Input
        resetInput();

        // Drawing
        resetDrawing();

        // Hardware accelerated rendering
        resetHardwareAcceleratedRendering();

        // Media
        resetMedia();

        // Monitoring
        resetMonitoring();

        // Apps
        resetApps();

        // For all calls of SystemProperties.set(...)
        pokeSystemProperties();

        // Enable development settings
        // But the developer options will be hidden in the next step
        Settings.Global.putInt(mResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                DEFAULT_DEVELOPMENT_SETTINGS_ENABLED);

        // Hide developer options
        mContext.getSharedPreferences(DevelopmentSettings.PREF_FILE, Context.MODE_PRIVATE)
                .edit().putBoolean(DevelopmentSettings.PREF_SHOW, false).commit();
    }

    private void resetOtherSettings() {
        resetDesktopBackupPassword();
        resetStayAwakeOptions();
        resetHDCPCheckingOptions();
        resetBtHciSnoopLogOptions();
    }

    private void resetDebugging() {
        resetUsbDebuggingOptions();
        resetBugReportShortcutOptions();
        resetMockLocation();
        resetViewAttributeInspectionOptions();
        resetDebuggerOptions();
        resetVerifyAppsOverUsbOptions();
        resetLogdSizeOption();
    }

    private void resetNetworking() {
        resetWifiDisplayCertificationOptions();
        resetWifiVerboseLoggingOptions();
        resetWifiAggressiveHandoverOptions();
        resetWifiAllowScansWithTrafficOptions();
//        resetLegacyDhcpClientOptions();
        resetMobileDataAlwaysOnOptions();
        resetUsbConfigurationOption();
    }

    /**
     * Do not reset it (do nothing now)
     */
    private void resetDesktopBackupPassword() {
        //IBackupManager backupManager = IBackupManager.Stub.asInterface(
        //        ServiceManager.getService(Context.BACKUP_SERVICE));
    }

    private void resetStayAwakeOptions() {
        Settings.Global.putInt(mResolver,
                Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                DEFAULT_KEEP_SCREEN_ON);
    }

    /**
     * Check
     * {@link com.android.settings.DevelopmentSettings#updateHdcpValues()}
     * for default value
     * Also use: adb shell getprop
     * Check the value of persist.sys.hdcp_checking
     */
    private void resetHDCPCheckingOptions() {
        SystemProperties.set(HDCP_CHECKING_PROPERTY, DEFAULT_HDCP_CHECKING);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeBtHciSnoopLogOptions()}
     */
    private void resetBtHciSnoopLogOptions() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        adapter.configHciSnoopLog(1 == DEFAULT_BT_HCI_SNOOP_LOG);
        Settings.Secure.putInt(mResolver,
                Settings.Secure.BLUETOOTH_HCI_LOG,
                DEFAULT_BT_HCI_SNOOP_LOG);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeUsbConfigurationOption(
     * Object newValue)}
     *
     * Also check the hide functions
     * {@link android.hardware.usb.UsbManager#setCurrentFunction(String function)}
     * {@link android.hardware.usb.UsbManager#setUsbDataUnlocked(boolean unlocked)}
     *
     * Use null for default function and false to avoid leaking sensitive user information
     * Currently, default is MTP
     * {@link android.hardware.usb.UsbManager#USB_FUNCTION_MTP}
     */
    private void resetUsbConfigurationOption() {
        UsbManager manager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        manager.setCurrentFunction(DEFAULT_USB_CONFIGURATION_CURRENT_FUNCTION);
        manager.setUsbDataUnlocked(DEFAULT_USB_CONFIGURATION_USB_DATA_UNLOCK);
    }

    private void resetInput() {
        resetShowTouchesOptions();
        resetPointerLocationOptions();
    }

    private void resetDrawing() {
        resetShowUpdatesOption();
        resetDebugLayoutOptions();
        resetForceRtlOptions();
        resetAnimationScaleOption();
        resetOverlayDisplayDevicesOptions();
        resetEnableMultiWindow();
    }

    private void resetHardwareAcceleratedRendering() {
        resetHardwareUiOptions();
        resetShowHwScreenUpdatesOptions();
        resetShowHwLayersUpdatesOptions();
        resetDebugHwOverdrawOptions();
        resetShowNonRectClipOptions();
        resetMsaaOptions();
        resetDisableOverlaysOption();
        resetSimulateColorSpace();
    }

    private void resetMedia() {
        resetUsbAudioOptions();
    }

    private void resetMonitoring() {
        resetStrictModeVisualOptions();
        resetCpuUsageOptions();
        resetTrackFrameTimeOptions();
        resetOpenGLTracesOptions();
    }

    private void resetApps() {
        resetImmediatelyDestroyActivitiesOptions();
        resetAppProcessLimitOptions();
        resetShowAllANRsOptions();
    }

    /**
     * USB debugging
     */
    private void resetUsbDebuggingOptions() {
        Settings.Global.putInt(mResolver,
                Settings.Global.ADB_ENABLED, DEFAULT_ENABLE_ADB);
    }

    /**
     * Bug report shortcut
     */
    private void resetBugReportShortcutOptions() {
        Settings.Secure.putInt(mResolver,
                Settings.Secure.BUGREPORT_IN_POWER_MENU, DEFAULT_BUG_REPORT_IN_POWER);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeMockLocation()}
     */
    private void resetMockLocation() {
        AppOpsManager appOpsManager =
                (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);

        // Disable the app op of the previous mock location app if such.
        List<AppOpsManager.PackageOps> packageOps =
                appOpsManager.getPackagesForOps(MOCK_LOCATION_APP_OPS);

        if (null == packageOps) return;

        // Should be one but in case we are in a bad state due to use of command line tools.
        for (AppOpsManager.PackageOps packageOp : packageOps) {
            if (AppOpsManager.MODE_ERRORED == packageOp.getOps().get(0).getMode()) continue;

            String oldMockLocationApp = packageOp.getPackageName();
            try {
                ApplicationInfo ai = mContext.getPackageManager().getApplicationInfo(
                        oldMockLocationApp, PackageManager.GET_DISABLED_COMPONENTS);
                appOpsManager.setMode(AppOpsManager.OP_MOCK_LOCATION, ai.uid,
                        oldMockLocationApp, AppOpsManager.MODE_ERRORED);
            } catch (PackageManager.NameNotFoundException e) {
                        /* ignore */
            }
        }
    }

    /**
     * Enable view attribute inspection
     * This will cause UI turning to black for a short time
     */
    private void resetViewAttributeInspectionOptions() {
        Settings.Global.putInt(mResolver,
                Settings.Global.DEBUG_VIEW_ATTRIBUTES, DEFAULT_DEBUG_VIEW_ATTRIBUTES);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#resetDebuggerOptions()}
     *
     * Set no default debug app and do not wait for debugger
     *
     * TODO
     * Check if use static
     */
    private void resetDebuggerOptions() {
        try {
            ActivityManagerNative.getDefault().setDebugApp(
                    null, false, true);
        } catch (RemoteException ex) {
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeVerifyAppsOverUsbOptions()}
     */
    private void resetVerifyAppsOverUsbOptions() {
        Settings.Global.putInt(mResolver,
                Settings.Global.PACKAGE_VERIFIER_INCLUDE_ADB, DEFAULT_VERIFY_APPS_OVER_USB);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeLogdSizeOption(Object newValue)}
     */
    private void resetLogdSizeOption() {
        String currentValue = SystemProperties.get(SELECT_LOGD_DEFAULT_SIZE_PROPERTY);
        if (null == currentValue) {
            currentValue = DEFAULT_LOG_RING_BUFFER_SIZE_IN_BYTES;
        }
        SystemProperties.set(SELECT_LOGD_SIZE_PROPERTY, currentValue);
        //pokeSystemProperties();
        try {
            Process p = Runtime.getRuntime().exec(
                    "logcat -b all -G " + currentValue);
            p.waitFor();
            Log.i(TAG, "Logcat ring buffer sizes set to: " +
                    currentValue);
        } catch (Exception e) {
            Log.w(TAG, "Cannot set logcat ring buffer sizes", e);
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeWifiDisplayCertificationOptions()}
     */
    private void resetWifiDisplayCertificationOptions() {
        Settings.Global.putInt(mResolver,
                Settings.Global.WIFI_DISPLAY_CERTIFICATION_ON,
                DEFAULT_WIFI_DISPLAY_CERTIFICATION);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeWifiVerboseLoggingOptions()}
     */
    private void resetWifiVerboseLoggingOptions() {
        mWifiManager.enableVerboseLogging(DEFAULT_WIFI_VERBOSE_LOGGING);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeWifiAggressiveHandoverOptions()}
     */
    private void resetWifiAggressiveHandoverOptions() {
        mWifiManager.enableAggressiveHandover(DEFAULT_WIFI_AGGRESSIVE_HANDOVER);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeWifiAllowScansWithTrafficOptions()}
     */
    private void resetWifiAllowScansWithTrafficOptions() {
        mWifiManager.setAllowScansWithTraffic(DEFAULT_WIFI_ALLOW_SCAN_WITH_TRAFFIC);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeLegacyDhcpClientOptions()}
     */
//    private void resetLegacyDhcpClientOptions() {
//        Settings.Global.putInt(mResolver,
//                Settings.Global.LEGACY_DHCP_CLIENT, DEFAULT_WIFI_LEGACY_DHCP_CLIENT);
//    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeMobileDataAlwaysOnOptions()}
     */
    private void resetMobileDataAlwaysOnOptions() {
        Settings.Global.putInt(mResolver,
                Settings.Global.MOBILE_DATA_ALWAYS_ON, DEFAULT_MOBILE_DATA_ALWAYS_ON);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeShowTouchesOptions()}
     */
    private void resetShowTouchesOptions() {
        Settings.System.putInt(mResolver,
                Settings.System.SHOW_TOUCHES, DEFAULT_SHOW_TOUCHES);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writePointerLocationOptions()}
     */
    private void resetPointerLocationOptions() {
        Settings.System.putInt(mResolver,
                Settings.System.POINTER_LOCATION, DEFAULT_POINTER_LOCATION);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeShowUpdatesOption()}
     *
     * Show surface updates
     *
     * Check
     * frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
     *
     * Code 1002: SHOW_UPDATES
     *
     * Care the implementation of SurfaceFlinger.cpp
     * For code 1002, "on" will be set if the input is 1
     * However, if the input is 0, "on" / "off" will be set to "off" / "on"
     */
    private void resetShowUpdatesOption() {
        if (!getFlingerOptions(SHOW_UPDATES)) return;

        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (null != flinger) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(DEFAULT_SHOW_SCREEN_UPDATES);
                flinger.transact(FLINGER_CODE_SHOW_UPDATES, data, null, 0);
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#updateFlingerOptions()}
     *
     * Check
     * frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
     *
     * Code 1010: interrogate
     * Return true to indicated that the value is set "on"
     * This function is especially needed by resetShowUpdatesOption()
     */
    private boolean getFlingerOptions(int target) {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (null != flinger) {
                Parcel data = Parcel.obtain();
                Parcel reply = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                flinger.transact(FLINGER_CODE_INTERROGATE, data, reply, 0);
                @SuppressWarnings("unused")
                int showCpu = reply.readInt();
                @SuppressWarnings("unused")
                int enableGL = reply.readInt();
                int showUpdates = reply.readInt();
                @SuppressWarnings("unused")
                int showBackground = reply.readInt();
                int disableOverlays = reply.readInt();
                reply.recycle();
                data.recycle();

                // Check target option
                switch (target) {
                    case SHOW_UPDATES:
                        return FLINGER_REPLY_SHOW_UPDATES_ON == showUpdates;
                    case DISABLE_OVERLAYS:
                        return FLINGER_REPLY_DISABLE_OVERLAYS_ON == disableOverlays;
                    default:
                        return false;
                }
            }
        } catch (RemoteException ex) {
        }
        return false;
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeDebugLayoutOptions()}
     */
    private void resetDebugLayoutOptions() {
        SystemProperties.set(View.DEBUG_LAYOUT_PROPERTY, DEFAULT_DEBUG_LAYOUT);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeForceRtlOptions()}
     */
    private void resetForceRtlOptions() {
        Settings.Global.putInt(mResolver,
                Settings.Global.DEVELOPMENT_FORCE_RTL, DEFAULT_FORCE_RTL);
        SystemProperties.set(Settings.Global.DEVELOPMENT_FORCE_RTL,
                Integer.valueOf(DEFAULT_FORCE_RTL).toString());
        LocalePicker.updateLocale(mContext.getResources().getConfiguration().locale);
    }

    /**
     * Check code for reference
     * {@link com.android.settings.DevelopmentSettings#resetDangerousOptions()}
     */
    private void resetAnimationScaleOption() {
        resetAnimationScaleOption(
                WINDOW_ANIMATION_SCALE, DEFAULT_WINDOW_ANIMATION_SCALE);
        resetAnimationScaleOption(
                TRANSITION_ANIMATION_SCALE, DEFAULT_TRANSITION_ANIMATION_SCALE);
        resetAnimationScaleOption(
                ANIMATOR_DURATION_SCALE, DEFAULT_ANIMATOR_DURATION_SCALE);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeAnimationScaleOption(
     * int which, ListPreference pref, Object newValue)}
     */
    private void resetAnimationScaleOption(int which, float defaultValue) {
        try {
            mWindowManager.setAnimationScale(which, defaultValue);
        } catch (RemoteException e) {
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#pokeSystemProperties()}
     * {@link com.android.settings.DevelopmentSettings.SystemPropPoker}
     *
     * For all calls of
     * {@link android.os.SystemProperties#set(String key, String val)}
     *
     * Because reset system settings is called using intent service,
     * this function should not be needed to use AsyncTask
     */
    public void pokeSystemProperties() {
        String[] services;
        services = ServiceManager.listServices();
        if (null == services) return ;
        for (String service : services) {
            IBinder obj = ServiceManager.checkService(service);
            if (null != obj) {
                Parcel data = Parcel.obtain();
                try {
                    obj.transact(IBinder.SYSPROPS_TRANSACTION, data, null, 0);
                } catch (RemoteException e) {
                } catch (Exception e) {
                    Log.i(TAG, "Someone wrote a bad service '" + service
                            + "' that doesn't like to be poked: " + e);
                }
                data.recycle();
            }
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeOverlayDisplayDevicesOptions(
     * Object newValue)}
     *
     * Simulate secondary displays
     */
    private void resetOverlayDisplayDevicesOptions() {
        Settings.Global.putString(mResolver,
                Settings.Global.OVERLAY_DISPLAY_DEVICES, DEFAULT_OVERLAY_DISPLAY_DEVICES);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#setEnableMultiWindow(boolean value)}
     */
    private void resetEnableMultiWindow() {
        SystemProperties.set(MULTI_WINDOW_SYSTEM_PROPERTY, DEFAULT_ENABLE_MULTI_WINDOW);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeHardwareUiOptions()}
     *
     * Force GPU rendering
     */
    private void resetHardwareUiOptions() {
        SystemProperties.set(HARDWARE_UI_PROPERTY, DEFAULT_FORCE_HARDWARE_UI);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeShowHwScreenUpdatesOptions()}
     *
     * Show GPU view updates
     */
    private void resetShowHwScreenUpdatesOptions() {
        SystemProperties.set(ThreadedRenderer.DEBUG_DIRTY_REGIONS_PROPERTY,
                DEFAULT_SHOW_HW_SCREEN_UPDATES);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeShowHwLayersUpdatesOptions()}
     *
     * Show GPU view updates
     */
    private void resetShowHwLayersUpdatesOptions() {
        SystemProperties.set(ThreadedRenderer.DEBUG_SHOW_LAYERS_UPDATES_PROPERTY,
                DEFAULT_SHOW_HW_LAYERS_UPDATES);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeDebugHwOverdrawOptions(
     * Object newValue)}
     *
     * Debug GPU view overdraw
     */
    private void resetDebugHwOverdrawOptions() {
        SystemProperties.set(ThreadedRenderer.DEBUG_OVERDRAW_PROPERTY,
                DEFAULT_DEBUG_HW_OVERDRAW);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeShowNonRectClipOptions(
     * Object newValue)}
     *
     * Debug non-rectangular clip operations
     */
    private void resetShowNonRectClipOptions() {
        SystemProperties.set(ThreadedRenderer.DEBUG_SHOW_NON_RECTANGULAR_CLIP_PROPERTY,
                DEFAULT_SHOW_NON_RECTANGULAR_CLIP);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeMsaaOptions()}
     */
    private void resetMsaaOptions() {
        SystemProperties.set(MSAA_PROPERTY, DEFAULT_FORCE_MSAA);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeDisableOverlaysOption()}
     *
     * Disable HW overlays
     */
    private void resetDisableOverlaysOption() {
        try {
            IBinder flinger = ServiceManager.getService("SurfaceFlinger");
            if (null != flinger) {
                Parcel data = Parcel.obtain();
                data.writeInterfaceToken("android.ui.ISurfaceComposer");
                data.writeInt(DEFAULT_DISABLE_OVERLAYS);
                flinger.transact(FLINGER_CODE_TOGGLE_USE_OF_HW_COMPOSER, data, null, 0);
                data.recycle();
            }
        } catch (RemoteException ex) {
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeSimulateColorSpace(Object value)}
     *
     * This value will be overridden by Accessibility
     * However, for resetting the settings, we can just reset it to Disabled
     * We should reset it to a particular value only if the default value is not Disabled
     *
     * Ensure Accessibility -> Color correction -> Correction mode
     * displays: "Overridden by Simulated color space"
     *
     * This function also resets Accessibility -> Color correction
     *
     * Check
     * frameworks/native/services/surfaceflinger/SurfaceFlinger.cpp
     *
     * Code 1014: daltonize
     * if n < 10:
     * mDaltonizer.setMode(Daltonizer::simulation); // simulation
     * mDaltonize = n > 0; // off (?)
     *
     * TODO
     * Check the default behavior
     */
    private void resetSimulateColorSpace() {
        Settings.Secure.putInt(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                DEFAULT_SIMULATE_COLOR_SPACE);
        Settings.Secure.putInt(mResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER,
                SET_CORRECTION_MODE_OVERRIDDEN_BY_SIMULATE_COLOR_SPACE);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeUSBAudioOptions()}
     *
     * Disable USB audio routing
     */
    private void resetUsbAudioOptions() {
        Settings.Secure.putInt(mResolver,
                Settings.Secure.USB_AUDIO_AUTOMATIC_ROUTING_DISABLED,
                DEFAULT_USB_AUDIO);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeStrictModeVisualOptions()}
     */
    private void resetStrictModeVisualOptions() {
        try {
            mWindowManager.setStrictModeVisualIndicatorPreference(DEFAULT_STRICT_MODE);
        } catch (RemoteException e) {
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeCpuUsageOptions()}
     *
     * DEFAULT_SHOW_CPU_USAGE is 0 and we will always stop the service
     */
    private void resetCpuUsageOptions() {
        Settings.Global.putInt(mResolver,
                Settings.Global.SHOW_PROCESSES, DEFAULT_SHOW_CPU_USAGE);
        Intent service = (new Intent())
                .setClassName("com.android.systemui", "com.android.systemui.LoadAverageService");
        mContext.stopService(service);
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeTrackFrameTimeOptions(Object newValue)}
     *
     * Profile GPU rendering
     */
    private void resetTrackFrameTimeOptions() {
        SystemProperties.set(ThreadedRenderer.PROFILE_PROPERTY, DEFAULT_TRACK_FRAME_TIME);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeOpenGLTracesOptions(Object newValue)}
     */
    private void resetOpenGLTracesOptions() {
        SystemProperties.set(OPENGL_TRACES_PROPERTY, DEFAULT_OPENGL_TRACES);
        //pokeSystemProperties();
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeImmediatelyDestroyActivitiesOptions()}
     *
     * This function should change Settings.Global.ALWAYS_FINISH_ACTIVITIES
     */
    private void resetImmediatelyDestroyActivitiesOptions() {
        try {
            ActivityManagerNative.getDefault().setAlwaysFinish(
                    DEFAULT_IMMEDIATELY_DESTROY_ACTIVITIES);
        } catch (RemoteException ex) {
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeAppProcessLimitOptions(
     * Object newValue)}
     */
    private void resetAppProcessLimitOptions() {
        try {
            ActivityManagerNative.getDefault().setProcessLimit(DEFAULT_APP_PROCESS_LIMIT);
        } catch (RemoteException e) {
        }
    }

    /**
     * Reference
     * {@link com.android.settings.DevelopmentSettings#writeShowAllANRsOptions()}
     */
    private void resetShowAllANRsOptions() {
        Settings.Secure.putInt(mResolver,
                Settings.Secure.ANR_SHOW_BACKGROUND, DEFAULT_SHOW_ALL_ANRS);
    }
}
