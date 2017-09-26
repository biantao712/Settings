/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.Telephony;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;

public class ApnPreference extends Preference implements
        CompoundButton.OnCheckedChangeListener, OnClickListener {
    final static String TAG = "ApnPreference";

    public ApnPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.cnasusres_preference_apn_item);
    }

    public ApnPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.apnPreferenceStyle);
    }

    public ApnPreference(Context context) {
        this(context, null);
    }

    private static String mSelectedKey = null;
    private static CompoundButton mCurrentChecked = null;
    private boolean mProtectFromCheckedChange = false;
    private boolean mSelectable = true;
    // +++ AMAX @ 20170119 7.1.1 Porting
    private boolean mApnReadOnly = false;
    // --- AMAX @ 20170119 7.1.1 Porting

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        View widget = view.findViewById(R.id.apn_radiobutton);
        if ((widget != null) && widget instanceof RadioButton) {
            RadioButton rb = (RadioButton) widget;
            if (mSelectable) {
                rb.setOnCheckedChangeListener(this);

                boolean isChecked = getKey().equals(mSelectedKey);
                if (isChecked) {
                    mCurrentChecked = rb;
                    mSelectedKey = getKey();
                }

                mProtectFromCheckedChange = true;
                rb.setChecked(isChecked);
                mProtectFromCheckedChange = false;
                rb.setVisibility(View.VISIBLE);
            } else {
                rb.setVisibility(View.GONE);
            }
        }

        View textLayout = view.findViewById(R.id.text_layout);
        if ((textLayout != null) && textLayout instanceof RelativeLayout) {
            textLayout.setOnClickListener(this);
        }
    }

    public boolean isChecked() {
        return getKey().equals(mSelectedKey);
    }

    public void setChecked() {
        mSelectedKey = getKey();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.i(TAG, "ID: " + getKey() + " :" + isChecked);
        if (mProtectFromCheckedChange) {
            return;
        }

        if (isChecked) {
            if (mCurrentChecked != null) {
                mCurrentChecked.setChecked(false);
            }
            mCurrentChecked = buttonView;
            mSelectedKey = getKey();
            callChangeListener(mSelectedKey);
        } else {
            mCurrentChecked = null;
            mSelectedKey = null;
        }
    }

    public void onClick(android.view.View v) {
        if ((v != null) && (R.id.text_layout == v.getId())) {
            // +++ Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
            String title = (getTitle() != null)? getTitle().toString() : "";
            Log.d(TAG, "onClick(): apn title = " + title);
            boolean isNotAllowedEditApn = AsusTelephonyUtils.isVerizon() && AsusTelephonyUtils.isVerizonSim() &&
                    !AsusTelephonyUtils.isVerizonClass3Apn(title) &&
                    !(SystemProperties.getInt("persist.radio.for_verizon_test", 0) == 1);
            if (isNotAllowedEditApn) {
                Log.d(TAG, "onClick(): Not allowed to modify apn setting title = " + getTitle() + ", summary = " + getSummary());
                return;
            }
            // --- Mark_Huang@20151119: Verizon VZ_REQ_LTEDATA_6742
            Context context = getContext();
            if (context != null) {
                int pos = Integer.parseInt(getKey());
                Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
                // +++ AMAX @ 20170119 7.1.1 Porting
                Intent intent = new Intent(Intent.ACTION_EDIT, url);
                intent.putExtra("DISABLE_EDITOR", mApnReadOnly);
                context.startActivity(intent);
                // --- AMAX @ 20170119 7.1.1 Porting
            }
        }
    }

    public void setSelectable(boolean selectable) {
        mSelectable = selectable;
    }

    public boolean getSelectable() {
        return mSelectable;
    }
// +++ AMAX @ 20170119 7.1.1 Porting
    public void setApnReadOnly(boolean apnReadOnly) {
        mApnReadOnly = apnReadOnly;
    }

    // +++ Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
    public void resetSelectKey(){
        Log.d(TAG,"resetSelectKey()");
        mCurrentChecked = null;
        mSelectedKey = null;
    }
    // --- Millie_Chang@201160817 :  reset default-related variable if default apn is deleted
// --- AMAX @ 20170119 7.1.1 Porting
}
