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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.storage.VolumeRecord;
import android.support.annotation.NonNull;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceGroup;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.deviceinfo.utils.FlattenStorageLayout;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.drawer.SettingsDrawerActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

/**
 * Panel showing both internal storage (both built-in storage and private
 * volumes) and removable storage (public volumes).
 */
public class StorageSettings extends SettingsPreferenceFragment implements Indexable {
    static final String TAG = "StorageSettings";

    private static final String TAG_VOLUME_UNMOUNTED = "volume_unmounted";
    private static final String TAG_DISK_INIT = "disk_init";

    static final int COLOR_PUBLIC = Color.parseColor("#ff9e9e9e");
    static final int COLOR_WARNING = Color.parseColor("#fff4511e");

    static final int[] COLOR_PRIVATE = new int[] {
            Color.parseColor("#ff26a69a"),
            Color.parseColor("#ffab47bc"),
            Color.parseColor("#fff2a600"),
            Color.parseColor("#ffec407a"),
            Color.parseColor("#ffc0ca33"),
    };

    //+++ Asus emmc total size
    static public final String ID_SYSTEM_RESERVED = "asus_reserved";
    //--- Asus emmc total size

    private StorageManager mStorageManager;

    private PreferenceCategory mInternalCategory;
    private PreferenceCategory mExternalCategory;
    private PreferenceCategory mMemoryCategory;
    private PreferenceScreen mMemorysetting;
    private PreferenceCategory mStorageSetting;
    private PreferenceScreen mSpaceClear;

    private StorageSummaryPreference mInternalSummary;
    private static long sTotalInternalStorage;
    //+++Asus emmc total size
    private long mEmmcTotalSize;
    //---
    //+++ Asus Flatten Storage Settings Layout
    private FlattenStorageLayout mFlattenLayoutHelper;
    //---

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_STORAGE;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_storage;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final Context context = getActivity();

        mStorageManager = context.getSystemService(StorageManager.class);
        mStorageManager.registerListener(mStorageListener);

        if (sTotalInternalStorage <= 0) {
            sTotalInternalStorage = mStorageManager.getPrimaryStorageSize();
        }

        addPreferencesFromResource(R.xml.device_info_storage);

        mInternalCategory = (PreferenceCategory) findPreference("storage_internal");
        mExternalCategory = (PreferenceCategory) findPreference("storage_external");
        mMemoryCategory = (PreferenceCategory) findPreference("memory_setting_category");
        mMemorysetting = (PreferenceScreen) findPreference("memory_setting");
        mStorageSetting = (PreferenceCategory) findPreference("storage_set");
        mSpaceClear = (PreferenceScreen) findPreference("space_clear");

        mInternalSummary = new StorageSummaryPreference(getPrefContext());
        //+++ Asus emmc total size
        mEmmcTotalSize = getTotalSize();
        //--- Asus emmc total size
        //+++ Asus Flatten Storage Settings Layout
        mFlattenLayoutHelper = new FlattenStorageLayout(this, mInternalCategory, mExternalCategory);
        //---

        setHasOptionsMenu(true);
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onVolumeStateChanged(VolumeInfo vol, int oldState, int newState) {
            if (isInteresting(vol)) {
                refresh();
            }
        }

        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            refresh();
        }
    };

    private static boolean isInteresting(VolumeInfo vol) {
        switch(vol.getType()) {
            case VolumeInfo.TYPE_PRIVATE:
            case VolumeInfo.TYPE_PUBLIC:
                return true;
            default:
                return false;
        }
    }

    public void refresh() {
        final Context context = getPrefContext();

        getPreferenceScreen().removeAll();


        mInternalCategory.removeAll();
        mExternalCategory.removeAll();

        mInternalCategory.addPreference(mInternalSummary);
        //+++ Asus Flatten Sotrage Settings
        mFlattenLayoutHelper.clearInternalVolumePoolIfNeed();
        //---

        int privateCount = 0;
        long privateUsedBytes = 0;
        long privateTotalBytes = 0;

        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        Collections.sort(volumes, VolumeInfo.getDescriptionComparator());
        //+++ Asus total emmc size
        VolumeInfo intVol = null;
        //---

        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                final long volumeTotalBytes = getTotalSize(vol);
                Log.d("blenda", "get volume, type private");
                final int color = COLOR_PRIVATE[privateCount++ % COLOR_PRIVATE.length];
                //+++ Asus Flatten Sotrage Settings
                StorageVolumePreference internalVolume = new StorageVolumePreference(context, vol, color, volumeTotalBytes);
                if (mFlattenLayoutHelper.updateInternalVolumePoolIfNeed(internalVolume)) {
                    mFlattenLayoutHelper.setManageStorageListener(internalVolume);
                }
                mInternalCategory.addPreference(internalVolume);
                //---
                if (vol.isMountedReadable()) {
                    final File path = vol.getPath();

                    if (path.getName().equals("data")){
                        intVol = vol;
                    }

                    privateUsedBytes += (volumeTotalBytes - path.getFreeSpace());
                    privateTotalBytes += volumeTotalBytes;
                    //+++ Asus Flatten Storage Settings
                    mFlattenLayoutHelper.showInternalStorageDetailIfNeed(vol, mStorageManager.findEmulatedForPrivate(vol));
                    //---
                }
            } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                Log.d("blenda", "get volume, type public");
                mExternalCategory.addPreference(
                        new StorageVolumePreference(context, vol, COLOR_PUBLIC, 0));
            }
        }

        //+++ Asus total emmc size
        if (mEmmcTotalSize > 0) {
            Log.d("blenda", "get system volume, mEmmcTotalSize: "+mEmmcTotalSize);
            StorageVolumePreference systemVolume = new StorageVolumePreference(context,
                    mEmmcTotalSize, intVol,
                    COLOR_PRIVATE[privateCount++ % COLOR_PRIVATE.length]);
            //+++ Asus Flatten Sotrage Settings
            mFlattenLayoutHelper.updateInternalVolumePoolIfNeed(systemVolume);
            //---
            mInternalCategory.addPreference(systemVolume);
            long reservedSize = mEmmcTotalSize - intVol.getPath().getTotalSpace();
            privateUsedBytes += reservedSize;
            privateTotalBytes += reservedSize;
        }
        //---

        // Show missing private volumes
        final List<VolumeRecord> recs = mStorageManager.getVolumeRecords();
        for (VolumeRecord rec : recs) {
            if (rec.getType() == VolumeInfo.TYPE_PRIVATE
                    && mStorageManager.findVolumeByUuid(rec.getFsUuid()) == null) {
                Log.d("blenda", "get missing private volumes, title: "+rec.getNickname());
                // TODO: add actual storage type to record
                final Drawable icon = context.getDrawable(R.drawable.ic_sim_sd);
                icon.mutate();
                icon.setTint(COLOR_PUBLIC);

                final Preference pref = new Preference(context);
                pref.setKey(rec.getFsUuid());
                pref.setTitle(rec.getNickname());
                pref.setSummary(com.android.internal.R.string.ext_media_status_missing);
                pref.setIcon(icon);
                mInternalCategory.addPreference(pref);
            }
        }

        // Show unsupported disks to give a chance to init
        final List<DiskInfo> disks = mStorageManager.getDisks();
        for (DiskInfo disk : disks) {
            if (disk.volumeCount == 0 && disk.size > 0) {
                Log.d("blenda", "unsupported disk, title: "+disk.getDescription());
                final Preference pref = new Preference(context);
                pref.setKey(disk.getId());
                pref.setTitle(disk.getDescription());
                pref.setSummary(com.android.internal.R.string.ext_media_status_unsupported);
                pref.setIcon(R.drawable.ic_sim_sd);
                mExternalCategory.addPreference(pref);
            }
        }

        final long privateFreeBytes = privateTotalBytes - privateUsedBytes;
        final BytesResult result = Formatter.formatBytes(getResources(), privateFreeBytes, 0);
//        mInternalSummary.setTitle(Html.fromHtml(TextUtils.expandTemplate(getText(R.string.storage_size_large),
//                result.value, result.units).toString()));
        final String used = Formatter.formatFileSize(context, privateUsedBytes);
        final String free = Formatter.formatFileSize(context, privateFreeBytes);
        final String total = Formatter.formatFileSize(context, privateTotalBytes);
        mInternalSummary.setTitle(getString(R.string.cn_storage_volume_title, free, total));
        mInternalSummary.setPercent((int) ((privateUsedBytes * 100) / privateTotalBytes));

//        mInternalSummary.setSummary(getString(R.string.storage_volume_summary, used, total));

        getPreferenceScreen().addPreference(mMemoryCategory);
        if (mInternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mInternalCategory);
            setPreferenceGroupChildrenLayout(mInternalCategory, R.layout.cn_storage_volume_no_divider);
        }
        if (mExternalCategory.getPreferenceCount() > 0) {
            getPreferenceScreen().addPreference(mExternalCategory);
            setPreferenceGroupChildrenLayout(mExternalCategory, R.layout.cn_storage_volume_no_divider);
        }

        //hide clear space
//        getPreferenceScreen().addPreference(mStorageSetting);
        /*if (mInternalCategory.getPreferenceCount() == 2
                && mExternalCategory.getPreferenceCount() == 0) {
            // Only showing primary internal storage, so just shortcut
            final Bundle args = new Bundle();
            args.putString(VolumeInfo.EXTRA_VOLUME_ID, VolumeInfo.ID_PRIVATE_INTERNAL);
            PrivateVolumeSettings.setVolumeSize(args, sTotalInternalStorage);
            Intent intent = Utils.onBuildStartFragmentIntent(getActivity(),
                    PrivateVolumeSettings.class.getName(), args, null, R.string.apps_storage, null,
                    false);
            intent.putExtra(SettingsDrawerActivity.EXTRA_SHOW_MENU, true);
            getActivity().startActivity(intent);
            finish();
        }*/
    }
    public void setPreferenceGroupChildrenLayout(PreferenceGroup group, int layoutResId){
        if(group == null) return;

        int count = group.getPreferenceCount();
        Preference p = group.getPreference(count-1);
        p.setLayoutResource(layoutResId);

    }
    //+++ Asus: getTotalSize
    private long getTotalSize() {
        long totalSize = 0L;
        try {
            String[] cmd = new String[]{"/system/bin/sh", "-c", "cat /data/data/emmc_total_size"};
            BufferedReader br = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(cmd).getInputStream()));
            String line = br.readLine();

            if(line != null) {
                totalSize = Long.parseLong(line) * 1024 * 1024 * 1024;
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return totalSize;
    }
    //----

    @Override
    public void onResume() {
        super.onResume();
        mStorageManager.registerListener(mStorageListener);
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        mStorageManager.unregisterListener(mStorageListener);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference pref) {
        final String key = pref.getKey();
        //+++ Asus: Flatten Storage Settings
        if (pref != mMemorysetting && pref != mSpaceClear) {
            if (pref instanceof StorageItemPreference) {
                mFlattenLayoutHelper.onStorageItemPreferenceTreeClick(this, mStorageManager, pref);
                //---
            } else if (pref instanceof StorageVolumePreference) {

                //+++Asus emmc total size
                if (key.equals(ID_SYSTEM_RESERVED)) {
                    //No need to do anything, if it is a reserved storage.
                    Log.i(TAG, ID_SYSTEM_RESERVED + ":" + ((String) pref.getTitle()));
                    return true;
                }
                //---
                // Picked a normal volume
                final VolumeInfo vol = mStorageManager.findVolumeById(key);

                if (vol == null) {
                    return false;
                }

                if (vol.getState() == VolumeInfo.STATE_UNMOUNTED) {
                    VolumeUnmountedFragment.show(this, vol.getId());
                    return true;
                } else if (vol.getState() == VolumeInfo.STATE_UNMOUNTABLE) {
                    DiskInitFragment.show(this, R.string.storage_dialog_unmountable, vol.getDiskId());
                    return true;
                }

                if (vol.getType() == VolumeInfo.TYPE_PRIVATE) {
                    if (mFlattenLayoutHelper.onInternalStorageVolumePreferenceTreeClick(mInternalSummary, vol)) {
                        return true;
                    }
                    final Bundle args = new Bundle();
                    args.putString(VolumeInfo.EXTRA_VOLUME_ID, vol.getId());
                    startFragment(this, PrivateVolumeSettings.class.getCanonicalName(),
                            -1, 0, args);
                    return true;

                } else if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
                    if (vol.isMountedReadable()) {
                        final Bundle args = new Bundle();
                        args.putString(VolumeInfo.EXTRA_VOLUME_ID, vol.getId());
                        startFragment(this, PublicVolumeSettings.class.getCanonicalName(),
                                -1, 0, args);
//                        startActivity(vol.buildBrowseIntent());
                        return true;
                    } else {
                        final Bundle args = new Bundle();
                        args.putString(VolumeInfo.EXTRA_VOLUME_ID, vol.getId());
                        startFragment(this, PublicVolumeSettings.class.getCanonicalName(),
                                -1, 0, args);
                        return true;
                    }
                }

            } else if (key.startsWith("disk:")) {
                // Picked an unsupported disk
                DiskInitFragment.show(this, R.string.storage_dialog_unsupported, key);
                return true;

            }else {
                // Picked a missing private volume
                final Bundle args = new Bundle();
                args.putString(VolumeRecord.EXTRA_FS_UUID, key);
                startFragment(this, PrivateVolumeForget.class.getCanonicalName(),
                        R.string.storage_menu_forget, 0, args);
                return true;
            }

            return false;
        } else {
            return super.onPreferenceTreeClick(pref);
        }
    }

    public static class MountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public MountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.mount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to mount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_mount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class UnmountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public UnmountTask(Context context, VolumeInfo volume) {
            mContext = context.getApplicationContext();
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.unmount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null) {
                Toast.makeText(mContext, mContext.getString(R.string.storage_unmount_success,
                        mDescription), Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to unmount " + mVolumeId, e);
                Toast.makeText(mContext, mContext.getString(R.string.storage_unmount_failure,
                        mDescription), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class VolumeUnmountedFragment extends DialogFragment {
        public static void show(Fragment parent, String volumeId) {
            final Bundle args = new Bundle();
            args.putString(VolumeInfo.EXTRA_VOLUME_ID, volumeId);

            final VolumeUnmountedFragment dialog = new VolumeUnmountedFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_VOLUME_UNMOUNTED);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager sm = context.getSystemService(StorageManager.class);

            final String volumeId = getArguments().getString(VolumeInfo.EXTRA_VOLUME_ID);
            final VolumeInfo vol = sm.findVolumeById(volumeId);

            //blenda, change mount dialog
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setPositiveButton(R.string.lockpattern_confirm_button_text,
                    new DialogInterface.OnClickListener() {
                /**
                 * Check if an {@link RestrictedLockUtils#sendShowAdminSupportDetailsIntent admin
                 * details intent} should be shown for the restriction and show it.
                 *
                 * @param restriction The restriction to check
                 * @return {@code true} iff a intent was shown.
                 */
                private boolean wasAdminSupportIntentShown(@NonNull String restriction) {
                    EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                            getActivity(), restriction, UserHandle.myUserId());
                    boolean hasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(
                            getActivity(), restriction, UserHandle.myUserId());
                    if (admin != null && !hasBaseUserRestriction) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), admin);
                        return true;
                    }

                    return false;
                }

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (wasAdminSupportIntentShown(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)) {
                        return;
                    }

                    if (vol.disk != null && vol.disk.isUsb() &&
                            wasAdminSupportIntentShown(UserManager.DISALLOW_USB_FILE_TRANSFER)) {
                        return;
                    }

                    new MountTask(context, vol).execute();
                }
            });

            builder.setNegativeButton(R.string.cancel, null);

            View view1 = LayoutInflater.from(builder.getContext()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
            TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
            title.setText(getActivity().getResources().getString(R.string.mount_sdcard_dialog_title));
            TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
            String text = getActivity().getResources().getString(R.string.mount_sdcard_dialog_content);
            message.setText(text);

            builder.setView(view1);
            AlertDialog alertDialog1 = builder.create();
//            alertDialog1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            Window dialogWindow = alertDialog1.getWindow();
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.BOTTOM;
            dialogWindow.setAttributes(lp);
            return alertDialog1;
            /*final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(
                    getText(R.string.storage_dialog_unmounted), vol.getDisk().getDescription()));

            builder.setPositiveButton(R.string.storage_menu_mount,
                    new DialogInterface.OnClickListener() {
                /**
                 * Check if an {@link RestrictedLockUtils#sendShowAdminSupportDetailsIntent admin
                 * details intent} should be shown for the restriction and show it.
                 *
                 * @param restriction The restriction to check
                 * @return {@code true} iff a intent was shown.
                 */
               /* private boolean wasAdminSupportIntentShown(@NonNull String restriction) {
                    EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                            getActivity(), restriction, UserHandle.myUserId());
                    boolean hasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(
                            getActivity(), restriction, UserHandle.myUserId());
                    if (admin != null && !hasBaseUserRestriction) {
                        RestrictedLockUtils.sendShowAdminSupportDetailsIntent(getActivity(), admin);
                        return true;
                    }

                    return false;
                }

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (wasAdminSupportIntentShown(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA)) {
                        return;
                    }

                    if (vol.disk != null && vol.disk.isUsb() &&
                            wasAdminSupportIntentShown(UserManager.DISALLOW_USB_FILE_TRANSFER)) {
                        return;
                    }

                    new MountTask(context, vol).execute();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);*/

//            return builder.create();
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            Log.d(TAG, this.getClass()+":onConfigurationChanged...dialog dismissed");
            this.dismissAllowingStateLoss();
        }
    }

    public static class DiskInitFragment extends DialogFragment {
        public static void show(Fragment parent, int resId, String diskId) {
            final Bundle args = new Bundle();
            args.putInt(Intent.EXTRA_TEXT, resId);
            args.putString(DiskInfo.EXTRA_DISK_ID, diskId);

            final DiskInitFragment dialog = new DiskInitFragment();
            dialog.setArguments(args);
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_DISK_INIT);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final StorageManager sm = context.getSystemService(StorageManager.class);

            final int resId = getArguments().getInt(Intent.EXTRA_TEXT);
            final String diskId = getArguments().getString(DiskInfo.EXTRA_DISK_ID);
            final DiskInfo disk = sm.findDiskById(diskId);

     /*       final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(TextUtils.expandTemplate(getText(resId), disk.getDescription()));

            builder.setPositiveButton(R.string.storage_menu_set_up,
                    new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Intent intent = new Intent(context, StorageWizardInit.class);
                    intent.putExtra(DiskInfo.EXTRA_DISK_ID, diskId);
                    startActivity(intent);
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            return builder.create();*/


            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setPositiveButton(R.string.storage_menu_set_up,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Intent intent = new Intent(context, CNStorageWizardFormatConfirm.class);
                            intent.putExtra(DiskInfo.EXTRA_DISK_ID, diskId);
                            intent.putExtra(CNStorageWizardFormatConfirm.EXTRA_FORMAT_PRIVATE, false);
                            startActivity(intent);
                        }
                    });
            builder.setNegativeButton(R.string.cancel, null);

            View view1 = LayoutInflater.from(builder.getContext()).inflate(R.layout.cnasusres_alertdialog_content_with_title, null);
            TextView title = (TextView) view1.getRootView().findViewById(R.id.alertdialog_title);
            title.setText(getActivity().getResources().getString(R.string.hint));
            TextView message = (TextView) view1.getRootView().findViewById(R.id.alertdialog_message);
            String text = TextUtils.expandTemplate(getText(resId), disk.getDescription()).toString();
            message.setText(text);

            builder.setView(view1);
            AlertDialog alertDialog1 = builder.create();
//            alertDialog1.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            Window dialogWindow = alertDialog1.getWindow();
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.BOTTOM;
            dialogWindow.setAttributes(lp);
            return alertDialog1;
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mLoader;

        private SummaryProvider(Context context, SummaryLoader loader) {
            mContext = context;
            mLoader = loader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                updateSummary();
            }
        }

        private void updateSummary() {
            // TODO: Register listener.
            final StorageManager storageManager = mContext.getSystemService(StorageManager.class);
            if (sTotalInternalStorage <= 0) {
                sTotalInternalStorage = storageManager.getPrimaryStorageSize();
            }
            final List<VolumeInfo> volumes = storageManager.getVolumes();
            long privateFreeBytes = 0;
            long privateTotalBytes = 0;
            for (VolumeInfo info : volumes) {
                final File path = info.getPath();
                if (info.getType() != VolumeInfo.TYPE_PRIVATE || path == null) {
                    continue;
                }
                privateTotalBytes += getTotalSize(info);
                privateFreeBytes += path.getFreeSpace();
            }
            long privateUsedBytes = privateTotalBytes - privateFreeBytes;
            mLoader.setSummary(this, mContext.getString(R.string.storage_summary,
                    Formatter.formatFileSize(mContext, privateUsedBytes),
                    Formatter.formatFileSize(mContext, privateTotalBytes)));
        }
    }

    private static long getTotalSize(VolumeInfo info) {
        // Device could have more than one primary storage, which could be located in the
        // internal flash (UUID_PRIVATE_INTERNAL) or in an external disk.
        // If it's internal, try to get its total size from StorageManager first
        // (sTotalInternalStorage), since that size is more precise because it accounts for
        // the system partition.
        //to
/*        if (info.getType() == VolumeInfo.TYPE_PRIVATE
                && Objects.equals(info.getFsUuid(), StorageManager.UUID_PRIVATE_INTERNAL)
                && sTotalInternalStorage > 0) {
            return sTotalInternalStorage;
        } else {*/
            final File path = info.getPath();
            if (path == null) {
                // Should not happen, caller should have checked.
                Log.e(TAG, "info's path is null on getTotalSize(): " + info);
                return 0;
            }
            return path.getTotalSpace();
        }
//    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    /**
     * Enable indexing of searchable data
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();

                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.storage_settings);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_storage_title);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_settings_title);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.internal_storage);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                final StorageManager storage = context.getSystemService(StorageManager.class);
                final List<VolumeInfo> vols = storage.getVolumes();
                for (VolumeInfo vol : vols) {
                    if (isInteresting(vol)) {
                        data.title = storage.getBestVolumeDescription(vol);
                        data.screenTitle = context.getString(R.string.memory_storage_title);
                        result.add(data);
                    }
                }

/*                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_size);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_available);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_apps_usage);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_dcim_usage);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_music_usage);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_downloads_usage);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_media_cache_usage);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);

                data = new SearchIndexableRaw(context);
                data.title = context.getString(R.string.memory_media_misc_usage);
                data.screenTitle = context.getString(R.string.memory_storage_title);
                result.add(data);*/

                return result;
            }
        };

}