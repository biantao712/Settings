/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.settings.resetSystemSettings.ResetSystemSettingsIntentService;

public class ResetSettingsConfirm extends InstrumentedFragment {
    private static final String TAG = "ResetSettingsConfirm";
    private static final boolean DEBUG = false;

    @Override
    protected int getMetricsCategory() {
        return RESET_SETTINGS_CONFIRM;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        // Check again (ResetSettings has done it)
        if (Process.myUserHandle().isOwner()) {
            //UserHandle.getCallingUserHandle().isOwner();
            View contentView = inflater.inflate(R.layout.reset_settings_confirm, null);
            contentView.findViewById(R.id.execute_reset_settings).setOnClickListener(
                    new Button.OnClickListener() {
                        public void onClick(View v) {
                            if (!Utils.isMonkeyRunning()) {
                                startToReset();
                            }
                        }
                    });
            return contentView;
        } else {
            return inflater.inflate(R.layout.reset_settings_disallowed_screen, null);
        }
    }

    private void startToReset() {
        Context context = getActivity();
        context.startService(new Intent(context, ResetSystemSettingsIntentService.class));
    }
}
