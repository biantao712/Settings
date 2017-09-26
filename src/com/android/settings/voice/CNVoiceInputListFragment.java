package com.android.settings.voice;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.android.settings.AppListPreferenceWithSettings;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.CNAppListPreference;

import java.util.ArrayList;
import java.util.List;
import com.android.internal.app.AssistUtils;
import com.android.internal.logging.MetricsProto.MetricsEvent;

public class CNVoiceInputListFragment extends SettingsPreferenceFragment
             implements CNAppListPreference.OnClickListener {

    private CNVoiceInputPrefHelper mHelper;


    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.APPLICATIONS_MANAGE_ASSIST;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.empty_fragment);
        mHelper = CNVoiceInputPrefHelper.getInstance(getActivity());

    }
    @Override
    public void onResume(){
        super.onResume();
        updateUI();
    }

    private void updateUI(){
        getPreferenceScreen().removeAll();
        List<CharSequence> applicationNames = mHelper.getApplicationNames();
        List<CharSequence> validatedPackageNames = mHelper.getValidatedPackageNames();
        List<Drawable> entryDrawables = mHelper.getEntryDrawables();
        int selectedIndex = mHelper.getSelectedIndex();
        if(applicationNames == null || validatedPackageNames == null || entryDrawables == null)  return;

        for (int i = 0; i<applicationNames.size(); i++){
            CNAppListPreference preference = new CNAppListPreference(getActivity());
            preference.setTitle(applicationNames.get(i));
            preference.setValue((String)validatedPackageNames.get(i));
            preference.setIcon(entryDrawables.get(i));
            preference.setOnClickListener(this);
            if (selectedIndex == i){
                preference.setChecked(true);
            } else{
                preference.setChecked(false);
            }
            getPreferenceScreen().addPreference(preference);
        }
    }
    @Override
    public void onRadioButtonClicked(CNAppListPreference emiter) {
        if (!emiter.isChecked()) {
            String newVoicePackage = (String) emiter.getValue();
            updateRidioButtonState(newVoicePackage);
            mHelper.setVoiceInput(newVoicePackage);
        }
    }


    protected void updateRidioButtonState(String packageName){
        for (int i = 0, count = getPreferenceScreen().getPreferenceCount(); i < count; i++) {
            CNAppListPreference pref = (CNAppListPreference) getPreferenceScreen().getPreference(i);
            String value = pref.getValue();
            if (value.equals(packageName)){
                pref.setChecked(true);
                mHelper.setSummary((String)pref.getTitle());
            }else{
                pref.setChecked(false);
            }
        }

    }
}
