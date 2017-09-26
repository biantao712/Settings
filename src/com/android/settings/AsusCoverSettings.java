
package com.android.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.R;

public class AsusCoverSettings extends SettingsPreferenceFragment implements Indexable {

    private static final String TAG = AsusCoverSettings.class.getSimpleName();
    private static final String ASUS_TRANSCOVER = "asus_transcover";
    private static final String DB_KEY_COVER_AUTOMATIC_UNLOCK = "asus_transcover_automatic_unlock";
    private static final String KEY_TRANSCOVER_CHECKBOX = "key_asus_transcover_checkbox";
    private static final String KEY_TRANSCOVER_AUTOMATIC_UNLOCK = "key_asus_transcover_automatic_unlock";
    private static final String COVER_ACIVITY  = "com.asus.flipcover.view.settings.CoverSettingsActivity";
    private CheckBoxPreference mTranscoverCheckBox = null, mTranscoverAutomaticUnlockCB = null;

    @Override
    public void onCreate(Bundle icicle) {
        // TODO Auto-generated method stub
        super.onCreate(icicle);
        int coverVersion = getCoverVersion(getActivity());
        if (coverVersion == 0) {
            addPreferencesFromResource(R.xml.asus_cover_settings);
            mTranscoverCheckBox = (CheckBoxPreference) findPreference(KEY_TRANSCOVER_CHECKBOX);
            mTranscoverAutomaticUnlockCB = (CheckBoxPreference) findPreference(KEY_TRANSCOVER_AUTOMATIC_UNLOCK);
            TrackerManager.sendEvents(getActivity(), TrackerName.TRACKER_MAIN_ENTRIES, Category.ASUSCOVER_ENTRY,
                   Action.ENTER_SETTINGS, TrackerManager.DEFAULT_LABEL, TrackerManager.DEFAULT_VALUE);
        } else if (coverVersion == 1) {
            Intent intent = new Intent(SettingsActivity.FLIPCOVER_ACTION);
            intent.setPackage(SettingsActivity.FLIPCOVER_PKG);
            if (isIntentAvailable(intent)) {
                startActivity(intent);
            }
            finish();
        } else if (coverVersion == 2) {
            Intent intent = new Intent(SettingsActivity.FLIPCOVER2_ACTION);
            intent.setPackage(SettingsActivity.FLIPCOVER2_PKG);
            if (isIntentAvailable(intent)) {
                startActivity(intent);
            }
            finish();
        } else if (coverVersion == 3) {
            Intent intent = new Intent(SettingsActivity.FLIPCOVER3_ACTION);
            intent.setPackage(SettingsActivity.FLIPCOVER3_PKG);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (isIntentAvailable(intent)) {
                startActivity(intent);
            }
            finish();
        } else {
            Log.d(TAG, "This devices didn't support Cover and it should not show any settings page.");
            finish();
        }
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        updateTranscover();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // TODO Auto-generated method stub
        // +++ jeson_li: ViewFlipCover
        if (preference == mTranscoverCheckBox) {
            try {
                Settings.System.putInt(
                        getContentResolver(), ASUS_TRANSCOVER,
                        mTranscoverCheckBox.isChecked() ? 1 : 0);
            } catch (Exception e) {
                // TODO: handle exception
            }
            updateTranscover();
        } else if (preference == mTranscoverAutomaticUnlockCB) {
            try {
                Settings.System.putInt(
                        getContentResolver(), DB_KEY_COVER_AUTOMATIC_UNLOCK,
                        mTranscoverAutomaticUnlockCB.isChecked() ? 1 : 0);
            } catch (Exception e) {
                // TODO: handle exception
            }
            updateTranscover();
        }
        // --- jeson_li: ViewFlipCover
        return super.onPreferenceTreeClick(preference);
    }

    // +++ jeson_li: ViewFlipCover
    private void updateTranscover() {
        boolean checked = true;
        try {
            checked = (Settings.System.getInt(
                    getContentResolver(), ASUS_TRANSCOVER, 1) != 0);
        } catch (Exception e) {
            // TODO: handle exception
        }
        mTranscoverCheckBox.setChecked(checked);

        if (mTranscoverAutomaticUnlockCB != null) {
            mTranscoverAutomaticUnlockCB.setEnabled(checked);
            boolean automaticUnlock = true;
            try {
                automaticUnlock = (Settings.System.getInt(
                        getContentResolver(), DB_KEY_COVER_AUTOMATIC_UNLOCK, 1) != 0);
            } catch (Exception e) {
                // TODO: handle exception
            }
            mTranscoverAutomaticUnlockCB.setChecked(automaticUnlock);
        }
    }

    private boolean isSnapView() {
        try {
            int myUserId = android.app.ActivityManagerNative.getDefault().getCurrentUser().id;
            Log.d(TAG, "myUserId:" + myUserId);
            android.os.UserManager mCoverUserManager = (android.os.UserManager) getActivity()
                    .getSystemService(android.content.Context.USER_SERVICE);
            android.content.pm.UserInfo user = mCoverUserManager.getUserInfo(myUserId);
            if (user == null) {
                Log.d(TAG, "userInfo=null");
            } else {
                boolean result = false;
                Log.d(TAG, "isSnapView:" + result);
                return result;
            }
        } catch (Exception e) {
            // TODO: handle exception
            Log.d(TAG, "isSnapView exception:" + e.toString());
        }
        return false;
    }
    // --- jeson_li: ViewFlipCover

    @Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return com.android.internal.logging.MetricsLogger.ASUS_COVER;
    }

    //for search asus flip cover on settings.
    /**
     *  Get Cover version
     * @param context
     * @return 
     * -1,Asus cover will not show on AsusSettings(Din't built-in Cover app).
     * 0, Asus cover item will show on AsusSettings and user should link to "com.android.settings.AsusCoverSettings(Din't built-in Cover app)"
     * 1,Asus cover item will show on AsusSettings and user should link to "com.asus.flipcover.view.settings.CoverSettingsActivity" on Cover1
     * 2,Asus cover item will show on AsusSettings and user should link to "com.asus.flipcover.view.settings.CoverSettingsActivity" on Cover2
     * 3,Asus cover item will show on AsusSettings and user should link to "com.asus.flipcover.view.settings.CoverSettingsActivity" on Cover3
     */
    protected static int getCoverVersion(Context context) {
        if (context == null) {
            Log.e(TAG, "Context is null");
            return -1;
        }
        final PackageManager pm = context.getPackageManager();
        boolean hasTranscoverFeature = pm.hasSystemFeature(SettingsActivity.FEATURE_ASUS_TRANSCOVER);
        boolean hasTranscoverInfoFeature = pm.hasSystemFeature(SettingsActivity.FEATURE_ASUS_TRANSCOVER_INFO);
        //Cover settings
        boolean isTranscoverSetting1 = pm.hasSystemFeature(
                SettingsActivity.FEATURE_ASUS_TRANSCOVER_SETTING_COVER1);
        //Cover2 settings
        boolean isTranscoverSetting2 = pm.hasSystemFeature(
                SettingsActivity.FEATURE_ASUS_TRANSCOVER_SETTING_COVER2);
        //Cover3 settings
        boolean isTranscoverSetting3 = pm
                .hasSystemFeature(SettingsActivity.FEATURE_ASUS_TRANSCOVER_SETTING_COVER3);

        Log.d(TAG,
                "hasTranscoverFeature:" + hasTranscoverFeature
                        + ",hasTranscoverInfoFeature:" + hasTranscoverInfoFeature
                        + ",isTranscoverSetting1:" + isTranscoverSetting1
                        + ",isTranscoverSetting2:" + isTranscoverSetting2
                        + ",isTranscoverSetting3:" + isTranscoverSetting3
                        + ",is VZWSku:" + isVZWSku());
        if ((!hasTranscoverFeature && !hasTranscoverInfoFeature) || isVZWSku()) {
          //Didn't built-in Cover application and it will not show ASUS Cover item in settings
            return -1;
        }
        else if ((hasTranscoverFeature && !hasTranscoverInfoFeature)) {
            //Cover without hole
           //Didn't built-in Cover application,but it will show ASUS Cover item in settings and launch AsusCoverSettings in AsusSettings
            return 0;
        }
        else if ((hasTranscoverFeature && hasTranscoverInfoFeature && isTranscoverSetting1)) {
            //Cover1
            return 1;
        }
        else if (hasTranscoverFeature && hasTranscoverInfoFeature && isTranscoverSetting2) {
            //Cover2
            return 2;
        }
        else if (hasTranscoverFeature && hasTranscoverInfoFeature && isTranscoverSetting3) {
            //Cover3
            return 3;
        }
        return 0;
    }

    private boolean isIntentAvailable(Intent intent) {
        List<ResolveInfo> list = getActivity().getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        boolean isAvailable = list.size() > 0;
        Log.d(TAG, "isIntentAvailable:" + isAvailable);
        return isAvailable;
    }

    private static boolean isVZWSku(){
        String sku = android.os.SystemProperties.get("ro.build.asus.sku").toUpperCase(Locale.US);
        return sku != null && sku.startsWith("VZW");
    }

    //for search asus flip cover on settings.
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    if (getCoverVersion(context) == -1) {
                        return null;
                    }
                    else {
                        final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                        final Resources res = context.getResources();
                        String coverAction = null;
                        String coverPkg = null;
                        String targetClass = null;
                        String title = res.getString(R.string.asus_cover);
                        String screentitle = res.getString(R.string.asus_cover);
                        int resid = R.drawable.ic_settings_cover;

                        int coverVersion = getCoverVersion(context);
                        Log.d(TAG, "CoverVersion:" + coverVersion);
                        if (coverVersion == -1) {
                            coverAction = null;
                            coverPkg = null;
                            targetClass = null;
                            title = null;
                            screentitle = null;
                        }
                        else if (coverVersion == 0) {
                            coverAction = null;
                            coverPkg = null;
                            targetClass = null;
                        }
                        else if (coverVersion == 1) {
                            coverAction = SettingsActivity.FLIPCOVER_ACTION;
                            coverPkg = SettingsActivity.FLIPCOVER_PKG;
                            targetClass = COVER_ACIVITY;
                        }
                        else if (coverVersion == 2) {
                            coverAction = SettingsActivity.FLIPCOVER2_ACTION;
                            coverPkg = SettingsActivity.FLIPCOVER2_PKG;
                            targetClass = COVER_ACIVITY;
                        }
                        else if (coverVersion == 3) {
                            coverAction = SettingsActivity.FLIPCOVER3_ACTION;
                            coverPkg = SettingsActivity.FLIPCOVER3_PKG;
                            targetClass = COVER_ACIVITY;
                            resid = R.drawable.ic_settings_cover3;
                        }

                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = title;
                        data.screenTitle = screentitle;
                        data.intentAction = coverAction;
                        data.intentTargetPackage = coverPkg;
                        data.intentTargetClass = targetClass;
                        data.iconResId = resid;
                        result.add(data);

                        return result;
                    }
                }
            };
}
