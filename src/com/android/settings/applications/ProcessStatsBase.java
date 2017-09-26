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

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.view.ViewGroup.LayoutParams;

import android.widget.Spinner;
import android.widget.TextView;

import com.android.internal.app.procstats.ProcessStats;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.ProcStatsData.MemInfo;

public abstract class ProcessStatsBase extends SettingsPreferenceFragment
        implements  OnItemSelectedListener, AdapterView.OnItemClickListener {
    private static final String DURATION = "duration";

    protected static final String ARG_TRANSFER_STATS = "transfer_stats";
    protected static final String ARG_DURATION_INDEX = "duration_index";

    protected static final int NUM_DURATIONS = 4;

    // The actual duration value to use for each duration option.  Note these
    // are lower than the actual duration, since our durations are computed in
    // batches of 3 hours so we want to allow the time we use to be slightly
    // smaller than the actual time selected instead of bumping up to 3 hours
    // beyond it.
    private static final long DURATION_QUANTUM = ProcessStats.COMMIT_PERIOD;
    protected static long[] sDurations = new long[] {
        3 * 60 * 60 * 1000 - DURATION_QUANTUM / 2, 6 * 60 *60 * 1000 - DURATION_QUANTUM / 2,
        12 * 60 * 60 * 1000 - DURATION_QUANTUM / 2, 24 * 60 * 60 * 1000 - DURATION_QUANTUM / 2
    };
    protected static int[] sDurationLabels = new int[] {
            R.string.menu_duration_3h, R.string.menu_duration_6h,
            R.string.menu_duration_12h, R.string.menu_duration_1d
    };

    private ViewGroup mSpinnerHeader;
    private RelativeLayout mFilterLayout;
    private ArrayAdapter<String> mFilterAdapter;

    protected ProcStatsData mStatsManager;
    protected int mDurationIndex;

    private ListView mListView;
    private TextView mDurationText;
    private PopupWindow mPopupWindow;

    protected ProcessStatsBase() {
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Bundle args = getArguments();
        mStatsManager = new ProcStatsData(getActivity(), icicle != null
                || (args != null && args.getBoolean(ARG_TRANSFER_STATS, false)));

        mDurationIndex = icicle != null
                ? icicle.getInt(ARG_DURATION_INDEX)
                : args != null ? args.getInt(ARG_DURATION_INDEX) : 0;
        mStatsManager.setDuration(icicle != null
                ? icicle.getLong(DURATION, sDurations[0]) : sDurations[0]);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DURATION, mStatsManager.getDuration());
        outState.putInt(ARG_DURATION_INDEX, mDurationIndex);
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatsManager.refreshStats(false);
        refreshUi();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity().isChangingConfigurations()) {
            mStatsManager.xferStats();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSpinnerHeader = (ViewGroup) setPinnedHeaderView(R.layout.cn_process_header);
        mFilterLayout = (RelativeLayout) mSpinnerHeader.findViewById(R.id.filter_spinner);
        mDurationText = (TextView)mSpinnerHeader.findViewById(R.id.duration_text);

        final View popupView = getActivity().getLayoutInflater().inflate(
                R.layout.cn_popup_window, null);
        mListView = (ListView) popupView.findViewById(R.id.listView1);
        mFilterAdapter = new ArrayAdapter<String>(getActivity(), R.layout.cnasusres_popup_list_item);
//        mFilterAdapter.setDropDownViewResource(R.layout.cn_spinner_dropdown_item);
        for (int i = 0; i < NUM_DURATIONS; i++) {
            mFilterAdapter.add(getString(sDurationLabels[i]));
        }
        mListView.setAdapter(mFilterAdapter);
//        mFilterSpinner.setSelection(mDurationIndex);

        mListView.setOnItemClickListener(this);
        mListView.setOnItemSelectedListener(this);
        mFilterLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow = new PopupWindow(view.getContext());
                mPopupWindow.setContentView(popupView);
                mPopupWindow.setWidth(480);
                mPopupWindow.setHeight(LayoutParams.WRAP_CONTENT);
                mPopupWindow.setFocusable(true);
//                popup.setOutsideTouchable(true);
//                popup.showAsDropDown(view);

                int[] location = new int[2];
                view.getLocationOnScreen(location);
                mPopupWindow.showAtLocation(view, Gravity.NO_GRAVITY, location[0]+view.getWidth()-mPopupWindow.getWidth(), location[1]+view.getHeight());

            }
        });

        mStatsManager.setDuration(sDurations[mDurationIndex]);
        mDurationText.setText(getString(sDurationLabels[mDurationIndex]));
    }
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Log.d("blenda", "onItemClick, postion: "+i);
        mPopupWindow.dismiss();
        mDurationIndex = i;
        mStatsManager.setDuration(sDurations[i]);
        mDurationText.setText(getString(sDurationLabels[i]));
        refreshUi();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d("blenda", "onItemSelected, postion: "+position);
        mDurationIndex = position;
        mStatsManager.setDuration(sDurations[position]);
        mDurationText.setText(getString(sDurationLabels[position]));
        refreshUi();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Select something.
//        mFilterSpinner.setSelection(0);
        mDurationText.setText(getString(sDurationLabels[0]));
    }

    public abstract void refreshUi();

    public static void launchMemoryDetail(SettingsActivity activity, MemInfo memInfo,
            ProcStatsPackageEntry entry, boolean includeAppInfo) {
        Bundle args = new Bundle();
        args.putParcelable(ProcessStatsDetail.EXTRA_PACKAGE_ENTRY, entry);
        args.putDouble(ProcessStatsDetail.EXTRA_WEIGHT_TO_RAM, memInfo.weightToRam);
        args.putLong(ProcessStatsDetail.EXTRA_TOTAL_TIME, memInfo.memTotalTime);
        args.putDouble(ProcessStatsDetail.EXTRA_MAX_MEMORY_USAGE,
                memInfo.usedWeight * memInfo.weightToRam);
        args.putDouble(ProcessStatsDetail.EXTRA_TOTAL_SCALE, memInfo.totalScale);
        args.putBoolean(AppHeader.EXTRA_HIDE_INFO_BUTTON, !includeAppInfo);
        activity.startPreferencePanel(ProcessStatsDetail.class.getName(), args,
                R.string.memory_usage, null, null, 0);
    }

    public static void launchMemoryDetail(SettingsActivity activity, MemInfo memInfo,
                                          ProcStatsPackageEntry entry, boolean includeAppInfo, String label) {
        Bundle args = new Bundle();
        args.putParcelable(ProcessStatsDetail.EXTRA_PACKAGE_ENTRY, entry);
        args.putDouble(ProcessStatsDetail.EXTRA_WEIGHT_TO_RAM, memInfo.weightToRam);
        args.putLong(ProcessStatsDetail.EXTRA_TOTAL_TIME, memInfo.memTotalTime);
        args.putDouble(ProcessStatsDetail.EXTRA_MAX_MEMORY_USAGE,
                memInfo.usedWeight * memInfo.weightToRam);
        args.putDouble(ProcessStatsDetail.EXTRA_TOTAL_SCALE, memInfo.totalScale);
        args.putBoolean(AppHeader.EXTRA_HIDE_INFO_BUTTON, !includeAppInfo);
        args.putString("EXTRA_LABEL", label);
        activity.startPreferencePanel(ProcessStatsDetail.class.getName(), args,
                R.string.memory_usage, null, null, 0);
    }
}
