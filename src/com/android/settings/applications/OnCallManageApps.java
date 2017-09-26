package com.android.settings.applications;

/**
 * Created by Blenda_Fu on 2017/1/3.
 */

public interface OnCallManageApps {
    public void destroyView();
    public void setSort(int type, boolean rebuild);
}
