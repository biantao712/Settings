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

package com.android.settings.nfc;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;

import java.util.List;

public final class PaymentDefaultDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

    public static final String TAG = "PaymentDefaultDialog";
    private static final int PAYMENT_APP_MAX_CAPTION_LENGTH = 40;

    private PaymentBackend mBackend;
    private ComponentName mNewDefault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //+++ tim_hu@asus.com
        if (!updateNFCTilesList()) {
            finish();
            return;
        }
        //---

        mBackend = new PaymentBackend(this);
        Intent intent = getIntent();
        ComponentName component = intent.getParcelableExtra(
                CardEmulation.EXTRA_SERVICE_COMPONENT);
        String category = intent.getStringExtra(CardEmulation.EXTRA_CATEGORY);

        setResult(RESULT_CANCELED);
        if (!buildDialog(component, category)) {
            finish();
        }

    }

    //+++ tim_hu@asus.com disable action receiver if nfc not available
    //exception cause by CTA test adb shell start action
    private boolean updateNFCTilesList(){
        PackageManager pm = getPackageManager();
        final UserManager um = UserManager.get(this);
        final boolean isAdmin = um.isAdminUser();
        String packageName = getPackageName();

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        boolean nfcEnable = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
                && pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)
                && adapter != null && adapter.isEnabled();

        setTileEnabled(new ComponentName(packageName,
                        PaymentDefaultDialog.class.getName()),
                nfcEnable, isAdmin, pm);
        setTileEnabled(new ComponentName(packageName,
                        Settings.AndroidBeamSettingsActivity.class.getName()),
                nfcEnable, isAdmin, pm);
        setTileEnabled(new ComponentName(packageName,
                        Settings.PaymentSettingsActivity.class.getName()),
                nfcEnable, isAdmin, pm);

        return nfcEnable;
    }

    private void setTileEnabled(ComponentName component, boolean enabled, boolean isAdmin, PackageManager pm) {
        pm.setComponentEnabledSetting(component, enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
    //---

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                mBackend.setDefaultPaymentApp(mNewDefault);
                setResult(RESULT_OK);
                break;
            case BUTTON_NEGATIVE:
                break;
        }
    }

    private boolean buildDialog(ComponentName component, String category) {
        if (component == null || category == null) {
            Log.e(TAG, "Component or category are null");
            return false;
        }

        if (!CardEmulation.CATEGORY_PAYMENT.equals(category)) {
            Log.e(TAG, "Don't support defaults for category " + category);
            return false;
        }

        // Check if passed in service exists
        PaymentAppInfo requestedPaymentApp = null;
        PaymentAppInfo defaultPaymentApp = null;

        List<PaymentAppInfo> services = mBackend.getPaymentAppInfos();
        for (PaymentAppInfo service : services) {
            if (component.equals(service.componentName)) {
                requestedPaymentApp = service;
            }
            if (service.isDefault) {
                defaultPaymentApp = service;
            }
        }

        if (requestedPaymentApp == null) {
            Log.e(TAG, "Component " + component + " is not a registered payment service.");
            return false;
        }

        // Get current mode and default component
        ComponentName defaultComponent = mBackend.getDefaultPaymentApp();
        if (defaultComponent != null && defaultComponent.equals(component)) {
            Log.e(TAG, "Component " + component + " is already default.");
            return false;
        }

        mNewDefault = component;
        // Compose dialog; get
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.nfc_payment_set_default_label);
        if (defaultPaymentApp == null) {
            String formatString = getString(R.string.nfc_payment_set_default);
            String msg = String.format(formatString,
                    sanitizePaymentAppCaption(requestedPaymentApp.label.toString()));
            p.mMessage = msg;
        } else {
            String formatString = getString(R.string.nfc_payment_set_default_instead_of);
            String msg = String.format(formatString,
                    sanitizePaymentAppCaption(requestedPaymentApp.label.toString()),
                    sanitizePaymentAppCaption(defaultPaymentApp.label.toString()));
            p.mMessage = msg;
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();

        return true;
    }

    private String sanitizePaymentAppCaption(String input) {
        String sanitizedString = input.replace('\n', ' ').replace('\r', ' ').trim();


        if (sanitizedString.length() > PAYMENT_APP_MAX_CAPTION_LENGTH) {
            return sanitizedString.substring(0, PAYMENT_APP_MAX_CAPTION_LENGTH);
        }

        return sanitizedString;
    }

}
