/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.net.NetworkPolicy;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.telephony.TelephonyManager;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.AsusTelephonyUtils;
import com.android.settings.R;
import com.android.settingslib.NetworkPolicyEditor;
import com.android.settingslib.net.DataUsageController;

import static android.net.NetworkPolicy.LIMIT_DISABLED;
import static android.net.NetworkPolicy.WARNING_DISABLED;
import static android.net.TrafficStats.GB_IN_BYTES;
import static android.net.TrafficStats.MB_IN_BYTES;

public class BillingCycleSettings extends DataUsageBase implements
        Preference.OnPreferenceChangeListener, DataUsageEditController {

    private static final String TAG = "BillingCycleSettings";
    private static final boolean LOGD = false;

    private static final String TAG_CONFIRM_LIMIT = "confirmLimit";
    private static final String TAG_CYCLE_EDITOR = "cycleEditor";
    private static final String TAG_WARNING_EDITOR = "warningEditor";

    private static final String KEY_BILLING_CYCLE = "billing_cycle";
    private static final String KEY_SET_DATA_WARNING = "set_data_warning";
    private static final String KEY_DATA_WARNING = "data_warning";
    private static final String KEY_SET_DATA_LIMIT = "set_data_limit";
    private static final String KEY_DATA_LIMIT = "data_limit";

    // +++ ckenken (ChiaHsiang_Kuo) @ 20161019 Add data warning switch VZ_REQ_UI_15710
    private static final String DATA_USAGE_WARNING_AT_LIMIT_KEY = "data_usage_warning_at_limit_key";
    private static final String TAG_CONFIRM_WARNING = "confirmWarning";
    // --- ckenken (ChiaHsiang_Kuo) @ 20161019 Add data warning switch VZ_REQ_UI_15710

    // +++ ckenken (ChiaHsiang_Kuo) @ 20161129 TT-915517 key for save cycle day
    private static final String KEY_CYCLE_DAY = "cycle_day";
    // --- ckenken (ChiaHsiang_Kuo) @ 20161129 TT-915517 key for save cycle day

    // +++ ckenken (ChiaHsiang_Kuo) @ 20170111 VZ_REQ_UI_15706
    public static final String DEFAULT_LIMIT_LEVEL = "5.0";
    public static final String DEFAULT_WARNING_LEVEL = "2.0";
    // --- ckenken (ChiaHsiang_Kuo) @ 20170111 VZ_REQ_UI_15706

    private NetworkTemplate mNetworkTemplate;
    private Preference mBillingCycle;
    private Preference mDataWarning;
    private SwitchPreference mEnableDataWarning;
    private SwitchPreference mEnableDataLimit;
    private Preference mDataLimit;
    private DataUsageController mDataUsageController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mDataUsageController = new DataUsageController(getContext());

        Bundle args = getArguments();
        mNetworkTemplate = args.getParcelable(DataUsageList.EXTRA_NETWORK_TEMPLATE);

        addPreferencesFromResource(R.xml.billing_cycle);
        mBillingCycle = findPreference(KEY_BILLING_CYCLE);
        mEnableDataWarning = (SwitchPreference) findPreference(KEY_SET_DATA_WARNING);
        mEnableDataWarning.setOnPreferenceChangeListener(this);
        mDataWarning = findPreference(KEY_DATA_WARNING);
        mEnableDataLimit = (SwitchPreference) findPreference(KEY_SET_DATA_LIMIT);
        mEnableDataLimit.setOnPreferenceChangeListener(this);
        // +++ ckenken (ChiaHsiang_Kuo) @ 20170112 VZ_REQ_UI_15706 change preference title
        if (AsusTelephonyUtils.isVerizon()) {
            mEnableDataLimit.setTitle(R.string.vzw_set_data_limit);
            mEnableDataWarning.setTitle(R.string.vzw_data_usage_mobile_data_warning_2017);
            mDataWarning.setTitle(R.string.vzw_data_warning);
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20170112 VZ_REQ_UI_15706 change preference title
        mDataLimit = findPreference(KEY_DATA_LIMIT);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePrefs();
    }

    private void updatePrefs() {
        NetworkPolicy policy = services.mPolicyEditor.getPolicy(mNetworkTemplate);
        mBillingCycle.setSummary(getString(R.string.billing_cycle_summary, policy != null ?
                policy.cycleDay : 1));
        if (policy != null && policy.warningBytes != WARNING_DISABLED) {
            mDataWarning.setSummary(Formatter.formatFileSize(getContext(), policy.warningBytes));
            mDataWarning.setEnabled(true);
            mEnableDataWarning.setChecked(true);
        } else {
            mDataWarning.setSummary(null);
            mDataWarning.setEnabled(false);
            mEnableDataWarning.setChecked(false);
        }
        if (policy != null && policy.limitBytes != LIMIT_DISABLED) {
            mDataLimit.setSummary(Formatter.formatFileSize(getContext(), policy.limitBytes));
            mDataLimit.setEnabled(true);
            mEnableDataLimit.setChecked(true);
        } else {
            mDataLimit.setSummary(null);
            mDataLimit.setEnabled(false);
            mEnableDataLimit.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mBillingCycle) {
            CycleEditorFragment.show(this);
            return true;
        } else if (preference == mDataWarning) {
            BytesEditorFragment.show(this, false);
            return true;
        } else if (preference == mDataLimit) {
            BytesEditorFragment.show(this, true);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mEnableDataLimit == preference) {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                // +++ ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
                ConfirmLimitFragment.setEnableLimitSwitch(mEnableDataLimit);
                // --- ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
                ConfirmLimitFragment.show(this);
            } else {
                setPolicyLimitBytes(LIMIT_DISABLED);
            }
            return true;
        } else if (mEnableDataWarning == preference) {
            boolean enabled = (Boolean) newValue;
            if (enabled) {
                // +++ AMAX @ 20170119 7.1.1 Porting
                if (AsusTelephonyUtils.isVerizon()) {
                    ConfirmWarningFragment.show(this);
                } else {
                    setPolicyWarningBytes(mDataUsageController.getDefaultWarningLevel());
                }
                // --- AMAX @ 20170119 7.1.1 Porting
            } else {
                setPolicyWarningBytes(WARNING_DISABLED);
            }
            return true;
        }
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.BILLING_CYCLE;
    }

    private void setPolicyLimitBytes(long limitBytes) {
        if (LOGD) Log.d(TAG, "setPolicyLimitBytes()");
        services.mPolicyEditor.setPolicyLimitBytes(mNetworkTemplate, limitBytes);
        updatePrefs();
    }

    private void setPolicyWarningBytes(long warningBytes) {
        if (LOGD) Log.d(TAG, "setPolicyWarningBytes()");
        services.mPolicyEditor.setPolicyWarningBytes(mNetworkTemplate, warningBytes);
        updatePrefs();
    }

    @Override
    public NetworkPolicyEditor getNetworkPolicyEditor() {
        return services.mPolicyEditor;
    }

    @Override
    public NetworkTemplate getNetworkTemplate() {
        return mNetworkTemplate;
    }

    @Override
    public void updateDataUsage() {
        updatePrefs();
    }

    /**
     * Dialog to edit {@link NetworkPolicy#warningBytes}.
     */
    public static class BytesEditorFragment extends DialogFragment
            implements DialogInterface.OnClickListener {
        private static final String EXTRA_TEMPLATE = "template";
        private static final String EXTRA_LIMIT = "limit";
        private View mView;

        public static void show(DataUsageEditController parent, boolean isLimit) {
            if (!(parent instanceof Fragment)) {
                return;
            }
            Fragment targetFragment = (Fragment) parent;
            if (!targetFragment.isAdded()) {
                return;
            }

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.getNetworkTemplate());
            args.putBoolean(EXTRA_LIMIT, isLimit);

            final BytesEditorFragment dialog = new BytesEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(targetFragment, 0);
            dialog.show(targetFragment.getFragmentManager(), TAG_WARNING_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final LayoutInflater dialogInflater = LayoutInflater.from(context);
            final boolean isLimit = getArguments().getBoolean(EXTRA_LIMIT);
            mView = dialogInflater.inflate(R.layout.data_usage_bytes_editor, null, false);
            setupPicker((EditText) mView.findViewById(R.id.bytes),
                    (Spinner) mView.findViewById(R.id.size_spinner));
            return new AlertDialog.Builder(context)
                    .setTitle(isLimit ? R.string.data_usage_limit_editor_title
                            : R.string.data_usage_warning_editor_title)
                    .setView(mView)
                    .setPositiveButton(R.string.data_usage_cycle_editor_positive, this)
                    .create();
        }

        private void setupPicker(EditText bytesPicker, Spinner type) {
            final DataUsageEditController target = (DataUsageEditController) getTargetFragment();
            final NetworkPolicyEditor editor = target.getNetworkPolicyEditor();

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final boolean isLimit = getArguments().getBoolean(EXTRA_LIMIT);
            final long bytes = isLimit ? editor.getPolicyLimitBytes(template)
                    : editor.getPolicyWarningBytes(template);
            final long limitDisabled = isLimit ? LIMIT_DISABLED : WARNING_DISABLED;

            if (bytes > 1.5f * GB_IN_BYTES) {
                final String bytesText = formatText(bytes / (float) GB_IN_BYTES);
                bytesPicker.setText(bytesText);
                bytesPicker.setSelection(0, bytesText.length());

                type.setSelection(1);
            } else {
                final String bytesText = formatText(bytes / (float) MB_IN_BYTES);
                bytesPicker.setText(bytesText);
                bytesPicker.setSelection(0, bytesText.length());

                type.setSelection(0);
            }
        }

        private String formatText(float v) {
            v = Math.round(v * 100) / 100f;
            return String.valueOf(v);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
                return;
            }
            final DataUsageEditController target = (DataUsageEditController) getTargetFragment();
            final NetworkPolicyEditor editor = target.getNetworkPolicyEditor();

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final boolean isLimit = getArguments().getBoolean(EXTRA_LIMIT);
            EditText bytesField = (EditText) mView.findViewById(R.id.bytes);
            Spinner spinner = (Spinner) mView.findViewById(R.id.size_spinner);

            String bytesString = bytesField.getText().toString();
            if (bytesString.isEmpty()) {
                bytesString = "0";
            }
            final long bytes = (long) (Float.valueOf(bytesString)
                    * (spinner.getSelectedItemPosition() == 0 ? MB_IN_BYTES : GB_IN_BYTES));
            if (isLimit) {
                editor.setPolicyLimitBytes(template, bytes);
            } else {
                editor.setPolicyWarningBytes(template, bytes);
            }
            target.updateDataUsage();
        }
    }

    /**
     * Dialog to edit {@link NetworkPolicy#cycleDay}.
     */
    public static class CycleEditorFragment extends DialogFragment implements
            DialogInterface.OnClickListener {
        private static final String EXTRA_TEMPLATE = "template";
        private NumberPicker mCycleDayPicker;

        public static void show(BillingCycleSettings parent) {
            if (!parent.isAdded()) return;

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mNetworkTemplate);

            final CycleEditorFragment dialog = new CycleEditorFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CYCLE_EDITOR);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final DataUsageEditController target = (DataUsageEditController) getTargetFragment();
            final NetworkPolicyEditor editor = target.getNetworkPolicyEditor();

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

            final View view = dialogInflater.inflate(R.layout.data_usage_cycle_editor, null, false);
            mCycleDayPicker = (NumberPicker) view.findViewById(R.id.cycle_day);

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            // +++ ckenken (ChiaHsiang_Kuo) @ 20161129 TT-915517 restore cycle day if stored
            int cycleDay = -1;
            if (null != savedInstanceState && savedInstanceState.getInt(KEY_CYCLE_DAY, -1) != -1) {
                cycleDay = savedInstanceState.getInt(KEY_CYCLE_DAY);
                Log.d(TAG, "save day = " + savedInstanceState.getInt(KEY_CYCLE_DAY));
            } else {
                cycleDay = editor.getPolicyCycleDay(template);
            }
            Log.d(TAG, "cycle day = " + cycleDay);
            // --- ckenken (ChiaHsiang_Kuo) @ 20161129 TT-915517 restore cycle day if stored

            mCycleDayPicker.setMinValue(1);
            mCycleDayPicker.setMaxValue(31);
            mCycleDayPicker.setValue(cycleDay);
            mCycleDayPicker.setWrapSelectorWheel(true);

            return builder.setTitle(R.string.data_usage_cycle_editor_title)
                    .setView(view)
                    .setPositiveButton(R.string.data_usage_cycle_editor_positive, this)
                    .create();
        }
        // +++ ckenken (ChiaHsiang_Kuo) @ 20161129 TT-915517 save cycle day before reCreate
        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            Log.d(TAG, "onSaveInstanceState()");
            outState.putInt(KEY_CYCLE_DAY, mCycleDayPicker.getValue());
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20161129 TT-915517 save cycle day before reCreate

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final DataUsageEditController target = (DataUsageEditController) getTargetFragment();
            final NetworkPolicyEditor editor = target.getNetworkPolicyEditor();

            // clear focus to finish pending text edits
            mCycleDayPicker.clearFocus();

            final int cycleDay = mCycleDayPicker.getValue();
            final String cycleTimezone = new Time().timezone;
            editor.setPolicyCycleDay(template, cycleDay, cycleTimezone);
            target.updateDataUsage();
        }
    }

    /**
     * Dialog to request user confirmation before setting
     * {@link NetworkPolicy#limitBytes}.
     */
    public static class ConfirmLimitFragment extends DialogFragment implements
            DialogInterface.OnClickListener {
        private static final String EXTRA_MESSAGE = "message";
        private static final String EXTRA_LIMIT_BYTES = "limitBytes";
        // +++ ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
        private static final String EXTRA_TEMPLATE = "template";
        private static final String EXTRA_IS_LIMIT = "limit";
        // --- ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
        public static final float FLOAT = 1.2f;
        // +++ ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
        private static SwitchPreference sEnableLimitSwitch;
        // --- ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N

        public static void show(BillingCycleSettings parent) {
            if (!parent.isAdded()) return;

            final NetworkPolicy policy = parent.services.mPolicyEditor
                    .getPolicy(parent.mNetworkTemplate);
            if (policy == null) return;

            final Resources res = parent.getResources();
            final CharSequence message;
            final long minLimitBytes = (long) (policy.warningBytes * FLOAT);
            final long limitBytes;

            // TODO: customize default limits based on network template
            message = res.getString(R.string.asus_data_usage_limit_dialog_mobile);
            limitBytes = Math.max(5 * GB_IN_BYTES, minLimitBytes);

            final Bundle args = new Bundle();
            args.putCharSequence(EXTRA_MESSAGE, message);
            args.putLong(EXTRA_LIMIT_BYTES, limitBytes);

            // +++ ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
            if (AsusTelephonyUtils.isVerizon()) {
                args.putParcelable(EXTRA_TEMPLATE, parent.mNetworkTemplate);
                args.putBoolean(EXTRA_IS_LIMIT, true);
            }
            // --- ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N

            final ConfirmLimitFragment dialog = new ConfirmLimitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_LIMIT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            // +++ ckenken (ChiaHsiang_Kuo) @ 20170119 prevent exception
            if (null == context) {
                return null;
            }
            // --- ckenken (ChiaHsiang_Kuo) @ 20170119 prevent exception
            final CharSequence message = getArguments().getCharSequence(EXTRA_MESSAGE);

            // +++ ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
            if (AsusTelephonyUtils.isVerizon()) {
                final View confirmLimitDialogLayoutView = getActivity().getLayoutInflater().inflate(R.layout.confirm_limit_dialog_layout, null);
                final TextView textViewTop = (TextView) confirmLimitDialogLayoutView.findViewById(R.id.confirmLimitTextView_top);
                final EditText bytesEditText = (EditText) confirmLimitDialogLayoutView.findViewById(R.id.limit_data_bytes);
                final Spinner confirmAlertSpinner = (Spinner) confirmLimitDialogLayoutView.findViewById(R.id.limit_data_size_spinner);
                final TextView textViewBottom = (TextView) confirmLimitDialogLayoutView.findViewById(R.id.confirmLimitTextView_bottom);
                textViewTop.setText(R.string.vzw_data_usage_limit_dialog_message_top_2016_Nov);
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                boolean isVoiceCapable = telephonyManager.isVoiceCapable();
                textViewBottom.setText(String.format(getString(R.string.vzw_data_usage_limit_dialog_message_bottom_2016_Nov), (isVoiceCapable) ? getString(R.string.vzw_phone) : getString(R.string.vzw_tablet)));
                setupPicker(bytesEditText, confirmAlertSpinner);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(confirmLimitDialogLayoutView);
                builder.setTitle(R.string.data_usage_limit_dialog_title);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String bytesString = bytesEditText.getText().toString();
                        if (bytesString.isEmpty()) {
                            bytesString = "0";
                        }
                        final long bytes = (long) (Float.valueOf(bytesString) * (confirmAlertSpinner.getSelectedItemPosition() == 0 ? MB_IN_BYTES : GB_IN_BYTES));
                        final BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
                        if (target != null) {
                            target.setPolicyLimitBytes(bytes);
                        }
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, this);
                return builder.create();
            } else {
                return new AlertDialog.Builder(context)
                        .setTitle(R.string.data_usage_limit_dialog_title)
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, this)
                        .setNegativeButton(android.R.string.cancel, this)
                        .create();
            }
            // --- ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            // +++ ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
            Log.d(TAG, "ConfirmLimitFragment onClick which = " + which);
            if (which != DialogInterface.BUTTON_POSITIVE) {
                if (null != sEnableLimitSwitch) {
                    sEnableLimitSwitch.setChecked(false);
                }
                return;
            }
            // --- ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
            final long limitBytes = getArguments().getLong(EXTRA_LIMIT_BYTES);
            final BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            if (target != null) {
                target.setPolicyLimitBytes(limitBytes);
            }
        }

        // +++ ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
        public static void setEnableLimitSwitch(SwitchPreference inputSwitch) {
            sEnableLimitSwitch = inputSwitch;
        }

        public static SwitchPreference getsEnableLimitSwitch() {
            return sEnableLimitSwitch;
        }

        private String formatText(float v) {
            v = Math.round(v * 100) / 100f;
            return String.valueOf(v);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (null != sEnableLimitSwitch) {
                sEnableLimitSwitch.setChecked(false);
            }
        }

        private void setupPicker(EditText bytesPicker, Spinner type) {
            final BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            final NetworkPolicyEditor editor = target.services.mPolicyEditor;

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final boolean isLimit = getArguments().getBoolean(EXTRA_IS_LIMIT);
            final long bytes = isLimit ? editor.getPolicyLimitBytes(template)
                    : editor.getPolicyWarningBytes(template);
            // +++ ckenken (ChiaHsiang_Kuo) @ 20170111 VZ_REQ_UI_15706
            if (AsusTelephonyUtils.isVerizon()) {
                if (bytes <= 0) {
                    bytesPicker.setText(DEFAULT_LIMIT_LEVEL);
                    type.setSelection(1);
                } else if (bytes > 1.5f * GB_IN_BYTES) {
                    bytesPicker.setText(formatText(bytes / (float) GB_IN_BYTES));
                    type.setSelection(1);
                } else {
                    bytesPicker.setText(formatText(bytes / (float) MB_IN_BYTES));
                    type.setSelection(0);
                }
            // --- ckenken (ChiaHsiang_Kuo) @ 20170111 VZ_REQ_UI_15706
            } else {
                if (bytes > 1.5f * GB_IN_BYTES) {
                    bytesPicker.setText(formatText(bytes / (float) GB_IN_BYTES));
                    type.setSelection(1);
                } else {
                    bytesPicker.setText(formatText(bytes / (float) MB_IN_BYTES));
                    type.setSelection(0);
                }
            }
        }
        // --- ckenken (ChiaHsiang_Kuo) @ 20161206 VZ_REQ_UI_15711 in Android N
    }
// +++ AMAX @ 20170119 7.1.1 Porting
    // +++ ckenken: Verizon VZ_REQ_UI_15706 VZ_REQ_UI_15710
    /**
     * Dialog to request user confirmation before setting
     * {@link NetworkPolicy#warningBytes}.
     */
    public static class ConfirmWarningFragment extends DialogFragment {
        private static final String EXTRA_WARNING_BYTES = "warningBytes";
        private static final String EXTRA_TEMPLATE = "template";
        private static final String EXTRA_LIMIT = "limit";

        public static void show(BillingCycleSettings parent) {
            if (!parent.isAdded()) return;

            final NetworkPolicy policy = parent.services.mPolicyEditor.getPolicy(parent.mNetworkTemplate);
            if (policy == null) return;

            Log.d(TAG, "ConfirmWarningFragment: show confirm warning dialog");

            final Bundle args = new Bundle();
            args.putParcelable(EXTRA_TEMPLATE, parent.mNetworkTemplate);
            args.putBoolean(EXTRA_LIMIT, false);

            final ConfirmWarningFragment dialog = new ConfirmWarningFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_CONFIRM_WARNING);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();

            final Resources res = context.getResources();
            android.telephony.TelephonyManager telephonyManager = (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            boolean isVoiceCapable = telephonyManager.isVoiceCapable();
            CharSequence message = res.getString(R.string.vzw_data_usage_warning_dialog_message_2016_Nov,
                    (isVoiceCapable)? res.getString(R.string.vzw_phone) : res.getString(R.string.vzw_tablet));

            final View confirmAlertDialogLayoutView = getActivity().getLayoutInflater().inflate(R.layout.confirm_alert_dialog_layout, null);
            final TextView textView = (TextView) confirmAlertDialogLayoutView.findViewById(R.id.confirmAlertTextView);
            final EditText bytesEditText = (EditText) confirmAlertDialogLayoutView.findViewById(R.id.alert_data_bytes);
            final Spinner confirmAlertSpinner = (Spinner) confirmAlertDialogLayoutView.findViewById(R.id.alert_data_size_spinner);
            textView.setText(message);
            setupPicker(bytesEditText, confirmAlertSpinner);

            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(confirmAlertDialogLayoutView);
            builder.setTitle(R.string.vzw_data_usage_warning_dialog_title);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
                    if (target != null) {
                        String bytesString = bytesEditText.getText().toString();
                        if (bytesString.isEmpty()) {
                            bytesString = "0";
                        }
                        final long bytes = (long) (Float.valueOf(bytesString) * (confirmAlertSpinner.getSelectedItemPosition() == 0 ? MB_IN_BYTES : GB_IN_BYTES));
                        target.setPolicyWarningBytes(bytes);
                    }
                }
            });

            return builder.create();
        }

        private String formatText(float v) {
            v = Math.round(v * 100) / 100f;
            return String.valueOf(v);
        }

        private void setupPicker(EditText bytesPicker, Spinner type) {
            final BillingCycleSettings target = (BillingCycleSettings) getTargetFragment();
            final NetworkPolicyEditor editor = target.services.mPolicyEditor;

            final NetworkTemplate template = getArguments().getParcelable(EXTRA_TEMPLATE);
            final boolean isLimit = getArguments().getBoolean(EXTRA_LIMIT);
            final long bytes = isLimit ? editor.getPolicyLimitBytes(template)
                    : editor.getPolicyWarningBytes(template);
            final long limitDisabled = isLimit ? LIMIT_DISABLED : WARNING_DISABLED;

            if (bytes <= 0) {
                bytesPicker.setText(DEFAULT_WARNING_LEVEL);
                type.setSelection(1);
            } else if (bytes > 1.5f * GB_IN_BYTES) {
                bytesPicker.setText(formatText(bytes / (float) GB_IN_BYTES));
                type.setSelection(1);
            } else {
                bytesPicker.setText(formatText(bytes / (float) MB_IN_BYTES));
                type.setSelection(0);
            }
        }

    }
    // --- ckenken: Verizon VZ_REQ_UI_15706 VZ_REQ_UI_15710
// --- AMAX @ 20170119 7.1.1 Porting
}
