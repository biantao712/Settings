package com.android.settings.applications;

import android.util.Log;

import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_ALL;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_BACKGROUND;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_INSTALLED;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_RUNNING_SERVICE;

/**
 * Created by Blenda_Fu on 2017/1/16.
 */

public class CNManageAppsSync {

    private static CNManageAppsSync mCNManageAppsSync;
    private OnCallManageApps listener_Installed = null;
    private OnCallManageApps listener_All = null;
    private OnCallServicePage listener_Running = null;
    private OnCallServicePage listener_Background = null;

    public static CNManageAppsSync getInstance(){
        if (mCNManageAppsSync == null)
            mCNManageAppsSync = new CNManageAppsSync();
        return mCNManageAppsSync;
    }

    public CNManageAppsSync(){
    }
    private boolean f_installed_in = false;
    private boolean f_all_in = false;

    public void onPageCreate(int type) {
        Log.d("blenda", "onPageCreate, type: "+type+"; f_installed_in: "+f_installed_in+"; f_all_in: "+f_all_in);
        if (type == EXTRA_APPLICATIONS_TYPE_INSTALLED) {
            f_installed_in = true;
            if (f_all_in){
                if (listener_All != null)
                    listener_All.destroyView();
            }
        }
        else if (type == EXTRA_APPLICATIONS_TYPE_ALL) {
            f_all_in = true;
            if (f_installed_in){
                if (listener_Installed!=null)
                    listener_Installed.destroyView();
            }
        }
    }

    public void onPageDestroy(int type) {
        if (type == EXTRA_APPLICATIONS_TYPE_INSTALLED)
            f_installed_in = false;
        else if (type == EXTRA_APPLICATIONS_TYPE_ALL)
            f_all_in = false;

        Log.d("blenda", "onPageDestroy, type: "+type+"; f_installed_in: "+f_installed_in+"; f_all_in: "+f_all_in);
    }

    public void setListener(Object listener, int type){
        if (type == EXTRA_APPLICATIONS_TYPE_INSTALLED){
            listener_Installed = (OnCallManageApps)listener;
        } else if (type == EXTRA_APPLICATIONS_TYPE_ALL){
            listener_All = (OnCallManageApps)listener;
        } else if (type == EXTRA_APPLICATIONS_TYPE_RUNNING_SERVICE){
            listener_Running = (OnCallServicePage)listener;
        } else if (type == EXTRA_APPLICATIONS_TYPE_BACKGROUND){
            listener_Background = (OnCallServicePage)listener;
        }
    }
    public void setSort(int sortOrder){

        if (f_installed_in) {
            if (listener_All != null)
            listener_All.setSort(sortOrder, false);
            if (listener_Installed != null)
            listener_Installed.setSort(sortOrder, true);
        }else if (f_all_in) {
            if (listener_All != null)
            listener_All.setSort(sortOrder, true);
            if (listener_Installed != null)
            listener_Installed.setSort(sortOrder, false);
        }
    }
    private int mSelectedTabPos = 0;
    public void setSelectedTabPos(int pos){
        mSelectedTabPos = pos;
    }
    public int getSelectedTabPos(){
        return mSelectedTabPos;
    }

    public void onTabSelectedChange(int pos, boolean select){
        if (select) {
            setSelectedTabPos(pos);
            if (pos == 1) {
                if (listener_Running != null)
                    listener_Running.onLoad();
            } else if (pos == 2) {
                if (listener_Background != null)
                    listener_Background.onLoad();
            }
        }else{
            if (pos == 1) {
                if (listener_Running != null)
                    listener_Running.onUnLoad();
            } else if (pos == 2) {
                if (listener_Background != null)
                    listener_Background.onUnLoad();
            }
        }
    }
}
