package com.android.settings.zenmotion2;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.util.Pair;

public class AppName {

    public String mLabel;//ap name
    public String mPinYin;// ap pinyin name
    public String mPinYinHeadChar;
    public String mPackageActivity;//ap package/ Activity name, which is used to launch app
	public String mPackage;
    public String mActivity;
	//ASUS App app name/ packagename
	public static final String APP_ASUS_BOOSTER = "taskmanager";
	public static final String APP_FRONT_CAMERA = "frontCamera";
	public static final String APP_CAMERA_PACKAGE = "com.asus.camera";
	public static final String SYMBOL = "/";
	public static final String WAKE_UP_SCREEN = "wakeUpScreen";
	public static final String NOT_ENABLED = "NotEnabled";
	//only index cheader character
	public AppName(){
		super();
		mLabel = null;
		mPinYin=null;
		mPinYinHeadChar=null;
		mPackageActivity=null;
		mPackage=null;
		mActivity=null;
	}

    public AppName(String name) {
        super();
        mLabel = name;
        mPinYin=StringHelper.getPingYin((String)mLabel);
        mPinYinHeadChar=mPinYin.substring(0,1);
        mPackageActivity=null;
		mPackage=null;
		mActivity=null;
    }

    public AppName(String name, String pinYinName) {
        super();
        mLabel = name;
        mPinYin = pinYinName;
        mPinYinHeadChar = mPinYin.substring(0, 1);//get firstletter
        mPackageActivity=null;
		mPackage=null;
		mActivity=null;
    }

	public AppName(String name, CharSequence packageActivityName){
        super();
		mLabel=name;
		mPinYin=StringHelper.getPingYin((String)mLabel);
        mPinYinHeadChar=mPinYin.substring(0,1);
		setPackageActivity((String)packageActivityName);		
	}

    public AppName(String name, String pinYinName, String packageActivityName){
        mLabel = name;
        mPinYin = pinYinName;
        mPinYinHeadChar = mPinYin.substring(0, 1);//get firstletter
		setPackageActivity(packageActivityName);
        //this.packageActivity=packageActivityName;
		//this.package=null;
		//this.activity=null;
    }

	public AppName(String packageName, String activityName, CharSequence name){
		mPackage=packageName;
		mActivity=activityName;
		mLabel=(String)name;
        mPinYin=StringHelper.getPingYin((String)mLabel);
        mPinYinHeadChar=mPinYin.substring(0,1);
        setPackageActivity();
	}
    //---set---
    public void setName(String name){
        mLabel = name;
    }
    public void setLabel(String name) {
        mLabel = name;
    }

    public void setPinYin(String pinYinName) {
        mPinYin = pinYinName;
        mPinYinHeadChar = mPinYin.substring(0, 1);//get firstletter
    }
	private Pair<String, String> parsePackageAndActivity(String savedValue) {
        if (APP_ASUS_BOOSTER.equals(savedValue)) return Pair.create(APP_ASUS_BOOSTER, null);
		if (WAKE_UP_SCREEN.equals(savedValue)) return Pair.create(WAKE_UP_SCREEN, null);
        if (APP_FRONT_CAMERA.equals(savedValue)) return Pair.create(APP_FRONT_CAMERA, null);
		if (NOT_ENABLED.equals(savedValue)) return Pair.create(NOT_ENABLED, null);
        if (null != savedValue && savedValue.contains(SYMBOL)) {
            String[] splitedValue = savedValue.split(SYMBOL);
            return Pair.create(splitedValue[0], splitedValue[1]);
        } else {
            // Use asus booster as default value
            return Pair.create(APP_ASUS_BOOSTER, null);
        }
    }
    public void setPackageActivity(String packageActivityName){
        mPackageActivity=packageActivityName;
		if(mPackageActivity==null){
			mPackage=null;
			mActivity=null;
		}
		else{
			Pair<String, String> pair = parsePackageAndActivity(packageActivityName);
			mPackage =pair.first;
			mActivity=pair.second;
		}
    }
	public void setPackageActivity(){
        mPackageActivity = (mActivity == null
                ? mPackage
                : mPackage + "/" + mActivity);
    }

	public void setPackage(String packageName){
		mPackage=packageName;
	}
	public void setActivity(String activityName){
		mActivity=activityName;
	}
    //---get---
    public String getName(){
	    return mLabel;
    }
    public String getLabel() {
        return mLabel;
    }
    public String getPinYin() {
        return mPinYin;
    }
    public String getPinYinHeadChar (){
        return mPinYinHeadChar;//get firstletter
    }
    public String getPackageActivity(){
        return mPackageActivity;
    }
	public String getPackage(){
		return mPackage;
	}
	public String getActivity(){
		return mActivity;
	}
    //---other---
}

