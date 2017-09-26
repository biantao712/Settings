package com.asus.settings.lockscreen.ui;

/**
 * Created by Wesley_Sun on 2017/2/17.
 */
//package com.asus.screenlock.setting;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.R;
import android.app.Activity;
import android.app.ActivityManager;

import com.asus.settings.lockscreen.ui.TypeItem;

/**
 * Created by Jason5_Li on 2016/2/25.
 * 时间天气样式界面
 */
public class StyleSetting implements View.OnClickListener,TypeItem.TypeItemListener  {
    public static final String TAG = "StyleSetting";
    private static StyleSetting mStyleSetting;
    public Activity mActivity = null;

    // +++++++++these string is used for communicate with System UI and LockScreen.
    public static final String SETTINGS_SYSTEM_ASUS_LOCKSCREEN_DATE_TIME_WEATHER_FORMAT =
            "SETTINGS_SYSTEM_ASUS_LOCKSCREEN_DATE_TIME_WEATHER_FORMAT";
    // ---------these string is used for communicate with System UI and LockScreen.



    /**
     * 获取实例
     *
     * @return StyleSetting
     */
    public static StyleSetting getInstance() {
        Log.d(TAG, "getInstance");
        if (mStyleSetting == null) {
            Log.d(TAG, "it is null,create it");
            mStyleSetting = new StyleSetting();
        }
        return mStyleSetting;
    }

    //private MainSetting.SettingListener mSettingListener;
    private static final int ChangeChecked = 12000;
    public TypeItem mTypeItem1 = null;
    public TypeItem mTypeItem2 = null;

    private Context mPkgContext = null;
    private LinearLayout mRootView = null;
    private ImageView mBackBtn = null;
    private TextView mTitle = null;

    private Handler mHandler;

    /**
     * 初始化
     */
    public void init(Context pkgContext, View rootView,Activity parentActivity) {
        try {
            Log.d(TAG, "enter init");
            mPkgContext = pkgContext;
            mActivity = parentActivity;
            if(mPkgContext == null || mActivity == null)
            {
                Log.d(TAG,"Error mPkgContext or mActivity is null");
            }
            else
            {
                Log.d(TAG,"valid mPkgContext and mActivity");
            }
            mTitle = ((TextView) rootView.findViewById(R.id.SLockSetTitleText));
            if (mTitle == null) {
                Log.d(TAG, "invalid mTitle view");
            }
            else
            {
                Log.d(TAG, "valid mTitle view");
            }
            mBackBtn = ((ImageView) rootView.findViewById(R.id.SLockSetTitleBackBtn));
            if (mBackBtn == null) {
                Log.d(TAG, "invalid mBackBtn view");
            }
            else
            {
                Log.d(TAG, "valid mBackBtn view");
                mBackBtn.setOnClickListener(this);
            }

            mTypeItem1 = (TypeItem) rootView.findViewById(R.id.SLockSetStyle1);
            if (mTypeItem1 == null) {
                Log.d(TAG, "invalid mTypeItem1 view");
            }
            else
            {
                Log.d(TAG, "valid mTypeItem1 view");
            }
            mTypeItem2 = (TypeItem) rootView.findViewById(R.id.SLockSetStyle2);
            if (mTypeItem2 == null) {
                Log.d(TAG, "invalid mTypeItem2 view");
            }
            else
            {
                Log.d(TAG, "valid mTypeItem2 view");
            }

            mRootView = (LinearLayout) rootView.findViewById(R.id.SLockSetTypeLay);
            if (mRootView == null) {
                Log.d(TAG, "invalid root view");
            }
            else
            {
                Log.d(TAG, "valid mRootView view");
            }

            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    Log.d(TAG,"handle message");
                    if (msg.what == ChangeChecked) {
                        Log.d(TAG,"handle message ChangeChecked");
                        changeChecked(msg.arg1);
                    }
                    else
                    {
                        Log.d(TAG,"<TODO>:: unkonwn message");
                    }
                }
            };
        } catch (Exception ee) {
            Log.d(TAG, "Error : " + ee);
        }
    }

//    public void setSettingListener(MainSetting.SettingListener listener) {
//        Log.d(TAG,"Set listener and do noting");
//        //mSettingListener = listener;
//    }

    private void changeChecked(int id) {
        try {
            Log.d(TAG, "changeChecked>>" + id);
            if (0 == id) {
                mTypeItem1.setChecked(true);
                mTypeItem1.setTypeImage(R.drawable.asus_type_01);
                mTypeItem2.setChecked(false);
                mTypeItem2.setTypeImage(R.drawable.asus_type_02_1);
                Settings.System.putInt(mPkgContext.getContentResolver(),
                        SETTINGS_SYSTEM_ASUS_LOCKSCREEN_DATE_TIME_WEATHER_FORMAT, 0);
                Log.d(TAG,"set date time weather format to SETTINGS_SYSTEM_ASUS_LOCKSCREEN_DATE_TIME_WEATHER_FORMAT " + 0);
                //WeatherManager.getInstance().styleChangedToNew();
            } else {
                mTypeItem1.setChecked(false);
                mTypeItem1.setTypeImage(R.drawable.asus_type_01_1);
                mTypeItem2.setChecked(true);
                mTypeItem2.setTypeImage(R.drawable.asus_type_02);

                //WeatherManager.getInstance().styleChangedToNormal();
                Settings.System.putInt(mPkgContext.getContentResolver(),
                        SETTINGS_SYSTEM_ASUS_LOCKSCREEN_DATE_TIME_WEATHER_FORMAT, 1);
                Log.d(TAG,"set date time weather format to SETTINGS_SYSTEM_ASUS_LOCKSCREEN_DATE_TIME_WEATHER_FORMAT " + 1);
            }
        } catch (Exception ee) {
            Log.d(TAG, "Error : " + ee);
        }
    }

    private void changeCheckedH(int id) {
        Log.d(TAG,"enter changeCheckedH");
        try {
            Log.d(TAG,"Send message ChangeChecked, id :" + id);
            Message message = new Message();
            message.what = ChangeChecked;
            message.arg1 = id;
            mHandler.sendMessage(message);
        } catch (Exception ee) {
            Log.d(TAG, "Exception :" + ee);
        }
    }

    /**
     * 界面显示
     * TypeItem 控件采用ViewStub方式，只有显示时，才会真正绘制
     *
     * @see TypeItem
     */
    public void show() {
        try {
            Log.d(TAG,"Enter show");
            if (mRootView != null) {
                Log.d(TAG,"mRootView is not null");
                mRootView.setVisibility(View.VISIBLE);
            } else {
                Log.d(TAG, "root view is null");
            }

            mTitle.setText(R.string.StyleItem);

            if (mTypeItem1.showReal()) {
                mTypeItem1.setTypeName(mPkgContext.getResources().getString(R.string.StyleOne));
                mTypeItem1.setTypeImage(R.drawable.asus_type_01);
                mTypeItem1.setTypeItemListener(this);
            }
            else
            {
                Log.d(TAG,"mTypeItem1 showReal fail");
            }

            if (mTypeItem2.showReal()) {
                mTypeItem2.setTypeName(mPkgContext.getResources().getString(R.string.StyleTwo));
                mTypeItem2.setTypeImage(R.drawable.asus_type_02);
                mTypeItem2.setTypeItemListener(this);
            }
            else
            {
                Log.d(TAG,"mTypeItem2 showReal fail");
            }

            //int type = WeatherManager.getInstance().getWeatherStyle();
            int type = Settings.System.getInt(mPkgContext.getContentResolver(), SETTINGS_SYSTEM_ASUS_LOCKSCREEN_DATE_TIME_WEATHER_FORMAT, 1);
            Log.d(TAG, "init original value of date time weather format in show() is : " + type);
            Log.d(TAG, "getWeatherStyle >>" + type);
            if (0 == type) {
                Log.d(TAG,"type 0: set Item1 checked");
                mTypeItem1.setChecked(true);
                mTypeItem1.setTypeImage(R.drawable.asus_type_01);
                mTypeItem2.setChecked(false);
                mTypeItem2.setTypeImage(R.drawable.asus_type_02_1);
            } else {
                Log.d(TAG,"type 1: set Item2 checked");
                mTypeItem1.setChecked(false);
                mTypeItem1.setTypeImage(R.drawable.asus_type_01_1);
                mTypeItem2.setChecked(true);
                mTypeItem2.setTypeImage(R.drawable.asus_type_02);
            }
        } catch (Exception ee) {
            Log.d(TAG, "Error : " + ee);
        }
    }

    public void hide() {
        Log.d(TAG,"enter hide");
        if (mRootView != null) {
            Log.d(TAG,"mRootView is nut null");
            mRootView.setVisibility(View.GONE);
        } else {
            Log.d(TAG, " root view is null");
        }
    }


    @Override
    public void onCheckChanged(View view, boolean check) {
        Log.d(TAG, "onCheckChanged view1>>" + view);
//        LogL.d("onCheckChanged view2>>"+mTypeItem1);
//        LogL.d("onCheckChanged X>>" + view.equals(mTypeItem1));
//        if (mSettingListener != null) {
//            mSettingListener.userActivity();
//        }
        if(mBackBtn != null) {
            if (view.equals(mBackBtn)) {
                Log.d(TAG, "Back button is clicked");
                return;
            }
        }

        changeCheckedH(view.equals(mTypeItem1) ? 0 : 1);
    }

    /**
     * 重绘字符串
     */
    public void reloadStrings() {
        mTypeItem1.setTypeName(mPkgContext.getResources().getString(R.string.StyleOne));
        mTypeItem2.setTypeName(mPkgContext.getResources().getString(R.string.StyleTwo));
        mTypeItem1.reloadString();
        mTypeItem2.reloadString();
    }

    /**
     * 按键消息处理
     * @param v View
     *          mBackBtn 返回按钮
     *          mStyleBtn 样式按钮
     */
    @Override
    public void onClick(View v) {
        try {
            Log.d(TAG,"Enter onclcik");
            if (v.equals(mBackBtn)) {
                Log.d(TAG, "is back button clicked ,return here.");
                mActivity.finish();
            } else {
                Log.d(TAG, "unknown button click");
            }
        }
        catch(Exception ee)
        {
            Log.d(TAG,"Error : " + ee);
        }
    }


}
