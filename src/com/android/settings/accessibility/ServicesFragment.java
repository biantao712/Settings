package com.android.settings.accessibility;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import com.android.internal.content.PackageMonitor;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;

import java.util.List;
import java.util.Set;

import static com.android.settings.accessibility.AccessibilitySettings.EXTRA_CHECKED;
import static com.android.settings.accessibility.AccessibilitySettings.EXTRA_COMPONENT_NAME;
import static com.android.settings.accessibility.AccessibilitySettings.EXTRA_PREFERENCE_KEY;
import static com.android.settings.accessibility.AccessibilitySettings.EXTRA_SETTINGS_COMPONENT_NAME;
import static com.android.settings.accessibility.AccessibilitySettings.EXTRA_SETTINGS_TITLE;
import static com.android.settings.accessibility.AccessibilitySettings.EXTRA_SUMMARY;
import static com.android.settings.accessibility.AccessibilitySettings.EXTRA_TITLE;

/**
 * Settings fragment containing services
 */

public class ServicesFragment extends SettingsPreferenceFragment {

    private static final String SERVICES_CATEGORY = "services_category";
    private PreferenceCategory mServicesCategory;
    private DevicePolicyManager mDpm;
    private Preference mNoServicesMessagePreference;
    private final Handler mHandler = new Handler();
	private static final long DELAY_UPDATE_SERVICES_MILLIS = 1000;

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null) {
                updateServicesPreferences();
            }
        }
    };

    private final PackageMonitor mSettingsPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            mHandler.postDelayed(mUpdateRunnable, DELAY_UPDATE_SERVICES_MILLIS);
        }
    };

    @Override
    protected int getMetricsCategory(){
        return MetricsEvent.SUW_ACCESSIBILITY;
    }

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_services);
        mServicesCategory = (PreferenceCategory) findPreference(SERVICES_CATEGORY);
        mDpm = (DevicePolicyManager) (getActivity()
                .getSystemService(Context.DEVICE_POLICY_SERVICE));
    }

    @Override
    public void onResume() {
        super.onResume();
        updateServicesPreferences();
        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        super.onPause();
    }

    private void updateServicesPreferences() {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go and then based on
        // the global accessibility state to decided whether it is enabled.

        // Generate.
        mServicesCategory.removeAll();

        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getActivity());

        List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(
                getActivity());
        List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        final boolean accessibilityEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1;

        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            AccessibilityServiceInfo info = installedServices.get(i);

            RestrictedPreference preference = new RestrictedPreference(getActivity());
            preference.setLayoutResource(R.layout.cnasusres_preference_parent);
            String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();

            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            ComponentName componentName = new ComponentName(serviceInfo.packageName,
                    serviceInfo.name);

            preference.setKey(componentName.flattenToString());

            preference.setTitle(title);
            final boolean serviceEnabled = accessibilityEnabled
                    && enabledServices.contains(componentName);
            String serviceEnabledString;
            if (serviceEnabled) {
                serviceEnabledString = getString(R.string.accessibility_feature_state_on);
            } else {
                serviceEnabledString = getString(R.string.accessibility_feature_state_off);
            }

            // Disable all accessibility services that are not permitted.
            String packageName = serviceInfo.packageName;
            boolean serviceAllowed =
                    permittedServices == null || permittedServices.contains(packageName);
            if (!serviceAllowed && !serviceEnabled) {
                EnforcedAdmin admin = RestrictedLockUtils.checkIfAccessibilityServiceDisallowed(
                        getActivity(), serviceInfo.packageName, UserHandle.myUserId());
                if (admin != null) {
                    preference.setDisabledByAdmin(admin);
                } else {
                    preference.setEnabled(false);
                }
            } else {
                preference.setEnabled(true);
            }

            preference.setSummary(serviceEnabledString);

            preference.setOrder(i);
            preference.setFragment(ToggleAccessibilityServicePreferenceFragment.class.getName());
            preference.setPersistent(true);

            Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, preference.getKey());
            extras.putBoolean(EXTRA_CHECKED, serviceEnabled);
            extras.putString(EXTRA_TITLE, title);

            String description = info.loadDescription(getPackageManager());
            if (TextUtils.isEmpty(description)) {
                description = getString(R.string.accessibility_service_default_description);
            }
            extras.putString(EXTRA_SUMMARY, description);

            String settingsClassName = info.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        getString(R.string.accessibility_menu_item_settings));
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(info.getResolveInfo().serviceInfo.packageName,
                                settingsClassName).flattenToString());
            }

            extras.putParcelable(EXTRA_COMPONENT_NAME, componentName);

            mServicesCategory.addPreference(preference);
        }

        if (mServicesCategory.getPreferenceCount() == 0) {
            if (mNoServicesMessagePreference == null) {
                mNoServicesMessagePreference = new Preference(getPrefContext());
                mNoServicesMessagePreference.setPersistent(false);
                mNoServicesMessagePreference.setLayoutResource(
                        R.layout.text_description_preference);
                mNoServicesMessagePreference.setSelectable(false);
                mNoServicesMessagePreference.setSummary(
                        getString(R.string.accessibility_no_services_installed));
            }
            mServicesCategory.addPreference(mNoServicesMessagePreference);
        }
    }
}
