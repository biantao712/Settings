package com.android.settings.zenmotion2;

import android.content.Context;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.content.Intent;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.os.SystemProperties;



import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import android.provider.SearchIndexableResource;
import android.provider.Settings;
import com.android.settings.SettingsPreferenceFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.view.View;
import android.view.ViewGroup;
import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;




import com.android.settings.analytic.AnalyticUtils.Category;
import com.android.settings.analytic.AnalyticUtils.Action;
import com.android.settings.analytic.TrackerManager;
import com.android.settings.analytic.TrackerManager.TrackerName;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
//import android.preference.Preference.OnPreferenceChangeListener;



import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.android.internal.logging.MetricsProto.MetricsEvent;
public class AsusGestureQuickStart extends SettingsPreferenceFragment implements 
	OnSharedPreferenceChangeListener, Indexable{
	private static final String TAG = "ZenMotion2_AsusGestureQuickStart";
	Context mContext=null;
	//Preference key
    static final String KEY_CURRENT_GESTURE_LAUNCH = "current_gesture_launch";
    static final String KEY_GESTURE_TUTORIAL = "gesture_tutorial";
    public static final String KEY_ASUS_GESTURE_QUICK_START_APP = "asus_gesture_quick_start_app";
    public static final String KEY_ASUS_TOUCH_GESTURE_SLEEP_CATEGORY = "asus_zenmotion2_touch_gesture_sleep_category";
    public static final String KEY_ASUS_GESTURE_APP = "asus_gesture_app";
	public static final String KEY_ASUS_GESTURE_TUTORIAL = "gesture_tutorial_group";
	public static final String KEY_ASUS_GESTURE_EMPTY = "gesture_empty_group";
	
	//gesture key
	static final String KEY_W_LAUNCH = "w_launch";
    static final String KEY_S_LAUNCH = "s_launch";
    static final String KEY_E_LAUNCH = "e_launch";
    static final String KEY_C_LAUNCH = "c_launch";
    static final String KEY_Z_LAUNCH = "z_launch";
    static final String KEY_V_LAUNCH = "v_launch";
	//Preference key
	private SharedPreferences mZenMotion2SharesPreferences;
	private SharedPreferences mQuiSharesPreferences;
	private static final String ZEN_MOTION2_DATA = "zen_motion2_data";
	private static final String QUICK_START_ENABLE = "quick_start_enable";
	private static final String FIRST_SET_INIT_VALUE = "first_set_init_value";
	private static boolean mFirs_set_Init_Vale=true;
		
	//system property
	private static final String PERSIST_ASUS_GESTURE_TYPE = "persist.asus.gesture.type";	
    private static final String DEFAULT_ASUS_GESTURE_TYPE = "1111111";
    private static final String DISABLE_ASUS_GESTURE_TYPE = "0000000";
	private static final String DEFAULT_CN_GESTURE_TYPE = "1001101";
	
    private static final int OP_ALL = 1 << 6;
    private static final int OP_W = 1 << 5;
    private static final int OP_S = 1 << 4;
    private static final int OP_E = 1 << 3;
    private static final int OP_C = 1 << 2;
    private static final int OP_Z = 1 << 1;
    private static final int OP_V = 1 << 0;

	SwitchPreference mSwitchQuickStart;
	Preference.OnPreferenceChangeListener mChangeListener;
	
	public static void setCurrentGesture(String gesture){
        //currentGesture = gesture;
    }
	public Drawable getAppIcon(Context context, String packagename){
         Drawable d = null;
         PackageManager pm=context.getPackageManager();
         try {
             d = pm.getApplicationIcon(packagename);
         } catch (PackageManager.NameNotFoundException e) {
             e.printStackTrace();
         }
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
	//setup the each gesture defaule vale
	public void getDefaultGestureAppData(Context context){
		//Log.i(TAG,"getDefaultGestureAppData+++");
		if(mZenMotion2SharesPreferences!=null){
			mFirs_set_Init_Vale=mZenMotion2SharesPreferences.getBoolean(FIRST_SET_INIT_VALUE,true);
			if(mFirs_set_Init_Vale){
				//Log.i(TAG,"readFirstSetPref:setInitValue:DEFAULT_CN_GESTURE_TYPE="+DEFAULT_CN_GESTURE_TYPE);
				setInitValue();
				//mFirs_set_Init_Vale=false;
				//saveFirstSetPref();
			}
			else{
				//Log.i(TAG,"readFirstSetPref:mFirs_set_Init_Vale=false, not first time setting!");
			}
		}
		String type = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE);
		if (type.length() != 7) {
			type = DEFAULT_ASUS_GESTURE_TYPE;
			writeSystemPropGesture(DEFAULT_ASUS_GESTURE_TYPE);
		}
		
		AppData v_app = GestureApp.getSettingsSystemKeyData(context, KEY_V_LAUNCH);
		v_app.setEnabled('1' == (type.charAt(6)));//Settings.System.GESTURE_TYPE6_APP
		GestureApp.setGestureAppData(KEY_V_LAUNCH,v_app);
		
		AppData c_app = GestureApp.getSettingsSystemKeyData(context, KEY_C_LAUNCH);
		c_app.setEnabled('1' == (type.charAt(4)));//Settings.System.GESTURE_TYPE4_APP
		GestureApp.setGestureAppData(KEY_C_LAUNCH,c_app);
		
		AppData e_app = GestureApp.getSettingsSystemKeyData(context, KEY_E_LAUNCH);
		e_app.setEnabled('1' == (type.charAt(3)));//Settings.System.GESTURE_TYPE3_APP
		GestureApp.setGestureAppData(KEY_E_LAUNCH,e_app);
		
		AppData w_app = GestureApp.getSettingsSystemKeyData(context, KEY_W_LAUNCH);
		w_app.setEnabled('1' == (type.charAt(1)));//Settings.System.GESTURE_TYPE1_APP
		GestureApp.setGestureAppData(KEY_W_LAUNCH,w_app);
		
		AppData s_app = GestureApp.getSettingsSystemKeyData(context, KEY_S_LAUNCH);
		s_app.setEnabled('1' == (type.charAt(2)));//Settings.System.GESTURE_TYPE2_APP
		GestureApp.setGestureAppData(KEY_S_LAUNCH,s_app);

		AppData z_app = GestureApp.getSettingsSystemKeyData(context, KEY_Z_LAUNCH);
		GestureApp.showAllInfo();
		z_app.setEnabled('1' == (type.charAt(5)));//Settings.System.GESTURE_TYPE5_APP
		GestureApp.setGestureAppData(KEY_Z_LAUNCH,z_app);
		//Log.i(TAG,"mFirs_set_Init_Vale="+String.valueOf(mFirs_set_Init_Vale));
		if(mFirs_set_Init_Vale){//set the initial state to "not assigned"
			//Log.i(TAG,"set assigned");
			v_app.setAssigned(true);
			c_app.setAssigned(true);
			e_app.setAssigned(true);
			w_app.setAssigned(true);
			s_app.setAssigned(true);
			z_app.setAssigned(true);
			mFirs_set_Init_Vale=false;
					//saveFirstSetPref();
		}
		else{
			v_app.setAssigned(true);
			c_app.setAssigned(true);
			e_app.setAssigned(true);
			w_app.setAssigned(true);
			s_app.setAssigned(true);
			z_app.setAssigned(true);
		}
		GestureApp.setGestureAppData(KEY_V_LAUNCH,v_app);
		GestureApp.setGestureAppData(KEY_C_LAUNCH,c_app);
		GestureApp.setGestureAppData(KEY_E_LAUNCH,e_app);
		GestureApp.setGestureAppData(KEY_W_LAUNCH,w_app);
		GestureApp.setGestureAppData(KEY_S_LAUNCH,s_app);		
		GestureApp.setGestureAppData(KEY_Z_LAUNCH,z_app);
		GestureApp.showAllInfo();
		//Log.i(TAG,"getDefaultGestureAppData---");
	}

	public void setInitValue(){
		if(mFirs_set_Init_Vale)
			//writeSystemPropGesture(DEFAULT_CN_GESTURE_TYPE);
			writeSystemPropGesture(DEFAULT_ASUS_GESTURE_TYPE);
		
	}
	public void readFirstSetPref(){
		return;
		/*
		if(mZenMotion2SharesPreferences!=null){
			mFirs_set_Init_Vale=mZenMotion2SharesPreferences.getBoolean(FIRST_SET_INIT_VALUE,true);
			if(mFirs_set_Init_Vale){
				//Log.i(TAG,"readFirstSetPref:setInitValue:DEFAULT_CN_GESTURE_TYPE="+DEFAULT_CN_GESTURE_TYPE);
				setInitValue();
				mFirs_set_Init_Vale=false;
				//saveFirstSetPref();
			}
			else{
				Log.i(TAG,"readFirstSetPref:mFirs_set_Init_Vale=false, not first time setting!");
			}
		}
		*/
	}
	public void saveFirstSetPref(){
		//return;
		///*
		//Log.i(TAG,"changeListener:onPreferenceChange+++");
		mFirs_set_Init_Vale=false;
		if(mZenMotion2SharesPreferences!=null){
			SharedPreferences.Editor editor=mZenMotion2SharesPreferences.edit();
			Log.i(TAG,"write false to "+FIRST_SET_INIT_VALUE);
			editor.putBoolean(FIRST_SET_INIT_VALUE,mFirs_set_Init_Vale);
			editor.commit();//it will sync to disk
		}
		//Log.i(TAG,"changeListener:onPreferenceChange---");
		//*/
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.zenmotion2_gesturequickstart);
		setHasOptionsMenu(true);
		//Log.i(TAG,"onCreate+++");
		mContext = getActivity();
		mZenMotion2SharesPreferences = getActivity().getSharedPreferences(ZEN_MOTION2_DATA,Context.MODE_PRIVATE);
		//Singleton design like global variable, set the default GestureApp value
		GestureApp gestureApp = GestureApp.getInstance();
		//end

		mChangeListener = new Preference.OnPreferenceChangeListener() {		
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				// Code goes here	
				//Log.i(TAG,"changeListener:onPreferenceChange+++");
				boolean switchstate = (boolean)newValue;
				//Log.i(TAG,"Check switchstate="+String.valueOf(switchstate));
				if(switchstate ==  false){// hide/ remove all GesturePreferences under the PreferenceCategory:asus_gesture_app
					//Log.i(TAG,"Switch Off");
					PreferenceScreen prefGestureMotion=(PreferenceScreen)findPreference(KEY_ASUS_GESTURE_QUICK_START_APP);
					if(prefGestureMotion!=null)
					{
						PreferenceGroup prefGestureApp= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_APP);
						PreferenceGroup prefTutorial= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_TUTORIAL);
						//PreferenceGroup prefempty= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_EMPTY);
						if(mSwitchQuickStart!=null){
							//Log.i(TAG,"remove mainSwitch");
							prefGestureMotion.removePreference(mSwitchQuickStart);
						}
						if(prefGestureApp!=null){
							//Log.i(TAG,"remove prefGestureApp");
							prefGestureMotion.removePreference(prefGestureApp);
						}
						if(prefTutorial!=null){
							//Log.i(TAG,"remove prefTutorial");
							prefGestureMotion.removePreference(prefTutorial);
						}
						/*
						if(prefempty!=null){
							//Log.i(TAG,"remove prefempty");
							prefGestureMotion.removePreference(prefempty);
						}
						*/
						setGestureTypeEnable(false);
						addPreferencesFromResource(R.xml.zenmotion2_gesturepreference_empty);
						/*
						prefGestureMotion.setTitle("");
						prefTutorial= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_TUTORIAL);
						prefTutorial.setTitle("");
						*/
					}
					else
					{
						//Log.i(TAG,"Switch Off:cannot fine the KEY_ASUS_GESTURE_QUICK_START_APP");
					}
				}
				else{//switch on, need to add all GesturePreferences.
					//Log.i(TAG,"Switch On");					
					readFirstSetPref();//read first_set or not
					getDefaultGestureAppData(getContext());
					PreferenceScreen prefGestureMotion=(PreferenceScreen)findPreference(KEY_ASUS_GESTURE_QUICK_START_APP);
					if(prefGestureMotion!=null)
					{
						PreferenceGroup prefGestureApp= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_APP);
						if(prefGestureApp==null){
							//Log.i(TAG,"call  setGestureAppPreferences");
							setGestureAppPreferencesFromResource((PreferenceScreen)prefGestureMotion);
							setGestureTypeEnable(true);
						}
					}
					else
					{
						//Log.i(TAG,"Switch On:cannot fine the KEY_ASUS_GESTURE_QUICK_START_APP");
					}
				}
				//Log.i(TAG,"changeListener:onPreferenceChange---");
				return true;
				//return false;
				}
			};
		//get the SwitchPreference status
		boolean switchstate=false;
		mSwitchQuickStart = (SwitchPreference) findPreference(KEY_ASUS_TOUCH_GESTURE_SLEEP_CATEGORY);
		if(mSwitchQuickStart!=null){
			//Log.i(TAG,"switchQuickStart:Check switchstate="+String.valueOf(switchstate));
			mSwitchQuickStart.setOnPreferenceChangeListener(mChangeListener);
		}
		//end
		
		//Log.i(TAG,"onCreate---");
	}
	@Override
    protected int getMetricsCategory() {
        // TODO Auto-generated method stub
        return MetricsEvent.MAIN_SETTINGS;
    }
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		//Log.i(TAG,"onActivityCreated");

	}
	//when SwitchPreference turn on, create the new gesture app preference layout
	//The global data GestureApp: store the six gestures internal data,
	// Appdata: store app data, like app name, package name. activity name and package/ activity name. the last name is used to launch app.
	//GesturePreference: custom preference, need to set these attributes, title, enabled, and packagename.
	//if enabled == true, the GesturePreference need to set the package name.
	//why need package name, since it is used to get the app icon in GesturePreference:onBindView()
	//void setGestureAppPreferences(PreferenceCategory prefCategory){
	void setGestureAppPreferences(PreferenceScreen prefScreen){
		boolean enabled=false;
		//Log.i(TAG,"setGestureAppPreferences+++");
		//delete tutorial 
		PreferenceGroup prefTutorial= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_TUTORIAL);
		if(prefTutorial!=null){
			prefScreen.removePreference(prefTutorial);
		}
		//end
		//addPreferencesFromResource(R.xml.zenmotion2_gesturepreference);
		
		//add preferenceCategory
		PreferenceGroup prefCategory = new PreferenceCategory(getActivity());
		prefCategory.setKey(KEY_ASUS_GESTURE_APP);	
		prefCategory.setTitle("");
		prefScreen.addPreference(prefCategory);
		//prefCategory.setDependency("asus_zenmotion2_touch_gesture_sleep_category");
		//endw
		GesturePreference prefGestureApp1= new GesturePreference(getActivity());
		prefGestureApp1.setKey(KEY_V_LAUNCH);
		prefGestureApp1.setTitle(R.string.V_launch_title);
		prefGestureApp1.setDefaultValue(true);
		//prefGestureApp1.setWidgetLayoutResource(R.layout.zenmotion2_gesturequickstart_applist_entrance);
		//GestureApp.showInfo(KEY_V_LAUNCH);
		AppData v_app = GestureApp.getGestureInfo(KEY_V_LAUNCH);
		enabled = v_app.getEnabled();
		//Log.i(TAG,"v_app enabled="+String.valueOf(enabled));
		prefGestureApp1.setEnabled(enabled);
		if(enabled) {
			prefGestureApp1.setPackageName(v_app.mPackage);
			//prefGestureApp1.setDrawIcon(v_app.icon);
		}
		prefCategory.addPreference(prefGestureApp1);

		GesturePreference prefGestureApp2 = new GesturePreference(getActivity());
		prefGestureApp2.setKey(KEY_C_LAUNCH);
		prefGestureApp2.setTitle(R.string.C_launch_title);
		prefGestureApp2.setDefaultValue(true);
		//prefGestureApp2.setWidgetLayoutResource(R.layout.zenmotion2_gesturequickstart_applist_entrance);
		//GestureApp.showInfo(KEY_C_LAUNCH);
		AppData c_app = GestureApp.getGestureInfo(KEY_C_LAUNCH);
		enabled =c_app.getEnabled();
		//Log.i(TAG,"c_app enabled="+String.valueOf(enabled));
		prefGestureApp2.setEnabled(enabled);
		if(enabled){
			prefGestureApp2.setPackageName(c_app.mPackage);
		}
		prefCategory.addPreference(prefGestureApp2);

		GesturePreference prefGestureApp3 = new GesturePreference(getActivity());
		prefGestureApp3.setKey(KEY_E_LAUNCH);
		prefGestureApp3.setTitle(R.string.E_launch_title);
		prefGestureApp3.setDefaultValue(true);
		//prefGestureApp3.setWidgetLayoutResource(R.layout.zenmotion2_gesturequickstart_applist_entrance);
		//GestureApp.showInfo(KEY_E_LAUNCH);
		AppData e_app = GestureApp.getGestureInfo(KEY_E_LAUNCH);
		enabled = e_app.getEnabled();
		//Log.i(TAG,"e_app enabled="+String.valueOf(enabled));
		prefGestureApp3.setEnabled(enabled);
		if(enabled){
			prefGestureApp3.setPackageName(e_app.mPackage);
		}
		prefCategory.addPreference(prefGestureApp3);

		GesturePreference prefGestureApp4 = new GesturePreference(getActivity());
		prefGestureApp4.setKey(KEY_W_LAUNCH);
		prefGestureApp4.setTitle(R.string.W_launch_title);
		prefGestureApp4.setDefaultValue(false);
		//prefGestureApp4.setWidgetLayoutResource(R.layout.zenmotion2_gesturequickstart_applist_entrance);
		//GestureApp.showInfo(KEY_W_LAUNCH);
		AppData w_app = GestureApp.getGestureInfo(KEY_W_LAUNCH);
		enabled = w_app.getEnabled();
		//Log.i(TAG,"w_app enabled="+String.valueOf(enabled));
		prefGestureApp4.setEnabled(enabled);
		if(enabled){
			prefGestureApp4.setPackageName(w_app.mPackage);
		}
		prefCategory.addPreference(prefGestureApp4);

		GesturePreference prefGestureApp5 = new GesturePreference(getActivity());
		prefGestureApp5.setKey(KEY_S_LAUNCH);
		prefGestureApp5.setTitle(R.string.S_launch_title);
		prefGestureApp5.setDefaultValue(false);
		//prefGestureApp5.setWidgetLayoutResource(R.layout.zenmotion2_gesturequickstart_applist_entrance);
		//GestureApp.showInfo(KEY_S_LAUNCH);
		AppData s_app = GestureApp.getGestureInfo(KEY_S_LAUNCH);
		enabled = s_app.getEnabled();
		//Log.i(TAG,"s_app enabled="+String.valueOf(enabled));
		prefGestureApp5.setEnabled(enabled);
		if(enabled){
			prefGestureApp5.setPackageName(s_app.mPackage);
		}

		prefCategory.addPreference(prefGestureApp5);

		GesturePreference prefGestureApp6 = new GesturePreference(getActivity());
		prefGestureApp6.setKey(KEY_Z_LAUNCH);
		prefGestureApp6.setTitle(R.string.Z_launch_title);
		prefGestureApp6.setDefaultValue(false);
		//prefGestureApp6.setWidgetLayoutResource(R.layout.zenmotion2_gesturequickstart_applist_entrance);
		//GestureApp.showInfo(KEY_Z_LAUNCH);
		AppData z_app = GestureApp.getGestureInfo(KEY_Z_LAUNCH);
		enabled = z_app.getEnabled();
		//Log.i(TAG,"z_app enabled="+String.valueOf(enabled));
		prefGestureApp6.setEnabled(enabled);
		//prefGestureApp6.setLayoutResource(R.layout.asusres_preference_material_nodivider);//hide divider
		if(enabled){
			prefGestureApp6.setPackageName(z_app.mPackage);
		}
		prefCategory.addPreference(prefGestureApp6);
		
		//add preferenceCategory tutorial back
		prefTutorial = new PreferenceCategory(getActivity());
		prefTutorial.setKey("gesture_tutorial_group");	
		prefTutorial.setTitle("");
		prefScreen.addPreference(prefTutorial);
		//add preference screen back
		PreferenceScreen prefScreenTutorial = getPreferenceManager().createPreferenceScreen(getActivity());
		prefScreenTutorial.setKey("gesture_tutorial");
		prefScreenTutorial.setTitle(R.string.Gestures_Quick_Start_Applications_Tutorial_title);
		prefScreenTutorial.setLayoutResource(R.layout.cnasusres_preference_parent_nodivider);
		prefTutorial.addPreference(prefScreenTutorial);		
		//end
		//Log.i(TAG,"setGestureAppPreferences---");
	}
	
	void setGestureAppPreferencesFromResource(PreferenceScreen prefScreen){
		boolean enabled=false;
		//Log.i(TAG,"setGestureAppPreferencesFromResource+++");
		//delete mainSwitch 
		if(mSwitchQuickStart!=null){
			//Log.i(TAG,"remove mainSwitch");
			prefScreen.removePreference(mSwitchQuickStart);
		}
		//end
		//delete tutorial 
		PreferenceGroup prefTutorial= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_TUTORIAL);
		if(prefTutorial!=null)
			prefScreen.removePreference(prefTutorial);
		//end
		//delete empty 
		/*
		PreferenceGroup prefEmpty= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_EMPTY);
		if(prefEmpty!=null)
			prefScreen.removePreference(prefEmpty);
		*/
		//end
		addPreferencesFromResource(R.xml.zenmotion2_gesturepreference);
		
		mSwitchQuickStart = (SwitchPreference) findPreference(KEY_ASUS_TOUCH_GESTURE_SLEEP_CATEGORY);
		if(mSwitchQuickStart!=null){
			//Log.i(TAG,"mSwitchQuickStart.setOnPreferenceChangeListener");
			mSwitchQuickStart.setOnPreferenceChangeListener(mChangeListener);
		}
		GesturePreference prefGestureApp1= (GesturePreference)findPreference(KEY_V_LAUNCH);
		if(prefGestureApp1!=null){
			//GestureApp.showInfo(KEY_V_LAUNCH);
			AppData v_app = GestureApp.getGestureInfo(KEY_V_LAUNCH);
			enabled = v_app.getEnabled();
			//Log.i(TAG,"v_app enabled="+String.valueOf(enabled));
			prefGestureApp1.setEnabled(enabled);
			prefGestureApp1.setAssigned(v_app.getAssigned());//get the initial state
			//Log.i(TAG,"v_app assigneded="+String.valueOf(v_app.getAssigned()));
			if(enabled) {
				prefGestureApp1.setPackageName(v_app.mPackage);
				prefGestureApp1.setActivityName(v_app.mActivity);
				prefGestureApp1.SetStatusText(v_app.mLabel);
				//prefGestureApp1.setDrawIcon(v_app.icon);
			}
		}

		GesturePreference prefGestureApp2= (GesturePreference)findPreference(KEY_C_LAUNCH);
		if(prefGestureApp2!=null){
			//GestureApp.showInfo(KEY_C_LAUNCH);
			AppData c_app = GestureApp.getGestureInfo(KEY_C_LAUNCH);
			enabled = c_app.getEnabled();
			//Log.i(TAG,"c_app enabled="+String.valueOf(enabled));
			prefGestureApp2.setEnabled(enabled);
			prefGestureApp2.setAssigned(c_app.getAssigned());//get the initial state
			//Log.i(TAG,"c_app assigneded="+String.valueOf(c_app.getAssigned()));
			if(enabled) {
				prefGestureApp2.setPackageName(c_app.mPackage);
				prefGestureApp2.setActivityName(c_app.mActivity);
				prefGestureApp2.SetStatusText(c_app.mLabel);
				//prefGestureApp2.setDrawIcon(c_app.icon);
			}
		}

		GesturePreference prefGestureApp3= (GesturePreference)findPreference(KEY_E_LAUNCH);
		if(prefGestureApp3!=null){
			//GestureApp.showInfo(KEY_E_LAUNCH);
			AppData e_app = GestureApp.getGestureInfo(KEY_E_LAUNCH);
			enabled = e_app.getEnabled();
			//Log.i(TAG,"e_app enabled="+String.valueOf(enabled));
			prefGestureApp3.setEnabled(enabled);
			prefGestureApp3.setAssigned(e_app.getAssigned());//get the initial state
			//Log.i(TAG,"e_app assigneded="+String.valueOf(e_app.getAssigned()));
			if(enabled) {
				prefGestureApp3.setPackageName(e_app.mPackage);
				prefGestureApp3.setActivityName(e_app.mActivity);
				prefGestureApp3.SetStatusText(e_app.mLabel);
				//prefGestureApp3.setDrawIcon(e_app.icon);
			}
		}
		
		GesturePreference prefGestureApp4= (GesturePreference)findPreference(KEY_W_LAUNCH);
		if(prefGestureApp4!=null){
			//GestureApp.showInfo(KEY_W_LAUNCH);
			AppData w_app = GestureApp.getGestureInfo(KEY_W_LAUNCH);
			enabled = w_app.getEnabled();
			prefGestureApp4.setEnabled(enabled);		
			prefGestureApp4.setAssigned(w_app.getAssigned());//get the initial state
			//Log.i(TAG,"w_app enabled="+String.valueOf(enabled));
			//Log.i(TAG,"w_app assigneded="+String.valueOf(w_app.getAssigned()));
			if(enabled) {
				prefGestureApp4.setPackageName(w_app.mPackage);
				prefGestureApp4.setActivityName(w_app.mActivity);
				prefGestureApp4.SetStatusText(w_app.mLabel);
				//prefGestureApp4.setDrawIcon(w_app.icon);
			}
		}

		GesturePreference prefGestureApp5= (GesturePreference)findPreference(KEY_S_LAUNCH);
		if(prefGestureApp5!=null){
			//GestureApp.showInfo(KEY_S_LAUNCH);
			AppData s_app = GestureApp.getGestureInfo(KEY_S_LAUNCH);
			enabled = s_app.getEnabled();
			prefGestureApp5.setEnabled(enabled);
			prefGestureApp5.setAssigned(s_app.getAssigned());//get the initial state
			//Log.i(TAG,"s_app enabled="+String.valueOf(enabled));
			//Log.i(TAG,"s_app assigneded="+String.valueOf(s_app.getAssigned()));
			if(enabled) {
				prefGestureApp5.setPackageName(s_app.mPackage);
				prefGestureApp5.setActivityName(s_app.mActivity);
				prefGestureApp5.SetStatusText(s_app.mLabel);
				//prefGestureApp5.setDrawIcon(s_app.icon);
			}
		}

		GesturePreference prefGestureApp6= (GesturePreference)findPreference(KEY_Z_LAUNCH);
		if(prefGestureApp6!=null){
			//GestureApp.showInfo(KEY_Z_LAUNCH);
			AppData z_app = GestureApp.getGestureInfo(KEY_Z_LAUNCH);
			enabled = z_app.getEnabled();
			prefGestureApp6.setEnabled(enabled);
			prefGestureApp6.setAssigned(z_app.getAssigned());//get the initial state
			prefGestureApp6.showDivider(false);
			//Log.i(TAG,"z_app enabled="+String.valueOf(enabled));
			//Log.i(TAG,"z_app assigneded="+String.valueOf(z_app.getAssigned()));
			if(enabled) {
				prefGestureApp6.setPackageName(z_app.mPackage);
				prefGestureApp6.setActivityName(z_app.mActivity);
				prefGestureApp6.SetStatusText(z_app.mLabel);
				//prefGestureApp5.setDrawIcon(z_app.icon);
			}
		}

		//Log.i(TAG,"setGestureAppPreferencesFromResource---");
	}
	/*
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue){
				Log.i(TAG,preference.getKey()+String.valueOf(newValue));		
	}
	*/
	//check SwitchPreference status, if off, need to hide(remove) all GesturePreferences under the PreferenceCategory:asus_gesture_app
	//if on, need to re-create all GesturePreferences under the PreferenceCategory
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	}

	
	// if GesturePreference is clicked, need to open new activity to select app to lauch.
	// if  preference.getKey()==KEY_GESTURE_TUTORIAL, need to open Tutorial Activity to watch the gesture demo.
	@Override
	public boolean onPreferenceTreeClick(Preference preference){
		//Log.i(TAG,"onPreferenceTreeClick+++");
		SharedPreferences.Editor editor = mZenMotion2SharesPreferences.edit();
		editor.putString(KEY_CURRENT_GESTURE_LAUNCH,preference.getKey());
		editor.commit();
		//Log.i(TAG, "Current gesture is "+preference.getKey());
		//compile error, and comment out the following code
		//setCurrentGesture(preference.getKey());
		//end
		if(!(preference instanceof SwitchPreference)){
			//lunch new Activity to setup app by each gesture: V, C, e,W, S, Z
			if(preference instanceof com.android.settings.zenmotion2.GesturePreference)
			{
				Intent it = new Intent();
				it.setAction("android.intent.action.MAIN");
				//it.setClass(this,com.android.settings.zenmotion2.AppList.class);
				ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.zenmotion2.AppList");
				it.setComponent(cn);
				Bundle bundle = new Bundle();
				bundle.putString(KEY_CURRENT_GESTURE_LAUNCH, preference.getKey());
				it.putExtras(bundle);
				startActivityForResult(it,0);

			}
			else{
				String key=preference.getKey();
				if(key!=null){
					if(key.equalsIgnoreCase(KEY_GESTURE_TUTORIAL)){
						Intent it = new Intent();
						it.setClass(getActivity(),TouchTutorialActivity2.class);
						startActivity(it);
					}
				}
			}
		}
		else{
			if(preference instanceof SwitchPreference){
				//Log.i(TAG, "SwitchPreference is clicked");
				if(mSwitchQuickStart!=null){		
					mSwitchQuickStart = (SwitchPreference) findPreference(KEY_ASUS_TOUCH_GESTURE_SLEEP_CATEGORY);
					if(mSwitchQuickStart!=null){
						//Log.i(TAG,"mSwitchQuickStart.setOnPreferenceChangeListener");
						mSwitchQuickStart.setOnPreferenceChangeListener(mChangeListener);
					}
					//boolean switchstate=mSwitchQuickStart.isChecked();
					boolean switchstate=((SwitchPreference)preference).isChecked();
					//Log.i(TAG,"Check switchstate="+String.valueOf(switchstate));
					editor.putBoolean(QUICK_START_ENABLE,switchstate);
					editor.commit();
				}
			}
		}
		
		//Log.i(TAG,"onPreferenceTreeClick---");
		return super.onPreferenceTreeClick(preference);
	}

	@Override // 覆寫 onActivityResult，傳值回來時會執行此方法。
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Log.i(TAG,"onActivityResult+++");
		if (resultCode == Activity.RESULT_OK) {
			//將包裹從 Intent 中取出。
			Bundle argument = data.getExtras();
			//將回傳值用指定的 key 取出。
			String gesturekey = argument.getString(KEY_CURRENT_GESTURE_LAUNCH);
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
			PreferenceScreen prefGestureMotion = (PreferenceScreen) findPreference(KEY_ASUS_GESTURE_QUICK_START_APP);
			String settingsSystemKey=getSettingsSystemKey(gesturekey);
			if (prefGestureMotion != null) {
				PreferenceGroup prefGestureApp = (PreferenceCategory) findPreference(KEY_ASUS_GESTURE_APP);
				if (prefGestureApp != null) {
					//setup the GesturePrefence data
					GesturePreference preferenceGesture = (GesturePreference) findPreference(gesturekey);
					AppData app = GestureApp.getGestureInfo(gesturekey);
					GestureApp.showInfo(gesturekey);
					GestureApp.showAllInfo();
					//Log.i(TAG,"update GestureApp status:");
					if(app.checkStatus()) {
						GestureApp gestureapp=GestureApp.getInstance();
						preferenceGesture.setEnabled(true);
						preferenceGesture.setAssigned(true);
						preferenceGesture.setPackageName(app.mPackage);
						preferenceGesture.setActivityName(app.mActivity);
						//why need to set icon here, since the onBindView() will not be called, we need to set the app icon to GesturePreference.
						preferenceGesture.SetAppIcon(app.mIcon);
						preferenceGesture.SetStatusText(app.mLabel);
						writekeyOPString(gesturekey, true);
						//write gesture key data to system key
						//Log.i(TAG,settingsSystemKey+"="+app.mPackageActivity);
						Settings.System.putString(getContentResolver(), settingsSystemKey, app.mPackageActivity);
						//Log.i(TAG,"Read "+settingsSystemKey+"="+Settings.System.getString(getContentResolver(), settingsSystemKey));
					}
					else {
						preferenceGesture.setEnabled(false);
						preferenceGesture.setAssigned(true);
						preferenceGesture.setPackageName(null);
						preferenceGesture.setActivityName(null);
						preferenceGesture.SetAppIcon(null);
						preferenceGesture.SetStatusText(getResources().getString(R.string.not_enabled));
						writekeyOPString(gesturekey, false);
						//write gesture key data to system key
						//Log.i(TAG,settingsSystemKey+"="+app.mPackage);
						Settings.System.putString(getContentResolver(), settingsSystemKey, "NotEnabled");
						//Settings.System.putString(getContentResolver(), settingsSystemKey, getResources().getString(R.string.not_enabled));
						//Log.i(TAG,"Read "+settingsSystemKey+"="+Settings.System.getString(getContentResolver(), settingsSystemKey));
					}
				}
			}
		}
		//Log.i(TAG,"onActivityResult---");
	}
	@Override
	public void onResume(){
		super.onResume();
		//Log.i(TAG,"onResume+++");
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
		//boolean switchstate = mZenMotion2SharesPreferences.getBoolean(QUICK_START_ENABLE,true);
		boolean switchstate =getGestureTypeEnable();
		//Log.i(TAG,"Check switchstate="+String.valueOf(switchstate));
		if(switchstate ==  false)// hide/ remove all GesturePreferences under the PreferenceCategory:asus_gesture_app
		{
			//Log.i(TAG,"Switch Off");
			PreferenceScreen prefGestureMotion=(PreferenceScreen)findPreference(KEY_ASUS_GESTURE_QUICK_START_APP);
			if(prefGestureMotion!=null)
			{
				PreferenceGroup prefGestureApp= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_APP);
				PreferenceGroup prefTutorial= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_TUTORIAL);
				//PreferenceGroup prefempty= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_EMPTY);				
				if(mSwitchQuickStart!=null){
					//Log.i(TAG,"remove mainSwitch");
					prefGestureMotion.removePreference(mSwitchQuickStart);
				}
				if(prefGestureApp!=null){
					//Log.i(TAG,"remove prefGestureApp");
					prefGestureMotion.removePreference(prefGestureApp);
				}
				if(prefTutorial!=null){
					//Log.i(TAG,"remove prefTutorial");
					prefGestureMotion.removePreference(prefTutorial);
				}
				/*
				if(prefempty!=null){
					//Log.i(TAG,"remove prefempty");
					prefGestureMotion.removePreference(prefempty);
				}
				*/
				addPreferencesFromResource(R.xml.zenmotion2_gesturepreference_empty);
				
				mSwitchQuickStart = (SwitchPreference) findPreference(KEY_ASUS_TOUCH_GESTURE_SLEEP_CATEGORY);
				if(mSwitchQuickStart!=null){
					//Log.i(TAG,"mSwitchQuickStart.setOnPreferenceChangeListener");
					mSwitchQuickStart.setOnPreferenceChangeListener(mChangeListener);
				}
				setGestureTypeEnable(false);
			}
			else{
				//Log.i(TAG,"Switch Off:cannot fine the KEY_ASUS_GESTURE_QUICK_START_APP");
			}
		}
		else{
			//Log.i(TAG,"Switch On");
			readFirstSetPref();//read first_set or not
			getDefaultGestureAppData(getContext());
			PreferenceScreen prefGestureMotion=(PreferenceScreen)findPreference(KEY_ASUS_GESTURE_QUICK_START_APP);
			if(prefGestureMotion!=null)
			{
				PreferenceGroup prefGestureApp= (PreferenceCategory)findPreference(KEY_ASUS_GESTURE_APP);
				if(prefGestureApp==null){
					//Log.i(TAG,"call  setGestureAppPreferences");
					setGestureAppPreferencesFromResource((PreferenceScreen)prefGestureMotion);
					setGestureTypeEnable(true);
				}
			}
			else{
				//Log.i(TAG,"Switch On:cannot fine the KEY_ASUS_GESTURE_QUICK_START_APP");
			}
		}
		if(mSwitchQuickStart!=null)
			mSwitchQuickStart.setChecked(switchstate);
		//Log.i(TAG,"onResume---");
	}
	@Override
	public void onPause(){
		//Log.i(TAG,"onPause+++");
		super.onPause();
		if(mSwitchQuickStart!=null){
			SharedPreferences.Editor editor = mZenMotion2SharesPreferences.edit();
			boolean switchstate = mSwitchQuickStart.isChecked();
			//Log.i(TAG,"Check switchstate="+String.valueOf(switchstate));
			editor.putBoolean(QUICK_START_ENABLE,switchstate);
			editor.commit();
			saveFirstSetPref();
			setGestureTypeEnable(switchstate);
			getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		}
		//Log.i(TAG,"onPause---");
	}
	private void writekeyOPString(String key, boolean isChecked) {
        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE);
        StringBuilder opStringBuilder = new StringBuilder(opString);
        switch (key) {
            case KEY_W_LAUNCH:
                opStringBuilder.setCharAt(1, isChecked ? '1' : '0');
                break;
            case KEY_S_LAUNCH:
                opStringBuilder.setCharAt(2, isChecked ? '1' : '0');
                break;
            case KEY_E_LAUNCH:
                opStringBuilder.setCharAt(3, isChecked ? '1' : '0');
                break;
            case KEY_C_LAUNCH:
                opStringBuilder.setCharAt(4, isChecked ? '1' : '0');
                break;
            case KEY_Z_LAUNCH:
                opStringBuilder.setCharAt(5, isChecked ? '1' : '0');
                break;
            case KEY_V_LAUNCH:
                opStringBuilder.setCharAt(6, isChecked ? '1' : '0');
                break;
            default:
                break;
        }

        int decimalValue = Integer.parseInt(opStringBuilder.toString(), 2);
        if (OP_ALL == decimalValue) { // 1000000
            opString = DISABLE_ASUS_GESTURE_TYPE;
        } else {
            if (decimalValue < OP_ALL && decimalValue > 0) { // 0xxxxxx -> 1xxxxxx
                opStringBuilder.setCharAt(0, '1');
            }
            opString = opStringBuilder.toString();
        }
		//Log.i(TAG, "Write "+key+":"+String.valueOf(isChecked));
        writeSystemPropGesture(String.format("%7s", opString.replace(' ', '0')));
    }
	private void writeSystemPropGesture(String opString) {
			//Log.i(TAG, "Write system property:" + PERSIST_ASUS_GESTURE_TYPE + " : " + opString);
			try {
				SystemProperties.set(PERSIST_ASUS_GESTURE_TYPE, opString);
			} catch (IllegalArgumentException e) {
				Log.w(TAG, e.toString());
			}
		}
	protected static boolean getSystemPropGestureIsChecked() {
        String opString = SystemProperties.get(PERSIST_ASUS_GESTURE_TYPE,DEFAULT_ASUS_GESTURE_TYPE);
        return (Integer.parseInt(opString, 2) & OP_ALL) != 0;
    }
	private void setGestureTypeEnable(boolean enabled){
		int decimalValue = Integer.parseInt(SystemProperties.get(
                PERSIST_ASUS_GESTURE_TYPE, DEFAULT_ASUS_GESTURE_TYPE), 2);
		if(enabled)
			decimalValue = decimalValue | OP_ALL;
		else
			decimalValue = decimalValue & ~OP_ALL;
		//Log.i(TAG, "setGestureTypeEnable:"+String.valueOf(enabled));
		writeSystemPropGesture(convertToFormatString(decimalValue));
	}
	private boolean getGestureTypeEnable(){
		return getSystemPropGestureIsChecked();
	}
	private String convertToFormatString(int value) {
        return String.format("%7s", Integer.toBinaryString(value)).replace(' ', '0');
    }
	String getSettingsSystemKey(String key) {
        //String key = getKey();
        return KEY_W_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE1_APP :
               KEY_S_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE2_APP :
               KEY_E_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE3_APP :
               KEY_C_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE4_APP :
               KEY_Z_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE5_APP :
               KEY_V_LAUNCH.equals(key) ? Settings.System.GESTURE_TYPE6_APP :
               null;
    }
}
