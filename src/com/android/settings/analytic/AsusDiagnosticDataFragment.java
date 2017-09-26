package com.android.settings.analytic;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.MetricsProto.MetricsEvent;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;


public class AsusDiagnosticDataFragment extends SettingsPreferenceFragment
                                                implements OnPreferenceClickListener {
    private static final String TAG = AsusDiagnosticDataFragment.class.getSimpleName();


    //   <CheckBoxPreference android:key="asus_analytics" />
    private static final String KEY_ASUS_ANALYTICS = "asus_analytics";
    private CheckBoxPreference mAsusAnalyticsPreference;

    //
    //  From Android N beginning,
    //
    //  (1)  The db key is deprecated. (Settings.System.ASUS_ANALYTICS = "asus_analytics";)
    //  (2)  We don't porting related db key to Settings Provider.
    //  (3)  Use Secure table intead of System table.
    //
    private static final String DB_KEY_ASUS_ANALYTICS = "asus_analytics"; // 1: enabled, 0: disabled.
    private static final int DB_VALUE_ASUS_ANALYTICS_ENABLED = 1;
    private static final int DB_VALUE_ASUS_ANALYTICS_DISABLED = 0;
    private static final boolean DEFAULT_VALUE_ASUS_ANALYTICS = false; // disabled


    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.asus_diagnostic_data);

        mAsusAnalyticsPreference = (CheckBoxPreference) findPreference(KEY_ASUS_ANALYTICS);

        if (mAsusAnalyticsPreference != null) {
            mAsusAnalyticsPreference.setOnPreferenceClickListener(this);
        } //END OF if (mAsusAnalyticsPreference != null)
        else {
            Log.e(TAG, " ERROR: onCreate(): Failed to get the Diagnostic Data preference!!!");
        }
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.ABOUT_LEGAL_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAsusAnalyticsPreference != null) {
            final Context context = getContext();

            mAsusAnalyticsPreference.setChecked(isAsusAnalyticsEnabled(context));
        } //END OF if (mAsusAnalyticsPreference != null)
    } //END OF onResume()

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        if (KEY_ASUS_ANALYTICS.equals(key)) {
            final Context context = getContext();
            final CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
            final boolean isChecked = checkBoxPreference.isChecked();

            if (!setAsusAnalyticsEnabled(context, isChecked)) {
                Log.w(TAG, " Warning: Cannot set db of DiagnosticData!!!"
                            + "  -->  rollback checked status of the DiagnosticData preference.");

                checkBoxPreference.setChecked(!isChecked);
            } //END OF if (!setAsusAnalyticsEnabled(context, isChecked))

            return true;
        } //END OF if (KEY_ASUS_ANALYTICS.equals(key))

        return false;
    } //END OF onPreferenceClick()



    private boolean isAsusAnalyticsEnabled(Context context) {
        if (context == null) {
            Log.e(TAG, " ERROR: Failed to get db of DiagnosticData!!!"
                            + "  (error= the context is null!) -->  return default ...");

            return DEFAULT_VALUE_ASUS_ANALYTICS;
        }

        try {
            //
            // we directly read database key.
            //
            //  1. don't call com.asus.analytics.AnalyticsSettings;
            //  2. From N beginning, we phase out com.asus.analytics.AnalyticsSettings.
            //  3. Becuase only Settings / AsusAnalytics use com.asus.analytics.AnalyticsSettings,
            //     but DataSDK directly access database key, it will the UI of Settings doesn't
            //     match the value of database key.
            //
            return (Settings.Secure.getInt(context.getContentResolver(),
                                                DB_KEY_ASUS_ANALYTICS)
                            == DB_VALUE_ASUS_ANALYTICS_ENABLED);
        }
        catch (Settings.SettingNotFoundException e) {
            //
            //  If secure "asus_analytics" isn't found, we will check system "asus_analytics",
            //  and then set 0 to system "asus_analytics".
            //
            Log.w(TAG, " Warning: Failed to find find db of DiagnosticData."
                                + "  (error=" + e.getMessage()
                                + ") -->  check previous settings ...", e);

            int dbValue = DB_VALUE_ASUS_ANALYTICS_DISABLED;

            try {
                dbValue = Settings.System.getInt(context.getContentResolver(),
                                                    DB_KEY_ASUS_ANALYTICS);
            }
            catch (Settings.SettingNotFoundException e2) {
                Log.w(TAG, " Warning: Failed to find find db of DiagnosticData."
                                + "  (error=" + e2.getMessage()
                                + ")  -->  ignore this ...", e2);
            }
            finally {
                try {
                    Settings.System.putInt(context.getContentResolver(),
                                                DB_KEY_ASUS_ANALYTICS,
                                                DB_VALUE_ASUS_ANALYTICS_DISABLED);
                }
                catch (Exception e3) {
                    Log.e(TAG, " ERROR: Failed to reset previous settings!!!"
                                        + "  (error=" + e3.getMessage()
                                        + ")  -->  ignore this ...", e3);
                }

                if (setAsusAnalyticsEnabled(context, dbValue)) {
                    return (dbValue == DB_VALUE_ASUS_ANALYTICS_ENABLED);
                }
                else {
                    return DEFAULT_VALUE_ASUS_ANALYTICS;
                }
            }
        }
        catch (Exception e) {
            Log.e(TAG, " ERROR: Failed to get db of DiagnosticData!!!"
                            + "  (error=" + e.getMessage()
                            + ")", e);

            return DEFAULT_VALUE_ASUS_ANALYTICS;
        }
    } //END OF isAsusAnalyticsEnabled()

    private boolean setAsusAnalyticsEnabled(Context context, boolean isEnabled) {
        return setAsusAnalyticsEnabled(context,
                        (isEnabled ? DB_VALUE_ASUS_ANALYTICS_ENABLED
                                    : DB_VALUE_ASUS_ANALYTICS_DISABLED));
    }

    private boolean setAsusAnalyticsEnabled(Context context, int enabledValue) {
        if (context == null) {
            Log.e(TAG, " ERROR: Failed to set db of DiagnosticData!!!"
                            + "  (error= the context is null!)");
            return false;
        }

        try {
            return Settings.Secure.putInt(context.getContentResolver(),
                                                DB_KEY_ASUS_ANALYTICS,
                                                enabledValue);
        }
        catch (Exception e) {
            Log.e(TAG, " ERROR: Failed to set db of DiagnosticData!!!"
                            + "  (error=" + e.getMessage()
                            + ")", e);

            return false;
        }
    } //END OF setAsusAnalyticsEnabled()
}
