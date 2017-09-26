package com.asus.bluetooth;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.settings.R;

import static com.asus.bluetooth.BluetoothPairedUtil.DEVICE_NAME_DA01_SPK;
import static com.asus.bluetooth.BluetoothPairedUtil.DEVICE_NAME_DA01_KB;
import static com.asus.bluetooth.BluetoothPairedUtil.DEVICE_NAME_DK01_KB;

public class BluetoothPairedService extends Service {

    private static final String TAG = "BluetoothPairedService";

    public static final String ACTION_SET_BT_PAIRED =
            "com.asus.bluetooth.action.SET_BT_PAIRED";
    public static final String ACTION_NOTIFICATION_CANCELLED =
            "com.asus.bluetooth.action.NOTIFICATION_CANCELLED";
    public static final int BOND_SUCCESS = 0;
    public static final int UNBOND_REASON_AUTH_FAILED = 1;
    public static final int UNBOND_REASON_AUTH_REJECTED = 2;
    public static final int UNBOND_REASON_AUTH_TIMEOUT = 3;

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mSetBTPairedReceiverOn = false;
    private boolean mPairWithDevices[] = {false, false, false};
    private static final int INDEX_DA01_SPK = 0;
    private static final int INDEX_DA01_KB = 1;
    private static final int INDEX_DK01_KB = 2;
    
    private boolean mClickNotification = false;
    private long mDiscoveryTimestamp = 0;
    private static final int DISCOVERY_TIME_INTERVAL = 3 * 60; // 3 min

    private NotificationManager mNotificationManager;
    private static final int SHOW_NOTIFICATION_ID = 2000;

    private BluetoothDevice mDeviceDA01Spk = null;
    private BluetoothDevice mDeviceDA01Kb = null;
    private BluetoothDevice mDeviceDK01Kb = null;

    private BluetoothA2dp mProxySpk = null;
    private BluetoothProfile mProxyKb = null;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                if (BluetoothAdapter.STATE_OFF == intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                    mHandler.sendEmptyMessageDelayed(STOP_SERVICE, 500);
                }
            } else if (ACTION_SET_BT_PAIRED.equals(intent.getAction())) {
                mClickNotification = true;
                mHandler.sendEmptyMessageDelayed(BT_SETTINGS, 300);
            } else if (ACTION_NOTIFICATION_CANCELLED.equals(intent.getAction())) {
                Log.d(TAG, "ACTION_NOTIFICATION_CANCELLED");
                mHandler.sendEmptyMessageDelayed(STOP_SERVICE, 500);
            }
        }
    };

    private BroadcastReceiver mSetBTPairedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Log.v(TAG, "mSetBTPairedReceiver: " + intent.getAction().toString());
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Log.d(TAG, "searching: " + device.getName());
                if (mClickNotification) {
                    if (device.getName() != null && device.getName().equals(DEVICE_NAME_DA01_SPK) && !mPairWithDevices[INDEX_DA01_SPK]) {
                        pairDevice(device);
                    } else if (device.getName() != null && device.getName().equals(DEVICE_NAME_DA01_KB) && !mPairWithDevices[INDEX_DA01_KB]) {
                        pairDevice(device);
                    } else if (device.getName() != null && device.getName().equals(DEVICE_NAME_DK01_KB) && !mPairWithDevices[INDEX_DK01_KB]) {
                        pairDevice(device);
                    }
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                final int state = intent
                        .getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,
                                BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
                        BluetoothDevice.ERROR);
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null && device.getName().equals(DEVICE_NAME_DA01_SPK) && state == BluetoothDevice.BOND_BONDED
                        && prevState == BluetoothDevice.BOND_BONDING) {
                    mPairWithDevices[INDEX_DA01_SPK] = true;
                    Log.d(TAG, "Paired: " + device.getName());
                    //mHandler.sendEmptyMessageDelayed(CHECK_BT_DEVICES_BOND, 500);
                } else if (device.getName() != null && device.getName().equals(DEVICE_NAME_DA01_KB) && state == BluetoothDevice.BOND_BONDED
                        && prevState == BluetoothDevice.BOND_BONDING) {
                    mPairWithDevices[INDEX_DA01_KB] = true;
                    Log.d(TAG, "Paired: " + device.getName());
                    //mHandler.sendEmptyMessageDelayed(CHECK_BT_DEVICES_BOND, 500);
                } else if (device.getName() != null && device.getName().equals(DEVICE_NAME_DK01_KB) && state == BluetoothDevice.BOND_BONDED
                        && prevState == BluetoothDevice.BOND_BONDING) {
                    mPairWithDevices[INDEX_DK01_KB] = true;
                    Log.d(TAG, "Paired: " + device.getName());
                    //mHandler.sendEmptyMessageDelayed(CHECK_BT_DEVICES_BOND, 500);
                } else if (state == BluetoothDevice.BOND_NONE
                        && prevState == BluetoothDevice.BOND_BONDED) {
                    Log.e(TAG, "------------------Unpaired------------------");
                    if (device.getName() != null && device.getName().equals(DEVICE_NAME_DA01_SPK)) {
                        mPairWithDevices[INDEX_DA01_SPK] = false;
                    } else if (device.getName() != null && device.getName().equals(DEVICE_NAME_DA01_KB)) {
                        mPairWithDevices[INDEX_DA01_KB] = false;
                    } else if (device.getName() != null && device.getName().equals(DEVICE_NAME_DK01_KB)) {
                        mPairWithDevices[INDEX_DK01_KB] = false;
                    }
                } else {
                    //Log.d(TAG, "else: state- " + state + "prestate - " + prevState);
                    mHandler.sendEmptyMessageDelayed(CHECK_BT_DEVICES_BOND, 500);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                if (mClickNotification) {
                    //Log.i(TAG, "ACTION_DISCOVERY_STARTED: " + getTotalSearchTime() + "s");
                    if (getTotalSearchTime() > DISCOVERY_TIME_INTERVAL) {
                        mBluetoothAdapter.cancelDiscovery();
                        mHandler.sendEmptyMessageDelayed(STOP_SERVICE, 300);
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                if (mClickNotification) {
                    Log.i(TAG, "ACTION_DISCOVERY_FINISHED: " + getTotalSearchTime() + "s");
                    mHandler.sendEmptyMessageDelayed(CHECK_BT_DEVICES_BOND, 500);
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName() != null
                        && (device.getName().equals(DEVICE_NAME_DA01_SPK) || device.getName().equals(DEVICE_NAME_DA01_KB) || device.getName().equals(DEVICE_NAME_DK01_KB))) {
                }
                Log.i(TAG, "ACTION_ACL_DISCONNECTED: " + device.getName());
                mHandler.sendEmptyMessageDelayed(CHECK_BT_DEVICES_BOND, 500);
            }
        }
    };

    // Handler messages
    public static final int START_SERVICE = 0;
    public static final int BT_SETTINGS = 1;
    public static final int CHECK_BT_DEVICES_BOND = 2;
    public static final int CONNECT = 3;
    public static final int STOP_SERVICE = 4;

    @SuppressLint("HandlerLeak")
    private BTHandler mHandler = null;
    class BTHandler extends Handler {
        private static final String HANDLER_TAG = "BTPair Handler";
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_SERVICE: {
                    Log.d(HANDLER_TAG, "START_SERVICE");
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ACTION_SET_BT_PAIRED);
                    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
                    filter.addAction(ACTION_NOTIFICATION_CANCELLED);
                    registerReceiver(mBroadcastReceiver, filter);
                    checkBTPairedDevices();
                    break;
                }

                case BT_SETTINGS: {
                    Log.d(HANDLER_TAG, "BT_SETTINGS");
                    if (mDiscoveryTimestamp == 0) {
                        mDiscoveryTimestamp = System.currentTimeMillis();
                        registerSetBTPairedReceiver();
                        if (mBluetoothAdapter.isDiscovering()) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                        if (mPairWithDevices[INDEX_DA01_SPK]) {
                            unpairDevice(mDeviceDA01Spk);
                        }
                        if (mPairWithDevices[INDEX_DA01_KB]) {
                            unpairDevice(mDeviceDA01Kb);
                        }
                        if (mPairWithDevices[INDEX_DK01_KB]) {
                            unpairDevice(mDeviceDK01Kb);
                        }
                        mBluetoothAdapter.startDiscovery();
                    }
                    Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    break;
                }

                case CHECK_BT_DEVICES_BOND: {
                    removeMessages(CHECK_BT_DEVICES_BOND);
                    Log.v(HANDLER_TAG, "CHECK_DA01_DEVICES_BOND: " + mPairWithDevices[INDEX_DA01_SPK] + ", " + mPairWithDevices[INDEX_DA01_KB] + ", " + mPairWithDevices[INDEX_DK01_KB]);
                    if (mPairWithDevices[INDEX_DA01_SPK] && mPairWithDevices[INDEX_DA01_KB]) {
                        sendEmptyMessageDelayed(CONNECT, 500);
                    } else if (mPairWithDevices[INDEX_DK01_KB]) {
                        sendEmptyMessageDelayed(CONNECT, 500);
                    } else {
                        if (mBluetoothAdapter != null && !mBluetoothAdapter.isDiscovering()) {
                            mBluetoothAdapter.startDiscovery();
                        }
                    }
                    break;
                }

                case CONNECT: {
                    Log.v(HANDLER_TAG, "CONNECT");
                    if (mPairWithDevices[INDEX_DA01_SPK] && mPairWithDevices[INDEX_DA01_KB]) {
                        if (mDeviceDA01Spk != null) {
                            connectToDevices(mDeviceDA01Spk);
                            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, mProxySpk);
                        }
                        if (mDeviceDA01Kb != null) {
                            connectToDevices(mDeviceDA01Kb);
                            mBluetoothAdapter.closeProfileProxy(getInputDeviceHiddenConstant(), mProxyKb);
                        }
                    } else if (mPairWithDevices[INDEX_DK01_KB]) {
                        if (mDeviceDK01Kb != null) {
                            connectToDevices(mDeviceDK01Kb);
                            mBluetoothAdapter.closeProfileProxy(getInputDeviceHiddenConstant(), mProxyKb);
                        }
                    }
                    sendEmptyMessageDelayed(STOP_SERVICE, 3000);
                    break;
                }

                case STOP_SERVICE: {
                    Log.d(HANDLER_TAG, "STOP_SERVICE");
                    stopForeground(false);
                    stopSelf();
                    break;
                }
            }
        }
    };

    @Override
    public void onCreate() {
        Log.w(TAG, "onCreate");
        super.onCreate();

        if (mHandler == null) {
            mHandler = new BTHandler();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "onStartCommand");
        if (mHandler != null) {
            mHandler.obtainMessage(START_SERVICE).sendToTarget();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "onDestroy");
        super.onDestroy();
        mHandler.removeMessages(CHECK_BT_DEVICES_BOND);
        mHandler = null;
        release();
        unregisterReceiver(mBroadcastReceiver);
        if (mSetBTPairedReceiverOn) {
            unregisterReceiver(mSetBTPairedReceiver);
        }
        if (mProxySpk != null) {
            mProxySpk = null;
        }
        if (mProxyKb != null) {
            mProxyKb = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    public void checkBTPairedDevices() {
        Set<BluetoothDevice> pairedDevices;
        // Get the default adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            // get paired devices
            pairedDevices = mBluetoothAdapter.getBondedDevices();
            //Log.d(TAG, "pairedDevices: " + pairedDevices.size());

            if (pairedDevices.size() == 0) {
                showBTPairedNotification();
            } else {
                for (BluetoothDevice device : pairedDevices) {
                    // Log.d(TAG, "Device name: " + device.getName() + ", BT: " + device.getAddress());
                    if ((device != null) && (device.getName() != null)) {
                        if (device.getName().equals(DEVICE_NAME_DA01_SPK)) {
                            mPairWithDevices[INDEX_DA01_SPK] = true;
                            mDeviceDA01Spk = device;
                        } else if (device.getName().equals(DEVICE_NAME_DA01_KB)) {
                            mPairWithDevices[INDEX_DA01_KB] = true;
                            mDeviceDA01Kb = device;
                        } else if (device.getName().equals(DEVICE_NAME_DK01_KB)) {
                            mPairWithDevices[INDEX_DK01_KB] = true;
                            mDeviceDK01Kb = device;
                        }
                    }
                } // for
                if ((mPairWithDevices[INDEX_DA01_SPK] && mPairWithDevices[INDEX_DA01_KB]) || mPairWithDevices[INDEX_DK01_KB]) {
                    Log.v(TAG, "No need to bond");
                    mHandler.sendEmptyMessage(STOP_SERVICE);
                } else {
                    showBTPairedNotification();
                }
            } // else
        }
    }

    private void pairDevice(BluetoothDevice device) {
        Log.d(TAG, "pairDevice: " + device.getName());
        device.createBond();

        if (device.getName().equals(DEVICE_NAME_DA01_SPK)) {
            mDeviceDA01Spk = device;
            // Establish connection to the proxy.
            mBluetoothAdapter.getProfileProxy(BluetoothPairedService.this, mProfileListener, BluetoothProfile.A2DP);
        } else if (device.getName().equals(DEVICE_NAME_DA01_KB)) {
            mDeviceDA01Kb = device;
            // Establish connection to the proxy.
            mBluetoothAdapter.getProfileProxy(BluetoothPairedService.this, mProfileListener, getInputDeviceHiddenConstant());
        } else if (device.getName().equals(DEVICE_NAME_DK01_KB)) {
            mDeviceDK01Kb = device;
            // Establish connection to the proxy.
            mBluetoothAdapter.getProfileProxy(BluetoothPairedService.this, mProfileListener, getInputDeviceHiddenConstant());
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass()
                .getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void registerSetBTPairedReceiver() {
        mSetBTPairedReceiverOn = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(mSetBTPairedReceiver, filter);
    }

    private int getTotalSearchTime() {
        int endTimestamp = (int) ((System.currentTimeMillis()-mDiscoveryTimestamp) / 1000L);
        return endTimestamp;
    }

    public void showBTPairedNotification() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        cancelAllNotification();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_SET_BT_PAIRED), 0);
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.stat_sys_data_bluetooth_error)
                .setTicker(getString(R.string.title_remind_user_to_set_bt))
                .setContentTitle(getString(R.string.title_remind_user_to_set_bt))
                .setContentText(getString(R.string.notification_remind_user_to_set_bt))
                .setProgress(0, 0, false).setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setDeleteIntent(getDeleteIntent())
                .build();

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.asus_bt_dock_pairing_notification);
        notification.bigContentView = contentView;
        mNotificationManager.notify(SHOW_NOTIFICATION_ID, notification);
    }

    public void cancelAllNotification() {
        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
        }
    }

    public void release() {
        cancelAllNotification();
        if (mNotificationManager != null) {
            mNotificationManager.cancel(SHOW_NOTIFICATION_ID);
        }
        mNotificationManager = null;
    }

    protected PendingIntent getDeleteIntent()
    {
       Intent intent = new Intent();
       intent.setAction(ACTION_NOTIFICATION_CANCELLED);
       return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void connectToDevices(BluetoothDevice device) {
        if (device.getName().equals(DEVICE_NAME_DA01_SPK)) {
            try {
                Method connect = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
                connect.invoke(mProxySpk, device);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            //mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.A2DP);
        } else if (device.getName().equals(DEVICE_NAME_DA01_KB) || device.getName().equals(DEVICE_NAME_DK01_KB)) {
            try {
                Class c = Class.forName("android.bluetooth.BluetoothInputDevice");
                Method connect = c.getMethod("connect", BluetoothDevice.class);
                connect.invoke(mProxyKb, device);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            //mBluetoothAdapter.getProfileProxy(this, mProfileListener, getInputDeviceHiddenConstant());
        }
    }

    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.i(TAG, "onServiceConnected: " + profile); //2,4
            if (profile == BluetoothProfile.A2DP) {
                mProxySpk = (BluetoothA2dp) proxy;
            } else if (profile == getInputDeviceHiddenConstant()){
                mProxyKb = (BluetoothProfile) proxy;
            }
        }

        public void onServiceDisconnected(int profile) {
            Log.e(TAG, "onServiceDisconnected: " + profile);
        }
    };

    private int getInputDeviceHiddenConstant() {
        Class<BluetoothProfile> clazz = BluetoothProfile.class;
        for (Field f : clazz.getFields()) {
            try {
                if (f.getName().equals("INPUT_DEVICE")) {
                    return f.getInt(null);
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
        }
        return -1;
    }
}