package com.android.settings.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import java.util.List;
import java.lang.reflect.Method;

import com.android.settings.R;

public class VerizonHelpUtils {
    public static final String INTRODUCTION_DIALOG = "INTRODUCTION_DIALOG";
    public static final String STEP_DIALOG = "STEP_DIALOG";
    public static final int TUTORIAL_DISABLE = 0;
    public static final int TUTORIAL_ENABLE  = 1;
    public static final int STEP_RESET = -1;
    public static final int STEP_0 = 0;
    public static final int STEP_1 = 1;
    public static final int STEP_2 = 2;
    public static final int STEP_3 = 3;
    public static final int STEP_4 = 4;
    public static final String TUTORIAL_WIFI = "Wi-Fi";
    public static final String TUTORIAL_BLUETOOTH = "Bluetooth";

    private static final String Tutorial_SETTINGS_URI = "content://com.asus.vzwhelp/tipSettings/";
    private static final String CP_KEY_TUTORIAL_STATUS = "TIP_STATUS";
    private static final String SP_NAME = "Help_Tutorial";
    private static final String SP_KEY_TUTORIAL_STEP = "Tutorial_Step_";
    private static final String SP_KEY_TUTORIAL_STATUS = "Tutorial_STATUS_";
    private static final String SP_KEY_TUTORIAL_POP_TIME = "Tutorial_Pop_Time_";
    private static final int TUTORIAL_DIALOG_WIDTH = 900;

    public static final String SCREEN_WIFI = "Wi-Fi";
    public static final String SCREEN_BLUETOOTH = "Bluetooth";
    public static final String SCREEN_DATA = "Data";
    public static final String SCREEN_TETHER = "Mobile Hotspot";
    public static final String VERIZON_SYSTEM_PROPERTY = "ro.asus.is_verizon_device"; 
    public static final String VERIZON_MACHINE = "1";
    
    public static AlertDialog dialog;
    public static int mStep = STEP_RESET;
    public static int isVerizonSKU =-1;

    public static int isTutorialEnable(Context context, String tutorialName) {
        String uri = Tutorial_SETTINGS_URI + tutorialName;
        Cursor cursor = context.getContentResolver().query(
                Uri.parse(uri), null , null, null, null );
        int status = TUTORIAL_DISABLE;
        if (cursor != null) {
            try {
                cursor.moveToNext();
                status = cursor.getInt(1);
            } finally {
                cursor.close();
            }
        } else {
            SharedPreferences pref = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            status = pref.getInt(SP_KEY_TUTORIAL_STATUS+tutorialName, TUTORIAL_DISABLE);
        }
        return status;
    }

    public static void setTutorialSetting(Context context, String tutorialName, int tutorialStatus) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(CP_KEY_TUTORIAL_STATUS, tutorialStatus);
        try { //catch exception if Help has been uninstalled
            context.getContentResolver().update(Uri.parse(Tutorial_SETTINGS_URI + tutorialName), contentValues, null, null);
        } catch (Exception e) {
            SharedPreferences pref = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
            pref.edit().putInt(SP_KEY_TUTORIAL_STATUS+tutorialName, tutorialStatus).commit();
        }
    }

    public static int getTutorialStep(Context context, String tutorialName) {
        SharedPreferences pref = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return pref.getInt(SP_KEY_TUTORIAL_STEP+tutorialName, STEP_RESET);
    }

    public static void setTutorialStep(Context context, String tutorialName, int step) {
        SharedPreferences pref = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        pref.edit().putInt(SP_KEY_TUTORIAL_STEP+tutorialName, step).commit();
    }

    public static void DisplayTutorial(Context context, String tutorialName, int step, DialogInterface.OnClickListener clicklistener) {
        if (getTutorialStep(context, tutorialName) > STEP_RESET && getTutorialStep(context, tutorialName) < step) {
            String[] title = null;
            String[] content = null;
            if (tutorialName.equals(TUTORIAL_WIFI)) {
                if (isWifiConnected(context)) {
                    if ( step == STEP_3 || (step == STEP_4 && mStep < STEP_1)) return;
                    step = STEP_4;
                }
                title = context.getResources().getStringArray(R.array.tutorial_wifi_step_title);
                content = context.getResources().getStringArray(R.array.tutorial_wifi_step_content);
            } else if (tutorialName.equals(TUTORIAL_BLUETOOTH)) {
                title = context.getResources().getStringArray(R.array.tutorial_bluetooth_step_title);
                content = context.getResources().getStringArray(R.array.tutorial_bluetooth_step_content);
            }
            showTutorialDialog(context, STEP_DIALOG, clicklistener, tutorialName, step, title[step-1], content[step-1]);
        }
    }

    public static void showTutorialDialog(final Context context, String dialogType,
            DialogInterface.OnClickListener clicklistener, final String tutorialName, int step,
            CharSequence title, CharSequence message) {
        mStep = step;
        setTutorialStep(context, tutorialName, step);
        LayoutInflater layoutInflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogLayout = layoutInflater.inflate(R.layout.tutorial_layout, null);
        if (dialog!= null && dialog.isShowing()) {
            dialog.dismiss();
        }
        if (dialogType.equals(INTRODUCTION_DIALOG)) {
            dialog = new AlertDialog.Builder(context)
                .setView(dialogLayout)
                .setPositiveButton(R.string.tutorial_continue, clicklistener)
                .setNegativeButton(R.string.tutorial_close, clicklistener)
                .create();
            CheckBox checkbox = (CheckBox)dialogLayout.findViewById(R.id.tutorial_checkbox);
            checkbox.setVisibility(View.VISIBLE);
            checkbox.setChecked(true);
            setTutorialSetting(context, tutorialName, STEP_0);
            checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) setTutorialSetting(context, tutorialName, TUTORIAL_DISABLE);
                    else setTutorialSetting(context, tutorialName, TUTORIAL_ENABLE);
                }
            });
        } else {
            dialog = new AlertDialog.Builder(context)
                .setPositiveButton(android.R.string.ok, clicklistener)
                .create();
        }
        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
        if (step == STEP_3) wmlp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        else wmlp.type = WindowManager.LayoutParams.TYPE_APPLICATION;
        dialog.getWindow().setAttributes(wmlp);
        dialog.setTitle(title);
        dialog.setMessage(message);
        ///if(!((Activity) context).isFinishing()) {
            dialog.show();
        ///}
        wmlp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        wmlp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(wmlp);
    }

    public static void closeTutorial(Context context) {
        if (dialog!= null && dialog.isShowing() && mStep == STEP_3) {
            dialog.dismiss();
        }
    }

    public static void resetTutorialStep(Context context, String tutorialName) {
    	if (dialog == null || !dialog.isShowing()) {
    		setTutorialStep(context, tutorialName, STEP_RESET);
    	}
    }

    public static void dismissDialog() {
        if (dialog!= null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private static boolean isWifiConnected(Context context) {
        final ConnectivityManager connectivity = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean wifiConnected = connectivity != null &&
                connectivity.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        return wifiConnected;
    }
    
    // +++ ShawnMC_Liu@2016/11/03: Verizon VZ_REQ_DEVHELP_10667
    public static void launchVzWHelp(Context context, String screen) {
    	if (context != null) {    		
	    	Intent helpIntent = new Intent();
	    	helpIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        helpIntent.setAction("com.asus.help.helpapplication");
	        helpIntent.putExtra("screen", screen);
	        List<ResolveInfo> infoList = context.getPackageManager().queryIntentActivities(helpIntent, PackageManager.GET_INTENT_FILTERS);
	        if (infoList.size() > 0) context.startActivity(helpIntent);
    	}
    }
    // ---

    public static boolean checkVerizonSystemPorperty(){
        Class<?> clazz = null;
        try {
             clazz = Class.forName("android.os.SystemProperties");
             Method method = clazz.getDeclaredMethod("get", String.class);
             String verizonProperty = (String)method.invoke(null, VERIZON_SYSTEM_PROPERTY);
             boolean systemPropertyCheckingResult = (verizonProperty.equalsIgnoreCase(VERIZON_MACHINE));
        
             return systemPropertyCheckingResult;                    
 
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
     	}                 
     }

    public static boolean isVerizonMachine(){
	   if (isVerizonSKU ==2){
		 return false;
	   } else if(isVerizonSKU == 1) {
		 return true;
	   } else if(isVerizonSKU == -1){
	     boolean isVerizon = checkVerizonSystemPorperty();
	  
	     if(isVerizon){
		   isVerizonSKU = 1;
		   return true;
	     } else {
		   isVerizonSKU = 2;
		   return false;
	     }		
	   } else{
		return false;
	   }   
    }

}