/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.applications;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.voice.VoiceInteractionService;
import android.service.voice.VoiceInteractionServiceInfo;
import android.speech.RecognitionService;
import android.support.v7.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.app.AssistUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.List;
import com.android.internal.logging.MetricsProto.MetricsEvent;


public class CNDefaultAssistFragment extends CNDefaultAppFragmentBase {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHelper = CNDefaultAssistHelper.getInstance(getActivity());
    }

    @Override
    public void onRadioButtonClicked(CNAppListPreference emiter) {

        if (!emiter.isChecked()) {
            String newAssitPackage = (String) emiter.getValue();
            if (newAssitPackage == null ||
                    newAssitPackage.contentEquals(mHelper.ITEM_NONE_VALUE)) {
                updateRidioButtonState(newAssitPackage);
                ((CNDefaultAssistHelper)mHelper).setDefaultAssist(mHelper.ITEM_NONE_VALUE);
                return;
            }

            String appLabel = (String)emiter.getTitle();
            confirmNewAssist(newAssitPackage, appLabel);
        }
    }

    private void confirmNewAssist(final String newAssitPackage, String appLabel) {

        final String title = getString(R.string.hint);
        final String message = getString(R.string.assistant_security_hint, appLabel);

        final DialogInterface.OnClickListener onAgree = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                updateRidioButtonState(newAssitPackage);
                ((CNDefaultAssistHelper)mHelper).setDefaultAssist(newAssitPackage);
            }
        };


        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(R.string.okay, onAgree);
        builder.setNegativeButton(R.string.dlg_cancel, null);
        builder.setCancelable(true);
        View view1 = LayoutInflater.from(getActivity()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
        TextView titleTv = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
        titleTv.setText(title);
        TextView messageTv = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
        messageTv.setText(message);

        builder.setView(view1);
        AlertDialog dialog = builder.show();

        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.BOTTOM;
        dialogWindow.setAttributes(lp);
    }

}
