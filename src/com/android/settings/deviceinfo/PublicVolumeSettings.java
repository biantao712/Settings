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
 * limitations under the License.
 */

package com.android.settings.deviceinfo;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.provider.DocumentsContract;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.deviceinfo.StorageSettings.MountTask;
import com.android.settings.deviceinfo.StorageSettings.UnmountTask;

import java.io.File;
import java.util.Objects;

/**
 * Panel showing summary and actions for a {@link VolumeInfo#TYPE_PUBLIC}
 * storage volume.
 */
public class PublicVolumeSettings extends SettingsPreferenceFragment {
    // TODO: disable unmount when providing over MTP/PTP

    private StorageManager mStorageManager;

    private String mVolumeId;
    private VolumeInfo mVolume;
    private DiskInfo mDisk;

    private StorageSummaryPreference mSummary;

    private Preference mMount;
    private Preference mUnmount;
    private Preference mFormatPublic;
    private Preference mFormatPrivate;
//    private Button mUnmount;

    private boolean mIsPermittedToAdopt;

    private PreferenceCategory mStorageCategory;
    private boolean isVolumeValid() {
        return (mVolume != null) && (mVolume.getType() == VolumeInfo.TYPE_PUBLIC)
                && mVolume.isMountedReadable();
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_STORAGE;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mIsPermittedToAdopt = UserManager.get(context).isAdminUser()
                && !ActivityManager.isUserAMonkey();

        mStorageManager = context.getSystemService(StorageManager.class);

        if (DocumentsContract.ACTION_DOCUMENT_ROOT_SETTINGS.equals(
                getActivity().getIntent().getAction())) {
            final Uri rootUri = getActivity().getIntent().getData();
            final String fsUuid = DocumentsContract.getRootId(rootUri);
            mVolume = mStorageManager.findVolumeByUuid(fsUuid);
        } else {
            final String volId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
            mVolume = mStorageManager.findVolumeById(volId);
        }

        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }

        mDisk = mStorageManager.findDiskById(mVolume.getDiskId());
        Preconditions.checkNotNull(mDisk);

        mVolumeId = mVolume.getId();

        addPreferencesFromResource(R.xml.device_info_storage_volume);
        mStorageCategory = (PreferenceCategory) findPreference("storage_category");
        mStorageCategory.setTitle(context.getResources().getString(R.string.storage_internal_title));
        getPreferenceScreen().setOrderingAsAdded(true);

        mSummary = new StorageSummaryPreference(getPrefContext());

        mMount = buildAction(R.string.storage_menu_mount);
        //blenda, replace unmount button with preference, and remove private format
        mUnmount = buildAction(R.string.storage_menu_unmount);
//        mUnmount = new Button(getActivity());
//        mUnmount.setText(R.string.storage_menu_unmount);
//        mUnmount.setOnClickListener(mUnmountListener);
        mFormatPublic = buildAction(R.string.storage_menu_format);
//        if (mIsPermittedToAdopt) {
//            mFormatPrivate = buildAction(R.string.storage_menu_format_private);
//        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // unmount button has been removed, don't need any more
/*        if (null == mUnmount) return;

        final Resources resources = getResources();
        final int padding = resources.getDimensionPixelSize(
                R.dimen.unmount_button_padding);
        final ViewGroup buttonBar = getButtonBar();
        buttonBar.removeAllViews();
        buttonBar.setPadding(padding, padding, padding, padding);
        buttonBar.addView(mUnmount, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));*/
    }

    public void update() {
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }

        getActivity().setTitle(mStorageManager.getBestVolumeDescription(mVolume));

        final Context context = getActivity();
        final PreferenceScreen screen = getPreferenceScreen();

        screen.removeAll();

        screen.addPreference(mStorageCategory);
        if (mVolume.isMountedReadable()) {
            addPreference(mSummary);

            final File file = mVolume.getPath();
            final long totalBytes = file.getTotalSpace();
            final long freeBytes = file.getFreeSpace();
            final long usedBytes = totalBytes - freeBytes;

            final BytesResult result = Formatter.formatBytes(getResources(), freeBytes, 0);
//            mSummary.setTitle(Html.fromHtml(TextUtils.expandTemplate(getText(R.string.storage_size_large),
//                    result.value, result.units).toString()));
            final String used = Formatter.formatFileSize(context, usedBytes);
            final String free = Formatter.formatFileSize(context, freeBytes);
            final String total = Formatter.formatFileSize(context, totalBytes);
//            mSummary.setSummary(getString(R.string.storage_volume_summary, used, total));
            mSummary.setTitle(getString(R.string.cn_storage_volume_title, free, total));
            mSummary.setPercent((int) ((usedBytes * 100) / totalBytes));
        }

        if (mVolume.getState() == VolumeInfo.STATE_UNMOUNTED) {
            addPreference(mMount);
        }
        //blenda, replace unmount button with preference
/*        if (mVolume.isMountedReadable()) {
            getButtonBar().setVisibility(View.VISIBLE);
        }*/
        addPreference(mUnmount);
        addPreference(mFormatPublic);
/*        if (mDisk.isAdoptable() && mIsPermittedToAdopt) {
            addPreference(mFormatPrivate);
        }*/
    }

    private void addPreference(Preference pref) {
        pref.setOrder(Preference.DEFAULT_ORDER);
        getPreferenceScreen().addPreference(pref);
    }

    private Preference buildAction(int titleRes) {
        return buildAction(titleRes, false);
    }

    private Preference buildAction(int titleRes, boolean last) {
        final Preference pref = new Preference(getPrefContext());
        if (!last)
            pref.setLayoutResource(R.layout.cnasusres_preference_parent);
        else
            pref.setLayoutResource(R.layout.cnasusres_preference_parent_nodivider);

        pref.setTitle(titleRes);
        return pref;
    }
    @Override
    public void onResume() {
        super.onResume();

        // Refresh to verify that we haven't been formatted away
        mVolume = mStorageManager.findVolumeById(mVolumeId);
        if (!isVolumeValid()) {
            getActivity().finish();
            return;
        }

        mStorageManager.registerListener(mStorageListener);
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        final Context context = getActivity();
        if (pref == mMount) {
            new MountTask(context, mVolume).execute();
        } else if (pref == mFormatPublic) {
            final Intent intent = new Intent(context, CNStorageWizardFormatConfirm.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            intent.putExtra(CNStorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
            startActivity(intent);
        } else if (pref == mFormatPrivate) {
            //final Intent intent = new Intent(context, StorageWizardFormatConfirm.class);
            final Intent intent = new Intent(context, StorageWizardIntInit.class);
            intent.putExtra(DiskInfo.EXTRA_DISK_ID, mDisk.getId());
            //intent.putExtra(StorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, true);
            startActivity(intent);
        } else if (pref == mUnmount){
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setPositiveButton(R.string.lockpattern_confirm_button_text,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            new UnmountTask(context, mVolume).execute();
                        }
                    });
            builder.setNegativeButton(R.string.cancel, null);

            View view1 = LayoutInflater.from(builder.getContext()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
            TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
            title.setText(getActivity().getResources().getString(R.string.unmount_sdcard_dialog_title));
            TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
            String text = getActivity().getResources().getString(R.string.unmount_sdcard_dialog_content);
            message.setText(text);

            builder.setView(view1);
            AlertDialog alertDialog1 = builder.show();
//            alertDialog1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            Window dialogWindow = alertDialog1.getWindow();
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.BOTTOM;
            dialogWindow.setAttributes(lp);
        }

        return super.onPreferenceTreeClick(pref);
    }

    private final View.OnClickListener mUnmountListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new UnmountTask(getActivity(), mVolume).execute();
        }
    };

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (Objects.equals(mVolume.getId(), vol.getId())) {
                mVolume = vol;
                update();
            }
        }

        @Override
        public void onVolumeRecordChanged(VolumeRecord rec) {
            if (Objects.equals(mVolume.getFsUuid(), rec.getFsUuid())) {
                mVolume = mStorageManager.findVolumeById(mVolumeId);
                update();
            }
        }
    };
}
