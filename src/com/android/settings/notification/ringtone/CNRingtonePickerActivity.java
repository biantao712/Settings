package com.android.settings.notification.ringtone;

import android.Manifest;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;
import com.android.settings.R;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

public class CNRingtonePickerActivity extends AppCompatActivity {

    private static final String TAG = "CNRingtonePicker";

    private TabHost mTabHost;

    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private static final String RINGTONE_TYPE = "ringtone_type";
    private static final String SELECTED_URI = "selected_uri";
    private static final String CURRENT_TAB = "current_tab";

    public static final String internalTag = "internal_ringtones";
    public static final String externalTag = "external_ringtones";

    private int mRingtoneType;

    public static  Uri ringtoneUri;

    private String currentTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cn_ringtone_picker);
        mRingtoneType = getIntent().getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE,RingtoneManager.TYPE_RINGTONE);
        if(savedInstanceState != null){
            mRingtoneType = savedInstanceState.getInt(RINGTONE_TYPE);
        }
        ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this,mRingtoneType);
        if (savedInstanceState != null) {
            String selectString = savedInstanceState.getString(SELECTED_URI);
            Log.i(TAG,"selectString = "+selectString);
            if (selectString != null) {
                ringtoneUri = Uri.parse(selectString);
            }
        }
        Log.i(TAG,"mRingtoneType = "+mRingtoneType);
        initView();
        setCustomActionBar();
        if (savedInstanceState != null) {
            String tab = savedInstanceState.getString(CURRENT_TAB);
            if (tab != null) {
                mTabHost.setCurrentTabByTag(tab);
            }
        }
    }

    private void setCustomActionBar(){
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(getString(R.string.ringtone_picker_title));
        setActionBar(mToolbar);

        ActionBar actionbar = getActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeButtonEnabled(true);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mToolbar.inflateMenu(R.menu.cn_ringtone_menu);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Log.i(TAG,"save uri = "+ringtoneUri);
                RingtoneManager.setActualDefaultRingtoneUri(CNRingtonePickerActivity.this,mRingtoneType,ringtoneUri);
                Intent resultIntent = new Intent();
                resultIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, ringtoneUri);
                setResult(RESULT_OK, resultIntent);
                finish();
                return true;
            }
        });

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(getColor(R.color.action_bar_background));

    }

    private void initView() {
        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec(internalTag)
                .setContent(R.id.internal_frg)
                .setIndicator(getString(R.string.internal_tag)));
        mTabHost.addTab(mTabHost.newTabSpec(externalTag)
                .setContent(R.id.external_frg)
                .setIndicator(getString(R.string.external_tag)));
        updateTab();

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

            @Override
            public void onTabChanged(String tabId) {

                if (externalTag.equals(tabId) && ContextCompat.checkSelfPermission(CNRingtonePickerActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(CNRingtonePickerActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }

                realTabChanged(tabId);
            }
        });
        Log.i(TAG,"ringtoneUri = "+ringtoneUri);
        if(ringtoneUri!=null && !isInternalUri(ringtoneUri)){
            mTabHost.setCurrentTabByTag(externalTag);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {

        if (requestCode == PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(CNRingtonePickerActivity.this, "Permission Grant", Toast.LENGTH_SHORT).show();
            } else
            {
                // Permission Denied
                Toast.makeText(CNRingtonePickerActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void realTabChanged(String tabId) {
        updateTab();
        CNRingtoneBaseFragment fragment = null;
        if (currentTab != null) {
            fragment = (CNRingtoneBaseFragment) getFragmentManager().findFragmentByTag(currentTab);
        } else {
            fragment = (CNRingtoneBaseFragment) getFragmentManager().findFragmentByTag(internalTag);
        }
        if (fragment != null) {
            fragment.reset();
        }
        if (tabId != null) {
            fragment = (CNRingtoneBaseFragment) getFragmentManager().findFragmentByTag(tabId);
        } else {
            fragment = (CNRingtoneBaseFragment) getFragmentManager().findFragmentByTag(internalTag);
        }
        if (fragment != null) {
            fragment.reset();
        }
        currentTab = tabId;
        Log.i(TAG,"tabId = "+tabId);
    }

    /**
     * 更新tab的样式
     */
    private void updateTab() {
        TabWidget tabWidget = mTabHost.getTabWidget();
        for(int i=0;i<tabWidget.getChildCount();i++){
            View view = tabWidget.getChildAt(i);
            TextView tv = (TextView) view.findViewById(android.R.id.title);
            tv.setTransformationMethod(null);
            tv.setSingleLine();
            tv.setEllipsize(TextUtils.TruncateAt.valueOf("END"));
            tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,TableRow.LayoutParams.MATCH_PARENT));
            tv.setGravity(Gravity.CENTER);
            if (mTabHost.getCurrentTab() == i) {
                tv.setTextColor(getColor(R.color.colorPrimary));
            } else {
                tv.setTextColor(getColor(android.R.color.black));
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cn_ringtone_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public int getRingtoneType(){
        return mRingtoneType;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG,"onSaveInstanceState");
        if (currentTab != null)
            outState.putString(CURRENT_TAB, currentTab);
        if (ringtoneUri != null) {
            outState.putString(SELECTED_URI, ringtoneUri.toString());
        }
        outState.putInt(RINGTONE_TYPE,mRingtoneType);
        super.onSaveInstanceState(outState);
    }

    public boolean isInternalUri(Uri uri) {
        boolean b = true;
        if (uri != null && uri.toString().contains("external")) {
            b = false;
        }
        return b;
    }
}
