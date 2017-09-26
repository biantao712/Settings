package com.android.settings.localepicker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.settings.SettingsPreferenceFragment;

import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference;
import com.android.settings.R;
import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.settings.inputmethod.InputMethodAndLanguageSettings;

import android.os.LocaleList;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import java.util.Locale;


public class LocaleListSelector  extends SettingsPreferenceFragment implements LocaleListSelectorRadioButtonPreference.OnClickListener {
    static final String TAG = "LocaleListSelector";
    private static final String SYSTEM_LANGUAGE_CATEGORY = "system_language_category";
    List<LocalePicker.LocaleInfo> mLocaleInfos;
    List<LocaleListSelectorRadioButtonPreference> mSelectorPreferenceList= new ArrayList<LocaleListSelectorRadioButtonPreference>();
    PreferenceCategory mPreGroup;
    final static String CURRENT_LOCALE = "current_locale";
    private Locale mSelectedLocale;
    private MyTask mTask;
    private Activity mActivity;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.USER_LOCALE_LIST;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        addPreferencesFromResource(R.xml.system_language_selector);

        boolean isInDeveloperMode = Settings.Global.getInt(getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0;
        mLocaleInfos = LocalePicker.getAllAssetLocales(getContext(), isInDeveloperMode);
        mPreGroup = (PreferenceCategory) findPreference(SYSTEM_LANGUAGE_CATEGORY);
    }

    @Override
    public void onResume() {
        super.onResume();
        mActivity = getActivity();
        getActivity().setTitle(R.string.pref_title_lang_selection);
        initLanguageListPreference();

    }


    private void initLanguageListPreference(){
        Locale current = getContext().getResources().getConfiguration().getLocales().get(0);
        mSelectorPreferenceList.clear();
        mPreGroup .removeAll();
        for(int i = 0; i < mLocaleInfos.size(); i ++){
            LocaleListSelectorRadioButtonPreference pref = new LocaleListSelectorRadioButtonPreference(getContext());
            pref.setTitle(mLocaleInfos.get(i).getLabel());
            pref.setKey(mLocaleInfos.get(i).getLabel());
            pref.setOnClickListener(this);
            mPreGroup.addPreference(pref);
            mSelectorPreferenceList.add(pref);
           if(current.toString().equals(mLocaleInfos.get(i).getLocale().toString())){
                pref.setChecked(true);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(LocaleListSelectorRadioButtonPreference emiter) {
        for(int i = 0; i < mSelectorPreferenceList.size(); i++){
            if(mSelectorPreferenceList.get(i) != emiter){
                mSelectorPreferenceList.get(i).setChecked(false);
            }else {
                mSelectorPreferenceList.get(i).setChecked(true);
                mSelectedLocale = mLocaleInfos.get(i).getLocale();

//                mTask = new MyTask();
//                mTask.execute("Update Locale");

            }
        }
        updateLocale();
    }

    public void updateLocale(){
        mActivity.onBackPressed();
        LocalePicker.updateLocale(mSelectedLocale);
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    private class MyTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            Log.i("Jack", "doInBackground(Params... params) called");
                        try {
                            Thread.sleep(500);
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                        LocalePicker.updateLocale(mSelectedLocale);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("Jack", "onPostExecute(Result result) called");
            if(mActivity != null){
                mActivity.onBackPressed();
            }else{
                Log.e(TAG, "mActivity == null");
            }

        }
    }




}
