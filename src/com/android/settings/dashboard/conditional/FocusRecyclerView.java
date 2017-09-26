/*
 * Copyright 2015, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.android.settings.dashboard.DashboardAdapter;

import com.android.settings.R;

/**
 * Version of RecyclerView that can have listeners for onWindowFocusChanged.
 */
public class FocusRecyclerView extends RecyclerView {

    private FocusListener mListener;

    //上一次的触摸点
    private int mLastX, mLastY;
    //当前触摸的item的位置
    private int mPosition;

    //item对应的布局
    private LinearLayout mItemLayout;
    //删除按钮
    private TextView mDelete;

    //最大滑动距离(即删除按钮的宽度)
    private int mMaxLength;
    //是否在垂直滑动列表
    private boolean isDragging;
    //item是在否跟随手指移动
    private boolean isItemMoving;

    //item是否开始自动滑动
    private boolean isStartScroll;
    //删除按钮状态   0：关闭 1：将要关闭 2：将要打开 3：打开
    private int mDeleteBtnState;
    //检测手指在滑动过程中的速度
    private VelocityTracker mVelocityTracker;
    private Scroller mScroller;
    private OnItemClickListener mItemClickListener;

    public FocusRecyclerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mScroller = new Scroller(context, new LinearInterpolator());
        mVelocityTracker = VelocityTracker.obtain();
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (mListener != null) {
            mListener.onWindowFocusChanged(hasWindowFocus);
        }
    }

    public void setListener(FocusListener listener) {
        mListener = listener;
    }

    public interface FocusListener {
        void onWindowFocusChanged(boolean hasWindowFocus);
    }
/*
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        mVelocityTracker.addMovement(e);

        int x = (int) e.getX();
        int y = (int) e.getY();
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d("Jack", "1:onTouchEvent action:ACTION_DOWN");
                if (mDeleteBtnState == 0) {
                    View view = findChildViewUnder(x, y);
                    if (view == null) {
                        return false;
                    }

                    DashboardAdapter.DashboardItemHolder viewHolder = (DashboardAdapter.DashboardItemHolder) getChildViewHolder(view);

                    mItemLayout = viewHolder.layout;
                    mPosition = viewHolder.getAdapterPosition();

                    if(mItemLayout == null){
                        return super.onTouchEvent(e);
                    }

                    mDelete = (TextView) mItemLayout.findViewById(R.id.item_delete);
                    mMaxLength = mDelete.getWidth();
                    mDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mItemClickListener.onDeleteClick(mPosition);
                            mItemLayout.scrollTo(0, 0);
                            mDeleteBtnState = 0;
                        }
                    });
                } else if (mDeleteBtnState == 3){
                    mScroller.startScroll(mItemLayout.getScrollX(), 0, -mMaxLength, 0, 200);
                    invalidate();
                    mDeleteBtnState = 0;
                    return false;
                }else{
                    return false;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                Log.d("Jack", "1:onTouchEvent action:ACTION_MOVE");
                if(mItemLayout == null){
                    return super.onTouchEvent(e);
                }
                int dx = mLastX - x;
                int dy = mLastY - y;

                int scrollX = mItemLayout.getScrollX();
                if (Math.abs(dx) > Math.abs(dy)) {//左边界检测
                    isItemMoving = true;
                    if (scrollX + dx <= 0) {
                        mItemLayout.scrollTo(0, 0);
                        return true;
                    } else if (scrollX + dx >= mMaxLength) {//右边界检测
                        mItemLayout.scrollTo(mMaxLength, 0);
                        return true;
                    }
                    mItemLayout.scrollBy(dx, 0);//item跟随手指滑动
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                Log.d("Jack", "1:onTouchEvent action:ACTION_UP");
                if(mItemLayout == null){
                    return super.onTouchEvent(e);
                }
                if (!isItemMoving  && mItemClickListener != null) {
                    mItemClickListener.onItemClick(mItemLayout, mPosition);
                    return super.onTouchEvent(e);
                }
                isItemMoving = false;

                mVelocityTracker.computeCurrentVelocity(1000);//计算手指滑动的速度
                float xVelocity = mVelocityTracker.getXVelocity();//水平方向速度（向左为负）
                float yVelocity = mVelocityTracker.getYVelocity();//垂直方向速度

                int deltaX = 0;
                int upScrollX = mItemLayout.getScrollX();

                if (Math.abs(xVelocity) > 100 && Math.abs(xVelocity) > Math.abs(yVelocity)) {
                    if (xVelocity <= -100) {//左滑速度大于100，则删除按钮显示
                        deltaX = mMaxLength - upScrollX;
                        mDeleteBtnState = 2;
                    } else if (xVelocity > 100) {//右滑速度大于100，则删除按钮隐藏
                        deltaX = -upScrollX;
                        mDeleteBtnState = 1;
                    }
                } else {
                    if (upScrollX >= mMaxLength / 2) {//item的左滑动距离大于删除按钮宽度的一半，则则显示删除按钮
                        deltaX = mMaxLength - upScrollX;
                        mDeleteBtnState = 2;
                    } else if (upScrollX < mMaxLength / 2) {//否则隐藏
                        deltaX = -upScrollX;
                        mDeleteBtnState = 1;
                    }
                }


                //item自动滑动到指定位置
                mScroller.startScroll(upScrollX, 0, deltaX, 0, 200);
                isStartScroll = true;
                invalidate();

                mVelocityTracker.clear();
                break;

        }

        mLastX = x;
        mLastY = y;
        //return super.onTouchEvent(e);
        return true;
    }
*/
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        mVelocityTracker.addMovement(ev);



        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //Log.d("Jack", "1:onTouchEvent action:ACTION_DOWN");
                if (mDeleteBtnState == 0) {
                    View view = findChildViewUnder(x, y);
                    if (view == null) {
                        return false;
                    }

                    DashboardAdapter.DashboardItemHolder viewHolder = (DashboardAdapter.DashboardItemHolder) getChildViewHolder(view);

                    mItemLayout = viewHolder.layout;
                    mPosition = viewHolder.getAdapterPosition();

                    if(mItemLayout == null){
                        return super.onInterceptTouchEvent(ev);
                    }

                    mDelete = (TextView) mItemLayout.findViewById(R.id.item_delete);
                    mMaxLength = mDelete.getWidth();

                } else if (mDeleteBtnState == 3){
                    if(x > mItemLayout.getWidth() - mDelete.getWidth()){
                        mItemClickListener.onDeleteClick(mPosition);
                    }
//                    mDelete.setOnClickListener(new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            mItemClickListener.onDeleteClick(mPosition);
//                            mItemLayout.scrollTo(0, 0);
//                            mDeleteBtnState = 0;
//                        }
//                    });
                    mScroller.startScroll(mItemLayout.getScrollX(), 0, -mMaxLength, 0, 200);
                    invalidate();
                    mDeleteBtnState = 0;
                    return false;
                }else{
                    return false;
                }

                break;
            case MotionEvent.ACTION_MOVE:
                //Log.d("Jack", "1:onTouchEvent action:ACTION_MOVE");
                if(mItemLayout == null){
                    return super.onInterceptTouchEvent(ev);
                }
                int dx = mLastX - x;
                int dy = mLastY - y;

                int scrollX = mItemLayout.getScrollX();
                if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 5 ) {//左边界检测
                    isItemMoving = true;
                    if (scrollX + dx <= 0) {
                        mItemLayout.scrollTo(0, 0);
                        mDeleteBtnState = 0;
                        return true;
                    } else if (scrollX + dx >= mMaxLength) {//右边界检测
                        mItemLayout.scrollTo(mMaxLength, 0);
                        return true;
                    }
                    mItemLayout.scrollBy(dx, 0);//item跟随手指滑动
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //Log.d("Jack", "1:onTouchEvent action:ACTION_UP");
                if(mItemLayout == null){
                    return super.onInterceptTouchEvent(ev);
                }
                if (!isItemMoving /*&& !isDragging */ && mItemClickListener != null && mDeleteBtnState == 0) {
                    mItemClickListener.onItemClick(mItemLayout, mPosition);
                    return super.onInterceptTouchEvent(ev);
                }
                isItemMoving = false;

                mVelocityTracker.computeCurrentVelocity(1000);//计算手指滑动的速度
                float xVelocity = mVelocityTracker.getXVelocity();//水平方向速度（向左为负）
                //Log.d("Jack", "xVelocity = " + xVelocity);
                float yVelocity = mVelocityTracker.getYVelocity();//垂直方向速度

                int deltaX = 0;
                int upScrollX = mItemLayout.getScrollX();

                if (Math.abs(xVelocity) > 100 && Math.abs(xVelocity) > Math.abs(yVelocity)) {
                    if (xVelocity <= -100) {//左滑速度大于100，则删除按钮显示
                        deltaX = mMaxLength - upScrollX;
                        mDeleteBtnState = 2;
                    } else if (xVelocity > 100) {//右滑速度大于100，则删除按钮隐藏
                        deltaX = -upScrollX;
                        mDeleteBtnState = 1;
                    }
                } else {
                    if (upScrollX >= mMaxLength / 2) {//item的左滑动距离大于删除按钮宽度的一半，则则显示删除按钮
                        deltaX = mMaxLength - upScrollX;
                        mDeleteBtnState = 2;
                    } else if (upScrollX < mMaxLength / 2) {//否则隐藏
                        deltaX = -upScrollX;
                        mDeleteBtnState = 1;
                    }
                }

                //item自动滑动到指定位置
                mScroller.startScroll(upScrollX, 0, deltaX, 0, 200);
                isStartScroll = true;
                invalidate();

                mVelocityTracker.clear();
                break;

        }

        mLastX = x;
        mLastY = y;

        //Log.d("Jack", "mDeleteBtnState = " + mDeleteBtnState);
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        //Log.d("Jack","computeScrollOffset" + mScroller.computeScrollOffset());
        //Log.d("Jack","isStartScroll" + isStartScroll);
        if (mScroller.computeScrollOffset()) {
            mItemLayout.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        } else if (isStartScroll) {
            isStartScroll = false;
            if (mDeleteBtnState == 1) {
                mDeleteBtnState = 0;
            }

            if (mDeleteBtnState == 2) {
                mDeleteBtnState = 3;
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        mVelocityTracker.recycle();
        super.onDetachedFromWindow();
    }

//    @Override
//    public void onScrollStateChanged(int state) {
//        super.onScrollStateChanged(state);
//        isDragging = state == SCROLL_STATE_DRAGGING;
//    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public interface OnItemClickListener {
        /**
         * item点击回调
         *
         * @param view
         * @param position
         */
        void onItemClick(View view, int position);

        /**
         * 删除按钮回调
         *
         * @param position
         */
        void onDeleteClick(int position);
    }

}
