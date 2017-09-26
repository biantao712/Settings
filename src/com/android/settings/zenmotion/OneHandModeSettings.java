
package com.android.settings.zenmotion;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.util.ResCustomizeConfig;

public class OneHandModeSettings extends SettingsPreferenceFragment {
    private static final String TAG = "OneHandModeSettings";

    // BEGIN leo_liao@asus.com, One-hand control
    private static final String KEY_ONEHAND_CTRL_CATEGORY = "onehand_ctrl_category";
    private static final String KEY_ONEHAND_CTRL_QUICK_TRIGGER = "onehand_ctrl_quick_trigger";

    private CheckBoxPreference mOneHandCtrlQuickTrigger;
    private int mOneHandCtrlQuickTriggerByDefault = 0;
    private boolean mOneHandCtrlFeatureEnabled = false;
    // END leo_liao@asus.com

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.one_hand_mode_settings);


        // BEGIN leo_liao@asus.com, One-hand control
        createOneHandCtrlSettings();
        // END leo_liao@asus.com
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        // BEGIN leo_liao@asus.com, One-hand control
        updateOneHandCtrlSettingsState();
        // END leo_liao@asus.com
    }

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsEvent.MAIN_SETTINGS;
    }

        @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // TODO Auto-generated method stub
        // BEGIN leo_liao@asus.com, One-hand control
        if (preference == mOneHandCtrlQuickTrigger) {
            onOneHandCtrlQuickTriggerClick();
        }
        // END leo_liao@asus.com
        return super.onPreferenceTreeClick(preference);
    }

    // BEGIN leo_liao@asus.com, One-hand control
    private void createOneHandCtrlSettings() {
        mOneHandCtrlFeatureEnabled = getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_ASUS_WHOLE_SYSTEM_ONEHAND);
        if (!mOneHandCtrlFeatureEnabled) {
            PreferenceCategory oneHandCtrlCategory = (PreferenceCategory) findPreference(
                    KEY_ONEHAND_CTRL_CATEGORY);
            getPreferenceScreen().removePreference(oneHandCtrlCategory);
        } else {
//            mOneHandCtrlQuickTriggerByDefault = getResources().getInteger(
//                    ResCustomizeConfig.getIdentifier(getActivity(), "integer" , "config_oneHandCtrlQuickTriggerByDefault"));
            mOneHandCtrlQuickTrigger = (CheckBoxPreference) findPreference(
                    KEY_ONEHAND_CTRL_QUICK_TRIGGER);
        }
    }

    private void updateOneHandCtrlSettingsState() {
        if (mOneHandCtrlFeatureEnabled && mOneHandCtrlQuickTrigger != null) {
            final int enabled = Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ONEHAND_CTRL_QUICK_TRIGGER_ENABLED,
                    mOneHandCtrlQuickTriggerByDefault, UserHandle.USER_CURRENT);
            mOneHandCtrlQuickTrigger.setChecked(enabled > 0 ? true : false);
        }
    }

    private void onOneHandCtrlQuickTriggerClick() {
        Settings.Secure.putIntForUser(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ONEHAND_CTRL_QUICK_TRIGGER_ENABLED,
                mOneHandCtrlQuickTrigger.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
    }
    // END leo_liao@asus.com

    public static class QuickTriggerSubSettings extends SubSettings { /* empty */}
}
