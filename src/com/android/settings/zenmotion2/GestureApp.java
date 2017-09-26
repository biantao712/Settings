package com.android.settings.zenmotion2;

import android.graphics.drawable.Drawable;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import android.provider.Settings;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import com.android.settings.R;
import android.content.pm.ActivityInfo;
import java.util.List;
import android.content.pm.ResolveInfo;
import java.util.Objects;






/**
 * Created by markg on 2016/9/4.
 */
public class GestureApp {
	private static final String TAG = "ZenMotion2_GestureApp";
    private static final String APP_ASUS_BOOSTER = "taskmanager";
    private static final String APP_FRONT_CAMERA = "frontCamera";
    private static final String APP_CAMERA_PACKAGE = "com.asus.camera";
	private static final String WAKE_UP_SCREEN = "wakeUpScreen";
    private static final String NOT_ENABLED = "NotEnabled";
	private static final String APP_ASUS_LAUNCHER3 = "com.asus.launcher3";
    static final String KEY_W_LAUNCH = "w_launch";
    static final String KEY_S_LAUNCH = "s_launch";
    static final String KEY_E_LAUNCH = "e_launch";
    static final String KEY_C_LAUNCH = "c_launch";
    static final String KEY_Z_LAUNCH = "z_launch";
    static final String KEY_V_LAUNCH = "v_launch";
    static AppData V;
    static AppData W;
    static AppData C;
    static AppData S;
    static AppData e;
    static AppData Z;
    static String curGesture;

    private static GestureApp ourInstance = new GestureApp();

    public static GestureApp getInstance() {
        return ourInstance;
    }
    private GestureApp() {
        curGesture=null;
        V=new AppData();
        W=new AppData();
        C=new AppData();
        S=new AppData();
        e=new AppData();
        Z=new AppData();
        /*
        v:app[0]
        c:app[1]
        e:app[2]
        w:app[3]
        s:app[4]
        z:app[5]
        */
    }
    public static void setGestureAppData(String key, AppData data){
        if(key.equalsIgnoreCase(KEY_W_LAUNCH)){
            setGestureWData(data);
        }else if(key.equalsIgnoreCase(KEY_V_LAUNCH)){
            setGestureVData(data);
        }else if(key.equalsIgnoreCase(KEY_C_LAUNCH)){
            setGestureCData(data);
        }else if(key.equalsIgnoreCase(KEY_S_LAUNCH)){
            setGestureSData(data);
        }else if(key.equalsIgnoreCase(KEY_E_LAUNCH)){
            setGestureEData(data);
        }else if(key.equalsIgnoreCase(KEY_Z_LAUNCH)){
            setGestureZData(data);
        }

    }
    public static void setGestureVData(AppData data){
		if(data==null) return;
		V.mIcon=data.mIcon;
		V.mEnabled=data.mEnabled;
		V.mAssigned=data.mAssigned;
		if(data.mActivity!=null)		
        	V.mActivity=new String(data.mActivity);
		else
			V.mActivity=null;
		if(data.mLabel!=null)
			V.mLabel=new String(data.mLabel);
		else
			V.mLabel=null;
		if(data.mPackageActivity!=null)
			V.mPackageActivity=new String(data.mPackageActivity);
		else
			V.mPackageActivity=null;
		if(data.mPackage!=null){
			//Log.i(TAG, "setGestureVData:"+data.packageName);
			V.mPackage=new String(data.mPackage);
			}
		else
			V.mPackage=null;
    }
    public static void setGestureCData(AppData data){
		if(data==null) return;
		C.mIcon=data.mIcon;
		C.mEnabled=data.mEnabled;
		C.mAssigned=data.mAssigned;
		if(data.mActivity!=null)		
        	C.mActivity=new String(data.mActivity);
		else
			C.mActivity=null;
		if(data.mLabel!=null)
			C.mLabel=new String(data.mLabel);
		else
			C.mLabel=null;
		if(data.mPackageActivity!=null)
			C.mPackageActivity=new String(data.mPackageActivity);
		else
			C.mPackageActivity=null;
		if(data.mPackage!=null){
			//Log.i(TAG, "setGestureCData:"+data.packageName);
			C.mPackage=new String(data.mPackage);
			}
		else
			C.mPackage=null;
    }
    public static void setGestureEData(AppData data){
		if(data==null) return;
		e.mIcon=data.mIcon;
		e.mEnabled=data.mEnabled;
		e.mAssigned=data.mAssigned;
		if(data.mActivity!=null)		
        	e.mActivity=new String(data.mActivity);
		else
			e.mActivity=null;
		if(data.mLabel!=null)
			e.mLabel=new String(data.mLabel);
		else
			e.mLabel=null;
		if(data.mPackageActivity!=null)
			e.mPackageActivity=new String(data.mPackageActivity);
		else
			e.mPackageActivity=null;
		if(data.mPackage!=null){
			//Log.i(TAG, "setGestureEData:"+data.packageName);
			e.mPackage=new String(data.mPackage);
			}
		else
			e.mPackage=null;
    }
    public static void setGestureWData(AppData data){
		if(data==null) return;
		W.mIcon=data.mIcon;
		W.mEnabled=data.mEnabled;
		W.mAssigned=data.mAssigned;
		if(data.mActivity!=null)		
        	W.mActivity=new String(data.mActivity);
		else
			W.mActivity=null;
		if(data.mLabel!=null)
			W.mLabel=new String(data.mLabel);
		else
			W.mLabel=null;
		if(data.mPackageActivity!=null)
			W.mPackageActivity=new String(data.mPackageActivity);
		else
			W.mPackageActivity=null;
		if(data.mPackage!=null){
			//Log.i(TAG, "setGestureWData:"+data.packageName);
			W.mPackage=new String(data.mPackage);
			}
		else
			W.mPackage=null;
    }
    public static void setGestureSData(AppData data){
		if(data==null) return;
		S.mIcon=data.mIcon;
		S.mEnabled=data.mEnabled;
		S.mAssigned=data.mAssigned;
		if(data.mActivity!=null)		
        	S.mActivity=new String(data.mActivity);
		else
			S.mActivity=null;
		if(data.mLabel!=null)
			S.mLabel=new String(data.mLabel);
		else
			S.mLabel=null;
		if(data.mPackageActivity!=null){
			//Log.i(TAG, "setGestureSData:"+data.packageName);
			S.mPackageActivity=new String(data.mPackageActivity);
			}
		else
			S.mPackageActivity=null;
		if(data.mPackage!=null)
			S.mPackage=new String(data.mPackage);
		else
			S.mPackage=null;
    }
    public static void setGestureZData(AppData data){
		if(data==null) return;
		Z.mIcon=data.mIcon;
		Z.mEnabled=data.mEnabled;
		Z.mAssigned=data.mAssigned;
		if(data.mActivity!=null)		
        	Z.mActivity=new String(data.mActivity);
		else
			Z.mActivity=null;
		if(data.mLabel!=null)
			Z.mLabel=new String(data.mLabel);
		else
			Z.mLabel=null;
		if(data.mPackageActivity!=null)
			Z.mPackageActivity=new String(data.mPackageActivity);
		else
			Z.mPackageActivity=null;
		if(data.mPackage!=null){
			//Log.i(TAG, "setGestureZData:"+data.packageName);
			Z.mPackage=new String(data.mPackage);
			}
		else
			Z.mPackage=null;
    }

    //----get----
    //from key to get the gesture info
    public static AppData getGestureInfo(String key){
        if(key.equalsIgnoreCase(KEY_W_LAUNCH)){
			//Log.i(TAG, "getGestureInfo:"+key+"("+KEY_W_LAUNCH+")"+":"+W.packageName);
            return W;
        	}
        if(key.equalsIgnoreCase(KEY_V_LAUNCH)){
			//Log.i(TAG, "getGestureInfo:"+key+"("+KEY_V_LAUNCH+")"+":"+V.packageName);
            return V;
        	}
        if(key.equalsIgnoreCase(KEY_C_LAUNCH)){
			//Log.i(TAG, "getGestureInfo:"+key+"("+KEY_C_LAUNCH+")"+":"+C.packageName);
            return C;
        	}
        if(key.equalsIgnoreCase(KEY_S_LAUNCH)){
			//Log.i(TAG, "getGestureInfo:"+key+"("+KEY_S_LAUNCH+")"+":"+S.packageName);
            return S;
        	}
        if(key.equalsIgnoreCase(KEY_E_LAUNCH)){
			//Log.i(TAG, "getGestureInfo:"+key+"("+KEY_E_LAUNCH+")"+":"+e.packageName);
            return e;
        	}
        if(key.equalsIgnoreCase(KEY_Z_LAUNCH)){
			//Log.i(TAG, "getGestureInfo:"+key+"("+KEY_Z_LAUNCH+")"+":"+Z.packageName);
            return Z;
        	}
        return null;
    }
    public AppData getGestureVData(){
		//Log.i(TAG, "return V:"+V.packageName);
        return V;
    }
    public AppData getGestureCData(){
		//Log.i(TAG, "return C:"+C.packageName);
        return C;
    }
    public AppData getGestureEData(){
		//Log.i(TAG, "return e:"+e.packageName);
        return e;
    }
    public AppData getGestureWData(){
		//Log.i(TAG, "return W:"+W.packageName);
        return W;
    }
    public AppData getGestureSData(){
		//Log.i(TAG, "return S:"+S.packageName);
        return S;
    }
    public AppData getGestureZData(){
		//Log.i(TAG, "return Z:"+Z.packageName);
        return Z;
    }
	public static String getSettingsSystemKey(String key) {
        //String key = getKey();
        return KEY_W_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE1_APP :
               KEY_S_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE2_APP :
               KEY_E_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE3_APP :
               KEY_C_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE4_APP :
               KEY_Z_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE5_APP :
               KEY_V_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE6_APP :
               null;
    }

    public static String getSettingsSystemKeyValue(Context context, String key) {
		String systemkey=getSettingsSystemKey(key);
		String keyvalue=Settings.System.getString(context.getContentResolver(),systemkey );
		//Log.i(TAG,"getSettingsSystemKeyValue("+key+":"+systemkey+")="+keyvalue);
        return keyvalue;
    }
	
	public static Drawable getAppIcon(Context context, String packagename){
         Drawable d = null;
         PackageManager pm=context.getPackageManager();
         try {
             d = pm.getApplicationIcon(packagename);
         } catch (PackageManager.NameNotFoundException e) {
             e.printStackTrace();
         }
        return d;
    }
	public static Drawable getAppIcon(Context context, String packageName, String activityName){
		//Log.w(TAG, "getAppIcon+++");
		Drawable d = null;
		PackageManager pm=context.getPackageManager();
		ComponentName cn=new ComponentName(packageName, activityName);
		//Log.w(TAG, "packagename="+packageName+" ,activityName="+activityName);		 
		try {
			 d=pm.getActivityIcon(cn);
		} catch (PackageManager.NameNotFoundException e) {
			 e.printStackTrace();
			 //Log.w(TAG, "getAppIcon---");
			 return null;
		}
		//Log.w(TAG, "getAppIcon---");
		return d;
    }
	
	public static String getAppLabel(Context context, String packageName) {
        PackageManager pkgManager = context.getPackageManager();
        if (pkgManager == null)
            return null;

        PackageInfo pkgInfo;
        try {
            pkgInfo = pkgManager.getPackageInfo(packageName, 0);
            if (pkgInfo == null)
                return null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        ApplicationInfo appInfo = pkgInfo.applicationInfo;
        if (appInfo == null)
            return null;

        CharSequence cs = appInfo.loadLabel(pkgManager);
        if (cs == null)
            return null;

        return cs.toString();
    }
	
	public static String getAppLabel(Context context, String packageName, String activityName) {
        PackageManager pkgManager = context.getPackageManager();
        if (pkgManager == null)
            return null;
		ComponentName cn=new ComponentName(packageName, activityName);
		ActivityInfo ai=null;
        try {
			ai=pkgManager.getActivityInfo(cn,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
		
        CharSequence cs = ai.loadLabel(pkgManager);
        if (cs == null)
            return null;

        return cs.toString();
    }
	 private static boolean supportFrontCamera(PackageManager pm) {
        Intent intent = new Intent("com.asus.camera.action.STILL_IMAGE_FRONT_CAMERA");
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }
	 
	 static void stAsusBoosterData(Context context,AppData appdata) {
	 	appdata.setPackage(APP_ASUS_BOOSTER);
		appdata.setActivity(null);
		appdata.setLabel(context.getString(R.string.app_asus_boost_name));
		appdata.setIcon(context.getDrawable(R.drawable.asus_icon_boost));
	 }
	 static void setFrontCameraData(Context context, AppData appdata) {
            Drawable icon = null;
            try {
                icon = context.getPackageManager().getApplicationIcon(APP_CAMERA_PACKAGE);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
			appdata.setPackage(APP_CAMERA_PACKAGE);
			appdata.setActivity(null);
			appdata.setIcon(icon);
			appdata.setLabel(context.getString(R.string.app_asus_front_camera_name));
    }
	
	//Get App Info for SmartLauncher Weather
	static void setSmartLauncherWeatherData(Context context, AppData appdata) {
		Drawable icon = null;
		try {
			icon = context.getPackageManager().getActivityIcon(new ComponentName(APP_ASUS_LAUNCHER3,"com.asus.zenlife.zlweather.ZLWeatherActivity"));
		} catch (NameNotFoundException e) {
			Log.w(TAG, "Failed getting icon for smartlauncher.");
			e.printStackTrace();
		}
		appdata.setIcon(icon);
		appdata.setLabel(context.getString(R.string.app_asus_front_camera_name));
		appdata.setPackage(APP_ASUS_LAUNCHER3);
		appdata.setActivity("com.asus.zenlife.zlweather.ZLWeatherActivity");
	}
	
	static void setWakeUpScreenData(Context context, AppData appdata) {
		
		appdata.setPackage(WAKE_UP_SCREEN);
		appdata.setActivity(null);
		appdata.setIcon(context.getDrawable(R.drawable.asus_ic_wake_up_screen));
		appdata.setLabel(context.getString(R.string.gesture_wake_up_screen));
	}
	static void setNotEnabledData(Context context, AppData appdata) {
		appdata.setPackage(NOT_ENABLED);
		appdata.setActivity(null);
		appdata.setIcon(null);
		appdata.setLabel(context.getString(R.string.not_enabled));
	}
	public static AppData getSettingsSystemKeyData(Context context, String key){
		AppData data=getGestureInfo(key);
		data.setPackageActivity(getSettingsSystemKeyValue(context,key));
		PackageManager pm = context.getPackageManager();
		String packageName=data.getPackage();
		String activityName=data.getActivity();
		//Log.i(TAG,"getSettingsSystemKeyData:PackageName="+packageName);
		if(packageName.equals(APP_FRONT_CAMERA)){
			if (supportFrontCamera(pm)) {
				setFrontCameraData(context,data);
			}
		}
		else if(packageName.equals(APP_ASUS_BOOSTER)){
			stAsusBoosterData(context,data);
		} else if(packageName.equals(APP_ASUS_LAUNCHER3)){
			setSmartLauncherWeatherData(context,data);
		}else if(packageName.equals(WAKE_UP_SCREEN)){
			setWakeUpScreenData(context,data);
		}else if(packageName.equals(NOT_ENABLED)){
			setNotEnabledData(context,data);
		}
		else{
			data.setIcon(getAppIcon(context,packageName, activityName));
			data.setLabel(getAppLabel(context, packageName, activityName));
		}
		return  data;
	}
	//-------------------
	public static void showInfo(String key){
		AppData data=getGestureInfo(key);
		Log.i(TAG,"key="+key+" PackageName="+data.getPackage()+" ActivityName="+data.getActivity()+" AppName="+data.getName()+
			"\nEnabled="+String.valueOf(data.getEnabled())+" Assigned="+String.valueOf(data.getAssigned())+" PackageActivityName="+data.getPackageActivity());
	}
	public static void showAllInfo(){
		Log.i(TAG,"showAllInfo+++");
		showInfo(KEY_W_LAUNCH);
		showInfo(KEY_V_LAUNCH);
		showInfo(KEY_C_LAUNCH);
		showInfo(KEY_E_LAUNCH);
		showInfo(KEY_S_LAUNCH);
		showInfo(KEY_Z_LAUNCH);
		Log.i(TAG,"showAllInfo---");
	}
}
