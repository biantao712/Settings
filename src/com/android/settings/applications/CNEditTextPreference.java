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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
//import android.preference.EditTextPreference;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.asus.cncommonres.AsusAlertDialogHelper;

/**
 * Ultra-simple subclass of EditTextPreference that allows the "title" to wrap onto multiple lines. (By default, the title of an EditTextPreference is singleLine="true"; see preference_holo.xml under frameworks/base. But in the "Respond via SMS" settings UI we want titles to be multi-line, since the customized messages might be fairly long, and should be able to wrap.) TODO: This is pretty cumbersome; it would be nicer for the framework to either allow modifying the title's attributes in XML, or at least provide some way from Java (given an EditTextPreference) to reach inside and get a handle to the "title" TextView. TODO: Also, it would reduce clutter if this could be an inner class in RespondViaSmsManager.java, but then there would be no way to reference the class from XML. That's because <com.android.phone.RespondViaSmsManager$MultiLineTitleEditTextPreference ... /> isn't valid XML syntax due to the "$" character. And Preference elements don't have a "class" attribute, so you can't do something like <view class="com.android.phone.Foo$Bar"> as you can with regular views.
 */
public class CNEditTextPreference extends EditTextPreference {

    private static final String TAG = "CNEditTextPreference";
    private EditText  mTextField;
    private TextView mTitle;
    private Context mContext;
    private String mKey = "";

    public CNEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.cnasusres_preference_with_status);
        mContext = context;
        mKey = getKey();
    }

    public CNEditTextPreference(Context context) {
        this(context, null);
    }

    private void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View view = LayoutInflater.from(builder.getContext()).inflate(R.layout.cnasusres_alertdialog_textfield_with_title, null);
        mTitle = (TextView) view.getRootView().findViewById(R.id.alertdialog_title);
        mTitle.setText(getDialogTitle());
        mTextField = (EditText) view.getRootView().findViewById(R.id.alertdialog_textfield);
        mTextField.setText(getText());
        mTextField.selectAll();
        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String text = mTextField.getText().toString();
                setText(text);
                if (mKey.equals("apn_password")) {
                    setSummary(ApnEditor.starify(text));
                } else {
                    setSummary(ApnEditor.checkNull(text));
                }
            }
        });
        builder.setNegativeButton(R.string.cancel,null);

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    @Override
    protected void onClick() {
        showDialog();
        //getPreferenceManager().showDialog(this);
    }
}
