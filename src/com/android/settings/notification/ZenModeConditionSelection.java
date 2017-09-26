/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.animation.LayoutTransition;
import android.app.INotificationManager;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.service.notification.ZenModeConfig;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.android.settings.R;

//import com.android.settings.util.ResCustomizeConfig;

import java.util.ArrayList;
import java.util.List;

public class ZenModeConditionSelection extends ListView {
    private static final String TAG = "ZenModeConditionSelection";
    private static final boolean DEBUG = true;

    private final INotificationManager mNoMan;
    private final Context mContext;
    private final List<Condition> mConditions;
    private final ConditionArrayAdapter mArrayAdapter;
    private final int mZenMode;

    private Condition mCondition;

    public ZenModeConditionSelection(Context context, int zenMode) {
        super(context);
        mContext = context;
        mZenMode = zenMode;
        mConditions = new ArrayList<Condition>();
        mNoMan = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));

        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ArrayList<Condition> list = (ArrayList<Condition>)mConditions;
                if (position == 0) {
                    setCondition(null);
                }
                else {
                    setCondition((Condition)(list.get(position - 1)));
                }
                mArrayAdapter.setSelected(position);

                for (int i = 0; i < getChildCount(); i++) {
                    DndCheckedTextView v = (DndCheckedTextView)getChildAt(i);
                    v.setChecked(false);
                }
                DndCheckedTextView checkedTextView = (DndCheckedTextView) view;
                checkedTextView.setChecked(true);
            }
        });

        mArrayAdapter = new ConditionArrayAdapter(mContext, R.layout.dnd_singlechoice_item);
        setAdapter(mArrayAdapter);

        mArrayAdapter.add(mContext.getString(com.android.internal.R.string.zen_mode_forever));
        // mArrayAdapter.add(mContext.getString(ResCustomizeConfig.getIdentifier(mContext, "string", "zen_mode_forever")));
        mArrayAdapter.setSelected(0); // load previous setting
        for (int i = ZenModeConfig.MINUTE_BUCKETS.length - 1; i >= 0; --i) {
            handleCondition(ZenModeConfig.toTimeCondition(mContext, ZenModeConfig.MINUTE_BUCKETS[i], UserHandle.myUserId()));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    protected void handleConditions(Condition[] conditions) {
        for (Condition c : conditions) {
            handleCondition(c);
        }
    }

    protected void handleCondition(Condition c) {
        if (mConditions.contains(c)) return;
        View v = findViewWithTag(c.id);

        if (c.state == Condition.STATE_TRUE || c.state == Condition.STATE_UNKNOWN) {
            if (v == null) {
                mArrayAdapter.add(c.summary);
            }
        }
        mConditions.add(c);
    }

    protected void setCondition(Condition c) {
        if (DEBUG) Log.d(TAG, "setCondition " + c);
        mCondition = c;
    }

    public void confirmCondition() {
        if (DEBUG) Log.d(TAG, "confirmCondition " + mCondition);
        try {
            mNoMan.setZenMode(mZenMode, mCondition != null ? mCondition.id : null, TAG);
        } catch (RemoteException e) {
            // noop
        }
    }

    private static String computeConditionText(Condition c) {
        return !TextUtils.isEmpty(c.line1) ? c.line1
                : !TextUtils.isEmpty(c.summary) ? c.summary
                : "";
    }

    private final class H extends Handler {
        private static final int CONDITIONS = 1;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == CONDITIONS) handleConditions((Condition[]) msg.obj);
        }
    }

    class ConditionArrayAdapter extends ArrayAdapter<String> {
        private int selectedIdx = 0;

        public ConditionArrayAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View v = super.getView(position, convertView, parent);

            DndCheckedTextView checkedTextView = (DndCheckedTextView) v;
            boolean checked = selectedIdx == position;
            checkedTextView.setChecked(checked);
            return checkedTextView;
        }

        public void setSelected(int idx) {
            selectedIdx = idx;
        }
    }
}
