package com.android.settings.tts;


import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.internal.logging.MetricsProto.MetricsEvent;

import android.speech.tts.TextToSpeech;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.provider.Settings.Secure.TTS_DEFAULT_RATE;

public class TtsSpeechRateSettingsFragment extends SettingsPreferenceFragment implements TtsSpeechRateRadioButtonPreference.OnClickListener {
    private static final String TTS_SPEED_RATE_CATEGORY = "tts_speech_rate_category";
    private String[] mTtsSpeedRateTitles;
    private String[] mTtsSpeedRateValues;
    private PreferenceCategory mTtsSpeedRateCategory;
    List<TtsSpeechRateRadioButtonPreference> mSelectorPreferenceList= new ArrayList<TtsSpeechRateRadioButtonPreference>();
    private TextToSpeech mTts = null;
    private int mCurrentSpeedRateIndex = 2;


    protected int getMetricsCategory() {
        return MetricsEvent.USER_LOCALE_LIST;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.tts_speech_rate_settings);
        mTtsSpeedRateTitles = getResources().getStringArray(
                R.array.tts_speech_rate_title);
        mTtsSpeedRateValues = getResources().getStringArray(
                R.array.tts_speech_rate_value);
        mTtsSpeedRateCategory = (PreferenceCategory) findPreference(TTS_SPEED_RATE_CATEGORY);
        int currentSpeedRate = android.provider.Settings.Secure.getInt(getContentResolver(),
                TTS_DEFAULT_RATE, 100);
        for(int i = 0; i < mTtsSpeedRateValues.length; i ++){
            if(currentSpeedRate == Integer.parseInt(mTtsSpeedRateValues[i]) + TextToSpeechSettings.MIN_SPEECH_RATE){
                mCurrentSpeedRateIndex = i;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.tts_default_rate_title);

        initTtsSpeedRatePreference();

    }

    public void initTtsSpeedRatePreference(){
        mTtsSpeedRateCategory .removeAll();
        mSelectorPreferenceList.clear();
        for (int i = 0; i < mTtsSpeedRateTitles.length; i ++){
            TtsSpeechRateRadioButtonPreference pref = new TtsSpeechRateRadioButtonPreference(getContext());
            pref.setTitle(mTtsSpeedRateTitles[i]);
            pref.setOnClickListener(this);
            mTtsSpeedRateCategory.addPreference(pref);
            mSelectorPreferenceList.add(pref);
            if(i == mCurrentSpeedRateIndex){
                pref.setChecked(true);
            }
        }
    }

    @Override
    public void onRadioButtonClicked(TtsSpeechRateRadioButtonPreference emiter) {
        int speedRateValue;
        Intent intent = new Intent();
        for(int i = 0; i < mSelectorPreferenceList.size(); i++){
            if(mSelectorPreferenceList.get(i) != emiter){
                mSelectorPreferenceList.get(i).setChecked(false);
            }else {
                mSelectorPreferenceList.get(i).setChecked(true);
                speedRateValue = Integer.parseInt(mTtsSpeedRateValues[i]);
                intent.putExtra("Data",speedRateValue);
                //updateSpeechRate(speedRateValue);

            }
        }
        setResult(0, intent);
        finish();
    }

    private void updateSpeechRate(int data) {
//        Intent intent = new Intent();
//        intent.putExtra("data", data);
//        intent.setClass(getContext(),TextToSpeechSettings.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
//        Bundle bundle = new Bundle();
//        bundle.putInt("Data", data);
//        startFragment(TtsSpeechRateSettingsFragment.this,
//                TextToSpeechSettings.class.getName(),
//                -1, -1, bundle);
    }
}
