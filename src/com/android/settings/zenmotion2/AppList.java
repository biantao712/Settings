package com.android.settings.zenmotion2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings;
//import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import android.graphics.drawable.ColorDrawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Window;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import java.util.Arrays;
import java.util.Objects;

import java.util.TreeSet;
import java.util.regex.Pattern;
import com.android.settings.R;

public class AppList extends AppCompatActivity implements View.OnClickListener{

	private static final String TAG = "ZenMotion2_AppList";
    private static final boolean DYNAMIC_INDEXBAR = true;
    private HashMap<String, Integer> selector;// 存放含有索引字母的位置
    private LinearLayout layoutIndex;
    private ListView listView;
    private TextView tv_show;//index header character
    private ListViewAdapter adapter;
    private String[] indexStr1 = {"!", "#","A", "B", "C", "D", "E", "F", "G", "H",
            "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U",
            "V", "W", "X", "Y", "Z"};
    private String[] indexStr2,indexStr;
	private String NotEnabled="\uD83D\uDEAB";//special unicode char: no entry
	private String notEnabled="!", num="#";
    private static List<AppData> mAppName = new ArrayList<AppData>();//'存放排序前的appname
    private List<AppData> mNewAppName = new ArrayList<AppData>();//'存放排序後的appname

    private int height;// Index bar的字体高度
    //private boolean flag = false;
    private boolean flag =true;
    public static Activity activity;
    private View oldView = null;
    private AppData oldItem = null;

    private static final Object mEntryLock = new Object();

    static final String KEY_CURRENT_GESTURE_LAUNCH = "current_gesture_launch";
    static final String KEY_W_LAUNCH = "w_launch";
    static final String KEY_S_LAUNCH = "s_launch";
    static final String KEY_E_LAUNCH = "e_launch";
    static final String KEY_C_LAUNCH = "c_launch";
    static final String KEY_Z_LAUNCH = "z_launch";
    static final String KEY_V_LAUNCH = "v_launch";

    private static final String APP_ASUS_BOOSTER = "taskmanager";
    private static final String APP_FRONT_CAMERA = "frontCamera";
    //private static final String APP_CAMERA_PACKAGE = "com.asus.camera";
    private static final String SYMBOL = "/";
    private static final String WAKE_UP_SCREEN = "wakeUpScreen";
    private int curIndex=-1;
	//toolbar
	Toolbar mToolbar;
	TextView mTitle;
	ImageView mActionBarABarckButton;
	//end

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    public static Context getContext() {
        return activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
		if(DYNAMIC_INDEXBAR)
			indexStr=indexStr2;
		else
			indexStr=indexStr1;
        //Singleton design like global variable
        GestureApp gestureApp = GestureApp.getInstance();
        Bundle bundle = this.getIntent().getExtras();
        //get the which gesture launched this ativity
        gestureApp.curGesture = bundle.getString(KEY_CURRENT_GESTURE_LAUNCH);
        //end
        
		//setTheme(R.style.Theme_CNAsusRes);
        setContentView(R.layout.zenmotion2_applist);
		setToolBar();
        //setupActionBar();
        //setActionBarTitle(getString(R.string.Application_list_title));
        //setActionBarTitle(getString(R.string.Gestures_Quick_Start_Applications_title));
        layoutIndex = (LinearLayout) this.findViewById(R.id.layout);
        layoutIndex.setBackgroundColor(Color.parseColor("#00ffffff"));
        listView = (ListView) findViewById(R.id.listView);
        tv_show = (TextView) findViewById(R.id.tv);
        tv_show.setVisibility(View.GONE);
        //setPackageNames();
        //setupEntries(getAppInfos());//set Appinfo name, package/ activity name, icon  to mAppName
        //Log.i(TAG,"onCreate:Begin sortIndex...");
        String[] allNames = sortIndex(mAppName);//get the group list
        sortList(allNames);
        selector = new HashMap<String, Integer>();
        //將數字部分加入索引"#"
        Pattern numberPattern = Pattern.compile("[0-9]");
        String preLetter;
        //for Appinfo name and app icon
        String appName;
        //Log.i(TAG,"onCreate:Begin indexbar grouping...");
        for (int j = 0; j < indexStr.length; j++) {// 循环字母表，找出newAppName中对应字母的位置
            for (int i = 0; i < mNewAppName.size(); i++) {
                appName=mNewAppName.get(i).getName();
                AppData app = GestureApp.getGestureInfo(GestureApp.curGesture);
                if(appName.equals(app.getName()))//set app of the current gesture to be checked
                {
                    //mNewAppName.get(i).setEnabled(true);
                    //mNewAppName.get(i).setEnabled(app.getEnabled());
					//mNewAppName.get(i).setAssigned(app.getAssigned());
					mNewAppName.get(i).setAssigned(true);
                    curIndex=i;
                }
                if (appName.equals(indexStr[j])) {
                    selector.put(indexStr[j], i);
                }
                else{
                    if(indexStr[j].equals(notEnabled) && (appName.equals(getResources().getString(R.string.not_enabled)))){
                        selector.put(indexStr[j], i);
						curIndex=i;
						//Log.i(TAG,"add to index:not enabled, curIndex="+String.valueOf(curIndex));
                    }
                    else if(!(indexStr[j].equals(notEnabled))){
                        // Group numbers together in the scroller
                        String firstLetter = appName.substring(0, 1);
                        if (numberPattern.matcher(firstLetter).matches()) {
                            selector.put(indexStr[j], i);
                        }

                    }
                }
            }

        }
        //end
        //button
        //Button btn =(Button) findViewById(R.id.noEnable);
        //btn.setOnClickListener(this);
        //end

        adapter = new ListViewAdapter(this, mNewAppName);
        listView.setAdapter(adapter);
        //listView.requestFocusFromTouch();
        //listView.setSelection(curIndex);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		//Log.i(TAG,"curIndex= "+String.valueOf(curIndex));
		if(curIndex>=0)
		{
	        listView.setItemChecked(curIndex, true);
			oldItem=((AppData) listView.getItemAtPosition(curIndex));
			oldView=adapter.getView(curIndex,null,listView);
	        //listView.smoothScrollToPosition(curIndex);
	        //adapter.notifyDataSetInvalidated();
	        //adapter.notifyDataSetChanged();
		}
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	//Log.i(TAG,"onItemClick+++");
                ListView listView = (ListView) parent;
                String sel = ((AppData) listView.getItemAtPosition(position)).getName();

                if(oldItem!=null){
                    if (oldView != null) {//取消前一次的選擇
                        ImageView isChecked = (ImageView) oldView.findViewById(R.id.isChecked);
						if(isChecked!=null)
	                        isChecked.setVisibility(View.GONE);
                    }
                    //oldItem.setCheckedIcon((ImageView) oldView.findViewById(R.id.isChecked));
                    oldItem.setEnabled(false);
                }
                oldItem=((AppData) listView.getItemAtPosition(position));
				if(oldItem!=null)
                	oldItem.setEnabled(true);
                oldView = view;
                ImageView isChecked = (ImageView) view.findViewById(R.id.isChecked);
				if(isChecked!=null)
                	isChecked.setVisibility(View.VISIBLE);
                ListViewAdapter adapter=(ListViewAdapter) listView.getAdapter();
				if(adapter!=null)
                	adapter.notifyDataSetChanged();
                AppData app = GestureApp.getGestureInfo(GestureApp.curGesture);
                if(app!=null) {
                    //get app packagename/ activity name
                    String name = ((AppData)listView.getItemAtPosition(position)).getPackageActivity();
					//Log.i(TAG,"PackageActivityName="+name);
                    if(name!=null){
                        app.setPackageActivity(name);
                        app.setEnabled(true);
						app.setAssigned(true);//assigned 
                        app.setLabel(sel);
                        ImageView appicon = (ImageView) view.findViewById(R.id.app_icon);
                        app.setDrawable(appicon.getDrawable());
						//Log.i(TAG,"setOnItemClickListener:onItemClick before setGestureAppData");
						//GestureApp.showAllInfo();
						GestureApp.setGestureAppData(GestureApp.curGesture,app);
						//Log.i(TAG,"setOnItemClickListener:onItemClick after setGestureAppData");
						GestureApp.showAllInfo();
                    }
                    else{
                        app.setEnabled(false);
						app.setAssigned(true);//assigned 
                        //app.setLabel("");
                        app.setLabel(getResources().getString(R.string.not_enabled));
                        app.setDrawable(null);
						//Log.i(TAG,"setOnItemClickListener:onItemClick before setGestureAppData");
						//GestureApp.showAllInfo();
						GestureApp.setGestureAppData(GestureApp.curGesture,app);
						//Log.i(TAG,"setOnItemClickListener:onItemClick after setGestureAppData");
						GestureApp.showAllInfo();
                    }
                }
				BackPressed();
            	//Log.i(TAG,"onItemClick---");
				finish();
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }
    public void onClick(View view){
        AppData app = GestureApp.getGestureInfo(GestureApp.curGesture);
        if(app!=null) {
            app.setEnabled(false);
            app.setLabel("");
            app.setDrawable(null);
            if (oldView != null) {//取消前一次的選擇
                ImageView isChecked = (ImageView) oldView.findViewById(R.id.isChecked);
                isChecked.setVisibility(View.GONE);
            }
            if(oldItem!=null){
                oldItem.setEnabled(false);
            }
        }
    }

	private void  setToolBar(){
    	//set statusbar
		getWindow().getDecorView().setSystemUiVisibility(
				  View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		//getWindow().setStatusBarColor(Color.TRANSPARENT);
		getWindow().setStatusBarColor(Color.parseColor("#f2f2f3"));
		//end
		mToolbar = (Toolbar)findViewById(R.id.zenmotion2_action_bar);
		setActionBar(mToolbar);
		//ActionBar actionBar = getActionBar();
		getActionBar().setDisplayHomeAsUpEnabled(false);
		getActionBar().setHomeButtonEnabled(false);
		getActionBar().setDisplayShowTitleEnabled(false);
		mToolbar.setContentInsetsRelative(0,0);
		mToolbar.setContentInsetsAbsolute(0,0);

		mTitle =(TextView) findViewById(R.id.zenmotion2_toolbar_title);
		mTitle.setText(getString(R.string.Gestures_Quick_Start_Applications_title));
		mActionBarABarckButton = (ImageView)findViewById(R.id.zenmotion2_action_bar_back);
		mActionBarABarckButton.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view){
				finish();
			}
		});
	}
    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
    	//set statusbar
		getWindow().getDecorView().setSystemUiVisibility(
				  View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(Color.TRANSPARENT);
		//end
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator(R.drawable.asusres_ic_ab_back_holo_light);
			//requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
			actionBar.setBackgroundDrawable(new ColorDrawable(android.R.color.transparent));
			//actionBar.setBackgroundDrawable(new ColorDrawable(Color.WHITE));		
        }
    }
	private void setActionbarTextColor(ActionBar actBar, int color) {
		String title = actBar.getTitle().toString();
		Spannable spannablerTitle = new SpannableString(title);
		spannablerTitle.setSpan(new ForegroundColorSpan(color), 0, spannablerTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		actBar.setTitle(spannablerTitle);
	}
	

    public void setActionBarTitle(String title) {
		ActionBar actionBar = getSupportActionBar();    // Or getSupportActionBar() if using appCompat
		int color = Color.BLACK;
		actionBar.setTitle(title);
		setActionbarTextColor(actionBar, color);
    }
	public void BackPressed(){
		Bundle argument = new Bundle();
		argument.putString(KEY_CURRENT_GESTURE_LAUNCH, GestureApp.curGesture);
		
		//取出上一個Activity傳過來的 Intent 物件。
		Intent intent = getIntent();
		//放入要回傳的包裹。
		intent.putExtras(argument);
		
		//設定回傳狀態。
		setResult(Activity.RESULT_OK, intent);
	}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
            case 16908322:
                //建立包裹，放入回傳值。
                Bundle argument = new Bundle();
                argument.putString(KEY_CURRENT_GESTURE_LAUNCH, GestureApp.curGesture);

                //取出上一個Activity傳過來的 Intent 物件。
                Intent intent = getIntent();
                //放入要回傳的包裹。
                intent.putExtras(argument);

                //設定回傳狀態。
                setResult(Activity.RESULT_OK, intent);
                //onBackPressed();
                //Log.i(TAG,"intent back to Setting,setResult ");
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * 重新排序获得一个新的List集合
     *
     * @param allNames
     */
    private void sortList(String[] allNames) {
        for (int i = 0; i < allNames.length; i++) {
            if (allNames[i].length() != 1) {//data item
                for (int j = 0; j < mAppName.size(); j++) {
                    if((i>0) && !(allNames[i].equals(allNames[i-1]))) {
                        if (allNames[i].equals(mAppName.get(j).getPinYin())) {
                            AppData p = new AppData(mAppName.get(j).getName(), mAppName
                                    .get(j).getPinYin(), mAppName.get(j).getPackageActivity(), mAppName.get(j).getIcon());
                            mNewAppName.add(p);
                        }
                    }
                }
            } else {//index header, "#", "A", "B", "C",...
				//Log.i(TAG, "index header["+String.valueOf(i)+"]="+allNames[i]);
                mNewAppName.add(new AppData(allNames[i]));// store index character
                if (allNames[i].equals(notEnabled)){
					AppData p = new AppData(getResources().getString(R.string.not_enabled));
					p.setDrawable(getResources().getDrawable(R.drawable.asusres_forbid));
                    //mNewAppName.add(new AppData(getResources().getString(R.string.not_enabled)));
                    mNewAppName.add(p);
					//Log.i(TAG, "not enabled:"+allNames[i]+"add"+getResources().getString(R.string.not_enabled));
                }
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // 在oncreate里面执行下面的代码没反应，因为oncreate里面得到的getHeight=0
        if (!flag) {// 这里为什么要设置个flag进行标记，我这里不先告诉你们，请读者研究，因为这对你们以后的开发有好处
            height = layoutIndex.getMeasuredHeight() / indexStr.length;
			//height = layoutIndex.getMeasuredHeight() / indexStr1.length;
            getIndexView();
            flag = true;
        }
    }

    /**
     * 获取排序后的新数据
     *
     * @param appname
     * @return
     */
    public String[] sortIndex(List<AppData> appname) {
        TreeSet<String> set = new TreeSet<String>();
        // 获取初始化数据源中的首字母，添加到set中
        Pattern numberPattern = Pattern.compile("[0-9]");
        for (AppData person : appname) {
            // Group numbers together in the scroller
            //String firstLetter = StringHelper.getPinYinHeadChar(person.getName()).substring(0, 1);
            String firstLetter = person.getPinYinHeadChar();
            if (numberPattern.matcher(firstLetter).matches()) {
                set.add(num);//number index char is "#"
            }
            else
            {
                //set.add(StringHelper.getPinYinHeadChar(person.getName()).substring(0, 1));
                set.add(person.getPinYinHeadChar());
            }
        }
        // 新数组的长度为原数据加上set的大小
        String[] names = new String[appname.size() + set.size()];
        if(DYNAMIC_INDEXBAR){
            indexStr2=new String[set.size()+1];//dynamically generate index, +1 for "!"
            indexStr=indexStr2;
        }

        int i = 0;
        if(DYNAMIC_INDEXBAR) {
            indexStr[0] = notEnabled;
			//Log.i(TAG, "not enabled:"+indexStr[0]);
        }
        for (String string : set) {
            names[i] = string;
            if(DYNAMIC_INDEXBAR) {
                indexStr[i+1] = string;
            }
            i++;
        }
        String[] pinYinNames = new String[appname.size()];
        for (int j = 0; j < appname.size(); j++) {
            /*
            appname.get(j).setPinYinName(
                    StringHelper
                            .getPingYin(appname.get(j).getName().toString()));
            pinYinNames[j] = StringHelper.getPingYin(appname.get(j).getName().toString());
            */
            names[i+j]=appname.get(j).getPinYin();
        }

        // 将原数据拷贝到新数据中
        //System.arraycopy(pinYinNames, 0, names, set.size(), pinYinNames.length);
        // 自动按照首字母排序
        Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
        String[] indexList = new String[appname.size() + set.size()+1];
        System.arraycopy(names, 0, indexList, 1, names.length);
        indexList[0]=notEnabled;
		//Log.i(TAG, "not enabled:"+indexList[0]);
        return indexList;
    }

    /**
     * 绘制索引列表
     */
    public void getIndexView() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, height);
		String text;
        for (int i = 0; i < indexStr.length; i++) {
            final TextView tv = new TextView(this);
            tv.setLayoutParams(params);
			text=indexStr[i];
			if(text.equals(notEnabled))
				text=NotEnabled;
            tv.setText(text);
            tv.setPadding(10, 0, 10, 0);
            layoutIndex.addView(tv);
            layoutIndex.setOnTouchListener(new View.OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event)

                {
                	String text;
                    float y = event.getY();
                    int index = (int) (y / height);
                    if (index > -1 && index < indexStr.length) {// 防止越界
                        String key = indexStr[index];
                        if (selector.containsKey(key)) {
                            int pos = selector.get(key);
                            if (listView.getHeaderViewsCount() > 0) {// 防止ListView有标题栏，本例中没有。
                                listView.setSelectionFromTop(
                                        pos + listView.getHeaderViewsCount(), 0);
                            } else {
                                listView.setSelectionFromTop(pos, 0);// 滑动到第一项
                            }
                            tv_show.setVisibility(View.VISIBLE);
							text=indexStr[index];
							if(text.equals(notEnabled))
								text=NotEnabled;
                            tv_show.setText(text);
                        }
                    }
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            layoutIndex.setBackgroundColor(Color
                                    //.parseColor("#606060"));
                                    .parseColor("#f2f2f3"));
                            break;

                        case MotionEvent.ACTION_MOVE:

                            break;
                        case MotionEvent.ACTION_UP:
                            layoutIndex.setBackgroundColor(Color
                                    .parseColor("#00ffffff"));
                            tv_show.setVisibility(View.GONE);
                            break;
                    }
                    return true;
                }
            });
        }
    }
    private static boolean supportFrontCamera(PackageManager pm) {
        Intent intent = new Intent("com.asus.camera.action.STILL_IMAGE_FRONT_CAMERA");
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    private static List<AppInfo> sortAppInfos(List<AppInfo> appInfos) {
        Collections.sort(appInfos, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo lhs, AppInfo rhs) {
                return Collator.getInstance().compare(lhs.mLabel, rhs.mLabel);
            }
        });
        return appInfos;
    }

    private static List<AppInfo> getAppInfos(final Context context) {
        //Log.i(TAG,"getAppInfos:Begin getAppInfos...");
        //Context context = getContext();
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        List<AppInfo> appInfos = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            appInfos.add(AppInfo.convertFromResolveInfo(resolveInfo, pm));
        }
        if (supportFrontCamera(pm)) {
            appInfos.add(AppInfo.getFrontCamera(context));
        }
        //check if AsusSmartLauncher is exists for CN SKU
        boolean isSmartLauncherInstalled = true;
        boolean isSmartLauncherEnabled = true;
        try {
            PackageInfo SmartLauncherPackageInfo = context.getPackageManager().getPackageInfo("com.asus.launcher3", PackageManager.GET_META_DATA);
            ApplicationInfo SmartLauncherApplicationInfo = SmartLauncherPackageInfo.applicationInfo;
            isSmartLauncherEnabled = SmartLauncherApplicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Failed querying com.asus.launcher3 app, Assuming it is not installed.");
            isSmartLauncherInstalled = false;
        }
        if (isSmartLauncherInstalled && isSmartLauncherEnabled) {
            appInfos.add(AppInfo.getSmartLauncherWeather(context));
        }
        appInfos.add(AppInfo.getAsusBooster(context));

        return appInfos;
    }
    public static void setPackageNames(final Context context) {
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                //return setupEntries(sortAppInfos(getAppInfos()));
                return setupEntries(getAppInfos(context));
            }

            @Override
            protected void onPostExecute(Integer result) {
                if (result != Integer.MIN_VALUE) {
                    //updateEntry(result);
                }
            }
        }.execute();
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

    String getSettingsSystemKeyValue(String key) {
        return Settings.System.getString(getContext().getContentResolver(), getSettingsSystemKey(key));
    }
    //create the ASUS specific app name
    private static Pair<String, String> parsePackageAndActivity(String savedValue) {
        if (APP_ASUS_BOOSTER.equals(savedValue)) return Pair.create(APP_ASUS_BOOSTER, null);
        if (WAKE_UP_SCREEN.equals(savedValue)) return Pair.create(WAKE_UP_SCREEN, null);
        if (APP_FRONT_CAMERA.equals(savedValue)) return Pair.create(APP_FRONT_CAMERA, null);
        if (null != savedValue && savedValue.contains(SYMBOL)) {
            String[] splitedValue = savedValue.split(SYMBOL);
            return Pair.create(splitedValue[0], splitedValue[1]);
        } else {
            // Use asus booster as default value
            return Pair.create(APP_ASUS_BOOSTER, null);
        }
    }

    private static Integer setupEntries(List<AppInfo> appInfos) {
        // Show the label and icon for each application package.(packageName/activityName)
        synchronized (mEntryLock) {
            Pair<String, String> pair = parsePackageAndActivity(null);
            String packageName = pair.first;
            String activityName = pair.second;
            if (!mAppName.isEmpty()) {
                // Keep the current one (first match) if it exists
                for (AppInfo app : appInfos) {
                    if (Objects.equals(packageName, app.mPackage) &&
                            Objects.equals(activityName, app.mActivity))
                        return appInfos.indexOf(app);
                }
                // Do nothing
                return Integer.MIN_VALUE;
            }
            Integer index = 0;
            mAppName.clear();

            for (AppInfo app : appInfos) {
                AppData p1 = new AppData(app.mLabel);
                p1.setPackageActivity(app.mActivity == null
                        ? app.mPackage
                        : app.mPackage + SYMBOL + app.mActivity);
                p1.setIcon(app.mIcon);
                mAppName.add(p1);
            }
            return index;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        /*
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "AppList Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.android.settings.zenmotion2/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
        */
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        /*
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "AppList Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.android.settings.zenmotion2/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
        */
    }
}
