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




public class DateTimeWeatherFormatActivity extends Activity {
    private static final String TAG = "DateTimeWeatherFormatActivity";
    public Activity mActivity = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            Log.d(TAG,"oncreate");
            super.onCreate(savedInstanceState);
            Log.d(TAG,"after super.onCreate");
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.view_screenlock_setting);
            // set status bar value.
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.TRANSPARENT);
            // set status bar value.

            Log.d(TAG,"setContentView(R.layout.view_screenlock_setting);");
            mActivity = this;
            View mRoot = (View)findViewById(R.id.SLockSettingLay);
            if(mRoot != null)
            {
                Log.d(TAG,"SLockSetTypeLay is not null");
            }
            else
            {
                Log.d(TAG,"SLockSetTypeLay is null");
            }

            Log.d(TAG,"get instance of style setting");
            StyleSetting.getInstance().init(getApplicationContext(),mRoot,mActivity);

            StyleSetting.getInstance().show();
            Log.d(TAG,"End here.");
        }
        catch (Exception ee)
        {
            Log.d(TAG,"Error : " + ee);
        }
    }

    @Override
    public void onDestroy(){
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }



}
