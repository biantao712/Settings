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

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.android.settings.R;
import com.android.settings.deviceinfo.StorageSettings.UnmountTask;
import com.android.settings.deviceinfo.utils.FlattenStorageLayout;

import java.io.File;
import java.util.Objects;

/**
 * Preference line representing a single {@link VolumeInfo}, possibly including
 * quick actions like unmounting.
 */
public class StorageVolumePreference extends Preference {
    private final StorageManager mStorageManager;
    private final VolumeInfo mVolume;

    private int mColor;
    private int mUsedPercent = -1;

    //+++ Asus Flatten Storage Setting Layout
    private boolean mIsFlatten = false;
    private boolean mIsShowMigrate = false;
    PopupMenu mPopupMenu = null;
    public interface OnManageStorageClickListener{
        void onManageStorageClick(VolumeInfo volumeID, MenuItem item);
    }
    private OnManageStorageClickListener mManageStorageListener;
    public void setManageStorageListener(OnManageStorageClickListener manageStorageListener) {
        this.mManageStorageListener = manageStorageListener;
    }
    private final View.OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mPopupMenu = new PopupMenu(getContext(), v);
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    if(mManageStorageListener!=null) {
                        mManageStorageListener.onManageStorageClick(mVolume, menuItem);
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            mPopupMenu.inflate(R.menu.storage_volume);

            final Menu menu = mPopupMenu.getMenu();
            final MenuItem rename = menu.findItem(R.id.storage_rename);
            final MenuItem mount = menu.findItem(R.id.storage_mount);
            final MenuItem unmount = menu.findItem(R.id.storage_unmount);
            final MenuItem format = menu.findItem(R.id.storage_format);
            final MenuItem migrate = menu.findItem(R.id.storage_migrate);

            // Actions live in menu for non-internal private volumes; they're shown
            // as preference items for public volumes.
            if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(mVolume.getId())) {
                rename.setVisible(false);
                mount.setVisible(false);
                unmount.setVisible(false);
                format.setVisible(false);
                migrate.setVisible(mIsShowMigrate);
            } else {
                mount.setVisible(mVolume.getState() == VolumeInfo.STATE_UNMOUNTED);
                if(mVolume.getState() != VolumeInfo.STATE_UNMOUNTED) {
                    rename.setVisible(mVolume.getType() == VolumeInfo.TYPE_PRIVATE);
                    unmount.setVisible(mVolume.isMountedReadable());
                    format.setVisible(true);
                    migrate.setVisible(mIsShowMigrate);
                }
            }
            format.setTitle(R.string.storage_menu_format_public);
            mPopupMenu.show();
        }
    };
    //---

    // TODO: ideally, VolumeInfo should have a total physical size.
    public StorageVolumePreference(Context context, VolumeInfo volume, int color, long totalBytes) {
        super(context);

        mStorageManager = context.getSystemService(StorageManager.class);
        mVolume = volume;
        mColor = color;

        //+++ Asus Flatten Storage Setting Layout
        mIsFlatten = FlattenStorageLayout.isFlatten();
        //---

        setLayoutResource(R.layout.cn_storage_volume);

        setKey(volume.getId());

        String volumeTitle = mStorageManager.getBestVolumeDescription(volume);

        if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(volume.getId()) ||VolumeInfo.ID_EMULATED_INTERNAL.equals(volume.getId())){
            volumeTitle = context.getResources().getString(R.string.storage_internal);
        }
        setTitle(volumeTitle);

        Drawable icon;
        if (VolumeInfo.ID_PRIVATE_INTERNAL.equals(volume.getId())) {
            icon = context.getDrawable(R.drawable.ic_settings_storage);
        } else {
            icon = context.getDrawable(R.drawable.ic_sim_sd);
        }

        if (volume.isMountedReadable()) {
            // TODO: move statfs() to background thread
            final File path = volume.getPath();
            if (totalBytes <= 0) {
                totalBytes = path.getTotalSpace();
            }
            final long freeBytes = path.getFreeSpace();
            final long usedBytes = totalBytes - freeBytes;

            final String used = Formatter.formatFileSize(context, usedBytes);
            final String free = Formatter.formatFileSize(context, freeBytes);
            final String total = Formatter.formatFileSize(context, totalBytes);
            setSummary(context.getString(R.string.cn_storage_volume_title, free, total));
            if (totalBytes > 0) {
                mUsedPercent = (totalBytes > 0)? ((int)((usedBytes * 100) / totalBytes)):-1;
            }

            if (freeBytes < mStorageManager.getStorageLowBytes(path)) {
                mColor = StorageSettings.COLOR_WARNING;
                icon = context.getDrawable(R.drawable.ic_warning_24dp);
            }

        } else {
            setSummary(volume.getStateDescription());
            mUsedPercent = -1;
        }

        icon.mutate();
        icon.setTint(mColor);
//        setIcon(icon);

        if (volume.getType() == VolumeInfo.TYPE_PUBLIC
                && volume.isMountedReadable()) {
//            setWidgetLayoutResource(R.layout.preference_storage_action);
        //+++ Asus Flatten Storage Settings Layout
        } else if (mIsFlatten && volume.getType() == VolumeInfo.TYPE_PRIVATE
                        && volume.isMountedReadable()) {
            // Only offer to migrate when not current storage
            final VolumeInfo privateVol = context.getPackageManager()
                    .getPrimaryStorageCurrentVolume();
            mIsShowMigrate = ((privateVol != null)
                    && (privateVol.getType() == VolumeInfo.TYPE_PRIVATE)
                    && !Objects.equals(mVolume, privateVol));
            if (!VolumeInfo.ID_PRIVATE_INTERNAL.equals(mVolume.getId()) || mIsShowMigrate) {
                setWidgetLayoutResource(R.layout.asus_preference_storage_setting_widget);
            }
        //---
        }
    }

    //+++ Asus emmc total size
    //This is constructor to generate empty volumePreference for system reserved volume preference
    public StorageVolumePreference(Context context, long emmcTotalSize, VolumeInfo volume, int color) {
        super(context);

        mStorageManager = context.getSystemService(StorageManager.class);
        mVolume = volume;
        mColor = color;

        //+++ Asus Flatten Storage Setting Layout
        mIsFlatten = FlattenStorageLayout.isFlatten();
        //---

        setLayoutResource(R.layout.cn_storage_volume);
        setKey(StorageSettings.ID_SYSTEM_RESERVED);
        setTitle(context.getString(R.string.memory_system_reserved_usage));
        setEnabled(false);
        File intVol = volume.getPath();
        Drawable icon = context.getDrawable(R.drawable.ic_settings_storage);
        final long reservedSize = emmcTotalSize - intVol.getTotalSpace();
        if (!mIsFlatten) {
            final String used = Formatter.formatFileSize(context, reservedSize);
            final String free = Formatter.formatFileSize(context, 0);
            final String total = Formatter.formatFileSize(context, reservedSize);
            setSummary(context.getString(R.string.cn_storage_volume_title, free, total));
        } else {
            //Format asked by Verizon
            setSummary(Formatter.formatFileSize(context, reservedSize));
        }
        mUsedPercent = (int) ((reservedSize * 100) / reservedSize);
        icon.mutate();
        icon.setTint(mColor);
//        setIcon(icon);
    }
    //-- Asus emmc total size

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        final ImageView unmount = (ImageView) view.findViewById(R.id.unmount);
        if (unmount != null) {
            unmount.setImageTintList(ColorStateList.valueOf(Color.parseColor("#8a000000")));
            unmount.setOnClickListener(mUnmountListener);
        }

        final ProgressBar progress = (ProgressBar) view.findViewById(android.R.id.progress);
//        if (mVolume.getType() == VolumeInfo.TYPE_PRIVATE && mUsedPercent != -1) {
//            progress.setVisibility(View.VISIBLE);
//            progress.setProgress(mUsedPercent);
//            progress.setProgressTintList(ColorStateList.valueOf(mColor));
//        } else {
            progress.setVisibility(View.GONE);
//        }

        //+++ Asus Flatten Storage Settings
        View manageView = view.findViewById(R.id.manage_storage);
        if (manageView != null) {
            manageView.setOnClickListener(mClickListener);
            clearPopupMenu();
        }
        //---

        super.onBindViewHolder(view);
    }

    private final View.OnClickListener mUnmountListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            new UnmountTask(getContext(), mVolume).execute();
        }
    };

    //+++ [Verizon] show volume's detail information
    public void clearPopupMenu() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
            mPopupMenu = null;
        }
    }
    //--- [Verizon] show volume's detail information
}
