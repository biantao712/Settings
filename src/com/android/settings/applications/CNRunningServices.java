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
package com.android.settings.applications;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_BACKGROUND;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_RUNNING_SERVICE;

public class CNRunningServices extends SettingsPreferenceFragment {

    private RunningProcessesView mRunningProcessesView;
    private View mLoadingContainer;
    private boolean mShowRunningServices = false;
    private int type;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getActivity().getIntent();
        Bundle args = getArguments();
        type = args.getInt(EXTRA_APPLICATIONS_TYPE);
        if (type == EXTRA_APPLICATIONS_TYPE_RUNNING_SERVICE){
            mShowRunningServices = false;
        }else if (type == EXTRA_APPLICATIONS_TYPE_BACKGROUND){
            mShowRunningServices = true;
        }
        mCNManageAppsSync = CNManageAppsSync.getInstance();
        setInterface();
    }

    private boolean needLoadData = false;
    private View rootView;
    private CNManageAppsSync mCNManageAppsSync;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {

        Log.d("blenda", "on create view, mShowRunningServices:　"+mShowRunningServices);
            rootView = inflater.inflate(R.layout.manage_applications_running, null);
            mRunningProcessesView = (RunningProcessesView) rootView.findViewById(
                    R.id.running_processes);
//            mRunningProcessesView.doCreate(false, true);
//            mRunningProcessesView.mAdapter.setShowBackground(mShowRunningServices);
            mLoadingContainer = rootView.findViewById(R.id.loading_container);
        if ((CNManageAppsSync.getInstance().getSelectedTabPos() == 1 && !mShowRunningServices) ||
                (CNManageAppsSync.getInstance().getSelectedTabPos() == 2 && mShowRunningServices)){

            mRunningProcessesView.doCreate(false, true);
            mRunningProcessesView.mAdapter.setShowBackground(mShowRunningServices);
        } else {
            needLoadData = true;
        }

        return rootView;
    }


    @Override
    public void onResume() {
        super.onResume();
        if (!needLoadData) {
            boolean haveData = mRunningProcessesView.doResume(this, mRunningProcessesAvail);
            Utils.handleLoadingContainer(mLoadingContainer, mRunningProcessesView, haveData, false);
        }
//        boolean haveData = mRunningProcessesView.doResume(this, mRunningProcessesAvail);
//        Utils.handleLoadingContainer(mLoadingContainer, mRunningProcessesView, haveData, false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mRunningProcessesView.doPause();
    }

    private void pauseProcessesView(){
        if (mRunningProcessesView != null)
            mRunningProcessesView.doPause();
    }

    private void refreshRunningProcessesView(){
        if (needLoadData && mRunningProcessesView != null) {
            mRunningProcessesView.doCreate(false, true);
            mRunningProcessesView.mAdapter.setShowBackground(mShowRunningServices);
            boolean haveData = mRunningProcessesView.doResume(this, mRunningProcessesAvail);
            Utils.handleLoadingContainer(mLoadingContainer, mRunningProcessesView, haveData, false);
            needLoadData = false;
        }else if (mRunningProcessesView != null){
            boolean haveData = mRunningProcessesView.doResume(this, mRunningProcessesAvail);
            Utils.handleLoadingContainer(mLoadingContainer, mRunningProcessesView, haveData, false);
        }
    }
    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RUNNING_SERVICES;
    }

    private final Runnable mRunningProcessesAvail = new Runnable() {
        @Override
        public void run() {
            Utils.handleLoadingContainer(mLoadingContainer, mRunningProcessesView, true, true);
        }
    };

    private void setInterface(){

        OnCallServicePage listener_callServicePage = new OnCallServicePage() {
            @Override
            public void onLoad() {
                refreshRunningProcessesView();
            }

            @Override
            public void onUnLoad() {
                pauseProcessesView();
            }
        };
        mCNManageAppsSync.setListener(listener_callServicePage, type);
    }
}
