package com.android.settings.switchenabler;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

public class LocationEnabler extends AbstractEnabler implements CompoundButton.OnCheckedChangeListener {

    private static final String MODE_CHANGING_ACTION =
            "com.android.settings.location.MODE_CHANGING";
    private static final String CURRENT_MODE_KEY = "CURRENT_MODE";
    private static final String NEW_MODE_KEY = "NEW_MODE";

    private Context mContext;
    private Switch mSwitch;
    private int mCurrentMode;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Broadcast receiver is always running on the UI thread here,
            // so we don't need consider thread synchronization.
            handleStateChanged();
        }
    };
    private boolean mValidListener = false;

    public LocationEnabler(Context context) {
        mContext = context;
    }

    @Override
    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;

        mSwitch = switch_;
        addOnCheckedChangeListener();
        handleStateChanged();
    }

    @Override
    public void resume() {
        addOnCheckedChangeListener();
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, filter);
        handleStateChanged();
    }

    @Override
    public void pause() {
        removeOnCheckedChangeListener();
        mContext.unregisterReceiver(mReceiver);
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            setLocationMode(android.provider.Settings.Secure.LOCATION_MODE_PREVIOUS);
        } else {
            setLocationMode(android.provider.Settings.Secure.LOCATION_MODE_OFF);
        }
    }

    private void addOnCheckedChangeListener(){
        if (mSwitch != null && !mValidListener) {
            mSwitch.setOnCheckedChangeListener(this);
            mValidListener = true;
        }
    }

    private void removeOnCheckedChangeListener(){
        if (mSwitch != null && mValidListener) {
            mSwitch.setOnCheckedChangeListener(null);
            mValidListener = false;
        }
    }
    private void handleStateChanged() {
        if (mSwitch == null) return;
        int mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF);
        mCurrentMode = mode;
        final boolean enabled = (mode != android.provider.Settings.Secure.LOCATION_MODE_OFF);

        EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(mContext,
                UserManager.DISALLOW_SHARE_LOCATION, UserHandle.myUserId());
        boolean hasBaseUserRestriction = RestrictedLockUtils.hasBaseUserRestriction(mContext,
                UserManager.DISALLOW_SHARE_LOCATION, UserHandle.myUserId());
        // Disable the whole switch bar instead of the switch itself. If we disabled the switch
        // only, it would be re-enabled again if the switch bar is not disabled.
        if (!hasBaseUserRestriction && admin != null) {
            mSwitch.setEnabled(false);
        } else {
            mSwitch.setEnabled(!isRestricted());
        }
        if (enabled != mSwitch.isChecked()) {
            mSwitch.setChecked(enabled);
        }
    }

    private void setLocationMode(int mode) {
        if (isRestricted()) {
            // Location toggling disabled by user restriction. Read the current location mode to
            // update the location master switch.
            handleStateChanged();
            return;
        }
        Intent intent = new Intent(MODE_CHANGING_ACTION);
        intent.putExtra(CURRENT_MODE_KEY, mCurrentMode);
        intent.putExtra(NEW_MODE_KEY, mode);
        mContext.sendBroadcast(intent, android.Manifest.permission.WRITE_SECURE_SETTINGS);
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.LOCATION_MODE, mode);
    }

    private boolean isRestricted() {
        final UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        return um.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION);
    }
}
