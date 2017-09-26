package com.android.settings.zenmotion2;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.ComponentName;

import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import com.android.settings.R;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

//compile error:Only a type can be imported. com.android.settings.zenmotion2.setDefaultGestureAppData resolves to a package
//import static com.android.settings.zenmotion2.setDefaultGestureAppData;

/**
 * Created by markg on 2016/9/5.
 */
public class GesturePreference extends Preference{
	private static final String TAG = "ZenMotion2_GesturePreference";
    private static final String APP_ASUS_BOOSTER = "taskmanager";
	private static final String APP_ASUS_LAUNCHER3 = "com.asus.launcher3";
	public static final String APP_FRONT_CAMERA = "frontCamera";
	public static final String APP_CAMERA_PACKAGE = "com.asus.camera";
    private static final boolean DEFAULT_VALUE = false;
    private boolean mEnabled;
	private boolean mAssigned;
    private Context mContext;
    private View mView;

    private ImageView appIcon;
    private TextView statusText;
    private ImageView rightArrow;

    private String text;
    private Drawable icon;
    private int iconId;
    //private CharSequence mPackageName; //use to get app icon
    //use to get app icon
    private String mPackageName; 
    private String mActivityName;
	//end

    static final String KEY_W_LAUNCH = "w_launch";
    static final String KEY_S_LAUNCH = "s_launch";
    static final String KEY_E_LAUNCH = "e_launch";
    static final String KEY_C_LAUNCH = "c_launch";
    static final String KEY_Z_LAUNCH = "z_launch";
    static final String KEY_V_LAUNCH = "v_launch";

    public GesturePreference(Context context) {
        super(context);
        //Log.i(TAG,"GesturePreference(context)");
        mContext = context;
        mPackageName="";
		mActivityName="";
        icon=null;
        iconId=-1;
        text="";
//        setWidgetLayoutResource(R.layout.zenmotion2_gesture_app_item);
        setLayoutResource(R.layout.zenmotion2_gesture_app_item);
    }
    public GesturePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        //Log.i(TAG,"GesturePreference(context, attrs)");
        mContext = context;
        TypedArray a=context.obtainStyledAttributes(attrs,R.styleable.GesturePreference);
        mEnabled = a.getBoolean(R.styleable.GesturePreference_Enabled,false);
		mAssigned = a.getBoolean(R.styleable.GesturePreference_Assigned,true);
        //Log.i(TAG,"enabled="+String.valueOf(enabled));
        mPackageName = (String)a.getText(R.styleable.GesturePreference_PackageName);
		mActivityName= null;
        text= (String) a.getText(R.styleable.GesturePreference_StatusText);
        //setWidgetLayoutResource(R.layout.zenmotion2_gesture_app_item);
        setLayoutResource(R.layout.zenmotion2_gesture_app_item);
        a.recycle();
    }
    public GesturePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //Log.i(TAG,"GesturePreference(context, attrs, defStyleAttr)");
        mContext = context;
        TypedArray a=context.obtainStyledAttributes(attrs,R.styleable.GesturePreference);
        mEnabled = a.getBoolean(R.styleable.GesturePreference_Enabled,false);
		mAssigned = a.getBoolean(R.styleable.GesturePreference_Assigned,true);
        //Log.i(TAG,"enabled="+String.valueOf(enabled));
        mPackageName = (String)a.getText(R.styleable.GesturePreference_PackageName);
		mActivityName= null;
        text= (String) a.getText(R.styleable.GesturePreference_StatusText);
        //setWidgetLayoutResource(R.layout.zenmotion2_gesture_app_item);
        setLayoutResource(R.layout.zenmotion2_gesture_app_item);
        a.recycle();
    }
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        //Log.i(TAG,"onGetDefaultValue");
        return a.getBoolean(index,DEFAULT_VALUE);
    }
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        //Log.i(TAG,"onSetInitialValue");

        if (restorePersistedValue) {
            // Restore existing state
            //enabled = this.getPersistedBoolean(DEFAULT_VALUE);
        } else {
            // Set default state from the XML attribute
            //enabled = (boolean) defaultValue;
            //persistBoolean(enabled);
        }
    }
    /*
    @Override
    protected View onCreateView(ViewGroup parent ) {
        Log.i("Mark Setting:","GesturePreference:onCreateView");
        super.onCreateView(parent);
        LayoutInflater li = (LayoutInflater)mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        View view= li.inflate( R.layout.gesture_app_item, null);
        mView=view;
        return view;
    }
    */
    @Override
    //compile error:
//ERROR: /home/mark_guo/project2/libra_n/packages/apps/AsusSettings/src/com/android/settings/zenmotion2/GesturePreference.java:114: The method onBindView(View) of type GesturePreference must override or implement a supertype method
//ERROR: /home/mark_guo/project2/libra_n/packages/apps/AsusSettings/src/com/android/settings/zenmotion2/GesturePreference.java:116: The method onBindView(View) is undefined for the type Preference
/*
    protected void onBindView(View view) {
        Log.i("Mark Setting:","GesturePreference:onBindView");
        super.onBindView(view);
        appIcon = (ImageView) view.findViewById(R.id.ap_icon);
        rightArrow = (ImageView) view.findViewById(R.id.ap_select_icon);
        statusText = (TextView) view.findViewById(R.id.ap_no_selected);
        // ...
        //super.onBindView(view);
        if(enabled){
            PackageManager pm=mContext.getPackageManager();
            try {
                icon = pm.getApplicationIcon((String) mPackageName);
                iconId=-1;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if(icon!=null)
                appIcon.setImageDrawable(icon);
            if(iconId!=-1)
                appIcon.setImageResource(iconId);
            text="";
            statusText.setText(text);
        }
        else{
            icon=null;
            appIcon.setImageDrawable(icon);
            iconId=-1;
            mPackageName="";
            statusText.setText(R.string.not_enabled);
            text=(String )statusText.getText();

        }
    }
*/

	public void onBindViewHolder(PreferenceViewHolder holder) {
			//Log.i(TAG,"onBindViewHolder+++");
			super.onBindViewHolder(holder);
			appIcon = (ImageView) holder.findViewById(R.id.ap_icon);
			rightArrow = (ImageView) holder.findViewById(R.id.ap_select_icon);
			statusText = (TextView) holder.findViewById(R.id.ap_name);
            holder.findViewById(R.id.divider).setVisibility(isShowDivider? View.VISIBLE:View.INVISIBLE);
			// ...
			//super.onBindView(view);
			//Log.i(TAG,"mEnabled="+String.valueOf(mEnabled));
			if(mEnabled){
				PackageManager pm=mContext.getPackageManager();
				try {
					if(mPackageName.equalsIgnoreCase(APP_ASUS_BOOSTER)){
						icon=mContext.getDrawable(R.drawable.asus_icon_boost);
					}
					//fixed get wrong icon for CN weather
					else if(mPackageName.equalsIgnoreCase(APP_ASUS_LAUNCHER3)){
						try {
							icon = mContext.getPackageManager().getActivityIcon(new ComponentName(APP_ASUS_LAUNCHER3,"com.asus.zenlife.zlweather.ZLWeatherActivity"));
						} catch (NameNotFoundException e) {
							Log.w(TAG, "Failed getting icon for smartlauncher.");
							e.printStackTrace();
							}
					}
					else if(mPackageName.equalsIgnoreCase(APP_FRONT_CAMERA)){
						Log.w(TAG, "get front camera icon");
						icon = mContext.getPackageManager().getApplicationIcon(APP_CAMERA_PACKAGE);
					}
					else if(mPackageName.equalsIgnoreCase(APP_CAMERA_PACKAGE)){
						Log.w(TAG, "get front camera icon");
						icon = mContext.getPackageManager().getApplicationIcon(APP_CAMERA_PACKAGE);
					}
					else{
						//icon = pm.getApplicationIcon((String) mPackageName);
						if(mActivityName!=null)
							icon = mContext.getPackageManager().getActivityIcon(new ComponentName(mPackageName,mActivityName));
					}
					iconId=-1;
				} catch (PackageManager.NameNotFoundException e) {
					e.printStackTrace();
				}
				
				appIcon.setVisibility(View.VISIBLE);
				statusText.setVisibility(View.VISIBLE);
				if(icon!=null)
					appIcon.setImageDrawable(icon);
				if(iconId!=-1)
					appIcon.setImageResource(iconId);
				statusText.setText(text);
			}
			else{
				icon=null;
				appIcon.setImageDrawable(icon);		
				iconId=-1;
				mPackageName="";
				appIcon.setVisibility(View.GONE);
				statusText.setVisibility(View.VISIBLE);
				if(!mAssigned){//initial state
					//Log.i(TAG,"not assigned");
					statusText.setText(R.string.not_assigned);
				}
				else{
					//Log.i(TAG,"not enabled");
					statusText.setText(R.string.not_enabled);
				}
				text=(String )statusText.getText();

			}
			//Log.i(TAG,"onBindViewHolder---");
		}
    //---set--------
    public void setDrawIcon(Drawable d){
        if(icon!=null)
            icon=d;
    }
    public void SetAppIcon(Drawable d){
        if(icon!=null){
            icon=d;
            appIcon.setImageDrawable(icon);
        }
        else
            appIcon.setImageDrawable(d);
    }
    public void SetAppIcon(int drawableId ){
        iconId=drawableId;
        appIcon.setImageResource(iconId);
    }
    public void SetStatusText(int enabled){
        statusText.setText(enabled);
    }
    public void SetStatusText(String str){
        text=str;
        if (statusText!=null)
            statusText.setText(text);
    }
    public void setEnabled(boolean value){
        mEnabled=value;
		if(mEnabled){
			if((appIcon!=null) && (statusText!=null)){
				appIcon.setVisibility(View.VISIBLE);
				//Log.i(TAG, "appIcon.setVisibility(View.VISIBLE)");
				statusText.setVisibility(View.VISIBLE);
				//Log.i(TAG, "statusText.setVisibility(View.GONE)");
			}
		}
		else{
			if((appIcon!=null) && (statusText!=null)){
				appIcon.setVisibility(View.GONE);
				//Log.i(TAG, "appIcon.setVisibility(View.GONE)");
				statusText.setVisibility(View.VISIBLE);
				//Log.i(TAG, "statusText.setVisibility(View.VISIBLE)");
			}
		}
    }
	public void setAssigned(boolean value){
		mAssigned=value;
	}
    public void setPackageName(String name){
        mPackageName=name;
    }
    public void setActivityName(String name){
        mActivityName=name;
    }
    //----get-----
    public ImageView getAppImageView(){
        return appIcon;
    }
    public TextView getStatusTextView(){
        return statusText;
    }
    public View getGesturePreferenceView(){
        return mView;
    }
    public boolean getEnabled(){
        return mEnabled;
    }
    public boolean getAssigned(){
        return mAssigned;
    }	
    public CharSequence getPackageName(){
        return mPackageName;
    }
    public CharSequence getActivityName(){
        return mActivityName;
    }

    private boolean isShowDivider = true;
    public void showDivider(boolean show){
        isShowDivider = show;
    }
}
