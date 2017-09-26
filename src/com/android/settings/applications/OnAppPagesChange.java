package com.android.settings.applications;

/**
 * Created by Blenda_Fu on 2017/1/3.
 */

public interface OnAppPagesChange {
    public void onPageCreate(int type);
    public void onPageDestroy(int type);
}
