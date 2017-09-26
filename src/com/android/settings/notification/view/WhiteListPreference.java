package com.android.settings.notification.view;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import com.android.settings.R;
import android.view.LayoutInflater;
import android.util.Log;

/**
 * Created by leaon_wang on 2017/2/24.
 */
public class WhiteListPreference extends Preference{

    private TextView dataTV;
    private String dataStr = "";
	private boolean isAllowBelowDivider = true;
	private View mDivier;

    public WhiteListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WhiteListPreference(Context context, AttributeSet attrs) {
        this(context, attrs, 0); //becare this states
    }

     @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
		dataTV = (TextView)holder.findViewById(R.id.data);
		dataTV.setText(dataStr);
		mDivier = holder.findViewById(R.id.divider);
		mDivier.setVisibility(isAllowBelowDivider ? View.VISIBLE:View.INVISIBLE);
    }

    public void setDataText(String str){
		Log.i("WhiteListPreference","ZenModeSettings.setDataText==>"+str);
        dataStr = str;
		notifyChanged();
    }
	
	public void setAllowBelowDivider(boolean b){
		isAllowBelowDivider = b;
		notifyChanged();
	}
}
