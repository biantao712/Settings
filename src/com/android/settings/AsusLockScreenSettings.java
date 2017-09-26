/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.SwitchPreference;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.app.admin.DevicePolicyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;
import com.android.settings.analytic.AnalyticUtils.Label;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
import com.asus.settings.lockscreen.AsusLSConsts;
import com.asus.settings.lockscreen.AsusLSDBHelper;
import com.asus.settings.lockscreen.AsusLSUtils;
//import com.asus.settings.lockscreen.ui.AsusQuickAccessSettings;
import com.asus.settings.lockscreen.ui.LockscreenClockWidgetSwitchPreference;
//import com.asus.settings.lockscreen.ui.LockscreenIntruderSelfieSwitchPreference;
//import com.asus.settings.lockscreen.ui.LockscreenShortcutSwitchPreference;
import com.asus.settings.lockscreen.ui.LockscreenSkipSlideSwitchPreference;
import com.asus.settings.lockscreen.ui.LockscreenShowMagzineSwitchPreference;
import com.asus.settings.lockscreen.ui.LockscreenStatusBarSwitchPreference;
import com.asus.settings.lockscreen.ui.LockscreenWeatherAnimationSwitchPreference;
//import com.asus.settings.lockscreen.ui.LockscreenWhatsNextSwitchPreference;

import java.util.ArrayList;
import java.util.List;
import android.content.res.Resources;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;



/**
 * Asus lockScreen settings.
 */
public class AsusLockScreenSettings extends AsusSecuritySettings
        implements CompoundButton.OnCheckedChangeListener, Indexable {

    private static final String TAG = "AsusLockScreenSettings_NewFeature";

    private static final String KEY_DISPLAY = "lockscreen_setting_display";
    private static final String KEY_SHORTCUT = "lockscreen_shortcuts_display";
    private static final String KEY_WALLPAPER = "lock_screen_wallpaper_display";
    private static final String KEY_SHORTCUT_SWITCH = "lockscreen_shortcuts_display";
    private static final String KEY_STATUSBAR_SWITCH = "lockscreen_statusbar_display";
    private static final String KEY_WHATS_NEXT_SWITCH = "lockscreen_whats_next_widget";
    private static final String KEY_INSTANT_CAMERA_SWITCH = "lockscreen_instant_camera_widget";
    private static final String KEY_ENABLE_CAMERA_SWITCH = "lockscreen_enable_camera_widget";


    private static final String KEY_LOCKSCRREN_SETTINGS_SET_PASSWORD = "lockscreen_settings_set_password";
    private static final String KEY_LOCKSCREEN_SETTINGS = "lockscreen_settings_nf";
    private static final String KEY_LOCKSCREEN_SET_PASSWORD = "lockscreen_set_password_nf";
    private static final String KEY_SKIP_SLIDE_SHOW_PATTERN = "lockscreen_skip_slide_show_pattern";
    private static final String KEY_SKIP_SLIDE_SWITCH = "lockscreen_skip_slide";
    private static final String KEY_SHOW_PATTERN_SWITCH = "lockscreen_show_pattern";


    private static final String KEY_INTRUDER_SELFIE_SWITCH = "lockscreen_intruder_selfie";
    //BEGIN:Jeffrey_Chiang@asus.com long press key to launch camera option
    private static final String KEY_LONGPRESS_INSTANT_CAMERA_SWITCH = "lockscreen_longpress_instant_camera_widget";
    //BEGIN:Jeffrey_Chiang@asus.com long press key to launch camera option

    // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
//    private static final String KEY_LOCKSCREEN_WALLPAPER_SWITCH = "lockscreen_wallpaper_settings";
    private static final String KEY_LOCKSCREEN_WALLPAPER_SWITCH = "lockscreen_settings_nf";
    private static final String KEY_LOCKSCREEN_THEME_SWITCH = "lockscreen_theme_settings";
    private static final String KEY_WEATHER_ANIMATION_SWITCH = "lockscreen_enable_weather_animation";
    private static final String KEY_LCOKSCREEN_CLOCK_WIDGET_SWITCH = "lockscreen_show_clock_widget";
    private static final String KEY_LCOKSCREEN_SLIDESHOW_WP_SWITCH = "lockscreen_slideshow_wallpaper";
    // END:Evelyn_Chang@asus.com for ZenFone3.

    // +++++++++these string is used for communicate with System UI and LockScreen.
    public static final String SETTINGS_SYSTEM_ASUS_LOCKSCREEN_PATTERN_ENABLE = "SETTINGS_SYSTEM_ASUS_LOCKSCREEN_PATTERN_ENABLE";
    // ---------these string is used for communicate with System UI and LockScreen.
    private DevicePolicyManager mDPM;



//    private LockscreenShortcutSwitchPreference mShortcutSwitchPref = null;
    private LockscreenStatusBarSwitchPreference mStatusBarSwitchPreference = null;
//    private LockscreenWhatsNextSwitchPreference mWhatsNextSwitchPref = null;
    private LockscreenInstantCameraSwitchPreference mInstantCameraSwitchPref = null;
    //private LockscreenEnableCameraSwitchPreference mEnableCameraSwitchPref = null;
    private Preference mLockScreenSettingsPref = null;
    private Preference mLockScreenSetPasswordPref = null;
    private LockscreenSkipSlideSwitchPreference mSkipSlideSwitchPref = null;
    private SwitchPreference mShowPatternSwitchPref = null;
    //BEGIN:Jeffrey_Chiang@asus.com long press key to launch camera option
    private LockscreenLongPressInstantCameraSwitchPreference mLongPressInstantCameraSwitchPref = null;
    //BEGIN:Jeffrey_Chiang@asus.com long press key to launch camera option

    // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
    private LockscreenWeatherAnimationSwitchPreference mWeatherAnimationSwitchPref = null;
    private LockscreenClockWidgetSwitchPreference mLSClockWidgetSwitchPref = null;
    // END:Evelyn_Chang@asus.com for ZenFone3.

    private boolean mIsStatusbarEnabled;
    private boolean mIsInstantCameraEnabled;
    //private boolean mIsCameraSettingEnabled;
    private boolean mIsSkipSlide;
    private boolean mIsShowPattern;
    private boolean mIsLongPressInstantCameraEnabled;

    // BEGIN:Evelyn_Chang@asus.com for Asus Lock Screen Weather Animation Effect.
    private boolean mIsWeatherAnimationEnabled;
    private boolean mShowClockWidget;
    // END:Evelyn_Chang@asus.com for Asus Lock Screen Weather Animation Effect.
    private boolean mIsCNLockScreen = false;

    // +++ asus flip cover
    private static final String DB_KEY_COVER_AUTOMATIC_UNLOCK = "asus_transcover_automatic_unlock";
    private static final String KEY_TRANSCOVER_AUTOMATIC_UNLOCK = "key_asus_transcover_automatic_unlock";
    private static final String LOCKSCREEN_SETTING_COVER = "lockscreen_setting_cover";
    private SwitchPreference mTranscoverAutomaticUnlockCB = null;
    private PreferenceCategory mCoverCategroy = null;

    private PreferenceCategory mSettingsSetPasswordCategroy = null;
    private PreferenceCategory mSkipSlideShowPatternCategroy = null;
    // --- asus flip cover

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TrackerManager.sendEvents(getActivity(), TrackerName.TRACKER_MAIN_ENTRIES, Category.LOCKSCREEN_ENTRY,
                Action.ENTER_SETTINGS, TrackerManager.DEFAULT_LABEL, TrackerManager.DEFAULT_VALUE);

        mIsCNLockScreen = SystemProperties.getBoolean("ro.asus.cnlockscreen", false);
        Log.d(TAG, "mIsCNLockScreen:" + mIsCNLockScreen);
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if(mDPM == null)
        {
            Log.d(TAG,"mDPM is null");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        root.setBackgroundColor(getResources().getColor(R.color.category_divider_background));
        return root;
    }

    protected PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = super.createPreferenceHierarchy();

        final UserManager um = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        //TODO
        /*
        // Mark Snap View.
        boolean mPrivateMode = Utils.isPrivateMode(um);
        int mPrivateUserId = Utils.getPrivateUserId(um);
        if(mPrivateMode && ActivityManager.getCurrentUser()== mPrivateUserId){
            return root;
        }*/

        addPreferencesFromResource(R.xml.asus_lockscreen_display_settings);
        mSettingsSetPasswordCategroy = (PreferenceCategory) root.findPreference(KEY_LOCKSCRREN_SETTINGS_SET_PASSWORD);
        mSkipSlideShowPatternCategroy = (PreferenceCategory) root.findPreference(KEY_SKIP_SLIDE_SHOW_PATTERN);
        if(mSkipSlideShowPatternCategroy != null)
        {
            mSkipSlideSwitchPref = (LockscreenSkipSlideSwitchPreference) mSkipSlideShowPatternCategroy.findPreference(KEY_SKIP_SLIDE_SWITCH);
            mShowPatternSwitchPref = (SwitchPreference) mSkipSlideShowPatternCategroy.findPreference(KEY_SHOW_PATTERN_SWITCH);
            if(mSkipSlideSwitchPref == null || mShowPatternSwitchPref == null)
            {
                Log.d(TAG,"mSkipSlideSwitchPref  or mShowPatternSwitchPref is null");
            }
        }
        else
        {
            Log.d(TAG,"mSkipSlideShowPatternCategroy is null");
        }

		if(mSettingsSetPasswordCategroy != null)
        {
            // set unlock password summary.
            String strPasswordSummary = "Original";
            mLockScreenSetPasswordPref = (Preference) mSettingsSetPasswordCategroy.findPreference(KEY_LOCKSCREEN_SET_PASSWORD);
            if(mLockScreenSetPasswordPref != null)
            {
                if (mLockPatternUtils.isLockScreenDisabled(UserHandle.myUserId())) {
                    strPasswordSummary = getString(R.string.unlock_set_unlock_mode_off_nf_title);
                } else {
                    switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
                        case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                            strPasswordSummary = getString(R.string.unlock_set_unlock_pattern_nf_title);
                            break;
                        case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                        case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                            strPasswordSummary = getString(R.string.unlock_set_unlock_pin_nf_title);
                            break;
                        case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                        case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                        case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                            strPasswordSummary = getString(R.string.unlock_set_unlock_password_nf_title);
                            break;
                        case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                            strPasswordSummary = getString(R.string.unlock_set_unlock_mode_none_nf_title);
                            break;
                    }
                }

                Log.d(TAG,"set password summary to :" + strPasswordSummary);
                mLockScreenSetPasswordPref.setSummary(strPasswordSummary);
            }
            else
            {
                Log.d(TAG,"mSettingsSetPasswordCategroy is null");
            }
        }
        else
        {
            Log.d(TAG,"mSettingsSetPasswordCategroy is null");
        }

        boolean isPattern = mLockPatternUtils.getKeyguardStoredPasswordQuality(UserHandle.myUserId()) == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
        Log.d(TAG,"is unlock method pattern:" + isPattern);
        if (!mLockPatternUtils.isSecure(UserHandle.myUserId())){
            try {
                Log.d(TAG, "Not secure, remove mSkipSlideSwitchPref mShowPatternSwitchPref");
                root.removePreference(mSkipSlideShowPatternCategroy);
                mSkipSlideShowPatternCategroy = null;
                mSkipSlideSwitchPref = null;
                mShowPatternSwitchPref = null;
            }
            catch(Exception ee)
            {
                Log.d(TAG,"Error of remove mSkipSlideSwitchPref mShowPatternSwitchPref: " + ee);
            }
        }
        else if(!isPattern  && mLockPatternUtils.isSecure(UserHandle.myUserId()))
        {
            if(mSkipSlideShowPatternCategroy != null && mShowPatternSwitchPref != null)
            {
                mSkipSlideShowPatternCategroy.removePreference(mShowPatternSwitchPref);
		        mShowPatternSwitchPref = null;
            }
            else
            {
                Log.d(TAG,"mShowPatternSwitchPref or mSkipSlideShowPatternCategroy is null");
            }
        }
        else
        {
            Log.d(TAG,"it is secure ,do not remove skip slide");
        }

        if (mIsCNLockScreen) {

            Preference display = findPreference(KEY_DISPLAY);
            if (display != null) {
                this.removePreference(KEY_DISPLAY);
            }
        }

        // BEGIN:Evelyn_Chang@asus.com for ZenFone3.
        Preference lsWallpaperPref = root.findPreference(KEY_LOCKSCREEN_WALLPAPER_SWITCH);
        // LockScreen Wallpaper
        if (lsWallpaperPref != null && (!AsusLSDBHelper.getDeviceSupportLSWallpaper(getContext()) || mIsCNLockScreen)) {
            root.removePreference(lsWallpaperPref);
        } else {
            if (AsusLSUtils.DEBUG_FLAG) {
                Log.w(TAG, "createPreferenceHierarchy: lsWallpaperPref=" + lsWallpaperPref);
            }
        }
        // +++ asus flip cover
        mTranscoverAutomaticUnlockCB = (SwitchPreference) root.findPreference(KEY_TRANSCOVER_AUTOMATIC_UNLOCK);
        mCoverCategroy = (PreferenceCategory) root.findPreference(LOCKSCREEN_SETTING_COVER);
        if (isVZWSku()) {
            updateTranscover();
        } else {
            if (root != null && mCoverCategroy != null) {
                root.removePreference(mCoverCategroy);
            }
        }
        // --- asus flip cover
        return root;
    }

    private boolean isWhatsNextServiceEnabled(){
        return false;
/*
        String AUTHORITY = "com.asus.sitd.whatsnext.contentprovider";
        Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
        Uri uri = Uri.withAppendedPath(CONTENT_URI, "whatsnextEnable");
        Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
        boolean isWhatsNextEnable = false;
        if (null != cursor) {
            try {
                isWhatsNextEnable = cursor.getCount() > 0;
            } catch(Exception e){
                Log.w(TAG,"isWhatsNextServiceEnabled ex:",e);
                isWhatsNextEnable = false;
            } finally {
                cursor.close();
            }
        }
        return isWhatsNextEnable;*/
    }

    @Override
    public void onResume() {
        super.onResume();

        updatePreference();

        if (mSkipSlideSwitchPref != null) {
            Log.d(TAG,"set listener skip slide");
            mSkipSlideSwitchPref.setOnSwitchCheckedChangeListener(this);
        }

        if(mShowPatternSwitchPref != null){
            Log.d(TAG,"set listener show pattern");
            mShowPatternSwitchPref.setOnPreferenceChangeListener(this);
        }
    }


    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Log.d(TAG,"onPreferenceTreeClick");
        final ContentResolver res = getContentResolver();
        final String key = preference.getKey();
        Log.d(TAG,"key is :" + key);
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
		
		if (KEY_SKIP_SLIDE_SWITCH.equals(key)) {
            if(mSkipSlideSwitchPref != null)
            {
                Log.d(TAG,"skip slide is clicked");
                mSkipSlideSwitchPref.setSwitchChecked(!mSkipSlideSwitchPref.isChecked());
            }
            else
            {
                Log.d(TAG,"mSkipSlideSwitchPref is null");
            }

        }
		
        if (preference == mShowPatternSwitchPref) {
            if(mShowPatternSwitchPref != null)
            {
                mIsShowPattern = mShowPatternSwitchPref.isChecked();
                Log.d(TAG,"preference is show pattern, update status to :" + mIsShowPattern);
                lockPatternUtils.setVisiblePatternEnabled(mShowPatternSwitchPref.isChecked(),UserHandle.myUserId());

                Settings.System.putInt(getContentResolver(),
                        SETTINGS_SYSTEM_ASUS_LOCKSCREEN_PATTERN_ENABLE, mIsShowPattern? 1:0);
            }
            else
            {
                Log.d(TAG,"mShowPatternSwitchPref is null");
            }
        }

        // +++ asus flip cover
        else if (KEY_TRANSCOVER_AUTOMATIC_UNLOCK.equals(key)) {
            try {
                Settings.System.putInt(
                        getContentResolver(), DB_KEY_COVER_AUTOMATIC_UNLOCK,
                        mTranscoverAutomaticUnlockCB.isChecked() ? 1 : 0);
            } catch (Exception e) {
                // TODO: handle exception
            }
            updateTranscover();
        }
        // --- asus flip cover
        else {
                Log.d(TAG,"not handle this , transfer to super");
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preference);
        }

        return true;
    }

    private void updatePreference() {
        final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        handleStateChanged();
		if (mSkipSlideSwitchPref != null){
            mSkipSlideSwitchPref.setSwitchChecked(mIsSkipSlide);
        }
		
        if(mShowPatternSwitchPref != null){
            mShowPatternSwitchPref.setChecked(mIsShowPattern);
            lockPatternUtils.setVisiblePatternEnabled(mIsShowPattern,UserHandle.myUserId());
        }
    }

    private void handleStateChanged() {
        int status = 0;
        status = Settings.System.getInt(getContentResolver(),
                Settings.System.ASUS_LOCKSCREEN_SKIP_SLID_DISABLED, 0);
        mIsSkipSlide = (status == 0 ? false : true);
        Log.d(TAG,"get skip slide data :" + mIsSkipSlide);

        status = Settings.System.getInt(getContentResolver(),
                SETTINGS_SYSTEM_ASUS_LOCKSCREEN_PATTERN_ENABLE, 1);
        mIsShowPattern = (status == 0 ? false : true);
        Log.d(TAG,"get show pattern data :" + mIsShowPattern);
    }

   @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
       final LockPatternUtils lockPatternUtils = mChooseLockSettingsHelper.utils();
        int id = buttonView.getId();
		
		if (id == R.id.switchskipslide) {
            if (buttonView instanceof Switch) {
                if (mIsSkipSlide != isChecked) {
                    mIsSkipSlide = isChecked;
                    int status = (mIsSkipSlide ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.ASUS_LOCKSCREEN_SKIP_SLID_DISABLED, status);
                    if(mSkipSlideSwitchPref != null)
                    {
                        mSkipSlideSwitchPref.setSwitchChecked(mIsSkipSlide);
                    }
                    else
                    {
                        Log.d(TAG,"mSkipSlideSwitchPref is null");
                    }
                    if (mIsSkipSlide) {
                        TrackerManager.sendEvents(getActivity(),
                                TrackerName.LockScreenSettings,
                                Category.SKIP_SLIDE,
                                Action.SKIP_SLIDE_STATUS_OPEN,
                                Label.LOCKSCREEN_MODE_OPEN,
                                TrackerManager.DEFAULT_VALUE);
                    } else {
                        TrackerManager.sendEvents(getActivity(),
                                TrackerName.LockScreenSettings,
                                Category.SKIP_SLIDE,
                                Action.SKIP_SLIDE_STATUS_OFF,
                                Label.LOCKSCREEN_MODE_OFF,
                                TrackerManager.DEFAULT_VALUE);
                    }
                }
            }

        }
		if (id == R.id.switchshowpattern) {
            Log.d(TAG,"id switch show pattern.");
            if (buttonView instanceof Switch) {
                if (mIsShowPattern != isChecked) {
                    mIsShowPattern = isChecked;
                    int status = (mIsShowPattern ? 1 : 0);
                    Settings.System.putInt(getContentResolver(),
                            SETTINGS_SYSTEM_ASUS_LOCKSCREEN_PATTERN_ENABLE, status);
                    if(mShowPatternSwitchPref!= null)
                    {
                        mShowPatternSwitchPref.setChecked(mIsShowPattern);
                        lockPatternUtils.setVisiblePatternEnabled(mIsShowPattern,UserHandle.myUserId());
                    }
                    else
                    {
                        Log.d(TAG,"mShowPatternSwitchPref is null");
                    }
                }
            }
        }
    }

    // +++ jeson_li: ViewFlipCover
    private void updateTranscover() {
        if (mTranscoverAutomaticUnlockCB != null) {
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
    // --- jeson_li: ViewFlipCover

    //Asus flip cover+++
    private boolean isVZWSku(){
        String sku = android.os.SystemProperties.get("ro.build.asus.sku").toUpperCase(Locale.US);
        return sku != null && sku.startsWith("VZW");
    }
    //Asus flip cover---

    // add to search
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                    final Resources res = context.getResources();

                    // Add fragment title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.asus_lockscreen_settings_title);
                    data.screenTitle = res.getString(R.string.asus_lockscreen_settings_title);
                    result.add(data);
                    return result;
                }

            };

}
