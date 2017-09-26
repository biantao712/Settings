/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;

import com.android.settings.R;

/**
 * Check box preference with check box replaced by radio button.
 *
 * Functionally speaking, it's actually a CheckBoxPreference. We only modified
 * the widget to RadioButton to make it "look like" a AccessibilityRadioButtonPreference.
 *
 * In other words, there's no "RadioButtonPreferenceGroup" in this
 * implementation. When you check one AccessibilityRadioButtonPreference, if you want to
 * uncheck all the other preferences, you should do that by code yourself.
 */
public class AccessibilityRadioButtonPreference extends CheckBoxPreference {
    public interface OnClickListener {
        public abstract void onRadioButtonClicked(AccessibilityRadioButtonPreference emiter);
    }

    private OnClickListener mListener = null;

    public AccessibilityRadioButtonPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setWidgetLayoutResource(R.layout.preference_widget_radiobutton);
    }

    public AccessibilityRadioButtonPreference(Context context, AttributeSet attrs) {
        this(context, attrs, com.android.internal.R.attr.checkBoxPreferenceStyle);
    }

    public AccessibilityRadioButtonPreference(Context context) {
        this(context, null);
    }

    void setOnClickListener(OnClickListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick() {
        if (mListener != null) {
            mListener.onRadioButtonClicked(this);
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
    }
}
