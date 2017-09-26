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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settingslib.applications.ApplicationsState;
import android.content.pm.ApplicationInfo;

// View Holder used when displaying views
public class DevelpAppViewHolder {
    public ApplicationsState.AppEntry entry;
    public View rootView;
    public TextView appName;
    public ImageView appIcon;
    public ImageView badge;
    public TextView summary;
    public TextView disabled;

    static public DevelpAppViewHolder createOrRecycle(LayoutInflater inflater, View convertView, ApplicationInfo info) {
        if (null == info){
            convertView = inflater.inflate(R.layout.first_debug_running_processes_item, null);
        } else {
            convertView = inflater.inflate(R.layout.debug_running_processes_item, null);
        }
            // Creates a ViewHolder and store references to the two children views
            // we want to bind data to.
            DevelpAppViewHolder holder = new DevelpAppViewHolder();
            holder.rootView = convertView;
            holder.appName = (TextView) convertView.findViewById(R.id.name);
            holder.appIcon = (ImageView) convertView.findViewById(R.id.icon);
            holder.summary = (TextView) convertView.findViewById(R.id.description);
            return holder;
            
    }

    void updateSizeText(CharSequence invalidSizeStr, int whichSize) {
        if (ManageApplications.DEBUG) Log.i(ManageApplications.TAG, "updateSizeText of "
                + entry.label + " " + entry + ": " + entry.sizeStr);
        if (entry.sizeStr != null) {
            switch (whichSize) {
                case ManageApplications.SIZE_INTERNAL:
                    summary.setText(entry.internalSizeStr);
                    break;
                case ManageApplications.SIZE_EXTERNAL:
                    summary.setText(entry.externalSizeStr);
                    break;
                default:
                    summary.setText(entry.sizeStr);
                    break;
            }
        } else if (entry.size == ApplicationsState.SIZE_INVALID) {
            summary.setText(invalidSizeStr);
        }
    }
}
