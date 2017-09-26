package com.asus.suw.lockscreen;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.widget.FrameLayout;

public class SuwRootView extends FrameLayout {

    public SuwRootView(Context context) {
        super(context, null);
    }

    public SuwRootView(final Context context, final AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public SuwRootView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
    }

    @Override
    protected void dispatchRestoreInstanceState(SparseArray<Parcelable> container) {
        try {
            super.dispatchRestoreInstanceState(container);
        } catch (Exception e) {
            Log.d("SuwRootView", "dispatchRestoreInstanceState ERROR");
        }
    }

}