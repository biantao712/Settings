/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.SeekBarVolumizer;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SeekBarPreference;

import java.util.Objects;

/** A slider preference that directly controls an audio stream volume (no dialog) **/
public class VolumeSeekBarPreference extends SeekBarPreference
        implements AudioManager.OnAudioFocusChangeListener {
    private static final String TAG = "VolumeSeekBarPreference";
    private boolean DEBUG = Log.isLoggable("volume", Log.DEBUG);

    private int mStream;
    private SeekBar mSeekBar;
    private SeekBarVolumizer mVolumizer;
    private Callback mCallback;
    private ImageView mIconView;
    private TextView mTitleTextView;
    private CharSequence mTitleText;
    private TextView mSuppressionTextView;
    private String mSuppressionText;
    private boolean mMuted;
    private boolean mZenMuted;
    private int mIconResId;
    private int mMuteIconResId;
    private boolean mStopped;

    private AudioManager mAudioManager;
    private boolean mHasAudioFocus = false;

    private int mPreProgress = 0;
	
	private View mDividerView;
	private boolean mIsDivider = true;


    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_volume_slider);
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public VolumeSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VolumeSeekBarPreference(Context context) {
        this(context, null);
    }

    public void setStream(int stream) {
        mStream = stream;
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void onActivityResume() {
        if (mStopped) {
            init();
        }
    }

    public void onActivityPause() {
        mStopped = true;
        if (mVolumizer != null) {
            abandonAudioFocus();
            mVolumizer.stop();
        }
    }
    
    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        if (mStream == 0) {
            Log.w(TAG, "No stream found, not binding volumizer");
            return;
        }
        mSeekBar = (SeekBar) view.findViewById(com.android.internal.R.id.seekbar);
        mIconView = (ImageView) view.findViewById(com.android.internal.R.id.icon);
        mSuppressionTextView = (TextView) view.findViewById(R.id.suppression_text);
        // +++ Add by Morphy_Huang@2016/11/17
        mTitleTextView = (TextView) view.findViewById(com.android.internal.R.id.title);
        // --- Add by Morphy_Huang@2016/11/17
		mDividerView = view.findViewById(R.id.divider);
		if(!mIsDivider && mDividerView != null){
			mDividerView.setVisibility(View.GONE);
		}
        init();
    }

    private void init() {
        if (mSeekBar == null) return;
        final SeekBarVolumizer.Callback sbvc = new SeekBarVolumizer.Callback() {
            @Override
            public void onSampleStarting(SeekBarVolumizer sbv) {
                if (mCallback != null) {
            	    /*
		     * AMAX ++++
                     * Add Request Audio Focus when user start to adjust volume preference
                     */
                    getAudioFocus();
	            // AMAX ++++
                    mCallback.onSampleStarting(sbv);
                }
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
                if (mCallback != null) {
                    mCallback.onStreamValueChanged(mStream, progress);
                }
            }
            @Override
            public void onMuted(boolean muted, boolean zenMuted) {
                if (mMuted == muted && mZenMuted == zenMuted) return;
                mMuted = muted;
                mZenMuted = zenMuted;
                updateIconView();
            }
        };
        final Uri sampleUri = mStream == AudioManager.STREAM_MUSIC ? getMediaVolumeUri() : null;
        if (mVolumizer == null) {
            mVolumizer = new SeekBarVolumizer(getContext(), mStream, sampleUri, sbvc);
        }
        mVolumizer.start();
        mVolumizer.setSeekBar(mSeekBar);
        updateIconView();
        mCallback.onStreamValueChanged(mStream, mSeekBar.getProgress());
        updateTitleText();
        updateSuppressionText();
        if (!isEnabled()) {
            mSeekBar.setEnabled(false);
            mVolumizer.stop();
        }
    }

    // during initialization, this preference is the SeekBar listener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {
        super.onProgressChanged(seekBar, progress, fromTouch);
        mCallback.onStreamValueChanged(mStream, progress);
    }

    private void updateIconView() {
        if (mIconView == null) return;
        // ++++ Morphy@2016/03/23 fix gating bug TT-755178 ++++
        int volume = mAudioManager.getStreamVolume(mStream);
        if (DEBUG) {
            Log.d(TAG, "updateIconView stream:" + mStream + " mMuted:" + mMuted + " mZenMuted:" + mZenMuted + " CurrentVolume:" + volume);
        }
        if (mIconResId != 0) {
            mIconView.setImageResource(mIconResId);
        } else if (volume > 0) {
            mIconView.setImageDrawable(getIcon());
        } else if (mMuteIconResId != 0 && (mMuted || mZenMuted)) {
            mIconView.setImageResource(mMuteIconResId);
        } else {
            mIconView.setImageDrawable(getIcon());
        }
	// ------- Morphy@2016/03/23 fix gating bug TT-755178 -------
    }

    public void showIcon(int resId) {
        // Instead of using setIcon, which will trigger listeners, this just decorates the
        // preference temporarily with a new icon.
        if (mIconResId == resId) return;
        mIconResId = resId;
        updateIconView();
    }

    public void setMuteIcon(int resId) {
        if (mMuteIconResId == resId) return;
        mMuteIconResId = resId;
        updateIconView();
    }

    public void setTitleText(CharSequence text) {
        mTitleText = text;
    }

    private void updateTitleText() {
        if(mTitleTextView != null && mSeekBar != null && mTitleText != null){
           mTitleTextView.setText(mTitleText);
        }
    }

    private Uri getMediaVolumeUri() {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + getContext().getPackageName()
                + "/" + R.raw.media_volume);
    }

    public void setSuppressionText(String text) {
        if (Objects.equals(text, mSuppressionText)) return;
        mSuppressionText = text;
        updateSuppressionText();
    }

    private void updateSuppressionText() {
        if (mSuppressionTextView != null && mSeekBar != null) {
            mSuppressionTextView.setText(mSuppressionText);
            final boolean showSuppression = !TextUtils.isEmpty(mSuppressionText);
            mSuppressionTextView.setVisibility(showSuppression ? View.VISIBLE : View.INVISIBLE);
            mSeekBar.setVisibility(showSuppression ? View.INVISIBLE : View.VISIBLE);
        }
    }

    public interface Callback {
        void onSampleStarting(SeekBarVolumizer sbv);
        void onStreamValueChanged(int stream, int progress);
    }

    /*
     * AMAX ++++ 
     * ADD RequestAudioFocus for VolumePreference Dialog
    */
    private void getAudioFocus() {
        if(!mHasAudioFocus) {
            mAudioManager.requestAudioFocus(this, mStream, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            mHasAudioFocus = true;
        }
    }

    /*
     * Add abandonAudioFocus for volumePreference Dialog
    */
    private void abandonAudioFocus() {
        if(mHasAudioFocus) {
            mAudioManager.abandonAudioFocus(this);
            mHasAudioFocus = false;
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        // TODO Auto-generated method stub
        // abandon audio focus
        abandonAudioFocus();
    }	
    // AMAX ++++
	
	//+++ Leaon_Wang
	public void setDivider(boolean isDivider){
		mIsDivider = isDivider;
		notifyChanged();
	}

}
