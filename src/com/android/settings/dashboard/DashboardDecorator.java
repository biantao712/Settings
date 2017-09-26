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

package com.android.settings.dashboard;

import com.android.settings.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.State;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

public class DashboardDecorator extends RecyclerView.ItemDecoration {

    private final Context mContext;
    private final Drawable mDivider;
    private static final String TAG = "DashboardDecorator";

    public DashboardDecorator(Context context) {
        mContext = context;
        TypedValue value = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.listDivider, value, true);
        mDivider = mContext.getDrawable(value.resourceId);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, State state) {
        final int childCount = parent.getChildCount();
        for (int i = 1; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final ViewHolder holder = parent.getChildViewHolder(child);

            // Rules of showing divider
            if (holder.getItemViewType() == R.layout.dashboard_category ||
                    holder.getItemViewType() == R.layout.category_divider) {
                continue;
            }
//            else if (holder.getItemViewType() == R.layout.dashboard_tile) {
//                // No divider if the dashboard_category is above the dashboard_tile
//                if (parent.getChildViewHolder(parent.getChildAt(i - 1)).getItemViewType()
//                        == R.layout.dashboard_category /*||
//                        parent.getChildViewHolder(parent.getChildAt(i - 1)).getItemViewType()
//                        == R.layout.category_divider*/) {
//                    continue;
//                }
//            }

            // The view passing the above rules will draw the divider.
            int top = getChildTop(child);
            // Special case of suggestion_header, the divider keeps the original look, no padding.
            if (holder.getItemViewType() == R.layout.suggestion_header
                    || (holder.getItemViewType() == R.layout.suggestion_tile
                    && parent.getChildViewHolder(parent.getChildAt(i - 1)).getItemViewType() == R.layout.suggestion_header)
                    || holder.getItemViewType() == R.layout.dashboard_spacer) {
                mDivider.setBounds(child.getLeft(), top, child.getRight(),
                        top + mDivider.getIntrinsicHeight());
            } else if (holder.getItemViewType() == R.layout.dashboard_tile &&
                    parent.getChildViewHolder(parent.getChildAt(i - 1)).getItemViewType()
                        == R.layout.dashboard_category){
                int paddingStartOfDivider = convertDpToPx(mContext.getResources()
                        .getDimension(R.dimen.dashboard_general_padding));
                int paddingEndOfDivider = convertDpToPx(mContext.getResources()
                        .getDimension(R.dimen.dashboard_general_padding));
                mDivider.setBounds(child.getLeft() /*+ paddingStartOfDivider*/ , top,
                        child.getRight() /*- paddingEndOfDivider*/,
                        top + mDivider.getIntrinsicHeight());

            }else {

                int paddingStartOfDivider = convertDpToPx(mContext.getResources()
                        .getDimension(R.dimen.dashboard_tile_divider_padding_start));
                int paddingEndOfDivider = convertDpToPx(mContext.getResources()
                        .getDimension(R.dimen.dashboard_general_padding));
                mDivider.setBounds(child.getLeft() + paddingStartOfDivider , top,
                        child.getRight() /*- paddingEndOfDivider*/,
                        top + mDivider.getIntrinsicHeight());
            }
            mDivider.draw(c);
        }
    }

    private int getChildTop(View child) {
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                .getLayoutParams();
        return child.getTop() + params.topMargin + Math.round(ViewCompat.getTranslationY(child));
    }

    private int convertDpToPx(float dp) {
        float notScaledDp = dp / mContext.getResources().getDisplayMetrics().density;
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, notScaledDp,
                mContext.getResources().getDisplayMetrics());
        return Math.round(px);
    }
}
