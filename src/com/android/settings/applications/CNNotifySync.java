package com.android.settings.applications;

import android.util.Log;

import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_ALL;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_INSTALLED;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_NOTIFY;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_NOTIFY_IMPORTANCE;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_NOTIFY_HEADSUP;
import static com.android.settings.SettingsActivity.EXTRA_APPLICATIONS_TYPE_NOTIFY_ALLOWED;
/**
 */

public class CNNotifySync {

    private static CNNotifySync mCNManageAppsSync;
    private OnCallManageApps listener_notify = null;
    private OnCallManageApps listener_notify_importance = null;
    private OnCallManageApps listener_notify_headsup = null;
    private OnCallManageApps listener_notify_allowed = null;

    public static CNNotifySync getInstance(){
        if (mCNManageAppsSync == null)
            mCNManageAppsSync = new CNNotifySync();
        return mCNManageAppsSync;
    }

    public CNNotifySync(){
    }
    private boolean notify_all = false;
    private boolean notify_importance = false;
    private boolean notify_headsup = false;
    private boolean notify_allowed = false;
    private void destroyNotifyAll()
    {
        if(notify_all)
        {
            if(listener_notify != null)
            {
                listener_notify.destroyView();
            }
        }
    }
    private void destroyNotifyImportance()
    {
        if(notify_importance){
            if(listener_notify_importance != null)
            {
                listener_notify_importance.destroyView();
            }
        }
    }
    private void destroyNotifyHeadsup()
    {
        if(notify_headsup){
            if(listener_notify_headsup != null){
                listener_notify_headsup.destroyView();
            }
        }
    }
    private void destroyNotifyAllowed()
    {
        if(notify_allowed){
            if(listener_notify_allowed != null){
                listener_notify_allowed.destroyView();
            }
        }
    }
    public void onPageCreate(int type) {
        if (type == EXTRA_APPLICATIONS_TYPE_NOTIFY) {
            notify_all = true;
            //destroyNotifyImportance();
            //destroyNotifyHeadsup();
            //destroyNotifyAllowed();
        }else if (type == EXTRA_APPLICATIONS_TYPE_NOTIFY_IMPORTANCE) {
            notify_importance = true;
            //destroyNotifyAll();
            //destroyNotifyHeadsup();
            //destroyNotifyAllowed();
        }else if(type == EXTRA_APPLICATIONS_TYPE_NOTIFY_HEADSUP){
            notify_headsup = true;
            //destroyNotifyAll();
            //destroyNotifyImportance();
            //destroyNotifyAllowed();
        }else if(type == EXTRA_APPLICATIONS_TYPE_NOTIFY_ALLOWED){
            notify_allowed = true;
            //destroyNotifyAll();
            //destroyNotifyImportance();
            //destroyNotifyHeadsup();
        }
    }

    public void onPageDestroy(int type) {
        if (type == EXTRA_APPLICATIONS_TYPE_NOTIFY){
            notify_all = false;
        }else if (type == EXTRA_APPLICATIONS_TYPE_NOTIFY_IMPORTANCE){
            notify_importance = false;
        }else if(type == EXTRA_APPLICATIONS_TYPE_NOTIFY_HEADSUP){
            notify_headsup = false;
        }else if(type == EXTRA_APPLICATIONS_TYPE_NOTIFY_ALLOWED){
            notify_allowed = false;
        }
    }

    public void setListener(OnCallManageApps listener, int type){
        if (type == EXTRA_APPLICATIONS_TYPE_NOTIFY){
            listener_notify = listener;
        } else if (type == EXTRA_APPLICATIONS_TYPE_NOTIFY_IMPORTANCE){
            listener_notify_importance = listener;
        }else if(type == EXTRA_APPLICATIONS_TYPE_NOTIFY_HEADSUP){
            listener_notify_headsup = listener;
        }else if(type == EXTRA_APPLICATIONS_TYPE_NOTIFY_ALLOWED){
            listener_notify_allowed = listener;
        }
    }
    public void setSort(int sortOrder){
        if(notify_all){
            if(listener_notify != null){
                listener_notify.setSort(sortOrder, true);
            }
        }else if(notify_importance){
            if(listener_notify_importance != null){
                listener_notify_importance.setSort(sortOrder,true);
            }
        }else if(notify_headsup){
            if(listener_notify_headsup !=null){
                listener_notify_headsup.setSort(sortOrder,true);
            }
        }else if(notify_allowed){
            if(listener_notify_allowed != null){
                listener_notify_allowed.setSort(sortOrder,true);
            }
        }
    }
}
