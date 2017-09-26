package com.android.settings.voice;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.android.internal.app.AssistUtils;
import com.android.settings.R;
import com.android.settings.applications.CNAppListPreference;
import com.android.settings.applications.CNDefaultAssistHelper;

import java.util.ArrayList;
import java.util.List;

public class CNVoiceInputPrefHelper {

    private VoiceInputHelper mHelper;

    // The assist component name to restrict available voice inputs.
    private ComponentName mAssistRestrict;

    public static final String ITEM_NONE_VALUE = "";
    private final List<Integer> mAvailableIndexes = new ArrayList<>();

    private static CNVoiceInputPrefHelper mInstance;
    private Context mContext;
    private List<CharSequence> applicationNames;
    private List<CharSequence> validatedPackageNames;
    private List<Drawable> entryDrawables;
    private int selectedIndex;
    private String mSummary;
    public CNVoiceInputPrefHelper(Context context){
        mContext = context;
    }
    public static CNVoiceInputPrefHelper getInstance(Context context){
        if (mInstance == null){
            mInstance = new CNVoiceInputPrefHelper(context);
        }
        return mInstance;
    }

    public List<CharSequence> getApplicationNames(){
        return applicationNames;
    }

    public List<CharSequence> getValidatedPackageNames(){
        return validatedPackageNames;
    }

    public List<Drawable> getEntryDrawables(){
        return entryDrawables;
    }

    public int getSelectedIndex(){
        return selectedIndex;
    }

    public String getSummary(){
        return mSummary;
    }

    public void setSummary(String summary){
        mSummary = summary;
    }


    public void setVoiceInput(String value) {
        if (value == null || value.equals(ITEM_NONE_VALUE)){
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.VOICE_INTERACTION_SERVICE, "");
            Settings.Secure.putString(mContext.getContentResolver(),
                    Settings.Secure.VOICE_RECOGNITION_SERVICE, "");
            return;
        }
        for (int i = 0; i < mHelper.mAvailableInteractionInfos.size(); ++i) {
            VoiceInputHelper.InteractionInfo info = mHelper.mAvailableInteractionInfos.get(i);
            if (info.key.equals(value)) {
                Settings.Secure.putString(mContext.getContentResolver(),
                        Settings.Secure.VOICE_INTERACTION_SERVICE, value);
                Settings.Secure.putString(mContext.getContentResolver(),
                        Settings.Secure.VOICE_RECOGNITION_SERVICE,
                        new ComponentName(info.service.packageName,
                                info.serviceInfo.getRecognitionService())
                                .flattenToShortString());
                return;
            }
        }

        for (int i = 0; i < mHelper.mAvailableRecognizerInfos.size(); ++i) {
            VoiceInputHelper.RecognizerInfo info = mHelper.mAvailableRecognizerInfos.get(i);
            if (info.key.equals(value)) {
                Settings.Secure.putString(mContext.getContentResolver(),
                        Settings.Secure.VOICE_INTERACTION_SERVICE, "");
                Settings.Secure.putString(mContext.getContentResolver(),
                        Settings.Secure.VOICE_RECOGNITION_SERVICE, value);
               return;
            }
        }

    }


    public void refreshVoiceInputs() {
        mHelper = new VoiceInputHelper(mContext);
        mHelper.buildUi();
        mAssistRestrict = CNDefaultAssistHelper.getInstance(mContext).getCurrentAssist();

        String curValue;
        if (mHelper.mCurrentVoiceInteraction != null) {
            curValue = mHelper.mCurrentVoiceInteraction.flattenToShortString();
        } else if (mHelper.mCurrentRecognizer != null) {
            curValue = mHelper.mCurrentRecognizer.flattenToShortString();
        } else {
            curValue = ITEM_NONE_VALUE;
        }
        final String assistKey =
                mAssistRestrict == null ? "" : mAssistRestrict.flattenToShortString();

        mAvailableIndexes.clear();
        applicationNames = new ArrayList<>();
        validatedPackageNames = new ArrayList<>();
        entryDrawables = new ArrayList<Drawable>();

        applicationNames.add(mContext.getResources().getText(R.string.app_list_preference_none));
        validatedPackageNames.add(ITEM_NONE_VALUE);
        entryDrawables.add(mContext.getDrawable(R.drawable.asusres_app_none));
        if (curValue.equals(ITEM_NONE_VALUE)) {
            selectedIndex = 0;
            mSummary = (String)mContext.getResources().getText(R.string.app_list_preference_none);
        }


        for (int i = 0; i < mHelper.mAvailableInteractionInfos.size(); ++i) {
            VoiceInputHelper.InteractionInfo info = mHelper.mAvailableInteractionInfos.get(i);
            applicationNames.add(info.appLabel);
            validatedPackageNames.add(info.key);
            entryDrawables.add(info.appIcon);

            if (info.key.contentEquals(assistKey)) {
                mAvailableIndexes.add(i+1);
            }
            if (info.key.equals(curValue)){
                selectedIndex = i+1;
                mSummary = (String)info.appLabel;
            }
        }

        final boolean assitIsService = !mAvailableIndexes.isEmpty();
        final int serviceCount = applicationNames.size();

        for (int i = 0; i < mHelper.mAvailableRecognizerInfos.size(); ++i) {
            VoiceInputHelper.RecognizerInfo info = mHelper.mAvailableRecognizerInfos.get(i);
            applicationNames.add(info.label);
            validatedPackageNames.add(info.key);
            entryDrawables.add(info.appIcon);
            if (!assitIsService) {
                mAvailableIndexes.add(serviceCount + i);
            }
            if (info.key.equals(curValue)){
                selectedIndex = i+serviceCount;
                mSummary = (String)info.label;
            }
        }
    }

    public ComponentName getCurrentService() {
        if (mHelper.mCurrentVoiceInteraction != null) {
            return mHelper.mCurrentVoiceInteraction;
        } else if (mHelper.mCurrentRecognizer != null) {
            return mHelper.mCurrentRecognizer;
        } else {
            return null;
        }
    }
}
