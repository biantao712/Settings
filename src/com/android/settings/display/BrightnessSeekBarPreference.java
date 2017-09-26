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

package com.android.settings.display;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder; 
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.settings.R;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.Handler;
import android.os.IPowerManager;
import android.provider.Settings;
import android.provider.Settings.System;
import android.util.Log;

/**
 * Based on android.preference.Preference, but uses support preference as base.
 */
public class BrightnessSeekBarPreference extends Preference
        implements OnSeekBarChangeListener, View.OnKeyListener {

    private int mProgress;
    private boolean mTrackingTouch;
    private SeekBar seekBar;

    private final ContentResolver RESOLVER;

    private int mOldBrightness;
    private int mOldAutomatic;
    private int mRestoreMauelBrightness;
    private int mRestoreBrighntessMode;
    private int mCurBrightness = -1;
    private float mGain;

    private final int mMinimumBacklight;
    private final int mMaximumBacklight;
    private final int BAR_RANGE;
    private static final float mAdjMin = 0.4f;
    private static final float mAdjMax = 1.6f;
    private static final float DEFAULT_GAIN = 1.0f;

    private boolean mAutomaticAvailable;
    private boolean mAutomaticMode;
    private boolean mDailCloseRestore = false;
    private boolean isBrightnessDialogTouched = false;
    private boolean mRestoredOldState;
    private static boolean isScreenRoation = false;

    private IPowerManager mPower;
    private Configuration mConfiguration;
    private static int mShowDialogOrientation;

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

    public BrightnessSeekBarPreference(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        RESOLVER = context.getContentResolver();
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
        mMaximumBacklight = pm.getMaximumScreenBrightnessSetting();
        BAR_RANGE = mMaximumBacklight - mMinimumBacklight;

        PackageManager pgm = context.getPackageManager();
        mAutomaticAvailable = pgm.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

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

        mConfiguration = getContext().getResources().getConfiguration();
        mShowDialogOrientation = mConfiguration.orientation;

        mRestoredOldState = false;

       // setMax(mMax);
        setLayoutResource(R.layout.asusres_preference_display_brightness_item);
    }

    public BrightnessSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BrightnessSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.seekBarPreferenceStyle);
    }

    public BrightnessSeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
       // view.itemView.setOnKeyListener(this);

        mCurBrightness = -1;
        seekBar = (SeekBar) view.findViewById(R.id.brightness_seekbar);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setMax(BAR_RANGE);
        mOldBrightness = getBrightness();

        seekBar.setProgress(mOldBrightness);
        PackageManager pm = getContext().getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT)) {
            seekBar.setEnabled(true);
        } else if (mAutomaticAvailable) {
            mOldAutomatic = getBrightnessMode();
            mAutomaticMode = mOldAutomatic == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

            mGain = getAutoBrightnessGain();
            if(mAutomaticMode) registerAutoBrightness(mAutomaticMode);

        } else {
            seekBar.setEnabled(true);
        }
    }

    @Override
    public CharSequence getSummary() {
        return null;
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
       // setProgress(restoreValue ? getPersistedInt(mProgress)
       //         : (Integer) defaultValue);
    }

   // @Override
   // protected Object onGetDefaultValue(TypedArray a, int index) {
       // return a.getInt(index, 0);
   // }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return false;
        }

        SeekBar seekBar = (SeekBar) v.findViewById(R.id.brightness_seekbar);
        if (seekBar == null) {
            return false;
        }
        return seekBar.onKeyDown(keyCode, event);
    }

    public void onSwitchChanged(boolean isChecked) {
        setMode(isChecked ? Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC :
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

        registerAutoBrightness(isChecked);
        if(!isChecked) {
            mCurBrightness = -1;
            seekBar.setProgress(getBrightness());
            setBrightness(seekBar.getProgress(), false);
        }
    }

    private void setMode(int mode) {
        mAutomaticMode = mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        Settings.System.putInt(getContext().getContentResolver(),
                               Settings.System.SCREEN_BRIGHTNESS_MODE, mode);
    }

    public void setProgress(int progress) {
     //   setProgress(progress, true);
    }

  /*  private void setProgress(int progress, boolean notifyChanged) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < 0) {
            progress = 0;
        }
        if (progress != mProgress) {
            mProgress = progress;
            persistInt(progress);
            if (notifyChanged) {
                notifyChanged();
            }
        }
    }

    public int getProgress() {
        return mProgress;
    }
*/

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(!mTrackingTouch) return;
        if(mAutomaticMode) {
            mGain = mAdjMin + (mAdjMax - mAdjMin) * ((float)progress / (float)seekBar.getMax());
            try {
                mPower.setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(mGain);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            setBrightness(progress, false);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isBrightnessDialogTouched = true;
        if(mAutomaticMode && mTrackingTouch) {
            int progress = seekBar.getProgress();
            mGain = mAdjMin + (mAdjMax - mAdjMin)
                    * ((float)progress / (float)seekBar.getMax());
            setAutoBrightnessGain(mGain);
        } else {
            setBrightnessOnStopTrackingTouch(seekBar);
        }
        mTrackingTouch = false;
    }

    private void onBrightnessModeChanged() {
        seekBar.setProgress(getBrightness());
    }

    private void onAutoBrightnessChanged() {
        mGain = getAutoBrightnessGain();
        int value = (int) ((mGain - mAdjMin) / (mAdjMax - mAdjMin) * BAR_RANGE);
        seekBar.setProgress(value);

    }

    private void setBrightnessOnStopTrackingTouch(SeekBar seekBar){
        setBrightness(seekBar.getProgress(), true);
    }

    private void onBrightnessChanged() {
        int brightness = getBrightness();
        seekBar.setProgress(brightness);
        if(!isBrightnessDialogTouched ){
            mOldBrightness = brightness;
            isBrightnessDialogTouched = false;
        }
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

    private void registerAutoBrightness(boolean enabled) {
        if (enabled) {
            seekBar.setMax(BAR_RANGE);
            int value = (int) ((mGain - mAdjMin) / (mAdjMax - mAdjMin) * BAR_RANGE);
            seekBar.setProgress(value);
        } else {
            seekBar.setMax(BAR_RANGE);
        }
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

    private int getBrightness(int mode) {
        int brightness =  mode;
        if ( mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
            float value = Settings.System.getFloatForUser(RESOLVER,
                          Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, 1.0f,
                          UserHandle.USER_CURRENT);
            brightness = (int) ((value - mAdjMin) / (mAdjMax - mAdjMin) * BAR_RANGE);
        } else {
            brightness = Settings.System.getIntForUser(RESOLVER,
                         Settings.System.SCREEN_BRIGHTNESS, mMaximumBacklight,
                         UserHandle.USER_CURRENT);

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

    @Override
    protected Parcelable onSaveInstanceState() {
        /*
         * Suppose a client uses this preference type without persisting. We
         * must save the instance state so it is able to, for example, survive
         * orientation changes.
         */

        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }

        // Save the instance state
        final SavedState myState = new SavedState(superState);
        myState.progress = mProgress;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        // Restore the instance state
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mProgress = myState.progress;
        notifyChanged();
    }

    /**
     * SavedState, a subclass of {@link BaseSavedState}, will store the state
     * of MyPreference, a subclass of Preference.
     * <p>
     * It is important to always call through to super methods.
     */
    private static class SavedState extends BaseSavedState {
        int progress;
        int max;

        public SavedState(Parcel source) {
            super(source);

            // Restore the click counter
            progress = source.readInt();
            max = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);

            // Save the click counter
            dest.writeInt(progress);
            dest.writeInt(max);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @SuppressWarnings("unused")
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
}
