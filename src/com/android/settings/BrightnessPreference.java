/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.os.IPowerManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.preference.Preference;
import android.preference.SeekBarDialogPreference;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import java.util.UUID;

public class BrightnessPreference extends CustomDialogPreference implements
    SeekBar.OnSeekBarChangeListener, CheckBox.OnCheckedChangeListener {
    private static final String TAG = "BrightnessPreference";


    private SeekBar mSeekBar;
    private CheckBox mCheckBox;

    private int mOldBrightness;
    private int mOldAutomatic;
    private int mRestoreMauelBrightness;
    private int mRestoreBrighntessMode;
    private int mCurBrightness = -1;

    private final int mMinimumBacklight;
    private final int mMaximumBacklight;
    //private final int BAR_RANGE;

    private float mRestoreAutoGain;
    private float mGain;

    private boolean mAutomaticAvailable;
    private boolean mAutomaticMode;
    private boolean DEBUG = SystemProperties.getBoolean("persist.sys.birghtdump", true);
    private boolean mIsTracking = false;
    private boolean mDailCloseRestore = false;
    private boolean mRestoredOldState;
    private boolean isBrightnessDialogTouched = false;

    private static int mShowDialogOrientation;
    private static int mSaveInstanceOrientation;
    private static final float mAdjMin = 0.4f;
    private static final float mAdjMax = 1.6f;
    private static final float DEFAULT_GAIN = 1.0f;
    private static boolean isScreenRoation = false;


    private Configuration mConfiguration;
    private IPowerManager mPower;
    private final ContentResolver RESOLVER;

    private ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if(mDailCloseRestore){
                return;
            }
            mCurBrightness = -1;
            onBrightnessChanged();
        }

        //+++ aras_yin@asus.com:
        //Fix bug - In manual mode and now is in min_backlight, seek bar will not set to 0 because Settings.System.SCREEN_BRIGHTNESS is not change.
        @Override
        public boolean deliverSelfNotifications() {
            return true;//super.deliverSelfNotifications();
        }
        //--- aras_yin@asus.com
    };

    private ContentObserver mBrightnessModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            onBrightnessModeChanged();
        }
    };

    private ContentObserver mAutoBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            float gain = getAutoBrightnessGain();
            if(gain != mGain) {
                onAutoBrightnessChanged();
            }
        }
    };




    public BrightnessPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        log("BrightnessPreference");
        RESOLVER = getContext().getContentResolver();
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();


        PackageManager pgm = context.getPackageManager();
        mAutomaticAvailable = pgm.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

        setDialogLayoutResource(R.layout.preference_dialog_brightness);
    }


    protected void showDialog() {
        //Fix bug - In manual mode and now is in min_backlight, seek bar will not set to 0 because Settings.System.SCREEN_BRIGHTNESS is not change.
        mBrightnessObserver.deliverSelfNotifications();

        RESOLVER.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
            mBrightnessObserver);

        RESOLVER.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true,
            mBrightnessModeObserver);

        RESOLVER.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ), true,
            mAutoBrightnessObserver);

        mConfiguration = this.getContext().getResources().getConfiguration();
        mShowDialogOrientation = mConfiguration.orientation;

        mRestoredOldState = false;
    }




    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        showDialog() ;
        mCurBrightness = -1;

        mSeekBar = (SeekBar)view.findViewById(R.id.seekbar);
        mSeekBar.setMax(mMaximumBacklight);
        mOldBrightness = onMaunelLevelToProgressBar();

        mCheckBox = (CheckBox) view.findViewById(R.id.automatic_mode);
        PackageManager pm = this.getContext().getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT)) {
            mCheckBox.setVisibility(View.GONE);
            mSeekBar.setEnabled(true);
        } else if (mAutomaticAvailable) {
            mCheckBox.setOnCheckedChangeListener(this);
            mOldAutomatic = getBrightnessMode();

            mAutomaticMode = mOldAutomatic == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

            mGain = getAutoBrightnessGain();
            if(mAutomaticMode) registerAutoBrightness(mAutomaticMode);
            mCheckBox.setChecked(mAutomaticMode);

        } else {
            mSeekBar.setEnabled(true);
        }
        mSeekBar.setOnSeekBarChangeListener(this);

    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        if(!mIsTracking) return;
        if(mAutomaticMode) {
            mGain = mAdjMin + (mAdjMax - mAdjMin) * ((float)progress / (float)seekBar.getMax());
            try {
                mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(mGain);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            onMaunelProgressBarToLevel(progress, false);
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        mIsTracking = true;
        log("onStartTrackingTouch");
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        // NA
        log("onStopTrackingTouch");
        isBrightnessDialogTouched = true;
        if(mAutomaticMode && mIsTracking) {
            int progress = seekBar.getProgress();
            mGain = mAdjMin + (mAdjMax - mAdjMin)
                    * ((float)progress / (float)seekBar.getMax());
            setAutoBrightnessGain(mGain);
        } else {
            onMaunelProgressBarToLevel(mSeekBar.getProgress(), true);
        }
        mIsTracking = false;
    }

    public void onMaunelProgressBarToLevel(int progress, boolean write){
        int level = (int) (Math.pow(progress / 255f, 2.2) *
                           (255f - mMinimumBacklight) + mMinimumBacklight) ;
        setBrightness(level, write);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setMode(isChecked ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC :
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        registerAutoBrightness(isChecked);
        if(!isChecked) {
            mCurBrightness = -1;
            if(mAutomaticMode){
                mSeekBar.setProgress(getBrightness());
                setBrightness(mSeekBar.getProgress(), false);
            } else{
                onMaunelLevelToProgressBar();
            }

        }
    }
    /*
    private void setBrightnessOnStopTrackingTouch(SeekBar seekBar){
        setBrightness(seekBar.getProgress(), true);
        }*/

    private void putBrightnessSetting(int brightness){
        final ContentResolver resolver = getContext().getContentResolver();
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
    }

    private int getBrightness(int mode) {
        int brightness =  mode;
        if ( mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            float value = Settings.System.getFloatForUser(RESOLVER,
                          Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 1.0f,
                          UserHandle.USER_CURRENT);
            brightness = (int) ((value - mAdjMin) / (mAdjMax - mAdjMin) * mMaximumBacklight);
        } else {
            brightness = Settings.System.getIntForUser(RESOLVER,
                         Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight,
                         UserHandle.USER_CURRENT);
            log("brightness=" + brightness);
        }
        return brightness;
    }

    private int getBrightness() {
        int mode = getBrightnessMode();
        return getBrightness( mode);
    }

    private int getBrightnessMode() {
        return Settings.System.getInt(getContext().getContentResolver(),
                                      Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    private void onBrightnessChanged() {
        int brightness = onMaunelLevelToProgressBar();
        if(!isBrightnessDialogTouched ){
            mOldBrightness = brightness;
            isBrightnessDialogTouched = false;
        }
    }

    public int onMaunelLevelToProgressBar(){
        int brightness = getBrightness();
        int currentValue = (int) (Math.pow((float)(brightness - mMinimumBacklight) /
                                           ((float)(255 - mMinimumBacklight)), 1f / 2.2f) * 255f);
        mSeekBar.setProgress(currentValue);
        return brightness;
    }

    private void onBrightnessModeChanged() {
        boolean checked =
            getBrightnessMode() == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        mCheckBox.setChecked(checked);
        if(!checked) {
            onMaunelLevelToProgressBar();
        }
    }

    private void onAutoBrightnessChanged() {
        mGain = getAutoBrightnessGain();
        int value = (int) ((mGain - mAdjMin) / (mAdjMax - mAdjMin) * mMaximumBacklight);
        mSeekBar.setProgress(value);

    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            onMaunelProgressBarToLevel(mSeekBar.getProgress(), true);
        } else {
            if(!isScreenRoation)restoreOldState();
        }
        isScreenRoation = false;
        RESOLVER.unregisterContentObserver(mBrightnessObserver);
        RESOLVER.unregisterContentObserver(mBrightnessModeObserver);
        RESOLVER.unregisterContentObserver(mAutoBrightnessObserver);

    }

    private void restoreOldState() {
        if (mRestoredOldState) return;
        mDailCloseRestore = true;
        setMode(mRestoreBrighntessMode);
        if (mAutomaticAvailable) {
            setAutoBrightnessGain(mRestoreAutoGain);
        }

        if(mRestoreBrighntessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL){
            setBrightness(mRestoreMauelBrightness, true);
        } else{
            if(getBrightness(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) != mRestoreMauelBrightness){
                putBrightnessSetting(mRestoreMauelBrightness);
            }
        }

        mRestoredOldState = true;
        mCurBrightness = -1;
    }

    private void setBrightness(int brightness, boolean write) {

        if(mAutomaticMode) return;

        try {
            if (mPower != null) {
                mPower.setTemporaryScreenBrightnessSettingOverride(brightness);
            }
            if (write) {
                mCurBrightness = -1;
                Settings.System.putInt(RESOLVER,
                                       Settings.System.SCREEN_BRIGHTNESS, brightness);
                RESOLVER.notifyChange(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), mBrightnessObserver);

            } else {
                mCurBrightness = brightness;
            }
        } catch (RemoteException doe) {
        }
    }

    private void setMode(int mode) {
        mAutomaticMode = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        Settings.System.putInt(getContext().getContentResolver(),
                               Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    private void registerAutoBrightness(boolean enabled) {
        if (enabled) {
            mSeekBar.setMax(mMaximumBacklight);
            int value = (int) ((mGain - mAdjMin) / (mAdjMax - mAdjMin) * mMaximumBacklight);
            mSeekBar.setProgress(value);
        } else {
            mSeekBar.setMax(mMaximumBacklight);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) return superState;

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.automatic = mCheckBox.isChecked();
        myState.progress = mSeekBar.getProgress();
        myState.oldAutomatic = mOldAutomatic == 1;
        myState.oldProgress = mOldBrightness;
        myState.curBrightness = mCurBrightness;
        myState.restoreGain = mRestoreAutoGain;
        myState.restoreMauel = mRestoreMauelBrightness;
        myState.restoreBrightMode = mRestoreBrighntessMode;
        mSaveInstanceOrientation = mConfiguration.orientation;
        isScreenRoation = (mSaveInstanceOrientation == mShowDialogOrientation) ? false : true;

        // Restore the old state when the activity or dialog is being paused
        if((mSaveInstanceOrientation == mShowDialogOrientation))restoreOldState();
        getDialog().dismiss();

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mOldBrightness = myState.oldProgress;
        mOldAutomatic = myState.oldAutomatic ? 1 : 0;
        setMode(myState.automatic ? 1 : 0);
        if(mAutomaticMode == false){
            onMaunelProgressBarToLevel(myState.progress , false);
        }
        mCurBrightness = myState.curBrightness;
        isScreenRoation = true;
        mRestoreAutoGain = myState.restoreGain;
        mRestoreMauelBrightness = myState.restoreMauel;
        mRestoreBrighntessMode = myState.restoreBrightMode;
    }

    private float getAutoBrightnessGain() {
        return Settings.System.getFloatForUser(RESOLVER,
                                               Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, DEFAULT_GAIN,
                                               UserHandle.USER_CURRENT);
    }

    private void setAutoBrightnessGain(float gain) {
        final ContentResolver resolver = getContext().getContentResolver();
        Settings.System.putFloat(resolver, Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, gain);
    }

    private static class SavedState extends BaseSavedState {

        boolean automatic;
        boolean oldAutomatic;
        int progress;
        int oldProgress;
        int curBrightness;
        float restoreGain;
        int restoreMauel;
        int restoreBrightMode;
        public SavedState(Parcel source) {
            super(source);
            automatic = source.readInt() == 1;
            progress = source.readInt();
            oldAutomatic = source.readInt() == 1;
            oldProgress = source.readInt();
            curBrightness = source.readInt();
            restoreGain = source.readInt();
            restoreMauel = source.readInt();
            restoreBrightMode = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(automatic ? 1 : 0);
            dest.writeInt(progress);
            dest.writeInt(oldAutomatic ? 1 : 0);
            dest.writeInt(oldProgress);
            dest.writeInt(curBrightness);
            dest.writeFloat(restoreGain);
            dest.writeInt(restoreMauel);
            dest.writeInt(restoreBrightMode);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
        new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static void log(String msg) {
        Log.v(TAG, msg);
    }


    @Override
    protected void onClick() {
        // TODO Auto-generated method stub
        super.onClick();
        log("onClick");
        mRestoreMauelBrightness = getBrightness(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        mRestoreAutoGain = getAutoBrightnessGain();
        mRestoreBrighntessMode = getBrightnessMode();
        mDailCloseRestore = false;
    }
}