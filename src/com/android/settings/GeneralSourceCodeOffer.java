/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Config;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toolbar;

import static java.security.AccessController.getContext;

/**
 * The "dialog" that shows from "License" in the Settings app.
 */
public class GeneralSourceCodeOffer extends AppCompatActivity {
    private static final String TAG = "GeneralSourceCodeOffer";
    private static final boolean LOGV = false || Config.LOGV;

    Toolbar mToolbar;
    TextView mTitle;
    ImageView mActionBarBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.legal_info_view);

        mToolbar = (Toolbar) findViewById(R.id.action_bar);
        setActionBar(mToolbar);
        getActionBar().setDisplayHomeAsUpEnabled(false);
        getActionBar().setHomeButtonEnabled(false);
        getActionBar().setDisplayShowTitleEnabled(false);
        mToolbar.setContentInsetsRelative(0,0);
        mToolbar.setContentInsetsAbsolute(0,0);

        mTitle = (TextView) findViewById(R.id.toolbar_title);
        mTitle.setText(getString(R.string.general_sourcecode_offer_title));
        mActionBarBackButton = (ImageView)findViewById(R.id.action_bar_back);
        mActionBarBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(getColor(R.color.action_bar_background));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open("general_sourcecode_offer.html")))) {
            StringBuilder data = new StringBuilder(1024);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                data.append(line);
            }
            if (TextUtils.isEmpty(data)) {
                showErrorAndFinish();
            } else {
                TextView legalView = (TextView) findViewById(R.id.legal_info_view);
                legalView.setText(Html.fromHtml(data.toString()));
                legalView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        } catch (FileNotFoundException e) {
            showErrorAndFinish();
        } catch (IOException e) {
            showErrorAndFinish();
        }
    }

    private void showErrorAndFinish() {
        Toast.makeText(this, R.string.settings_license_activity_unavailable, Toast.LENGTH_LONG)
                .show();
        finish();
    }
}
