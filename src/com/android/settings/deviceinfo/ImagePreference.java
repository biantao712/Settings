package com.android.settings.deviceinfo;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import com.android.settings.AsusTelephonyUtils;
import android.util.Log;

import com.android.settings.R;

public class ImagePreference extends Preference {
 
    private ImageView mSignalStrengthImage;
    private ImageView mFemtocellImage;


    public ImagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.verizon_preference_image);
    }


    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        mSignalStrengthImage = (ImageView) view.findViewById(R.id.signalStrengthImage);
        mFemtocellImage = (ImageView) view.findViewById(R.id.femtocellImage);
        if (mSignalStrengthImage != null) {
            if (!AsusTelephonyUtils.isVerizon()) {
                mSignalStrengthImage.setVisibility(View.GONE);
            }
        }
        if (mFemtocellImage != null) {
            mFemtocellImage.setImageResource(R.drawable.ic_femtocell);
            mFemtocellImage.setVisibility(View.GONE);
        }
    }

    public void showFemtocell (boolean show) {
        if (mFemtocellImage != null) {
            if (show) {
                mFemtocellImage.setVisibility(View.VISIBLE);
            } else {
                mFemtocellImage.setVisibility(View.GONE);
            }
        }
    }

    public void setSignalImage (int level){
        int[] ICONS = {
                R.drawable.asus_ic_signal_strength_0,
                R.drawable.asus_ic_signal_strength_1,
                R.drawable.asus_ic_signal_strength_2,
                R.drawable.asus_ic_signal_strength_3,
                R.drawable.asus_ic_signal_strength_4,
                R.drawable.asus_ic_signal_strength_5,
                R.drawable.asus_ic_signal_strength_no
        };
        if (mSignalStrengthImage != null) {
            mSignalStrengthImage.setImageResource(ICONS[level]);
        }
    }
}
