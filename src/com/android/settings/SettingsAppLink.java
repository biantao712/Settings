package com.android.settings;

import android.content.ComponentName;
import android.content.Intent;

/**
 * chrisit_chang App link
 * parsing the input string and determine which activity is expected
 * returning the intent of the required format
 */
public class SettingsAppLink {

    private static final String SETTINGS_PACKAGE = "com.android.settings";

    //the table of all supplied shortcuts used by app link
    //example: asussettings://shortcut/wireless
    private static final String APP_LINK_WIRELESS = "wireless";
    private static final String APP_LINK_MOBILE_NETWORK = "mobile_network";
    private static final String APP_LINK_TETHER = "tether";
    private static final String APP_LINK_DISPLAY = "display";
    private static final String APP_LINK_SOUND = "sound";
    private static final String APP_LINK_INTERNAL_STORAGE = "internal_storage";
    private static final String APP_LINK_APPS = "apps";
    private static final String APP_LINK_ACCOUNTS = "accounts";
    private static final String APP_LINK_SYSTEM_UPDATE = "system_update";

    public static Intent getIntent(String linkName) {

        Intent finalIntent = new Intent();

        switch (linkName) {
            case APP_LINK_APPS:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        SETTINGS_PACKAGE, "Settings$ManageApplicationsActivity"));
                break;
            case APP_LINK_SYSTEM_UPDATE:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        "com.asus.systemupdate", "SystemUpdateActivity"));
                break;
            case APP_LINK_SOUND:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        SETTINGS_PACKAGE, "Settings$SoundSettingsActivity"));
                break;
            case APP_LINK_INTERNAL_STORAGE:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        SETTINGS_PACKAGE, "Settings$StorageSettingsActivity"));
                break;
            case APP_LINK_ACCOUNTS:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        SETTINGS_PACKAGE, "Settings$AccountSettingsActivity"));
                break;
            case APP_LINK_DISPLAY:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        SETTINGS_PACKAGE, "Settings$DisplaySettingsActivity"));
                break;
            case APP_LINK_TETHER:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        SETTINGS_PACKAGE, "TetherSettings"));
                break;
            case APP_LINK_WIRELESS:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        SETTINGS_PACKAGE, "Settings$WirelessSettingsActivity"));
                break;
            case APP_LINK_MOBILE_NETWORK:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        "com.android.phone", "MobileNetworkSettings"));
                break;
            default:
                finalIntent.setComponent(getComponentFromPackageAndClass(
                        SETTINGS_PACKAGE, "Settings"));
                break;
        }
        return finalIntent;
    }

    private static ComponentName getComponentFromPackageAndClass(String packageName
            , String className) {
        return new ComponentName(packageName, packageName + "." + className);
    }
}
