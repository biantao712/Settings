package com.android.settings.zenmotion2;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.content.ComponentName;
import com.android.settings.R;
import android.util.Log;



/**
 * Created by mark_guo on 2016/9/1.
 */

/**
 * This is used to get all app info in device, like package/ activity name, icon, app name.
 */
public class AppInfo extends AppData{
	private static final String TAG = "ZenMotion2_AppInfo";
	//public AppData Data;
	//---constructor---
	public AppInfo(){
	//Data = new AppData();
	super();
	}
	AppInfo(String packageName, String activityName, CharSequence label, Drawable icon) {
		super(packageName, activityName, label, icon);
	}
	AppInfo(String packageName, String activityName, CharSequence label){
		super(packageName, activityName, label);
	}
	//---get---
	static AppInfo getAsusBooster(Context context) {
		return new AppInfo(APP_ASUS_BOOSTER, null,
				context.getString(R.string.app_asus_boost_name),
				context.getDrawable(R.drawable.asus_icon_boost));
	}
	static AppInfo getFrontCamera(Context context) {
		Drawable icon = null;
		try {
			icon = context.getPackageManager().getApplicationIcon(APP_CAMERA_PACKAGE);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}
		return new AppInfo(APP_FRONT_CAMERA, null,
				context.getString(R.string.app_asus_front_camera_name), icon);
	}
	//Get App Info for SmartLauncher Weather
	static AppInfo getSmartLauncherWeather(Context context) {
		Drawable icon = null;
		try {
			icon = context.getPackageManager().getActivityIcon(new ComponentName("com.asus.launcher3","com.asus.zenlife.zlweather.ZLWeatherActivity"));
		}catch (PackageManager.NameNotFoundException e) {
			//Log.w(TAG, "Failed getting icon for smartlauncher.");
			e.printStackTrace();
		}
		return new AppInfo("com.asus.launcher3", "com.asus.zenlife.zlweather.ZLWeatherActivity", context.getString(R.string.app_asus_smartlauncher_weather_name), icon);
	}
	static AppInfo getWakeUpScreen(Context context) {
		return new AppInfo(WAKE_UP_SCREEN, null,
				context.getString(R.string.gesture_wake_up_screen),
				context.getDrawable(R.drawable.asus_ic_wake_up_screen));
	}
	static AppInfo getNotEnabled(Context context) {
		return new AppInfo(NOT_ENABLED, null,
				context.getString(R.string.not_enabled),
				context.getDrawable(R.drawable.asus_ic_wake_up_screen));
	}
	//---set---
	//---others---
	static AppInfo convertFromResolveInfo(ResolveInfo resolveInfo, PackageManager pm) {
		//Log.w(TAG, "convertFromResolveInfo+++");
		//Log.w(TAG, "packagename="+resolveInfo.activityInfo.packageName+" ,activityName="+resolveInfo.activityInfo.name);
		//Log.w(TAG, "convertFromResolveInfo---");
		return new AppInfo(resolveInfo.activityInfo.packageName,
				resolveInfo.activityInfo.name, resolveInfo.activityInfo.loadLabel(pm),
				resolveInfo.activityInfo.loadIcon(pm));
	}
}

