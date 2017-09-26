package com.android.settings.analytic;

import java.util.Locale;

import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.settings.analytic.TrackerManager.TrackerName;

public class GATracker extends AnalyticTracker {

    private static Tracker sGaTracker = null;
    private final TrackerName mTrackerName;
    private static String CUSTOM_DIMENTION_PARAM = "&cd";

    public static String TRACKER_ID_WIFI = "UA-62115594-1";  //test: UA-62115594-2
    public static String TRACKER_ID_ZENMOTION = "UA-62115594-3";
    public static String PROPERTY_ID = "UA-68209866-1";// Linjack_li GA Account  AsusSettings_LockScreen
    public static String TRACKER_ID_MAIN_ENTRIES = "UA-62115594-5";  //test:UA-62115594-6
    public static String TRACKER_ID_FONTLOCALELANGUAGE = "UA-62115594-7"; //Font/Locale/Language

    private static final String[] CUSTOM_DIMENSIONS = {
        Build.MODEL , "version",
        "SYSPROP_BUILD_PRODUCT", Build.TYPE, Build.DEVICE, Build.PRODUCT
    };

    public GATracker(Context context, TrackerName trackerName) {
        Log.e("GATracker","GATracker getInstance");
        mTrackerName = trackerName;
        sGaTracker = GoogleAnalytics.getInstance(context).newTracker(mTrackerName.getTrackerID());
        initGaTracker(context);
        //GoogleAnalytics.getInstance(context).setLocalDispatchPeriod(10);
    }

    private void initGaTracker(Context context) {
        for (int i = 0; i < CUSTOM_DIMENSIONS.length; i++) {
            sGaTracker.set(getCustomDimensionKey(i + 1), CUSTOM_DIMENSIONS[i]);
        }
        sGaTracker.setSampleRate(mTrackerName.getSampleRate());
        if (!mTrackerName.isAutoTrack()) return;
        sGaTracker.enableAutoActivityTracking(true);
        sGaTracker.enableAdvertisingIdCollection(true);
        Thread.UncaughtExceptionHandler exceptionHandler = new ExceptionReporter(sGaTracker,
                Thread.getDefaultUncaughtExceptionHandler(), context);
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);
    }

    public static String getCustomDimensionKey(int index) {
        return String.format(Locale.US, "%s%d", CUSTOM_DIMENTION_PARAM, index);
    }

    @Override
    public void activityStart(Activity activity) {
        GoogleAnalytics.getInstance(activity).reportActivityStart(activity);
    }

    @Override
    public void activityStop(Activity activity) {
        GoogleAnalytics.getInstance(activity).reportActivityStop(activity);
    }

    @Override
    public void sendEvents(Context context, String category, String action, String label,
            Long value) {
        Log.e("GATracker","sendEvents");
        sGaTracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());
    }

    @Override
    public void sendTiming(Context context, String category, long intervalInMillis, String name,
            String label) {
        sGaTracker.send(new HitBuilders.TimingBuilder()
                .setCategory(category)
                .setLabel(label)
                .setValue(intervalInMillis)
                .setVariable(name)
                .build());
    }

    @Override
    public void sendException(Context context, String description, Throwable ex, boolean fatal) {
        sGaTracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(new AnalyticsExceptionParser().getDescription(description, ex))
                .setFatal(fatal)
                .build());
    }

    @Override
    public void sendView(Context context, String viewName) {
        sGaTracker.setScreenName(viewName);
        sGaTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}

