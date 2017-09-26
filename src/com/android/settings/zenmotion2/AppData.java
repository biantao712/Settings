package com.android.settings.zenmotion2;

import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.util.Log;



/**
 * Created by markg on 2016/9/4.
 * This is used to store the app data for gesture, like package/ activity name, app name, icon and wheather the gesture has been assigned app.
 */
public class AppData extends AppName{
	private static final String TAG = "ZenMotion2_AppData";
	//public AppName mName;//name.label, name.pinYin, name.pinYinHeadChar, name.package, name.activity, name.packageActivity
	public Drawable mIcon; // ap icon
	public boolean mEnabled; // wheather the gesture has been assigned app and enabled
	public boolean mAssigned;//the gesture not assigned "app" or "not enabled", initial state
	//---constructor---
	public AppData(){
		//mName=new AppName();// all names are null.
		super();
		mIcon=null;
		mEnabled=false;
		mAssigned=false;
	}
	public AppData(String name){
		super(name);
		mEnabled=false;
		mAssigned=false;
	}
	public AppData(String packageName, String activityName, CharSequence label, Drawable icon) {
		//mName=new AppName(packageName, activityName, label);
		super(packageName, activityName, label);
		mEnabled=false;
		mIcon = icon;
		mAssigned=false;
	}

	public AppData(String packageName, String activityName, CharSequence label){
		//mName=new AppName(packageName, activityName, label);
		super(packageName, activityName, label);
		mEnabled=false;
		mIcon = null;
		mAssigned=false;
	}
	public AppData(String label, String pinYinName,String packageActivity, Drawable icon) {
		//mName=new AppName(packageName, activityName, label);
		super(label, pinYinName, packageActivity);
		mEnabled=false;
		mIcon = icon;
		mAssigned=false;
	}
	//----------set---------------------
	public void setDrawable(Drawable appicon){
		mIcon=appicon;
	}
	public void setIcon(Drawable appicon){
		mIcon=appicon;
	}
	public void setEnabled(boolean enabled){
		mEnabled=enabled;
	}
	public void setAssigned(boolean assigned){
		mAssigned=assigned;
	}
	
	//----------get---------------------
	public Drawable getIcon(){
		return mIcon;
	}
	public boolean getEnabled(){
		return mEnabled;
	}
	public boolean getAssigned(){
		return mAssigned;
	}
	//-----other-----------------
	public boolean checkStatus(){
		return mEnabled;
	}
}

