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

import android.annotation.LayoutRes;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.storage.DiskInfo;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.R;
import com.android.setupwizardlib.SetupWizardLayout;
import com.android.setupwizardlib.view.Illustration;

import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

import com.asus.cncommonres.AsusActivity;

public abstract class CNStorageWizardBase extends AsusActivity {
    protected StorageManager mStorage;

    protected VolumeInfo mVolume;
    protected DiskInfo mDisk;

    private View mCustomNav;
    private Button mCustomNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStorage = getSystemService(StorageManager.class);

        final String volumeId = getIntent().getStringExtra(VolumeInfo.EXTRA_VOLUME_ID);
        if (!TextUtils.isEmpty(volumeId)) {
            mVolume = mStorage.findVolumeById(volumeId);
        }

        final String diskId = getIntent().getStringExtra(DiskInfo.EXTRA_DISK_ID);
        if (!TextUtils.isEmpty(diskId)) {
            mDisk = mStorage.findDiskById(diskId);
        } else if (mVolume != null) {
            mDisk = mVolume.getDisk();
        }

//        setTheme(R.style.SetupWizardStorageStyle);

        if (mDisk != null) {
            mStorage.registerListener(mStorageListener);
        }
        setActionBar();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(getColor(R.color.action_bar_background));
    }


    private void setActionBar(){
        setAsusActionBarType(AsusActivity.ASUS_ACTIONBAR_SECOND_LEVEL);
        setTitle(R.string.storage_menu_format);

    }
    @Override
    protected void onDestroy() {
        mStorage.unregisterListener(mStorageListener);
        super.onDestroy();
    }


    protected ProgressBar getProgressBar() {
        return (ProgressBar) findViewById(R.id.progress_bar);
    }

    protected void setCurrentProgress(int progress) {
        getProgressBar().setProgress(progress);
        ((TextView) findViewById(R.id.progress_value)).setText(
                NumberFormat.getPercentInstance().format((double) progress / 100));
    }


    protected VolumeInfo findFirstVolume(int type) {
        final List<VolumeInfo> vols = mStorage.getVolumes();
        for (VolumeInfo vol : vols) {
            if (Objects.equals(mDisk.getId(), vol.getDiskId()) && (vol.getType() == type)) {
                return vol;
            }
        }
        return null;
    }

    private final StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onDiskDestroyed(DiskInfo disk) {
            // We know mDisk != null.
            if (mDisk.id.equals(disk.id)) {
                finish();
            }
        }
    };
}
