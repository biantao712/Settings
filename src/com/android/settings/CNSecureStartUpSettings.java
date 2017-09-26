package com.android.settings;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Switch;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;

public class CNSecureStartUpSettings extends OptionsMenuFragment {

    private static final String TAG = "CNSecureStartUpSettings";

    private static final int ENABLE_ENCRYPTION_REQUEST_VERIFY = 2002;

    private static final int MY_USER_ID = UserHandle.myUserId();
    private LockPatternUtils mLockPatternUtils;
    private int mPwdType = -1;
    private String mPwd = null;
    private View mContentView;
    private Switch mStartupSwitch;

    @Override
    protected int getMetricsCategory() {
        return MetricsEvent.RESET_NETWORK;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.cn_secure_startup_settings, null);
        establishInitialState();
        return mContentView;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mLockPatternUtils = new LockPatternUtils(getActivity());
    }

    private void establishInitialState() {
        if(mContentView == null)  return;

        RelativeLayout startupContainer = (RelativeLayout) mContentView.findViewById(R.id.startup_switch_container);
        startupContainer.setOnClickListener(null);
        boolean isSecure = mLockPatternUtils.isSecure(MY_USER_ID);
        startupContainer.setEnabled(isSecure);
        startupContainer.setClickable(isSecure);

        mStartupSwitch = (Switch) mContentView.findViewById(R.id.startup_switch);
        boolean required = mLockPatternUtils.isCredentialRequiredToDecrypt(false);
        mStartupSwitch.setChecked(required);

        startupContainer.setOnClickListener(mListener);
    }

    private final View.OnClickListener mListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            if(mStartupSwitch == null)  return;
            boolean isChecked = mStartupSwitch.isChecked();
            mStartupSwitch.setChecked(!isChecked);

            if (!runKeyguardConfirmation(ENABLE_ENCRYPTION_REQUEST_VERIFY)) {
                mStartupSwitch.setChecked(isChecked);
            }
        }
    };

    private boolean runKeyguardConfirmation(int request) {
        Resources res = getActivity().getResources();
        return new ChooseLockSettingsHelper(getActivity(), this).launchConfirmationActivityNeedPwd(
                request, res.getText(R.string.encryption_interstitial_header), true);
    }

    private boolean isDoNotAskCredentialsOnBootSet() {
        DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm.getDoNotAskCredentialsOnBoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == ENABLE_ENCRYPTION_REQUEST_VERIFY && resultCode == Activity.RESULT_OK){
            mPwdType = data.getIntExtra("pwdtype",-1);
            mPwd = data.getStringExtra("pwd");
            if(mPwdType == -1 || mPwd == null){
                Log.w(TAG,"ENABLE_ENCRYPTION_REQUEST pwd not assign value");
                return;
            }

            boolean required = mLockPatternUtils.isCredentialRequiredToDecrypt(false);
            required = !required;
            Log.d(TAG, "change startup required = " + required);
            mLockPatternUtils.setCredentialRequiredToDecrypt(required);

            if(LockPatternUtils.isDeviceEncryptionEnabled()){
                if(required && !isDoNotAskCredentialsOnBootSet()){
                    final IBinder service = ServiceManager.getService("mount");
                    if (service == null) {
                        Log.e(TAG, "Could not find the mount service to update the encryption password");
                        return;
                    }
                    Log.d(TAG,"begin changeEncryptionPassword");
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... dummy) {
                            IMountService mountService = IMountService.Stub.asInterface(service);
                            try {
                                mountService.changeEncryptionPassword(mPwdType, mPwd);
                                Log.d(TAG,"changeEncryptionPassword finish");
                            } catch (RemoteException e) {
                                Log.e(TAG, "Error changing encryption password", e);
                            }
                            return null;
                        }
                    }.execute();
                }else{
                    Log.d(TAG,"clearEncryptionPassword");
                    mLockPatternUtils.clearEncryptionPassword();
                }
            }
        }
        establishInitialState();
    }

}
