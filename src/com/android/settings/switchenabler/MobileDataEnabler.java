package com.android.settings.switchenabler;

import android.content.Context;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.widget.CompoundButton;
import android.widget.Switch;

public class MobileDataEnabler  extends AbstractEnabler implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "MobileDataEnabler";
    private Context mContext;
    private Switch mSwitch;
    private boolean mStateMachineEvent;
    ConnectivityManager mCm;
    private ContentObserver mDataConnectionObserver = null;
    private static final int MOBILE_DATA_DISABLE = 0 ;
    private static final int MOBILE_DATA_ENABLE = 1 ;
    private TelephonyManager mTelephonyManager;
    public MobileDataEnabler(Context context, Switch switch_) {
        mContext = context;
        mSwitch = switch_;
        mCm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        mTelephonyManager = TelephonyManager.from(context);
    }

    @Override
    public void setSwitch(Switch switchEnabler) {
        if (mSwitch == switchEnabler) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switchEnabler;
        handleStateChanged();
        mSwitch.setOnCheckedChangeListener(this);
    }

    @Override
    public void start() {
        // listen for Mobile data settings changes
        mDataConnectionObserver = new DataConnectionContentObserver(new Handler());
        mContext.getContentResolver().registerContentObserver(android.provider.Settings.Global.getUriFor(android.provider.Settings.Global.MOBILE_DATA),
                true, mDataConnectionObserver);
    }

    @Override
    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
    }


    protected void setMobileDataEnabledByObserver(boolean enabled) {
        // TODO Auto-generated method stub
        mSwitch.setChecked(enabled);
    }

    @Override
    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    @Override
    public void stop() {
        mContext.getContentResolver().unregisterContentObserver(mDataConnectionObserver);
    }

    private class DataConnectionContentObserver extends ContentObserver {
        DataConnectionContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            setSwitchChecked(mTelephonyManager.getDataEnabled());
        }
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (mStateMachineEvent) return;
        mTelephonyManager.setDataEnabled(isChecked);
        setMobileDataEnabledByObserver(isChecked);
    }

    protected void handleStateChanged() {
        setSwitchChecked(mTelephonyManager.getDataEnabled()? true : false);
    }

    private void setSwitchChecked(boolean checked) {
        if (checked != mSwitch.isChecked()) {
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }
}
