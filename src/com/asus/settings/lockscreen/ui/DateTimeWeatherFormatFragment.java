package com.asus.settings.lockscreen.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.android.settings.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.os.SystemProperties;
import android.widget.Switch;
import android.widget.CompoundButton;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.util.MemInfoReader;

import java.util.Locale;
import com.asus.settings.lockscreen.ui.StyleSetting;
import com.asus.settings.lockscreen.ui.TypeItem;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.InstrumentedFragment;


/*
 *from package application
 */
import com.android.settings.applications.*;


public class DateTimeWeatherFormatFragment extends InstrumentedFragment {
    private static final String TAG = "DateTimeWeatherFormat_NF";
    public Activity mActivity = null;
    private View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"Enter onCreate");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"enter onCreateView");
        final Context contextThemeWrapper = new ContextThemeWrapper(getActivity(), R.style.CNManageAppTheme);
        Log.d(TAG,"get context ThemeWrapper");
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);
        Log.d(TAG,"get inflate");
        mRootView = localInflater.inflate(R.layout.view_screenlock_setting, null);
        Log.d(TAG,"setContentView(R.layout.view_screenlock_setting);");
        initViews(mRootView);
        Log.d(TAG,"after init Views");

        Log.d(TAG,"set background color");
        mRootView.setBackgroundColor(getResources().getColor(R.color.dashboard_list_background));
        return mRootView;
    }


    private void initViews(View view) {
        try {
            Log.d(TAG,"initViews");
            mActivity = getActivity();
            if(mActivity == null)
            {
                Log.d(TAG,"mActivity is null");
            }
            else
            {
                Log.d(TAG,"mActivity is not null");
            }
            View mRoot = (View)mRootView.findViewById(R.id.SLockSettingLay);
            if(mRoot != null)
            {
                Log.d(TAG,"SLockSetTypeLay is not null");
            }
            else
            {
                Log.d(TAG,"SLockSetTypeLay is null");
            }

            Log.d(TAG,"get instance of style setting");
            StyleSetting.getInstance().init(getActivity().getApplicationContext(),mRoot,mActivity);

            StyleSetting.getInstance().show();
            Log.d(TAG,"End here.");
        }
        catch (Exception ee)
        {
            Log.d(TAG,"Error : " + ee);
        }
    }

    @Override
    protected int getMetricsCategory() {
        Log.d(TAG,"Enter getMetricsCategory");
        return MetricsEvent.MANAGE_APPLICATIONS;
    }

    @Override
    public void onResume() {
        Log.d(TAG,"Enter onResume");
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"Enter onDestroyView");
        super.onDestroyView();
        mRootView = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG,"Enter onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        Log.d(TAG,"Enter onStop");
        super.onStop();
    }

}
