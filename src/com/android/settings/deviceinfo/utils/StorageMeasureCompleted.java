package com.android.settings.deviceinfo.utils;

import android.util.Log;

import com.android.settingslib.deviceinfo.StorageMeasurement;

import java.lang.ref.WeakReference;

public class StorageMeasureCompleted  implements StorageMeasurement.MeasurementReceiver{

    private static final String TAG = "StorageMeasureCompleted";
    private final String mVolumeID;

    private WeakReference<OnMeasureCompletedListener> onMeasureCompletedListener;

    public interface OnMeasureCompletedListener{
        void onMeasureCompleted(StorageMeasurement.MeasurementDetails details, String volumeID);
    }

    public StorageMeasureCompleted(String volumeID){
        this.mVolumeID = volumeID;
    }

    public void setOnMeasureCompletedListener(OnMeasureCompletedListener measureCompletedListener) {
        if (onMeasureCompletedListener == null || onMeasureCompletedListener.get() == null) {
            onMeasureCompletedListener = new WeakReference<OnMeasureCompletedListener>(measureCompletedListener);
        }
    }

    @Override
    public void onDetailsChanged(StorageMeasurement.MeasurementDetails details) {
        final OnMeasureCompletedListener receiver = (onMeasureCompletedListener != null) ? onMeasureCompletedListener.get() : null;
        if(receiver!=null){
            Log.i(TAG, mVolumeID + "Completed!");
            receiver.onMeasureCompleted(details, mVolumeID);
        } else {
            Log.i(TAG, "onMeasureCompletedListener is null");
        }
    }
}
